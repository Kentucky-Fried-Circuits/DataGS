package dataGS;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Vector;

/**
 * TCP/IP socket thread for getting data into DataGS
 * accepts single character keys and string values. Terminated with \n
 * @author James Jarvis
 *
 */
public class DataGSServerThread extends Thread {
	protected Socket socket = null;
	protected InputStream inputStream;
	protected OutputStream outputStream;
	private int[] rawBuffer;
	private Log log, threadLog;

	private final static boolean debug=false;


	public DateFormat dateFormat;




	protected Vector<ChannelData> channelDataListeners;

	public void addChannelDataListener(ChannelData c) {
		channelDataListeners.add(c);

	}

	public DataGSServerThread(
			Socket socket, 
			Log l,

			DateFormat df,
			int socketTimeout) {

		/* set our thread name */
		super(socket.getInetAddress().getHostAddress() + ":" + socket.getLocalPort());
		this.socket = socket;

		/* throw SocketTimeoutException after blocking for 62 seconds on read */
		try {
			socket.setSoTimeout(socketTimeout*1000);
		} catch ( SocketException e ) {
			System.err.println("# Caught SocketException while setting read timeout.");
			System.err.println(e);
		}

		channelDataListeners=new Vector<ChannelData>();

		log=l;
		threadLog=l;
		rawBuffer = new int[1024];


		memcacheLog("Starting thread: " + socket.getInetAddress().getHostAddress() + ":" + socket.getLocalPort());

		/*
		if ( null == log ) {
			System.err.println("# Opening MySQL connection in thread");
			log = new LogMySQL(myHost,myUser,myPass,myDB,myPort);
			try {
				log.connect();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		 */

		//dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		dateFormat=df;


	}


	private void memcacheLog(String s) {
		System.err.println("# [LOG] " + s);
	}

	/** Jump here when our packet starts with an A to Z */
	private void channelData() {


		//	System.err.println("# Assuming text output data logger");


		try {
			/* read the rest of the packet, until '\n' or '\r' encountered */
			for ( int i=1 ; i<rawBuffer.length ; i++ ) {
				rawBuffer[i]=inputStream.read();

				if ( '\n'==rawBuffer[i] || '\r'==rawBuffer[i] )
					break;
			}
		} catch (SocketTimeoutException set) {
			System.err.println("# channelData() SocketTimeoutException from " + socket.getInetAddress().getHostAddress() + ":");
			System.err.println("# " + set);
			if ( debug ) 
				set.printStackTrace();
			shutdown(set,null);
		} catch (IOException e) {
			System.err.println("# channelData() IOException from " + socket.getInetAddress().getHostAddress() + ":");
			System.err.println("# " + e);
			if ( debug )
				e.printStackTrace();
			shutdown(e,null);
		}

		byte b[]=new byte[rawBuffer.length];

		for ( int i=0 ; i<rawBuffer.length ; i++ ) {
			if ( rawBuffer[i]=='\n' || rawBuffer[i]=='\r')
				break;

			b[i]=(byte) (rawBuffer[i] & 0xff);
		}
		String line = new String(b);
		//		System.err.println("# Line: " + line);


		try {
			String ch=b[0] + "";
//			double d=Double.parseDouble(line.substring(1));
			//	System.err.println("# ch=" + ch + " d=" + d + " line.substring(1)=" + line.substring(1));

			for ( int i=0 ; i<channelDataListeners.size() ; i++ ) {
				channelDataListeners.elementAt(i).ingest(ch, line.substring(1));
			}

		} catch ( Exception e ) {
			System.err.println("# Error parsing: " + line);
		}




		//int nFields = line.split(",").length;
		//System.err.println("# nFields: " + nFields);




	}

	protected void shutdown(Exception eGotUsHere, String errorString) {
		String s;

		s="# shutdown() on " + socket.getInetAddress().getHostAddress() + " ";
		memcacheLog(s);

		if ( null != eGotUsHere ) {
			/* print exception, stack trace, and log */
			String exceptionStrackTrace=""; 
			try {
				exceptionStrackTrace = org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(eGotUsHere);
			} catch ( Exception e ) {
				exceptionStrackTrace = "org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(eGotUsHere) actually threw an exception:\n" + e;
			}

			/* print to screen */
			memcacheLog("# shutdown() was triggered by exception: ");
			memcacheLog("# " + eGotUsHere.toString());
			memcacheLog("# with stack trace: ");
			memcacheLog("# " + exceptionStrackTrace);

			/* log to mysql */
		}
		if ( null != errorString ) {
			/* print error string and log */
			memcacheLog("# shutdown() was triggered with this message: ");
			memcacheLog("# " + errorString);
		}


		if ( null != socket && !socket.isClosed() ) {
			memcacheLog(" [socket closing] ");
			try {
				if ( false == socket.isInputShutdown() ) {
					socket.shutdownInput();
					memcacheLog("# input is not shutdown. Shutting down input.");
				}
				if ( false == socket.isOutputShutdown() ) {
					socket.shutdownOutput();
					memcacheLog("# output is not shutdown. Shutting down output.");
				}

				socket.close();
			} catch (IOException e) {
				memcacheLog("# error closing socket in shutdown()");
				e.printStackTrace();
			}
		}
		memcacheLog(" ... done");

		String sql = "INSERT INTO connection (logdate,localPort,remoteIP,remotePort,status) VALUES (" +
				"'" +  dateFormat.format(new Date()) + "', " +
				socket.getLocalPort() + ", " + 
				"'" + socket.getInetAddress().getHostAddress() + "', " +
				socket.getPort() + ", " + 
				"'closed'" +
				")";
		//		System.err.println("# SQL: " + sql);


		log.queryAutoCreate(sql, "worldDataProto.connection", "connection");

		if ( null==threadLog ) {
			/* MySQL connection was created in this thread ... we are responsible for closing it */
			log.close();
		}
	}

	public void run() {
		memcacheLog("# Connection @ " + dateFormat.format(new Date()) + " from " + socket.getInetAddress().getHostAddress());

		try {
			inputStream=socket.getInputStream();
			outputStream=socket.getOutputStream();

			while ( ! socket.isInputShutdown() ) {
				/* scan for start of packet marker, which is an upper case A to Z */
				int b;
				do {
					/* block waiting for a character */
					b=inputStream.read();

					if ( -1 == b ) {
						String error = String.format("# End of stream reached from " +  socket.getInetAddress().getHostAddress());
						System.err.println(error);
						shutdown(null,error);
						return;
					}


				} while ( b<'A' && b<'Z' ); 

				for ( int i=0 ; i<rawBuffer.length ; i++ )
					rawBuffer[i]=0xff;

				/* found start of packet */
				rawBuffer[0]=b;

				channelData();
			}

			shutdown(null,"# Normal shutdown");

			//		System.err.println("# Connection from " + socket.getInetAddress().getHostAddress() + " closed");

		} catch (SocketTimeoutException set) {
			memcacheLog("# run() SocketTimeoutException from " + socket.getInetAddress().getHostAddress() + ":");
			memcacheLog("# " + set);
			if ( debug )
				set.printStackTrace();
			shutdown(set,null);
		} catch (IOException e) {
			memcacheLog("# run() IOException from " + socket.getInetAddress().getHostAddress() + ":");
			memcacheLog("# " + e);
			if ( debug )
				e.printStackTrace();
			shutdown(e,null);
		} catch (Exception e) {
			memcacheLog("# run() caught Exception and is shutting down.");
			if ( debug )
				e.printStackTrace();
			shutdown(e,null);
		}
	}

	boolean alive() {
		return true;
	}

}
