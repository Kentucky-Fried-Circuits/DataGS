package dataGS;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringEscapeUtils;



public class HistoryPointJSON {

	
	public static String toJSON(long time, Map<String, SynchronizedSummaryData> data, Map<String, ChannelDescription> chanDesc) {
		String json;

		json = "{\"time\":" + time + ","; /* open data element and add the timestamp */

		json += "\"data\": {"; /* open data array */

		Iterator<Entry<String, SynchronizedSummaryData>> it = data.entrySet().iterator();
		if ( ! data.isEmpty() ) {
			while (it.hasNext()) {
				Map.Entry<String, SynchronizedSummaryData> pairs = (Map.Entry<String, SynchronizedSummaryData>)it.next();

				if ( false==chanDesc.get(pairs.getKey()).history ) {
				//	System.err.println("## HistoryPointJSON is skipping " + pairs.getKey() + " because history is false");
					continue;
				}
				
				System.err.println("## HistoryPointJSON is adding " + pairs.getKey() + " because history is true");
				
				/* if we have have this key in the channel description map, we use the precision from there. Otherwise we
				 * just go with default precision.
				 */
				if ( pairs.getValue().mode==ChannelDescription.Modes.SAMPLE ) {
					/* just dump the current value */
					json += "\"" + StringEscapeUtils.escapeJson( pairs.getKey() ) + "\": \"" + StringEscapeUtils.escapeJson( pairs.getValue().sampleValue ) + "\"";
				} else if ( chanDesc.containsKey(pairs.getKey() )) {
					double mean = pairs.getValue().getMean();
					if ( Double.isNaN( mean ) ) {
						json += "\"" + StringEscapeUtils.escapeJson( pairs.getKey() ) + "\":null,";
					} else {
						json += "\"" + StringEscapeUtils.escapeJson( pairs.getKey() ) + "\":" + StringEscapeUtils.escapeJson( numberPrecision(pairs.getValue().getMean(), chanDesc.get( pairs.getKey() ).precision ) );
					}
				} else {
					System.err.println("# No channel description found for " + pairs.getKey() + " using default double.");
					double mean = pairs.getValue().getMean();
					if ( Double.isNaN( mean ) ) {
						json += "\"" + pairs.getKey() + "\":null," ;
					}else {

						json += "\"" + pairs.getKey() + "\":" + pairs.getValue().getMean() ;
					}
				}

				json += ",";

			}
			/* remove the last comma */
			if ( ',' == json.charAt(json.length()-1) ) {
				json = json.substring(0,json.length()-2);
			}
		}

		
		json += "}"; /* close data array */

		json += "}"; /* close whole data point element */
		return json;
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

		Iterator<Entry<String, SynchronizedSummaryData>> it = data.entrySet().iterator();
		if ( ! data.isEmpty() ) {
			while (it.hasNext()) {
				Map.Entry<String, SynchronizedSummaryData> pairs = (Map.Entry<String, SynchronizedSummaryData>)it.next();

				if ( false==chanDesc.get(pairs.getKey()).log ) {
				//	System.err.println("## HistoryPointJSON is skipping " + pairs.getKey() + " because history is false");
					continue;
				}
				
				System.err.println("## HistoryPointJSON is adding " + pairs.getKey() + " because history is true");
				
				/* if we have have this key in the channel description map, we use the precision from there. Otherwise we
				 * just go with default precision.
				 */
				if ( pairs.getValue().mode==ChannelDescription.Modes.SAMPLE ) {
					/* just dump the current value */
					csv[0] +=" \"" + StringEscapeUtils.escapeCsv(pairs.getValue().sampleValue) + "\"";
				} else if ( chanDesc.containsKey(pairs.getKey() )) {
					csv[0] += "" + StringEscapeUtils.escapeCsv(numberPrecision(pairs.getValue().getMean(), chanDesc.get( pairs.getKey() ).precision ))+"";
				} else {
					System.err.println("# No channel description found for " + pairs.getKey() + " using default double.");
					csv[0] += "" + pairs.getValue().getMean() + "" ;
				}
				csv[1] +=" \"" +StringEscapeUtils.escapeCsv(pairs.getKey())+"\",";
				csv[0] += ",";
	
			}
			/* remove the last comma */
			if ( ',' == csv[0].charAt(csv[0].length()-1) ) {
				csv[0] = csv[0].substring(0,csv[0].length()-1);
			}
			if ( ',' == csv[1].charAt(csv[1].length()-1) ) {
				csv[1] = csv[1].substring(0,csv[1].length()-1);
			}
		}


		return csv;
	}
	
	
	public static String numberPrecision(double val,int prec){
		String format = "" + val ;

		if ( prec >= 0 ) {
			format = String.format("%."+prec+"f",val);
		} else {
			//format = String.format();
			format=String.format( "%s",Math.round(Math.pow( 10, -prec ) * Math.round(val/Math.pow( 10, -prec ))));

		}
		return format;
	}
}