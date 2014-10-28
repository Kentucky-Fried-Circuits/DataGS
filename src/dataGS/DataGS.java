package dataGS;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.Timer;

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dataGS.ChannelDescription.Modes;
/* Command line parsing from Apache */
/* JSON */

public class DataGS implements ChannelData, JSONData {
	private final boolean debug=false;

	private Log log;
	private Timer threadMaintenanceTimer;

	private Vector<DataGSServerThread> connectionThreads;
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
	protected Date date;
	protected SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

	protected int intervalSummary;
	protected Timer dataTimer;
	protected String dataLastJSON;

	/* JSON array of our latest completed data */
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


	/*
	 * this method is called by our HTTP server to get dynamic data from us
	 */
	public String getJSON(int resource) {
		if ( JSON_NOW == resource ) {
			synchronized ( dataNowJSON) {
				return "{\"data\": [" + dataNowJSON + "]}";
			}
		} else if ( JSON_HISTORY == resource ) {
			synchronized ( historyJSON) {
				return "{\"history\":" + historyJSON.toString() + "}";				
			}
		} else if ( JSON_HISTORY_FILES == resource ) {
			synchronized ( historyFiles ) {
				return "{\"history_files\": {" + historyFiles + "}}";
			}
		} else if ( JSON_SUMMARY_STATS == resource ) {
			if ( summaryReady ) {
				return "{\"summary_stats\": [" + dailySummaryJSON() + "]}";
			} else {
				return "invalid";
			}
		}


		return "invalid";
	}


	/* track our connection threads for TCP incoming connections */
	private void threadMaintenanceTimer() {
		for ( int i=0 ; i<connectionThreads.size(); i++ ) {
			DataGSServerThread conn=connectionThreads.elementAt(i);

			/* delete dead threads */
			if ( ! conn.isAlive() ) {
				connectionThreads.remove(conn);
			}
		}
	}

	/* take are accumulating samples and publish them */
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

