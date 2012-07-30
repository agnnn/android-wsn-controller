package de.rwth.comsys.helpers;

import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

/**
 * Creates a OutputHandler receiving all log messages
 * @author Christian & Stephan
 *
 */
public class OutputHandler extends Handler
{
	private TextView textView;

	/**
	 * creates the OutputHandler which appends all received messages to the given TextView
	 * @param textView
	 */
	public OutputHandler(TextView textView)
	{
		this.textView = textView;
	}

	@Override
	public void handleMessage(Message msg)
	{
		String msgStr = msg.getData().getString(null);
		this.textView.append(msgStr + "\n");
	}
}
