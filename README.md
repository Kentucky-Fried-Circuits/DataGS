# DataGS

## Summary
Gather data from TCP/IP (simple ASCII format) or serial port (WorldData format) and process and make available

## Data served via HTTP

### favicon.ico
Little icon for web browser to display. Loaded from `www/favicon.ico`

### json.html
Simple page with links to the different files. Read from file system.

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

## Starting the software

### Examples
```
java -Djava.rmi.server.hostname=192.168.10.201 -Dcom.sun.management.jmxremote 
-Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.local.only=false 
-Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false 
-DSERIAL_PORT_LIST=/dev/ttyUSB1 
-cp .:bin:commons-cli-1.2.jar:commons-lang3-3.3.2.jar:commons-math3-3.3.jar:gson-2.3.jar:mysql-connector-java-5.1.7-bin.jar:commons-collections4-4.0.jar:jspComm.jar:Serialio.jar 
dataGS.DataGS -j 8080 -l 4010 -i 10000 -c www/channels_magWebPro.json -r /dev/ttyUSB1 -R 57600 -a
```

### Enable remote profile via JMXREMOTE
``` 
-Djava.rmi.server.hostname=192.168.10.201
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9010
-Dcom.sun.management.jmxremote.local.only=false 
-Dcom.sun.management.jmxremote.authenticate=false 
-Dcom.sun.management.jmxremote.ssl=false
```
Replace `192.168.10.201` with the IP of your public interface. Replace `9010` with a unique local port.

Use software such as VisualVM (from Oracle) for monitoring

### Specifying available serial ports under non-Windows operating systems

The serialio.com library we use for accessing serial ports doesn't have support for auto-detecting serial ports
under most operating systems. But we can tell the Java VM what serial ports are available. 

For example:

```
-DSERIAL_PORT_LIST=/dev/ttyUSB0
```

Multiple ports can be seperated with a colon. Example:

```
-DSERIAL_PORT_LIST=/dev/ttyUSB0:/dev/ttyUSB1
```

### Setting the classpath

Java needs to have the current directory, the bin directory, and the name of all the requires JAR files in the `-cp` 
argument. The program itself is then started with `packageName.className`. Or `dataGS.DataGS`.
