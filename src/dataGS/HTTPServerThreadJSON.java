package dataGS;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

public class HTTPServerThreadJSON extends Thread {
	Socket socket;


	public HTTPServerThreadJSON (Socket s) {
		/* set our thread name */
		super(s.getInetAddress().getHostAddress() + ":" + s.getLocalPort());
		socket=s;

		/* throw SocketTimeoutException after blocking for 62 seconds on read */
		try {
			socket.setSoTimeout(1000);
		} catch ( SocketException e ) {
			System.err.println("# Caught SocketException while setting read timeout.");
			System.err.println(e);
		}
	}

	public void run() {
		try {
			InputStream  input  = socket.getInputStream();
			OutputStream output = socket.getOutputStream();
			long time = System.currentTimeMillis();

			while ( -1 != input.read() );
			
			
			output.write(("HTTP/1.1 200 OK\n\n<html><body>" +
					"Singlethreaded Server: " +
					time +
					"</body></html>").getBytes());
			output.close();
			input.close();
			System.out.println("Request processed: " + time);
		} catch ( Exception e ) {
			System.err.println("# httpServerThreadJSON caught exception " + e);
		}
	}

}
