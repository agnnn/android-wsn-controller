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
    private ArrayList<FileManagerEntry> fileManagerEntries = new ArrayList<FileManagerEntry> ();
    
    //TODO start mediascanner and get all important files from storage
    // update Filemanager by scanner
    
    


	/**
	 * @return the fileManagerEntries
	 */
	public ArrayList<FileManagerEntry> getFileManagerEntries()
	{
		return fileManagerEntries;
	}


	


	
    
   
}
