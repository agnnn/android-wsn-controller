package de.rwth.comsys;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import de.rwth.comsys.Enums.ErrorCodes;
import de.rwth.comsys.Enums.RecordTypes;

import android.os.Environment;
import android.util.Log;

/**
 * Loads a "main.ihex" file from external storage, which is placed in folder "WSN".
 * Parses the file to Records. 
 * @author Christian
 *
 */
public class HexLoader {
	
	
	private ArrayList<Record> records = new ArrayList<Record>();
	private File iHexFile = null;
	private long dateOfFile = 0;
	
	private HexLoader(){}
	
	/**
	 * Loads a iHex file from the given position on storage.
	 * Parses the file to Records.
	 * Does some syntax/semantic checks.
	 * @param pathToFile
	 */
	public HexLoader(String pathToFile)
	{
		// check access to storage
		if(checkAccessToExternalStorage() == false) return;
		
		iHexFile = new File(pathToFile);

	    try {
	        
	        FileReader fileReader = new FileReader(iHexFile);
	        BufferedReader bufferedReader = new BufferedReader(fileReader);

	        String row = "";

	        while( (row = bufferedReader.readLine()) != null )
	        {
	        	if(parseToRecord(row)==false)
	        	{
	        		handleError(ErrorCodes.HEXLOADER_RECORD_PARSING_ERROR, "");
	        		break;
	        	}	
	        }

	        bufferedReader.close();
	        
	        
	        // check for Start Segment Address Record
	        if(Record.checkStartSegmentAddressRecord(records)==false)
	        {
	        	handleError(ErrorCodes.HEXLOADER_RECORD_INVALID_START_SEGMENT_ADDRESS_RECORD, "");
	        }
	        
	        // check for end of file record
	        if(Record.checkEndOfFileRecord(records)==false)
	        {
	        	handleError(ErrorCodes.HEXLOADER_RECORD_INVALID_END_OF_FILE_RECORD, "");
	        }
	        
	        // set date of file
	        setDateOfFile(iHexFile.lastModified());
				        
	    } catch (IOException e) {
	    	handleError(ErrorCodes.HEXLOADER_CANT_LOAD_FILE, iHexFile.toString() + " " + e.toString());
	    }
	    
	}

	
	/**
	 * Handles error and if necessary, clears "records".
	 */
	private void handleError(ErrorCodes e, String additionalMsg) {
			
		//log error
		switch(e)
		{
		case HEXLOADER_RECORD_PARSING_ERROR: 
			//clear
			records.clear();
			Log.w("Hexloader", "Error: Can't parse file!");
			break;
		case HEXLOADER_RECORD_INVALID_START_SEGMENT_ADDRESS_RECORD:
			//clear
			records.clear();
			Log.w("Hexloader", "Error: No start segment address record found or double entry!");
			break;
		case HEXLOADER_NO_ACCESS_TO_STORAGE:
			Log.w("Hexloader", "Error: No access to storage!");
			break;	
		case HEXLOADER_CANT_LOAD_FILE:
			Log.w("Hexloader", "Error: Can't read from file: " + additionalMsg);
			break;	
		case HEXLOADER_RECORD_INVALID_END_OF_FILE_RECORD:
			records.clear();
			Log.w("Hexloader", "Error: No end of file record found or double entry!");
			break;
		default: 
			break;	
		}
	}


	/**
	 * Converts a IntelHex row into a Record.
	 * @param row
	 * @return
	 */
	private boolean parseToRecord(String row)
	{
		char[] splittedRow = new char[row.length()];
		String startCode = ":";
		short byteCount;
		short addressHighByte;
		short addressLowByte;
		RecordTypes recordType;
		short[] data;
		
		
		//split line into characters
		for(int i= 0; i < row.length(); i++)
		{
			splittedRow[i] = row.charAt(i);
		}	
		
		
		//start parsing
		try
		{	
			
			//check startSymbol
			startCode = String.valueOf(splittedRow[0]);
			if(startCode.equals(":")==false) return false;
			
			//offset of 2, because we want to parse a byte
			byteCount = Short.decode("0x"+ new String(splittedRow, 1,2));
			addressHighByte = Short.decode("0x"+ new String(splittedRow, 3,2));
			addressLowByte = Short.decode("0x"+ new String(splittedRow, 5,2));
			short recordTypeAsShort = Short.parseShort(new String(splittedRow, 7,2));
			
			switch (recordTypeAsShort)
			{
				case 0x00:
					recordType = RecordTypes.DATA_RECORD;
					break;
				case 0x01:
					recordType = RecordTypes.END_OF_FILE_RECORD;
					break;
				case 0x02:
					recordType = RecordTypes.EXTENDED_SEGMENT_ADDRESS_RECORD;
					break;
				case 0x03:
					recordType = RecordTypes.START_SEGMENT_ADDRESS_RECORD;
					break;
				default: return false;	
			}
			
			data = new short[byteCount];
			
			int j = 9; // start char
			for(int i = 0; (i < data.length) && (j < splittedRow.length); i++)
			{	
				
				data[i] = Short.decode("0x"+ new String(splittedRow, j, 2));
				j = j + 2;
			}
			
			
			
			//create record and check checksum
			Record record = Record.createRecord(byteCount, addressHighByte, addressLowByte, recordType, data);
			
			//successfully created?
			if(record == null)
			{
				return false;
			}
			else
			{
				records.add(record);
				return true;
			}
		}
		catch(Throwable t)
		{
			Log.w("HexLoader", "Parsing error: ", t);
			return false;
		}
	}
	
		
	
	/**
	 * Checks if we can read/write to external storage. 
	 * @return boolean 
	 */
	public boolean checkAccessToExternalStorage()
	{	
		String state = Environment.getExternalStorageState();
		
		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    return true;
		}
		
		// Can't read nor write
		handleError(ErrorCodes.HEXLOADER_NO_ACCESS_TO_STORAGE, "");
		return false;
	}
	
		
	/**
	 * @return the records
	 */
	public ArrayList<Record> getRecords() {
		return records;
	}


	/**
	 * 
	 * @return the dateOfFile 0 if no file exists.
	 */
	public long getDateOfFile() {
		return dateOfFile;
	}


	/**
	 * @param dateOfFile the dateOfFile to set
	 */
	public void setDateOfFile(long dateOfFile) {
		this.dateOfFile = dateOfFile;
	}

	
}
