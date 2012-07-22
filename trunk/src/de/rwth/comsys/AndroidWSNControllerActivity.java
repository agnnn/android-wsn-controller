package de.rwth.comsys;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.app.ActionBar;
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
import android.util.Log;
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
import de.rwth.comsys.ihex.Record;

public class AndroidWSNControllerActivity extends Activity {

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
	String filePath = "";
	ArrayList<Integer> tosIds = null;

	/**
	 * @return the indices of the checked motes in the moteList
	 */
	public long[] getCheckedItems() {
		final SparseBooleanArray checkedItems = moteList
				.getCheckedItemPositions();
		long[] positions = new long[] {};
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
			if (isChecked)
				indices.add(position);
		}

		// convert array list to simple type
		long[] resultIndices = new long[indices.size()];
		for (int i = 0; i < indices.size(); i++) {
			resultIndices[i] = indices.get(i);
		}
		return resultIndices;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Button myButtonLoad = (Button) findViewById(R.id.button2);
		// myButtonLoad.setOnClickListener(buttonLoadListener);
		Button getVersionButton = (Button) findViewById(R.id.button3);
		getVersionButton.setOnClickListener(getBSLVersionListener);

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
		moteListAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_multiple_choice,
				android.R.id.text1, moteListStrings);

		// Assign adapter to ListView
		moteList.setAdapter(moteListAdapter);
		moteList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
		tabHost.setup();

		TabSpec spec = tabHost.newTabSpec("one");
		spec.setIndicator("MOTELIST");
		spec.setContent(R.id.tab1);
		tabHost.addTab(spec);

		TabSpec spec2 = tabHost.newTabSpec("two");
		spec2.setIndicator("Info");
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
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(mUsbReceiver, filter);

		telosBConnect = new TelosBConnector(mManager, this);
		// textView.append("telosBConnector created\n");
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
		{
			refreshMoteList();
			return true;
		}
		case R.id.action_flash:
		{

			ArrayList<CharSequence> strMoteList = new ArrayList<CharSequence>();
			final SparseBooleanArray checkedItems = moteList.getCheckedItemPositions();
			for (int i = 0; i < moteListAdapter.getCount(); i++) {
				if(checkedItems.indexOfKey(i) >= 0)
				{
					strMoteList.add(moteListAdapter.getItem(i));
				}
			}
			
			Intent myIntent = new Intent(AndroidWSNControllerActivity.this,
					FlashActivity.class);
			myIntent.putCharSequenceArrayListExtra("motes", strMoteList);
			
			long[] items = getCheckedItems();
			ArrayList<Integer> itemsInt = new ArrayList<Integer>();
			for (int i = 0; i < items.length; i++) {
				itemsInt.add((int)items[i]);
			}
			myIntent.putIntegerArrayListExtra("moteIndices", itemsInt);
			startActivityForResult(myIntent,42);

			
			/*try {
				HexLoader hexLoader = HexLoader
						.createHexLoader(Environment
								.getExternalStorageDirectory()
								.getAbsolutePath()
								+ File.separator
								+ "WSN"
								+ File.separator
								+ "main.ihex");
				telosBConnect.execFlash(hexLoader.getRecords());
			} catch (Exception e) {
				textView.append(e.getMessage() + "\n");
				return false;
			}
			*/
			return true;
		}
		case R.id.action_erase:
		{
			try {
				telosBConnect.execMassErase(getCheckedItems());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}
		case R.id.action_sf_on:
		{
			try {
				long[] checkedItems = getCheckedItems();

				int j = 0;
				for (int i = 0; i < moteListAdapter.getCount(); i++) {
					if (checkedItems[j] == i) {
						int idx = i;
						String listItem = (String) moteList
								.getItemAtPosition(idx);
						moteListAdapter.remove(listItem);
						int end = listItem.indexOf("sf");
						if (end != -1)
							listItem = listItem.substring(0, end);

						String sf = "200" + Integer.toString(i);
						textView.append("start sf for idx: " + idx + "\n");
						IOHandler.doOutput("twrgsdg");
						if (telosBConnect.execSerialForwarder(sf, idx)) {
							moteListAdapter
									.insert(listItem + " sf: " + sf, idx);
						} else {
							textView.append("add item at pos: " + idx + "\n");
							moteListAdapter.insert(listItem, idx);
						}
						j++;
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				textView.append("error: " + e.getMessage() + "\n");
			}
			return true;
		}
		case R.id.action_sf_off:
		{
			try {
				long[] checkedItems = getCheckedItems();
				for (int i = 0; i < (int) checkedItems.length; i++) {
					int idx = (int) checkedItems[i];

					if (telosBConnect.execStopSerialForwarder(idx)) {
						textView.append("### stopped: " + i + "\n");
						String listItem = (String) moteList
								.getItemAtPosition(idx);
						moteListAdapter.remove(listItem);
						int end = listItem.indexOf("sf");
						if (end != -1)
							listItem = listItem.substring(0, end).trim();
						moteListAdapter.insert(listItem, idx);
					} else {
						textView.append("### stopped failed: " + i + "\n");
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				textView.append("error: " + e.getMessage() + "\n");
			}
			return true;
		}
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void refreshMoteList() {
		telosBConnect.clear();
		// listen for new devices
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(mUsbReceiver, filter);

		HashMap<String, UsbDevice> deviceList = mManager.getDeviceList();

		if (deviceList.isEmpty())
			textView.append("Nothing found! \n");

		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		UsbDevice currentDevice = null;
		// clear the list view
		moteListAdapter.clear();
		boolean moteFound = false;
		while (deviceIterator.hasNext()) {
			moteFound = true;
			currentDevice = deviceIterator.next();
			mManager.requestPermission(currentDevice, mPermissionIntent);
			// textView.append("device: " + mDevice.getDeviceName() +
			// " found\n");
		}
		if (!moteFound)
			moteListAdapter.add("no mote available");
	}

	/**
	 * @return the uiHandler
	 */
	public Handler getUiHandler() {
		return uiHandler;
	}

	// OnClickListener iterates over connected devices and requests permission
	private OnClickListener getBSLVersionListener = new OnClickListener() {
		public void onClick(View v) {
			try {
				textView.setText("");
				// telosBConnect.execGetBslVersion();
				// telosBConnect.execSerialForwarder("2001", 0);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				textView.append(e.getMessage() + "\n");
			}
		}
	};

	// OnClickListener sends a packet to mDevice
	private OnClickListener buttonLoadListener = new OnClickListener() {
		public void onClick(View v) {
			HexLoader hexLoader = HexLoader.createHexLoader(Environment
					.getExternalStorageDirectory().getAbsolutePath()
					+ File.separator + "WSN" + File.separator + "main.ihex");
			if (hexLoader != null) {
				textView.append("Loaded lines: "
						+ hexLoader.getRecords().size() + "\n");
			} else {
				textView.append("HexLoader == null\n");
			}
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
						if (device != null && telosBConnect != null) {
							// TODO what happens if someone refreshes the
							// motelist when there are SFs started
							/*
							 * int curIdx = (mDevice.size()-1); String
							 * deviceName = device.getDeviceName();
							 * if(telosBConnect.getSFState(curIdx)) { deviceName
							 * += "sf: 200"+curIdx; } else {
							 * textView.append("getSFState: false for idx: "
							 * +curIdx); }
							 */
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
	private FileManagerEntry fmEntry;

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

	Runnable getOutputRunnable() {
		Runnable act = new Runnable() {
			public void run() {
				textView.append("blaslgq\n");
			};
		};
		return act;
	}

	public TelosBConnector getTelosBConnecter() {
		return this.telosBConnect;
	}
	
	public void generateFlashFiles()
	{
		fmEntry = new FileManagerEntry();
		fmEntry.setFile(new File(this.filePath));
		
		if(tosIds != null)
		{
			fmEntry.setTosNodeIds(tosIds);
			FileManager.getInstance().getFileManagerEntries().add(fmEntry);
			
			Log.w("FLASH", "start load: ");
			fmEntry.loadFile(this);
		}
		else
		{
			Log.w("FLASHING","tos ids is null");
		}
	}	
	
	public synchronized void onFinishedLoad(boolean success)
	{
		Log.w("FLASH", "finished load: "+success);
		if(success)
		{
			fmEntry.generateFlashableFiles(this);
		}
	}
	
	public synchronized void onFinishedGenerate(boolean success)
	{
		Log.w("FLASH", "finished generate"+success);
		if(success)
		{
			HashMap<Integer, ArrayList<Record>> flashData = fmEntry.getiHexRecordsListByNodeId();
			HashMap<Integer,FlashMapping> newData = new HashMap<Integer,FlashMapping>();
			
			HexLoader hexLoader = HexLoader
					.createHexLoader(Environment
							.getExternalStorageDirectory()
							.getAbsolutePath()
							+ File.separator
							+ "WSN"
							+ File.separator
							+ "main.ihex");
			long[] moteListIndices = getCheckedItems();
				Log.w("GENERATED","indices null");
			Log.w("GENERATED", "nodeIdSize: "+tosIds.size());
			for (int i=0;i<tosIds.size();i++) {
				
				Log.w("GENERATED","motlistindex: "+(int)moteListIndices[i]);
				Log.w("GENERATED","nodeId: "+tosIds.get(i));
				Log.w("GENERATED","flashData size: "+flashData.size());
				//FlashMapping fm = new FlashMapping((int)moteListIndices[i],flashData.get(tosIds.get(i)));
				FlashMapping fm = new FlashMapping((int)moteListIndices[i],hexLoader.getRecords());
				
				/*for (Record rec : flashData.get(0)) {
					Log.w("IHEX",rec.toString());
				}*/
				newData.put(tosIds.get(i), fm);
			}
			
			ArrayList<Record> toCmp = flashData.get(0);
			int i=0;
			for (Record rec : hexLoader.getRecords()) {
				if(!toCmp.get(i).equals(rec))
				{
					Log.w("COMPARE","difference at record "+i);
					Log.w("COMPARE",rec.toString());
					Log.w("COMPARE",toCmp.get(i).toString());
				}
				i++;
			}
			
			try {
				telosBConnect.execFlash(newData);
			} catch (Exception e) {
				Log.e("FLASHING",e.getMessage());
			}		
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		Log.w("FLASHING","we are successfully returned: "+requestCode);
		super.onActivityResult(requestCode, resultCode, data);
		switch(resultCode)
		{
		case Activity.RESULT_OK:
		{
			filePath = data.getStringExtra("path");
			tosIds = data.getIntegerArrayListExtra("nodeIds");
			
			Log.w("FLASHING","generate flash files");
			generateFlashFiles();
			break;
		}
		default:
			break;
		}
	}
	
	
}