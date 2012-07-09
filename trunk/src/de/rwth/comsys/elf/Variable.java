/**
 * 
 */
package de.rwth.comsys.elf;

/**
 * Data structure for a variable, its position in file and its size in bytes. -1 reflects null.
 * 
 * @author Christian & Stephan
 * 
 */
public class Variable
{
	private String name = "";
	private int fileOffset = -1;
	private int sizeInBytes = -1;
	private int value = -1;




	@SuppressWarnings("unused")
	private Variable()
	{
	}




	/**
	 * Constructor. Initializes variables with -1 (null).
	 */
	public Variable(String name, int fileOffset, int sizeInBytes, int value)
	{
		this.name = name;
		this.fileOffset = fileOffset;
		this.sizeInBytes = sizeInBytes;
		this.value = value;
	}




	/**
	 * Checks if all attributes are set.
	 * 
	 * @return true if all attributes != -1
	 */
	public boolean hasNoNullAttribute()
	{
		if (this.name == "")
			return false;
		if (this.fileOffset == -1)
			return false;
		if (this.sizeInBytes == -1)
			return false;
		if (this.value == -1)
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
	 * @param name
	 *            the name to set
	 */
	public void setName(String name)
	{
		this.name = name;
	}




	/**
	 * @return the fileOffset
	 */
	public int getFileOffset()
	{
		return fileOffset;
	}




	/**
	 * @param fileOffset
	 *            the fileOffset to set
	 */
	public void setFileOffset(int fileOffset)
	{
		this.fileOffset = fileOffset;
	}




	/**
	 * @return the sizeInBytes
	 */
	public int getSizeInBytes()
	{
		return sizeInBytes;
	}




	/**
	 * @param sizeInBytes
	 *            the sizeInBytes to set
	 */
	public void setSizeInBytes(int sizeInBytes)
	{
		this.sizeInBytes = sizeInBytes;
	}




	/**
	 * @return the value
	 */
	public int getValue()
	{
		return value;
	}




	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(int value)
	{
		this.value = value;
	}

}
