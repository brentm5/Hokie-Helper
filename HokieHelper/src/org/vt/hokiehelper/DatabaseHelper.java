/**
 * 
 */
package org.vt.hokiehelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

/**
 * @author Brent Montague
 * 
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String TAG = DatabaseHelper.class.getName();

	private static final String DATABASE_NAME = "hokie_helper_data";

	private static final int DATABASE_VERSION = 1;

	private static final String DATABASE_CREATE_PERSISTENT_DATA = "CREATE TABLE persistentData (id integer primary key autoincrement, key text not null, value text);";
	private static final String DATABASE_CREATE_MAPS_DATA = "CREATE TABLE mapsData (id integer primary key autoincrement, name text not null,lat real not null, long real not null, url text);";
	private static final String DATABASE_CREATE_NEWS_DATA = "CREATE TABLE newsData (id integer primary key autoincrement, title text not null,date text not null, article text not null, url text);";
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "Created Database " + DATABASE_NAME);
		db.execSQL(DATABASE_CREATE_PERSISTENT_DATA);
		db.execSQL(DATABASE_CREATE_MAPS_DATA);
		db.execSQL(DATABASE_CREATE_NEWS_DATA);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
				+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS todo");
		onCreate(db);
	}

}
