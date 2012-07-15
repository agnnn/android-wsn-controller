package de.rwth.comsys;

import java.io.File;
import java.util.HashMap;

import de.rwth.comsys.elf.ElfLoader;
import de.rwth.comsys.ihex.HexLoader;

/**
 * Used to manage an ELF file 
 * and their corresponding generated files w.r.t TOS_NODE_ID.
 * @author Christian & Stephan
 *
 */
public class FileManagerEntry
{
	private File file = null;
	
	// Integer reflects Node_ID
	private HashMap<Integer,HexLoader> hexLoaders =  new HashMap<Integer, HexLoader>();
	private HashMap<Integer,ElfLoader> elfLoaders =  new HashMap<Integer, ElfLoader>();
	
	/**
	 * @return the hexLoaders
	 */
	public HashMap<Integer,HexLoader> getHexLoaders()
	{
		return hexLoaders;
	}
	/**
	 * @param hexLoaders the hexLoaders to set
	 */
	public void setHexLoaders(HashMap<Integer,HexLoader> hexLoaders)
	{
		this.hexLoaders = hexLoaders;
	}
	/**
	 * @return the elfLoaders
	 */
	public HashMap<Integer,ElfLoader> getElfLoaders()
	{
		return elfLoaders;
	}
	/**
	 * @param elfLoaders the elfLoaders to set
	 */
	public void setElfLoaders(HashMap<Integer,ElfLoader> elfLoaders)
	{
		this.elfLoaders = elfLoaders;
	}
	/**
	 * @return the file
	 */
	public File getFile()
	{
		return file;
	}
	/**
	 * @param file the file to set
	 */
	public void setFile(File file)
	{
		this.file = file;
	}
}
