<?
define("DB_HOST","localhost");
define("DB_USER","root");
define("DB_PASSWORD","roadtoad");

function _open_mysql($database_name) {
        /* If we want a database connection then we'll make one here */
        $db = @mysql_connect(DB_HOST,DB_USER,DB_PASSWORD);
        @mysql_select_db($database_name,$db);

        return $db;
}
?>
