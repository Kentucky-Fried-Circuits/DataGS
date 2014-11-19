package dataGS;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.queue.CircularFifoQueue;

public class DataRecent {
	private Map<String, ChannelDescription> channelDesc;
	private List<String> channelIndexes;
	private CircularFifoQueue<double[]> points;
	/* data we are building so we can eventually add to CircularFifoQueue */
	private double[] wValues;



	public DataRecent(int maxPoints,Map<String, ChannelDescription> c) {
		channelDesc=c;
		points=new CircularFifoQueue<double[]>(maxPoints);
		channelIndexes = new ArrayList<String>();
		channelIndexes.add("time");

		/* iterate through channel description and add history channel names to channelIndexes */
		Iterator<Entry<String, ChannelDescription>> it = channelDesc.entrySet().iterator();
		while ( it.hasNext() ) {
			Map.Entry<String, ChannelDescription> pairs = (Map.Entry<String, ChannelDescription>)it.next();
			if ( pairs.getValue().dayStats ) {
				channelIndexes.add(pairs.getKey());
			}
		}
	}


	public int size()  {
		return points.size();
	}

	public int maxSize() {
		return points.maxSize();
	}

	public void startPoint(long time) {
		/* call this at the start of a new measurement */
		wValues=new double[channelIndexes.size()];
		
		for ( int i=1 ; i<wValues.length ; i++ ) {
			wValues[i]=Double.NaN;
		}
		
		/* index zero is always timestamp */
		wValues[0]=time;
	}

	public void addChannel(String channelName, double data) {
//		System.out.println(">-> adding " + data + " to " + channelName );
		/* add channels one by one */
		int index=channelIndexes.indexOf(channelName);

		if ( -1 == index ) {
			return;
		}

		wValues[index]=data;
	}

	public void endPoint() {
		/* put into our CircularFifoQueue */
		synchronized ( points ) {
			points.add(wValues);	
		}
	}


	public String toRecentJSON() {
		StringBuilder s=new StringBuilder();
		s.append("{\"recent\": [");
		
		
		/* convert channelNames list to an array ... since we are about to hit it a million times */
		String[] channelNames = new String[channelIndexes.size()];
		for ( int i=0 ; i<channelNames.length ; i++ ) {
			/* channelName contains the name if we are going to put it into recent.json, otherwise it will be null */
			if ( channelDesc.containsKey(channelIndexes.get(i)) && channelDesc.get(channelIndexes.get(i)).recent ) {
				channelNames[i]=channelIndexes.get(i);
			} else {
				channelNames[i]=null;
			}
		}

		synchronized ( points ) {
			for ( int i=0 ; i<points.size() ; i++ ) {
				double[] point=points.get(i);
				s.append("\n{ ");
				
				s.append( UtilJSON.putDouble("time", point[0]) + ",");
				
				s.append("\"data\": {");
				for ( int j=1 ; j<point.length ; j++ ) {
					/* skip channels with null channelName */
					if ( null == channelNames[j] )
						continue;

					
					s.append( UtilJSON.putDouble(channelNames[j], point[j]));
					s.append(",\n");
				}
				
				if ( ',' == s.charAt( s.length()-2 ) ) {
					s.deleteCharAt( s.length() - 2 );
				}
				
				s.append("}");
		
				
				
				s.append("},");
			}
		}
		
		if ( ',' == s.charAt( s.length()-1 ) ) {
			s.deleteCharAt( s.length() - 1 );
		}

		s.append("]}");
		
		return s.toString();
	}
	
	public String toDayStatsJSON() {
		/* spin for the circular FIFO for each day and generate statistics for all channels within */
		int n=channelIndexes.size();
		
		double[] min = new double[n];
		double[] max = new double[n];
		double[] avg = new double[n];
		int[] count = new int[n];
		
		/* initialize our min max average values */
		for ( int i=0 ; i < min.length ; i++ ) {
			min[i]=Double.MAX_VALUE;
			max[i]= -Double.MAX_VALUE;
			avg[i]=0.0;
			count[i]=0;
		}
		
		synchronized ( points ) {
			for ( int i=0 ; i<points.size() ; i++ ) {
				double[] point=points.get(i);
				
				for ( int j=0 ; j<point.length ; j++ ) {
					if ( Double.isNaN(point[j]) ) {
						/* skip doing stats on Nan's */
						continue;
					}
					
					if ( point[j] < min[j] )
						min[j]=point[j];
					if ( point[j] > max[j] )
						max[j]=point[j];
					
					avg[j] += point[j];
					
					count[j]++;
				}
			}
		}
		
		for ( int i=0 ; i<avg.length ; i++ ) {
			avg[i]=avg[i] / count[i];
		}
		
		
		/* stats are built ... now put into JSON */
		StringBuilder sb=new StringBuilder();
		sb.append("{\"dayStats\": {");
		
		for ( int i=0 ; i<min.length ; i++ ) {
			sb.append("\n\"" + channelIndexes.get(i) + "\": {");

			sb.append( UtilJSON.putInt("n", count[i]) + ",");
			sb.append( UtilJSON.putDouble("min", min[i]) + ",");
			sb.append( UtilJSON.putDouble("max", max[i]) + ",");
			sb.append( UtilJSON.putDouble("avg", avg[i]) );

			sb.append("},");
		}
		
		if ( ',' == sb.charAt(sb.length()-1) ) {
			sb.deleteCharAt(sb.length()-1);
		}
		
		
		sb.append("\n} }\n");
		
		
//		sb.append("n(channels)=" + n + "\n");
//		sb.append("n(points)=" + points.size() + "\n");
//		
//		for ( int i=0 ; i<min.length ; i++ ) {
//			sb.append("\t[" + i + "] name=" + channelIndexes.get(i) + "min=" + min[i] + " max=" + max[i] + " avg[i]=" + avg[i] + " count[i]=" + count[i] + "\n");
//		}
		
		return sb.toString();
	}

	public String toString() {
		String s;

		s="Channel indexes:\n";
		for ( int i=0 ; i<channelIndexes.size() ; i++ ) {
			s += "[" + i + "] " + channelIndexes.get(i) + "\n";
		}

		return s;
	}

}
