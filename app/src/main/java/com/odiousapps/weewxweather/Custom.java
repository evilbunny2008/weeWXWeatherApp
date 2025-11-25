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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

@SuppressWarnings("deprecation")
public class Custom extends Fragment
{
	private SafeWebView wv;
	private SwipeRefreshLayout swipeLayout;
	private final ViewTreeObserver.OnScrollChangedListener scl = () -> swipeLayout.setEnabled(wv.getScrollY() == 0);

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
		});

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		weeWXAppCommon.LogMessage("Custom.onViewCreated()");
		super.onViewCreated(view, savedInstanceState);

		if(wv == null)
			wv = WebViewPreloader.getInstance().getWebView(requireContext());

		if(wv.getParent() != null)
			((ViewGroup)wv.getParent()).removeView(wv);

		FrameLayout fl = view.findViewById(R.id.custom);
		fl.removeAllViews();
		fl.addView(wv);

		if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
		{
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2)
			{
				if(WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
				{
					try
					{
						WebSettingsCompat.setAlgorithmicDarkeningAllowed(wv.getSettings(), true);
					} catch(Exception e) {
						weeWXAppCommon.doStackOutput(e);
					}
				}
			} else {
				if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY))
					WebSettingsCompat.setForceDarkStrategy(wv.getSettings(),
							WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY);

				if(WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
					WebSettingsCompat.setAlgorithmicDarkeningAllowed(wv.getSettings(), true);

				int mode = WebSettingsCompat.FORCE_DARK_OFF;

				if(KeyValue.mode == 1)
					mode = WebSettingsCompat.FORCE_DARK_ON;

				WebSettingsCompat.setForceDark(wv.getSettings(), mode);
			}
		}

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

		loadCustom(false);
	}

	@Override
	public void onDestroyView()
	{
		weeWXAppCommon.LogMessage("Custom.onDestroyView()");
		super.onDestroyView();

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

			wv.post(() -> wv.loadDataWithBaseURL("file:///android_res/", tmpStr,
					"text/html", "utf-8", null));
			return;
		}

		if(forced && npwsll[1] > 0)
		{
			wv.post(() -> wv.reload());
			return;
		}

		String custom = weeWXAppCommon.GetStringPref("CUSTOM_URL", weeWXApp.CUSTOM_URL_default);
		String custom_url = weeWXAppCommon.GetStringPref("custom_url", weeWXApp.custom_url_default);

		if((custom == null || custom.isBlank()) && (custom_url == null || custom_url.isBlank()))
		{
			String tmpStr = weeWXApp.current_dialog_html
					.replaceAll("WARNING_BODY", weeWXApp.getAndroidString(R.string.custom_url_not_set_or_blank));

			wv.post(() -> wv.loadDataWithBaseURL("file:///android_res/", tmpStr,
					"text/html", "utf-8", null));
			return;
		}

		if(custom_url != null && !custom_url.isBlank())
			wv.post(() -> wv.loadUrl(custom_url));
		else if(custom != null && !custom.isBlank())
			wv.post(() -> wv.loadUrl(custom));
	}
}