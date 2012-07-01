package de.rwth.comsys;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;

import de.rwth.comsys.Enums.FTDI232BM_Baudrates;
import de.rwth.comsys.Enums.FTDI_Constants;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.widget.TextView;

/**
 * Establishes and manages connection between FTDI and Android.
 * Thanks to : http://code.google.com/p/android-ftdi-usb2serial-driver-package/
 * @author Christian & Stephan
 *
 */
public class FTDI_Interface {
	private UsbDeviceConnection deviceConnection;
	private FtdiInterfaceError lastError;
	private UsbEndpoint sendEndpoint;
	private UsbEndpoint receiveEndpoint;
	private int maxSendPacketSize;
	private int maxReceivePacketSize;
	private FTDI232BM_Baudrates baudrate;
	
	private FTDI_Interface(){};
	
	/**
	 * Create a new FTDI_Interface instance to connect to a certain device with
	 * a ftdi chip
	 * 
	 * @param deviceConnection
	 */
	public FTDI_Interface(UsbDeviceConnection deviceConnection,
			UsbEndpoint sendEndpoint, UsbEndpoint receiveEndpoint) {
		if (sendEndpoint.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK)
			throw new IllegalArgumentException(
					"the type of the sendEndpoint is not supported!");
		if (receiveEndpoint.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK)
			throw new IllegalArgumentException(
					"the type of the sendEndpoint is not supported!");
		if (sendEndpoint.getDirection() != UsbConstants.USB_DIR_OUT)
			throw new IllegalArgumentException(
					"the direction of the sendEndpoint is not correct!");
		if (receiveEndpoint.getDirection() != UsbConstants.USB_DIR_IN)
			throw new IllegalArgumentException(
					"the direction of the sendEndpoint is not correct!");

		this.sendEndpoint = sendEndpoint;
		this.receiveEndpoint = receiveEndpoint;
		this.deviceConnection = deviceConnection;
		this.maxSendPacketSize = sendEndpoint.getMaxPacketSize();
		this.maxReceivePacketSize = receiveEndpoint.getMaxPacketSize();
		
		baudrate = FTDI232BM_Baudrates.FTDI232BM_BAUDRATE_9600;
	}

