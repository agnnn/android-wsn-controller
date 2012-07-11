package de.rwth.comsys;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import de.rwth.comsys.Enums.RecordTypes;

/**
 * 
 * Reflects one Intel Hex Record entry. Example
 * ":10010000214601360121470136007EFE09D2190140".
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
	private short[] data; // as bytes
	private short checksum;




	private Record()
	{
	};




	/**
	 * Creates a Record and sets checksum.
	 * 
	 * @param byteCount
	 * @param addressHighByte
	 * @param addressLowByte
	 * @param recordType
	 * @param data
	 * @return Record
	 */
	public static Record createRecord(short byteCount, short addressHighByte, short addressLowByte,
			RecordTypes recordType, short[] data)
	{
		Record newRecord = new Record();

		newRecord.setByteCount(byteCount);
		newRecord.setAddressHighByte(addressHighByte);
		newRecord.setAddressLowByte(addressLowByte);
		newRecord.setRecordType(recordType);
		newRecord.setData(data);

		// calculate checksum
		int sum = 0;

		sum += newRecord.getByteCount();
		sum += newRecord.getAddressHighByte();
		sum += newRecord.getAddressLowByte();
		sum += newRecord.getRecordType().getCode();

		// add up data
		for (int i = 0; i < data.length; i++)
		{
			sum += data[i] & 0xFF;
		}

		byte[] sumArray = ByteBuffer.allocate(4).putInt(sum).array();

		// 2 complement of last byte
		short checksum = (short) (((~sumArray[3]) + 1) & 0xFF);

		newRecord.setChecksum(checksum);

		if (checkChecksum(newRecord) == false)
		{
			// Log.w("Record", "Can't create record! Checksum fails!");
			return null;
		}

		return newRecord;
	}




	/**
	 * Creates a Start Segment Address Record.
	 * 
	 * @param segment
	 * @param offset
	 * @return
	 */
	public static Record createStartSegmentAddressRecord(int segment, int offset)
	{
		Record newRecord = new Record();
		short[] data = new short[4];

		data[0] = (short) ((segment >> 8) & 0xFF);
		data[1] = (short) (segment & 0xFF);
		data[2] = (short) ((offset >> 8) & 0xFF);
		data[3] = (short) (offset & 0xFF);

		newRecord.setByteCount((short) 4);
		newRecord.setAddressHighByte((short) 0);
		newRecord.setAddressLowByte((short) 0);
		newRecord.setRecordType(RecordTypes.START_SEGMENT_ADDRESS_RECORD);
		newRecord.setData(data);

		// calculate checksum
		int sum = 0;

		sum += newRecord.getByteCount();
		sum += newRecord.getAddressHighByte();
		sum += newRecord.getAddressLowByte();
		sum += newRecord.getRecordType().getCode();

		// add up data
		for (int i = 0; i < data.length; i++)
		{
			sum += data[i] & 0xFF;
		}

		byte[] sumArray = ByteBuffer.allocate(4).putInt(sum).array();

		// 2 complement of last byte
		short checksum = (short) (((~sumArray[3]) + 1) & 0xFF);

		newRecord.setChecksum(checksum);

		/*
		 * if(checkChecksum(newRecord)==false) { Log.w("Record",
		 * "Can't create record! Checksum fails!"); return null; }
		 */

		return newRecord;

	}




	/**
	 * Creates an End Of File Record.
	 * 
	 * @param segment
	 * @param offset
	 * @return
	 */
	public static Record createEndOfFileRecord()
	{
		Record newRecord = new Record();

		newRecord.setByteCount((short) 0);
		newRecord.setAddressHighByte((short) 0);
		newRecord.setAddressLowByte((short) 0);
		newRecord.setRecordType(RecordTypes.END_OF_FILE_RECORD);
		newRecord.setData(null);
		newRecord.setChecksum((short) 0xFF);

		/*
		 * if(checkChecksum(newRecord)==false) { Log.w("Record",
		 * "Can't create record! Checksum fails!"); return null; }
		 */

		return newRecord;

	}




	/**
	 * Checks if a ihex record has correct checksum.
	 * 
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

		for (int i = 0; i < record.getData().length; i++)
		{
			sum += record.getData()[i];
		}

		sum += record.getChecksum();

		sum = sum & 0xFF;

		// compare
		if (sum == 0)
			return true;

		return false;
	}




	/**
	 * Searches for a Start Segment Address Record and returns the calculated start
	 * address (16-bit).
	 * 
	 * @param records List of Records
	 * @return start address
	 */
	public static int getStartAddress(ArrayList<Record> records)
	{
		int address = 0;
		Record currentRecord = null;
		for (Iterator<Record> iter = records.iterator(); iter.hasNext();)
		{
			currentRecord = iter.next();
			if (currentRecord.getRecordType().getCode() == RecordTypes.START_SEGMENT_ADDRESS_RECORD.getCode())
			{
				// Segment (first 2 bytes of data) * 16 + Offset (last 2 bytes of data)
				int segmentFirst = currentRecord.getData()[0];
				int segmentLast = currentRecord.getData()[1];
				int offsetFirst = currentRecord.getData()[2];
				int offsetLast = currentRecord.getData()[3];
				int segment = (segmentFirst << 8) | (segmentLast);
				int offset = (offsetFirst << 8) | (offsetLast);
				address = (segment * 16 + offset);
			}
		}

		return address;
	}




	/**
	 * Searches for a Start Segment Address Record and returns true iff only one exits.
	 * 
	 * @param records List of Records
	 * @return result
	 */
	public static boolean checkStartSegmentAddressRecord(ArrayList<Record> records)
	{
		boolean foundRecordOnce = false;
		boolean foundRecordTwice = false;

		Record currentRecord = null;
		for (Iterator<Record> iter = records.iterator(); iter.hasNext();)
		{
			currentRecord = iter.next();
			if (currentRecord.getRecordType().getCode() == RecordTypes.START_SEGMENT_ADDRESS_RECORD.getCode())
			{
				if (foundRecordOnce == false)
				{
					foundRecordOnce = true;
				}
				else
				{
					foundRecordTwice = true;
				}

			}
		}

		if (foundRecordOnce && foundRecordTwice)
		{
			return false;
		}
		else
		{
			return foundRecordOnce;
		}
	}




	/**
	 * Searches for a End Of File Record and returns true if it is the last record.
	 * 
	 * @param records List of Records
	 * @return result
	 */
	public static boolean checkEndOfFileRecord(ArrayList<Record> records)
	{
		Record currentRecord = null;
		for (Iterator<Record> iter = records.iterator(); iter.hasNext();)
		{
			currentRecord = iter.next();
			if (currentRecord.getRecordType().getCode() == RecordTypes.END_OF_FILE_RECORD.getCode())
			{
				// must be last element
				if (iter.hasNext())
				{
					return false;
				}
				else
				{
					return true;
				}

			}
		}

		return false;

	}




	/**
	 * Removes all types of records except DATA_RECORD.
	 * 
	 * @param records
	 * @return DATA_RECORD records
	 */
	public static ArrayList<Record> getOnlyDataRecords(ArrayList<Record> records)
	{
		ArrayList<Record> result = new ArrayList<Record>();
		Record currentRecord = null;

		for (Iterator<Record> iter = records.iterator(); iter.hasNext();)
		{
			currentRecord = iter.next();

			if (currentRecord.getRecordType().getCode() == RecordTypes.DATA_RECORD.getCode())
			{
				result.add(currentRecord);
			}
		}

		return result;

	}




	/**
	 * Writes records to System.out.println().
	 * 
	 * @param records
	 */
	public static void printRecords(ArrayList<Record> records)
	{
		for (Iterator<Record> iterator = records.iterator(); iterator.hasNext();)
		{
			Record currentRecord = iterator.next();

			System.out.println(currentRecord.toString());
		}
	}




	@Override
	public String toString()
	{
		String result = "";
		String dataString = "";
		String tempString = "";

		if (this.getData() != null)
		{
			for (int i = 0; i < this.getData().length; i++)
			{
				tempString = Integer.toHexString(this.getData()[i] & 0xFF).toUpperCase();
				// add leading zeros if necessary
				tempString = addLeadingZeros(tempString, 2);
				dataString = dataString + tempString;

			}
		}

		String byteCountString = Integer.toHexString(this.getByteCount() & 0xFF).toUpperCase();
		String addressHighByteString = Integer.toHexString(this.getAddressHighByte() & 0xFF).toUpperCase();
		String addressLowByteString = Integer.toHexString(this.getAddressLowByte() & 0xFF).toUpperCase();
		String recordTypeString = Integer.toHexString(this.getRecordType().getCode() & 0xFF).toUpperCase();
		String checkSumString = Integer.toHexString(this.getChecksum() & 0xFF).toUpperCase();
		
		// add leading zeros if necessary
		byteCountString = addLeadingZeros(byteCountString, 2);
		addressHighByteString = addLeadingZeros(addressHighByteString, 2);
		addressLowByteString = addLeadingZeros(addressLowByteString, 2);
		recordTypeString = addLeadingZeros(recordTypeString, 2);
		checkSumString = addLeadingZeros(checkSumString, 2);

		// complete string
		result = ":" + byteCountString + addressHighByteString + addressLowByteString + recordTypeString + dataString
				+ checkSumString;

		return result;
	}




	/**
	 * Adds leading zeros to a String by parameter length.
	 * 
	 * @param str
	 * @param length Length of the result.
	 * @return result String with leading zeros.
	 */
	public static String addLeadingZeros(String str, int length)
	{
		if (str == null)
			str = "";

		while (str.length() < length)
		{
			str = "0" + str;

		}
		return str;
	}




	/**
	 * @return the startCode
	 */
	public String getStartCode()
	{
		return startCode;
	}




	/**
	 * @param startCode the startCode to set
	 */
	public void setStartCode(String startCode)
	{
		this.startCode = startCode;
	}




	/**
	 * @return the byteCount
	 */
	public short getByteCount()
	{
		return byteCount;
	}




	/**
	 * @param byteCount the byteCount to set
	 */
	public void setByteCount(short byteCount)
	{
		this.byteCount = byteCount;
	}




	/**
	 * @return the addressHighByte
	 */
	public short getAddressHighByte()
	{
		return addressHighByte;
	}




	/**
	 * @param addressHighByte the addressHighByte to set
	 */
	public void setAddressHighByte(short addressHighByte)
	{
		this.addressHighByte = addressHighByte;
	}




	/**
	 * @return the addressLowByte
	 */
	public short getAddressLowByte()
	{
		return addressLowByte;
	}




	/**
	 * @param addressLowByte the addressLowByte to set
	 */
	public void setAddressLowByte(short addressLowByte)
	{
		this.addressLowByte = addressLowByte;
	}




	/**
	 * @return the recordType
	 */
	public RecordTypes getRecordType()
	{
		return recordType;
	}




	/**
	 * @param recordType the recordType to set
	 */
	public void setRecordType(RecordTypes recordType)
	{
		this.recordType = recordType;
	}




	/**
	 * A short represents a byte.
	 * 
	 * @return the data
	 */
	public short[] getData()
	{
		return data;
	}




	/**
	 * @param data the data to set
	 */
	public void setData(short[] data)
	{
		this.data = data;
	}




	/**
	 * @return the checksum
	 */
	public short getChecksum()
	{
		return checksum;
	}




	/**
	 * @param checksum the checksum to set
	 */
	public void setChecksum(short checksum)
	{
		this.checksum = checksum;
	}

}
