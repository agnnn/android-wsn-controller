/**
 * 
 */
package de.rwth.comsys;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import de.rwth.comsys.Enums.FTDI232BM_Matching_MSP430_Baudrates;
import de.rwth.comsys.Enums.FTDI_Constants;
import de.rwth.comsys.Enums.MSP430Variant;

/**
 * Used to communicate with the device without freezing the GUI. Executes
 * different commands which are stored in "ArrayList<MSP430Command> commands" .
 * 
 * @author Christian & Stephan
 * 
 */
public class ProgrammerThreadMSP430 extends Thread {
	private FTDI_Interface ftdiInterface;
	private ArrayList<MSP430Command> commands;
	public int timeout = 3000;
	private AndroidWSNControllerActivity context;

	public ProgrammerThreadMSP430(ArrayList<MSP430Command> commands,
			FTDI_Interface ftdiInterface) {
		this.ftdiInterface = ftdiInterface;
		ftdiInterface.setLineProperties(FTDI_Constants.DATA_BITS_8,
				FTDI_Constants.STOP_BITS_1, FTDI_Constants.PARITY_EVEN,
				FTDI_Constants.BREAK_OFF);
		this.commands = commands;
	}

	public void run() {
		sendResetSequence(true); // reset seq
		for (MSP430Command cmd : commands) {
			IOHandler.doOutput("pollCmd: " + cmd.getCommand().toString());
			
			switch (cmd.getCommand()) {
			case MASS_ERASE:
				doMassErase();
				break;

			case SET_PASSWORD:
				setPassword(cmd.getData());
				break;

			case TRANSMIT_PASSWORD:
				transmitPassword(cmd.getData());
				break;

			case FLASH:
				flash(cmd.getRecords());
				break;

			case LOAD_PC:
				loadPC(cmd.getStartAddress());
				break;

			case CHANGE_BAUDRATE:
				changeBaudrate(cmd.getBaudrate(), cmd.getVariant());
				//ftdiInterface.resetUsb();
				break;

			case TX_BSL_VERSION:
				requestBSLVersion();
				break;

			default:
				break;
			}
		}
		// before the end of the thread set the programmingRunning variable to false;
	}

	private void setPassword(byte[] password) {
		ftdiInterface.write(password, 5000);
	}

	private void transmitPassword(byte[] data) {
		try {
			// Request to transmit password
			int i = 0;
			boolean success = false;

			while (!(success || i > 3)) {
				sendBslSync(); // sendHeader

				Thread.sleep(20);

				boolean writeResult = ftdiInterface.write(data, 2000);
				/*
				 * for (byte b : data) { IOHandler.doOutput("0x"+Integer.toHexString(b));
				 * }
				 */
				IOHandler.doOutput("Write password: " + writeResult);

				byte[] readResult = ftdiInterface.read(5000);
				IOHandler.doOutput("Answer password:");
				for (byte b : readResult) {
					IOHandler.doOutput("0x" + Integer.toHexString(b & 0xFF) + ",");
				}
				if (readResult != null && readResult.length > 0) {
					if ((readResult[0] & 0xFF) == 0x90)
						break;
				}
				i++;
			}
		} catch (InterruptedException e) {
			IOHandler.doOutput(e.getMessage());
		}
	}

	/**
	 * Sends a loaded ihex file to device. The ihex file is represented by
	 * Record.java.
	 */
	private void flash(ArrayList<Record> records) {

		if (records == null) {
			IOHandler.doOutput("Flashing: Records == null !");
			return;
		}

		IOHandler.doOutput("Flashing...");

		int maxRetrys = 5;
		int currentRetry = 0;

		boolean successfullySend;
		boolean successfullyWritten;
		Record currentRecord;
		byte[] data;
		byte[] readResult;

		// prepare records
		records = Record.getOnlyDataRecords(records);

		// is there anything to transmit?
		while (!records.isEmpty()) {
			// get and remove first record
			currentRecord = records.get(0);
			records.remove(0);

			successfullySend = false;
			successfullyWritten = false;
			currentRetry = 0;

			// transmit
			try {
				// retransmit necessary?
				while (!successfullySend && (maxRetrys > currentRetry)) {
					// send sync sequence
					sendBslSync();

					// wait a short moment
					Thread.sleep(10);

					// build message
					data = MSP430PacketFactory.createRXDataBlockCommand(
							currentRecord.getData(),
							currentRecord.getAddressHighByte(),
							currentRecord.getAddressLowByte());

					// successfully build packet?
					if (data == null) {
						IOHandler.doOutput("Can't build packet!");
						return;
					}

					// send message
					/*IOHandler.doOutput("Writting to address 0x"
							+ Integer.toHexString(currentRecord
									.getAddressHighByte())
							+ Integer.toHexString(currentRecord
									.getAddressLowByte()));
*/
					successfullyWritten = ftdiInterface.write(data, 2000);

					// ack receiving
					if (successfullyWritten) {
						readResult = ftdiInterface.read(2000);

						if (readResult != null && readResult.length > 0) {
							// is ack?
							if ((readResult[0] & 0xFF) == 0x90) {
								//IOHandler.doOutput("OK");
								successfullySend = true;
							}
						}
					}

					// retry limiter
					currentRetry++;
				}
			} catch (InterruptedException e) {
				IOHandler.doOutput(e.getMessage());
			}

			// no successfully transmission and max count of retries reached ?
			if ((!successfullySend) && (maxRetrys == currentRetry)) {
				IOHandler.doOutput("Abort!");
				return;
			}
		}

		IOHandler.doOutput("Succesfully flashed!");

	}

