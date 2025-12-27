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

@SuppressWarnings("deprecation")
public class Custom extends Fragment
{
	private SafeWebView wv;
	private SwipeRefreshLayout swipeLayout;
	private final ViewTreeObserver.OnScrollChangedListener scl = () -> swipeLayout.setEnabled(wv.getScrollY() == 0);
	private static long lastRefresh = 0;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState)
	{
		weeWXAppCommon.LogMessage("Custom.onCreateView()");
		super.onCreateView(inflater, container, savedInstanceState);

		View view = inflater.inflate(R.layout.fragment_custom, container, false);

		swipeLayout = view.findViewById(R.id.swipeToRefresh);
		swipeLayout.post(() -> swipeLayout.setRefreshing(true));
		swipeLayout.setOnRefreshListener(() ->
		{
			swipeLayout.setRefreshing(true);
			weeWXAppCommon.LogMessage("onRefresh();");
			loadCustom(true);
			lastRefresh = weeWXAppCommon.getCurrTime();
		});

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		weeWXAppCommon.LogMessage("Custom.onViewCreated()");
		super.onViewCreated(view, savedInstanceState);

		weeWXAppCommon.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);

		if(wv == null)
			wv = WebViewPreloader.getInstance().getWebView();

		if(wv.getParent() != null)
			((ViewGroup)wv.getParent()).removeView(wv);

		FrameLayout fl = view.findViewById(R.id.custom);
		fl.removeAllViews();
		fl.addView(wv);

		wv.getViewTreeObserver().addOnScrollChangedListener(scl);

		wv.setOnPageFinishedListener((v, url) -> swipeLayout.setRefreshing(false));

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

		setMode();

		loadCustom(false);
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
						weeWXAppCommon.doStackOutput(e);
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
		weeWXAppCommon.LogMessage("Custom.onDestroyView()");
		super.onDestroyView();

		weeWXAppCommon.NotificationManager.getNotificationLiveData().removeObservers(getViewLifecycleOwner());

		if(wv != null)
		{
			ViewParent parent = wv.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(wv);

			wv.getViewTreeObserver().removeOnScrollChangedListener(scl);

			WebViewPreloader.getInstance().recycleWebView(wv);

			weeWXAppCommon.LogMessage("Custom.onDestroyView() recycled wv...");
		}
	}

	private void loadCustom(boolean forced)
	{
		long[] npwsll = weeWXAppCommon.getNPWSLL();
		if(!forced && npwsll[1] <= 0)
		{
			String tmpStr = weeWXApp.current_dialog_html
					.replaceAll("WARNING_BODY", weeWXApp.getAndroidString(R.string.manual_update_set_refresh_screen_to_load));

			wv.post(() -> wv.loadDataWithBaseURL(null, tmpStr,
					"text/html", "utf-8", null));
			return;
		}

		String custom = (String)KeyValue.readVar("CUSTOM_URL", weeWXApp.CUSTOM_URL_default);
		String custom_url = (String)KeyValue.readVar("custom_url", weeWXApp.custom_url_default);

		if((custom == null || custom.isBlank()) && (custom_url == null || custom_url.isBlank()))
		{
			String tmpStr = weeWXApp.current_dialog_html
					.replaceAll("WARNING_BODY", weeWXApp.getAndroidString(R.string.custom_url_not_set_or_blank));

			wv.post(() -> wv.loadDataWithBaseURL(null, tmpStr,
					"text/html", "utf-8", null));
			return;
		}

		if(custom_url != null && !custom_url.isBlank())
			wv.post(() -> wv.loadUrl(custom_url));
		else if(custom != null && !custom.isBlank())
			wv.post(() -> wv.loadUrl(custom));
	}

	private final Observer<String> notificationObserver = str ->
	{
		weeWXAppCommon.LogMessage("Custom.java notificationObserver: " + str);

		if(str.equals(weeWXAppCommon.REFRESH_WEATHER_INTENT))
		{
			long currtime = weeWXAppCommon.getCurrTime();

			if(lastRefresh + 30 > currtime)
				return;

			lastRefresh = currtime;

			int pos = (int)KeyValue.readVar("updateInterval", weeWXApp.updateInterval_default);
			if(pos > 0 && KeyValue.isVisible)
				loadCustom(true);
		}

		if(str.equals(weeWXAppCommon.REFRESH_DARKMODE_INTENT))
			setMode();
	};
}