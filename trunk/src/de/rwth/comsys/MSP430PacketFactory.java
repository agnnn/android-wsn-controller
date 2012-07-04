package de.rwth.comsys;

import de.rwth.comsys.Enums.FTDI232BM_Matching_MSP430_Baudrates;
import de.rwth.comsys.Enums.MSP430Variant;

/**
 * Used to create packets for MSP430 device (serial standard protocol). 
 * For details have a look on SLAU319B.pdf.
 * @author Christian & Stephan
 *
 */
public class MSP430PacketFactory {
	private  MSP430PacketFactory (){}
		
	/**
	 * Creates a MSP430 RX PASSWORD command.
	 * 80 10 24 24 xx xx xx xx D1 D2 … D20 CKL CKH ACK
	 * @param password
	 * @return packet to send
		 */
	public static byte[] createSetPasswordCommand(byte[] password)
	{
		byte[] bytesUSB = new byte[11+password.length];
		
		short HEADER = 0x80;
		short CMD = 0x10; //get password
		short L1  = 0x24;
		short L2  = 0x24;
		short AL  = 0x00;
		short AH  = 0x00; 
		short LL  = 0x00;
		short LH  = 0x00;
		short CKL = 0x80 ^ 0x24;
		short CKH = 0x10 ^ 0x24;
		short ACK = 0x90;
		
		bytesUSB[0] = (byte)HEADER; 
		bytesUSB[1] = (byte)CMD;
		bytesUSB[2] = (byte)L1;
		bytesUSB[3] = (byte)L2;
		bytesUSB[4] = (byte)AL;
		bytesUSB[5] = (byte)AH;
		bytesUSB[6] = (byte)LL;
		bytesUSB[7] = (byte)LH;
		
		for(int i=0;i<password.length;i++)
		{
			byte curByte = password[i];
			if(i%2 == 0)
			{
				CKL ^= curByte;
			}
			else
			{
				CKH ^= curByte;
			}
			bytesUSB[8+i] = curByte;
		}
		bytesUSB[8+password.length] = (byte)~CKL;
		bytesUSB[9+password.length] = (byte)~CKH;
		bytesUSB[10+password.length] = (byte)ACK;
		
		return bytesUSB;
	}
	
	/**
	 * Creates a MSP430 TX BSL VERSION command, which invokes the MSP430 to
	 * respond with data from startAddress until startAddress + length (16-bit
	 * blocks). 
	 * TX BSL version 80 1E 04 04 xx xx xx xx CKL CKH
	 * @return packet to send
	 * 
	 */
	public static byte[] createRequestBslVersionCommand()
	{	
		// 10 = HEADER, CMD, L1, L2, AL, AH, LL, LH, CKL, CKH 
		int countOfAllBytes = 10;

		// data to send in short, because of signed bytes problem
		short[] result = new short[countOfAllBytes];

		result[0] = 0x80; 					// HEADER
		result[1] = 0x1E; 					// CMD
		result[2] = 04; 					// L1: Number of bytes consisting of AL through Dn.
											// Restrictions: L1 = L2, L1 < 250, L1 even
		result[3] = result[2]; 				// L2:= L1
		result[4] = 0x00; 					// AL: any data
		result[5] = 0x00; 					// AH: any data
		result[6] = 0x00; 					// LL: any data
		result[7] = 0x00; 					// LH: always 0
		result[8] = getCKL(result);
		result[9] = getCKH(result);
		
		return ByteConverter.convertShortArrayToByteArray(result);
		
	}
	
	
		
		
	/**
	 * Creates a MSP430 TX DATA BLOCK command, which invokes the MSP430 to respond with
	 * data from startAddress until startAddress + length (16-bit blocks).
	 * SLAU319B.pdf
	 * TX DATA BLOCK 80 14 04 04 AL AH LL LH CKL CKH
	 * @param startAddressHighByte 
	 * @param startAddressLowByte
	 * @param length How many 16-bit blocks shall be read? 
	 * TODO Limited to 20 blocks (FTDI endpoint 64 byte)? , 16 best w.r.t I16HEX!
	 * @return packet to send or null if input was wrong
	 */
	public static byte[] createTXDataBlockCommand(short startAddressHighByte, short startAddressLowByte, short length)
	{	
		// check length
		if(length > 20 || length < 1) return null;
	
		// 10 = HEADER, CMD, L1, L2, AL, AH, LL, LH, CKL, CKH
		int countOfAllBytes = 10;
		
				
		// data to send in short, because of signed bytes problem
		short[] result = new short[countOfAllBytes];
		
		result[0]  = 0x80; 							//HEADER
		result[1]  = 0x14; 							//CMD
		result[2]  = 04;	 						//L1: Number of bytes consisting of AL through Dn. Restrictions: L1 = L2, L1 < 250, L1 even
		result[3]  = result[2];						//L2:= L1
		result[4]  = startAddressLowByte; 			//AL: start address
		result[5]  = startAddressHighByte; 			//AH
		result[6]  = length; 						//LL
		result[7]  = 0x00;							//LH: always 0
		result[8]  = getCKL(result); 
		result[9]  = getCKH(result); 
		
		
		return ByteConverter.convertShortArrayToByteArray(result);
	}
	
	
	
