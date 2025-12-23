package com.odiousapps.weewxweather;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
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
	private SafeWebView forecastWebView, radarWebView;
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
                        !floatingCheckBox.isChecked() && radarWebView.getScrollY() == 0);

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


			String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
			if(radtype != null && radtype.equals("image"))
				weeWXAppCommon.getRadarImage(true, false);
			else
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

		weeWXAppCommon.LogMessage("Forecast.java -- adding notification manager...");
		weeWXAppCommon.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);

		if(forecastWebView == null)
			forecastWebView = WebViewPreloader.getInstance().getWebView(requireContext());

		if(forecastWebView.getParent() != null)
			((ViewGroup)forecastWebView.getParent()).removeView(forecastWebView);

		if(forecastWebView != null)
			forecastWebView.getViewTreeObserver().addOnScrollChangedListener(forecastScrollListener);

		if(radarWebView != null)
			radarWebView.getViewTreeObserver().addOnScrollChangedListener(radarScrollListener);

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

		forecastWebView.setOnPageFinishedListener((v, url) ->
		{
			weeWXAppCommon.LogMessage("forecastWebView.onPageFinished()");
			stopRefreshing();
		});

		radarWebView.setOnPageFinishedListener((v, url) ->
		{
			weeWXAppCommon.LogMessage("radarWebView.onPageFinished()");
			stopRefreshing();
		});

		if(weeWXAppCommon.isPrefSet("radarforecast"))
		{
			boolean disableSwipeOnRadar = (boolean)KeyValue.readVar("disableSwipeOnRadar", weeWXApp.disableSwipeOnRadar_default);
			floatingCheckBox.setChecked(disableSwipeOnRadar);

			updateSwipe();
			updateScreen(true);

			if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) == weeWXApp.RadarOnForecastScreen)
				loadRadar(false);
			else
				getForecast();
		}
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		weeWXAppCommon.NotificationManager.getNotificationLiveData().removeObservers(getViewLifecycleOwner());

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

	private void setMode()
	{
		boolean fdm = (boolean)KeyValue.readVar("force_dark_mode", weeWXApp.force_dark_mode_default);
		boolean darkmode = (int)KeyValue.readVar("mode", weeWXApp.mode_default) == 1;

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
						WebSettingsCompat.setAlgorithmicDarkeningAllowed(radarWebView.getSettings(), darkmode);
					} catch(Exception e) {
						weeWXAppCommon.doStackOutput(e);
					}
				}
			} else {
				if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY))
				{
					WebSettingsCompat.setForceDarkStrategy(radarWebView.getSettings(),
							WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY);

					if(WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
						WebSettingsCompat.setAlgorithmicDarkeningAllowed(radarWebView.getSettings(), darkmode);
				}

				int mode = WebSettingsCompat.FORCE_DARK_OFF;
				if(darkmode)
					mode = WebSettingsCompat.FORCE_DARK_ON;

				WebSettingsCompat.setForceDark(radarWebView.getSettings(), mode);
			}
		}
	}

	private void stopRefreshing()
	{
		if(swipeLayout1.isRefreshing())
			swipeLayout1.post(() -> swipeLayout1.setRefreshing(false));

		if(swipeLayout2.isRefreshing())
			swipeLayout2.post(() -> swipeLayout2.setRefreshing(false));
	}

	private void loadRadar(boolean forced)
	{
		weeWXAppCommon.LogMessage("Forecast.java loadRadar()");

		if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.RadarOnForecastScreen)
			return;

		String radarURL = (String)KeyValue.readVar("RADAR_URL", weeWXApp.RADAR_URL_default);
		if(radarURL == null || radarURL.isBlank())
		{
			failedRadarWebViewDownload(R.string.radar_url_not_set);
			return;
		}

		String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
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
			              weeWXApp.html_header_rest +
			              "\n\t<img class='radarImage' alt='Radar Image' src='" + radar + "'>\n" +
			              weeWXApp.html_footer;

			radarWebView.post(() -> radarWebView.loadDataWithBaseURL(null, html,
					"text/html", "utf-8", null));
			stopRefreshing();
			return;
		}

		long[] npwsll = weeWXAppCommon.getNPWSLL();
		if(!forced && npwsll[1] <= 0)
		{
			weeWXAppCommon.LogMessage("Manual updating set, don't autoload the radar webpage...", KeyValue.w);
			failedRadarWebViewDownload(R.string.manual_update_set_refresh_screen_to_load);
			stopRefreshing();
			return;
		}

		weeWXAppCommon.LogMessage("Loading radar page... url: " + radarURL);
		radarWebView.post(() -> radarWebView.loadUrl(radarURL));

		stopRefreshing();
	}

	private void failedRadarWebViewDownload(int resId)
	{
		String html = weeWXApp.current_dialog_html.replaceAll("WARNING_BODY", weeWXApp.getAndroidString(resId));

		radarWebView.post(() -> radarWebView.loadDataWithBaseURL(null, html,
				"text/html", "utf-8", null));

		stopRefreshing();
	}

	private void failedRadarWebViewDownload(String str)
	{
		final String html = weeWXApp.current_html_headers + weeWXApp.html_header_rest + str + weeWXApp.html_footer;

		radarWebView.post(() -> radarWebView.loadDataWithBaseURL(null, html,
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

		boolean disableSwipeOnRadar = (boolean)KeyValue.readVar("disableSwipeOnRadar", weeWXApp.disableSwipeOnRadar_default);
		floatingCheckBox.setChecked(disableSwipeOnRadar);

		updateScreen(false);
		updateSwipe();
	}

	public void onPause()
	{
		super.onPause();

		weeWXAppCommon.LogMessage("Forecast.java onPause()");

		if(!isVisible)
			return;

		isVisible = false;

		weeWXAppCommon.LogMessage("Enabling swipe between screens...");
		if(!swipeLayout1.isEnabled())
			swipeLayout1.post(() -> swipeLayout1.setEnabled(true));
		if(!swipeLayout2.isEnabled())
			swipeLayout2.post(() -> swipeLayout2.setEnabled(true));
		activity.setUserInputPager(true);

		stopRefreshing();
	}

	private final Observer<String> notificationObserver = str ->
	{
		weeWXAppCommon.LogMessage("Forecast.java notificationObserver: " + str);

		String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
		if(radtype == null)
			radtype = "";

		if(str.equals(weeWXAppCommon.REFRESH_FORECAST_INTENT))
			getForecast();

		if(str.equals(weeWXAppCommon.REFRESH_RADAR_INTENT))
			loadRadar(false);

		if(str.equals(weeWXAppCommon.REFRESH_DARKMODE_INTENT))
			setMode();

		if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) == weeWXApp.RadarOnForecastScreen &&
		   str.equals(weeWXAppCommon.STOP_RADAR_INTENT) && radtype.equals("image"))
			stopRefreshing();

		if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) == weeWXApp.ForecastOnForecastScreen &&
		   str.equals(weeWXAppCommon.STOP_FORECAST_INTENT))
			stopRefreshing();

		if(str.equals(weeWXAppCommon.EXIT_INTENT))
			onPause();
	};

	private void updateScreen(boolean setRefreshing)
	{
		weeWXAppCommon.LogMessage("Forecast.java updateScreen()");

		if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) == weeWXApp.ForecastOnForecastScreen)
		{
			weeWXAppCommon.LogMessage("Displaying forecastWebView...");
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
				swipeLayout1.setBackgroundColor((int)KeyValue.readVar("bgColour", weeWXApp.bgColour_default));
				if(swipeLayout1.isRefreshing() != setRefreshing)
					swipeLayout1.setRefreshing(setRefreshing);
				swipeLayout1.setEnabled(true);
			});
		} else {
			weeWXAppCommon.LogMessage("Displaying radar framelayout");
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
				if(swipeLayout2.isRefreshing() != setRefreshing)
					swipeLayout2.setRefreshing(setRefreshing);
				swipeLayout2.setEnabled(!floatingCheckBox.isChecked());
			});

			boolean rotate = false;

			String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
			if(radtype == null || radtype.isBlank())
			{
				if(rl.getAngle() != 0)
					rl.post(() -> rl.setAngle(0));

				if(floatingCheckBox.getVisibility() != View.GONE)
					floatingCheckBox.post(() -> floatingCheckBox.setVisibility(View.GONE));

				String tmp = String.format(weeWXApp.getAndroidString(R.string.radar_type_is_invalid), radtype);
				failedRadarWebViewDownload(tmp);
				return;
			}

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
			} else if(radtype.equals("webpage")) {
				weeWXAppCommon.LogMessage("Showing the floating checkbox...");
				if(floatingCheckBox.getVisibility() != View.VISIBLE)
					floatingCheckBox.post(() -> floatingCheckBox.setVisibility(View.VISIBLE));
			} else {
				if(rl.getAngle() != 0)
					rl.post(() -> rl.setAngle(0));

				if(floatingCheckBox.getVisibility() != View.GONE)
					floatingCheckBox.post(() -> floatingCheckBox.setVisibility(View.GONE));

				String tmp = String.format(weeWXApp.getAndroidString(R.string.radar_type_is_invalid), radtype);
				failedRadarWebViewDownload(tmp);
				return;
			}

			if(!rotate)
			{
				weeWXAppCommon.LogMessage("Set RotatedLayout angle to 0...");
				if(rl.getAngle() != 0)
					rl.post(() -> rl.setAngle(0));
			}
		}

		weeWXAppCommon.LogMessage("Forecast.updateScreen() finished...");
	}

	private void getForecast()
	{
		if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.ForecastOnForecastScreen)
			return;

		boolean ret = weeWXAppCommon.getForecast(false, false);
		if(!ret)
		{
			String LastForecastError = (String)KeyValue.readVar("LastForecastError", "");
			if(LastForecastError != null && !LastForecastError.isBlank())
			{
				weeWXAppCommon.LogMessage("getForecast returned the following error: " + LastForecastError, KeyValue.w);
				showTextFC(LastForecastError);
			} else {
				weeWXAppCommon.LogMessage("getForecast returned an unknown error...", KeyValue.w);
				showTextFC(weeWXApp.getAndroidString(R.string.unknown_error_occurred));
			}
		}

		weeWXAppCommon.LogMessage("getForecast returned some content...");
		generateForecast((String)KeyValue.readVar("forecastData", ""));
	}

	private void generateForecast(String forecastData)
	{
		String fctype = (String)KeyValue.readVar("fctype", "");
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
		if(forecastData == null || forecastData.isBlank())
		{
			showTextFC(weeWXApp.getAndroidString(R.string.still_downloading_forecast_data));
			stopRefreshing();
			return;
		}

		if(fctype == null || fctype.isBlank())
		{
			String finalErrorStr = String.format(weeWXApp.getAndroidString(R.string.forecast_type_is_invalid), fctype);
			showTextFC(finalErrorStr);
			stopRefreshing();
			return;
		}

		switch(fctype.toLowerCase(Locale.ENGLISH))
		{
			case "yahoo" ->
			{
				String[] content = weeWXAppCommon.processYahoo(forecastData, true);
				if(content != null && content.length >= 2)
				{
					updateForecast(fctype, content[0], content[1]);
					return;
				}
			}
			case "weatherzone" ->
			{
				String[] content = weeWXAppCommon.processWZ(forecastData, true);
				if(content != null && content.length >= 2)
				{
					updateForecast(fctype, content[0], content[1]);
					return;
				}
			}
			case "met.no" ->
			{
				String[] content = weeWXAppCommon.processMetNO(forecastData, true);
				if(content != null && content.length >= 2)
				{
					updateForecast(fctype, content[0], content[1]);
					return;
				}
			}
			case "wmo.int" ->
			{
				String[] content = weeWXAppCommon.processWMO(forecastData, true);
				if(content != null && content.length >= 2)
				{
					updateForecast(fctype, content[0], content[1]);
					return;
				}
			}
			case "weather.gov" ->
			{
				String[] content = weeWXAppCommon.processWGOV(forecastData, true);
				if(content != null && content.length >= 2)
				{
					updateForecast(fctype, content[0], content[1]);
					return;
				}
			}
			case "weather.gc.ca" ->
			{
				String[] content = weeWXAppCommon.processWCA(forecastData, true);
				if(content != null && content.length >= 2)
				{
					updateForecast(fctype, content[0], content[1]);
					return;
				}
			}
			case "weather.gc.ca-fr" ->
			{
				String[] content = weeWXAppCommon.processWCAF(forecastData, true);
				if(content != null && content.length >= 2)
				{
					updateForecast(fctype, content[0], content[1]);
					return;
				}
			}
			case "metoffice.gov.uk" ->
			{
				String[] content = weeWXAppCommon.processMET(forecastData, true);
				if(content != null && content.length >= 2)
				{
					updateForecast(fctype, content[0], content[1]);
					return;
				}
			}
			case "bom2" ->
			{
				String[] content = weeWXAppCommon.processBOM2(forecastData, true);
				if(content != null && content.length >= 2)
				{
					updateForecast(fctype, content[0], content[1]);
					return;
				}
			}
			case "bom3" ->
			{
				String[] content = weeWXAppCommon.processBOM3(forecastData, true);
				if(content != null && content.length >= 2)
				{
					updateForecast(fctype, content[0], content[1]);
					return;
				}
			}
			case "aemet.es" ->
			{
				String[] content = weeWXAppCommon.processAEMET(forecastData, true);
				if(content != null && content.length >= 2)
				{
					updateForecast(fctype, content[0], content[1]);
					return;
				}
			}
			case "dwd.de" ->
			{
				String[] content = weeWXAppCommon.processDWD(forecastData, true);
				if(content != null && content.length >= 2)
				{
					updateForecast(fctype, content[0], content[1]);
					return;
				}
			}
			case "metservice.com" ->
			{
				String[] content = weeWXAppCommon.processMetService(forecastData, true);
				if(content != null && content.length >= 2)
				{
					updateForecast(fctype, content[0], content[1]);
					return;
				}
			}
			case "openweathermap.org" ->
			{
				String[] content = weeWXAppCommon.processOWM(forecastData, true);
				if(content != null && content.length >= 2)
				{
					updateForecast(fctype, content[0], content[1]);
					return;
				}
			}
			case "weather.com" ->
			{
				String[] content = weeWXAppCommon.processWCOM(forecastData, true);
				if(content != null && content.length >= 2)
				{
					updateForecast(fctype, content[0], content[1]);
					return;
				}
			}
			case "met.ie" ->
			{
				String[] content = weeWXAppCommon.processMETIE(forecastData, true);
				if(content != null && content.length >= 2)
				{
					updateForecast(fctype, content[0], content[1]);
					return;
				}
			}
			case "tempoitalia.it" ->
			{
				String[] content = weeWXAppCommon.processTempoItalia(forecastData, true);
				if(content != null && content.length >= 2)
				{
					updateForecast(fctype, content[0], content[1]);
					return;
				}
			}
			default ->
			{
				String finalErrorStr = String.format(weeWXApp.getAndroidString(R.string.forecast_type_is_invalid), fctype);
				showTextFC(finalErrorStr);
				stopRefreshing();
				return;
			}
		}

		showTextFC(weeWXApp.getAndroidString(R.string.still_downloading_forecast_data));
		stopRefreshing();
	}

	private void showTextFC(String text)
	{
		if(text == null || text.isBlank())
			text = weeWXApp.getAndroidString(R.string.forecast_url_not_set);

		String html = weeWXApp.current_html_headers +
		              weeWXApp.html_header_rest + text +
		              weeWXApp.html_footer;

		forecastWebView.post(() -> forecastWebView.loadDataWithBaseURL(null, html,
				"text/html", "utf-8", null));
	}

	private void updateForecast(String fctype, String bits, String desc)
	{
		if(fctype == null || fctype.isBlank())
			return;

		String fc = weeWXApp.current_html_headers +
		            weeWXApp.script_header +
		            weeWXApp.html_header_rest +
		            weeWXApp.inline_arrow +
		            bits;

		if(weeWXAppCommon.web_debug_on)
			fc += weeWXApp.debug_html;

		fc += weeWXApp.html_footer;

		if(weeWXAppCommon.debug_html)
			CustomDebug.writeOutput(requireContext(), "forecast", fc, isVisible(), requireActivity());


		String finalfc = fc;

		forecastWebView.post(() -> forecastWebView.loadDataWithBaseURL("file:///android_asset/", finalfc,
					"text/html", "utf-8", null));

		TextView tv1 = rootView.findViewById(R.id.forecast);
		tv1.post(() ->
		{
			tv1.setTextColor((int)KeyValue.readVar("fgColour", weeWXApp.fgColour_default));
			tv1.setBackgroundColor((int)KeyValue.readVar("bgColour", weeWXApp.bgColour_default));
			tv1.setText(desc);
		});

		String ext = "_light.svg";
		if((int)KeyValue.readVar("theme", weeWXApp.theme_default) != R.style.AppTheme_weeWXApp_Dark_Common)
			ext = "_dark.svg";

		String finalext = ext;

		switch(fctype.toLowerCase(Locale.ENGLISH))
		{
			case "yahoo" -> im.post(() -> im.setImageDrawable(weeWXApp.loadSVGFromAssets("logos/yahoo" + finalext)));
			case "weatherzone" -> im.post(() -> im.setImageDrawable(weeWXApp.loadSVGFromAssets("logos/wz" + finalext)));
			case "met.no" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/met_no.png")));
			case "wmo.int" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/wmo.png")));
			case "weather.gov" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/wgov.png")));
			case "weather.gc.ca", "weather.gc.ca-fr" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/wca.png")));
			case "metoffice.gov.uk" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/met.png")));
			case "bom2", "bom3" -> im.post(() -> im.setImageDrawable(weeWXApp.loadSVGFromAssets("logos/bom" + finalext)));
			case "aemet.es" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/aemet.png")));
			case "dwd.de" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/dwd.png")));
			case "metservice.com" -> im.post(() -> im.setImageDrawable(weeWXApp.loadSVGFromAssets("logos/metservice" + finalext)));
			case "meteofrance.com" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/mf.png")));
			case "openweathermap.org" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/owm.png")));
			case "apixu.com" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/apixu.png")));
			case "weather.com" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/weather_com.png")));
			case "met.ie" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/met_ie.png")));
			case "ilmeteo.it" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/ilmeteo_it.png")));
			case "tempoitalia.it" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/tempoitalia_it.png")));
		}
	}

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