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
	private Thread t;
	private boolean disableSwipeOnRadar;
	private boolean disabledSwipe;
	private MainActivity activity;
	private final ViewTreeObserver.OnScrollChangedListener forecastScrollListener = () ->
														swipeLayout1.setEnabled(forecastWebView.getScrollY() == 0);
	private final ViewTreeObserver.OnScrollChangedListener radarScrollListener = () ->
														swipeLayout2.setEnabled(!floatingCheckBox.isChecked() &&
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
		swipeLayout1.setEnabled(true);
		swipeLayout1.setOnRefreshListener(() ->
		{
			swipeLayout1.setRefreshing(true);
			Common.LogMessage("swipeLayout1.onRefresh();");
			getForecast(true);
		});

		swipeLayout2 = rootView.findViewById(R.id.swipeToRefresh2);
		swipeLayout2.setRefreshing(false);
		swipeLayout2.setOnRefreshListener(() ->
		{
			swipeLayout2.setRefreshing(true);
			Common.LogMessage("swipeLayout2.onRefresh();");
			loadRadar(true);
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

		Common.LogMessage("Forecast.java -- adding notification manager...");
		Common.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(),
				notificationObserver);

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
						Common.doStackOutput(e);
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

		loadRadar(false);
		getForecast(false);
		updateScreen(true);
		addListeners();
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

		Common.NotificationManager.getNotificationLiveData().removeObserver(notificationObserver);
		removeListeners();

		if(forecastWebView != null)
		{
			ViewParent parent = forecastWebView.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(forecastWebView);

			forecastWebView.getViewTreeObserver().removeOnScrollChangedListener(forecastScrollListener);

			WebViewPreloader.getInstance().recycleWebView(forecastWebView);

			Common.LogMessage("Stats.onDestroyView() recycled forecastWebView...");
		}

		if(radarWebView != null)
		{
			ViewParent parent = radarWebView.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(radarWebView);

			radarWebView.getViewTreeObserver().removeOnScrollChangedListener(radarScrollListener);

			WebViewPreloader.getInstance().recycleWebView(radarWebView);

			Common.LogMessage("Stats.onDestroyView() recycled radarWebView...");
		}
	}

	private void stopRefreshing()
	{
		if(swipeLayout1.isRefreshing())
			swipeLayout1.post(() -> swipeLayout1.setRefreshing(false));

		if(swipeLayout2.isRefreshing())
			swipeLayout2.post(() -> swipeLayout2.setRefreshing(false));
	}

	private void loadRadar(boolean forceDownload)
	{
		Bitmap bm;
		String radar;
		Common.LogMessage("Forecast.loadRadar()");

		if(Common.GetBoolPref("radarforecast", true))
		{
			Common.LogMessage("This line in loadRadar() shouldn't be hit...");
			return;
		}

		String radarURL = Common.GetStringPref("RADAR_URL", "");
		if(radarURL == null || radarURL.isBlank())
		{
			failedRadarWebViewDownload(R.string.radar_url_not_set, null);
			return;
		}

		String radtype = Common.GetStringPref("radtype", "image");
		if(radtype == null || radtype.isBlank())
		{
			failedRadarWebViewDownload(R.string.radar_url_not_set, null);
			return;
		}

		if(radtype.equals("image"))
		{
			try
			{
				bm = Common.loadOrDownloadImage(radarURL, "radar.gif", forceDownload);
				if(bm == null)
				{
					failedRadarWebViewDownload(R.string.radar_download_failed, radarURL);
					return;
				}

				float sd = weeWXApp.getDensity();
				int height = Math.round((float)Resources.getSystem().getDisplayMetrics().widthPixels / sd * 0.955f);
				int width = Math.round((float)Resources.getSystem().getDisplayMetrics().heightPixels / sd * 0.955f);

				radar = "data:image/jpeg;base64," + Common.toBase64(Common.bitmapToBytes(bm));

				final String html = Common.current_html_headers +
				                    "\n\t<img class='radarImage' alt='Radar Image' src='" + radar + "'>\n" +
				                    Common.html_footer;

				radarWebView.post(() -> radarWebView.loadDataWithBaseURL("file:///android_res/drawable/", html,
							"text/html", "utf-8", null));
				stopRefreshing();
				return;
			} catch(Exception ignored) {}

			failedRadarWebViewDownload(R.string.radar_download_failed, radarURL);
		} else {
			Common.LogMessage("Loading radar page... url: " + radarURL);
			radarWebView.post(() -> radarWebView.loadUrl(radarURL));
			stopRefreshing();
		}

		Common.LogMessage("Forecast.loadRadar() finished...");
	}

	private void failedRadarWebViewDownload(int resId, String url)
	{
		Common.LogMessage("Loading radar image from URL: " + url + " failed...");

		final String html = Common.current_html_headers +
		                    weeWXApp.getAndroidString(resId) +
		                    Common.html_footer;

		radarWebView.post(() -> radarWebView.loadDataWithBaseURL("file:///android_res/drawable/", html,
					"text/html", "utf-8", null));

		stopRefreshing();
	}

	public void onResume()
	{
		super.onResume();

		Common.LogMessage("Forecast.onResume()");

		disableSwipeOnRadar = Common.GetBoolPref("disableSwipeOnRadar", false);
		floatingCheckBox.setChecked(disableSwipeOnRadar);
		updateSwipe();

		if(isVisible)
			return;

		isVisible = true;

		Common.LogMessage("Forecast.onResume() -- updating the value of the floating checkbox...");
		swipeLayout2.post(() -> swipeLayout2.setEnabled(rfl.getVisibility() == View.VISIBLE &&
		                                                !floatingCheckBox.isChecked()));
		updateSwipe();
		updateScreen(false);
	}

	public void onPause()
	{
		super.onPause();

		Common.LogMessage("Forecast.onPause()");

		if(!isVisible)
			return;

		isVisible = false;

		doPause();
		Common.LogMessage("Forecast.onPause()-- removing notification manager...");
	}

	private final Observer<String> notificationObserver = s ->
	{
		Common.LogMessage("notificationObserver == " + s);

		if(s.equals(Common.UPDATE_INTENT) || s.equals(Common.REFRESH_INTENT))
			updateScreen(true);

		if(s.equals(Common.EXIT_INTENT))
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

	void updateListeners()
	{
		removeListeners();
		addListeners();
	}

	private void updateScreen(boolean setRefreshing)
	{
		Common.LogMessage("Forecast.updateScreen()");

		if(Common.GetBoolPref("radarforecast", true))
		{
			Common.LogMessage("Displaying forecastWebView...");
			if(rfl.getVisibility() != View.GONE)
				rfl.post(() -> rfl.setVisibility(View.GONE));

			if(swipeLayout1.getVisibility() != View.VISIBLE)
				swipeLayout1.post(() -> swipeLayout1.setVisibility(View.VISIBLE));

			swipeLayout2.post(() ->
			{
				swipeLayout2.setRefreshing(false);
				swipeLayout2.setEnabled(false);
			});

			swipeLayout1.post(() ->
			{
				swipeLayout1.setBackgroundColor(KeyValue.bgColour);
				swipeLayout1.setRefreshing(setRefreshing);
				swipeLayout1.setEnabled(true);
			});
		} else {
			Common.LogMessage("Displaying radar framelayout");
			if(swipeLayout1.getVisibility() != View.GONE)
				swipeLayout1.post(() -> swipeLayout1.setVisibility(View.GONE));

			if(rfl.getVisibility() != View.VISIBLE)
				rfl.post(() -> rfl.setVisibility(View.VISIBLE));

			swipeLayout1.post(() ->
			{
				if(swipeLayout1.isRefreshing())
					swipeLayout1.setRefreshing(false);
				swipeLayout1.setEnabled(false);
			});

			swipeLayout2.post(() ->
			{
				if(swipeLayout2.isRefreshing())
					swipeLayout2.setRefreshing(setRefreshing);
				swipeLayout2.setEnabled(!floatingCheckBox.isChecked());
			});

			boolean rotate = false;

			String radtype = Common.GetStringPref("radtype", "image");
			if(radtype != null)
			{
				if(radtype.equals("image"))
				{
					Common.LogMessage("Hiding the floating checkbox...");
					if(floatingCheckBox.getVisibility() != View.GONE)
						floatingCheckBox.post(() -> floatingCheckBox.setVisibility(View.GONE));

					Bitmap bmp1 = Common.loadImage("/radar.gif");
					if(bmp1 != null && bmp1.getWidth() > bmp1.getHeight() &&
					   weeWXApp.getHeight() > weeWXApp.getWidth())
					{
						Common.LogMessage("Hide radarWebView, show rotated layout...");
						rotate = true;
						if(rl.getAngle() != -90)
							rl.post(() -> rl.setAngle(-90));
					}
				} else {
					Common.LogMessage("Showing the floating checkbox...");
					//if(floatingCheckBox.getVisibility() != View.VISIBLE)
					floatingCheckBox.post(() -> floatingCheckBox.setVisibility(View.VISIBLE));
				}

				if(!rotate)
				{
					Common.LogMessage("Set RotatedLayout angle to 0...");
					if(rl.getAngle() != 0)
						rl.post(() -> rl.setAngle(0));
				}
			}
		}

		Common.LogMessage("Forecast.updateScreen() finished...");
	}

	private void getForecast(boolean force)
	{
		if(!Common.GetBoolPref("radarforecast", true))
			return;

		final String forecast_url = Common.GetStringPref("FORECAST_URL", "");
		if(forecast_url == null || forecast_url.isBlank())
		{
			final String html = Common.current_html_headers +
			                    "Forecast URL not set. Edit inigo-settings.txt to change." +
			                    Common.html_footer;

			forecastWebView.post(() ->
			{
				WebViewPreloader.wipeCache(forecastWebView);
				forecastWebView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
			});

			stopRefreshing();

			return;
		}

		if(!Common.checkConnection() && !force)
		{
			Common.LogMessage("Not on wifi and not a forced refresh");
			return;
		}

		final String forecastData = Common.GetStringPref("forecastData", "");
		if(forecastData == null)
			return;

		if(!forecastData.isBlank())
			generateForecast();

		if(t != null)
		{
			if(t.isAlive())
				t.interrupt();
			t = null;
		}

		t = new Thread(() ->
		{
			try
			{
				long current_time = Math.round(System.currentTimeMillis() / 1000.0);

				if(forecastData.isBlank() || Common.GetLongPref("rssCheck", 0) +
				                             7190 < current_time || force)
				{
					Common.LogMessage("no forecast data or cache is more than 2 hours old " +
					                  "or was forced");

					String tmp = Common.downloadForecast();

					Common.LogMessage("updating rss cache");
					Common.SetLongPref("rssCheck", current_time);
					Common.SetStringPref("forecastData", tmp);

					generateForecast();
					stopRefreshing();
				}
			} catch(Exception e) {
				Common.doStackOutput(e);
			}
		});

		t.start();
	}

	private void generateForecast()
	{
		if(t != null)
		{
			if(t.isAlive())
				t.interrupt();
			t = null;
		}

		t = new Thread(() ->
		{
			Common.LogMessage("getting json data");
			String data;
			String fctype = Common.GetStringPref("fctype", "Yahoo");
			if(fctype == null)
				return;

			data = Common.GetStringPref("forecastData", "");
			if(data == null)
				return;

			if(data.isBlank())
			{
				final String html = Common.current_html_headers +
				                    weeWXApp.getAndroidString(R.string.forecast_url_not_set) +
				                    Common.html_footer;

				forecastWebView.post(() ->
				{
					WebViewPreloader.wipeCache((forecastWebView));
					forecastWebView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
				});
				stopRefreshing();
				return;
			}

			switch(fctype.toLowerCase(Locale.ENGLISH))
			{
				case "yahoo" ->
				{
					String[] content = Common.processYahoo(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "weatherzone" ->
				{
					String[] content = Common.processWZ(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "yr.no" ->
				{
					String[] content = Common.processYR(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "met.no" ->
				{
					String[] content = Common.processMetNO(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "wmo.int" ->
				{
					String[] content = Common.processWMO(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "weather.gov" ->
				{
					String[] content = Common.processWGOV(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "weather.gc.ca" ->
				{
					String[] content = Common.processWCA(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "weather.gc.ca-fr" ->
				{
					String[] content = Common.processWCAF(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "metoffice.gov.uk" ->
				{
					String[] content = Common.processMET(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "bom2" ->
				{
					String[] content = Common.processBOM2(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "bom3" ->
				{
					String[] content = Common.processBOM3(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "aemet.es" ->
				{
					String[] content = Common.processAEMET(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "dwd.de" ->
				{
					String[] content = Common.processDWD(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "metservice.com" ->
				{
					String[] content = Common.processMetService(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "openweathermap.org" ->
				{
					String[] content = Common.processOWM(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "weather.com" ->
				{
					String[] content = Common.processWCOM(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "met.ie" ->
				{
					String[] content = Common.processMETIE(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "tempoitalia.it" ->
				{
					String[] content = Common.processTempoItalia(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
			}

			stopRefreshing();
		});

		t.start();
	}

	private void updateForecast(final String bits, final String desc)
	{
		final String fc = Common.current_html_headers + bits + Common.html_footer;

		CustomDebug.writeDebug("forecast.html", fc);

		forecastWebView.post(() ->
		{
			WebViewPreloader.wipeCache(forecastWebView);
			forecastWebView.loadDataWithBaseURL("file:///android_res/drawable/", fc,
					"text/html", "utf-8", null);
		});

		TextView tv1 = rootView.findViewById(R.id.forecast);
		tv1.post(() ->
		{
			tv1.setTextColor(KeyValue.fgColour);
			tv1.setBackgroundColor(KeyValue.bgColour);
			tv1.setText(desc);
		});

		String fctype = Common.GetStringPref("fctype", "yahoo");
		if(fctype == null)
			return;

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
					im.post(() -> im.setColorFilter(new ColorMatrixColorFilter(Common.NEGATIVE)));
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
					im.post(() -> im.setColorFilter(new ColorMatrixColorFilter(Common.NEGATIVE)));
				else
					im.post(() -> im.setColorFilter(null));
			}
			case "weather.com" -> im.post(() -> im.setImageResource(R.drawable.weather_com));
			case "met.ie" ->
			{
				im.post(() -> im.setImageResource(R.drawable.met_ie));
				if(KeyValue.theme == R.style.AppTheme_weeWXApp_Dark_Common)
					im.post(() -> im.setColorFilter(new ColorMatrixColorFilter(Common.NEGATIVE)));
				else
					im.post(() -> im.setColorFilter(null));
			}
			case "ilmeteo.it" -> im.post(() -> im.setImageResource(R.drawable.ilmeteo_it));
			case "tempoitalia.it" -> im.post(() -> im.setImageResource(R.drawable.tempoitalia_it));
		}

		//stopRefreshing();
	}

	void doPause()
	{
		if(disabledSwipe)
		{
			disabledSwipe = false;
			Common.LogMessage("Enabling swipe between screens...");
			activity.setUserInputPager(true);
		}
	}

	@SuppressWarnings("ConstantConditions")
	public void updateSwipe()
	{
		if(!isVisible)
			return;

		if((disabledSwipe && disableSwipeOnRadar) || (!disabledSwipe && !disableSwipeOnRadar))
			return;

		if(!disabledSwipe && disableSwipeOnRadar && rfl.getVisibility() == View.VISIBLE)
		{
			disabledSwipe = true;
			Common.LogMessage("Disabling swipe between screens...");
			activity.setUserInputPager(false);
			return;
		}

		if(disabledSwipe && !disableSwipeOnRadar)
		{
			disabledSwipe = false;
			Common.LogMessage("Enabling swipe between screens...");
			activity.setUserInputPager(true);
		}
	}

	@Override
	public void onClick(View view)
	{
		disableSwipeOnRadar = floatingCheckBox.isChecked();
		Common.SetBoolPref("disableSwipeOnRadar", disableSwipeOnRadar);
		swipeLayout2.post(() -> swipeLayout2.setEnabled(rfl.getVisibility() == View.VISIBLE &&
		                                                !floatingCheckBox.isChecked()));
		updateListeners();
		updateSwipe();
		Common.LogMessage("Forecast.onClick() finished...");
	}
}