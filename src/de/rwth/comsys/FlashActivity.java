package de.rwth.comsys;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

public class FlashActivity extends Activity {

	private MenuItem flashItem = null;
	private ArrayList<CharSequence> moteList;
	ListView moteListView;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_flash);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		moteList = getIntent().getCharSequenceArrayListExtra("motes");
		if(moteList == null)
			moteList = new ArrayList<CharSequence>();
		
		ArrayList<String> ml = new ArrayList<String>();
		for (int i=0;i<moteList.size();i++) {
			ml.add((String)moteList.get(i));
		}
		FlashListViewAdapter moteAdapter = new FlashListViewAdapter(this, R.layout.flash_row, ml);
		
		moteListView = (ListView) findViewById(R.id.flashListView);
		moteListView.setAdapter(moteAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_flash, menu);
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
					FileManagerEntry fmEntry = new FileManagerEntry();
					fmEntry.setFile(new File((String)items[item]));
					FileManager.getInstance().getFileManagerEntries().add(fmEntry);
					
					if(flashItem != null)
						flashItem.setVisible(true);
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
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
}
