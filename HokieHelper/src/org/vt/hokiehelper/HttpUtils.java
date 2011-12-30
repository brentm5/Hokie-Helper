package org.vt.hokiehelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

public class HttpUtils {

	private static HttpUtils instance_;
	private HttpParams params_;
	private HttpClient client_;
	//private HttpContext context_;

	public static synchronized HttpUtils get() {
		if (instance_ == null) {
			instance_ = new HttpUtils();
		}

		return instance_;
	}

	private HttpUtils() {
		SchemeRegistry reg = new SchemeRegistry();
		//sslSocFactory.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
		reg.register(new Scheme("https",SSLSocketFactory.getSocketFactory(),443));
		reg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		
		params_ = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params_, 5000);
		HttpConnectionParams.setSoTimeout(params_, 3000);
		params_.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 30);
	    params_.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(30));
	    params_.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
	    //HttpProtocolParams.setVersion(params_, HttpVersion.HTTP_1_1);
		
		ThreadSafeClientConnManager connMgr = new ThreadSafeClientConnManager(params_, reg);
		client_ = new DefaultHttpClient(connMgr, params_);
		//context_ = new BasicHttpContext();
	}

	public void doGet(String url, HttpCallback callback) {
		HttpGet get = new HttpGet(url);
		HttpRequestInfo rinfo = new HttpRequestInfo(get, callback);
		AsyncHttpTask task = new AsyncHttpTask();
		task.execute(rinfo);
	}

	public void doPost(String url, Map<String, String> params,
			HttpCallback callback) {
		try {

			HttpPost post = new HttpPost(url);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
					params.size());

			for (String key : params.keySet()) {
				nameValuePairs
						.add(new BasicNameValuePair(key, params.get(key)));
			}

			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(
					nameValuePairs);
			post.setEntity(entity);

			HttpRequestInfo rinfo = new HttpRequestInfo(post, callback);
			AsyncHttpTask task = new AsyncHttpTask();
			task.execute(rinfo);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String responseToString(HttpResponse response) throws IOException {
		InputStream in = response.getEntity().getContent();
		InputStreamReader ir = new InputStreamReader(in);
		BufferedReader bin = new BufferedReader(ir);
		String line = null;
		StringBuffer buff = new StringBuffer();
		while ((line = bin.readLine()) != null) {
			buff.append(line + "\n");
		}
		bin.close();
		return buff.toString();
	}
	
    public void tearDownHttpStack() {
        instance_ = null;
}
}
