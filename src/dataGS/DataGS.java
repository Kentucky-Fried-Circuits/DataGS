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
import org.apache.commons.lang3.StringEscapeUtils;


/* Memcache client for logging */
import net.spy.memcached.MemcachedClient;




/* JSON */
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dataGS.ChannelDescription.Modes;

public class DataGS implements ChannelData, JSONData {
	private Log log;
	private Timer threadMaintenanceTimer;

	private Vector<DataGSServerThread> connectionThreads;
	private MemcachedClient memcache;
	private int portNumber;

	/* channel description data */
	protected Map<String, ChannelDescription> channelDesc;

	/* data to summarize and send */
	protected Map<String, SynchronizedSummaryData> data;
	protected Map<String, DataPoint> dataLast;
	protected int intervalSummary;
	protected Timer dataTimer;
	protected String dataLastJSON;
	
	protected String historyFiles;
	protected String logLocalDir;
	/* history data */
	protected CircularFifoQueue<String> historyJSON;

	/* supported databases */
	public static final int DATABASE_TYPE_MYSQL = 0;
	public static final int DATABASE_TYPE_SQLITE = 1;
	public static final int DATABASE_TYPE_NONE = 2;

	/* supported JSON resource requests */
	public static final int JSON_NOW=0;
	public static final int JSON_HISTORY=1;
	public static final int JSON_HISTORY_FILES = 2;
	
	/* loglocal */
	LogLocal logLocal;
	
	
	public String getJSON(int resource) {
		if ( JSON_NOW == resource ) {
			return "{\"data\": [" + dataLastJSON + "]}";
		} else if ( JSON_HISTORY == resource ) {
			return "{\"history\":" + historyJSON.toString() + "}";
		} else if ( JSON_HISTORY_FILES == resource ) {
			return "{\"history_files\": {" + historyFiles + "}}";
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

			
			/* last (ie current) data JSON */
			Iterator<Entry<String, SynchronizedSummaryData>> it = data.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, SynchronizedSummaryData> pairs = (Map.Entry<String, SynchronizedSummaryData>)it.next();

				dataLast.put(pairs.getKey(),new DataPoint(pairs.getKey(),now,pairs.getValue()));
			}

			/* create a JSON data history point and put into limited length FIFO */
			if ( null != historyJSON ) {
				historyJSON.add(HistoryPointJSON.toJSON(now, data, channelDesc));
				//	System.err.println("# historyJSON is " + historyJSON.size() + " of " + historyJSON.maxSize() + " maximum.");
			}
			
			/* loglocal */
			if ( null != logLocal) {
				/* log line to local file */
				logLocal.log( HistoryPointJSON.toCSV( data, channelDesc )[0], new Date(now), HistoryPointJSON.toCSV( data, channelDesc )[1] );
			}
			
			/* clear statistics for next pass */
			data.clear();
		}


