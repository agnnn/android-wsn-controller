package de.rwth.comsys;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.widget.TextView;
import android.widget.Toast;
import de.rwth.comsys.enums.FTDI232BM_Matching_MSP430_Baudrates;
import de.rwth.comsys.enums.MSP430Variant;
import de.rwth.comsys.enums.MSP430_Commands;
import de.rwth.comsys.ihex.Record;

/**
 * Used to set up an Android communication with a Telosb. Provides several commands which
 * can be executed.
 * 
 * @author Christian & Stepahn
 * 
 */
public class TelosBConnector
{

	private TextView textView;
	private UsbInterface mUSBInterface;
	private ArrayList<UsbDevice> mDevice;
	private ArrayList<UsbEndpoint> sendingEndpointMSP430;
	private ArrayList<UsbEndpoint> receivingEndpointMSP430;
	private ArrayList<UsbDeviceConnection> mDeviceConnection;
	private ArrayList<FTDI_Interface> ftdiInterface;
	private UsbManager mManager;
	private ArrayList<MSP430Command> commandList;
	private byte[] password;
	private final int PASSWORD_LENGTH = 32;
	private MSP430Variant deviceVariant = null;
	private AndroidWSNControllerActivity context;
	ProgrammerThreadMSP430 myThread = null;




	public TelosBConnector(UsbManager usbManager, AndroidWSNControllerActivity parentActivity)
	{
		if (usbManager == null)
			throw new IllegalArgumentException("Error: usbManager is null");
		this.mManager = usbManager;
		this.context = parentActivity;
		this.commandList = new ArrayList<MSP430Command>();
		this.textView = context.getOutputTextView();
		this.mDevice = new ArrayList<UsbDevice>();
		this.sendingEndpointMSP430 = new ArrayList<UsbEndpoint>();
		this.receivingEndpointMSP430 = new ArrayList<UsbEndpoint>();
		this.mDeviceConnection = new ArrayList<UsbDeviceConnection>() ;
		this.ftdiInterface = new ArrayList<FTDI_Interface>();

		// set the default password
		byte[] defaultPwd = new byte[PASSWORD_LENGTH];
		for (int i = 0; i < PASSWORD_LENGTH; i++)
		{
			defaultPwd[i] = (byte) 0xFF;
		}
		this.password = defaultPwd;
	}




	public boolean execSerialForwarder(String dstPort, int idx) throws UnknownHostException, IOException
	{
		if (myThread != null && myThread.isAlive())
		{
			textView.append("Error: a programming is currently running\n");
			return false;
		}
		Integer port = Integer.valueOf(dstPort);
		if (port != null)
		{
			// IOHandler.doOutput("try starting ServerSocket Service");
			SocketService.setContext(context);
			SocketService.setInterface(ftdiInterface.get(idx));
			Intent mySocketIntent = new Intent(context, ServerSocket.class);
			context.startService(mySocketIntent);
			return false;
		}
		return false;
	}




	/**
	 * Executes a mass erase command, which erases the entire flash memory area. Invokes
	 * execution thread.
	 * 
	 * @throws Exception
	 */
	public void execMassErase() throws Exception
	{
		if (myThread != null && myThread.isAlive())
		{
			textView.append("Error: a programming is currently running\n");
			return;
		}
		textView.append("exec mass erase\n");
		if (mDeviceConnection != null)
		{
			commandList.clear();
			commandList.add(new MSP430Command(MSP430_Commands.MASS_ERASE, MSP430PacketFactory.createMassEraseCommand()));
			startExecutionThread(context.getCheckedItems());
		}
		else
			throw new Exception("No Connection available");
	}




	/**
	 * Flashes the given Records. Performs a mass erase, transmit password, the flash and
	 * a load pc command.
	 * 
	 * @param file
	 * @throws Exception
	 */
	public void execFlash(ArrayList<Record> records) throws Exception
	{
		if (myThread != null && myThread.isAlive())
		{
			textView.append("Error: a programming is currently running\n");
			return;
		}
		textView.append("exec flash erase\n");

		if (mDeviceConnection != null)
		{
			commandList.clear();
			commandList.add(new MSP430Command(MSP430_Commands.MASS_ERASE));
			commandList.add(new MSP430Command(MSP430_Commands.TRANSMIT_PASSWORD, MSP430PacketFactory
					.createSetPasswordCommand(this.password)));
			// commandList.add(new MSP430Command(MSP430_Commands.TX_BSL_VERSION));
			// TODO request variant
			commandList.add(new MSP430Command(MSP430_Commands.CHANGE_BAUDRATE,
					FTDI232BM_Matching_MSP430_Baudrates.BAUDRATE_38400, MSP430Variant.MSP430_F161x));
			// commandList.add(new MSP430Command(MSP430_Commands.TRANSMIT_PASSWORD,
			// MSP430PacketFactory.createSetPasswordCommand(this.password)));
			commandList.add(new MSP430Command(MSP430_Commands.FLASH, records));
			commandList.add(new MSP430Command(MSP430_Commands.LOAD_PC, Record.getStartAddress(records)));
			startExecutionThread(context.getCheckedItems());
		}
		else
			throw new Exception("No Connection available");
	}




