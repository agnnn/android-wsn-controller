package de.rwth.comsys.elf;

/**
 * Data structure reflects an ELF Program Header. value = -1 is equivalent to null. Info:
 * http://www.skyfree.org/linux/references/ELF_Format.pdf
 * 
 * @author Christian & Stephan
 */
public class ProgramHeader
{
	private long type;
	private long offset;
	private long virtualAddress;
	private long physicalAddress;
	private long fileSize;
	private long memorySize;
	private long flags;
	private long alignment;




	/**
	 * Constructor. Initializes variables with -1 (null).
	 */
	public ProgramHeader()
	{
		this.type = -1;
		this.offset = -1;
		this.virtualAddress = -1;
		this.physicalAddress = -1;
		this.fileSize = -1;
		this.memorySize = -1;
		this.flags = -1;
		this.alignment = -1;
	}




	/**
	 * Checks if all attributes are set.
	 * 
	 * @return true if all attributes != -1
	 */
	public boolean hasNoNullAttribute()
	{
		if (this.type == -1)
			return false;
		if (this.offset == -1)
			return false;
		if (this.virtualAddress == -1)
			return false;
		if (this.physicalAddress == -1)
			return false;
		if (this.fileSize == -1)
			return false;
		if (this.memorySize == -1)
			return false;
		if (this.flags == -1)
			return false;
		if (this.alignment == -1)
			return false;
		return true;
	}




	/**
	 * @return the type
	 */
	public long getType()
	{
		return type;
	}




	/**
	 * @param type the type to set
	 */
	public void setType(long type)
	{
		this.type = type;
	}




	/**
	 * @return the offset
	 */
	public long getOffset()
	{
		return offset;
	}




	/**
	 * @param offset the offset to set
	 */
	public void setOffset(long offset)
	{
		this.offset = offset;
	}




	/**
	 * @return the virtualAddress
	 */
	public long getVirtualAddress()
	{
		return virtualAddress;
	}




	/**
	 * @param virtualAddress the virtualAddress to set
	 */
	public void setVirtualAddress(long virtualAddress)
	{
		this.virtualAddress = virtualAddress;
	}




	/**
	 * @return the physicalAddress
	 */
	public long getPhysicalAddress()
	{
		return physicalAddress;
	}




	/**
	 * @param physicalAddress the physicalAddress to set
	 */
	public void setPhysicalAddress(long physicalAddress)
	{
		this.physicalAddress = physicalAddress;
	}




	/**
	 * @return the fileSize
	 */
	public long getFileSize()
	{
		return fileSize;
	}




	/**
	 * @param fileSize the fileSize to set
	 */
	public void setFileSize(long fileSize)
	{
		this.fileSize = fileSize;
	}




	/**
	 * @return the memorySize
	 */
	public long getMemorySize()
	{
		return memorySize;
	}




	/**
	 * @param memorySize the memorySize to set
	 */
	public void setMemorySize(long memorySize)
	{
		this.memorySize = memorySize;
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
	 * @return the alignment
	 */
	public long getAlignment()
	{
		return alignment;
	}




	/**
	 * @param alignment the alignment to set
	 */
	public void setAlignment(long alignment)
	{
		this.alignment = alignment;
	}

}
