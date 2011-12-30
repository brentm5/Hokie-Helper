package org.vt.hokiehelper;

import java.text.DateFormat;
import java.util.Date;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;

public class NewsActivity extends ListActivity {

	private static final String TAG = NewsActivity.class.getName();
	private ActionBar actionBar_;
	private NewsDatabaseAdapter articleDatabase_;
	private ProgressDialog p_;
	private Handler updateHandler_;
	private PersistentDataDatabaseAdapter persistentDataDatabase_;
	private Date lastUpdated_;
	private boolean needUpdate = false;

	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "News Activiy Created");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.news);
		// Find the root view

		updateHandler_ = new Handler(new UpdateCallback());
		articleDatabase_ = new NewsDatabaseAdapter(this, updateHandler_);
		articleDatabase_.open();
		persistentDataDatabase_ = new PersistentDataDatabaseAdapter(this);
		persistentDataDatabase_.open();
		Log.d(TAG, "Opened Database");

		actionBar_ = (ActionBar) findViewById(R.id.actionbar);
		actionBar_.setTitle("News");

		Action homeAction = new IntentAction(this, ActionBarHelper.createHomeIntent(this), R.drawable.home_button);
		actionBar_.setHomeAction(homeAction);
		Action refreshAction = new IntentAction(this, ActionBarHelper.createRefreshIntent(this, this.getClass()), R.drawable.refresh_button);
		actionBar_.addAction(refreshAction);

		refreshArticles();
	}
	
	public void onNewIntent(Intent newIntent) {
		String action = newIntent.getAction();
		if(action == null) {
			return;
		}
		Log.d(TAG, "Action recieved: " + action);
		if (action.contentEquals(ActionBarHelper.ACTION_REFRESH)) {
			updateArticles();
		}
	}
	
	public void onStart() {
		Log.d(TAG, "Starting news activity");
		super.onStart();
		// Now create a new list adapter bound to the cursor.
		// SimpleListAdapter is designed for binding to a Cursor.
		String lastUpdate = persistentDataDatabase_	.fetchData("LAST_NEWS_UPDATE");
		Log.d(TAG, lastUpdate.toString());

		// If there is no entry in the persistent data it will update the
		// weather and add and entry
		if (lastUpdate.equals("")) {
			Log.d(TAG, "News last update variable not found so we update the articles");
			// then we need to setup the initial value of the date
			updateArticles();
		} else {
			try {
				Log.d(TAG, "News last update variable found");
				lastUpdated_ = new Date(lastUpdate);
			} catch (Exception e) {
				Log.e(TAG, "An error in parsing the string to a date occured " + lastUpdate.toString(), e);
				updateArticles();
			}
		}
		// set the title to the recent conditions or to the default title
		if ((new Date().getTime() - lastUpdated_.getTime()) > 3600000) {
			updateArticles();
		}
		if(!needUpdate){
			Toast.makeText(this, "Update Not Needed for news Database", 1000).show();
		}
	}
	
	public void onDestroy() {
		Log.d(TAG, "Destroying news activity");
		articleDatabase_.close();
		persistentDataDatabase_.close();
		super.onDestroy();
	}

	private void updateArticles() {
		needUpdate = true;
		p_ = ProgressDialog.show(this, "Please Wait", "Fetching articles..");
		articleDatabase_.updateDatabase(); // articles will be refreshed after update complete
		lastUpdated_ = new Date();
		DateFormat DF = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.MEDIUM);
		String currentTimeFormatted = DF.format(lastUpdated_);
		persistentDataDatabase_.updateData(4, "LAST_NEWS_UPDATE", currentTimeFormatted);
		Toast.makeText(this, "News Database updating", 1000).show();
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		try {
			Intent viewerInfo = new Intent(NewsActivity.this, ArticleViewerActivity.class);
			Cursor articleInfo = articleDatabase_.getArticle((int) id);
			viewerInfo.putExtra("title", articleInfo.getString(0));
			viewerInfo.putExtra("url", articleInfo.getString(1));
			articleInfo.close();
			startActivityIfNeeded(viewerInfo,0);
		} catch (Exception e) {
			Log.e(TAG, "An error occured ", e);
		}
	}

	public void refreshArticles() {
		try {
			Cursor c = articleDatabase_.getArticles();
			if(c != null) {
				startManagingCursor(c);
				String[] columns = { NewsDatabaseAdapter.KEY_TITLE };
				int[] to = { R.id.article_title };
				SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.article_list_view, c, columns, to);
				setListAdapter(adapter);
			}
		} catch (Exception e) {
			Log.e(TAG, "An error occured ", e);
		}
	}

	private class UpdateCallback implements Handler.Callback {
		public boolean handleMessage(Message msg) {
			try {
				if(msg.what == 1) {
					// database finished updating or already up to date, now refresh the gui
					refreshArticles();
				} else if(msg.what == -1) {
					// error when updating
				} else {
					// don't know what this is
				}
				if(p_.isShowing())
					p_.dismiss();
				return true;
			} catch (Exception e) {
				Log.e(TAG, "An error occured ", e);
				return false;
			}
		}

	}
}
