package dataGS;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.Timer;

import net.spy.memcached.MemcachedClient;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dataGS.ChannelDescription.Modes;
/* Command line parsing from Apache */
/* Memcache client for logging */
/* JSON */

public class DataGS implements ChannelData, JSONData {
	private Log log;
	private Timer threadMaintenanceTimer;

	private Vector<DataGSServerThread> connectionThreads;
	private MemcachedClient memcache;
	private int portNumber;

	/* channel description data */
	protected Map<String, ChannelDescription> channelDesc;
	protected boolean processAllData;
	
	/* data to summarize and send */
	protected Map<String, SynchronizedSummaryData> data;
	protected Map<String, DataPoint> dataLast;
	protected Map<String, DataPoint> dataNow;

	/* data for the historical data page */
	protected Map<String, HashMap<String, SynchronizedSummaryData>> summaryStatsFromHistory;
	protected boolean summaryReady=false;
	Date date;
	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

	protected int intervalSummary;
	protected Timer dataTimer;
	protected String dataLastJSON;
	protected String dataNowJSON;

	protected String historyFiles;
	protected String logLocalDir;

	protected String summaryStatsJson;

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
	public static final int JSON_SUMMARY_STATS = 3;



	/* loglocal */
	LogLocal logLocal;


	public String getJSON(int resource) {
		if ( JSON_NOW == resource ) {
			return "{\"data\": [" + dataNowJSON + "]}";
		} else if ( JSON_HISTORY == resource ) {
			return "{\"history\":" + historyJSON.toString() + "}";
		} else if ( JSON_HISTORY_FILES == resource ) {
			return "{\"history_files\": {" + historyFiles + "}}";
		} else if ( JSON_SUMMARY_STATS == resource ) {
			if ( summaryReady ) {
				return "{\"summary_stats\": [" + summaryJSON() + "]}";
			} else {
				return "invalid";
			}
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
			dataNow.clear();



			/* TODO get today's syncSumData from summaryStatsFromHistory if ready */
			Map<String, SynchronizedSummaryData> today;
			date = new Date();


			/* last (ie current) data JSON */
			Iterator<Entry<String, SynchronizedSummaryData>> it = data.entrySet().iterator();
			//System.out.println("DGS iterate");
			//System.out.flush();
			while (it.hasNext()) {
				Map.Entry<String, SynchronizedSummaryData> pairs = (Map.Entry<String, SynchronizedSummaryData>)it.next();

				dataNow.put(pairs.getKey(),new DataPoint(pairs.getKey(),now,pairs.getValue()));
				//System.out.println(pairs.getKey()+"="+pairs.getValue());

				if ( summaryReady ){
					today = summaryStatsFromHistory.get( sdf.format( date ) );
					String ch = pairs.getKey();	 


					if ( today.containsKey( ch ) ) {
						if ( today.get(ch).mode == Modes.AVERAGE ) {
							Double s = pairs.getValue().getMean();
							Double d;
							try {
								d=new Double(s);
								today.get(ch).addValue(d);
								//			System.err.println("# ingested " + ch + " as double with value=" + d);
							} catch ( NumberFormatException e ) {
								System.err.println("# error ingesting s=" + s + " as a double. Giving up");
								return;
							}
						} else if ( today.get(ch).mode == Modes.SAMPLE ) {
							String s = pairs.getValue().sampleValue;
							today.get(ch).addValue(s);	
						}
					}
				}

				//today.put();
			}
			//	System.out.println("finished DGS iterate");
			//	System.out.flush();
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
			System.out.println("clearing data");
			data.clear();
		}


		synchronized ( dataNowJSON ) {
			dataNowJSON="";

			Iterator<Entry<String, DataPoint>> it = dataNow.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, DataPoint> pairs = (Map.Entry<String, DataPoint>)it.next();

				/* debugging */
				//System.out.println(pairs.getKey() + " = " + pairs.getValue());

				/* use the DataPoint.toJSON() method to encode the members we care about into JSON */
				dataNowJSON += pairs.getValue().toJSON() + ", ";


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
			if ( dataNowJSON.length() >= 2 ) {
				dataNowJSON = dataNowJSON.substring(0, dataNowJSON.length()-2);
			}
		}



	}



