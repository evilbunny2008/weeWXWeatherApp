package com.odiousapps.weewxweather;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

@SuppressWarnings("SameParameterValue")
public class Forecast extends Fragment
{
	private Common common;
	private View rootView;
	private WebView wv1, wv2;
	private SwipeRefreshLayout swipeLayout;
	private TextView forecast;
	private ImageView im;
	private RotateLayout rl;

	public Forecast()
	{
	}

	Forecast(Common common)
	{
		this.common = common;
	}

	public static Forecast newInstance(Common common)
	{
		return new Forecast(common);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		CommonViewModel commonViewModel = new ViewModelProvider(this).get(CommonViewModel.class);

		rootView = inflater.inflate(R.layout.fragment_forecast, container, false);

		wv1 = rootView.findViewById(R.id.webView1);
		wv2 = rootView.findViewById(R.id.webView2);

		swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
		swipeLayout.setRefreshing(true);

		if(commonViewModel.getCommon() == null)
		{
			commonViewModel.setCommon(common);
		} else {
			common = commonViewModel.getCommon();
			common.reload(common.context);
		}

		loadWidgets();

		return rootView;
	}

	private void loadWidgets()
	{
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
					swipeLayout.setRefreshing(false);
			}
		});

		wv2.setWebViewClient(new WebViewClient()
		{
			@Override
			public void onPageFinished(WebView view, String url)
			{
				super.onPageFinished(view, url);
				if(wv2.getVisibility() == View.VISIBLE)
					swipeLayout.setRefreshing(false);
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

		File f2 = new File(common.context.getFilesDir(), "/radar.gif");
		long[] period = common.getPeriod();

		if(!common.GetStringPref("RADAR_URL", "").isEmpty() && f2.lastModified() + period[0] < System.currentTimeMillis())
			reloadWebView(false);

		long current_time = Math.round(System.currentTimeMillis() / 1000.0);
		if(!common.GetBoolPref("radarforecast", true) && common.GetLongPref("rssCheck", 0) + 7190 < current_time)
			getForecast(false);
	}

	private void loadWebView()
	{
		if(common.GetBoolPref("radarforecast", true))
			return;

		if (common.GetStringPref("radtype", "image").equals("image"))
		{
			String radar = common.context.getFilesDir() + "/radar.gif";
			File rf = new File(radar);

			if(!rf.exists() && !common.GetStringPref("RADAR_URL", "").isEmpty() && common.checkConnection())
			{
				reloadWebView(true);
				return;
			}

			if (!rf.exists() || common.GetStringPref("RADAR_URL", "").isEmpty())
			{
				String html = Common.current_html_headers;
				html += "Radar URL not set or is still downloading. You can go to settings to change.";
				html += Common.html_footer;

				wv1.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null);

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

			String html = Common.current_html_headers;
			html += "\t<div style='text-align:center;'>\n" +
					"\t<img style='margin:0px;padding:0px;border:0px;text-align:center;max-height:" + height + "px;max-width:" + width + "px;width:auto;height:auto;'\n" +
					"\tsrc='" + radar + "'>\n" +
					"\t</div>";
			html += Common.html_footer;

			wv1.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null);
			rl.setVisibility(View.VISIBLE);
			wv2.setVisibility(View.GONE);
		} else if(common.GetStringPref("radtype", "image").equals("webpage") &&
				!common.GetStringPref("RADAR_URL", "").isEmpty()) {
			rl.setVisibility(View.GONE);
			wv2.setVisibility(View.VISIBLE);
			wv2.loadUrl(common.GetStringPref("RADAR_URL", ""));
		}
	}

	private void reloadWebView(boolean force)
	{
		if(common.GetBoolPref("radarforecast", true))
			return;

		Common.LogMessage("reload radar...");
		final String radar = common.GetStringPref("RADAR_URL", "");

		if(radar.isEmpty() || common.GetStringPref("radtype", "image").equals("webpage"))
		{
			loadWebView();
			return;
		}

		if(!common.checkConnection() && !force)
		{
			Common.LogMessage("Not on wifi and not a forced refresh");
			swipeLayout.setRefreshing(false);
			return;
		}

		Thread t = new Thread(() ->
		{
			try
			{
				Common.LogMessage("starting to download image from: " + radar);
				File file = new File(common.context.getFilesDir(), "/radar.gif.tmp");
				File f = common.downloadJSOUP(file, radar);
				Common.LogMessage("done downloading " + f.getAbsolutePath() + ", prompt handler to draw to movie");
				File f2 = new File(common.context.getFilesDir(), "/radar.gif");
				if(f.renameTo(f2))
				{
					Handler mHandler = new Handler(Looper.getMainLooper());
					mHandler.post(() ->
					{
						wv1.post(() -> wv1.reload());
						wv2.post(() -> wv2.reload());
					});
				}
			} catch (Exception e) {
				Common.doStackOutput(e);
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

		if(s.equals(Common.UPDATE_INTENT) || s.equals(Common.REFRESH_INTENT))
			updateScreen();

		if(s.equals(Common.EXIT_INTENT))
			onPause();
	};

	private void updateScreen()
	{
		if (common.GetBoolPref("radarforecast", true))
		{
			Common.LogMessage("Displaying forecast");
			getForecast(false);
			forecast.setVisibility(View.VISIBLE);
			im.setVisibility(View.VISIBLE);
			rl.setVisibility(View.GONE);
			wv2.setVisibility(View.VISIBLE);
		} else {
			Common.LogMessage("Displaying radar");
			if (common.GetStringPref("radtype", "image").equals("image"))
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
		if(!common.GetBoolPref("radarforecast", true))
			return;

		final String forecast_url = common.GetStringPref("FORECAST_URL", "");

		if(forecast_url.isEmpty())
		{
			final String html = Common.current_html_headers + "Forecast URL not set. Edit inigo-settings.txt to change." + Common.html_footer;

			Handler mHandler = new Handler(Looper.getMainLooper());
			mHandler.post(() ->
					wv2.loadDataWithBaseURL(null, html, "text/html", "utf-8", null));

			return;
		}

		if(!common.checkConnection() && !force)
		{
			Common.LogMessage("Not on wifi and not a forced refresh");
			return;
		}

		if(!common.GetStringPref("forecastData", "").isEmpty())
			generateForecast();

		Thread t = new Thread(() ->
		{
			try
			{
				long current_time = Math.round(System.currentTimeMillis() / 1000.0);

				if(common.GetStringPref("forecastData", "").isEmpty() || common.GetLongPref("rssCheck", 0) + 7190 < current_time)
				{
					Common.LogMessage("no forecast data or cache is more than 2 hour old");

					Handler mHandler = new Handler(Looper.getMainLooper());
					mHandler.post(() ->
							swipeLayout.setRefreshing(true));

					String tmp = common.downloadForecast();

					Common.LogMessage("updating rss cache");
					common.SetLongPref("rssCheck", current_time);
					common.SetStringPref("forecastData", tmp);
					wv1.post(() ->
							wv1.post(() -> wv1.reload()));

					wv2.post(() ->
							wv2.post(() -> wv2.reload()));
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
			String fctype = common.GetStringPref("fctype", "Yahoo");

			data = common.GetStringPref("forecastData", "");
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
					String[] content = common.processYahoo(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "weatherzone" ->
				{
					String[] content = common.processWZ(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "yr.no" ->
				{
					String[] content = common.processYR(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "met.no" ->
				{
					String[] content = common.processMetNO(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "bom.gov.au" ->
				{
					String[] content = common.processBOM(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "wmo.int" ->
				{
					String[] content = common.processWMO(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "weather.gov" ->
				{
					String[] content = common.processWGOV(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "weather.gc.ca" ->
				{
					String[] content = common.processWCA(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "weather.gc.ca-fr" ->
				{
					String[] content = common.processWCAF(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "metoffice.gov.uk" ->
				{
					String[] content = common.processMET(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "bom2" ->
				{
					String[] content = common.processBOM2(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "bom3" ->
				{
					String[] content = common.processBOM3(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "aemet.es" ->
				{
					String[] content = common.processAEMET(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "dwd.de" ->
				{
					String[] content = common.processDWD(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "metservice.com" ->
				{
					String[] content = common.processMetService(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "openweathermap.org" ->
				{
					String[] content = common.processOWM(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "weather.com" ->
				{
					String[] content = common.processWCOM(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "met.ie" ->
				{
					String[] content = common.processMETIE(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
				case "tempoitalia.it" ->
				{
					String[] content = common.processTempoItalia(data, true);
					if (content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
				}
			}
		});

		t.start();
	}

	private void updateForecast(final String bits, final String desc)
	{
		Handler mHandler = new Handler(Looper.getMainLooper());
		mHandler.post(() ->
		{
			final String fc = Common.current_html_headers + "<div style='text-align:center'>" + bits + "</div>" + Common.html_footer;

			Handler mHandler1 = new Handler(Looper.getMainLooper());
			mHandler1.post(() ->
			{
				wv2.clearFormData();
				wv2.clearHistory();
				wv2.clearCache(true);
				wv2.loadDataWithBaseURL("file:///android_res/drawable/", fc, "text/html", "utf-8", null);
			});

			TextView tv1 = rootView.findViewById(R.id.forecast);
			tv1.setText(desc);

			switch (common.GetStringPref("fctype", "yahoo").toLowerCase(Locale.ENGLISH))
			{
				case "yahoo" -> im.setImageResource(R.drawable.purple);
				case "weatherzone" -> im.setImageResource(R.drawable.wz);
				case "yr.no" -> im.setImageResource(R.drawable.yrno);
				case "met.no" -> im.setImageResource(R.drawable.met_no);
				case "bom.gov.au" ->
				{
					im.setImageResource(R.drawable.bom);
					im.setColorFilter(null);
				}
				case "wmo.int" -> im.setImageResource(R.drawable.wmo);
				case "weather.gov" -> im.setImageResource(R.drawable.wgov);
				case "weather.gc.ca", "weather.gc.ca-fr" -> im.setImageResource(R.drawable.wca);
				case "metoffice.gov.uk" -> im.setImageResource(R.drawable.met);
				case "bom2", "bom3" ->
				{
					im.setImageResource(R.drawable.bom);
					im.setColorFilter(null);
				}
				case "aemet.es" -> im.setImageResource(R.drawable.aemet);
				case "dwd.de" -> im.setImageResource(R.drawable.dwd);
				case "metservice.com" -> im.setImageResource(R.drawable.metservice);
				case "meteofrance.com" -> im.setImageResource(R.drawable.mf);
				case "openweathermap.org" -> im.setImageResource(R.drawable.owm);
				case "apixu.com" ->
				{
					im.setImageResource(R.drawable.apixu);
					im.setColorFilter(null);
				}
				case "weather.com" -> im.setImageResource(R.drawable.weather_com);
				case "met.ie" ->
				{
					im.setImageResource(R.drawable.met_ie);
					im.setColorFilter(null);
				}
				case "ilmeteo.it" -> im.setImageResource(R.drawable.ilmeteo_it);
				case "tempoitalia.it" -> im.setImageResource(R.drawable.tempoitalia_it);
			}
		});
	}
}