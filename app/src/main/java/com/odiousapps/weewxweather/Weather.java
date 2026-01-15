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

import java.time.Duration;
import java.time.Instant;
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

import static com.odiousapps.weewxweather.weeWXAppCommon.doStackOutput;
import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;

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

	private boolean current_refreshed = true;
	private boolean forecast_refresh = true;

	private final ViewTreeObserver.OnScrollChangedListener scl = () -> swipeLayout.setEnabled(current.getScrollY() == 0);

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

	private Instant lastRunForecast = Instant.EPOCH, lastRunRadar = Instant.EPOCH;

	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState)
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
			current_refreshed = false;
			forecast_refresh = false;
			swipeLayout.setRefreshing(true);
			LogMessage("onRefresh()");
			forceRefresh();
		});

		floatingCheckBox = rootView.findViewById(R.id.floatingCheckBoxMain);
		floatingCheckBox.setOnClickListener(this);

		boolean disableSwipeOnRadar = (boolean)KeyValue.readVar("disableSwipeOnRadar", weeWXApp.disableSwipeOnRadar_default);
		floatingCheckBox.setChecked(disableSwipeOnRadar);

		swipeLayout.setRefreshing(true);

		LogMessage("Weather.onViewCreated()-- adding notification manager...");
		weeWXAppCommon.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);

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

		weeWXAppCommon.NotificationManager.getNotificationLiveData().removeObservers(getViewLifecycleOwner());

		if(current != null)
		{
			ViewParent parent = current.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(current);

			current.getViewTreeObserver().removeOnScrollChangedListener(scl);

			weeWXApp.getInstance().wvpl.recycleWebView(current);

			LogMessage("Weather.onDestroyView() recycled current...");
		}

		if(forecast != null)
		{
			ViewParent parent = forecast.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(forecast);

			weeWXApp.getInstance().wvpl.recycleWebView(forecast);

			LogMessage("Weather.onDestroyView() recycled forecast...");
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

		LogMessage("Weather.java loadWebview() height: " + weeWXApp.getHeight());
		LogMessage("Weather.java loadWebview() width: " + weeWXApp.getWidth());
		LogMessage("Weather.java loadWebview() density: " + weeWXApp.getDensity());

		boolean wasNull = webView == null;

		if(wasNull)
		{
			LogMessage("Weather.java loadWebview() webView == null");
			webView = weeWXApp.getInstance().wvpl.getWebView();
		}

		if(webView.getParent() != null)
			((ViewGroup)webView.getParent()).removeView(webView);

		frameLayout.removeAllViews();
		frameLayout.addView(webView);

		if(isCurrent)
			webView.getSettings().setTextZoom(100);

		if(doSwipe)
			webView.getViewTreeObserver().addOnScrollChangedListener(scl);

		webView.setOnPageFinishedListener((v, url) ->
		{
			if(url.strip().equals("data:text/html,"))
				return;

			LogMessage("Just loaded URL: " + url);

			if(isCurrent)
			{
				scheduler.schedule(() -> adjustHeight(current, 1), 100, TimeUnit.MILLISECONDS);
				scheduler.schedule(() -> adjustHeight(current, 2), 200, TimeUnit.MILLISECONDS);
				scheduler.schedule(() -> adjustHeight(current, 3), 300, TimeUnit.MILLISECONDS);
				return;
			}

			forecast_refresh = true;
			stopRefreshing();
		}, false);

		if(wasNull)
			return webView;

		return null;
	}

	private void adjustHeight(SafeWebView webView, int attempt)
	{
		if(webView == null || current == null || (current.getHeight() == 0 && swipeLayout.getHeight() == 0))
			return;

		LogMessage("current.getHeight(): " + current.getHeight());
		LogMessage("swipeLayout.getHeight(): " + swipeLayout.getHeight());

		if(current.getHeight() == 0 || swipeLayout.getHeight() == 0)
			return;

		webView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
		int heightMH = webView.getMeasuredHeight();

		LogMessage("webView measured height: " + heightMH);

		float curdensity =  weeWXApp.getDensity();

		int heightPx = Math.round(current.getHeight() / curdensity);

		LogMessage("heightPx: " + heightPx);

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
					LogMessage("value: " + value);

					if(value == null || value.isBlank() || value.equalsIgnoreCase("null"))
					{
						LogMessage("current.evaluateJavascript() value is null " +
						                          "or blank or equals 'null'", KeyValue.v);

						if(attempt == 3)
						{
							current_refreshed = true;
							stopRefreshing();
						}

						return;
					}

					//doStackTrace(0);

					int heightInPx = (int)Float.parseFloat(value.replaceAll("[\"']", ""));
					LogMessage("From Javascript heightInPx: " + heightInPx);

					if(heightInPx == 0)
					{
						LogMessage("heightInPx is 0 skipping...");

						if(attempt == 3)
						{
							current_refreshed = true;
							stopRefreshing();
						}

						return;
					}

//					if((heightInPx > 0 && heightInPx < 200) || heightInPx > 250)
					if(heightInPx > 250)
					{
						LogMessage("heightInPx is out of bounds...");
						//current.post(() -> current.reload());
						//current.post(() -> current.invalidate());
						if(!current.isInLayout())
							current.post(() -> current.requestLayout());

						if(attempt == 3)
						{
							current_refreshed = true;
							stopRefreshing();
						}

						return;
					}

					// Post a Runnable to make sure contentHeight is available
					float density =  weeWXApp.getDensity();
					LogMessage("1DP: " + density);
					int height = Math.round(heightInPx * density);
					LogMessage("height in DP: " + height);

					int contentHeightPx = tv1.getHeight() + tv2.getHeight() + height;
					LogMessage("contentHeightPx: " + contentHeightPx);

					ViewGroup.LayoutParams params = swipeLayout.getLayoutParams();

					if(params.height != contentHeightPx)
					{
						LogMessage("params.height != contentHeightPx");
						params.height = contentHeightPx;
						swipeLayout.post(() -> swipeLayout.setLayoutParams(params));
					}

					current_refreshed = true;
					stopRefreshing();
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

		Instant now = Instant.now();

		if(Math.abs(Duration.between(lastRunRadar, now).toSeconds()) < 5)
		{
			LogMessage("We already ran less than 5s ago, skipping...", KeyValue.d);
			if(!swipeLayout.isRefreshing())
			{
				forecast_refresh = true;
				stopRefreshing();
			}

			return;
		}

		lastRunRadar = now;

		String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
		if(radtype == null || radtype.isBlank())
		{
			LogMessage("radtype: " + radtype);
			String tmp = String.format(weeWXApp.getAndroidString(R.string.radar_type_is_invalid), radtype);
			loadWebViewContent(tmp);
			forecast_refresh = true;
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

			long[] npwsll = weeWXAppCommon.getNPWSLL();
			if(npwsll[1] <= 0)
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

		String lastDownload = (String)KeyValue.readVar("LastDownload", "");
		if(lastDownload == null || lastDownload.isBlank())
		{
			forceCurrentRefresh(R.string.attempting_to_download_data_txt);
			return;
		}

		String[] bits = lastDownload.split("\\|");

		LogMessage("bits.length: " + bits.length, KeyValue.d);

		if(bits.length <= 1)
		{
			LogMessage("bits.length <= 1", true, KeyValue.e);
			forceCurrentRefresh(R.string.error_occurred_while_attempting_to_update);
			return;
		}
		
		if(bits.length < 65)
		{
			LogMessage("bits.length < 65", KeyValue.w);
			forceCurrentRefresh(R.string.error_occurred_while_attempting_to_update);
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

		LogMessage("bits.length: " + bits.length, KeyValue.d);

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
			LogMessage("loadAndShowWebView() url != null && text != null...", true, KeyValue.w);
			return;
		}

		if(text != null)
		{
			wv.post(() ->
			{
				LogMessage("text.length(): " + text.length());
				wv.loadDataWithBaseURL("file:///android_asset/", text, "text/html", "utf-8", null);

				if(isCurrnet)
				{
					wv.postDelayed(() ->
					{
						float density = weeWXApp.getDensity();
						LogMessage("density: " + density);
						LogMessage("current.getHeight(): " + current.getHeight());

						int heightInPx = Math.round(current.getHeight() / density);
						LogMessage("heightInPx: " + heightInPx);

						if(heightInPx == 0)
						{
							LogMessage("heightInPx is 0 skipping...");
							return;
						}

						if(heightInPx < 120 || heightInPx > 250)
						{
							LogMessage("heightInPx is out of bounds " + heightInPx + ", reloading and invalidating...");
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
					LogMessage("density: " + density);
					LogMessage("current.getHeight(): " + current.getHeight());

					int heightInPx = Math.round(current.getHeight() / density);
					LogMessage("heightInPx: " + heightInPx);

					if(heightInPx == 0)
					{
						LogMessage("heightInPx is 0 skipping...");
						return;
					}

					if(heightInPx < 120 || heightInPx > 250)
					{
						LogMessage("heightInPx is out of bounds " + heightInPx + "reloading and invalidating...");
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
					LogMessage("density: " + density);
					LogMessage("current.getHeight(): " + current.getHeight());

					int heightInPx = Math.round(current.getHeight() / density);
					LogMessage("heightInPx: " + heightInPx);

					if(heightInPx == 0)
					{
						LogMessage("heightInPx is 0 skipping...");
						return;
					}

					if(heightInPx < 120 || heightInPx > 250)
					{
						LogMessage("heightInPx is out of bounds " + heightInPx + ", invalidating...");
						current.post(() -> current.invalidate());
					}
				}
			});
		}
	}

	void loadWebViewURL(boolean forced, String url)
	{
		Instant now = Instant.now();

		LogMessage("loadWebViewURL() url: " + url);

		if(forecast == null)
		{
			LogMessage("loadWebViewURL() forecast == null, skipping...", true, KeyValue.w);
			forecast_refresh = true;
			stopRefreshing();
			return;
		}

		String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
		if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.RadarOnHomeScreen ||
		   (radtype != null && radtype.equals("image")))
		{
			LogMessage("loadWebViewURL() radarforecast != weeWXApp.RadarOnHomeScreen or radtype == image...", KeyValue.w);
			forecast_refresh = true;
			stopRefreshing();
			return;
		}

		if(Math.abs(Duration.between(lastRunForecast, now).toSeconds()) < 5)
		{
			LogMessage("loadWebViewURL() ran less than 5s ago, skipping...", KeyValue.d);
			forecast_refresh = true;
			stopRefreshing();
			return;
		}

		long[] npwsll = weeWXAppCommon.getNPWSLL();
		if(!forced && npwsll[1] <= 0)
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

		String html = weeWXApp.current_dialog_html.replace("WARNING_BODY", weeWXApp.getAndroidString(resId));

		loadAndShowWebView(forecast, html, null, false);
	}

	void loadWebViewContent(String text)
	{
		if(forecast == null)
		{
			forecast_refresh = true;
			stopRefreshing();
			return;
		}

		LogMessage("Weather.loadWebviewContent()");

		loadAndShowWebView(forecast, text, null, false);
	}

	void forceCurrentRefresh(int resId)
	{
		forceCurrentRefresh(weeWXApp.getAndroidString(resId));
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
		LogMessage("forceCurrentRefresh() calling loadAndShowWebView()");
	}

	private void forceRefresh()
	{
		LogMessage("forceRefresh()");

		if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) == weeWXApp.RadarOnHomeScreen)
		{
			String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
			if(radtype != null && radtype.equals("image"))
			{
				weeWXAppCommon.getRadarImage(true, false, false, false);
			} else {
				String radarURL = (String)KeyValue.readVar("RADAR_URL", "");
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
			LogMessage("Let's force download fresh forecast data...");
			weeWXAppCommon.getForecast(true, false, false);
		}

		LogMessage("Let's force download fresh weather data...");
		weeWXAppCommon.getWeather(true, false, false);
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
				String tmp = String.format(weeWXApp.getAndroidString(R.string.radar_type_is_invalid), radtype);
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

			String fctype = (String)KeyValue.readVar("fctype", "");
			if(fctype == null || fctype.isBlank())
			{
				LogMessage("Weather.loadWebView() forecast type is invalid: " + fctype, true, KeyValue.w);
				String finalErrorStr = String.format(weeWXApp.getAndroidString(R.string.forecast_type_is_invalid), fctype);
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
					loadWebViewContent(weeWXApp.getAndroidString(R.string.failed_to_process_forecast_data));

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
					loadWebViewContent(weeWXApp.getAndroidString(R.string.failed_to_process_forecast_data));

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
					loadWebViewContent(weeWXApp.getAndroidString(R.string.failed_to_process_forecast_data));

				KeyValue.putVar("LastForecastError", null);
				return;
			}

			String logo = "file:///android_asset/logos/";
			switch(fctype.toLowerCase(Locale.ENGLISH))
			{
				case "aemet.es" -> logo += "aemet.png";
				case "bom2", "bom3" -> logo += "bom" + extSVG;
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
					String finalErrorStr = String.format(weeWXApp.getAndroidString(R.string.forecast_type_is_invalid), fctype);
					loadWebViewContent(finalErrorStr);
					return;
				}
			}

			sb.append("<div style='text-align:center'><img src='")
					.append(logo)
					.append("' height='45px' />\n</div>\n")
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

		if(!forecast_refresh || !current_refreshed)
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
				                   weeWXApp.getAndroidString(R.string.unknown_error_occurred) +
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

		if(weeWXAppCommon.getRadarImage(false, false, false, false) != null)
		{
			LogMessage("Weather.loadOrReloadRadarImage() done downloading radar.gif, prompt to show");
			loadWebView();
			return;
		}

		html = weeWXApp.current_html_headers + weeWXApp.html_header_rest +
		       weeWXApp.getAndroidString(R.string.radar_still_downloading) +
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

		if(KeyValue.isPrefSet("radarforecast") &&
		   current_refreshed && forecast_refresh)
		{
			current_refreshed = false;
			forecast_refresh = false;

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
		{
			current_refreshed = false;
			drawWeather();
		}

		if(str.equals(weeWXAppCommon.STOP_WEATHER_INTENT))
		{
			current_refreshed = true;
			stopRefreshing();
		}

		boolean radarforecast = (boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default);

		if(radarforecast == weeWXApp.RadarOnHomeScreen && radtype.equals("image"))
		{
			if(str.equals(weeWXAppCommon.REFRESH_RADAR_INTENT))
			{
				forecast_refresh = false;
				drawRadar();
			}

			if(str.equals(weeWXAppCommon.STOP_RADAR_INTENT))
			{
				forecast_refresh = true;
				stopRefreshing();
			}
		}

		if(radarforecast == weeWXApp.ForecastOnHomeScreen)
		{
			if(str.equals(weeWXAppCommon.REFRESH_FORECAST_INTENT))
			{
				LogMessage("Weather.notificationObserver running reloadForecast()");
				forecast_refresh = false;
				reloadForecast();
			}

			if(str.equals(weeWXAppCommon.STOP_FORECAST_INTENT))
			{
				LogMessage("Weather.notificationObserver running stopRefreshing()");
				forecast_refresh = true;
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