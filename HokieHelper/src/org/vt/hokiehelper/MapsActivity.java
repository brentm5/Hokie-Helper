package org.vt.hokiehelper;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;

public class MapsActivity extends MapActivity {

	private static final String TAG = MapsActivity.class.getName();
	private static final GeoPoint BURRUSS = new GeoPoint(
			(int) (37.228591 * 1E6), (int) (-80.423198 * 1E6));
	
	private ActionBar actionBar_;
	private MapView map_;

	private MapController mc_;
	private MapsDatabaseAdapter mapsDatabase_;
	private MyLocationOverlay userOverlay_;
	private BuildingOverlays buildings_;

	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "Map Activity Created");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		// Find the root view

		// setup the GUI elements
		actionBar_ = (ActionBar) findViewById(R.id.actionbar);
		map_ = (MapView) findViewById(R.id.map);

		mapsDatabase_ = new MapsDatabaseAdapter(this);
		mapsDatabase_.open();
		mapsDatabase_.checkDatabaseVersion();

		// setup the actionbar
		actionBar_.setTitle("Maps");

		Action homeAction = new IntentAction(this,
				ActionBarHelper.createHomeIntent(this), R.drawable.home_button);
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
		// TODO Not sure if this is the best zoom, need to test more
		mc_.setZoom(15);
		map_.setBuiltInZoomControls(true);

		// Add user location overlay
		userOverlay_ = new MyLocationOverlay(this, map_);
		map_.getOverlays().add(userOverlay_);
		userOverlay_.runOnFirstFix(new Runnable() {
			public void run() {
				mc_.animateTo(userOverlay_.getMyLocation());
			}
		});

		buildings_ = new BuildingOverlays(getResources().getDrawable(R.drawable.marker),this);
		map_.getOverlays().add(buildings_);
		// Keep this here, or else will receive an error on map when first started. 
		buildings_.addBuilding("Burruss Hall", BURRUSS);
	}

	public void onNewIntent(Intent newIntent) {
		String action = newIntent.getAction();
		if(action == null) {
			return;
		}
		if (action.contentEquals(ActionBarHelper.ACTION_LIST)) {
			// List button was pressed
			// Go to buildings activity
			Intent showList = new Intent(this, BuildingsActivity.class);
			startActivity(showList);

		} else if (action.contentEquals(ActionBarHelper.ACTION_SEARCH)) {
			// Search button was pressed
			onSearchRequested();

		} else if (action.contentEquals(Intent.ACTION_SEARCH)) {
			String query = newIntent.getStringExtra(SearchManager.QUERY);
			Cursor results = mapsDatabase_.searchLocations(query);
			displaySearchResult(results);		
		} else if(action.contentEquals(Intent.ACTION_VIEW)) {
			Log.d(TAG, "URI received for ACTION_VIEW, " + newIntent.getDataString());
			int id = Integer.valueOf(newIntent.getData().getLastPathSegment());
			Cursor building = mapsDatabase_.fetchLocationData(id);
			displaySearchResult(building);
		} else {
			Log.d(TAG, "Received intent with action " + action + ", ignoring it.");
		}
	}

	private void displaySearchResult(Cursor result) {
		if(result != null) {
			String name = result.getString(result.getColumnIndex(MapsDatabaseAdapter.KEY_NAME));
			GeoPoint point = 
					new GeoPoint( (int) (result.getDouble(result.getColumnIndex(MapsDatabaseAdapter.KEY_LAT)) * 1E6), 
							(int) (result.getDouble(result.getColumnIndex(MapsDatabaseAdapter.KEY_LONG))* 1E6));
			result.close();
			// Calculate span between the building and current location
			GeoPoint userPoint = userOverlay_.getMyLocation();
			if(userPoint != null) {
				int bLat = point.getLatitudeE6();
				int userLat = userPoint.getLatitudeE6();
				
				int bLon = point.getLongitudeE6();
				int userLon = point.getLongitudeE6();
				
				int latSpan = (bLat > userLat) ? bLat - userLat : userLat - bLat;
				int lonSpan = (bLon > userLon) ? bLon - userLon : userLon - bLon;
				mc_.zoomToSpan((int)(latSpan * 2.1), (int)(lonSpan * 2.1));
			}
			clearBuildingOverlays();
			buildings_.addBuilding(name, point);
			map_.invalidate();

		} else {
			Log.e(TAG, "Search result was null, not able to display it");
		}
		Log.d(TAG, "Current overlays " + map_.getOverlays().toString());
	}

	public void onPause() {
		Log.d(TAG, "Pausing maps activity");
		super.onPause();
	}

	public void onStop() {
		Log.d(TAG, "Stopping maps activity");
		super.onStop();
		// Stop listening for location updates when the activity is stopped,
		// ie not on screen anymore
		userOverlay_.disableMyLocation();
		userOverlay_.disableCompass();
	}

	public void onStart() {
		super.onStart();
		Log.d(TAG, "Starting maps activity");
		// Start listening for location updates when the activity is started
		userOverlay_.enableMyLocation();
		userOverlay_.enableCompass();
	}

	public void onDestroy() {
		Log.d(TAG, "Destroying maps activity");
		mapsDatabase_.close();
		super.onDestroy();
	}

	public void clearBuildingOverlays() {
		//		map_.getOverlays().remove(buildings_);
		buildings_.clear();
	}

	private class BuildingOverlays extends ItemizedOverlay<OverlayItem> {

		private Context context_;
		private ArrayList<OverlayItem> buildingOverlays_ = new ArrayList<OverlayItem>();
		private AlertDialog d_;

		private BuildingOverlays(Drawable defaultMarker) {
			super(boundCenterBottom(defaultMarker));
		}

		public BuildingOverlays(Drawable defaultMarker, Context context) {
			this(defaultMarker);
			context_ = context;
			AlertDialog.Builder builder = new AlertDialog.Builder(context_);
			builder.setCancelable(true);
			d_ = builder.create();

		}

		@Override
		protected OverlayItem createItem(int i) {
			return buildingOverlays_.get(i);
		}

		@Override
		public int size() {
			return buildingOverlays_.size();
		}

		public void addBuilding(String name, GeoPoint location) {
			OverlayItem overlay = new OverlayItem(location, name, "");
			addOverlay(overlay);
			Log.d(TAG, name + " added to the map @ " + location.toString());
			mc_.animateTo(location);
		}

		public void addOverlay(OverlayItem overlay) {
			buildingOverlays_.add(overlay);
			populate();
		}

		public void clear() {
			buildingOverlays_.clear();
			populate();
			Log.d(TAG, "Cleared map markers");
		}

		protected boolean onTap(int index) {
			GeoPoint point = buildingOverlays_.get(index).getPoint();
			float latitude = point.getLatitudeE6() / 1000000F;
			float longitude = point.getLongitudeE6() / 1000000F;
			final String mapsUrl = "http://maps.google.com/maps?f=d&daddr=" +
					latitude + "," + longitude; 
			d_.setMessage(point.toString());
			d_.setTitle(buildingOverlays_.get(index).getTitle());
			d_.setButton(DialogInterface.BUTTON_POSITIVE, "Walking Directions",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String url = mapsUrl + "&dirflg=w";
					Intent wDirections = new Intent(Intent.ACTION_VIEW, 
							Uri.parse(url));
					context_.startActivity(wDirections);							
				}				
			});
			d_.setButton(DialogInterface.BUTTON_NEUTRAL, "Bus Directions",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String url = mapsUrl + "&dirflg=r";
					Intent bDirections = new Intent(Intent.ACTION_VIEW, 
							Uri.parse(url));
					context_.startActivity(bDirections);							
				}				
			});
			d_.setButton(DialogInterface.BUTTON_NEGATIVE, "Driving Directions",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String url = mapsUrl + "&dirflg=d";
					Intent dDirections = new Intent(Intent.ACTION_VIEW, 
							Uri.parse(url));
					context_.startActivity(dDirections);							
				}				
			});
			d_.show();
			return true;
		}

	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}