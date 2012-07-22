package de.rwth.comsys;

import java.util.ArrayList;

import de.rwth.comsys.ihex.Record;

public class FlashMapping {
	private ArrayList<Record> records;
	private int interfaceIndex;
	
	public FlashMapping(int interfaceIndex, ArrayList<Record> records)
	{
		this.records = records;
		this.interfaceIndex = interfaceIndex;
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

	/**
	 * @return the interfaceIndex
	 */
	public int getInterfaceIndex() {
		return interfaceIndex;
	}

	/**
	 * @param interfaceIndex the interfaceIndex to set
	 */
	public void setInterfaceIndex(int interfaceIndex) {
		this.interfaceIndex = interfaceIndex;
	}
	
	

}
