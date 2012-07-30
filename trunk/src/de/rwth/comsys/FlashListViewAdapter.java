package de.rwth.comsys;

import java.util.Arrays;
import java.util.List;

import de.rwth.comsys.helpers.IOHandler;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FlashListViewAdapter extends ArrayAdapter<String> {
	int resource;
	static LayoutInflater inflater;
	static boolean[] listenerAdded;

	public FlashListViewAdapter(Context cont, int _resource, List<String> items) {
		super(cont, _resource, items);
		resource = _resource;
		inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		listenerAdded = new boolean[items.size()];
		Arrays.fill(listenerAdded, false);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View vi=convertView;

		String moteName = getItem(position);
		if (convertView == null) {
			vi = inflater.inflate(R.layout.flash_row, null);
		} 
		
		TextView t1 = (TextView) vi.findViewById(R.id.mote_name);
		if(t1 != null)
			t1.setText(moteName);
		EditText b1 = (EditText) vi.findViewById(R.id.list_nodeId);
		if(b1 == null)
		{
			//IOHandler.doOutput("EditText is null");
			Log.w("FLASH","EditText is null");
			
		}
		else
		{
			b1.setText(Integer.toString(position));
			if(!listenerAdded[position]){
				b1.addTextChangedListener(new CustomTextWatcher(position));
				listenerAdded[position] = true;
			}
		}
		
		return vi;
	}
}
