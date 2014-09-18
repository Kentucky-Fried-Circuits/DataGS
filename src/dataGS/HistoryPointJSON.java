package dataGS;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.descriptive.SynchronizedSummaryStatistics;

public class HistoryPointJSON {

	public static String toJSON(long time, Map<Integer, SynchronizedSummaryStatistics> data) {
		String json;
		
		json = "{\"time\":" + time + ","; /* open data element and add the timestamp */
/*
[
    {
        "time": 12345,
        "data": {
            "65": 9876.54321,
            "92": 321
        }
    }
]
 */
		
		
		json += "\"data\": {"; /* open data array */
		
		Iterator<Entry<Integer, SynchronizedSummaryStatistics>> it = data.entrySet().iterator();
		if ( ! data.isEmpty() ) {
			while (it.hasNext()) {
				Map.Entry<Integer, SynchronizedSummaryStatistics> pairs = (Map.Entry<Integer, SynchronizedSummaryStatistics>)it.next();

				json += "\"" + pairs.getKey() + "\":" + pairs.getValue().getMean();

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

}
