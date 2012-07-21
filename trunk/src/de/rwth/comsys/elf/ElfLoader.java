package de.rwth.comsys.elf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import android.os.Environment;

import de.rwth.comsys.enums.ELFErrorCode;
import de.rwth.comsys.enums.RecordTypes;
import de.rwth.comsys.helpers.ByteConverter;
import de.rwth.comsys.ihex.Record;

/**
 * Used to load a ELF file from external storage and to parse the file. Limited to files
 * smaller than the max value of Integers in Bytes.
 * 
 * @author Christian & Stephan
 * 
 */
public class ElfLoader
{

	private byte[] loadedFile = null;
	private Header header = null;
	private ArrayList<ProgramHeader> programHeaders = null;
	private ArrayList<SectionHeader> sectionHeaders = null;
	private ArrayList<byte[]> sectionDataArrays = null;
	private HashMap<Integer, String> sectionHeaderNames = null;
	private HashMap<Integer, String> symbolTableEntryNames = null;
	private ArrayList<SymbolTableEntry> symbolTableEntries = null;




	private ElfLoader()
	{
	}




	/**
	 * Loads a ELF file and parses the file.
	 * 
	 * @param pathName
	 * @return ElfLoader or null if error occurs
	 */
	public static ElfLoader createElfLoader(String pathName)
	{
		int offset = 0;
		int sectionLength = 0;
		ElfLoader newElfLoader = new ElfLoader();

		// check input
		if (pathName == null)
			return null;
		// TODO handleError

		// check access to storage
		if (newElfLoader.checkAccessToExternalStorage() == false)
			return null;

		File file = new File(pathName);
		FileInputStream input;

		try
		{
			input = new FileInputStream(file);

		} catch (Exception e)
		{
			return null;
		}

		// should never happen, because we are dealing with MCUs
		if (file.length() > Integer.MAX_VALUE)
		{
			newElfLoader.errorHandler(ELFErrorCode.ELF_FILE_TOO_LARGE);
			return null;
		}

		byte[] buffer = new byte[(int) file.length()];

		try
		{
			// fill buffer
			if (input.read(buffer) == -1)
				return null;

			input.close();

		} catch (Exception e)
		{
			return null;
		}

		newElfLoader.loadedFile = buffer;

		// parse header
		newElfLoader.header = newElfLoader.parseElfHeader();
		if (newElfLoader.header == null || !newElfLoader.header.hasNoNullAttribute())
		{
			newElfLoader.errorHandler(ELFErrorCode.ELF_PARSE_HEADER_ERROR);
			return null;
		}

		// parse section headers
		newElfLoader.sectionHeaders = newElfLoader.parseSectionHeader(
				(int) newElfLoader.header.getSectionHeaderTableFileOffset(),
				newElfLoader.header.getSectionHeaderTableEntrySize()
						* newElfLoader.header.getSectionHeaderTableEntryCount(),
				newElfLoader.header.getSectionHeaderTableEntrySize());
		if (newElfLoader.header == null)
		{
			newElfLoader.errorHandler(ELFErrorCode.ELF_PARSE_SECTION_HEADERS_ERROR);
			return null;
		}

		// parse section header names
		SectionHeader stringTableSectionHeader = newElfLoader.sectionHeaders.get(newElfLoader.header
				.getSectionHeaderStringTableIndex());
		offset = (int) stringTableSectionHeader.getSectionFileOffset();
		sectionLength = (int) stringTableSectionHeader.getSectionSize();
		newElfLoader.sectionHeaderNames = newElfLoader.parseStringTable(offset, sectionLength);
		if (newElfLoader.sectionHeaderNames == null)
		{
			newElfLoader.errorHandler(ELFErrorCode.ELF_PARSE_SECTION_HEADERS_NAMES_ERROR);
			return null;
		}

		// solve name by offsetInStringTable and sectionHeaderNames
		for (Iterator<SectionHeader> iterator = newElfLoader.sectionHeaders.iterator(); iterator.hasNext();)
		{
			SectionHeader currentSectionHeader = iterator.next();
			Integer key = (int) (stringTableSectionHeader.getSectionFileOffset() + currentSectionHeader
					.getOffsetInStringTable());
			currentSectionHeader.setName(newElfLoader.sectionHeaderNames.get(key));
		}

		// parse program headers
		newElfLoader.programHeaders = newElfLoader.parseProgramHeaders(
				(int) newElfLoader.header.getProgramHeaderTableFileOffset(),
				newElfLoader.header.getProgramHeaderTableEntrySize()
						* newElfLoader.header.getProgramHeaderTableEntryCount(),
				newElfLoader.header.getProgramHeaderTableEntrySize());
		if (newElfLoader.programHeaders == null)
		{
			newElfLoader.errorHandler(ELFErrorCode.ELF_PARSE_PROGRAM_HEADERS_ERROR);
			return null;
		}

		// search symbolTableSection by symbolTableHeader
		SectionHeader symbolTableSectionHeader = null;
		String symtab = ".symtab";
		for (Iterator<SectionHeader> iterator = newElfLoader.sectionHeaders.iterator(); iterator.hasNext();)
		{
			SectionHeader currentSectionHeader = iterator.next();

			if (symtab.equals(currentSectionHeader.getName()))
			{
				symbolTableSectionHeader = currentSectionHeader;
				break;
			}
		}
		if (symbolTableSectionHeader == null)
		{
			newElfLoader.errorHandler(ELFErrorCode.ELF_SYMTABLE_SECTION_HEADER_NOT_FOUND_ERROR);
			return null;
		}

		// parse symbolTable Section
		offset = (int) symbolTableSectionHeader.getSectionFileOffset();
		sectionLength = (int) symbolTableSectionHeader.getSectionSize();
		int symbolTableEntrySize = (int) symbolTableSectionHeader.getFixedTableEntrySize();
		newElfLoader.symbolTableEntries = newElfLoader.parseSymbolTable(offset, sectionLength, symbolTableEntrySize);
		if (newElfLoader.symbolTableEntries == null)
		{
			newElfLoader.errorHandler(ELFErrorCode.ELF_PARSE_SYMTABLE_ERROR);
			return null;
		}

		// search symbolTableNameSection by symbolTableNameHeader
		SectionHeader symbolTableNameHeader = null;
		String symbolTableNameSectionName = ".strtab";
		for (Iterator<SectionHeader> iterator = newElfLoader.sectionHeaders.iterator(); iterator.hasNext();)
		{
			SectionHeader currentSectionHeader = iterator.next();

			if (symbolTableNameSectionName.equals(currentSectionHeader.getName()))
			{
				symbolTableNameHeader = currentSectionHeader;
				break;
			}
		}
		if (symbolTableNameHeader == null)
		{
			newElfLoader.errorHandler(ELFErrorCode.ELF_SYMTABLE_NAMES_SECTION_HEADER_NOT_FOUND_ERROR);
			return null;
		}

		// parse symbolTableNames
		offset = (int) symbolTableNameHeader.getSectionFileOffset();
		sectionLength = (int) symbolTableNameHeader.getSectionSize();
		newElfLoader.symbolTableEntryNames = newElfLoader.parseStringTable(offset, sectionLength);
		if (newElfLoader.symbolTableEntryNames == null)
		{
			newElfLoader.errorHandler(ELFErrorCode.ELF_PARSE_SYMBTABLE_ENTRY_NAMES_ERROR);
			return null;
		}

		// solve name by offsetInStringTable and sectionHeaderNames
		for (Iterator<SymbolTableEntry> iterator = newElfLoader.symbolTableEntries.iterator(); iterator.hasNext();)
		{
			SymbolTableEntry currentSymbolTableEntry = iterator.next();
			Integer key = (int) (symbolTableNameHeader.getSectionFileOffset() + currentSymbolTableEntry
					.getOffsetInStringTable());
			String name = newElfLoader.symbolTableEntryNames.get(key);
			currentSymbolTableEntry.setName(name);
		}

		return newElfLoader;

	}




