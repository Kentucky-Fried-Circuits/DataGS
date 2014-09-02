package dataGS;

//import java.util.Map;


public class HTTPServerJSON extends NanoHTTPD {
    public static final String MIME_JAVASCRIPT = "text/javascript";
    public static final String MIME_CSS = "text/css";
    public static final String MIME_JPEG = "image/jpeg";
    public static final String MIME_PNG = "image/png";
    public static final String MIME_SVG = "image/svg+xml";
    public static final String MIME_JSON = "application/json";
	
    protected lastDataJSON lastData;
	
	public HTTPServerJSON(int port, lastDataJSON s) {
		super(port);

		lastData=s;
	}

	@Override public Response serve(IHTTPSession session) {
		Method method = session.getMethod();
		String uri = session.getUri();
		String mime_type = NanoHTTPD.MIME_PLAINTEXT;
		System.out.println(method + " '" + uri + "' ");

		/*
		String msg = "<html><body><h1>Hello server</h1>\n";
		Map<String, String> parms = session.getParms();

		if (parms.get("username") == null)
			msg +=
			"<form action='?' method='get'>\n" +
					"  <p>Your name: <input type='text' name='username'></p>\n" +
					"</form>\n";
		else
			msg += "<p>Hello, " + parms.get("username") + "!</p>";

		msg += "</body></html>\n";
		*/
		
		Response response = new NanoHTTPD.Response( Response.Status.OK,MIME_JSON,"{\"data\": [" + lastData.getLastDataJSON() + "]}");
		response.addHeader("Access-Control-Allow-Origin", "http://192.168.30.238");
		
		return response;
		
//		return new NanoHTTPD.Response( Response.Status.OK,MIME_JSON,lastData.getLastDataJSON());
		//return new NanoHTTPD.Response( Response.Status.OK,mime_type,msg);
		//return new NanoHTTPD.Response(msg);
	}


}
