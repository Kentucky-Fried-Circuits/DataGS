package dataGS;
import java.net.*;
import javax.swing.Timer;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
/* Command line parsing from Apache */
import org.apache.commons.cli.*;
/* Memcache client for logging */
import net.spy.memcached.MemcachedClient;

/* statistics */
import org.apache.commons.math3.stat.descriptive.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/* to upload this to data.aprsworld.com use:
 * cd /home/world/planet
 * rsync -ave ssh DataGS/ aprsworld.com:DataGS/
 */
public class DataGS implements ChannelData {
	private Log log, threadLog;
	private Timer threadMaintenanceTimer;

	private Vector<DataGSServerThread> connectionThreads;
	private MemcachedClient memcache;
	private int portNumber;


	/* data to summarize and send */
	protected SynchronizedSummaryStatistics[] dataCh;
	protected int intervalSummary;
	protected Timer dataTimer;

	/* supported databases */
	public static final int DATABASE_TYPE_MYSQL = 0;
	public static final int DATABASE_TYPE_SQLITE = 1;


	@SuppressWarnings("unused")
	private void memcacheLog(String s) {

		if ( null == memcache ) {
			//			System.err.println("# [LOG] " + s);
			return;
		}

		String key="DATAGS_" + portNumber;
		long index = memcache.incr(key + "_INDEX",1,0,3600*24*2);

		memcache.set(key + "_" + index,3600*24*2, s);
		System.err.println("# [" + key + "_" + index + "] " + s);

	}

	@SuppressWarnings("unused")
	private String connectionThreadInfo(DataGSServerThread conn) {
		String s="# connectionThreadInfo() debug debug of " + conn.getName() + "\n";

		try {
			s = s + "# isAlive(): " + conn.isAlive() + "\n";
			s = s + "# getState(): " + conn.getState() + "\n";
			s = s + "# socket.isBound(): " + conn.socket.isBound() + "\n";
			s = s + "# socket.isClosed(): " + conn.socket.isClosed() + "\n";
			s = s + "# socket.isConnected(): " + conn.socket.isConnected() + "\n";
			s = s + "# socket.isInputShutdown(): " + conn.socket.isInputShutdown() + "\n";
			s = s + "# socket.isOutputShutdown(): " + conn.socket.isOutputShutdown() + "\n";
			if ( ! conn.socket.isClosed() ) {
				s = s + "# socket.getReceiveBufferSize():" + conn.socket.getReceiveBufferSize() + "\n";
				s = s + "# socket.getSendBufferSize():" + conn.socket.getSendBufferSize() + "\n";
				s = s + "# socket.getSoLinger():" + conn.socket.getSoLinger() + "\n";
				s = s + "# socket.getSoTimeout():" + conn.socket.getSoTimeout() + "\n";
				s = s + "# socket.getInetAddress():" + conn.socket.getInetAddress() + "\n";
				s = s + "# socket.getReuseAddress():" + conn.socket.getReuseAddress() + "\n";
				s = s + "# socket.getTcpNoDelay():" + conn.socket.getTcpNoDelay() + "\n";
			}
		} catch ( Exception e ) {
			s = s + "# connectionThreadInfo() caught an exception - can you believe that!\n";
			s = s + "# " + e + "\n";
		}

		return s;
	}

	private void threadMaintenanceTimer() {
		for ( int i=0 ; i<connectionThreads.size(); i++ ) {
			DataGSServerThread conn=connectionThreads.elementAt(i);

			if ( ! conn.isAlive() ) {
				connectionThreads.remove(conn);
			}
		}

	}

	private void dataMaintenanceTimer() {
		System.err.println("######### dataMaintenanceTimer() #########");

		synchronized (dataCh) {
			for ( int i=0 ; i<dataCh.length ; i++ ) {
				if ( null == dataCh[i] )
					continue;

				
				/* put into MySQL table */
				String table = "adc_" + i;
				String sql = String.format("INSERT INTO %s VALUES(now(), %d, %f, %f, %f, %f)",
						table,
						dataCh[i].getN(),
						dataCh[i].getMean(),
						dataCh[i].getMin(),
						dataCh[i].getMax(),
						dataCh[i].getStandardDeviation()
						);
				log.queryAutoCreate(sql, "dataGSProto.analogDoubleSummarized", table);


				//System.err.println("# Channel " + i);

				
				Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
				System.err.println(gson.toJson(new adcDouble(i,dataCh[i])));
				
				//System.err.println(dataCh[i].toString());
				//System.err.println("# SQL: " + sql);

				/* clear statistics now for next pass */
				dataCh[i].clear();								
			}
		}
	}

