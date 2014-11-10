package dataGS;
import java.util.Date;

public class RecordMagWeb {
	private static final boolean markDate=false;

	public static final int maxAgeCurrent=250;
	private static final boolean debug=false;
//	private String serialNumber;
	private int vScale;

	/* save buffer for worldDataCollector support */
	private int[] buffer;
	
	/* meta data */
	public int sequenceNumber;
	public int firmware_year;
	public int firmware_month;
	public int firmware_day;
	
	/* packet ages determine what we want to insert and what we want to leave null */
	public int age_inverter;
	public int length_inverter;
	public int age_remote, age_remote_0xA0, age_remote_0xA1, age_remote_0xA2, age_remote_0xA3, age_remote_0xA4, age_remote_0x80;
	public int age_ags_0xA1, age_ags_0xA2;
	public int age_rtr, age_bmk;

	/* inverter parameters */
	public int i_status, i_fault;
	public double i_dc_volts;
	public int i_dc_amps, i_ac_volts_out, i_ac_volts_in;
	public int i_led_invert, i_led_charge;
	public int i_temp_battery, i_temp_transformer, i_temp_fet;
	public int i_amps_in, i_amps_out;
	public double i_ac_hz;
	public int i_stack_mode;
	public double i_revision;
	public int i_model;

	/* remote */
	public int r_search_watts;
	public int r_battery_size;
	public int r_battery_type;
	public double r_absorb_volts;
	public int r_charger_amps;
	public int r_input_amps;
	public double r_revision;
	public int r_parallel_threshold;
	public int r_force_charge;
	public int r_auto_genstart;
	public double r_low_batt_cut_out;
	public int r_vac_cut_out;
	public double r_float_volts;
	public double r_eq_volts;
	public int r_absorb_time_minutes;
	public int r_hours;
	public int r_minutes;

	/* remote 0xA0 footer */
	public int r_ags_gen_run_time_minutes;
	public int r_ags_start_tempF;
	public double r_ags_start_vdc;
	public int r_ags_quiet_time;

	/* remote 0xA1 footer */
	public int r_ags_start_time_hour;
	public int r_ags_start_time_minute;
	public int r_ags_stop_time_hour;
	public int r_ags_stop_time_minute;
	public double r_ags_stop_vdc;
	public int r_ags_start_delay_seconds;
	public int r_ags_stop_delay_seconds;
	public int r_ags_max_run_time_minutes;

	/* remote 0xA2 footer */
	public int r_ags_start_soc;
	public int r_ags_stop_soc;
	public int r_ags_start_amps;
	public int r_ags_start_amps_delay_seconds;
	public int r_ags_stop_amps;
	public int r_ags_stop_amps_delay_seconds;

	/* remote 0xA3 footer */
	public 	int r_ags_quiet_time_begin_hour;
	public int r_ags_quiet_time_begin_minute;
	public int r_ags_quiet_time_end_hour;
	public int r_ags_quiet_time_end_minute;
	public int r_ags_exercise_days;
	public int r_ags_exercise_start_hour;
	public int r_ags_exercise_start_minute;
	public int r_ags_exercise_run_time_minutes;
	public int r_ags_top_off_minutes;

	/* remote 0xA4 footer */
	public int r_ags_warm_up_seconds;
	public int r_ags_cool_down_seconds;

	/* remote 0x80 footer */
	public int r_bmk_efficiency;
	public int r_bmk_battery_size;


	/* AGS 0xA1 parameters */
	public int a_status, a_temperature,a_gen_runtime_minutes;
	public double a_voltage;
	public double a_revision;
	
	/* AGS 0xA2 parameters */
	public int a_days_since_last;

	/* BMK parameters */
	public int b_state_of_charge;
	public double b_dc_volts, b_dc_amps, b_dc_min_volts, b_dc_max_volts;
	public int b_amph_in_out;
	public double b_amph_trip;
	public int b_amph_cumulative;
	public int b_fault;
	public double b_revision;

	/* RTR parameters */
	public double rtr_revision;

	private int lCRC, rCRC;
	@SuppressWarnings("unused")
	private Date rxDate;

