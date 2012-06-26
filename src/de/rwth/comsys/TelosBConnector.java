package de.rwth.comsys;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import de.rwth.comsys.Enums.BSLCoreCommandsMSP430;
import de.rwth.comsys.Enums.FTDI_Constants;
import de.rwth.comsys.Enums.MSP430_Command;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.TextView;

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
	public TelosBConnector(UsbManager usbManager,TextView tv)
	{
		if(usbManager == null)
			throw new IllegalArgumentException("Error: usbManager is null");
		this.mManager = usbManager;
		this.textView = tv;
		this.commandList = new ArrayList<MSP430Command>();
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
	
	private void startExecutionThread()
	{
		Thread thread = null;
		
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
					myThread.setTextView(textView);
					thread = new Thread(myThread);
					thread.run();
					return;
				}catch(Exception e){
					textView.append("Can't start SendReceiverThreadMSP430!\n");
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
	// 80 10 24 24 xx xx xx xx D1 D2 … D20 CKL CKH ACK
	private ByteBuffer getReceivePasswordCommand()
	{
		short[] bytesUSB = new short[11];
		
		short HEADER = 0x3F;
		short CMD = 0x10; //get password
		short L1  = 0x24;
		short L2  = 0x24;
		short AL  = 0x00;
		short AH  = 0x00; 
		short LL  = 0x00;
		short LH  = 0x00;
		short CKL = 0x10 ^ 0x24 ^ 0x24;
		short CKH = 0x24 ^ 0x24;
		short ACK = 0x90;
		
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
	


}
