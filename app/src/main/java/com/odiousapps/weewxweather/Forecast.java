package com.odiousapps.weewxweather;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

@SuppressWarnings("SameParameterValue")
public class Forecast extends Fragment
{
	private View rootView;
	private WebView wv1, wv2;
	private SwipeRefreshLayout swipeLayout;
	private TextView forecast;
	private ImageView im;
	private RotateLayout rl;

	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		rootView = inflater.inflate(R.layout.fragment_forecast, container, false);

		wv1 = rootView.findViewById(R.id.webView1);
		wv2 = rootView.findViewById(R.id.webView2);

		swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
		swipeLayout.setRefreshing(true);

		swipeLayout.setOnRefreshListener(() ->
		{
			swipeLayout.setRefreshing(true);
			Common.LogMessage("onRefresh();");
			reloadWebView(true);
			getForecast(true);
		});

		wv1.setWebViewClient(new WebViewClient()
		{
			@Override
			public void onPageFinished(WebView view, String url)
			{
				super.onPageFinished(view, url);
				if(rl.getVisibility() == View.VISIBLE)
					stopRefreshing();
			}
		});

		wv2.setWebViewClient(new WebViewClient()
		{
			@Override
			public void onPageFinished(WebView view, String url)
			{
				super.onPageFinished(view, url);
				if(wv2.getVisibility() == View.VISIBLE)
					stopRefreshing();
			}
		});

		rl = rootView.findViewById(R.id.rotateWeb);

		Common.setWebview(wv1);
		Common.setWebview(wv2);

		wv1.getViewTreeObserver().addOnScrollChangedListener(() -> swipeLayout.setEnabled(wv1.getScrollY() == 0));
		wv1.getViewTreeObserver().addOnScrollChangedListener(() -> swipeLayout.setEnabled(wv1.getScrollY() == 0));
		wv2.getViewTreeObserver().addOnScrollChangedListener(() -> swipeLayout.setEnabled(wv2.getScrollY() == 0));
		wv2.getViewTreeObserver().addOnScrollChangedListener(() -> swipeLayout.setEnabled(wv2.getScrollY() == 0));

		forecast = rootView.findViewById(R.id.forecast);
		im = rootView.findViewById(R.id.logo);

		updateScreen();

		File f2 = new File(Common.getFilesDir(), "/radar.gif");
		long[] period = Common.getPeriod();

		if(!Common.GetStringPref("RADAR_URL", "").isEmpty() && f2.lastModified() + period[0] < System.currentTimeMillis())
			reloadWebView(false);

		long current_time = Math.round(System.currentTimeMillis() / 1000.0);
		if(!Common.GetBoolPref("radarforecast", true) && Common.GetLongPref("rssCheck", 0) + 7190 < current_time)
			getForecast(false);

