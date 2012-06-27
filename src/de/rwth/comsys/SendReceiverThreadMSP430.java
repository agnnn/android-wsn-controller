/**
 * 
 */
package de.rwth.comsys;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import de.rwth.comsys.Enums.FTDI_Constants;
import de.rwth.comsys.Enums.MSP430_Command;
import de.rwth.comsys.Enums.ThreadStates;

/**
 * @author Christian
 * 
 */
public class SendReceiverThreadMSP430 extends Thread {
	private FTDI_Interface ftdiInterface;
	private ArrayList<MSP430Command> commands;
	public int timeout = 4000;
	private AndroidWSNControllerActivity context;

	public SendReceiverThreadMSP430(ArrayList<MSP430Command> commands, FTDI_Interface ftdiInterface) {
		this.ftdiInterface = ftdiInterface;
		ftdiInterface.setLineProperties(FTDI_Constants.DATA_BITS_8, FTDI_Constants.STOP_BITS_1,FTDI_Constants.PARITY_EVEN,FTDI_Constants.BREAK_OFF);
		this.commands = commands;
	}

	public void setContext(AndroidWSNControllerActivity context)
	{
		this.context = context;
	}
	
	public void run() {
		for (MSP430Command cmd : commands) {
			doOutput("pollCmd: "+cmd.getCommand().toString());
			switch (cmd.getCommand()) {
			case MASS_ERASE: {
				doMassErase(cmd.getData());
				break;
			}
			case SET_PASSWORD: {
				setPassword(cmd.getData());
				break;
			}
			case TRANSMIT_PASSWORD: {
				transmitPassword(cmd.getData());
				break;
			}
			}
		}
	}

	private void setPassword(byte[] password) {
		ftdiInterface.write(password, 5000);
	}


	private void transmitPassword(byte[] data) {
		try{
			// Request to transmit password
			int i=0;
			boolean success = false;

			while(!(success || i>1))
			{
				sendBslSync(); 				// sendHeader
				
				Thread.sleep(200);
								
				boolean writeResult = ftdiInterface.write(data, 1000);
				doOutput("Write password: "+writeResult);
				
				byte[] readResult = ftdiInterface.read(5000);
				doOutput("Answer password:");
				for (byte b : readResult) {
					doOutput("0x"+Integer.toHexString(b & 0xFF)+",");
				}
				if(readResult != null && readResult.length > 0)
				{
					if((readResult[0] & 0xFF) == 0x90)
						break;
				}
				i++;
			}
		}catch(InterruptedException e)
		{
			doOutput(e.getMessage());
		}
	}
	
	private void doMassErase(byte[] data) {
		try{
			// Request to mass erase
			int i=0;
			boolean success = false;

			while(!(success || i>5))
			{
				sendResetSequence(true); 	//reset seq
				sendBslSync(); 				// sendHeader
				
				Thread.sleep(200);
								
				boolean writeResult = ftdiInterface.write(data, 1000);
				doOutput("Write massErase: "+writeResult);
				
				byte[] readResult = ftdiInterface.read(5000);
				doOutput("Answer massErase:");
				for (byte b : readResult) {
					doOutput("0x"+Integer.toHexString(b & 0xFF)+",");
				}
				if(readResult != null && readResult.length > 0)
				{
					if((readResult[0] & 0xFF) == 0x90)
						break;
				}
				i++;
			}
		}catch(InterruptedException e)
		{
			doOutput(e.getMessage());
		}
	}


	private boolean sendBslSync() {
		byte data[] = new byte[1];
		data[0] = (byte)0x80;
		
		ftdiInterface.write(data, timeout);
		byte[] resp = ftdiInterface.read(timeout);
		if(resp.length > 0)
		{
			if(resp[0] == 0x90)
				return true;
		}
		return false;
	}

