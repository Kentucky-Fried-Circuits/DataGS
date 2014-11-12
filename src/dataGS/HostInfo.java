package dataGS;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;

import org.apache.commons.lang3.StringEscapeUtils;

public class HostInfo {
	
	
//	public String hostname;
	
//	public StorageInfo[] stores;
	
	
	public void update() {
		System.out.println("# store, total, used, available, readyOnly, name, type");

		for (FileStore store : FileSystems.getDefault().getFileStores()) {
			try { 
				long total = store.getTotalSpace() / 1024;
				long used = (store.getTotalSpace() - store.getUnallocatedSpace()) / 1024;
				long avail = store.getUsableSpace() / 1024;
				System.out.format("%-30s %12d %12d %12d %b %s %s%n", store.toString(), total, used, avail,store.isReadOnly(),store.name(),store.type());
				
				if ( store.toString().startsWith("/media/usb0") ) {
					System.out.println("# this is our USB drive");
				}
				
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}	
	}
	
	public static String toJSON() {
		StringBuilder sb = new StringBuilder();
		
		String hostname="unknown";
		try {
			hostname=InetAddress.getLocalHost().getHostName();
		} catch ( UnknownHostException e ) {
			System.err.println("# UnknownHostException while trying to determine local hostname");
		}

		sb.append("{");
		sb.append( UtilJSON.putString("hostname", hostname) + "," );
		

		/* drive space and status */
		sb.append( "\"drives\": [");
		
		for (FileStore store : FileSystems.getDefault().getFileStores()) {
			try {
				sb.append( "{");
				
				long total = store.getTotalSpace() / 1024;
				long used = (store.getTotalSpace() - store.getUnallocatedSpace()) / 1024;
				long avail = store.getUsableSpace() / 1024;
				
				sb.append( UtilJSON.putLong("total", total) + "," );
				sb.append( UtilJSON.putLong("used", used) + "," );
				sb.append( UtilJSON.putLong("avail", avail) + "," );
				sb.append( UtilJSON.putBoolean("readOnly",store.isReadOnly()) + "," );
				sb.append( UtilJSON.putString("name", store.name()) + "," );
				sb.append( UtilJSON.putString("type", store.type()) + "," );
				sb.append( UtilJSON.putString("description", store.toString())  );
				
				sb.append("},");
								
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}
		
		/* delete last comma ... which could be from hostname or from a filesystem */
		if ( ',' == sb.charAt(sb.length()-1) ) {
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append("]"); /* close drives */
		
		sb.append("}");
		
		
		
		return sb.toString();
	}

}
