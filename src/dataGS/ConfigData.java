/*
 * Quick and Dirty Configuration Handler
 * 
 * TODO: Clean up code.
 */
package dataGS;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.google.gson.*;

public class ConfigData {
	private String config_filename;
	private String lock_filename;
	
	public ConfigData(String configFile, String lockFile) {
		config_filename = configFile;
		lock_filename = lockFile;		
	}
	
	public JsonObject getJSON() {
		if (config_filename == null) {
			return new JsonObject();
		}
		try {
			byte[] encoded = Files.readAllBytes(Paths.get(config_filename));
			String json_string = new String(encoded, Charset.forName("UTF-8"));
			JsonParser parser = new JsonParser();
			return parser.parse(json_string).getAsJsonObject();
		} catch (Exception e) {
			return null;
		}		
	}
	
	public String getValue (String name) {
		JsonObject json = getJSON();
		if (json == null || json.get(name) == null) {
			return null;
		}
		return json.get(name).getAsString();
	}
	
	public boolean setValue (String name, String value) {
		if (config_filename == null || (lock_filename != null && !(new File(lock_filename)).exists())) {
			return false;
		}
		JsonObject json = getJSON();
		if (json == null) {
			json = new JsonObject();
		}
		json.addProperty(name, value);
		try {
			Writer writer = new FileWriter(config_filename);
	        Gson gson = new GsonBuilder().create();
	        gson.toJson(json, writer);
	        writer.close();
	    } catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public static void main (String args[]) {
		ConfigData config = new ConfigData("/tmp/config", "/tmp/lock");
		if (args.length == 1) {
			System.out.println(config.getValue(args[0]));
		} else if (args.length == 2) {
			if (config.setValue(args[0], args[1])) {
				System.out.println("Value successfully set.");
			} else {
				System.out.println("ERROR: Value not set!");
			}
		} else {
			System.out.println("Invalid syntax... 1 argument get, 2 set!");
		}
	}
}
