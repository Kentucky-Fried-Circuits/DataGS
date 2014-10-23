package dataGS;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;


public class TestCSV {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// TODO Auto-generated method stub
		 File csvData = new File("/home/ian/magWeb_MW5001_loglocal/20141022.csv");
		 Scanner scanner;
		 CSVParser parser;
		 try { 
			 scanner = new Scanner(csvData);
			 String header=scanner.nextLine();
			 parser = CSVParser.parse(header, CSVFormat.DEFAULT);
			 header= header.replace( "\"", "" );
			 header= header.replace( " ", "" );			 
			// CSVFormat format = CSVFormat.DEFAULT.withHeader(  );
			 while (scanner.hasNext()){

				// parser = CSVParser.parse(scanner.nextLine(), CSVFormat.DEFAULT.withHeader( "Data Date (UTC)", "Milliseconds",  "i_fault",  "b_dc_volts",  "b_amph_in_out",  "i_temp_fet",  "i_status",  "b_dc_amps",  "b_state_of_charge",  "i_dc_amps",  "i_ac_volts_out",  "i_amps_out",  "i_amps_in",  "a_temperature",  "i_ac_volts_in",  "i_temp_battery",  "i_ac_hz",  "i_temp_transformer",  "i_dc_volts" ));
				 parser = CSVParser.parse(scanner.nextLine(), CSVFormat.DEFAULT.withHeader(header.split( "," )));
				 System.out.println(header);
				 for (CSVRecord csvRecord : parser) {
					 
					 System.out.println("b_state_of_charge:"+csvRecord.get("b_state_of_charge"));
					 System.out.println("b_dc_volts:"+csvRecord.get("b_dc_volts"));
				 }

			 }
		 } catch ( IOException e ) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 }
		

	}

}
