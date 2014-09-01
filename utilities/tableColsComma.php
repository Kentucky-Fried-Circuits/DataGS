#!/usr/local/bin/php -q
<?
require "db_config.php";

$db=_open_mysql("worldDataProto");

$table=$_SERVER["argv"][1]; //"wnc_basic_A2651_30403";
$sql="describe " . $table;

$query=mysql_query($sql,$db);

$cols=array();
while ( $r=mysql_fetch_array($query) ) {
        $cols[]=$r["Field"];
}


printf("INSERT INTO %s (%s)\n VALUES(%s)",$table,implode(', ',$cols),implode(', ',$cols));
?>

