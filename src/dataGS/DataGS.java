package dataGS;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.Timer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dataGS.ChannelDescription.Modes;
/* Command line parsing from Apache */
/* JSON */

public class DataGS implements ChannelData, JSONData {
	private final boolean debug=false;

	/* database log */
	private Log log;

	/* TCP/IP */
	private Timer threadMaintenanceTimer;
	private Vector<DataGSServerThread> connectionThreads;
	private int portNumber;

	/* channel description data */
	protected Map<String, ChannelDescription> channelDesc;
	protected boolean processAllData;

	/* data to summarize and send */
	protected Map<String, SynchronizedSummaryData> data;
	protected Map<String, DataPoint> dataNow;

	/* data for the historical data page */
	protected Date date;
	protected final SimpleDateFormat sdfYYYYMMDD = new SimpleDateFormat("yyyyMMdd");

	/* data for the min, max, average data that is generated for each day */
	protected Map<String, HashMap<String, SynchronizedSummaryData>> historyStatsByDay;
	protected boolean historyStatsByDayReady=false;
	/* JSON array of log local files */
	protected String historyDayLogFilesJSON;


	protected int intervalSummary;
	protected Timer dataTimer;


	/* JSON array of our latest completed data */
	protected StringBuilder dataNowJSON;

	/* log local (CSV) */
	protected LogLocal logLocal;
	protected String logLocalDir;

	/* web server */
	protected File documentRoot;

	/* history data */
	protected DataRecent dataRecent;

	/* 24 hour min, max, and averaged data with channel as the key */ 
	protected Map<String, DescriptiveStatistics> dayStats;

	/* supported databases */
	public static final int DATABASE_TYPE_MYSQL  = 0;
	public static final int DATABASE_TYPE_SQLITE = 1;
	public static final int DATABASE_TYPE_NONE   = 2;

	/* supported JSON resource requests */
	public static final int JSON_NOW            = 0;
	public static final int JSON_RECENT_DATA    = 1;
	public static final int JSON_HISTORY_FILES  = 2;
	public static final int JSON_HISTORY_BY_DAY = 3;
	public static final int JSON_DAY_STATS      = 4;
	public static final int JSON_HOST_INFO      = 5;


