package de.rwth.comsys;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import de.rwth.comsys.Enums.FTDI232BM_Matching_MSP430_Baudrates;
import de.rwth.comsys.Enums.FTDI_Constants;

import android.util.Log;

public class SocketThread extends Thread {

	private final int MAX_BUFFER_SIZE = 255;
	private final int WRITE_TIMEOUT = 1000;
	private final long READ_CYCLE_TIMEOUT = 1000;

	private boolean stopped = false;
	private ServerSocket serverSocket = null;
	private FTDI_Interface ftdiInterface = null;
	private OutputStream outStream = null;
	private InputStream inStream = null;
	private Socket socket = null;

	public SocketThread(FTDI_Interface ftdiInterface) {
		this.ftdiInterface = ftdiInterface;
	}

	@Override
	public void destroy() {
		IOHandler.doOutput("Thread destroyed");
		try {
			if(outStream != null){
				outStream.close();
				outStream = null;
			}
			if(inStream != null){
				inStream.close();
				inStream = null;
			}
			if(socket != null){
				socket.close();
				socket = null;
			}
			if(serverSocket != null){
				serverSocket.close();
				serverSocket = null;
			}
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		stopped = true;
		ftdiInterface = null;
	}

	@Override
	public void interrupt() {
		this.destroy();
	}

	@Override
	public void run() {
		
		/*if(ftdiInterface.setBaudrate(FTDI232BM_Matching_MSP430_Baudrates.BAUDRATE_115200))
		{
			IOHandler.doOutput("successfully set baudrate to "+FTDI232BM_Matching_MSP430_Baudrates.BAUDRATE_115200+" on ftdi");
		}*/
		
		/*ftdiInterface.setLineProperties(FTDI_Constants.DATA_BITS_8,
				FTDI_Constants.STOP_BITS_1, FTDI_Constants.PARITY_NONE, // was even
				FTDI_Constants.BREAK_OFF);
		*/
		
		try {
			serverSocket = new ServerSocket(2001);
			// at this point a socket was successfully created
			IOHandler.doOutput("Socket opened successfully");
			socket = serverSocket.accept();

			OutputStream outStream = socket.getOutputStream();
			outStream.flush();
			InputStream inStream = socket.getInputStream();
			byte[] buffer = new byte[MAX_BUFFER_SIZE];
			int readBytes = 0;
			int available = 0;
			while (!socket.isClosed() && !stopped) // as long as there is listener
			{
				IOHandler.doOutput("start forwarding");
				// first perform a write operation from the user to the mote
				available = inStream.available();
				if(available > 0)
				{
					readBytes = inStream.read(buffer);
					if (readBytes != -1) {
						ftdiInterface.write(buffer, WRITE_TIMEOUT);
					}
				}
				else
				{
					IOHandler.doOutput("no User input");
				}
				// now send the data from the mote to the user
				byte[] readMoteData = ftdiInterface.read(200);
				// if some data is received, forward it to the user socket
				if (readMoteData != null && readMoteData.length > 0) {
					IOHandler.doOutput("there is data to forward:");
					for (int i = 0; i < readMoteData.length; i++) {
						byte b = readMoteData[i];
						IOHandler.doOutput("0x"+Integer.toHexString(b));
					}
					outStream.write(readMoteData);
					outStream.flush();
				} else {
					IOHandler.doOutput("no data from mote");
				}

				Thread.sleep(READ_CYCLE_TIMEOUT);
			}
			IOHandler.doOutput("close everythin");
			outStream.close();
			inStream.close();
			socket.close();
			serverSocket.close();

		} catch (IOException e) {
			IOHandler.doOutput(e.getMessage());
			Log.e("SocketThread", "err: " + e.getMessage());
		} catch (Exception e) {
			Log.e("ServerSocket", "exception" + e.getMessage());
		}

		// if everything went well there is a need to open the socket again?!
	}

}
