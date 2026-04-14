package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.odiousapps.weewxweather.weeWXAppCommon.doStackOutput;
import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;
import static com.odiousapps.weewxweather.weeWXAppCommon.is_blank;
import static com.odiousapps.weewxweather.weeWXAppCommon.is_valid_url;

@SuppressWarnings("SequencedCollectionMethodCanBeUsed")
public class SafeWebView extends WebView
{
	private final List<String> bad_paths = new ArrayList<>();
	private String Url = null;
	private String data = null;
	private String baseUrl = null;
	private String mimeType = null;
	private String encoding = null;
	private String historyUrl = null;
	private boolean hasBeenDestroyed = false;
	private boolean timeout;
	private boolean outputDebugLogs = false;

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
		if(hasBeenDestroyed)
		{
			LogMessage("SafeWebView.initSettings() hasBeenDestroyed is true, stop actions on dead webviews...");
			return;
		}

		bad_paths.add("favicon.ico");

		timeout = true;

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
		if(hasBeenDestroyed)
		{
			LogMessage("SafeWebView.destroy() hasBeenDestroyed is true, stop actions on dead webviews...");
			return;
		}

		if(outputDebugLogs)
			LogMessage("SafeWebView.destroy() setting hasBeenDestroyed to true, finish destroying now...");

		hasBeenDestroyed = true;

		try
		{
			if(outputDebugLogs)
				LogMessage("SafeWebView.destroy() stopping WebView loading...");

			stopLoading();
		} catch(Exception ignored) {}

		try
		{
			if(outputDebugLogs)
				LogMessage("SafeWebView.destroy() clearing cache, history and formdata...");

			clearCache(true);
			clearHistory();
			clearFormData();
		} catch(Exception ignored) {}

		try
		{
			if(outputDebugLogs)
				LogMessage("SafeWebView.destroy() wiping cookies...");

			CookieManager cookieManager = CookieManager.getInstance();
			cookieManager.removeAllCookies(null);
			cookieManager.flush();
		} catch(Exception ignored) {}

		try
		{
			if(outputDebugLogs)
				LogMessage("SafeWebView.destroy() removing any bridges...");

			removeJavascriptInterface("AndroidBridge");
		} catch(Exception ignored) {}

		try
		{
			if(outputDebugLogs)
				LogMessage("SafeWebView.destroy() removing from any parents...");

			ViewGroup parent = (ViewGroup)getParent();
			if(parent != null)
				parent.removeView(this);

		} catch(Exception ignored) {}

		try
		{
			if(outputDebugLogs)
				LogMessage("SafeWebView.destroy() removing any views");

			removeAllViews();
		} catch(Exception ignored) {}

		if(outputDebugLogs)
			LogMessage("SafeWebView.destroy() calling super.destroy()");

