package de.rwth.comsys;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import de.rwth.comsys.enums.FTDI232BM_Matching_MSP430_Baudrates;
import de.rwth.comsys.enums.MSP430Variant;
import de.rwth.comsys.enums.MSP430_Commands;
import de.rwth.comsys.helpers.IOHandler;
import de.rwth.comsys.ihex.Record;

/**
 * Used to set up an Android communication with a TelosB mote with a FT232BM USB UART converting chip 
 * and a MSP430 microcontroller. 
 * Provides several commands which can be executed.
 * 
 * @author Christian & Stepahn
 * 
 */
public class TelosBConnector
{
	private UsbInterface mUSBInterface;
	private ArrayList<UsbDevice> mDevice;
	private ArrayList<UsbEndpoint> sendingEndpointMSP430;
	private ArrayList<UsbEndpoint> receivingEndpointMSP430;
	private ArrayList<UsbDeviceConnection> mDeviceConnection;
	private SocketServiceConnection serviceConnection;
	private static ArrayList<FTDI_Interface> ftdiInterface;
	private UsbManager mManager;
//private ArrayList<MSP430Command> commandList;
	private byte[] password;
	private final int PASSWORD_LENGTH = 32;
	private MSP430Variant deviceVariant = null;
	private Activity context;
	ArrayList<ProgrammerThreadMSP430> myThread = null;
	long[] checkedItems;

	/**
	 * Sets the parentActivity as the context and allows communications over the usbManager
	 * @param usbManager
	 * @param parentActivity
	 */
	public TelosBConnector(UsbManager usbManager, Activity parentActivity)
	{
		if (usbManager == null)
			throw new IllegalArgumentException("Error: usbManager is null");
		this.mManager = usbManager;
		this.context = parentActivity;
//		this.commandList = new ArrayList<MSP430Command>();
		//this.textView = context.getOutputTextView();
		this.mDevice = new ArrayList<UsbDevice>();
		this.sendingEndpointMSP430 = new ArrayList<UsbEndpoint>();
		this.receivingEndpointMSP430 = new ArrayList<UsbEndpoint>();
		this.mDeviceConnection = new ArrayList<UsbDeviceConnection>() ;
		ftdiInterface = new ArrayList<FTDI_Interface>();
		this.myThread = new ArrayList<ProgrammerThreadMSP430>();
		SocketServiceConnection.setContext(context);

		// set the default password
		setDefaultPassword();
	}
	
	/**
	 * resets the default password to 32 times 0xFF
	 */
	private void setDefaultPassword()
	{
		// set the default password
		byte[] defaultPwd = new byte[PASSWORD_LENGTH];
		for (int i = 0; i < PASSWORD_LENGTH; i++)
		{
			defaultPwd[i] = (byte) 0xFF;
		}
		this.password = defaultPwd;
	}
	
	/**
	 * resets the TelosBConnector
	 * this is necessary when the activity returns from the background
	 */
	public void clear()
	{
		this.mDevice = new ArrayList<UsbDevice>();
		this.sendingEndpointMSP430 = new ArrayList<UsbEndpoint>();
		this.receivingEndpointMSP430 = new ArrayList<UsbEndpoint>();
		this.mDeviceConnection = new ArrayList<UsbDeviceConnection>() ;
		ftdiInterface = new ArrayList<FTDI_Interface>();
		this.myThread = new ArrayList<ProgrammerThreadMSP430>();
		
		// set the default password
		setDefaultPassword();
	}

