package org.vt.hokiehelper;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class InfoDatabaseAdapter implements HttpCallback {

	private static final String TAG = InfoDatabaseAdapter.class.getName();
	public static final String KEY_ROWID = "_id";
	public static final String KEY_TITLE = "name";
	public static final String KEY_DESCRIPTION = "desc";
	public static final String KEY_TYPE = "type";
	public static final String KEY_PAYLOAD = "payload";
	public static final String TYPE_PHONE = "phone";
	public static final String TYPE_EMAIL = "email";
	public static final String TYPE_WEB = "web";
	private static final String DATABASE_TABLE = "infoData";
	private HttpUtils utils_ = HttpUtils.get();
	private PersistentDataDatabaseAdapter persistentDataDatabase_;
	private Context context_;
	private Double version_ = null;
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	private Handler returnHandler_;

	public InfoDatabaseAdapter(Context context, Handler updateHandler) {
		this.context_ = context;
		persistentDataDatabase_ = new PersistentDataDatabaseAdapter(this.context_);
		returnHandler_ = updateHandler;
		Log.d(TAG, "Info database adapater created");
		checkDatabaseVersion();
	}

	public InfoDatabaseAdapter open() throws SQLException {
		dbHelper = new DatabaseHelper(context_);
		database = dbHelper.getWritableDatabase();
		Log.d(TAG, "Info database opened");
		return this;
	}

	public Cursor fetchAll() {
		String[] columns = { KEY_ROWID , KEY_TITLE, KEY_TYPE, KEY_DESCRIPTION, KEY_PAYLOAD };
		return database.query(DATABASE_TABLE, columns, null, null, null, null, null);
	}

	public boolean deleteAll() {
		Log.d(TAG, "Deleting the Info Database");
		return database.delete(DATABASE_TABLE, null, null) > 0;
	}
	
	private void updateDatabaseVersion(double version){
		persistentDataDatabase_.open();
		persistentDataDatabase_.updateData(5, "INFO_DATABASE_VERSION", Double.toString(version));
		Log.d(TAG, "Updated Maps database version to "+ version);
		persistentDataDatabase_.close();
	}
	
	private double getDatbaseVersion() {
		persistentDataDatabase_.open();
		String version = persistentDataDatabase_.fetchData("INFO_DATABASE_VERSION");
		Log.d(TAG, "Current Maps database version "+ version);
		persistentDataDatabase_.close();
		if(version == null || version.equals("")){ 
			return 0.0;
		}
		return Double.parseDouble(version);
	}
	
	public long updateInfoData(int id, String title, String desc, String type,  String payload) {
		ContentValues initialValues = createContentValues(id, title, desc, type, payload);
		Log.d(TAG, "Added data " + title + " of type " + type);
		return database.insertWithOnConflict(DATABASE_TABLE, null, initialValues, SQLiteDatabase.CONFLICT_REPLACE);
		
	}

	private ContentValues createContentValues(int id, String title, String desc,  String type, String payload) {
		ContentValues values = new ContentValues();
		values.put(KEY_ROWID, id);
		values.put(KEY_TITLE, title);
		values.put(KEY_DESCRIPTION, desc);
		values.put(KEY_TYPE, type);
		values.put(KEY_PAYLOAD, payload);
		return values;
	}
	
	public void close() {
		Log.d(TAG, "Closing info database");
		dbHelper.close();
	}
	
	public void checkDatabaseVersion(){
		Log.d(TAG, "Info Database checked for version");
		utils_.doGet("http://hokiehelper.appspot.com/infodatabaseupdate?q=version", this);
	}
	
	public void updateDatabase() {
		Log.d(TAG, "Info Database updated");
		utils_.doGet("http://hokiehelper.appspot.com/infodatabaseupdate?q=database", this);
	}
	
	public void onError(Exception e) {
		Log.e(TAG, "An Error Occured", e);
	}

	public void onResponse(final HttpResponse resp) {
		Log.d(TAG, "Got a response");
		try {
			if(version_ == null){
				UpdateDatabaseVersion updater = new UpdateDatabaseVersion();
				updater.execute(resp);
			}else{	
				UpdateDatabaseTask updater = new UpdateDatabaseTask();
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
					updateDatabase();
					return true;
				}
				return false; 
			} catch(Exception e) {
				onError(e);
				return false;
			}
			
		}
		protected void onPostExecute(Boolean result) {
			if(result){
				returnHandler_.sendEmptyMessage(2);
			}else{
				returnHandler_.sendEmptyMessage(0);
			}
		}
	}
	
	private class UpdateDatabaseTask extends AsyncTask<HttpResponse, Void, Boolean> {
		protected Boolean doInBackground(HttpResponse... params) {
			try {
				updateDatabaseVersion(0);
				deleteAll();
				String json = utils_.responseToString(params[0]);
				JSONArray parsedArray = new JSONArray(json);
				Log.d(TAG, "Got a json response with array length "+ parsedArray.length());
				for(int i = 0; i < parsedArray.length(); i++){
					JSONObject temp = (JSONObject) parsedArray.get(i);
					updateInfoData((int)temp.getInt(KEY_ROWID), temp.getString(KEY_TITLE).toString(),temp.getString(KEY_DESCRIPTION).toString(),temp.getString(KEY_TYPE).toString(),temp.getString(KEY_PAYLOAD).toString());
				}
				updateDatabaseVersion(version_);
				return true; 
			} catch(Exception e) {
				onError(e);
				return false;
			}
		}
		
		protected void onPostExecute(Boolean result) {
			if(result){
				returnHandler_.sendEmptyMessage(1);
			}else{
				returnHandler_.sendEmptyMessage(-1);
			}
		}
	}
	
	
}
