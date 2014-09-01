#!/usr/local/bin/php -q
<?
$argv=$_SERVER["argv"];

$dtTable[1]="wnc_basic_";
$dtTable[100]="xrw2g_basic_";
$dtTable[32767]="rsTest_";

$db=mysql_connect("localhost","root","roadtoad");
mysql_select_db("worldData",$db);

$sql=sprintf("SELECT * FROM rsTapData_%s ORDER BY packet_date",$argv[1]);
$query=mysql_query($sql,$db);


$last=null;
$mnt=-1;
$mnc=-1;

$sf=array();

while ( $r=mysql_fetch_array($query,MYSQL_ASSOC) ) {
	if ( $r['measurementNumber'] != $last['measurementNumber'] ) {
		/* mark what time our measurement series started at */
		$mnt=$r['packet_date'];
		$mnc=1;
	} else {
		if ( $r['packet_date'] != $mnt ) {
			/* our date is not the same as the first packet_date associated with this measurement number */

			if ( "" != $argv[2] ) {
				$delta=strtotime($r['packet_date'])-strtotime($mnt);
				printf("%s, but series started at %s (%d in series) (delta from base %d)\n",$r['packet_date'],$mnt,$mnc,$delta);
			} else {
				/* now find our table and FIX IT! */
				$sf[]=sprintf("UPDATE %s%s_%s SET packet_date='%s' WHERE packet_date='%s' AND measurementNumber=%d;",
					$dtTable[$r['deviceTypeWorld']],
					$argv[1],
					$r['deviceSerialNumber'],
					$mnt,
					$r['packet_date'],
					$r['measurementNumber']
					);
		
				$sf[]=sprintf("UPDATE rsTapData_%s SET packet_date='%s' WHERE packet_date='%s' AND measurementNumber=%d AND deviceSerialNumber=%d;",
					$argv[1],
					$mnt,
					$r['packet_date'],
					$r['measurementNumber'],
					$r['deviceSerialNumber']
					);
			}

			$mnc++;
		}
	}

	$last=$r;
}

/*
mysql> describe rsTapData_A2654;
+--------------------+----------------------+------+-----+---------+-------+
| Field              | Type                 | Null | Key | Default | Extra |
+--------------------+----------------------+------+-----+---------+-------+
| packet_date        | datetime             | NO   |     | NULL    |       |
| measurementNumber  | int(10) unsigned     | NO   | MUL | NULL    |       |
| deviceTypeWorld    | smallint(5) unsigned | NO   |     | NULL    |       |
| deviceSerialNumber | int(10) unsigned     | NO   |     | NULL    |       |
| deviceException    | smallint(5) unsigned | NO   |     | NULL    |       |
+--------------------+----------------------+------+-----+---------+-------+
*/


$sfn=count($sf);
for ( $i=0 ; $i<$sfn ; $i++ ) {
	printf("%s\n",$sf[$i]);
}


mysql_close($db);

?>
