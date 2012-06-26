package de.rwth.comsys;

import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
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
	UsbInterface mUSBInterface = null;
	private TelosBConnector telosBConnect;
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	

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
        
        telosBConnect = new TelosBConnector(mManager,textView);
        textView.append("telosBConnector created\n");
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
			try {
				telosBConnect.execMassErase();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				textView.append(e.getMessage()+"\n");
			}
		}
	};
	
	
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
		                    	telosBConnect.connectDevice(mDevice);
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