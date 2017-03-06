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


	int vScale=1;

	/* meta data */
	int firmware_year;
	int firmware_month;
	int firmware_day;

	/* packet ages determine what we want to insert and what we want to leave null */
	int length_inverter;









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
			data.put("age_inverter", String.valueOf(d.age));

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
		} else if ( 1 == d.id ) {
			/* remote basic with no footer */
			data.put("age_remote", String.valueOf(d.age));
			
			/* remote parameters */
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
			int r_hour_meter_reset;
			int r_is_msh_re;
			double r_low_batt_cut_out;
			int r_vac_cut_out;
			double r_float_volts;
			double r_eq_volts;
			int r_absorb_time_minutes;
			
			
			/* remote base */
			r_search_watts=d.data[1];
			data.put("r_search_watts", String.valueOf(r_search_watts));
			
			r_battery_size=d.data[2];
			if ( r_battery_size < 160)
				r_battery_size *= 10;
			else if ( r_battery_size >= 180 )
				r_battery_size = (r_battery_size-200)*100;
			data.put("r_battery_size", String.valueOf(r_battery_size));
			
			r_battery_type=d.data[3];
			data.put("r_battery_type", String.valueOf(r_battery_type));
			
			r_absorb_volts=0.0;
			if ( r_battery_type > 100 ) {
				r_absorb_volts = r_battery_type / 10.0;
				r_absorb_volts *= vScale;
			}
			data.put("r_absorb_volts", String.valueOf(r_absorb_volts));

			r_charger_amps=d.data[4];
			data.put("r_charger_amps", String.valueOf(r_charger_amps));
			
			r_input_amps=d.data[5];
			data.put("r_input_amps", String.valueOf(r_input_amps));
			
			r_revision=d.data[6]/10.0;
			data.put("r_revision", String.valueOf(r_revision));
			
			r_parallel_threshold=(d.data[7]&0x0f);
			data.put("r_parallel_threshold", String.valueOf(r_parallel_threshold));
			
			r_force_charge=(d.data[7]&0xf0)>>4;
			data.put("r_force_charge", String.valueOf(r_force_charge));
			
			r_auto_genstart=(d.data[8] & 0b111);
			data.put("r_auto_genstart", String.valueOf(r_auto_genstart));
			
			r_hour_meter_reset=(d.data[8]>>6) & 0b1;
			data.put("r_hour_meter_reset", String.valueOf(r_hour_meter_reset));
			
			r_is_msh_re=(d.data[8]>>6) & 0b1;
			data.put("r_is_msh_re", String.valueOf(r_is_msh_re));
			
			r_low_batt_cut_out=d.data[9]/10.0;
			if ( 4 == vScale )
				r_low_batt_cut_out *= 2;
			data.put("r_low_batt_cut_out", String.valueOf(r_low_batt_cut_out));
			
			r_vac_cut_out=d.data[10];
			data.put("r_vac_cut_out", String.valueOf(r_vac_cut_out));
			
			r_float_volts=d.data[11]/10.0;
			r_float_volts *= vScale;
			data.put("r_float_volts", String.valueOf(r_float_volts));
			
			r_eq_volts=(d.data[12]/10.0)*vScale + r_absorb_volts;
			data.put("r_eq_volts", String.valueOf(r_eq_volts));

			r_absorb_time_minutes=d.data[13]*6;
			data.put("r_absorb_time_minutes", String.valueOf(r_absorb_time_minutes));
			
			
			if ( debug ) {
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
				System.out.printf ("R Hour Meter Reset:    %d\n",r_hour_meter_reset);
				System.out.printf ("R Is MSH-RE:           %d\n",r_is_msh_re);
				System.out.printf ("R Low Batt Cut Out:    %2.1f\n",r_low_batt_cut_out);
				System.out.printf ("R VAC Cut Out:         %d\n",r_vac_cut_out);
				System.out.printf ("R Float Volts:         %2.1f\n",r_float_volts);
				System.out.printf ("R EQ Volts:            %2.1f\n",r_eq_volts);
				System.out.printf ("R Absorb Time Minutes: %d\n",r_absorb_time_minutes);
			} 

		} else if ( 2 == d.id ) {
			/* remote with 0xA0 footer */
			data.put("age_remote_0xA0", String.valueOf(d.age));
			
			/* remote 0xA0 footer */
			int r_hours;
			int r_minutes;	
			int r_ags_gen_run_time_minutes;
			int r_ags_start_tempF;
			double r_ags_start_vdc;
			int r_ags_quiet_time;
			
			r_hours=d.data[0];
			data.put("r_hours", String.valueOf(r_hours));
			
			r_minutes=d.data[1];
			data.put("r_minutes", String.valueOf(r_minutes));
			
			r_ags_gen_run_time_minutes=d.data[2]*6;
			data.put("r_ags_gen_run_time_minutes", String.valueOf(r_ags_gen_run_time_minutes));
			
			r_ags_start_tempF=d.data[3];
			data.put("r_ags_start_tempF", String.valueOf(r_ags_start_tempF));
			
			r_ags_start_vdc=(d.data[4]/10.0)*vScale;
			data.put("r_ags_start_vdc", String.valueOf(r_ags_start_vdc));
			
			r_ags_quiet_time=d.data[5];
			data.put("r_ags_quiet_time", String.valueOf(r_ags_quiet_time));
			
			if ( debug ) {
				System.out.printf ("R Hours:Minutes:       %02d:%02d\n",r_hours,r_minutes);
				System.out.printf ("R AGS Runtime minutes: %d\n",r_ags_gen_run_time_minutes);
				System.out.printf ("R AGS Start Temp F:    %d\n",r_ags_start_tempF);
				System.out.printf ("R AGS Start VDC:       %2.1f\n",r_ags_start_vdc);
				System.out.printf ("R AGS Quiet Time:      %d\n",r_ags_quiet_time);
			}
		} else if ( 3 == d.id ) {
			/* remote with 0xA1 footer */
			data.put("age_remote_0xA1", String.valueOf(d.age));

			/* remote 0xA1 footer */
			int r_ags_start_time_hour;
			int r_ags_start_time_minute;
			int r_ags_stop_time_hour;
			int r_ags_stop_time_minute;
			double r_ags_stop_vdc;
			int r_ags_start_delay_seconds;
			int r_ags_stop_delay_seconds;
			int r_ags_max_run_time_minutes;
			
			r_ags_start_time_minute=d.data[0]*15;
			r_ags_start_time_hour=r_ags_start_time_minute/60;
			data.put("r_ags_start_time_hour", String.valueOf(r_ags_start_time_hour));
			
			r_ags_start_time_minute -= (r_ags_start_time_hour*60);
			data.put("r_ags_start_time_minute", String.valueOf(r_ags_start_time_minute));
			
			r_ags_stop_time_minute=d.data[1]*15;
			r_ags_stop_time_hour=r_ags_stop_time_minute/60;
			data.put("r_ags_stop_time_hour", String.valueOf(r_ags_stop_time_hour));
			
			r_ags_stop_time_minute -= (r_ags_stop_time_hour*60);
			data.put("r_ags_stop_time_minute", String.valueOf(r_ags_stop_time_minute));
			
			r_ags_stop_vdc=d.data[2];
			if ( 255 != r_ags_stop_vdc )
				r_ags_stop_vdc = (d.data[2]/10.0)*vScale;
			data.put("r_ags_stop_vdc", String.valueOf(r_ags_stop_vdc));
			
			r_ags_start_delay_seconds=d.data[3];
			if ( 0x80 == ( 0x80 & r_ags_start_delay_seconds) ) {
				r_ags_start_delay_seconds = (0x7f & r_ags_start_delay_seconds)*60;
			}
			data.put("r_ags_start_delay_seconds", String.valueOf(r_ags_start_delay_seconds));
			
			r_ags_stop_delay_seconds=d.data[4];
			if ( 0x80 == ( 0x80 & r_ags_stop_delay_seconds) ) {
				r_ags_stop_delay_seconds = (0x7f & r_ags_stop_delay_seconds)*60;
			}
			data.put("r_ags_stop_delay_seconds", String.valueOf(r_ags_stop_delay_seconds));
			
			r_ags_max_run_time_minutes=d.data[5]*6;
			data.put("r_ags_max_run_time_minutes", String.valueOf(r_ags_max_run_time_minutes));
			
			
			if ( debug ) {
				System.out.printf ("R AGS Start hour:min:  %02d:%02d\n",r_ags_start_time_hour,r_ags_start_time_minute);
				System.out.printf ("R AGS Stop hour:min:   %02d:%02d\n",r_ags_stop_time_hour,r_ags_stop_time_minute);
				System.out.printf ("R AGS Stop VDC:        %2.1f\n",r_ags_stop_vdc);
				System.out.printf ("R AGS Start Delay sec: %d\n",r_ags_start_delay_seconds);
				System.out.printf ("R AGS Stop Delay sec:  %d\n",r_ags_stop_delay_seconds);
				System.out.printf ("R AGS Max Runtime min: %d\n",r_ags_max_run_time_minutes);
			}
		} else if ( 4 == d.id ) {
			/* remote with 0xA2 footer */
			data.put("age_remote_0xA2", String.valueOf(d.age));

			/* remote 0xA2 footer */
			int r_ags_start_soc;
			int r_ags_stop_soc;
			int r_ags_start_amps;
			int r_ags_start_amps_delay_seconds;
			int r_ags_stop_amps;
			int r_ags_stop_amps_delay_seconds;
			
			r_ags_start_soc=d.data[0];
			data.put("r_ags_start_soc", String.valueOf(r_ags_start_soc));
			
			r_ags_stop_soc=d.data[1];
			data.put("r_ags_stop_soc", String.valueOf(r_ags_stop_soc));
			
			r_ags_start_amps=d.data[2];
			data.put("r_ags_start_amps", String.valueOf(r_ags_start_amps));
			
			r_ags_start_amps_delay_seconds=d.data[3];
			if ( 0x80 == ( 0x80 & r_ags_start_amps_delay_seconds) ) {
				r_ags_start_amps_delay_seconds = (0x7f & r_ags_start_amps_delay_seconds)*60;
			}
			data.put("r_ags_start_amps_delay_seconds", String.valueOf(r_ags_start_amps_delay_seconds));
			
			r_ags_stop_amps=d.data[4];
			data.put("r_ags_stop_amps", String.valueOf(r_ags_stop_amps));
			
			r_ags_stop_amps_delay_seconds=d.data[5];
			if ( 0x80 == ( 0x80 & r_ags_stop_amps_delay_seconds) ) {
				r_ags_stop_amps_delay_seconds = (0x7f & r_ags_stop_amps_delay_seconds)*60;
			}
			data.put("r_ags_stop_amps_delay_seconds", String.valueOf(r_ags_stop_amps_delay_seconds));

			
			if ( debug ) {
				System.out.printf ("R AGS Start SOC:       %d\n",r_ags_start_soc);
				System.out.printf ("R AGS Stop SOC:        %d\n",r_ags_stop_soc);
				System.out.printf ("R AGS Start Amps:      %d\n",r_ags_start_amps);
				System.out.printf ("R AGS Start Amp Delay: %d\n",r_ags_start_amps_delay_seconds);
				System.out.printf ("R AGS Stop Amps:       %d\n",r_ags_stop_amps);
				System.out.printf ("R AGS Stop Amp Delay:  %d\n",r_ags_stop_amps_delay_seconds);
			}


		} else if ( 5 == d.id ) {
			/* remote with 0xA3 footer */
			data.put("age_remote_0xA3", String.valueOf(d.age));
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

			r_ags_quiet_time_begin_minute=d.data[0]*15;
			r_ags_quiet_time_begin_hour=r_ags_quiet_time_begin_minute/60;
			data.put("r_ags_quiet_time_begin_hour", String.valueOf(r_ags_quiet_time_begin_hour));
			
			r_ags_quiet_time_begin_minute -= (r_ags_quiet_time_begin_hour*60);
			data.put("r_ags_quiet_time_begin_minute", String.valueOf(r_ags_quiet_time_begin_minute));
			
			r_ags_quiet_time_end_minute=d.data[1]*15;
			r_ags_quiet_time_end_hour=r_ags_quiet_time_end_minute/60;
			data.put("r_ags_quiet_time_end_hour", String.valueOf(r_ags_quiet_time_end_hour));
			
			r_ags_quiet_time_end_minute -= (r_ags_quiet_time_end_hour*60);
			data.put("r_ags_quiet_time_end_minute", String.valueOf(r_ags_quiet_time_end_minute));
			
			r_ags_exercise_days=d.data[2];
			data.put("r_ags_exercise_days", String.valueOf(r_ags_exercise_days));
			
			r_ags_exercise_start_minute=d.data[3]*15;
			r_ags_exercise_start_hour=r_ags_exercise_start_minute/60;
			data.put("r_ags_exercise_start_hour", String.valueOf(r_ags_exercise_start_hour));
			
			r_ags_exercise_start_minute -= (r_ags_exercise_start_hour*60);
			data.put("r_ags_exercise_start_minute", String.valueOf(r_ags_exercise_start_minute));
			
			r_ags_exercise_run_time_minutes = d.data[4]*6;
			data.put("r_ags_exercise_run_time_minutes", String.valueOf(r_ags_exercise_run_time_minutes));
			
			r_ags_top_off_minutes=d.data[5];
			data.put("r_ags_top_off_minutes", String.valueOf(r_ags_top_off_minutes));

			if ( debug ) {
				System.out.printf ("R AGS Quiet Begin:     %02d:%02d\n",r_ags_quiet_time_begin_hour, r_ags_quiet_time_begin_minute);
				System.out.printf ("R AGS Quiet End:       %02d:%02d\n",r_ags_quiet_time_end_hour, r_ags_quiet_time_end_minute);
				System.out.printf ("R AGS Exercise Days:   %d\n",r_ags_exercise_days);
				System.out.printf ("R AGS Exercise Time:   %02d:%02d\n",r_ags_exercise_start_hour,r_ags_exercise_start_minute);
				System.out.printf ("R AGS Exercise Run min:%d\n",r_ags_exercise_run_time_minutes);
				System.out.printf ("R AGS Top off minutes: %d\n",r_ags_top_off_minutes);
			}
		} else if ( 6 == d.id ) {
			/* remote with 0xA4 footer */
			data.put("age_remote_0xA4", String.valueOf(d.age));
			/* remote 0xA4 footer */
			int r_ags_warm_up_seconds;
			int r_ags_cool_down_seconds;
			int r_ags_100soc_days;
			int r_ags_100soc_start_hour;
			int r_ags_100soc_start_minute;
			
			r_ags_warm_up_seconds=d.data[0];
			if ( 0x80 == ( 0x80 & r_ags_warm_up_seconds) ) {
				r_ags_warm_up_seconds = (0x7f & r_ags_warm_up_seconds)*60;
			}
			data.put("r_ags_warm_up_seconds", String.valueOf(r_ags_warm_up_seconds));
			
			r_ags_cool_down_seconds=d.data[1];
			if ( 0x80 == ( 0x80 & r_ags_cool_down_seconds) ) {
				r_ags_cool_down_seconds = (0x7f & r_ags_cool_down_seconds)*60;
			}
			data.put("r_ags_cool_down_seconds", String.valueOf(r_ags_cool_down_seconds));

			r_ags_100soc_days = d.data[2];
			data.put("r_ags_100soc_days", String.valueOf(r_ags_100soc_days));
			
			r_ags_100soc_start_minute=d.data[0]*15;
			r_ags_100soc_start_hour=r_ags_100soc_start_minute/60;
			r_ags_100soc_start_minute -= (r_ags_100soc_start_hour*60);
			data.put("r_ags_100soc_start_hour", String.valueOf(r_ags_100soc_start_hour));
			data.put("r_ags_100soc_start_minute", String.valueOf(r_ags_100soc_start_minute));

			if ( debug ) {
				System.out.printf ("R AGS Warm Up Sec:     %d\n",r_ags_warm_up_seconds);
				System.out.printf ("R AGS Cool Down Sec:   %d\n",r_ags_cool_down_seconds);
				System.out.printf ("R AGS 100% SOC Days:   %d\n",r_ags_100soc_days);
				System.out.printf ("R AGS 100% SOC Time:   %02d:%02d\n",r_ags_100soc_start_hour,r_ags_100soc_start_minute);
			}
		} else if ( 7 == d.id ) {
			/* remote with 0x80 footer */
			data.put("age_remote_0x80", String.valueOf(d.age));
			/* remote 0x80 footer */
			int r_hours;
			int r_minutes;
			int r_bmk_efficiency;
			int r_bmk_battery_size;
			
			r_hours=d.data[0];
			data.put("r_hours", String.valueOf(r_hours));
			
			r_minutes=d.data[1];
			data.put("r_minutes", String.valueOf(r_minutes));

			r_bmk_efficiency=d.data[2];
			data.put("r_bmk_efficiency", String.valueOf(r_bmk_efficiency));
			
			/* not implemented: resets at d.data[3] */
			
			r_bmk_battery_size=d.data[4]*10;
			data.put("r_bmk_battery_size", String.valueOf(r_bmk_battery_size));
			
			if ( debug ) {
				System.out.printf ("R Hours:Minutes:       %02d:%02d\n",r_hours,r_minutes);
				System.out.printf ("R BMK Batt Effic:      %d\n",r_bmk_efficiency);
				System.out.printf ("R BMK Batt Size:       %d\n",r_bmk_battery_size);
			}


		} else if ( 8 == d.id ) {
			/* remote with 0x11 footer */
			data.put("age_remote_0x11", String.valueOf(d.age));
			
			/* MSH related */
			int r_msh_shore_amps;
			int r_msh_vac_cut_out_input2;
			
			r_msh_shore_amps=d.data[0];
			data.put("r_msh_shore_amps", String.valueOf(r_msh_shore_amps));
			
			r_msh_vac_cut_out_input2=d.data[1];
			data.put("r_msh_vac_cut_out_input2", String.valueOf(r_msh_vac_cut_out_input2));
			
			if ( debug ) {
				System.out.printf ("R MSH Shore Amps:      %d\n",r_msh_shore_amps);
				System.out.printf ("R MSH VAC Cut Out In2: %d\n",r_msh_vac_cut_out_input2);				
			}

		} else if ( 9 == d.id ) {
			/* remote with 0xC0 footer */
			data.put("age_remote_0xC0", String.valueOf(d.age));

		} else if ( 10 == d.id ) {
			/* remote with 0xC1 footer */
			data.put("age_remote_0xC1", String.valueOf(d.age));

		} else if ( 11 == d.id ) {
			/* remote with 0xC2 footer */
			data.put("age_remote_0xC2", String.valueOf(d.age));

		} else if ( 12 == d.id ) {
			/* remote with 0xC3 footer */
			data.put("age_remote_0xC3", String.valueOf(d.age));

		} else if ( 13 == d.id ) {
			/* remote with 0xC4 footer */
			data.put("age_remote_0xC4", String.valueOf(d.age));

			
		} else if ( 14 == d.id ) {
			/* AGS 0xA1 */
			data.put("age_ags_0xA1", String.valueOf(d.age));

			/* AGS 0xA1 parameters */
			int a_status, a_temperature,a_gen_runtime_minutes;
			double a_voltage;
			double a_revision;

			/* AGS */
			a_status=d.data[1];
			data.put("a_status", String.valueOf(a_status) );

			a_revision=d.data[2]/10.0;
			data.put("a_revision", String.valueOf(a_revision) );

			a_temperature=d.data[3];
			data.put("a_temperature", String.valueOf(a_temperature) );

			a_gen_runtime_minutes=d.data[4]*6;
			data.put("a_gen_runtime_minutes", String.valueOf(a_gen_runtime_minutes) );

			a_voltage=(d.data[5]/10.0)*vScale;
			data.put("a_voltage", String.valueOf(a_voltage) );

			if ( debug ) {
				System.out.printf ("A Status:              %d\n",a_status);
				System.out.printf ("A Revision:            %1.1f\n",a_revision);
				System.out.printf ("A Temperature:         %d\n",a_temperature);
				System.out.printf ("A Gen Runtime Minutes: %d\n",a_gen_runtime_minutes);
				System.out.printf ("A Voltage:             %2.1f\n",a_voltage);
			}

		} else if ( 15 == d.id ) {
			/* AGS 0xA2 */
			data.put("age_ags_0xA2", String.valueOf(d.age));

			/* AGS 0xA2 parameters */
			int a_days_since_last; 		/* "1 Day is 24 Hours from when the Gen Stopped" */
			int a_days_since_last_100; 
			int a_gen_runtime_total;

			a_days_since_last=d.data[1];
			data.put("a_days_since_last", String.valueOf(a_days_since_last) );

			a_days_since_last_100=d.data[2];
			data.put("a_days_since_last_100", String.valueOf(a_days_since_last_100) );

			a_gen_runtime_total=(d.data[3]<<8) + d.data[4];
			data.put("a_gen_runtime_total", String.valueOf(a_gen_runtime_total) );

			if ( debug ) {
				System.out.printf ("A Days Since Last Run: %d\n",a_days_since_last);
				System.out.printf ("A Days Since Last 100: %d\n",a_days_since_last_100);
				System.out.printf ("A Gen Runtime Total:   %d\n",a_gen_runtime_total);
			}
		} else if ( 16 == d.id ) {
			/* RTR 0x91 */
			data.put("age_rtr", String.valueOf(d.age));

			/* RTR parameters */
			double rtr_revision;

			rtr_revision=d.data[1]/10.0;
			data.put("rtr_revision", String.valueOf(rtr_revision) );

			if ( debug ) {
				System.out.printf ("RTR Revision:          %1.1f\n",rtr_revision);
			}

		} else if ( 17 == d.id ) {
			/* BMK 0x81 */
			data.put("age_bmk", String.valueOf(d.age));

			/* BMK parameters */
			int b_state_of_charge;
			double b_dc_volts, b_dc_amps, b_dc_min_volts, b_dc_max_volts;
			int b_amph_in_out;
			double b_amph_trip;
			int b_amph_cumulative;
			int b_fault;
			double b_revision;

			b_state_of_charge=d.data[1];
			data.put("b_state_of_charge", String.valueOf(b_state_of_charge));

			b_dc_volts=((d.data[2]<<8) + d.data[3]);
			b_dc_volts /= 100.0;
			data.put("b_dc_volts", String.valueOf(b_dc_volts));

			b_dc_amps=((d.data[4]<<8) + d.data[5]);
			if ( (d.data[4]>>7)==1 ) b_dc_amps -= 65536.0; 
			b_dc_amps /= 10.0;
			data.put("b_dc_amps", String.valueOf(b_dc_amps));

			b_dc_min_volts=((d.data[6]<<8) + d.data[7]);
			b_dc_min_volts /= 100.0;
			data.put("b_dc_min_volts", String.valueOf(b_dc_min_volts));

			b_dc_max_volts=((d.data[8]<<8) + d.data[9]);
			b_dc_max_volts /= 100.0;
			data.put("b_dc_max_volts", String.valueOf(b_dc_max_volts));

			b_amph_in_out =((d.data[10]<<8) + d.data[11]);
			if ( (d.data[10]>>7)==1 ) b_amph_in_out -= 65536.0;
			data.put("b_amph_in_out", String.valueOf(b_amph_in_out));

			b_amph_trip=((d.data[12]<<8) + d.data[13]);
			b_amph_trip /= 10.0;
			data.put("b_amph_trip", String.valueOf(b_amph_trip));

			b_amph_cumulative = ((d.data[14]<<8) + d.data[15])*100;
			data.put("b_amph_cumulative", String.valueOf(b_amph_cumulative));

			b_revision=d.data[16]/10.0;
			data.put("b_revision", String.valueOf(b_revision));

			b_fault=d.data[17];
			data.put("b_fault", String.valueOf(b_fault));

			if ( debug ) {
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
		} else if ( 18 == d.id ) {
			/* PT 0xC1 */
			data.put("age_pt_0xC1", String.valueOf(d.age));
			
			/* PT 0xC1 parameters */
			int pt_charge_mode;
			int pt_regulation_mode;
			int pt_fault;
			double pt_battery_volts;
			double pt_battery_amps;
			double pt_pv_volts;
			int pt_charge_time_minutes;
			double pt_target_battery_volts;
			int pt_relay_state;
			int pt_alarm_state;
			int pt_fan;
			int pt_day;
			int pt_temp_battery;
			int pt_temp_inductor;
			int pt_temp_fet;
			
			pt_charge_mode = (d.data[2]>>3) & 0b11;	/* bits {4,3} */
			data.put("pt_charge_mode", String.valueOf(pt_charge_mode));
			
			pt_regulation_mode = d.data[2] & 0b111;	/* bits {2,1,0} */
			data.put("pt_regulation_mode", String.valueOf(pt_regulation_mode));
			
			pt_fault = d.data[3] >> 3; /* bits { 7,6,5,4,3 } */
			data.put("pt_fault", String.valueOf(pt_fault));
			
			pt_battery_volts = (d.data[4]<<8) + d.data[5];
			pt_battery_volts /= 10.0;
			data.put("pt_battery_volts", String.valueOf(pt_battery_volts));
			
			pt_battery_amps = (d.data[6]<<8) + d.data[7];
			pt_battery_amps /= 10.0;
			data.put("pt_battery_amps", String.valueOf(pt_battery_amps));
			
			pt_pv_volts = (d.data[8]<<8) + d.data[9];
			pt_pv_volts /= 10.0;
			data.put("pt_pv_volts", String.valueOf(pt_pv_volts));
			
			pt_charge_time_minutes = d.data[10] * 6;
			data.put("pt_charge_time_minutes", String.valueOf(pt_charge_time_minutes));
			
			pt_target_battery_volts = d.data[11];
			pt_target_battery_volts /= 10.0;
			pt_target_battery_volts *= vScale;	/* TODO: should this be PT battery nominal voltage */
			data.put("pt_target_battery_volts", String.valueOf(pt_target_battery_volts));
			
			pt_relay_state = (d.data[12]>>7) & 0b1;
			data.put("pt_relay_state", String.valueOf(pt_relay_state));
			
			pt_alarm_state = (d.data[12]>>6) & 0b1;
			data.put("pt_alarm_state", String.valueOf(pt_alarm_state));
			
			pt_fan = (d.data[12]>>4) & 0b1;
			data.put("pt_fan", String.valueOf(pt_fan));
			
			pt_day = (d.data[12]>>3) & 0b1;
			data.put("pt_day", String.valueOf(pt_day));
			
			pt_temp_battery = d.data[13];
			data.put("pt_temp_battery", String.valueOf(pt_temp_battery));
			
			pt_temp_inductor = d.data[14];
			data.put("pt_temp_inductor", String.valueOf(pt_temp_inductor));
			
			pt_temp_fet = d.data[15];
			data.put("pt_temp_fet", String.valueOf(pt_temp_fet));
			
			
			if ( debug ) {
				/* TODO: implement PT 0xC1 debug output */
				System.out.printf ("PT 0xC1 Charge Mode:   %d\n",pt_charge_mode);
				System.out.println("PT 0xC1 need to implement remaining debugging out");
			}
			
		} else if ( 19 == d.id ) {
			/* PT 0xC2 */
			data.put("age_pt_0xC2", String.valueOf(d.age));
			
			/* PT 0xC2 parameters */
			int pt_lifetime_kwh;
			double pt_resettable_kwh;
			double pt_ground_fault_amps;
			int pt_batt_voltage_nominal;
			int pt_stacker_info;
			/* TODO: dip switches and version not clear which is which ... need to implement */
			int pt_model;
			int pt_output_current_rating;
			int pt_input_voltage_rating;
			
			pt_lifetime_kwh = (d.data[2]<<8) + d.data[3];
			pt_lifetime_kwh *= 10;
			data.put("pt_lifetime_kwh", String.valueOf(pt_lifetime_kwh));			
			pt_resettable_kwh = (d.data[4]<<8) + d.data[5];
			pt_lifetime_kwh *= 0.1;
			data.put("pt_resettable_kwh", String.valueOf(pt_resettable_kwh));
			
			pt_ground_fault_amps = d.data[6];
			pt_ground_fault_amps *= 0.010;
			data.put("pt_ground_fault_amps", String.valueOf(pt_ground_fault_amps));
			
			pt_batt_voltage_nominal = ( d.data[7]>>6 );	/* bits 7,6 */
			if ( 0==pt_batt_voltage_nominal )
				pt_batt_voltage_nominal=12;
			else if ( 1==pt_batt_voltage_nominal )
				pt_batt_voltage_nominal=24;
			else if ( 2==pt_batt_voltage_nominal )
				pt_batt_voltage_nominal=48;
			data.put("pt_batt_voltage_nominal", String.valueOf(pt_batt_voltage_nominal));
			
			pt_stacker_info = ( d.data[7] & 0b111111 );	/* bits 5,4,3,2,1,0 */
			data.put("pt_stacker_info", String.valueOf(pt_stacker_info));
			
			/* DIP switches and/or version are probably at d.data[8] */
			
			pt_model = d.data[9];
			pt_model *= 10;
			data.put("pt_model", String.valueOf(pt_model));
			
			pt_output_current_rating = d.data[10];
			pt_output_current_rating *= 5;
			data.put("pt_output_current_rating", String.valueOf(pt_output_current_rating));
			
			pt_input_voltage_rating = d.data[11];
			pt_input_voltage_rating *= 10;
			data.put("pt_input_voltage_rating", String.valueOf(pt_input_voltage_rating));
			
			if ( debug ) {
				System.out.printf ("PT 0xC2 Lifetime kWh:  %d\n",pt_lifetime_kwh);
				System.out.printf ("PT 0xC2 Resettable kWh:%1.1f\n",pt_resettable_kwh);
				System.out.printf ("PT 0xC2 Ground Fault i:%1.2f amps\n",pt_ground_fault_amps);
				System.out.printf ("PT 0xC2 Battery v Nom: %d\n",pt_batt_voltage_nominal);
				System.out.printf ("PT 0xC2 Stacker Info:  %d\n",pt_stacker_info);
				System.out.printf ("PT 0xC2 Model:         %d\n",pt_model);
				System.out.printf ("PT 0xC2 Out Cur Rating:%d\n",pt_output_current_rating);
				System.out.printf ("PT 0xC2 In Volt Rating:%d\n",pt_input_voltage_rating);
			}
		} else if ( 20 == d.id ) {
			/* PT 0xC3 */
			data.put("age_pt_0xC3", String.valueOf(d.age));

			/* PT 0xC3 parameters */
			int pt_record_number;
			int pt_daily_pv_max_volts;
			int pt_daily_pv_max_volts_minutes;
			double pt_daily_kwh;
			double pt_daily_batt_max_volts;			/* need to scale with vScale */
			int pt_daily_batt_max_volts_minutes;
			double pt_daily_batt_min_volts;			/* need to scale with vScale */
			int pt_daily_batt_min_volts_minutes;
			int pt_daily_operational_minutes;
			int pt_daily_amp_hours;
			int pt_daily_peak_power;
			int pt_daily_peak_power_minutes;

			pt_record_number = -1;	/* TODO: PT record number not implemented */
			data.put("pt_record_number", String.valueOf(pt_record_number));

			pt_daily_pv_max_volts = d.data[4];
			data.put("pt_daily_pv_max_volts", String.valueOf(pt_daily_pv_max_volts));
			
			pt_daily_pv_max_volts_minutes = d.data[5] * 6;
			data.put("pt_daily_pv_max_volts_minutes", String.valueOf(pt_daily_pv_max_volts_minutes));

			pt_daily_kwh = d.data[6];
			pt_daily_kwh /= 10.0;
			data.put("pt_daily_kwh", String.valueOf(pt_daily_kwh));

			pt_daily_batt_max_volts = d.data[7];
			pt_daily_batt_max_volts /= 10.0;
			pt_daily_batt_max_volts *= vScale;
			data.put("pt_daily_batt_max_volts", String.valueOf(pt_daily_batt_max_volts));

			pt_daily_batt_max_volts_minutes = d.data[8] * 6;
			data.put("pt_daily_batt_max_volts_minutes", String.valueOf(pt_daily_batt_max_volts_minutes));

			pt_daily_batt_min_volts = d.data[9];
			pt_daily_batt_min_volts /= 10.0;
			pt_daily_batt_min_volts *= vScale;
			data.put("pt_daily_batt_min_volts", String.valueOf(pt_daily_batt_min_volts));

			pt_daily_batt_min_volts_minutes = d.data[10] * 6;
			data.put("pt_daily_batt_min_volts_minutes", String.valueOf(pt_daily_batt_min_volts_minutes));

			pt_daily_operational_minutes = d.data[11] * 6;
			data.put("pt_daily_operational_minutes", String.valueOf(pt_daily_operational_minutes));

			pt_daily_amp_hours = d.data[12] * 10;
			data.put("pt_daily_amp_hours", String.valueOf(pt_daily_amp_hours));

			pt_daily_peak_power = d.data[13] * 100;
			data.put("pt_daily_peak_power", String.valueOf(pt_daily_peak_power));

			pt_daily_peak_power_minutes = d.data[14] * 6;
			data.put("pt_daily_peak_power_minutes", String.valueOf(pt_daily_peak_power_minutes));

			if ( debug ) {				
				System.out.printf ("PT 0xC3 Record Number: %d\n",pt_record_number);

				System.out.printf ("PT 0xC3 Daily PV MaxV: %d\n",pt_daily_pv_max_volts);
				System.out.printf ("PT 0xC3 Daily PV Max:  %d minutes\n",pt_daily_pv_max_volts_minutes);
				System.out.printf ("PT 0xC3 Daily kWh:     %1.1f\n",pt_daily_kwh);
				System.out.printf ("PT 0xC3 Daily BattMaxV:%1.1f\n",pt_daily_batt_max_volts);
				System.out.printf ("PT 0xC3 Daily BattMaxV:%d minutes\n",pt_daily_batt_max_volts_minutes);
				System.out.printf ("PT 0xC3 Daily BattMinV:%1.1f\n",pt_daily_batt_min_volts);
				System.out.printf ("PT 0xC3 Daily BattMinV:%d minutes\n",pt_daily_batt_min_volts_minutes);
				System.out.printf ("PT 0xC3 Daily Operate :%d minutes\n",pt_daily_operational_minutes);
				System.out.printf ("PT 0xC3 Daily amp/hour:%d\n",pt_daily_amp_hours);
				System.out.printf ("PT 0xC3 Daily Peak Pow:%d\n",pt_daily_peak_power);
				System.out.printf ("PT 0xC3 Daily Peak Pow:%d minutes\n",pt_daily_peak_power_minutes);
			}
		} else if ( 21 == d.id ) {
			/* ACLD 0xD1 */
			data.put("age_acld", String.valueOf(d.age));

			/* ACLD parameters */
			int acld_fault;
			int acld_mode;
			int acld_active;
			double acld_power_out_kw;
			double acld_target_battery_volts;
			int acld_model;
			int acld_temp_fet;
			double acld_version;

			acld_fault=(d.data[1]>>4);			/* bits 7,6,5,4 */
			data.put("acld_fault", String.valueOf(acld_fault));

			acld_mode=((d.data[1]>>1) & 0x0f);	/* bits 3,2,1 */
			data.put("acld_mode", String.valueOf(acld_mode));

			acld_active=( d.data[1] & 0x01 );	/* bit 0 */
			data.put("acld_active", String.valueOf(acld_active));

			acld_power_out_kw=d.data[2];
			acld_power_out_kw  /= 10.0;
			data.put("acld_power_out_kw", String.valueOf(acld_power_out_kw));

			acld_target_battery_volts=d.data[4];
			acld_target_battery_volts /= 10.0;
			data.put("acld_target_battery_volts", String.valueOf(acld_target_battery_volts));

			acld_model=d.data[5];
			data.put("acld_model", String.valueOf(acld_model));

			acld_temp_fet=d.data[6];
			data.put("acld_temp_fet", String.valueOf(acld_temp_fet));

			acld_version=d.data[7];
			data.put("acld_version", String.valueOf(acld_version));

			if ( debug ) {
				System.out.printf ("ACLD Fault:            %df\n",acld_fault);
				System.out.printf ("ACLD Mode:             %df\n",acld_mode);
				System.out.printf ("ACLD Active:           %df\n",acld_active);
				System.out.printf ("ACLD Power Out kW:     %1.1f\n",acld_power_out_kw);
				System.out.printf ("ACLD Target Batt Volt: %1.1f\n",acld_target_battery_volts);
				System.out.printf ("ACLD Model:            %df\n",acld_model);
				System.out.printf ("ACLD FET temperature   %d\n",acld_temp_fet);
				System.out.printf ("ACLD Version:          %1.1f\n",acld_version);
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

			System.out.printf ("Length Inverter:       %d bytes\n",length_inverter);







			System.out.printf ("Firmware:                  %02d-%02d-%02d\n",firmware_year,firmware_month,firmware_day);
			System.out.println("--------------------------------------");

			//			for ( int i=100 ; i<=124 ; i++ ) {
			//				System.out.printf("# buff[%d]=0x%02X=%d\n",i,buff[i],buff[i]);
			//			}


			System.out.flush();
		}
	}

}
