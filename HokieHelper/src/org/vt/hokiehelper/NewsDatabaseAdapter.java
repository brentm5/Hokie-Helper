package org.vt.hokiehelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.JSONValue;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.Toast;

/**
 * @author Brent Montague
 * 
 */
public class NewsDatabaseAdapter implements HttpCallback {

	private static final String TAG = NewsDatabaseAdapter.class.getName();
	public static final String KEY_ROWID = "_id";
	public static final String KEY_TITLE = "title";
	public static final String KEY_URL = "url";
	private static final String DATABASE_TABLE = "newsData";
	private HttpUtils utils_ = HttpUtils.get();
	private Context context_;
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	private Handler returnHandler_;
	
	public NewsDatabaseAdapter(Context context, Handler updateHandler) {
		this.context_ = context;
		returnHandler_ = updateHandler;
		Log.d(TAG, "News Database created");
	}

	public NewsDatabaseAdapter open() throws SQLException {
		dbHelper = new DatabaseHelper(context_);
		database = dbHelper.getWritableDatabase();
		Log.d(TAG, "News Database opened");
		return this;
	}
	
	public long updateNewsData(int id, String title, String url) {
		ContentValues initialValues = createContentValues(id, title, url);
		Log.d(TAG, "Added data " + title);
		return database.insertWithOnConflict(DATABASE_TABLE, null,
				initialValues, SQLiteDatabase.CONFLICT_REPLACE);
	}

	
	public Cursor getArticle(Integer id){
		Cursor retCursor = database.query(DATABASE_TABLE, new String[] {KEY_TITLE,KEY_URL}, KEY_ROWID + " = ?",new String[] {id.toString()},
				null, null, null);
		if(retCursor.moveToFirst()){
			Log.d(TAG, "Got soemthing " + retCursor.toString());
			return retCursor;
		}else{
			return null;
		}
		
	}
	
	public Cursor getArticles(){
		Cursor retCursor = database.query(DATABASE_TABLE, null,null,null, null, null, null);
		if(retCursor.moveToFirst()){
			return retCursor;
		}else{
			return null;
		}
	}

	public void updateDatabase() {
		utils_.doGet("http://hokiehelper.appspot.com/newsdatabaseupdate", this);
	}

	public boolean deleteAll() {
		Log.d(TAG, "Deleting the Article Database");
		return database.delete(DATABASE_TABLE, null, null) > 0;
	}
	
	

	private ContentValues createContentValues(int id, String title,
		 String url) {
		ContentValues values = new ContentValues();
		values.put(KEY_ROWID, id);
		values.put(KEY_TITLE, title);
		values.put(KEY_URL, url);
		return values;
	}

	public void close() {
		Log.d(TAG, "Closing news database");
		dbHelper.close();
	}

	public void onError(Exception e) {
		returnHandler_.sendEmptyMessage(-1); //may need to check this return value, ignoring for now
		Log.e(TAG, "An Error Occured", e);
	}

	public void onResponse(final HttpResponse resp) {
		Log.d(TAG, "Got a response");
		try {
			UpdateDatabaseTask updater = new UpdateDatabaseTask();
			updater.execute(resp);
		} catch (Exception e) {
			this.onError(e);
		}

	}
	
	private class UpdateDatabaseTask extends AsyncTask<HttpResponse, Void, Boolean> {
		protected Boolean doInBackground(HttpResponse... params) {
			
			try {
				deleteAll();
				String json = utils_.responseToString(params[0]);
				JSONArray parsedArray = new JSONArray(json);
				Log.d(TAG, "Got a json response with array length "+ parsedArray.length());
				for(int i = 0; i < parsedArray.length(); i++){
					JSONObject temp = (JSONObject) parsedArray.get(i);
					updateNewsData((int)temp.getInt(KEY_ROWID), temp.getString(KEY_TITLE).toString(),temp.getString(KEY_URL).toString());
				}
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
