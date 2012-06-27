package de.rwth.comsys;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.widget.TextView;
import de.rwth.comsys.Enums.MSP430_Command;

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
	private AndroidWSNControllerActivity context;
	
	public TelosBConnector(UsbManager usbManager,AndroidWSNControllerActivity parentActivity)
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
	
	public void execMassErase() throws Exception
	{
		textView.append("exec mass erase\n");
		if(mDeviceConnection != null)
		{
			commandList.clear();
			commandList.add(new MSP430Command(MSP430_Command.MASS_ERASE,getMassEraseCommand()));
			startExecutionThread();
		}
		else
			throw new Exception("No Connection available");
	}
	
	public void execFlash(String file) throws Exception
	{
		textView.append("exec flash erase\n");
		/*FileInputStream filein = context.openFileInput(file);
		int available = filein.available();
		byte[] data = new byte[available];
		int readBytes = filein.read(data);*/
		
		if(mDeviceConnection != null)
		{
			commandList.clear();
			commandList.add(new MSP430Command(MSP430_Command.MASS_ERASE,getMassEraseCommand()));
			commandList.add(new MSP430Command(MSP430_Command.TRANSMIT_PASSWORD, getReceivePasswordCommand(this.password)));
			//commandList.add(new MSP430Command(MSP430_Command.FLASH, data, (short)0, (short)0));
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
		
		textView.append("productId: 0x"+Integer.toHexString(productId)+"\n");
		switch (productId){
			// MSP430
			case 0x6001: 
				try{
					textView.append("start execution\n");
					SendReceiverThreadMSP430 myThread = new SendReceiverThreadMSP430( commandList,ftdiInterface );
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
	
	private byte[] getMassEraseCommand()
	{
		byte[] bytesUSB = new byte[11];
		
		short HEADER = 0x80;
		short CMD = 0x18; //bsl version; 0x18 mass erase
		short L1  = 0x04;
		short L2  = 0x04;
		short AL  = 0x00;
		short AH  = 0xFF; // means every 
		short LL  = 0x06;
		short LH  = 0xA5;
		short CKL = ~(0x80 ^ 0x04 ^ 0x00 ^ 0x06);
		short CKH = ~(0x18 ^ 0x04 ^ 0x0FF^ 0xA5);
		short ACK = 0x90;
		
		bytesUSB[0] = (byte)(HEADER & 0xFF); 
		bytesUSB[1] = (byte)(CMD & 0xFF);
		bytesUSB[2] = (byte)(L1 & 0xFF);
		bytesUSB[3] = (byte)(L2 & 0xFF);
		bytesUSB[4] = (byte)(AL & 0xFF);
		bytesUSB[5] = (byte)(AH & 0xFF);
		bytesUSB[6] = (byte)(LL & 0xFF);
		bytesUSB[7] = (byte)(LH & 0xFF);
		bytesUSB[8] = (byte)(CKL & 0xFF);
		bytesUSB[9] = (byte)(CKH & 0xFF);
		bytesUSB[10] = (byte)(ACK & 0xFF);
		
		return bytesUSB;
	}
	
	/**
	 * Sets global device, endpoints, opens connection to MSP430.
	 * @param device
	 */
	public void connectDevice(UsbDevice device) 
	{
		textView.append("setDevice " + device+"\n");
		if (device.getInterfaceCount() != 1) {
			textView.append("Could not find interface!\n");
			return;
		}

		mUSBInterface = device.getInterface(0);

		// MSP430 has 2 endpoints (SLAU319B.pdf, paragraph 1.5)
		if (mUSBInterface.getEndpointCount() != 2) {
			textView.append("Can't find all endpoints!\n");
			return;
		}
		textView.append("interfaceId: "+mUSBInterface.getId()+"\n");

		
		// endpoint should be of type HID
		UsbEndpoint endpoint0 = mUSBInterface.getEndpoint(0);
		UsbEndpoint endpoint1 = mUSBInterface.getEndpoint(1);
		
		if (endpoint0.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) 
		{
			textView.append("type: "+endpoint0.getType()+" endpoint 1 has not correct type\n");
			return;
		}
		if (endpoint1.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) {
			textView.append("endpoint 1 has not correct type\n");
			return;
		}

		// set device and endpoints
		mDevice = device;
		
		// HOST to device
		if(endpoint1.getDirection() == UsbConstants.USB_DIR_OUT){
			sendingEndpointMSP430 = endpoint1;
		}else{
			textView.append("Wrong endpoint 0 direction!\n");
			return;
		}
		// device to HOST
		if(endpoint0.getDirection() == UsbConstants.USB_DIR_IN){
			receivingEndpointMSP430 = endpoint0;
		}else{
			textView.append("Wrong endpoint 1 direction!\n");
			return;
		}
		
		
		// open connection
		if (device != null) {
			
			UsbDeviceConnection connection = mManager.openDevice(device);
			
			// get exclusive access to device
			if (connection != null && connection.claimInterface(mUSBInterface, true)) {
				textView.append("open SUCCESS\n");
				mDeviceConnection = connection;
				
				ftdiInterface = new FTDI_Interface(mDeviceConnection,sendingEndpointMSP430,receivingEndpointMSP430);
				ftdiInterface.resetUsb();
				ftdiInterface.setBaudrate();

			} else {
				textView.append("open FAIL");
				mDeviceConnection = null;
			}
		}
	}
	// 8 + 32 + 2
	// 80 10 24 24 xx xx xx xx D1 D2 … D20 CKL CKH ACK
	private byte[] getReceivePasswordCommand(byte[] password)
	{
		byte[] bytesUSB = new byte[11+PASSWORD_LENGTH];
		
		short HEADER = 0x80;
		short CMD = 0x10; //get password
		short L1  = 0x24;
		short L2  = 0x24;
		short AL  = 0x00;
		short AH  = 0x00; 
		short LL  = 0x00;
		short LH  = 0x00;
		short CKL = 0x80 ^ 0x24;
		short CKH = 0x10 ^ 0x24;
		short ACK = 0x90;
		
		bytesUSB[0] = (byte)HEADER; 
		bytesUSB[1] = (byte)CMD;
		bytesUSB[2] = (byte)L1;
		bytesUSB[3] = (byte)L2;
		bytesUSB[4] = (byte)AL;
		bytesUSB[5] = (byte)AH;
		bytesUSB[6] = (byte)LL;
		bytesUSB[7] = (byte)LH;
		
		for(int i=0;i<PASSWORD_LENGTH;i++)
		{
			byte curByte = password[i];
			if(i%2 == 0)
			{
				CKL ^= curByte;
			}
			else
			{
				CKH ^= curByte;
			}
			bytesUSB[8+i] = curByte;
		}
		bytesUSB[8+PASSWORD_LENGTH] = (byte)~CKL;
		bytesUSB[9+PASSWORD_LENGTH] = (byte)~CKH;
		bytesUSB[10+PASSWORD_LENGTH] = (byte)ACK;
		
		return bytesUSB;
	}
	
	//TX BSL version 80 1E 04 04 xx xx xx xx – – – – CKL CKH
	private ByteBuffer getTransmitBslVersionCmd()
	{
		short[] bytesUSB = new short[11];
		
		short HEADER = 0x3F;
		short CMD = 0x1E; //get bsl version
		short L1  = 0x04;
		short L2  = 0x04;
		short AL  = 0x00;
		short AH  = 0x00; 
		short LL  = 0x00;
		short LH  = 0x00;
		short CKL = 0x1E ^ 0x04 ^ 0x04;
		short CKH = 0x04 ^ 0x04;
		short ACK = 0x00;
		
		bytesUSB[0] = HEADER; 
		bytesUSB[1] = CMD;
		bytesUSB[2] = L1;
		bytesUSB[3] = L2;
		bytesUSB[4] = AL;
		bytesUSB[5] = AH;
		bytesUSB[6] = LL;
		bytesUSB[7] = LH;
		bytesUSB[8] = CKL;
		bytesUSB[9] = CKH;
		bytesUSB[10] = ACK;
		
		ByteBuffer byteBuf = ByteConverter.getByteBufferFromShort(bytesUSB);
		return byteBuf;
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

}
