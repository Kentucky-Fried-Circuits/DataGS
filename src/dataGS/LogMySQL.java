package dataGS;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class LogMySQL implements Log {
	protected Connection con;
	protected Statement stmt;
	protected String url;

	protected String username,password;

	public void connect() throws Exception {
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		con = DriverManager.getConnection(url,username,password);

		stmt = con.createStatement();
	}

	public void close() {
		try {
			con.close();
		} catch ( Exception e ) {
			System.err.println(e);
		}
	}

	private void missingTable(String sql, String proto, String table) {
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
				/* this table appears not to exist. Go to our non-existant table error handler */
				missingTable(s,proto,table);
			}

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
		PreparedStatement stmt=null;


		//		System.out.print("# LogMySQL executing : " + s);
		try {
			stmt = con.prepareStatement("INSERT INTO " + table + " (packet_date, port, source, rssi, options, data) VALUES (now(), ?, ?, ?, ?, ?)");

			if ( null == stmt )
				return;

			stmt.setString(1,new Integer(port).toString());
			stmt.setString(2,new Integer(source).toString());
			stmt.setString(3,new Integer(rssi).toString());
			stmt.setString(4,new Integer(options).toString());
			stmt.setString(5,data);

			stmt.executeUpdate();

		} catch ( SQLException e ) {
			if ( 0==e.getSQLState().compareTo("42S02") ) {
				/* this table appears not to exist. Go to our non-existant table error handler */
			//	missingTable(s,proto,table);
			}

			if ( null != e) {
				System.err.println(e);
				//System.err.println("# SQL that triggered exception: " + s);
			}

			//		} catch ( Exception e ) {
			//			
			//			if ( null != e)
			//				System.err.println(e);
			//			System.err.println("# SQL that triggered exception: " + s);
		}
	}


	public void query(String s) {
		if ( null == stmt )
			return;

		//		System.out.print("# LogMySQL executing : " + s);
		try {
			stmt.executeUpdate(s);
		} catch ( Exception e ) {

			if ( null != e)
				System.err.println(e);
			System.err.println("# SQL that triggered exception: " + s);
		}
	}

	public LogMySQL (String hostname, String username, String password, String database, int portNumber) {
		url="jdbc:mysql://" + hostname + ":" + portNumber + "/" + database;

		this.username=username;
		this.password=password;

		System.err.println("# JDBC URL=" + url);
		System.err.println("# username=" + username + " password=" + password);
	}
}