	/*
	 * this method is called by our HTTP server to get dynamic data from us
	 */
	public String getJSON(int resource) {
		if ( JSON_NOW == resource ) {
			synchronized ( dataNowJSON) {
				return dataNowJSON.toString();
			}
		} else if ( JSON_RECENT_DATA == resource ) {
			synchronized ( dataRecent) {
				return dataRecent.toRecentJSON();				
			}
		} else if ( JSON_HISTORY_FILES == resource ) {
			synchronized ( historyDayLogFilesJSON ) {
				loadHistoryFromFiles();
				return "{\"history_files\": {" + historyDayLogFilesJSON + "}}";
			}
		} else if ( JSON_HISTORY_BY_DAY == resource ) {
			if ( historyStatsByDayReady ) {
				return dailySummaryJSON();
			} else {
				return "invalid";
			}
		} else if ( JSON_DAY_STATS == resource ) {
			synchronized ( dataRecent) {
				return dataRecent.toDayStatsJSON();				
			}			
		} else if ( JSON_HOST_INFO == resource ) {
			return HostInfo.toJSON();
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
			dataRecent.startPoint(now);

			/* get today's syncSumData from summaryStatsFromHistory if ready */
			Map<String, SynchronizedSummaryData> today;
			date = new Date();


			/* last (ie current) data JSON */
			Iterator<Entry<String, SynchronizedSummaryData>> it = data.entrySet().iterator();
			//TODO remove
			int countx = 0;
			
			while (it.hasNext()) {
				Map.Entry<String, SynchronizedSummaryData> pairs = (Map.Entry<String, SynchronizedSummaryData>)it.next();
				String channel = pairs.getKey();
				//TODO null pointer exception happens
				System.out.println(countx+"= "+channel);
				countx++;
				if ( null != pairs.getValue() ) {
					dataNow.put(channel,new DataPoint(channel,now,pairs.getValue()));
				}
				

				if ( null != dataRecent ) {
					dataRecent.addChannel(channel, pairs.getValue().getValueSampleOrAverage());
				}

				/* add data to dayStats */

				if ( dayStats.containsKey( channel ) ) {

					Double s = pairs.getValue().getMean();
					Double d;
					try {
						d=new Double(s);
						dayStats.get( channel ).addValue( d );
					} catch ( NumberFormatException e ) {
						System.err.println("# error ingesting s=" + s + " as a double. Giving up");
						return;
					}

				}

				if ( historyStatsByDayReady ) {
					String todayDateKey = sdfYYYYMMDD.format( date );

					if ( ! historyStatsByDay.containsKey( todayDateKey ) ) {
						/* we don't have today in history ... initialize today */
						historyStatsByDay.put( todayDateKey,  createNewSummaryData());

					}

					today = historyStatsByDay.get( sdfYYYYMMDD.format( date ) );


					if ( today.containsKey( channel ) ) {
						if ( today.get(channel).mode == Modes.AVERAGE ) {
							Double s = pairs.getValue().getMean();
							Double d;
							try {
								d=new Double(s);
								today.get(channel).addValue(d);
							} catch ( NumberFormatException e ) {
								System.err.println("# error ingesting s=" + s + " as a double. Giving up");
								return;
							}
						} else if ( today.get(channel).mode == Modes.SAMPLE ) {
							Double s = pairs.getValue().sampleValue;
							today.get(channel).addValue(s);	
						}
					}



				}
			}


			/* loglocal */
			if ( null != logLocal) {
				/* String array [0] is the line to be written and [1] is the headers if the headers are required  */
				String[] lineAndHeader = HistoryPointExport.toCSV( data, channelDesc );
				/* log line to local file */
				logLocal.log( lineAndHeader[0], new Date(now), lineAndHeader[1] );
			}

			/* clear statistics for next pass */
			data.clear();
		}
		dataRecent.endPoint();


		synchronized ( dataNowJSON ) {
			dataNowJSON=new StringBuilder("{\"data\": [");

			Iterator<Entry<String, DataPoint>> it = dataNow.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, DataPoint> pairs = (Map.Entry<String, DataPoint>)it.next();


				/* use the DataPoint.toJSON() method to encode the members we care about into JSON */
				dataNowJSON.append(pairs.getValue().toJSON());

				if ( it.hasNext() ) 
					dataNowJSON.append(", ");



				/* insert into MySQL */
				//				String table = "adc_" + pairs.getKey();
				//				DataPoint a = pairs.getValue();
				//				String sql = String.format("INSERT INTO %s VALUES(now(), %d, %f, %f, %f, %f)",
				//						table,
				//						a.n,
				//						a.avg,
				//						a.min,
				//						a.max,
				//						a.stddev
				//						);
				//
				//				log.queryAutoCreate(sql, "dataGSProto.analogDoubleSummarized", table);

			}
			dataNowJSON.append("]}");
		}



	}


	/**
	 * 
	 * @return a fully instantiated hash map containing SynchronizedSummaryData by channel
	 */
	protected HashMap<String, SynchronizedSummaryData> createNewSummaryData(){
		/* initialize SynchronizedSummaryData */
		HashMap<String, SynchronizedSummaryData> ssd = new HashMap<String, SynchronizedSummaryData>();

		/* iterate through channelDesc */
		Iterator<Entry<String, ChannelDescription>> it = channelDesc.entrySet().iterator();
		while ( it.hasNext() ) {
			Map.Entry<String, ChannelDescription> pairs = (Map.Entry<String, ChannelDescription>)it.next();

			/* instantiate the SynchronizedSummaryData for that channel */
			if ( pairs.getValue().historyByDay ) {
				ssd.put( pairs.getValue().id, new SynchronizedSummaryData(pairs.getValue().mode) );
			}
		}
		/* return instantiated  SynchronizedSummaryData hashmap*/
		return ssd;
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
	 * This method returns a hashmap of the summary of the file
	 * @param fileAbsolutePath The path to the files that need to be opened and summarized
	 * @return HashMap with the channel name as the key and SynchronizedSummaryData as the value
	 */
	public HashMap<String, SynchronizedSummaryData> loadHistoryDayFromFile(String fileAbsolutePath){
		/* hashmap to be returned */
		Map<String, SynchronizedSummaryData> thisFileStats = new HashMap<String, SynchronizedSummaryData>();

		System.err.println("----->getSyncSumDatEntry working on " + fileAbsolutePath);



		/* iterate through channelDesc, finding every channel that contains the key summaryStatsFromHistory */

		Iterator<Entry<String, ChannelDescription>> it = channelDesc.entrySet().iterator();
		while ( it.hasNext() ) {
			Map.Entry<String, ChannelDescription> pairs = (Map.Entry<String, ChannelDescription>)it.next();

			/* instantiate the SynchronizedSummaryData for that channel */
			if ( pairs.getValue().historyByDay ) {
				thisFileStats.put( pairs.getValue().id, new SynchronizedSummaryData(pairs.getValue().mode) );
			}
		}


		File csvDataFile = new File(fileAbsolutePath);
		CSVParser parser = null;
		try {
			parser = CSVParser.parse(csvDataFile, Charset.defaultCharset(), CSVFormat.DEFAULT);
		} catch ( IOException e ) {
			System.err.println("# IOException while working on: " + fileAbsolutePath);
			return null;
		}


		/* fieldsToParse is an array of fields we are interested in parsing, terminated by -1 */
		int fieldsToParse[]=null;
		String[] headerTokens=null;

		for (CSVRecord csvRecord : parser) {
			/* first record is header */
			if ( null == headerTokens ) {
				headerTokens = new String[csvRecord.size()];
				fieldsToParse = new int[csvRecord.size()+1];
				int j=0;

				for ( int i=0 ; i<csvRecord.size() ; i++ ) {
					headerTokens[i]=csvRecord.get(i);
					headerTokens[i]=headerTokens[i].replace( " ", "" );
					headerTokens[i]=headerTokens[i].replace( "\"", "" );


					/* add to a list of fields we are interested in parsing */
					if ( thisFileStats.containsKey(headerTokens[i]) ) {
						fieldsToParse[j]=i;
						fieldsToParse[j+1]=-1;
						j++;
					}
				}
				
				//System.err.println("-------------------> headerTokens");
				for ( int i=0 ; i<headerTokens.length ; i++ ) {
					System.err.println("# headerTokens[" + i + "] is " + headerTokens[i] );
				}

				//				for ( int i=0 ; i<fieldsToParse.length && fieldsToParse[i] != -1 ; i++ ) {
				//					System.out.printf("fieldsToParse[%d]=%d (key for that field is headerTokens[%d]='%s')\n",
				//							i,
				//							fieldsToParse[i],
				//							fieldsToParse[i],
				//							headerTokens[fieldsToParse[i]]
				//					);
				//				}


				continue;
			}

			/* data record */
			for ( int i=0 ; i<fieldsToParse.length && fieldsToParse[i] != -1 ; i++ ) {
				Double d=0.0;

				/* skip lines that don't have enough columns */
				if ( fieldsToParse[i] >= csvRecord.size() )
					break;

				try {
					/* get rid of anything besides numbers and decimal point */
					String v=csvRecord.get(fieldsToParse[i]);
					
					/* skip parsing if we have null or empty string */
					if ( null == v || 0 == v.length() || !NumberUtils.isNumber(v) )
						continue;

					d=Double.parseDouble(v);
				} catch ( Exception e ) {
					System.err.println("# Exception on field " + fieldsToParse[i] + ": " + csvRecord.get(fieldsToParse[i]));
					e.printStackTrace();
				}

				thisFileStats.get(headerTokens[fieldsToParse[i]]).addValue(d);
			}
		}


		/* debug dump of what we gathered for the day */
		//		for (Map.Entry<String, SynchronizedSummaryData> entry : thisFileStats.entrySet()) {
		//		    String key = entry.getKey();
		//		    SynchronizedSummaryData value = entry.getValue();
		//		    System.err.println("# key=" + key + " min: " + value.getMin() + " max: " + value.getMax() + " mean:" + value.getMean());
		//		}


		return (HashMap<String, SynchronizedSummaryData>) thisFileStats;
	}

	protected void loadHistoryFromFiles() {
		/* get array of all filenames from logLocalDir */
		String[] files = new UtilFiles().listFilesForFolder( logLocalDir );

		if ( null == files || 0==files.length ) {
			historyDayLogFilesJSON="\"files\":[]";
			return;
		} 

		historyDayLogFilesJSON="\"files\":["+filesToJson( files )+"]";

		System.err.println("# " + files.length + " files listed for historyFiles.json");
		System.err.flush();




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
		if ( ! processAllData && (! channelDesc.containsKey(ch) || (! channelDesc.get(ch).log && ! channelDesc.get(ch).dayStats)) ) {
			return;
		}



		/* if data hashtable doesn't have the key for this channel, we add it
		 * the mode (sampled or averaged) is read from channel description or defaults to sample  */
		if ( ! data.containsKey(ch) ) {
			//			System.err.println("# Putting value " + s + " to data (" + ch + " as SynchronizedSummaryData with mode " + channelDesc.get(ch).mode + ")");

			if ( channelDesc.containsKey(ch) ) {
				data.put(ch, new SynchronizedSummaryData( channelDesc.get(ch).mode ) );
			} else {
				data.put(ch, new SynchronizedSummaryData( Modes.SAMPLE ) );
			}
		}


		/* actually add the value to data structure */
		Double d;
		try {
			d=new Double(s);
			data.get(ch).addValue(d);
		} catch ( NumberFormatException e ) {
			System.err.println("# error ingesting s=" + s + " as a double. Giving up");
			return;
		}




	}


	protected void loadChannelMapFile(String channelMapFile) {
		long startTime = System.currentTimeMillis();
		System.err.println("# channel map file is " + channelMapFile);
		File cmf = new File(channelMapFile);


		if ( cmf.exists() && ! cmf.isDirectory() ) {

			System.err.println("# Loading channel description from " + channelMapFile + " ...");
			System.err.flush();

			/* used for deserializing json */
			Gson gson = new GsonBuilder().create();
			ChannelDescription cd;

			/* get string array of json objects to deserialize  */
			String[] jsonStrArray = UtilFiles.getJsonFromFile(channelMapFile);
			System.err.println(	"# " + (System.currentTimeMillis()-startTime) + " ms to read file ... ");

			/* iterate through jsonStrArray and create a ChannelDescription object 
			 * and add it to the hash map */
			for ( int i = 0; i<jsonStrArray.length; i++ ) {
				cd = gson.fromJson( jsonStrArray[i], ChannelDescription.class );
				channelDesc.put( cd.id, cd );
			}
			System.err.println("# " + channelDesc.size() + " channels loaded in " + 
					(System.currentTimeMillis()-startTime) + " ms total.");
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
		String channelMapFile="channelDescriptions/channels.json";
		int dataHistoryJSONHours=24;
		String documentRootName="www/";

		logLocalDir=null;
		processAllData=false;
		intervalSummary = 1000;
		data = new HashMap<String, SynchronizedSummaryData>();
		dataNow = new HashMap<String, DataPoint>();
		dataNowJSON=new StringBuilder("");


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
		options.addOption("b", "http-document-root", true, "webserver document root directory");
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
			if ( line.hasOption("http-document-root") ) {
				documentRootName=line.getOptionValue("http-document-root");
			}
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


		dataRecent=null;
		if ( dataHistoryJSONHours > 0 ) {
			int nPoints=(dataHistoryJSONHours*60*60)/(intervalSummary/1000);

			System.err.printf("# Enabling history JSON for %d hours (%d data points at %d millisecond interval rate)\n",
					dataHistoryJSONHours,
					nPoints,
					intervalSummary);
			dataRecent = new DataRecent(nPoints,channelDesc);

			/* create hashmap to contain 24 hour min max average stats by day */
			dayStats = new HashMap<String, DescriptiveStatistics>();

			instantiateDayStats( nPoints );


		} else {
			System.err.println("# History JSON disabled");
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


		/* attempt to load history from files if logLocalDir is set */
		if ( null != logLocalDir ) {
			System.err.println("# Loading History from LogLocal directory " + logLocalDir );
			System.err.flush();
			loadHistoryFromFiles();

			System.err.println("# Starting thread to read logLocal files and summarize for history.json");
			/* Create summary in another thread */
			(new Thread(new summaryHistoryThread())).start();
		}






		/* timer to periodically handle the data */
		dataTimer = new javax.swing.Timer(intervalSummary, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dataMaintenanceTimer();
			}
		});
		dataTimer.start();






		/* built in http server to provide data */
		if ( 0 != httpPort ) {

			documentRoot=new File(documentRootName);
			if ( ! documentRoot.exists() || ! documentRoot.isDirectory()  ) {
				System.err.println("# HTTP server document root is invalid: " + documentRootName);
				System.err.println("# HTTP server not starting");
			} else {
				System.err.println("# HTTP server listening on port " + httpPort + " with document root absolute path " + documentRoot.getAbsolutePath());
				System.err.flush();
				HTTPServerJSON httpd = new HTTPServerJSON(httpPort, this, channelMapFile, logLocalDir, documentRoot);
				httpd.start();
			}
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

	private void instantiateDayStats(int nPoints){

		/* iterate through channels */

		Iterator<Entry<String,ChannelDescription>> chIt = channelDesc.entrySet().iterator();
		DescriptiveStatistics ds;

		while ( chIt.hasNext() ){

			/* if history is true */
			ChannelDescription cd = (ChannelDescription) chIt.next().getValue();

			if ( cd.dayStats && cd.mode == ChannelDescription.Modes.AVERAGE ){

				/* create the SyncDescStat */
				ds = new SynchronizedDescriptiveStatistics();

				/* set the number of points to keep for summarizing */
				ds.setWindowSize( nPoints );

				/* put SyncDescStat in the hashmap with channel(id) as the key */
				dayStats.put( cd.id, ds );
			}



		}
	}

	public String dailySummaryJSON() {
		StringBuilder json = new StringBuilder();

		json.append("{\"summary_stats\": [");

		/*
		 {
    		"day": 20130503,
    		"n": "2835.0",
    		"i_temp_battery_avg": "15.727689594356253",
    		"i_temp_battery_max": "18.0",
    		"i_temp_battery_min": "14.0",
    		"i_dc_volts_avg": "25.078483245149886",
    		"i_dc_volts_max": "26.0",
    		"i_dc_volts_min": "24.2",
    		"b_state_of_charge_min": "79.0",
    		"b_state_of_charge_max": "89.0",
    		"b_state_of_charge_avg": "82.01657848324525"
		},
		 */

		Iterator<Entry<String,HashMap<String, SynchronizedSummaryData>>> ite = historyStatsByDay.entrySet().iterator();

		while ( ite.hasNext() ) {
			Entry<String,HashMap<String, SynchronizedSummaryData>> entry = ite.next();

			json.append("{");
			json.append( UtilJSON.putString("day", entry.getKey()) + "," );


			boolean firstEntry=true;
			/* iterator for this day */
			Iterator<Entry<String, SynchronizedSummaryData>> itd = entry.getValue().entrySet().iterator();
			while ( itd.hasNext() ) {
				Entry<String, SynchronizedSummaryData> day = itd.next();

				if ( firstEntry ) {
					/* we get our "n" for the day from the first entry in the map */
					json.append("\"n\": " + day.getValue().getN() + ",");
					firstEntry=false;
				}

				//	        	int precision = channelDesc.get(entry.getKey()).precision;

				json.append( UtilJSON.putDouble(day.getKey() + "_min", day.getValue().getMin()) + ",");
				json.append( UtilJSON.putDouble(day.getKey() + "_max", day.getValue().getMax()) + ",");
				json.append( UtilJSON.putDouble(day.getKey() + "_avg", day.getValue().getMean()) );

				/* add the last comma only if we have something else coming */
				if ( itd.hasNext() ) {
					json.append(",\n");
				}
			}

			json.append("}");
			json.append("\n");

			/* add the last comma only if we have something else coming */
			if ( ite.hasNext() ) {
				json.append(",\n");
			}
		}

		json.append("]}");

		return json.toString();
	}




	/* Main method */
	public static void main(String[] args) throws IOException {
		System.err.println("# Major version: 2014-11-21 (precision)");
		System.err.println("# java.library.path: " + System.getProperty( "java.library.path" ));

		DataGS d=new DataGS();
		d.run(args);
	}


	/* thread that runs the method to create the summary stats json */
	/* TODO: get rid of inner class ... doesn't meet APRS World's coding standards */
	public class summaryHistoryThread implements Runnable {
		/**
		 * 
		 * @param files is the String array of files that will be summarized
		 */
		public void createSummaryStatsFromHistory(String[] files){

			/* initialize summaryStatsFromHistory hashmap NOTE: this may need to move somewhere else*/
			historyStatsByDay = new HashMap<String, HashMap<String, SynchronizedSummaryData>>();

			String date;
			for ( int i = 0 ; i < files.length ; i++ ) {
				date=FilenameUtils.getBaseName( files[i] );
				historyStatsByDay.put( date, loadHistoryDayFromFile( files[i] ) );
			}
		}

		public void run() {
			long startTime = System.currentTimeMillis();

			String[] files = new UtilFiles().listFilesForFolder(logLocalDir, true);

			createSummaryStatsFromHistory( files );

			System.err.println("# SummaryHistoryThread completed summarizing logLocalFiles in "+((System.currentTimeMillis()-startTime)/1000)+" seconds");
			historyStatsByDayReady = true;
		}

	}


}