	/**
	 * Creates a list of records to flash. Filters ELF segments to get important data by
	 * program header type "PC_LOAD".
	 * 
	 * @return list of records
	 */
	public ArrayList<Record> createIhexRecords()
	{
		ArrayList<Record> result = new ArrayList<Record>();
		Record record = null;
		byte[] littleEndian = null;
		byte[] dataChunk = null;
		byte[] currentSegment = null;
		long offset = 0;
		long size = 0;

		// first PC_LOAD segment can have wrong physical start address,
		// because of page size... see ELF documentation.
		int startAddress = (int) header.getEntryPointAddress();
		boolean firstSegmentPassed = false;

		for (ProgramHeader curHeader : programHeaders)
		{

			if (curHeader.getType() == 1) // PC_LOAD
			{
				// MSP430 compiler fault -> get first segment by SectionHeader ".text"
				if (!firstSegmentPassed)
				{	
					if(sectionHeaders!=null)
					{	
						String textHeaderString = ".text";
						for (Iterator<SectionHeader> iterator = sectionHeaders.iterator(); iterator.hasNext();)
						{
							SectionHeader currentSectionHeader = iterator.next();
							if(textHeaderString.equals(currentSectionHeader))
							{
								offset = currentSectionHeader.getSectionFileOffset();
								size = curHeader.getFileSize() - offset;
								break;
							}
							
						}
						
					
					}
					else
					{
						return null;
					}
				}
				else
				{
					offset = curHeader.getOffset();
					size = curHeader.getFileSize();
				}

				currentSegment = Arrays.copyOfRange(loadedFile, (int) offset, (int) (offset + size));
				int processedBytes = currentSegment.length;

				// split segment into records
				for (int i = 0; i < currentSegment.length; i += 16)
				{

					if (processedBytes > 16)
					{
						dataChunk = Arrays.copyOfRange(currentSegment, i, i + 16);
						processedBytes -= 16;
					}
					else
					{
						dataChunk = Arrays.copyOfRange(currentSegment, i, i + processedBytes);
					}

					// ensure correct start address
					if (!firstSegmentPassed)
					{
						// MSP430 compiler fault -> hardcoded
						offset = 0x94;
						littleEndian = createLittleEndianIntArrayByValue(startAddress + i);

					}
					else
					{
						littleEndian = createLittleEndianIntArrayByValue((int) (curHeader.getPhysicalAddress() + i));

					}
					record = Record.createRecord((short) dataChunk.length, (short) littleEndian[1],
							(short) littleEndian[0], RecordTypes.DATA_RECORD,
							ByteConverter.convertByteArrayToShortArray(dataChunk));

					result.add(record);
				}

				firstSegmentPassed = true;

			}
		}

		// Start Segment Address Record
		record = Record.createStartSegmentAddressRecord(0, startAddress);
		result.add(record);

		// End Of File Record
		record = Record.createEndOfFileRecord();
		result.add(record);

		return result;
	}