	/**
	 * Sends a LoadPC command with specified startAddress to device .
	 * 
	 * @param 16-bit startAddress
	 */
	private void loadPC(int startAddress) {

		int maxRetrys = 5;
		int currentRetry = 0;

		boolean successfullySend = false;
		boolean successfullyWritten = false;

		byte[] data;
		byte[] readResult;

		// prepare startAddress
		short startAddressHighByte = (short) ((startAddress >> 8) & 0xFF);
		short startAddressLowByte = (short) (startAddress & 0xFF);
		IOHandler.doOutput("StartAdress: " + startAddressHighByte + "   "
				+ startAddressLowByte);
		// transmit
		try {
			// retransmit necessary?
			while (!successfullySend && (maxRetrys > currentRetry)) {
				// send sync sequence
				sendBslSync();

				// wait a short moment
				Thread.sleep(10);

				// build message
				data = MSP430PacketFactory.createLoadPCCommand(
						startAddressHighByte, startAddressLowByte);

				// send message
				IOHandler.doOutput("Load PC at address 0x"
						+ Integer.toHexString(startAddressHighByte)
						+ Integer.toHexString(startAddressLowByte));
				successfullyWritten = ftdiInterface.write(data, 2000);

				// ack receiving
				if (successfullyWritten) {
					readResult = ftdiInterface.read(1000);

					if (readResult != null && readResult.length > 0) {
						// is ack?
						if ((readResult[0] & 0xFF) == 0x90) {
							IOHandler.doOutput("OK");
							successfullySend = true;
						}
					}
				}

				// retry limiter
				currentRetry++;
			}
		} catch (InterruptedException e) {
			IOHandler.doOutput(e.getMessage());
		}

		// no successfully transmission and max count of retries reached ?
		if ((!successfullySend) && (maxRetrys == currentRetry)) {
			IOHandler.doOutput("Abort!");
			return;
		}

		IOHandler.doOutput("Succesfully moved program counter vector!");

	}

	/**
	 * Sends a CHANGE BAUDRATE command for a MSP430 device. Get parameters by TX
	 * BSL Version command. Changes Ftdi_Interface baudrate.
	 * 
	 * @param baudrate
	 * @param variant
	 */
	private boolean changeBaudrate(
			FTDI232BM_Matching_MSP430_Baudrates baudrate, MSP430Variant variant) {

		if (baudrate == null || variant == null) {
			IOHandler.doOutput("changeBaudrate: wrong input");
			return false;
		}

		int maxRetrys = 5;
		int currentRetry = 0;

		boolean successfullySend = false;
		boolean successfullyWritten = false;
		boolean successfullyChangedBaudrateOfFTDI = false;

		byte[] data;
		byte[] readResult;
		
		// transmit
		try {
			// retransmit necessary?
			while (!successfullySend && (maxRetrys > currentRetry)) {
				// send sync sequence
				sendBslSync();

				// wait a short moment
				Thread.sleep(10);

				// build message
				data = MSP430PacketFactory.createChangeBaudrateCommand(baudrate, variant);

				// successfully build packet?
				if (data == null) {
					IOHandler.doOutput("Can't build packet!");
					return false;
				}

				// send message
				IOHandler.doOutput("Changing baudrate to " + baudrate.getBaudrate());
				successfullyWritten = ftdiInterface.write(data, 2000);

				// ack receiving
				if (successfullyWritten) {
					readResult = ftdiInterface.read(2000);

					if (readResult != null && readResult.length > 0) {
						// is ack?
						if ((readResult[0] & 0xFF) == 0x90) {
							IOHandler.doOutput("OK");
							successfullySend = true;
							// wait a short moment to give the
							// clock system time for stabilization
							Thread.sleep(1000);
						}
					}
				}

				// retry limiter
				currentRetry++;
			}
		} catch (InterruptedException e) {
			IOHandler.doOutput(e.getMessage());
		}

		// no successfully transmission and max count of retries reached ?
		if ((!successfullySend) && (maxRetrys == currentRetry)) {
			IOHandler.doOutput("Abort!");
			return false;
		}

		IOHandler.doOutput("Succesfully changed baudrate of MSP430!\nChanging baudrate of FTDI...");

		// change baudrate of ftdi
		currentRetry = 0;
		/*while (!successfullyChangedBaudrateOfFTDI && (maxRetrys > currentRetry)) {
			successfullyChangedBaudrateOfFTDI = ftdiInterface.setBaudrate(baudrate);
			currentRetry++;
		}

		if ((!successfullyChangedBaudrateOfFTDI) && (maxRetrys == currentRetry)) {
			IOHandler.doOutput("Abort!");
			return false;
		}

		if(!sendBslSync())
		{
			IOHandler.doOutput("sync after baudrate reset failed!");
		}
		*/
		//ftdiInterface.setLineProperties(FTDI_Constants.DATA_BITS_8,
		//		FTDI_Constants.STOP_BITS_1,FTDI_Constants.PARITY_EVEN,FTDI_Constants.BREAK_OFF);
		return successfullyChangedBaudrateOfFTDI;
	}

