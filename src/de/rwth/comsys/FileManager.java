package de.rwth.comsys;

import java.io.File;
import java.util.ArrayList;


/**
 * Data structure to manage ELF and INTEL HEX files thread-safe.
 * Singleton pattern.
 * @author Christian & Stephan
 *
 */
public final class FileManager
{
	
	// Singleton implementation
    private static final FileManager INSTANCE = new FileManager();

   
    private FileManager() {}

    
    public static FileManager getInstance() {
        return INSTANCE;
    }	
	
    

	// attributes
    private ArrayList<FileManagerEntry> files = new ArrayList<FileManagerEntry> ();
    
    //TODO start mediascanner and get all important files from storage
    // update Filemanager by scanner
    public void loadFile(File file)
    {
    	FileLoaderTask task = new FileLoaderTask();
		//TODO task.execute();
    }
    
    /**
	 * @return the files
	 */
	public ArrayList<FileManagerEntry> getFiles()
	{
		return files;
	}


	/**
	 * @param files the files to set
	 */
	public void setFiles(ArrayList<FileManagerEntry> files)
	{
		this.files = files;
	}
}
