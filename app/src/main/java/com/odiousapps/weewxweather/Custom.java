package com.odiousapps.weewxweather;

import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

@SuppressWarnings("deprecation")
public class Custom extends Fragment
{
	private Common common;
	private WebView wv;
	private SwipeRefreshLayout swipeLayout;

	public Custom()
	{
	}

	Custom(Common common)
	{
		this.common = common;
	}

	public static Custom newInstance(Common common)
	{
		return new Custom(common);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		CommonViewModel commonViewModel =
				new ViewModelProvider(this).get(CommonViewModel.class);

		if(commonViewModel.getCommon() == null)
		{
			commonViewModel.setCommon(common);
		} else {
			common = commonViewModel.getCommon();
			common.reload(common.context);
		}

		View rootView = inflater.inflate(R.layout.fragment_custom, container, false);

		wv = rootView.findViewById(R.id.custom);
		Common.setWebview(wv);

		if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
		{
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2)
			{
				if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
				{
					try
					{
						WebSettingsCompat.setAlgorithmicDarkeningAllowed(wv.getSettings(), true);
					} catch (Exception e) {
						Common.doStackOutput(e);
					}
				}
			} else {
				if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY))
					WebSettingsCompat.setForceDarkStrategy(wv.getSettings(),
							WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY);

				if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
					WebSettingsCompat.setAlgorithmicDarkeningAllowed(wv.getSettings(), true);

				WebSettingsCompat.setForceDark(wv.getSettings(),
						(common.getDayNightTheme() == 1) ? WebSettingsCompat.FORCE_DARK_ON :
						 WebSettingsCompat.FORCE_DARK_OFF);
			}
		}

		swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
		swipeLayout.setRefreshing(true);
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

		wv.setWebViewClient(new WebViewClient()
		{
			@Override
			public void onPageFinished(WebView view, String url)
			{
				super.onPageFinished(view, url);
				swipeLayout.setRefreshing(false);
			}
		});

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

		if(custom.isEmpty() && custom_url.isEmpty())
			return;

		if(!custom_url.isEmpty())
			wv.loadUrl(custom_url);
		else
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