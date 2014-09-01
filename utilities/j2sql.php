#!/usr/local/bin/php -q
<?
$argv=$_SERVER['argv'];

$fp=fopen($argv[1],"r");
while ( $l=fgets($fp,1024) ) {
	$l=trim($l);

	if ( "/*" == substr($l,0,2) ) {
		echo $l . "\n";
		continue;
	} 

	if ( strlen($l) < 3 )
		continue;

	$l=str_replace(';','',$l);
	$parts=explode(' ',$l);
	$vars=substr($l,strlen($parts[0]));
	$vars=explode(',',$vars);

	$type=$parts[0];
	if ( "double" == $type )
		$myType="FLOAT";
	else if ( "int" == $type ) 
		$myType="SMALLINT";
	else 
		$myType=strtoupper($type);

//	print_r($vars);

	//printf("# type=%s variables={ ",$type);
	for ( $i=0 ; $i<count($vars) ; $i++ ) {
		$vars[$i]=trim($vars[$i]);
		$vars[$i]=str_replace(',','',$vars[$i]);
//		printf("%s\n",$vars[$i]);
		printf("\t'%s' %s NULL,\n",$vars[$i],$myType);
	}



}

/*
CREATE TABLE `worldDataProto`.`mTest` (
`packet_date` DATETIME NOT NULL ,
`tiny` TINYINT UNSIGNED NULL ,
`short` SMALLINT UNSIGNED NULL ,
`f` FLOAT NULL
) ENGINE = MYISAM 
*/



?>
