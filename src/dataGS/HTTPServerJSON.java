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

		System.err.println("URL is: " + uri);
		
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
			// history returns text/plain ... historyCSV returns MIME_CSV
			

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
			// history returns text/plain ... historyCSV returns MIME_CSV
			

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
			try {
				response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, new FileInputStream( channelMapFile ) );
			} catch ( FileNotFoundException e ) {
				response = new NanoHTTPD.Response( Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found" );
			}
		}
		/* dynamically generated JSON */
		else if ( uri.endsWith( "live.json" ) ) {
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_LIVE ), gzipAllowed );
		} else if ( uri.endsWith( "now.json" ) ) {
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_NOW ), gzipAllowed );
		} else if ( uri.endsWith( "history.json" ) ) {
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_HISTORY ), gzipAllowed );
		} else if ( uri.endsWith( "historyFiles.json" ) ) {
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_HISTORY_FILES ), gzipAllowed );
		} else if ( uri.endsWith( "summaryStats.json" ) ) {
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_SUMMARY_STATS ), gzipAllowed );
		} 
		/* for internet explorer */
		else if ( uri.endsWith( "live.dat" ) ) {
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_PLAINTEXT, data.getJSON( DataGS.JSON_LIVE ), gzipAllowed );
		} else if ( uri.endsWith( "now.dat" ) ) {
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_PLAINTEXT, data.getJSON( DataGS.JSON_NOW ), gzipAllowed );
		} else if ( uri.endsWith( "history.dat" ) ) {
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_PLAINTEXT, data.getJSON( DataGS.JSON_HISTORY ), gzipAllowed );
		} else if ( uri.endsWith( "historyFiles.dat" ) ) {
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_PLAINTEXT, data.getJSON( DataGS.JSON_HISTORY_FILES ), gzipAllowed );
		} else if ( uri.endsWith( "summaryStats.dat" ) ) {
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_PLAINTEXT, data.getJSON( DataGS.JSON_SUMMARY_STATS ), gzipAllowed );
		} else {
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
