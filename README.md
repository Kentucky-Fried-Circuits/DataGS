# DataGS

## Summary
Gather data from TCP/IP (simple ASCII format) or serial port (WorldData format) and process and make available

## Command line arguments

### MySQL related arguments
`
		options.addOption("d", "database", true, "MySQL database");
		options.addOption("h", "host", true, "MySQL host");
		options.addOption("p", "password", true, "MySQL password");
		options.addOption("u", "user", true, "MySQL username");
`

### SQLite3 related arguments
`
		options.addOption("s", "SQLite-URL",true,"SQLite URL (e.g. DataGS.db");
		options.addOption("S", "SQLite-proto-URL",true,"SQLite prototype URL (e.g. DataGSProto.db");
`

### DataGSCollector related arguments
`
		options.addOption("i", "interval", true, "Interval to summarize over (milliseconds)");
		options.addOption("l", "listen-port", true, "DataGSCollector Listening Port");
		options.addOption("t", "socket-timeout",true, "DataGSCollector connection socket timeout");
		options.addOption("c", "channel-map", true, "Location of channel map JSON file");
		options.addOption("a", "process-all-data",false,"Process all data, even if it isn't in channel map");
`

### Serial port data source arguments 
`
		options.addOption("r", "serialPortWorldData",true,"Serial Port to listen for worldData packets");
		options.addOption("R", "serialPortWorldDataSpeed",true,"Serial port speed");
`

### Data output (JSON) arguments
`
		options.addOption("j", "http-port", true, "webserver port, 0 to disable");
		options.addOption("H", "json-history-hours", true, "hours of history data to make available, 0 to disable");
`

### Local Logging arguments 
`
		options.addOption("w", "loglocal-directory", true, "directory for logging csv files");
`