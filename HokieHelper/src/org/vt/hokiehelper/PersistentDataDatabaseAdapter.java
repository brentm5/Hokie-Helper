package org.vt.hokiehelper;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

/**
 * @author Brent Montague
 * 
 */
public class PersistentDataDatabaseAdapter {

	private static final String TAG = PersistentDataDatabaseAdapter.class.getName();
	
	public static final String KEY_ROWID = "id";
	public static final String KEY_KEY = "key";
	public static final String KEY_VALUE = "value";
	private static final String DATABASE_TABLE = "persistentData";
	private Context context;
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;

	public PersistentDataDatabaseAdapter(Context context) {
		this.context = context;
	}

	public PersistentDataDatabaseAdapter open() throws SQLException {
		dbHelper = new DatabaseHelper(context);
		database = dbHelper.getWritableDatabase();
		return this;
	}

	public long updateData(int id, String key, String value) {
		ContentValues initialValues = createContentValues(id, key, value);
		Log.d(TAG, "Added data " + key + "With value " + value);
		return database.insertWithOnConflict(DATABASE_TABLE, null, initialValues, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public boolean deleteRow(int rowId) {
		return database.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}
	
	public String fetchData(String key) {
		Log.d(TAG, "Finding data with KEY " + key);
		Cursor tempCursur = database.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_KEY, KEY_VALUE }, KEY_KEY + "= '" + key + "'", null, null, null, null, null);
		if(tempCursur.getCount() == 0){
			return new String();
		}else {
			tempCursur.moveToFirst();
			return tempCursur.getString(2);
		}
	}

	public boolean deleteAllData() {
		return database.delete(DATABASE_TABLE, null, null) > 0;
	}
	
	private ContentValues createContentValues(int id, String key,String value) {
		ContentValues values = new ContentValues();
		values.put(KEY_ROWID, id);
		values.put(KEY_KEY, key);
		values.put(KEY_VALUE, value);
		return values;
	}

	public void close() {
		dbHelper.close();
	}
}
