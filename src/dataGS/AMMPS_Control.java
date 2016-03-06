package dataGS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

public class AMMPS_Control {
	protected Timer decisionTimer;

	/* AGS config data file */
	ConfigData config;
	/* DataGS where we send our AGS trigger(s) back to */
	ChannelData datags_out;

	/* Magnum data needed to make decisions from */
	private double magnum_b_dc_volts=-1.0;
	private double magnum_r_hours=-1.0;
	private double magnum_r_minutes=-1.0;
	private double magnum_b_state_of_charge=-1.0;
	private double magnum_b_dc_amps=-1.0;
	private double magnum_i_temp_transformer=-1.0;
	private double magnum_i_temp_battery=-1.0;

	/* constructor */
	public AMMPS_Control(ConfigData cd, ChannelData datags) {
		config=cd;
		datags_out=datags;
		
		System.err.println("# AMMPS_Control constructed.");

		/* timer to periodically handle the data */
		decisionTimer = new javax.swing.Timer(1000, new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				try{
					ags();	
				} catch ( Exception ee ) {
					ee.printStackTrace();
				}

			}
		});
		decisionTimer.start();
	}

	public String toString() {
		String s="";

		s += "AMMPS Control toString():";
		s += "\nAGS Data:";
		s += "\n        magnum_b_dc_volts=" + magnum_b_dc_volts;
		s += "\n magnum_b_state_of_charge=" + magnum_b_state_of_charge;
		s += "\n         magnum_b_dc_amps=" + magnum_b_dc_amps;
		s += "\nmagnum_i_temp_transformer=" + magnum_i_temp_transformer;
		s += "\n    magnum_i_temp_battery=" + magnum_i_temp_battery;
		s += "\n           magnum_r_hours=" + magnum_r_hours;
		s += "\n         magnum_r_minutes=" + magnum_r_minutes;


		return s;
	}


	private static void send_killlall_to_ammps_control(String signal) {
		try {
			Runtime.getRuntime().exec("killall -" + signal + " ammps_control");
		} catch ( Exception e ) {
			System.err.println("# Caught exception while sending signal to ammps_control");
			System.err.println(e);
		}
	}

	public static void generator_run_open_contactor() {
		send_killlall_to_ammps_control("SIGUSR1");
	}
	public static void generator_run_closed_contactor() {
		send_killlall_to_ammps_control("SIGUSR2");
	}
	public static void generator_stop() {
		send_killlall_to_ammps_control("SIGURG");
	}

	/* we get all data here. Save what we want */
	public void ingest(String ch, double d) {
		//		System.err.println("# AMMPS_Control ingest(ch=" + ch + " d=" + d);

		if ( 0==ch.compareTo("b_dc_volts") )
			magnum_b_dc_volts=d;
		else if ( 0==ch.compareTo("r_hours") )
			magnum_r_hours=d;
		else if ( 0==ch.compareTo("r_minutes") )
			magnum_r_minutes=d;
		else if ( 0==ch.compareTo("b_state_of_charge") )
			magnum_b_state_of_charge=d;
		else if ( 0==ch.compareTo("b_dc_amps") )
			magnum_b_dc_amps=d;
		else if ( 0==ch.compareTo("i_temp_transformer") )
			magnum_i_temp_transformer=d;
		else if ( 0==ch.compareTo("i_temp_battery") )
			magnum_i_temp_battery=d;


	}
	
	private final static int TEMPERATURE_BATT_STATE_IDLE=0;
	private final static int TEMPERATURE_BATT_STATE_RUNNING=1;
	private int temperature_batt_state=TEMPERATURE_BATT_STATE_IDLE;
	
	private double temperature_batt_run_counter_seconds;
	
	private boolean ags_temperature_battery(){
		/* check if enabled */
		if ( 0 != config.getValue("ags_system_battery_temperature_enable").compareTo("1")  )
			return false;
		
		double config_start_temperature = (Double.parseDouble(config.getValue("ags_system_battery_temperature_exceeds_degrees_f"))-32.0)/1.8;
		double config_duration_seconds = Double.parseDouble(config.getValue("ags_system_battery_duration_hours"))*3600.0;

		System.err.println("");
		System.err.println("@@@@ ags_temperature_battery()");
		/* state */
		System.err.print  ("@@@@               temperature_batt_state=");
		if ( TEMPERATURE_BATT_STATE_IDLE == temperature_batt_state )           
			System.err.println("TEMPERATURE_BATT_STATE_IDLE");
		if ( TEMPERATURE_BATT_STATE_RUNNING == temperature_batt_state )        
				System.err.println("TEMPERATURE_BATT_STATE_RUNNING");
		System.err.println("@@@@ temperature_batt_run_counter_seconds=" + temperature_batt_run_counter_seconds);
		/* config */
		System.err.println("@@@@             config_start_temperature=" + config_start_temperature);
		System.err.println("@@@@              config_duration_seconds=" + config_duration_seconds);
		System.err.println("");

		if ( TEMPERATURE_BATT_STATE_IDLE == temperature_batt_state ) {
			if ( magnum_i_temp_battery >= config_start_temperature ) {
				temperature_batt_run_counter_seconds=0.0;
				temperature_batt_state = TEMPERATURE_BATT_STATE_RUNNING;
				return true;
			}
			return false;
		}

		
		if ( TEMPERATURE_BATT_STATE_RUNNING == temperature_batt_state ) {
			/* restart duration counter if we are above threshold */ 
			if ( magnum_i_temp_battery >= config_start_temperature ) {
				temperature_batt_run_counter_seconds=0.0;
				System.err.println("temperature_batt_run_counter_seconds reset, now " + temperature_batt_run_counter_seconds);
			} else {
				temperature_batt_run_counter_seconds += 1.0;
				System.err.println("temperature_batt_run_counter_seconds incremented, now " + temperature_batt_run_counter_seconds);
			}
			
			/* shut down if we achieved our run duration */
			if ( temperature_batt_run_counter_seconds >= config_duration_seconds ) {
				temperature_batt_state = TEMPERATURE_BATT_STATE_IDLE;
				return false;
			}
			return true;
		}
		
		/* shouldn't get here, but java requires the return anyhow */
		return false;
	}

	private final static int TEMPERATURE_XFMR_STATE_IDLE=0;
	private final static int TEMPERATURE_XFMR_STATE_RUNNING=1;
	private int temperature_xfmr_state=TEMPERATURE_XFMR_STATE_IDLE;
	
	private double temperature_xfmr_run_counter_seconds;
	
	private boolean ags_temperature_transformer(){
		/* check if enabled */
		if ( 0 != config.getValue("ags_system_transformer_temperature_enable").compareTo("1")  )
			return false;
		
		double config_start_temperature = (Double.parseDouble(config.getValue("ags_system_transformer_temperature_exceeds_degrees_f"))-32.0)/1.8;
		double config_duration_seconds = Double.parseDouble(config.getValue("ags_system_transformer_duration_hours"))*3600.0;

		System.err.println("");
		System.err.println("@@@@ ags_temperature_transformer()");
		/* state */
		System.err.print  ("@@@@               temperature_xfmr_state=");
		if ( TEMPERATURE_XFMR_STATE_IDLE == temperature_xfmr_state )           
			System.err.println("TEMPERATURE_XFMR_STATE_IDLE");
		if ( TEMPERATURE_XFMR_STATE_RUNNING == temperature_xfmr_state )        
				System.err.println("TEMPERATURE_XFMR_STATE_RUNNING");
		System.err.println("@@@@ temperature_xfmr_run_counter_seconds=" + temperature_xfmr_run_counter_seconds);
		/* config */
		System.err.println("@@@@             config_start_temperature=" + config_start_temperature);
		System.err.println("@@@@              config_duration_seconds=" + config_duration_seconds);
		System.err.println("");

		if ( TEMPERATURE_XFMR_STATE_IDLE == temperature_xfmr_state ) {
			if ( magnum_i_temp_transformer >= config_start_temperature ) {
				temperature_xfmr_run_counter_seconds=0.0;
				temperature_xfmr_state = TEMPERATURE_XFMR_STATE_RUNNING;
				return true;
			}
			return false;
		}

		
		if ( TEMPERATURE_XFMR_STATE_RUNNING == temperature_xfmr_state ) {
			/* restart duration counter if we are above threshold */ 
			if ( magnum_i_temp_transformer >= config_start_temperature ) {
				temperature_xfmr_run_counter_seconds=0.0;
				System.err.println("temperature_xfmr_run_counter_seconds reset, now " + temperature_xfmr_run_counter_seconds);
			} else {
				temperature_xfmr_run_counter_seconds += 1.0;
				System.err.println("temperature_xfmr_run_counter_seconds incremented, now " + temperature_xfmr_run_counter_seconds);
			}
			
			/* shut down if we achieved our run duration */
			if ( temperature_xfmr_run_counter_seconds >= config_duration_seconds ) {
				temperature_xfmr_state = TEMPERATURE_XFMR_STATE_IDLE;
				return false;
			}
			return true;
		}
		
		/* shouldn't get here, but java requires the return anyhow */
		return false;
	}

	
	private final static int DC_AMPS_STATE_IDLE=0;
	private final static int DC_AMPS_STATE_RUNNING=1;
	private int dc_amps_state=DC_AMPS_STATE_IDLE;
	
	private double dc_amps_run_counter_seconds;
	
	private boolean ags_dc_amps(){
		/* check if enabled */
		if ( 0 != config.getValue("ags_system_amps_enable").compareTo("1")  )
			return false;
		
		double config_start_amps = Double.parseDouble(config.getValue("ags_system_amps_start_amps"));
		double config_duration_seconds = Double.parseDouble(config.getValue("ags_system_amps_duration_hours"))*3600.0;

		System.err.println("");
		System.err.println("@@@@ ags_dc_amps()");
		/* state */
		System.err.print  ("@@@@              dc_amps_state=");
		if ( DC_AMPS_STATE_IDLE == dc_amps_state )           System.err.println("DC_AMPS_STATE_IDLE");
		if ( DC_AMPS_STATE_RUNNING == dc_amps_state )        System.err.println("DC_AMPS_STATE_RUNNING");
		System.err.println("@@@@ dc_run_run_counter_seconds=" + dc_amps_run_counter_seconds);
		/* config */
		System.err.println("@@@@          config_start_amps=" + config_start_amps);
		System.err.println("@@@@    config_duration_seconds=" + config_duration_seconds);
		System.err.println("");

		if ( DC_AMPS_STATE_IDLE == dc_amps_state ) {
			if ( magnum_b_dc_amps <= config_start_amps ) {
				dc_amps_run_counter_seconds=0.0;
				dc_amps_state = DC_AMPS_STATE_RUNNING;
				return true;
			}
			return false;
		}


		
		if ( DC_AMPS_STATE_RUNNING == dc_amps_state ) {
			/* restart duration counter if we are below threshold */ 
			if ( magnum_b_dc_amps <= config_start_amps ) {
				dc_amps_run_counter_seconds=0.0;
			} else {
				dc_amps_run_counter_seconds += 1.0;
			}
			
			/* shut down if we achieved our run duration */
			if ( dc_amps_run_counter_seconds >= config_duration_seconds ) {
				dc_amps_state = DC_AMPS_STATE_IDLE;
				return false;
			}
			return true;
		}
		
		/* shouldn't get here, but java requires the return anyhow */
		return false;
	}

	private final static int HOUR_STATE_IDLE=0;
	private final static int HOUR_STATE_RUNNING=1;
	private int hour_state=HOUR_STATE_IDLE;
	
	private double hour_run_counter_seconds;
	
	private boolean ags_hour(){
		/* check if enabled */
		if ( 0 != config.getValue("ags_system_time_enable").compareTo("1")  )
			return false;
		
		double config_start_hour = Double.parseDouble(config.getValue("ags_system_time_start_hour"));
		double config_duration_seconds = Double.parseDouble(config.getValue("ags_system_time_duration_hours"))*3600.0;
		System.err.println("");
		System.err.println("@@@@ ags_hour()");
		/* state */
		System.err.print  ("@@@@               hour_state=");
		if ( HOUR_STATE_IDLE == hour_state )           System.err.println("HOUR_STATE_IDLE");
		if ( HOUR_STATE_RUNNING == hour_state )        System.err.println("HOUR_STATE_RUNNING");
		System.err.println("@@@@ hour_run_counter_seconds=" + hour_run_counter_seconds);
		/* config */
		System.err.println("@@@@        config_start_hour=" + config_start_hour);
		System.err.println("@@@@  config_duration_seconds=" + config_duration_seconds);
		System.err.println("");

		if ( HOUR_STATE_IDLE == hour_state ) {
			if ( magnum_r_hours == config_start_hour && magnum_r_minutes == 0.0 ) {
				/* first minute in start_hour */
				hour_run_counter_seconds=0.0;
				hour_state = HOUR_STATE_RUNNING;
				return true;
			}
			return false;
		}
		
		if ( HOUR_STATE_RUNNING == hour_state ) {
			hour_run_counter_seconds += 1.0;
			
			if ( hour_run_counter_seconds >= config_duration_seconds ) {
				hour_state = HOUR_STATE_IDLE;
				return false;
			}
			return true;
		}
		
		/* shouldn't get here, but java requires the return anyhow */
		return false;
	}

	
	private final static int SOC_STATE_IDLE=0;
	private final static int SOC_STATE_RUNNING=1;
	private int soc_state=SOC_STATE_IDLE;

	private boolean ags_soc() {

		/* check if enabled */
		if ( 0 != config.getValue("ags_system_soc_enable").compareTo("1")  )
			return false;
		
		double config_startSOC = Double.parseDouble(config.getValue("ags_system_soc_start_soc"));
		double config_stopSOC = Double.parseDouble(config.getValue("ags_system_soc_stop_soc"));
		
		System.err.println("");
		System.err.println("@@@@ ags_soc()");
		/* state */
		System.err.print  ("@@@@        soc_state=");
		if ( SOC_STATE_IDLE == vdc_state )           System.err.println("SOC_STATE_IDLE");
		if ( SOC_STATE_RUNNING == vdc_state )        System.err.println("SOC_STATE_RUNNING");
		/* config */
		System.err.println("@@@@ config_start_soc=" + config_startSOC);
		System.err.println("@@@@  config_stop_soc=" + config_stopSOC);
		System.err.println("");

		if ( soc_state == SOC_STATE_IDLE ) {
			/* check if we need to charge */
			if ( magnum_b_state_of_charge <= config_startSOC ) {
				soc_state = SOC_STATE_RUNNING;
				return true;
			}

			return false;
		} 
		
		/* SOC_STATE_RUNNING */

		/* check if we are done charging */
		if ( magnum_b_state_of_charge >= config_stopSOC ) {
			soc_state = SOC_STATE_IDLE;
			return false;
		}

		return true;
	}
	
	private final static int VDC_STATE_IDLE=0;
	private final static int VDC_STATE_IN_START_DELAY=1;
	private final static int VDC_STATE_RUNNING=2;
	private final static int VDC_STATE_IN_STOP_DELAY=3;
	private int vdc_state=VDC_STATE_IDLE;
	

	private double vdc_counter_start_delay_seconds;
	private double vdc_counter_stop_delay_seconds;

	
	private boolean ags_vdc() {

		/* check if enabled */
		if ( 0 != config.getValue("ags_system_voltage_enable").compareTo("1")  )
			return false;

		
		double config_start_vdc = Double.parseDouble(config.getValue("ags_system_voltage_start_voltage_vdc"));
		double config_start_delay_seconds = Double.parseDouble(config.getValue("ags_system_voltage_start_delay_seconds"));
		double config_stop_vdc = Double.parseDouble(config.getValue("ags_system_voltage_stop_vdc"));
		double config_stop_delay_seconds = Double.parseDouble(config.getValue("ags_system_voltage_stop_delay_minutes"))*60.0;

		
		System.err.println("");
		System.err.println("@@@@ ags_vdc()");
		/* state */
		System.err.print  ("@@@@                       vdc_state=");
		if ( VDC_STATE_IDLE == vdc_state )           System.err.println("VDC_STATE_IDLE");
		if ( VDC_STATE_IN_START_DELAY == vdc_state ) System.err.println("VDC_IN_START_DELAY");
		if ( VDC_STATE_RUNNING == vdc_state )        System.err.println("VDC_STATE_RUNNING");
		if ( VDC_STATE_IN_STOP_DELAY == vdc_state )  System.err.println("VDC_STATE_IN_STOP_DELAY");
		System.err.println("@@@@ vdc_counter_start_delay_seconds=" + vdc_counter_start_delay_seconds);
		System.err.println("@@@@  vdc_counter_stop_delay_seconds=" + vdc_counter_stop_delay_seconds);
		/* config */
		System.err.println("@@@@                config_start_vdc=" + config_start_vdc);
		System.err.println("@@@@      config_start_delay_seconds=" + config_start_delay_seconds);
		System.err.println("@@@@                 config_stop_vdc=" + config_stop_vdc);
		System.err.println("@@@@       config_stop_delay_seconds=" + config_stop_delay_seconds);
		System.err.println("");
		
		if ( VDC_STATE_IDLE == vdc_state ) {
			/* transition to VDC_STATE_IN_START_DELAY if voltage < start vdc */
			if ( magnum_b_dc_volts <= config_start_vdc ) {
				vdc_state=VDC_STATE_IN_START_DELAY;
				vdc_counter_start_delay_seconds=0.0;
			}
			
			return false;
		}
		
		if ( VDC_STATE_IN_START_DELAY == vdc_state ) {
			/* above start threshold, go back to idle */
			if ( magnum_b_dc_volts > config_start_vdc ) {
				vdc_state=VDC_STATE_IDLE;
			} else {
				vdc_counter_start_delay_seconds += 1.0;
				
				if ( vdc_counter_start_delay_seconds >= config_start_delay_seconds )  {
					vdc_state=VDC_STATE_RUNNING;
					
					return true;
				}
			}		
			
			return false;
		}
		
		if ( VDC_STATE_RUNNING == vdc_state ) {
			if ( magnum_b_dc_volts > config_stop_vdc ) {
				vdc_state=VDC_STATE_IN_STOP_DELAY;
				vdc_counter_stop_delay_seconds=0.0;
			}
			
			return true;
		}
		
		if ( VDC_STATE_IN_STOP_DELAY == vdc_state ) {
			/* below stop voltage, go back to running */
			if ( magnum_b_dc_volts < config_stop_vdc ) {
				vdc_state=VDC_STATE_RUNNING;
			} else {
				vdc_counter_stop_delay_seconds += 1.0;
				
				if ( vdc_counter_stop_delay_seconds >= config_stop_delay_seconds )  {
					/* done with cycle */
					vdc_state=VDC_STATE_IDLE;
					
					return false;
				}
			}	
			return true;
		}
		
		
		return false;
	}
	
	private int bit_modify(int intValue, int bitPos, boolean bitValue ) {
		if ( true == bitValue ) {
			intValue |= 1<<bitPos;
		} else {
			intValue &= ~(1<<bitPos);
		}
		return intValue;
	}

	private final static int AGS_STATE_IDLE=0;
	private final static int AGS_STATE_WARMUP=1;
	private final static int AGS_STATE_RUNNING=2;
	private final static int AGS_STATE_COOLDOWN=3;
	private int ags_state=AGS_STATE_IDLE;
	

	private double ags_counter_warmup_seconds;
	private double ags_counter_cooldown_seconds;

	
	private static int AGS_RUN_BIT_DC_AMPS=0;
	private static int AGS_RUN_BIT_HOUR=1;
	private static int AGS_RUN_BIT_SOC=2;
	private static int AGS_RUN_BIT_TEMPERATURE_BATTERY=3;
	private static int AGS_RUN_BIT_TEMPERATURE_TRANSFORMER=4;
	private static int AGS_RUN_BIT_VDC=5;
	private static int AGS_RUN_BIT_EXERCISE=6;
	private static int AGS_RUN_BIT_WARMUP=7;
	private static int AGS_RUN_BIT_COOLDOWN=8;
	
	private void ags() {
		int ags_run=0;

		System.err.println("### ags() debug AMMPS_Control.toString())\n" + toString());

		
		/* evaluate AGS conditions and set appropriate bits in ags_run to show was is requesting generator run */
		ags_run = bit_modify( ags_run, AGS_RUN_BIT_DC_AMPS, ags_dc_amps() );
		ags_run = bit_modify( ags_run, AGS_RUN_BIT_HOUR, ags_hour() );
		ags_run = bit_modify( ags_run, AGS_RUN_BIT_SOC, ags_soc() );
		ags_run = bit_modify( ags_run, AGS_RUN_BIT_TEMPERATURE_BATTERY, ags_temperature_battery() );
		ags_run = bit_modify( ags_run, AGS_RUN_BIT_TEMPERATURE_TRANSFORMER, ags_temperature_transformer() );
		ags_run = bit_modify( ags_run, AGS_RUN_BIT_VDC, ags_vdc() );
// FIXME		ags_run = bit_modify( ags_run, AGS_RUN_BIT_EXERCISE, ags_exercise() );

		
		double config_warmup_seconds = Double.parseDouble(config.getValue("ags_system_warmup_seconds"));
		double config_cooldown_seconds = Double.parseDouble(config.getValue("ags_system_cooldown_seconds"));
		
		/* idle, warmup, running, cooldown state machine */
		if ( ags_state == AGS_STATE_IDLE ) {
			/* warmup state bit */
			ags_run = bit_modify( ags_run, AGS_RUN_BIT_WARMUP, false );
			/* cooldown state bit */
			ags_run = bit_modify( ags_run, AGS_RUN_BIT_COOLDOWN, false );

			
			if ( ags_run != 0 ) {
				/* somebody has requested we run */
				ags_counter_warmup_seconds=0.0;
				ags_state=AGS_STATE_WARMUP;
			}
			
			/* nothing calling for run, generator stopped */
			generator_stop();
		} else if ( ags_state == AGS_STATE_WARMUP ) {
			/* warmup state bit */
			ags_run = bit_modify( ags_run, AGS_RUN_BIT_WARMUP, true );
			/* cooldown state bit */
			ags_run = bit_modify( ags_run, AGS_RUN_BIT_COOLDOWN, false );

			ags_counter_warmup_seconds += 1.0;
			
			if ( ags_counter_warmup_seconds >= config_warmup_seconds ) {
				ags_state=AGS_STATE_RUNNING;
			}
			
			/* start generator with load contactor open so generator can warm up */
			generator_run_open_contactor();
		} else if ( ags_state == AGS_STATE_RUNNING ) {
			/* warmup state bit */
			ags_run = bit_modify( ags_run, AGS_RUN_BIT_WARMUP, false );
			/* cooldown state bit */
			ags_run = bit_modify( ags_run, AGS_RUN_BIT_COOLDOWN, false );
			
			if ( ags_run == 0 ) {
				/* don't need to run anymore */
				ags_counter_cooldown_seconds=0.0;
				ags_state=AGS_STATE_COOLDOWN;
			}
			
			/* deliver power to load */
			generator_run_closed_contactor();
		} else if ( ags_state == AGS_STATE_COOLDOWN ) {
			/* warmup state bit */
			ags_run = bit_modify( ags_run, AGS_RUN_BIT_WARMUP, false );
			/* cooldown state bit */
			ags_run = bit_modify( ags_run, AGS_RUN_BIT_COOLDOWN, true );
			
			ags_counter_cooldown_seconds += 1.0;
			
			if ( ags_counter_cooldown_seconds >= config_cooldown_seconds ) {
				ags_state=AGS_STATE_IDLE;
			}
			
			/* turn off load contactor so generator can cool down */
			generator_run_open_contactor();			
		}
		
		
		
		
		System.err.println("### ags()                ags_run=" + ags_run);
		System.err.print  ("###                    ags_state=");
		if ( AGS_STATE_IDLE == ags_state ) System.err.println("AGS_STATE_IDLE");
		if ( AGS_STATE_WARMUP == ags_state ) System.err.println("AGS_STATE_WARMUP");
		if ( AGS_STATE_RUNNING == ags_state ) System.err.println("AGS_STATE_RUNNING");
		if ( AGS_STATE_COOLDOWN == ags_state ) System.err.println("AGS_STATE_COOLDOWN");
		
		System.err.println("###   ags_counter_warmup_seconds=" + ags_counter_warmup_seconds);
		System.err.println("###        config_warmup_seconds=" + config_warmup_seconds);
		System.err.println("### ags_counter_cooldown_seconds=" + ags_counter_cooldown_seconds);
		System.err.println("###      config_cooldown_seconds=" + config_cooldown_seconds);
		
		/* send AGS GEN RUN CAUSE back into DataGS data stream */
		datags_out.ingest("67", Integer.toString(ags_run));
	}

}
