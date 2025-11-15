package com.odiousapps.weewxweather;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ColorMatrixColorFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

@SuppressWarnings({"SameParameterValue", "unused", "deprecation"})
public class Forecast extends Fragment implements View.OnClickListener
{
	private View rootView;
	private WebView forecastWebView, radarWebView;
	private SwipeRefreshLayout swipeLayout1, swipeLayout2;
	private ImageView im;
	private FrameLayout rfl;
	private RotateLayout rl;
	private MaterialCheckBox floatingCheckBox;
	private boolean isVisible = false;
	private MainActivity activity;
	private final ViewTreeObserver.OnScrollChangedListener forecastScrollListener = () ->
														swipeLayout1.setEnabled(floatingCheckBox.getVisibility() != View.VISIBLE &&
																				forecastWebView.getScrollY() == 0);
	private final ViewTreeObserver.OnScrollChangedListener radarScrollListener = () ->
														swipeLayout2.setEnabled(floatingCheckBox.getVisibility() == View.VISIBLE &&
														                        !floatingCheckBox.isChecked() &&
														                        radarWebView.getScrollY() == 0);

	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		activity = (MainActivity)getActivity();

		rootView = inflater.inflate(R.layout.fragment_forecast, container, false);

		rfl = rootView.findViewById(R.id.radar_layout);
		rl = rootView.findViewById(R.id.rotateLayout);

		swipeLayout1 = rootView.findViewById(R.id.swipeToRefresh1);
		swipeLayout1.setOnRefreshListener(() ->
		{
			swipeLayout1.setRefreshing(true);
			weeWXAppCommon.LogMessage("swipeLayout1.onRefresh();");
			weeWXAppCommon.getForecast(true, false);
		});

		swipeLayout2 = rootView.findViewById(R.id.swipeToRefresh2);
		swipeLayout2.setOnRefreshListener(() ->
		{
			swipeLayout2.setRefreshing(true);
			weeWXAppCommon.LogMessage("swipeLayout2.onRefresh();");


			String radtype = weeWXAppCommon.GetStringPref("radtype", weeWXApp.radtype_default);
			if(radtype != null && radtype.equals("image"))
				weeWXAppCommon.getRadarImage(true, false);
			else
				loadRadar();
		});

		im = rootView.findViewById(R.id.logo);

		floatingCheckBox = rootView.findViewById(R.id.floatingCheckBox);
		floatingCheckBox.setOnClickListener(this);

