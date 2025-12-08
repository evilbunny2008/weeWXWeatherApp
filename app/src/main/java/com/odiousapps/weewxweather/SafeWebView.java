package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressWarnings("unused")
public class SafeWebView extends WebView
{
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

	/** Initialize SafeWebView safely with common settings. */
	@SuppressLint("SetJavaScriptEnabled")
	void initSettings()
	{
		try
		{
			// Always safe to create on API 24+
			loadData("", "text/html", "utf-8");
			WebSettings ws = getSettings();
			ws.setJavaScriptEnabled(true);
			ws.setDomStorageEnabled(true);
			ws.setLoadsImagesAutomatically(true);
			ws.setAllowContentAccess(true);
			ws.setAllowFileAccess(true);

			ws.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
			ws.setLoadWithOverviewMode(true);
			ws.setUseWideViewPort(true);
			ws.setDisplayZoomControls(false);
			ws.setBuiltInZoomControls(false);

			setLayerType(View.LAYER_TYPE_SOFTWARE, null);

			setOverScrollMode(View.OVER_SCROLL_NEVER);
			setNestedScrollingEnabled(true);
			setVerticalScrollBarEnabled(false);
			setHorizontalScrollBarEnabled(false);
			setWebChromeClient(new myWebChromeClient());

			// Optional: apply user agent
			ws.setUserAgentString(NetworkClient.UA);
		} catch(Throwable t) {
			weeWXAppCommon.doStackOutput(t);
		}
	}

	public void onPause()
	{
		try
		{
			loadData("", "text/html", "utf-8");
			stopLoading();
			pauseTimers();

			removeAllViews();
			clearCache(false);
			clearHistory();
			clearFormData();
			removeJavascriptInterface("AndroidBridge");

			ViewGroup parent = (ViewGroup)getParent();
			if(parent != null)
				parent.removeView(this);
		} catch(Throwable t) {
			weeWXAppCommon.doStackOutput(t);
		}

		super.onPause();
	}

	@Override
	public void loadUrl(@NonNull String url)
	{
		// Example: log URLs
		Log.d("SafeWebView", "Loading URL: " + url);
		super.loadUrl(url);
	}

	public void setOnPageFinishedListener(final OnPageFinishedListener listener)
	{
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
		{
			setWebViewClient(new WebViewClient()
			{
				@Override
				public void onPageFinished(WebView view, String url)
				{
					super.onPageFinished(view, url);
					listener.onPageFinished(SafeWebView.this, url);
				}
/*
				@Override
				public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request)
				{
					String url = request.getUrl().toString().strip();
					if(url.isBlank())
						return null;

					OkHttpClient okHttpClient = NetworkClient.getInstance(url);
					Request okHttprequest = NetworkClient.getRequest(false, url);

					if(okHttprequest == null)
						return null;

					// Use your OkHttpClient with Conscrypt + custom CAs
					try(Response response = okHttpClient.newCall(okHttprequest).execute())
					{
						byte[] bytes = response.body().bytes(); // fully read body
						ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

						String contentType = response.header("Content-Type", "text/html");
						String mime = "text/html";
						String encoding = "utf-8"; // adjust based on server response

						if(contentType != null)
						{
							String[] parts = contentType.split(";");
							mime = parts[0].trim();

							if(parts.length > 1)
							{
								for(int i = 1; i < parts.length; i++)
								{
									String part = parts[i].trim();
									if(part.startsWith("charset="))
										encoding = part.substring("charset=".length());
								}
							}
						}

						return new WebResourceResponse(mime, encoding, bais);
					} catch(UnknownHostException ignored) {
					} catch(Exception e) {
						weeWXAppCommon.LogMessage("Error! e: " + e, true);
						weeWXAppCommon.doStackOutput(e);
					}

					return null;
				}
*/
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
			weeWXAppCommon.LogMessage("ConsoleMessage: " + cm.message());
			return true;
		}
	}
}