package dataGS;

import org.apache.commons.math3.stat.descriptive.SynchronizedSummaryStatistics;

public class adcDouble {
	public long n;
	public double avg;
	public double min;
	public double max;
	public double stddev;
	
	public adcDouble(SynchronizedSummaryStatistics s) {
		n=s.getN();
		avg=s.getMean();
		min=s.getMin();
		max=s.getMax();
		stddev=s.getStandardDeviation();
	}
	
	public String toString() {
		return "n=" + n + " avg=" + avg + " min=" + min + " max=" + max + " stddev=" + stddev;
	}
}
