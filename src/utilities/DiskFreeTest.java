package utilities;


import java.net.URI;
import java.nio.file.*;
import java.text.NumberFormat;

public class DiskFreeTest {
	public static void main(String args[]) {
		NumberFormat nf = NumberFormat.getNumberInstance();
		
//		Path root = FileSystems.getDefault().getPath("www/foo.bar");

		for (Path root : FileSystems.getDefault().getRootDirectories()) {
			System.out.print(root + ": ");

			try	{
				FileStore store = Files.getFileStore(root);
				
				long available = store.getUsableSpace();
				long total = store.getTotalSpace();
				
				long availableMegabytes = available / 1024;
				long totalMegabytes = total / 1024;
				double percentFree = ( (double) availableMegabytes / (double) totalMegabytes) * 100.0;
				
				System.out.println("readyOnly=" + store.isReadOnly() + " available=" + availableMegabytes + " total=" + totalMegabytes + " % free=" + percentFree );
				
			
				
				
//				System.out.println("available=" + nf.format(store.getUsableSpace()) + ", total=" + nf.format(store.getTotalSpace()));
			} catch (Exception e)	{
				System.out.println("error querying space: " + e.toString());
			}
		}

	}
}
