package dataGS;
import java.net.*;

import javax.swing.Timer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;


/* Command line parsing from Apache */
import org.apache.commons.cli.*;
import org.apache.commons.collections4.queue.CircularFifoQueue;


/* Memcache client for logging */
import net.spy.memcached.MemcachedClient;



/* statistics */
import org.apache.commons.math3.stat.descriptive.*;

/* JSON */
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DataGS implements ChannelData, JSONData {
	private Log log;
	private Timer threadMaintenanceTimer;

	private Vector<DataGSServerThread> connectionThreads;
	private MemcachedClient memcache;
	private int portNumber;


	/* data to summarize and send */
	protected Map<Integer, SynchronizedSummaryStatistics> data;
	protected Map<Integer, AdcDouble> dataLast;
	protected int intervalSummary;
	protected Timer dataTimer;
	protected String dataLastJSON;

	/* history data */
	protected CircularFifoQueue<String> historyJSON;

	/* supported databases */
	public static final int DATABASE_TYPE_MYSQL = 0;
	public static final int DATABASE_TYPE_SQLITE = 1;
	public static final int DATABASE_TYPE_NONE = 2;

	/* supported JSON resource requests */
	public static final int JSON_NOW=0;
	public static final int JSON_HISTORY=1;

	public String getJSON(int resource) {
		if ( JSON_NOW == resource ) {
			return "{\"data\": [" + dataLastJSON + "]}";
		} else if ( JSON_HISTORY == resource ) {
			return "{\"history\":" + historyJSON.toString() + "}";
		}


		return "invalid";
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
		long now = System.currentTimeMillis();

		//		System.err.println("######### dataMaintenanceTimer() #########");

		synchronized (data) {
			dataLast.clear();

			/* iterate through and export summary */
			Iterator<Entry<Integer, SynchronizedSummaryStatistics>> it = data.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, SynchronizedSummaryStatistics> pairs = (Map.Entry<Integer, SynchronizedSummaryStatistics>)it.next();

				dataLast.put(pairs.getKey(),new AdcDouble(pairs.getKey(),now,pairs.getValue()));
			}

			/* create a JSON data history point and put into limited length FIFO */
			if ( null != historyJSON ) {
				historyJSON.add(HistoryPointJSON.toJSON(now, data));
				//				System.err.println("# historyJSON is " + historyJSON.size() + " of " + historyJSON.maxSize() + " maximum.");
			}


			/* clear statistics for next pass */
			data.clear();
		}


		/* export latest statistics to JSON */
		Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();


		synchronized ( dataLastJSON ) {
			dataLastJSON="";

			Iterator<Entry<Integer, AdcDouble>> it = dataLast.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, AdcDouble> pairs = (Map.Entry<Integer, AdcDouble>)it.next();
				System.out.println(pairs.getKey() + " = " + pairs.getValue());

				dataLastJSON += gson.toJson(pairs.getValue()) + ", ";


				/* insert into MySQL */
				String table = "adc_" + pairs.getKey();
				AdcDouble a = pairs.getValue();
				String sql = String.format("INSERT INTO %s VALUES(now(), %d, %f, %f, %f, %f)",
						table,
						a.n,
						a.avg,
						a.min,
						a.max,
						a.stddev
				);

				log.queryAutoCreate(sql, "dataGSProto.analogDoubleSummarized", table);

			}

			/* remove last comma */
			if ( dataLastJSON.length() >= 2 ) {
				dataLastJSON = dataLastJSON.substring(0, dataLastJSON.length()-2);
			}
		}
	}

	public void ingest(int channel, double value) {
		Integer ch = new Integer(channel);

		/* initialize the channel if it hasn't be already */
		if ( false == data.containsKey(ch) ) {
			data.put(ch, new SynchronizedSummaryStatistics());
		}


		data.get(ch).addValue(value);
	}


	public void run(String[] args) throws IOException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		/* parse options or use defaults */
		Options options = new Options();
		/* MySQL database connection parameters */
		String myHost, myUser, myPass, myDB;
		int myPort;
		myHost="localhost";
		myUser="";
		myPass="";
		myDB="dataGS";
		myPort=3306;

		/* SQLite connection parameters */
		String sqliteURL="";
		String sqliteProtoURL="";

		/* serial port parameters */
		String serialPortWorldData="";

		/* Data GS parameters */
		portNumber=4010;
		int httpPort=0;
		int socketTimeout=62;
		int stationTimeout=121;
		boolean logConnection=false;
		boolean memcachedDebug=false;
		int databaseType=DATABASE_TYPE_NONE;

		int dataHistoryJSONHours=24;


		intervalSummary = 1000;
		data = new HashMap<Integer, SynchronizedSummaryStatistics>();
		dataLast = new HashMap<Integer, AdcDouble>();

		/* MySQL options */
		options.addOption("d", "database", true, "MySQL database");
		options.addOption("h", "host", true, "MySQL host");
		options.addOption("p", "password", true, "MySQL password");
		options.addOption("u", "user", true, "MySQL username");

		/* sqlite options */
		options.addOption("s", "SQLite-URL",true,"SQLite URL (e.g. DataGS.db");
		options.addOption("S", "SQLite-proto-URL",true,"SQLite prototype URL (e.g. DataGSProto.db");

		/* DataGSCollector options */
		options.addOption("i", "interval", true, "Interval to summarize over (milliseconds)");
		options.addOption("l", "listen-port", true, "DataGSCollector Listening Port");
		options.addOption("t", "socket-timeout",true, "DataGSCollector connection socket timeout");
		options.addOption("T", "station-timeout",true, "DataGSCollector station history timeout");
		options.addOption("m", "memcacheDebug", false, "Debug messages written to memcached per station");

		/* serial port data source options */
		options.addOption("R", "serialPortWorldData",true,"Serial Port to listen for worldData packets");


		/* built-in web server options */
		options.addOption("j", "http-port", true, "webserver port, 0 to disable");
		options.addOption("H", "json-history-hours", true, "hours of history data to make available, 0 to disable");

		/* parse command line */
		CommandLineParser parser = new PosixParser();
		try {
			CommandLine line = parser.parse( options, args );

			/* MySQL */
			if ( line.hasOption("host") ) myHost=line.getOptionValue("host");
			if ( line.hasOption("user") ) myUser=line.getOptionValue("user");
			if ( line.hasOption("password") ) myPass=line.getOptionValue("password");
			if ( line.hasOption("database") ) myDB=line.getOptionValue("database");

			/* SQLite */
			if ( line.hasOption("SQLite-URL") ) sqliteURL= line.getOptionValue("SQLite-URL");
			if ( line.hasOption("SQLite-proto-URL") ) sqliteProtoURL= line.getOptionValue("SQLite-proto-URL");

			/* web server */
			if ( line.hasOption("http-port") ) {
				httpPort = Integer.parseInt(line.getOptionValue("http-port"));
			}
			if ( line.hasOption("json-history-hours") ) {
				dataHistoryJSONHours = Integer.parseInt(line.getOptionValue("json-history-hours"));
			}

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

			/* serial port */
			if ( line.hasOption("serialPortWorldData") ) {
				serialPortWorldData=line.getOptionValue("serialPortWorldData");
			}


			if ( line.hasOption("memcacheDebug") ) memcachedDebug=true;




		} catch (ParseException e) {
			System.err.println("# Error parsing command line: " + e);
		}


		historyJSON=null;
		if ( dataHistoryJSONHours > 0 ) {
			int nPoints=(dataHistoryJSONHours*60*60)/(intervalSummary/1000);

			System.err.printf("# Enabling history JSON for %d hours (%d data points at %d millisecond interval rate)\n",
					dataHistoryJSONHours,
					nPoints,
					intervalSummary);
			historyJSON = new CircularFifoQueue<String>(nPoints);
		} else {
			System.err.println("# History JSON disabled");
			historyJSON=null;
		}



		connectionThreads=new Vector<DataGSServerThread>();
		ServerSocket serverSocket = null;
		boolean listening = true;

		if ( null != myUser && "" != myUser) {
			databaseType=DATABASE_TYPE_MYSQL;
		}

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
		} else if ( DATABASE_TYPE_SQLITE == databaseType ) {
			System.err.println("# Opening SQLite database");
			log = new LogSQLite(sqliteProtoURL,sqliteURL);
			try {
				log.connect();
			} catch (Exception e) {
				e.printStackTrace();
			}			
		} else if ( DATABASE_TYPE_NONE == databaseType ) {
			log = new LogNull();
		}


		/* serial port for WorldData packets */
		if ( "" != serialPortWorldData ) {
			System.err.println("# Listening for WorldData packets on " + serialPortWorldData);
		}


		/* socket for DataGS packets */
		if ( 0 != portNumber ) {
			System.err.println("# Listening on port " + portNumber + " with " + socketTimeout + " second socket timeout and " + stationTimeout + " second station timeout");
			try {
				serverSocket = new ServerSocket(portNumber);
			} catch (IOException e) {
				System.err.println("# Could not listen on port: " + portNumber);
				System.exit(-1);
			}
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
		dataLastJSON="";
		DataGSStatus status = new DataGSStatus(log,portNumber);
		status.start();
		status.updateStatus();




		memcache=null;
		if ( true == memcachedDebug ) {
			try {
				memcache=new MemcachedClient(new InetSocketAddress("localhost",11211));
			} catch ( IOException e ) {
				memcache=null;
			}
		}

		if ( 0 != httpPort ) {
			System.err.println("# HTTP server listening on port " + httpPort);
			HTTPServerJSON httpd = new HTTPServerJSON(httpPort, this);
			httpd.start();
		} else {
			System.err.println("# HTTP server disabled.");
		}

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
					log,
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
		System.err.println("# Major version: 2014-09-19 (precision)");

		DataGS d=new DataGS();


		d.run(args);
	}
}