	/**
	 * Sets the corresponding connection parameters
	 * 
	 * @param bits
	 *            <ul>
	 *            <li> {@link FTDI_Constants#DATA_BITS_7}</li>
	 *            <li> {@link FTDI_Constants#DATA_BITS_8}</li>
	 *            </ul>
	 * @param sbit
	 *            <ul>
	 *            <li>{@link FTDI_Constants#STOP_BITS_1}</li>
	 *            <li>{@link FTDI_Constants#STOP_BITS_15}</li>
	 *            <li>{@link FTDI_Constants#STOP_BITS_2}</li>
	 *            </ul>
	 * @param parity
	 *            <ul>
	 *            <li>{@link FTDI_Constants#PARITY_EVEN}</li>
	 *            <li>{@link FTDI_Constants#PARITY_MARK}</li>
	 *            <li> {@link FTDI_Constants#PARITY_NONE}</li>
	 *            <li> {@link FTDI_Constants#PARITY_ODD}</li>
	 *            <li>{@link FTDI_Constants#PARITY_SPACE}</li>
	 *            </ul>
	 * @param break_type
	 *            <ul>
	 *            <li>{@link FTDI_Constants#BREAK_OFF}</li>
	 *            <li> {@link FTDI_Constants#BREAK_ON}</li>
	 *            </ul>
	 * @return true if success, false if failure the last error can be read with
	 *         getLastErrorString or getLastError
	 */
	public boolean setLineProperties(int bits, int sbit, int parity,
			int break_type) {
		int value = bits;

		switch (parity) {
		case FTDI_Constants.PARITY_NONE:
			value |= (0x00 << 8);
			break;
		case FTDI_Constants.PARITY_ODD:
			value |= (0x01 << 8);
			break;
		case FTDI_Constants.PARITY_EVEN:
			value |= (0x02 << 8);
			break;
		case FTDI_Constants.PARITY_MARK:
			value |= (0x03 << 8);
			break;
		case FTDI_Constants.PARITY_SPACE:
			value |= (0x04 << 8);
			break;
		default:
			lastError = FtdiInterfaceError.SET_LINE_PROPERTY_PARITY_NOT_SUPPORTED;
			return false;
		}

		switch (sbit) {
		case FTDI_Constants.STOP_BITS_1:
			value |= (0x00 << 11);
			break;
		case FTDI_Constants.STOP_BITS_15:
			value |= (0x01 << 11);
			break;
		case FTDI_Constants.STOP_BITS_2:
			value |= (0x02 << 11);
			break;
		default:
			lastError = FtdiInterfaceError.SET_LINE_PROPERTY_STOPBIT_NOT_SUPPORTED;
			return false;

		}

		switch (break_type) {
		case FTDI_Constants.BREAK_OFF:
			value |= (0x00 << 14);
			break;
		case FTDI_Constants.BREAK_ON:
			value |= (0x01 << 14);
			break;
		default:
			lastError = FtdiInterfaceError.SET_LINE_PROPERTY_BREAKTYPE_NOT_SUPPORTED;
			return false;
		}

		int result = 0;
		result = deviceConnection.controlTransfer(
				FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE,
				FTDI_Constants.SIO_SET_DATA_REQUEST, value,
				FTDI_Constants.INTERFACE_ANY, null, 0, 2000);

		if (result == 0) {
			result = deviceConnection.controlTransfer(
					FTDI_Constants.FTDI_DEVICE_IN_REQTYPE,
					FTDI_Constants.SIO_SET_DATA_REQUEST, value,
					FTDI_Constants.INTERFACE_ANY, null, 0, 2000);
			if (result != 0)
				this.lastError = FtdiInterfaceError.SET_LINE_PROPERTY_IN_FAILED;
		} else {
			this.lastError = FtdiInterfaceError.SET_LINE_PROPERTY_OUT_FAILED;
		}

		return (result == 0);
	}

	/**
	 * Sets the baudrate to a constant value of 9600
	 * 
	 * @return true if success, false if failure the last error can be read with
	 *         getLastErrorString or getLastError
	 */
	public boolean setBaudrate() {
		int result = 0;
		result = deviceConnection.controlTransfer(
				FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE,
				FTDI_Constants.SIO_SET_BAUDRATE_REQUEST, 0x4138,
				FTDI_Constants.INTERFACE_ANY, null, 0, 2000);
		if (result == 0) {
			result = deviceConnection.controlTransfer(
					FTDI_Constants.FTDI_DEVICE_IN_REQTYPE,
					FTDI_Constants.SIO_SET_BAUDRATE_REQUEST, 0x4138,
					FTDI_Constants.INTERFACE_ANY, null, 0, 2000);
			if (result != 0)
				lastError = FtdiInterfaceError.SET_BAUDRATE_IN_FAILED;
		} else {
			lastError = FtdiInterfaceError.SET_BAUDRATE_OUT_FAILED;
		}
		return (result == 0);
	}
	
	
	/**
	 * Sets the baudrate.
	 * 
	 * @param baudrate
	 * @return true if success, false if failure the last error can be read with
	 *         getLastErrorString or getLastError
	 */
	public boolean setBaudrate(FTDI232BM_Baudrates baudrate) {
		int result = 0;
		// send change
		result = deviceConnection.controlTransfer(
				FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE,
				FTDI_Constants.SIO_SET_BAUDRATE_REQUEST, baudrate.getFtdiHexCode(),
				FTDI_Constants.INTERFACE_ANY, null, 0, 2000);
		// get ack?
		if (result == 0) {
			result = deviceConnection.controlTransfer(
					FTDI_Constants.FTDI_DEVICE_IN_REQTYPE,
					FTDI_Constants.SIO_SET_BAUDRATE_REQUEST, baudrate.getFtdiHexCode(),
					FTDI_Constants.INTERFACE_ANY, null, 0, 2000);
			if (result != 0)
			{	
				lastError = FtdiInterfaceError.SET_BAUDRATE_IN_FAILED;
			}
			else
			{	// got ack
				this.baudrate = baudrate;
			}
		} else {
			lastError = FtdiInterfaceError.SET_BAUDRATE_OUT_FAILED;
		}
		
		return (result == 0);
	}
	
