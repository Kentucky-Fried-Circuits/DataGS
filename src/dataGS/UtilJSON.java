package dataGS;

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
		return "\"" + title + "\": \"" + value + "\"";
	}
	
	/**
	 * Make a JSON object with an string value.
	 * @param object name
	 * @param string value
	 * @return string with JSON representation 
	 */
	public static String putString(String title, String value) {
		return "\"" + title + "\": \"" + value + "\""; 		
	}
}
