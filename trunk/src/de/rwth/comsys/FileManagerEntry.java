package de.rwth.comsys;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.util.Log;
import de.rwth.comsys.elf.ElfLoader;
import de.rwth.comsys.helpers.IOHandler;
import de.rwth.comsys.ihex.HexLoader;
import de.rwth.comsys.ihex.Record;

/**
 * Used to manage an ELF file 
 * and their corresponding generated files w.r.t TOS_NODE_ID.
 * @author Christian & Stephan
 *
 */

public class FileManagerEntry
{
	private File file = null;
	private ArrayList<Integer> tosNodeIds = new ArrayList<Integer>();
	private ElfLoader elfLoader =  null;
	private HexLoader hexLoader =  null;
	
	// Integer reflects Node_ID
	private HashMap<Integer , ArrayList<Record>> iHexRecordsListByNodeId =  new HashMap<Integer, ArrayList<Record>>();
	
	
	
	/**
	 * Starts asyncTask which loads and parses the file.
	 */
	public void loadFile(AndroidWSNControllerActivity context)
    {
		if(file!=null){
			FileLoaderTask task = new FileLoaderTask();
			task.registerListener(context);
			task.execute(new FileManagerEntry[] {this});
		}
		else
		{
			Log.w("LOAD","FileManagerEntry: Can't start FileLoaderTask, because File is null!");;
		}
    	
    }
	
	/**
	 * Starts asyncTask which generates missing ElfLoaders and HexLoaders by tosNodeIds.
	 * @param tosNodeIds Defines which binaries should be generated. 
	 */
	public void generateFlashableFiles(AndroidWSNControllerActivity context){
		
		if(!tosNodeIds.isEmpty()){
			FileGeneratorTask task = new FileGeneratorTask();
			task.registerListener(context);
			task.execute(new FileManagerEntry[] {this});
		}
		else
		{
			Log.w("LOAD","FileManagerEntry: Can't start FileGeneratorTask, because tosNodeIds is empty!");;
		}
	}
	
	
	
	/**
	 * @return the file
	 */
	public File getFile()
	{
		return file;
	}
	/**
	 * @param file the file to set
	 */
	public void setFile(File file)
	{
		this.file = file;
	}

	/**
	 * @return the tosNodeIds
	 */
	public ArrayList<Integer> getTosNodeIds()
	{
		return tosNodeIds;
	}

	/**
	 * @param tosNodeIds the tosNodeIds to set
	 */
	public void setTosNodeIds(ArrayList<Integer> tosNodeIds)
	{
		this.tosNodeIds = tosNodeIds;
	}

	/**
	 * @return the elfLoader
	 */
	public ElfLoader getElfLoader()
	{
		return elfLoader;
	}

	/**
	 * @param elfLoader the elfLoader to set
	 */
	public void setElfLoader(ElfLoader elfLoader)
	{
		this.elfLoader = elfLoader;
	}

	/**
	 * @return the hexLoader
	 */
	public HexLoader getHexLoader()
	{
		return hexLoader;
	}

	/**
	 * @param hexLoader the hexLoader to set
	 */
	public void setHexLoader(HexLoader hexLoader)
	{
		this.hexLoader = hexLoader;
	}

	/**
	 * @return the iHexRecordsListByNodeId
	 */
	public HashMap<Integer , ArrayList<Record>> getiHexRecordsListByNodeId()
	{
		return iHexRecordsListByNodeId;
	}

	/**
	 * @param iHexRecordsListByNodeId the iHexRecordsListByNodeId to set
	 */
	public void setiHexRecordsListByNodeId(HashMap<Integer , ArrayList<Record>> iHexRecordsListByNodeId)
	{
		this.iHexRecordsListByNodeId = iHexRecordsListByNodeId;
	}

	
	
}
