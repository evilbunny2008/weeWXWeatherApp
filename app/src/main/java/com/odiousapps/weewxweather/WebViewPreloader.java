package com.odiousapps.weewxweather;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.widget.FrameLayout;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unused", "SameParameterValue", "all"})
public class WebViewPreloader
{
	private String private_javascript = """
	document.addEventListener("DOMContentLoaded", function()
	{
		(function()
		{
			try
			{
			if(window.__android_detector_installed)
			{
			    console.log('detector: already installed');
			    return;
			}
			
			console.log('detector: start');
			window.__android_detector_installed = true;
			
			window.__android_pending = 0;
			
			(function(open, send)
			{
				XMLHttpRequest.prototype.open = function()
				{
					this._url = arguments[1];
					return open.apply(this, arguments);
				};
			
				XMLHttpRequest.prototype.send = function()
				{
					window.__android_pending++;
					var xhr = this;
					function done()
					{
						try
						{
							if(!xhr.__android_done)
							{
								xhr.__android_done = true;
								window.__android_pending--;
							}
						}catch(e){}
					}
			
					xhr.addEventListener('load', done);
					xhr.addEventListener('error', done);
					xhr.addEventListener('abort', done);
					xhr.addEventListener('timeout', done);
					return send.apply(this, arguments);
				};
			
			})
			
			(XMLHttpRequest.prototype.open, XMLHttpRequest.prototype.send);
			
			if(window.fetch)
			{
				var _fetch = window.fetch;
				window.fetch = function()
				{
					window.__android_pending++;
					return _fetch.apply(this, arguments)
						.then(function(r){ window.__android_pending--; return r; })
						.catch(function(e){ window.__android_pending--; throw e; });
				};
			}
			
			var lastChange = Date.now();
			var mo = new MutationObserver(function(){ lastChange = Date.now(); });
			mo.observe(document,
			{
			    childList:true, subtree:true, attributes:true, characterData:true
			});
			
			function sendHtml(note)
			{
				try
				{
					var html = document.documentElement ? document.documentElement.outerHTML : '';
					console.log('detector: sending html, len=' + html.length + ', note=' + note);
					if(window.AndroidBridge && window.AndroidBridge.onReady)
						AndroidBridge.onReady(html);
				} catch(e) {
					console.log('detector: send failed ' + e);
				}
			}
			
			var stableMs = 600;
			var pollInterval = 250;
			var maxWait = 15000;
			var minLength = 1024;
			var start = Date.now();
			
			(function check()
			{
				try
				{
					var now = Date.now();
					var pending = window.__android_pending || 0;
					
					var html = "";
					try
					{
						html = document.documentElement ? document.documentElement.outerHTML : "";
					} catch(e) {
						console.log('detector top error:'+e);
					}
					
					var htmlLen = html.length;
					
					// Only send when stable, loaded, and long enough
					if(pending === 0 && (now - lastChange) > stableMs && htmlLen >= minLength)
					{
						console.log('detector: stable & length ok ('+htmlLen+')');
						sendHtml('stable');
						mo.disconnect();
						return;
					}
					
					// timeout → send whatever we have
					if((now - start) > maxWait)
					{
						console.log('detector: timeout, htmlLen='+htmlLen);
						sendHtml('timeout');
						mo.disconnect();
						return;
					}
					
					console.log('detector: waiting… pending=' + pending + ' stable='+(now-lastChange) + ' htmlLen='+htmlLen);
					
				} catch(e) {
					console.log('detector: check err '+e);
				}
			
				setTimeout(check, pollInterval);
			})();
			
			// Time out after DEFAULT_TIMEOUTms unless function finishes sooner...
			setTimeout(function()
			{
				try
				{
					var html = document.documentElement ? document.documentElement.outerHTML : "";
					var htmlLen = html.length;
					if(window.__android_pending===0 && htmlLen >= minLength)
					{
						console.log('detector: fallback triggered (len='+htmlLen+')');
						sendHtml('fallback');
						mo.disconnect();
					}
				} catch(e) {
				console.log('detector top error:'+e);
				}
			}, DEFAULT_TIMEOUT);
			
			} catch(ex) {
				console.log('detector top error:'+ex);
			}
		})();
	});
	""";

	private Queue<SafeWebView> preloadedWebViews = new LinkedList<>();
	private boolean isRunning = false;
	private CountDownLatch latch = new CountDownLatch(1);
	private String[] htmlHolder = new String[1];
	private String html = "";
	private ViewGroup rootView;
	private FrameLayout wvContainer;
	private SafeWebView wv;
	private Handler handler;

