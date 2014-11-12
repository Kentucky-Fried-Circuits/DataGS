package dataGS;

import org.apache.commons.lang3.StringEscapeUtils;

public class UtilJSON {
	/**
	 * Make a JSON object with a double. Convert a NaN to "null".
	 * @param object name
	 * @param double value
	 * @return string with JSON representation 
	 */
	public static String putDouble(String title, Double value) {
		if ( Double.isNaN(value) ) {
			return "\"" + title + "\": null";	
		}
		return "\"" + title + "\":" + value; 		
	}
	
	/**
	 * Make a JSON object with an integer value.
	 * @param object name
	 * @param integer value
	 * @return string with JSON representation 
	 */
	public static String putInt(String title, int value) {
		return "\"" + title + "\": " + value;
	}
	
	/**
	 * Make a JSON object with an long value.
	 * @param object name
	 * @param string value
	 * @return string with JSON representation 
	 */
	public static String putLong(String title, long value) {
		return "\"" + title + "\": " + value; 		
	}
	
	/**
	 * Make a JSON object with an string value.
	 * @param object name
	 * @param string value
	 * @return string with JSON representation 
	 */
	public static String putString(String title, String value) {
		return "\"" + title + "\": \"" + StringEscapeUtils.escapeJson(value) + "\""; 		
	}
	
	/**
	 * Make a JSON object with an boolean value.
	 * @param object name
	 * @param string value
	 * @return string with JSON representation 
	 */
	public static String putBoolean(String title, boolean value) {
		if ( value ) 
			return "\"" + title + "\": true";
		return "\"" + title + "\": false";
	}
}