		synchronized ( dataLastJSON ) {
			dataLastJSON="";

			Iterator<Entry<String, DataPoint>> it = dataLast.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, DataPoint> pairs = (Map.Entry<String, DataPoint>)it.next();

				/* debugging */
				System.out.println(pairs.getKey() + " = " + pairs.getValue());

				/* use the DataPoint.toJSON() method to encode the members we care about into JSON */
				dataLastJSON += pairs.getValue().toJSON() + ", ";


				/* insert into MySQL */
				String table = "adc_" + pairs.getKey();
				DataPoint a = pairs.getValue();
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
	
	
	/* go through a directory and return a string with every filename in directory, ignoring sub directories */
	public String listFilesForFolder(String dirName) {
		final File directory = new File(dirName);
		String files="";
		for ( final File fileEntry : directory.listFiles() ) {
			/* if  not a directory */
			if ( !fileEntry.isDirectory() ) {
				files+="\""+StringEscapeUtils.escapeJson( fileEntry.getName() )+"\",";
				//System.out.println(fileEntry.getName());
			}
		}
		return files.substring(0,files.length()-1 );
	}

	public void ingest(String ch, String s) {
		/* create with appropriate mode, if we have this channel in our channel description */
		if ( channelDesc.containsKey(ch) ) {
			data.put(ch, new SynchronizedSummaryData( channelDesc.get(ch).mode ) );
		} else {
			data.put(ch, new SynchronizedSummaryData(ChannelDescription.Modes.AVERAGE));
		}
		
		
		if ( data.get(ch).mode == Modes.AVERAGE ) {
			Double d;
			try {
				d=new Double(s);
				data.get(ch).addValue(d);
	//			System.err.println("# ingested " + ch + " as double with value=" + d);
			} catch ( NumberFormatException e ) {
				System.err.println("# error ingesting s=" + s + " as a double. Giving up");
				return;
			}
		} else if ( data.get(ch).mode == Modes.SAMPLE ) {
			data.get(ch).addValue(s);	
		}
				
		
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
		int serialPortWorldDataSpeed=9600;

		/* Data GS parameters */
		portNumber=4010;
		int httpPort=0;
		int socketTimeout=62;
		int stationTimeout=121;
		boolean logConnection=false;
		boolean memcachedDebug=false;
		int databaseType=DATABASE_TYPE_NONE;
		String channelMapFile="www/channels.json";

		int dataHistoryJSONHours=24;


		intervalSummary = 1000;
		data = new HashMap<String, SynchronizedSummaryData>();
		dataLast = new HashMap<String, DataPoint>();

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
		options.addOption("c", "channel-map", true, "Location of channel map JSON file");

		/* serial port data source options */
		options.addOption("r", "serialPortWorldData",true,"Serial Port to listen for worldData packets");
		options.addOption("R", "serialPortWorldDataSpeed",true,"Serial port speed");


		/* built-in web server options */
		options.addOption("j", "http-port", true, "webserver port, 0 to disable");
		options.addOption("H", "json-history-hours", true, "hours of history data to make available, 0 to disable");

		/* loglocal */
		options.addOption("w", "loglocal-directory", true, "directory for logging csv files");
		
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
			if ( line.hasOption("channel-map") ) {
				channelMapFile=line.getOptionValue("channel-map");
			}

			/* serial port */
			if ( line.hasOption("serialPortWorldData") ) {
				serialPortWorldData=line.getOptionValue("serialPortWorldData");
			}
			if ( line.hasOption("serialPortWorldDataSpeed") ) {
				serialPortWorldDataSpeed = Integer.parseInt(line.getOptionValue("serialPortWorldDataSpeed"));
			}


			if ( line.hasOption("memcacheDebug") ) memcachedDebug=true;
			
			/* loglocal */
			if ( line.hasOption("loglocal-directory") ) {
				logLocalDir = line.getOptionValue("loglocal-directory");
				logLocal = new LogLocal( logLocalDir, true );
				
			} 




		} catch (ParseException e) {
			System.err.println("# Error parsing command line: " + e);
		}


		/* load channels.json and de-serialize it into a hashmap */
		channelDesc = new HashMap<String, ChannelDescription>();

		System.err.println("# channel map file is " + channelMapFile);
		File cmf = new File(channelMapFile);
		if ( cmf.exists() && ! cmf.isDirectory() ) {
			System.err.print("# Loading channel description from " + channelMapFile + " ...");

			/* used for deserializing json */
			Gson gson = new GsonBuilder().create();
			ChannelDescription cd;

			/* get string array of json objects to deserialize  */
			String[] jsonStrArray = getJson(channelMapFile);
			
			/* iterate through jsonStrArray and create a ChannelDescription object 
			 * and add it to the hash map */
			for ( int i = 0; i<jsonStrArray.length; i++ ) {
				cd = gson.fromJson( jsonStrArray[i], ChannelDescription.class );
				channelDesc.put( cd.id, cd );
			}
			System.err.println(" done. " + channelDesc.size() + " channels loaded.");
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



		/* track our data source threads */
		connectionThreads=new Vector<DataGSServerThread>();
		/* timer to periodically clear thread listing */
		threadMaintenanceTimer = new javax.swing.Timer(5000, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				threadMaintenanceTimer();
			}
		});
		threadMaintenanceTimer.start();


		/* serial port for WorldData packets */
		if ( "" != serialPortWorldData ) {
			System.err.println("# Listening for WorldData packets on " + serialPortWorldData);

			WorldDataSerialReader ser = new WorldDataSerialReader(serialPortWorldData, serialPortWorldDataSpeed);
			WorldDataProcessor worldProcessor = new WorldDataProcessor();
			worldProcessor.addChannelDataListener(this);
			ser.addPacketListener(worldProcessor);
		}