	public String[] listFilesForFolder(String dirName) {
		return listFilesForFolder( dirName, false );
	}
	/** 
	 * 
	 * @param dirName is a String of the directory name to get files from
	 * @param absolute is a boolean that decideds to return just the filename or the absolute path
	 * @return String array of filenames
	 */
	public String[] listFilesForFolder(String dirName, boolean absolute) {

		/* go through a directory and return a string[] with every filename in directory, ignoring sub directories */
		try{
			final File directory = new File(dirName);
			List<String> files = new ArrayList<String>();
			for ( final File fileEntry : directory.listFiles() ) {
				/* if  not a directory */
				String fn = fileEntry.getName();
				if ( !fileEntry.isDirectory() && !fn.contains("~") ) {
					if ( NumberUtils.isNumber(FilenameUtils.getBaseName( fn )) ) {
						if ( absolute ) {
							files.add( fileEntry.getAbsolutePath() );
						} else {
							files.add( fileEntry.getName() );
						}
					}

				}
			}
			String[] sort = files.toArray( new String[ files.size() ] );
			Arrays.sort(sort, Collections.reverseOrder());
			return  sort;
		} catch (Exception e) {
			System.err.println("Directory does not exist");
			return new String[] {"does-not-exist"};
		}

	}



	/**
	 * 
	 * @param files to be converted to json
	 * @return a json String of the files
	 */
	public String filesToJson(String[] files){
		String json="";
		for ( int i = 0 ; i < files.length ; i++ ) {
			json+="\""+StringEscapeUtils.escapeJson( files[i] )+"\",";

		}
		return json.substring( 0, json.length()-1 );
	}

	/**
	 * 
	 * @param files is the String array of files that will be summarized
	 */
	public void createSummaryStatsFromHistory(String[] files){

		/* initialize summaryStatsFromHistory hashmap NOTE: this may need to move somewhere else*/
		long time = System.currentTimeMillis();
		summaryStatsFromHistory = new HashMap<String, HashMap<String, SynchronizedSummaryData>>();

		String date;
		for ( int i = 0 ; i < files.length ; i++ ) {
			date=FilenameUtils.getBaseName( files[i] );//FilenameUtils.removeExtension(files[i]);
			//System.out.println(files[i]);
			summaryStatsFromHistory.put( date, getSyncSumDatEntry( files[i] ) );
		}
		System.out.println("Files all traveled. Took "+((System.currentTimeMillis()-time)/1000)+" seconds");

	}



	/**
	 * This method returns a hashmap of the summary of the file
	 * @param absolutePath The path to the files that need to be opened and summarized
	 * @return HashMap with the channel name as the key and SynchronizedSummaryData as the value
	 */
	public HashMap<String, SynchronizedSummaryData> getSyncSumDatEntry(String absolutePath){
		/* hashmap to be returned */
		Map<String, SynchronizedSummaryData> tempSummaryStat = new HashMap<String, SynchronizedSummaryData>();

		/* iterate through channelDesc, finding every channel that contains the key summaryStatsFromHistory */

		Iterator<Entry<String, ChannelDescription>> it = channelDesc.entrySet().iterator();

		while ( it.hasNext() ) {

			Map.Entry<String, ChannelDescription> pairs = (Map.Entry<String, ChannelDescription>)it.next();
			if ( pairs.getValue().summaryStatsFromHistory ) {
				//System.out.println(pairs.getValue().id);
				tempSummaryStat.put( pairs.getValue().id, new SynchronizedSummaryData(pairs.getValue().mode) );
			}
		}

		/* open file from absolutePath and use apache commons csv parser to get info and summarize it */
		File file = new File(absolutePath);
		//	System.out.println(absolutePath);
		/* Scanner is being used to read in the csv file line by line */
		Scanner scanner;
		CSVParser parser;
		try { 
			Iterator<Entry<String, SynchronizedSummaryData>> itS;
			Map.Entry<String, SynchronizedSummaryData> pairs;
			/* open the file */
			scanner = new Scanner(file);
			/* first line of the csv file is the header */
			String header=scanner.nextLine();
			//parser = CSVParser.parse(header, CSVFormat.DEFAULT);
			/* We have to remove the quotes and the spaces in the header line */
			header= header.replace( "\"", "" );
			header= header.replace( " ", "" );	
			String line = "";
			while (scanner.hasNext()){

				line = scanner.nextLine();
				line= line.replace( "\"", "" );
				line= line.replace( " ", "" );
				parser = CSVParser.parse(line, CSVFormat.DEFAULT.withHeader( header.split( "," ) ));
				/* parse each csv line */
				for (CSVRecord csvRecord : parser) {
					/* iterate through tempSumStat and add each csv value needed to it's SyncSumData */
					itS = tempSummaryStat.entrySet().iterator();
					while(itS.hasNext()){
						try{
							pairs = (Map.Entry<String, SynchronizedSummaryData>)itS.next();
							String sVal = csvRecord.get(pairs.getKey()).replace( "\"", "" );
							if ( !sVal.contains( "NULL" )  ){

								/* convert String value into a double. Originally done in one line but broken apart for readability. */
								double dval = Double.parseDouble(sVal);

								/* add double */
								pairs.getValue().addValue( dval );
							}
						}catch(Exception e){
							System.err.println(e);
							System.err.println("header: " + header);
							System.err.println("line: " + line);
						}
					}
				}

			}
			/* close scanner */
			scanner.close();

		} catch ( IOException e ) {
			e.printStackTrace();
			System.err.println("error occured on this day: " + absolutePath);
		}



		return (HashMap<String, SynchronizedSummaryData>) tempSummaryStat;
	}



