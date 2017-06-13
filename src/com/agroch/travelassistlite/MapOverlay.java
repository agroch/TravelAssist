package com.agroch.travelassistlite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;

public class MapOverlay extends ItemizedOverlay {

	private ArrayList<MapItem> mOverlays = new ArrayList<MapItem>();
	private List<MapItem> walkedGp = new ArrayList<MapItem>();
	private List<GeoPoint> routePoints = new ArrayList<GeoPoint>();
	private Context mContext;
	private MapView mMap;
	private Drawable mDefMarker;
	private MapItem currLoc;
	private MapItem searchedLoc;
	private long systemTime = System.currentTimeMillis();

	private OverlayItemDataSource datasource;
	private boolean drawPath = false;
	private boolean mapVisible = true;

	public MapOverlay(Drawable defaultMarker, Context context, MapView map) {
		super(boundCenterBottom(defaultMarker));
		mDefMarker = defaultMarker;
		mContext = context;
		mMap = map;
		datasource = new OverlayItemDataSource(mContext);
		datasource.open();
		populate();
	}

	@Override
	protected MapItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

	@Override
	protected boolean onTap(int index) {

		System.gc();

		MapItem item = mOverlays.get(index);

		Dialog dialog = new Dialog(mContext);

		dialog.setContentView(R.layout.photo_view);
		dialog.setTitle(item.getTitle());

		String addr = "";

		if (item.getSnippet().contains("jpg")) {
			ImageView image = (ImageView) dialog.findViewById(R.id.image);
			Bitmap bm = Util.decodeBitmap(item.getSnippet());
			image.setImageBitmap(bm);
			addr = item.getAddress();
		} else if (item.getSnippet().length() > 0
				&& item.getSnippet().contains(addr)) {
			addr = item.getSnippet();
		} else {
			addr = Util.translateGeoPointToAddress(mContext, item.getPoint());
		}

		TextView text = (TextView) dialog.findViewById(R.id.text);
		text.setText(addr);

		dialog.show();
		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, MapView mapView) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if ((System.currentTimeMillis() - systemTime) < ViewConfiguration
					.getDoubleTapTimeout()) {
				mMap.getController().zoomInFixing((int) event.getX(),
						(int) event.getY());
			}
			break;
		case MotionEvent.ACTION_UP:
			systemTime = System.currentTimeMillis();
			break;
		}

		return false;
	}

	public void addOverlay(MapItem overlay) {
		mOverlays.add(overlay);
		// store points to database

		setLastFocusedIndex(-1);
		populate();
	}

	public void deleteOverlay(MapItem overlay) {
		mOverlays.remove(overlay);
		// remove points from database
		datasource.deleteOverlayItem(overlay);

		setLastFocusedIndex(-1);
		populate();
	}

	public void addPictPlace(MapItem overlay) {
		addOverlay(overlay);

		datasource.createOverlayItem(overlay);

		DetermineAddressTask task = new DetermineAddressTask();
		task.execute(new String[] { "" });

	}

	public ArrayList<MapItem> getOverlays() {
		return mOverlays;
	}

	public void clearOverlayItems() {
		mOverlays.clear();
		this.walkedGp.clear();

		mMap.invalidate();

		if (currLoc != null) {
			addOverlay(currLoc);
		}

		setLastFocusedIndex(-1);

	}

	public List<MapItem> getWalkedGp() {
		return walkedGp;
	}

	public void setWalkedGp(List<MapItem> walkedGp) {
		this.walkedGp = walkedGp;
	}

	public void clearWalkedGps() {
		this.walkedGp.clear();
		this.mOverlays.remove(searchedLoc);
		this.routePoints.clear();
		datasource.deletePathItems();
		mMap.invalidate();
	}

	public MapItem getSearchedLoc() {
		return searchedLoc;
	}

	public void setSearchedLoc(MapItem Loc) {

		this.mOverlays.remove(searchedLoc);

		this.searchedLoc = Loc;

		addOverlay(searchedLoc);

	}

	// public boolean onTouchEvent(MotionEvent event) {
	// Path path = new Path();
	// if(event.getAction() == MotionEvent.ACTION_DOWN){
	//
	// path.moveTo(event.getX(), event.getY());
	// path.lineTo(event.getX(), event.getY());
	// }else if(event.getAction() == MotionEvent.ACTION_MOVE){
	// path.lineTo(event.getX(), event.getY());
	// }else if(event.getAction() == MotionEvent.ACTION_UP){
	// path.lineTo(event.getX(), event.getY());
	// }
	// return true;
	// }

	public void draw(Canvas canvas, MapView mapv, boolean shadow) {

		if (shadow == false) {

			Paint mPaint = new Paint();
			mPaint.setDither(true);
			mPaint.setColor(Color.RED);
			mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
			mPaint.setStrokeJoin(Paint.Join.ROUND);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStrokeWidth(8);
			mPaint.setAlpha(60);

			Point p1 = new Point();
			Point p2 = new Point();
			Path path = new Path();

			Iterator gpIer = walkedGp.iterator();
			Projection proj = mMap.getProjection();

			GeoPoint lastPoint = null;
			boolean drawpath = false;
			while (gpIer.hasNext()) {
				MapItem item = (MapItem) gpIer.next();
				GeoPoint newPoint = item.getPoint();

				if (item.getType().contentEquals(MapItem.START_POINT)) {
					drawpath = true;
				} else if (item.getType().contentEquals(MapItem.END_POINT)) {
					drawpath = false;
					lastPoint = null;
				}

				if (drawpath) {

					if (lastPoint == null) {
						lastPoint = newPoint;
					}

					proj.toPixels(lastPoint, p1);
					proj.toPixels(newPoint, p2);

					path.moveTo(p2.x, p2.y);
					path.lineTo(p1.x, p1.y);
					// store old GP
					lastPoint = newPoint;

				}
			}

			// draw walked path
			canvas.drawPath(path, mPaint);

			if (routePoints != null) {
				path.reset();

				mPaint.setAntiAlias(true);
				mPaint.setColor(Color.BLUE);
				mPaint.setStyle(Paint.Style.STROKE);
				mPaint.setStrokeWidth(8);
				mPaint.setAlpha(90);

				GeoPoint GpA = null;
				for (int i = 0; i < routePoints.size(); i++) {
					GeoPoint GpB = routePoints.get(i);
					if (GpA == null) {
						GpA = GpB;
					}

					proj.toPixels(GpA, p1);
					proj.toPixels(GpB, p2);

					path.moveTo(p2.x, p2.y);
					path.lineTo(p1.x, p1.y);
					// store old GP
					GpA = GpB;

				}

				if (!path.isEmpty()) {
					canvas.drawPath(path, mPaint);
				}
			}

		}

		super.draw(canvas, mapv, shadow);
	}

	public void switchDrawPath() {

		if (drawPath) {
			drawPath = false;
			if (currLoc != null) {
				currLoc.setType(MapItem.END_POINT);
				walkedGp.add(currLoc);
				datasource.createOverlayItem(currLoc);
			}
		} else {
			drawPath = true;
			if (currLoc != null) {
				currLoc.setType(MapItem.START_POINT);
				walkedGp.add(currLoc);
				datasource.createOverlayItem(currLoc);
			}
		}
	}

	public boolean isDrawPath() {
		return drawPath;
	}

	public void refreshMyLocation(GeoPoint point) {

		MapItem myLocOverlay = new MapItem(point, "Aktuelle Position", "");

		myLocOverlay.setType(MapItem.TRACK_POINT);

		if (drawPath) {
			walkedGp.add(myLocOverlay);
			datasource.createOverlayItem(myLocOverlay);
		}

		// update current location
		mOverlays.remove(currLoc);
		currLoc = myLocOverlay;
		addOverlay(myLocOverlay);

//		refresh the map view only if its visible
		if (isMapVisible()) {
			navToCurrGp();
		}
	}

	public boolean isMapVisible() {
		return mapVisible;
	}

	public void setMapVisible(boolean mapVisible) {
		this.mapVisible = mapVisible;
		if(isMapVisible()){
			navToCurrGp();
		}
	}
	
	
	private void navToCurrGp(){
		MapController mc = mMap.getController();
		mc.animateTo(currLoc.getPoint());
		mMap.getController().setZoom(17);
		mMap.invalidate();
	}

	public void adjustMapZoomFactor(ArrayList<GeoPoint> items) {
		int minLat = Integer.MAX_VALUE;
		int maxLat = Integer.MIN_VALUE;
		int minLon = Integer.MAX_VALUE;
		int maxLon = Integer.MIN_VALUE;

		for (GeoPoint item : items) {

			int lat = item.getLatitudeE6();
			int lon = item.getLongitudeE6();

			maxLat = Math.max(lat, maxLat);
			minLat = Math.min(lat, minLat);
			maxLon = Math.max(lon, maxLon);
			minLon = Math.min(lon, minLon);
		}

		double fitFactor = 1.5;
		mMap.getController().zoomToSpan(
				(int) (Math.abs(maxLat - minLat) * fitFactor),
				(int) (Math.abs(maxLon - minLon) * fitFactor));

		mMap.getController().animateTo(
				new GeoPoint((maxLat + minLat) / 2, (maxLon + minLon) / 2));

		mMap.invalidate();

	}

	protected void finalize() {
		datasource.close();
	}

	private class DetermineAddressTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			for (MapItem item : mOverlays) {

				if (item.getType().contentEquals(MapItem.TYPE_PICTURE)
						&& item.getAddress().length() == 0) {
					String address = Util.translateGeoPointToAddress(mContext,
							item.getPoint());

					if (address != "") {
						item.setAddress(address);
						datasource.updateOverlayItem(item);
					}

				}

			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			//
		}
	}

	public void drawRoute(GeoPoint src, GeoPoint dest, String transMode) {
		// connect to map web service
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(makeUrl(src, dest, transMode));
		HttpResponse response;
		try {
			response = httpclient.execute(httppost);

			HttpEntity entity = response.getEntity();
			InputStream is = null;

			is = entity.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					is, "iso-8859-1"), 8);
			StringBuilder sb = new StringBuilder();
			sb.append(reader.readLine() + "\n");
			String line = "0";
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
			reader.close();
			String result = sb.toString();
			JSONObject jsonObject = new JSONObject(result);
			JSONArray routeArray = jsonObject.getJSONArray("routes");
			JSONObject routes = routeArray.getJSONObject(0);
			JSONObject overviewPolylines = routes
					.getJSONObject("overview_polyline");
			String encodedString = overviewPolylines.getString("points");
			routePoints = decodePoly(encodedString);
			mMap.invalidate();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}

	}

	private List<GeoPoint> decodePoly(String encoded) {

		List<GeoPoint> poly = new ArrayList<GeoPoint>();
		int index = 0, len = encoded.length();
		int lat = 0, lng = 0;

		while (index < len) {
			int b, shift = 0, result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dlat;

			shift = 0;
			result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lng += dlng;

			GeoPoint p = new GeoPoint((int) (((double) lat / 1E5) * 1E6),
					(int) (((double) lng / 1E5) * 1E6));
			poly.add(p);
		}

		return poly;
	}

	private String makeUrl(GeoPoint src, GeoPoint dest, String mode) {

		StringBuilder urlString = new StringBuilder();

		urlString.append("http://maps.googleapis.com/maps/api/directions/json");
		urlString.append("?origin=");// from
		urlString.append(Double.toString((double) src.getLatitudeE6() / 1.0E6));
		urlString.append(",");
		urlString
				.append(Double.toString((double) src.getLongitudeE6() / 1.0E6));
		urlString.append("&destination=");// to
		urlString
				.append(Double.toString((double) dest.getLatitudeE6() / 1.0E6));
		urlString.append(",");
		urlString
				.append(Double.toString((double) dest.getLongitudeE6() / 1.0E6));
		urlString.append("&sensor=false");
		urlString.append("&mode=" + mode);

		Log.d("xxx", "URL=" + urlString.toString());
		return urlString.toString();
	}

}
