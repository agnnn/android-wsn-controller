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
import de.rwth.comsys.Enums.FTDI232BM_Matching_MSP430_Baudrates;
import de.rwth.comsys.Enums.MSP430Variant;
import de.rwth.comsys.Enums.MSP430_Commands;

/**
 * Used to set up an Android communication with a Telosb.
 * Provides several commands which can be executed.
 * @author Christian & Stepahn
 *
 */
public class TelosBConnector {
	
	private TextView textView;
	private UsbInterface mUSBInterface;
	private UsbDevice mDevice;
	private UsbEndpoint sendingEndpointMSP430;
	private UsbEndpoint receivingEndpointMSP430;
	private UsbDeviceConnection mDeviceConnection;
	private FTDI_Interface ftdiInterface;
	private UsbManager mManager;
	private ArrayList<MSP430Command> commandList;
	private byte[] password;
	private final int PASSWORD_LENGTH = 32;
	private MSP430Variant deviceVariant = null;
	private AndroidWSNControllerActivity context;
	ProgrammerThreadMSP430 myThread = null;
	
	public TelosBConnector(UsbManager usbManager, AndroidWSNControllerActivity parentActivity)
	{
		if(usbManager == null)
			throw new IllegalArgumentException("Error: usbManager is null");
		this.mManager = usbManager;
		this.context = parentActivity;
		this.commandList = new ArrayList<MSP430Command>();
		this.textView = context.getOutputTextView();
		
		// set the default password
		byte[] defaultPwd = new byte[PASSWORD_LENGTH];
		for (int i=0;i<PASSWORD_LENGTH;i++) {
			defaultPwd[i] =  (byte)0xFF;
		}
		this.password = defaultPwd;
	}
	
	public boolean execSerialForwarder(String dstPort) throws UnknownHostException, IOException
	{
		if(myThread != null && myThread.isAlive())
		{
			textView.append("Error: a programming is currently running\n");
			return false;
		}
		Integer port = Integer.valueOf(dstPort);
		if(port != null)
		{
			Intent mySocketIntent = new Intent(context, SocketService.class);
			SocketService.setFtdiInterface(ftdiInterface);
			context.startService(mySocketIntent);
			return true;
		}
		return false;
	}
	
	/**
	 * Executes a mass erase command,
	 * which erases the entire flash memory area.
	 * Invokes execution thread.
	 * @throws Exception
	 */
	public void execMassErase() throws Exception
	{
		if(myThread != null && myThread.isAlive())
		{
			textView.append("Error: a programming is currently running\n");
			return;
		}
		textView.append("exec mass erase\n");
		if(mDeviceConnection != null)
		{
			commandList.clear();
			commandList.add(new MSP430Command(MSP430_Commands.MASS_ERASE,MSP430PacketFactory.createMassEraseCommand()));
			startExecutionThread();
		}
		else
			throw new Exception("No Connection available");
	}
	
	/**
	 * Flashes the given Records.
	 * Performs a mass erase, 
	 * transmit password, 
	 * the flash and a load pc command.
	 * @param file
	 * @throws Exception
	 */
	public void execFlash(ArrayList<Record> records) throws Exception
	{
		if(myThread != null && myThread.isAlive())
		{
			textView.append("Error: a programming is currently running\n");
			return;
		}
		textView.append("exec flash erase\n");
		
		if(mDeviceConnection != null)
		{
			commandList.clear();
			commandList.add(new MSP430Command(MSP430_Commands.MASS_ERASE));
			commandList.add(new MSP430Command(MSP430_Commands.TRANSMIT_PASSWORD, MSP430PacketFactory.createSetPasswordCommand(this.password)));
			//commandList.add(new MSP430Command(MSP430_Commands.TX_BSL_VERSION));
			//TODO request variant
			commandList.add(new MSP430Command(MSP430_Commands.CHANGE_BAUDRATE, FTDI232BM_Matching_MSP430_Baudrates.BAUDRATE_38400, MSP430Variant.MSP430_F161x));
			//commandList.add(new MSP430Command(MSP430_Commands.TRANSMIT_PASSWORD, MSP430PacketFactory.createSetPasswordCommand(this.password)));
			commandList.add(new MSP430Command(MSP430_Commands.FLASH, records));
			commandList.add(new MSP430Command(MSP430_Commands.LOAD_PC, Record.getStartAddress(records)));
			startExecutionThread();
		}
		else
			throw new Exception("No Connection available");
	}
	
