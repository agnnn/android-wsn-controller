package de.rwth.comsys;

import java.util.ArrayList;

import de.rwth.comsys.Enums.MSP430_Command;

public class MSP430Command {
	private MSP430_Command command;
	private byte[] data;
	private int startAddress;
	private short length;
	
	private ArrayList<Record> records;
	
	
	public MSP430Command(MSP430_Command command, byte[] data, int startAddress, ArrayList<Record> records, short length ) {
		this.command = command;
		this.data = data;
		this.setStartAddress(startAddress);
		this.records = (records);
		this.length = length;
	}
	
	public MSP430Command(MSP430_Command command) {
		this(command, null, 0, null, (short) 0);
	}
	
	public MSP430Command(MSP430_Command command, byte[] data) {
		this(command, data, 0, null, (short) 0);
	}
	
	public MSP430Command(MSP430_Command command, int startAddress) {
		this(command, null, startAddress, null, (short) 0);
	}
	
	public MSP430Command(MSP430_Command command, ArrayList<Record> records) {
		this(command, null, 0, records, (short) 0);
	}
	
	public MSP430Command(MSP430_Command command, int startAddress, short length) {
		this(command, null, startAddress, null, length);
	}
	
	
	
	/**
	 * @return the command
	 */
	public MSP430_Command getCommand() {
		return command;
	}
	
	/**
	 * @param command the command to set
	 */
	public void setCommand(MSP430_Command command) {
		this.command = command;
	}
	
	/**
	 * @return the data
	 */
	public byte[] getData() {
		return data;
	}
	
	/**
	 * @param data the data to set
	 */
	public void setData(byte[] data) {
		this.data = data;
	}
	/**
	 * @return the startAddress
	 */
	

	/**
	 * @return the records
	 */
	public ArrayList<Record> getRecords() {
		return records;
	}

	/**
	 * @param records the records to set
	 */
	public void setRecords(ArrayList<Record> records) {
		this.records = records;
	}

	/**
	 * @return the startAddress
	 */
	public int getStartAddress() {
		return startAddress;
	}

	/**
	 * @param startAddress the startAddress to set
	 */
	public void setStartAddress(int startAddress) {
		this.startAddress = startAddress;
	}

	/**
	 * @return the length
	 */
	public short getLength() {
		return length;
	}

	/**
	 * @param length the length to set
	 */
	public void setLength(short length) {
		this.length = length;
	}
	
	

}
