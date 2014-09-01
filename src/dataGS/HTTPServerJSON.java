package dataGS;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class HTTPServerJSON extends Thread{
	ServerSocket serverSocket;

	public void run() {
		boolean listening=true;
		int portNumber=9000;


		System.err.println("# Listening on port " + portNumber);
		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.err.println("# Could not listen on port: " + portNumber);
			System.exit(-1);
		}


		/* spin through and accept new connections as quickly as we can */
		while ( listening ) {
			try {
				Socket socket=serverSocket.accept();
				/* setup our sockets to send RST as soon as close() is called ... this is the default action */
				socket.setSoLinger (false, 0);


				HTTPServerThreadJSON conn = new HTTPServerThreadJSON(socket);
				conn.start();
			} catch ( Exception e ) {
				System.err.println("# Caught exception while accepingt socket connection." + e);
			}
		}


		try {
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
