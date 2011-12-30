package org.vt.hokiehelper;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.json.simple.JSONValue;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;

public class FootballActivity extends ListActivity implements HttpCallback {

	private static final String TAG = FootballActivity.class.getName();
	private ActionBar actionBar_;
	private HttpUtils utils_ = HttpUtils.get();
	private ProgressDialog p_;

	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "Football Activity Created");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.football);
		// Find the root view

		actionBar_ = (ActionBar) findViewById(R.id.actionbar);
		actionBar_.setTitle("Football");

		Action homeAction = new IntentAction(this,
				ActionBarHelper.createHomeIntent(this), R.drawable.home_button);
		actionBar_.setHomeAction(homeAction);

		update();
	}

	private void update() {
		p_ = ProgressDialog.show(this, "Please Wait", "Fetching football schedule...");
		utils_.doGet("http://hokiehelper.appspot.com/footballschedule", this);
	}

	public void onResponse(HttpResponse resp) {
		try {
			UpdateScheduleTask updater = new UpdateScheduleTask(this);
			updater.execute(resp);
		}catch (Exception e) {
			onError(e);
		}
	}

	public void onError(Exception e) {
		Log.e(TAG, "An error Occured", e);
		if(p_.isShowing()) {
			p_.dismiss();
		}
	}

	private class UpdateScheduleTask extends
	AsyncTask<HttpResponse, Void, ArrayList <HashMap<String, Object>>> {

		Context context_;

		public UpdateScheduleTask(Context context) {
			context_ = context;
		}

		@SuppressWarnings("unchecked")
		protected ArrayList<HashMap<String, Object>> doInBackground(
				HttpResponse... params) {
			ArrayList <HashMap<String, Object>> schedule = null;
			try {				
				String json = utils_.responseToString(params[0]);
				// Parse json
				schedule = (ArrayList <HashMap<String, Object>>)(JSONValue.parse(json));

			} catch(Exception e) {
				onError(e);				
			} 
			return schedule;
		}

		protected void onPostExecute(ArrayList <HashMap<String, Object>> result) {
			ListAdapter adapter = new GameAdapter(context_,R.layout.football_list_view, result);
			setListAdapter(adapter);
			// Expecting that p_ is shown when we first send off the doGet to httputils
			if(p_.isShowing())
				p_.dismiss();
		}
	}

	private class GameAdapter extends ArrayAdapter<HashMap<String, Object>>{

		private ArrayList<HashMap<String, Object>> schedule_; 


		public GameAdapter(Context context, int textViewResourceId, ArrayList<HashMap<String, Object>> schedule){
			super(context, textViewResourceId, schedule);
			schedule_ = schedule;
		}

		public int getCount() {
			return schedule_.size();
		}

		public HashMap<String, Object> getItem(int position) {
			return schedule_.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			HashMap<String, Object> game = schedule_.get(position);
			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.football_list_view, null);
				holder = new ViewHolder();
				holder.football_date = (TextView) convertView.findViewById(R.id.football_date);
				holder.football_info = (TextView) convertView.findViewById(R.id.football_info);
				holder.football_loc = (TextView) convertView.findViewById(R.id.football_loc);
				holder.football_opponent = (TextView) convertView.findViewById(R.id.football_opponent);
				holder.football_rank = (TextView) convertView.findViewById(R.id.football_rank);
				holder.football_special = (TextView) convertView.findViewById(R.id.football_special);			    
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag(); 
			}
			holder.football_date.setText((CharSequence) game.get("date"));
			holder.football_info.setText((CharSequence) game.get("info"));
			holder.football_loc.setText((CharSequence) game.get("loc"));
			holder.football_opponent.setText((CharSequence) game.get("opponent"));
			String rank = (String) game.get("rank");
			if(rank != null) {
				rank = "AP Rank: " + rank;
			} else {
				rank = "Unranked";
			}
			holder.football_rank.setText(rank);
			String special = (String) game.get("special");
			if(special != null) {	
				holder.football_special.setVisibility(View.VISIBLE);
				holder.football_special.setText(special);
			}
			else {
				holder.football_special.setText("");
				holder.football_special.setVisibility(View.GONE);

			}
			
			convertView.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					
					
				}
			});

			return convertView;
		}

		class ViewHolder {
			TextView football_date;
			TextView football_info;
			TextView football_loc;
			TextView football_opponent;
			TextView football_rank;
			TextView football_special;
		}
	}
}
