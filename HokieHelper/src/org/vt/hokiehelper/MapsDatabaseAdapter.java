package org.vt.hokiehelper;

import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

/**
 * @author Brent Montague
 * 
 */
public class MapsDatabaseAdapter implements HttpCallback {

	private static final String TAG = MapsDatabaseAdapter.class.getName();

	public static final String KEY_ROWID = "_id";
	public static final String KEY_NAME = "name";
	public static final String KEY_LAT = "lat";
	public static final String KEY_LONG = "long";
	public static final String KEY_URL = "url";
	public static final String KEY_KEYWORDS = "keywords";

	private static final String DATABASE_TABLE = "mapData";

	private static final HashMap<String,String> columnMap_ = buildColumnMap();
	private HttpUtils utils_ = HttpUtils.get();
	private PersistentDataDatabaseAdapter persistentDataDatabase_;
	private Context context_;
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	private Double version_ = null;
	private boolean needUpdate = false;
	

	public MapsDatabaseAdapter(Context context) {
		this.context_ = context;
		Log.d(TAG, "Maps Database Adapter created");
	}

	public MapsDatabaseAdapter open() throws SQLException {
		dbHelper = new DatabaseHelper(context_);
		database = dbHelper.getWritableDatabase();
		Log.d(TAG, "Maps Database opened");
		return this;
	}

	public Cursor fetchLocationData(int id) {
		Log.d(TAG, "Finding locaion with id " + id);
		// If this is used in BuildingsProvider, need to add 
		Cursor cursor =  query(new String[] {	KEY_ROWID, KEY_NAME, KEY_URL, KEY_LAT, KEY_LONG}, KEY_ROWID + "= " + id ,	null);
		if(cursor == null) {
			Log.d(TAG, "No location found with id " + id);
		}
		return cursor;
	}

	public Cursor searchLocations(String searchTerm) {
		// TODO Currently only matching by name, need to be able to search by abbr as well. 
		// Need to change current key words, should not include the name. Just the abbreviation, possibly other common names
		String selection = KEY_KEYWORDS + " MATCH ?";
		String[] selectionArgs = new String[] {searchTerm+"*"};
		Cursor cursor = query(new String[] { KEY_ROWID, KEY_NAME, KEY_LAT, KEY_LONG, SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, SearchManager.SUGGEST_COLUMN_TEXT_1}, selection, selectionArgs);
		if(cursor != null) {
			Log.d(TAG, "Searching for " + searchTerm + " returned " + cursor.getCount() + " results");
		} else {
			Log.d(TAG, "No results found for " + searchTerm);
		}
		return cursor;
	}

	public Cursor getBuildings(){
		return query(null, null, null);
	}

	/**
	 * Performs a database query.
	 * @param selection The selection clause
	 * @param selectionArgs Selection arguments for "?" components in the selection
	 * @param columns The columns to return
	 * @return A Cursor over all rows matching the query
	 */
	public Cursor query(String[] columns, String selection, String[] selectionArgs ) {
		/* The SQLiteBuilder provides a map for all possible columns requested to
		 * actual columns in the database, creating a simple column alias mechanism
		 * by which the ContentProvider does not need to know the real column names
		 */
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(DATABASE_TABLE);
		builder.setProjectionMap(columnMap_);

		Cursor cursor = builder.query(database,
				columns, selection, selectionArgs, null, null, KEY_NAME );

		if (cursor == null) {
			return null;
		} else if (!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}
		return cursor;
	}