	/**
	 * checks if the specified service is still running
	 * @param serviceName
	 * @return true if running, false otherwise
	 */
	private boolean isMyServiceRunning(String serviceName) {
	    ActivityManager manager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (serviceName.equals(service.service.getClassName())) {
	        	IOHandler.doOutput("service running: "+service.service.toString());
	            return true;
	        }
	    }
	    return false;
	}

	/**
	 * Starts a SerialForwarder for the given mote specified by the index in the motelist by use of a SocketService
	 * 
	 * @param dstPort
	 * @param idx
	 * @return true if a serial forwarder was started successfully, false if an error occured or one was already running
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public boolean execSerialForwarder(String dstPort, int idx) throws UnknownHostException, IOException
	{
		//IOHandler.doOutput("execSerialForwarder\n");
		if ((myThread.size()-1) >= idx && myThread.get(idx) != null && myThread.get(idx).isAlive())
		{
			IOHandler.doOutput("Error: a programming is currently running\n");
			return false;
		}
		
		/**
		 * TODO There is still a listindex bug starting the SF
		 * because a wrong association is made from checked list indices and not correctly from all indices
		 */
		if(!SocketService.running)
		{
			IOHandler.doOutput("socket service not running");
			SocketService.setContext(context);
			
			Intent mySocketIntent = new Intent(context,SocketService.class);
			
			// starts the created SocketService
			ComponentName serviceName = context.startService(mySocketIntent);
			if(serviceName != null)
			{
				SocketServiceConnection conn = new SocketServiceConnection();
				serviceConnection = conn;
				context.bindService(mySocketIntent, conn, Context.BIND_AUTO_CREATE);
				
				SocketService.running = true;
			}
		}
	
		// if the service is already running or the previous start of the service was successful
		// then create the serial forwarder
		if(SocketService.running)
		{
			Integer port = Integer.valueOf(dstPort);
			if (port != null && (ftdiInterface.size()-1 >= idx))
			{		
				IOHandler.doOutput("telosBConnector: startSerialForwarder");
				serviceConnection.startSerialForwarder(port,idx);
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Returns if a serial forwarder is running for a given mote
	 * @param idx, the mote index in the mote list
	 * @return true if running, false otherwise
	 */
	public boolean getSFState(int idx)
	{
		if(SocketService.running)
		{
			return serviceConnection.getSFState(idx);
		}
		return false;
	}
	
	/**
	 * Stops a serial forwarder for a given mote
	 * 
	 * @param idx, the index of the mote in the motelist
	 * @return true if successfully sent the stop command, false if no forwarder was running for the given mote
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public boolean execStopSerialForwarder(int idx) throws UnknownHostException, IOException
	{
		if ((myThread.size()-1) >= idx && myThread.get(idx) != null && myThread.get(idx).isAlive())
		{
			IOHandler.doOutput("Error: a programming is currently running\n");
			return false;
		}
		
		serviceConnection.stopSocket(idx);
		return true;
	}


	/**
	 * Executes a mass erase command, which erases the entire flash memory area. Invokes
	 * execution thread.
	 * 
	 * @throws Exception
	 */
	public void execMassErase(long[] indeces) throws Exception
	{
		
		for (int i = 0; i < (int)indeces.length; i++) {
			int nodeIndex = (int)indeces[i];
			
			if (mDeviceConnection.get(nodeIndex) != null)
			{
				ArrayList<MSP430Command> commands = new ArrayList<MSP430Command>();
				commands.clear();
				commands.add(new MSP430Command(MSP430_Commands.MASS_ERASE, MSP430PacketFactory.createMassEraseCommand()));
				startExecutionThread(commands,nodeIndex);
			}
			else
				throw new Exception("No Connection available");
		}
	}

	/**
	 * Flashes the given Records. Performs a mass erase, transmit password, the flash and
	 * a load pc command.
	 * 
	 * @param file
	 * @throws Exception
	 */
	public void execFlash(HashMap<Integer,FlashMapping> flashData) throws Exception
	{
		Log.w("FLASHING","start flash");
		
		for (Map.Entry<Integer,FlashMapping> entry : flashData.entrySet()) {
			ArrayList<MSP430Command> commands = new ArrayList<MSP430Command>();
			int nodeId = entry.getKey();
			FlashMapping nodeInfo = entry.getValue();
			int nodeIndex = nodeInfo.getInterfaceIndex();
			ArrayList<Record> nodeRecord = nodeInfo.getRecords();
			Log.w("FLASHING","start for index: "+nodeIndex+ " nodeId: "+nodeId);
			commands.clear();
			commands.add(new MSP430Command(MSP430_Commands.MASS_ERASE));
			commands.add(new MSP430Command(MSP430_Commands.TRANSMIT_PASSWORD, MSP430PacketFactory
					.createSetPasswordCommand(this.password)));
			// commandList.add(new MSP430Command(MSP430_Commands.TX_BSL_VERSION));
			// TODO request variant
			commands.add(new MSP430Command(MSP430_Commands.CHANGE_BAUDRATE,
					FTDI232BM_Matching_MSP430_Baudrates.BAUDRATE_38400, MSP430Variant.MSP430_F161x));
			// commandList.add(new MSP430Command(MSP430_Commands.TRANSMIT_PASSWORD,
			// MSP430PacketFactory.createSetPasswordCommand(this.password)));
			commands.add(new MSP430Command(MSP430_Commands.FLASH, nodeRecord));
			commands.add(new MSP430Command(MSP430_Commands.LOAD_PC, Record.getStartAddress(nodeRecord)));
			Log.w("FLASHING","start execution thread");
			startExecutionThread(commands,nodeIndex);
		}
	}


	/**
	 * internal method to start a ProgrammerThread executing a list of commands for a certain mote
	 * @param commands
	 * @param nodeIndex
	 */
	private void startExecutionThread(ArrayList<MSP430Command> commands,int nodeIndex)
	{
		// iterate all indices where the checkbox is set
		Log.e("FLASHING","device connections: "+mDeviceConnection.size());	
		// device and connection set?
		if (mDeviceConnection.get(nodeIndex) == null)
		{
			Log.e("FLASHING","deviceConnection 0");
			IOHandler.doOutput("no connection available\n");
			return;
		}

		int productId = mDevice.get(nodeIndex).getProductId();

		// IOHandler.doOutput("productId: 0x"+Integer.toHexString(productId)+"\n");
		switch (productId)
		{
		// MSP430
		case 0x6001:
			try
			{
				IOHandler.doOutput("start execution\n");
				Log.w("FLASHING","before starting execution thread");
				ProgrammerThreadMSP430 newThread = new ProgrammerThreadMSP430(commands, ftdiInterface.get(nodeIndex));
				newThread.setContext(this.context);
				myThread.add(newThread);
				newThread.start();
			} catch (Exception e)
			{
				IOHandler.doOutput("Error: SendReceiverThreadMSP430! - " + e.getMessage() + "\n");
			}
			break;
		default:
			Log.e("FLASHING","nod product id found");
			IOHandler.doOutput("Can't find corresponding product id! \n");
			break;
		}
	}

	/**
	 * Sets global device, endpoints, opens connection to MSP430.
	 * 
	 * @param device
	 */
	public void connectDevice(UsbDevice device)
	{
		
		//Log.w("FLASHING","setDevice " + device+"\n");
		if (device.getInterfaceCount() != 1)
		{
			Log.w("FLASHING","Could not find interface!\n");
			return;
		}
		
		mUSBInterface = device.getInterface(0);

		// FTDI has 2 endpoints
		if (mUSBInterface.getEndpointCount() != 2)
		{
			Log.w("FLASHING","Can't find all endpoints!\n");
			return;
		}
		Log.w("FLASHING","interfaceId: " + mUSBInterface.getId() + "\n");

		// endpoint should be of type HID
		UsbEndpoint endpoint0 = mUSBInterface.getEndpoint(0);
		UsbEndpoint endpoint1 = mUSBInterface.getEndpoint(1);

		if (endpoint0.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK)
		{
			Log.w("FLASHING","type: " + endpoint0.getType() + " endpoint 1 has not correct type\n");
			return;
		}
		if (endpoint1.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK)
		{
			Log.w("FLASHING","endpoint 1 has not correct type\n");
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
				Log.w("FLASHING","Wrong endpoint 0 direction!\n");
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
				Log.w("FLASHING","Wrong endpoint 1 direction!\n");
				return;
			}
			
			mDevice.add(device);
			UsbDeviceConnection connection = mManager.openDevice(device);

			// get exclusive access to device
			if (connection != null && connection.claimInterface(mUSBInterface, true))
			{
				Log.w("FLASHING","open SUCCESS\n");
				mDeviceConnection.add(connection);

				// set up a ftdi communication
				FTDI_Interface curInterface = new FTDI_Interface(connection, endpoint1, endpoint0);
				curInterface.resetUsb();
				curInterface.setBaudrate();
				
				ftdiInterface.add(curInterface);
			}
			else
			{
				Log.w("FLASHING","open FAIL");
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

	/**
	 * currently not implemented
	 * @throws Exception
	 */
	public void execGetBslVersion() throws Exception
	{
		/*
		long[] checkedItems = context.getCheckedItems();
		ArrayList<Long> itemsToFlash = new ArrayList<Long>();
		for (long itemIdx : checkedItems) {
			if (!(myThread.get((int)itemIdx) != null && myThread.get((int)itemIdx).isAlive()))
			{
				itemsToFlash.add(itemIdx);				
			}
		}
		long[] itemAry = new long[itemsToFlash.size()];
		
		for (int i=0;i<itemsToFlash.size();i++) {
			itemAry[i] = itemsToFlash.get(i);
		}
		
		if (mDeviceConnection != null)
		{
			commandList.clear();
			commandList.add(new MSP430Command(MSP430_Commands.TX_BSL_VERSION));
			startExecutionThread(itemAry);
		}
		else
			throw new Exception("No Connection available");
			*/
	}

	/**
	 * gets called from the FTDI_Interface if the device variant was read out of the MSP430 chip
	 * @param variant
	 */
	public synchronized void setDeviceVariant(MSP430Variant variant)
	{
		this.deviceVariant = variant;
	}
	
	/**
	 * returns the interface for a certain mote
	 * @param idx, index of a mote specified by its position in the mote list
	 * @return
	 */
	public static synchronized FTDI_Interface getInterfaceByIdx(int idx)
	{
		return ftdiInterface.get(idx);
	}
}
