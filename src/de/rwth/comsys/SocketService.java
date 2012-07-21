package de.rwth.comsys;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import de.rwth.comsys.helpers.IOHandler;

public class SocketService extends Service {

	private static AndroidWSNControllerActivity context;
	private volatile HashMap<Integer,SocketThread> threads;

	final int READ_CYCLE_TIMEOUT = 1000; // time between 2 read cycles
	final int WRITE_TIMEOUT = 100; // timeout for a write operation
	final int READ_TIMEOUT = 100;
	final int MAX_BUFFER_SIZE = 255;
	private NotificationManager mNM;
	public static volatile boolean running = false;
	private final IBinder mBinder = new ServiceBinder();

	@Override
	public void onStart(Intent intent, int startid) {
		this.threads = new HashMap<Integer,SocketThread>();
		running = true;
	}
	
	public void startNewSocket(int port, int index)
	{
		if(index != -1 && port != -1)
		{
			if(!threads.containsKey(index))
			{
				FTDI_Interface ftdiInterface = TelosBConnector.getInterfaceByIdx(index);
			
				SocketThread socketThread = new SocketThread(index,port,ftdiInterface);
				threads.put(index, socketThread);
				socketThread.start();
				
				IOHandler.doOutput("new socket thread started...");
			}
			else
				IOHandler.doOutput("Error: Socket already opened");
		}
		else
			IOHandler.doOutput("Error: idx="+index+" port="+port);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return mBinder;
	}

	@Override
	public void onDestroy() {
		IOHandler.doOutput("service stopped!!!");
		
		// mNM.cancel(NOTIFICATION);
	}

	@Override
	public void onCreate() {
		// Toast.makeText(context, "Created", Toast.LENGTH_LONG).show();
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	}

	public static synchronized void setContext(
			AndroidWSNControllerActivity context2) {
		if (context != null) {
			context = context2;
		}
	}
	
	private class SocketThread extends Thread {
		private int port;
		private FTDI_Interface ftdiInterface;
		private volatile boolean stopped;
		private ServerSocket inOutSocket;
		private Socket mySock;
		private OutputStream outStream;
		private InputStream inStream;
		private int index;
		
		public SocketThread(int index,int port,FTDI_Interface ftdiInterface)
		{
			this.index = index;
			this.port = port;
			this.ftdiInterface = ftdiInterface;
			this.stopped = false;
		}
		public void stopSocket()
		{
			IOHandler.doOutput("in thread... stopped = true");
			try {
				inOutSocket.close();
			} catch (IOException e) {
				IOHandler.doOutput("Error: "+e.getMessage());
				e.printStackTrace();
			}
			stopped = true;
		}
	    @Override
	    public void run() {
	    	// Toast.makeText(this, "My Service Started", Toast.LENGTH_LONG).show();
			//IOHandler.doOutput("startid: " + startid);
			try {
				inOutSocket = new ServerSocket(port);
				if (inOutSocket == null) {
					IOHandler.doOutput("error: null pointer");
					return;
				}
				IOHandler.doOutput("wait for connection on port: " + port);
				mySock = inOutSocket.accept();
				IOHandler.doOutput("connection accepted");
				outStream = mySock.getOutputStream();
				outStream.flush();
				inStream = mySock.getInputStream();
				byte[] buffer = new byte[MAX_BUFFER_SIZE];
				int readBytes = 0;
				IOHandler.doOutput("start data transmit...");
				while (!mySock.isClosed() && !stopped) // as long as there is a
														// listener
				{
					IOHandler.doOutput("start forwarding");
					// first perform a write operation from the user to the mote
					readBytes = inStream.read(buffer);
					if (readBytes != -1) {
						ftdiInterface.write(buffer, WRITE_TIMEOUT);
					}
					// now send the data from the mote to the user
					byte[] readMoteData = ftdiInterface.read(100);
					// if some data is received, forward it to the user socket
					if (readMoteData != null) {
						IOHandler.doOutput("there is data to forward:");
						outStream.write(readMoteData);
						outStream.flush();
					} else {
						IOHandler.doOutput("no data from mote");
					}

					Thread.sleep(READ_CYCLE_TIMEOUT);
				}

				mySock.close();
				IOHandler.doOutput("port "+port+" closed");

			} catch (IOException e) {
				IOHandler.doOutput(e.getMessage());
			} // this thread waits for a connection
			catch (InterruptedException e) {
				IOHandler.doOutput(e.getMessage());
				e.printStackTrace();
			} catch (Exception e) {
				IOHandler.doOutput(e.getMessage());
			}
			finally
			{
				threads.remove(index);
			}
			// if everything went well there is a need to open the socket again?! 
	    }  
	};
	
	public class ServiceBinder extends Binder {
		SocketService getService() {
	        return SocketService.this;
	    }
	}

	public void stopSocket(int index) {
		SocketThread mySocketThread = threads.get(index);
		if(mySocketThread != null && mySocketThread.isAlive())
		{
			mySocketThread.stopSocket();
		}
		else
		{
			IOHandler.doOutput("Error: no thread available!");
		}
	}

	public boolean getSFState(int idx) {
		if(threads != null)
		{
			return threads.containsKey(idx);
		}
		return false;
	}
}
