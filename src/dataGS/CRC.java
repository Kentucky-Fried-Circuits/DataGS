package dataGS;

import java.util.Vector;

/**
 * Calculate CRC using Modbus CRC format.
 * @author James Jarvis
 *
 */
public class CRC {
	
	public static int crc_chk(int data[],int start, int length) {
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
	
	public static int crc_chk(Vector<Integer> data, int start, int length) {
		int j;
		int reg_crc=0xFFFF;

		for ( int i=start ; i<(length+start) ; i++ ) {
			reg_crc ^= data.elementAt(i);

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

}
