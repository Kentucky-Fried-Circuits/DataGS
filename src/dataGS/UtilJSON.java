package dataGS;

public class UtilJSON {
	public static String putDouble(String title, Double value) {
		if ( Double.isNaN(value) ) {
			return "\"" + title + "\": null";	
		}
		return "\"" + title + "\":" + value; 		
	}
	
	public static String putString(String title, String value) {
		return "\"" + title + "\": \"" + value + "\""; 		
	}
}
