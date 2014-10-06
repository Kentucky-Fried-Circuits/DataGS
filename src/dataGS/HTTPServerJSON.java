package dataGS;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class HTTPServerJSON extends NanoHTTPD {

    public static final boolean DEBUG = false;

    public static final String MIME_JAVASCRIPT = "text/javascript";
    public static final String MIME_CSS = "text/css";
    public static final String MIME_JPEG = "image/jpeg";
    public static final String MIME_GIF = "image/gif";
    public static final String MIME_PNG = "image/png";
    public static final String MIME_SVG = "image/svg+xml";
    public static final String MIME_JSON = "application/json";

    protected JSONData data;

    public HTTPServerJSON(int port, JSONData s) {

	super( port );

	data = s;

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
	} else if ( uri.endsWith( "channels.json" ) ) {
	    try {
		response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, new FileInputStream(
			"www/channels.json" ) );
	    } catch ( FileNotFoundException e ) {
		response = new NanoHTTPD.Response( Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found" );
	    }
	}
	/* dynamically generated JSON */
	else if ( uri.endsWith( "now.json" ) ) {
	    response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_NOW ), gzipAllowed );
	} else if ( uri.endsWith( "history.json" ) ) {
	    response = new NanoHTTPD.Response( Response.Status.OK, MIME_JSON, data.getJSON( DataGS.JSON_HISTORY ), gzipAllowed );
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
