package dataGS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class UtilFiles {
	public String[] listFilesForFolder(String dirName) {
		return listFilesForFolder( dirName, false );
	}
	
	/** 
	 * 
	 * @param dirName is a String of the directory name to get files from
	 * @param absolute is a boolean that decideds to return just the filename or the absolute path
	 * @return String array of filenames
	 */
	public String[] listFilesForFolder(String dirName, boolean absolute) {

		/* go through a directory and return a string[] with every filename in directory, ignoring sub directories */
		try {
			final File directory = new File(dirName);
			List<String> files = new ArrayList<String>();
			for ( final File fileEntry : directory.listFiles() ) {
				/* if  not a directory */
				String fn = fileEntry.getName();
				if ( !fileEntry.isDirectory() && !fn.contains("~") ) {
					if ( NumberUtils.isNumber(FilenameUtils.getBaseName( fn )) ) {
						if ( absolute ) {
							files.add( fileEntry.getAbsolutePath() );
						} else {
							files.add( fileEntry.getName() );
						}
					}

				}
			}
			String[] sort = files.toArray( new String[ files.size() ] );
			Arrays.sort(sort, Collections.reverseOrder());
			return  sort;
		} catch (Exception e) {
			return null;
		}

	}
	
	/* This method opens the file passed to it 
	 * and returns the json object array
	 *  as a string array of json objects */
	public static String[] getJsonFromFile( String filename ){
		/* create BufferedREader to open file and read character by character */
		BufferedReader br;

		/* these will be used when reading in the file. 
		 * token is each character read in and 
		 * toSplit is to end up as a string representation of the array of json objects */
		char token;
		String toSplit="";
		boolean start = false;
		try{
			br = new BufferedReader( new FileReader(filename) );
			boolean readToken = true;
			while ( readToken ) {
				token = (char) br.read();
				/* If we get a -1 then that means we reached the end of the file */
				if ( (char)-1 == token ){
					readToken = false;
					break;
				}

				/* This bracket indicates that we have gotten to the end of the json object array */
				if ( ']' == token ) {
					start=false;
					//readToken=false;
				}

				/* If we have found the beginning of the json object array, then we
				 * add the token to the toSplit string */
				if ( start ) {
					toSplit+=token;
				}

				/* This bracket indicates that we have found the beginning of the json object array */
				if ( '[' == token ) 
					start=true;
			}
			/* we are done with the file so we close the bufferedreader */
			br.close();
		} catch ( Exception e ) {
			System.err.println(e);
		}
		//System.out.println(toSplit);
		/* Now we have a string that looks something like this
		 *  { <string of json object> },{ <string of json object> },{ <string of json object> },...{ <string of json object> }
		 * 
		 * 
		 *  */
		String[] split = toSplit.split( "},");

		
		/* iterate through the split array and add the '}' bracket back without the ',' comma */
		for ( int i = 0; i < split.length-1; i++ ){
			split[i] = split[i] + "}";

		}

		return split;
	}
}
