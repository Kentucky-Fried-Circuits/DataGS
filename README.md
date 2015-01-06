# DataGS

## Summary
Gather data from TCP/IP (simple ASCII format) or serial port (WorldData format) and process and make available

## Channel Description File Format
<a name="chanDescFileFormat"></a>
The channel description file is in JSON format. It specifies channel names, descriptions, and other attributes for
a channel.

Example file with one element:
```
{
	"data": [
        {
            "id": "age_inverter",
            "title": "Inverter Age (255 indicates old data)",
            "description": "Inverter Age (255 indicates old data)",
            "units": "none",
            "precision": 2,
            "sortOrder": 240,
            "dayStats": "false",
            "log": "true",
            "historyByDay": "false",
            "recent": "false",
            "mode": "SAMPLE"
        }
	]
}
```

* ```"id"``` is the channel id or name.
* ```"title"``` is used as the column name in the historical data table
* ```"description"``` is used as the column name in the log file
* ```"units"``` is the metric in which the channel's data is measured e.i. MPH, kWh, or %
* ```"precision"``` is the number of digits the data is displayed with. Positive numbers indicate how many decimal points to round to and negative numbers indicates what digit to round to e.i.
| number | precision | result |
| 12.2345 | 0 | 12 |
| 12.2345 | 2 | 12.23 |
| 12.2345 | -1 | 10 |

* ```"sortOrder"``` is the order in which the data appears in the log file. The channel with the lowest number is first, then the next lowest number is second, etc...
* ```"dayStats"```indicates if the channel's data will be included in the dayStats.json file.
* ```"log"``` indicates if this channel will have its data saved in log files.
* ```"historyByDay"``` indicates if the channel's data will be included in the historyByDay.json file.
* ```"recent"``` indicates if the channel's data will be included in the recent.json file.
* ```"mode"``` can be  ```"SAMPLE"``` or ```"AVERAGE"``` and indicates how the channel's data will be presented in the now.json file.


Note that channel id's will be single character letters for data received via TCP/IP / ASCII. Longer channel
names are possible for data that comes in via WorldDataProcessor via reflection.



## Data served via HTTP

