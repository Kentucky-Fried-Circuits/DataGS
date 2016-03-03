package dataGS;
import java.util.Date;

/*
AMMPS generator received CAN message
'#'                 0  STX
UNIT ID PREFIX      1  First character (A-Z) for serial number
UNIT ID MSB         2  sending station ID MSB
UNIT ID LSB         3  sending station ID LSB
PACKET LENGTH       4  number of byte for packet including STX through CRC (20)
PACKET TYPE         5  type of packet we are sending (34)

CAN ID MSB          6  CAN ID MSB
CAN ID              7  CAN ID
CAN ID              8  CAN ID
CAN ID LSB          9  CAN ID LSB
CAN DATA[0]         10  CAN DATA first byte
CAN DATA[1]         11  CAN DATA
CAN DATA[2]         12 CAN DATA
CAN DATA[3]         13 CAN DATA
CAN DATA[4]         14 CAN DATA
CAN DATA[5]         15 CAN DATA
CAN DATA[6]         16 CAN DATA
CAN DATA[7]         17 CAN DATA last byte

CRC MSB             18 high byte of CRC on everything after STX and before CRC
CRC LSB             19 low byte of CRC
 */

public class RecordAMMPS_CAN_RX {
	private String serialNumber;
	public int can_id;
	public int can_data[];
	
	private int lCRC, rCRC;
	private Date rxDate;

	
	/* ammps_control software */
	/* (done) CAN ID 0xFF FF FF FF */
	public long gen_control_state_timeMillis; 
	public int gen_control_state;
	public int gen_control_switch;
	
	/* from generator */
	/* (done) CAN ID 0x18FF1720 */
	public long gen_status_timeMillis;
	public int gen_status;
	public int gen_status_switch_position;
	
	/* (done) CAN ID 18FF2300 or 18FF2320 */
	public long gen_operating_timeMillis;
	public double gen_fuel_level;
	public double gen_maint_countdown;
	public double gen_runtime;
	
	/* CAN ID 18FF2400 or 18FF2420 */
	public long gen_event_update_timeMillis;
	public int gen_event_warning;
	public int gen_event_fault;
	public int gen_event_status_estop;
	public int gen_event_status_battleshort;
	public int gen_event_status_deadbus;
	public int gen_event_status_cb;
	
	
	

	public RecordAMMPS_CAN_RX() {
		lCRC=-1;
		rCRC=-2;

		can_id=0;
		can_data=new int[8];
		
		gen_control_state_timeMillis=0;
		gen_status_timeMillis=0;
		gen_operating_timeMillis=0;
		gen_event_update_timeMillis=0;
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


	public void parseRecord(int[] buff) {
		rxDate = new Date();

		StringBuilder sb = new StringBuilder();

		/* Serial number */
		sb.append((char) buff[1]);
		sb.append( (int) (buff[2] << 8) + buff[3]); 
		serialNumber=sb.toString();
		//System.err.print("Serial Number=" + serialNumber + " ");


	
		can_id=(buff[6]<<24)+(buff[7]<<16)+(buff[8]<<8)+buff[9];
		
		for ( int i=0 ; i<8 ; i++ ) {
			can_data[i]=buff[10+i];
		}

		rCRC = (buff[18] << 8) + buff[19];
		lCRC=crc_chk(buff,1,17);

		if ( ! isValid() ) {
			return;
		}
		
		if ( 0xFFFFFFFF == can_id ) {
			/* tested with ammps_control */
			System.err.println("# AMMPS_CONTROL Packet");
			
			gen_control_state_timeMillis=System.currentTimeMillis();
			gen_control_state=can_data[0];
			gen_control_switch=can_data[1];
			
			System.err.printf("## gen_control_timeMillis: %d\n",gen_control_state_timeMillis);
			System.err.printf("##      gen_control_state: %d (0x%x)\n",gen_control_state,gen_control_state);
			System.err.printf("##     gen_control_switch: %d (0x%x)\n",gen_control_switch,gen_control_switch);
		} else if ( 0x18FF1720 == can_id ) {
			System.err.println("# AMMPS Status Packet");
			
			gen_status_timeMillis=System.currentTimeMillis();
			gen_status=(can_data[0] & 0xf);
			gen_status_switch_position=(can_data[0]>>4) & 0xff;
			
			System.err.printf("##      gen_status_timeMillis: %d\n",gen_status_timeMillis);
			System.err.printf("##                 gen_status: %d\n",gen_status);
			System.err.printf("## gen_status_switch_position: %d\n",gen_status_switch_position);
		} else if ( 0x18FF2300  == can_id ||  0x18FF2320 == can_id ) {
			/* tested with simulator */
			System.err.println("# AMMPS Generator Operating");
			
			gen_operating_timeMillis=System.currentTimeMillis();
			gen_fuel_level=((can_data[1]<<8) + can_data[0])/10.0;
			gen_maint_countdown=((can_data[3]<<8) + can_data[2])/10.0;
			gen_runtime=((can_data[6]<<16) + (can_data[5]<<8) + can_data[4])/10.0;

			System.err.printf("## gen_control_timeMillis: %d\n",gen_operating_timeMillis);
			System.err.printf("##         gen_fuel_level: %f\n",gen_fuel_level);
			System.err.printf("##    gen_maint_countdown: %f\n",gen_maint_countdown);
			System.err.printf("##            gen_runtime: %f\n",gen_runtime);
			
		} else if ( 0x18FF2400 == can_id || 0x18FF2420 == can_id ) {
			/* tested with simulator */
			System.err.println("# AMMPS Generator Event");
			
			gen_event_update_timeMillis=System.currentTimeMillis();
			gen_event_warning=(can_data[1]<<8) + can_data[0];
			gen_event_fault=(can_data[3]<<8) + can_data[2];
			gen_event_status_estop=(can_data[4]>>7) & 0x01;
			gen_event_status_battleshort=(can_data[4]>>6) & 0x01;
			gen_event_status_deadbus=(can_data[4]>>5) & 0x01;
			gen_event_status_cb=(can_data[4]>>4) & 0x01;
			
			System.err.printf("##  gen_event_update_timeMillis: %d\n",gen_event_update_timeMillis);
			System.err.printf("##            gen_event_warning: %d\n",gen_event_warning);
			System.err.printf("##              gen_event_fault: %d\n",gen_event_fault);
			System.err.printf("##       gen_event_status_estop: %d\n",gen_event_status_estop);
			System.err.printf("## gen_event_status_battleshort: %d\n",gen_event_status_battleshort);
			System.err.printf("##     gen_event_status_deadbus: %d\n",gen_event_status_deadbus);
			System.err.printf("##          gen_event_status_cb: %d\n",gen_event_status_cb);
		}
		


//		System.out.printf("# rCRC=0x%04X lCRC=0x%04X\n",rCRC,lCRC);
 

	}

}
