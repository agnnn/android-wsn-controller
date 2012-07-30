package de.rwth.comsys;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import de.rwth.comsys.ihex.Record;

public class FlashActivity extends Activity implements MediaScannerConnectionClient{

	private MenuItem flashItem = null;
	private ArrayList<CharSequence> moteList;
	private FileManagerEntry fmEntry = null;
	private ListView moteListView;
	private FlashListViewAdapter moteAdapter;
	private ArrayList<Integer> moteListIndices; 
	ArrayList<Integer> tosNodeIds;
	String filePath;
	private MediaScannerConnection mConnection;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_flash);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		moteList = getIntent().getCharSequenceArrayListExtra("motes");
		moteListIndices = getIntent().getIntegerArrayListExtra("moteIndices");
		if(moteList == null)
		{
			moteList = new ArrayList<CharSequence>();
			Log.e("GENERATED", "error retrieving data from intent");
		}
		
		ArrayList<String> ml = new ArrayList<String>();
		for (int i=0;i<moteList.size();i++) {
			ml.add((String)moteList.get(i));
		}
		moteAdapter = new FlashListViewAdapter(this, R.layout.flash_row, ml);
		
		moteListView = (ListView) findViewById(R.id.flashListView);
		moteListView.setAdapter(moteAdapter);
		
		mConnection= new MediaScannerConnection(this, this);
		mConnection.connect();
		
		fmEntry = null;

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_flash, menu);
		flashItem = menu.findItem(R.id.startFlash);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.selectImage:
			final CharSequence[] items = generateFileList();

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Select an image to flash!");
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					
					Toast.makeText(getApplicationContext(), items[item],
							Toast.LENGTH_SHORT).show();
					
					filePath = (String)items[item];
										
					if(flashItem != null)
						flashItem.setVisible(true);
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		case R.id.startFlash:
			
			tosNodeIds = new ArrayList<Integer>();
			
			for (int i = 0; i < moteAdapter.getCount(); i++) {
				//customListView = (RelativeLayout)moteAdapter.getView(0, customListView, null);
				/*RelativeLayout customListView = (RelativeLayout)moteAdapter.getView(0, null, null);
				Log.w("FLASHING","customListView: "+customListView);
				Log.w("FLASHING","childrenCount: "+customListView.getChildCount());
				
				//int children = customListView.getChildCount();
				EditText nodeIdInput = (EditText)customListView.getChildAt(1);
				Log.w("FLASHING","nodeIdInput: "+nodeIdInput);
				int moteId = Integer.parseInt(nodeIdInput.getText().toString());
				Log.w("FLASHING","moteId: "+moteId);
			/*	int moteId = CustomTextWatcher.getNodeId(i);
				Log.w("FLASHING","moteId added: "+moteId);*/
				tosNodeIds.add(i);
			}
			
			Intent resultIntent = new Intent();
			resultIntent.putIntegerArrayListExtra("nodeIds", tosNodeIds);
			resultIntent.putExtra("path", filePath);
			setResult(Activity.RESULT_OK,resultIntent);
			Log.w("FLASHING","return to app");
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private CharSequence[] generateFileList() {
		
		MediaScannerConnection mConnection;
				
		Uri uri = MediaStore.Files.getContentUri("external");
		// every column
		String[] projection = null;

		// exclude media files, they would be here also.
		String selection = MediaStore.Files.FileColumns.TITLE + "= ?";
		        
		String[] selectionArgs = {"main.exe"};

		String sortOrder = null; // unordered
		
		Cursor cursor =  managedQuery( uri, projection, selection, selectionArgs, sortOrder);
		
		cursor.moveToFirst();
		int columnIndex = cursor.getColumnIndexOrThrow("_data");
		int items = cursor.getCount();
		CharSequence[] result = new CharSequence[items];
		int i=0;
		
//		Toast.makeText(this, "", duration)
		while(!cursor.isAfterLast())
		{
			result[i++] = cursor.getString(columnIndex);
			cursor.moveToNext();
		}
		return result;
	}

	public void onMediaScannerConnected()
	{
		// TODO Auto-generated method stub
		mConnection.scanFile(Environment.getExternalStorageDirectory() + "/WSN/main.exe.nodeid", null);
		mConnection.scanFile(Environment.getExternalStorageDirectory() + "/WSN/main.exe.serial", null);
	}


	public void onScanCompleted(String path, Uri uri)
	{
		// TODO Auto-generated method stub
		mConnection.disconnect(); 
	}
	
	
}
