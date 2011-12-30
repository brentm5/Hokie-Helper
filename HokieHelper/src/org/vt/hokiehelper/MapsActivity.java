package org.vt.hokiehelper;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;

public class MapsActivity extends MapActivity {

	private static final String TAG = MapsActivity.class.getName();
	private static final GeoPoint BURRUSS = new GeoPoint((int) (37.228591 * 1E6), (int) (-80.423198 * 1E6));
	
	private ActionBar actionBar_;
	private MapView map_;
	
	private MapController mc_;
	private LocationManager locManager_;
	private LocationListener locListener_;
	private boolean listening_ = false;
	private Location curLoc_;

	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "Map Activity Created");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		// Find the root view

		actionBar_ = (ActionBar) findViewById(R.id.actionbar);
		map_ = (MapView) findViewById(R.id.map);
		
		actionBar_.setTitle("Maps");

		Action homeAction = new IntentAction(this, 
				ActionBarHelper.createHomeIntent(this),
				R.drawable.home_button);
		actionBar_.setHomeAction(homeAction);

		Action listAction = new IntentAction(this, 
				ActionBarHelper.createListIntent(this, this.getClass()),
				R.drawable.list_button);
		actionBar_.addAction(listAction);
		
		Action searchAction = new IntentAction(this, 
				ActionBarHelper.createSearchIntent(this, this.getClass()),
				R.drawable.search_button);
		actionBar_.addAction(searchAction);
		
		mc_ = map_.getController();
		locManager_ = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locListener_ = new MyLocationListener();
		// TODO Not sure if this is the best zoom, need to test more
		mc_.setZoom(16);
		mc_.setCenter(BURRUSS);
	}			

	public void onStop() {
		super.onStop();
		// Stop listening for location updates when the activity is stopped, 
		// ie not on screen anymore
		stopLocationUpdates();
		listening_ = false;
	}
	
	public void onStart() {
		super.onStart();
		// Start listening for location updates when the activity is started
		listening_ = startLocationUpdates();
	}
	
	public void onNewIntent(Intent newIntent) {
		String action = newIntent.getAction();
		Toast t = Toast.makeText(this, "Received intent with action: " + action, 1000);
		t.show();
		if(action.contentEquals(ActionBarHelper.ACTION_LIST)) {
			// List button was pressed
			// Go to buildings activity
		}
		else if(action.contentEquals(Intent.ACTION_SEARCH)) {
			// Search button was pressed
//			swapBars();
			onSearchRequested();
		}
	}
	
	private void stopLocationUpdates() {
		if(listening_) {
			locManager_.removeUpdates(locListener_);
			listening_ = false;
		}
	}
	
	private boolean startLocationUpdates() {
		String provider = LocationManager.GPS_PROVIDER;
		// If gps disabled, use the network
		if(!locManager_.isProviderEnabled(provider)) { 
			// If the network is disabled, then there is now way to get location
			if(!locManager_.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
				// No way to get location, therefore fail
				return false;
			}
			provider = LocationManager.NETWORK_PROVIDER;
			
		}
		Log.d(TAG, "Using " + provider + " as a location provider.");
		locManager_.requestLocationUpdates(provider, 10000, 100, locListener_, Looper.myLooper());
		return true;
	}
	
	public class MyLocationListener implements LocationListener
	{
		public void onLocationChanged(Location loc)
		{
			curLoc_ = loc;
			double lat = loc.getLatitude();
			double lon = loc.getLongitude();
			
			GeoPoint g = new GeoPoint( (int)(lat*1E6), (int)(lon*1E6));
			mc_.animateTo(g);
			
			String text = "Current location is: " +
			"\nLatitude = " + lat +
			"\nLongitude = " + lon;
			
			Toast.makeText( getApplicationContext(),text,Toast.LENGTH_SHORT).show();
		}

		//@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}

		//@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		//@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}