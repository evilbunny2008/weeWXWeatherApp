package com.odiousapps.weewxweather;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textview.MaterialTextView;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

@SuppressWarnings({"unused", "deprecation"})
public class Weather extends Fragment implements View.OnClickListener
{
	private boolean isVisible;
	private String lastURL;
	private SafeWebView current, forecast;
	private SwipeRefreshLayout swipeLayout;
	private MaterialCheckBox floatingCheckBox;
	private LinearLayout fll;
	private MainActivity activity;
	private MaterialTextView tv1, tv2;

	private final ViewTreeObserver.OnScrollChangedListener scl = () -> swipeLayout.setEnabled(current.getScrollY() == 0);

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

	private long lastRunForecast, lastRunRadar; //, lastRunWeather;

	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		weeWXAppCommon.LogMessage("Weather.onCreateView()");

		int bgColour = weeWXApp.getColours().bgColour;
		int fgColour = weeWXApp.getColours().fgColour;

		activity = (MainActivity)getActivity();

		View rootView = inflater.inflate(R.layout.fragment_weather, container, false);

		fll = rootView.findViewById(R.id.floating_linear_layout);
		LinearLayout currentll = rootView.findViewById(R.id.currentLinearLayout);
		currentll.setBackgroundColor(bgColour);

		tv1 = rootView.findViewById(R.id.textView);
		tv2 = rootView.findViewById(R.id.textView2);

		tv1.setTextColor(fgColour);
		tv2.setTextColor(fgColour);

		swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
		swipeLayout.setBackgroundColor(bgColour);
		fll.setBackgroundColor(bgColour);

		swipeLayout.setOnRefreshListener(() ->
		{
			swipeLayout.setRefreshing(true);
			weeWXAppCommon.LogMessage("onRefresh()");
			forceRefresh();
		});

		floatingCheckBox = rootView.findViewById(R.id.floatingCheckBoxMain);
		floatingCheckBox.setOnClickListener(this);

		boolean disableSwipeOnRadar = (boolean)KeyValue.readVar("disableSwipeOnRadar", weeWXApp.disableSwipeOnRadar_default);
		floatingCheckBox.setChecked(disableSwipeOnRadar);

		return rootView;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		weeWXAppCommon.LogMessage("Weather.onViewCreated()");
		super.onViewCreated(view, savedInstanceState);

		swipeLayout.setRefreshing(true);