	public void ingest(String ch, String s) {

		/* null is used as a flag that we have a complete measurement set and we can publish to 
		 * "live" page that shows that absolutely latest set of samples */ 

		if ( null == ch )
			return;


		/* we don't need to do anything if we aren't using the channel */
		if ( ! processAllData && (! channelDesc.containsKey(ch) || (! channelDesc.get(ch).log && ! channelDesc.get(ch).history)) ) {
			return;
		}



		/* if data hashtable doesn't have the key for this channel, we add it
		 * the mode (sampled or averaged) is read from channel description or defaults to sample  */
		if ( ! data.containsKey(ch) ) {
			System.err.println("# Putting to data (" + ch + " as SynchronizedSummaryData with mode " + channelDesc.get(ch).mode + ")");
			
			if ( channelDesc.containsKey(ch) ) {
				data.put(ch, new SynchronizedSummaryData( channelDesc.get(ch).mode ) );
			} else {
				data.put(ch, new SynchronizedSummaryData( Modes.SAMPLE ) );
			}
		}


		if ( data.get(ch).mode == Modes.AVERAGE ) {
			Double d;
			try {
				d=new Double(s);
				data.get(ch).addValue(d);
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


	processAllData=false;
	intervalSummary = 1000;
	data = new HashMap<String, SynchronizedSummaryData>();
	dataLast = new HashMap<String, DataPoint>();
	dataNow = new HashMap<String, DataPoint>();


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
	options.addOption("a", "process-all-data",false,"Process all data, even if it isn't in channel map");

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
		if ( line.hasOption("process-all-data") ) {
			processAllData=true;
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
		System.err.flush();

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
		System.err.flush();

	}


	historyJSON=null;
	if ( dataHistoryJSONHours > 0 ) {
		int nPoints=(dataHistoryJSONHours*60*60)/(intervalSummary/1000);

		System.err.printf("# Enabling history JSON for %d hours (%d data points at %d millisecond interval rate)\n",
				dataHistoryJSONHours,
				nPoints,
				intervalSummary);
		System.err.flush();
		historyJSON = new CircularFifoQueue<String>(nPoints);
	} else {
		System.err.println("# History JSON disabled");
		System.err.flush();
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
		System.err.flush();

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
		System.err.flush();
		try {
			serverSocket = new ServerSocket(portNumber);
			listening=true;
		} catch (IOException e) {
			System.err.println("# Could not listen on port: " + portNumber);
			System.exit(-1);
		}
	} else {
		System.err.println("# DataGS socket disabled because portNumber=0");
		System.err.flush();
	}





	/* timer to periodically handle the data */
	dataTimer = new javax.swing.Timer(intervalSummary, new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			dataMaintenanceTimer();
		}
	});
	dataTimer.start();




	/* get array of all filenames from logLocalDir */
	String[] files = listFilesForFolder( logLocalDir );

	/* if files are returned, create the JSON file with all the loglocal files in it */
	if ( !files[0].equals("does-not-exist") ){
		historyFiles="\"files\":["+filesToJson( files )+"]";
	} else {
		historyFiles="\"files\":[]";
	}

	/* Create summary in another thread */
	(new Thread(new summaryHistoryThread())).start();

	/* start status update thread */
	dataLastJSON="";
	dataNowJSON="";

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
		System.err.flush();
		HTTPServerJSON httpd = new HTTPServerJSON(httpPort, this, channelMapFile, logLocalDir);
		httpd.start();
	} else {
		System.err.println("# HTTP server disabled.");
		System.err.flush();
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
	System.err.flush();
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



public String summaryJSON(){
	try {
		/* The string to be returned */
		String json = "";
		/* part is used to avoid having to remove the last comma for each day */
		String part = "";
		//HashMap<String,SynchronizedSummaryData> ssfh= summaryStatsFromHistory.get( date );

		/* iterator for the Map with date (string) as key and Map as value */
		Iterator<Entry<String,HashMap<String, SynchronizedSummaryData>>> itS;
		itS = summaryStatsFromHistory.entrySet().iterator();

		/* the map with columnNames (string) as key and SyncSumData as value */
		HashMap<String,SynchronizedSummaryData> ssd;
		/* iterator for the columnName (string) with date as key and SyncSumData as value */
		Iterator<Entry<String, SynchronizedSummaryData>> it;
		/* The key value pair that uses the columnName (string) as a key */
		Map.Entry<String, SynchronizedSummaryData> pairs;


		/* The key value pair that uses the date (string) as a key */
		Map.Entry<String,HashMap<String, SynchronizedSummaryData>> dateMap;// = (Map.Entry<String,HashMap<String, SynchronizedSummaryData>>)it.next();
		/* iterate through summaryStatsFrom History */
		while ( itS.hasNext() ) {

			dateMap = (Map.Entry<String,HashMap<String, SynchronizedSummaryData>>)itS.next();
			json += "{";
			/* The map that has a SyncSumData */
			ssd= summaryStatsFromHistory.get( dateMap.getKey() );
			it = ssd.entrySet().iterator();
			if ( it.hasNext() ){
				pairs = (Map.Entry<String, SynchronizedSummaryData>)it.next();
				//	System.out.println(pairs.getValue().toString());
				json+="\"day\":"+dateMap.getKey()+",";
				json+="\"n\":"+NaNcheck(ssd.get( pairs.getKey() ).getN())+",";


				/* get the first SyncSumData */
				part="\""+pairs.getKey()+"_min\":"+NaNcheck(ssd.get( pairs.getKey() ).getMin())+",";
				part+="\""+pairs.getKey()+"_max\":"+NaNcheck(ssd.get( pairs.getKey() ).getMax())+",";
				part+="\""+pairs.getKey()+"_avg\":"+NaNcheck(ssd.get( pairs.getKey() ).getMean())+"";
			}
			while ( it.hasNext() ) {
				//Map.Entry<String, SynchronizedSummaryData> pairs = (Map.Entry<String, SynchronizedSummaryData>)it.next();
				pairs = (Map.Entry<String, SynchronizedSummaryData>)it.next();
				part="\""+pairs.getKey()+"_min\":"+NaNcheck(ssd.get( pairs.getKey() ).getMin())+","+part;
				part="\""+pairs.getKey()+"_max\":"+NaNcheck(ssd.get( pairs.getKey() ).getMax())+","+part;
				part="\""+pairs.getKey()+"_avg\":"+NaNcheck(ssd.get( pairs.getKey() ).getMean())+","+part;
			}
			json += part+"},";

		}

		/* remove the last comma */
		return json.substring( 0, json.length()-1 );
	}catch(Exception e){
		/* if syncSumData isn't ready, set to initializing*/
		return null;
	}
}

/* check if NaN */
public String NaNcheck( double check ){
	if( Double.isNaN( check ) ){
		return "null";
	}

	return "\""+check+"\"";
}
public String NaNcheck( String check ){

	return "\""+check+"\"";
}

/* thread that runs the method to create the summary stats json */
public class summaryHistoryThread implements Runnable {

	public void run() {
		long timer = System.currentTimeMillis();
		String[] files = listFilesForFolder(logLocalDir, true);
		if ( !files[0].equals( "does-not-exist" ) ) {
			createSummaryStatsFromHistory( files );
			System.out.println("summary creation took: "+((System.currentTimeMillis()-timer)/1000)+" seconds");
			summaryReady = true;
		} else {
			summaryReady = false;
		}
	}

}

/* Main method */
public static void main(String[] args) throws IOException {
	System.err.println("# Major version: 2014-10-28 (precision)");
	System.err.println("# java.library.path: " + System.getProperty( "java.library.path" ));

	DataGS d=new DataGS();
	d.run(args);
}

}
