package dataGS;

import org.apache.commons.math3.stat.descriptive.SynchronizedSummaryStatistics;

public class AdcDouble {
	public int channel;
	public long time;
	public long n;
	public double avg;
	public double min;
	public double max;
	public double stddev;
	
	public AdcDouble(int ch, long t, SynchronizedSummaryStatistics s) {
		channel=ch;
		time=t;
		n=s.getN();
		avg=s.getMean();
		min=s.getMin();
		max=s.getMax();
		stddev=s.getStandardDeviation();
		
	}
	
	public String toString() {
		return String.format("n=%d avg=%.6f min=%.6f max=%.6f stddev=" + stddev,n,avg,min,max);
		//return "time=" + time + " n=" + n + " avg=" + avg + " min=" + min + " max=" + max + " stddev=" + stddev;
		//return "time=" + time + " n=" + n + " avg=" + avg + " min=" + min + " max=" + max + " stddev=" + stddev;
	}
}
