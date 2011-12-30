package org.vt.hokiehelper;


import java.text.DateFormat;
import java.util.Date;


import org.apache.http.HttpResponse;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.markupartist.android.widget.ActionBar;

import android.app.Activity;
import android.app.LauncherActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

public class HokieHelperMainActivity extends Activity implements
View.OnClickListener {
    /** Called when the activity is first created. */
	
	private static final String TAG = HokieHelperMainActivity.class.getName();
	
	//GUI Elements
	private GridView mainMenu_;
	private ActionBar actionBar_;
	private PersistentDataDatabaseAdapter persistentDataDatabase_;
	private HttpUtils utils_;
	private String lastConditions_;
	private Date lastUpdated_;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "Main Menu Activiy Created");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        // Find the root view
        
        utils_ = HttpUtils.get(); //setup our http stack

        mainMenu_ = (GridView) findViewById(R.id.mainMenu);
        mainMenu_.setAdapter(new MainMenuGridAdapter(this));
        actionBar_ = (ActionBar) findViewById(R.id.actionbar);
        Log.d(TAG, "Setup GUI elements");
        
        //Setup the persistent database adapter
        persistentDataDatabase_ = new PersistentDataDatabaseAdapter(this);
        persistentDataDatabase_.open();
        Log.d(TAG, "Opened Database");
        
        String lastUpdate = persistentDataDatabase_.fetchData("LAST_WEATHER_UPDATE");
        //If there is no entry in the persistent data it will update the weather and add and entry
        if( lastUpdate.equals("")){
        	Log.d(TAG, "Weather last update variable not found so we update the weather");
        	// then we need to setup the initial value of the date
        	updateWeather();
        }else {
        	try{
        		Log.d(TAG, "Weather last update variable found");
        		lastUpdated_ = new Date( lastUpdate );
        	}catch(Exception e) {
        		Log.e(TAG, "An error in parsing the string occured" + lastUpdated_.getDate() + "hell  "+ lastUpdated_.toString(), e);
        		updateWeather();
        	}
        	try {
        		lastConditions_ = persistentDataDatabase_.fetchData("LAST_WEATHER_CONDITIONS");
        		actionBar_.setTitle(lastConditions_);
        	}catch(Exception e){
        		Log.e(TAG, "An error in the database for some reason", e);
        	}
        }
		actionBar_.setTitle(R.string.app_name);
		
		if((new Date().getTime() - lastUpdated_.getTime()) > 900000){
			Log.d(TAG, "Weather Updated");
			updateWeather();
		}
    }
    
    @Override
	protected void onStop() {
		super.onStop();
		persistentDataDatabase_.close();
	}

	public void onStart() {
    	super.onStart();
    	persistentDataDatabase_.open();
		if((new Date().getTime() - lastUpdated_.getTime()) > 900000){
			updateWeather();
		}else {
    		actionBar_.setTitle(lastConditions_);
		}
    }
    
    public void onNewIntent(Intent newIntent) {
    	setIntent(newIntent);
    }

    public void onClick(View v) {
    	Log.d(TAG, "A list view was clicked");
    	Intent nextActivity = null;
    	switch (v.getId()) {
		case 0:
			 nextActivity = new Intent(HokieHelperMainActivity.this,
			 MapsActivity.class);	
			break;
		case 1:
			nextActivity = new Intent(HokieHelperMainActivity.this,
					 DiningActivity.class);
			break;
		case 2:
			nextActivity = new Intent(HokieHelperMainActivity.this,
					 NewsActivity.class);
			break;
		case 3:
			nextActivity = new Intent(HokieHelperMainActivity.this,
					 FootballActivity.class);
			break;
		case 4:
			nextActivity = new Intent(HokieHelperMainActivity.this,
					 RecSportsActivity.class);
			break;
		case 5:
			nextActivity = new Intent(HokieHelperMainActivity.this,
					 InfoActivity.class);
			break;
		default:

			break;

		};
		if(nextActivity != null) {
			startActivity(nextActivity);
		}
    }
    
	private void updateWeather() {
		String searchUrl = "http://mobile.srh.weather.gov/port_mp_ns.php?CityName=Blacksburg&site=RNK&State=VA&warnzone=VAZ014";
		//String searchUrl = "http://forecast.weather.gov/MapClick.php?lat=37.20800&lon=-80.40800&FcstType=dwml";
		utils_.doGet(searchUrl, new WeatherCallback());
		Log.d(TAG, "Sending get request to update weather");
		lastUpdated_ = new Date();
		DateFormat DF = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.MEDIUM);
        String currentTimeFormatted = DF.format(lastUpdated_);
        persistentDataDatabase_.updateData(1, "LAST_WEATHER_UPDATE", currentTimeFormatted);
	}
	
	private class WeatherCallback implements HttpCallback {
		public void onResponse(HttpResponse resp) {
			Document doc = null;
			try {
				Log.d(TAG, "Weather Updated");
				doc = Jsoup.parse(utils_.responseToString(resp));
				Log.d(TAG, "Response gotten from request");
				String forecast = doc.body().childNode(0).childNode(10).toString();
				forecast = forecast.replace("&deg;", "\u00B0");
				lastConditions_ = "Weather: " + forecast.replace(":", " -");
				persistentDataDatabase_.updateData(2, "LAST_WEATHER_CONDITIONS", lastConditions_);
				actionBar_.setTitle(lastConditions_);
				Toast t = Toast.makeText(getApplicationContext(), "Weather updated", 1000);
				t.show();
			} catch (Exception e) {
				Log.e(TAG, "Error getting weather response: " + e.getLocalizedMessage());
			}

		}

		public void onError(Exception e) {
			Log.e(TAG, "Error when getting weather: " + e.getLocalizedMessage());
		}
	}
}