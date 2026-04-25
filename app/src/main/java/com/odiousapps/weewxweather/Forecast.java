package com.odiousapps.weewxweather;

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

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import static com.odiousapps.weewxweather.weeWXApp.getAndroidString;
import static com.odiousapps.weewxweather.weeWXAppCommon.bitmapToBytes;
import static com.odiousapps.weewxweather.weeWXAppCommon.doStackOutput;
import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;
import static com.odiousapps.weewxweather.weeWXAppCommon.getGsonContent;
import static com.odiousapps.weewxweather.weeWXAppCommon.getImage;
import static com.odiousapps.weewxweather.weeWXAppCommon.is_blank;
import static com.odiousapps.weewxweather.weeWXAppCommon.NPWSLL;
import static com.odiousapps.weewxweather.weeWXAppCommon.getNPWSLL;
import static com.odiousapps.weewxweather.weeWXAppCommon.processUpdateInBG;
import static com.odiousapps.weewxweather.weeWXAppCommon.toBase64;
import static com.odiousapps.weewxweather.weeWXNotificationManager.observeNotifications;
import static com.odiousapps.weewxweather.weeWXNotificationManager.removeNotificationObserver;

@DontObfuscate
@SuppressWarnings("deprecation")
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

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
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
			LogMessage("swipeLayout1.onRefresh();");
			processUpdateInBG(true, false, false, true,
					false, true, false, false);
		});

		swipeLayout2 = rootView.findViewById(R.id.swipeToRefresh2);
		swipeLayout2.setOnRefreshListener(() ->
		{
			swipeLayout2.setRefreshing(true);
			LogMessage("swipeLayout2.onRefresh();");


			String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
			if(radtype != null && radtype.equals("image"))
				processUpdateInBG(true, false, false, true,
						false, false, true, false);
			else
				loadRadar(true);
		});

		im = rootView.findViewById(R.id.logo);

		floatingCheckBox = rootView.findViewById(R.id.floatingCheckBox);
		floatingCheckBox.setOnClickListener(this);

		if(forecastWebView == null)
		{
			forecastWebView = new SafeWebView(weeWXApp.getInstance());
			forecastWebView.getViewTreeObserver().addOnScrollChangedListener(forecastScrollListener);
		}

		FrameLayout forecastFL = rootView.findViewById(R.id.forecastWebView);
		forecastFL.addView(forecastWebView);

		if(radarWebView == null)
		{
			radarWebView = new SafeWebView(weeWXApp.getInstance());
			radarWebView.getViewTreeObserver().addOnScrollChangedListener(radarScrollListener);
		}

		FrameLayout fl = rootView.findViewById(R.id.radarFrameLayout);
		fl.addView(radarWebView);

		forecastWebView.setOnCustomPageFinishedListener((v, url) ->
		{
			LogMessage("forecastWebView.onPageFinished()");
			stopRefreshing();
		}, false);

		radarWebView.setOnCustomPageFinishedListener((v, url) ->
		{
			LogMessage("radarWebView.onPageFinished()");
			stopRefreshing();
		}, false);

		if(KeyValue.isPrefSet("radarforecast"))
		{
			boolean disableSwipeOnRadar = (boolean)KeyValue.readVar("disableSwipeOnRadar", weeWXApp.disableSwipeOnRadar_default);
			floatingCheckBox.setChecked(disableSwipeOnRadar);

			updateSwipe();
			updateScreen();

			if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) == weeWXApp.RadarOnForecastScreen)
				loadRadar(false);
			else
				getForecast();
		}

		observeNotifications(getViewLifecycleOwner(), notificationObserver);

		return rootView;
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		removeNotificationObserver(notificationObserver);

		if(forecastWebView != null)
		{
			ViewParent parent = forecastWebView.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(forecastWebView);

			forecastWebView.getViewTreeObserver().removeOnScrollChangedListener(forecastScrollListener);

			forecastWebView.destroy();

			forecastWebView = null;

			LogMessage("Forecast.onDestroyView() forecastWebView destroyed...");
		}

		if(radarWebView != null)
		{
			ViewParent parent = radarWebView.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(radarWebView);

			radarWebView.getViewTreeObserver().removeOnScrollChangedListener(radarScrollListener);

			radarWebView.destroy();

			radarWebView = null;

			LogMessage("Forecast.onDestroyView() radarWebView destroyed...");
		}
	}

	private void setMode()
	{
		boolean fdm = (boolean)KeyValue.readVar(MainActivity.FORCE_DARK_MODE, weeWXApp.force_dark_mode_default);
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
						doStackOutput(e);
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
		LogMessage("Forecast.java loadRadar()");

		if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.RadarOnForecastScreen)
			return;

		String radarURL = (String)KeyValue.readVar("RADAR_URL", "");
		if(is_blank(radarURL))
		{
			failedRadarWebViewDownload(R.string.radar_url_not_set);
			return;
		}

		String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
		if(is_blank(radtype))
		{
			String tmp = String.format(getAndroidString(R.string.radar_type_is_invalid), radtype);
			failedRadarWebViewDownload(tmp);
			return;
		}

		if(radtype.equals("image"))
		{
			Bitmap bm = getImage(weeWXApp.radarFilename);
			if(bm == null)
			{
				failedRadarWebViewDownload(R.string.radar_download_failed);
				stopRefreshing();
				return;
			}

			LogMessage("Loading radar image... url: " + radarURL);
			String radar = "data:image/jpeg;base64," + toBase64(bitmapToBytes(bm));

			String html = weeWXApp.current_html_headers +
						  weeWXApp.html_header_rest +
						  "\n\t<img class='radarImage' alt='Radar Image' src='" + radar + "'>\n" +
						  weeWXApp.html_footer;

			radarWebView.post(() -> radarWebView.loadDataWithBaseURL(null, html,
					"text/html", "utf-8", null));
			stopRefreshing();
			return;
		}

		NPWSLL npwsll = getNPWSLL();
		if(!forced && npwsll.periodTime() <= 0)
		{
			LogMessage("Manual updating set, don't autoload the radar webpage...", KeyValue.w);
			failedRadarWebViewDownload(R.string.manual_update_set_refresh_screen_to_load);
			stopRefreshing();
			return;
		}

		LogMessage("Loading radar page... url: " + radarURL);
		radarWebView.post(() -> radarWebView.loadUrl(radarURL));

		stopRefreshing();
	}

	private void failedRadarWebViewDownload(int resId)
	{
		String html = weeWXApp.current_dialog_html.replace(weeWXApp.WARNING_BODY, getAndroidString(resId));

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

		LogMessage("Forecast.onResume()");

		if(isVisible)
			return;

		isVisible = true;

		if(KeyValue.isPrefSet("radarforecast"))
		{
			boolean disableSwipeOnRadar = (boolean)KeyValue.readVar("disableSwipeOnRadar", weeWXApp.disableSwipeOnRadar_default);
			floatingCheckBox.setChecked(disableSwipeOnRadar);

			updateSwipe();
			updateScreen();
		}
	}

	public void onPause()
	{
		super.onPause();

		LogMessage("Forecast.java onPause()");

		if(!isVisible)
			return;

		isVisible = false;

		LogMessage("Enabling swipe between screens...");
		if(!swipeLayout1.isEnabled())
			swipeLayout1.post(() -> swipeLayout1.setEnabled(true));

		if(!swipeLayout2.isEnabled())
			swipeLayout2.post(() -> swipeLayout2.setEnabled(true));

		activity.setUserInputPager(true);

		stopRefreshing();
	}

	private final Observer<String> notificationObserver = str ->
	{
		LogMessage("Forecast.java notificationObserver: " + str);

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

	private void updateScreen()
	{
		LogMessage("Forecast.java updateScreen()");

		if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) == weeWXApp.ForecastOnForecastScreen)
		{
			LogMessage("Displaying forecastWebView...");
			if(rfl.getVisibility() != View.GONE)
				rfl.post(() -> rfl.setVisibility(View.GONE));

			if(swipeLayout1.getVisibility() != View.VISIBLE)
				swipeLayout1.post(() -> swipeLayout1.setVisibility(View.VISIBLE));

			swipeLayout2.post(() ->
			{
				if(swipeLayout2.isRefreshing())
					swipeLayout2.setRefreshing(false);
				swipeLayout2.setEnabled(false);
			});

			swipeLayout1.post(() ->
			{
				swipeLayout1.setBackgroundColor(weeWXApp.getColours().bgColour);
				if(swipeLayout1.isRefreshing())
					swipeLayout1.setRefreshing(false);
				swipeLayout1.setEnabled(true);
			});
		} else {
			LogMessage("Displaying radar framelayout");
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
					swipeLayout2.setRefreshing(false);
				swipeLayout2.setEnabled(!floatingCheckBox.isChecked());
			});

			boolean rotate = false;

			String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
			if(is_blank(radtype))
			{
				if(rl.getAngle() != 0)
					rl.post(() -> rl.setAngle(0));

				if(floatingCheckBox.getVisibility() != View.GONE)
					floatingCheckBox.post(() -> floatingCheckBox.setVisibility(View.GONE));

				String tmp = String.format(getAndroidString(R.string.radar_type_is_invalid), radtype);
				failedRadarWebViewDownload(tmp);
				return;
			}

			if(radtype.equals("image"))
			{
				LogMessage("Hiding the floating checkbox...");
				if(floatingCheckBox.getVisibility() != View.GONE)
					floatingCheckBox.post(() -> floatingCheckBox.setVisibility(View.GONE));

				Bitmap bmp1 = getImage(weeWXApp.radarFilename);
				if(bmp1 != null && bmp1.getWidth() > bmp1.getHeight() &&
				   weeWXApp.getHeight() > weeWXApp.getWidth())
				{
					LogMessage("Hide radarWebView, show rotated layout...");
					rotate = true;
					if(rl.getAngle() != -90)
						rl.post(() -> rl.setAngle(-90));
				}
			} else if(radtype.equals("webpage")) {
				LogMessage("Showing the floating checkbox...");
				if(floatingCheckBox.getVisibility() != View.VISIBLE)
					floatingCheckBox.post(() -> floatingCheckBox.setVisibility(View.VISIBLE));
			} else {
				if(rl.getAngle() != 0)
					rl.post(() -> rl.setAngle(0));

				if(floatingCheckBox.getVisibility() != View.GONE)
					floatingCheckBox.post(() -> floatingCheckBox.setVisibility(View.GONE));

				String tmp = String.format(getAndroidString(R.string.radar_type_is_invalid), radtype);
				failedRadarWebViewDownload(tmp);
				return;
			}

			if(!rotate)
			{
				LogMessage("Set RotatedLayout angle to 0...");
				if(rl.getAngle() != 0)
					rl.post(() -> rl.setAngle(0));
			}
		}

		LogMessage("Forecast.updateScreen() finished...");
	}

	private void getForecast()
	{
		if((boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.ForecastOnForecastScreen)
			return;

		LogMessage("Forecast.getForecast() getForecast returned some content...");
		String forecastGson = (String)KeyValue.readVar("forecastGsonEncoded", "");
		boolean hasForecastGson = forecastGson != null && forecastGson.length() > 128;
		if(hasForecastGson)
			LogMessage("forecastGson.length(): " + forecastGson.length());

		if(!hasForecastGson)
		{
			showTextFC(getAndroidString(R.string.still_downloading_forecast_data));
			stopRefreshing();
			return;
		}

		String fctype = (String)KeyValue.readVar(weeWXApp.FCTYPE, "");
		if(is_blank(fctype))
		{
			String finalErrorStr = String.format(getAndroidString(R.string.forecast_type_is_invalid), fctype);
			showTextFC(finalErrorStr);
			stopRefreshing();
			return;
		}

		String[] content = getGsonContent(forecastGson, false);
		if(content[0] != null && content[0].equals("error"))
		{
			if(!is_blank(content[1]))
			{
				showTextFC(content[1]);
			} else {
				showTextFC(getAndroidString(R.string.still_downloading_forecast_data));
			}

			stopRefreshing();
			return;
		}

		updateForecast(fctype, content[1], content.length > 2 ? content[2] : "");
	}

	private void showTextFC(String text)
	{
		if(is_blank(text))
			text = String.format(Locale.getDefault(),getAndroidString(R.string.forecast_url_not_set), "inigo-settings.txt");

		String html = weeWXApp.current_html_headers +
					  weeWXApp.html_header_rest + text +
					  weeWXApp.html_footer;

		forecastWebView.post(() -> forecastWebView.loadDataWithBaseURL(null, html,
				"text/html", "utf-8", null));
	}

	private void updateForecast(String fctype, String bits, String desc)
	{
		if(is_blank(fctype) || is_blank(bits))
			return;

		String fc = weeWXApp.current_html_headers +
					weeWXApp.script_header +
					weeWXApp.html_header_rest +
					weeWXApp.inline_arrow +
					bits;

		if(weeWXAppCommon.web_debug_on)
			fc += weeWXApp.debug_html;

		fc += weeWXApp.html_footer;

//		if(weeWXAppCommon.debug_html)
//			CustomDebug.writeOutput(requireContext(), "forecast", fc, isVisible(), requireActivity());

		String finalfc = fc;

		forecastWebView.post(() -> forecastWebView.loadDataWithBaseURL("file:///android_asset/", finalfc,
					"text/html", "utf-8", null));

		TextView tv1 = rootView.findViewById(R.id.forecast);
		tv1.post(() ->
		{
			tv1.setBackgroundColor(weeWXApp.getColours().bgColour);
			tv1.setTextColor(weeWXApp.getColours().fgColour);
			tv1.setText(desc);
		});

		String extSVG = "_light.svg";
		String extPNG = "_light.png";
		if((int)KeyValue.readVar("theme", weeWXApp.theme_default) != R.style.AppTheme_weeWXApp_Dark_Common)
		{
			extSVG = "_dark.svg";
			extPNG = "_dark.png";
		}

		String finalextSVG = extSVG;
		String finalextPNG = extPNG;

		boolean isDarkTheme = (int)KeyValue.readVar("theme", weeWXApp.theme_default) == R.style.AppTheme_weeWXApp_Dark_Common;

		switch(fctype.toLowerCase(Locale.ENGLISH).strip())
		{
			case "yahoo" -> im.post(() -> im.setImageDrawable(weeWXApp.loadSVGFromAssets("logos/yahoo" + finalextSVG)));
			case "weatherzone", "weatherzone2" -> im.post(() -> im.setImageDrawable(weeWXApp.loadSVGFromAssets("logos/wz" + finalextSVG)));
			case "met.no" -> im.post(() -> {
				im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/met_no.png"));
				if(isDarkTheme)
					im.setColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
				else
					im.clearColorFilter();
			});
			case "wmo.int" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/wmo.png")));
			case "weather.gov" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/wgov.png")));
			case "weather.gc.ca", "weather.gc.ca-fr" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/wca.png")));
			case "metoffice.gov.uk" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/met.png")));
			case "bom2", "bom3daily", "bom3hourly" -> im.post(() -> im.setImageDrawable(weeWXApp.loadSVGFromAssets("logos/bom" + finalextSVG)));
			case "aemet.es" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/aemet.png")));
			case "dwd.de" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/dwd.png")));
			case "metservice.com" -> im.post(() -> im.setImageDrawable(weeWXApp.loadSVGFromAssets("logos/metservice" + finalextSVG)));
			case "meteofrance.com" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/mf.png")));
			case "openweathermap.org" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/owm.png")));
			case "apixu.com" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/apixu.png")));
			case "weather.com" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/weather_com.svg")));
			case "met.ie" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/met_ie" + finalextPNG)));
			case "ilmeteo.it" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/ilmeteo_it.png")));
			case "tempoitalia.it" -> im.post(() -> im.setImageBitmap(weeWXApp.loadBitmapFromAssets("logos/tempoitalia_it.png")));
		}
	}

	public void updateSwipe()
	{
		if(rfl.getVisibility() == View.VISIBLE && floatingCheckBox.isChecked() && isVisible())
		{
			LogMessage("Disabling swipe between screens...");
			activity.setUserInputPager(false);
			swipeLayout2.setEnabled(false);
			return;
		}

		LogMessage("Enabling swipe between screens...");
		activity.setUserInputPager(true);
		swipeLayout2.setEnabled(true);
	}

	@Override
	public void onClick(View view)
	{
		KeyValue.putVar("disableSwipeOnRadar", floatingCheckBox.isChecked());
		updateSwipe();
		LogMessage("Forecast.onClick() finished...");
	}
}