package org.vt.hokiehelper;

import org.apache.http.HttpResponse;
import org.apache.http.client.RedirectHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;

public class AsyncHttpTask extends
		AsyncTask<HttpRequestInfo, Integer, HttpRequestInfo> {

	@Override
	protected HttpRequestInfo doInBackground(HttpRequestInfo... params) {
		HttpRequestInfo rInfo = params[0];
		try {
			DefaultHttpClient client = new DefaultHttpClient();
//			RedirectHandler rHandler = new SpaceRedirectHandler();
//			client.setRedirectHandler(rHandler);
			HttpResponse resp = client.execute(rInfo.getRequest());
			rInfo.setResponse(resp);
		} catch (Exception e) {
			rInfo.setException(e);
		}

		return rInfo;
	}

	@Override
	protected void onPostExecute(HttpRequestInfo result) {
		super.onPostExecute(result);
		result.requestFinished();
	}

}
