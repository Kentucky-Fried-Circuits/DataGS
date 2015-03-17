package dataGS;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang3.StringEscapeUtils;


/**
 * static class for converting a HistoryPoint into JSON or CSV
 * @author James Jarvis
 */
public class HistoryPointExport {
	public final static String DEFAULT_DATE_LABEL="Data Date (UTC)";
	public final static String DEFAULT_MILLISECONDS_LABEL="Milliseconds";

	private static Comparator<String> comp = new Comparator<String>() {

		public int compare(String o1, String o2) {

			String[] kp1 = o1.split( "\\|" );
			String[] kp2 = o2.split( "\\|" );
			int i1;
			int i2;
			try{
				i1 = Integer.parseInt(kp1[0]+'0');
			} catch (Exception e){
				i1 = 0;
			}
			try{
				i2 = Integer.parseInt(kp2[0]+'0');
			} catch (Exception e){
				i2 = 0;
			}

			if ( i1 > i2 )
				return 1;

			if ( i1 < i2 )
				return -1;

			if ( i1 == i2 ) {
				return kp1[1].compareTo( kp2[1] );

			}

			return o1.compareTo( o2 );
		}
	};

	public static String toJSON(long time, Map<String, SynchronizedSummaryData> data, Map<String, ChannelDescription> chanDesc) {
		StringBuilder json=new StringBuilder();

		/* open data element and add the timestamp */
		json.append("{\"time\":" + time + ","); 

		/* open data array */
		json.append("\"data\": {");


		/* if we have any data points, we dump them */
		if ( ! data.isEmpty() ) {
			Map.Entry<String, SynchronizedSummaryData> pairs;
			Iterator<Entry<String, SynchronizedSummaryData>> it = data.entrySet().iterator();

			/* iterate through and add to JSON points that have history==true in channel description */
			while (it.hasNext()) {
				pairs = (Map.Entry<String, SynchronizedSummaryData>)it.next();


				if ( false==chanDesc.get(pairs.getKey()).dayStats ) {
					/* skip adding because channelDescription history is false */
					continue;
				}


				/* if we have have this key in the channel description map, we use the precision from there. Otherwise we
				 * just go with default precision.
				 */
				if ( pairs.getValue().mode==ChannelDescription.Modes.SAMPLE ) {
					/* just dump the current value */
					json.append("\"" + StringEscapeUtils.escapeJson( pairs.getKey() ) + "\": \"" + StringEscapeUtils.escapeJson( pairs.getValue().sampleValue + "" ) + "\"");

				} else if ( chanDesc.containsKey(pairs.getKey() )) {
					double mean = pairs.getValue().getMean();
					if ( Double.isNaN( mean ) ) {
						json.append("\"" + StringEscapeUtils.escapeJson( pairs.getKey() ) + "\":null,");
					} else {
						json.append("\"" + StringEscapeUtils.escapeJson( pairs.getKey() ) + "\":" + StringEscapeUtils.escapeJson( numberPrecision(pairs.getValue().getMean(), chanDesc.get( pairs.getKey() ).precision ) ));
					}

				} else {
					System.err.println("# No channel description found for " + pairs.getKey() + " using default double.");
					double mean = pairs.getValue().getMean();
					if ( Double.isNaN( mean ) ) {
						json.append("\"" + pairs.getKey() + "\":null,");
					}else {
						json.append("\"" + pairs.getKey() + "\":" + pairs.getValue().getMean());
					}
				}
				json.append( "," );
			}

			/* remove the last comma, if it exists */
			if ( ',' == json.charAt(json.length()-1) ) {
				json.deleteCharAt(json.length()-1);
			}
		}

		/* close data array */
		json.append("}");

		/* close whole data point element */
		json.append("}");

		return json.toString();
	}

