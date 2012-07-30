package de.rwth.comsys;

import java.util.HashMap;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;


/**
 * @author Christian & Stephan
 * 
 * captures the change of an EditText field and saves the last change of a moteId in a HashMap
 */
public class CustomTextWatcher implements TextWatcher {
	
	private static HashMap<Integer,Integer> nodeNumbers = new HashMap<Integer,Integer>();
	private int position;
	
	public CustomTextWatcher(int position)
	{
		this.position = position;
	}

	public void afterTextChanged(Editable s) {
		
		String input = s.toString();
		if(input != null && !input.isEmpty())
		{
			Log.w("FLASHING","added text: "+input+" to map on key "+this.position);
			nodeNumbers.put(this.position,Integer.valueOf(s.toString()));
		}
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub

	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub

	}
	
	public static int getNodeId(int pos)
	{
		return nodeNumbers.get(pos);
	}

}
