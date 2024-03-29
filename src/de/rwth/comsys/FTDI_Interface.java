package de.rwth.comsys;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;
import de.rwth.comsys.enums.FTDI232BM_Matching_MSP430_Baudrates;
import de.rwth.comsys.enums.FTDI_Constants;
import de.rwth.comsys.enums.FtdiInterfaceError;

/**
 * Establishes and manages connection between FTDI and Android.
 * 
 * @author Christian & Stephan
 * 
 */
public class FTDI_Interface
{
	private UsbDeviceConnection deviceConnection;
	private FtdiInterfaceError lastError;
	private UsbEndpoint sendEndpoint;
	private UsbEndpoint receiveEndpoint;
	private int maxReceivePacketSize;
	private FTDI232BM_Matching_MSP430_Baudrates baudrate;


	@SuppressWarnings("unused")
	private FTDI_Interface()
	{
	};

	/**
	 * Create a new FTDI_Interface instance to connect to a certain device with a ftdi
	 * chip
	 * 
	 * @param deviceConnection
	 */
	public FTDI_Interface(UsbDeviceConnection deviceConnection, UsbEndpoint sendEndpoint, UsbEndpoint receiveEndpoint)
	{
		if (sendEndpoint.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK)
			throw new IllegalArgumentException("the type of the sendEndpoint is not supported!");
		if (receiveEndpoint.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK)
			throw new IllegalArgumentException("the type of the sendEndpoint is not supported!");
		if (sendEndpoint.getDirection() != UsbConstants.USB_DIR_OUT)
			throw new IllegalArgumentException("the direction of the sendEndpoint is not correct!");
		if (receiveEndpoint.getDirection() != UsbConstants.USB_DIR_IN)
			throw new IllegalArgumentException("the direction of the sendEndpoint is not correct!");

		this.sendEndpoint = sendEndpoint;
		this.receiveEndpoint = receiveEndpoint;
		this.deviceConnection = deviceConnection;
		sendEndpoint.getMaxPacketSize();
		this.maxReceivePacketSize = receiveEndpoint.getMaxPacketSize();

		baudrate = FTDI232BM_Matching_MSP430_Baudrates.BAUDRATE_9600;
	}




