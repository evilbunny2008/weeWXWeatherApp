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

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;


import static com.odiousapps.weewxweather.weeWXApp.getAndroidString;
import static com.odiousapps.weewxweather.weeWXAppCommon.weeWXNotificationManager;
import static com.odiousapps.weewxweather.weeWXAppCommon.cssToSVG;
import static com.odiousapps.weewxweather.weeWXAppCommon.deg2Str;
import static com.odiousapps.weewxweather.weeWXAppCommon.doStackOutput;
import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;
import static com.odiousapps.weewxweather.weeWXAppCommon.formatString;
import static com.odiousapps.weewxweather.weeWXAppCommon.getDateTimeStr;
import static com.odiousapps.weewxweather.weeWXAppCommon.getJson;
import static com.odiousapps.weewxweather.weeWXAppCommon.getSinceHour;
import static com.odiousapps.weewxweather.weeWXAppCommon.hasElement;
import static com.odiousapps.weewxweather.weeWXAppCommon.json_keys;
import static com.odiousapps.weewxweather.weeWXAppCommon.processUpdateInBG;
import static com.odiousapps.weewxweather.weeWXAppCommon.str2Int;

@SuppressWarnings("deprecation")
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

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(50);

	private long lastRunForecast = 0, lastRunRadar = 0;

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		LogMessage("Weather.onCreateView()");

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
			LogMessage("onRefresh()");
			forceRefresh();
		});

		floatingCheckBox = rootView.findViewById(R.id.floatingCheckBoxMain);
		floatingCheckBox.setOnClickListener(this);

		boolean disableSwipeOnRadar = (boolean)KeyValue.readVar("disableSwipeOnRadar", weeWXApp.disableSwipeOnRadar_default);
		floatingCheckBox.setChecked(disableSwipeOnRadar);

		//swipeLayout.setRefreshing(false);

		LogMessage("Weather.onViewCreated()-- adding notification manager...");
		weeWXNotificationManager.observeNotifications(getViewLifecycleOwner(), notificationObserver);

		FrameLayout currentFrameLayout = rootView.findViewById(R.id.current);
		FrameLayout forecastFrameLayout = rootView.findViewById(R.id.forecast);

		if(current == null)
			current = loadWebview(null, currentFrameLayout, true, true);
		else
			loadWebview(current, currentFrameLayout, true, true);

		if(current != null)
			LogMessage("current.getHeight(): " + current.getHeight());

		if(forecast == null)
			forecast = loadWebview(null, forecastFrameLayout, false, false);
		else
			loadWebview(forecast, forecastFrameLayout, false, false);

		return rootView;
	}

	@Override
	public void onDestroyView()
	{
		LogMessage("Weather.onDestroyView()");
		super.onDestroyView();

		weeWXNotificationManager.removeNotificationObserver(notificationObserver);

		if(current != null)
		{
			ViewParent parent = current.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(current);

			current.getViewTreeObserver().removeOnScrollChangedListener(scl);

			current.destroy();

			current = null;

			LogMessage("Weather.onDestroyView() current destroyed...");
		}

		if(forecast != null)
		{
			ViewParent parent = forecast.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(forecast);

			forecast.destroy();

			forecast = null;

			LogMessage("Weather.onDestroyView() forecast destroyed...");
		}
	}

	private void setMode()
	{
		boolean darkmode = (int)KeyValue.readVar("mode", weeWXApp.mode_default) == 1;
		boolean fdm = (boolean)KeyValue.readVar(MainActivity.FORCE_DARK_MODE, weeWXApp.force_dark_mode_default);

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
						doStackOutput(e);
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

	private SafeWebView loadWebview(SafeWebView webView, FrameLayout frameLayout, boolean doSwipe, boolean isCurrent)
	{
		//boolean dynamicSizing = viewid == R.id.current && weeWXApp.getWidth() < 1100;

		LogMessage("Weather.java loadWebview() isCurrent: " + isCurrent);
		LogMessage("Weather.java loadWebview() height: " + weeWXApp.getHeight());
		LogMessage("Weather.java loadWebview() width: " + weeWXApp.getWidth());
		LogMessage("Weather.java loadWebview() density: " + weeWXApp.getDensity());

		boolean wasNull = webView == null;

		if(wasNull)
		{
			LogMessage("Weather.java loadWebview() webView == null");
			webView = new SafeWebView(weeWXApp.getInstance());
		}

		frameLayout.addView(webView);

		if(isCurrent)
			webView.getSettings().setTextZoom(100);

		if(doSwipe)
			webView.getViewTreeObserver().addOnScrollChangedListener(scl);

		webView.setOnCustomPageFinishedListener((v, url) ->
		{
			if(url.strip().equals("data:text/html,"))
			{
				stopRefreshing();
				return;
			}

			LogMessage("Weather.java loadWebview() Just loaded URL: " + url);

			if(isCurrent)
			{
				LogMessage("Weather.java loadWebview() triggering newAttempt() resize code");
				newAttempt(1, v);
				return;
			}

			stopRefreshing();
		}, false);

		if(wasNull)
			return webView;

		return null;
	}

	private void newAttempt(int attempt, SafeWebView webView)
	{
		if(attempt > 3)
		{
			LogMessage("newAttempt() Attempt #4, we'll stop refreshing and return...");
			stopRefreshing();
			return;
		}

		LogMessage("newAttempt() Attempt #" + attempt + ", setting a new scheduler...");
		scheduler.schedule(() -> adjustHeight(webView, attempt), attempt * 100L, TimeUnit.MILLISECONDS);
	}

	private void adjustHeight(SafeWebView webView, int attempt)
	{
		if(attempt > 3)
		{
			LogMessage("adjustHeight() Attempt #4, we'll stop refreshing and return...");
			stopRefreshing();
			return;
		}

		if(webView == null || current == null)
		{
			LogMessage("adjustHeight() webView == null || current == null...");
			stopRefreshing();
			return;
		}

		LogMessage("adjustHeight() Processing attempt #" + attempt + "...");

		LogMessage("adjustHeight() current.getHeight(): " + current.getHeight());
		LogMessage("adjustHeight() swipeLayout.getHeight(): " + swipeLayout.getHeight());

		if(webView.getHeight() == 0 || swipeLayout.getHeight() == 0)
		{
			LogMessage("adjustHeight() webView.getHeight() == 0 || swipeLayout.getHeight() == 0");
			newAttempt(attempt + 1, webView);
			return;
		}

		webView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

		int heightMH = webView.getMeasuredHeight();

		LogMessage("adjustHeight() webView measured height: " + heightMH);

		float curdensity =  weeWXApp.getDensity();

		int heightPx = Math.round(current.getHeight() / curdensity);

		LogMessage("adjustHeight() heightPx: " + heightPx);

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
					LogMessage("adjustHeight() value: " + value);

					if(value == null || value.isBlank() || value.equalsIgnoreCase("null"))
					{
						LogMessage("adjustHeight() webView.evaluateJavascript() value is null " +
												  "or blank or equals 'null'", KeyValue.v);

						newAttempt(attempt + 1, webView);
						return;
					}

					//doStackTrace(0);

					int heightInPx = str2Int(value);
					LogMessage("adjustHeight() From Javascript heightInPx: " + heightInPx);

					if(heightInPx == 0)
					{
						LogMessage("adjustHeight() heightInPx is 0 skipping...");

						newAttempt(attempt + 1, webView);
						return;
					}

//					if((heightInPx > 0 && heightInPx < 200) || heightInPx > 250)
					if(heightInPx > 250)
					{
						LogMessage("adjustHeight() heightInPx is out of bounds...");
						//current.post(() -> current.reload());
						//current.post(() -> current.invalidate());
						if(!current.isInLayout())
							current.post(() -> current.requestLayout());

						newAttempt(attempt + 1, webView);
						return;
					}

					// Post a Runnable to make sure contentHeight is available
					float density =  weeWXApp.getDensity();
					LogMessage("adjustHeight() 1DP: " + density);
					int height = Math.round(heightInPx * density);
					LogMessage("adjustHeight() height in DP: " + height);

					int contentHeightPx = tv1.getHeight() + tv2.getHeight() + height;
					LogMessage("adjustHeight() contentHeightPx: " + contentHeightPx);

					ViewGroup.LayoutParams params = swipeLayout.getLayoutParams();

					if(params.height != contentHeightPx)
					{
						LogMessage("adjustHeight() params.height != contentHeightPx");
						params.height = contentHeightPx;
						swipeLayout.post(() -> swipeLayout.setLayoutParams(params));
						newAttempt(attempt + 1, webView);
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
		LogMessage("drawRadar()");

		if(!(boolean)KeyValue.isPrefSet("radarforecast") ||
		   (boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.RadarOnHomeScreen)
			return;

		long now = System.currentTimeMillis();
		long dur = (now - lastRunRadar) / 1000;
		if(dur < 5)
		{
			LogMessage("We already ran less than 5s ago, skipping...", KeyValue.d);
			if(!swipeLayout.isRefreshing())
				stopRefreshing();

			return;
		}

		lastRunRadar = now;

		String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
		if(radtype == null || radtype.isBlank())
		{
			LogMessage("radtype: " + radtype);
			String tmp = String.format(getAndroidString(R.string.radar_type_is_invalid), radtype);
			loadWebViewContent(tmp);
			stopRefreshing();
			return;
		}

		if(radtype.equals("image"))
		{
			LogMessage("Update the radar iamge from cache...");
			loadOrReloadRadarImage();
		} else {
			String radarURL = (String)KeyValue.readVar("RADAR_URL", "");
			if(radarURL == null || radarURL.isBlank())
			{
				LogMessage("radar URL not set or blank: " + radarURL, true, KeyValue.w);
				loadWebViewContent(R.string.radar_url_not_set);
				return;
			}

			weeWXAppCommon.NPWSLL npwsll = weeWXAppCommon.getNPWSLL();
			if(npwsll.periodTime() <= 0)
			{
				LogMessage("Manual updating set, don't autoload the radar webpage...", true, KeyValue.d);
				loadWebViewContent(R.string.manual_update_set_refresh_screen_to_load);
				return;
			}

			LogMessage("Update the radar webview page... url: " + radarURL);
			loadWebViewURL(false, radarURL);
		}
	}

	void drawWeather()
	{
		LogMessage("drawWeather()");

		if(!KeyValue.isPrefSet(json_keys[0] + weeWXApp.TIME_EXT))
		{
			forceCurrentRefresh(R.string.still_downloading_weather_data);
			return;
		}

		long report_time = Math.round((double)getJson("report_time", 0D) * 1_000L);
		String tempSym = KeyValue.getLabel("current_outTemp", "°C");
		String humSym = KeyValue.getLabel("current_outHumidity", "%");
		String pressSym = KeyValue.getLabel("current_barometer", "hPa");
		String speedSym = KeyValue.getLabel("current_windGust", "km/h");
		String rainSym = KeyValue.getLabel("day_rain_sum", "mm");

		checkFields(tv1, (String)getJson("station_location", ""));
		checkFields(tv2, weeWXApp.getInstance().sdf18.format(new Date(report_time)));

		String tmpStr = formatString("current_outTemp");
		if(tmpStr == null || tmpStr.isBlank())
			return;

		final StringBuilder sb = new StringBuilder();
		sb.append("\n<div class='todayCurrent'>\n");
		sb.append("\t<div class='topRowCurrent'>\n");
		sb.append("\t\t<div class='mainTemp'>");
		sb.append(tmpStr).append(tempSym);
		sb.append("</div>\n");

		tmpStr = formatString("current_appTemp");
		if(tmpStr == null || tmpStr.isBlank())
			return;

		sb.append("\t\t<div class='apparentTemp'>AT:<br/>");
		sb.append(tmpStr).append(tempSym);
		sb.append("</div>\n\t</div>\n\n");

		sb.append("\t<div class='dataTableCurrent'>\n");

		sb.append("\t\t<div class='dataRowCurrent'>\n");

		tmpStr = formatString("current_windGust");
		if(tmpStr == null || tmpStr.isBlank())
			return;

		sb.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(weeWXAppCommon.fiToSVG("flaticon-windy"))
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(tmpStr)
				.append(speedSym)
				.append("</div>\n");

		tmpStr = formatString("current_barometer");
		if(tmpStr == null || tmpStr.isBlank())
			return;

		sb.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(tmpStr)
				.append(pressSym)
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(cssToSVG("wi-barometer"))
				.append("</div>\n");

		sb.append("\t\t</div>\n\t\t<div class='dataRowCurrent'>\n");

		tmpStr = deg2Str("current_windGustDir", "current_windGust");
		if(tmpStr == null || tmpStr.isBlank())
			return;

		tmpStr = tmpStr.strip();

		sb.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(cssToSVG("wi-wind-deg", Math.round((float)getJson("current_windGustDir", 0f))))
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(tmpStr)
				.append("</div>\n");

		sb.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(formatString("current_outHumidity"))
				.append(humSym)
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(cssToSVG("wi-humidity"))
				.append("</div>\n");

		sb.append("\t\t</div>\n\t\t<div class='dataRowCurrent'>\n");

		int since_hour = (int)getJson("since_hour", 0);
		String since = getSinceHour(since_hour, R.string.since);
		String rain = formatString("day_rain_sum");
		if(rain == null || rain.isBlank())
			return;

		if(since_hour > 0)
		{
			rain = formatString("since_today");
			if(rain == null || rain.isBlank())
				return;
		}

		rain += rainSym + " ";

		sb.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(cssToSVG("wi-umbrella"))
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(rain)
				.append(since)
				.append("</div>\n");

		tmpStr = formatString("current_dewpoint");
		if(tmpStr == null || tmpStr.isBlank())
			return;

		sb.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(tmpStr)
				.append(tempSym)
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(cssToSVG("wi-raindrop"))
				.append("</div>\n");

		sb.append("\t\t</div>\n");

		boolean hasRadiation = hasElement("current_radiation");
		boolean hasUV = hasElement("current_UV");

		if(hasRadiation || hasUV)
		{
			sb.append("\t\t<div class='dataRowCurrent'>\n");

			if(hasUV)
			{
				tmpStr = formatString("current_UV");
				if(tmpStr == null || tmpStr.isBlank())
					return;

				sb.append("\t\t\t<div class='dataCellCurrent left'>")
						.append(weeWXAppCommon.fiToSVG("flaticon-women-sunglasses"))
						.append("</div>\n")
						.append(weeWXApp.currentSpacer)
						.append("\t\t\t<div class='dataCellCurrent left'>")
						.append(tmpStr)
						.append(KeyValue.getLabel("current_UV", "UVI").strip())
						.append("</div>\n");
			} else {
				sb.append(weeWXApp.emptyField);
			}

			if(hasRadiation)
			{
				tmpStr = formatString("current_radiation");
				if(tmpStr == null || tmpStr.isBlank())
					return;

				sb.append("\t\t\t<div class='dataCellCurrent right'>")
						.append(tmpStr)
						.append(KeyValue.getLabel("current_radiation", "W/m²"))
						.append("</div>\n")
						.append(weeWXApp.currentSpacer)
						.append("\t\t\t<div class='dataCellCurrent right'>")
						.append(weeWXAppCommon.fiToSVG("flaticon-women-sunglasses"))
						.append("</div>\n");
			} else {
				sb.append(weeWXApp.emptyField);
			}

			sb.append("\t\t</div>\n");
		}

		boolean hasInTemp = hasElement("current_inTemp");
		boolean hasInHumidity = hasElement("current_inHumidity");

		if((hasInTemp || hasInHumidity) &&
		   (boolean)KeyValue.readVar("showIndoor", weeWXApp.showIndoor_default))
		{
			sb.append("\t\t<div class='dataRowCurrent'>\n");

			if(hasInTemp)
			{
				tmpStr = formatString("current_inTemp");
				if(tmpStr == null || tmpStr.isBlank())
					return;

				sb.append("\t\t\t<div class='dataCellCurrent left'>")
						.append(weeWXAppCommon.fiToSVG("flaticon-home-page"))
						.append("</div>\n")
						.append(weeWXApp.currentSpacer)
						.append("\t\t\t<div class='dataCellCurrent left'>")
						.append(tmpStr)
						.append(tempSym)
						.append("</div>\n");
			} else {
				sb.append(weeWXApp.emptyField);
			}

			if(hasInHumidity)
			{
				tmpStr = formatString("current_inHumidity");
				if(tmpStr == null || tmpStr.isBlank())
					return;

				sb.append("\t\t\t<div class='dataCellCurrent right'>")
						.append(tmpStr)
						.append(humSym)
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

		long sunrise = Math.round((double)getJson("day_sun_rise", 0D) * 1_000L);
		if(sunrise == 0)
			return;

		sb.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(cssToSVG("wi-sunrise"))
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(weeWXApp.getInstance().sdf20.format(new Date(sunrise)))
				.append("</div>\n");

		long sunset = Math.round((double)getJson("day_sun_set", 0D) * 1_000L);
		if(sunset == 0)
			return;

		sb.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(weeWXApp.getInstance().sdf20.format(new Date(sunset)))
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(cssToSVG("wi-sunset"))
				.append("</div>\n");

		sb.append("\t\t</div>\n\t\t<div class='dataRowCurrent'>\n");

		boolean next_moon = (boolean)KeyValue.readVar("next_moon", weeWXApp.next_moon_default);

		boolean has_moon_next = hasElement("day_moon_next_rise") && hasElement("day_moon_next_set");

		long moon_rise = Math.round((double)getJson("day_moon_rise", 0D) * 1_000L);
		long moon_set = Math.round((double)getJson("day_moon_set", 0D) * 1_000L);

		if(next_moon && has_moon_next)
		{
			moon_rise = Math.round((double)getJson("day_moon_next_rise", 0D) * 1_000L);
			moon_set = Math.round((double)getJson("day_moon_next_set", 0D) * 1_000L);
		}

		tmpStr = getDateTimeStr(moon_rise, 4);
		if(tmpStr == null || tmpStr.isBlank())
			return;

		sb.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(cssToSVG("wi-moonrise"))
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent left'>")
				.append(tmpStr)
				.append("</div>\n");

		tmpStr = getDateTimeStr(moon_set, 4);
		if(tmpStr == null || tmpStr.isBlank())
			return;

		sb.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(tmpStr)
				.append("</div>\n")
				.append(weeWXApp.currentSpacer)
				.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(cssToSVG("wi-moonset"))
				.append("</div>\n");

		sb.append("\t\t</div>\n\n");

		sb.append("\t</div>\n</div>\n");

		forceCurrentRefresh(sb.toString());
	}

	private void loadAndShowWebView(SafeWebView wv, String text, String url, boolean isCurrnet)
	{
		if(text != null && url != null)
		{
			LogMessage("loadAndShowWebView() url != null && text != null...", true, KeyValue.w);
			return;
		}

		if(text != null)
		{
			wv.post(() ->
			{
				LogMessage("loadAndShowWebView() text.length(): " + text.length());
				wv.loadDataWithBaseURL("file:///android_asset/", text, "text/html", "utf-8", null);

				if(isCurrnet)
				{
					wv.postDelayed(() ->
					{
						float density = weeWXApp.getDensity();
						LogMessage("loadAndShowWebView() density: " + density);
						LogMessage("loadAndShowWebView() current.getHeight(): " + current.getHeight());

						int heightInPx = Math.round(current.getHeight() / density);
						LogMessage("loadAndShowWebView() heightInPx: " + heightInPx);

						if(heightInPx == 0)
						{
							LogMessage("loadAndShowWebView() heightInPx is 0 skipping...");
							return;
						}

						if(heightInPx < 120 || heightInPx > 250)
						{
							LogMessage("loadAndShowWebView() heightInPx is out of bounds " + heightInPx + ", reloading and invalidating...");
							//current.post(() -> current.reload());
							//current.post(() -> current.invalidate());
							if(!current.isInLayout())
								current.post(() -> current.requestLayout());
						}
					}, 300);
				}
			});
		} else if(url != null) {
			LogMessage("loadAndShowWebView() load url: " + url);

			wv.post(() ->
			{
				wv.loadUrl(url);

				if(isCurrnet)
				{
					float density = weeWXApp.getDensity();
					LogMessage("loadAndShowWebView() density: " + density);
					LogMessage("loadAndShowWebView() current.getHeight(): " + current.getHeight());

					int heightInPx = Math.round(current.getHeight() / density);
					LogMessage("loadAndShowWebView() heightInPx: " + heightInPx);

					if(heightInPx == 0)
					{
						LogMessage("loadAndShowWebView() heightInPx is 0 skipping...");
						return;
					}

					if(heightInPx < 120 || heightInPx > 250)
					{
						LogMessage("loadAndShowWebView() heightInPx is out of bounds " + heightInPx + "reloading and invalidating...");
						current.post(() -> current.reload());
						//current.post(() -> current.invalidate());
					}
				}
			});
		} else {
			LogMessage("loadAndShowWebView() reload wv...");

			wv.post(() ->
			{
				wv.reload();

				if(isCurrnet)
				{
					float density = weeWXApp.getDensity();
					LogMessage("loadAndShowWebView() density: " + density);
					LogMessage("loadAndShowWebView() current.getHeight(): " + current.getHeight());

					int heightInPx = Math.round(current.getHeight() / density);
					LogMessage("loadAndShowWebView() heightInPx: " + heightInPx);

					if(heightInPx == 0)
					{
						LogMessage("loadAndShowWebView() heightInPx is 0 skipping...");
						return;
					}

					if(heightInPx < 120 || heightInPx > 250)
					{
						LogMessage("loadAndShowWebView() heightInPx is out of bounds " + heightInPx + ", invalidating...");
						current.post(() -> current.invalidate());
					}
				}
			});
		}
	}

	void loadWebViewURL(boolean forced, String url)
	{
		long now = System.currentTimeMillis();

		LogMessage("loadWebViewURL() url: " + url);

		if(forecast == null)
		{
			LogMessage("loadWebViewURL() forecast == null, skipping...", true, KeyValue.w);
			stopRefreshing();
			return;
		}

		String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
		if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.RadarOnHomeScreen ||
		   (radtype != null && radtype.equals("image")))
		{
			LogMessage("loadWebViewURL() radarforecast != weeWXApp.RadarOnHomeScreen or radtype == image...", KeyValue.w);
			stopRefreshing();
			return;
		}

		long dur = (now - lastRunForecast) / 1000;
		if(dur < 5)
		{
			LogMessage("loadWebViewURL() ran less than 5s ago, skipping...", KeyValue.d);
			stopRefreshing();
			return;
		}

		weeWXAppCommon.NPWSLL npwsll = weeWXAppCommon.getNPWSLL();
		if(!forced && npwsll.periodTime() <= 0)
		{
			LogMessage("Manual updating set, don't autoload the radar webpage...", KeyValue.d);
			loadWebViewContent(R.string.manual_update_set_refresh_screen_to_load);
			return;
		}

		lastRunForecast = now;

		if(url.equals(lastURL))
		{
			LogMessage("loadWebViewURL() forecast.reload()");
			loadAndShowWebView(forecast, null, null, false);
			return;
		}

		lastURL = url;

		LogMessage("loadWebViewURL() post lastURL check...");

		loadAndShowWebView(forecast, null, url, false);
		LogMessage("loadWebViewURL() url: " + url + " should have loaded...");
	}

	void loadWebViewContent(int resId)
	{
		LogMessage("loadWebViewContent() resId: " + resId);

		String html = weeWXApp.current_dialog_html.replace(weeWXApp.WARNING_BODY, getAndroidString(resId));

		loadAndShowWebView(forecast, html, null, false);
	}

	void loadWebViewContent(String text)
	{
		if(forecast == null)
		{
			stopRefreshing();
			return;
		}

		LogMessage("Weather.loadWebviewContent()");

		loadAndShowWebView(forecast, text, null, false);
	}

	void forceCurrentRefresh(int resId)
	{
		forceCurrentRefresh(getAndroidString(resId));
	}

	void forceCurrentRefresh(String body)
	{
		if(current == null)
		{
			LogMessage("Weather.forceCurrentRefresh() current == null", KeyValue.d);
			return;
		}

		LogMessage("forceCurrentRefresh()");

		body = weeWXAppCommon.indentNonBlankLines(body, 2) + "\n\n";

		String str = weeWXApp.current_html_headers + weeWXApp.html_header_rest + body;

		if(weeWXAppCommon.web_debug_on)
			str += weeWXApp.debug_html;

		str += weeWXApp.html_footer;

		//if(weeWXAppCommon.debug_html)
		//	CustomDebug.writeOutput(requireContext(), "current_conditions", str, isVisible(), requireActivity());

		loadAndShowWebView(current, str, null, true);
		newAttempt(1, current);
		//LogMessage("forceCurrentRefresh() calling loadAndShowWebView()");
	}

	private void forceRefresh()
	{
		LogMessage("forceRefresh()");

		boolean radar = false;
		boolean forecast = false;

		if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) == weeWXApp.RadarOnHomeScreen)
		{
			String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
			if(radtype != null && radtype.equals("image"))
			{
				radar = true;
			} else {
				String radarURL = (String)KeyValue.readVar("RADAR_URL", "");
				if(radarURL == null || radarURL.isBlank())
				{
					String html = weeWXApp.current_html_headers + weeWXApp.html_header_rest +
								  getAndroidString(R.string.radar_url_not_set) +
								  weeWXApp.html_footer;
					loadWebViewContent(html);
				} else {
					loadWebViewURL(true, radarURL);
				}
			}
		} else {
			forecast = true;
		}

		processUpdateInBG(true, false, false, true,
				true, forecast, radar, false);
	}

	private void loadWebView()
	{
		LogMessage("Weather.loadWebView()");

		if(!KeyValue.isPrefSet("radarforecast"))
			return;

		boolean radarForecast = (boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default);

		final StringBuilder sb = new StringBuilder();

		sb.append(weeWXApp.current_html_headers)
				.append(weeWXApp.script_header)
				.append(weeWXApp.html_header_rest)
				.append(weeWXApp.inline_arrow);

		if(radarForecast == weeWXApp.RadarOnHomeScreen)
		{
			String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
			String radarURL = (String)KeyValue.readVar("RADAR_URL", "");

			if(radtype == null || radtype.isBlank())
			{
				LogMessage("Weather.loadWebView() radar type type is invalid: " + radtype, true, KeyValue.w);
				updateFLL(View.GONE);
				String tmp = String.format(getAndroidString(R.string.radar_type_is_invalid), radtype);
				loadWebViewContent(tmp);
				return;
			}

			if(radarURL == null || radarURL.isBlank())
			{
				LogMessage("Weather.loadWebView() radar URL is null or blank", true, KeyValue.w);
				updateFLL(View.GONE);
				loadWebViewContent(R.string.radar_url_not_set);
				return;
			}

			if(radtype.equals("webpage"))
			{
				updateFLL(View.VISIBLE);
				LogMessage("Loading radarURL: " + radarURL);
				loadWebViewURL(false, radarURL);
			} else {
				loadOrReloadRadarImage();
			}
		} else {
			updateFLL(View.GONE);

			if(weeWXApp.getWidth() > weeWXApp.getHeight())
				sb.append("\n<div style='margin-top:10px'></div>\n\n");

			String fctype = (String)KeyValue.readVar(weeWXApp.FCTYPE, "");
			if(fctype == null || fctype.isBlank())
			{
				LogMessage("Weather.loadWebView() forecast type is invalid: " + fctype, true, KeyValue.w);
				String finalErrorStr = String.format(getAndroidString(R.string.forecast_type_is_invalid), fctype);
				loadWebViewContent(finalErrorStr);
				return;
			}

			String forecastGson = (String)KeyValue.readVar("forecastGsonEncoded", "");
			boolean hasForecastGson = forecastGson != null && forecastGson.length() > 128;
			if(hasForecastGson)
				LogMessage("forecastGson.length(): " + forecastGson.length());

			if(!hasForecastGson)
			{
				LogMessage("Weather.loadWebView() forecastGson is null or blank, " +
										  "now check if there is a forecast error...", true, KeyValue.w);

				String tmpStr = (String)KeyValue.readVar("LastForecastError", "");
				if(tmpStr != null && !tmpStr.isBlank())
					loadWebViewContent(tmpStr);
				else
					loadWebViewContent(getAndroidString(R.string.failed_to_process_forecast_data));

				KeyValue.putVar("LastForecastError", null);
				return;
			}

			String extPNG = "_light.png";
			String extSVG = "_light.svg";
			if((int)KeyValue.readVar("theme", weeWXApp.theme_default) != R.style.AppTheme_weeWXApp_Dark_Common)
			{
				extPNG = "_dark.png";
				extSVG = "_dark.svg";
			}

			String[] content = weeWXAppCommon.getGsonContent(forecastGson, false);
			if(content.length < 2)
			{
				LogMessage("Weather.loadWebView() forecastGson is null or blank, " +
										  "now check if there is a forecast error...", true, KeyValue.w);

				String tmpStr = (String)KeyValue.readVar("LastForecastError", "");
				if(tmpStr != null && !tmpStr.isBlank())
					loadWebViewContent(tmpStr);
				else
					loadWebViewContent(getAndroidString(R.string.failed_to_process_forecast_data));

				KeyValue.putVar("LastForecastError", null);
				return;
			}

			if(content[0] != null && content[0].equals("error"))
			{
				LogMessage("Weather.loadWebView() forecastGson is null or blank, " +
										  "now check if there is a forecast error...", true, KeyValue.w);

				if(content[1] != null && !content[1].isBlank())
				{
					loadWebViewContent(content[1]);
					return;
				}

				String tmpStr = (String)KeyValue.readVar("LastForecastError", "");
				if(tmpStr != null && !tmpStr.isBlank())
					loadWebViewContent(tmpStr);
				else
					loadWebViewContent(getAndroidString(R.string.failed_to_process_forecast_data));

				KeyValue.putVar("LastForecastError", null);
				return;
			}

			String logo = "file:///android_asset/logos/";
			switch(fctype.toLowerCase(Locale.ENGLISH).strip())
			{
				case "aemet.es" -> logo += "aemet.png";
				case "bom2", "bom3daily", "bom3hourly" -> logo += "bom" + extSVG;
				case "dwd.de" -> logo += "dwd.png";
				case "met.ie" -> logo += "met_ie" + extPNG;
				case "met.no" -> logo += "met_no.png";
				case "metoffice.gov.uk" -> logo += "met.png";
				case "metservice.com" -> logo += "metservice" + extSVG;
				case "openweathermap.org" -> logo += "owm.png";
				case "tempoitalia.it" -> logo += "tempoitalia_it.png";
				case "weather.com" -> logo += "weather_com.svg";
				case "weather.gc.ca", "weather.gc.ca-fr" -> logo += "wca.png";
				case "weather.gov" -> logo += "wgov.png";
				case "weatherzone", "weatherzone2" -> logo += "wz" + extSVG;
				case "wmo.int" -> logo += "wmo.png";
				case "yahoo" -> logo += "yahoo" + extSVG;
				default ->
				{
					String finalErrorStr = String.format(getAndroidString(R.string.forecast_type_is_invalid), fctype);
					loadWebViewContent(finalErrorStr);
					return;
				}
			}

			String logoStyle = "height:45px";
			if(fctype.equals("met.no") && (int)KeyValue.readVar("theme", weeWXApp.theme_default) == R.style.AppTheme_weeWXApp_Dark_Common)
				logoStyle += ";filter:invert(1)";

			sb.append("<div style='text-align:center'><img src='")
					.append(logo)
					.append("' style='").append(logoStyle).append("' />\n</div>\n")
					.append(content[1]);

			sb.append(weeWXApp.html_footer);

			//if(weeWXAppCommon.debug_html)
			//	CustomDebug.writeOutput(requireContext(), "forecast", sb.toString(), isVisible(), requireActivity());

			loadWebViewContent(sb.toString());
		}
	}

	private void stopRefreshing()
	{
		LogMessage("stopRefreshing()");
		if(!swipeLayout.isRefreshing())
			return;

		swipeLayout.post(() -> swipeLayout.setRefreshing(false));
	}

	private void reloadForecast()
	{
		LogMessage("Weather.reloadForecast()");

		boolean ret = weeWXAppCommon.getForecast(false, false, false);

		if(!ret)
		{
			String LastForecastError = (String)KeyValue.readVar("LastForecastError", "");
			if(LastForecastError != null && !LastForecastError.isBlank())
			{
				LogMessage("Weather.reloadForecast() getForecast returned the following error: " +
										  LastForecastError, true, KeyValue.w);
				loadWebViewContent(weeWXApp.current_html_headers + weeWXApp.html_header_rest +
								   LastForecastError + weeWXApp.html_footer);
			} else {
				LogMessage("Weather.reloadForecast() getForecast returned an unknown error...", true, KeyValue.w);
				loadWebViewContent(weeWXApp.current_html_headers + weeWXApp.html_header_rest +
								   getAndroidString(R.string.unknown_error_occurred) +
								   weeWXApp.html_footer);
			}

			KeyValue.putVar("LastForecastError", null);
		}

		LogMessage("Weather.reloadForecast() getForecast returned some content...");
		loadWebView();
	}

	private void loadOrReloadRadarImage()
	{
		LogMessage("Weather.loadOrReloadRadarImage()");

		if(!(boolean)KeyValue.isPrefSet("radarforecast") ||
		   (boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.RadarOnHomeScreen)
			return;

		updateFLL(View.GONE);

		String html;

		if(weeWXAppCommon.getRadarImage(false, false, false) != null)
		{
			LogMessage("Weather.loadOrReloadRadarImage() done downloading radar.gif, prompt to show");
			loadWebView();
			return;
		}

		html = weeWXApp.current_html_headers + weeWXApp.html_header_rest +
			   getAndroidString(R.string.radar_still_downloading) +
			   weeWXApp.html_footer;
		LogMessage("Weather.loadOrReloadRadarImage() Failed to download radar image", KeyValue.w);
		loadWebViewContent(html);
	}

	public void onResume()
	{
		super.onResume();

		LogMessage("Weather.onResume()");

		if(isVisible)
			return;

		isVisible = true;

		LogMessage("Weather.onResume() -- updating the value of the floating checkbox...");

		updateFLL();

		if(KeyValue.isPrefSet("radarforecast"))
		{
//			if(current != null)
//				adjustHeight(current, 3);

			LogMessage("Weather.onViewCreated() radarforecast is set");
			boolean radarforecast = (boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default);
			LogMessage("Weather.onViewCreated() radarforecast: " + radarforecast);

			if(radarforecast == weeWXApp.RadarOnHomeScreen)
				drawRadar();
			else
				reloadForecast();

			drawWeather();
		} else {
			LogMessage("Weather.onViewCreated() radarforecast isn't set");
		}
	}

	public void onPause()
	{
		super.onPause();

		LogMessage("Weather.onPause()");

		if(!isVisible)
			return;

		isVisible = false;

		LogMessage("Enabling swipe between screens...");
		activity.setUserInputPager(true);
	}

	private final Observer<String> notificationObserver = str ->
	{
		LogMessage("Weather.java notificationObserver: " + str);
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

		} else {
			if(str.equals(weeWXAppCommon.REFRESH_FORECAST_INTENT))
			{
				LogMessage("Weather.notificationObserver running reloadForecast()");
				reloadForecast();
			}

			if(str.equals(weeWXAppCommon.STOP_FORECAST_INTENT))
			{
				LogMessage("Weather.notificationObserver running stopRefreshing()");
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
		LogMessage("Forecast.onClick() finished...");
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