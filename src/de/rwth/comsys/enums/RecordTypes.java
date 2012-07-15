package de.rwth.comsys.enums;

/**
 * Record types of Intel Hex I16HEX format.
 * 
 * @author Christian, Stephan
 * 
 */
public enum RecordTypes
{
	DATA_RECORD(0x00), END_OF_FILE_RECORD(0x01), EXTENDED_SEGMENT_ADDRESS_RECORD(0x02), START_SEGMENT_ADDRESS_RECORD(
			0x03);

	private short code;




	private RecordTypes(int code)
	{
		this.code = (short) code;
	}




	public short getCode()
	{
		return code;
	}
}
