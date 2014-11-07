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
			if ( pairs.getValue().history ) {
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


	public String toJSON() {
		StringBuilder s=new StringBuilder();
		s.append("{\"history\": [");
		
		
		/* convert channelNames list to an array ... since we are about to hit it a million times */
		String[] channelNames = new String[channelIndexes.size()];
		for ( int i=0 ; i<channelNames.length ; i++ ) {
			channelNames[i]=channelIndexes.get(i);
		}

		synchronized ( points ) {
			for ( int i=0 ; i<points.size() ; i++ ) {
				double[] point=points.get(i);
				s.append("\n{ ");
				
				for ( int j=0 ; j<point.length ; j++ ) {
					s.append( UtilJSON.putDouble(channelNames[j], point[j]));
					s.append(",\n");
				}
		
				if ( ',' == s.charAt( s.length()-2 ) ) {
					s.deleteCharAt( s.length() - 2 );
				}
				
				s.append("},");
			}
		}
		
		if ( ',' == s.charAt( s.length()-1 ) ) {
			s.deleteCharAt( s.length() - 1 );
		}

		s.append("]}");
		
		return s.toString();
	}
	
	public String toRecentStats() {
		return "foobar";
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
