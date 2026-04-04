package com.odiousapps.weewxweather;

import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;


import static com.odiousapps.weewxweather.weeWXApp.getAndroidString;
import static com.odiousapps.weewxweather.weeWXAppCommon.doStackOutput;
import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;
import static com.odiousapps.weewxweather.weeWXAppCommon.getNPWSLL;

@SuppressWarnings("deprecation")
public class Custom extends Fragment
{
	private SafeWebView wv;
	private SwipeRefreshLayout swipeLayout;
	private final ViewTreeObserver.OnScrollChangedListener scl = () -> swipeLayout.setEnabled(wv.getScrollY() == 0);
	private long lastRefresh = 0;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		LogMessage("Custom.onCreateView()");
		super.onCreateView(inflater, container, savedInstanceState);

		View view = inflater.inflate(R.layout.fragment_custom, container, false);

		swipeLayout = view.findViewById(R.id.swipeToRefresh);
		swipeLayout.post(() -> swipeLayout.setRefreshing(true));
		swipeLayout.setOnRefreshListener(() ->
		{
			swipeLayout.setRefreshing(true);
			LogMessage("onRefresh();");
			loadCustom(true);
			lastRefresh = System.currentTimeMillis();
		});

		if(wv == null)
			wv = new SafeWebView(weeWXApp.getInstance());

		wv.setDebugLogging(true);

		FrameLayout fl = view.findViewById(R.id.custom);
		fl.removeAllViews();
		fl.addView(wv);

		wv.getViewTreeObserver().addOnScrollChangedListener(scl);

		wv.setOnCustomPageFinishedListener((v, url) -> swipeLayout.setRefreshing(false), true);

		wv.setOnKeyListener((v, keyCode, event) ->
		{
			if(wv != null && wv.canGoBack() && event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK)
			{
				wv.goBack();
				return true;
			}

			return false;
		});

		setMode();

		loadCustom(false);

		weeWXAppCommon.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);

		return view;
	}

	private void setMode()
	{
		boolean fdm = (boolean)KeyValue.readVar("force_dark_mode", weeWXApp.force_dark_mode_default);
		boolean darkmode = (int)KeyValue.readVar("mode", weeWXApp.mode_default) == 1;

		if(darkmode && !fdm)
			darkmode = false;

		if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK) && fdm)
		{
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2)
			{
				if(WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
				{
					try
					{
						WebSettingsCompat.setAlgorithmicDarkeningAllowed(wv.getSettings(), darkmode);
					} catch(Exception e) {
						doStackOutput(e);
					}
				}
			} else {
				if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY))
					WebSettingsCompat.setForceDarkStrategy(wv.getSettings(),
							WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY);

				if(WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
					WebSettingsCompat.setAlgorithmicDarkeningAllowed(wv.getSettings(), darkmode);

				int mode = WebSettingsCompat.FORCE_DARK_OFF;
				if(darkmode)
					mode = WebSettingsCompat.FORCE_DARK_ON;

				WebSettingsCompat.setForceDark(wv.getSettings(), mode);
			}
		}
	}

	@Override
	public void onDestroyView()
	{
		LogMessage("Custom.onDestroyView()");
		super.onDestroyView();

		weeWXAppCommon.NotificationManager.getNotificationLiveData().removeObservers(getViewLifecycleOwner());

		if(wv != null)
		{
			ViewParent parent = wv.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(wv);

			wv.getViewTreeObserver().removeOnScrollChangedListener(scl);

			wv.destroy();

			wv = null;

			LogMessage("Custom.onDestroyView() wv destroyed...");
		}
	}

	private void loadCustom(boolean forced)
	{
		weeWXAppCommon.NPWSLL npwsll = getNPWSLL();
		if(!forced && npwsll.periodTime() <= 0)
		{
			String tmpStr = weeWXApp.current_dialog_html
					.replace("WARNING_BODY", getAndroidString(R.string.manual_update_set_refresh_screen_to_load));

			wv.post(() -> wv.loadDataWithBaseURL(null, tmpStr,
					"text/html", "utf-8", null));
			return;
		}

		String custom = (String)KeyValue.readVar("CUSTOM_URL", "");
		String custom_url = (String)KeyValue.readVar("custom_url", "");

		LogMessage("loadCustom() custom: " + custom);
		LogMessage("loadCustom() custom_url: " + custom_url);

		if((custom == null || custom.isBlank()) && (custom_url == null || custom_url.isBlank()))
		{
			String tmpStr = weeWXApp.current_dialog_html
					.replace("WARNING_BODY", getAndroidString(R.string.custom_url_not_set_or_blank));

			wv.post(() -> wv.loadDataWithBaseURL(null, tmpStr,
					"text/html", "utf-8", null));
			return;
		}

		wv.stopLoading();

		if(custom_url != null && !custom_url.isBlank())
			wv.post(() -> wv.loadUrl(custom_url));
		else if(custom != null && !custom.isBlank())
			wv.post(() -> wv.loadUrl(custom));
	}

	private final Observer<String> notificationObserver = str ->
	{
		LogMessage("Custom.java notificationObserver: " + str);

		if(str.equals(weeWXAppCommon.REFRESH_WEATHER_INTENT))
		{
			long now = System.currentTimeMillis();

			if((now - lastRefresh) / 1000 < 30)
				return;

			lastRefresh = now;

			int pos = (int)KeyValue.readVar("UpdateFrequency", weeWXApp.UpdateFrequency_default);
			if(pos > 0 && KeyValue.isVisible)
				loadCustom(true);
		}

		if(str.equals(weeWXAppCommon.REFRESH_DARKMODE_INTENT))
			setMode();
	};
}