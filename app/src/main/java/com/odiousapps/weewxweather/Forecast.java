package com.odiousapps.weewxweather;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

@SuppressWarnings({"SameParameterValue", "unused"})
public class Forecast extends Fragment implements View.OnClickListener
{
	private View rootView;
	private WebView forecastWebView, radarRotated, radarWebView;
	private SwipeRefreshLayout swipeLayout1, swipeLayout2;
	private ImageView im;
	private FrameLayout fl;
	private RotateLayout rl;
	private MaterialCheckBox floatingCheckBox;
	private boolean isVisible;
	private boolean disableSwipeOnRadar;
	private boolean disabledSwipe;
	private MainActivity activity;
	private ViewTreeObserver.OnScrollChangedListener forecastScrollListener, radarScrollListener1, radarScrollListener2;
	private boolean hasLoaded;

	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		activity = (MainActivity)getActivity();

		rootView = inflater.inflate(R.layout.fragment_forecast, container, false);

		fl = rootView.findViewById(R.id.radarFrameLayout);
		rl = rootView.findViewById(R.id.rotateLayout);

		forecastWebView = rootView.findViewById(R.id.forecastWebView);
		Common.setWebview(forecastWebView);

		radarWebView = rootView.findViewById(R.id.radarWebView);
		Common.setWebview(radarWebView);

		radarRotated = rootView.findViewById(R.id.radarRotated);
		Common.setWebview(radarRotated);

		swipeLayout1 = rootView.findViewById(R.id.swipeToRefresh1);
		swipeLayout1.setEnabled(true);
		swipeLayout1.setOnRefreshListener(() ->
		{
			swipeLayout1.setRefreshing(true);
			Common.LogMessage("swipeLayout1.onRefresh();", true);
			getForecast(true);
		});

		swipeLayout2 = rootView.findViewById(R.id.swipeToRefresh2);
		swipeLayout2.setOnRefreshListener(() ->
		{
			swipeLayout2.setRefreshing(true);
			Common.LogMessage("swipeLayout2.onRefresh();", true);
			reloadRadar(true);
		});

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

		im = rootView.findViewById(R.id.logo);

		File f2 = new File(Common.getFilesDir(), "/radar.gif");
		long[] period = Common.getPeriod();

		String radarURl = Common.GetStringPref("RADAR_URL", "");
		String radtype = Common.GetStringPref("radtype", "");
		if(radarURl != null && !radarURl.isEmpty() && radtype != null && radtype.equals("image") &&
				f2.lastModified() + period[0] < System.currentTimeMillis())
			reloadRadar(false);

		if(radtype != null && !radtype.equals("image") && radarURl != null && !radarURl.isEmpty())
			loadRadar();

		long current_time = Math.round(System.currentTimeMillis() / 1000.0);
		if(!Common.GetBoolPref("radarforecast", true) && Common.GetLongPref("rssCheck", 0) + 7190 < current_time)
			getForecast(false);

		forecastScrollListener = () ->
				swipeLayout1.setEnabled(forecastWebView.getScrollY() == 0);

		radarScrollListener1 = () ->
				swipeLayout2.setEnabled(rl.getVisibility() == View.GONE &&
						!floatingCheckBox.isChecked() && radarWebView.getScrollY() == 0);

		radarScrollListener2 = () ->
				swipeLayout2.setEnabled(rl.getVisibility() == View.VISIBLE &&
						!floatingCheckBox.isChecked() && radarWebView.getScrollY() == 0);

		radarWebView.setWebViewClient(new WebViewClient()
		{
			@Override
			public void onPageFinished(WebView view, String url)
			{
				super.onPageFinished(view, url);
				if(fl.getVisibility() == View.VISIBLE && radarWebView.getVisibility() == View.VISIBLE)
					stopRefreshing();
			}
		});

		radarRotated.setWebViewClient(new WebViewClient()
		{
			@Override
			public void onPageFinished(WebView view, String url)
			{
				super.onPageFinished(view, url);
				if(fl.getVisibility() == View.VISIBLE && rl.getVisibility() == View.VISIBLE)
					stopRefreshing();
			}
		});

		floatingCheckBox = rootView.findViewById(R.id.floatingCheckBox);
		floatingCheckBox.setOnClickListener(this);

		return rootView;
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
		Common.LogMessage("Forecast.loadRadar()", true);

		if(Common.GetBoolPref("radarforecast", true))
			return;

		String radarURL = Common.GetStringPref("RADAR_URL", "");
		if(radarURL == null)
			return;

