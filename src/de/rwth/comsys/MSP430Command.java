package de.rwth.comsys;

import java.util.ArrayList;

import de.rwth.comsys.enums.FTDI232BM_Matching_MSP430_Baudrates;
import de.rwth.comsys.enums.MSP430Variant;
import de.rwth.comsys.enums.MSP430_Commands;
import de.rwth.comsys.ihex.Record;

public class MSP430Command
{

	private MSP430_Commands command;
	private byte[] data;
	private int startAddress;
	private ArrayList<Record> records;
	private short length;
	private FTDI232BM_Matching_MSP430_Baudrates baudrate;
	private MSP430Variant variant;




	public MSP430Command(MSP430_Commands command, byte[] data, int startAddress, ArrayList<Record> records,
			short length, FTDI232BM_Matching_MSP430_Baudrates baudrate, MSP430Variant variant)
	{
		this.command = command;
		this.data = data;
		this.setStartAddress(startAddress);
		this.records = (records);
		this.length = length;
		this.baudrate = baudrate;
		this.variant = variant;
	}




	public MSP430Command(MSP430_Commands command)
	{
		this(command, null, 0, null, (short) 0, null, null);
	}




	public MSP430Command(MSP430_Commands command, byte[] data)
	{
		this(command, data, 0, null, (short) 0, null, null);
	}




	public MSP430Command(MSP430_Commands command, int startAddress)
	{
		this(command, null, startAddress, null, (short) 0, null, null);
	}




	public MSP430Command(MSP430_Commands command, ArrayList<Record> records)
	{
		this(command, null, 0, records, (short) 0, null, null);
	}




	public MSP430Command(MSP430_Commands command, int startAddress, short length)
	{
		this(command, null, startAddress, null, length, null, null);
	}




	public MSP430Command(MSP430_Commands command, FTDI232BM_Matching_MSP430_Baudrates baudrate, MSP430Variant variant)
	{
		this(command, null, 0, null, (short) 0, baudrate, variant);
	}




	/**
	 * @return the command
	 */
	public MSP430_Commands getCommand()
	{
		return command;
	}




	/**
	 * @param command the command to set
	 */
	public void setCommand(MSP430_Commands command)
	{
		this.command = command;
	}




	/**
	 * @return the data
	 */
	public byte[] getData()
	{
		return data;
	}




	/**
	 * @param data the data to set
	 */
	public void setData(byte[] data)
	{
		this.data = data;
	}




	/**
	 * @return the startAddress
	 */

	/**
	 * @return the records
	 */
	public ArrayList<Record> getRecords()
	{
		return records;
	}




	/**
	 * @param records the records to set
	 */
	public void setRecords(ArrayList<Record> records)
	{
		this.records = records;
	}




	/**
	 * @return the startAddress
	 */
	public int getStartAddress()
	{
		return startAddress;
	}




	/**
	 * @param startAddress the startAddress to set
	 */
	public void setStartAddress(int startAddress)
	{
		this.startAddress = startAddress;
	}




	/**
	 * @return the length
	 */
	public short getLength()
	{
		return length;
	}




	/**
	 * @param length the length to set
	 */
	public void setLength(short length)
	{
		this.length = length;
	}




	/**
	 * @return the baudrate
	 */
	public FTDI232BM_Matching_MSP430_Baudrates getBaudrate()
	{
		return baudrate;
	}




	/**
	 * @param baudrate the baudrate to set
	 */
	public void setBaudrate(FTDI232BM_Matching_MSP430_Baudrates baudrate)
	{
		this.baudrate = baudrate;
	}




	/**
	 * @return the variant
	 */
	public MSP430Variant getVariant()
	{
		return variant;
	}




	/**
	 * @param variant the variant to set
	 */
	public void setVariant(MSP430Variant variant)
	{
		this.variant = variant;
	}

}
