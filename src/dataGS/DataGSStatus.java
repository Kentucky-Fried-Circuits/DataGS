package dataGS;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Socket;
import java.util.Vector;

public class DataGSStatus extends Thread {
	private Log db;
	private int port;
	private javax.swing.Timer timer;
	private Vector<Socket> connections;
	public final boolean debug=false;

	public DataGSStatus(Log m,int p) {
		super("DataGSStatusThread");
		db=m;
		port=p;
		connections = new Vector<Socket>();
	}

	public void updateStatus() {

		if ( debug ) {
			String sql = "REPLACE INTO worldDataCollectorStatus (status_date,port) VALUES(now(), " + port + ")";
			db.queryAutoCreate(sql, "worldDataProto.worldDataCollectorStatus", "worldDataCollectorStatus");

			sql = "INSERT INTO worldDataCollectorStatusHistory (status_date,port) VALUES(now(), " + port + ")";
			db.queryAutoCreate(sql, "worldDataProto.worldDataCollectorStatusHistory", "worldDataCollectorStatusHistory");
		}

		/* no need to dump active connections if there are none */
		if ( 0 == connections.size() ) {
			return;
		}

		Socket[] toDelete = new Socket[connections.size()+1];
		toDelete[0]=null;
		int toDeleteIndex=0;

		System.err.println("# Active connections over last minute: ");
		for ( int i=0 ; i<connections.size() ; i++ ) {
			Socket s = connections.elementAt(i);

			System.err.printf("#\t[%d] %s:%d",
					i,
					s.getInetAddress().getHostAddress(),
					s.getPort()
					);

			if ( s.isConnected() ) {
				System.err.print(" CONNECTED");
			} 
			if ( s.isBound() ){ 
				System.err.print(" BOUND");
			}
			if ( s.isInputShutdown() ){ 
				System.err.print(" INPUTSHUTDOWN");
			}
			if ( s.isOutputShutdown() ){ 
				System.err.print(" OUTPUTSHUTDOWN");
			}
			if ( s.isClosed() ) { 
				System.err.print(" CLOSED");
				toDelete[toDeleteIndex++]=s;
				toDelete[toDeleteIndex]=null;
			}
			System.err.println("");
		}

		/* delete closed connections */
		for ( int i=0 ; i<toDelete.length ; i++ ) {
			if ( null == toDelete[i] )
				break;

			connections.remove(toDelete[i]);
		}

	}

	public void addConnection(Socket s) {
		connections.add(s);
	}


	public void run() {
		/* add a status record every 60 seconds */
		timer = new javax.swing.Timer(60000, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateStatus();
			}
		});

		if ( ! timer.isRunning() ) {
			timer.start();
		}

	}
}