	private void startExecutionThread(long[] cbIndices)
	{
		textView.append("start exec thread for "+cbIndices.length+" motes\n");

		// iterate all indices where the checkbox is set
		for (int i = 0; i < cbIndices.length; i++) {
			int idx = (int)cbIndices[i];
			
			// device and connection set?
			if (mDeviceConnection.get(idx) == null)
			{
				textView.append("no connection available\n");
				return;
			}
	
			int productId = mDevice.get(idx).getProductId();
	
			// textView.append("productId: 0x"+Integer.toHexString(productId)+"\n");
			switch (productId)
			{
			// MSP430
			case 0x6001:
				try
				{
					textView.append("start execution\n");
					myThread = new ProgrammerThreadMSP430(commandList, ftdiInterface.get(idx));
					myThread.setContext(this.context);
					myThread.start();
				} catch (Exception e)
				{
					textView.append("Error: SendReceiverThreadMSP430! - " + e.getMessage() + "\n");
				}
				break;
			default:
				textView.append("Can't find corresponding product id! \n");
				break;
			}
		}
		return;
	}




	/**
	 * Sets global device, endpoints, opens connection to MSP430.
	 * 
	 * @param device
	 */
	public void connectDevice(UsbDevice device)
	{
		
		//textView.append("setDevice " + device+"\n");
		if (device.getInterfaceCount() != 1)
		{
			textView.append("Could not find interface!\n");
			return;
		}
		
		mUSBInterface = device.getInterface(0);

		// FTDI has 2 endpoints
		if (mUSBInterface.getEndpointCount() != 2)
		{
			textView.append("Can't find all endpoints!\n");
			return;
		}
		textView.append("interfaceId: " + mUSBInterface.getId() + "\n");

		// endpoint should be of type HID
		UsbEndpoint endpoint0 = mUSBInterface.getEndpoint(0);
		UsbEndpoint endpoint1 = mUSBInterface.getEndpoint(1);

		if (endpoint0.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK)
		{
			textView.append("type: " + endpoint0.getType() + " endpoint 1 has not correct type\n");
			return;
		}
		if (endpoint1.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK)
		{
			textView.append("endpoint 1 has not correct type\n");
			return;
		}

		// open connection
		if (device != null)
		{
			// set device and endpoints
			// HOST to device
			if (endpoint1.getDirection() == UsbConstants.USB_DIR_OUT)
			{
				sendingEndpointMSP430.add(endpoint1);
			}
			else
			{
				textView.append("Wrong endpoint 0 direction!\n");
				return;
			}
			// device to HOST
			if (endpoint0.getDirection() == UsbConstants.USB_DIR_IN)
			{
				receivingEndpointMSP430.add(endpoint0);
			}
			else
			{
				// in case of failure remove the last item
				int size = sendingEndpointMSP430.size();
				sendingEndpointMSP430.remove(size-1);
				textView.append("Wrong endpoint 1 direction!\n");
				return;
			}
			
			mDevice.add(device);
			UsbDeviceConnection connection = mManager.openDevice(device);

			// get exclusive access to device
			if (connection != null && connection.claimInterface(mUSBInterface, true))
			{
				textView.append("open SUCCESS\n");
				mDeviceConnection.add(connection);

				// set up a ftdi communication
				FTDI_Interface curInterface = new FTDI_Interface(connection, endpoint1, endpoint0);
				curInterface.resetUsb();
				curInterface.setBaudrate();
				
				ftdiInterface.add(curInterface);
			}
			else
			{
				textView.append("open FAIL");
				mDeviceConnection = null;
			}
		}
	}




	/**
	 * @return the password
	 */
	public byte[] getPassword()
	{
		return password;
	}




	/**
	 * @param password the password to set
	 */
	public void setPassword(byte[] password)
	{
		this.password = password;
	}




	public void execGetBslVersion() throws Exception
	{
		if (myThread != null && myThread.isAlive())
		{
			textView.append("Error: a programming is currently running\n");
			return;
		}
		textView.append("exec getBslVersion\n");
		if (mDeviceConnection != null)
		{
			commandList.clear();
			commandList.add(new MSP430Command(MSP430_Commands.TX_BSL_VERSION));
			startExecutionThread(context.getCheckedItems());
		}
		else
			throw new Exception("No Connection available");
	}




	public synchronized void setDeviceVariant(MSP430Variant variant)
	{
		this.deviceVariant = variant;
	}
}
