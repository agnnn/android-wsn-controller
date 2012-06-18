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
import android.hardware.usb.UsbRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AndroidWSNControllerActivity extends Activity {

	private UsbManager mManager = null;
	private UsbDevice mDevice = null;
	private UsbDeviceConnection mDeviceConnection = null;
	private UsbEndpoint sendingEndpointMSP430 = null; // view of HOST
	private UsbEndpoint receivingEndpointMSP430 = null;	//view of HOST
	private TextView textView = null;
	private PendingIntent mPermissionIntent = null;
	

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
			while (deviceIterator.hasNext()) {
				mDevice = deviceIterator.next();
				
				mManager.requestPermission(mDevice, mPermissionIntent);
				

			}
		}
	};
	
	// OnClickListener sends a packet to mDevice
	private OnClickListener buttonSendListener = new OnClickListener() {
		public void onClick(View v) {
			
			if(mDeviceConnection != null){
				byte[] bytesUSB = new byte[3];
				byte header = 0x3F;
				
				byte cmd = 0x15;
				byte al = 0x0;
				byte am = 0x0;
				byte ah = 0x0;
				
				
				
				bytesUSB[0]=header;
				bytesUSB[1]= 1;
				bytesUSB[2] = cmd;
				
//				bytesUSB[2] = cmd ;
//				bytesUSB[3] = am;
//				bytesUSB[4] = ah;
				for(int i = 5; i< bytesUSB.length; i++){
					bytesUSB[i] = 7;
				}
				
				ByteBuffer buffer = ByteBuffer.allocate(64);
		        UsbRequest request = new UsbRequest();
		        request.initialize(mDeviceConnection, receivingEndpointMSP430);
		        request.queue(buffer, 64);
				mDeviceConnection.bulkTransfer(sendingEndpointMSP430, bytesUSB, bytesUSB.length, 200); //do in another thread
				if (mDeviceConnection.requestWait() == request) {
					
					for(int j = 0; j< buffer.capacity(); j++){
						textView.append(buffer.get()+"\n");
					}
					
				}
				
				
			}
			
		}
	};
	
	
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

		UsbInterface mUSBInterface = null;
		
		mUSBInterface = device.getInterface(0);

		// MSP430 has 2 endpoints (SLAU319B.pdf, paragraph 1.5)
		if (mUSBInterface.getEndpointCount() != 2) {
			textView.append("Can't find all endpoints!\n");
			return;
		}

		
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
	
	private static final String ACTION_USB_PERMISSION =
		    "com.android.example.USB_PERMISSION";
		private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

		    public void onReceive(Context context, Intent intent) {
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
}