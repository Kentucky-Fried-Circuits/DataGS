package dataGS;
import java.sql.*;

public class LogSQLite implements Log {
	protected Connection con;
	protected Statement stmt;

	protected String databaseURL, databaseProtoURL;

	public void connect() throws Exception {
		Class.forName("org.sqlite.JDBC");
		con = DriverManager.getConnection(databaseURL);

		stmt = con.createStatement();
		stmt.setQueryTimeout(30);
	}

	public void close() {
		try {
			con.close();
		} catch ( Exception e ) {
			System.err.println(e);
		}
	}

	protected void missingTable(String sql, String proto, String table) {
		/* create the time like the prototype table in worldDataProto */
		String sqlNewTable="CREATE TABLE " + table + " LIKE " + proto;
		query(sqlNewTable);
		System.err.println("# creating table from prototype using: " + sqlNewTable);
		/* try the query again */
		query(sql);
	}

	public void queryAutoCreate(String s, String proto, String table) {
		if ( null == stmt )
			return;
		
			
		/* replace MySQL functions with SQLite functions */
		s=s.replace("now()", "DATETIME()");
		s=s.replace("NOW()", "DATETIME()");
		s=s.replace("SEC_TO_TIME(", "(");
		s=s.replace("sec_to_time(", "(");
		
		//System.err.println("# LogSQLite queryAutoCreate: " + s);
		
		try {
			stmt.executeUpdate(s);
		
		} catch ( SQLException e ) {
			if ( null != e) {
				System.err.println(e);
				System.err.println("# SQL that triggered exception: " + s);
			}
		}
	}
	
	public void queryAutoCreateFromString(String s, String sqlToCreate, String table) {
		if ( null == stmt )
			return;
		
		/* first check if lock on table is free */
		String sql="SELECT IS_FREE_LOCK('" + table + "')";
		try {
			ResultSet rs = stmt.executeQuery(sql);
			rs.first();
			if ( 1 != rs.getInt(1) ) {
				System.err.println("# queryAutoCreate found that table " + table + " is locked. Will skip performing query.");
				System.err.println("# SQL that would have been executed is: " + s);
				return;
			}
		} catch ( Exception e ) {
			System.err.println("# queryAutoCreate got exeception while checking if lock for " + table + " was free.");
			System.err.println(e);
			return;
		}
		
		

		try {
			stmt.executeUpdate(s);
			//		} catch ( MySQLSyntaxErrorException e ) {
		} catch ( SQLException e ) {
			if ( 0==e.getSQLState().compareTo("42S02") ) {
				/* this table appears not to exist. */
				
				/* create a new table from our passed in SQL */
				query(sqlToCreate);
				System.err.println("# creating table: " + sqlToCreate);
				/* try the query again */
				query(sql);
				
			}

			if ( null != e) {
				System.err.println(e);
				System.err.println("# SQL that triggered exception: " + s);
			}
		}
	}

	public void insertDigiAPI(String table, String proto, int port, int source, int rssi, int options, String data) {
		return; 
	}


	public void query(String s) {
		if ( null == stmt )
			return;

		System.out.print("# LogSQLite executing : " + s);
		try {
			stmt.executeUpdate(s);
		} catch ( Exception e ) {

			if ( null != e)
				System.err.println(e);
			System.err.println("# SQL that triggered exception: " + s);
		}
	}

	public LogSQLite (String dbp, String db) {
		databaseURL="jdbc:sqlite:" + db;
		databaseProtoURL="jdbc:sqlite:" + dbp;
		


		System.err.println("# JDBC URL=" + databaseURL + " prototype database=" + databaseProtoURL);
	}
}
