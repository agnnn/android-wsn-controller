package de.rwth.comsys;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class SocketService extends Service {
	private ServerSocket inOutSocket;
	private Socket mySock;
	private static FTDI_Interface ftdiInterface;
	private OutputStream outStream;
	private InputStream inStream;
	private static AndroidWSNControllerActivity context;
	
	final int READ_CYCLE_TIMEOUT = 1000; // time between 2 read cycles
	final int WRITE_TIMEOUT = 100; // timeout for a write operation
	final int READ_TIMEOUT = 100;
	final int MAX_BUFFER_SIZE = 255;
	private volatile boolean stopped;
	
	@Override
	public void onStart(Intent intent, int startid) {
		Toast.makeText(this, "My Service Started", Toast.LENGTH_LONG).show();
		IOHandler.doOutput("service started!");
		try {
			mySock = inOutSocket.accept();
			outStream = mySock.getOutputStream();
			outStream.flush();
			inStream = mySock.getInputStream();
			byte[] buffer = new byte[MAX_BUFFER_SIZE];
			int readBytes = 0;
			while(!mySock.isClosed() && !stopped) // as long as there is a listener
			{
				IOHandler.doOutput("start forwarding");
				// first perform a write operation from the user to the mote
				readBytes = inStream.read(buffer);
				if(readBytes != -1)
				{
					ftdiInterface.write(buffer, WRITE_TIMEOUT);
				}
				// now send the data from the mote to the user
				byte[] readMoteData = ftdiInterface.read(100);
				// if some data is received, forward it to the user socket
				if(readMoteData != null)
				{
					IOHandler.doOutput("there is data to forward:");
					outStream.write(readMoteData);
					outStream.flush();
				}
				else
				{
					IOHandler.doOutput("no data from mote");
				}
				
				Thread.sleep(READ_CYCLE_TIMEOUT);
			}
			
			mySock.close();
			
		} catch (IOException e) {
			IOHandler.doOutput(e.getMessage());
		} // this thread waits for a connection
		catch (InterruptedException e) {
			IOHandler.doOutput(e.getMessage());
			e.printStackTrace();
		}
		
		// if everything went well there is a need to open the socket again?! 
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onDestroy() {
		Toast.makeText(context, "My Service Stopped", Toast.LENGTH_LONG).show();
		stopped = true;
	}

	public static void setInterface(FTDI_Interface ftdiInterface2) {
		ftdiInterface = ftdiInterface2;
		Toast.makeText(context, "added interface", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onCreate() {
		Toast.makeText(context, "Created", Toast.LENGTH_LONG).show();
	}

	public static void setContext(AndroidWSNControllerActivity context2) {
		context = context2;
	}
	
	
}