	public void ingest(int channel, double value) {
		if ( channel > dataCh.length || channel < 0 ) {
			System.err.println("# Channel " + channel + " is out of range. dataCh.length is " + dataCh.length);
			return;
		}

		/* initialize the channel if it hasn't be already */
		if ( null == dataCh[channel] ) {
			System.err.println("# Channel " + channel + " not initialized");
			dataCh[channel] = new SynchronizedSummaryStatistics();
		}



		dataCh[channel].addValue(value);
	}


	public void run(String[] args) throws IOException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		/* parse options or use defaults */
		Options options = new Options();
		/* MySQL database connection parameters */
		String myHost, myUser, myPass, myDB;
		int myPort;
		myHost="localhost";
		myUser="root";
		myPass="roadtoad";
		myDB="dataGS";
		myPort=3306;

		/* SQLite connection parameters */
		String sqliteURL="";
		String sqliteProtoURL="";

		/* Data GS parameters */
		boolean oneMySQLPerThread=false;
		portNumber=4010;
		int socketTimeout=62;
		int stationTimeout=121;
		boolean logConnection=false;
		boolean memcachedDebug=false;
		int databaseType=DATABASE_TYPE_MYSQL;


		dataCh = new SynchronizedSummaryStatistics[256];
		intervalSummary = 1000;

		/* MySQL options */
		options.addOption("d", "database", true, "MySQL database");
		options.addOption("h", "host", true, "MySQL host");
		options.addOption("p", "password", true, "MySQL password");
		options.addOption("u", "user", true, "MySQL username");
		options.addOption("o", "one-mysql-per-thread", false, "Open a MySQL connection for each thread");

		/* sqlite options */
		options.addOption("s", "SQLite-URL",true,"SQLite URL (e.g. DataGS.db");
		options.addOption("S", "SQLite-proto-URL",true,"SQLite prototype URL (e.g. DataGSProto.db");

		/* DataGSCollector options */
		options.addOption("i", "interval", true, "Interval to summarize over (milliseconds)");
		options.addOption("l", "listen-port", true, "DataGSCollector Listening Port");
		options.addOption("t", "socket-timeout",true, "DataGSCollector connection socket timeout");
		options.addOption("T", "station-timeout",true, "DataGSCollector station history timeout");
		options.addOption("m", "memcacheDebug", false, "Debug messages written to memcached per station");

		/* parse command line */
		CommandLineParser parser = new PosixParser();
		try {
			CommandLine line = parser.parse( options, args );

			/* MySQL */
			if ( line.hasOption("host") ) myHost=line.getOptionValue("host");
			if ( line.hasOption("user") ) myUser=line.getOptionValue("user");
			if ( line.hasOption("password") ) myPass=line.getOptionValue("password");
			if ( line.hasOption("database") ) myDB=line.getOptionValue("database");
			if ( line.hasOption("one-mysql-per-thread") ) oneMySQLPerThread=true;

			/* SQLite */
			if ( line.hasOption("SQLite-URL") ) sqliteURL= line.getOptionValue("SQLite-URL");
			if ( line.hasOption("SQLite-proto-URL") ) sqliteProtoURL= line.getOptionValue("SQLite-proto-URL");

			/* DataGSCollector */
			if ( line.hasOption("interval") ) {
				intervalSummary = Integer.parseInt(line.getOptionValue("interval"));
			}
			if ( line.hasOption("listen-port") ) {
				portNumber = Integer.parseInt(line.getOptionValue("listen-port"));
			}
			if ( line.hasOption("socket-timeout") ) {
				socketTimeout = Integer.parseInt(line.getOptionValue("socket-timeout"));
			}
			if ( line.hasOption("station-timeout") ) {
				stationTimeout = Integer.parseInt(line.getOptionValue("station-timeout"));
			}

			if ( line.hasOption("memcacheDebug") ) memcachedDebug=true;


		} catch (ParseException e) {
			System.err.println("# Error parsing command line: " + e);
		}


