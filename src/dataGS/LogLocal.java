package dataGS;
import java.io.*;
import java.text.*;
import java.util.*;


public class LogLocal { 
	protected String directory;
	protected DateFormat dayFormat;
	protected DateFormat dateFormat;
	protected boolean autoTimestamp;

	/** constructor
	 * @param String directory to write log files to
	 * @param boolean make the timestamp automatically
	 */
	public LogLocal(String d, boolean autoTimestamp) {
		directory=d;
		dayFormat = new SimpleDateFormat("yyyyMMdd");
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,S");
		this.autoTimestamp=autoTimestamp;
	}

	/* write a line to a file named yyyyMMdd in directory
	 * @param line String to write to file
	 * @param timeStamp Time of record
	 * @return true if write was sucessfull, false otherwise
	 */
	public boolean log(String line, Date timeStamp) {
		String filename= directory + "/" + dayFormat.format(timeStamp) + ".CSV";

		
		try { 
			/* check to see if the directory exists, otherwise try to create it */
			File f=new File(directory);
			if ( false == f.isDirectory() && false == f.mkdirs() ) {
				/* unable to create directory */
				System.err.println("# LogLocal error creating directory: " + directory);
				return false;
			}

			/* Open the file and append to it */
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename,true));

			if ( autoTimestamp || null == timeStamp ) {
				line = dateFormat.format(timeStamp) + "," + line;
			}
			
			/* write line */
			writer.write(line + "\r\n");
			System.err.println("# LogLocal: " + filename + " - " + line); 

			/* close the file */
			writer.close();
		} catch ( Exception e ) {
			System.err.println("# LogLocal exception: " + e);
			return false;
		}

		return true;
	}

	/* write a line to a file named yyyyMMdd in directory
	 * @param line String to write to file
	 * @return true if write was sucessfull, false otherwise
	 */
	public boolean log(String line) {
		/* default to now */
		return log(line,new Date());
	}

}
