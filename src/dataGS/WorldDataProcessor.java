package dataGS;

public class WorldDataProcessor implements WorldDataListener {
	HandlerMagWeb magWeb;

	public WorldDataProcessor ( ) {
		magWeb=null;
	}
	
	public void WorldDataPacketReceived(int[] rawBuffer) {
		boolean valid=false;
		System.err.println("# WorldDataPacketReceived in WorldDataProcessor ... ");
		
		switch ( rawBuffer[5] ) {
			case 25:
				System.err.println("# MagWeb complete packet");
				if ( null == magWeb ) {
					magWeb = new HandlerMagWeb();
				}
				magWeb.processPacket(rawBuffer);
				break;
			default:
				System.err.println("# Un-implement / incorrect WorldData format");
		}

	}

}