	/**
	 * Creates a MSP430 LOAD PC packet. 
	 * SLAU319B.pdf
	 * Load PC 80 1A 04 04 AL AH x x CKL CKH ACK
	 * @param data to write must be have even length <= 52 and > 4
	 * @param startAddress 16-bit address < 1FF
	 * @return packet to send 
	 */
	public static byte[] createLoadPCCommand(short startAddressHighByte, short startAddressLowByte)
	{	
		// HEADER, CMD, L1, L2, AL, AH, LL, LH, CKL, CKH, ACK
		int countOfAllBytes = 11; 
				
		// data to send in short, because of signed bytes problem
		short[] result = new short[countOfAllBytes];
		
		result[0]  = 0x80; 							//HEADER
		result[1]  = 0x1A; 							//CMD
		result[2]  = 0x04;	 						//L1: Number of bytes consisting of AL through Dn. Restrictions: L1 = L2, L1 < 250, L1 even
		result[3]  = result[2];						//L2:= L1
		result[4]  = startAddressLowByte; 			//AL: start address
		result[5]  = startAddressHighByte; 			//AH
		result[6]  = 0x00; 							//LL any data
		result[7]  = 0x00;							//LH: always 0
		result[8]  = getCKL(result); 
		result[9]  = getCKH(result); 
		result[10]  = 0x90;  //ACK
		
		return ByteConverter.convertShortArrayToByteArray(result);
	}
	
	
	/**
	 * Creates a CHANGE BAUDRATE packet. 
	 * SLAU319B.pdf
	 * Load PC 80 1A 04 04 AL AH x x CKL CKH ACK
	 * @param data to write must be have even length <= 52 and > 4
	 * @param startAddress 16-bit address < 1FF
	 * @return packet to send or null if input was wrong
	 */
	public static byte[] createChangeBaudrateCommand(FTDI232BM_Matching_MSP430_Baudrates baudrate, MSP430Variant variant)
	{	
		// HEADER, CMD, L1, L2, AL, AH, LL, LH, CKL, CKH, ACK
		int countOfAllBytes = 11; 
				
		// data to send in short, because of signed bytes problem
		short[] result = new short[countOfAllBytes];
		
		//	prepare AL, AH, LL
		short al = 0;
		short ah = 0;
		short ll = 0;
		
		/*switch(variant)
		{
			case MSP430_F149:
				switch (baudrate)
				{
				case BAUDRATE_9600:
					al = 0x80;
					ah = 0x85;
					ll = 0x00;
					break;
				case BAUDRATE_19200:
					al = 0xE0;
					ah = 0x86;
					ll = 0x01;
					break;
				case BAUDRATE_38400:
					al = 0xE0;
					ah = 0x87;
					ll = 0x02;
					break;
				default:
					return null;
				}
				break;
			case MSP430_F449:
				switch (baudrate)
				{
				case BAUDRATE_9600:
					al = 0x00;
					ah = 0x98;
					ll = 0x00;
					break;
				case BAUDRATE_19200:
					al = 0x00;
					ah = 0xB0;
					ll = 0x01;
					break;
				case BAUDRATE_38400:
					al = 0x00;
					ah = 0xC8;
					ll = 0x02;
					break;
				default:
					return null;
				}
				break;
			case MSP430_F2131:
				switch (baudrate)
				{
				case BAUDRATE_9600:
					al = 0x80;
					ah = 0x85;
					ll = 0x00;
					break;
				case BAUDRATE_19200:
					al = 0x00;
					ah = 0x8B;
					ll = 0x01;
					break;
				case BAUDRATE_38400:
					al = 0x80;
					ah = 0x8C;
					ll = 0x02;
					break;
				default:
					return null;
				}
				break;
			default:
				return null;
				
		}*/
		
		result[0]  = 0x80; 				//HEADER
		result[1]  = 0x20; 				//CMD
		result[2]  = 0x04;	 			//L1: Number of bytes consisting of AL through Dn. Restrictions: L1 = L2, L1 < 250, L1 even
		result[3]  = 0x04;				//L2:= L1
		result[4]  = 0xE0; 				//AL
		result[5]  = 0x87; 				//AH
		result[6]  = 0x02; 				//LL 
		result[7]  = 0x00;				//LH: always 0
		result[8]  = getCKL(result); 
		result[9]  = getCKH(result); 
		result[10]  = 0x90;  //ACK
		
		return ByteConverter.convertShortArrayToByteArray(result);
	}
	
	
	/**
	 * Creates a MSP430 RX Data block packet. 
	 * SLAU319B.pdf
	 * RX data block 80 12 n n AL AH n-4 0 D1 D2 ... Dn-4 CKL CKH ACK
	 * @param data to write must be have even length <= 52 and > 4 
	 * @param startAddressHighByte
	 * @param startAddressLowByte
	 * @return packet to send or null if input was wrong
	 */
	public static byte[] createRXDataBlockCommand(short[] data, short startAddressHighByte, short startAddressLowByte)
	{	
		
		//check length
		if(((data.length > 52) || (data.length < 4)) && (data.length % 2 == 1)) return null;
		
		// 11 = HEADER, CMD, L1, L2, AL, AH, LL, LH, CKL, CKH, ACK
		int countOfAllBytes = 11 + data.length;
		
				
		// data to send in short, because of signed bytes problem
		short[] result = new short[countOfAllBytes];
		
		result[0]  = 0x80; 							//HEADER
		result[1]  = 0x12; 							//CMD
		result[2]  = (short) (data.length + 4);	 	//L1: Number of bytes consisting of AL through Dn. Restrictions: L1 = L2, L1 < 250, L1 even
		result[3]  = result[2];						//L2:= L1
		result[4]  = startAddressLowByte; 			//AL: start address
		result[5]  = startAddressHighByte; 	//AH
		result[6]  = (short) data.length; 			//LL
		result[7]  = 0x00;							//LH: always 0
		
		for(int i = 8; i < result.length - 3; i++)
		{	
			result[i] = data[i-8];
		}
		
		result[result.length - 3]  = getCKL(result); 
		result[result.length - 2]  = getCKH(result); 
		result[result.length - 1]  = 0x90;  //ACK
		
		return ByteConverter.convertShortArrayToByteArray(result);
	}
	
	
	
