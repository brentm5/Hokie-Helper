package org.vt.hokiehelper;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;

public class ArticleViewerActivity extends Activity implements HttpCallback {

	private static final String TAG = ArticleViewerActivity.class.getName();
	//	private static final String host = "http://localhost:8888/";
	private static final String host = "http://hokiehelper.appspot.com/";

	private ActionBar actionBar_;
	private TextView title_;
	private TextView author_;
	private TextView body_;
	private ImageView image_;
	private HttpUtils myUtil_;
	private ProgressDialog p_;



	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "Article Activity Created");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.articleviewer);
		// Find the root view

		actionBar_ = (ActionBar) findViewById(R.id.actionbar);

		Action homeAction = new IntentAction(this,
				ActionBarHelper.createHomeIntent(this), R.drawable.home_button);
		actionBar_.setHomeAction(homeAction);		

		title_ = (TextView) findViewById(R.id.article_viewer_title);
		author_ = (TextView) findViewById(R.id.article_viewer_author);
		body_ = (TextView) findViewById(R.id.article_viewer_body);
		image_ = (ImageView) findViewById(R.id.article_viewer_image);

		myUtil_ = HttpUtils.get();

		p_ = ProgressDialog.show(this, "", "Fetching article...");
		handleIntent(getIntent());
	}

	public void onDestroy() {
		if(p_.isShowing()) {
			p_.dismiss();
		}
		super.onDestroy();
	}

	protected void onNewIntent(Intent intent) {
		// This activity should only be created once, after which it will go here instead of recreating the activity
		// while the application is running
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {		
		Bundle extras = intent.getExtras();
		title_.setText(extras.getString("title"));

		String link = extras.getString("url");
		String requestUrl = host + "articleparser?link=" + link;
		Log.d(TAG, "Fetching article for " + link);
		myUtil_.doGet(requestUrl, this);
	}

	public void onResponse(HttpResponse resp) {

		try {			
			String responseText = myUtil_.responseToString(resp);
			// Convert from unicode to UTF-8
			responseText = new String(responseText.getBytes("UTF-8"), "UTF-8");
			Log.d(TAG, "Response: " + responseText);
			JSONObject articleInfo = (JSONObject) JSONValue.parse(responseText);
			Log.d(TAG, "ArticleInfo: " + articleInfo.toString());
			author_.setText((String)articleInfo.get("author"));
			body_.setText((String)articleInfo.get("body"));
			String imgUrl = (String)articleInfo.get("image");
			if(imgUrl == null) {
				image_.setVisibility(View.GONE);
			}
			else {
				imgUrl = "http://www.vtnews.vt.edu" + imgUrl;
				String alttext = (String)articleInfo.get("alttext");
				image_.setVisibility(View.VISIBLE);
				// TODO this is incorrect, in order to do this we need to create
				// an AsyncTask that downloads the image in a separate thread
				// then sets it
//				image_.setImageURI(Uri.parse(imgUrl));
			}

		} catch (IOException e) {
			Log.e(TAG, "Response to string failed", e);
			Toast t = Toast.makeText(getApplicationContext(), "Error getting article, please try again" , 1000);
			t.show();
			finish();
		}
		if(p_.isShowing())
			p_.dismiss();

	}

	public void onError(Exception e) {
		Log.e(TAG, "Error getting article", e);
		Toast t = Toast.makeText(getApplicationContext(), "Error getting article, please try again" , 1000);
		t.show();
		finish();

	}
}
