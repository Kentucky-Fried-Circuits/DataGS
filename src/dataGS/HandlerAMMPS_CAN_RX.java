package dataGS;

public class HandlerAMMPS_CAN_RX { // implements HandlerInterfaceBinary {	
	public HandlerAMMPS_CAN_RX () {
	}

	public boolean processPacket(int[] rawBuffer, Log log) {
		RecordAMMPS_CAN_RX r = new RecordAMMPS_CAN_RX();
		r.parseRecord(rawBuffer);

		/* make sure we have a valid CRC */
		if ( true != r.isValid() ) {
			return false;
		}

		if ( false ) {
			System.out.println("### AMMPS CAN RX ###");
			System.out.printf("# CAN ID: 0x%X\n",r.can_id);
			for ( int i=0 ; i<r.can_data.length ; i++ ) {
				System.out.printf("# CAN_DATA[%d]=0x%02X\n",i,r.can_data[i]);
			}
		}

		return true;

	}
}
