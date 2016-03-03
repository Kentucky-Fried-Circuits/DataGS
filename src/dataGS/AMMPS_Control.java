package dataGS;

public class AMMPS_Control {
	
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

	public void process_RecordMagWeb(RecordMagWeb r) {
		System.err.println("# AMPS_Control got RecordMagWeb!");
		System.err.println("#            r_hours=" + r.r_hours);
		System.err.println("#          r_minutes=" + r.r_minutes);
		System.err.println("#          b_dc_amps=" + r.b_dc_amps);
		System.err.println("#         b_dc_volts=" + r.b_dc_volts);
		System.err.println("#  b_state_of_charge=" + r.b_state_of_charge);
		System.err.println("#     i_temp_battery=" + r.i_temp_battery);
		System.err.println("# i_temp_transformer=" + r.i_temp_transformer);
	}

}
