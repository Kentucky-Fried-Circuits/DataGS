package dataGS;
//import java.util.Date;

public class RecordPS2Tap {
	private String serialNumber;
	public int sequenceNumber;
	public int packetLength;

	/* PowerSyncII values */
	public int system_state, last_fault, user_state, autorun_enabled, autostart_count;
	public int bus_voltage, ac_voltage, dc_current, dc_voltage;
	public int ac_frequency, output_power, energy_produced;
	public int soft_grid,aio_dsp_rev;
	public int wireless_last_operation, wireless_last_register, wireless_last_result;

	private int lCRC, rCRC;
	//private Date rxDate;

	public RecordPS2Tap() {
		lCRC=-1;
		rCRC=-2;
	}

	public boolean isValid() {
		return lCRC==rCRC;
	}

	public int crc_chk(int data[],int start, int length) {
		int j;
		int reg_crc=0xFFFF;

		for ( int i=start ; i<(length+start) ; i++ ) {
			reg_crc ^= data[i];

			for ( j=0 ; j<8 ; j++ ) {
				if ( (reg_crc&0x01) == 1 ) { 
					reg_crc=(reg_crc>>1) ^ 0xA001;
				} else {
					reg_crc=reg_crc>>1;
				}
			}	
		}

		return reg_crc;
	}

	public void parseRecord(int[] buff) throws Exception {
//		rxDate = new Date();

		StringBuilder sb = new StringBuilder();

		/* Serial number */
		sb.append((char) buff[1]);
		sb.append((buff[2] << 8) + buff[3] );
		serialNumber=sb.toString();

		packetLength = buff[4];
		/* packet length */
		if ( 34 != packetLength && 42 != packetLength  ) {
			return;
		}
		

		/* packet type */
		if ( 14 != buff[5] ) {
			return;
		}

		/* remote values */
		system_state = (buff[6]<<8) + buff[7];
		last_fault = (buff[8]<<8) + buff[9];
		user_state = (buff[10]<<8) + buff[11];
		autorun_enabled = (buff[12]<<8) + buff[13];
		bus_voltage = (buff[14]<<8) + buff[15];
		ac_voltage = (buff[16]<<8) + buff[17];

		/* DC current is signed */
		dc_current = (buff[18]<<8) + buff[19];
		if ( dc_current > 32767 )
			dc_current=dc_current-65536;

		dc_voltage = (buff[20]<<8) + buff[21];
		ac_frequency = (buff[22]<<8) + buff[23];

		/* output power is signed */
		output_power = (buff[24]<<8) + buff[25];
		if ( output_power > 32767 )
			output_power=output_power-65536;

		energy_produced = (buff[26]<<8) + buff[27];

		/* DTI bug ... jumps from 29825 to 35710 per Andrew's e-mail */
		if ( energy_produced > 29825 ) {
			energy_produced = energy_produced - (35710-29825);
		}
		
		
		autostart_count = (buff[28]<<8) + buff[29];

		/* sequence number */
		sequenceNumber = (buff[30] << 8) + buff[31];

		if ( 34 == buff[4] ) {
			/* remote CRC */
			rCRC=(buff[32] << 8) + buff[33];
			lCRC=crc_chk(buff,1,31);
		} else if ( 42 == buff[4] ) {
			/* additional data added on 2011-05-27 */
			soft_grid = (buff[32] << 8) + buff[33];
			aio_dsp_rev = (buff[34] << 8) + buff[35];
			wireless_last_operation = buff[36];
			wireless_last_register = buff[37];
			wireless_last_result = (buff[38] << 8) + buff[39];

			/* remote CRC */
			rCRC=(buff[40] << 8) + buff[41];
			lCRC=crc_chk(buff,1,39);
		}
		
		
	}	
}
