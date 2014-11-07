package utilities;

import java.io.IOException;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;

import dataGS.DayStats;
import dataGS.Stat;


public class testStat {
	
	//DayStats ds;
	
	
	public static void main(String[] args) throws IOException {
		
		double stat;

		DescriptiveStatistics[] ds = new DescriptiveStatistics[15];
		
		Random random = new Random();
		
		String[] names = {"volts","amps","jules","bobby","watts","amph","in","ac_out","i_dc","more","dumb","names","test","ugh","last"};
		
		
		for ( int j = 0 ; j < ds.length ; j++ ) {
			
			ds[j] = new SynchronizedDescriptiveStatistics();
			ds[j].setWindowSize( 8640 );
			for (int i = 1 ; i < 9000 ; i++ ){
				stat = (random.nextInt(20-10+1)+10)*j;
				ds[j].addValue( stat );
			}
			System.out.println("number: "+j);
			System.out.println("min: "+ds[j].getMin());
			System.out.println("max: "+ds[j].getMax());
			System.out.println("avg: "+ds[j].getMean());
		}

	}
}