	/**
	 * Sends a TX DATA BLOCK command with specified startAddress to device. Used
	 * to read data from memory.
	 * 
	 * @param length
	 *            How many 16-bit blocks shall be read?
	 * @param 16-bit startAddress
	 * @return received data from this startAddress
	 */
	private void requestData(int startAddress, short length) {
		// TODO alles
		/**
		 * int maxRetrys = 5; int currentRetry = 0;
		 * 
		 * boolean successfullySend = false; boolean successfullyWritten =
		 * false;
		 * 
		 * byte[] data; byte[] readResult;
		 * 
		 * // prepare startAddress short startAddressHighByte = (short)
		 * ((startAddress >> 8) & 0xFF); short startAddressLowByte = (short)
		 * (startAddress & 0xFF); IOHandler.doOutput("StartAdress: "+ startAddressHighByte
		 * +"   "+startAddressLowByte ); // transmit try{ // retransmit
		 * necessary? while(!successfullySend && (maxRetrys>currentRetry)) { //
		 * send sync sequence sendBslSync();
		 * 
		 * // wait a short moment Thread.sleep(10);
		 * 
		 * // build message data =
		 * MSP430PacketFactory.createLoadPCCommand(startAddressHighByte,
		 * startAddressLowByte);
		 * 
		 * // successfully build packet? if (data == null) {
		 * IOHandler.doOutput("Can't build packet!"); return; }
		 * 
		 * // send message IOHandler.doOutput("Load PC at address 0x"+Integer.toHexString(
		 * startAddressHighByte)+Integer.toHexString(startAddressLowByte));
		 * successfullyWritten = ftdiInterface.write(data, 2000);
		 * 
		 * // ack receiving if(successfullyWritten) { readResult =
		 * ftdiInterface.read(1000);
		 * 
		 * if(readResult != null && readResult.length > 0) { // is ack?
		 * if((readResult[0] & 0xFF) == 0x90) { IOHandler.doOutput("OK"); successfullySend
		 * = true; } } }
		 * 
		 * // retry limiter currentRetry++; } } catch(InterruptedException e) {
		 * IOHandler.doOutput(e.getMessage()); }
		 * 
		 * // no successfully transmission and max count of retries reached ?
		 * if((!successfullySend) && (maxRetrys==currentRetry) ) {
		 * IOHandler.doOutput("Abort!"); return; }
		 * 
		 * 
		 * IOHandler.doOutput("Succesfully moved program counter vector!");
		 */
	}

	/**
	 * TODO
	 * 
	 */
	private void requestBSLVersion() {
		int maxRetrys = 5;
		int currentRetry = 0;

		boolean successfullySent = false;
		boolean successfullyWritten = false;

		byte[] data;
		byte[] readResult = null;

		// transmit
		try {
			// retransmit necessary?
			while (!successfullySent && (maxRetrys > currentRetry)) {
				// send sync sequence
				sendBslSync();

				// wait a short moment
				Thread.sleep(20);

				// build message
				data = MSP430PacketFactory.createRequestBslVersionCommand();

				// send message
				IOHandler.doOutput("Requesting BSL Version...");
				successfullyWritten = ftdiInterface.write(data, 2000);

				// ack receiving
				if (successfullyWritten) {
					readResult = ftdiInterface.read(2000);

					if (readResult != null && readResult.length > 0) {
						// is ack?
						if ((readResult[0] & 0xFF) == 0xA0) {
							IOHandler.doOutput("get BSLVersion failed");
						}
						else
						{
							if(readResult.length >= 6)
							{
								byte highVersion = readResult[4];
								byte lowVersion = readResult[5];
								/*for (byte b : readResult) {
									IOHandler.doOutput("0x"+Integer.toHexString(b));
								}*/
								MSP430Variant variant = null;
								if((variant = MSP430Variant.getDeviceVersion(highVersion, lowVersion)) != null)
								{
									context.getTelosBConnecter().setDeviceVariant(variant);
									IOHandler.doOutput("DeviceVariant: "+variant.toString());
								}
								else
								{
									IOHandler.doOutput("no known device");
								}
							}
							else
							{
								IOHandler.doOutput("wrong answer format");
							}
							successfullySent = true;
						}
					}
				}

				// retry limiter
				currentRetry++;
			}
		} catch (InterruptedException e) {
			IOHandler.doOutput(e.getMessage());
		}

		// no successfully transmission and max count of retries reached ?
		if ((!successfullySent) && (maxRetrys == currentRetry)) {
			IOHandler.doOutput("Abort!");
			return;
		}

		IOHandler.doOutput("Succesfully requested BSL Version!");
	}

