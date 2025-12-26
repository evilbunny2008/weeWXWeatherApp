package com.odiousapps.weewxweather;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unused", "SameParameterValue", "all"})
public class WebViewPreloader
{
	private static WebViewPreloader instance;
	private final Queue<SafeWebView> preloadedWebViews = new LinkedList<>();
	private boolean isRunning = false;

	WebViewPreloader()
	{
	}

	void init(int count)
	{
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
			return;

		try
		{
			Context context = weeWXApp.getInstance();

			if(context == null)
				return;

			for(int i = 0; i < count; i++)
			{
				SafeWebView webView = generateWebView(context);

				synchronized(preloadedWebViews)
				{
					preloadedWebViews.add(webView);
				}
			}
		} catch (Throwable t) {
			weeWXAppCommon.doStackOutput(t);
		}
	}

	static synchronized WebViewPreloader getInstance()
	{
		if(instance == null)
			instance = new WebViewPreloader();

		return instance;
	}

	private SafeWebView generateWebView(Context context)
	{
		try
		{
			return new SafeWebView(context);
		} catch(Throwable t) {
			weeWXAppCommon.doStackOutput(t);
		}

		return null;
	}

	SafeWebView getWebView(Context context)
	{
		try
		{
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
			{
				synchronized(preloadedWebViews)
				{
					SafeWebView wv;

					if(!preloadedWebViews.isEmpty())
					{
						wv = preloadedWebViews.poll();
						if(wv != null)
						{
							wv.initSettings();
							wv.onResume();
							wv.resumeTimers();
							return wv;
						}
					}

					wv = generateWebView(context);
					return wv;
				}
			} else {
				return generateWebView(context);
			}
		} catch(Throwable t) {
			weeWXAppCommon.doStackOutput(t);
		}

		return null;
	}

	void recycleWebView(SafeWebView wv)
	{
		if(wv == null)
			return;

		try
		{
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
			{
				wv.onPause();
				synchronized(preloadedWebViews)
				{
					preloadedWebViews.add(wv);
				}
			} else {
				destroyWebView(wv);
			}
		} catch(Throwable t) {
			weeWXAppCommon.doStackOutput(t);
		}
	}

	void destroyWebView(SafeWebView wv)
	{
		if(wv == null)
			return;

		try
		{
			wv.onPause();
			wv.destroy();
			wv = null;
		} catch (Throwable t) {
			weeWXAppCommon.doStackOutput(t);
		}
	}

	void destroyAll()
	{
		try
		{
			synchronized(preloadedWebViews)
			{
				for(SafeWebView wv : preloadedWebViews)
					destroyWebView(wv);

				preloadedWebViews.clear();
			}
		} catch (Throwable t) {
			weeWXAppCommon.doStackOutput(t);
		}
	}

	static String getHTML(String url)
	{
		Context context = weeWXApp.getInstance();

		if(context == null)
			return null;

		if(instance.isRunning)
			return null;

		instance.isRunning = true;

		CountDownLatch latch = new CountDownLatch(1);
		final String[] htmlHolder = new String[1];
		String html = "";

		try
		{
			// Optionally remove bridge + cleanup on UI thread
			new Handler(Looper.getMainLooper()).post(() ->
			{
				SafeWebView wv = new SafeWebView(context);
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

				wv.setOnPageFinishedListener((view, URL) ->
				{
					weeWXAppCommon.LogMessage("Page has finished loading, just need to wait for JS to finish running now...");
					view.evaluateJavascript(bridge.javascript, null);

					try
					{
						root.removeView(container);
						container.removeAllViews();

						instance.recycleWebView(wv);
					} catch(Exception e) {
						weeWXAppCommon.LogMessage("Error! e: " + e, true, KeyValue.e);
					}
				});

				wv.post(() -> wv.loadUrl(url));
			});
		} catch (Throwable t) {
			weeWXAppCommon.doStackOutput(t);
		}

		try
		{
			// wait on background thread (not UI)
			boolean ok = latch.await(weeWXAppCommon.default_timeout, TimeUnit.MILLISECONDS);
			html = ok ? htmlHolder[0] : null;
		} catch(InterruptedException e) {
			weeWXAppCommon.LogMessage("Error! e: " + e, true, KeyValue.e);
		}

		instance.isRunning = false;

		return html;
	}
}