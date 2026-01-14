package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.odiousapps.weewxweather.weeWXAppCommon.doStackOutput;
import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;

@SuppressWarnings({"unused", "SequencedCollectionMethodCanBeUsed"})
public class SafeWebView extends WebView
{
	private static final List<String> bad_paths = new ArrayList<>();

	public SafeWebView(@NonNull Context context)
	{
		super(context);
		initSettings();
	}

	public SafeWebView(@NonNull Context context, @Nullable AttributeSet attrs)
	{
		super(context, attrs);
		initSettings();
	}

	public SafeWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		initSettings();
	}

	// Initialize SafeWebView safely with common settings.
	@SuppressLint("SetJavaScriptEnabled")
	void initSettings()
	{
		bad_paths.add("favicon.ico");

		try
		{
//			ws.setAllowFileAccess(true);
//			ws.setAllowFileAccessFromFileURLs(true);
//			ws.setAllowUniversalAccessFromFileURLs(true);

			// Always safe to create on API 24+
			loadData("", "text/html", "utf-8");
			WebSettings ws = getSettings();
			ws.setJavaScriptEnabled(true);
			ws.setDomStorageEnabled(true);
			ws.setLoadsImagesAutomatically(true);
			ws.setAllowContentAccess(true);

			ws.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
			ws.setLoadWithOverviewMode(true);
			ws.setUseWideViewPort(true);
			ws.setDisplayZoomControls(false);
			ws.setBuiltInZoomControls(false);

			//ws.setCacheMode(WebSettings.LOAD_NO_CACHE);

			// Apply user agent
			ws.setUserAgentString(NetworkClient.UA);

			setOverScrollMode(View.OVER_SCROLL_NEVER);
			setNestedScrollingEnabled(true);
			setVerticalScrollBarEnabled(false);
			setHorizontalScrollBarEnabled(false);
			setWebChromeClient(new myWebChromeClient());
		} catch(Throwable t) {
			doStackOutput(t);
		}
	}

	public void destroy()
	{
		try
		{
			loadData("", "text/html", "utf-8");

			stopLoading();
			pauseTimers();

			removeAllViews();
			clearCache(true);
			clearHistory();
			clearFormData();
			removeJavascriptInterface("AndroidBridge");

			ViewGroup parent = (ViewGroup)getParent();
			if(parent != null)
				parent.removeView(this);

			CookieManager cookieManager = CookieManager.getInstance();
			cookieManager.removeAllCookies(null);
			cookieManager.flush();

			LogMessage("SafeWebView.destroy() Finished destroying myself!");
		} catch(Exception e) {
			doStackOutput(e);
		}

		try
		{
			LogMessage("SafeWebView.destroy() calling super.destroy()");
			super.destroy();
		} catch(Exception ignored) {}
	}

	@Override
	public void loadUrl(@NonNull String url)
	{
		LogMessage("Loading URL: " + url);
		super.loadUrl(url);
	}

	public void setOnPageFinishedListener(final OnPageFinishedListener listener, final boolean overrideRequest)
	{
		if(overrideRequest)
		{
			setWebViewClient(new WebViewClient()
			{
				@Override
				public void onPageFinished(WebView view, String url)
				{
					super.onPageFinished(view, url);
					listener.onPageFinished(SafeWebView.this, url);
				}

				@Override
				public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request)
				{
					String url = request.getUrl().toString().strip();
					if(url.isBlank())
						return null;

					String method = request.getMethod();
					if(method.equalsIgnoreCase("options"))
						return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));

					// Simple host/URL Path blocking
					HttpUrl url2 = HttpUrl.parse(url);
					if(url2 == null)
						return null;

					List<String> paths = url2.encodedPathSegments();
					if(!paths.isEmpty())
					{
						String path = paths.get(paths.size() - 1);

						if(bad_paths.contains(path))
						{
							LogMessage("SafeWebView.shouldInterceptRequest() Bad path found: " + path);
							return null;
						}
					}

					Request.Builder b = new Request.Builder().url(url);
					for(Map.Entry<String, String> h : request.getRequestHeaders().entrySet())
						b.header(h.getKey(), h.getValue());

					b.header("User-Agent", NetworkClient.UA);

					if(method.equalsIgnoreCase("post"))
						b.post(RequestBody.create(new byte[0], null));

					OkHttpClient okHttpClient = NetworkClient.getNoTimeoutInstance();

					try(Response response = okHttpClient.newCall(b.build()).execute())
					{
						byte[] bytes = response.body().bytes();

						LogMessage("SafeWebView.shouldInterceptRequest() Got a response of " + bytes.length);

						ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

						String encoding = "UTF-8";
						String mime = "text/plain";

						MediaType mt = response.body().contentType();
						if(mt != null)
						{
							mime = mt.type() + "/" + mt.subtype();

							Charset cs = mt.charset();
							if(cs != null)
							{
								encoding = cs.name();
							}
						}

						return new WebResourceResponse(mime, encoding, bais);
					} catch(SocketTimeoutException | UnknownHostException ignored) {
					} catch(Exception e) {
						LogMessage("SafeWebView.shouldInterceptRequest() Error! e: " + e, true, KeyValue.e);
						doStackOutput(e);
					}

					return null;
				}
			});
		} else {
			setWebViewClient(new WebViewClient()
			{
				@Override
				public void onPageFinished(WebView view, String url)
				{
					super.onPageFinished(view, url);
					listener.onPageFinished(SafeWebView.this, url);
				}
			});
		}
	}

	public interface OnPageFinishedListener
	{
		void onPageFinished(SafeWebView view, String url);
	}

	static final class myWebChromeClient extends WebChromeClient
	{
		@Override
		public boolean onConsoleMessage(ConsoleMessage cm)
		{
			String msg = cm.message().strip();
			if(msg.isBlank())
				return true;

			if(msg.startsWith("message="))
			{
				msg = msg.substring(8);
				LogMessage("ConsoleMessage: " + msg, KeyValue.d);
			} else
				LogMessage("ConsoleMessage: " + msg, KeyValue.v);

			return true;
		}
	}
}