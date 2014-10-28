package dataGS;




import net.sf.json.util.JSONUtils;


import dataGS.ChannelDescription.Modes;

public class DataPoint {
	public String channel;
	public long time;
	public long n;
	public double avg;
	public double min;
	public double max;
	public double stddev;
	public String sampleValue; 
	public ChannelDescription.Modes mode;


	
	public DataPoint(String ch, long t, SynchronizedSummaryData s) {
		channel=ch;
		time=t;
		
		if ( s.mode==Modes.AVERAGE ) {
			n=s.getN();
			avg=s.getMean();
			min=s.getMin();
			max=s.getMax();
			stddev=s.getStandardDeviation();
			sampleValue=null;
		} else {
			n=1;
			sampleValue=s.sampleValue;

			avg=Double.NaN;
			min=Double.NaN;
			max=Double.NaN;
			stddev=Double.NaN;

		}

		mode=s.mode;
	}

	public String toJSON() {
		/* {
		 * "channel":"r_parallel_threshold",
		 * "time":1412886520295,
		 * "n":1,
		 * "avg":NaN,
		 * "min":NaN,
		 * "max":NaN,
		 * "stddev":NaN,
		 * "sampleValue":"6",
		 * "mode":"SAMPLE"
		 * } 
		 */
		
		String s="{ \"channel\": \"" + channel + "\",";
		s += "\"time\": " + time + ",";
		if ( mode == Modes.SAMPLE ) {
			if ( JSONUtils.isNumber(sampleValue) ) {
				s += "\"sampleValue\": " + sampleValue + ",";
			} else {
				s += "\"sampleValue\": \"" + sampleValue + "\",";
			}
		} else if ( mode == Modes.AVERAGE ) {
			s += "\"n\": " + n + ",";

			/* JSON doesn't have a way of representing NaN properly. So we make the as non-quoted nulls */
			if ( avg == Double.NaN ) {
				s += "\"avg\": null,";
			} else {
				s += "\"avg\": " + Double.toString(avg) + ",";
			}
			
			if ( min == Double.NaN ) {
				s += "\"min\": null,";
			} else {
				s += "\"min\": " + Double.toString(min) + ",";
			}
			
			if ( max == Double.NaN ) {
				s += "\"max\": null,";
			} else {
				s += "\"max\": " + Double.toString(max) + ",";
			}
			
			if ( stddev == Double.NaN ) {
				s += "\"stddev\": null,";
			} else {
				s += "\"stddev\": " + Double.toString(stddev) + ",";
			}
		}
		
		s += "\"mode\": \"" + mode + "\"";
		s += "}";
		
		return s;
	}


	public String toString() {
		if ( mode==Modes.SAMPLE ) {
			return "value=" + sampleValue + " mode=" + mode;
		}

		return String.format("n=%d avg=%.6f min=%.6f max=%.6f stddev=" + stddev + " mode=" + mode,n,avg,min,max);

	}
}
