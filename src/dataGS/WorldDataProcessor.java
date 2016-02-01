package dataGS;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Vector;

public class WorldDataProcessor implements WorldDataListener {
	RecordMagWeb magWeb;
	RecordPS2Tap ps2tap;

	protected Vector<ChannelData> channelDataListeners;

	public void addChannelDataListener(ChannelData c) {
		channelDataListeners.add(c);

	}

	public WorldDataProcessor ( ) {
		channelDataListeners=new Vector<ChannelData>();

		magWeb=null;

	}


	protected void reflectToDataGS(Object o) {
		long now = System.currentTimeMillis();
				
		/* use reflection to send off the public variables from the Record */
		Class rmw = o.getClass();
		Field rmwf[] = rmw.getFields();

		
		
		for ( int i=0 ; i<rmwf.length ; i++ ) {
			Field f = rmwf[i];

			/* send this channel to each of the channel data listeners */
			for ( int j=0 ; j<channelDataListeners.size(); j++ ) {
				try { 
					String s=rmw.getDeclaredField(f.getName()).get(o).toString();
					channelDataListeners.elementAt(j).ingest(f.getName(), s);
				} catch ( Exception e ) {
					System.err.println("# Exception while sending data to channelDataListener(s)...");
					e.printStackTrace();
				}
			}
			
		}
		
		/* send a null to ingest to let it know that we have sent the complete record */
		for ( int j=0 ; j<channelDataListeners.size(); j++ ) {
			channelDataListeners.elementAt(j).ingest(null, new Long(now).toString());
		}
	}

	public void WorldDataPacketReceived(int[] rawBuffer) {
		System.err.print("# WorldDataProcessor received packet @ " + System.currentTimeMillis() + " " );
		
		switch ( rawBuffer[5] ) {
		case 14:
			System.err.println("(PS2Tap Binary packet) ");
			if ( null == ps2tap ) {
				ps2tap = new RecordPS2Tap();
			}
			ps2tap.parseRecord(rawBuffer);
			if ( ps2tap.isValid() ) {
				/* use reflection to send all public variables off to DataGS */
				reflectToDataGS( (Object) ps2tap);
				
			}
			break;
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
			System.err.println("(Un-implemented / incorrect WorldData format rawBuffer[5]=" + Integer.toString(rawBuffer[5]) + ")");
		}

		System.err.flush();
		
	}

}
