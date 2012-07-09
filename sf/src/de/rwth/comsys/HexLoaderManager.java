package de.rwth.comsys;

import java.util.ArrayList;

/**
 * Singleton
 * Used to store loaded ihex files.
 * Gives the ability to flash synchronized.
 * 
 * @author Christian & Stephan
 *
 */
public final class HexLoaderManager {
	
	private final static HexLoaderManager instance =  new HexLoaderManager();
	
	// Holds data
	private ArrayList<HexLoader> records = new ArrayList<HexLoader>();
	
	/**
	 * Don't use!
	 */
	private HexLoaderManager (){}
	
	/**
	 * Returns a instance of RequestedDataManager.
	 * @return
	 */
	public static HexLoaderManager getRequestedDataManager()
	{	
		return instance;
	}

	/**
	 * @return the records
	 */
	public ArrayList<HexLoader> getRecords() {
		return records;
	}

	/**
	 * @param records the records to set
	 */
	public void setRecords(ArrayList<HexLoader> records) {
		this.records = records;
	}

	
}
