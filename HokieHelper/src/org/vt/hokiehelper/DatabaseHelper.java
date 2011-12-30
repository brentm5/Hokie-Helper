/**
 * 
 */
package org.vt.hokiehelper;

import android.app.SearchManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * @author Brent Montague
 * 
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String TAG = DatabaseHelper.class.getName();

	private static final String DATABASE_NAME = "HOKIE_HELPER_DATA";

	private static final int DATABASE_VERSION = 1;

	private static final String DATABASE_CREATE_PERSISTENT_DATA = "CREATE TABLE persistentData (_id integer primary key autoincrement, key text not null, value text);";
	private static final String DATABASE_CREATE_MAP_DATA = "CREATE VIRTUAL TABLE mapData USING fts3(_id integer primary key autoincrement, name text not null,lat real not null, long real not null, url text, keywords text );";
	private static final String DATABASE_CREATE_NEWS_DATA = "CREATE TABLE newsData (_id integer primary key autoincrement, title text not null, url text not null);";
	private static final String DATABASE_CREATE_INFO_DATA = "CREATE TABLE infoData (_id integer primary key autoincrement, name text not null, desc text, type text not null, payload text not null);";

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "Created Database " + DATABASE_NAME);
		db.execSQL(DATABASE_CREATE_PERSISTENT_DATA);
		db.execSQL(DATABASE_CREATE_MAP_DATA);
		db.execSQL(DATABASE_CREATE_NEWS_DATA);
		db.execSQL(DATABASE_CREATE_INFO_DATA);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS todo");
		onCreate(db);
	}

}
