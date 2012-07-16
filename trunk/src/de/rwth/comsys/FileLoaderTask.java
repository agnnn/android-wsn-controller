package de.rwth.comsys;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import de.rwth.comsys.elf.ElfLoader;
import de.rwth.comsys.ihex.HexLoader;

import android.os.AsyncTask;

public class FileLoaderTask extends AsyncTask<File, Integer, FileManagerEntry>
{

	@Override
	protected FileManagerEntry doInBackground(File... params)
	{
		FileManagerEntry result = new FileManagerEntry();
		File[] files = params;
		File file = null;
		ElfLoader elfLoader = null;
		HexLoader hexLoader = null;
		String extension = null;

		// cut off the rest
		if (files != null)
		{
			file = files[0];
		}
		else
		{
			return null;
		}

		String fileName = file.getName();

		try
		{
			// crashes if fileName has no extension
			extension = fileName.substring(0, fileName.lastIndexOf('.'));
			if (extension == null || extension.isEmpty())
				return null;
		} catch (Exception e)
		{
			// TODO handle error
			return null;
		}

		// which loader has to be activated ?
		if (extension.equals("exe"))
		{
			elfLoader = ElfLoader.createElfLoader(file.getAbsolutePath());
		}
		else if (extension.equals("ihex"))
		{
			hexLoader = HexLoader.createHexLoader(file.getAbsolutePath());
		}

		// add loaders
		if (elfLoader != null)
		{
			// default key is 1 = TOS_NODE_ID
			result.getElfLoaders().put(1, elfLoader);
		}
		else if (hexLoader != null)
		{
			// default key is 1 = TOS_NODE_ID
			result.getHexLoaders().put(1, hexLoader);
		}
		else
		{
			return null;
		}

		result.setFile(file);

		return result;
	}




	@Override
	protected void onPostExecute(FileManagerEntry entry)
	{
		// update entry of file manager
		if (entry != null)
		{
			ArrayList<FileManagerEntry> files = FileManager.getInstance().getFiles();
			FileManagerEntry currentFileManagerEntry = null;
			boolean foundEntry = false;
			for (Iterator<FileManagerEntry> iterator = files.iterator(); iterator.hasNext();)
			{
				currentFileManagerEntry = iterator.next();

				// compare file paths
				if (currentFileManagerEntry.getFile().getAbsolutePath().equals(entry.getFile().getAbsolutePath()))
				{
					foundEntry = true;
					break;
				}

			}

			// overwrite entry
			if (foundEntry == true)
			{
				//TODO add or overwrite hexloaders
				currentFileManagerEntry = entry;
			}
			else
			{
				// TODO handle Error
			}
		}

	}

}
