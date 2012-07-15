package de.rwth.comsys.elf;

/**
 * Data structure reflects an ELF header. value = -1 is equivalent to null. Info:
 * http://www.skyfree.org/linux/references/ELF_Format.pdf
 * 
 * @author Christian & Stephan
 */
public class Header
{

	private short[] ident;
	private int type;
	private int architecture;
	private long version;
	private long entryPointAddress;
	private long programHeaderTableFileOffset;
	private long sectionHeaderTableFileOffset;
	private long flags;
	private int headerSizeInBytes;
	private int programHeaderTableEntrySize;
	private int programHeaderTableEntryCount;
	private int sectionHeaderTableEntrySize;
	private int sectionHeaderTableEntryCount;
	private int sectionHeaderStringTableIndex;

	static final int SIZE = 52; // count of all hex values of an elf header




	/**
	 * Constructor. Initializes variables with -1 (null).
	 */
	public Header()
	{
		this.ident = new short[16];

		for (int i = 0; i < this.ident.length; i++)
		{
			this.ident[i] = -1;
		}

		this.ident = new short[16];
		this.type = -1;
		this.architecture = -1;
		this.version = -1;
		this.entryPointAddress = -1;
		this.programHeaderTableFileOffset = -1;
		this.sectionHeaderTableFileOffset = -1;
		this.flags = -1;
		this.headerSizeInBytes = -1;
		this.programHeaderTableEntrySize = -1;
		this.programHeaderTableEntryCount = -1;
		this.sectionHeaderTableEntrySize = -1;
		this.sectionHeaderTableEntryCount = -1;
		this.sectionHeaderStringTableIndex = -1;
	}




	/**
	 * Checks if all attributes are set.
	 * 
	 * @return true if all attributes != -1
	 */
	public boolean hasNoNullAttribute()
	{
		for (int i = 0; i < this.ident.length; i++)
		{
			if (this.ident[i] == -1)
				return false;
		}

		if (this.type == -1)
			return false;
		if (this.architecture == -1)
			return false;
		if (this.version == -1)
			return false;
		if (this.entryPointAddress == -1)
			return false;
		if (this.programHeaderTableFileOffset == -1)
			return false;
		if (this.sectionHeaderTableFileOffset == -1)
			return false;
		if (this.flags == -1)
			return false;
		if (this.headerSizeInBytes == -1)
			return false;
		if (this.programHeaderTableEntrySize == -1)
			return false;
		if (this.programHeaderTableEntryCount == -1)
			return false;
		if (this.sectionHeaderTableEntrySize == -1)
			return false;
		if (this.sectionHeaderTableEntryCount == -1)
			return false;
		if (this.sectionHeaderStringTableIndex == -1)
			return false;

		return true;
	}




	/**
	 * @return the ident
	 */
	public short[] getIdent()
	{
		return ident;
	}




	/**
	 * @param ident the ident to set
	 */
	public void setIdent(short[] ident)
	{
		this.ident = ident;
	}




	/**
	 * @return the type
	 */
	public int getType()
	{
		return type;
	}




	/**
	 * @param type the type to set
	 */
	public void setType(int type)
	{
		this.type = type;
	}




	/**
	 * @return the architecture
	 */
	public int getArchitecture()
	{
		return architecture;
	}




	/**
	 * @param architecture the architecture to set
	 */
	public void setArchitecture(int architecture)
	{
		this.architecture = architecture;
	}




	/**
	 * @return the version
	 */
	public long getVersion()
	{
		return version;
	}




	/**
	 * @param version the version to set
	 */
	public void setVersion(long version)
	{
		this.version = version;
	}




	/**
	 * @return the entryPointAddress
	 */
	public long getEntryPointAddress()
	{
		return entryPointAddress;
	}




	/**
	 * @param entryPointAddress the entryPointAddress to set
	 */
	public void setEntryPointAddress(long entryPointAddress)
	{
		this.entryPointAddress = entryPointAddress;
	}




