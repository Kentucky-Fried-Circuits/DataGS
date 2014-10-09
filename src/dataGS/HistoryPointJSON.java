package dataGS;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import dataGS.ChannelDescription.Modes;



public class HistoryPointJSON {

	
	public static String toJSON(long time, Map<String, SynchronizedSummaryData> data, Map<String, ChannelDescription> chanDesc) {
		String json;

		json = "{\"time\":" + time + ","; /* open data element and add the timestamp */

		json += "\"data\": {"; /* open data array */

		Iterator<Entry<String, SynchronizedSummaryData>> it = data.entrySet().iterator();
		if ( ! data.isEmpty() ) {
			while (it.hasNext()) {
				Map.Entry<String, SynchronizedSummaryData> pairs = (Map.Entry<String, SynchronizedSummaryData>)it.next();

				
				/* if we have have this key in the channel description map, we use the precision from there. Otherwise we
				 * just go with default precision.
				 */
				if ( pairs.getValue().mode==ChannelDescription.Modes.SAMPLE ) {
					/* just dump the current value */
					json += "\"" + pairs.getKey() + "\": \"" + pairs.getValue().sampleValue + "\"";
				} else if ( chanDesc.containsKey(pairs.getKey() )) {
					json += "\"" + pairs.getKey() + "\":" + numberPrecision(pairs.getValue().getMean(), chanDesc.get( pairs.getKey() ).precision );
				} else {
					System.err.println("# No channel description found for " + pairs.getKey() + " using default double.");
					json += "\"" + pairs.getKey() + "\":" + pairs.getValue().getMean() ;
				}

				if ( it.hasNext() ) {
					/* add a comma if there is another element coming up */
					json += ",";
				}

			}
		}

		json += "}"; /* close data array */

		json += "}"; /* close whole data point element */
		return json;
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