	private void sendResetSequence(boolean invokeBsl) {
		if(invokeBsl)
		{
			telosWriteCmd((byte)0,(byte)1);
			telosWriteCmd((byte)0,(byte)3);
			telosWriteCmd((byte)0,(byte)1);
			telosWriteCmd((byte)0,(byte)3);
			telosWriteCmd((byte)0,(byte)2);
			telosWriteCmd((byte)0,(byte)0);
		}
		else{
			telosWriteCmd((byte)0,(byte)3);
			telosWriteCmd((byte)0,(byte)2);
			telosWriteCmd((byte)0,(byte)0);
	        try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	private void telosStop() {
		setDTR(true);
		setRTS(false);
		setDTR(false);
	}

	private void telosWriteByte(byte dataByte) {
		telosWriteBit( ( dataByte & 0x80) > 0 );
        telosWriteBit( ( dataByte & 0x40) > 0 );
        telosWriteBit( ( dataByte & 0x20) > 0);
        telosWriteBit( ( dataByte & 0x10) > 0);
        telosWriteBit( ( dataByte & 0x08) > 0);
        telosWriteBit( ( dataByte & 0x04) > 0);
        telosWriteBit( ( dataByte & 0x02) > 0);
        telosWriteBit( ( dataByte & 0x01) > 0);
        telosWriteBit( false );  // "acknowledge"
	}

	private void telosStart() {
		setDTR(false);
		setRTS(false);
		setDTR(true);
	}
	
	/**
	 * Sets the dtr.
	 *
	 * @param dtr: DTR setting. Can only be SIO_SET_DTR_HIGH or SIO_SET_DTR_LOW.
	 * @return 0: Everything is OK.
	 *  -1: USB controlTransfer method failed.
	 *  -2: input value cannot be recognized.
	 */
	private int setDTR(boolean state)
	{
		short usb_val;
		
		if (state)
	        usb_val = FTDI_Constants.SIO_SET_DTR_HIGH;
	    else
	        usb_val = FTDI_Constants.SIO_SET_DTR_LOW;
		
		if (ftdiInterface.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE, 
				  						  FTDI_Constants.SIO_SET_MODEM_CTRL_REQUEST, 
				  						  usb_val,
				  						  FTDI_Constants.INTERFACE_ANY, null, 0, 2000) != 0)
		{
			//doOutput(Integer.toString(-1)+",");
			Log.e("ftdi_control","USB controlTransfer operation failed.");
			return -1;
		}
		else
		{
			//doOutput(Integer.toString(0)+",");
			return 0;
		}
	}
	
	/**
	 * Sets the rts.
	 *
	 * @param rts: the RTS setting. Can only be SIO_SET_RTS_HIGH or SIO_SET_RTS_LOW.
	 * @return 0: Everything is OK.
	 *  -1: USB controlTransfer method failed.
	 *  -2: input value cannot be recognized.
	 */
	private int setRTS(boolean state)
	{
		short usb_val;
		
		if (state)
	        usb_val = FTDI_Constants.SIO_SET_RTS_HIGH;
	    else
	        usb_val = FTDI_Constants.SIO_SET_RTS_LOW;
		
		if (ftdiInterface.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE, 
				  							  FTDI_Constants.SIO_SET_MODEM_CTRL_REQUEST, 
				  							  usb_val,
				  							  FTDI_Constants.INTERFACE_ANY, null, 0, 2000) != 0)
		{
			//doOutput(Integer.toString(-1)+",");
			Log.e("ftdi_control","USB controlTransfer operation failed. ");
			return -1;
		}
		else
		{
			//doOutput(Integer.toString(0)+",");
			return 0;
		}
		
	}

	private void telosWriteBit(boolean bit)
	{
		try
		{
			setRTS(true);
			setDTR(!bit);
			Thread.sleep(0,2);
			setRTS(false);
			Thread.sleep(0,1);
			setRTS(true);
		} 
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void telosWriteCmd(byte addr, byte cmd)
	{
		telosStart();
		telosWriteByte((byte)(0x90 | (addr << 1)));
		telosWriteByte(cmd);
		telosStop();
	}
	
	private void doOutput(final String msg)
	{
		if(this.context == null)
			return;
		
        //do something before you send the message
		Handler handler = context.getUiHandler();
    	Bundle data = new Bundle();
		data.putString(null, msg);
		Message message = handler.obtainMessage();
		message.setData(data);
		
		handler.sendMessage(message);
	}
}
