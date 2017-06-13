package com.agroch.travelassistlite;

import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;

import com.google.android.maps.GeoPoint;

public class Util {

	public static String convertTimestamp(long time) {
		SimpleDateFormat startTime = new SimpleDateFormat("yyyyMMddHHmmss");
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		Date date = cal.getTime();

		return startTime.format(date);

	}

	public static Date timestamp2Date(String timestamp) {
		SimpleDateFormat time = new SimpleDateFormat("yyyyMMddHHmmss");
		Date date = null;
		try {
			date = time.parse(timestamp);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return date;

	}

	public static String translateGeoPointToAddress(Context context,
			GeoPoint paramGeoPoint) {
		String address = "";
		Geocoder localGeocoder = new Geocoder(context, Locale.getDefault());
		try {
			List localList = localGeocoder.getFromLocation(
					paramGeoPoint.getLatitudeE6() / 1E6,
					paramGeoPoint.getLongitudeE6() / 1E6, 1);

			if (localList.size() > 0) {
				for (int i = 0; i < ((Address) localList.get(0))
						.getMaxAddressLineIndex(); i++) {
					address += ((Address) localList.get(0)).getAddressLine(i)
							+ "\n";
				}
			} else {
				// second try to resolve address
				address = translateGeoPointToAddress(context, paramGeoPoint);
			}
		} catch (IOException localIOException) {
			localIOException.printStackTrace();
		}
		return address;
	}

	public static Bitmap decodeBitmap(String path, int scaleFactor) {

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = scaleFactor;

//		cleanup memory to avoid out of memory errors
		System.gc();
		
		Bitmap bm = BitmapFactory.decodeFile(path);

		return bm;

	}

	public static Bitmap decodeBitmap(String path) {

		return decodeBitmap(path, 4);

	}
	
	public static boolean fileExists( Context ctx,  String path) {
	
		try {
			FileReader fr = new FileReader(path);
			fr.close();
			
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	

}
