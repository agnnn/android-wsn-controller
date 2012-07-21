package de.rwth.comsys;

import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.provider.MediaStore;
import android.widget.SimpleCursorAdapter;




public class MediaStoreActivity extends ListActivity implements MediaScannerConnectionClient
{	
	private MediaScannerConnection mConnection;
	private SimpleCursorAdapter mAdapter;
	
	
	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		// Prepare an empty adapter
		mAdapter = new SimpleCursorAdapter(this,
	                android.R.layout.simple_list_item_2, null,
	                new String[] { "_data" }, // lists path of files
	                new int[] { android.R.id.text1 }, 0);
		
		setListAdapter(mAdapter); 
		
		
		mConnection= new MediaScannerConnection(this, this);
		mConnection.connect();
		
		// Prepare the loader.
		getLoaderManager().initLoader(0, null, myLoaderListener);	
	}
	

	private final LoaderManager.LoaderCallbacks<Cursor> myLoaderListener = new LoaderCallbacks<Cursor>() {
		
		public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1)
		{	
			Uri uri = MediaStore.Files.getContentUri("external");
	
			// every column
			String[] projection = null;
	
			// exclude media files, they would be here also.
			String selection = MediaStore.Files.FileColumns.TITLE + "= ?";
			        
			String[] selectionArgs = {"main.exe"};
	
			String sortOrder = null; // unordered
			
			CursorLoader cursorLoader = new CursorLoader(MediaStoreActivity.this, uri, projection, selection, selectionArgs, sortOrder);
			 
	        return cursorLoader;
		}
	
		
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
		{
			// Swap the new cursor in.
			mAdapter.swapCursor(cursor);
			
		}
	
		public void onLoaderReset(Loader<Cursor> arg0)
		{
			// This is called when the last Cursor provided to onLoadFinished()
	        // above is about to be closed.  We need to make sure we are no
	        // longer using it.
			 mAdapter.swapCursor(null);
		}
 
	};
	

	


	public void onMediaScannerConnected()
	{
		// TODO Auto-generated method stub
		mConnection.scanFile(Environment.getExternalStorageDirectory() + "/WSN", null);
	}


	public void onScanCompleted(String path, Uri uri)
	{
		// TODO Auto-generated method stub
		mConnection.disconnect(); 
		
		
	}
	
}