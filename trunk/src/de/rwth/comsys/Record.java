package de.rwth.comsys;

import java.util.ArrayList;
import java.util.Iterator;

import android.util.Log;
import de.rwth.comsys.Enums.RecordTypes;

/**
 * 
 * Reflects one Intel Hex Record entry.
 * Example ":10010000214601360121470136007EFE09D2190140".
 * 
 * @author Christian, Stephan
 *
 */
public class Record
{
	private String startCode = ":";
	private short byteCount; //
	private short addressHighByte;
	private short addressLowByte;
	private RecordTypes recordType;
	private	short[] data; // as bytes
	private short checksum;
	
	
	private Record(){};
	
	/**
	 * Creates a Record and checks checksum. 
	 * @param byteCount
	 * @param addressHighByte
	 * @param addressLowByte
	 * @param recordType
	 * @param data
	 * @param checksum
	 * @return Record or null if checksum fails.
	 */
	public static Record createRecord(short byteCount, short addressHighByte, short addressLowByte, RecordTypes recordType, short[] data, short checksum)
	{	
		Record newRecord = new Record();
		
		newRecord.setByteCount(byteCount);
		newRecord.setAddressHighByte(addressHighByte);
		newRecord.setAddressLowByte(addressLowByte) ;
		newRecord.setRecordType(recordType);
		newRecord.setData(data);
		newRecord.setChecksum(checksum);
		
		if(checkChecksum(newRecord)==false)
		{	
			Log.w("Record", "Can't create record! Checksum fails!");
			return null;
		}
		
		return newRecord;
	}
	
	/**
	 * Checks if a ihex record has correct checksum.
	 * @param record 
	 * @return 
	 */
	public static boolean checkChecksum(Record record)
	{
		
		int sum = 0;
		
		sum += record.getByteCount();
		sum += record.getAddressHighByte();
		sum += record.getAddressLowByte();
		sum += record.getRecordType().getCode();
		
		for(int i = 0; i < record.getData().length; i++)
		{
			sum += record.getData()[i];
		}
		
		sum += record.getChecksum();
		
		sum =  sum & 0xFF;
		
		// compare
		if( sum == 0) return true;
		
		return false;
	}
	
	/**
	 * Searches for a Start Segment Address Record and returns the calculated start address (16-bit).
	 * @param records List of Records
	 * @return start address
	 */
	public static int getStartAddress(ArrayList<Record> records)
	{	
		int address = 0; 
		Record currentRecord = null;
		for(Iterator<Record> iter = records.iterator(); iter.hasNext();)
		{
			currentRecord = iter.next();
			if(currentRecord.getRecordType().getCode() == RecordTypes.START_SEGMENT_ADDRESS_RECORD.getCode())
			{	
				// Segment (first 2 bytes of data) * 16 + Offset (last 2 bytes of data)
				int segmentFirst = currentRecord.getData()[0];
				int segmentLast = currentRecord.getData()[1];
				int offsetFirst = currentRecord.getData()[2];
				int offsetLast = currentRecord.getData()[3];
				int segment = (segmentFirst << 8) | (segmentLast);
				int offset  = (offsetFirst << 8) | (offsetLast);
				address = (segment * 16 + offset);
			}
		}
		
		return address;
	}
	
	/**
	 * Searches for a Start Segment Address Record and returns true iff only one exits.
	 * @param records List of Records
	 * @return result
	 */
	public static boolean checkStartSegmentAddressRecord(ArrayList<Record> records)
	{	
		boolean foundRecordOnce = false;
		boolean foundRecordTwice = false;
		
		Record currentRecord = null;
		for(Iterator<Record> iter = records.iterator(); iter.hasNext();)
		{
			currentRecord = iter.next();
			if(currentRecord.getRecordType().getCode() == RecordTypes.START_SEGMENT_ADDRESS_RECORD.getCode())
			{	
				if(foundRecordOnce == false)
				{
					foundRecordOnce =  true;
				}
				else
				{
					foundRecordTwice = true;
				}
				
			}
		}
		
		if(foundRecordOnce && foundRecordTwice)
		{
			return false;
		}
		else
		{
			return foundRecordOnce;
		}
	}

	/**
	 * @return the startCode
	 */
	public String getStartCode() {
		return startCode;
	}

	/**
	 * @param startCode the startCode to set
	 */
	public void setStartCode(String startCode) {
		this.startCode = startCode;
	}

	/**
	 * @return the byteCount
	 */
	public short getByteCount() {
		return byteCount;
	}

	/**
	 * @param byteCount the byteCount to set
	 */
	public void setByteCount(short byteCount) {
		this.byteCount = byteCount;
	}

	/**
	 * @return the addressHighByte
	 */
	public short getAddressHighByte() {
		return addressHighByte;
	}

	/**
	 * @param addressHighByte the addressHighByte to set
	 */
	public void setAddressHighByte(short addressHighByte) {
		this.addressHighByte = addressHighByte;
	}

	/**
	 * @return the addressLowByte
	 */
	public short getAddressLowByte() {
		return addressLowByte;
	}

	/**
	 * @param addressLowByte the addressLowByte to set
	 */
	public void setAddressLowByte(short addressLowByte) {
		this.addressLowByte = addressLowByte;
	}

	/**
	 * @return the recordType
	 */
	public RecordTypes getRecordType() {
		return recordType;
	}

	/**
	 * @param recordType the recordType to set
	 */
	public void setRecordType(RecordTypes recordType) {
		this.recordType = recordType;
	}

	/**
	 * A short represents a byte.
	 * @return the data 
	 */
	public short[] getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(short[] data) {
		this.data = data;
	}

	/**
	 * @return the checksum
	 */
	public short getChecksum() {
		return checksum;
	}

	/**
	 * @param checksum the checksum to set
	 */
	public void setChecksum(short checksum) {
		this.checksum = checksum;
	}

	

	
	
}
