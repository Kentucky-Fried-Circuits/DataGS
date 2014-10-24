package dataGS;

import java.lang.reflect.Field;
import java.util.Vector;

public class WorldDataProcessor implements WorldDataListener {
	RecordMagWeb magWeb;

	protected Vector<ChannelData> channelDataListeners;

	public void addChannelDataListener(ChannelData c) {
		channelDataListeners.add(c);

	}

	public WorldDataProcessor ( ) {
		channelDataListeners=new Vector<ChannelData>();

		magWeb=null;

	}

	@SuppressWarnings("unchecked")
	protected void reflectToDataGS(Object o) {

		/* use reflection to send off the public variables from the Record */
		Class rmw = o.getClass();
		Field rmwf[] = rmw.getFields();

		for ( int i=0 ; i<rmwf.length ; i++ ) {
			Field f = rmwf[i];

			/* send this channel to each of the channel data listeners */
			for ( int j=0 ; j<channelDataListeners.size(); j++ ) {
				try { 

					//					System.err.println("# name=>" + f.getName() + " value=" +  rmw.getDeclaredField(f.getName()).get(o)       );

					String s=rmw.getDeclaredField(f.getName()).get(o).toString();


					channelDataListeners.elementAt(j).ingest(f.getName(), s);
				} catch ( Exception e ) {
					System.err.println("# Exception while sending data to channelDataListener(s)...");
					e.printStackTrace();
				}
				channelDataListeners.elementAt(j).ingest(null, null);

			}
			
		}
	}

	public void WorldDataPacketReceived(int[] rawBuffer) {
		System.err.print("# WorldDataProcessor received packet ");

		switch ( rawBuffer[5] ) {
		case 25:
			System.err.println("(MagWeb complete packet) ");
			if ( null == magWeb ) {
				magWeb = new RecordMagWeb();
			}
			magWeb.parseRecord(rawBuffer);
			if ( magWeb.isValid() ) {
				/* use reflection to send all public variables off to DataGS */
				reflectToDataGS( (Object) magWeb);
				
			}
			break;
		default:
			System.err.println("(Un-implemented / incorrect WorldData format)");
		}

	}

}
