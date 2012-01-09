package org.vt.hokiehelper;

import java.text.DateFormat;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.GridView;

import com.google.ads.AdRequest;
import com.google.ads.AdView;
import com.markupartist.android.widget.ActionBar;

public class HokieHelperMainActivity extends Activity implements
View.OnClickListener, HttpCallback {
	/** Called when the activity is first created. */

	private static final String TAG = HokieHelperMainActivity.class.getName();

	private static final String DEFAULT_TITLE = "Hokie Helper - Menu";

	// GUI Elements
	private GridView mainMenu_;
	private Handler handler_ = new Handler();
	private HttpUtils utils_ = HttpUtils.get();
	private ActionBar actionBar_;
	private PersistentDataDatabaseAdapter persistentDataDatabase_;
	private String lastConditions_;
	private Date lastUpdated_;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "Main Menu Activity Created");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Find the root view
		mainMenu_ = (GridView) findViewById(R.id.mainMenu);
		mainMenu_.setAdapter(new MainMenuGridAdapter(this));
		actionBar_ = (ActionBar) findViewById(R.id.actionbar);
		Log.d(TAG, "Setup GUI elements");

		Eula.show(this);
		Log.d(TAG, "Show EULA if need be");

		// Look up the AdView as a resource and load a request.
//		AdView adView = (AdView) findViewById(R.id.adView);
//		adView.loadAd(new AdRequest());

		// Setup the persistent database adapter
		persistentDataDatabase_ = new PersistentDataDatabaseAdapter(this);
		persistentDataDatabase_.open();
		Log.d(TAG, "Opened Database");

		try {
			lastConditions_ = persistentDataDatabase_.fetchData("LAST_WEATHER_CONDITIONS");
		} catch (Exception e) {
			Log.e(TAG, "An error in the database for some reason", e);
		}

		String lastUpdate = persistentDataDatabase_	.fetchData("LAST_WEATHER_UPDATE");
		// If there is no entry in the persistent data it will update the
		// weather and add and entry
		if (lastUpdate.equals("")) {
			Log.d(TAG,
			"Weather last update variable not found so we update the weather");
			// then we need to setup the initial value of the date
			updateWeather();
		} else {
			try {
				Log.d(TAG, "Weather last update variable found");
				lastUpdated_ = new Date(lastUpdate);
			} catch (Exception e) {
				Log.e(TAG, "An error in parsing the string to a date occured "
						+ lastUpdate.toString(), e);
				updateWeather(); // jsut redo it to make sure and reset the
				// error
			}
		}

		// set the title to the recent conditions or to the default title
		if ((new Date().getTime() - lastUpdated_.getTime()) > 900000) {
			updateWeather();
		} else {
			if (lastConditions_ == null || lastConditions_.equals(""))
				actionBar_.setTitle(DEFAULT_TITLE);
			else
				actionBar_.setTitle(lastConditions_);
		}
	}

	protected void onRestart() {
		super.onRestart();
		//persistentDataDatabase_.open();
		if ((new Date().getTime() - lastUpdated_.getTime()) > 900000) {
			updateWeather();
		} else {
			if (lastConditions_ == null || lastConditions_.equals(""))
				actionBar_.setTitle(DEFAULT_TITLE);
			else
				actionBar_.setTitle(lastConditions_);
		}
	}

	protected void onDestroy() {
		super.onDestroy();
		persistentDataDatabase_.close();
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

		}
		;
		if (nextActivity != null) {
			startActivity(nextActivity);
		}
	}

	private void updateWeather() {
		actionBar_.setTitle("Updating...");
		String searchUrl = "http://mobile.srh.weather.gov/port_mp_ns.php?CityName=Blacksburg&site=RNK&State=VA&warnzone=VAZ014";
		// String searchUrl =
		// "http://forecast.weather.gov/MapClick.php?lat=37.20800&lon=-80.40800&FcstType=dwml";
		utils_.doGet(searchUrl, this);
		Log.d(TAG, "Sending get request to update weather");

		// Update the alst updated in the database
		lastUpdated_ = new Date();
		DateFormat DF = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
				DateFormat.MEDIUM);
		String currentTimeFormatted = DF.format(lastUpdated_);
		persistentDataDatabase_.updateData(1, "LAST_WEATHER_UPDATE",
				currentTimeFormatted);
	}

	public void onResponse(final HttpResponse resp) {
		handler_.post(new WeatherParser(resp));
	}

	public void onError(Exception e) {
		actionBar_.setTitle(DEFAULT_TITLE);
		Log.e(TAG, "Error when getting weather: " + e.getLocalizedMessage());
	}

	// The weather parser runnable
	private class WeatherParser implements Runnable {
		private HttpResponse resp_;

		public WeatherParser(HttpResponse resp) {
			resp_ = resp;
		}

		public void run() {
			Document doc = null;
			try {
				Log.d(TAG, "Weather Updated");
				doc = Jsoup.parse(utils_.responseToString(resp_));
				Log.d(TAG, "Response gotten from request");
				String forecast = doc.body().childNode(0).childNode(10)
				.toString();
				forecast = forecast.replace("&deg;", "\u00B0");
				lastConditions_ = "Weather: " + forecast.replace(":", " -");
				persistentDataDatabase_.updateData(2,
						"LAST_WEATHER_CONDITIONS", lastConditions_);
				actionBar_.setTitle(lastConditions_);
			} catch (Exception e) {
				Log.e(TAG,
						"Error getting weather response: "
						+ e.getLocalizedMessage());
				actionBar_.setTitle(DEFAULT_TITLE);
			}

		}
	}
}