	/* calculated values to make it easier for us to run statistics */
	public double i_ac_volts_out_over_80,i_amps_out_inverting,i_amps_out_charging,b_dc_watts;
	
	
	public RecordMagWeb() {
		lCRC=-1;
		rCRC=-2;

		sequenceNumber=0;
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
		if ( markDate ) {
			rxDate = new Date();
		}

		
		//StringBuilder sb = new StringBuilder();

		/* Serial number */
		//sb.append((char) buff[1]);
		//if ( 'M' == buff[1] ) {
		//	sb.append('W');
		//}
		//sb.append( (int) (buff[2] << 8) + buff[3]); 
		//serialNumber=sb.toString();
		//System.err.print("Serial Number=" + serialNumber + " ");
		//}
		
		/* saving the buffer so we can change it */
		buffer = buff;
		
		if ( 25 != buff[5] || buff.length < 126 ) 
			return;

		sequenceNumber=(buff[6]<<8)+buff[7];

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

		/* inverter */
		i_status=buff[20];
		i_fault=buff[21];
		i_dc_volts=((buff[22]<<8) + buff[23]);
		i_dc_volts /= 10.0;
		
		/* as of 2012-02 this can be signed ... needed for MSH inverters */
		i_dc_amps=((buff[24]<<8) + buff[25]);
		if ( (buff[24]>>7)==1 ) i_dc_amps -= 65536;
		
		i_ac_volts_out=buff[26];
		i_ac_volts_in=buff[27];
		/* 28 inverter led */
		/* 29 charger led */
		i_revision=buff[30]/10.0;
		i_temp_battery=buff[31];
		i_temp_transformer=buff[32];
		i_temp_fet=buff[33];
		i_model=buff[34];

		if ( i_model <= 50 )
			vScale=1;
		else if ( i_model <= 107 )
			vScale=2;
		else if ( i_model < 150 )
			vScale=4;
		else
			vScale=1;

		
		i_stack_mode=buff[35];		
		
		i_amps_in=buff[36];
		if ( (buff[36]>>7)==1 ) i_amps_in -= 128;
		
		i_amps_out=buff[37];
		if ( (buff[37]>>7)==1 ) i_amps_out -= 128;
		
		/* when in charge mode, amps out is really amps in minus amps out */
		if ( 0x01==i_status || 0x02==i_status || 0x04==i_status || 0x08==i_status )
			i_amps_out=i_amps_in-i_amps_out;


		i_ac_hz=((buff[38]<<8) + buff[39]);
		i_ac_hz /= 10.0;
		if ( i_ac_hz > 80 )
			i_ac_hz=0.0;
		

		i_led_invert=(0xf0 & buff[121])>>4;
		i_led_charge=(0x0f & buff[121]);

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

		/* special cases */
	
		
		/* BMK watts */
		b_dc_watts = b_dc_volts * b_dc_amps;
		
		/* if i_ac_volts is more than 80 */
		if ( i_ac_volts_out > 80 ) {
			
			i_ac_volts_out_over_80 = i_ac_volts_out;
			
		} else {
			
			i_ac_volts_out_over_80 = Double.NaN;
			
		}
		
		/* i_amps_out when inverting */
		if ( i_amps_out<100 && 0x01!=i_status && 0x02!=i_status && 0x04!=i_status && 0x08!=i_status ) {
			
			i_amps_out_inverting = i_amps_out;
			
		} else {
			
			i_amps_out_inverting = Double.NaN;
			
		}
		
		/* i_amps_out when charging */
		if ( i_amps_out<100 && i_status>=0x01 && i_status<=0x08 ) {
			
			i_amps_out_charging = i_amps_out;
			
		} else {
			
			i_amps_out_charging = Double.NaN;
			
		}
		
		

		if ( debug ) {
			System.out.flush();
			System.out.println("");

			System.out.printf ("Sequence Number:       %d\n",sequenceNumber);
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

			if ( age_inverter < 255 ) {
				System.out.printf ("I Status:              0x%02X\n",i_status);
				System.out.printf ("I Fault:               0x%02X\n",i_fault);
				System.out.printf ("I DC Volt:             %2.1f\n",i_dc_volts);
				System.out.printf ("I DC Amps:             %d\n",i_dc_amps);
				System.out.printf ("I AC Volts Out:        %d\n",i_ac_volts_out);
				System.out.printf ("I AC Volts In:         %d\n",i_ac_volts_in);
				System.out.printf ("I Invert LED:          0x%02X\n",i_led_invert);
				System.out.printf ("I Charge LED:          0x%02X\n",i_led_charge);
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
	
	public int[] getMagWebPacket(char serialPrefix, short serialNumber){
		//TODO((serialNumber>>8)&0xFF),(byte)(serialNumber&0xFF)
		int[] ibuf = buffer;
		ibuf[1]=serialPrefix;

		ibuf[2] = ( serialNumber >> 8 ) & 0xff; 
		ibuf[3] = ( serialNumber ) & 0xff;

		int lCRC = crc_chk(ibuf,1,124);
		
		ibuf[125]= ( lCRC >> 8 ) & 0xff;
		ibuf[126]= ( lCRC ) & 0xff;
	
		return ibuf;
	}

}
