	function msieversion(){
		var ua = window.navigator.userAgent
		var msie = ua.indexOf ( "MSIE " )

		if ( msie > 0 )      // If Internet Explorer, return version number
			return parseInt (ua.substring (msie+5, ua.indexOf (".", msie )))
		else                 // If another browser, return 0
			return 0

	}


	/* some versions of internet explorer do not have access to this */
	if (!window.location.origin) {
		window.location.origin = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':' + window.location.port: '');
	}

	var root = window.location.origin + ":8080/data/";



	var urlHist = root+"recent.json";
	var urlNow = root+"now.json";
	var urlFiles = root+"historyFiles.json";
	var urlSummary = root+"historyByDay.json";
	var urlChannel = root+"channels.json";
	var urlLive = root+"live.json";
	var urlDay = root+"dayStats.json";
	var urlHostname = root+"hostinfo.json";

	if ( msieversion() ) {
		isIE = true;
		urlHist = root + "recent.dat";
		urlNow =  root + "now.dat";
		urlFiles =  root + "historyFiles.dat";
		urlSummary = root+"historyByDay.dat";
		//urlChannel = root+"channels.dat";
		urlLive = root+"live.dat";
		urlDay = root+"dayStats.dat";
		urlHostname = root+"hostinfo.dat";
	}
