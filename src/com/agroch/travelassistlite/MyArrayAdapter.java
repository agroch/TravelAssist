package com.agroch.travelassistlite;

import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.agroch.travelassistlite.R;

public class MyArrayAdapter extends ArrayAdapter<Address> {
	private final Context context;
	private final Address[] values;
	private double[] currLocationData;

	public MyArrayAdapter(Context context, Address[] values, double[] geoData) {
		super(context, R.layout.rowlayout, values);
		this.context = context;
		this.values = values;
		this.currLocationData = geoData;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.rowlayout, parent, false);

		// set icon for each result
		ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
		imageView.setImageResource(R.drawable.checkered_flag);

		// set text for each result
		TextView textView = (TextView) rowView.findViewById(R.id.place);
		String addr = "";

		for (int j = 0; j < values[position].getMaxAddressLineIndex(); j++) {
			addr += values[position].getAddressLine(j) + "\n";
		}

		textView.setText(addr);

		// set distance for each result

		if (currLocationData != null) {
			Location currlocation = new Location("");
			currlocation.setLatitude(currLocationData[0]);
			currlocation.setLongitude(currLocationData[1]);

			Location destination = new Location("");
			destination.setLatitude(values[position].getLatitude());
			destination.setLongitude(values[position].getLongitude());
			
			float distance = currlocation.distanceTo(destination);
			
			TextView distanceTv = (TextView) rowView.findViewById(R.id.place_distance);
			
			distanceTv.setText(distanceTv.getText() + " " + String.format("%.2f", (distance / 1000)) + " km");
			
		}

		return rowView;
	}

}
