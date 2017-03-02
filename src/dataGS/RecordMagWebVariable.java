package dataGS;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Iterator;


public class RecordMagWebVariable {
	public static final int maxAgeCurrent=250;
	static final boolean debug=false;
	public String serialNumber;


	public Map<String, String> data;


	int vScale;

	/* meta data */
	int firmware_year;
	int firmware_month;
	int firmware_day;

	/* packet ages determine what we want to insert and what we want to leave null */
	int age_inverter;
	int length_inverter;
	int age_remote, age_remote_0xA0, age_remote_0xA1, age_remote_0xA2, age_remote_0xA3, age_remote_0xA4, age_remote_0x80;
	int age_ags_0xA1, age_ags_0xA2;
	int age_rtr, age_bmk;


	/* remote */
	int r_search_watts;
	int r_battery_size;
	int r_battery_type;
	double r_absorb_volts;
	int r_charger_amps;
	int r_input_amps;
	double r_revision;
	int r_parallel_threshold;
	int r_force_charge;
	int r_auto_genstart;
	double r_low_batt_cut_out;
	int r_vac_cut_out;
	double r_float_volts;
	double r_eq_volts;
	int r_absorb_time_minutes;
	int r_hours;
	int r_minutes;

	/* remote 0xA0 footer */
	int r_ags_gen_run_time_minutes;
	int r_ags_start_tempF;
	double r_ags_start_vdc;
	int r_ags_quiet_time;

	/* remote 0xA1 footer */
	int r_ags_start_time_hour;
	int r_ags_start_time_minute;
	int r_ags_stop_time_hour;
	int r_ags_stop_time_minute;
	double r_ags_stop_vdc;
	int r_ags_start_delay_seconds;
	int r_ags_stop_delay_seconds;
	int r_ags_max_run_time_minutes;

	/* remote 0xA2 footer */
	int r_ags_start_soc;
	int r_ags_stop_soc;
	int r_ags_start_amps;
	int r_ags_start_amps_delay_seconds;
	int r_ags_stop_amps;
	int r_ags_stop_amps_delay_seconds;

	/* remote 0xA3 footer */
	int r_ags_quiet_time_begin_hour;
	int r_ags_quiet_time_begin_minute;
	int r_ags_quiet_time_end_hour;
	int r_ags_quiet_time_end_minute;
	int r_ags_exercise_days;
	int r_ags_exercise_start_hour;
	int r_ags_exercise_start_minute;
	int r_ags_exercise_run_time_minutes;
	int r_ags_top_off_minutes;

	/* remote 0xA4 footer */
	int r_ags_warm_up_seconds;
	int r_ags_cool_down_seconds;

	/* remote 0x80 footer */
	int r_bmk_efficiency;
	int r_bmk_battery_size;


	/* AGS 0xA1 parameters */
	int a_status, a_temperature,a_gen_runtime_minutes;
	double a_voltage;
	double a_revision;

	/* AGS 0xA2 parameters */
	int a_days_since_last;

	/* BMK parameters */
	int b_state_of_charge;
	double b_dc_volts, b_dc_amps, b_dc_min_volts, b_dc_max_volts;
	int b_amph_in_out;
	double b_amph_trip;
	int b_amph_cumulative;
	int b_fault;
	double b_revision;

	/* RTR parameters */
	double rtr_revision;

	public int lCRC, rCRC;
	public Date rxDate;


