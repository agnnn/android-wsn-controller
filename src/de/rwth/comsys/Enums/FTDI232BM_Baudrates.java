package de.rwth.comsys.Enums;

public enum FTDI232BM_Baudrates {
	FTDI232BM_BAUDRATE_300(300, 0x2710),
    FTDI232BM_BAUDRATE_600(600, 0x1388),
    FTDI232BM_BAUDRATE_1200(1200, 0x09C4),
    FTDI232BM_BAUDRATE_2400(2400, 0x04E2),
    FTDI232BM_BAUDRATE_4800(4800, 0x0271),
    FTDI232BM_BAUDRATE_9600(9600, 0x4138),
    FTDI232BM_BAUDRATE_19200(19200, 0x809C),
    FTDI232BM_BAUDRATE_38400(38400, 0xC04E),
    FTDI232BM_BAUDRATE_57600(57600, 0x0034),
    FTDI232BM_BAUDRATE_115200(115200, 0x001A),
    FTDI232BM_BAUDRATE_230400(230400, 0x000D),
    FTDI232BM_BAUDRATE_460800(460800, 0x4006),
    FTDI232BM_BAUDRATE_921600(921600, 0x8003);
	
	private int baudrate;
	private int ftdiHexCode;
	
	private FTDI232BM_Baudrates(int baudrate, int ftdiHexCode)
	{
		this.baudrate = baudrate;
	}

	/**
	 * @return the baudrate
	 */
	public int getBaudrate() {
		return baudrate;
	}

	/**
	 * @return the ftdiHexCode
	 */
	public int getFtdiHexCode() {
		return ftdiHexCode;
	}

	
}
