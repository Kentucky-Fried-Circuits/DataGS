package dataGS;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringEscapeUtils;


public class LogLocal { 
	protected String directory;
	protected DateFormat dayFormat;
	protected DateFormat dateFormat;
	protected boolean autoTimestamp;
	protected String labelDate;
	protected String labelMilliseconds;

	final static String DEFAULT_DATE_LABEL="Data Date (UTC)";
	final static String DEFAULT_MILLISECONDS_LABEL="Milliseconds";
	
	
	/** constructor
	 * @param String directory to write log files to
	 * @param boolean make the timestamp automatically
	 */
	public LogLocal(String d, boolean autoTimestamp) {
		this(d, autoTimestamp,DEFAULT_DATE_LABEL,DEFAULT_MILLISECONDS_LABEL);
	}
	
	/** constructor
	 * @param String directory to write log files to
	 * @param boolean make the timestamp automatically
	 * @param label to use in the header for the date
	 * @param label to use in the header for milliseconds
	 */
	public LogLocal(String d, boolean autoTimestamp,String dateDefault, String millisecondsDefault) {
		directory=d;
		dayFormat = new SimpleDateFormat("yyyyMMdd");
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,S");
		this.autoTimestamp=autoTimestamp;
		labelDate = StringEscapeUtils.escapeCsv(dateDefault);
		labelMilliseconds = StringEscapeUtils.escapeCsv(millisecondsDefault);
	}
	
	/** write a line to a file named yyyyMMdd in directory
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
	
	/** write a line to a file named yyyyMMdd in directory
	 * @param line String to write to file
	 * @param timeStamp Time of record
	 * @param header for if a new csv file is created
	 * @return true if write was sucessfull, false otherwise
	 */
	public boolean log(String line, Date timeStamp, String header) {
		String filename= directory + "/" + dayFormat.format(timeStamp) + ".CSV";

		
		try { 
			/* check to see if the directory exists, otherwise try to create it */
			File f=new File(directory);
			if ( false == f.isDirectory() && false == f.mkdirs() ) {
				/* unable to create directory */
				System.err.println("# LogLocal error creating directory: " + directory);
				return false;
			}
			
			/* check if the file already exists */
			boolean writeHeader = !new File(filename).isFile();
			
			
			/* Open the file and append to it */
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename,true));

			
			
			if ( autoTimestamp || null == timeStamp ) {
				line = dateFormat.format(timeStamp) + "," + line;
			}
		
			/* write header if file didn't already exist */
			if ( writeHeader ) {
				
				writer.write("\""+labelDate +"\",\""+ labelMilliseconds+"\","+header + "\r\n");
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

	/** write a line to a file named yyyyMMdd in directory
	 * @param line String to write to file
	 * @return true if write was sucessfull, false otherwise
	 */
	public boolean log(String line) {
		/* default to now */
		return log(line,new Date());
	}

}
