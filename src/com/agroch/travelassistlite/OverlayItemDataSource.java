package com.agroch.travelassistlite;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.maps.GeoPoint;

public class OverlayItemDataSource {
	// Database fields

	public SQLiteDatabase database;
	private MySQLiteHelper dbHelper;
	private String[] allColumns = { MySQLiteHelper.COLUMN_ID,
			MySQLiteHelper.COLUMN_TYPE, MySQLiteHelper.COLUMN_TITLE,
			MySQLiteHelper.COLUMN_SNIPPET, MySQLiteHelper.COLUMN_GP_LAT,
			MySQLiteHelper.COLUMN_GP_LONG, MySQLiteHelper.COLUMN_TIMESTAMP, MySQLiteHelper.COLUMN_ADDRESS };
	private ArrayList<MapItem> items = new ArrayList<MapItem>();

	public OverlayItemDataSource(Context context) {
		dbHelper = new MySQLiteHelper(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public MapItem createOverlayItem(MapItem overlay) {
		ContentValues values = new ContentValues();

		// collect all values from MapItem for DB-INSERT
		values.put(MySQLiteHelper.COLUMN_TYPE, overlay.getType());
		values.put(MySQLiteHelper.COLUMN_TITLE, overlay.getTitle());
		values.put(MySQLiteHelper.COLUMN_SNIPPET, overlay.getSnippet());
		values.put(MySQLiteHelper.COLUMN_GP_LAT, overlay.getPoint()
				.getLatitudeE6());
		values.put(MySQLiteHelper.COLUMN_GP_LONG, overlay.getPoint()
				.getLongitudeE6());
		values.put(MySQLiteHelper.COLUMN_ADDRESS, overlay.getAddress());
		values.put(MySQLiteHelper.COLUMN_TIMESTAMP, overlay.getTimeStamp());

		long insertId = database.insert(MySQLiteHelper.TABLE_OVERLAYITEMS,
				null, values);

		if (insertId == -1) {
			return null;
		}

		Cursor cursor = database.query(MySQLiteHelper.TABLE_OVERLAYITEMS,
				allColumns, MySQLiteHelper.COLUMN_ID + " = " + insertId, null,
				null, null, null);
		cursor.moveToFirst();
		MapItem newOverlayItem = cursorToOverlayItem(cursor);
		items.add(newOverlayItem);
		cursor.close();
		return newOverlayItem;
	}

	public boolean updateOverlayItem(MapItem overlay) {
		ContentValues values = new ContentValues();

		// collect all values from MapItem for DB-INSERT
		values.put(MySQLiteHelper.COLUMN_TYPE, overlay.getType());
		values.put(MySQLiteHelper.COLUMN_TITLE, overlay.getTitle());
		values.put(MySQLiteHelper.COLUMN_SNIPPET, overlay.getSnippet());
		values.put(MySQLiteHelper.COLUMN_GP_LAT, overlay.getPoint()
				.getLatitudeE6());
		values.put(MySQLiteHelper.COLUMN_GP_LONG, overlay.getPoint()
				.getLongitudeE6());
		values.put(MySQLiteHelper.COLUMN_ADDRESS, overlay.getAddress());
		values.put(MySQLiteHelper.COLUMN_TIMESTAMP, overlay.getTimeStamp());

		int aff_rows = database.update(MySQLiteHelper.TABLE_OVERLAYITEMS,
				values, MySQLiteHelper.COLUMN_TIMESTAMP + " = " + overlay.getTimeStamp(), null);

		if (aff_rows > 0) {
			return true;
		} else {
			return false;
		}

	}

	public void deleteOverlayItem(MapItem overlay) {
		int item_id = database.delete(MySQLiteHelper.TABLE_OVERLAYITEMS,
				MySQLiteHelper.COLUMN_ID + " = " + overlay.getId(), null);
	}

	public void deleteAllOverlayItems() {
		database.delete(MySQLiteHelper.TABLE_OVERLAYITEMS, null, null);
	}
	
	public void deletePathItems() {
		int item_id = database.delete(MySQLiteHelper.TABLE_OVERLAYITEMS,
				MySQLiteHelper.COLUMN_TYPE + " != '" + MapItem.TYPE_PICTURE + "'", null);
	}

	public List<MapItem> getOverlayItems(String clause) {
		List<MapItem> overlays = new ArrayList<MapItem>();

		Cursor cursor = database.query(MySQLiteHelper.TABLE_OVERLAYITEMS,
				allColumns, clause, null, null, null,
				MySQLiteHelper.COLUMN_TIMESTAMP + " ASC");

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			MapItem overlay = cursorToOverlayItem(cursor);
			overlays.add(overlay);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return overlays;
	}

	private MapItem cursorToOverlayItem(Cursor cursor) {
		GeoPoint gp = new GeoPoint(cursor.getInt(4), cursor.getInt(5));
		MapItem overlay = new MapItem(gp, cursor.getString(2),
				cursor.getString(3));
		overlay.setId(cursor.getInt(0));
		overlay.setTimeStamp(cursor.getString(6));
		overlay.setType(cursor.getString(1));
		overlay.setAddress(cursor.getString(7));
		return overlay;
	}
}
