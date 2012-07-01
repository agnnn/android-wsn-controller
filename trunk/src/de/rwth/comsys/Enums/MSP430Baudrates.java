package de.rwth.comsys.Enums;

public enum MSP430Baudrates {
	
	BAUDRATE_9600(9600), BAUDRATE_19200(19200), BAUDRATE_38400(38400);
	
	
	private int baudrate;
	
	private MSP430Baudrates(int baudrate)
	{
		this.baudrate = baudrate;
	}
	
	/**
	 * 
	 * @return the corresponding baudrate.
	 */
	public int getBaudrate()
	{
		return baudrate;
	}
}
