package de.rwth.comsys;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import android.app.ActionBar;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData.Item;
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
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import de.rwth.comsys.helpers.IOHandler;
import de.rwth.comsys.helpers.OutputHandler;
import de.rwth.comsys.ihex.HexLoader;

public class AndroidWSNControllerActivity extends Activity
{

	private UsbManager mManager = null;
	private ArrayList<UsbDevice> mDevice = null;
	private TextView textView = null;
	private PendingIntent mPermissionIntent = null;
	UsbInterface mUSBInterface = null;
	private OutputHandler uiHandler;
	private TelosBConnector telosBConnect;
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	ListView moteList;
	ArrayAdapter<String> moteListAdapter;

	/**
	 * @return the indices of the checked motes in the moteList
	 */
	public long[] getCheckedItems() {
		final SparseBooleanArray checkedItems = moteList.getCheckedItemPositions();
		long[] positions = new long[]{};
		if (checkedItems == null) {
			// That means our list is not able to handle selection
			// (choiceMode is CHOICE_MODE_NONE for example)
			return positions;
		}
		ArrayList<Integer> indices = new ArrayList<Integer>();
	
		// For each element in the status array
		final int checkedItemsCount = checkedItems.size();
		for (int i = 0; i < checkedItemsCount; ++i) {
			// This tells us the item position we are looking at
			final int position = checkedItems.keyAt(i);
			
			// And this tells us the item status at the above position
			final boolean isChecked = checkedItems.valueAt(i);
			if(isChecked)
				indices.add(position);
		}
		
		// convert array list to simple type
		long[] resultIndices = new long[indices.size()];
		for (int i=0;i<indices.size();i++) {
			resultIndices[i] = indices.get(i);
		}
		return resultIndices;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// get button and register listener
		Button myButtonConnect = (Button) findViewById(R.id.button);
		myButtonConnect.setOnClickListener(buttonConnectListener);
		Button myButtonSend = (Button) findViewById(R.id.button1);
		myButtonSend.setOnClickListener(buttonSendListener);
		Button myButtonLoad = (Button) findViewById(R.id.button2);
		myButtonLoad.setOnClickListener(buttonLoadListener);
		Button getVersionButton = (Button) findViewById(R.id.button3);
		getVersionButton.setOnClickListener(getBSLVersionListener);
		Button startSFButton = (Button) findViewById(R.id.button4);
		startSFButton.setOnClickListener(startSFListener);
		textView = (TextView) findViewById(R.id.textView);
		textView.setMovementMethod(new ScrollingMovementMethod());
		moteList = (ListView) findViewById(R.id.listView1);
		String[] values = new String[] { "no motes connected" };
		ArrayList<String> moteListStrings = new ArrayList<String>();
		moteListStrings.addAll(Arrays.asList(values));

		mDevice = new ArrayList<UsbDevice>();
		// First paramenter - Context
		// Second parameter - Layout for the row
		// Third parameter - ID of the View to which the data is written
		// Forth - the Array of data
		moteListAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice, android.R.id.text1, moteListStrings);

		// Assign adapter to ListView
		moteList.setAdapter(moteListAdapter);
		moteList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		
		TabHost tabHost = (TabHost)findViewById(android.R.id.tabhost);
		tabHost.setup();

		TabSpec spec = tabHost.newTabSpec("one");
		spec.setIndicator("MOTELIST");
		spec.setContent(R.id.tab1);
		tabHost.addTab(spec);
		
		TabSpec spec2 = tabHost.newTabSpec("two");
		spec2.setIndicator("SF");
		spec2.setContent(R.id.tab2);
		tabHost.addTab(spec2);
		
		ActionBar actionBar = getActionBar();

		// init io handler
		IOHandler.setContext(this);

		// retrieve USB Service
		mManager = (UsbManager) getSystemService(Context.USB_SERVICE);

		// create a ui handler for display updates from another thread
		uiHandler = new OutputHandler(textView);

		// listen for new devices
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(mUsbReceiver, filter);

