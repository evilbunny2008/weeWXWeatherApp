package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class Custom extends Fragment
{
	private final Common common;
	private WebView wv;
	private SwipeRefreshLayout swipeLayout;

	Custom(Common common)
	{
		this.common = common;
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.fragment_custom, container, false);
		wv = rootView.findViewById(R.id.custom);
		wv.getSettings().setUserAgentString(Common.UA);
		wv.getSettings().setJavaScriptEnabled(true);

		WebSettings settings = wv.getSettings();
		settings.setDomStorageEnabled(true);

		swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
		swipeLayout.setOnRefreshListener(() ->
		{
			Common.LogMessage("wv.getScrollY() == " + wv.getScrollY());
			swipeLayout.setRefreshing(true);
			Common.LogMessage("onRefresh();");
			reloadWebView();
			swipeLayout.setRefreshing(false);
		});

		wv.getViewTreeObserver().addOnScrollChangedListener(() ->
		{
			Common.LogMessage("wv.getScrollY() == " + wv.getScrollY());
			swipeLayout.setEnabled(wv.getScrollY() == 0);
		});
		wv.setWebViewClient(new WebViewClient());

		wv.setOnKeyListener((v, keyCode, event) ->
		{
			if(event.getAction() == KeyEvent.ACTION_DOWN)
			{
				if((keyCode == KeyEvent.KEYCODE_BACK))
				{
					if(wv != null)
					{
						if(wv.canGoBack())
						{
							wv.goBack();
							return true;
						}
					}
				}
			}

			return false;
		});

		wv.setWebChromeClient(new WebChromeClient()
		{
			@Override
			public boolean onConsoleMessage(ConsoleMessage cm)
			{
				Common.LogMessage("My Application: " + cm.message());
				return super.onConsoleMessage(cm);
			}
		});

		reloadWebView();
		return rootView;
	}

	private void reloadWebView()
	{
		Common.LogMessage("reload custom...");

		String custom = common.GetStringPref("CUSTOM_URL", "");
		String custom_url = common.GetStringPref("custom_url", "");

		if (Common.isEmpty(custom) && Common.isEmpty(custom_url))
			return;

		if(!Common.isEmpty(custom_url))
			custom = custom_url;

		wv.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
		wv.loadUrl(custom);
	}

	public void onResume()
	{
		super.onResume();
		Common.LogMessage("custom.java -- registerReceiver");
	}

	public void onPause()
	{
		super.onPause();
		Common.LogMessage("custom.java -- unregisterReceiver");
	}
}