		super.destroy();
	}

	@Override
	public void loadUrl(@NonNull String url)
	{
		if(hasBeenDestroyed)
		{
			LogMessage("SafeWebView.loadUrl() hasBeenDestroyed is true, stop actions on dead webviews...");
			return;
		}

		if(outputDebugLogs)
			LogMessage("SafeWebView.loadUrl() Loading URL: " + url);

		this.Url = url;
		this.baseUrl = null;
		this.data = null;
		this.mimeType = null;
		this.encoding = null;
		this.historyUrl = null;
		super.loadUrl(url);
	}

	public void loadDataWithBaseURL(String baseUrl, @NonNull String data, String mimeType, String encoding, String historyUrl)
	{
		if(hasBeenDestroyed)
		{
			LogMessage("SafeWebView.loadDataWithBaseURL() hasBeenDestroyed is true, stop actions on dead webviews...");
			return;
		}

		if(outputDebugLogs)
			LogMessage("SafeWebView.loadDataWithBaseURL() calling super.loadDataWithBaseURL()");

		this.Url = null;
		this.baseUrl = baseUrl;
		this.data = data;
		this.mimeType = mimeType;
		this.encoding = encoding;
		this.historyUrl = historyUrl;
		super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
	}

	public void setOnCustomPageFinishedListener(final OnCustomPageFinishedListener listener, boolean overrideRequest)
	{
		CustomDns customDns = new CustomDns();
		if(overrideRequest)
		{
			setWebViewClient(new WebViewClient()
			{
				@Override
				public boolean onRenderProcessGone(WebView webView, RenderProcessGoneDetail detail)
				{
					if(hasBeenDestroyed)
					{
						LogMessage("SafeWebView.onRenderProcessGone() hasBeenDestroyed is true, stop actions on dead webviews...");
						return false;
					}

					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
					{
						if(!detail.didCrash())
						{
							// OOM kill — clean up and recreate
							restartWebview((SafeWebView)webView);

							// true = handled, prevents app crash
							return true;
						}
					}

					return false; // crash — let it propagate
				}

				@Override
				public void onPageStarted(WebView view, String url, Bitmap favicon)
				{
					super.onPageStarted(view, url, favicon);

					if(hasBeenDestroyed)
					{
						LogMessage("SafeWebView.onPageFinished() hasBeenDestroyed is true, stop actions on dead webviews...");
						return;
					}

					new Thread(() ->
					{
						timeout = true;

						try
						{
							Thread.sleep(30_000);
						} catch (InterruptedException e) {
							doStackOutput(e);
						}

						if(timeout)
						{
							if(outputDebugLogs)
								LogMessage("SafeWebView.onPageStarted() timeout! url: " + url);
						}
					}).start();
				}

				@Override
				public void onPageFinished(WebView view, String url)
				{
					super.onPageFinished(view, url);

					timeout = false;

					if(hasBeenDestroyed)
					{
						LogMessage("SafeWebView.onPageFinished() hasBeenDestroyed is true, stop actions on dead webviews...");
						return;
					}

					listener.onCustomPageFinished(SafeWebView.this, url);
				}

				@Override
				public void onLoadResource(WebView view, String url)
				{
					super.onLoadResource(view, url);

					if(outputDebugLogs)
						LogMessage("onLoadResource() url: " + url + ", progress: " + view.getProgress());
				}

				@Override
				public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request)
				{
					if(hasBeenDestroyed)
					{
						LogMessage("SafeWebView.shouldInterceptRequest() hasBeenDestroyed is true, stop actions on dead webviews...");
						return new WebResourceResponse("text/html", "UTF-8", null);
					}

					String url = request.getUrl().toString().strip();
					if(is_blank(url))
						return new WebResourceResponse("text/html", "UTF-8", null);

					String hostname = request.getUrl().getHost();
					if(!is_blank(hostname))
					{
						for(String bad_domain : customDns.bad_domains)
						{
							if(hostname.toLowerCase(Locale.ENGLISH).endsWith(bad_domain))
							{
								LogMessage("SafeWebView.shouldInterceptRequest() url blocked: " + url);
								return new WebResourceResponse("text/html", "UTF-8", null);
							}
						}
					}

					String method = request.getMethod();
					if(method.equalsIgnoreCase("options"))
						return new WebResourceResponse(weeWXApp.CONTENT_TYPE, "UTF-8", new ByteArrayInputStream(new byte[0]));

					// Simple host/URL Path blocking
					HttpUrl url2 = HttpUrl.parse(url);
					if(url2 == null)
						return new WebResourceResponse("text/html", "UTF-8", null);

					List<String> paths = url2.encodedPathSegments();
					if(!paths.isEmpty())
					{
						String path = paths.get(paths.size() - 1);

						if(bad_paths.contains(path))
						{
							LogMessage("SafeWebView.shouldInterceptRequest() Bad path found: " + path);
							return new WebResourceResponse("text/html", "UTF-8", null);
						}
					}

					Request.Builder b = new Request.Builder().header("Connection", "close").url(url);
					for(Map.Entry<String, String> h : request.getRequestHeaders().entrySet())
						b.header(h.getKey(), h.getValue());

					b.header("User-Agent", NetworkClient.UA);

					if(method.equalsIgnoreCase("post"))
						b.post(RequestBody.create(new byte[0], null));

					OkHttpClient okHttpClient = NetworkClient.getNoTimeoutInstance();

					try(Response response = okHttpClient.newCall(b.build()).execute())
					{
						byte[] bytes = response.body().bytes();

						if(outputDebugLogs)
							LogMessage("SafeWebView.shouldInterceptRequest() Got a response of " + bytes.length);

						ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

						String encoding = "UTF-8";
						String mime = weeWXApp.CONTENT_TYPE;

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

						WebResourceResponse wr = new WebResourceResponse(mime, encoding, bais);

						// copy CORS (and any other) headers
						Headers okHeaders = response.headers();
						Map<String, String> map = new HashMap<>();
						String[] responseHeaderNames = {
							// CORS
							"Access-Control-Allow-Origin",
							"Access-Control-Allow-Methods",
							"Access-Control-Allow-Headers",
							"Access-Control-Allow-Credentials",
							"Access-Control-Expose-Headers",
							"Access-Control-Max-Age",

							// Caching / freshness
							"Cache-Control",
							"Expires",
							"Pragma",
							"Age",
							"ETag",
							"If-None-Match",
							"Last-Modified",

							// Content metadata
							"Content-Type",
							"Content-Language",
							"Content-Encoding",
							"Content-Disposition",
							"Content-Range",
							"Accept-Ranges",

							// Cookies / sessions
							"Set-Cookie",
							"Set-Cookie2",
							"Cookie",

							// Security / policies
							"Strict-Transport-Security",
							"X-Frame-Options",
							"Content-Security-Policy",
							"Content-Security-Policy-Report-Only",
							"Referrer-Policy",
							"Permissions-Policy",
							"Expect-CT",
							"Feature-Policy",           // legacy name
							"X-Content-Type-Options",
							"X-XSS-Protection",

							// Cross-origin / timing / hints
							"Timing-Allow-Origin",
							"Origin",
							"Vary",
							"Link",
							"Server",
							"Host",
							"Via",

							// Performance / resource hints
							"Accept-Ranges",
							"Allow",
							"Retry-After",
							"Warning",
						};

						for(String name : responseHeaderNames)
						{
							List<String> values = okHeaders.values(name);
							if(!values.isEmpty())
							{
								// join multiple values with comma (HTTP allows multiple), or add first only
								map.put(name, String.join(", ", values));
							}
						}

						if(!map.isEmpty())
							wr.setResponseHeaders(map);

						return wr;
					} catch(SocketTimeoutException | UnknownHostException ignored) {
						//doStackOutput(se);
					} catch(Exception e) {
//						LogMessage("SafeWebView.shouldInterceptRequest() Error! e: " + e, true, KeyValue.e);
						doStackOutput(e);
					}

					return null;
				}
			});
		} else {
			setWebViewClient(new WebViewClient()
			{
				@Override
				public boolean onRenderProcessGone(WebView webView, RenderProcessGoneDetail detail)
				{
					if(hasBeenDestroyed)
					{
						LogMessage("SafeWebView.onRenderProcessGone() hasBeenDestroyed is true, stop actions on dead webviews...");
						return false;
					}

					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
					{
						if(!detail.didCrash())
						{
							// OOM kill — clean up and recreate
							restartWebview((SafeWebView)webView);

							// true = handled, prevents app crash
							return true;
						}
					}

					return false; // crash — let it propagate
				}

				@Override
				public void onPageStarted(WebView view, String url, Bitmap favicon)
				{
					super.onPageStarted(view, url, favicon);

					if(hasBeenDestroyed)
					{
						LogMessage("SafeWebView.onPageFinished() hasBeenDestroyed is true, stop actions on dead webviews...");
						return;
					}

					new Thread(() ->
					{
						timeout = true;

						try
						{
							Thread.sleep(30_000);
						} catch (InterruptedException e) {
							doStackOutput(e);
						}

						if(timeout)
						{
							if(outputDebugLogs)
								LogMessage("SafeWebView.onPageStarted() timeout! url: " + url);
						}
					}).start();
				}

				@Override
				public void onPageFinished(WebView view, String url)
				{
					super.onPageFinished(view, url);

					if(hasBeenDestroyed)
					{
						LogMessage("SafeWebView.onPageFinished() hasBeenDestroyed is true, stop actions on dead webviews...");
						return;
					}

					listener.onCustomPageFinished(SafeWebView.this, url);
				}

				@Override
				public void onLoadResource(WebView view, String url)
				{
					super.onLoadResource(view, url);

					if(outputDebugLogs)
						LogMessage("onLoadResource() url: " + url + ", progress: " + view.getProgress());
				}
			});
		}
	}

	private void restartWebview(SafeWebView webViewToRemake)
	{
		if(hasBeenDestroyed)
		{
			//LogMessage("SafeWebView.restartWebview() hasBeenDestroyed is true, stop actions on dead webviews...");
			return;
		}

		webViewToRemake.destroy();
		webViewToRemake = new SafeWebView(weeWXApp.getInstance());
		if(this.Url != null)
		{
			LogMessage("SafeWebView.restartWebview() restarting with URL: " + this.Url);
			webViewToRemake.loadUrl(this.Url);
		} else if(this.data != null) {
			LogMessage("SafeWebView.restartWebview() restarting with data...");
			webViewToRemake.loadDataWithBaseURL(this.baseUrl, this.data, this.mimeType, this.encoding, this.historyUrl);
		}
	}

	public void setDebugLogging(boolean newState)
	{
		if(newState != outputDebugLogs)
			outputDebugLogs = newState;
	}

	public interface OnCustomPageFinishedListener
	{
		void onCustomPageFinished(SafeWebView view, String url);
	}

	static final class myWebChromeClient extends WebChromeClient
	{
		@Override
		public boolean onConsoleMessage(ConsoleMessage cm)
		{
			String msg = cm.message().strip();
			if(is_valid_url(msg))
				return true;

			if(msg.startsWith("message="))
			{
				msg = msg.substring(8);
				LogMessage("SafeWebView.onConsoleMessage(): " + msg, KeyValue.d);
			} else
				LogMessage("SafeWebView.onConsoleMessage(): " + msg, KeyValue.v);

			return true;
		}
	}
}