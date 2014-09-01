package dataGS;

public class FloatUtility {
	public static float buildFloatFromInts (int data[],int start) {                                                                                
		return Float.intBitsToFloat((((data[start+1])&0xFFFF)<<16) + (data[start]&0xFFFF));                                                                                             
	}
	
	public static float buildFloatFromIntsLittle (int data[],int start) {                                                                                
		return Float.intBitsToFloat((((data[start+0])&0xFFFF)<<16) + (data[start+1]&0xFFFF));                                                                                             
	}
}
