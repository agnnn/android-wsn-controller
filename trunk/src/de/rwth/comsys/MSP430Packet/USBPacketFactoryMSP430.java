package de.rwth.comsys.MSP430Packet;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import de.rwth.comsys.Enums.BSLCoreCommandsMSP430;

public class USBPacketFactoryMSP430 {

	// (SLAU319B.pdf, paragraph 1.5)
	private static final int endPointSize = 64;
	
	private ByteBuffer buffer = null;
			
	public void USBPacketMSP430 (){
		if(buffer == null){
			buffer = ByteBuffer.allocate(endPointSize);
		}
	}
	
	
	
	public ByteBuffer createRXDataBlockPacket(){
		return buffer;
		
	}
	
	public ByteBuffer createRXDataBlockFastPacket(){
		return buffer;
		
	}
	
	
}