		weeWXAppCommon.LogMessage("Weather.onViewCreated()-- adding notification manager...");
		weeWXAppCommon.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);

		if(current == null)
			current = loadWebview(null, view, R.id.current, true, true);
		else
			loadWebview(current, view, R.id.current, true, true);

		if(current != null)
			weeWXAppCommon.LogMessage("current.getHeight(): " + current.getHeight());

		if(forecast == null)
			forecast = loadWebview(null, view, R.id.forecast, false, false);
		else
			loadWebview(forecast, view, R.id.forecast, false, false);

		if((boolean)KeyValue.readVar("radarforecast_isset", false))
		{
			weeWXAppCommon.LogMessage("Weather.onViewCreated() radarforecast_isset == true");
			boolean radarforecast = (boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default);
			weeWXAppCommon.LogMessage("Weather.onViewCreated() radarforecast: " + radarforecast);

			if(radarforecast == weeWXApp.RadarOnHomeScreen)
				drawRadar();
			else
				reloadForecast();

			drawWeather();

			//loadWebView();
		} else {
			weeWXAppCommon.LogMessage("Weather.onViewCreated() radarforecast_isset == false");
		}
	}

	@Override
	public void onDestroyView()
	{
		weeWXAppCommon.LogMessage("Weather.onDestroyView()");
		super.onDestroyView();

		weeWXAppCommon.NotificationManager.getNotificationLiveData().removeObservers(getViewLifecycleOwner());

		if(current != null)
		{
			ViewParent parent = current.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(current);

			current.getViewTreeObserver().removeOnScrollChangedListener(scl);

			weeWXApp.getInstance().wvpl.recycleWebView(current);

			weeWXAppCommon.LogMessage("Weather.onDestroyView() recycled current...");
		}

		if(forecast != null)
		{
			ViewParent parent = forecast.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(forecast);

			weeWXApp.getInstance().wvpl.recycleWebView(forecast);

			weeWXAppCommon.LogMessage("Weather.onDestroyView() recycled forecast...");
		}
	}

	private void setMode()
	{
		boolean darkmode = (int)KeyValue.readVar("mode", weeWXApp.mode_default) == 1;
		boolean fdm = (boolean)KeyValue.readVar("force_dark_mode", weeWXApp.force_dark_mode_default);

		if(darkmode && !fdm)
			darkmode = false;

		if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
		{
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2)
			{
				if(WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
				{
					try
					{
						WebSettingsCompat.setAlgorithmicDarkeningAllowed(forecast.getSettings(), darkmode);
					} catch(Exception e) {
						weeWXAppCommon.doStackOutput(e);
					}
				}
			} else {
				if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY))
				{
					WebSettingsCompat.setForceDarkStrategy(forecast.getSettings(),
							WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY);

					if(WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
						WebSettingsCompat.setAlgorithmicDarkeningAllowed(forecast.getSettings(), darkmode);
				}

				int mode = WebSettingsCompat.FORCE_DARK_OFF;
				if(darkmode)
					mode = WebSettingsCompat.FORCE_DARK_ON;

				WebSettingsCompat.setForceDark(forecast.getSettings(), mode);
			}
		}
	}

	private SafeWebView loadWebview(SafeWebView webView, View view, int viewid, boolean doSwipe, boolean isCurrent)
	{
		//boolean dynamicSizing = viewid == R.id.current && weeWXApp.getWidth() < 1100;

		weeWXAppCommon.LogMessage("Weather.java loadWebview() height: " + weeWXApp.getHeight());
		weeWXAppCommon.LogMessage("Weather.java loadWebview() width: " + weeWXApp.getWidth());
		weeWXAppCommon.LogMessage("Weather.java loadWebview() density: " + weeWXApp.getDensity());

		boolean wasNull = webView == null;

		if(wasNull)
		{
			weeWXAppCommon.LogMessage("Weather.java loadWebview() webView == null");
			webView = weeWXApp.getInstance().wvpl.getWebView();
		}

		if(webView.getParent() != null)
			((ViewGroup)webView.getParent()).removeView(webView);

		FrameLayout frameLayout = view.findViewById(viewid);
		frameLayout.removeAllViews();
		frameLayout.addView(webView);

		if(viewid == R.id.current)
			webView.getSettings().setTextZoom(100);

		if(doSwipe)
			webView.getViewTreeObserver().addOnScrollChangedListener(scl);

		webView.setOnPageFinishedListener((v, url) ->
		{
			if(url.strip().equals("data:text/html,"))
				return;

			weeWXAppCommon.LogMessage("url: " + url);

			// if(dynamicSizing && isCurrent)
			if(isCurrent)
			{
				scheduler.schedule(() -> adjustHeight(current), 100, TimeUnit.MILLISECONDS);
				scheduler.schedule(() -> adjustHeight(current), 200, TimeUnit.MILLISECONDS);
				scheduler.schedule(() -> adjustHeight(current), 300, TimeUnit.MILLISECONDS);
//			} else if(viewid == R.id.current) {
//				weeWXAppCommon.LogMessage("dynamicSizing is false...");
			}

			stopRefreshing();
		});

		if(wasNull)
			return webView;

		return null;
	}

	private void adjustHeight(SafeWebView webView)
	{
		if(webView == null || current == null || (current.getHeight() == 0 && swipeLayout.getHeight() == 0))
			return;

		weeWXAppCommon.LogMessage("current.getHeight(): " + current.getHeight());
		weeWXAppCommon.LogMessage("swipeLayout.getHeight(): " + swipeLayout.getHeight());

		if(current.getHeight() == 0 || swipeLayout.getHeight() == 0)
			return;

		webView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
		int heightMH = webView.getMeasuredHeight();

		weeWXAppCommon.LogMessage("webView measured height: " + heightMH);

		float curdensity =  weeWXApp.getDensity();

		int heightPx = Math.round(current.getHeight() / curdensity);

		weeWXAppCommon.LogMessage("heightPx: " + heightPx);

		webView.post(() -> webView.evaluateJavascript("""
					(function()
					{
						return Math.max(
							document.body.scrollHeight,
							document.body.offsetHeight,
							document.documentElement.clientHeight,
							document.documentElement.scrollHeight,
							document.documentElement.offsetHeight);
					})();
					""", value ->
				{
					weeWXAppCommon.LogMessage("value: " + value);

					if(value == null || value.isBlank() || value.equals("null"))
					{
						weeWXAppCommon.LogMessage("current.evaluateJavascript() value is null " +
						                          "or blank or equals 'null'", true, KeyValue.w);
						return;
					}

					//weeWXAppCommon.doStackTrace(0);

					int heightInPx = (int)Float.parseFloat(value.replaceAll("[\"']", ""));
					weeWXAppCommon.LogMessage("From Javascript heightInPx: " + heightInPx);

					if(heightInPx == 0)
					{
						weeWXAppCommon.LogMessage("heightInPx is 0 skipping...");
						return;
					}

//					if((heightInPx > 0 && heightInPx < 200) || heightInPx > 250)
					if(heightInPx > 250)
					{
						weeWXAppCommon.LogMessage("heightInPx is out of bounds reloading and invalidating...");
						current.post(() -> current.reload());
						current.post(() -> current.invalidate());
						return;
					}

					// Post a Runnable to make sure contentHeight is available
					float density =  weeWXApp.getDensity();
					weeWXAppCommon.LogMessage("1DP: " + density);
					int height = Math.round(heightInPx * density);
					weeWXAppCommon.LogMessage("height in DP: " + height);

					int contentHeightPx = tv1.getHeight() + tv2.getHeight() + height;
					weeWXAppCommon.LogMessage("contentHeightPx: " + contentHeightPx);

					ViewGroup.LayoutParams params = swipeLayout.getLayoutParams();

					if(params.height != contentHeightPx)
					{
						weeWXAppCommon.LogMessage("params.height != contentHeightPx");
						params.height = contentHeightPx;
						swipeLayout.post(() -> swipeLayout.setLayoutParams(params));
					}
				}
		));
	}

	private void checkFields(TextView tv, String txt)
	{
		if(!tv.getText().toString().equals(txt))
			tv.post(() -> tv.setText(txt));
	}

	private void drawRadar()
	{
		weeWXAppCommon.LogMessage("drawRadar()");

		long current_time = weeWXAppCommon.getCurrTime();

		if(lastRunRadar + 5 > current_time)
		{
			weeWXAppCommon.LogMessage("We already ran less than 5s ago, skipping...", KeyValue.d);
			stopRefreshing();
			return;
		}

		lastRunRadar = current_time;

		if(!(boolean)KeyValue.readVar("radarforecast_isset", false) ||
		   (boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.RadarOnHomeScreen)
		{
			stopRefreshing();
			return;
		}

		String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
		if(radtype == null || radtype.isBlank())
		{
			weeWXAppCommon.LogMessage("radtype: " + radtype);
			String tmp = String.format(weeWXApp.getAndroidString(R.string.radar_type_is_invalid), radtype);
			loadWebViewContent(tmp);
			stopRefreshing();
			return;
		}

		if(radtype.equals("image"))
		{
			weeWXAppCommon.LogMessage("Update the radar iamge from cache...");
			loadOrReloadRadarImage();
		} else {
			String radarURL = (String)KeyValue.readVar("RADAR_URL", weeWXApp.RADAR_URL_default);
			if(radarURL == null || radarURL.isBlank())
			{
				weeWXAppCommon.LogMessage("radar URL not set or blank: " + radarURL, true, KeyValue.w);
				loadWebViewContent(R.string.radar_url_not_set);
				stopRefreshing();
				return;
			}

			long[] npwsll = weeWXAppCommon.getNPWSLL();
			if(npwsll[1] <= 0)
			{
				weeWXAppCommon.LogMessage("Manual updating set, don't autoload the radar webpage...", true, KeyValue.d);
				loadWebViewContent(R.string.manual_update_set_refresh_screen_to_load);
				stopRefreshing();
				return;
			}

			weeWXAppCommon.LogMessage("Update the radar webview page... url: " + radarURL);
			loadWebViewURL(false, radarURL);
		}
	}

	void drawWeather()
	{
		weeWXAppCommon.LogMessage("drawWeather()");

		String lastDownload = (String)KeyValue.readVar("LastDownload", "");
		if(lastDownload == null || lastDownload.isBlank())
		{
			forceCurrentRefresh(R.string.attempting_to_download_data_txt);
			stopRefreshing();
			return;
		}

		String[] bits = lastDownload.split("\\|");

		weeWXAppCommon.LogMessage("bits.length: " + bits.length, KeyValue.d);

		if(bits.length <= 1)
		{
			weeWXAppCommon.LogMessage("bits.length <= 1", true, KeyValue.e);
			forceCurrentRefresh(R.string.error_occurred_while_attempting_to_update);
			stopRefreshing();
			return;
		}
		
		if(bits.length < 65)
		{
			weeWXAppCommon.LogMessage("bits.length < 65", KeyValue.w);
			forceCurrentRefresh(R.string.error_occurred_while_attempting_to_update);
			stopRefreshing();
			return;
		}

		checkFields(tv1, bits[56]);
		checkFields(tv2, bits[54] + " " + bits[55]);

		final StringBuilder sb = new StringBuilder();
		sb.append("\n<div class='todayCurrent'>\n");
		sb.append("\t<div class='topRowCurrent'>\n");
		sb.append("\t\t<div class='mainTemp'>");
		sb.append(bits[0]).append(bits[60]);
		sb.append("</div>\n");

		sb.append("\t\t<div class='apparentTemp'>AT:<br/>");
		if(bits.length > 203)
			sb.append(bits[203]).append(bits[60]);
		else
			sb.append(weeWXApp.emptyField);
		sb.append("</div>\n\t</div>\n\n");

		sb.append("\t<div class='dataTableCurrent'>\n");

		sb.append("\t\t<div class='dataRowCurrent'>\n");

		sb.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(weeWXAppCommon.fiToSVG("flaticon-windy"))
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(bits[25])
				.append(bits[61])
				.append("</div>\n");
		sb.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(bits[37])
				.append(bits[63])
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(weeWXAppCommon.cssToSVG("wi-barometer"))
				.append("</div>\n");

		sb.append("\t\t</div>\n\t\t<div class='dataRowCurrent'>\n");

		if(bits[27] != null)
			bits[27] = bits[27].strip();
		else
			bits[27] = "";

		if(bits[30] != null)
			bits[30] = bits[30].strip();
		else
			bits[30] = "N/A";

		String dir = bits[30];
		int direction = weeWXAppCommon.getDirection(dir);

		try
		{
			if(!bits[27].isBlank() && !bits[27].equals("N/A"))
				direction = (int)Float.parseFloat(bits[27]);
		} catch(NumberFormatException ignored) {}

		weeWXAppCommon.LogMessage("bits.length: " + bits.length, KeyValue.d);

		if(bits.length >= 293)
		{
			dir = bits[293];
			direction = (int)Float.parseFloat(bits[292]);
		}

		sb.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(weeWXAppCommon.cssToSVG("wi-wind-deg", direction))
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(dir)
				.append("</div>\n");

		sb.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(bits[6])
				.append(bits[64])
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(weeWXAppCommon.cssToSVG("wi-humidity"))
				.append("</div>\n");

		sb.append("\t\t</div>\n\t\t<div class='dataRowCurrent'>\n");

		String rain = bits[20] + bits[62] + " " +
		              weeWXApp.getAndroidString(R.string.since) + " mn";
		if(bits.length > 160 && !bits[160].isBlank())
			rain = bits[158] + bits[62] + " " +
			       weeWXApp.getAndroidString(R.string.since) + " " + bits[160];

		sb.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(weeWXAppCommon.cssToSVG("wi-umbrella"))
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(rain)
				.append("</div>\n");
		sb.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(bits[12])
				.append(bits[60])
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(weeWXAppCommon.cssToSVG("wi-raindrop"))
				.append("</div>\n");

		sb.append("\t\t</div>\n");

		bits[43] = bits[43].strip();

		if(bits[43].contains("N/A"))
			bits[43] = "";

		bits[45] = bits[45].strip();

		if(bits[45].contains("N/A"))
			bits[45] = "";

		if(!bits[43].isBlank() || !bits[45].isBlank())
		{
			sb.append("\t\t<div class='dataRowCurrent'>\n");

			if(!bits[45].isBlank())
			{
				sb.append("\t\t\t<div class='dataCellCurrent left'>")
						.append(weeWXAppCommon.fiToSVG("flaticon-women-sunglasses"))
						.append("</div>\n")
						.append(weeWXApp.currentSpacer)
						.append("\t\t\t<div class='dataCellCurrent left'>")
						.append(bits[45])
						.append(" UVI</div>\n");
			} else {
				sb.append(weeWXApp.emptyField);
			}

			if(!bits[43].isBlank())
			{
				sb.append("\t\t\t<div class='dataCellCurrent right'>")
						.append(bits[43]).append(" W/m²</div>\n")
						.append(weeWXApp.currentSpacer)
						.append("\t\t\t<div class='dataCellCurrent right'>")
						.append(weeWXAppCommon.fiToSVG("flaticon-women-sunglasses"))
						.append("</div>\n");
			} else {
				sb.append(weeWXApp.emptyField);
			}

			sb.append("\t\t</div>\n");
		}

		if(bits.length > 166 && (!bits[161].isBlank() || !bits[166].isBlank()) &&
		   (boolean)KeyValue.readVar("showIndoor", weeWXApp.showIndoor_default))
		{
			sb.append("\t\t<div class='dataRowCurrent'>\n");

			if(!bits[161].isBlank())
			{
				sb.append("\t\t\t<div class='dataCellCurrent left'>")
						.append(weeWXAppCommon.fiToSVG("flaticon-home-page"))
						.append("</div>\n")
						.append(weeWXApp.currentSpacer)
						.append("\t\t\t<div class='dataCellCurrent left'>")
						.append(bits[161])
						.append(bits[60])
						.append("</div>\n");
			} else {
				sb.append(weeWXApp.emptyField);
			}

			if(!bits[166].isBlank())
			{
				sb.append("\t\t\t<div class='dataCellCurrent right'>")
						.append(bits[166])
						.append(bits[64])
						.append("</div>\n")
						.append(weeWXApp.currentSpacer)
						.append("\t\t\t<div class='dataCellCurrent right'>")
						.append(weeWXAppCommon.fiToSVG("flaticon-home-page"))
						.append("</div>\n");
			} else {
				sb.append(weeWXApp.emptyField);
			}

			sb.append("\t\t</div>\n");
		}

		sb.append("\t\t<div class='dataRowCurrent'>\n");

		sb.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(weeWXAppCommon.cssToSVG("wi-sunrise"))
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(bits[57])
				.append("</div>\n");

		sb.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(bits[58])
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(weeWXAppCommon.cssToSVG("wi-sunset"))
				.append("</div>\n");

		sb.append("\t\t</div>\n\t\t<div class='dataRowCurrent'>\n");

		boolean next_moon = (boolean)KeyValue.readVar("next_moon", weeWXApp.next_moon_default);

		if(next_moon && bits.length >= 292 && !bits[209].isBlank() && !bits[291].isBlank())
		{
			sb.append("\t\t\t<div class='dataCellCurrent left'>")
					.append(weeWXAppCommon.cssToSVG("wi-moonrise"))
					.append("</div>\n")
					.append(weeWXApp.currentSpacer)
					.append("\t\t\t<div class='dataCellCurrent left'>")
					.append(weeWXAppCommon.doMoon(bits[290]))
					.append("</div>\n");
			sb.append("\t\t\t<div class='dataCellCurrent right'>")
					.append(weeWXAppCommon.doMoon(bits[291]))
					.append("</div>\n")
					.append(weeWXApp.currentSpacer)
					.append("\t\t\t<div class='dataCellCurrent right'>")
					.append(weeWXAppCommon.cssToSVG("wi-moonset"))
					.append("</div>\n");
		} else {
			sb.append("\t\t\t<div class='dataCellCurrent left'>")
					.append(weeWXAppCommon.cssToSVG("wi-moonrise"))
					.append("</div>\n")
					.append(weeWXApp.currentSpacer)
					.append("\t\t\t<div class='dataCellCurrent left'>")
					.append(bits[47])
					.append("</div>\n");
			sb.append("\t\t\t<div class='dataCellCurrent right'>")
					.append(bits[48])
					.append("</div>\n")
					.append(weeWXApp.currentSpacer)
					.append("\t\t\t<div class='dataCellCurrent right'>")
					.append(weeWXAppCommon.cssToSVG("wi-moonset"))
					.append("</div>\n");
		}

		sb.append("\t\t</div>\n\n");

		sb.append("\t</div>\n</div>\n");

		forceCurrentRefresh(sb.toString());
	}

	private void loadAndShowWebView(SafeWebView wv, String text, String url, boolean isCurrnet)
	{
		if(text != null && url != null)
		{
			weeWXAppCommon.LogMessage("loadAndShowWebView() url != null && text != null...", true, KeyValue.w);
			return;
		}

		if(text != null)
		{
			wv.post(() ->
			{
				wv.loadDataWithBaseURL("file:///android_asset/", text, "text/html", "utf-8", null);

				if(isCurrnet)
				{
					float density = weeWXApp.getDensity();
					weeWXAppCommon.LogMessage("density: " + density);
					weeWXAppCommon.LogMessage("current.getHeight(): " + current.getHeight());

					int heightInPx = Math.round(current.getHeight() / density);
					weeWXAppCommon.LogMessage("heightInPx: " + heightInPx, KeyValue.w);

					if(heightInPx == 0)
					{
						weeWXAppCommon.LogMessage("heightInPx is 0 skipping...");
						return;
					}

					if(heightInPx < 120 || heightInPx > 250)
					{
						weeWXAppCommon.LogMessage("heightInPx is out of bounds " + heightInPx + ", reloading and invalidating...");
						current.post(() -> current.reload());
						//current.post(() -> current.invalidate());
					}
				}
			});
		} else if(url != null) {
			weeWXAppCommon.LogMessage("loadAndShowWebView() load url: " + url);

			wv.post(() ->
			{
				wv.loadUrl(url);

				if(isCurrnet)
				{
					float density = weeWXApp.getDensity();
					weeWXAppCommon.LogMessage("density: " + density);
					weeWXAppCommon.LogMessage("current.getHeight(): " + current.getHeight());

					int heightInPx = Math.round(current.getHeight() / density);
					weeWXAppCommon.LogMessage("heightInPx: " + heightInPx);

					if(heightInPx == 0)
					{
						weeWXAppCommon.LogMessage("heightInPx is 0 skipping...");
						return;
					}

					if(heightInPx < 120 || heightInPx > 250)
					{
						weeWXAppCommon.LogMessage("heightInPx is out of bounds " + heightInPx + "reloading and invalidating...");
						current.post(() -> current.reload());
						//current.post(() -> current.invalidate());
					}
				}
			});
		} else {
			weeWXAppCommon.LogMessage("loadAndShowWebView() reload wv...");

			wv.post(() ->
			{
				wv.reload();

				if(isCurrnet)
				{
					float density = weeWXApp.getDensity();
					weeWXAppCommon.LogMessage("density: " + density);
					weeWXAppCommon.LogMessage("current.getHeight(): " + current.getHeight());

					int heightInPx = Math.round(current.getHeight() / density);
					weeWXAppCommon.LogMessage("heightInPx: " + heightInPx);

					if(heightInPx == 0)
					{
						weeWXAppCommon.LogMessage("heightInPx is 0 skipping...");
						return;
					}

					if(heightInPx < 120 || heightInPx > 250)
					{
						weeWXAppCommon.LogMessage("heightInPx is out of bounds " + heightInPx + ", invalidating...");
						current.post(() -> current.invalidate());
					}
				}
			});
		}
	}

	void loadWebViewURL(boolean forced, String url)
	{
		long current_time = weeWXAppCommon.getCurrTime();

		weeWXAppCommon.LogMessage("loadWebViewURL() url: " + url);

		if(forecast == null)
		{
			weeWXAppCommon.LogMessage("loadWebViewURL() forecast == null, skipping...", true, KeyValue.w);
			stopRefreshing();
			return;
		}

		String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
		if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.RadarOnHomeScreen ||
		   (radtype != null && radtype.equals("image")))
		{
			weeWXAppCommon.LogMessage("loadWebViewURL() radarforecast != weeWXApp.RadarOnHomeScreen or radtype == image...", KeyValue.w);
			stopRefreshing();
			return;
		}

		if(lastRunForecast + 5 > current_time)
		{
			weeWXAppCommon.LogMessage("loadWebViewURL() ran less than 5s ago, skipping...", KeyValue.d);
			stopRefreshing();
			return;
		}

		long[] npwsll = weeWXAppCommon.getNPWSLL();
		if(!forced && npwsll[1] <= 0)
		{
			weeWXAppCommon.LogMessage("Manual updating set, don't autoload the radar webpage...", KeyValue.d);
			loadWebViewContent(R.string.manual_update_set_refresh_screen_to_load);
			return;
		}

		lastRunForecast = current_time;

		if(url.equals(lastURL))
		{
			weeWXAppCommon.LogMessage("loadWebViewURL() forecast.reload()");
			loadAndShowWebView(forecast, null, null, false);
			return;
		}

		lastURL = url;

		weeWXAppCommon.LogMessage("loadWebViewURL() post lastURL check...");

		loadAndShowWebView(forecast, null, url, false);
		weeWXAppCommon.LogMessage("loadWebViewURL() url: " + url + " should have loaded...");
		stopRefreshing();
	}

	void loadWebViewContent(int resId)
	{
		weeWXAppCommon.LogMessage("loadWebViewContent() resId: " + resId);

		String html = weeWXApp.current_dialog_html.replaceAll("WARNING_BODY", weeWXApp.getAndroidString(resId));

		loadAndShowWebView(forecast, html, null, false);
		stopRefreshing();
	}

	void loadWebViewContent(String text)
	{
		if(forecast == null)
		{
			stopRefreshing();
			return;
		}

		weeWXAppCommon.LogMessage("Weather.loadWebviewContent()");

		loadAndShowWebView(forecast, text, null, false);
		stopRefreshing();
	}

	void forceCurrentRefresh(int resId)
	{
		forceCurrentRefresh(weeWXApp.getAndroidString(resId));
	}

	void forceCurrentRefresh(String body)
	{
		if(current == null)
		{
			weeWXAppCommon.LogMessage("Weather.forceCurrentRefresh() current == null", KeyValue.d);
			return;
		}

		weeWXAppCommon.LogMessage("forceCurrentRefresh()");

		body = weeWXAppCommon.indentNonBlankLines(body, 2) + "\n\n";

		String str = weeWXApp.current_html_headers + weeWXApp.html_header_rest + body;

		if(weeWXAppCommon.web_debug_on)
			str += weeWXApp.debug_html;

		str += weeWXApp.html_footer;

		if(weeWXAppCommon.debug_html)
			CustomDebug.writeOutput(requireContext(), "current_conditions", str, isVisible(), requireActivity());

		loadAndShowWebView(current, str, null, true);
		weeWXAppCommon.LogMessage("forceCurrentRefresh() calling loadAndShowWebView()");
	}

	private void forceRefresh()
	{
		weeWXAppCommon.LogMessage("forceRefresh()");

		if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) == weeWXApp.RadarOnHomeScreen)
		{
			String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
			if(radtype != null && radtype.equals("image"))
			{
				weeWXAppCommon.getRadarImage(true, false);
			} else {
				String radarURL = (String)KeyValue.readVar("RADAR_URL", weeWXApp.RADAR_URL_default);
				if(radarURL == null || radarURL.isBlank())
				{
					String html = weeWXApp.current_html_headers + weeWXApp.html_header_rest +
					              weeWXApp.getAndroidString(R.string.radar_url_not_set) +
					              weeWXApp.html_footer;
					loadWebViewContent(html);
				} else {
					loadWebViewURL(true, radarURL);
				}
			}
		} else {
			weeWXAppCommon.LogMessage("Let's force download fresh forecast data...");
			weeWXAppCommon.getForecast(true, false);
		}

		weeWXAppCommon.LogMessage("Let's force download fresh weather data...");
		weeWXAppCommon.getWeather(true, false);
	}

	private void loadWebView()
	{
		weeWXAppCommon.LogMessage("loadWebView()");
		loadWebView((String)KeyValue.readVar("fctype", ""),
				(String)KeyValue.readVar("forecastData", ""));
	}

	private void loadWebView(String fctype, String forecastData)
	{
		weeWXAppCommon.LogMessage("loadWebView(fctype, forecastData)");

		boolean radarForecast = (boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default);
		String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
		String radarURL = (String)KeyValue.readVar("RADAR_URL", weeWXApp.RADAR_URL_default);

		loadWebView(radarForecast, radtype, radarURL, fctype, forecastData);
	}

	private void loadWebView(boolean radarForecast, String radtype, String radarURL, String fctype, String forecastData)
	{
		weeWXAppCommon.LogMessage("Weather.loadWebView(radarForecast, radtype, radarURL, fctype, forecastData)");

		final StringBuilder sb = new StringBuilder();

		sb.append(weeWXApp.current_html_headers)
				.append(weeWXApp.script_header)
				.append(weeWXApp.html_header_rest)
				.append(weeWXApp.inline_arrow);

		if(radarForecast == weeWXApp.RadarOnHomeScreen)
		{
			if(radtype == null || radtype.isBlank())
			{
				weeWXAppCommon.LogMessage("Weather.loadWebView() radar type type is invalid: " + radtype, true, KeyValue.w);
				updateFLL(View.GONE);
				String tmp = String.format(weeWXApp.getAndroidString(R.string.radar_type_is_invalid), radtype);
				loadWebViewContent(tmp);
				stopRefreshing();
				return;
			}

			if(radarURL == null || radarURL.isBlank())
			{
				weeWXAppCommon.LogMessage("Weather.loadWebView() radar URL is null or blank", true, KeyValue.w);
				updateFLL(View.GONE);
				loadWebViewContent(R.string.radar_url_not_set);
				stopRefreshing();
				return;
			}

			if(radtype.equals("webpage"))
			{
				updateFLL(View.VISIBLE);
				weeWXAppCommon.LogMessage("Loading radarURL: " + radarURL);
				loadWebViewURL(false, radarURL);
			} else {
				loadOrReloadRadarImage();
			}

			stopRefreshing();
		} else {
			updateFLL(View.GONE);

			if(weeWXApp.getWidth() > weeWXApp.getHeight())
				sb.append("\n<div style='margin-top:10px'></div>\n\n");

			if(fctype == null || fctype.isBlank())
			{
				weeWXAppCommon.LogMessage("Weather.loadWebView() forecast type type is invalid: " + fctype, true, KeyValue.w);
				String finalErrorStr = String.format(weeWXApp.getAndroidString(R.string.forecast_type_is_invalid), fctype);
				loadWebViewContent(finalErrorStr);
				return;
			}

			if(forecastData == null || forecastData.isBlank())
			{
				forecastData = (String)KeyValue.readVar("forecastData", "");
				if(forecastData == null || forecastData.isBlank())
				{
					weeWXAppCommon.LogMessage("Weather.loadWebView() forecast was null or blank", true, KeyValue.w);
					loadWebViewContent(R.string.wasnt_able_to_connect_forecast);
					return;
				}
			}

			String base_url = "file:///android_asset/logos/";

			String extPNG = "_light.png";
			String extSVG = "_light.svg";
			if((int)KeyValue.readVar("theme", weeWXApp.theme_default) != R.style.AppTheme_weeWXApp_Dark_Common)
			{
				extPNG = "_dark.png";
				extSVG = "_dark.svg";
			}

			switch(fctype.toLowerCase(Locale.ENGLISH))
			{
				case "yahoo" ->
				{
					String[] content = weeWXAppCommon.processYahoo(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'><img src='")
							.append(base_url).append("yahoo").append(extSVG)
							.append("' height='45px' />\n</div>\n")
							.append(content[0]);
				}
				case "weatherzone" ->
				{
					String[] content = weeWXAppCommon.processWZ(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'><img src='")
							.append(base_url).append("wz").append(extSVG)
							.append("' height='45px' />\n</div>\n")
							.append(content[0]);
				}
				case "met.no" ->
				{
					String[] content = weeWXAppCommon.processMetNO(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					String imgStr = base_url + "met_no.png";
					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(imgStr).append("' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "wmo.int" ->
				{
					String[] content = weeWXAppCommon.processWMO(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					String imgStr = base_url + "wmo.png";
					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(imgStr).append("' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "weather.gov" ->
				{
					String[] content = weeWXAppCommon.processWGOV(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					String imgStr = base_url + "wgov.png";
					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(imgStr).append("' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "weather.gc.ca" ->
				{
					String[] content = weeWXAppCommon.processWCA(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					String imgStr = base_url + "wca.png";
					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(imgStr).append("' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "weather.gc.ca-fr" ->
				{
					String[] content = weeWXAppCommon.processWCAF(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					String imgStr = base_url + "wca.png";
					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(imgStr).append("' height='29px'/>")
							.append("</div><")
							.append(content[0]);
				}
				case "metoffice.gov.uk" ->
				{
					String[] content = weeWXAppCommon.processMET(forecastData, false);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					String imgStr = base_url + "met.png";
					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(imgStr).append("' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "bom2", "bom3" ->
				{
					String[] content;

					weeWXAppCommon.LogMessage("doing bom2/bom3...");

					if(fctype.toLowerCase(Locale.ENGLISH).equals("bom3"))
						content = weeWXAppCommon.processBOM3(forecastData);
					else if(fctype.toLowerCase(Locale.ENGLISH).equals("bom2"))
						content = weeWXAppCommon.processBOM2(forecastData);
					else
						return;

					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'><img src='file:///android_asset/logos/bom")
							.append(extSVG)
							.append("' height='45px' />\n</div>\n")
							.append(content[0]);
				}
				case "aemet.es" ->
				{
					String[] content = weeWXAppCommon.processAEMET(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					String imgStr = base_url + "aemet.png";
					sb.append("<div style='text-align:center'>")
							.append("<img src='")
							.append(imgStr)
							.append("' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "dwd.de" ->
				{
					String[] content = weeWXAppCommon.processDWD(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					String imgStr = base_url + "dwd.png";
					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(imgStr).append("' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "metservice.com" ->
				{
					String[] content = weeWXAppCommon.processMetService(forecastData, false);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'><img src='file:///android_asset/logos/metservice")
							.append(extSVG)
							.append("' height='45px' />\n</div>\n")
							.append(content[0]);
				}
				case "openweathermap.org" ->
				{
					String[] content = weeWXAppCommon.processOWM(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					String imgStr = base_url + "owm.png";
					sb.append("<div style='text-align:center'>")
							.append("<img src='")
							.append(imgStr)
							.append("' height='30px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "weather.com" ->
				{
					String[] content = weeWXAppCommon.processWCOM(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					String imgStr = base_url + "weather_com.svg";
					sb.append("<div style='text-align:center'>")
							.append("<img src='")
							.append(imgStr)
							.append("' height='45px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "met.ie" ->
				{
					String[] content = weeWXAppCommon.processMETIE(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					String imgStr = base_url + "met_ie";
					sb.append("<div style='text-align:center'>")
							.append("<img src='")
							.append(imgStr)
							.append(extPNG)
							.append("' height='45px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "tempoitalia.it" ->
				{
					String[] content = weeWXAppCommon.processTempoItalia(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					String imgStr = base_url + "tempoitalia_it.png";
					sb.append("<div style='text-align:center'>")
							.append("<img src='")
							.append(imgStr)
							.append("' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				default ->
				{
					String finalErrorStr = String.format(weeWXApp.getAndroidString(R.string.forecast_type_is_invalid), fctype);
					loadWebViewContent(finalErrorStr);
					return;
				}
			}

			sb.append(weeWXApp.html_footer);

			if(weeWXAppCommon.debug_html)
				CustomDebug.writeOutput(requireContext(), "forecast", sb.toString(), isVisible(), requireActivity());

			loadWebViewContent(sb.toString());
			stopRefreshing();
		}
	}

	private void stopRefreshing()
	{
		weeWXAppCommon.LogMessage("stopRefreshing()");
		if(!swipeLayout.isRefreshing())
			return;

		swipeLayout.post(() -> swipeLayout.setRefreshing(false));
	}

	private void reloadForecast()
	{
		weeWXAppCommon.LogMessage("Weather.reloadForecast()");

		String fctype = (String)KeyValue.readVar("fctype", weeWXApp.fctype_default);

		if(fctype.equals("weatherzone2"))
			return;

		if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.ForecastOnHomeScreen)
			return;

		updateFLL(View.GONE);

		boolean ret = weeWXAppCommon.getForecast(false, false);

		weeWXAppCommon.LogMessage("Weather.reloadForecast() fctype: " + fctype);
		//weeWXAppCommon.LogMessage("Weather.reloadForecast() forecastData: " + KeyValue.readVar("forecastData", weeWXApp.forecastData_default));

		if(!ret)
		{
			String LastForecastError = (String)KeyValue.readVar("LastForecastError", "");
			if(LastForecastError != null && !LastForecastError.isBlank())
			{
				weeWXAppCommon.LogMessage("Weather.reloadForecast() getForecast returned the following error: " +
				                          LastForecastError, true, KeyValue.w);
				loadWebViewContent(weeWXApp.current_html_headers + weeWXApp.html_header_rest +
				                   LastForecastError + weeWXApp.html_footer);
				KeyValue.putVar("LastForecastError", null);
			} else {
				weeWXAppCommon.LogMessage("Weather.reloadForecast() getForecast returned an unknown error...", true, KeyValue.w);
				loadWebViewContent(weeWXApp.current_html_headers + weeWXApp.html_header_rest +
				                   weeWXApp.getAndroidString(R.string.unknown_error_occurred) +
				                   weeWXApp.html_footer);
			}
		}

		weeWXAppCommon.LogMessage("Weather.reloadForecast() getForecast returned some content...");
		loadWebView((String)KeyValue.readVar("fctype", ""),
				(String)KeyValue.readVar("forecastData", ""));
	}

	private void loadOrReloadRadarImage()
	{
		weeWXAppCommon.LogMessage("Weather.loadOrReloadRadarImage()");

		if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.RadarOnHomeScreen)
			return;

		updateFLL(View.GONE);

		String html;

		if(weeWXAppCommon.getRadarImage(false, false) != null)
		{
			weeWXAppCommon.LogMessage("Weather.loadOrReloadRadarImage() done downloading radar.gif, prompt to show");
			loadWebView();
			stopRefreshing();
			return;
		}

		html = weeWXApp.current_html_headers + weeWXApp.html_header_rest +
		       weeWXApp.getAndroidString(R.string.radar_still_downloading) +
		       weeWXApp.html_footer;
		weeWXAppCommon.LogMessage("Weather.loadOrReloadRadarImage() Failed to download radar image", KeyValue.w);
		loadWebViewContent(html);
		stopRefreshing();
	}

	public void onResume()
	{
		super.onResume();

		weeWXAppCommon.LogMessage("Weather.onResume()");

		if(isVisible)
			return;

		isVisible = true;

		weeWXAppCommon.LogMessage("Weather.onResume() -- updating the value of the floating checkbox...");

		updateFLL();

		if(current != null)
			adjustHeight(current);
	}

	public void onPause()
	{
		super.onPause();

		weeWXAppCommon.LogMessage("Weather.onPause()");

		if(!isVisible)
			return;

		isVisible = false;

		weeWXAppCommon.LogMessage("Enabling swipe between screens...");
		activity.setUserInputPager(true);
	}

	private final Observer<String> notificationObserver = str ->
	{
		weeWXAppCommon.LogMessage("Weather.java notificationObserver: " + str);
		String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
		if(radtype == null)
			radtype = "";

		if(str.equals(weeWXAppCommon.REFRESH_WEATHER_INTENT))
			drawWeather();

		if(str.equals(weeWXAppCommon.STOP_WEATHER_INTENT))
			stopRefreshing();

		boolean radarforecast = (boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default);

		if(radarforecast == weeWXApp.RadarOnHomeScreen && radtype.equals("image"))
		{
			if(str.equals(weeWXAppCommon.REFRESH_RADAR_INTENT))
				drawRadar();

			if(str.equals(weeWXAppCommon.STOP_RADAR_INTENT))
				stopRefreshing();
		}

		if(radarforecast == weeWXApp.ForecastOnHomeScreen)
		{
			if(str.equals(weeWXAppCommon.REFRESH_FORECAST_INTENT))
			{
				weeWXAppCommon.LogMessage("Weather.notificationObserver running reloadForecast()");
				reloadForecast();
			}

			if(str.equals(weeWXAppCommon.STOP_FORECAST_INTENT))
			{
				weeWXAppCommon.LogMessage("Weather.notificationObserver running stopRefreshing()");
				stopRefreshing();
			}
		}

		if(str.equals(weeWXAppCommon.REFRESH_DARKMODE_INTENT))
			setMode();

		if(str.equals(weeWXAppCommon.EXIT_INTENT))
			onPause();
	};

	@Override
	public void onClick(View view)
	{
		KeyValue.putVar("disableSwipeOnRadar", floatingCheckBox.isChecked());
		updateFLL();
		weeWXAppCommon.LogMessage("Forecast.onClick() finished...");
	}

	private void updateFLL()
	{
		boolean radarForecast = (boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default);
		String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);

		if(radarForecast == weeWXApp.RadarOnHomeScreen && radtype.equals("webpage"))
			updateFLL(View.VISIBLE);
		else
			updateFLL(View.GONE);

		activity.setUserInputPager(fll.getVisibility() != View.VISIBLE || !floatingCheckBox.isChecked() || !isVisible());
	}

	private void updateFLL(int visibility)
	{
		if(fll.getVisibility() != visibility)
		{
			requireActivity().runOnUiThread(() -> fll.setVisibility(visibility));
			updateFLL();
		}
	}
}