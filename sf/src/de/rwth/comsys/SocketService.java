package de.rwth.comsys;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SocketService extends Service {
	
	private SocketThread socketThread = null;
	private static FTDI_Interface ftdiInterface;

	public static boolean serviceRunning = false;
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	public static void setFtdiInterface(FTDI_Interface ftdiInterfaceParam)
	{
		ftdiInterface = ftdiInterfaceParam;
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(!serviceRunning)
		{
			if(ftdiInterface != null)
			{
				socketThread = new SocketThread(ftdiInterface);
				socketThread.start();
			}
			else
			{
				IOHandler.doOutput("Service start but no Socket created");
				serviceRunning = true;
			}
		}
		else
		{
			IOHandler.doOutput("Service already running...");
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		IOHandler.doOutput("about to destroy service - cleanUp..");
		
		if(socketThread != null)
		{
			SocketThread killHelper = socketThread;
			socketThread = null;
			killHelper.interrupt();
		}
		serviceRunning = false;
		super.onDestroy();
	}

}