		telosBConnect = new TelosBConnector(mManager, this);
		textView.append("telosBConnector created\n");
	}   
 
	/**
	 * Setting up actionbar menu icons/layout.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
  
	
	/**
	 * Reacts on clicking on actionbar items.
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.action_refresh:
	        	
	        case R.id.action_flash:
	        	
	        case R.id.action_erase:	
	            // app icon in action bar clicked; go home
	            //Intent intent = new Intent(this, HomeActivity.class);
	            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            //startActivity(intent);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}


	/**
	 * @return the uiHandler
	 */
	public Handler getUiHandler()
	{
		return uiHandler;
	}

	// OnClickListener iterates over connected devices and requests permission
	private OnClickListener buttonConnectListener = new OnClickListener()
	{
		public void onClick(View v)
		{

			HashMap<String, UsbDevice> deviceList = mManager.getDeviceList();

			if (deviceList.isEmpty())
				textView.append("Nothing found! \n");

			Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
			UsbDevice currentDevice = null;
			// clear the list view
			moteListAdapter.clear();
			boolean moteFound = false;
			while (deviceIterator.hasNext())
			{
				moteFound = true;
				currentDevice = deviceIterator.next();
				mManager.requestPermission(currentDevice, mPermissionIntent);
				//textView.append("device: " + mDevice.getDeviceName() + " found\n");
			}
			if(!moteFound)
				moteListAdapter.add("no mote available");
		}
	};

	// OnClickListener iterates over connected devices and requests permission
	private OnClickListener getBSLVersionListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			try
			{
				telosBConnect.execGetBslVersion();
			} catch (Exception e)
			{
				// TODO Auto-generated catch block
				textView.append(e.getMessage() + "\n");
			}
		}
	};

	// OnClickListener iterates over connected devices and requests permission
	private OnClickListener startSFListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			try
			{
				long[] checkedItems = moteList.getCheckedItemIds();
				for (int i = 0; i < (int)checkedItems.length; i++) {
					int idx = (int)checkedItems[i];
					telosBConnect.execSerialForwarder("2001",idx);
				}
			} catch (Exception e)
			{
				// TODO Auto-generated catch block
				textView.append(e.getMessage() + "\n");
			}
		}
	};
	// OnClickListener sends a packet to mDevice
	private OnClickListener buttonSendListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			try
			{
				HexLoader hexLoader = HexLoader.createHexLoader(Environment.getExternalStorageDirectory().getAbsolutePath()
						+ File.separator + "WSN" + File.separator + "main.ihex");
					telosBConnect.execFlash(hexLoader.getRecords());
			} catch (Exception e)
			{
				textView.append(e.getMessage() + "\n");
			}
		}

		
	};

	// OnClickListener sends a packet to mDevice
	private OnClickListener buttonLoadListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			HexLoader hexLoader = HexLoader.createHexLoader(Environment.getExternalStorageDirectory().getAbsolutePath()
					+ File.separator + "WSN" + File.separator + "main.ihex");
			if(hexLoader!=null)
			{	
				textView.append("Loaded lines: " + hexLoader.getRecords().size()+"\n");
			}
			else
			{
				textView.append("HexLoader == null\n");
			}
		}
	};

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
	{
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action))
			{
				synchronized (this)
				{
					UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
					{
						if (device != null)
						{
							// call method to set up device communication
							mDevice.add(device);
							telosBConnect.connectDevice(device);
							
							// add device to the list view
							moteListAdapter.add(device.getDeviceName());
						}
					}
				}
			}
		}
	};




	@Override
	public void onPause()
	{
		super.onPause();
	}




	@Override
	public void onResume()
	{
		super.onResume();
	}




	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}




	public TextView getOutputTextView()
	{
		return textView;
	}




	Runnable getOutputRunnable()
	{
		Runnable act = new Runnable()
		{
			public void run()
			{
				textView.append("blaslgq\n");
			};
		};
		return act;
	}




	public TelosBConnector getTelosBConnecter()
	{
		return this.telosBConnect;
	}
}