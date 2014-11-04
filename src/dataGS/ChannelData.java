package dataGS;

/**
 * Interface for accepting data that is key value.
 * @author James Jarvis
 *
 */
public interface ChannelData {
	public void ingest(String channel, String value);
}