	/**
	 * Sets the corresponding connection parameters
	 * 
	 * @param bits <ul>
	 *        <li> {@link FTDI_Constants#DATA_BITS_7}</li>
	 *        <li> {@link FTDI_Constants#DATA_BITS_8}</li>
	 *        </ul>
	 * @param sbit <ul>
	 *        <li>{@link FTDI_Constants#STOP_BITS_1}</li>
	 *        <li>{@link FTDI_Constants#STOP_BITS_15}</li>
	 *        <li>{@link FTDI_Constants#STOP_BITS_2}</li>
	 *        </ul>
	 * @param parity <ul>
	 *        <li>{@link FTDI_Constants#PARITY_EVEN}</li>
	 *        <li>{@link FTDI_Constants#PARITY_MARK}</li>
	 *        <li> {@link FTDI_Constants#PARITY_NONE}</li>
	 *        <li> {@link FTDI_Constants#PARITY_ODD}</li>
	 *        <li>{@link FTDI_Constants#PARITY_SPACE}</li>
	 *        </ul>
	 * @param break_type <ul>
	 *        <li>{@link FTDI_Constants#BREAK_OFF}</li>
	 *        <li> {@link FTDI_Constants#BREAK_ON}</li>
	 *        </ul>
	 * @return true if success, false if failure the last error can be read with
	 *         getLastErrorString or getLastError
	 */
	public boolean setLineProperties(int bits, int sbit, int parity, int break_type)
	{
		int value = bits;

		switch (parity)
		{
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

		switch (sbit)
		{
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

		switch (break_type)
		{
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
		result = deviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE,
				FTDI_Constants.SIO_SET_DATA_REQUEST, value, FTDI_Constants.INTERFACE_ANY, null, 0, 2000);

		if (result == 0)
		{
			result = deviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_IN_REQTYPE,
					FTDI_Constants.SIO_SET_DATA_REQUEST, value, FTDI_Constants.INTERFACE_ANY, null, 0, 2000);
			if (result != 0)
				this.lastError = FtdiInterfaceError.SET_LINE_PROPERTY_IN_FAILED;
		}
		else
		{
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
	public boolean setBaudrate()
	{
		int result = 0;
		result = deviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE,
				FTDI_Constants.SIO_SET_BAUDRATE_REQUEST, 0x4138, FTDI_Constants.INTERFACE_ANY, null, 0, 2000);
		if (result == 0)
		{
			result = deviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_IN_REQTYPE,
					FTDI_Constants.SIO_SET_BAUDRATE_REQUEST, 0x4138, FTDI_Constants.INTERFACE_ANY, null, 0, 2000);
			if (result != 0)
				lastError = FtdiInterfaceError.SET_BAUDRATE_IN_FAILED;
		}
		else
		{
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
	public boolean setBaudrate(FTDI232BM_Matching_MSP430_Baudrates baudrate)
	{
		int result = 0;
		// send change
		result = deviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE,
				FTDI_Constants.SIO_SET_BAUDRATE_REQUEST, baudrate.getFtdiHexCode(), FTDI_Constants.INTERFACE_ANY, null,
				0, 2000);
		// get ack?
		if (result == 0)
		{
			result = deviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_IN_REQTYPE,
					FTDI_Constants.SIO_SET_BAUDRATE_REQUEST, baudrate.getFtdiHexCode(), FTDI_Constants.INTERFACE_ANY,
					null, 0, 2000);
			if (result != 0)
			{
				lastError = FtdiInterfaceError.SET_BAUDRATE_IN_FAILED;
			}
			else
			{ // got ack
				this.baudrate = baudrate;
			}
		}
		else
		{
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
	public boolean resetUsb()
	{
		int result = 0;
		result = deviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE,
				FTDI_Constants.SIO_RESET_REQUEST, FTDI_Constants.SIO_RESET_SIO, FTDI_Constants.INTERFACE_ANY, null, 0,
				2000);

		if (result == 0)
		{

			result = deviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_IN_REQTYPE,
					FTDI_Constants.SIO_RESET_REQUEST, FTDI_Constants.SIO_RESET_SIO, FTDI_Constants.INTERFACE_ANY, null,
					0, 2000);
			if (result != 0)
				lastError = FtdiInterfaceError.RESET_USB_IN_FAILED;
		}
		else
		{
			lastError = FtdiInterfaceError.RESET_USB_OUT_FAILED;
		}
		return (result == 0);
	}




	/**
	 * Reads all available bytes on the receiving endpoint within the given timeout, if
	 * the stream is closed before the timeout occurs all the received data to this point
	 * is returned
	 * 
	 * @return null if no data was received byte[] array of data on success
	 */
	public byte[] read(int timeout)
	{
		ByteBuffer overallBuffer = ByteBuffer.allocate(maxReceivePacketSize);
		int overallRead = 0;
		try
		{
			int curRead = 0;
			long startTime = new Date().getTime();
			long curTime;
			byte[] buffer = new byte[64]; // max buffer size of FT232BM chip
			// TODO current chunk size must be determined
			
			// read within timeout or end of stream
			do
			{
				curRead = deviceConnection.bulkTransfer(receiveEndpoint, buffer, buffer.length, timeout);

				if (curRead != -1)
				{
					if (curRead > 2) // more than the ftdi head bytes received? // was 2
					{
						if ((buffer[2] & 0xFF) != 0) // was buffer[2]
						{
							overallBuffer.put(buffer, 2, curRead);
							overallRead += curRead - 2;
							break;
						}
					}
				}

				curTime = new Date().getTime();
				Thread.sleep(5);
			} while ((curTime - startTime < timeout) && (curRead != -1));

		} catch (Exception e)
		{
			lastError = FtdiInterfaceError.DATA_READ_THREAD_INTERUPT;
		}
		return Arrays.copyOfRange(overallBuffer.array(), 0, overallRead);
	}




	/**
	 * Writes the given data into the usb sending endpoint
	 * 
	 * @return true if the send was successful false if an error occured
	 */
	public boolean write(byte[] data, int timeout)
	{
		int sentBytes = deviceConnection.bulkTransfer(this.sendEndpoint, data, data.length, timeout);

		if (sentBytes != data.length)
			lastError = FtdiInterfaceError.DATA_WRITE_NOT_ALL_BYTES_SENT;
		return (sentBytes == data.length);
	}




	/**
	 * Returns the last occured error as String
	 * 
	 * @return String the String representation of the error
	 */
	public String getLastErrorString()
	{
		return lastError.toString();
	}




	/**
	 * Return the last occurred error
	 * 
	 * @return int the error num
	 */
	public FtdiInterfaceError getLastError()
	{
		return lastError;
	}




	/**
	 * public int controlTransfer(int ftdiDeviceOutReqtype, int sioSetModemCtrlRequest,
	 * short usb_val, int interfaceNum, byte[] data, int length, int timeout) {
	 * 
	 * return (deviceConnection.controlTransfer(ftdiDeviceOutReqtype,
	 * sioSetModemCtrlRequest, usb_val, interfaceNum, data, length, timeout)); }
	 */

	/**
	 * @return the baudrate
	 */
	public FTDI232BM_Matching_MSP430_Baudrates getBaudrate()
	{
		return baudrate;
	}




	/**
	 * Sets the dtr.
	 * 
	 * @param dtr: DTR setting. Can only be SIO_SET_DTR_HIGH or SIO_SET_DTR_LOW.
	 * @return 0: Everything is OK. -1: USB controlTransfer method failed. -2: input value
	 *         cannot be recognized.
	 */
	public int setDTR(boolean state)
	{
		short usb_val;

		if (state)
			usb_val = FTDI_Constants.SIO_SET_DTR_HIGH;
		else
			usb_val = FTDI_Constants.SIO_SET_DTR_LOW;

		if (deviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE,
				FTDI_Constants.SIO_SET_MODEM_CTRL_REQUEST, usb_val, FTDI_Constants.INTERFACE_ANY, null, 0, 2000) != 0)
		{

			Log.e("ftdi_control", "USB controlTransfer operation failed.");
			return -1;
		}
		else
		{

			return 0;
		}
	}

	
	/**
	 * Sets the dtr.
	 * 
	 * @param dtr: DTR setting. Can only be SIO_SET_DTR_HIGH or SIO_SET_DTR_LOW.
	 * @return 0: Everything is OK. -1: USB controlTransfer method failed. -2: input value
	 *         cannot be recognized.
	 */
	public boolean setFlowCtrl(int state)
	{
		switch(state)
		{
		case FTDI_Constants.SIO_DISABLE_FLOW_CTRL:
		case FTDI_Constants.SIO_DTR_DSR_HS:
		case FTDI_Constants.SIO_RTS_CTS_HS:
		case FTDI_Constants.SIO_XON_XOFF_HS: break;
			default:
				return false;
		}

		if (deviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE,
				FTDI_Constants.SIO_SET_FLOW_CTRL_REQUEST, state, FTDI_Constants.INTERFACE_ANY, null, 0, 2000) != 0)
		{

			Log.e("ftdi_control", "USB controlTransfer operation failed.");
			return false;
		}
		else
		{

			return true;
		}
	}



	/**
	 * Sets the rts.
	 * 
	 * @param rts: the RTS setting. Can only be SIO_SET_RTS_HIGH or SIO_SET_RTS_LOW.
	 * @return 0: Everything is OK. -1: USB controlTransfer method failed. -2: input value
	 *         cannot be recognized.
	 */
	public int setRTS(boolean state)
	{
		short usb_val;

		if (state)
			usb_val = FTDI_Constants.SIO_SET_RTS_HIGH;
		else
			usb_val = FTDI_Constants.SIO_SET_RTS_LOW;

		if (deviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE,
				FTDI_Constants.SIO_SET_MODEM_CTRL_REQUEST, usb_val, FTDI_Constants.INTERFACE_ANY, null, 0, 2000) != 0)
		{

			Log.e("ftdi_control", "USB controlTransfer operation failed. ");
			return -1;
		}
		else
		{

			return 0;
		}

	}

    /**
     * Sets the bit mask bit mode.
     *
     * @param bitmask : Every bit marks the I/O configuration for GPIO pins. High/ON value configures the pin as output. LOW/OFF as input. ?? TODO: need to verify this!
     * @param bitmode : set the bitbang mode. Must be MPSSE_BITMODE_RESET, MPSSE_BITMODE_BITBANG, 
     *                                      MPSSE_BITMODE_MPSSE, MPSSE_BITMODE_SYNCBB, MPSSE_BITMODE_MCU,MPSSE_BITMODE_OPTO
     *                                      MPSSE_BITMODE_CBUS, or MPSSE_BITMODE_SYNCFF
     * @return 0: Everything is OK.
     *  -1: USB controlTransfer method failed.
     *  -2: input value cannot be recognized.
     */
    public int SetBitMaskBitMode(byte bitmask, byte bitmode)
    {
            //check if the bitmode is valide. bitmask no need to check.
            int combinedSetupValue = (bitmode << 8) | bitmask;
            int r;
            if((r = deviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE, FTDI_Constants.SIO_SET_BITMODE_REQUEST, 
                                                                            combinedSetupValue, FTDI_Constants.INTERFACE_ANY, null, 0, 2000))  != 0)
            {
                    Log.e("ftdiInterface","USB controlTransfer operation failed. controlTransfer return value is:"+Integer.toString(r));
                    return -1;
            }
            else
            {
                    //need to keep a record of the bitmode, and let upper level methods be able to poll the bitmode status.
                    return 0;
            }
    }
       
    /**
     * Convert baud rate
     *
     * @param baudrate the baudrate
     * @param value_index: values that gives to usb control transfer. First int is "value", second is "index"
     * 
     * @return: the actual baud rate we get. The actual baud rate cannot always equals to the desired baud rate
     * due to clock pre-scale stuff inside the FTDI chips.
     */
    private int convertBaudRate(int baudrate, int[] value_index)
    {
        final byte[] am_adjust_up = {0, 0, 0, 1, 0, 3, 2, 1};
        final byte[] am_adjust_dn = {0, 0, 0, 1, 0, 1, 2, 3};
        final byte[] frac_code = {0, 3, 2, 4, 1, 5, 6, 7};
        int divisor, best_divisor, best_baud, best_baud_diff, value, index;
        long encoded_divisor;
        
        int deviceType = FTDI_Constants.DEVICE_TYPE_BM;

        if (baudrate <= 0)
        {
            // Return error
            return -1;
        }

        divisor = 24000000 / baudrate;

        if (deviceType == FTDI_Constants.DEVICE_TYPE_AM)
        {
            // Round down to supported fraction (AM only)
            divisor -= am_adjust_dn[divisor & 7];
        }

        // Try this divisor and the one above it (because division rounds down)
        best_divisor = 0;
        best_baud = 0;
        best_baud_diff = 0;
        for (int i = 0; i < 2; i++)
        {
            int try_divisor = divisor + i;
            int baud_estimate;
            int baud_diff;

            // Round up to supported divisor value
            if (try_divisor <= 8)
            {
                // Round up to minimum supported divisor
                try_divisor = 8;
            }
            else if (deviceType != FTDI_Constants.DEVICE_TYPE_AM && try_divisor < 12)
            {
                // BM doesn't support divisors 9 through 11 inclusive
                try_divisor = 12;
            }
            else if (divisor < 16)
            {
                // AM doesn't support divisors 9 through 15 inclusive
                try_divisor = 16;
            }
            else
            {
                if (deviceType == FTDI_Constants.DEVICE_TYPE_AM)
                {
                    // Round up to supported fraction (AM only)
                    try_divisor += am_adjust_up[try_divisor & 7];
                    if (try_divisor > 0x1FFF8)
                    {
                        // Round down to maximum supported divisor value (for AM)
                        try_divisor = 0x1FFF8;
                    }
                }
                else
                {
                    if (try_divisor > 0x1FFFF)
                    {
                        // Round down to maximum supported divisor value (for BM)
                        try_divisor = 0x1FFFF;
                    }
                }
            }
            // Get estimated baud rate (to nearest integer)
            baud_estimate = (24000000 + (try_divisor / 2)) / try_divisor;
            // Get absolute difference from requested baud rate
            if (baud_estimate < baudrate)
            {
                baud_diff = baudrate - baud_estimate;
            }
            else
            {
                baud_diff = baud_estimate - baudrate;
            }
            if (i == 0 || baud_diff < best_baud_diff)
            {
                // Closest to requested baud rate so far
                best_divisor = try_divisor;
                best_baud = baud_estimate;
                best_baud_diff = baud_diff;
                if (baud_diff == 0)
                {
                    // Spot on! No point trying
                    break;
                }
            }
        }
        // Encode the best divisor value
        encoded_divisor = (best_divisor >> 3) | (frac_code[best_divisor & 7] << 14);
        // Deal with special cases for encoded value
        if (encoded_divisor == 1)
        {
            encoded_divisor = 0;    // 3000000 baud
        }
        else if (encoded_divisor == 0x4001)
        {
            encoded_divisor = 1;    // 2000000 baud (BM only)
        }
        // Split into "value" and "index" values
        value = (short)(encoded_divisor & 0xFFFF);
        if (deviceType == FTDI_Constants.DEVICE_TYPE_2232C || deviceType == FTDI_Constants.DEVICE_TYPE_2232H || deviceType == FTDI_Constants.DEVICE_TYPE_4232H)
        {
            index = (short)(encoded_divisor >> 8);
            index &= 0xFF00;
            index |= FTDI_Constants.INTERFACE_ANY;
        }
        else
        {
            index = (short)(encoded_divisor >> 16);
        }
        value_index[0]=value;
        value_index[1]=index;
        // Return the nearest baud rate
        return best_baud;
    }
    
    /**
     * Sets the baud rate.
     *
     * @param baudrate the baudrate
     * @return postive num: Everything is OK. num is the actual baud rate.
     *              -1: the usb controlTransfer function has failed.
     *              -2: Cannot implement the desired baud rate.
     *              -3: The error between implemented baud rate and desired baud rate >5 percent.
     */
    public int setBaudRate(int baudrate)
    {
        int value, index;
        int actual_baudrate;
        int[] value_index = {0,0};

        //TODO: Verify if this works fine. the FTDI_Interface need to know if the bitbang mode is enabled or not.
        //          Let's read more about FTDI hardware documents.
        //          So far it seems set, as long as the bitmode is NOT MPSSE_BITMODE_RESET, it shall be classified as "bigbang_enabled"
        /*if(this.isBitBangEnabled())
        {
            baudrate = baudrate*4;
        }*/
        
        actual_baudrate = convertBaudRate(baudrate, value_index);//this function will change the items in value_index.
        value = value_index[0];
        index = value_index[1];
        
        if (actual_baudrate <= 0)
        {
            //write to log, this is a impossible baudrate
            Log.e("ftdiInterface","The actual baud rate calculated as zero or negative value, impossible to implement: " + Integer.toString(actual_baudrate));
            return -2;
        }

        // Check within tolerance (about 5%)
        if ((actual_baudrate * 2 < baudrate /* Catch overflows */ )
                || ((actual_baudrate < baudrate) ? (actual_baudrate * 21 < baudrate * 20) : (baudrate * 21 < actual_baudrate * 20)))
        {
            //Unsupported baudrate. Note: bitbang baudrates are automatically multiplied by 4
            Log.e("ftdiInterface", "The actual baud rate: "+ Integer.toString(actual_baudrate)+" has >5% error from desired baud rate: "+ Integer.toString(baudrate));
            return -3;
        }
        
        int r;
        if(( r = deviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE, FTDI_Constants.SIO_SET_BAUDRATE_REQUEST,
                    value, index, null, 0, 2000)) != 0)
        {
            //write to log: setting new baudrate failed
            Log.e("ftdiInterface","USB controlTransfer operation failed. controlTransfer return value is:"+Integer.toString(r));
            return -1;
        }
        else
        {
                //need to keep a record of what baudrate is setup.
           // mBaudRate = actual_baudrate;
                return actual_baudrate;
        }
    }


}
