package dataGS;

import java.util.Iterator;

import org.apache.commons.collections4.queue.CircularFifoQueue;


/**
 * 
 * @author ian
 * 
 * an object that holds a CircularFifoQueue of data points with easy to access statistics
 * 
 */

public class DayStats {

	protected CircularFifoQueue<Stat> stats;
	protected String channel;
	
	public DayStats(int n, String ch) {

		stats = new CircularFifoQueue<Stat>( n );
		channel = ch;

	}
	
	/**
	 * 
	 * @param s add a data point to the fifo
	 */
	public void add(Stat s){
	
		stats.add( s );

		
	}
	
	
	/**
	 * 
	 * @return minimum data point in the fifo
	 */
	public double getMin(){
		
		double min = 0;
		Stat s;
		Iterator<Stat> it = stats.iterator();
		if ( it.hasNext() ) {
			s = (Stat) it.next();
			
			min = s.data;
			while ( it.hasNext() ) {
				
				s = (Stat) it.next();
				if ( min > s.data )
					min = s.data;
			
			}
		} else {
			
			return Double.NaN;
		}
		
		return min;
	}
	

	
	/**
	 * 
	 * @return maximum data point in the fifo
	 */
	public double getMax(){
		
		double max = 0;
		Stat s;
		Iterator<Stat> it = stats.iterator();
		if ( it.hasNext() ) {
			s = (Stat) it.next();
			
			max = s.data;
			while ( it.hasNext() ) {
				
				s = (Stat) it.next();
				if ( max < s.data )
					max = s.data;
			
			}
		} else {
			
			return Double.NaN;
		}
		
		return max;
	}
	
	/**
	 * 
	 * @return average of the data points in the fifo
	 */
	public double getAvg(){
		
		double avg = 0;
		
		Iterator<Stat> it = stats.iterator();
		
		while(it.hasNext()){
			Stat s = (Stat) it.next();
			avg += s.data;
			
		}
		return avg/stats.size();
	}
	/**
	 * 
	 * @return number of elements in the fifo
	 */
	public int getSize(){
		
		return stats.size();
		
	}
	/**
	 * 
	 * @return name of the channel
	 */
	public String getChannel(){
		return channel;
	}

}
