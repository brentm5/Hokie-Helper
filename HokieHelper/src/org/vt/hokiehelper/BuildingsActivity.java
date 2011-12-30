package org.vt.hokiehelper;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;

public class BuildingsActivity extends ListActivity {
	
	private static final String TAG = BuildingsActivity.class.getName();
	private ActionBar actionBar_;
	private AutoCompleteTextView searchBar_;
	
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "Buildings Activiy Created");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		// Find the root view

		actionBar_ = (ActionBar) findViewById(R.id.actionbar);
		actionBar_.setTitle("Maps");

		Action homeAction = new IntentAction(this, 
				ActionBarHelper.createHomeIntent(this),
				R.drawable.home_button);
		actionBar_.setHomeAction(homeAction);
		//TODO consider only showing one of these at a time
		Action mapAction = new IntentAction(this, 
				ActionBarHelper.createMapIntent(this, this.getClass()),	
				R.drawable.map_button);
		actionBar_.addAction(mapAction);
		
		Action searchAction = new IntentAction(this, 
				ActionBarHelper.createSearchIntent(this, this.getClass()),
				R.drawable.search_button);
		actionBar_.addAction(searchAction);
	}
	
	public void onNewIntent(Intent newIntent) {
		String action = newIntent.getAction();
		Toast t = Toast.makeText(this, "Received intent with action: " + action, 1000);
		t.show();
		if(action.contentEquals(ActionBarHelper.ACTION_MAP)) {
			// Map button was pressed
			// Go to map activity
		}
		else if(action.contentEquals(Intent.ACTION_SEARCH)) {
			// Search button was pressed or search query was passed
			String query = newIntent.getStringExtra(SearchManager.QUERY);
			if(query == null) {
				onSearchRequested();
			} else {
				performSearch(query);
			}
		}
	}
	
	// Maybe maps should be the searchable given that we want to put something on a mapview once
	// the search is performed
	private void performSearch(String query) {
		
	}
	

}