	/**
	 * Calculates position in loadedFile of a variable by name.
	 * 
	 * @param name
	 * @param sizeInBytes
	 * @return null or Variable
	 */
	public Variable getVariableBySymbolTableName(String name)
	{
		if (symbolTableEntries == null || sectionHeaders == null || name == "")
			return null;

		Variable variable = null;

		SymbolTableEntry searchedEntry = null;

		for (Iterator<SymbolTableEntry> iterator = symbolTableEntries.iterator(); iterator.hasNext();)
		{
			SymbolTableEntry currentSymbolTableEntry = iterator.next();
			String currentName = currentSymbolTableEntry.getName();
			if (currentName != null && currentName.equals(name))
			{
				searchedEntry = currentSymbolTableEntry;
				break;
			}
		}

		// search file offset of name
		if (searchedEntry != null)
		{
			// belongs to section ?
			SectionHeader sectionHeader = sectionHeaders.get(searchedEntry.getSectionIndex());

			// calculate offset in file
			int offset = (int) (sectionHeader.getSectionFileOffset() + (searchedEntry.getValue() - sectionHeader
					.getAddress()));
			int size = (int) searchedEntry.getSize();
			variable = new Variable(name, offset, size, (int) getValue(offset, size));

		}

		return variable;
	}




	/**
	 * Manipulates loadedFile by name and value.
	 * 
	 * @param name of entry in symbolTable
	 * @param value to write
	 * @return true if writing was successful
	 */
	public boolean manipulateLoadedFileByName(String name, int value)
	{
		// get offset and size of entry
		Variable var = getVariableBySymbolTableName(name);
		if (var == null || var.hasNoNullAttribute() == false
				|| (loadedFile.length < var.getSizeInBytes() + var.getFileOffset()))
		{
			errorHandler(ELFErrorCode.ELF_VARIABLE_INCORRECT);
			return false;
		}

		// transform value to little endian format
		byte[] valuesToWrite = createLittleEndianIntArrayByValue(value);

		// compare size of old value with new value
		int size = 0;
		for (int i = 0; i < valuesToWrite.length; i++)
		{
			if (valuesToWrite[i] != 0)
			{
				size++;
			}

		}

		// ensure to not run out of range and to overwrite all bytes
		if (size > var.getSizeInBytes() || var.getSizeInBytes() > 4)
			return false;

		// overwrite up to 4 bytes
		for (int i = 0; i < valuesToWrite.length && i < var.getSizeInBytes(); i++)
		{
			byte currentByte = valuesToWrite[i];
			loadedFile[var.getFileOffset() + i] = currentByte;
		}
		// TODO write zeros if size is large than 4 bytes

		return true;
	}




