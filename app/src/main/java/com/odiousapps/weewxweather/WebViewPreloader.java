package com.odiousapps.weewxweather;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unused", "SetJavaScriptEnabled", "SameParameterValue", "all"})
public class WebViewPreloader
{
	private static WebViewPreloader instance;
	private final Queue<WebView> preloadedWebViews = new LinkedList<>();
	private boolean isRunning = false;

	WebViewPreloader()
	{
	}

	void init(Context context, int count)
	{
		for(int i = 0; i < count; i++)
		{
			WebView webView = generateWebView(context);

			synchronized(preloadedWebViews)
			{
				preloadedWebViews.add(webView);
			}
		}
	}

	static synchronized WebViewPreloader getInstance()
	{
		if(instance == null)
			instance = new WebViewPreloader();

		return instance;
	}

	private WebView generateWebView(Context context)
	{
		WebView wv = new WebView(context);
		return wv;
	}

	WebView getWebView(Context context)
	{
		synchronized(preloadedWebViews)
		{
			WebView wv;

			if(!preloadedWebViews.isEmpty())
			{
				wv = preloadedWebViews.poll();
				if(wv != null)
				{
					setWebview(wv);
					wv.onResume();
					wv.resumeTimers();
					return wv;
				}
			}

			wv = generateWebView(context);
			setWebview(wv);
			return wv;
		}
	}

	void recycleWebView(WebView wv)
	{
		if(wv == null)
			return;

		wv.loadData("", "text/html", "utf-8");
		wv.stopLoading();
		wv.pauseTimers();
		wv.onPause();
		wv.removeAllViews();
		wv.clearCache(false);
		wv.clearHistory();
		wv.clearFormData();
		wv.removeJavascriptInterface("AndroidBridge");

		ViewGroup parent = (ViewGroup)wv.getParent();
		if(parent != null)
			parent.removeView(wv);

		synchronized(preloadedWebViews)
		{
			preloadedWebViews.add(wv);
		}
	}

	void destroyWebView(WebView wv)
	{
		if(wv == null)
			return;

		wv.loadData("", "text/html", "utf-8");
		wv.stopLoading();
		wv.pauseTimers();
		wv.onPause();
		wv.removeAllViews();
		wv.clearCache(false);
		wv.clearHistory();
		wv.clearFormData();
		wv.removeJavascriptInterface("AndroidBridge");

		ViewGroup parent = (ViewGroup)wv.getParent();
		if(parent != null)
			parent.removeView(wv);
		wv.destroy();
		wv = null;
	}

	void destroyAll()
	{
		synchronized(preloadedWebViews)
		{
			for(WebView wv : preloadedWebViews)
				wv.destroy();

			preloadedWebViews.clear();
		}
	}

	private static void setWebview(WebView wv)
	{
		wv.loadData("", "text/html", "utf-8");
		wv.getSettings().setUserAgentString(weeWXAppCommon.UA);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.getSettings().setDomStorageEnabled(true);
		wv.getSettings().setLoadsImagesAutomatically(true);
		wv.getSettings().setAllowFileAccess(true);
		wv.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
		wv.getSettings().setLoadWithOverviewMode(true);
		wv.getSettings().setUseWideViewPort(true);
		wv.getSettings().setDisplayZoomControls(false);
		wv.getSettings().setBuiltInZoomControls(false);

		wv.setOverScrollMode(View.OVER_SCROLL_NEVER);
		wv.setNestedScrollingEnabled(true);
		wv.setVerticalScrollBarEnabled(false);
		wv.setHorizontalScrollBarEnabled(false);
		wv.setWebChromeClient(new myWebChromeClient());
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

	static String getHTML(Context context, String url, int timeoutMs)
	{
		if(instance.isRunning)
			return null;

		instance.isRunning = true;

		CountDownLatch latch = new CountDownLatch(1);
		final String[] htmlHolder = new String[1];
		String html = "";

		// Optionally remove bridge + cleanup on UI thread
		new Handler(Looper.getMainLooper()).post(() ->
		{
			WebView wv = new WebView(context);
			setWebview(wv);
			wv.onResume();
			wv.resumeTimers();

			FrameLayout container = new FrameLayout(context);
			container.setAlpha(0f);
			container.addView(wv, new FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));

			ViewGroup root = (ViewGroup)((Activity)context).getWindow().getDecorView();
			root.addView(container, new FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT));

			JsBridge bridge = new JsBridge(latch, htmlHolder, wv);
			wv.addJavascriptInterface(bridge, "AndroidBridge");

			wv.setWebViewClient(new WebViewClient()
			{
				@Override
				public void onPageFinished(WebView view, String url)
				{
					weeWXAppCommon.LogMessage("Page has finished loading, just need to wait for JS to finish running now...");
					view.evaluateJavascript(bridge.javascript, null);

					try
					{
						root.removeView(container);
						container.removeAllViews();

						//instance.destroyWebView(wv);
					} catch(Exception ignored) {}
				}
			});

			wv.post(() -> wv.loadUrl(url));
		});

		try
		{
			// wait on background thread (not UI)
			boolean ok = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
			html = ok ? htmlHolder[0] : null;
		} catch(InterruptedException ignored) {}

		instance.isRunning = false;

		return html;
	}
}