		String str1 = Common.GetStringPref("radtype", "image");
		if(str1 != null && str1.equals("image"))
		{
			String radar = Common.getFilesDir() + "/radar.gif";
			File rf = new File(radar);

			if(!rf.exists() && !radarURL.isEmpty() && Common.checkConnection())
			{
				reloadRadar(true);
				return;
			}

			if(!rf.exists() || radarURL.isEmpty())
			{
				Common.LogMessage("Loading radar image from URL: " + radarURL, true);

				final String html = Common.current_html_headers +
						"Radar URL not set or is still downloading. You can go to settings to change." +
						Common.html_footer;

				Common.LogMessage("Loading radar page... html: " + html, true);
				radarWebView.post(() -> radarWebView.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null));
				radarRotated.post(() -> radarRotated.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null));
				stopRefreshing();
				return;
			}

			float sd = Resources.getSystem().getDisplayMetrics().density;

			int height = Math.round((float) Resources.getSystem().getDisplayMetrics().widthPixels / sd * 0.955f);
			int width = Math.round((float) Resources.getSystem().getDisplayMetrics().heightPixels / sd * 0.955f);

			try
			{
				File f = new File(radar);
				try(FileInputStream imageInFile = new FileInputStream(f))
				{
					byte[] imageData = new byte[(int) f.length()];
					if(imageInFile.read(imageData) > 0)
						radar = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
				} catch(Exception e) {
					Common.doStackOutput(e);
				}
			} catch(Exception e) {
				Common.doStackOutput(e);
			}

			final String html = Common.current_html_headers +
					"\t<div style='text-align:center;'>\n" +
					"\t<img style='margin:0px;padding:0px;border:0px;text-align:center;max-height:" +
						height + "px;max-width:" + width + "px;width:auto;height:auto;'\n" +
					"\tsrc='" + radar + "'>\n" +
					"\t</div>" +
					Common.html_footer;

			Common.LogMessage("Loading radar page... html: " + html, true);
			radarWebView.post(() -> radarWebView.loadDataWithBaseURL("file:///android_res/drawable/", html,
					"text/html", "utf-8", null));
			radarRotated.post(() -> radarRotated.loadDataWithBaseURL("file:///android_res/drawable/", html,
					"text/html", "utf-8", null));
		} else {
			Common.LogMessage("Loading radar page... url: " + radarURL, true);

			radarWebView.post(() -> radarWebView.loadUrl(radarURL));
		}

		Common.LogMessage("Forecast.loadRadar() finished...", true);
		stopRefreshing();
	}

	private void reloadRadar(boolean force)
	{
		Common.LogMessage("Forecast.reloadRadar()", true);
		if(Common.GetBoolPref("radarforecast", true))
			return;

		Common.LogMessage("reload radar...", true);
		final String radar = Common.GetStringPref("RADAR_URL", "");
		if(radar == null)
			return;

		String fctype = Common.GetStringPref("radtype", "image");
		if(fctype == null)
			return;

		if(radar.isEmpty() || fctype.equals("webpage"))
		{
			loadRadar();
			return;
		}

		if(!Common.checkConnection() && !force)
		{
			Common.LogMessage("Not on wifi and not a forced refresh", true);
			stopRefreshing();
			return;
		}

		Thread t = new Thread(() ->
		{
			try
			{
				Common.LogMessage("Starting to download image from: " + radar, true);
				File file = new File(Common.getFilesDir(), "/radar.gif.tmp");
				File f = Common.downloadJSOUP(file, radar);
				String ftmp = f != null ? f.getAbsolutePath() : null;
				if(ftmp != null)
				{
					Common.LogMessage("Done downloading " + f.getAbsolutePath() + ", prompt handler to draw to movie", true);
					File f2 = new File(Common.getFilesDir(), "/radar.gif");
					if(!f.renameTo(f2))
						Common.LogMessage("Failed to rename '"+ f.getAbsolutePath() + "' to '" + f2.getAbsolutePath() + "'", true);
				}

				if(radarWebView.getVisibility() == View.VISIBLE)
					radarWebView.post(() -> radarWebView.reload());
				else
					radarRotated.post(() -> radarRotated.reload());

				stopRefreshing();
			} catch(Exception e) {
				Common.doStackOutput(e);
				stopRefreshing();
			}
		});

		t.start();
	}

	public void onResume()
	{
		super.onResume();

		Common.LogMessage("Forecast.onResume()", true);

		if(isVisible)
			return;

		isVisible = true;

		Common.LogMessage("Forecast.onResume()-- adding notification manager...", true);
		Common.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);

		Common.LogMessage("Forecast.onResume() -- updating the value of the floating checkbox...", true);
		disableSwipeOnRadar = Common.GetBoolPref("disableSwipeOnRadar", false);
		floatingCheckBox.setChecked(disableSwipeOnRadar);

		swipeLayout2.post(() -> swipeLayout2.setEnabled(fl.getVisibility() == View.VISIBLE && !floatingCheckBox.isChecked()));

		updateScreen();
		updateSwipe();
		addListeners();
	}

	public void onPause()
	{
		super.onPause();

		Common.LogMessage("Forecast.onPause()", true);

		if(!isVisible)
			return;

		isVisible = false;

		Common.NotificationManager.getNotificationLiveData().removeObserver(notificationObserver);

		doPause();
		removeListeners();

		Common.LogMessage("Forecast.onPause()-- removing notification manager...", true);
	}

	private final Observer<String> notificationObserver = s ->
	{
		Common.LogMessage("notificationObserver == " + s, true);

//		if(s.equals(Common.UPDATE_INTENT) || s.equals(Common.REFRESH_INTENT))
//			updateScreen();

		if(s.equals(Common.EXIT_INTENT))
			onPause();
	};

	void removeListeners()
	{
		if(forecastWebView != null && forecastScrollListener != null && forecastWebView.getViewTreeObserver().isAlive())
			forecastWebView.getViewTreeObserver().removeOnScrollChangedListener(forecastScrollListener);

		if(radarWebView != null && radarScrollListener1 != null && radarWebView.getViewTreeObserver().isAlive())
			radarWebView.getViewTreeObserver().removeOnScrollChangedListener(radarScrollListener1);

		if(radarRotated != null && radarScrollListener2 != null && radarRotated.getViewTreeObserver().isAlive())
			radarRotated.getViewTreeObserver().removeOnScrollChangedListener(radarScrollListener2);
	}

	void addListeners()
	{
		if(forecastWebView != null && forecastScrollListener != null && swipeLayout1.getVisibility() == View.VISIBLE)
			forecastWebView.getViewTreeObserver().addOnScrollChangedListener(forecastScrollListener);
		else
			if(radarWebView != null && radarScrollListener1 != null && radarWebView.getVisibility() == View.VISIBLE)
				radarWebView.getViewTreeObserver().addOnScrollChangedListener(radarScrollListener1);
			else if(radarRotated != null && radarScrollListener2 != null)
				radarRotated.getViewTreeObserver().addOnScrollChangedListener(radarScrollListener2);
	}

	void updateListeners()
	{
		removeListeners();
		addListeners();
	}

	private void loadData()
	{

		updateSwipe();
		updateListeners();

		if(!hasLoaded)
		{
			hasLoaded = true;
			loadRadar();
			getForecast(false);
		}

		stopRefreshing();
	}

	private void updateScreen()
	{
		Common.LogMessage("Forecast.updateScreen()", true);

		if(Common.GetBoolPref("radarforecast", true))
		{
			Common.LogMessage("Displaying forecastWebView...", true);
			if(fl.getVisibility() != View.GONE)
				fl.post(() -> fl.setVisibility(View.GONE));

			if(swipeLayout1.getVisibility() != View.VISIBLE)
				swipeLayout1.post(() -> swipeLayout1.setVisibility(View.VISIBLE));

			swipeLayout2.post(() -> swipeLayout2.setEnabled(false));
			swipeLayout1.post(() ->
			{
				swipeLayout1.setRefreshing(true);
				swipeLayout1.setEnabled(true);
			});
		} else {
			Common.LogMessage("Displaying radar framelayout", true);
			if(swipeLayout1.getVisibility() != View.GONE)
				swipeLayout1.post(() -> swipeLayout1.setVisibility(View.GONE));

			if(fl.getVisibility() != View.VISIBLE)
				fl.post(() -> fl.setVisibility(View.VISIBLE));

			swipeLayout1.post(() -> swipeLayout1.setEnabled(false));
			swipeLayout2.post(() ->
			{
				swipeLayout2.setRefreshing(true);
				swipeLayout2.setEnabled(!floatingCheckBox.isChecked());
			});

			String radtype = Common.GetStringPref("radtype", "image");
			if(radtype != null)
			{
				if(radtype.equals("image"))
				{
					Common.LogMessage("Hide radarWebView, show rotated layout...", true);
					radarWebView.post(() -> radarWebView.setVisibility(View.GONE));
					rl.post(() -> rl.setVisibility(View.VISIBLE));
				} else {
					Common.LogMessage("Hide rotated layout, show radarWebView...", true);
					rl.post(() -> rl.setVisibility(View.GONE));
					radarWebView.post(() -> radarWebView.setVisibility(View.VISIBLE));
				}
			}
		}

		loadData();

		Common.LogMessage("Forecast.updateScreen() finished...", true);
	}

	private void getForecast(boolean force)
	{
		if(!Common.GetBoolPref("radarforecast", true))
			return;

		final String forecast_url = Common.GetStringPref("FORECAST_URL", "");
		if(forecast_url == null || forecast_url.isEmpty())
		{
			final String html = Common.current_html_headers + "Forecast URL not set. Edit inigo-settings.txt to change." + Common.html_footer;

			forecastWebView.post(() ->
					forecastWebView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null));

			stopRefreshing();

			return;
		}

		if(!Common.checkConnection() && !force)
		{
			Common.LogMessage("Not on wifi and not a forced refresh", true);
			return;
		}

		final String forecastData = Common.GetStringPref("forecastData", "");
		if(forecastData == null)
			return;

		if(!forecastData.isEmpty())
			generateForecast();

		Thread t = new Thread(() ->
		{
			try
			{
				long current_time = Math.round(System.currentTimeMillis() / 1000.0);

				if(forecastData.isEmpty() || Common.GetLongPref("rssCheck", 0) + 7190 < current_time || force)
				{
					Common.LogMessage("no forecast data or cache is more than 2 hour old or was forced", true);

					String tmp = Common.downloadForecast();

					Common.LogMessage("updating rss cache", true);
					Common.SetLongPref("rssCheck", current_time);
					Common.SetStringPref("forecastData", tmp);

					forecastWebView.post(() -> forecastWebView.reload());
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
		Thread t = new Thread(() ->
		{
			Common.LogMessage("getting json data", true);
			String data;
			String fctype = Common.GetStringPref("fctype", "Yahoo");
			if(fctype == null)
				return;

			data = Common.GetStringPref("forecastData", "");
			if(data == null)
				return;

			if(data.isEmpty())
			{
				final String html = Common.current_html_headers + getString(R.string.forecast_url_not_set) + Common.html_footer;

				forecastWebView.post(() ->
						forecastWebView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null));
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
				case "bom.gov.au" ->
				{
					String[] content = Common.processBOM(data, true);
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
		final String fc = Common.current_html_headers + "<div style='text-align:center'>" + bits + "</div>" + Common.html_footer;

		forecastWebView.post(() ->
		{
			forecastWebView.clearFormData();
			forecastWebView.clearHistory();
			forecastWebView.clearCache(true);
			forecastWebView.loadDataWithBaseURL("file:///android_res/drawable/", fc, "text/html", "utf-8", null);
		});

		TextView tv1 = rootView.findViewById(R.id.forecast);
		tv1.post(() -> tv1.setText(desc));

		String fctype = Common.GetStringPref("fctype", "yahoo");
		if(fctype == null)
			return;

		switch(fctype.toLowerCase(Locale.ENGLISH))
		{
			case "yahoo" -> im.post(() -> im.setImageResource(R.drawable.purple));
			case "weatherzone" -> im.post(() -> im.setImageResource(R.drawable.wz));
			case "yr.no" -> im.post(() -> im.setImageResource(R.drawable.yrno));
			case "met.no" -> im.post(() -> im.setImageResource(R.drawable.met_no));
			case "bom.gov.au" ->
			{
				im.post(() -> im.setImageResource(R.drawable.bom));
				im.post(() -> im.setColorFilter(null));
			}
			case "wmo.int" -> im.post(() -> im.setImageResource(R.drawable.wmo));
			case "weather.gov" -> im.post(() -> im.setImageResource(R.drawable.wgov));
			case "weather.gc.ca", "weather.gc.ca-fr" -> im.post(() -> im.setImageResource(R.drawable.wca));
			case "metoffice.gov.uk" -> im.post(() -> im.setImageResource(R.drawable.met));
			case "bom2", "bom3" ->
			{
				im.post(() -> im.setImageResource(R.drawable.bom));
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
				im.post(() -> im.setColorFilter(null));
			}
			case "weather.com" -> im.post(() -> im.setImageResource(R.drawable.weather_com));
			case "met.ie" ->
			{
				im.post(() -> im.setImageResource(R.drawable.met_ie));
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
			Common.LogMessage("Enabling swipe between screens...", true);
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

		if(!disabledSwipe && disableSwipeOnRadar && fl.getVisibility() == View.VISIBLE)
		{
			disabledSwipe = true;
			Common.LogMessage("Disabling swipe between screens...", true);
			activity.setUserInputPager(false);
			return;
		}

		if(disabledSwipe && !disableSwipeOnRadar)
		{
			disabledSwipe = false;
			Common.LogMessage("Enabling swipe between screens...", true);
			activity.setUserInputPager(true);
		}
	}

	@Override
	public void onClick(View view)
	{
		disableSwipeOnRadar = floatingCheckBox.isChecked();
		Common.SetBoolPref("disableSwipeOnRadar", disableSwipeOnRadar);
		swipeLayout2.post(() -> swipeLayout2.setEnabled(fl.getVisibility() == View.VISIBLE && !floatingCheckBox.isChecked()));
		updateListeners();
		updateSwipe();
		Common.LogMessage("Forecast.onClick() finished...", true);
	}
}