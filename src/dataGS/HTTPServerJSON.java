package dataGS;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class HTTPServerJSON extends NanoHTTPD {

	public static final boolean DEBUG = false;

	public static final String MIME_JAVASCRIPT = "text/javascript";
	public static final String MIME_CSS = "text/css";
	public static final String MIME_JPEG = "image/jpeg";
	public static final String MIME_GIF = "image/gif";
	public static final String MIME_PNG = "image/png";
	public static final String MIME_SVG = "image/svg+xml";
	public static final String MIME_JSON = "application/json";
	public static final String MIME_CSV = "text/csv";
	public static final String MIME_PLAIN = "text/plain";

	protected JSONData data;
	protected String channelMapFile;
	protected String logLocalDirectory;

	public HTTPServerJSON(int port, JSONData s, String c, String logLocalDirectory) {
		super( port );
		data = s;
		channelMapFile = c;
		this.logLocalDirectory = logLocalDirectory;
	}

	@Override
	public Response serve(IHTTPSession session) {

		Method method = session.getMethod();
		String uri = session.getUri();

		boolean gzipAllowed = false;

		if ( DEBUG ) {
			System.out.println( method + " '" + uri + "' " );
			System.err.println( "parms: " + session.getHeaders() );
		}

		/* checks the headers from the client to see if encoding in gzip is allowed */
		if ( session.getHeaders().containsKey( "accept-encoding" ) ) {
			if ( session.getHeaders().get( "accept-encoding" ).contains( "gzip" ) ) {
				gzipAllowed = true;
			}
		}


		
		Response response = null;
		
		/* choose which document to return */
		/* file system */
		if ( uri.endsWith( "favicon.ico" ) ) {
			try {
				response = new NanoHTTPD.Response( Response.Status.OK, MIME_GIF,
						new FileInputStream( "www/favicon.ico" ) );
			} catch ( FileNotFoundException e ) {
				response = new NanoHTTPD.Response( Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found" );
			}
		} else if ( uri.startsWith( "/history/") && uri.endsWith (".csv") ) {
			/* logged history file from filesystem. Return as MIME_CSV */
			/* only if the stuff between /history/ and .csv is numeric */
			
			if( FilenameUtils.getBaseName( uri ) != null &&
					/* If all chars of basename are a number */
					NumberUtils.isNumber( FilenameUtils.getBaseName( uri ) ) ){
		
				try {

					response = new NanoHTTPD.Response( Response.Status.OK, MIME_CSV,
							new FileInputStream( logLocalDirectory+"/" + FilenameUtils.getBaseName( uri )+".csv" ) );
				} catch ( FileNotFoundException e ) {
					response = new NanoHTTPD.Response( Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found" );
				}	
			} else {
				response = new NanoHTTPD.Response( Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found" );
			}

		} else if ( uri.startsWith( "/history/") && uri.endsWith (".txt") ) {
			/* logged history file from filesystem. Return as MIME_PLAIN */
			/* only if the stuff between /history/ and .csv is numeric */
			if( FilenameUtils.getBaseName( uri ) != null &&
					/* If all chars of basename are a number */
					NumberUtils.isNumber( FilenameUtils.getBaseName( uri ) )  ){

				try {

					response = new NanoHTTPD.Response( Response.Status.OK, MIME_PLAIN,
							new FileInputStream( logLocalDirectory+"/" + FilenameUtils.getBaseName( uri )+".csv" ) );
				} catch ( FileNotFoundException e ) {
					response = new NanoHTTPD.Response( Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found" );
				}	
			} else {
				response = new NanoHTTPD.Response( Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found" );
			}

		} else if ( uri.endsWith( "channels.json" ) ) {
			/* channel description file from filesystem */
			try {
				response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, new FileInputStream( channelMapFile ) );
			} catch ( FileNotFoundException e ) {
				response = new NanoHTTPD.Response( Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found" );
			}
		} 
		
		/* dynamically generated */
		else if ( uri.endsWith( "live.json" ) ) {
			/* absolutely latest samples */
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_LIVE ), gzipAllowed );
		} else if ( uri.endsWith( "now.json" ) ) {
			/* interval averaged or sampled */
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_NOW ), gzipAllowed );
		} else if ( uri.endsWith( "history.json" ) ) {
			/* time series data */
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_HISTORY ), gzipAllowed );
		} else if ( uri.endsWith( "historyFiles.json" ) ) {
			/* listing of log files from filesystem */
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_HISTORY_FILES ), gzipAllowed );
		} else if ( uri.endsWith( "summaryStats.json" ) ) {
			
			if ( data.getJSON( DataGS.JSON_SUMMARY_STATS ).equals( "invalid" ) ) {
				response = new NanoHTTPD.Response( Response.Status.NO_CONTENT, MIME_PLAINTEXT, "Not Found" );
			}else{
				response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_SUMMARY_STATS ), gzipAllowed );
			}
		} 
		
		
		
		/* dynamically generated for internet explorer */
		else if ( uri.endsWith( "live.dat" ) ) {
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_PLAINTEXT, data.getJSON( DataGS.JSON_LIVE ), gzipAllowed );
		} else if ( uri.endsWith( "now.dat" ) ) {
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_PLAINTEXT, data.getJSON( DataGS.JSON_NOW ), gzipAllowed );
		} else if ( uri.endsWith( "history.dat" ) ) {
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_PLAINTEXT, data.getJSON( DataGS.JSON_HISTORY ), gzipAllowed );
		} else if ( uri.endsWith( "historyFiles.dat" ) ) {
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_PLAINTEXT, data.getJSON( DataGS.JSON_HISTORY_FILES ), gzipAllowed );
		} else if ( uri.endsWith( "summaryStats.dat" ) ) {
			if ( data.getJSON( DataGS.JSON_SUMMARY_STATS ).equals( "invalid" ) ) {
				response = new NanoHTTPD.Response( Response.Status.NO_CONTENT, MIME_PLAINTEXT, "Not Found" );
			}else{
				response = new NanoHTTPD.Response( Response.Status.OK, MIME_PLAINTEXT, data.getJSON( DataGS.JSON_SUMMARY_STATS ), gzipAllowed );
			}
		} 
		
		/* not found */
		else {
			response = new NanoHTTPD.Response( Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found" );
		}
		

		/*
		 * this allows the website with the AJAX page to be on a different
		 * server than us
		 */
		response.addHeader( "Access-Control-Allow-Origin", session.getHeaders().get( "origin" ) );

		return response;
	}
	


}
