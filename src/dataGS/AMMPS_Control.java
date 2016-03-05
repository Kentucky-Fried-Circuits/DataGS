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

	public void ingest(String ch, double d) {
		System.err.println("# AMMPS_Control ingest(ch=" + ch + " d=" + d);
	}

}
