package de.rwth.comsys;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import android.widget.TextView;

public class ByteConverter {
	static TextView tve;
	static void bla(TextView tv)
	{
		tve = tv;
	}
	
	static short[] getByteStreamFromSigned(ByteBuffer data)
	{
		/*
		 * print the received data stream for debug purpose
		 */
		tve.append("\ndbg output\n");
		//ShortBuffer sb = data.asShortBuffer();
		for(int i=0;i<data.capacity();i++)
		{
			tve.append(data.get(i)+",");
			
		}
		short[] resultArr = new short[data.capacity()];
		byte nextByte = 0;
		if(data.capacity() > 0)
			nextByte = data.get(0);
		for (int i=1;i<data.capacity();i++) {
			short previousByte = data.get(i);
			//System.out.println("previousByte: "+String.valueOf(previousByte));
			//System.out.println("nextByte: "+String.valueOf(nextByte));
			//tve.append("firstByte: "+previousByte+"\n");
			//tve.append("nextByte: "+nextByte+"\n");
			byte sign = (byte) (nextByte & 1);
			short result = 0;
			if(sign == 1)
			{
				//System.out.println("invert!");
				result = getOneComplement((byte)previousByte);
			}
			else
			{
				//System.out.println("no invert!");
				result = (short)(previousByte);
			}
			
			//System.out.println("result: "+String.valueOf(result));
			resultArr[i-1] = result;
			previousByte = (byte)(nextByte >> 1);
		}
		
		return resultArr;
	}
	
	static ByteBuffer getByteBufferFromShort(short[] data)
	{
		ByteBuffer buf = ByteBuffer.allocate(data.length);
		for (short dataElem : data) {
			//byte highByte = (byte)(dataElem << 8);
			byte lowByte = (byte)dataElem;
			buf.put(lowByte);
			//buf.put(highByte);
		}
		return buf;
	}
	
	static short getOneComplement(byte byteVal)
	{
		return (short)(((~byteVal) + 1) & 0xFF);
	}
	
	static short getOneComplement(short byteVal)
	{
		return (short)(((~byteVal) + 1) & 0xFFFF);
	}
}
