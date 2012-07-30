package de.rwth.comsys;

import de.rwth.comsys.helpers.IOHandler;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.Toast;

/**
 * 
 * @author Christian & Stephan
 *
 * Allows accessing a Service after creation and provides a simple interface for calling some
 * methods in the SocketService class
 */
public class SocketServiceConnection implements ServiceConnection {

	private SocketService myService = null;
	private static Activity context = null;

	public void onServiceConnected(ComponentName name, IBinder service) {
		if (context != null)
			Toast.makeText(context, "SF-Service started", Toast.LENGTH_SHORT)
					.show();
		else
			IOHandler.doOutput("SF-Service started");

		myService = ((SocketService.ServiceBinder) service).getService();
		IOHandler.doOutput("added Service conn: " + myService.hashCode());
	}

	public void onServiceDisconnected(ComponentName name) {
		if (context != null)
			Toast.makeText(context, "SF-Service stopped", Toast.LENGTH_SHORT)
					.show();
		else
			IOHandler.doOutput("SF-Service stopped");
	}

	/**
	 * stops the underlying service
	 */
	public void stopService() {
		IOHandler.doOutput("connector: stop service: " + myService.toString());
		if (myService != null) {
			myService.stopSelf();
		} else
			IOHandler.doOutput("Error: service is null");
	}

	/**
	 * stops the underlying services sockets
	 * @param index
	 */
	public void stopSocket(int index) {
		if (myService != null) {
			IOHandler.doOutput("connector: stop socket... idx: " + index);
			myService.stopSocket(index);
		} else
			IOHandler.doOutput("Error: service is null");
	}

	/**
	 * Sets the context for the ServiceConnection for directing the log output
	 * @param context2, the calling Activity 
	 */
	public static void setContext(Activity context2) {
		context = context2;
	}

	/**
	 * creates a serial forwarder for the given port and the mote specified through the mote index in the moteList
	 * @param port
	 * @param index
	 */
	public void startSerialForwarder(int port, int index) {
		if (myService != null) {
			myService.startNewSocket(port, index);
		}
	}

	/**
	 * returns the serial forwarder state of the underlying service for a given mote
	 * specified by its index in the moteList
	 * @param idx
	 * @return true if running, false otherwise
	 */
	public boolean getSFState(int idx) {
		if (myService != null) {
			return myService.getSFState(idx);
		}
		return false;
	}
}
