package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.ColorMatrixColorFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class Forecast extends Fragment
{
	private final Common common;
	private View rootView;
	private WebView wv1, wv2;
	private SwipeRefreshLayout swipeLayout;
	private TextView forecast;
	private ImageView im;
	private RotateLayout rl;
	private int dark_theme;

	Forecast(Common common)
	{
		this.common = common;
		dark_theme = common.GetIntPref("dark_theme", 2);
		if(dark_theme == 2)
			dark_theme = common.getSystemTheme();
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		rootView = inflater.inflate(R.layout.fragment_forecast, container, false);
		swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
		swipeLayout.setOnRefreshListener(() ->
		{
			swipeLayout.setRefreshing(true);
			Common.LogMessage("onRefresh();");
			reloadWebView(true);
			getForecast(true);
		});

		rl = rootView.findViewById(R.id.rotateWeb);
		wv1 = rootView.findViewById(R.id.webView1);
		wv2 = rootView.findViewById(R.id.webView2);

		wv1.getSettings().setUserAgentString(Common.UA);
		wv1.getSettings().setJavaScriptEnabled(true);

		wv1.getViewTreeObserver().addOnScrollChangedListener(() -> swipeLayout.setEnabled(wv1.getScrollY() == 0));

		wv1.setWebChromeClient(new WebChromeClient()
		{
			@Override
			public boolean onConsoleMessage(ConsoleMessage cm)
			{
				return true;
			}
		});
		wv1.getViewTreeObserver().addOnScrollChangedListener(() -> swipeLayout.setEnabled(wv1.getScrollY() == 0));

		wv2.getSettings().setUserAgentString(Common.UA);
		wv2.getSettings().setJavaScriptEnabled(true);

		wv2.getViewTreeObserver().addOnScrollChangedListener(() -> swipeLayout.setEnabled(wv2.getScrollY() == 0));

		wv2.setWebChromeClient(new WebChromeClient()
		{
			@Override
			public boolean onConsoleMessage(ConsoleMessage cm)
			{
				return true;
			}
		});
		wv2.getViewTreeObserver().addOnScrollChangedListener(() -> swipeLayout.setEnabled(wv2.getScrollY() == 0));

		wv2.setWebViewClient(new WebViewClient());

		forecast = rootView.findViewById(R.id.forecast);
		im = rootView.findViewById(R.id.logo);

		updateScreen();

		File f2 = new File(common.context.getFilesDir(), "/radar.gif");
		long[] period = common.getPeriod();

		if(!common.GetStringPref("RADAR_URL", "").equals("") && f2.lastModified() + period[0] < System.currentTimeMillis())
			reloadWebView(false);

		long current_time = Math.round(System.currentTimeMillis() / 1000.0);
		if(!common.GetBoolPref("radarforecast", true) && common.GetLongPref("rssCheck", 0) + 7190 < current_time)
			getForecast(false);

		return rootView;
	}

	private void loadWebView()
	{
		if(common.GetBoolPref("radarforecast", true))
			return;

		swipeLayout.setRefreshing(true);

		if (common.GetStringPref("radtype", "image").equals("image"))
		{
			String radar = common.context.getFilesDir() + "/radar.gif";
			File rf = new File(radar);

			if(!rf.exists() && !common.GetStringPref("RADAR_URL", "").equals("") && common.checkConnection())
			{
				reloadWebView(true);
				return;
			}

			if (!rf.exists() || common.GetStringPref("RADAR_URL", "").equals(""))
			{
				String html = "<html>";
				if (dark_theme == 1)
					html += "<head><style>body{color: #fff; background-color: #000;}</style></head>";
				html += "<body>Radar URL not set or is still downloading. You can go to settings to change.</body></html>";
				wv1.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null);
				swipeLayout.setRefreshing(false);
				return;
			}

			float sd = Resources.getSystem().getDisplayMetrics().density;

			int height = Math.round((float) Resources.getSystem().getDisplayMetrics().widthPixels / sd * 0.955f);
			int width = Math.round((float) Resources.getSystem().getDisplayMetrics().heightPixels / sd * 0.955f);

			String html = "<!DOCTYPE html>\n" +
					"<html>\n" +
					"  <head>\n" +
					"	<meta charset='utf-8'>\n" +
					"	<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n";
			if (dark_theme == 1)
				html += "<style>body{color: #fff; background-color: #000;}</style>";

			try
			{
				File f = new File(radar);
				try(FileInputStream imageInFile = new FileInputStream(f))
				{
					byte[] imageData = new byte[(int) f.length()];
					if (imageInFile.read(imageData) > 0)
						radar = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			html += "  </head>\n" +
					"  <body>\n" +
					"\t<div style='text-align:center;'>\n" +
					"\t<img style='margin:0px;padding:0px;border:0px;text-align:center;max-height:" + height + "px;max-width:" + width + "px;width:auto;height:auto;'\n" +
					"\tsrc='" + radar + "'>\n" +
					"\t</div>\n" +
					"  </body>\n" +
					"</html>";
			wv1.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null);
			rl.setVisibility(View.VISIBLE);
			wv2.setVisibility(View.GONE);
			swipeLayout.setRefreshing(false);
		} else if (common.GetStringPref("radtype", "image").equals("webpage") && !common.GetStringPref("RADAR_URL", "").equals("")) {
			rl.setVisibility(View.GONE);
			wv2.setVisibility(View.VISIBLE);

			if(common.checkConnection())
			{
				wv2.clearCache(true);
				wv2.clearHistory();
				wv2.clearFormData();
				WebSettings webSettings = wv2.getSettings();
				webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
			}

			wv2.loadUrl(common.GetStringPref("RADAR_URL", ""));

			swipeLayout.setRefreshing(false);
		}

		swipeLayout.setRefreshing(false);
	}

	@SuppressWarnings("SameParameterValue")
	private void reloadWebView(boolean force)
	{
		if(common.GetBoolPref("radarforecast", true))
			return;

		Common.LogMessage("reload radar...");
		final String radar = common.GetStringPref("RADAR_URL", "");

		if(radar.equals("") || common.GetStringPref("radtype", "image").equals("webpage"))
		{
			loadWebView();
			swipeLayout.setRefreshing(false);
			return;
		}

		if(!common.checkConnection() && !force)
		{
			Common.LogMessage("Not on wifi and not a forced refresh");
			swipeLayout.setRefreshing(false);
			return;
		}

		swipeLayout.setRefreshing(true);

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
						common.SendRefresh();
						swipeLayout.setRefreshing(false);
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			Handler mHandler = new Handler(Looper.getMainLooper());
			mHandler.post(() -> swipeLayout.setRefreshing(false));
		});

		t.start();
	}

	public void onResume()
	{
		super.onResume();
		updateScreen();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Common.UPDATE_INTENT);
		filter.addAction(Common.REFRESH_INTENT);
		filter.addAction(Common.EXIT_INTENT);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
			common.context.registerReceiver(serviceReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
		else
			common.context.registerReceiver(serviceReceiver, filter);
		Common.LogMessage("forecast.java -- registerReceiver");
	}

	public void onPause()
	{
		super.onPause();
		try
		{
			common.context.unregisterReceiver(serviceReceiver);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Common.LogMessage("forecast.java -- unregisterReceiver");
	}

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

		swipeLayout.setRefreshing(false);
	}

	private final BroadcastReceiver serviceReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			try
			{
				Common.LogMessage("Weather() We have a hit, so we should probably update the screen.");
				String action = intent.getAction();
				if(action != null && (action.equals(Common.UPDATE_INTENT) || action.equals(Common.REFRESH_INTENT)))
				{
					dark_theme = common.GetIntPref("dark_theme", 2);
					if(dark_theme == 2)
						dark_theme = common.getSystemTheme();
					updateScreen();
				} else if(action != null && action.equals(Common.EXIT_INTENT)) {
					onPause();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	private void getForecast(boolean force)
	{
		if(!common.GetBoolPref("radarforecast", true))
			return;

		final String forecast_url = common.GetStringPref("FORECAST_URL", "");

		if(forecast_url.equals(""))
		{
			final String html = "<html><body>Forecast URL not set. Edit inigo-settings.txt to change.</body></html>";

			Handler mHandler = new Handler(Looper.getMainLooper());
			mHandler.post(() ->
			{
				wv2.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
				swipeLayout.setRefreshing(false);
			});

			return;
		}

		if(!common.checkConnection() && !force)
		{
			Common.LogMessage("Not on wifi and not a forced refresh");
			Handler mHandler = new Handler(Looper.getMainLooper());
			mHandler.post(() ->
			{
				common.SendRefresh();
				swipeLayout.setRefreshing(false);
			});
			return;
		}

		swipeLayout.setRefreshing(true);

		if(!common.GetStringPref("forecastData", "").equals(""))
			generateForecast();

		Thread t = new Thread(() ->
		{
			Handler mHandler = new Handler(Looper.getMainLooper());

			try
			{
				long current_time = Math.round(System.currentTimeMillis() / 1000.0);

				if(common.GetStringPref("forecastData", "").equals("") || common.GetLongPref("rssCheck", 0) + 7190 < current_time)
				{
					Common.LogMessage("no forecast data or cache is more than 2 hour old");

					String tmp = common.downloadForecast();

					Common.LogMessage("updating rss cache");
					common.SetLongPref("rssCheck", current_time);
					common.SetStringPref("forecastData", tmp);
					mHandler.post(() ->
					{
						common.SendRefresh();
						swipeLayout.setRefreshing(false);
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		t.start();
	}

	private void generateForecast()
	{
		Thread t = new Thread(() ->
		{
			Handler mHandler = new Handler(Looper.getMainLooper());
			mHandler.post(() -> swipeLayout.setRefreshing(true));

			Common.LogMessage("getting json data");
			String data;
			String fctype = common.GetStringPref("fctype", "Yahoo");

			data = common.GetStringPref("forecastData", "");
			if(data.equals(""))
			{
				String tmp = "<html>";
				if(dark_theme == 1)
					tmp += "<head><style>body{color: #fff; background-color: #000;}</style></head>";
				tmp += "<body>" + getString(R.string.forecast_url_not_set) + "</body></html>";

				final String html = tmp;

				mHandler.post(() ->
				{
					wv1.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
					swipeLayout.setRefreshing(false);
				});
				return;
			}

			switch(fctype.toLowerCase(Locale.ENGLISH))
			{
				case "yahoo":
				{
					String[] content = common.processYahoo(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "weatherzone":
				{
					String[] content = common.processWZ(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "yr.no":
				{
					String[] content = common.processYR(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "met.no":
				{
					String[] content = common.processMetNO(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "bom.gov.au":
				{
					String[] content = common.processBOM(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "wmo.int":
				{
					String[] content = common.processWMO(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "weather.gov":
				{
					String[] content = common.processWGOV(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "weather.gc.ca":
				{
					String[] content = common.processWCA(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "weather.gc.ca-fr":
				{
					String[] content = common.processWCAF(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "metoffice.gov.uk":
				{
					String[] content = common.processMET(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "bom2":
				{
					String[] content = common.processBOM2(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "bom3":
				{
					String[] content = common.processBOM3(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "aemet.es":
				{
					String[] content = common.processAEMET(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "dwd.de":
				{
					String[] content = common.processDWD(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "metservice.com":
				{
					String[] content = common.processMetService(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "openweathermap.org":
				{
					String[] content = common.processOWM(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "weather.com":
				{
					String[] content = common.processWCOM(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "met.ie":
				{
					String[] content = common.processMETIE(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
				case "tempoitalia.it":
				{
					String[] content = common.processTempoItalia(data, true);
					if(content != null && content.length >= 2)
						updateForecast(content[0], content[1]);
					break;
				}
			}

			mHandler.post(() -> swipeLayout.setRefreshing(false));
		});

		t.start();
	}

	private void updateForecast(final String bits, final String desc)
	{
		Handler mHandler = new Handler(Looper.getMainLooper());
		mHandler.post(() ->
		{
			String tmpfc = "<html>";
			if (dark_theme == 1)
				tmpfc += "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.style_sheet_header + "</head>";
			else
				tmpfc += "<head>" + Common.style_sheet_header + "</head>";
			tmpfc += "<body style='text-align:center'>" + bits + "</body></html>";

			final String fc = tmpfc;

			Handler mHandler1 = new Handler(Looper.getMainLooper());
			mHandler1.post(() ->
			{
				wv2.clearFormData();
				wv2.clearHistory();
				wv2.clearCache(true);
				wv2.loadDataWithBaseURL("file:///android_res/drawable/", fc, "text/html", "utf-8", null);
			});

			TextView tv1 = rootView.findViewById(R.id.forecast);
			if (dark_theme == 0)
			{
				tv1.setTextColor(0xff000000);
				tv1.setBackgroundColor(0xffffffff);
				im.setBackgroundColor(0xffffffff);
			} else {
				tv1.setTextColor(0xffffffff);
				tv1.setBackgroundColor(0xff000000);
				im.setBackgroundColor(0xff000000);
			}
			tv1.setText(desc);

			switch (common.GetStringPref("fctype", "yahoo").toLowerCase(Locale.ENGLISH))
			{
				case "yahoo":
					im.setImageResource(R.drawable.purple);
					break;
				case "weatherzone":
					im.setImageResource(R.drawable.wz);
					break;
				case "yr.no":
					im.setImageResource(R.drawable.yrno);
					break;
				case "met.no":
					im.setImageResource(R.drawable.met_no);
					break;
				case "bom.gov.au":
					im.setImageResource(R.drawable.bom);
					if(dark_theme == 1)
						im.setColorFilter(new ColorMatrixColorFilter(Common.NEGATIVE));
					else
						im.setColorFilter(null);
					break;
				case "wmo.int":
					im.setImageResource(R.drawable.wmo);
					break;
				case "weather.gov":
					im.setImageResource(R.drawable.wgov);
					break;
				case "weather.gc.ca":
				case "weather.gc.ca-fr":
					im.setImageResource(R.drawable.wca);
					break;
				case "metoffice.gov.uk":
					im.setImageResource(R.drawable.met);
					break;
				case "bom2":
				case "bom3":
					im.setImageResource(R.drawable.bom);
					if(dark_theme == 1)
					{
						im.setColorFilter(new ColorMatrixColorFilter(Common.NEGATIVE));
					} else {
						im.setColorFilter(null);
					}
					break;
				case "aemet.es":
					im.setImageResource(R.drawable.aemet);
					break;
				case "dwd.de":
					im.setImageResource(R.drawable.dwd);
					break;
				case "metservice.com":
					im.setImageResource(R.drawable.metservice);
					if(dark_theme == 1)
						im.setColorFilter(new ColorMatrixColorFilter(Common.NEGATIVE));
					else
						im.setColorFilter(null);
					break;
				case "meteofrance.com":
					im.setImageResource(R.drawable.mf);
					break;
				case "openweathermap.org":
					im.setImageResource(R.drawable.owm);
					break;
				case "apixu.com":
					im.setImageResource(R.drawable.apixu);
					if(dark_theme == 1)
						im.setColorFilter(new ColorMatrixColorFilter(Common.NEGATIVE));
					else
						im.setColorFilter(null);
					break;
				case "weather.com":
					im.setImageResource(R.drawable.weather_com);
					break;
				case "met.ie":
					im.setImageResource(R.drawable.met_ie);
					if(dark_theme == 1)
						im.setColorFilter(new ColorMatrixColorFilter(Common.NEGATIVE));
					else
						im.setColorFilter(null);
					break;
				case "ilmeteo.it":
					im.setImageResource(R.drawable.ilmeteo_it);
					break;
				case "tempoitalia.it":
					im.setImageResource(R.drawable.tempoitalia_it);
					break;
			}
		});
	}
}