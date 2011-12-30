package org.vt.hokiehelper;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class DiningActivity extends Activity {
	
	private static final String TAG = DiningActivity.class.getName();
	private ActionBar actionBar_;
	
	public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "Dining Activiy Created");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dining);
        // Find the root view
        
        actionBar_ = (ActionBar) findViewById(R.id.actionbar);
		actionBar_.setTitle("Dining");
        
        Action homeAction = new IntentAction(this, ActionBarHelper.createHomeIntent(this), R.drawable.home_button);
        actionBar_.setHomeAction(homeAction);
        
	}
}
