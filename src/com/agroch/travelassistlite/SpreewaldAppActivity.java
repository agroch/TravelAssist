package com.agroch.travelassistlite;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.agroch.travelassistlite.slideshow.SliderActivity;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class SpreewaldAppActivity extends MapActivity {
	/** Called when the activity is first created. */
	private LocationManager lm;
	private NotificationManager mNotificationManager;
	private LocationListener locationListener;
	private MapView mapView;
	private Location currLocat;
	private MapOverlay overlay;
	private static final int CAMERA_PIC_REQUEST = 1337;
	private static final int SEARCH_REQUEST = 1338;
	private static final int SHOW_REQUEST = 1339;
	private static final int NOTIFY_BACKROUND_ACTIVITY = 1340;
	private Drawable pictIcon;
	private OverlayItemDataSource datasource;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// handle statusbar notifications
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// ---use the LocationManager class to obtain GPS locations---
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationListener = new MyLocationListener();

		checkLocationProvider();

		Criteria crit = new Criteria();
		crit.setAccuracy(Criteria.ACCURACY_MEDIUM);
		String bestProvider = lm.getBestProvider(crit, false);

		currLocat = lm.getLastKnownLocation(bestProvider);

		LinearLayout ll = (LinearLayout) findViewById(R.id.main_ll);

		mapView = (MapView) ll.findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapView.setSatellite(true);

		List<Overlay> mapOverlays = mapView.getOverlays();

		pictIcon = getResources().getDrawable(R.drawable.foto);
		pictIcon.setBounds(0, 0, pictIcon.getMinimumWidth(),
				pictIcon.getMinimumHeight());

		Drawable drawable = this.getResources()
				.getDrawable(R.drawable.pin_blue);

		overlay = new MapOverlay(drawable, this, mapView);
		mapOverlays.add(overlay);

		datasource = new OverlayItemDataSource(this);

		try {
			datasource.open();
			// datasource.database.execSQL("ALTER TABLE overlayitems ADD COLUMN address text;");

			List<MapItem> oIList = datasource
					.getOverlayItems(MySQLiteHelper.COLUMN_TYPE + " = " + "'"
							+ MapItem.TYPE_PICTURE + "'");

			for (Iterator iterator = oIList.iterator(); iterator.hasNext();) {
				MapItem overlayItem = (MapItem) iterator.next();
				if (Util.fileExists(getBaseContext(), overlayItem.getSnippet())) {
					overlayItem.setMarker(pictIcon);
					overlay.addOverlay(overlayItem);
				} else {
					datasource.deleteOverlayItem(overlayItem);
				}
			}

			List<MapItem> trackList = datasource
					.getOverlayItems(MySQLiteHelper.COLUMN_TYPE + " != " + "'"
							+ MapItem.TYPE_PICTURE + "'");

			overlay.setWalkedGp(trackList);

			datasource.close();

		} catch (SQLException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}

		double lat, lng;
		// only for debugging + testing
		if (currLocat == null) {
			lat = Double.parseDouble("51.937968");
			lng = Double.parseDouble("13.889269");
		} else {
			lat = currLocat.getLatitude();
			lng = currLocat.getLongitude();
		}

		GeoPoint gpSrc = new GeoPoint((int) (lat * 1E6), (int) (lng * 1E6));

		overlay.refreshMyLocation(gpSrc);

		mapView.getController().animateTo(gpSrc);
		mapView.getController().setZoom(17);
		mapView.invalidate();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	public void captureImage(View view) {
		Intent cameraIntent = new Intent(
				android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
	}

	public void locationSearch(View view) {
		EditText searchField = (EditText) findViewById(R.id.search_field);

		if (searchField.getText().toString() != "") {

			Intent searchIntent = new Intent(this, SearchActivity.class);
			searchIntent.putExtra("Search", searchField.getText().toString());

			if (currLocat != null) {
				double[] geoData = new double[] { currLocat.getLatitude(),
						currLocat.getLongitude() };
				searchIntent.putExtra("currLocation", geoData);
			}

			startActivityForResult(searchIntent, SEARCH_REQUEST);
		} else {
			Toast.makeText(this, "Bitte geben Sie einen Suchbegriff ein!",
					Toast.LENGTH_LONG).show();

		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_CANCELED) {
			return;
		}

		switch (requestCode) {
		case CAMERA_PIC_REQUEST:
			if (data != null && currLocat != null) {
				GeoPoint picGp = new GeoPoint(
						(int) (currLocat.getLatitude() * 1E6) + 50,
						(int) (currLocat.getLongitude() * 1E6) - 50);

				String imagePath = getPathOfLastImage();
				MapItem picOverlay = new MapItem(picGp, "Aufgenommenes Foto",
						imagePath);

				picOverlay.setType(MapItem.TYPE_PICTURE);
				picOverlay.setMarker(pictIcon);
				overlay.addPictPlace(picOverlay);
			}

			break;
		case SEARCH_REQUEST:
			if (data != null) {
				double[] geodata = data.getExtras().getDoubleArray(
						"place_geodata");
				String placeDescr = data.getExtras().getString("place_name");

				if (geodata != null) {
					GeoPoint srcGp = new GeoPoint(
							(int) (currLocat.getLatitude() * 1E6),
							(int) (currLocat.getLongitude() * 1E6));

					GeoPoint placeGp = new GeoPoint((int) (geodata[0] * 1E6),
							(int) (geodata[1] * 1E6));

					calculateRoute(srcGp, placeGp);

					MapItem placeOverlay = new MapItem(new GeoPoint(
							placeGp.getLatitudeE6() + 50,
							placeGp.getLongitudeE6() - 50), "Gesuchter Ort",
							placeDescr);

					Drawable icon = getResources().getDrawable(
							R.drawable.checkered_flag);
					icon.setBounds(0, 0, icon.getMinimumWidth(),
							icon.getMinimumHeight());

					placeOverlay.setMarker(icon);
					overlay.setSearchedLoc(placeOverlay);
					ArrayList<GeoPoint> tmpGps = new ArrayList<GeoPoint>();
					tmpGps.add(placeGp);
					tmpGps.add(srcGp);

					overlay.adjustMapZoomFactor(tmpGps);
				}
			}
			break;
		default:
			break;
		}

		setResult(0);
	}

	public Location getCurrLocat() {
		return currLocat;
	}

	public void setCurrLocat(Location currLocat) {
		this.currLocat = currLocat;
	}

	private String getPathOfLastImage() {
		final String[] imageColumns = { MediaStore.Images.Media._ID,
				MediaStore.Images.Media.DATA };
		final String imageOrderBy = MediaStore.Images.Media._ID + " DESC";
		Cursor imageCursor = getContentResolver().query(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageColumns,
				null, null, imageOrderBy);
		if (imageCursor.moveToFirst()) {
			String fullPath = imageCursor.getString(imageCursor
					.getColumnIndex(MediaStore.Images.Media.DATA));
			// Log.d(TAG, "getLastImageId::id " + id);
			// Log.d(TAG, "getLastImageId::path " + fullPath);
			imageCursor.close();
			return fullPath;
		} else {
			return "";
		}
	}

	public void update_info_table(Location newLocat) {

		DecimalFormat dec = new DecimalFormat("####.##");

		if (newLocat.hasSpeed()) {

			TextView tv_speed = (TextView) this
					.findViewById(R.id.main_speed_data);

			double curr_speed = newLocat.getSpeed() * 3.6;

			tv_speed.setText(dec.format(curr_speed) + " km/h");
		}

		// update distance information
		double distance = 0;
		if (newLocat != null && currLocat != null
				&& newLocat.getProvider().contains("gps")
				&& currLocat.getProvider().contains("gps")) {

			TextView tv_dist = (TextView) this
					.findViewById(R.id.main_distance_data);

			try {
				distance = dec.parse(tv_dist.getText().toString())
						.doubleValue();
			} catch (ParseException e) {
				e.printStackTrace();
			}

			distance += newLocat.distanceTo(currLocat) / 1000;

			tv_dist.setText(dec.format(distance) + " km");
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.layout.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onBackPressed() {
		Intent setIntent = new Intent(Intent.ACTION_MAIN);
		setIntent.addCategory(Intent.CATEGORY_HOME);
		setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(setIntent);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.slide_show:
			Intent slideIntent = new Intent(this, SliderActivity.class);
			startActivityForResult(slideIntent, SHOW_REQUEST);
			return true;
		case R.id.clean_map:
			Toast.makeText(this, "Lösche Pfad(e) von Karte", Toast.LENGTH_LONG)
					.show();
			overlay.clearWalkedGps();
			return true;
		case R.id.draw_path:
			overlay.switchDrawPath();

			if (overlay.isDrawPath()) {
				item.setTitle("Pfad aus");
			} else {
				item.setTitle("Pfad ein");
			}

			return true;
		case R.id.exit_appl:
			onExitActivity();
			finish();
			System.exit(0);

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void checkLocationProvider() {
		if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30000,
					50, locationListener);
		}

		if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 4,
					locationListener);
		} else {
			// activate GPS

			// switchGpsStatus(true);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("GPS aktivieren?")
					.setCancelable(false)
					.setPositiveButton("Ja",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									Intent in = new Intent(
											android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);

									startActivity(in);

									lm.requestLocationUpdates(
											LocationManager.GPS_PROVIDER,
											10000, 4, locationListener);
								}
							})
					.setNegativeButton("Nein",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});

			AlertDialog alert = builder.create();
			alert.show();

		}

	}

	private void switchGpsStatus(boolean active) {
		String provider = Settings.Secure.getString(getContentResolver(),
				Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

		if (provider.contains("gps") && active) {
			// GPS is already on
			return;
		}

		if (!provider.contains("gps") && !active) {
			// GPS is already off
			return;
		}

		final Intent poke = new Intent();
		poke.setClassName("com.android.settings",
				"com.android.settings.widget.SettingsAppWidgetProvider");
		poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
		poke.setData(Uri.parse("3"));
		sendBroadcast(poke);

		if (active) {
			Toast.makeText(this, "GPS aktiviert", Toast.LENGTH_LONG).show();
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 4,
					locationListener);
		} else {
			Toast.makeText(this, "GPS deaktiviert", Toast.LENGTH_LONG).show();
		}
	}

	public void calculateRoute(final GeoPoint srcGp, final GeoPoint dstGp) {
		final CharSequence[] itemsLabel = { "Auto", "Fahrrad", "Fuß" };
		final CharSequence[] items = { "driving", "bicycling", "walking" };
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Route berechnen für ...");
		builder.setItems(itemsLabel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int item) {
				Toast.makeText(getApplicationContext(), itemsLabel[item],
						Toast.LENGTH_SHORT).show();
				overlay.drawRoute(srcGp, dstGp, (String) items[item]);
				return;
			}
		});
		builder.create().show();
	}

	private void showStatusbarNotification() {

		CharSequence tickerText = getResources().getString(R.string.app_name);
		long when = System.currentTimeMillis();

		Notification notification = new Notification(R.drawable.app_icon_v2,
				tickerText, when);

		Intent notificationIntent = new Intent(this, SpreewaldAppActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(getApplicationContext(), tickerText,
				"läuft im Hintergrund", contentIntent);

		mNotificationManager.notify(NOTIFY_BACKROUND_ACTIVITY, notification);

	}

	@Override
	protected void onPause() {
		overlay.setMapVisible(false);
		showStatusbarNotification();
		super.onPause();
	}

	@Override
	protected void onStop() {
		overlay.setMapVisible(false);
		super.onStop();
	}

	@Override
	protected void onResume() {
		overlay.setMapVisible(true);
		mNotificationManager.cancel(NOTIFY_BACKROUND_ACTIVITY);
		super.onResume();
	}

	@Override
	protected void onRestart() {
		try {
			datasource.open();
			List<MapItem> oIList = datasource
					.getOverlayItems(MySQLiteHelper.COLUMN_TYPE + " = " + "'"
							+ MapItem.TYPE_PICTURE + "'");

			for (Iterator iterator = oIList.iterator(); iterator.hasNext();) {
				MapItem overlayItem = (MapItem) iterator.next();
				if (!Util
						.fileExists(getBaseContext(), overlayItem.getSnippet())) {
					overlay.deleteOverlay(overlayItem);
				}
			}

			datasource.close();

		} catch (SQLException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}

		overlay.setMapVisible(true);

		super.onRestart();
	}

	private void onExitActivity() {

		switchGpsStatus(false);

		// store last relevant information in DB before exit
		datasource.open();

		GeoPoint endPoint = new GeoPoint((int) (currLocat.getLatitude() * 1E6),
				(int) (currLocat.getLongitude() * 1E6));

		MapItem endItem = new MapItem(endPoint, "", "");
		endItem.setType(MapItem.END_POINT);

		datasource.createOverlayItem(endItem);

		datasource.close();
		
		mNotificationManager.cancelAll();
	}

	private class MyLocationListener implements LocationListener {

		private static final int ONE_MINUTES = 1000 * 60;

		public void onLocationChanged(Location loc) {
			if (loc != null) {

				// GPS information via network are not accurate enough
				if (currLocat != null && loc.getProvider().contains("network")) {
					return;
				}

				update_info_table(loc);

				GeoPoint p = new GeoPoint((int) (loc.getLatitude() * 1E6),
						(int) (loc.getLongitude() * 1E6));

				overlay.refreshMyLocation(p);

				currLocat = loc;

			}
		}

		public void onProviderDisabled(String provider) {
			// Toast.makeText(getBaseContext(),
			// "Provider " + provider + " deaktiviert!",
			// Toast.LENGTH_SHORT).show();
		}

		public void onProviderEnabled(String provider) {
			// Toast.makeText(getBaseContext(),
			// "Provider " + provider + " aktiviert!", Toast.LENGTH_SHORT)
			// .show();
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {

		}

		/**
		 * Determines whether one Location reading is better than the current
		 * Location fix
		 * 
		 * @param location
		 *            The new Location that you want to evaluate
		 * @param currentBestLocation
		 *            The current Location fix, to which you want to compare the
		 *            new one
		 */
		protected boolean isBetterLocation(Location location,
				Location currentBestLocation) {
			if (currentBestLocation == null) {
				// A new location is always better than no location
				return true;
			}

			// Check whether the new location fix is newer or older
			long timeDelta = location.getTime() - currentBestLocation.getTime();
			boolean isSignificantlyNewer = timeDelta > ONE_MINUTES;
			boolean isSignificantlyOlder = timeDelta < -ONE_MINUTES;
			boolean isNewer = timeDelta > 0;

			// If it's been more than two minutes since the current location,
			// use the new location
			// because the user has likely moved
			if (isSignificantlyNewer) {
				return true;
				// If the new location is more than two minutes older, it must
				// be worse
			} else if (isSignificantlyOlder) {
				return false;
			}

			// Check whether the new location fix is more or less accurate
			int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
					.getAccuracy());
			boolean isLessAccurate = accuracyDelta > 0;
			boolean isMoreAccurate = accuracyDelta < 0;
			boolean isSignificantlyLessAccurate = accuracyDelta > 200;

			// Check if the old and new location are from the same provider
			boolean isFromSameProvider = isSameProvider(location.getProvider(),
					currentBestLocation.getProvider());

			// Determine location quality using a combination of timeliness and
			// accuracy
			if (isMoreAccurate) {
				return true;
			} else if (isNewer && !isLessAccurate) {
				return true;
			} else if (isNewer && !isSignificantlyLessAccurate
					&& isFromSameProvider) {
				return true;
			}
			return false;
		}

		/** Checks whether two providers are the same */
		private boolean isSameProvider(String provider1, String provider2) {
			if (provider1 == null) {
				return provider2 == null;
			}
			return provider1.equals(provider2);
		}
	}

}