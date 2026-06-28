package com.odiousapps.weewxweather;

import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;

import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;
import static com.odiousapps.weewxweather.weeWXAppCommon.is_blank;

class myWebChromeClient extends WebChromeClient
{
	@Override
	public boolean onConsoleMessage(ConsoleMessage cm)
	{
		String msg = cm.message().strip();
		if(is_blank(msg) || msg.contains("has been blocked by CORS policy") ||
				msg.contains("Cannot read properties of null") ||
				msg.contains("isolines Error loading/rendering isolines Error: Failed to fetch") ||
				msg.contains("was preloaded using link preload but not used within a few seconds") ||
				msg.contains("Unexpected end of JSON inpu"))
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
