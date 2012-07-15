package de.rwth.comsys.enums;

public enum FTDI232BM_Matching_MSP430_Baudrates
{

	BAUDRATE_9600(9600, 0x4138), BAUDRATE_19200(19200, 0x809C), BAUDRATE_38400(38400, 0xC04E);

	private int baudrate;
	private int ftdiHexCode;




	private FTDI232BM_Matching_MSP430_Baudrates(int baudrate, int ftdiHexCode)
	{
		this.baudrate = baudrate;
	}




	/**
	 * @return the baudrate
	 */
	public int getBaudrate()
	{
		return baudrate;
	}




	/**
	 * @return the ftdiHexCode
	 */
	public int getFtdiHexCode()
	{
		return ftdiHexCode;
	}

}
