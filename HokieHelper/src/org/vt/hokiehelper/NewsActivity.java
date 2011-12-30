package org.vt.hokiehelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;

public class NewsActivity extends Activity {
	
	private static final String TAG = NewsActivity.class.getName();
	private ActionBar actionBar_;
	
	public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "News Activiy Created");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news);
        // Find the root view
        
        actionBar_ = (ActionBar) findViewById(R.id.actionbar);
		actionBar_.setTitle("News");
        
        Action homeAction = new IntentAction(this, ActionBarHelper.createHomeIntent(this), 
        		R.drawable.home_button);
        actionBar_.setHomeAction(homeAction);
        Action refreshAction = new IntentAction(this, 
        		ActionBarHelper.createRefreshIntent(this, this.getClass()), R.drawable.refresh_button);
        actionBar_.addAction(refreshAction);
	}
	
	public void onNewIntent(Intent newIntent) {;
		String action = newIntent.getAction();
		Toast t = Toast.makeText(this, "Received intent with action: " + action, 1000);
		t.show();
		if(action.contentEquals(ActionBarHelper.ACTION_REFRESH)) {
			// Refresh button was pressed
		}
	}
}
