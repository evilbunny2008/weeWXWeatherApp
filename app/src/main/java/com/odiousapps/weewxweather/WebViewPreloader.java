package com.odiousapps.weewxweather;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.widget.FrameLayout;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.odiousapps.weewxweather.weeWXAppCommon.doStackOutput;
import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;
import static com.odiousapps.weewxweather.weeWXAppCommon.is_blank;
import static com.odiousapps.weewxweather.weeWXAppCommon.is_valid_url;

public class WebViewPreloader
{
	private boolean isRunning = false;
	private CountDownLatch latch = null;
	private String[] htmlHolder = null;
	private ViewGroup rootView;
	private FrameLayout wvContainer;
	private Handler handler;
	private SafeWebView wv;

	private String[] search_terms = null;
	private String[] search_terms_and_not_have = null;
	private String[] search_terms_or_not_have = null;

	WebViewPreloader()
	{}

	String getHTML(String url, String[] searchTerms, String[] mustNotContain, String[] orNotContain) throws IOException
	{
		LogMessage("WebViewPreloader.getHTML()...");

		if(!is_valid_url(url))
		{
			LogMessage("WebViewPreloader.getHTML() !is_valid_url(url), skipping...", KeyValue.e);
			return null;
		}

		if(isRunning)
		{
			LogMessage("WebViewPreloader.getHTML() Already running, skipping...", KeyValue.w);
			return null;
		}

		isRunning = true;

		search_terms = searchTerms;
		search_terms_and_not_have = mustNotContain;
		search_terms_or_not_have = orNotContain;

		latch = new CountDownLatch(1);
		htmlHolder = new String[1];

		try
		{
			handler = new Handler(Looper.getMainLooper());
			handler.post(() ->
			{
				if(wvContainer == null)
				{
					LogMessage("WebViewPreloader.getHTML() Creating wvContainer...");
					wvContainer = new FrameLayout(weeWXApp.getInstance());
					wvContainer.setAlpha(0f);
				}

				if(rootView == null)
				{
					LogMessage("WebViewPreloader.getHTML() Getting rootView...");
					Activity mainActivity = MainActivity.getInstance();
					if(mainActivity == null)
						return;

					rootView = (ViewGroup)(mainActivity).getWindow().getDecorView();
					rootView.addView(wvContainer, new FrameLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.MATCH_PARENT));
				}

				if(wv == null)
				{
					wv = new SafeWebView(weeWXApp.getInstance());
					LogMessage("WebViewPreloader.getHTML() Adding final_wv to wvContainer...");
					wvContainer.addView(wv, new FrameLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT));

					wv.addJavascriptInterface(this, "AndroidBridge");
				}

				long delay = 0;
				int wait = 10_000;
				for(int attempt = 1; attempt <= 10; attempt++)
				{
					int thisAttempt = attempt;
					long doattempt = Math.round(delay + wait * attempt);
					float doattempt_in_sec = Math.round(doattempt / 100.0f) / 10.0f;

					LogMessage("Setting a check for " + doattempt_in_sec + "s time...");
					handler.postDelayed(() -> wv.evaluateJavascript("""
						(function()
						{
							console.log("message=Attempt #ATTEMPT_NUM, Time: " + new Date().toLocaleTimeString());
							if(document == null)
								console.log("message=document == null");
							else if(document.readyState == null)
								console.log("message=document.readyState == null");
							else if(document.documentElement == null)
								console.log("message=document.documentElement == null");
							else
								AndroidBridge.injectHTML(ATTEMPT_NUM, document.readyState, document.documentElement.outerHTML);

							return;
						})();
						""".replace("ATTEMPT_NUM", "" + thisAttempt), null), doattempt
					);
				}

				LogMessage("WebViewPreloader.getHTML() wv.loadUrl(" + url + ")");
				wv.loadUrl(url);
			});
		} catch(Throwable t) {
			doStackOutput(t);
		}

		String html = null;

		try
		{
			LogMessage("WebViewPreloader.getHTML() Wait on background thread for HTML data");
			boolean ok = latch.await(weeWXAppCommon.default_webview_timeout, TimeUnit.MILLISECONDS);
			html = ok ? htmlHolder[0] : null;
		} catch(InterruptedException e) {
			LogMessage("WebViewPreloader.getHTML() Error! e: " + e, true, KeyValue.e);
		}

		wv.post(() -> wv.loadUrl("about:blank"));

		isRunning = false;

		if(html != null && html.startsWith("error|"))
		{
			String[] error = html.strip().split("\\|", 2);
			if(error[0].equalsIgnoreCase("error"))
			{
				deleteWV();
				if(!is_blank(error[1]))
					throw new IOException(error[1]);

				throw new IOException("An error occurred but it's not clear what");
			}
		}

		return html;
	}

	void deleteWV()
	{
		if(rootView != null && wvContainer != null && wv != null)
		{
			rootView.post(() ->
			{
				if(wv != null)
				{
					wv.stopLoading();
					wv.removeJavascriptInterface("AndroidBridge");
					wv.destroy();
				}

				if(wvContainer != null)
					wvContainer.removeAllViews();

				if(rootView != null && wvContainer != null)
					rootView.removeView(wvContainer);

				rootView = null;
				wvContainer = null;
				wv = null;
			});
		} else if(wvContainer != null && wv != null) {
			wvContainer.post(() ->
			{
				if(wv != null)
				{
					wv.removeJavascriptInterface("AndroidBridge");
					wv.destroy();
				}

				if(wvContainer != null)
					wvContainer.removeAllViews();

				wvContainer = null;
				wv = null;
			});
		} else if(rootView != null && wvContainer != null) {
			rootView.post(() ->
			{
				wvContainer.removeAllViews();
				rootView.removeView(wvContainer);
				rootView = null;
				wvContainer = null;
			});
		} else if(rootView != null && wv != null) {
			wv.post(() ->
			{
				wv.stopLoading();
				wv.removeJavascriptInterface("AndroidBridge");
				wv.destroy();
				wv = null;
				rootView = null;
			});
		} else if(rootView != null) {
			rootView = null;
		} else if(wvContainer != null) {
			wvContainer.post(() ->
			{
				wvContainer.removeAllViews();
				wvContainer = null;
			});
		} else if(wv != null) {
			wv.post(() ->
			{
				wv.stopLoading();
				wv.removeJavascriptInterface("AndroidBridge");
				wv.destroy();
				wv = null;
			});
		}
	}

	@JavascriptInterface
	public synchronized void injectHTML(int attempt, String docstate, String html)
	{
		if(latch.getCount() == 0)
			return;

		if(html != null)
			LogMessage("injectHTML() Got the following Attempt: #" + attempt + ", docstate: " +
								  docstate + ", HTML length: " + html.length());
		else
			LogMessage("injectHTML() Attempt: #" + attempt + ", html == null...");

		if((html == null || html.length() < 512) && attempt < 10)
			return;

		boolean hasST = search_terms != null && search_terms.length > 0;
		boolean andHasNT = search_terms_and_not_have != null && search_terms_and_not_have.length > 0;
		boolean orHasNT = search_terms_or_not_have != null && search_terms_or_not_have.length > 0;

		if(attempt < 10 && (hasST || andHasNT || orHasNT))
		{
			Element body = Jsoup.parse(html).body();

//			LogMessage("injectHTML() Writting to WZtest_body_before_" + nowTS + ".html");
//			CustomDebug.writeDebug("weeWX", "WZtest_body_before_" + nowTS + ".html", body.html());

			JsoupHelper.cleanWZDoc(body, false);
			if(!body.hasText())
			{
				LogMessage("injectHTML() Body has no text... body: " + body);
				return;
			}

//			LogMessage("injectHTML() Writting to WZtest_body_after_" + nowTS + ".html");
//			CustomDebug.writeDebug("weeWX", "WZtest_body_after_" + nowTS + ".html", body.html());

			String text = JsoupHelper.normaliseText(body.text());
			if(is_blank(text))
				return;

			if(andHasNT)
			{
				boolean found_all = true;
				for(String st : search_terms_and_not_have)
				{
					if(!(" " + text + " ").contains(" " + st + " "))
					{
						found_all = false;
						break;
					}
				}

				if(found_all)
				{
					LogMessage("injectHTML() All not search terms found: " +
											  String.join(", ", search_terms_and_not_have));
					LogMessage("injectHTML() text: " + text);
					htmlHolder[0] = "error|All bad search terms found, will retry later...";
					handler.removeCallbacksAndMessages(null);
					latch.countDown();
					return;
				}
			}

			if(orHasNT)
			{
				boolean found_any = false;
				for(String st : search_terms_or_not_have)
				{
					if((" " + text + " ").contains(" " + st + " "))
					{
						found_any = true;
						break;
					}
				}

				if(found_any)
				{
					LogMessage("injectHTML() One or more not search terms found: " +
											  String.join(", ", search_terms_or_not_have));
					LogMessage("injectHTML() text: " + text);
					htmlHolder[0] = "error|At least one bad search term found, will retry later...";
					handler.removeCallbacksAndMessages(null);
					latch.countDown();
					return;
				}
			}

			if(hasST)
			{
				String stNotFound = "";
				boolean found_all = true;
				for(String st : search_terms)
				{
					if(!(" " + text + " ").contains(" " + st + " "))
					{
						stNotFound = st;
						found_all = false;
						break;
					}
				}

				if(found_all)
				{
					LogMessage("injectHTML() All search terms found, now we can move on...");
					htmlHolder[0] = body.outerHtml().strip();
					handler.removeCallbacksAndMessages(null);
					latch.countDown();
					return;
				}

				LogMessage("injectHTML() Not all search terms found, missing: " + stNotFound + ", list: " +
										  String.join(", ", search_terms));
				//LogMessage("injectHTML() text: " + text);
			}

			return;
		}

		htmlHolder[0] = html;

		if((docstate.equals("complete") && html != null && html.length() > 10_000) || attempt >= 10)
		{
			LogMessage("injectHTML() We seem to have gotten html, cancelling the rest of the checks...");
			handler.removeCallbacksAndMessages(null);
			latch.countDown();
		}
	}
}