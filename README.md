# DataGS

## Contents

1. ###[Summary](#sum)

2. ###[Channel Description File Format](#chanDescFileFormat)

3. ###[Data served via HTTP](#dataServed)
   * [/data/json.html](#jsonhtml)
   * [/data/history/YYYYMMDD.csv or /data/history/YYYYMMDD.txt](#yyyyjson)
   * [/data/channels.json or /data/channels.dat](#channeljson)
   * [/data/now.json or /data/now.dat](#nowjson)
   * [/data/recent.json or /data/recent.dat](#recentjson)
   * [/data/historyFiles.json or /data/historyFiles.dat](#histfilejson)
   * [/data/historyByDay.json or /data/historyByDay.dat](#histdayjson)
   * [/data/dayStats.json or /data/dayStats.dat](#daystatjson)
   * [/data/hostinfo.json or /data/hostinfo.dat](#hostjson)

4. ###[Command line arguments](#cmdarg)
   * [DataGSCollector related arguments](#collectorarg)
   * [Serial port data source arguments](#serialarg)
   * [Data output (JSON) arguments](#outputarg)
   * [Local Logging arguments](#logarg)
 
5. ###[Starting the software](#startsoft)
   * [Examples](#startex)
   * [Enable remote profile via JMXREMOTE](#jmx)
   * [Specifying available serial ports under non-Windows operating systems](#serialnonwindows)
   * [Setting the classpath](#classpath)

6. ###[Website](#website)
   * [Current Conditions](#conditions)
   * [Current Settings](#settings)
   * [Historical Data](#historical)

<a name="sum"></a>
## Summary
Gather data from TCP/IP (simple ASCII format) or serial port (WorldData format) and process and make available

<a name="chanDescFileFormat"></a>
## Channel Description File Format
The channel description file is in JSON format. It specifies channel names, descriptions, and other attributes for
a channel.

Example file with one element:
```javascript
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

* ```"id"``` is the channel id or name. Data type: ```String```
* ```"title"``` is used as the column name in the historical data table. Data type: ```String```
* ```"description"``` is used as the column name in the log file. Data type: ```String```
* ```"units"``` is the metric in which the channel's data is measured.  Data type: ```String```
			* ex: MPH, kWh, or %
* ```"precision"``` is the number of digits the data is displayed with. Positive numbers indicate how many decimal points to round to and negative numbers indicates what digit to round to.  Data type: ```Integer```
	
ex:
| number | precision | result |
|---|---|---|
| 12.2345 | 0 | 12 |
| 12.5345 | 0 | 13 |
| 12.2345 | 2 | 12.23 |
| 12.2355 | 2 | 12.24 |
| 12.2345 | -1 | 10 |
| 15.2345 | -1 | 20 |
| 5.2345 | -3 | 0 |
	

* ```"sortOrder"``` is the order in which the data appears in the log file. The channel with the lowest number is first, then the next lowest number is second, etc... Data type: ```Integer```
* ```"dayStats"```indicates if the channel's data will be included in the [dayStats.json](#daystatjson) file. Data type: ```String```
* ```"log"``` indicates if this channel will have its data saved in log files. Data type: ```String```
* ```"historyByDay"``` indicates if the channel's data will be included in the [historyByDay.json](#histdayjson) file. Data type: ```String```
* ```"recent"``` indicates if the channel's data will be included in the [recent.json](#recentjson) file. Data type: ```String```
* ```"mode"``` can be  ```"SAMPLE"``` or ```"AVERAGE"``` and indicates how the channel's data will be presented in the [now.json](#nowjson) file. Data type: ```String```


Note that channel id's will be single character letters for data received via TCP/IP / ASCII. Longer channel
names are possible for data that comes in via WorldDataProcessor via reflection.


<a name="dataServed"></a>
## Data served via HTTP

<a name="jsonhtml"></a>
### /data/json.html
Simple page with links to the different files. Read from file system. For information on parsing JSON in multiple programming and scripting languages, visit [JSON.org](http://www.json.org/)

<a name="nowjson"></a>
### /data/now.json or /data/now.dat
Interval statistics or sample of the last batch of data processed. Data is process at interval
specified by the [`-i`](#collectorarg) argument.
The two modes, ```"SAMPLE"``` and ```"AVERAGE"```, change how the data for the channel is presented. 

Example file with both modes present:
```javascript
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
* ```"avg"``` is the average of the data points received within the given interval.
* ```"min"``` is the minimum value of the data points received within the given interval.
* ```"max"``` is the maximum value of the data points received within the given interval.
* ```"stddev"``` is the standard deviation of the data points received within the given interval.

Please note that ```"sampleValue"``` will always be represented in `string` format where as the data from ```"AVERAGE"``` is `numeric`.

Both modes contain ```"time"``` which is a UNIX timestamp representation of when that data was generated. For more information on the UNIX timestamp, visit http://www.unixtimestamp.com/

Returned as `application/json` if URI ends with `.json` or as `text/plain` if the URI ends with `.dat`

<a name="yyyyjson"></a>
### /data/history/YYYYMMDD.csv or /data/history/YYYYMMDD.txt 
Example file:
```javascript
"Data Date (UTC)","Milliseconds",b_dc_power,i_dc_power,calc_add_power,gen_power,load_power,b_dc_volts,b_dc_amps,i_ac_volts_in,b_amph_in_out,b_state_of_charge,i_dc_volts,i_dc_amps,i_ac_volts_out,i_amps_out,i_amps_in,i_ac_hz,i_status,i_fault,i_temp_transformer,i_temp_fet,i_temp_battery,a_gen_run_hours_since_boot,a_gen_runtime_minutes,age_inverter,a_temperature
"Data Date (UTC)","Milliseconds",DC Power (RMK),DC Power (Inverter),Calculated Renewable Power,AC In Power,AC Out Power,VDC (BMK),DC Amps (BMK),VAC In (Inverter),Ah In/Out (BMK),SOC (BMK),VDC (Inveter),DC Amps (Inverter),VAC Out (Inverter),AC Amps Out,AC Amps IN,AC Out Hz,Inverter Status,Fault Code,Transformer Temp (C ),FET Temp (C ),Battery Temp (C ),Total Generator Runtime (Since AGS Boot),Generator Runtime (Current Cycle),Inverter Age (255 indicates old data),AGS Temp (C )
2014-12-19 10:05:45,472,0,0,0,0,0,13.56,0.0,179.60,21.0,100,13.6,0,118,0.00,0.00,60.0,2.0,0.0,29.00,24.00,18.00,12.0,12.0,1.0,70.20
2014-12-19 10:05:55,473,0,0,0,0,0,13.57,0.0,179.30,21.0,100,13.6,0,118,0.00,0.00,60.1,2.0,0.0,29.00,24.00,18.00,12.0,12.0,1.0,70.20
```
This file uses the comma separated values (CSV) format. A column is created for every channel that has `log` set to true. The first line of the file uses the channel's `id` as the column header. The second line uses the channel's `title` as the column header. The third row and beyond are the values corresponding to their respective column headers.  Please note that `Data Date (UTC)"` and`"Milliseconds"` are the same on both the first and second line. This is because their values are generated when the log file is written to and are not values received from an outside source. 

CSV files can be opened in most spread sheet applications such as Microsoft Excel and LibreOffice Calc. For more information on the CSV format, visit http://en.wikipedia.org/wiki/Comma-separated_values

Logged history file from file system. Return as MIME type `test/csv` if the URI ends with `.csv` or as 
`text/plain` if the URI ends with `.txt`. 
History files are stored in the log local directory which is set with the [`-w`](#collectorarg) argument

<a name="channeljson"></a>
### /data/channels.json or /data/channels.dat
Channel description map as loaded from filesystem. File system location is set with the [`-c`](#collectorarg) argument.

For example file, see [Channel Description File Format](#chanDescFileFormat).

Returned as `application/json` if URI ends with `.json` or as `text/plain` if the URI ends with `.dat`

<a name="recentjson"></a>
### /data/recent.json or /data/recent.dat
Time series data covering from now to the number of hours specified by the [`-H`](#collectorarg) argument.

Example file :
```javascript
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
*CAUTION:* This file will contain every data point recorded for the specified channels from the last X amount of hours meaning this has the potential to use up a lot of memory. For example, Let's say we have an interval ( argument [```i```](#collectorarg) ) of 10000 milliseconds ( 10 seconds ) and json-history-hours ( argument [```H```](#collectorarg) ) of 24 hours. For every channel that contains ```"recent": "true"``` in this scenario will add 8640 data points to the file. Make sure to be mindful of your device's specs when configuring these settings. If you are looking to create a graph spanning a period of time, use this. However, if you just want 24 hour summary data, consider using dayStats instead.

* ```"time"``` is a UNIX timestamp representation of when that data was generated.
* ```"data"``` is an object containing key-value pairs of channels that contain ```"recent": "true"``` in it's channel description.

Returned as `application/json` if URI ends with `.json` or as `text/plain` if the URI ends with `.dat`

<a name="histfilejson"></a>
### /data/historyFiles.json or /data/historyFiles.dat
Listing of the log files available in the [log local directory](#collectorarg).

Example file :
```javascript
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

<a name="histdayjson"></a>
### /data/historyByDay.json or /data/historyByDay.dat
Daily statistics that summarize the values of all of the files in the log local directory. 
Statistics are generated on all of the channels that have [`history`](#channeljson) set to true in the channel description map.

Example file:
```javascript
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

* ```"day"``` YYYYMMDD representation of the date. Data type: `String`
* ```"n"``` is the number of data points used to compute these values. Data type: `Integer`
* ```"avg"``` is the average of the data points received on that day. Data type: `Float`
* ```"min"``` is the minimum value of the data points received on that day. Data type: `Float`
* ```"max"``` is the maximum value of the data points received on that day. Data type: `Float`

Computing the results is done at startup and then continually updated. If the results aren't yet available, 
will return an HTTP response of `NO CONTENT` (HTTP result code 204).


Returned as `application/json` if URI ends with `.json` or as `text/plain` if the URI ends with `.dat`

<a name="daystatjson"></a>
### /data/dayStats.json or /data/dayStats.dat
The summarized data covering from now to the number of hours specified by the [`-H`](#collectorarg) argument.
Statistics are generated on all of the channels that have `dayStats` set to true in the channel description map.

Example file:
```javascript
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

* ```"n"``` is the number of data points used to compute these values. Data type: `Integer`
* ```"avg"``` is the average of the data points received within 24 hours. Data type: `Float`
* ```"min"``` is the minimum value of the data points received within 24 hours. Data type: `Float`
* ```"max"``` is the maximum value of the data points received within 24 hours. Data type: `Float`


Returned as `application/json` if URI ends with `.json` or as `text/plain` if the URI ends with `.dat`

<a name="hostjson"></a>
### /data/hostinfo.json or /data/hostinfo.dat
Hostname, firmware date, and the drives of the server.

Example file:
```javascript
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
* `hostname` comes from the java.net method `InetAddress.getLocalHost().getHostName();`
* `firmware_date` is the most recent build date of DataGS.
* `drives`
	* `total` is the total amount of storage the drive has in kilobytes.
	* `used` is the amount of used storage the drive has in kilobytes.
	* `avail` is the amount of available storage the drive has in kilobytes.
	* `readOnly` if true, the drive is read-only. If false, drive is not read-only.
	* `name` is the name of the drive.
	* `type` is the file system or architecture of the drive.
	* `description` is the location of the file with a description.

Returned as `application/json` if URI ends with `.json` or as `text/plain` if the URI ends with `.dat`

<a name="cmdarg"></a>
## Command line arguments

<a name="collectorarg"></a>
### DataGSCollector related arguments
| Argument | Title | Require Argument | Description |
|---|---|---|---|
| i | interval | true | Interval to summarize over (milliseconds) |
| l | listen-port | true | DataGSCollector Listening Port |
| t | socket-timeout | true | DataGSCollector connection socket timeout |
| c | channel-map | true | Location of channel map JSON file |
| a | process-all-data | false | Process all data, even if it isn't in channel map |





<a name="serialarg"></a>
### Serial port data source arguments 
| Argument | Title | Require Argument | Description |
|---|---|---|---|
| r | serialPortWorldData | true | Serial Port to listen for worldData packets |
| R | serialPortWorldDataSpeed | true | Serial port speed |



<a name="outputarg"></a>
### Data output (JSON) arguments

| Argument | Title | Require Argument | Description |
|---|---|---|---|
| b | http-document-root | true | webserver document root directory |
| j | http-port | true | webserver port, 0 to disable |
| h | json-history-hours | true | hours of history data to make available, 0 to disable |


<a name="logarg"></a>
### Local Logging arguments 

| Argument | Title | Require Argument | Description |
|---|---|---|---|
| w | loglocal-directory | true | directory for logging csv files |

<a name="startsoft"></a>
## Starting the software

<a name="startex"></a>
### Examples
```
#!/bin/bash
cd /home/aprs/DataGS
java 
	-DSERIAL_PORT_LIST=/dev/ttyAMA0 
	-cp .:jars:bin:jars/commons-cli-1.2.jar:jars/commons-lang3-3.3.2.jar:jars/commons-math3-3.3.jar:jars/gson-2.3.jar:jars/mysql-connector-java-5.1.7-bin.jar:jars/commons-collections4-4.0.jar:jars/jspComm.jar:jars/Serialio.jar:jars/json-lib-2.4-jdk15.jar:jars/commons-io-2.4.jar:jars/commons-csv-1.0.jar 
	dataGS.DataGS -j 8080 -l 4010 -i 10000 -c channelDescriptions/channels_magWebPro.json 
	-r /dev/ttyAMA0 -R 57600 -a -w /data/logLocal
```

<a name="jmx"></a>
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

<a name="serialnonwindows"></a>
### Specifying available serial ports under non-Windows operating systems

The serialio.com library we use for accessing serial ports doesn't have support for auto-detecting serial ports
under most operating systems. But we can tell the Java VM what serial ports are available. 

For example:

```
-DSERIAL_PORT_LIST=/dev/ttyUSB0
```

Multiple ports can be separated with a colon. Example:

```
-DSERIAL_PORT_LIST=/dev/ttyUSB0:/dev/ttyUSB1
```

<a name="classpath"></a>
### Setting the classpath

Java needs to have the current directory, the bin directory, and the name of all the requires JAR files in the `-cp` 
argument. The program itself is then started with `packageName.className`. Or `dataGS.DataGS`.

<a name="website"></a>
## Website
DataGS has a built in web server. The website included in the www/ directory was created to display information received from a magnum inverter. The website includes pages for Current Conditions, Current Settings, and Historical Data. These web pages use Ajax to retrieve data from the json pages created by DataGS.

<a name="conditions"></a>
### Current Conditions
The Current Conditions page displays current data as well as summarized data from the last 24 hours. The data displayed includes data from the battery monitor (example: ME-BMK), Magnum inverter, and Auto Generator Start. Also included on the Current Conditions page is an alarm feature that warns the user of any faults that may occur with the inverter by playing an alarm sound and displaying an error message on the screen.

<a name="settings"></a>
### Current Settings
The Current Settings page displays the current settings of devices connected to the Magnum network, such as: inverter settings, remote settings, Automatic Generator Start settings, battery monitor settings.

<a name="historical"></a>
### Historical Data
The Historical Data page allows the user to view data by month. The user may select up to 6 months to view on the same page. The charts display the average, min, and max values over each month for the battery state of charge, renewable watts, battery watts, load watts, and generator watts. Underneath the charts is the same data represented in a table as well as links to the raw data in both text and CSV format.