	public RecordMagWebVariable() {
		lCRC=-1;
		rCRC=-2;

		data = new HashMap<String, String>();
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


	private void parse_DataBlock(RecordMagWebVariable_DataBlock d) {
//		System.err.println("# parse_DataBlock() received d with id='" + d.id + "' and length=" + d.data.length );

		if ( 0 == d.id ) {
			/* inverter */
			/* inverter parameters */
			int i_status, i_fault;
			double i_dc_volts;
			int i_dc_amps, i_ac_volts_out, i_ac_volts_in;
			int i_led_invert, i_led_charge;
			int i_temp_battery, i_temp_transformer, i_temp_fet;
			int i_amps_in, i_amps_out;
			double i_ac_hz;
			int i_stack_mode;
			double i_revision;
			int i_model;


			i_status=d.data[0];
			data.put("i_status", String.valueOf(i_status));

			i_fault=d.data[1];
			data.put("i_fault", String.valueOf(i_fault));

			i_dc_volts=((d.data[2]<<8) + d.data[3]);
			i_dc_volts /= 10.0;
			data.put("i_dc_volts", String.valueOf(i_dc_volts) );

			/* as of 2012-02 this can be signed ... needed for MSH inverters */
			i_dc_amps=((d.data[4]<<8) + d.data[5]);
			if ( (d.data[4]>>7)==1 ) i_dc_amps -= 65536;
			data.put("i_dc_amps", String.valueOf(i_dc_amps) );

			i_ac_volts_out=d.data[6];
			data.put("i_ac_volts_out", String.valueOf(i_ac_volts_out) );
			i_ac_volts_in=d.data[7];
			data.put("i_ac_volts_in", String.valueOf(i_ac_volts_in) );


			/* 28 inverter led */
			/* 29 charger led */

			i_revision=d.data[10]/10.0;
			data.put("i_revision", String.valueOf(i_revision) );

			i_temp_battery=d.data[11];
			data.put("i_temp_battery", String.valueOf(i_temp_battery) );

			i_temp_transformer=d.data[12];
			data.put("i_temp_transformer", String.valueOf(i_temp_transformer) );

			i_temp_fet=d.data[13];
			data.put("i_temp_fet", String.valueOf(i_temp_fet) );

			i_model=d.data[14];
			data.put("i_model", String.valueOf(i_model) );

			if ( i_model <= 50 )
				vScale=1;
			else if ( i_model <= 107 )
				vScale=2;
			else if ( i_model < 150 )
				vScale=4;
			else
				vScale=1;


			i_stack_mode=d.data[15];		
			data.put("i_stack_mode", String.valueOf(i_stack_mode) );

			i_amps_in=d.data[16];
			if ( (d.data[16]>>7)==1 ) i_amps_in -= 128;
			data.put("i_amps_in", String.valueOf(i_amps_in) );

			i_amps_out=d.data[17];
			if ( (d.data[17]>>7)==1 ) i_amps_out -= 128;

			/* when in charge mode, amps out is really amps in minus amps out */
			if ( 0x01==i_status || 0x02==i_status || 0x04==i_status || 0x08==i_status )
				i_amps_out=i_amps_in-i_amps_out;
			
			data.put("i_amps_out", String.valueOf(i_amps_out) );


			i_ac_hz=((d.data[18]<<8) + d.data[19]);
			i_ac_hz /= 10.0;
			if ( i_ac_hz > 80 )
				i_ac_hz=0.0;
			data.put("i_ac_hz", String.valueOf(i_ac_hz) );

			/* LEDs are special ... how annoying
			i_led_invert=(0xf0 & buff[121])>>4;
			i_led_charge=(0x0f & buff[121]);
			 */
			if ( debug ) {
				System.out.printf ("I Status:              0x%02X\n",i_status);
				System.out.printf ("I Fault:               0x%02X\n",i_fault);
				System.out.printf ("I DC Volt:             %2.1f\n",i_dc_volts);
				System.out.printf ("I DC Amps:             %d\n",i_dc_amps);
				System.out.printf ("I AC Volts Out:        %d\n",i_ac_volts_out);
				System.out.printf ("I AC Volts In:         %d\n",i_ac_volts_in);
//				System.out.printf ("I Invert LED:          0x%02X\n",i_led_invert);
//				System.out.printf ("I Charge LED:          0x%02X\n",i_led_charge);
				System.out.printf ("I Temp Battery:        %d\n",i_temp_battery);
				System.out.printf ("I Temp Xfrmer:         %d\n",i_temp_transformer);
				System.out.printf ("I Temp FETs:           %d\n",i_temp_fet);
				System.out.printf ("I Amps In:             %d\n",i_amps_in);
				System.out.printf ("I Amps Out:            %d\n",i_amps_out);
				System.out.printf ("I Frequency:           %2.1f\n",i_ac_hz);
				System.out.printf ("I Stack Mode:          %d\n",i_stack_mode);
				System.out.printf ("I Model:               %d\n",i_model);
				System.out.printf ("I Revision:            %1.1f\n",i_revision);
			}
		}
	}

	public void parseRecord(int[] buff) {
		int realLength=0;
		Vector<RecordMagWebVariable_DataBlock> data_block = new Vector<RecordMagWebVariable_DataBlock>();

		rxDate = new Date();

		/* start fresh */
		data.clear();

		StringBuilder sb = new StringBuilder();

		/* Serial number */
		/* extend M prefix to MW prefix */
		sb.append((char) buff[1]);
		if ( 'M' == buff[1] ) {
			sb.append('W');
		}
		sb.append( (int) (buff[2] << 8) + buff[3]); 
		serialNumber=sb.toString();

		//		System.err.print("Serial Number=" + serialNumber + " ");


		/* sanity check to make sure we have the right packet type */
		if ( 35 != buff[5] ) 
			return;

		realLength = ((buff[6]<<8) + buff[7]);

		/* now we have 0 or more data blocks */
		//		System.err.println("# RecordMagWebVariable realLength=" + realLength);

		if ( realLength > 9 ) {
			/* must have data block */
			int j=8;

			while ( (realLength-j) > 2 ) {
				//				System.err.println("# RecordMagWebVariable found data block realLength=" + realLength + " j=" + j);
				int id, age, length;

				id = buff[j++];
				age = buff[j++];
				length = buff[j++];

				data_block.add(new RecordMagWebVariable_DataBlock(id, age, Arrays.copyOfRange(buff, j, j+length)));

				j += length;
			}
		}

		//		System.err.println("# RecordMagWebVariable data_block.size()=" + data_block.size());

		if ( debug ) {
			for ( int i=0 ; i<realLength ; i++ ) {
				System.err.printf("# RecordMagWebVariable buff[%d]=0x%02X\n",i,buff[i]);
			}
		}


		/* iterate through data blocks are parse to key / value pairs for data */
		Iterator<RecordMagWebVariable_DataBlock> it = data_block.iterator();
		while ( it.hasNext() ) {
			parse_DataBlock(it.next());
		}


		/* CRC calculations */
		rCRC = (buff[realLength-2] << 8) + buff[realLength-1];
		lCRC=crc_chk(buff,1,realLength-3);



		return;
	}	

	public void extra(int[] buff) {

		/* packet ages */
		age_inverter=buff[8];
		age_remote=buff[9];
		age_remote_0xA0=buff[10];
		age_remote_0xA1=buff[11];
		age_remote_0xA2=buff[12];
		age_remote_0xA3=buff[13];
		age_remote_0xA4=buff[14];
		age_remote_0x80=buff[15];
		age_ags_0xA1=buff[16];
		age_ags_0xA2=buff[17];
		age_rtr=buff[18];
		age_bmk=buff[19];


		/* remote base */
		r_search_watts=buff[42];

		r_battery_size=buff[43];
		if ( r_battery_size < 160)
			r_battery_size *= 10;
		else if ( r_battery_size >= 180 )
			r_battery_size = (r_battery_size-200)*100;

		r_battery_type=buff[44];
		r_absorb_volts=0.0;
		if ( r_battery_type > 100 ) {
			r_absorb_volts = r_battery_type / 10.0;
			r_absorb_volts *= vScale;
		}

		r_charger_amps=buff[45];
		r_input_amps=buff[46];
		r_revision=buff[47]/10.0;
		r_parallel_threshold=(buff[48]&0x0f);
		r_force_charge=(buff[48]&0xf0)>>4;
		r_auto_genstart=buff[49];

		r_low_batt_cut_out=buff[50]/10.0;
		if ( 4 == vScale )
			r_low_batt_cut_out *= 2;

		r_vac_cut_out=buff[51];

		r_float_volts=buff[52]/10.0;
		r_float_volts *= vScale;

		r_eq_volts=(buff[53]/10.0)*vScale + r_absorb_volts;


		r_absorb_time_minutes=buff[54]*6;
		r_hours=buff[55]; /* not always (usually?) hours */
		r_minutes=buff[56]; /* not always (usually?) minutes */

		r_ags_gen_run_time_minutes=buff[59]*6;
		r_ags_start_tempF=buff[60];
		r_ags_start_vdc=(buff[61]/10.0)*vScale;
		r_ags_quiet_time=buff[62];

		r_ags_start_time_minute=buff[63]*15;
		r_ags_start_time_hour=r_ags_start_time_minute/60;
		r_ags_start_time_minute -= (r_ags_start_time_hour*60);
		r_ags_stop_time_minute=buff[64]*15;
		r_ags_stop_time_hour=r_ags_stop_time_minute/60;
		r_ags_stop_time_minute -= (r_ags_stop_time_hour*60);
		r_ags_stop_vdc=buff[65];
		if ( 255 != r_ags_stop_vdc )
			r_ags_stop_vdc = (buff[65]/10.0)*vScale;
		r_ags_start_delay_seconds=buff[66];
		if ( 0x80 == ( 0x80 & r_ags_start_delay_seconds) ) {
			r_ags_start_delay_seconds = (0x7f & r_ags_start_delay_seconds)*60;
		}
		r_ags_stop_delay_seconds=buff[67];
		if ( 0x80 == ( 0x80 & r_ags_stop_delay_seconds) ) {
			r_ags_stop_delay_seconds = (0x7f & r_ags_stop_delay_seconds)*60;
		}
		r_ags_max_run_time_minutes=buff[68]*6;

		r_ags_start_soc=buff[69];
		r_ags_stop_soc=buff[70];
		r_ags_start_amps=buff[71];
		r_ags_start_amps_delay_seconds=buff[72];
		if ( 0x80 == ( 0x80 & r_ags_start_amps_delay_seconds) ) {
			r_ags_start_amps_delay_seconds = (0x7f & r_ags_start_amps_delay_seconds)*60;
		}
		r_ags_stop_amps=buff[73];
		r_ags_stop_amps_delay_seconds=buff[74];
		if ( 0x80 == ( 0x80 & r_ags_stop_amps_delay_seconds) ) {
			r_ags_stop_amps_delay_seconds = (0x7f & r_ags_stop_amps_delay_seconds)*60;
		}

		r_ags_quiet_time_begin_minute=buff[75]*15;
		r_ags_quiet_time_begin_hour=r_ags_quiet_time_begin_minute/60;
		r_ags_quiet_time_begin_minute -= (r_ags_quiet_time_begin_hour*60);
		r_ags_quiet_time_end_minute=buff[76]*15;
		r_ags_quiet_time_end_hour=r_ags_quiet_time_end_minute/60;
		r_ags_quiet_time_end_minute -= (r_ags_quiet_time_end_hour*60);
		r_ags_exercise_days=buff[77];
		r_ags_exercise_start_minute=buff[78]*15;
		r_ags_exercise_start_hour=r_ags_exercise_start_minute/60;
		r_ags_exercise_start_minute -= (r_ags_exercise_start_hour*60);
		r_ags_exercise_run_time_minutes = buff[79]*6;
		r_ags_top_off_minutes=buff[80];

		r_ags_warm_up_seconds=buff[81];
		if ( 0x80 == ( 0x80 & r_ags_warm_up_seconds) ) {
			r_ags_warm_up_seconds = (0x7f & r_ags_warm_up_seconds)*60;
		}
		r_ags_cool_down_seconds=buff[82];
		if ( 0x80 == ( 0x80 & r_ags_cool_down_seconds) ) {
			r_ags_cool_down_seconds = (0x7f & r_ags_cool_down_seconds)*60;
		}


		r_bmk_efficiency=buff[89];
		r_bmk_battery_size=buff[91]*10;


		/* AGS */
		a_status=buff[93];
		a_revision=buff[94]/10.0;
		a_temperature=buff[95];
		a_gen_runtime_minutes=buff[96]*6;
		a_voltage=(buff[97]/10.0)*vScale;
		a_days_since_last=buff[98];

		/* RTR */
		rtr_revision=buff[103]/10.0;

		/* BMK */
		b_state_of_charge=buff[104];

		b_dc_volts=((buff[105]<<8) + buff[106]);
		b_dc_volts /= 100.0;

		b_dc_amps=((buff[107]<<8) + buff[108]);
		if ( (buff[107]>>7)==1 ) b_dc_amps -= 65536.0; 
		b_dc_amps /= 10.0;

		b_dc_min_volts=((buff[109]<<8) + buff[110]);
		b_dc_min_volts /= 100.0;

		b_dc_max_volts=((buff[111]<<8) + buff[112]);
		b_dc_max_volts /= 100.0;

		b_amph_in_out =((buff[113]<<8) + buff[114]);
		if ( (buff[113]>>7)==1 ) b_amph_in_out -= 65536.0;

		b_amph_trip=((buff[115]<<8) + buff[116]);
		b_amph_trip /= 10.0;

		b_amph_cumulative = ((buff[117]<<8) + buff[118])*100;

		b_revision=buff[119]/10.0;
		b_fault=buff[120];

		firmware_year=buff[122]+2000;
		firmware_month=buff[123];
		firmware_day=(buff[124]&0x0f);

		/* length inverter is 4 bits 
		 * ... we extend to five bits by knowing packets cannot be < 12 
		 * ... new range is 12 to 27
		 * */
		length_inverter=((buff[124]>>4) & 0x0f);
		if ( length_inverter < 12 )
			length_inverter += 16;

		/* remote CRC */
		rCRC = (buff[125] << 8) + buff[126];
		lCRC=crc_chk(buff,1,124);



		if ( debug ) {
			System.out.flush();
			System.out.println("");

			System.out.printf ("Age Inverter:          %d\n",age_inverter);
			System.out.printf ("Length Inverter:       %d bytes\n",length_inverter);
			System.out.printf ("Age Remote:            %d\n",age_remote);
			System.out.printf ("Age Remote 0xA0:       %d\n",age_remote_0xA0);
			System.out.printf ("Age Remote 0xA1:       %d\n",age_remote_0xA1);
			System.out.printf ("Age Remote 0xA2:       %d\n",age_remote_0xA2);
			System.out.printf ("Age Remote 0xA3:       %d\n",age_remote_0xA3);
			System.out.printf ("Age Remote 0xA4:       %d\n",age_remote_0xA4);
			System.out.printf ("Age Remote 0x80:       %d\n",age_remote_0x80);
			System.out.printf ("Age AGS 0xA1:          %d\n",age_ags_0xA1);
			System.out.printf ("Age AGS 0xA2:          %d\n",age_ags_0xA2);
			System.out.printf ("Age RTR:               %d\n",age_rtr);
			System.out.printf ("Age BMK:               %d\n",age_bmk);


			if ( age_remote < 255 ) {
				System.out.printf ("R Search Watts:        %d\n",r_search_watts);
				System.out.printf ("R Battery Size:        %d\n",r_battery_size);
				System.out.printf ("R Battery Type:        %d\n",r_battery_type);
				System.out.printf ("R Absorb Voltage:      %1.1f\n",r_absorb_volts);
				System.out.printf ("R Charger Amps:        %d\n",r_charger_amps);
				System.out.printf ("R Input Amps:          %d\n",r_input_amps);
				System.out.printf ("R Revision:            %1.1f\n",r_revision);
				System.out.printf ("R Parallel Threshold:  %d\n",r_parallel_threshold);
				System.out.printf ("R Force Charge:        %d\n",r_force_charge);
				System.out.printf ("R Auto Genstart:       %d\n",r_auto_genstart);
				System.out.printf ("R Low Batt Cut Out:    %2.1f\n",r_low_batt_cut_out);
				System.out.printf ("R VAC Cut Out:         %d\n",r_vac_cut_out);
				System.out.printf ("R Float Volts:         %2.1f\n",r_float_volts);
				System.out.printf ("R EQ Volts:            %2.1f\n",r_eq_volts);
				System.out.printf ("R Absorb Time Minutes: %d\n",r_absorb_time_minutes);
				System.out.printf ("R Hours:Minutes:       %02d:%02d\n",r_hours,r_minutes);
			} 

			if ( age_remote_0xA0 < 255 ) {
				System.out.printf ("R AGS Runtime minutes: %d\n",r_ags_gen_run_time_minutes);
				System.out.printf ("R AGS Start Temp F:    %d\n",r_ags_start_tempF);
				System.out.printf ("R AGS Start VDC:       %2.1f\n",r_ags_start_vdc);
				System.out.printf ("R AGS Quiet Time:      %d\n",r_ags_quiet_time);
			}

			if ( age_remote_0xA1 < 255 ) {
				System.out.printf ("R AGS Start hour:min:  %02d:%02d\n",r_ags_start_time_hour,r_ags_start_time_minute);
				System.out.printf ("R AGS Stop hour:min:   %02d:%02d\n",r_ags_stop_time_hour,r_ags_stop_time_minute);
				System.out.printf ("R AGS Stop VDC:        %2.1f\n",r_ags_stop_vdc);
				System.out.printf ("R AGS Start Delay sec: %d\n",r_ags_start_delay_seconds);
				System.out.printf ("R AGS Stop Delay sec:  %d\n",r_ags_stop_delay_seconds);
				System.out.printf ("R AGS Max Runtime min: %d\n",r_ags_max_run_time_minutes);
			}

			if ( age_remote_0xA2 < 255 ) {
				System.out.printf ("R AGS Start SOC:       %d\n",r_ags_start_soc);
				System.out.printf ("R AGS Stop SOC:        %d\n",r_ags_stop_soc);
				System.out.printf ("R AGS Start Amps:      %d\n",r_ags_start_amps);
				System.out.printf ("R AGS Start Amp Delay: %d\n",r_ags_start_amps_delay_seconds);
				System.out.printf ("R AGS Stop Amps:       %d\n",r_ags_stop_amps);
				System.out.printf ("R AGS Stop Amp Delay:  %d\n",r_ags_stop_amps_delay_seconds);
			}

			if ( age_remote_0xA3 < 255 ) {
				System.out.printf ("R AGS Quiet Begin:     %02d:%02d\n",r_ags_quiet_time_begin_hour, r_ags_quiet_time_begin_minute);
				System.out.printf ("R AGS Quiet End:       %02d:%02d\n",r_ags_quiet_time_end_hour, r_ags_quiet_time_end_minute);
				System.out.printf ("R AGS Exercise Days:   %d\n",r_ags_exercise_days);
				System.out.printf ("R AGS Exercise Time:   %02d:%02d\n",r_ags_exercise_start_hour,r_ags_exercise_start_minute);
				System.out.printf ("R AGS Exercise Run min:%d\n",r_ags_exercise_run_time_minutes);
				System.out.printf ("R AGS Top off minutes: %d\n",r_ags_top_off_minutes);
			}

			if ( age_remote_0xA4 < 255 ) {
				System.out.printf ("R AGS Warm Up Sec:     %d\n",r_ags_warm_up_seconds);
				System.out.printf ("R AGS Cool Down Sec:   %d\n",r_ags_cool_down_seconds);
			}

			if ( age_remote_0x80 < 255 ) {
				System.out.printf ("R BMK Batt Effic:      %d\n",r_bmk_efficiency);
				System.out.printf ("R BMK Batt Size:       %d\n",r_bmk_battery_size);
			}

			if ( age_ags_0xA1 < 255 ) {
				System.out.printf ("A Status:              %d\n",a_status);
				System.out.printf ("A Revision:            %1.1f\n",a_revision);
				System.out.printf ("A Temperature:         %d\n",a_temperature);
				System.out.printf ("A Gen Runtime Minutes: %d\n",a_gen_runtime_minutes);
				System.out.printf ("A Voltage:             %2.1f\n",a_voltage);
			}

			if ( age_ags_0xA2 < 255 ) {
				System.out.printf ("A Days Since Last Run: %d\n",a_days_since_last);
			}

			if ( age_bmk < 255 ) {
				System.out.printf ("B SOC:                 %d%%\n",b_state_of_charge);
				System.out.printf ("B DC Volts:            %2.1f\n",b_dc_volts);
				System.out.printf ("B DC Amps:             %2.1f\n",b_dc_amps);
				System.out.printf ("B DC Min Volts:        %2.1f\n",b_dc_min_volts);
				System.out.printf ("B DC Max Volts:        %2.1f\n",b_dc_max_volts);
				System.out.printf ("B AmpH In/Out:         %d\n",b_amph_in_out);
				System.out.printf ("B AmpH Trip:           %2.1f\n",b_amph_trip);
				System.out.printf ("B AmpH Cumulative:     %d\n",b_amph_cumulative);
				System.out.printf ("B Fault:               %d\n",b_fault);
				System.out.printf ("B Revision:            %1.1f\n",b_revision);
			}

			if ( age_rtr < 255 ) {
				System.out.printf ("RTR Revision:          %1.1f\n",rtr_revision);
			}

			System.out.printf ("Firmware:                  %02d-%02d-%02d\n",firmware_year,firmware_month,firmware_day);
			System.out.println("--------------------------------------");

			//			for ( int i=100 ; i<=124 ; i++ ) {
			//				System.out.printf("# buff[%d]=0x%02X=%d\n",i,buff[i],buff[i]);
			//			}


			System.out.flush();
		}
	}

}
