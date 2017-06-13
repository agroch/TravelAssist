package com.agroch.travelassistlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {

	public static final String TABLE_OVERLAYITEMS = "overlayitems";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TYPE = "type";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_SNIPPET = "snippet";
	public static final String COLUMN_GP_LAT = "gp_latitude";
	public static final String COLUMN_GP_LONG = "gp_longitude";
	public static final String COLUMN_ADDRESS = "address";
	public static final String COLUMN_TIMESTAMP = "timestamp";

	private static final String DATABASE_NAME = "overlays.db";
	private static final int DATABASE_VERSION = 1;

	// Database creation sql statement
	public static final String DATABASE_CREATE = "create table IF NOT EXISTS "
			+ TABLE_OVERLAYITEMS + "( " 
			+ COLUMN_ID + " integer primary key autoincrement, " 
			+ COLUMN_TYPE + " text not null, " 
			+ COLUMN_TITLE + " text not null, "
			+ COLUMN_SNIPPET + " text not null, " 
			+ COLUMN_GP_LAT + " integer not null, " 
			+ COLUMN_GP_LONG + " integer not null, "
			+ COLUMN_TIMESTAMP + " integer not null, "
			+ COLUMN_ADDRESS + " text "
			+ ");";

	public MySQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(MySQLiteHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_OVERLAYITEMS);
		onCreate(db);
	}
	
	public String addNewColToTable(SQLiteDatabase db, String newColumnDef ){
		try {
			db.execSQL("ALTER TABLE " + TABLE_OVERLAYITEMS + " ADD COLUMN " + newColumnDef );
		} catch (Exception e) {
			return e.getMessage();
		}
		
		return null;
		
	}

}
