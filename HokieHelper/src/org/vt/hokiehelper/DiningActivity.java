package org.vt.hokiehelper;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;

public class DiningActivity extends ListActivity {

	private static final String TAG = DiningActivity.class.getName();
	private ActionBar actionBar_;
	private HttpUtils utils_ = HttpUtils.get();
	private ProgressDialog p_;
	private JSONArray specials_ = null;
	private ArrayList<DiningHall> locations_ = new ArrayList<DiningHall>();
	private AlertDialog hallInfo_;
	
	
	// Use some type of lock to make them wait for the other before creating the
	// DiningHall object

	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "Dining Activiy Created");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dining);
		// Find the root view

		actionBar_ = (ActionBar) findViewById(R.id.actionbar);
		actionBar_.setTitle("Dining");

		Action homeAction = new IntentAction(this,
				ActionBarHelper.createHomeIntent(this), R.drawable.home_button);
		actionBar_.setHomeAction(homeAction);

		p_ = ProgressDialog.show(this, "Please Wait", "Loading Dining Halls...");
		hallInfo_ = new AlertDialog.Builder(this).setCancelable(true).create();
		getHours();
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		DiningHall hall = locations_.get(position);
		hallInfo_.setTitle(hall.getName());
		hallInfo_.setMessage(hall.getHours());
		hallInfo_.show();
	}

	private void getHours() {
		utils_.doGet("http://hokiehelper.appspot.com/dininghours", new HoursCallback());
	}

	private void getSpecials() {
		utils_.doGet("http://hokiehelper.appspot.com/diningspecials", new SpecialsCallback());
	}

	private void setupAdapter() {
		Collections.sort(locations_);
		DiningAdapter adapter = new DiningAdapter(locations_,this);
		setListAdapter(adapter);
		if(p_.isShowing()) {
			p_.dismiss();
		}
	}

	private class DiningAdapter extends BaseAdapter{

		private ArrayList<DiningHall> diningHalls_; 
		private LayoutInflater Inflater_;


		public DiningAdapter(ArrayList<DiningHall> list, Context context){
			diningHalls_ = list;
			Inflater_ = LayoutInflater.from(context);
		}

		public int getCount() {
			return diningHalls_.size();
		}

		public String getItem(int position) {
			return (String) diningHalls_.get(position).getName();
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = Inflater_.inflate(R.layout.dining_list_view, null);
				holder = new ViewHolder();
				holder.dining_title = (TextView) convertView.findViewById(R.id.dining_title);
				holder.dining_hours = (TextView) convertView.findViewById(R.id.dining_hours);
				holder.dining_status = (ImageView) convertView.findViewById(R.id.dining_status);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag(); 
			}

			holder.dining_title.setText((String)diningHalls_.get(position).getName());
			holder.dining_hours.setText((String) diningHalls_.get(position).getDesc());

			if(diningHalls_.get(position).isOpen()){
				holder.dining_status.setImageResource(R.drawable.green_circle);			
			}else{
				holder.dining_status.setImageResource(R.drawable.red_circle);	
			}
			return convertView;
		}

		class ViewHolder {
			TextView dining_title;
			TextView dining_hours;
			ImageView dining_status;
		}
	}

	private class HoursCallback implements HttpCallback {

		public void onResponse(HttpResponse resp) {
			String responseText;
			try {
				responseText = utils_.responseToString(resp);
				JSONArray hours = (JSONArray) JSONValue.parse(responseText);
				for(int i=0; i< hours.size();i++){
					JSONObject place = (JSONObject) hours.get(i);
					//Log.d(TAG, place.get("hall").toString().toLowerCase());
					//					DiningHall add = new DiningHall((String) place.get("hall"),(JSONArray) place.get("hours"), (JSONObject) specials_.get(0));
					DiningHall add = new DiningHall((String) place.get("hall"),(JSONArray) place.get("hours"));
					locations_.add(add);
				}
				setupAdapter();
				getSpecials();
			} catch (IOException e) {
				onError(e);
			} catch (Exception e) {
				onError(e);
			}
		}

		public void onError(Exception e) {
			if(p_.isShowing()) {
				p_.dismiss();
			}
			Log.e(TAG, "Received an error when getting hours", e);
			Toast t = Toast.makeText(getApplicationContext(), "Error getting hours, please try again", 1000);
			t.show();
			// Can't really do anything without the hours, finish the activity
			finish();
		}

	}

	private class SpecialsCallback implements HttpCallback {

		public void onResponse(HttpResponse resp) {
			String responseText;
			try {
				responseText = utils_.responseToString(resp);
				specials_ = (JSONArray) JSONValue.parse(responseText);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				onError(e);
			}
		}


		public void onError(Exception e) {
			if(p_.isShowing()) {
				p_.dismiss();
			}
			Log.e(TAG, "Received an error when getting specials", e);
			Toast t = Toast.makeText(getApplicationContext(), "Error getting specials, please try again", 1000);
			t.show();
		}

	}

	private class DiningHall implements Comparable<DiningHall>{
		private String name_ = "";
		private JSONArray hours_ = null;
		private Boolean isOpen_ = null;
		private String openHours_ = "Closed";
		private String allHours_ = null;
		private JSONObject specials_ = null;

		public DiningHall(String name, JSONArray hours, JSONObject specials) {
			this.name_ = name;
			this.hours_ = hours;
			this.specials_ = specials;
			doCalculations();
		}

		public DiningHall(String name, JSONArray hours) {
			this.name_ = name;
			this.hours_ = hours;
			this.specials_ = null;
			doCalculations();
		}

		public String getName() {
			return name_;
		}
		
		public String getHours() {
			return allHours_;
		}
		
		public String getDesc(){
			if(this.openHours_ == null)
				this.doCalculations();
			return openHours_;
		}

		public void doCalculations() {
			try {
				DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.MEDIUM);
				Date now = new Date();
				StringBuilder hours = new StringBuilder();
				//Log.d(TAG, this.name_);
				for(int i=0; i< this.hours_.size(); i++){
					JSONObject hall = (JSONObject) this.hours_.get(i);
					Date start = df.parse( (String) hall.get("start"));
					Date end = df.parse( (String) hall.get("end"));
					String name = (String) hall.get("name");
					DateFormat formatter = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
					hours.append(name + ":\n" + formatter.format(start) + " to " + formatter.format(end) + "\n\n");
					if(start.before(now) && end.after(now)){
						this.openHours_ = name;
						this.isOpen_ = true;
						break;
					}
				}
				allHours_ = hours.toString().trim();
				if(this.isOpen_ == null)
					this.isOpen_ = false;
			} catch (ParseException e) {
				Log.e(TAG, "Parse error",e);
				this.isOpen_ = false;
			}
		}

		public boolean hasSpecials() {
			return this.specials_==null;
		}

		public boolean isOpen() {
			if(this.isOpen_ == null)
				this.doCalculations();
			return this.isOpen_;
		}

		public int compareTo(DiningHall other) {
			if(!this.isOpen() && other.isOpen())
				return 1;
			else if(this.isOpen() && !other.isOpen())
				return -1;
			else
				return 0;
		}

	}
}