	public void execChangeBaudrate() throws Exception
	{
		if(myThread != null && myThread.isAlive())
		{
			textView.append("Error: a programming is currently running\n");
			return;
		}
		textView.append("exec change baudrate\n");
		
		if(mDeviceConnection != null)
		{
			commandList.clear();
			commandList.add(new MSP430Command(MSP430_Commands.CHANGE_BAUDRATE, FTDI232BM_Matching_MSP430_Baudrates.BAUDRATE_38400, MSP430Variant.MSP430_F161x));
			// here a reset must be done....
			startExecutionThread();
		}
		else
			throw new Exception("No Connection available");
	}
	
	private void startExecutionThread()
	{
		textView.append("start exec thread\n");
		
		// device and connection set?
		if(mDeviceConnection == null){
			textView.append("no connection available\n");
			return;
		}
		
		int productId = mDevice.getProductId();
		
		//textView.append("productId: 0x"+Integer.toHexString(productId)+"\n");
		switch (productId){
			// MSP430
			case 0x6001: 
				try{
					textView.append("start execution\n");
					myThread = new ProgrammerThreadMSP430( commandList, ftdiInterface );
					myThread.setContext(this.context);
					myThread.start();
					return;
				}catch(Exception e){
					textView.append("Error: SendReceiverThreadMSP430! - "+e.getMessage()+"\n");
				}
				break;
			default: 
				textView.append("Can't find corresponding product id! \n");
				break;
		}
		return;
	}
	
	
	
	/**
	 * Sets global device, endpoints, opens connection to MSP430.
	 * @param device
	 */
	public boolean connectDevice(UsbDevice device) 
	{
		//textView.append("setDevice " + device+"\n");
		if (device.getInterfaceCount() != 1) {
			textView.append("Could not find interface!\n");
			return false;
		}

		mUSBInterface = device.getInterface(0);

		// FTDI  has 2 endpoints 
		if (mUSBInterface.getEndpointCount() != 2) {
			textView.append("Can't find all endpoints!\n");
			return false;
		}
		textView.append("interfaceId: "+mUSBInterface.getId()+"\n");

		
		// endpoint should be of type HID
		UsbEndpoint endpoint0 = mUSBInterface.getEndpoint(0);
		UsbEndpoint endpoint1 = mUSBInterface.getEndpoint(1);
		
		if (endpoint0.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) 
		{
			textView.append("type: "+endpoint0.getType()+" endpoint 1 has not correct type\n");
			return false;
		}
		if (endpoint1.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) {
			textView.append("endpoint 1 has not correct type\n");
			return false;
		}

		// set device and endpoints
		mDevice = device;
		
		// HOST to device
		if(endpoint1.getDirection() == UsbConstants.USB_DIR_OUT){
			sendingEndpointMSP430 = endpoint1;
		}else{
			textView.append("Wrong endpoint 0 direction!\n");
			return false;
		}
		// device to HOST
		if(endpoint0.getDirection() == UsbConstants.USB_DIR_IN){
			receivingEndpointMSP430 = endpoint0;
		}else{
			textView.append("Wrong endpoint 1 direction!\n");
			return false;
		}
		
		
		// open connection
		if (device != null) {
			
			UsbDeviceConnection connection = mManager.openDevice(device);
			
			// get exclusive access to device
			if (connection != null && connection.claimInterface(mUSBInterface, true)) {
				textView.append("open SUCCESS\n");
				mDeviceConnection = connection;
				
				//set up a ftdi communication
				ftdiInterface = new FTDI_Interface(mDeviceConnection, sendingEndpointMSP430, receivingEndpointMSP430);
				ftdiInterface.resetUsb();
				ftdiInterface.setBaudrate();
			} else {
				textView.append("open FAIL");
				mDeviceConnection = null;
			}
			return true;
		}
		return false;
	}
	
	
	/**
	 * @return the password
	 */
	public byte[] getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(byte[] password) {
		this.password = password;
	}

	public void execGetBslVersion() throws Exception {
		if(myThread != null && myThread.isAlive())
		{
			textView.append("Error: a programming is currently running\n");
			return;
		}
		textView.append("exec getBslVersion\n");
		if(mDeviceConnection != null)
		{
			commandList.clear();
			commandList.add(new MSP430Command(MSP430_Commands.TX_BSL_VERSION));
			startExecutionThread();
		}
		else
			throw new Exception("No Connection available");		
	}

	public synchronized void setDeviceVariant(MSP430Variant variant) {
		this.deviceVariant = variant;
	}
}