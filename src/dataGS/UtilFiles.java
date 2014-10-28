package dataGS;

import java.io.File;
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
}