	/**
	 * Builds a map for all columns that may be requested, which will be given to the 
	 * SQLiteQueryBuilder. This is a good way to define aliases for column names, but must include 
	 * all columns, even if the value is the key. This allows the ContentProvider to request
	 * columns w/o the need to know real column names and create the alias itself.
	 */
	private static HashMap<String,String> buildColumnMap() {
		HashMap<String,String> map = new HashMap<String,String>();
		map.put(KEY_ROWID, KEY_ROWID);
		map.put(KEY_NAME, KEY_NAME);
		map.put(KEY_LAT, KEY_LAT);
		map.put(KEY_LONG, KEY_LONG);
		map.put(KEY_URL, KEY_URL);
		map.put(KEY_KEYWORDS, KEY_KEYWORDS);
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_1, KEY_KEYWORDS + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1);
		map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, KEY_ROWID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
		return map;
	}

	public long updateMapData(int id, String name, double lat, double lon, String url, String keywords) {
		ContentValues initialValues = createContentValues(id, name, lat, lon, url, keywords);
		Log.d(TAG, id + " Added data " + name + "With value " + initialValues.toString());
		//return database.insert(DATABASE_TABLE, null, initialValues);
		return database.insertWithOnConflict(DATABASE_TABLE, null, initialValues, SQLiteDatabase.CONFLICT_REPLACE);
	}

	private void updateDatabaseVersion(double version){
		persistentDataDatabase_ = new PersistentDataDatabaseAdapter(this.context_);
		persistentDataDatabase_.open();
		persistentDataDatabase_.updateData(3, "MAPS_DATABASE_VERSION", Double.toString(version));
		Log.d(TAG, "Updated Maps database version to "+ version);
		persistentDataDatabase_.close();
	}

	private double getDatbaseVersion() {
		persistentDataDatabase_ = new PersistentDataDatabaseAdapter(this.context_);
		persistentDataDatabase_.open();
		String version = persistentDataDatabase_.fetchData("MAPS_DATABASE_VERSION");
		Log.d(TAG, "Current Maps database version "+ version);
		persistentDataDatabase_.close();
		if(version == null || version.equals("")){
			return 0.0;
		}
		return Double.parseDouble(version);
	}

	public void checkDatabaseVersion(){
		Log.d(TAG, "Maps Database checked for version");
		utils_.doGet("http://hokiehelper.appspot.com/mapsdatabaseupdate?q=version", this);
	}

	private void updateDatabase() {
		Log.d(TAG, "Maps Database updated");
		utils_.doGet("http://hokiehelper.appspot.com/mapsdatabaseupdate?q=database", this);
	}

	public boolean deleteAll() {
		Log.d(TAG, "Deleting the Maps Database");
		return database.delete(DATABASE_TABLE, null, null) > 0;
	}

	private ContentValues createContentValues(int id, String name, double lat,
			double lon, String url, String keywords) {
		ContentValues values = new ContentValues();
		values.put(KEY_ROWID, id);
		values.put(KEY_LAT, lat);
		values.put(KEY_NAME, name);
		values.put(KEY_LONG, lon);
		values.put(KEY_URL, url);
		values.put(KEY_KEYWORDS, keywords);
		return values;
	}

	public boolean isOpen() {
		return (dbHelper != null);
	}
	public void close() {
		Log.d(TAG, "Closing maps database");
		if(dbHelper != null)
			dbHelper.close();
	}

	public void onError(Exception e) {
		Log.e(TAG, "An exception occured when updating buildings", e);
	}

	public void onResponse(final HttpResponse resp) {
		Log.d(TAG, "Got a response");
		try {
			if(version_ == null){
				UpdateDatabaseVersion updater = new UpdateDatabaseVersion();
				updater.execute(resp);
			}else {
				UpdateDatabase updater = new UpdateDatabase();
				updater.execute(resp);
			}
		} catch (Exception e) {
			this.onError(e);
		}

	}

	private class UpdateDatabaseVersion extends AsyncTask<HttpResponse, Void, Boolean> {
		protected Boolean doInBackground(HttpResponse... params) {
			try {
				String responseBody = utils_.responseToString(params[0]);
				JSONObject parsedObject = new JSONObject(responseBody);
				if(parsedObject.getDouble("version") > getDatbaseVersion() ) {
					version_ = parsedObject.getDouble("version");
					Log.d(TAG, "We should really update this database");
					needUpdate = true;
					updateDatabase();
					
				}
				return true; 
			} catch(Exception e) {
				onError(e);
				return false;
			}
		}
		protected void onPostExecute(Boolean result) {
			//Testing toast
			if(result.booleanValue() == true && needUpdate == false) {
				Toast.makeText(context_, "Database Update Not Needed", 1000).show();
			}
		}
	}

	private class UpdateDatabase extends AsyncTask<HttpResponse, Void, Boolean> {
		
		protected void onPreExecute() {
			//testing toast
			Toast.makeText(context_, "Buildings are being updated", 1000).show();
		}
		protected Boolean doInBackground(HttpResponse... params) {
			try {
				String responseBody = utils_.responseToString(params[0]);
				updateDatabaseVersion(0);
				deleteAll();
				JSONArray parsedArray = new JSONArray(responseBody);
				Log.d(TAG, "Got a json response with array length "+ parsedArray.length());
				for(int i = 0; i < parsedArray.length(); i++){
					JSONObject temp = (JSONObject) parsedArray.get(i);
					// XXX change "name" back to KEY_NAME, need to figure out how to do aliases
					updateMapData((int)temp.getInt(KEY_ROWID), temp.getString(KEY_NAME).toString(), (double) temp.getDouble(KEY_LAT), (double) temp.getDouble(KEY_LONG), temp.getString(KEY_URL).toString(), temp.getString(KEY_KEYWORDS).toString());
				}
				updateDatabaseVersion(version_);
				return true; 
			} catch(Exception e) {
				onError(e);
				return false;
			}
		}
		protected void onPostExecute(Boolean result) {
			//Testing toast
			if(result.booleanValue() == true) {
				Toast.makeText(context_, "Buildings updated", 1000).show();
			}
		}
	}
}
