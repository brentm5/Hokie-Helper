package org.vt.hokiehelper;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
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
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;

public class RecSportsActivity extends ListActivity implements HttpCallback {

	private static final String TAG = RecSportsActivity.class.getName();
	private ActionBar actionBar_;
	private HttpUtils utils_ = HttpUtils.get();
	private ProgressDialog p_;
	private AlertDialog classInfo_;
	private ArrayList<HashMap<String, Object>> classes_;

	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "Rec Sports Activity Created");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recsports);
		// Find the root view

		actionBar_ = (ActionBar) findViewById(R.id.actionbar);
		actionBar_.setTitle("Recreational Sports");

		Action homeAction = new IntentAction(this, ActionBarHelper.createHomeIntent(this), R.drawable.home_button); 
		actionBar_.setHomeAction(homeAction);
		Action refreshAction = new IntentAction(this, ActionBarHelper.createRefreshIntent(this, this.getClass()), R.drawable.refresh_button);
		actionBar_.addAction(refreshAction);
		refresh();
		classInfo_ = new AlertDialog.Builder(this).setCancelable(true).create();
	}
	protected void onListItemClick(ListView l, View v, int position, long id) {
		HashMap<String, Object> curClass = classes_.get(position);
		classInfo_.setTitle((String)curClass.get("name"));
		classInfo_.setMessage((String) (curClass.get("building") + " - " + curClass.get("classroom")));
		classInfo_.show();
	}

	private void refresh() {
		p_ = ProgressDialog.show(this, "Please Wait", "Refreshing classes...");
		utils_.doGet("http://hokiehelper.appspot.com/recsportspassthrough", this);
	}

	protected void onNewIntent(Intent newIntent) {
		String action = newIntent.getAction();
		if(action == null) {
			return;
		}
		Log.d(TAG, "Action recieved: " + action);
		if (action.contentEquals(ActionBarHelper.ACTION_REFRESH)) {
			refresh();
		}
	}

	public void onResponse(HttpResponse resp) {
		try {
			UpdateClassesTask updater = new UpdateClassesTask(this);
			updater.execute(resp);
		}catch (Exception e) {
			onError(e);
		}

	}

	public void onError(Exception e) {
		Log.e(TAG, "An error Occured", e);
		if(p_.isShowing())
			p_.dismiss();
	}

	private class UpdateClassesTask extends AsyncTask<HttpResponse, Void, ArrayList <HashMap<String, Object>>> {

		Context context_;
		
		public UpdateClassesTask(Context context) {
			context_ = context;
		}
		
		@Override
		protected ArrayList<HashMap<String, Object>> doInBackground(
				HttpResponse... params) {
			ArrayList <HashMap<String, Object>> classes = new ArrayList<HashMap<String,Object>>();
			try {				
				String json = utils_.responseToString(params[0]);
				//parse xml add to hashmap
				Object obj = JSONValue.parse(json);
				JSONArray array=(JSONArray)obj;
				HashMap<String, Object> hm;
				Time now = new Time();
				now.setToNow();
				for(int i=0; i< array.size(); i++){
					Time time = new Time();
					JSONObject class_ = (JSONObject) array.get(i);
					time.parse3339((String)class_.get("starts"));
					if(time.after(now)) {
						hm = new HashMap<String, Object>();
						hm.put("name", (String)class_.get("name"));
						hm.put("classroom", (String)class_.get("classroom"));
						hm.put("building", (String)class_.get("building"));
						hm.put("start", time);
						hm.put("spacesleft", (String)class_.get("spacesleft"));
						hm.put("spaces", (String)class_.get("spaces"));
						classes.add(hm);
					}
				}
				if(classes.isEmpty()){
					hm = new HashMap<String, Object>();
					hm.put("name", "No Classes Left for Today");
					hm.put("classroom", "");
					hm.put("building", "");
					hm.put("start", "");
					hm.put("spacesleft", "");
					hm.put("spaces", "");
					classes.add(hm);
				}
			} catch(Exception e) {
				onError(e);				
			} 
			return classes;
		}
		
		protected void onPostExecute(ArrayList <HashMap<String, Object>> result) {
			ListAdapter adapter = new ClassesAdapter(result,context_);
			classes_ = result;
			setListAdapter(adapter);
			// Expecting that p_ is shown when we first send off the doGet to httputils
			if(p_.isShowing())
				p_.dismiss();
		}
	}

	private class ClassesAdapter extends BaseAdapter{

		private ArrayList<HashMap<String, Object>> classes_; 
		private LayoutInflater Inflater_;


		public ClassesAdapter(ArrayList<HashMap<String, Object>> classes, Context context){
			classes_ = classes;
			Inflater_ = LayoutInflater.from(context);
		}

		public int getCount() {
			return classes_.size();
		}

		public Object getItem(int position) {
			return classes_.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			
			if (convertView == null) {
			    convertView = Inflater_.inflate(R.layout.recsports_list_view, null);
			    holder = new ViewHolder();
			    holder.class_name = (TextView) convertView.findViewById(R.id.class_name);
			    holder.classroom = (TextView) convertView.findViewById(R.id.class_location);
			    holder.start_time = (TextView) convertView.findViewById(R.id.class_start);
			    holder.status_dot = (ImageView) convertView.findViewById(R.id.class_status);
			    convertView.setTag(holder);
			}else {
				holder = (ViewHolder) convertView.getTag(); 
			}
			try {
				String totalSpaces = (String) classes_.get(position).get("spaces");
				String spacesLeft = (String) classes_.get(position).get("spacesleft");
				Double percentleft = ((double)Integer.decode(spacesLeft) / (double) Integer.decode(totalSpaces))*100.0;
				holder.class_name.setText((String) classes_.get(position).get("name") + " - " + spacesLeft + "/" +  totalSpaces);
				String location = (String) classes_.get(position).get("classroom") + "\n" + (String) classes_.get(position).get("building");
				//holder.status_dot.setVisibility(View.GONE);
				if(percentleft == 0.0){
					holder.status_dot.setImageResource(R.drawable.red_circle);
				}else if(percentleft >= 40.0){
					holder.status_dot.setImageResource(R.drawable.green_circle);
				}else {
					holder.status_dot.setImageResource(R.drawable.orange_circle);
				}
				holder.classroom.setText(location);
				//DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
				Time start = (Time) classes_.get(position).get("start");
				Date trial = new Date(start.toMillis(true));
				DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
				//holder.start_time.setText(df.format(start).toString());
				holder.start_time.setText((String)df.format(trial).toString());
			} catch (NumberFormatException e) {
				holder.status_dot.setVisibility(View.GONE);
				holder.class_name.setText((String) classes_.get(position).get("name"));
				holder.classroom.setText("");
				holder.start_time.setText("");
				onError(e);
			}catch (Exception e1){
				onError(e1);
			}
			return convertView;
		}

		class ViewHolder {
			TextView class_name;
			TextView classroom;
			TextView start_time;
			ImageView status_dot;
		}
	}
}
