package de.rwth.comsys;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AndroidWSNControllerActivity extends Activity {

	private UsbManager mManager = null;
	private UsbDevice mDevice = null;
	private TextView textView = null;
	private PendingIntent mPermissionIntent = null;
	UsbInterface mUSBInterface = null;
	private OutputHandler uiHandler;
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
		Button myButtonLoad = (Button) findViewById(R.id.button2);
		myButtonLoad.setOnClickListener(buttonLoadListener);
		textView = (TextView) findViewById(R.id.textView);
		textView.setMovementMethod(new ScrollingMovementMethod());

		// retrieve USB Service
		mManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		
		// create a ui handler for display updates from another thread
		uiHandler = new OutputHandler(textView);
		
		// listen for new devices
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(mUsbReceiver, filter);

		telosBConnect = new TelosBConnector(mManager, this);
		textView.append("telosBConnector created\n");
	}
	
	/**
	 * @return the uiHandler
	 */
	public Handler getUiHandler() {
		return uiHandler;
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
				textView.append("device: " + mDevice.getDeviceName()
						+ " found\n");
			}
		}
	};

	// OnClickListener sends a packet to mDevice
	private OnClickListener buttonSendListener = new OnClickListener() {
		public void onClick(View v) {
			try {
				telosBConnect.execFlash((new HexLoader(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "WSN" +  File.separator + "main.ihex")).getRecords());
			} catch (Exception e) {
				textView.append(e.getMessage() + "\n");
			}
		}
	};

	// OnClickListener sends a packet to mDevice
	private OnClickListener buttonLoadListener = new OnClickListener() {
		public void onClick(View v) {
			HexLoader test = new HexLoader(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "WSN" +  File.separator + "main.ihex");
			textView.append("Loaded lines: "+test.getRecords().size());
		}
	};
	
	
	
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbDevice device = (UsbDevice) intent
							.getParcelableExtra(UsbManager.EXTRA_DEVICE);

					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						if (device != null) {
							// call method to set up device communication
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
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public TextView getOutputTextView() {
		return textView;
	}
	
	Runnable getOutputRunnable()
	{
		Runnable act = new Runnable()
		{
			public void run() {
				textView.append("blaslgq\n");
			};
		};
       return act;
	}
}