	/**
	 * Creates a MSP430 Mass Erase command packet, 
	 * which erases the entire flash memory area.
	 * SLAU319B.pdf
	 * MASS ERASE 80  18 04 04 xx xx 06 A5 CKL CKH ACK
	 * @return packet to send
	 */
	public static byte[] createMassEraseCommand()
	{
		byte[] bytesUSB = new byte[11];
		
		short HEADER = 0x80;
		short CMD = 0x18; //bsl version; 0x18 mass erase
		short L1  = 0x04;
		short L2  = 0x04;
		short AL  = 0x00;
		short AH  = 0xFF; // means every 
		short LL  = 0x06;
		short LH  = 0xA5;
		short CKL = ~(0x80 ^ 0x04 ^ 0x00 ^ 0x06);
		short CKH = ~(0x18 ^ 0x04 ^ 0x0FF^ 0xA5);
		short ACK = 0x90;
		
		bytesUSB[0] = (byte)(HEADER & 0xFF); 
		bytesUSB[1] = (byte)(CMD & 0xFF);
		bytesUSB[2] = (byte)(L1 & 0xFF);
		bytesUSB[3] = (byte)(L2 & 0xFF);
		bytesUSB[4] = (byte)(AL & 0xFF);
		bytesUSB[5] = (byte)(AH & 0xFF);
		bytesUSB[6] = (byte)(LL & 0xFF);
		bytesUSB[7] = (byte)(LH & 0xFF);
		bytesUSB[8] = (byte)(CKL & 0xFF);
		bytesUSB[9] = (byte)(CKH & 0xFF);
		bytesUSB[10] = (byte)(ACK & 0xFF);
			
		return bytesUSB;
	}
		
		
	/**
	 * Calculates CKL value for a MSP430 packet.
	 * SLAU319B.pdf
	 * @param data complete packet with ckl, ckh, ack
	 * @return ckl
	 */
	public static short getCKL(short[] data)
	{
		short ckl = 0;
		
		// -3 = without ckl, ckh and ack
		for(int i=0; i < data.length - 3; i++)
		{
			if(i%2 == 0)
			{
				ckl ^= data[i];
			}
		}
		
		return (short) (~ckl);
		
	}
	
	/**
	 * Calculates CKH value for a MSP430 packet.
	 * SLAU319B.pdf
	 * @param data complete packet with ckl, ckh, ack
	 * @return ckh
	 */
	private static short getCKH(short[] data)
	{
		short ckh = 0;
		
		// -3 = without ckl, ckh and ack
		for(int i=0; i < data.length - 3; i++)
		{
			if(i%2 == 1)
			{
				ckh ^= data[i];
			}
		}
		
		return (short) (~ckh);
	}
}