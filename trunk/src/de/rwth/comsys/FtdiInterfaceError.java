package de.rwth.comsys;

public enum FtdiInterfaceError {
	SET_LINE_PROPERTY_OUT_FAILED, 
	SET_LINE_PROPERTY_IN_FAILED, 
	SET_LINE_PROPERTY_PARITY_NOT_SUPPORTED,
	SET_LINE_PROPERTY_STOPBIT_NOT_SUPPORTED,
	SET_LINE_PROPERTY_BREAKTYPE_NOT_SUPPORTED,
	SET_BAUDRATE_IN_FAILED,
	SET_BAUDRATE_OUT_FAILED,
	RESET_USB_IN_FAILED,
	RESET_USB_OUT_FAILED,
	DATA_WRITE_NOT_ALL_BYTES_SENT,
	DATA_READ_THREAD_INTERUPT
}
