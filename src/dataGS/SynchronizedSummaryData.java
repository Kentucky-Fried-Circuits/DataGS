package dataGS;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class SynchronizedSummaryData extends SummaryStatistics {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8103864497559994427L;
	public ChannelDescription.Modes mode;
	public String sampleValue;
	
	public SynchronizedSummaryData(ChannelDescription.Modes m) {
//		System.err.println("(SynchronizedSummaryData) created with mode=" + m);
		mode=m;
	}
	
	public void addValue(String v) {
//		System.err.println("(SynchronizedSummaryData) got called with addValue(String)!");
		sampleValue=v;
	}
	
	public void setMode(ChannelDescription.Modes m) {
		mode=m;
	}
	
	public ChannelDescription.Modes getMode() {
		return mode;
	}
	
	public String toString() {
		if ( mode == ChannelDescription.Modes.SAMPLE )
			return sampleValue;
		
		return this.toString();
	}
}