### /data/json.html
Simple page with links to the different files. Read from file system. For information on parsing JSON in multiple programming and scripting languages, visit [JSON.org](http://www.json.org/)

### /data/history/YYYYMMDD.csv or /data/history/YYYYMMDD.txt 
Logged history file from file system. Return as MIME type `test/csv` if the URI ends with `.csv` or as 
`text/plain` if the URI ends with `.txt`. 
History files are stored in the log local directory which is set with the -w argument

### /data/channels.json or /data/channels.dat
Channel description map as loaded from filesystem. File system location is set with the -c argument.

For example file, see [Channel Description File Format](#chanDescFileFormat).

Returned as `application/json` if URI ends with `.json` or as `text/plain` if the URI ends with `.dat`

### /data/now.json or /data/now.dat
Interval statistics or sample of the last batch of data processed. Data is process at interval
specified by the -i argument.
The two modes, ```"SAMPLE"``` and ```"AVERAGE"```, change how the data for the channel is presented. 

Example file with both modes present:
```
{
	data: [
		{
			channel: "r_parallel_threshold",
			time: 1419258385610,
			sampleValue: "6.0",
			mode: "SAMPLE"
		},
		{
			channel: "i_ac_volts_out",
			time: 1419258385610,
			n: 10,
			avg: 118.3,
			min: 117,
			max: 119,
			stddev: 0.8232726023485638,
			mode: "AVERAGE"
		}
	]
}
```

If the channel is using the mode ```"SAMPLE"```, the channel data is obtained through ```"sampleValue"```.
If the channel is using the mode ```"AVERAGE"```, the channel data is split up between five values: 

* ```"n"``` is the number of data points used to compute these values.
* ```"avg"``` is the average of the data points recieved within the given interval.
* ```"min"``` is the minimum value of the data points recieved within the given interval.
* ```"max"``` is the maximum value of the data points recieved within the given interval.
* ```"stddev"``` is the standard deviation of the data points recieved within the given interval.


Both modes contain ```"time"``` which is a unix timestamp representation of when that data was generated.

Returned as `application/json` if URI ends with `.json` or as `text/plain` if the URI ends with `.dat`

### /data/recent.json or /data/recent.dat
Time series data covering from now to the number of hours specified by the -H argument.

Example file :
```
{
	recent: [
		{
			time: 1419174506269,
			data: {
				b_amph_in_out: 22,
				calc_add_power: 0,
				b_dc_watts: 0,
				b_state_of_charge: 100
			}
		}
	]
}
```
*CAUTION:* This file will contain every data point recorded for the specified channels from the last X amount of hours meaning this has the potential to use up a lot of memory. For example, Let's say we have an interval ( argument ```i``` ) of 10000 milliseconds ( 10 seconds ) and json-history-hours ( argument ```H``` ) of 24 hours. For every channel that contains ```"recent": "true"``` in this scenario will add 8640 data points to the file. Make sure to be mindful of your device's specs when configuring these settings. If you are looking to create a graph spanning a period of time, use this. However, if you just want 24 hour summary data, consider using dayStats instead.

* ```"time"``` is a unix timestamp representation of when that data was generated.
* ```"data"``` is an object containing key-value pairs of channels that contain ```"recent": "true"``` in it's channel description.

Returned as `application/json` if URI ends with `.json` or as `text/plain` if the URI ends with `.dat`

### /data/historyFiles.json or /data/historyFiles.dat
Listing of the log files available in the log local directory.

Example file :
```
{
	history_files: {
		files: [
			"20141222.csv",
			"20141221.csv",
			"20141220.csv",
			"20141219.csv"
		]
	}
}
```

Returned as `application/json` if URI ends with `.json` or as `text/plain` if the URI ends with `.dat`

### /data/historyByDay.json or /data/historyByDay.dat
Daily statistics that summarize the values of all of the files in the log local directory. 
Statistics are generated on all of the columns that have `history` set to true in the channel description map.

Example file:
```
{
	summary_stats: [
		{
			day: "20141220",
			n: 8638,
			gen_power_min: 0,
			gen_power_max: 0,
			gen_power_avg: 0,
			b_state_of_charge_min: 100,
			b_state_of_charge_max: 100,
			b_state_of_charge_avg: 100,
			calc_add_power_min: -9.152,
			calc_add_power_max: 44.7228,
			calc_add_power_avg: 0.23513608345553358,
			i_dc_power_min: 0,
			i_dc_power_max: 38.08,
			i_dc_power_avg: 0.05220331352422104,
			b_dc_power_min: -9.152,
			b_dc_power_max: 62.71640000000001,
			b_dc_power_avg: 0.2874055799953692,
			i_dc_volts_min: 12.69,
			i_dc_volts_max: 13.670000000000002,
			i_dc_volts_avg: 13.537876694708153
		}
	]
}
```

* ```"day"``` YYYYMMDD representation of the date
* ```"n"``` is the number of data points used to compute these values.
* ```"avg"``` is the average of the data points recieved on that day.
* ```"min"``` is the minimum value of the data points recieved on that day.
* ```"max"``` is the maximum value of the data points recieved on that day.

Computing the results is done at startup and then continually updated. If the results aren't yet available, 
will return an HTTP response of `NO CONTENT` (HTTP result code 204).


Returned as `application/json` if URI ends with `.json` or as `text/plain` if the URI ends with `.dat`

### /data/dayStats.json or /data/dayStats.dat

The summarised data from the last 24 hours.
Statistics are generated on all of the columns that have `dayStats` set to true in the channel description map.

Example file:
```
{
	dayStats: {
		i_ac_volts_out: {
			n: 8640,
			min: 117.1,
			max: 121.6,
			avg: 119.18382330246877
		}
	}
}
```

* ```"n"``` is the number of data points used to compute these values.
* ```"avg"``` is the average of the data points recieved within 24 hours.
* ```"min"``` is the minimum value of the data points recieved within 24 hours.
* ```"max"``` is the maximum value of the data points recieved within 24 hours.


Returned as `application/json` if URI ends with `.json` or as `text/plain` if the URI ends with `.dat`

### /data/hostinfo.json or /data/hostinfo.dat

Hostname, firmware date, and the drives of the server.

Example file:
```
{
	hostname: "A3432",
	firmware_date: "2014-11-19",
	drives: [
		{
			total: 3023728,
			used: 1872024,
			avail: 978392,
			readOnly: false,
			name: "rootfs",
			type: "rootfs",
			description: "/ (rootfs)"
		},
		{
			total: 4418624,
			used: 57032,
			avail: 4345208,
			readOnly: false,
			name: "/dev/mmcblk0p3",
			type: "ext4",
			description: "/data (/dev/mmcblk0p3)"
		}
	]
}
```

* `total` the total amount of storage the drive has in kilobytes.
* `used` the amount of used storage the drive has in kilobyes.
* `avail` the amount of available storage the drive has in kilobyes.
* `readOnly` if true, the drive is read-only. If false, drive is not read-only.
* `name` the name of the drive.
* `type` the file system or architecture of the drive.
* `description` the location of the file with a description.

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
options.addOption("b", "http-document-root", true, "webserver document root directory");
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
#!/bin/bash
cd /home/aprs/DataGS
# for remote profiling and debugging
java 
	-DSERIAL_PORT_LIST=/dev/ttyAMA0 
	-cp .:jars:bin:jars/commons-cli-1.2.jar:jars/commons-lang3-3.3.2.jar:jars/commons-math3-3.3.jar:jars/gson-2.3.jar:jars/mysql-connector-java-5.1.7-bin.jar:jars/commons-collections4-4.0.jar:jars/jspComm.jar:jars/Serialio.jar:jars/json-lib-2.4-jdk15.jar:jars/commons-io-2.4.jar:jars/commons-csv-1.0.jar 
	dataGS.DataGS -j 8080 -l 4010 -i 10000 -c channelDescriptions/channels_magWebPro.json 
	-r /dev/ttyAMA0 -R 57600 -a -w /data/logLocal
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
