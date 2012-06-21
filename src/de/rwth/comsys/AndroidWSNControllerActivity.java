package de.rwth.comsys;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import java.io.*;

import de.rwth.comsys.Enums.FTDI_Constants;

public class AndroidWSNControllerActivity extends Activity {

	private UsbManager mManager = null;
	private UsbDevice mDevice = null;
	private UsbDeviceConnection mDeviceConnection = null;
	private UsbEndpoint sendingEndpointMSP430 = null; // view of HOST
	private UsbEndpoint receivingEndpointMSP430 = null;	//view of HOST
	private TextView textView = null;
	private PendingIntent mPermissionIntent = null;
	UsbInterface mUSBInterface = null;
	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
				// get button and register listener
		Button myButtonConnect = (Button) findViewById(R.id.button);
		myButtonConnect.setOnClickListener(buttonConnectListener);
		Button myButtonSend = (Button) findViewById(R.id.button1);
		myButtonSend.setOnClickListener(buttonSendListener);
		textView =   (TextView) findViewById(R.id.textView);
		
		// retrieve USB Service
		mManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		
		
		// listen for new devices
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
	}

	
	// OnClickListener iterates over connected devices and requests permission
	private OnClickListener buttonConnectListener = new OnClickListener() {
		public void onClick(View v) {
			
			HashMap<String, UsbDevice> deviceList = mManager.getDeviceList();

			
			if (deviceList.isEmpty())
				textView.append("Nothing found! \n");

			Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
			while (deviceIterator.hasNext()) 
			{
				mDevice = deviceIterator.next();
				mManager.requestPermission(mDevice, mPermissionIntent);
				textView.append("device: "+mDevice.getDeviceName()+" found\n");
			}
		}
	};
	
	// OnClickListener sends a packet to mDevice
	private OnClickListener buttonSendListener = new OnClickListener() {
		public void onClick(View v) {
			
			if(mDeviceConnection != null){
				
				resetTelosb(true);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				byte[] startSeq = new byte[1];
				startSeq[0] = (byte) 0x80;
				//startSeq[1] = (byte) 0x01;
				//startSeq[2] = (byte) 0x80;
				
				ByteBuffer startSeqRespBuf = ByteBuffer.allocate(2);
				UsbRequest startSeqResponse = new UsbRequest();
				startSeqResponse.initialize(mDeviceConnection, receivingEndpointMSP430);
				startSeqResponse.queue(startSeqRespBuf, 2);
				
				mDeviceConnection.bulkTransfer(sendingEndpointMSP430, startSeq, 1, 200);
				textView.append("sent1: 0x"+Integer.toHexString((byte)startSeq[0])+"\n");
				//textView.append("sent2: 0x"+Integer.toHexString((byte)startSeq[1])+"\n");
				//textView.append("sent3: 0x"+Integer.toHexString((byte)startSeq[2])+"\n");
				ByteConverter.bla(textView);
				if (mDeviceConnection.requestWait() == startSeqResponse) {
					textView.append("orig result: \n");
					textView.append("1: 0x"+Integer.toHexString(startSeqRespBuf.get(0))+"\n");
					textView.append("2: 0x"+Integer.toHexString(((short)startSeqRespBuf.get(1)) & 0x00FF)+"\n");
					//textView.append("3: 0x"+Integer.toHexString(((short)startSeqRespBuf.get(2)) & 0x00FF)+"\n");
					short[] res = ByteConverter.getByteStreamFromSigned(startSeqRespBuf);
					textView.append("result init: 0x"+Integer.toHexString(res[0])+"\n");
					textView.append("res: "+String.valueOf(res[0]));
					/*for(int j = 0; j< startSeqRespBuf.capacity(); j++){
						textView.append(startSeqRespBuf.get()+"\n");
					}*/
				}

			/*	ByteBuffer buffer = ByteBuffer.allocate(2);
		        UsbRequest response = new UsbRequest();
		        response.initialize(mDeviceConnection, receivingEndpointMSP430);
		        response.queue(buffer, 2);
		        // Request to mass erase
		        ByteBuffer massEraseBuf = getMassEraseCommand();
				mDeviceConnection.bulkTransfer(sendingEndpointMSP430, massEraseBuf.array(), massEraseBuf.capacity(), 200); //do in another thread
				if (mDeviceConnection.requestWait() == response) {
					
					textView.append("MassEraseCmd("+massEraseBuf.capacity()+"):\n");
					for(int j = 0; j< massEraseBuf.capacity(); j++){
						textView.append("0x"+Integer.toHexString(massEraseBuf.get(j))+",");
					}
				}
				
				short[] res = ByteConverter.getByteStreamFromSigned(buffer);
				
				textView.append("\nMassEraseResponse("+res.length+"):\n");
				for(int i=0;i<res.length;i++)
				{
					textView.append("0x"+Integer.toHexString(res[i])+",");
				}
				*/
				/*
				 *  set password cmd to unlock pw protected commands
				 */
				
				/*ByteBuffer buffer2 = ByteBuffer.allocate(32);
		        UsbRequest response2 = new UsbRequest();
		        response.initialize(mDeviceConnection, receivingEndpointMSP430);
		        response.queue(buffer2, 32);
		        // Request to mass erase
		        ByteBuffer readPW = getReceivePasswordCommand();
				mDeviceConnection.bulkTransfer(sendingEndpointMSP430, readPW.array(), readPW.capacity(), 200); //do in another thread
				if (mDeviceConnection.requestWait() == response2) {
					
					textView.append("MassEraseCmd("+readPW.capacity()+"):\n");
					for(int j = 0; j< readPW.capacity(); j++){
						textView.append("0x"+Integer.toHexString(readPW.get(j))+",");
					}
				}
				
				short[] res2 = ByteConverter.getByteStreamFromSigned(buffer2);
				
				textView.append("\ngetPasswordResp:("+res2.length+"):\n");
				for(int i=0;i<res2.length;i++)
				{
					textView.append("0x"+Integer.toHexString(res2[i])+",");
				}*/
				
				/*
				 *  read bsl version
				 */				
				/*
				ByteBuffer buffer3 = ByteBuffer.allocate(32);
		        UsbRequest response3 = new UsbRequest();
		        response3.initialize(mDeviceConnection, receivingEndpointMSP430);
		        response3.queue(buffer3, 32);
		        // Request to mass erase
		        ByteBuffer readVersion = getTransmitBslVersionCmd();
				mDeviceConnection.bulkTransfer(sendingEndpointMSP430, readVersion.array(), readVersion.capacity(), 200); //do in another thread
			
				
				if (mDeviceConnection.requestWait() == response3) {
					
					textView.append("read version cmd("+readVersion.capacity()+"):\n");
					for(int j = 0; j< readVersion.capacity(); j++){
						textView.append("0x"+Integer.toHexString(readVersion.get(j))+",");
					}
				}
				
				short[] res3 = ByteConverter.getByteStreamFromSigned(buffer3);
				
				textView.append("\nreadVerionsResp:("+res3.length+"):\n");
				for(int i=0;i<res3.length;i++)
				{
					textView.append("0x"+Integer.toHexString(res3[i])+",");
				}*/
			}
		}
	};
	
	private ByteBuffer getMassEraseCommand()
	{
		short[] bytesUSB = new short[11];
		
		short HEADER = 0x80;
		short CMD = 0x02; //bsl version; 0x18 mass erase
		short L1  = 0x04;
		short L2  = 0x04;
		short AL  = 0x00;
		short AH  = 0xFF; // means every 
		short LL  = 0x06;
		short LH  = 0xA5;
		short CKL = 0x18 ^ 0x04 ^ 0x04 ^ 0xFF ^ 0x06;
		short CKH = 0x04 ^ 0x04 ^ 0xFF ^ 0x06 ^ 0xA5;
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
	
	/*
	 * Invokes SendReceiverThread by Device.
	 * @return SendReceiverThread successfully started
	 */
	private boolean sendCommand() {
			Thread thread = null;
			
			// device and connection set?
			if(mDeviceConnection != null){
				return false;
			}
			
			int productId = mDevice.getProductId();
			
			
			switch (productId){
				
				// MSP430
				case 200: 
					try{
						thread = new Thread(new SendReceiverThreadMSP430(mDevice, mDeviceConnection, sendingEndpointMSP430, receivingEndpointMSP430 ));
						thread.run();
						return true;
					}catch(Exception e){
						textView.append("Can't start SendReceiverThreadMSP430!\n");
					}
					break;
				default: 
					textView.append("Can't find corresponding SendReceiverThread by product id! \n");
					break;
					
			}
			return false;
			
	}
	
	
	/**
	 * Sets global device, endpoints, opens connection to MSP430.
	 * @param device
	 */
	private void connectDeviceMSP430(UsbDevice device) {

		//textView.append("setDevice " + device);
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
		
		
//		textView.append("\n Endpoint 0: "+"\n" +endpoint0+"\n Type: "+endpoint0.getType() +"\n");
//		textView.append("\n Endpoint 1: "+"\n" +endpoint1+"\n Type: "+endpoint1.getType() +"\n");
		
		if (endpoint0.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) {
			textView.append("endpoint 0 has not correct type type\n");
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

			} else {
				textView.append("open FAIL");
				mDeviceConnection = null;
			}
		}
	}
	
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
	{
		    public void onReceive(Context context, Intent intent)
		    {
		        String action = intent.getAction();
		        if (ACTION_USB_PERMISSION.equals(action)) {
		            synchronized (this) {
		                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

		                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
		                    if(device != null){
		                      //call method to set up device communication
		                    	connectDeviceMSP430(mDevice);
		                   }
		                } 
		            }
		        }
		    }
		};
	

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		// TODO check connection
		// reset view
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// TODO release device
	}
	
	private void resetTelosb(boolean invokeBsl) {
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
	private int setDTR(boolean dtr)
	{
		int iDtr = FTDI_Constants.SIO_SET_DTR_LOW;
		if(dtr == true)
			iDtr = FTDI_Constants.SIO_SET_DTR_HIGH;
		switch(iDtr)
		{
		case FTDI_Constants.SIO_SET_DTR_HIGH:
		case FTDI_Constants.SIO_SET_DTR_LOW:
			break;
		default:
			Log.e("ftdi_control","The DTR value can only be SIO_SET_DTR_HIGH or SIO_SET_DTR_LOW: "+ Integer.toString(iDtr));
			return -2;
		}
		int r;
		if((r = mDeviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE, 
													 FTDI_Constants.SIO_SET_MODEM_CTRL_REQUEST,
													 iDtr,
													 mUSBInterface.getId(),
													 null,
													 0, 
													 2000)) != 0)
		{
			Log.e("ftdi_control","USB controlTransfer operation failed. controlTransfer return value is:"+Integer.toString(r));
			//textView.append(Integer.toString(-1)+",");
			return -1;
		}
		else
		{
			//textView.append(Integer.toString(0)+",");
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
	private int setRTS(boolean rts)
	{
		int iRts = FTDI_Constants.SIO_SET_RTS_LOW;
		if(rts == true)
			iRts = FTDI_Constants.SIO_SET_RTS_HIGH;
		switch(iRts)
		{
		case FTDI_Constants.SIO_SET_RTS_HIGH:
		case FTDI_Constants.SIO_SET_RTS_LOW:
			break;
		default:
			Log.e("ftdi_control","The RTS value can only be SIO_SET_RTS_HIGH or SIO_SET_RTS_LOW: "+ Integer.toString(iRts));
			return -2;
		}
		int r;
		if((r = mDeviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE, 
												  FTDI_Constants.SIO_SET_MODEM_CTRL_REQUEST,
												  iRts, mUSBInterface.getId(), null, 0, 2000)) != 0)
		{
			//textView.append(Integer.toString(-1)+",");
			Log.e("ftdi_control","USB controlTransfer operation failed. controlTransfer return value is:"+Integer.toString(r));
			return -1;
		}
		else
		{
			//textView.append(Integer.toString(0)+",");
			return 0;
		}
		
	}

	private void telosWriteBit(boolean bit)
	{
		try
		{
			setRTS(true);
			setDTR(!bit);
			Thread.sleep(0,002);
			setRTS(false);
			Thread.sleep(0,001);
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
}