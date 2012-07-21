package de.rwth.comsys;

import de.rwth.comsys.helpers.IOHandler;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.Toast;

public class SocketServiceConnection implements ServiceConnection {

	private SocketService myService = null;
	private static AndroidWSNControllerActivity context = null;
	
	public void onServiceConnected(ComponentName name, IBinder service) {
		if(context != null)
			Toast.makeText(context, "SF-Service started", Toast.LENGTH_SHORT).show();
		else
			IOHandler.doOutput("SF-Service started");
		
		myService = ((SocketService.ServiceBinder)service).getService();
		IOHandler.doOutput("added Service conn: "+myService.hashCode());
	}

	public void onServiceDisconnected(ComponentName name) {
		if(context != null)
			Toast.makeText(context, "SF-Service stopped", Toast.LENGTH_SHORT).show();
		else
			IOHandler.doOutput("SF-Service stopped");
	}

	public void stopService()
	{
		IOHandler.doOutput("connector: stop service: "+myService.toString());
		if(myService != null)
		{
			myService.stopSelf();
		}
		else
			IOHandler.doOutput("Error: service is null");
	}
	
	public void stopSocket(int index)
	{
		if(myService != null)
		{
			IOHandler.doOutput("connector: stop socket... idx: "+index);
			myService.stopSocket(index);
		}
		else
			IOHandler.doOutput("Error: service is null");
	}
	
	public static void setContext(AndroidWSNControllerActivity context2)
	{
		context = context2;
	}

	public void startSerialForwarder(int port, int index) {
		if(myService != null)
		{
			myService.startNewSocket(port, index);
		}
	}

	public boolean getSFState(int idx) {
		if(myService != null)
		{
			return myService.getSFState(idx);
		}
		return false;
	}	
}
