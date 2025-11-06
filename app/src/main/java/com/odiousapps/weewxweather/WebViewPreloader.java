package com.odiousapps.weewxweather;

import android.content.Context;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.util.LinkedList;
import java.util.Queue;

@SuppressWarnings({"unused", "SetJavaScriptEnabled", "SameParameterValue"})
public class WebViewPreloader
{
	private static WebViewPreloader instance;
	private final Queue<WebView> preloadedWebViews = new LinkedList<>();

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

	WebView generateWebView(Context context)
	{
		WebView webView = new WebView(context.getApplicationContext());
		setWebview(webView);
		return webView;
	}

	WebView getWebView(Context context)
	{
		if(preloadedWebViews.isEmpty())
			return generateWebView(context);

		synchronized(preloadedWebViews)
		{
			return preloadedWebViews.poll();
		}
	}

	void recycleWebView(WebView webView)
	{
		if(webView == null)
			return;

		wipeCache(webView);
		setWebview(webView);

		synchronized(preloadedWebViews)
		{
			preloadedWebViews.add(webView);
		}
	}

	void destroyAll()
	{
		synchronized(preloadedWebViews)
		{
			for(WebView webView : preloadedWebViews)
				webView.destroy();

			preloadedWebViews.clear();
		}
	}

	private void setWebview(WebView wv)
	{
		wv.loadUrl("about:blank");
		wv.stopLoading();
		wv.removeAllViews();
		wv.getSettings().setUserAgentString(Common.UA);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.setOverScrollMode(View.OVER_SCROLL_NEVER);
		wv.setNestedScrollingEnabled(true);
		wv.clearCache(true);
		wv.clearHistory();
		wv.clearFormData();
		wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		wv.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
		wv.getSettings().setLoadWithOverviewMode(true);
		wv.getSettings().setUseWideViewPort(true);
		//wv.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
		wv.setScrollContainer(true);
		wv.getSettings().setDisplayZoomControls(false);
		wv.getSettings().setBuiltInZoomControls(false);
		wv.setVerticalScrollBarEnabled(false);
		wv.setHorizontalScrollBarEnabled(false);
		wv.getSettings().setLoadWithOverviewMode(true);
		wv.getSettings().setUseWideViewPort(true);
		//wv.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		wv.setWebChromeClient(new myWebChromeClient());
	}

	static void wipeCache(WebView webView)
	{
		webView.clearFormData();
		webView.clearHistory();
		webView.clearCache(true);
	}

	static final class myWebChromeClient extends WebChromeClient
	{
		@Override
		public boolean onConsoleMessage(ConsoleMessage cm)
		{
			return true;
		}
	}
}