package org.vt.hokiehelper;



import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;

public class InfoActivity extends ListActivity {

	private static final String TAG = InfoActivity.class.getName();
	private ActionBar actionBar_;
	private InfoDatabaseAdapter infoDatabase_;
	private Handler updateHandler_;
	private ProgressDialog p_ = null;
	private PersistentDataDatabaseAdapter persistentDataDatabase_;
	private Context context_;

	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "Info Activiy Created");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info);
		
		context_ = this;
		// Find the root view
		
		updateHandler_ = new Handler(new UpdateCallback());
		
		actionBar_ = (ActionBar) findViewById(R.id.actionbar);
		actionBar_.setTitle("Information");

		Action homeAction = new IntentAction(this,
				ActionBarHelper.createHomeIntent(this), R.drawable.home_button);
		actionBar_.setHomeAction(homeAction);
		
		
		persistentDataDatabase_ = new PersistentDataDatabaseAdapter(this);
		persistentDataDatabase_.open();
		String version = persistentDataDatabase_.fetchData("INFO_DATABASE_VERSION");
		persistentDataDatabase_.close();
		if(version == null || version.equals("")){
			p_ = ProgressDialog.show(this, "Please Wait", "Loading Information...");
		}
		
		infoDatabase_ = new InfoDatabaseAdapter(this,updateHandler_);
		infoDatabase_.open();
		refreshInfo();
	}

	public void onDestroy() {
		Log.d(TAG, "Destroying info activity");
		infoDatabase_.close();
		super.onDestroy();
	}

	public void refreshInfo() {
		Cursor c = infoDatabase_.fetchAll();
		startManagingCursor(c);
		//String[] columns = { infoDatabase_.KEY_TITLE, infoDatabase_.KEY_DESCRIPTION, infoDatabase_.KEY_TYPE, infoDatabase_.KEY_PAYLOAD };
		InfoCursorAdapter adapter = new InfoCursorAdapter(this, c, true);
		setListAdapter(adapter);
	}
	
	public class InfoCursorAdapter extends CursorAdapter {
		 private LayoutInflater layoutInflater_;
		 private Context context_;

		public InfoCursorAdapter(Context context, Cursor c,boolean autoRequery) {
			super(context, c, autoRequery);
			context_ = context;
			layoutInflater_ = LayoutInflater.from(context); 
		}

		@Override
		public void bindView(View view, final Context context, Cursor cursor) {
			final String title = cursor.getString(cursor.getColumnIndexOrThrow(infoDatabase_.KEY_TITLE));
			final String desc = cursor.getString(cursor.getColumnIndexOrThrow(infoDatabase_.KEY_DESCRIPTION));
			final String type = cursor.getString(cursor.getColumnIndexOrThrow(infoDatabase_.KEY_TYPE));
			final String payload = cursor.getString(cursor.getColumnIndexOrThrow(infoDatabase_.KEY_PAYLOAD));

	        TextView title_text = (TextView) view.findViewById(R.id.info_name);
	        TextView title_desc = (TextView) view.findViewById(R.id.info_desc);
	        ImageView type_image = (ImageView) view.findViewById(R.id.info_type);
	        
	        //Setupt eh view
	        if (title_text != null) {
	            title_text.setText(title);
	        }
	        
	        if (title_desc != null && desc != null) {
	        	if(desc.equals("")) {
	        		//title_desc.setVisibility(View.GONE);
	        	}else{
	        		title_desc.setText(desc);
	        	}
	        }
	        
	        if(type_image != null && type != null){
	        	if(type.equals(infoDatabase_.TYPE_PHONE)){
	        		type_image.setImageResource(R.drawable.phone);
	    			view.setOnClickListener(new View.OnClickListener() {
	    				
	    				public void onClick(View v) {
	    					try {
								Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+payload));
								startActivity(i);
							} catch (Exception e) {
								Toast.makeText(context, "Faulty Payload", 600).show();
								Log.e(TAG, "Something went terribly wrong, probly faulty url ", e);
							}
	    				}
	    			});
	        	}else if(type.equals(infoDatabase_.TYPE_WEB)){
	        		type_image.setImageResource(R.drawable.webpage);
	    			view.setOnClickListener(new View.OnClickListener() {
	    				
	    				public void onClick(View v) {
	    					try {
								Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(payload));
								startActivity(i);
							} catch (Exception e) {
								Toast.makeText(context, "Faulty Payload", 600).show();
								Log.e(TAG, "Something went terribly wrong, probly faulty url ", e);
							}
	    				}
	    			});
	        	}else if(type.equals(infoDatabase_.TYPE_EMAIL)){
	        		type_image.setImageResource(R.drawable.email);
	    			view.setOnClickListener(new View.OnClickListener() {
	    				
	    				public void onClick(View v) {
	    					try {
								Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
								emailIntent.setType("plain/text");
								emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{ payload});
								startActivity(Intent.createChooser(emailIntent, "Send mail..."));
							} catch (Exception e) {
								Toast.makeText(context, "Faulty Payload", 600).show();
								Log.e(TAG, "Something went terribly wrong, probly faulty url ", e);
							}
	    				}
	    			});
	        	}else{
	        		type_image.setVisibility(View.GONE);
	        	}
	        	
	        }
	       
	  
		}

		@Override
		public View newView(final Context context, final Cursor cursor, ViewGroup parent) {
			View v = layoutInflater_.inflate(R.layout.info_list_view, parent, false);
	        return v;
		}

	}
	private class UpdateCallback implements Handler.Callback {
		public boolean handleMessage(Message msg) {
			if(msg.what == 1) {
				// database finished updating or already up to date, now refresh the gui
				refreshInfo();
				if(p_ != null && p_.isShowing())
					p_.dismiss();
			} else if(msg.what == -1) {
				// error when updating
				
			} else if(msg.what == 0){
				//this is if the version is up to date and the databse does not need to be updated there shouldn be a progress meter
				if(p_ != null && p_.isShowing())
					p_.dismiss();
				
			}else if(msg.what == 2){
				if(p_ == null)
					p_ = ProgressDialog.show(context_, "Please Wait", "Loading Information...");
				
				// don't know what this is
				//this is if the version returns that it needs to be updated
			}

			return true;
		}

	}

}