		/* socket for DataGS packets */
		ServerSocket serverSocket = null;
		boolean listening = false;

		if ( 0 != portNumber ) {
			System.err.println("# Listening on port " + portNumber + " with " + socketTimeout + " second socket timeout and " + stationTimeout + " second station timeout");
			try {
				serverSocket = new ServerSocket(portNumber);
				listening=true;
			} catch (IOException e) {
				System.err.println("# Could not listen on port: " + portNumber);
				System.exit(-1);
			}
		} else {
			System.err.println("# DataGS socket disabled because portNumber=0");
		}





		/* timer to periodically handle the data */
		dataTimer = new javax.swing.Timer(intervalSummary, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dataMaintenanceTimer();
			}
		});
		dataTimer.start();



		/* start status update thread */
		dataLastJSON="";
		historyFiles="\"files\":["+listFilesForFolder( logLocalDir )+"]";
		DataGSStatus status = new DataGSStatus(log,portNumber);
		status.start();
		status.updateStatus();



		/* memcache debugging */
		memcache=null;
		if ( true == memcachedDebug ) {
			try {
				memcache=new MemcachedClient(new InetSocketAddress("localhost",11211));
			} catch ( IOException e ) {
				memcache=null;
			}
		}

		/* built in http server to provide data */
		if ( 0 != httpPort ) {
			System.err.println("# HTTP server listening on port " + httpPort);
			HTTPServerJSON httpd = new HTTPServerJSON(httpPort, this, channelMapFile, logLocalDir);
			httpd.start();
		} else {
			System.err.println("# HTTP server disabled.");
		}

		/* spin through and accept new connections as quickly as we can ... in DataGS format. */
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

		if ( null != serverSocket ) {
			System.err.print ("# DataGS shuting down server socket ... ");
			serverSocket.close();
		}

		if ( null != threadMaintenanceTimer && threadMaintenanceTimer.isRunning() ) {
			threadMaintenanceTimer.stop();
		}

		if ( null != dataTimer && dataTimer.isRunning() ) {
			dataTimer.stop();
		}


		System.err.println("# dataGS done");
	}

	/* This method opens the file passed to it 
	 * and returns the json object array
	 *  as a string array of json objects */
	public static String[] getJson( String filename ){
		/* create BufferedREader to open file and read character by character */
		BufferedReader br;

		/* these will be used when reading in the file. 
		 * token is each character read in and 
		 * toSplit is to end up as a string representation of the array of json objects */
		char token;
		String toSplit="";
		boolean start = false;
		try{
			br = new BufferedReader( new FileReader(filename) );
			boolean readToken = true;
			while ( readToken ) {
				token = (char) br.read();
				/* If we get a -1 then that means we reached the end of the file */
				if ( (char)-1 == token ){
					readToken = false;
					break;
				}

				/* This bracket indicates that we have gotten to the end of the json object array */
				if ( ']' == token ) {
					start=false;
					//readToken=false;
				}

				/* If we have found the beginning of the json object array, then we
				 * add the token to the toSplit string */
				if ( start ) {
					toSplit+=token;
				}

				/* This bracket indicates that we have found the beginning of the json object array */
				if ( '[' == token ) 
					start=true;
			}
			/* we are done with the file so we close the bufferedreader */
			br.close();
		} catch ( Exception e ) {
			System.err.println(e);
		}
		//System.out.println(toSplit);
		/* Now we have a string that looks something like this
		 *  { <string of json object> },{ <string of json object> },{ <string of json object> },...{ <string of json object> }
		 * 
		 * 
		 *  */
		String[] split = toSplit.split( "},");

		/* iterate through the split array and add the '}' bracket back without the ',' comma */
		for ( int i = 0; i < split.length-1; i++ ){
			split[i] = split[i] + "}";

		}

		return split;
	}

	public static void main(String[] args) throws IOException {
		System.err.println("# Major version: 2014-10-15 (ian laptop)");
		System.err.println("# java.library.path: " + System.getProperty( "java.library.path" ));

		DataGS d=new DataGS();
		d.run(args);
	}
}
