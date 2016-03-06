package dataGS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
	public static final String MIME_WAV = "audio/x-wav";
	public static final String MIME_MPEG = "audio/mpeg";
	public static final String MIME_HTML = "text/html";

	protected JSONData data;
	protected String channelMapFile;
	protected String logLocalDirectory;
	protected File documentRoot;
	protected ConfigData config;

	public HTTPServerJSON(int port, JSONData s, String c, String logLocalDirectory, File  w, ConfigData config) {
		super( port );
		data = s;
		channelMapFile = c;
		this.logLocalDirectory = logLocalDirectory;
		documentRoot=w;
		this.config = config;
	}


	public Response serveFromFilesystem(String uri) {
		if ( uri.contains("..") ) {
			return new NanoHTTPD.Response( Response.Status.FORBIDDEN, MIME_PLAINTEXT, ".. in URI not permitted" );
		}

		/* try index.html if we have a trailing slash */
		if ( uri.endsWith("/") ) {
			uri = uri + "index.html";
		}
		
		File file = new File(documentRoot,uri);
	
		
	//	System.err.println("# Checking if " + file.getAbsoluteFile() + " exists and is not a directory");
		
	
		
		if ( ! file.exists() || file.isDirectory() ) {
			return new NanoHTTPD.Response( Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found" );
		}

		String luri = uri.toLowerCase();
		String mime = "";
		if ( luri.endsWith("js") )
			mime=MIME_JAVASCRIPT;
		else if ( luri.endsWith("css") ) 
			mime=MIME_CSS;
		else if ( luri.endsWith("jpg") )
			mime=MIME_JPEG;
		else if ( luri.endsWith("gif") )
			mime=MIME_GIF;
		else if ( luri.endsWith("png") )
			mime=MIME_PNG;
		else if ( luri.endsWith("svg") )
			mime=MIME_SVG;
		else if ( luri.endsWith("json") )
			mime=MIME_JSON;
		else if ( luri.endsWith("csv") )
			mime=MIME_CSV;
		else if ( luri.endsWith("wav") )
			mime=MIME_WAV;
		else if ( luri.endsWith("mp3") )
			mime=MIME_MPEG;
		else if ( luri.endsWith("html") )
			mime=MIME_HTML;
		else
			mime=MIME_PLAIN;

		try {
			return new NanoHTTPD.Response( Response.Status.OK, mime,new FileInputStream(file) );
		} catch ( FileNotFoundException e ) {
			e.printStackTrace();
		}
		
		return new NanoHTTPD.Response( Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found" );
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
		} else if ( uri.endsWith( "/data/json.html" ) ) {
			try {
				response = new NanoHTTPD.Response( Response.Status.OK, MIME_HTML,
						new FileInputStream( "www/json.html" ) );
			} catch ( FileNotFoundException e ) {
				response = new NanoHTTPD.Response( Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found" );
			}			
		} else if ( uri.startsWith( "/data/history/") && uri.endsWith (".csv") ) {
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

		} else if ( uri.startsWith( "/data/history/") && uri.endsWith (".txt") ) {
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

		} else if ( uri.endsWith( "/data/channels.json" ) ) {
			/* channel description file from filesystem */
			try {
				response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, new FileInputStream( channelMapFile ) );
			} catch ( FileNotFoundException e ) {
				response = new NanoHTTPD.Response( Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found" );
			}
		} 

		/* configuration */
		else if ( uri.endsWith("/configuration.json") ) {
			if (method == Method.POST) {
				try {
					Integer contentLength = Integer.parseInt(session.getHeaders().get("content-length"));
					byte[] buffer = new byte[contentLength];
					session.getInputStream().read(buffer, 0, contentLength);
					
					String json_string = new String(buffer, Charset.forName("UTF-8"));
					JsonParser parser = new JsonParser();
					JsonObject jsonObject = parser.parse(json_string).getAsJsonObject();
					
					Set<Entry<String, JsonElement>> set = jsonObject.entrySet();
					boolean error = false;
					for (Entry<String, JsonElement> entry : set) {
						if (!config.setValue(entry.getKey(), entry.getValue().getAsString())) {
							error = true;
						}
					}
					Response.Status status = Response.Status.OK;
					if (error) {
						status = Response.Status.FORBIDDEN;
					}
					JsonObject json = config.getJSON();
					if (json == null) {
						response = new NanoHTTPD.Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "", gzipAllowed);
					} else {
						response = new NanoHTTPD.Response( status, MIME_PLAINTEXT, config.getJSON().toString(), gzipAllowed );
					}
				} catch (Exception e) {
					response = new NanoHTTPD.Response( Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "", gzipAllowed );
				}
			} else {
				JsonObject json = config.getJSON();
				if (json == null) {
					response = new NanoHTTPD.Response(Response.Status.OK, MIME_PLAINTEXT, "{}", gzipAllowed);
				} else {
					response = new NanoHTTPD.Response(Response.Status.OK, MIME_PLAINTEXT, json.toString(), gzipAllowed );
				}
			}
		}
		
		/* dynamically generated */
		else if ( uri.endsWith( "/data/now.json" ) ) {
			/* interval averaged or sampled */
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_NOW ), gzipAllowed );
		} else if ( uri.endsWith( "/data/recent.json" ) ) {
			/* time series data */
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_RECENT_DATA ), gzipAllowed );
		} else if ( uri.endsWith( "/data/historyFiles.json" ) ) {
			/* listing of log files from filesystem */
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_HISTORY_FILES ), gzipAllowed );
		} else if ( uri.endsWith( "/data/historyByDay.json" ) ) {
			/* daily summaries from local log files */
			if ( data.getJSON( DataGS.JSON_HISTORY_BY_DAY ).equals( "invalid" ) ) {
				response = new NanoHTTPD.Response( Response.Status.NO_CONTENT, MIME_PLAINTEXT, "Not Found" );
			}else{
				response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_HISTORY_BY_DAY ), gzipAllowed );
			}
		} else if ( uri.endsWith( "/data/dayStats.json" ) ) {
			/* summarized 24 hour data */
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_DAY_STATS ), gzipAllowed );
		} else if ( uri.endsWith( "/data/hostinfo.json" ) ) {
			/* meta info */
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_HOST_INFO ), gzipAllowed );
		}



		/* dynamically generated for internet explorer */
		else if ( uri.endsWith( "/data/now.dat" ) ) {
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_PLAINTEXT, data.getJSON( DataGS.JSON_NOW ), gzipAllowed );
		} else if ( uri.endsWith( "/data/recent.dat" ) ) {
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_PLAINTEXT, data.getJSON( DataGS.JSON_RECENT_DATA ), gzipAllowed );
		} else if ( uri.endsWith( "/data/historyFiles.dat" ) ) {
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_PLAINTEXT, data.getJSON( DataGS.JSON_HISTORY_FILES ), gzipAllowed );
		} else if ( uri.endsWith( "/data/historyByDay.dat" ) ) {
			if ( data.getJSON( DataGS.JSON_HISTORY_BY_DAY ).equals( "invalid" ) ) {
				response = new NanoHTTPD.Response( Response.Status.NO_CONTENT, MIME_PLAINTEXT, "Not Found" );
			}else{
				response = new NanoHTTPD.Response( Response.Status.OK, MIME_PLAINTEXT, data.getJSON( DataGS.JSON_HISTORY_BY_DAY ), gzipAllowed );
			}
		} else if ( uri.endsWith( "/data/dayStats.dat" ) ) {
			/* summarized 24 hour data */
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_PLAINTEXT, data.getJSON( DataGS.JSON_DAY_STATS ), gzipAllowed );
		} else if ( uri.endsWith( "/data/hostinfo.dat" ) ) {
			/* meta info */
			response = new NanoHTTPD.Response( Response.Status.OK, MIME_PLAINTEXT, data.getJSON( DataGS.JSON_HOST_INFO ), gzipAllowed );
		}
		/* serve from filesystem */
		else {
			return serveFromFilesystem(uri);
		}

		//		/* not found */
		//		else {
		//			response = new NanoHTTPD.Response( Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found" );
		//		}


		/*
		 * this allows the website with the AJAX page to be on a different
		 * server than us
		 */
		response.addHeader( "Access-Control-Allow-Origin", session.getHeaders().get( "origin" ) );

		return response;
	}



}
