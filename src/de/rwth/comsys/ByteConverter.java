package de.rwth.comsys;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Formatter;



public class ByteConverter {
	
	
	
	
	
	/**
	 * Converts an array of short to an array of bytes.
	 * @param data
	 * @return
	 */
	public static byte[] convertShortArrayToByteArray(short[] data)
	{
		byte[] result = new byte[data.length];	
		
		for(int i = 0; i < data.length; i++)
		{
			result[i]= (byte) (data[i] & 0xFF);
		}
		
		return result;
	}	
	
	/**
	 * Converts an array of bytes to an array of short.
	 * @param data
	 * @return
	 */
	public static short[] convertByteArrayToShortArray(byte[] data)
	{
		short[] result = new short[data.length];	
		
		for(int i = 0; i < data.length; i++)
		{
			result[i] = (short) (data[i] & 0xFF);
		}
		
		return result;
	}	
	
	
}
