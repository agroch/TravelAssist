package com.agroch.travelassistlite.slideshow;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.*;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.agroch.travelassistlite.R;
import com.agroch.travelassistlite.R.id;
import com.agroch.travelassistlite.R.layout;
import com.agroch.travelassistlite.MapItem;
import com.agroch.travelassistlite.MySQLiteHelper;
import com.agroch.travelassistlite.OverlayItemDataSource;
import com.agroch.travelassistlite.Util;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

public class SliderActivity extends Activity {

	public int currentimageindex = 0;
	Timer timer;
	TimerTask task;
	ImageView slidingimage;
	TextView date;
	TextView time;
	TextView place;
	private CoverFlow coverFlow = null;

	private ArrayList<MapItem> IMAGES = new ArrayList<MapItem>();
	private Animation rotateimage;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		date = (TextView) findViewById(R.id.slide_date_data);
		time = (TextView) findViewById(R.id.slide_time_data);
		place = (TextView) findViewById(R.id.slide_place_data);

		// read picture from database
		try {

			OverlayItemDataSource datasource = new OverlayItemDataSource(this);
			datasource.open();
			List<MapItem> oIList = datasource
					.getOverlayItems(MySQLiteHelper.COLUMN_TYPE + " = " + "'"
							+ MapItem.TYPE_PICTURE + "'");

			for (Iterator iterator = oIList.iterator(); iterator.hasNext();) {
				MapItem overlayItem = (MapItem) iterator.next();
				IMAGES.add(overlayItem);
			}

			datasource.close();

		} catch (SQLException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}


		setContentView(R.layout.slideshow);
		

		LinearLayout ll = (LinearLayout) findViewById(R.id.coverflow);

		coverFlow = new CoverFlow(this);
		coverFlow.setAdapter(new ImageAdapter(this, IMAGES));

		ImageAdapter coverImageAdapter = new ImageAdapter(this, IMAGES);

		coverImageAdapter.createReflectedImages();

		coverFlow.setAdapter(coverImageAdapter);

		coverFlow.setSpacing(-15);
		coverFlow.setSelection(0, true);
		coverFlow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		ll.addView(coverFlow);

	}

	// @Override
	// public void onCreate(Bundle savedInstanceState) {
	// super.onCreate(savedInstanceState);
	// setContentView(R.layout.slideshow);
	//
	// date = (TextView) findViewById(R.id.slide_date_data);
	// time = (TextView) findViewById(R.id.slide_time_data);
	// place = (TextView) findViewById(R.id.slide_place_data);
	//
	// slidingimage = (ImageView) findViewById(R.id.ImageView3_Left);
	// rotateimage = AnimationUtils.loadAnimation(this, R.anim.custom_anim);
	//
	// // read picture from database
	// try {
	//
	// OverlayItemDataSource datasource = new OverlayItemDataSource(this);
	// datasource.open();
	// List<MapItem> oIList = datasource
	// .getOverlayItems(MySQLiteHelper.COLUMN_TYPE + " = " + "'"
	// + MapItem.TYPE_PICTURE + "'");
	//
	// for (Iterator iterator = oIList.iterator(); iterator.hasNext();) {
	// MapItem overlayItem = (MapItem) iterator.next();
	// IMAGES.add(overlayItem);
	// }
	//
	// datasource.close();
	//
	// } catch (SQLException e) {
	// Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
	// }
	//
	// final Handler mHandler = new Handler();
	//
	// // Create runnable for posting
	// final Runnable mUpdateResults = new Runnable() {
	// public void run() {
	//
	// AnimateandSlideShow();
	//
	// }
	// };
	//
	// int delay = 1000; // delay for 1 sec.
	//
	// int period = 8000; // repeat every 4 sec.
	//
	// Timer timer = new Timer();
	//
	// timer.scheduleAtFixedRate(new TimerTask() {
	//
	// public void run() {
	//
	// mHandler.post(mUpdateResults);
	//
	// }
	//
	// }, delay, period);
	//
	// }

	/**
	 * Helper method to start the animation on the splash screen
	 */
	private void AnimateandSlideShow() {

		if (IMAGES.size() > 0) {
			slidingimage.setImageBitmap(null);

			System.gc();

			MapItem tmpItem = IMAGES.get(currentimageindex % IMAGES.size());

			String addr = Util.translateGeoPointToAddress(this,
					tmpItem.getPoint());

			if (addr != "") {
				place.setText(addr);
			} else {
				place.setText("-");
			}

			Date tmpDate = Util.timestamp2Date(tmpItem.getTimeStamp());

			date.setText(tmpDate.getDay() + "." + tmpDate.getMonth() + "."
					+ (1900 + tmpDate.getYear()));
			time.setText(tmpDate.getHours() + ":" + tmpDate.getMinutes());

			Bitmap bm = BitmapFactory.decodeFile(tmpItem.getSnippet());
			bm.prepareToDraw();

			slidingimage.setImageBitmap(bm);

			slidingimage.startAnimation(rotateimage);

			currentimageindex++;
		}

	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.layout.slidemenu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.exit_slide:
			finish();
			android.os.Process.killProcess(android.os.Process.myPid());
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		// ImageView img = (ImageView) findViewById(R.id.ImageView3_Left);
		//
		// AnimationDrawable anim = (AnimationDrawable) img.getBackground();
		//
		// int count = anim.getNumberOfFrames();
		// for (int i = 0; i < count; i++) {
		// anim.getFrame(i).setCallback(null);
		// }
		//
		// img.getBackground().setCallback(null);
		//
		// img = null;
	}

}