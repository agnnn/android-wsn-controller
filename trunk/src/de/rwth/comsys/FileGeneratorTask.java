package de.rwth.comsys;

import java.util.ArrayList;
import java.util.Iterator;

import de.rwth.comsys.elf.ElfLoader;
import de.rwth.comsys.helpers.IOHandler;
import de.rwth.comsys.ihex.Record;

import android.os.AsyncTask;

/**
 * 
 * TODO add comment
 *
 * Manipulates TOS_NODE_ID (necessary) and ActiveMessageAddressC__addr (if present).
 * 
 * @author Christian & Stephan
 *
 */
public class FileGeneratorTask extends AsyncTask<FileManagerEntry, Integer, Boolean>
{

	@Override
	protected Boolean doInBackground(FileManagerEntry... params)
	{
		FileManagerEntry result = params[0];
		ArrayList<Integer> tosNodeIds = result.getTosNodeIds();
		
		if (result.getElfLoader()!=null)
		{

			// check for every node id if it is necessary to updated binaries
			for (Iterator<Integer> iterator = tosNodeIds.iterator(); iterator.hasNext();)
			{
				Integer currentNodeId = iterator.next();
				
				// already generated?
				if (result.getiHexRecordsListByNodeId().containsKey(currentNodeId) == false)
				{
					ElfLoader currentElfLoader = result.getElfLoader();
					
					// Manipulate
					boolean checkManipulation = currentElfLoader.manipulateLoadedFileByName("TOS_NODE_ID", currentNodeId);
					if(checkManipulation == false)
					{
						IOHandler.doOutput("FileGeneratorTask: Can't manipulate loadedFile by TOS_NODE_ID!");
						return false;
					}
					
					// Manipulate
					currentElfLoader.manipulateLoadedFileByName("ActiveMessageAddressC__addr", currentNodeId);
					
					// update ihexRecordsList ... convert elf to ihex
					ArrayList<Record> currentRecords = currentElfLoader.createIhexRecords();
					if(currentRecords != null && !currentRecords.isEmpty())
					{
						result.getiHexRecordsListByNodeId().put(currentNodeId, currentRecords);
					}
					else
					{						
						IOHandler.doOutput("FileGeneratorTask: Can't create Records!");
						return false;
					}
					
				}
			}
			

		}
		else
		{
			IOHandler.doOutput("FileGeneratorTask: Nothing to generate, because no ELF is avaible!");
		}

		return true;
	}
	
	
	@Override
	protected void onPostExecute(Boolean successful)
	{

	}
}