		return rootView;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		weeWXAppCommon.LogMessage("Forecast.java -- adding notification manager...");
		weeWXAppCommon.NotificationManager.getNotificationLiveData().removeObservers(getViewLifecycleOwner());
		weeWXAppCommon.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);

		if(forecastWebView == null)
			forecastWebView = WebViewPreloader.getInstance().getWebView(requireContext());

		if(forecastWebView.getParent() != null)
			((ViewGroup)forecastWebView.getParent()).removeView(forecastWebView);

		FrameLayout forecastFL = rootView.findViewById(R.id.forecastWebView);
		forecastFL.removeAllViews();
		forecastFL.addView(forecastWebView);

		if(radarWebView == null)
			radarWebView = WebViewPreloader.getInstance().getWebView(requireContext());

		if(radarWebView.getParent() != null)
			((ViewGroup)radarWebView.getParent()).removeView(radarWebView);

		FrameLayout fl = rootView.findViewById(R.id.radarFrameLayout);
		fl.removeAllViews();
		fl.addView(radarWebView);

		forecastWebView.setWebViewClient(new WebViewClient()
		{
			@Override
			public void onPageFinished(WebView view, String url)
			{
				super.onPageFinished(view, url);
				if(swipeLayout1.getVisibility() == View.VISIBLE)
					stopRefreshing();
			}
		});

		radarWebView.setWebViewClient(new WebViewClient()
		{
			@Override
			public void onPageFinished(WebView view, String url)
			{
				super.onPageFinished(view, url);
				if(swipeLayout1.getVisibility() != View.VISIBLE)
					stopRefreshing();
			}
		});

		if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
		{
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2)
			{
				if(WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
				{
					try
					{
						WebSettingsCompat.setAlgorithmicDarkeningAllowed(radarWebView.getSettings(),true);
					} catch(Exception e) {
						weeWXAppCommon.doStackOutput(e);
					}
				}
			} else {
				if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY))
				{
					WebSettingsCompat.setForceDarkStrategy(forecastWebView.getSettings(),
							WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY);

					if(WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
						WebSettingsCompat.setAlgorithmicDarkeningAllowed(forecastWebView.getSettings(),
								true);

					WebSettingsCompat.setForceDarkStrategy(radarWebView.getSettings(),
							WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY);

					if(WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
						WebSettingsCompat.setAlgorithmicDarkeningAllowed(radarWebView.getSettings(),
								true);
				}

				int mode = WebSettingsCompat.FORCE_DARK_OFF;

				if(KeyValue.mode == 1)
					mode = WebSettingsCompat.FORCE_DARK_ON;

				WebSettingsCompat.setForceDark(forecastWebView.getSettings(), mode);
				WebSettingsCompat.setForceDark(radarWebView.getSettings(), mode);
			}
		}

		if(savedInstanceState != null)
		{
			forecastWebView.restoreState(savedInstanceState);
			radarWebView.restoreState(savedInstanceState);
		}

		if(weeWXAppCommon.isPrefSet("radarforecast"))
		{
			if(weeWXAppCommon.GetBoolPref("radarforecast", weeWXApp.radarforecast_default) == weeWXApp.RadarOnForecastScreen)
				loadRadar();
			else
				getForecast();

			boolean disableSwipeOnRadar = weeWXAppCommon.GetBoolPref("disableSwipeOnRadar", weeWXApp.disableSwipeOnRadar_default);
			floatingCheckBox.setChecked(disableSwipeOnRadar);

			updateSwipe();
			updateScreen(true);

			addListeners();
		}

		stopRefreshing();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState)
	{
		super.onSaveInstanceState(outState);

		if(forecastWebView != null)
			forecastWebView.saveState(outState);

		if(radarWebView != null)
			radarWebView.saveState(outState);
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		removeListeners();

		if(forecastWebView != null)
		{
			ViewParent parent = forecastWebView.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(forecastWebView);

			forecastWebView.getViewTreeObserver().removeOnScrollChangedListener(forecastScrollListener);

			WebViewPreloader.getInstance().recycleWebView(forecastWebView);

			weeWXAppCommon.LogMessage("Forecast.onDestroyView() recycled forecastWebView...");
		}

		if(radarWebView != null)
		{
			ViewParent parent = radarWebView.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(radarWebView);

			radarWebView.getViewTreeObserver().removeOnScrollChangedListener(radarScrollListener);

			WebViewPreloader.getInstance().recycleWebView(radarWebView);

			weeWXAppCommon.LogMessage("Forecast.onDestroyView() recycled radarWebView...");
		}
	}

	private void stopRefreshing()
	{
		if(swipeLayout1.isRefreshing())
			swipeLayout1.post(() -> swipeLayout1.setRefreshing(false));

		if(swipeLayout2.isRefreshing())
			swipeLayout2.post(() -> swipeLayout2.setRefreshing(false));
	}

	private void loadRadar()
	{
		weeWXAppCommon.LogMessage("Forecast.java loadRadar()");

		if(weeWXAppCommon.GetBoolPref("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.RadarOnForecastScreen)
			return;

		String radarURL = weeWXAppCommon.GetStringPref("RADAR_URL", weeWXApp.RADAR_URL_default);
		if(radarURL == null || radarURL.isBlank())
		{
			failedRadarWebViewDownload(R.string.radar_url_not_set);
			return;
		}

		String radtype = weeWXAppCommon.GetStringPref("radtype", weeWXApp.radtype_default);
		if(radtype == null || radtype.isBlank())
		{
			String tmp = String.format(weeWXApp.getAndroidString(R.string.radar_type_is_invalid), radtype);
			failedRadarWebViewDownload(tmp);
			return;
		}

		if(radtype.equals("image"))
		{
			Bitmap bm = weeWXAppCommon.getRadarImage(false, false);
			if(bm == null)
			{
				failedRadarWebViewDownload(R.string.radar_download_failed);
				stopRefreshing();
				return;
			}

			float sd = weeWXApp.getDensity();
			int height = Math.round((float)Resources.getSystem().getDisplayMetrics().widthPixels / sd * 0.955f);
			int width = Math.round((float)Resources.getSystem().getDisplayMetrics().heightPixels / sd * 0.955f);

			weeWXAppCommon.LogMessage("Loading radar image... url: " + radarURL);
			String radar = "data:image/jpeg;base64," + weeWXAppCommon.toBase64(weeWXAppCommon.bitmapToBytes(bm));

			String html = weeWXApp.current_html_headers +
		                       "\n\t<img class='radarImage' alt='Radar Image' src='" + radar + "'>\n" +
			                    weeWXApp.html_footer;

			radarWebView.post(() -> radarWebView.loadDataWithBaseURL("file:///android_res/", html,
						"text/html", "utf-8", null));
		} else {
			weeWXAppCommon.LogMessage("Loading radar page... url: " + radarURL);
			radarWebView.post(() -> radarWebView.loadUrl(radarURL));
		}

		stopRefreshing();
	}

	private void failedRadarWebViewDownload(int resId)
	{
		final String html = weeWXApp.current_html_headers +
		                    weeWXApp.getAndroidString(resId) +
		                    weeWXApp.html_footer;

		radarWebView.post(() -> radarWebView.loadDataWithBaseURL("file:///android_res/", html,
				"text/html", "utf-8", null));

		stopRefreshing();
	}

	private void failedRadarWebViewDownload(String str)
	{
		final String html = weeWXApp.current_html_headers + str + weeWXApp.html_footer;

		radarWebView.post(() -> radarWebView.loadDataWithBaseURL("file:///android_res/", html,
				"text/html", "utf-8", null));

		stopRefreshing();
	}

	public void onResume()
	{
		super.onResume();

		weeWXAppCommon.LogMessage("Forecast.onResume()");

		if(isVisible)
			return;

		isVisible = true;

		boolean disableSwipeOnRadar = weeWXAppCommon.GetBoolPref("disableSwipeOnRadar", weeWXApp.disableSwipeOnRadar_default);
		floatingCheckBox.setChecked(disableSwipeOnRadar);

		updateScreen(true);
		updateSwipe();
		stopRefreshing();
	}

	public void onPause()
	{
		super.onPause();

		weeWXAppCommon.LogMessage("Forecast.java onPause()");

		if(!isVisible)
			return;

		isVisible = false;

		weeWXAppCommon.LogMessage("Enabling swipe between screens...");
		activity.setUserInputPager(true);
	}

	private final Observer<String> notificationObserver = s ->
	{
		weeWXAppCommon.LogMessage("notificationObserver: " + s);

		if(s.equals(weeWXAppCommon.REFRESH_FORECAST_INTENT))
			getForecast();

		if(s.equals(weeWXAppCommon.REFRESH_RADAR_INTENT))
			loadRadar();

		if(s.equals(weeWXAppCommon.EXIT_INTENT))
			onPause();
	};

	void removeListeners()
	{
		if(forecastWebView != null && forecastWebView.getViewTreeObserver().isAlive())
			forecastWebView.getViewTreeObserver().removeOnScrollChangedListener(forecastScrollListener);

		if(radarWebView != null && radarWebView.getViewTreeObserver().isAlive())
			radarWebView.getViewTreeObserver().removeOnScrollChangedListener(radarScrollListener);
	}

	void addListeners()
	{
		if(forecastWebView != null)
			forecastWebView.getViewTreeObserver().addOnScrollChangedListener(forecastScrollListener);

		if(radarWebView != null)
			radarWebView.getViewTreeObserver().addOnScrollChangedListener(radarScrollListener);
	}

	private void updateScreen(boolean setRefreshing)
	{
		weeWXAppCommon.LogMessage("Forecast.java updateScreen()");

		if(weeWXAppCommon.GetBoolPref("radarforecast", weeWXApp.radarforecast_default) == weeWXApp.ForecastOnForecastScreen)
		{
			weeWXAppCommon.LogMessage("Displaying forecastWebView...");
			if(rfl.getVisibility() != View.GONE)
				rfl.post(() -> rfl.setVisibility(View.GONE));

			if(swipeLayout1.getVisibility() != View.VISIBLE)
				swipeLayout1.post(() -> swipeLayout1.setVisibility(View.VISIBLE));

			if(swipeLayout2.isEnabled())
				swipeLayout2.post(() ->
				{
					swipeLayout2.setRefreshing(false);
					swipeLayout2.setEnabled(false);
				});

			if(!swipeLayout1.isEnabled())
				swipeLayout1.post(() ->
				{
					swipeLayout1.setBackgroundColor(KeyValue.bgColour);
					swipeLayout1.setRefreshing(setRefreshing);
					swipeLayout1.setEnabled(true);
				});
		} else {
			weeWXAppCommon.LogMessage("Displaying radar framelayout");
			if(swipeLayout1.getVisibility() != View.GONE)
				swipeLayout1.post(() -> swipeLayout1.setVisibility(View.GONE));

			if(rfl.getVisibility() != View.VISIBLE)
				rfl.post(() -> rfl.setVisibility(View.VISIBLE));

			if(swipeLayout1.isEnabled())
				swipeLayout1.post(() ->
				{
					if(swipeLayout1.isRefreshing())
						swipeLayout1.setRefreshing(false);
					swipeLayout1.setEnabled(false);
				});

			if(!swipeLayout2.isEnabled())
				swipeLayout2.post(() ->
				{
					if(swipeLayout2.isRefreshing())
						swipeLayout2.setRefreshing(setRefreshing);
					swipeLayout2.setEnabled(!floatingCheckBox.isChecked());
				});

			boolean rotate = false;

			String radtype = weeWXAppCommon.GetStringPref("radtype", weeWXApp.radtype_default);
			if(radtype != null && !radtype.isBlank())
			{
				if(radtype.equals("image"))
				{
					weeWXAppCommon.LogMessage("Hiding the floating checkbox...");
					if(floatingCheckBox.getVisibility() != View.GONE)
						floatingCheckBox.post(() -> floatingCheckBox.setVisibility(View.GONE));

					Bitmap bmp1 = weeWXAppCommon.getImage(weeWXApp.radarFilename);
					if(bmp1 != null && bmp1.getWidth() > bmp1.getHeight() &&
					   weeWXApp.getHeight() > weeWXApp.getWidth())
					{
						weeWXAppCommon.LogMessage("Hide radarWebView, show rotated layout...");
						rotate = true;
						if(rl.getAngle() != -90)
							rl.post(() -> rl.setAngle(-90));
					}
				} else {
					weeWXAppCommon.LogMessage("Showing the floating checkbox...");
					if(floatingCheckBox.getVisibility() != View.VISIBLE)
						floatingCheckBox.post(() -> floatingCheckBox.setVisibility(View.VISIBLE));
				}

				if(!rotate)
				{
					weeWXAppCommon.LogMessage("Set RotatedLayout angle to 0...");
					if(rl.getAngle() != 0)
						rl.post(() -> rl.setAngle(0));
				}
			}
		}

		weeWXAppCommon.LogMessage("Forecast.updateScreen() finished...");
	}

	private void getForecast()
	{
		if(weeWXAppCommon.GetBoolPref("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.ForecastOnForecastScreen)
		{
			weeWXAppCommon.LogMessage("Forecast.java getForecast() this line shouldn't be hit... weeWXApp.ForecastOnForecastScreen is false...");
			stopRefreshing();
			return;
		}

		String[] ret = weeWXAppCommon.getForecast(false, false);
		String forecastData = ret[1];

		if(ret[0].equals("error"))
		{
			if(forecastData != null && !forecastData.isBlank())
			{
				weeWXAppCommon.LogMessage("getForecast returned the following error: " + forecastData);
				showTextFC(forecastData);
			} else {
				weeWXAppCommon.LogMessage("getForecast returned an unknown error...");
				showTextFC(weeWXApp.getAndroidString(R.string.unknown_error_occurred));
			}
		}

		weeWXAppCommon.LogMessage("getForecast returned some content...");
		generateForecast(forecastData);

		stopRefreshing();
	}

	private void generateForecast()
	{
		weeWXAppCommon.LogMessage("Getting forecast forecastData from shared prefs...");
		String forecastData = weeWXAppCommon.GetStringPref("forecastData", weeWXApp.forecastData_default);
		if(forecastData == null || forecastData.isBlank())
		{
			showTextFC("Forecast forecastData is blank...");
			stopRefreshing();
			return;
		}

		generateForecast(forecastData);
	}

	private void generateForecast(String forecastData)
	{
		if(forecastData == null || forecastData.isBlank())
		{
			showTextFC("Forecast data is blank...");
			stopRefreshing();
			return;
		}

		weeWXAppCommon.LogMessage("Getting fctype from shared prefs...");
		String fctype = weeWXAppCommon.GetStringPref("fctype", weeWXApp.fctype_default);
		if(fctype == null || fctype.isBlank())
		{
			showTextFC("Forecast type is blank...");
			stopRefreshing();
			return;
		}

		generateForecast(fctype, forecastData);
	}

	private void generateForecast(String fctype, String forecastData)
	{
		if(fctype == null || fctype.isBlank() || forecastData == null || forecastData.isBlank())
		{
			showTextFC("Forecast type is blank or forecast data is blank...");
			stopRefreshing();
			return;
		}

		switch(fctype.toLowerCase(Locale.ENGLISH))
		{
			case "yahoo" ->
			{
				String[] content = weeWXAppCommon.processYahoo(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
			case "weatherzone" ->
			{
				String[] content = weeWXAppCommon.processWZ(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
			case "yr.no" ->
			{
				String[] content = weeWXAppCommon.processYR(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
			case "met.no" ->
			{
				String[] content = weeWXAppCommon.processMetNO(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
			case "wmo.int" ->
			{
				String[] content = weeWXAppCommon.processWMO(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
			case "weather.gov" ->
			{
				String[] content = weeWXAppCommon.processWGOV(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
			case "weather.gc.ca" ->
			{
				String[] content = weeWXAppCommon.processWCA(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
			case "weather.gc.ca-fr" ->
			{
				String[] content = weeWXAppCommon.processWCAF(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
			case "metoffice.gov.uk" ->
			{
				String[] content = weeWXAppCommon.processMET(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
			case "bom2" ->
			{
				String[] content = weeWXAppCommon.processBOM2(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
			case "bom3" ->
			{
				String[] content = weeWXAppCommon.processBOM3(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
			case "aemet.es" ->
			{
				String[] content = weeWXAppCommon.processAEMET(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
			case "dwd.de" ->
			{
				String[] content = weeWXAppCommon.processDWD(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
			case "metservice.com" ->
			{
				String[] content = weeWXAppCommon.processMetService(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
			case "openweathermap.org" ->
			{
				String[] content = weeWXAppCommon.processOWM(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
			case "weather.com" ->
			{
				String[] content = weeWXAppCommon.processWCOM(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
			case "met.ie" ->
			{
				String[] content = weeWXAppCommon.processMETIE(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
			case "tempoitalia.it" ->
			{
				String[] content = weeWXAppCommon.processTempoItalia(forecastData, true);
				if(content != null && content.length >= 2)
					updateForecast(fctype, content[0], content[1]);
			}
		}
	}

	private void showTextFC(String text)
	{
		if(text == null || text.isBlank())
			text = weeWXApp.getAndroidString(R.string.forecast_url_not_set);

		String html = weeWXApp.current_html_headers + text + weeWXApp.html_footer;

		forecastWebView.post(() -> forecastWebView.loadDataWithBaseURL(null, html,
				"text/html", "utf-8", null));
	}

	private void updateForecast(String fctype, String bits, final String desc)
	{
		if(fctype == null || fctype.isBlank())
			return;

		final String fc = weeWXApp.current_html_headers + bits + weeWXApp.html_footer;

		forecastWebView.post(() -> forecastWebView.loadDataWithBaseURL("file:///android_res/", fc,
					"text/html", "utf-8", null));

		TextView tv1 = rootView.findViewById(R.id.forecast);
		tv1.post(() ->
		{
			tv1.setTextColor(KeyValue.fgColour);
			tv1.setBackgroundColor(KeyValue.bgColour);
			tv1.setText(desc);
		});

		switch(fctype.toLowerCase(Locale.ENGLISH))
		{
			case "yahoo" -> im.post(() -> im.setImageResource(R.drawable.purple));
			case "weatherzone" -> im.post(() -> im.setImageResource(R.drawable.wz));
			case "yr.no" -> im.post(() -> im.setImageResource(R.drawable.yrno));
			case "met.no" -> im.post(() -> im.setImageResource(R.drawable.met_no));
			case "wmo.int" -> im.post(() -> im.setImageResource(R.drawable.wmo));
			case "weather.gov" -> im.post(() -> im.setImageResource(R.drawable.wgov));
			case "weather.gc.ca", "weather.gc.ca-fr" -> im.post(() ->
					im.setImageResource(R.drawable.wca));
			case "metoffice.gov.uk" -> im.post(() -> im.setImageResource(R.drawable.met));
			case "bom2", "bom3" ->
			{
				im.post(() -> im.setImageResource(R.drawable.bom));
				if(KeyValue.theme == R.style.AppTheme_weeWXApp_Dark_Common)
					im.post(() -> im.setColorFilter(new ColorMatrixColorFilter(weeWXAppCommon.NEGATIVE)));
				else
					im.post(() -> im.setColorFilter(null));
			}
			case "aemet.es" -> im.post(() -> im.setImageResource(R.drawable.aemet));
			case "dwd.de" -> im.post(() -> im.setImageResource(R.drawable.dwd));
			case "metservice.com" -> im.post(() -> im.setImageResource(R.drawable.metservice));
			case "meteofrance.com" -> im.post(() -> im.setImageResource(R.drawable.mf));
			case "openweathermap.org" -> im.post(() -> im.setImageResource(R.drawable.owm));
			case "apixu.com" ->
			{
				im.post(() -> im.setImageResource(R.drawable.apixu));
				if(KeyValue.theme == R.style.AppTheme_weeWXApp_Dark_Common)
					im.post(() -> im.setColorFilter(new ColorMatrixColorFilter(weeWXAppCommon.NEGATIVE)));
				else
					im.post(() -> im.setColorFilter(null));
			}
			case "weather.com" -> im.post(() -> im.setImageResource(R.drawable.weather_com));
			case "met.ie" ->
			{
				im.post(() -> im.setImageResource(R.drawable.met_ie));
				if(KeyValue.theme == R.style.AppTheme_weeWXApp_Dark_Common)
					im.post(() -> im.setColorFilter(new ColorMatrixColorFilter(weeWXAppCommon.NEGATIVE)));
				else
					im.post(() -> im.setColorFilter(null));
			}
			case "ilmeteo.it" -> im.post(() -> im.setImageResource(R.drawable.ilmeteo_it));
			case "tempoitalia.it" -> im.post(() -> im.setImageResource(R.drawable.tempoitalia_it));
		}
	}

	@SuppressWarnings("ConstantConditions")
	public void updateSwipe()
	{
		if(rfl.getVisibility() == View.VISIBLE && floatingCheckBox.isChecked() && isVisible())
		{
			weeWXAppCommon.LogMessage("Disabling swipe between screens...");
			activity.setUserInputPager(false);
			swipeLayout2.setEnabled(false);
			return;
		}

		weeWXAppCommon.LogMessage("Enabling swipe between screens...");
		activity.setUserInputPager(true);
		swipeLayout2.setEnabled(true);
	}

	@Override
	public void onClick(View view)
	{
		weeWXAppCommon.SetBoolPref("disableSwipeOnRadar", floatingCheckBox.isChecked());
		updateSwipe();
		weeWXAppCommon.LogMessage("Forecast.onClick() finished...");
	}
}