package dataGS;

public interface ChannelData {
	public void ingest(String channel, double value);
	public void ingest(String channel, String value);
}