	WebViewPreloader()
	{}

	void init(int count)
	{
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
			return;

		try
		{
			for(int i = 0; i < count; i++)
			{
				SafeWebView webView = generateWebView();

				synchronized(preloadedWebViews)
				{
					preloadedWebViews.add(webView);
				}
			}
		} catch (Throwable t) {
			weeWXAppCommon.doStackOutput(t);
		}
	}

	private SafeWebView generateWebView()
	{
		try
		{
			return new SafeWebView(weeWXApp.getInstance().getApplicationContext());
		} catch(Throwable t) {
			weeWXAppCommon.doStackOutput(t);
		}

		return null;
	}

	SafeWebView getWebView()
	{
		try
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

				wv = generateWebView();
				return wv;
			}
/*
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
			{
			} else {
				return generateWebView(context);
			}
 */
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
			wv.onPause();
			synchronized(preloadedWebViews)
			{
				preloadedWebViews.add(wv);
			}
/*
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
			{
			} else {
				destroyWebView(wv);
			}
*/
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

	String getHTML(Context context, String url)
	{
		if(context == null)
			return null;

		if(isRunning)
			return null;

		isRunning = true;

		String javascript = private_javascript.replaceAll("DEFAULT_TIMEOUT", "" + weeWXAppCommon.default_timeout);

		try
		{
			// Optionally remove bridge + cleanup on UI thread
			handler = new Handler(Looper.getMainLooper());
			handler.post(() ->
			{
				wv = new SafeWebView(context);
				wv.onResume();
				wv.resumeTimers();

				wvContainer = new FrameLayout(context);
				wvContainer.setAlpha(0f);
				wvContainer.addView(wv, new FrameLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT));

				rootView = (ViewGroup)((Activity)context).getWindow().getDecorView();
				rootView.addView(wvContainer, new FrameLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT));

				wv.addJavascriptInterface(this, "AndroidBridge");

				wv.setOnPageFinishedListener((view, URL) ->
				{
					weeWXAppCommon.LogMessage("Page has finished loading, just need to wait for JS to finish running now...");
					wv.post(() -> wv.evaluateJavascript("""
							window.addEventListener("load", (event) =>
							{
								console.log("page is fully loaded");
								const txt = document.documentElement.innerHTML;
		                        console.log("HTML returned: " + txt.length + "bytes...");
								if(window.AndroidBridge && window.AndroidBridge.onReady)
									AndroidBridge.onReady(txt);
							});
							""", null
					));
				});

				wv.loadUrl(url);
			});
		} catch (Throwable t) {
			weeWXAppCommon.doStackOutput(t);
		}

		try
		{
			// wait on background thread (not UI)
			boolean ok = latch.await(weeWXAppCommon.default_timeout, TimeUnit.MILLISECONDS);
			html = ok ? htmlHolder[0] : null;

			handler.post(() ->
			{
				rootView.removeView(wvContainer);
				wvContainer.removeAllViews();
				wv.removeJavascriptInterface("AndroidBridge");
				recycleWebView(wv);
			});
		} catch(InterruptedException e) {
			weeWXAppCommon.LogMessage("Error! e: " + e, true, KeyValue.e);
		}

		isRunning = false;

		return html;
	}

	@JavascriptInterface
	public void onReady(String html)
	{
		weeWXAppCommon.LogMessage("Got the following HTML length: " + html.length());

		if(html == null || html.isBlank() || html.length() < 10_000)
			return;

		html = "<html>" + html + "</html>";

		// This runs on the UI thread internally, but be careful with heavy work
		htmlHolder[0] = cleanReturnedHtml(html);

		latch.countDown();
	}

	String cleanReturnedHtml(String jsonEncodedHtml)
	{
		if(jsonEncodedHtml == null || jsonEncodedHtml.isBlank())
			return null;

		String s = jsonEncodedHtml.strip();

		// remove surrounding quotes if present
		if(s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2)
			s = s.substring(1, s.length() - 1);

		// common escape sequences returned by evaluateJavascript
		s = s.replace("\\u003C", "<");
		s = s.replace("\\n", "\n");
		s = s.replace("\\\"", "\"");
		s = s.replace("\\r", "\r");

		return s;
	}

	String escapeJsString(String s)
	{
		if(s == null)
			return "";

		return s.replace("\\", "\\\\")
				.replace("'", "\\'")
				.replace("\"", "\\\"");
	}
}