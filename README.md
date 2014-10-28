# DataGS

## Summary
Gather data from TCP/IP (simple ASCII format) or serial port (WorldData format) and process and make available

## Data served via HTTP

### favicon.ico
Little icon for web browser to display. Loaded from `www/favicon.ico`

### history/YYYYMMDD.csv or history/YYYYMMDD.txt 
Logged history file from file system. Return as MIME type `test/csv` if the URI ends with `.csv` or as 
`text/plain` if the URI ends with `.txt`. 
History files are stored in the log local directory which is set with the -w argument

### channels.json
Channel description map as loaded from filesystem. File system location is set with the -c argument.
Returned as MIME type `application/json`.

### now.json
Interval statistics or sample of the last batch of data processed. Data is process at interval
specified by the -i argument.

### history.json or history.dat
Time series data covering from now to the number of hours specified by the -H argument.

Returned as `application/json` if URI ends with `.json` or as `text/plain` if the URI ends with `.dat`

### historyFiles.json or historyFiles.dat
Listing of the log files available in the log local directory.

Returned as `application/json` if URI ends with `.json` or as `text/plain` if the URI ends with `.dat`

### summaryStats.json or summaryStats.dat
Daily statistics that summarize the values of all of the files in the log local directory. 
Statistics are generated on all of the columns that have `history` set to true in the channel description map.

Computing the results is done at startup and then continually updated. If the results aren't yet available, 
will return an HTTP response of `NO CONTENT` (HTTP result code 204).

Returned as `application/json` if URI ends with `.json` or as `text/plain` if the URI ends with `.dat`


## Command line arguments

### MySQL related arguments
```
options.addOption("d", "database", true, "MySQL database");
options.addOption("h", "host", true, "MySQL host");
options.addOption("p", "password", true, "MySQL password");
options.addOption("u", "user", true, "MySQL username");
```

### SQLite3 related arguments
```
options.addOption("s", "SQLite-URL",true,"SQLite URL (e.g. DataGS.db");
options.addOption("S", "SQLite-proto-URL",true,"SQLite prototype URL (e.g. DataGSProto.db");
```

### DataGSCollector related arguments
```
options.addOption("i", "interval", true, "Interval to summarize over (milliseconds)");
options.addOption("l", "listen-port", true, "DataGSCollector Listening Port");
options.addOption("t", "socket-timeout",true, "DataGSCollector connection socket timeout");
options.addOption("c", "channel-map", true, "Location of channel map JSON file");
options.addOption("a", "process-all-data",false,"Process all data, even if it isn't in channel map");
```

### Serial port data source arguments 
```
options.addOption("r", "serialPortWorldData",true,"Serial Port to listen for worldData packets");
options.addOption("R", "serialPortWorldDataSpeed",true,"Serial port speed");
```

### Data output (JSON) arguments
```
options.addOption("j", "http-port", true, "webserver port, 0 to disable");
options.addOption("H", "json-history-hours", true, "hours of history data to make available, 0 to disable");
```

### Local Logging arguments 
```
options.addOption("w", "loglocal-directory", true, "directory for logging csv files");
```