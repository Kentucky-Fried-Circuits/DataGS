package dataGS;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringEscapeUtils;


/**
 * static class for converting a HistoryPoint into JSON or CSV
 * @author James Jarvis
 */
public class HistoryPointExport {
	public final static String DEFAULT_DATE_LABEL="Data Date (UTC)";
	public final static String DEFAULT_MILLISECONDS_LABEL="Milliseconds";
	
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

		Iterator<Entry<String, SynchronizedSummaryData>> it = data.entrySet().iterator();
		if ( ! data.isEmpty() ) {
			while (it.hasNext()) {
				Map.Entry<String, SynchronizedSummaryData> pairs = (Map.Entry<String, SynchronizedSummaryData>)it.next();

				
				if ( !chanDesc.containsKey( pairs.getKey() ) || false==chanDesc.get(pairs.getKey()).log ) {
					continue;
				}
				
				
				//System.err.println("## HistoryPointJSON is adding " + pairs.getKey() + " because history is true");
				
				/* if we have have this key in the channel description map, we use the precision from there. Otherwise we
				 * just go with default precision.
				 */
				if ( pairs.getValue().mode==ChannelDescription.Modes.SAMPLE ) {
					/* just dump the current value */
					csvData.append( StringEscapeUtils.escapeCsv(pairs.getValue().sampleValue + "") );
				} else 
				if ( chanDesc.containsKey(pairs.getKey() )) {
					csvData.append( StringEscapeUtils.escapeCsv(numberPrecision(pairs.getValue().getMean(), chanDesc.get( pairs.getKey() ).precision )) );
				} else {
					System.err.println("# No channel description found for " + pairs.getKey() + " using default double.");
					csvData.append("" + pairs.getValue().getMean() );
				}
				
				/* print the key if the description isn't available */
				if ( ! chanDesc.containsKey(pairs.getKey()) || 0 == chanDesc.get( pairs.getKey() ).description.length() ) {
					csvHeader.append(StringEscapeUtils.escapeCsv(pairs.getKey()) + ",");
					csvToken.append(StringEscapeUtils.escapeCsv(pairs.getKey()) + ",");
				} else {
					csvHeader.append( StringEscapeUtils.escapeCsv( chanDesc.get( pairs.getKey() ).description) + ",");
					csvToken.append(StringEscapeUtils.escapeCsv(pairs.getKey()) + ",");
				}
				
				csvData.append(",");
			}
			
			
			/* remove the last commas */
			if ( ',' == csvHeader.charAt(csvHeader.length()-1) )
				csvHeader.deleteCharAt(csvHeader.length()-1);
			if ( ',' == csvData.charAt(csvData.length()-1) )
				csvData.deleteCharAt(csvData.length()-1);
			
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