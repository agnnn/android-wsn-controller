package de.rwth.comsys;

import java.util.ArrayList;

import de.rwth.comsys.Enums.MSP430_Command;

public class MSP430Command {
	private MSP430_Command command;
	private byte[] data;
	private short startAddress;
	private short endAddress;
	private ArrayList<Record> records;
	
	
	public MSP430Command(MSP430_Command command, byte[] data, short startAddress, short endAddress, ArrayList<Record> records ) {
		this.command = command;
		this.data = data;
		this.startAddress = startAddress;
		this.endAddress = endAddress;
		this.records = (records);
	}
	
	public MSP430Command(MSP430_Command command) {
		this(command,null,(short)0,(short)0, null);
	}
	
	public MSP430Command(MSP430_Command command, byte[] data) {
		this(command,data,(short)0,(short)0, null);
	}
	
	public MSP430Command(MSP430_Command command, ArrayList<Record> records) {
		this(command, null, (short)0, (short)0, records);
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
	public short getStartAddress() {
		return startAddress;
	}
	/**
	 * @param startAddress the startAddress to set
	 */
	public void setStartAddress(short startAddress) {
		this.startAddress = startAddress;
	}
	/**
	 * @return the endAddress
	 */
	public short getEndAddress() {
		return endAddress;
	}
	/**
	 * @param endAddress the endAddress to set
	 */
	public void setEndAddress(short endAddress) {
		this.endAddress = endAddress;
	}

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
	
	

}
