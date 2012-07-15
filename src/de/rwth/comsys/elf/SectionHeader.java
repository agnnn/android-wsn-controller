package de.rwth.comsys.elf;

/**
 * Data structure reflects a section header of ELF. value = -1 is equivalent to null.
 * Info: http://www.skyfree.org/linux/references/ELF_Format.pdf
 * 
 * @author Christian & Stephan
 * 
 */
public class SectionHeader
{

	private String name;
	private long offsetInStringTable; // name
	private long type;
	private long flags;
	private long address;
	private long sectionFileOffset;
	private long sectionSize;
	private long linkToAnotherSection;
	private long info;
	private long alignment;
	private long fixedTableEntrySize;




	/**
	 * Constructor. Initializes variables with -1 (null).
	 */
	public SectionHeader()
	{
		this.setName("");
		this.offsetInStringTable = -1;
		this.type = -1;
		this.flags = -1;
		this.address = -1;
		this.setSectionFileOffset(-1);
		this.sectionSize = -1;
		this.linkToAnotherSection = -1;
		this.info = -1;
		this.alignment = -1;
		this.fixedTableEntrySize = -1;
	}




	/**
	 * Checks if all attributes are set.
	 * 
	 * @return true if all attributes != -1
	 */
	public boolean hasNoNullAttribute()
	{
		if (this.offsetInStringTable == -1)
			return false;
		if (this.type == -1)
			return false;
		if (this.flags == -1)
			return false;
		if (this.address == -1)
			return false;
		if (this.getSectionFileOffset() == -1)
			return false;
		if (this.sectionSize == -1)
			return false;
		if (this.linkToAnotherSection == -1)
			return false;
		if (this.info == -1)
			return false;
		if (this.alignment == -1)
			return false;
		if (this.fixedTableEntrySize == -1)
			return false;

		return true;
	}




	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}




	/**
	 * @param name the name to set
	 */
	public void setName(String name)
	{
		this.name = name;
	}




	/**
	 * @return the sectionFileOffset
	 */
	public long getSectionFileOffset()
	{
		return sectionFileOffset;
	}




	/**
	 * @param sectionFileOffset the sectionFileOffset to set
	 */
	public void setSectionFileOffset(long sectionFileOffset)
	{
		this.sectionFileOffset = sectionFileOffset;
	}




	/**
	 * @return the sectionSize
	 */
	public long getSectionSize()
	{
		return sectionSize;
	}




	/**
	 * @param sectionSize the sectionSize to set
	 */
	public void setSectionSize(long sectionSize)
	{
		this.sectionSize = sectionSize;
	}




	/**
	 * @return the offsetInStringTable
	 */
	public long getOffsetInStringTable()
	{
		return offsetInStringTable;
	}




	/**
	 * @param offsetInStringTable the offsetInStringTable to set
	 */
	public void setOffsetInStringTable(long offsetInStringTable)
	{
		this.offsetInStringTable = offsetInStringTable;
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
	 * @return the address
	 */
	public long getAddress()
	{
		return address;
	}




	/**
	 * @param address the address to set
	 */
	public void setAddress(long address)
	{
		this.address = address;
	}




	/**
	 * @return the linkToAnotherSection
	 */
	public long getLinkToAnotherSection()
	{
		return linkToAnotherSection;
	}




	/**
	 * @param linkToAnotherSection the linkToAnotherSection to set
	 */
	public void setLinkToAnotherSection(long linkToAnotherSection)
	{
		this.linkToAnotherSection = linkToAnotherSection;
	}




	/**
	 * @return the info
	 */
	public long getInfo()
	{
		return info;
	}




	/**
	 * @param info the info to set
	 */
	public void setInfo(long info)
	{
		this.info = info;
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




	/**
	 * @return the fixedTableEntrySize
	 */
	public long getFixedTableEntrySize()
	{
		return fixedTableEntrySize;
	}




	/**
	 * @param fixedTableEntrySize the fixedTableEntrySize to set
	 */
	public void setFixedTableEntrySize(long fixedTableEntrySize)
	{
		this.fixedTableEntrySize = fixedTableEntrySize;
	}

}
