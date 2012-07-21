package de.rwth.comsys;

import java.util.ArrayList;
import java.util.Iterator;

import android.os.AsyncTask;
import de.rwth.comsys.elf.ElfLoader;
import de.rwth.comsys.helpers.IOHandler;
import de.rwth.comsys.ihex.HexLoader;

/**
 * Loads a file from memory by specific Loader and overwrites the given "FileManagerEntry"
 * in "FileManager".
 * 
 * @author Christian & Stephan
 * 
 */
public class FileLoaderTask extends AsyncTask<FileManagerEntry, Integer, Boolean>
{

	@Override
	protected Boolean doInBackground(FileManagerEntry... params)
	{
		FileManagerEntry result;
		ElfLoader elfLoader = null;
		HexLoader hexLoader = null;
		String extension = null;
		String fileName = null;

		// cut off rest and set fileName
		if (params != null)
		{
			result = params[0];
			fileName = result.getFile().getName();
		}
		else
		{
			// stop execution
			IOHandler.doOutput("FileLoaderTask: wrong input!");
			return false;
		}

		// get extension
		try
		{
			// crashes if fileName has no extension
			extension = fileName.substring(0, fileName.lastIndexOf('.'));
			if (extension == null || extension.isEmpty())
			{
				IOHandler.doOutput("FileLoaderTask: File has no extension!");
				return false;
			}

		} catch (Exception e)
		{
			IOHandler.doOutput("FileLoaderTask: File has no extension!");
			return false;
		}

		// which loader has to be activated ?
		if (extension.equals("exe"))
		{
			elfLoader = ElfLoader.createElfLoader(result.getFile().getAbsolutePath());
		}
		else if (extension.equals("ihex"))
		{
			hexLoader = HexLoader.createHexLoader(result.getFile().getAbsolutePath());
		}

		// add loaders
		if (elfLoader != null)
		{
			// default key is 1 = TOS_NODE_ID
			result.setElfLoader(elfLoader);
		}
		else if (hexLoader != null)
		{
			// default key is 1 = TOS_NODE_ID
			result.setHexLoader(hexLoader);
			result.getiHexRecordsListByNodeId().put(1, hexLoader.getRecords());
		}
		else
		{
			IOHandler.doOutput("FileLoaderTask: Can't create any loader!");
			return false;
		}

		// update entry of file manager
		ArrayList<FileManagerEntry> files = FileManager.getInstance().getFileManagerEntries();
		FileManagerEntry currentFileManagerEntry = null;
		boolean foundEntry = false;
		for (Iterator<FileManagerEntry> iterator = files.iterator(); iterator.hasNext();)
		{
			currentFileManagerEntry = iterator.next();

			// compare file paths
			if (currentFileManagerEntry.getFile().getAbsolutePath().equals(result.getFile().getAbsolutePath()))
			{
				foundEntry = true;
				break;
			}

		}

		// overwrite entry
		if (foundEntry == true)
		{
			currentFileManagerEntry = result;
		}
		else
		{
			IOHandler.doOutput("FileLoaderTask: Can't overwrite/find FileManagerEntry!");
		}

		return true;
	}




	@Override
	protected void onPostExecute(Boolean successful)
	{

	}

}
