package dataGS;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class SynchronizedSummaryData extends SummaryStatistics {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8103864497559994427L;
	public ChannelDescription.Modes mode;
	public Double sampleValue;
	
	public SynchronizedSummaryData(ChannelDescription.Modes m) {
//		System.err.println("(SynchronizedSummaryData) created with mode=" + m);
		mode=m;
	}
	
	public void addValue(Double v) {
//		System.err.println("(SynchronizedSummaryData) got called with addValue(String)!");
		if ( mode == ChannelDescription.Modes.AVERAGE ) {
			super.addValue(v);
		} else {
			sampleValue=v;
		}
	}
	
	public void setMode(ChannelDescription.Modes m) {
		mode=m;
	}
	
	public ChannelDescription.Modes getMode() {
		return mode;
	}
	
	public String toString() {
		if ( mode == ChannelDescription.Modes.SAMPLE )
			return sampleValue + "";
		return super.toString();
	}

	public Double getValueSampleOrAverage() {
		if ( mode == ChannelDescription.Modes.SAMPLE )
			return sampleValue;
		return super.getMean();
	}
	
}
