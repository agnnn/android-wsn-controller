package de.rwth.comsys;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
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
		textView.setMovementMethod(new ScrollingMovementMethod());
		
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
				
				//usbPurgeRXBuffer();
				//usbPurgeTXBuffer();
				//resetDevice();
				ByteConverter.bla(textView);
				ftdi_set_line_property(FTDI_Constants.DATA_BITS_8, FTDI_Constants.STOP_BITS_1,FTDI_Constants.PARITY_EVEN,FTDI_Constants.BREAK_OFF);
				int maxPacketSize = sendingEndpointMSP430.getMaxPacketSize();
				int attributes = sendingEndpointMSP430.getAttributes();
				textView.append("attribs: "+attributes+"\n");
				textView.append("packSize: "+maxPacketSize+"\n");
				
		        // Request to mass erase
		        
		       
		        int i=0;
		        boolean success = false;
		        
		        
		        while(!(success || i>5))
		        {
		        	sendResetSequence(true); 	//reset seq
					sendBslSync(); 				// sendHeader
		        	try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        	/* clear data buffer */
		        	byte[] buffer = new byte[maxPacketSize];
		        	int dataSize = mDeviceConnection.bulkTransfer(receivingEndpointMSP430, buffer, maxPacketSize, 5000);
		        	textView.append("discarded "+dataSize+" bytes before massErase\n");
		        	byte[] massEraseBuf = getMassEraseCommand();
		        	mDeviceConnection.bulkTransfer(sendingEndpointMSP430, massEraseBuf, massEraseBuf.length, 2000); //do in another thread
		        	try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        	buffer = new byte[maxPacketSize];
		        	dataSize = mDeviceConnection.bulkTransfer(receivingEndpointMSP430, buffer, maxPacketSize, 5000);
		        	if(dataSize >=0 )
		        	{
		        		textView.append("mass erase sent "+massEraseBuf+" bytes("+i+"):"+dataSize+" bytes Response\n");
		        		for (int j=0;j<maxPacketSize;j++) 
		        		{
		        			if(j>=2)
		        			{
		        				textView.append(Integer.toString(j)+": 0x"+Integer.toHexString((byte)buffer[j] & 0xFF)+"\n");
		        				if(j == 2)
		        				{
		        					if((buffer[j] & 0xFF) == 0x90)
		        					{
		        						success = true;
		        					}
		        					//break;
		        				}
		        				if(j == 5)
		        					break;
		        			}
		        		}
		        	}
		        	else
		        	{
		        		textView.append("noDataToReceive("+i+")");
		        	}
		        	i++;
		        }
		        textView.append("\nMassEraseFinish\n");
				
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
	
	private void sendBslSync() 
	{
		try 
		{
			int i=0;
			//boolean success = false;
			int maxPacketSize = 64;
			byte[] buffer = new byte[maxPacketSize];
			int recvResult = mDeviceConnection.bulkTransfer(receivingEndpointMSP430, buffer, maxPacketSize, 5000);
			textView.append("Discarded "+recvResult+" bytes before sync\n");
			//while(!(success || i > 5))
			{
				byte[] startSeq = new byte[1];
				startSeq[0] = (byte) 0x80;

				mDeviceConnection.bulkTransfer(sendingEndpointMSP430, startSeq, 1, 2000);
				buffer = new byte[maxPacketSize];

				Thread.sleep(500);

				recvResult = mDeviceConnection.bulkTransfer(receivingEndpointMSP430, buffer, maxPacketSize, 5000);
				if(recvResult >=2 )
				{
					textView.append("sendStartSeq("+i+"): "+recvResult+" bytes Response\n");
					for (int j=2;j<recvResult;j++) {

						if(j >= 0)
						{
							textView.append(Integer.toString(j)+": 0x"+Integer.toHexString((byte)buffer[j] & 0xFF)+"\n");
							if(j == 0)
							{
								int res = (buffer[j] & 0xFF);
								if( res == 0x90)
								{
									break;
								}
							}
						}
					}
				}
				else
				{
					textView.append("noDataThere: "+recvResult+"\n");
				}
				i++;
			}
		}
		catch (InterruptedException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	// 80 10 24 24 xx xx xx xx D1 D2 � D20 CKL CKH ACK
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
	
	//TX BSL version 80 1E 04 04 xx xx xx xx � � � � CKL CKH
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
		
		
//		textView.append("\n Endpoint 0: "+"\n" +endpoint0+"\n Type: "+endpoint0.getType() +"\n");
//		textView.append("\n Endpoint 1: "+"\n" +endpoint1+"\n Type: "+endpoint1.getType() +"\n");
		
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
				ftdi_usb_reset();
				ftdi_set_baudrate();
				//textView.append("ftdi_usb_reset: "+ftdi_usb_reset()+"\n");
				//textView.append("ftdi_set_baudrate 9600: "+ftdi_set_baudrate()+"\n");

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
		
		if (mDeviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE, 
				  							  FTDI_Constants.SIO_SET_MODEM_CTRL_REQUEST, 
				  							  usb_val,
				  							  FTDI_Constants.INTERFACE_ANY, null, 0, 2000) != 0)
		{
			//textView.append(Integer.toString(-1)+",");
			Log.e("ftdi_control","USB controlTransfer operation failed.");
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
	private int setRTS(boolean state)
	{
		short usb_val;
		
		if (state)
	        usb_val = FTDI_Constants.SIO_SET_RTS_HIGH;
	    else
	        usb_val = FTDI_Constants.SIO_SET_RTS_LOW;
		
		if (mDeviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE, 
				  							  FTDI_Constants.SIO_SET_MODEM_CTRL_REQUEST, 
				  							  usb_val,
				  							  FTDI_Constants.INTERFACE_ANY, null, 0, 2000) != 0)
		{
			//textView.append(Integer.toString(-1)+",");
			Log.e("ftdi_control","USB controlTransfer operation failed. ");
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
	

	protected boolean validateDataBits(int data_bits_type)
	{
		switch(data_bits_type){//FTDI document D2xx programming guide says it only supports 7 or 8.
		case FTDI_Constants.DATA_BITS_7:
		case FTDI_Constants.DATA_BITS_8:
			return true;
		default:
			Log.e("bla","Cannot recognize the data bits setting: "+ Integer.toString(data_bits_type));
			return false;
		}
	}
	
	protected boolean validateStopBits(int stop_bits_type)
	{
		switch(stop_bits_type){
		case FTDI_Constants.STOP_BITS_1:
		case FTDI_Constants.STOP_BITS_15:
		case FTDI_Constants.STOP_BITS_2:
			return true;
		default:
			Log.e("bla","Cannot recognize the stop bits setting: "+ Integer.toString(stop_bits_type));
			return false;
		}
	}
	
	protected boolean validateParity(int parity_type)
	{
		switch(parity_type){
		case FTDI_Constants.PARITY_EVEN:
		case FTDI_Constants.PARITY_MARK:
		case FTDI_Constants.PARITY_NONE:
		case FTDI_Constants.PARITY_ODD:
		case FTDI_Constants.PARITY_SPACE:
			return true;
		default:
			Log.e("bla","Cannot recognize the parity setting: "+ Integer.toString(parity_type));
			return false;
		}
	}
	
	protected boolean validateBreak(int break_type)
	{
		switch(break_type)
		{
		case FTDI_Constants.BREAK_OFF:
		case FTDI_Constants.BREAK_ON:
			return true;
		default:
			Log.e("bla","Cannot recognize the break setting: "+ Integer.toString(break_type));
			return false;
		}
	}
	
	private int usbPurgeRXBuffer()
	{
		return mDeviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE, FTDI_Constants.SIO_RESET_PURGE_RX,
				FTDI_Constants.SIO_RESET_SIO, FTDI_Constants.INTERFACE_ANY, null, 0, 2000);
		//TODO: I give it a INTERFACE_ANY as parameter. Need to verify if it is correct. I believe the Index doesn't matter.
	}
	private int usbPurgeTXBuffer()
	{
		return mDeviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE, FTDI_Constants.SIO_RESET_PURGE_TX, 
				FTDI_Constants.SIO_RESET_SIO, FTDI_Constants.INTERFACE_ANY, null, 0, 2000);
		//TODO: I give it a INTERFACE_ANY as parameter. Need to verify if it is correct. I believe the Index doesn't matter.
	}
	
	private int ftdi_usb_reset()
	{
		int result = mDeviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE, 
													   FTDI_Constants.SIO_RESET_REQUEST, 
													   FTDI_Constants.SIO_RESET_SIO, 
													   FTDI_Constants.INTERFACE_ANY, null, 0, 2000);
		textView.append("resetUsb on ReqOutType: "+result+"\n");
		
		result = mDeviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_IN_REQTYPE, 
				   								   FTDI_Constants.SIO_RESET_REQUEST, 
				   								   FTDI_Constants.SIO_RESET_SIO, 
				   								   FTDI_Constants.INTERFACE_ANY, null, 0, 2000);
		textView.append("resetUsb on ReqInType: "+result+"\n");
		return 0;
	}
	
	
	private int ftdi_set_baudrate() {
		int result = mDeviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE, 
													   FTDI_Constants.SIO_SET_BAUDRATE_REQUEST, 
													   0x4138, 
													   FTDI_Constants.INTERFACE_ANY, null, 0, 2000);
		textView.append("setBaudrate on ReqOutType: "+result+"\n");
		
		result = mDeviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_IN_REQTYPE, 
											   	   FTDI_Constants.SIO_SET_BAUDRATE_REQUEST, 
											   	   0x4138, 
											   	   FTDI_Constants.INTERFACE_ANY, null, 0, 2000);
		textView.append("setBaudrate on ReqInType: "+result+"\n");
		return 0;
	}
	
	private int ftdi_set_line_property(int bits,int sbit, int parity,int break_type)
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
		}

		switch (break_type)
		{
		case FTDI_Constants.BREAK_OFF:
			value |= (0x00 << 14);
			break;
		case FTDI_Constants.BREAK_ON:
			value |= (0x01 << 14);
			break;
		}

		int result = mDeviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_OUT_REQTYPE, 
													   FTDI_Constants.SIO_SET_DATA_REQUEST, 
													   value,
													   FTDI_Constants.INTERFACE_ANY, null, 0, 2000);
		textView.append("setLineProp on ReqOutType: "+result+"\n");
		result = mDeviceConnection.controlTransfer(FTDI_Constants.FTDI_DEVICE_IN_REQTYPE, 
				   								   FTDI_Constants.SIO_SET_DATA_REQUEST, 
				   								   value,
				   								   FTDI_Constants.INTERFACE_ANY, null, 0, 2000);
		
		textView.append("setLineProp on ReqInType: "+result+"\n");

		return 0;
	}

}