	/**
	 * 
	 * @param data hash map to convert to csv
	 * @param chanDesc used for setting precision on values saved to csv
	 * @return String[0] is the data csv line and String[1] is the header csv line
	 */
	public static String[] toCSV(Map<String, SynchronizedSummaryData> data, Map<String, ChannelDescription> chanDesc) {
		/* csv[0] will be the data line and csv[1] will be the header line */
		String[] csv={"",""};
		/* csvHeader is the description and csvToken */
		StringBuilder csvHeader=new StringBuilder();
		StringBuilder csvToken=new StringBuilder();
		StringBuilder csvData=new StringBuilder();

		/* hashmap that has sortOrder|token as a key and a string array [token,header,value] */
		Map< String, String[] > tData = new TreeMap< String, String[] >(comp);

		/* lines holds a value for tData ex) {"Battery SOC","b_state_of_charge","100"} */
		/* this way we only have to iterate through one map */
		String value;
		String token;
		String header;
		synchronized(data){
			Iterator<Entry<String, SynchronizedSummaryData>> it = data.entrySet().iterator();
			if ( ! data.isEmpty() ) {
				while (it.hasNext()) {
					Map.Entry<String, SynchronizedSummaryData> pairs = (Map.Entry<String, SynchronizedSummaryData>)it.next();


					if ( !chanDesc.containsKey( pairs.getKey() ) || false==chanDesc.get(pairs.getKey()).log ) {
						continue;
					}

					/* if we have have this key in the channel description map, we use the precision from there. Otherwise we
					 * just go with default precision.
					 */
					if ( pairs.getValue().mode==ChannelDescription.Modes.SAMPLE ) {
						/* just dump the current value */
						value = StringEscapeUtils.escapeCsv(pairs.getValue().sampleValue + "");

					} else 
						if ( chanDesc.containsKey(pairs.getKey() )) {
							value = StringEscapeUtils.escapeCsv(numberPrecision(pairs.getValue().getMean(), chanDesc.get( pairs.getKey() ).precision ));
						} else {

							System.err.println("# No channel description found for " + pairs.getKey() + " using default double.");
							value ="" + pairs.getValue().getMean();
						}

					/* print the key if the description isn't available */

					if ( chanDesc.containsKey(pairs.getKey()) && null != chanDesc.get( pairs.getKey() ).description ) {
						if ( 0 == chanDesc.get( pairs.getKey() ).description.length() ) {
							header = StringEscapeUtils.escapeCsv(pairs.getKey());
							token = StringEscapeUtils.escapeCsv(pairs.getKey());
						} else {
							header = StringEscapeUtils.escapeCsv( chanDesc.get( pairs.getKey() ).description);
							token = StringEscapeUtils.escapeCsv(pairs.getKey());
						}
					} else {

						header = StringEscapeUtils.escapeCsv(pairs.getKey());
						token = StringEscapeUtils.escapeCsv(pairs.getKey());
					}

					/* a new array must be made everytime otherwise the treemap just holds a bunch of references to the same array */

					String[] lines={"","",""};
					lines[0] = token;
					lines[1] = header;
					lines[2] = value;

					tData.put( chanDesc.get( pairs.getKey() ).sortOrder+"|"+pairs.getKey(),lines);

				}


			}
		}
		Iterator<Entry<String, String[]>>xt = tData.entrySet().iterator();

		Entry<String, String[]> pairt;

		String[] line;

		while(xt.hasNext()){
			pairt=xt.next();
			line = pairt.getValue();
			csvToken.append(line[0]);
			csvHeader.append(line[1]);
			csvData.append(line[2]);
			//System.out.println(line[0]+"=>"+line[2]);
			if ( xt.hasNext() ){
				csvToken.append( "," );
				csvHeader.append( "," );
				csvData.append( "," );
			}

		}

		csv[0]=csvData.toString();
		csv[1]="\""+ DEFAULT_DATE_LABEL +"\",\""+ DEFAULT_MILLISECONDS_LABEL+"\","+csvToken.toString()+
				"\n\""+DEFAULT_DATE_LABEL +"\",\""+ DEFAULT_MILLISECONDS_LABEL+"\","+csvHeader.toString();

		return csv;
	}


	public static String numberPrecision(double val,int prec){
		if(Double.isNaN( val )){
			return "null";
		}
		String format = "" + val ;

		if ( prec >= 0 ) {
			format = String.format("%."+prec+"f",val);
		} else {
			format=String.format( "%s",Math.round(Math.pow( 10, -prec ) * Math.round(val/Math.pow( 10, -prec ))));

		}
		return format;
	}
}