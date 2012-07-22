package de.rwth.comsys;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FlashListViewAdapter extends ArrayAdapter<String> {
	int resource;

	public FlashListViewAdapter(Context cont, int _resource, List<String> items) {
		super(cont, _resource, items);
		resource = _resource;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RelativeLayout rl;

		String moteName = getItem(position);
		if (convertView == null) {
			rl = new RelativeLayout(getContext());
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			vi.inflate(resource, rl, true);
		} else {
			rl = (RelativeLayout) convertView;
		}
		TextView t1 = (TextView) rl.findViewById(R.id.mote_name);
		t1.setText(moteName);
		EditText b1 = (EditText) rl.findViewById(R.id.nodeId);
		b1.setText(position);
		return rl;
	}
}
