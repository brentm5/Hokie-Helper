package org.vt.hokiehelper;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;

public class BuildingsActivity extends ListActivity {

	private static final String TAG = BuildingsActivity.class.getName();
	private ActionBar actionBar_;
	private MapsDatabaseAdapter mapsDatabase_;

	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "Buildings Activity Created");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.buildings);
		// Find the root view

		actionBar_ = (ActionBar) findViewById(R.id.actionbar);
		actionBar_.setTitle("Buildings");

		Action homeAction = new IntentAction(this,
				ActionBarHelper.createHomeIntent(this), R.drawable.home_button);
		actionBar_.setHomeAction(homeAction);
		// TODO consider only showing one of these at a time
		Action mapAction = new IntentAction(this,
				ActionBarHelper.createMapIntent(this, MapsActivity.class),
				R.drawable.map_button);
		actionBar_.addAction(mapAction);

		Action searchAction = new IntentAction(this,
				ActionBarHelper.createSearchIntent(this, this.getClass()),
				R.drawable.search_button);
		actionBar_.addAction(searchAction);

		mapsDatabase_ = new MapsDatabaseAdapter(this);
		mapsDatabase_.open();
		
		refreshList();
	}

	public void onDestroy() {
		mapsDatabase_.close();
		super.onDestroy();
	}

	public void refreshList() {
		try {
			Cursor c = mapsDatabase_.getBuildings();
			if(c != null) {
				Log.d(TAG, "Cursor has " +  c.getCount() + " buildings");
				startManagingCursor(c);	
				String[] columns = { MapsDatabaseAdapter.KEY_NAME };
				int[] to = { R.id.building_name };
				SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.building_list_view, c, columns, to);
				setListAdapter(adapter);
			} else {
				Log.e(TAG, "Cursor null when refreshing list, database may be empty");
			}
		} catch (Exception e) {
			Log.e(TAG, "An error occured when refreshing list ", e);
		}
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		try {
			Intent mapInfo = new Intent(this, MapsActivity.class);
			mapInfo.setAction(Intent.ACTION_VIEW);
			mapInfo.setData(Uri.parse(String.valueOf(id)));
			startActivityIfNeeded(mapInfo,0);
		} catch (Exception e) {
			Log.e(TAG, "An error occured ", e);
		}
	}


	public void onNewIntent(Intent newIntent) {
		String action = newIntent.getAction();
		if(action == null) {
			return;
		}
		if (action.contentEquals(ActionBarHelper.ACTION_MAP)) {
			// Map button was pressed, go to map activity
			Intent showMap = new Intent(this, MapsActivity.class);
			startActivity(showMap);
		} else if (action.contentEquals(ActionBarHelper.ACTION_SEARCH)) {
			// Search button was pressed
			onSearchRequested();

		} else {
			Log.d(TAG, "Received intent with action " + action + ", ignoring it.");
		}
	}


}
