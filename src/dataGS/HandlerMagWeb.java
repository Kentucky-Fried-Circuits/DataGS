package dataGS;
import java.text.DecimalFormat;




public class HandlerMagWeb { 	

	public boolean processPacket(int[] rawBuffer) {
		RecordMagWeb r = new RecordMagWeb();
		r.parseRecord(rawBuffer);

		/* make sure we have a valid CRC */
		if ( true != r.isValid() ) {
			return false;
		}

		StringBuilder sb=new StringBuilder();
		StringBuilder va=new StringBuilder();
		DecimalFormat df1 = new DecimalFormat("0.0");
		DecimalFormat df2 = new DecimalFormat("0.00");

		String table = "magWeb_" + r.serialNumber;

		sb.append("INSERT INTO ");
		sb.append(table);
		/* mandatory columns */
		sb.append(	" (packet_date, sequenceNumber, firmware_date, age_inverter, length_inverter, age_remote, " +
					" age_remote_0xA0, age_remote_0xA1, age_remote_0xA2, age_remote_0xA3, age_remote_0xA4, " + 
					"age_remote_0x80, age_ags_0xA1, age_ags_0xA2, age_rtr, age_bmk, ");
		
		va.append("now(), ");
		va.append(r.sequenceNumber + ", ");
		va.append("'" + r.firmware_year + "-" + r.firmware_month + "-" + r.firmware_day + "',");
		va.append(r.age_inverter + ", ");
		if ( r.length_inverter > 0 ) {
			va.append(r.length_inverter + ", ");
		} else {
			va.append("null, ");
		}
		va.append(r.age_remote + ", ");
		
		va.append(r.age_remote_0xA0 + ", ");
		va.append(r.age_remote_0xA1 + ", ");
		va.append(r.age_remote_0xA2 + ", ");
		va.append(r.age_remote_0xA3 + ", ");
		va.append(r.age_remote_0xA4 + ", ");
		
		va.append(r.age_remote_0x80 + ", ");
		va.append(r.age_ags_0xA1 + ", ");
		va.append(r.age_ags_0xA2 + ", ");
		va.append(r.age_rtr + ", ");
		va.append(r.age_bmk + ", ");
		
		
		/* add individual devices if we have recent data from them */
		/* inverter */
		if ( r.age_inverter < RecordMagWeb.maxAgeCurrent ) {
			sb.append(	"i_status, i_fault, i_dc_volts, i_dc_amps, i_ac_volts_out, i_ac_volts_in, " +
					"i_led_invert, i_led_charge, i_temp_battery, i_temp_transformer, i_temp_fet, " + 
					"i_amps_in, i_amps_out, i_ac_hz, i_stack_mode, i_revision, i_model, ");

			va.append(r.i_status + ", ");
			va.append(r.i_fault + ", ");
			va.append(df1.format(r.i_dc_volts) + ", ");
			va.append(r.i_dc_amps + ", ");
			va.append(r.i_ac_volts_out + ", ");
			va.append(r.i_ac_volts_in + ", ");
		
			va.append(r.i_led_invert + ", ");
			va.append(r.i_led_charge + ", ");
			va.append(r.i_temp_battery + ", ");
			va.append(r.i_temp_transformer + ", ");
			va.append(r.i_temp_fet + ", ");
		
			if ( r.length_inverter >= 20 || 0==r.length_inverter ) {
				va.append(r.i_amps_in + ", ");
				va.append(r.i_amps_out + ", ");
				va.append(df1.format(r.i_ac_hz) + ", ");
				va.append(r.i_stack_mode + ", ");
			} else {
				va.append("null, "); /* amps in */
				va.append("null, "); /* amps out */
				va.append("null, "); /* ac hz */
				va.append("null, "); /* stack mode */
			}
			va.append(df1.format(r.i_revision) + ", ");
			va.append(r.i_model + ", ");
		}
		/* remote base */
		if ( r.age_remote < RecordMagWeb.maxAgeCurrent ) {
			sb.append(	"r_search_watts, r_battery_size, r_battery_type, r_absorb_volts, " +
						"r_charger_amps, r_input_amps, r_revision, r_parallel_threshold, " + 
						"r_force_charge, r_auto_genstart, r_low_batt_cut_out, r_vac_cut_out, " +
						"r_float_volts, r_eq_volts, r_absorb_time, ");
			
			va.append(r.r_search_watts + ", ");
			va.append(r.r_battery_size + ", ");
			va.append(r.r_battery_type + ", ");
			va.append(df1.format(r.r_absorb_volts) + ", ");
			
			va.append(r.r_charger_amps + ", ");
			va.append(r.r_input_amps + ", ");
			va.append(df1.format(r.r_revision) + ", ");
			va.append(r.r_parallel_threshold + ", ");
			
			va.append(r.r_force_charge + ", ");
			va.append(r.r_auto_genstart + ", ");
			va.append(df1.format(r.r_low_batt_cut_out) + ", ");
			va.append(r.r_vac_cut_out + ", ");
			
			va.append(df1.format(r.r_float_volts) + ", ");
			va.append(df1.format(r.r_eq_volts) + ", ");
			va.append("SEC_TO_TIME( " + r.r_absorb_time_minutes*60 + "), ");
		}
		
		/* remote AGS 0xA0 */
		if ( r.age_remote_0xA0 < RecordMagWeb.maxAgeCurrent ) {
			sb.append(	"r_ags_gen_run_time, r_ags_start_tempF, r_ags_start_vdc, r_ags_quiet_time, ");
			
			va.append("SEC_TO_TIME(" + r.r_ags_gen_run_time_minutes*60 + "), ");
			va.append(r.r_ags_start_tempF + ", ");
			va.append(df1.format(r.r_ags_start_vdc) + ", ");
			va.append(r.r_ags_quiet_time + ", ");
		}

		/* remote AGS 0xA1 */
		if ( r.age_remote_0xA1 < RecordMagWeb.maxAgeCurrent ) {
			sb.append(	"r_ags_start_time, r_ags_stop_time, r_ags_stop_vdc, r_ags_start_delay, " +
						"r_ags_stop_delay, r_ags_max_run_time, ");					
			
			va.append("'" + r.r_ags_start_time_hour + ":" + r.r_ags_start_time_minute + ":00', ");
			va.append("'" + r.r_ags_stop_time_hour + ":" + r.r_ags_stop_time_minute + ":00', ");
			va.append(df1.format(r.r_ags_stop_vdc) + ", ");
			va.append("SEC_TO_TIME(" + r.r_ags_start_delay_seconds + "), ");

			va.append("SEC_TO_TIME(" + r.r_ags_stop_delay_seconds + "), ");
			va.append("SEC_TO_TIME(" + r.r_ags_max_run_time_minutes*60 + "), ");
			
		}
		
		/* remote AGS 0xA2 */
		if ( r.age_remote_0xA2 < RecordMagWeb.maxAgeCurrent ) {
			sb.append(	"r_ags_start_soc, r_ags_stop_soc, r_ags_start_amps, r_ags_start_amps_delay, " +
						"r_ags_stop_amps, r_ags_stop_amps_delay, ");
			
			va.append(r.r_ags_start_soc + ", ");
			va.append(r.r_ags_stop_soc + ", ");
			va.append(r.r_ags_start_amps + ", ");
			va.append("SEC_TO_TIME(" + r.r_ags_start_amps_delay_seconds + "), ");
			
			va.append(r.r_ags_stop_amps + ", ");
			va.append("SEC_TO_TIME(" + r.r_ags_stop_amps_delay_seconds + "), ");
		}
		
		/* remote AGS 0xA3 */
		if ( r.age_remote_0xA3 < RecordMagWeb.maxAgeCurrent ) {
			sb.append(	"r_ags_quiet_time_begin, r_ags_quiet_time_end, r_ags_exercise_days, " + 
						"r_ags_exercise_start, r_ags_exercise_run_time, r_ags_top_off, ");
			
			va.append("'" + r.r_ags_quiet_time_begin_hour + ":" + r.r_ags_quiet_time_begin_minute + ":00', ");
			va.append("'" + r.r_ags_quiet_time_end_hour + ":" + r.r_ags_quiet_time_end_minute + ":00', ");
			va.append(r.r_ags_exercise_days + ", ");
			
			va.append("'" + r.r_ags_exercise_start_hour + ":" + r.r_ags_exercise_start_minute + ":00', ");
			va.append("SEC_TO_TIME(" + r.r_ags_exercise_run_time_minutes*60 + "), ");
			va.append("SEC_TO_TIME(" + r.r_ags_top_off_minutes*60 + "), ");
		}
		
		/* remote AGS 0xA4 */
		if ( r.age_remote_0xA4 < RecordMagWeb.maxAgeCurrent ) {
			sb.append(	"r_ags_warm_up, r_ags_cool_down, ");
			
			va.append("SEC_TO_TIME(" + r.r_ags_warm_up_seconds + "), ");
			va.append("SEC_TO_TIME(" + r.r_ags_cool_down_seconds + "), ");
		}
		
		/* remote BMK 0x80 */
		if ( r.age_remote_0x80 < RecordMagWeb.maxAgeCurrent ) {
			sb.append(	"r_bmk_efficiency, r_bmk_battery_size, ");	
			
			va.append(r.r_bmk_efficiency + ", ");
			va.append(r.r_bmk_battery_size + ", ");
		}
		
		/* AGS 0xA1 */
		if ( r.age_ags_0xA1 < RecordMagWeb.maxAgeCurrent ) {
			sb.append(	"a_status, a_temperature, a_gen_run_time, a_voltage, a_revision, ");	

			va.append(r.a_status + ", ");
			va.append(r.a_temperature + ", ");
			va.append("SEC_TO_TIME(" + r.a_gen_runtime_minutes*60 + "), ");
			va.append(df2.format(r.a_voltage) + ", ");
			va.append(df1.format(r.a_revision) + ", ");
		}
		
		/* AGS 0xA2 */
		if ( r.age_ags_0xA2 < RecordMagWeb.maxAgeCurrent ) {
			sb.append(	"a_days_since_last, ");			
			va.append(r.a_days_since_last + ", ");
		}
		
		/* BMK */
		if ( r.age_bmk < RecordMagWeb.maxAgeCurrent ) {
			sb.append(	"b_state_of_charge, b_dc_volts, b_dc_amps, b_dc_min_volts, " + 
						"b_dc_max_volts, b_amph_in_out, b_amph_trip, b_amph_cumulative, b_fault, b_revision, ");
			
			va.append(r.b_state_of_charge + ", ");
			va.append(df2.format(r.b_dc_volts) + ", ");
			va.append(df2.format(r.b_dc_amps) + ", ");
			va.append(df2.format(r.b_dc_min_volts) + ", ");
			
			va.append(df2.format(r.b_dc_max_volts) + ", ");
			va.append(r.b_amph_in_out + ", ");
			va.append(df2.format(r.b_amph_trip) + ", ");
			va.append(r.b_amph_cumulative + ", ");
			va.append(r.b_fault + ", ");
			va.append(df1.format(r.b_revision) + ", ");
		}
		
		/* RTR */
		if ( r.age_rtr < RecordMagWeb.maxAgeCurrent ) {
			sb.append(	"rtr_revision, ");
			va.append(df1.format(r.rtr_revision) + ", ");
		}
		
		
		/* base SQL query: check for trailing ',' and remove it */
		if ( ' ' == sb.charAt(sb.length()-1) ) 
			sb.deleteCharAt(sb.length()-1);
		if ( ',' == sb.charAt(sb.length()-1) ) 
			sb.deleteCharAt(sb.length()-1);

		/* values: check for trailing ',' and remove it */
		if ( ' ' == va.charAt(va.length()-1) ) 
			va.deleteCharAt(va.length()-1);
		if ( ',' == va.charAt(va.length()-1) ) 
			va.deleteCharAt(va.length()-1);

		sb.append(") VALUES(");
		sb.append(va.toString());
		sb.append(")");
		
		System.out.println("SQL: " + sb.toString());

//		log.queryAutoCreate(sb.toString(),"worldDataProto.magWeb",table);

		return true;

	}
}