	/**
	 * Stores the loadedFile in a new file with the given filename at the given path.
	 * 
	 * @param path
	 * @param filename
	 * @return true if writing was successful
	 */
	public boolean storeLoadedFile(String path, String filename)
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

			FileOutputStream output = new FileOutputStream(file);
			output.write(loadedFile, 0, loadedFile.length);
			output.close();

		} catch (Exception e)
		{
			return false;
		}

		return true;
	}




	/**
	 * Parses section headers of ELF. Parse ELF header at first to get sectionHeaderSize
	 * etc..
	 * 
	 * @param offset
	 * @param sectionLength
	 * @param sectionHeaderSize
	 * @return
	 */
	private ArrayList<ProgramHeader> parseProgramHeaders(int offset, int sectionLength, int programHeaderSize)
	{
		ArrayList<ProgramHeader> programHeaders = new ArrayList<ProgramHeader>();

		for (int i = offset; (i < this.loadedFile.length) && (i < sectionLength + offset); i = i + programHeaderSize)
		{
			ProgramHeader currentHeader = new ProgramHeader();

			currentHeader.setType(getValue(i, 4));
			currentHeader.setOffset(getValue(i + 4, 4));
			currentHeader.setVirtualAddress(getValue(i + 8, 4));
			currentHeader.setPhysicalAddress(getValue(i + 12, 4));
			currentHeader.setFileSize(getValue(i + 16, 4));
			currentHeader.setMemorySize(getValue(i + 20, 4));
			currentHeader.setFlags(getValue(i + 24, 4));
			currentHeader.setAlignment(getValue(i + 28, 4));

			// parsed successful?
			if (currentHeader.hasNoNullAttribute())
			{
				programHeaders.add(currentHeader);
			}
			else
			{
				return null;
			}
		}

		return programHeaders;
	}




	/**
	 * Parses section headers of ELF. Parse ELF header at first to get sectionHeaderSize
	 * etc..
	 * 
	 * @param offset
	 * @param sectionLength
	 * @param sectionHeaderSize
	 * @return
	 */
	private ArrayList<SectionHeader> parseSectionHeader(int offset, int sectionLength, int sectionHeaderSize)
	{
		ArrayList<SectionHeader> sectionHeaders = new ArrayList<SectionHeader>();

		for (int i = offset; (i < this.loadedFile.length) && (i < sectionLength + offset); i = i + sectionHeaderSize)
		{
			SectionHeader currentHeader = new SectionHeader();

			currentHeader.setOffsetInStringTable(getValue(i, 4));
			currentHeader.setType(getValue(i + 4, 4));
			currentHeader.setFlags(getValue(i + 8, 4));
			currentHeader.setAddress(getValue(i + 12, 4));
			currentHeader.setSectionFileOffset(getValue(i + 16, 4));
			currentHeader.setSectionSize(getValue(i + 20, 4));
			currentHeader.setLinkToAnotherSection(getValue(i + 24, 4));
			currentHeader.setInfo(getValue(i + 28, 4));
			currentHeader.setAlignment(getValue(i + 32, 4));
			currentHeader.setFixedTableEntrySize(getValue(i + 36, 4));

			// parsed successful?
			if (currentHeader.hasNoNullAttribute())
			{
				sectionHeaders.add(currentHeader);
			}
			else
			{
				return null;
			}
		}

		return sectionHeaders;
	}




	/**
	 * Parses header of ELF.
	 * 
	 * @param sectionHeaderByteArray
	 * @param numberOfSections
	 * @return header or null if error occurs
	 */
	private Header parseElfHeader()
	{
		if (loadedFile == null || loadedFile.length < 52)
			return null;

		Header newHeader = new Header();
		short[] ident = newHeader.getIdent();

		// fill ident array
		for (int j = 0; j < (loadedFile.length) && (j < ident.length); j++)
		{
			ident[j] = (short) (loadedFile[j] & 0xFF);
			// TODO check values ... correct elf file?
		}

		newHeader.setType((int) getValue(16, 2));
		newHeader.setArchitecture((int) getValue(18, 2));
		newHeader.setVersion(getValue(20, 4));
		newHeader.setEntryPointAddress(getValue(24, 4));
		newHeader.setProgramHeaderTableFileOffset(getValue(28, 4));
		newHeader.setSectionHeaderTableFileOffset(getValue(32, 4));
		newHeader.setFlags(getValue(36, 4));
		newHeader.setHeaderSizeInBytes((int) getValue(40, 2));
		newHeader.setProgramHeaderTableEntrySize((int) getValue(42, 2));
		newHeader.setProgramHeaderTableEntryCount((int) getValue(44, 2));
		newHeader.setSectionHeaderTableEntrySize((int) getValue(46, 2));
		newHeader.setSectionHeaderTableEntryCount((int) getValue(48, 2));
		newHeader.setSectionHeaderStringTableIndex((int) getValue(50, 2));

		// parsed successful?
		if (!newHeader.hasNoNullAttribute())
		{
			return null;
		}

		return newHeader;
	}




	/**
	 * Parses a symbol table of an ELF file.
	 * 
	 * @param offset
	 * @param symbolTableLength
	 * @param symbolTableEntrySize
	 * @return null or symbolTableEntries
	 */
	private ArrayList<SymbolTableEntry> parseSymbolTable(int offset, int symbolTableLength, int symbolTableEntrySize)
	{
		if (this.loadedFile == null)
			return null;

		ArrayList<SymbolTableEntry> symbolTableEntries = new ArrayList<SymbolTableEntry>();

		for (int i = offset; i < this.loadedFile.length && (i < symbolTableLength + offset); i = i
				+ symbolTableEntrySize)
		{
			SymbolTableEntry currentSymbolTableEntry = new SymbolTableEntry();

			currentSymbolTableEntry.setOffsetInStringTable(getValue(i, 4));
			currentSymbolTableEntry.setValue(getValue(i + 4, 4));
			currentSymbolTableEntry.setSize(getValue(i + 8, 4));
			currentSymbolTableEntry.setInfo((short) getValue(i + 12, 1));
			currentSymbolTableEntry.setOther((short) getValue(i + 13, 1));
			currentSymbolTableEntry.setSectionIndex((int) getValue(i + 14, 2));

			// parsed successful?
			if (currentSymbolTableEntry.hasNoNullAttribute())
			{
				symbolTableEntries.add(currentSymbolTableEntry);
			}
			else
			{
				return null;
			}
		}

		return symbolTableEntries;

	}




	/**
	 * Parses a String table section of ELF.
	 * 
	 * @return hashmap<offset in section, name>
	 */
	private HashMap<Integer, String> parseStringTable(int offset, int tableLength)
	{

		HashMap<Integer, String> names = new HashMap<Integer, String>();
		String name = "";

		for (int i = offset; i < this.loadedFile.length && (i < tableLength + offset); i++)
		{
			short currentByte = (short) (this.loadedFile[i] & 0xFF);
			// new word starts and is not the beginning of reading
			if (currentByte == 0x00 && !name.isEmpty())
			{
				// offset = currentPosition - length of string (ASCII sign ==
				// one byte)
				names.put(i - name.length(), name);
				name = "";
			}
			else if (!(currentByte == 0x00))
			{
				// build string
				name = name + (char) currentByte;
			}
		}

		return names;

	}




	/**
	 * Prints loaded Bytes as HEX.
	 */
	@SuppressWarnings("unused")
	private void printBytesAsHex()
	{
		String output = "";
		if (this.loadedFile == null)
		{
			System.out.print("Nothing was loaded!\n");
		}

		for (int i = 0; i < this.loadedFile.length; i++)
		{
			if (i % 16 == 0)
				System.out.print("\n");

			output = Integer.toHexString(this.loadedFile[i] & 0xFF).toUpperCase();

			if (output.length() == 1)
				output = "0" + output;

			System.out.print(output + " ");
		}

		System.out.print("\n");
	}




	/**
	 * Calculates a value by shifting. Source is loadedFile. Format is little endian.
	 * 
	 * @param offset
	 * @param length of bytes to read
	 * @return -1 if error occurs
	 */
	private long getValue(int offset, int length)
	{
		if (this.loadedFile == null || this.loadedFile.length < (offset + length))
			return -1;

		long tempShift = 0;
		long result = 0;
		int counter = 0;

		for (int i = offset; i < this.loadedFile.length && counter < length; i++)
		{
			if (counter != 0)
			{
				tempShift = this.loadedFile[i] & 0xFF;
				result |= tempShift << (8 * counter);
			}
			else
			{
				result |= this.loadedFile[i] & 0xFF;
			}
			counter++;

		}
		return result;

	}




	/**
	 * Creates an array of value. Format is little endian. Size is always 4.
	 * 
	 * @param value
	 * @return byteArray in little endian format
	 */
	private byte[] createLittleEndianIntArrayByValue(int value)
	{
		byte[] temp = ByteBuffer.allocate(4).putInt(value).array();

		byte[] result = new byte[4];

		// to little endian
		for (int i = 0; i < result.length; i++)
		{
			result[i] = temp[result.length - i - 1];
		}

		return result;
	}




	/**
	 * Handles errors.
	 * 
	 * @param error
	 */
	private void errorHandler(ELFErrorCode error)
	{
		// TODO add all errors
		switch (error)
		{
		case ELF_FILE_TOO_LARGE:
			System.out.println("ELFLoader: File is too large!\n");
			break;
		case ELF_PARSE_HEADER_ERROR:
			System.out.println("ELFLoader: Can't parse header!\n");
			break;
		case ELF_PARSE_SECTION_HEADERS_ERROR:
			System.out.println("ELFLoader: Can't parse section headers!\n");
			break;
		case ELF_PARSE_SECTION_HEADERS_NAMES_ERROR:
			System.out.println("ELFLoader: Can't parse section header names!\n");
			break;
		case ELF_PARSE_SYMTABLE_ERROR:
			System.out.println("ELFLoader: Can't parse symbol table!\n");
			break;
		case ELF_PARSE_SYMBTABLE_ENTRY_NAMES_ERROR:
			System.out.println("ELFLoader: Can't parse symbol table entry names!\n");
			break;
		case ELF_SYMTABLE_SECTION_HEADER_NOT_FOUND_ERROR:
			System.out.println("ELFLoader: Can't find symbol table section header!\n");
			break;
		case ELF_SYMTABLE_NAMES_SECTION_HEADER_NOT_FOUND_ERROR:
			System.out.println("ELFLoader: Can't find section header of symbol table entry names!\n");
			break;
		default:
			System.out.println("ELFLoader: Undefined error!\n");
			break;
		}
	}




	/**
	 * Checks if we can read/write to external storage.
	 * 
	 * @return boolean
	 */
	public boolean checkAccessToExternalStorage()
	{
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state))
		{
			// We can read and write the media
			return true;
		}

		// Can't read nor write
		errorHandler(ELFErrorCode.ELFLOADER_NO_ACCESS_TO_STORAGE);
		return false;
	}




	/**
	 * @return the sectionDataArrays
	 */
	public ArrayList<byte[]> getSectionDataArrays()
	{
		return sectionDataArrays;
	}




	/**
	 * @return the header
	 */
	public Header getHeader()
	{
		return header;
	}

}