		return rootView;
	}

	private void stopRefreshing()
	{
		if(swipeLayout.isRefreshing())
			swipeLayout.post(() -> swipeLayout.setRefreshing(false));
	}

	private void loadWebView()
	{
		if(Common.GetBoolPref("radarforecast", true))
			return;

		if (Common.GetStringPref("radtype", "image").equals("image"))
		{
			String radar = Common.getFilesDir() + "/radar.gif";
			File rf = new File(radar);

			if(!rf.exists() && !Common.GetStringPref("RADAR_URL", "").isEmpty() && Common.checkConnection())
			{
				reloadWebView(true);
				return;
			}

			if (!rf.exists() || Common.GetStringPref("RADAR_URL", "").isEmpty())
			{
				final String html = Common.current_html_headers +
						"Radar URL not set or is still downloading. You can go to settings to change." +
						Common.html_footer;

				wv1.post(() -> wv1.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null));
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
					if (imageInFile.read(imageData) > 0)
						radar = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
				} catch (Exception e) {
					Common.doStackOutput(e);
				}
			} catch (Exception e) {
				Common.doStackOutput(e);
			}

			final String html = Common.current_html_headers +
					"\t<div style='text-align:center;'>\n" +
					"\t<img style='margin:0px;padding:0px;border:0px;text-align:center;max-height:" + height + "px;max-width:" + width + "px;width:auto;height:auto;'\n" +
					"\tsrc='" + radar + "'>\n" +
					"\t</div>" +
					Common.html_footer;

			wv1.post(() -> wv1.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null));
			stopRefreshing();
			rl.setVisibility(View.VISIBLE);
			wv2.setVisibility(View.GONE);
		} else if(Common.GetStringPref("radtype", "image").equals("webpage") &&
				!Common.GetStringPref("RADAR_URL", "").isEmpty()) {
			rl.setVisibility(View.GONE);
			wv2.setVisibility(View.VISIBLE);
			wv2.post(() -> wv2.loadUrl(Common.GetStringPref("RADAR_URL", "")));
			stopRefreshing();
		}
	}

	private void reloadWebView(boolean force)
	{
		if(Common.GetBoolPref("radarforecast", true))
			return;

		Common.LogMessage("reload radar...");
		final String radar = Common.GetStringPref("RADAR_URL", "");

		if(radar.isEmpty() || Common.GetStringPref("radtype", "image").equals("webpage"))
		{
			loadWebView();
			return;
		}

		if(!Common.checkConnection() && !force)
		{
			Common.LogMessage("Not on wifi and not a forced refresh");
			stopRefreshing();
			return;
		}

		Thread t = new Thread(() ->
		{
			try
			{
				Common.LogMessage("Starting to download image from: " + radar);
				File file = new File(Common.getFilesDir(), "/radar.gif.tmp");
				File f = Common.downloadJSOUP(file, radar);
				String ftmp = f != null ? f.getAbsolutePath() : null;
				if(ftmp != null)
				{
					Common.LogMessage("Done downloading " + f.getAbsolutePath() + ", prompt handler to draw to movie");
					File f2 = new File(Common.getFilesDir(), "/radar.gif");
					if(!f.renameTo(f2))
						Common.LogMessage("Failed to rename '"+ f.getAbsolutePath() + "' to '" + f2.getAbsolutePath() + "'");
				}

				wv1.post(() -> wv1.reload());
				wv2.post(() -> wv2.reload());
				stopRefreshing();
			} catch (Exception e) {
				Common.doStackOutput(e);
				stopRefreshing();
			}
		});

		t.start();
	}

	public void onResume()
	{
		super.onResume();
		Common.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);
		Common.LogMessage("forecast.java -- registerReceiver");
	}

	public void onPause()
	{
		super.onPause();
		Common.NotificationManager.getNotificationLiveData().removeObserver(notificationObserver);
		Common.LogMessage("forecast.java -- unregisterReceiver");
	}

	private final Observer<String> notificationObserver = s ->
	{
		Common.LogMessage("notificationObserver == " + s);

		//if(s.equals(Common.UPDATE_INTENT) || s.equals(Common.REFRESH_INTENT))
		//	updateScreen();

		if(s.equals(Common.EXIT_INTENT))
			onPause();
	};

	private void updateScreen()
	{
		if (Common.GetBoolPref("radarforecast", true))
		{
			Common.LogMessage("Displaying forecast");
			getForecast(false);
			forecast.setVisibility(View.VISIBLE);
			im.setVisibility(View.VISIBLE);
			rl.setVisibility(View.GONE);
			wv2.setVisibility(View.VISIBLE);
			stopRefreshing();
		} else {
			Common.LogMessage("Displaying radar");
			if (Common.GetStringPref("radtype", "image").equals("image"))
			{
				forecast.setVisibility(View.GONE);
				im.setVisibility(View.GONE);
				rl.setVisibility(View.VISIBLE);
				wv2.setVisibility(View.GONE);
			} else {
				forecast.setVisibility(View.GONE);
				im.setVisibility(View.GONE);
				rl.setVisibility(View.GONE);
				wv2.setVisibility(View.VISIBLE);
			}
			loadWebView();
		}
	}

	private void getForecast(boolean force)
	{
		if(!Common.GetBoolPref("radarforecast", true))
			return;

		final String forecast_url = Common.GetStringPref("FORECAST_URL", "");

		if(forecast_url.isEmpty())
		{
			final String html = Common.current_html_headers + "Forecast URL not set. Edit inigo-settings.txt to change." + Common.html_footer;

			wv2.post(() ->
					wv2.loadDataWithBaseURL(null, html, "text/html", "utf-8", null));
			stopRefreshing();

			return;
		}

		if(!Common.checkConnection() && !force)
		{
			Common.LogMessage("Not on wifi and not a forced refresh");
			return;
		}

		if(!Common.GetStringPref("forecastData", "").isEmpty())
			generateForecast();

		Thread t = new Thread(() ->
		{
			try
			{
				long current_time = Math.round(System.currentTimeMillis() / 1000.0);

				if(Common.GetStringPref("forecastData", "").isEmpty() || Common.GetLongPref("rssCheck", 0) + 7190 < current_time)
				{
					Common.LogMessage("no forecast data or cache is more than 2 hour old");

					stopRefreshing();

					String tmp = Common.downloadForecast();

					Common.LogMessage("updating rss cache");
					Common.SetLongPref("rssCheck", current_time);
					Common.SetStringPref("forecastData", tmp);

					wv1.post(() -> wv1.reload());
					wv2.post(() -> wv2.reload());
					stopRefreshing();
				}
			} catch (Exception e) {
				Common.doStackOutput(e);
			}
		});

		t.start();
	}

	private void generateForecast()
	{
		Thread t = new Thread(() ->
		{
			Common.LogMessage("getting json data");
			String data;
			String fctype = Common.GetStringPref("fctype", "Yahoo");

			data = Common.GetStringPref("forecastData", "");
			if(data.isEmpty())
			{
				final String html = Common.current_html_headers + getString(R.string.forecast_url_not_set) + Common.html_footer;

				wv1.post(() ->
						wv1.loadDataWithBaseURL(null, html, "text/html", "utf-8", null));
				return;
			}

			switch (fctype.toLowerCase(Locale.ENGLISH))
			{
				case "yahoo" ->
				{
					String[] content = Common.processYahoo(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "weatherzone" ->
				{
					String[] content = Common.processWZ(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "yr.no" ->
				{
					String[] content = Common.processYR(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "met.no" ->
				{
					String[] content = Common.processMetNO(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "bom.gov.au" ->
				{
					String[] content = Common.processBOM(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "wmo.int" ->
				{
					String[] content = Common.processWMO(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "weather.gov" ->
				{
					String[] content = Common.processWGOV(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "weather.gc.ca" ->
				{
					String[] content = Common.processWCA(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "weather.gc.ca-fr" ->
				{
					String[] content = Common.processWCAF(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "metoffice.gov.uk" ->
				{
					String[] content = Common.processMET(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "bom2" ->
				{
					String[] content = Common.processBOM2(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "bom3" ->
				{
					String[] content = Common.processBOM3(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "aemet.es" ->
				{
					String[] content = Common.processAEMET(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "dwd.de" ->
				{
					String[] content = Common.processDWD(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "metservice.com" ->
				{
					String[] content = Common.processMetService(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "openweathermap.org" ->
				{
					String[] content = Common.processOWM(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "weather.com" ->
				{
					String[] content = Common.processWCOM(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "met.ie" ->
				{
					String[] content = Common.processMETIE(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "tempoitalia.it" ->
				{
					String[] content = Common.processTempoItalia(data, true);
					if (content != null && content.length >= 2)
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

		wv2.post(() ->
		{
			wv2.clearFormData();
			wv2.clearHistory();
			wv2.clearCache(true);
			wv2.loadDataWithBaseURL("file:///android_res/drawable/", fc, "text/html", "utf-8", null);
		});

		TextView tv1 = rootView.findViewById(R.id.forecast);
		tv1.post(() -> tv1.setText(desc));

		switch (Common.GetStringPref("fctype", "yahoo").toLowerCase(Locale.ENGLISH))
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

		stopRefreshing();
	}
}