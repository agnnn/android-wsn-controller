package de.rwth.comsys.ihex;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import de.rwth.comsys.enums.ErrorCodes;
import de.rwth.comsys.enums.RecordTypes;

import android.os.Environment;
import android.util.Log;

/**
 * Used to load an iHex file from external storage and to parse the file.
 * 
 * @author Christian & Stephan
 * 
 */
public class HexLoader
{

	private ArrayList<Record> records = new ArrayList<Record>();
	private File iHexFile = null;
	private long dateOfFile = 0;





	private HexLoader()
	{
	}




	/**
	 * Loads a iHex file from the given position on storage. Parses the file to Records.
	 * Does some syntax/semantic checks.
	 * 
	 * @param pathToFile
	 * @return HexLoader or null if error occurs
	 */
	public static HexLoader createHexLoader(String pathToFile)
	{	
		//check input
		if(pathToFile == null) return null;
		//TODO handleError
		
		HexLoader newHexLoader = new HexLoader();
		
		// check access to storage
		if (newHexLoader.checkAccessToExternalStorage() == false)
			return null;
		
		
		
		newHexLoader.iHexFile = new File(pathToFile);

		try
		{

			FileReader fileReader = new FileReader(newHexLoader.iHexFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String row = "";

			while ((row = bufferedReader.readLine()) != null)
			{
				if (newHexLoader.parseToRecord(row) == false)
				{
					newHexLoader.handleError(ErrorCodes.HEXLOADER_RECORD_PARSING_ERROR, "");
					bufferedReader.close();
					return null;
				}
			}

			bufferedReader.close();

			// check for Start Segment Address Record
			if (Record.checkStartSegmentAddressRecord(newHexLoader.records) == false)
			{
				newHexLoader.handleError(ErrorCodes.HEXLOADER_RECORD_INVALID_START_SEGMENT_ADDRESS_RECORD, "");
				return null;
			}

			// check for end of file record
			if (Record.checkEndOfFileRecord(newHexLoader.records) == false)
			{
				newHexLoader.handleError(ErrorCodes.HEXLOADER_RECORD_INVALID_END_OF_FILE_RECORD, "");
				return null;
			}

			// set date of file
			newHexLoader.setDateOfFile(newHexLoader.iHexFile.lastModified());

		} catch (IOException e)
		{
			newHexLoader.handleError(ErrorCodes.HEXLOADER_CANT_LOAD_FILE, newHexLoader.iHexFile.toString() + " " + e.toString());
		}
		
		
		
		return newHexLoader;

	}




	/**
	 * Handles error and if necessary, clears "records".
	 */
	private void handleError(ErrorCodes e, String additionalMsg)
	{

		// log error
		switch (e)
		{
		case HEXLOADER_RECORD_PARSING_ERROR:
			Log.w("Hexloader", "Error: Can't parse file!");
			break;
		case HEXLOADER_RECORD_INVALID_START_SEGMENT_ADDRESS_RECORD:
			Log.w("Hexloader", "Error: No start segment address record found or double entry!");
			break;
		case HEXLOADER_NO_ACCESS_TO_STORAGE:
			Log.w("Hexloader", "Error: No access to storage!");
			break;
		case HEXLOADER_CANT_LOAD_FILE:
			Log.w("Hexloader", "Error: Can't read from file: " + additionalMsg);
			break;
		case HEXLOADER_RECORD_INVALID_END_OF_FILE_RECORD:
			Log.w("Hexloader", "Error: No end of file record found or double entry!");
			break;
		default:
			break;
		}
	}




	/**
	 * Converts a IntelHex row into a Record.
	 * 
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

		// split line into characters
		for (int i = 0; i < row.length(); i++)
		{
			splittedRow[i] = row.charAt(i);
		}

		// start parsing
		try
		{

			// check startSymbol
			startCode = String.valueOf(splittedRow[0]);
			if (startCode.equals(":") == false)
				return false;

			// offset of 2, because we want to parse a byte
			byteCount = Short.decode("0x" + new String(splittedRow, 1, 2));
			addressHighByte = Short.decode("0x" + new String(splittedRow, 3, 2));
			addressLowByte = Short.decode("0x" + new String(splittedRow, 5, 2));
			short recordTypeAsShort = Short.parseShort(new String(splittedRow, 7, 2));

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
			default:
				return false;
			}

			data = new short[byteCount];

			int j = 9; // start char
			for (int i = 0; (i < data.length) && (j < splittedRow.length); i++)
			{

				data[i] = Short.decode("0x" + new String(splittedRow, j, 2));
				j = j + 2;
			}

			// create record and check checksum
			Record record = Record.createRecord(byteCount, addressHighByte, addressLowByte, recordType, data);

			// successfully created?
			if (record == null)
			{
				return false;
			}
			else
			{
				records.add(record);
				return true;
			}
		} catch (Throwable t)
		{
			Log.w("HexLoader", "Parsing error: ", t);
			return false;
		}
	}

	/**
	 * Stores all Records in a new file with the given filename at the given path.
	 * 
	 * @param path
	 * @param filename
	 * @return true if writing was successful
	 */
	public boolean storeRecords(String path, String filename)
	{
		// check access to storage
		if (this.checkAccessToExternalStorage() == false)
			return false;

		File file = new File(path + filename);

		try
		{
			boolean ok = file.createNewFile();
			if (ok == false)
			{
				return false;
			}
			
			FileWriter fileWriter = new FileWriter(file);
			
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

			String currentRow = "";
			
			for (Iterator<Record> iterator = records.iterator(); iterator.hasNext();)
			{
				currentRow = iterator.next().toString();
				
				if(iterator.hasNext())
				{
					bufferedWriter.append(currentRow+"\n");
				}
				else
				{
					bufferedWriter.write(currentRow);
				}
			}
			
			bufferedWriter.close();

		} catch (Exception e)
		{
			return false;
		}

		return true;
	}


	/**
	 * Checks if we can read/write to external storage.
	 * 
	 * @return boolean
	 */
	private boolean checkAccessToExternalStorage()
	{
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state))
		{
			// We can read and write the media
			return true;
		}

		// Can't read nor write
		this.handleError(ErrorCodes.HEXLOADER_NO_ACCESS_TO_STORAGE, "");
		return false;
	}




	/**
	 * @return the records
	 */
	public ArrayList<Record> getRecords()
	{
		return records;
	}




	/**
	 * 
	 * @return the dateOfFile 0 if no file exists.
	 */
	public long getDateOfFile()
	{
		return dateOfFile;
	}




	/**
	 * @param dateOfFile the dateOfFile to set
	 */
	public void setDateOfFile(long dateOfFile)
	{
		this.dateOfFile = dateOfFile;
	}

}
