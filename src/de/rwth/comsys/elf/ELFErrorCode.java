package de.rwth.comsys.elf;

public enum ELFErrorCode
{	
	ELF_FILE_TOO_LARGE,
	ELF_PARSE_HEADER_ERROR,
	ELF_PARSE_SECTION_HEADERS_ERROR,
	ELF_PARSE_SECTION_HEADERS_NAMES_ERROR,
	ELF_PARSE_SYMTABLE_ERROR,
	ELF_PARSE_SYMBTABLE_ENTRY_NAMES_ERROR,
	ELF_SYMTABLE_SECTION_HEADER_NOT_FOUND_ERROR,
	ELF_SYMTABLE_NAMES_SECTION_HEADER_NOT_FOUND_ERROR;
	
	
}
