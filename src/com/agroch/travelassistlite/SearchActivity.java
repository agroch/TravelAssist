package com.agroch.travelassistlite;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class SearchActivity extends Activity {

	private ArrayList<String> SEARCHRESULTS = new ArrayList<String>();
	private List<Address> addresses = new ArrayList<Address>();
	private int selectedItem = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = this.getIntent().getExtras();
		String search = (String) extras.get("Search");
		double[] currGeo = extras.getDoubleArray("currLocation");

		setContentView(R.layout.search_result);
		
		
		doLocationSearch(search);

		Address[] listItems = addresses.toArray(new Address[addresses.size()]);

		MyArrayAdapter adapter = new MyArrayAdapter(this, listItems, currGeo);

		
		ListView lv = (ListView) findViewById(R.id.search_result_list);
		lv.setAdapter(adapter);

		
		lv.setTextFilterEnabled(true);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				selectedItem = position;
				Toast.makeText(getApplicationContext(), SEARCHRESULTS.get(selectedItem),
						Toast.LENGTH_LONG).show();
				finish();
			}
		});
	}

	@Override
	public void finish() {
		// Prepare data intent

		if (selectedItem != -1) {
			Intent data = new Intent();

			double[] geoData = new double[] {
					addresses.get(selectedItem).getLatitude(),
					addresses.get(selectedItem).getLongitude() };

			data.putExtra("place_geodata", geoData);
			data.putExtra("place_name", SEARCHRESULTS.get(selectedItem));
			// Activity finished ok, return the data
			setResult(RESULT_OK, data);
		}
		
		super.finish();
	}

	public void doLocationSearch(String query) {

		// use Geocoder to get latitude/longitude from a specific address
		Geocoder geoCoder = new Geocoder(this, Locale.getDefault());
		try {
			addresses = geoCoder.getFromLocationName(query, 20);

			for (Address address : addresses) {
				String addr = new String();
				for (int j = 0; j < address.getMaxAddressLineIndex(); j++) {
					addr += address.getAddressLine(j) + "\n";
				}
				SEARCHRESULTS.add(addr);
			}

		} catch (Exception e) {
			// TODO: handle exception
		}

	}

}