			while (it.hasNext()) {
				Map.Entry<String, SynchronizedSummaryData> pairs = (Map.Entry<String, SynchronizedSummaryData>)it.next();

				dataNow.put(pairs.getKey(),new DataPoint(pairs.getKey(),now,pairs.getValue()));

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

		if ( null == ch ) {
			long now = System.currentTimeMillis();
			long started = 0;

			try {
				started=Long.parseLong(s);
			} catch ( NumberFormatException e ) {
				return;
			}

			if ( debug ) {
				System.err.println("# whole packet took " + (now-started) + "ms to be ingested");
				System.err.flush();
			}

			return;
		}



		/* we don't need to do anything if we aren't using the channel */
		if ( ! processAllData && (! channelDesc.containsKey(ch) || (! channelDesc.get(ch).log && ! channelDesc.get(ch).history)) ) {
			return;
		}



		/* if data hashtable doesn't have the key for this channel, we add it
		 * the mode (sampled or averaged) is read from channel description or defaults to sample  */
		if ( ! data.containsKey(ch) ) {
			//System.err.println("# Putting to data (" + ch + " as SynchronizedSummaryData with mode " + channelDesc.get(ch).mode + ")");

			if ( channelDesc.containsKey(ch) ) {
				data.put(ch, new SynchronizedSummaryData( channelDesc.get(ch).mode ) );
			} else {
				data.put(ch, new SynchronizedSummaryData( Modes.SAMPLE ) );
			}
		}


		/* actually add the value to data structure */
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


	protected void loadHistoryFromFiles() {
		/* get array of all filenames from logLocalDir */
		String[] files = new UtilFiles().listFilesForFolder( logLocalDir );

		if ( null == files || 0==files.length ) {
			historyFiles="\"files\":[]";
		} else {
			historyFiles="\"files\":["+filesToJson( files )+"]";
		}
		System.err.println("# " + files.length + " files listed for historyFiles.json");
		System.err.flush();


		System.err.println("# Starting thread to read logLocal files and summarize for history.json");
		/* Create summary in another thread */
		(new Thread(new summaryHistoryThread())).start();
	}
	
	protected void loadChannelMapFile(String channelMapFile) {
		long startTime = System.currentTimeMillis();
		System.err.println("# channel map file is " + channelMapFile);
		File cmf = new File(channelMapFile);
		
		
		if ( cmf.exists() && ! cmf.isDirectory() ) {

			System.err.print("# Loading channel description from " + channelMapFile + " ...");
			System.err.flush();

			/* used for deserializing json */
			Gson gson = new GsonBuilder().create();
			ChannelDescription cd;

			/* get string array of json objects to deserialize  */
			String[] jsonStrArray = UtilFiles.getJsonFromFile(channelMapFile);

			/* iterate through jsonStrArray and create a ChannelDescription object 
			 * and add it to the hash map */
			for ( int i = 0; i<jsonStrArray.length; i++ ) {
				cd = gson.fromJson( jsonStrArray[i], ChannelDescription.class );
				channelDesc.put( cd.id, cd );
			}
			System.err.println(" done. " + channelDesc.size() + " channels loaded in " + 
					(System.currentTimeMillis()-startTime) + " ms.");
			System.err.flush();

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
		portNumber=0;
		int httpPort=0;
		int socketTimeout=62;
		int databaseType=DATABASE_TYPE_NONE;
		String channelMapFile="www/channels.json";
		int dataHistoryJSONHours=24;


		logLocalDir=null;
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

		File cmf = new File(channelMapFile);
		if ( cmf.exists() && ! cmf.isDirectory() ) {
			loadChannelMapFile(channelMapFile);
		} else {
			System.err.println("# " + channelMapFile + " not found. Using empty channel map");
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

		if ( processAllData ) {
			System.err.println("# Processing all data channels, even if not enabled or found in channel file.");
		} else {
			System.err.println("# Processing only data marked log or history in channel file.");
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








		/* timer to periodically handle the data */
		dataTimer = new javax.swing.Timer(intervalSummary, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dataMaintenanceTimer();
			}
		});
		dataTimer.start();


		/* attempt to load history from files if logLocalDir is set */
		if ( null != logLocalDir ) {
			System.err.println("# Loading History from LogLocal directory " + logLocalDir );
			System.err.flush();
			loadHistoryFromFiles();
		}



		dataLastJSON="";
		dataNowJSON="";



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

		/* socket for DataGS packets */
		ServerSocket serverSocket = null;
		boolean listening = false;

		if ( 0 != portNumber ) {
			System.err.println("# Listening on port " + portNumber + " with " + socketTimeout + " second socket timeout");
			System.err.flush();
			try {
				serverSocket = new ServerSocket(portNumber);
				listening=true;
			} catch (IOException e) {
				System.err.println("# Could not listen on port: " + portNumber);
				System.exit(-1);
			}

			/* start status update thread */
			DataGSStatus status = new DataGSStatus(log,portNumber);
			status.start();
			status.updateStatus();
		} else {
			System.err.println("# DataGS socket disabled because portNumber=0");
			System.err.flush();
		}

		/* spin through and accept new connections as quickly as we can ... in DataGS format. */
		while ( listening ) {
			Socket socket=serverSocket.accept();
			/* setup our sockets to send RST as soon as close() is called ... this is the default action */
			socket.setSoLinger (true, 0);


			DataGSServerThread conn = new DataGSServerThread(
					socket,
					log,
					dateFormat,
					socketTimeout
					);

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





	public String dailySummaryJSON(){
		/* The string to be returned */
		String json = "";
		/* part is used to avoid having to remove the last comma for each day */
		String part = "";

		/* @Ian - what in here would throw an exception? Is that normal flow */
		try {
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
			Map.Entry<String,HashMap<String, SynchronizedSummaryData>> dateMap;

			/* iterate through summaryStatsFrom History */
			while ( itS.hasNext() ) {

				dateMap = (Map.Entry<String,HashMap<String, SynchronizedSummaryData>>)itS.next();
				json += "{";
				/* The map that has a SyncSumData */
				ssd= summaryStatsFromHistory.get( dateMap.getKey() );
				it = ssd.entrySet().iterator();
				if ( it.hasNext() ){
					pairs = (Map.Entry<String, SynchronizedSummaryData>)it.next();

					json+="\"day\":"+dateMap.getKey()+",";
					json+="\"n\":"+NaNcheck(ssd.get( pairs.getKey() ).getN())+",";


					/* get the first SyncSumData */
					part="\""+pairs.getKey()+"_min\":"+NaNcheck(ssd.get( pairs.getKey() ).getMin())+",";
					part+="\""+pairs.getKey()+"_max\":"+NaNcheck(ssd.get( pairs.getKey() ).getMax())+",";
					part+="\""+pairs.getKey()+"_avg\":"+NaNcheck(ssd.get( pairs.getKey() ).getMean())+"";
				}
				while ( it.hasNext() ) {
					pairs = (Map.Entry<String, SynchronizedSummaryData>)it.next();
					part="\""+pairs.getKey()+"_min\":"+NaNcheck(ssd.get( pairs.getKey() ).getMin())+","+part;
					part="\""+pairs.getKey()+"_max\":"+NaNcheck(ssd.get( pairs.getKey() ).getMax())+","+part;
					part="\""+pairs.getKey()+"_avg\":"+NaNcheck(ssd.get( pairs.getKey() ).getMean())+","+part;
				}
				json += part+"},";

			}

			/* remove the last comma */
			return json.substring( 0, json.length()-1 );
		} catch (Exception e) {
			/* if syncSumData isn't ready, set to initializing*/
			return null;
		}
	}

	/* check if NaN */
	public String NaNcheck( double check ){
		if ( Double.isNaN( check ) ){
			return "null";
		}

		return "\""+check+"\"";
	}

	/* Main method */
	public static void main(String[] args) throws IOException {
		System.err.println("# Major version: 2014-10-28 (precision)");
		System.err.println("# java.library.path: " + System.getProperty( "java.library.path" ));

		DataGS d=new DataGS();
		d.run(args);
	}


	/* thread that runs the method to create the summary stats json */
	/* TODO: get rid of inner class ... doesn't meet APRS World's coding standards */
	public class summaryHistoryThread implements Runnable {

		public void run() {
			long startTime = System.currentTimeMillis();

			String[] files = new UtilFiles().listFilesForFolder(logLocalDir, true);

			if ( null != files && 0 != files.length ) {

				createSummaryStatsFromHistory( files );

				System.err.println("# SummaryHistoryThread completed summarizing logLocalFiles in "+((System.currentTimeMillis()-startTime)/1000)+" seconds");
				summaryReady = true;
			} else {
				summaryReady = false;
			}
		}

	}


}