		connectionThreads=new Vector<DataGSServerThread>();
		ServerSocket serverSocket = null;
		boolean listening = true;


		/* if SQLite database URL is specified, then we use SQLite database ... otherwise MySQL */
		if ( null != sqliteURL && "" != sqliteURL ) {
			databaseType=DATABASE_TYPE_SQLITE;
		}

		if ( DATABASE_TYPE_MYSQL == databaseType ) {
			System.err.println("# Opening MySQL connection");
			log = new LogMySQL(myHost,myUser,myPass,myDB,myPort);
			try {
				log.connect();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if ( DATABASE_TYPE_SQLITE == databaseType ) {
			System.err.println("# Opening SQLite database");
			log = new LogSQLite(sqliteProtoURL,sqliteURL);
			try {
				log.connect();
			} catch (Exception e) {
				e.printStackTrace();
			}			
		}




		System.err.println("# Listening on port " + portNumber + " with " + socketTimeout + " second socket timeout and " + stationTimeout + " second station timeout");
		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.err.println("# Could not listen on port: " + portNumber);
			System.exit(-1);
		}


		
		
		
		/* timer to periodically clear thread listing */
		threadMaintenanceTimer = new javax.swing.Timer(5000, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				threadMaintenanceTimer();
			}
		});
		threadMaintenanceTimer.start();


		/* timer to periodically handle the data */
		dataTimer = new javax.swing.Timer(intervalSummary, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dataMaintenanceTimer();
			}
		});
		dataTimer.start();



		/* start status update thread */
		DataGSStatus status = new DataGSStatus(log,portNumber);
		status.start();
		status.updateStatus();

		/* if oneMySQLPerThread then we send a null threadLog and the thread is responsible for opening its own connections */
		if ( false == oneMySQLPerThread ) {
			threadLog=log;
		} else {
			threadLog=null;
		}

		memcache=null;
		if ( true == memcachedDebug ) {
			try {
				memcache=new MemcachedClient(new InetSocketAddress("localhost",11211));
			} catch ( IOException e ) {
				memcache=null;
			}
		}
		
		HTTPServerJSON httpd = new HTTPServerJSON(9000);
		httpd.start();

		/* spin through and accept new connections as quickly as we can */
		while ( listening ) {
			Socket socket=serverSocket.accept();
			/* setup our sockets to send RST as soon as close() is called ... this is the default action */
			socket.setSoLinger (true, 0);


			if ( logConnection ) {
				/* Log the connection before starting the new thread */
				status.addConnection(socket);
				String sql = "INSERT INTO connection (logdate,localPort,remoteIP,remotePort,status) VALUES (" +
						"'" +  dateFormat.format(new Date()) + "', " +
						socket.getLocalPort() + ", " + 
						"'" + socket.getInetAddress().getHostAddress() + "', " +
						socket.getPort() + ", " + 
						"'accept'" +
						")";
				log.queryAutoCreate(sql, "DataGSProto.connection", "connection");
			}

			DataGSServerThread conn = new DataGSServerThread(
					socket,
					threadLog,
					myHost, 
					myUser, 
					myPass, 
					myDB,
					myPort,
					dateFormat,
					socketTimeout,
					stationTimeout, 
					memcache);

			connectionThreads.add(conn);
			conn.addChannelDataListener(this);
			conn.start();

			System.err.println("# connectionThreads.size()=" + connectionThreads.size());
		}

		System.err.print ("# DataGS shuting down server socket ... ");
		serverSocket.close();

		if ( null != threadMaintenanceTimer && threadMaintenanceTimer.isRunning() ) {
			threadMaintenanceTimer.stop();
		}

		if ( null != dataTimer && dataTimer.isRunning() ) {
			dataTimer.stop();
		}
		

		System.err.println("done");
	}

	public static void main(String[] args) throws IOException {
		System.err.println("# Major version: 2014-09-01 (precision)");

		DataGS d=new DataGS();


		d.run(args);
	}
}