	/**
	 * Resets the usb device
	 * 
	 * @return true if success, false if failure the last error can be read with
	 *         getLastErrorString or getLastError
	 */
	public boolean resetUsb() {
		int result = 0;
		result = deviceConnection.controlTransfer(
				FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE,
				FTDI_Constants.SIO_RESET_REQUEST, FTDI_Constants.SIO_RESET_SIO,
				FTDI_Constants.INTERFACE_ANY, null, 0, 2000);

		if (result == 0) {

			result = deviceConnection.controlTransfer(
					FTDI_Constants.FTDI_DEVICE_IN_REQTYPE,
					FTDI_Constants.SIO_RESET_REQUEST,
					FTDI_Constants.SIO_RESET_SIO, FTDI_Constants.INTERFACE_ANY,
					null, 0, 2000);
			if (result != 0)
				lastError = FtdiInterfaceError.RESET_USB_IN_FAILED;
		} else {
			lastError = FtdiInterfaceError.RESET_USB_OUT_FAILED;
		}
		return (result == 0);
	}

	/**
	 * Reads all available bytes on the receiving endpoint within the given
	 * timeout, if the stream is closed before the timeout occurs all the
	 * received data to this point is returned
	 * 
	 * @return null if no data was received byte[] array of data on success
	 */
	public byte[] read(int timeout) {
		ByteBuffer overallBuffer = ByteBuffer.allocate(maxReceivePacketSize);
		int overallRead = 0;
		try {
			int curRead = 0;
			long startTime = new Date().getTime();
			long curTime;
			byte[] buffer = new byte[maxReceivePacketSize];

			do {
				curRead = deviceConnection.bulkTransfer(receiveEndpoint,
						buffer, buffer.length, timeout);

				if (curRead != -1) {
					if (curRead > 2) // more than the ftdi head bytes received?
					{
						if ((buffer[2] & 0xFF) != 0) {
							overallBuffer.put(buffer, 2, curRead);
							overallRead += curRead - 2;
							break;
						}
					}
				}

				curTime = new Date().getTime();
				Thread.sleep(500);
			} while ((curTime - startTime < timeout) && (curRead != -1)); // read
																			// within
																			// timeout
																			// or
																			// end
																			// of
																			// stream

		} catch (Exception e) {
			lastError = FtdiInterfaceError.DATA_READ_THREAD_INTERUPT;
		}
		return Arrays.copyOfRange(overallBuffer.array(), 0, overallRead);
	}

	/**
	 * Writes the given data into the usb sending endpoint
	 * 
	 * @return true if the send was successful false if an error occured
	 */
	public boolean write(byte[] data, int timeout) {
		int sentBytes = deviceConnection.bulkTransfer(this.sendEndpoint, data,
				data.length, timeout);

		if (sentBytes != data.length)
			lastError = FtdiInterfaceError.DATA_WRITE_NOT_ALL_BYTES_SENT;
		return (sentBytes == data.length);
	}

	/**
	 * Returns the last occured error as String
	 * 
	 * @return String the String representation of the error
	 */
	public String getLastErrorString() {
		return lastError.toString();
	}

	/**
	 * Return the last occurred error
	 * 
	 * @return int the error num
	 */
	public FtdiInterfaceError getLastError() {
		return lastError;
	}

	
	public int controlTransfer(int ftdiDeviceOutReqtype,
			int sioSetModemCtrlRequest, short usb_val, int interfaceNum,
			byte[] data, int length, int timeout) {

		return (deviceConnection.controlTransfer(ftdiDeviceOutReqtype,
				sioSetModemCtrlRequest, usb_val, interfaceNum, data, length,
				timeout));
	}

	/**
	 * @return the baudrate
	 */
	public FTDI232BM_Baudrates getBaudrate() {
		return baudrate;
	}
}
