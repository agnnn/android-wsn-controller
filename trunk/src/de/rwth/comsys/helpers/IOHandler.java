package de.rwth.comsys.helpers;

import de.rwth.comsys.AndroidWSNControllerActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class IOHandler
{
	private static AndroidWSNControllerActivity context;




	/**
	 * This is a thread save version of doing output operation on the UI
	 * 
	 * @param msg
	 */
	public static synchronized void doOutput(final String msg)
	{
		if (context != null)
		{
			// do something before you send the message
			Handler handler = context.getUiHandler();
			Bundle data = new Bundle();
			data.putString(null, msg);
			Message message = handler.obtainMessage();
			message.setData(data);

			handler.sendMessage(message);
		}
	}




	public static void setContext(AndroidWSNControllerActivity androidWSNControllerActivity)
	{
		IOHandler.context = androidWSNControllerActivity;
	}
}
