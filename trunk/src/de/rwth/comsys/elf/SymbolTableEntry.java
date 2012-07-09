package de.rwth.comsys.elf;

public class SymbolTableEntry
{
	private String name;
	private long offsetInStringTable; // name
	private long value; // reflects absolute value, address etc by context
	private long size;
	private short info;
	private short other;
	private int sectionIndex;




	/**
	 * Constructor. Initializes variables with -1 (null).
	 */
	public SymbolTableEntry()
	{
		this.setName("");
		this.offsetInStringTable = -1;
		this.value = -1;
		this.size = -1;
		this.info = -1;
		this.other = -1;
		this.sectionIndex = -1;
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
		if (this.value == -1)
			return false;
		if (this.size == -1)
			return false;
		if (this.info == -1)
			return false;
		if (this.other == -1)
			return false;
		if (this.sectionIndex == -1)
			return false;

		return true;
	}




	/**
	 * @return the value
	 */
	public long getValue()
	{
		return value;
	}




	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(long value)
	{
		this.value = value;
	}




	/**
	 * @return the size
	 */
	public long getSize()
	{
		return size;
	}




	/**
	 * @param size
	 *            the size to set
	 */
	public void setSize(long size)
	{
		this.size = size;
	}




	/**
	 * @return the info
	 */
	public short getInfo()
	{
		return info;
	}




	/**
	 * @param info
	 *            the info to set
	 */
	public void setInfo(short info)
	{
		this.info = info;
	}




	/**
	 * @return the other
	 */
	public short getOther()
	{
		return other;
	}




	/**
	 * @param other
	 *            the other to set
	 */
	public void setOther(short other)
	{
		this.other = other;
	}




	/**
	 * @return the sectionIndex
	 */
	public int getSectionIndex()
	{
		return sectionIndex;
	}




	/**
	 * @param sectionIndex
	 *            the sectionIndex to set
	 */
	public void setSectionIndex(int sectionIndex)
	{
		this.sectionIndex = sectionIndex;
	}




	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}




	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name)
	{
		this.name = name;
	}




	/**
	 * @return the offsetInStringTable
	 */
	public long getOffsetInStringTable()
	{
		return offsetInStringTable;
	}




	/**
	 * @param offsetInStringTable
	 *            the offsetInStringTable to set
	 */
	public void setOffsetInStringTable(long offsetInStringTable)
	{
		this.offsetInStringTable = offsetInStringTable;
	}

}