	/**
	 * @return the programHeaderTableFileOffset
	 */
	public long getProgramHeaderTableFileOffset()
	{
		return programHeaderTableFileOffset;
	}




	/**
	 * @param programHeaderTableFileOffset the programHeaderTableFileOffset to set
	 */
	public void setProgramHeaderTableFileOffset(long programHeaderTableFileOffset)
	{
		this.programHeaderTableFileOffset = programHeaderTableFileOffset;
	}




	/**
	 * @return the sectionHeaderTableFileOffset
	 */
	public long getSectionHeaderTableFileOffset()
	{
		return sectionHeaderTableFileOffset;
	}




	/**
	 * @param sectionHeaderTableFileOffset the sectionHeaderTableFileOffset to set
	 */
	public void setSectionHeaderTableFileOffset(long sectionHeaderTableFileOffset)
	{
		this.sectionHeaderTableFileOffset = sectionHeaderTableFileOffset;
	}




	/**
	 * @return the flags
	 */
	public long getFlags()
	{
		return flags;
	}




	/**
	 * @param flags the flags to set
	 */
	public void setFlags(long flags)
	{
		this.flags = flags;
	}




	/**
	 * @return the headerSizeInBytes
	 */
	public int getHeaderSizeInBytes()
	{
		return headerSizeInBytes;
	}




	/**
	 * @param headerSizeInBytes the headerSizeInBytes to set
	 */
	public void setHeaderSizeInBytes(int headerSizeInBytes)
	{
		this.headerSizeInBytes = headerSizeInBytes;
	}




	/**
	 * @return the programHeaderTableEntrySize
	 */
	public int getProgramHeaderTableEntrySize()
	{
		return programHeaderTableEntrySize;
	}




	/**
	 * @param programHeaderTableEntrySize the programHeaderTableEntrySize to set
	 */
	public void setProgramHeaderTableEntrySize(int programHeaderTableEntrySize)
	{
		this.programHeaderTableEntrySize = programHeaderTableEntrySize;
	}




	/**
	 * @return the programHeaderTableEntryCount
	 */
	public int getProgramHeaderTableEntryCount()
	{
		return programHeaderTableEntryCount;
	}




	/**
	 * @param programHeaderTableEntryCount the programHeaderTableEntryCount to set
	 */
	public void setProgramHeaderTableEntryCount(int programHeaderTableEntryCount)
	{
		this.programHeaderTableEntryCount = programHeaderTableEntryCount;
	}




	/**
	 * @return the sectionHeaderTableEntrySize
	 */
	public int getSectionHeaderTableEntrySize()
	{
		return sectionHeaderTableEntrySize;
	}




	/**
	 * @param sectionHeaderTableEntrySize the sectionHeaderTableEntrySize to set
	 */
	public void setSectionHeaderTableEntrySize(int sectionHeaderTableEntrySize)
	{
		this.sectionHeaderTableEntrySize = sectionHeaderTableEntrySize;
	}




	/**
	 * @return the sectionHeaderTableEntryCount
	 */
	public int getSectionHeaderTableEntryCount()
	{
		return sectionHeaderTableEntryCount;
	}




	/**
	 * @param sectionHeaderTableEntryCount the sectionHeaderTableEntryCount to set
	 */
	public void setSectionHeaderTableEntryCount(int sectionHeaderTableEntryCount)
	{
		this.sectionHeaderTableEntryCount = sectionHeaderTableEntryCount;
	}




	/**
	 * @return the sectionHeaderStringTableIndex
	 */
	public int getSectionHeaderStringTableIndex()
	{
		return sectionHeaderStringTableIndex;
	}




	/**
	 * @param sectionHeaderStringTableIndex the sectionHeaderStringTableIndex to set
	 */
	public void setSectionHeaderStringTableIndex(int sectionHeaderStringTableIndex)
	{
		this.sectionHeaderStringTableIndex = sectionHeaderStringTableIndex;
	}
}
