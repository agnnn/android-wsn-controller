package de.rwth.comsys;

import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

public class OutputHandler extends Handler {
	private TextView textView;
	public OutputHandler(TextView textView)
	{
		this.textView = textView;
	}
	@Override
	public void handleMessage(Message msg)
	{
		String msgStr = msg.getData().getString(null);
		this.textView.append(msgStr+"\n");
	}
}