	/**
	 * Sends a mass erase command, which erases the entire flash memory area.
	 * 
	 */
	private void doMassErase() {
		try {
			// Request to mass erase
			int i = 0;
			boolean success = false;

			while (!(success || i > 5)) {
				
				sendBslSync(); // sendHeader

				Thread.sleep(200);

				boolean writeResult = ftdiInterface.write(
						MSP430PacketFactory.createMassEraseCommand(), 1000);
				IOHandler.doOutput("Write massErase: " + writeResult);

				byte[] readResult = ftdiInterface.read(5000);
				IOHandler.doOutput("Answer massErase:");
				for (byte b : readResult) {
					IOHandler.doOutput("0x" + Integer.toHexString(b & 0xFF) + ",");
				}
				if (readResult != null && readResult.length > 0) {
					if ((readResult[0] & 0xFF) == 0x90)
						break;
				}
				i++;
			}
		} catch (InterruptedException e) {
			IOHandler.doOutput(e.getMessage());
		}
	}

	private boolean sendBslSync() {
		byte data[] = new byte[1];
		data[0] = (byte) 0x80;
		//IOHandler.doOutput("BSLSync...");
		ftdiInterface.write(data, timeout);
		byte[] resp = ftdiInterface.read(timeout);
		if (resp.length > 0) {
			if ((resp[0] & 0xFF) == 0x90) {
				//IOHandler.doOutput("BSLSync ACK");
				return true;
			}

		}
		IOHandler.doOutput("BSLSync failed");
		return false;
	}

	private void sendResetSequence(boolean invokeBsl) {
		if (invokeBsl) {
			telosWriteCmd((byte) 0, (byte) 1);
			telosWriteCmd((byte) 0, (byte) 3);
			telosWriteCmd((byte) 0, (byte) 1);
			telosWriteCmd((byte) 0, (byte) 3);
			telosWriteCmd((byte) 0, (byte) 2);
			telosWriteCmd((byte) 0, (byte) 0);
		} else {
			telosWriteCmd((byte) 0, (byte) 3);
			telosWriteCmd((byte) 0, (byte) 2);
			telosWriteCmd((byte) 0, (byte) 0);
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}

	}

	private void telosStop() {
		ftdiInterface.setDTR(true);
		ftdiInterface.setRTS(false);
		ftdiInterface.setDTR(false);
	}

	private void telosWriteByte(byte dataByte) {
		telosWriteBit((dataByte & 0x80) > 0);
		telosWriteBit((dataByte & 0x40) > 0);
		telosWriteBit((dataByte & 0x20) > 0);
		telosWriteBit((dataByte & 0x10) > 0);
		telosWriteBit((dataByte & 0x08) > 0);
		telosWriteBit((dataByte & 0x04) > 0);
		telosWriteBit((dataByte & 0x02) > 0);
		telosWriteBit((dataByte & 0x01) > 0);
		telosWriteBit(false); // "acknowledge"
	}

	private void telosStart() {
		ftdiInterface.setDTR(false);
		ftdiInterface.setRTS(false);
		ftdiInterface.setDTR(true);
	}

	private void telosWriteBit(boolean bit) {
		try {
			ftdiInterface.setRTS(true);
			ftdiInterface.setDTR(!bit);
			Thread.sleep(0, 2);
			ftdiInterface.setRTS(false);
			Thread.sleep(0, 1);
			ftdiInterface.setRTS(true);
		} catch (InterruptedException e) {

			e.printStackTrace();
		}
	}

	private void telosWriteCmd(byte addr, byte cmd) {
		telosStart();
		telosWriteByte((byte) (0x90 | (addr << 1)));
		telosWriteByte(cmd);
		telosStop();
	}

	public void setContext(AndroidWSNControllerActivity context) {
		this.context = context;
	}
}
