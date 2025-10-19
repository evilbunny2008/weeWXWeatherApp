package com.odiousapps.weewxweather;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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

import static java.lang.Math.round;

public class Weather extends Fragment
{
	private Common common;
	private View rootView;
	private WebView forecast;
	private WebView current;
	private SwipeRefreshLayout swipeLayout;

	public Weather()
	{
	}

	Weather(Common common)
	{
		this.common = common;
	}

	public static Weather newInstance(Common common)
	{
		return new Weather(common);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		CommonViewModel commonViewModel = new ViewModelProvider(this).get(CommonViewModel.class);

		if(commonViewModel.getCommon() == null)
		{
			commonViewModel.setCommon(common);
		} else {
			common = commonViewModel.getCommon();
			common.reload(common.context);
		}

		rootView = inflater.inflate(R.layout.fragment_weather, container, false);

		swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
		swipeLayout.setRefreshing(true);
		swipeLayout.setOnRefreshListener(() ->
		{
			swipeLayout.setRefreshing(true);
			Common.LogMessage("onRefresh();");
			forceRefresh();
		});

		forecast = rootView.findViewById(R.id.forecast);
		forecast.setWebViewClient(new WebViewClient()
		{
			@Override
			public void onPageFinished(WebView view, String url)
			{
				super.onPageFinished(view, url);
				swipeLayout.setRefreshing(false);
			}
		});

		Common.setWebview(forecast);
		forecast.getViewTreeObserver().addOnScrollChangedListener(() -> swipeLayout.setEnabled(forecast.getScrollY() == 0));
		loadWebView();

		current = rootView.findViewById(R.id.current);
		Common.setWebview(current);

		drawEverything();

		return rootView;
	}

	private void checkFields(TextView tv, String txt)
	{
		if (!tv.getText().toString().equals(txt))
			tv.post(() -> tv.setText(txt));
	}

	void drawEverything()
	{
		int iw = 17;

		Common.LogMessage("drawEverything()");

		File f2 = new File(common.context.getFilesDir(), "/radar.gif");
		long[] period = common.getPeriod();

		if(!common.GetStringPref("RADAR_URL", "").isEmpty() && f2.lastModified() + period[0] < System.currentTimeMillis())
			reloadWebView(false);

		long current_time = Math.round(System.currentTimeMillis() / 1000.0);
		if(!common.GetBoolPref("radarforecast", true) && common.GetLongPref("rssCheck", 0) + 7190 < current_time)
			reloadForecast(false);

		Common.LogMessage("updateFields()");
		String[] bits = common.GetStringPref("LastDownload","").split("\\|");
		if(bits.length < 65)
			return;

		checkFields(rootView.findViewById(R.id.textView), bits[56]);
		checkFields(rootView.findViewById(R.id.textView2), bits[54] + " " + bits[55]);

		String stmp;
		final StringBuilder sb = new StringBuilder();

		sb.append(Common.current_html_headers);
		sb.append("<table style='width:100%;border:0px;'>");

		stmp = "<tr><td style='font-size:36pt;text-align:right;'>" + bits[0] + bits[60] + "</td>";
		if(bits.length > 203)
			stmp += "<td style='font-size:18pt;text-align:right;vertical-align:bottom;'>AT: " + bits[203] + bits[60] +"</td></tr></table>";
		else
			stmp += "<td>&nbsp</td></tr></table>";
		sb.append(stmp);
		sb.append("<table style='width:100%;border:0px;'>");

		stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-windy'></i></td><td>" + bits[25] + bits[61] + "</td>" +
				"<td style='text-align:right;'>" + bits[37] + bits[63] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-barometer'></i></td></tr>";
		sb.append(stmp);

		stmp = "<tr><td><i style='font-size:" + iw + "px;' class='wi wi-wind wi-towards-" + bits[30].toLowerCase(Locale.ENGLISH) + "'></i></td><td>" + bits[30] + "</td>" +
				"<td style='text-align:right;'>" + bits[6] + bits[64] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-humidity'></i></td></tr>";
		sb.append(stmp);

		String rain = bits[20] + bits[62] + " " + common.context.getString(R.string.since) + " mn";
		if(bits.length > 160 && !bits[160].isEmpty())
			rain = bits[158] + bits[62] + " " + common.context.getString(R.string.since) + " " + bits[160];

		stmp = "<tr><td><i style='font-size:" + iw + "px;' class='wi wi-umbrella'></i></td><td>" + rain + "</td>" +
				"<td style='text-align:right;'>" + bits[12] + bits[60] + "</td><td><i style='font-size:" + round(iw * 1.4) + "px;' class='wi wi-raindrop'></i></td></tr>";
		sb.append(stmp);

		stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-women-sunglasses'></i></td><td>" + bits[45] + "UVI</td>" +
				"<td style='text-align:right;'>" + bits[43] + "W/m²</td><td><i style='font-size:" + iw + "px;' class='flaticon-women-sunglasses'></i></td></tr>";
		sb.append(stmp);

		if(bits.length > 202 && common.GetBoolPref("showIndoor", false))
		{
			stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td><td>" + bits[161] + bits[60] + "</td>" +
					"<td style='text-align:right;'>" + bits[166] + bits[64] + "</td><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td></tr>";
			sb.append(stmp);
		}

		stmp = "</table>";
		sb.append(stmp);

		sb.append("<table style='width:100%;border:0px;'>");

		stmp = "<tr>" +
				"<td><i style='font-size:" + iw + "px;' class='wi wi-sunrise'></i></td><td>" + bits[57] + "</td>" +
				"<td><i style='font-size:" + iw + "px;' class='wi wi-sunset'></i></td><td>" + bits[58] + "</td>" +
				"<td><i style='font-size:" + iw + "px;' class='wi wi-moonrise'></i></td><td>" + bits[47] + "</td>" +
				"<td><i style='font-size:" + iw + "px;' class='wi wi-moonset'></i></td><td>" + bits[48] + "</td></tr>";
		Common.LogMessage("stmp = " + stmp);
		sb.append(stmp);

		stmp = "</table>";
		sb.append(stmp);

		sb.append(Common.html_footer);

		forceCurrentRefresh(sb.toString());
	}

	void forceForecastRefresh(String str)
	{
		Common.LogMessage("forceForecastRefresh(): str=" + str);

		forecast.post(() ->
		{
			forecast.clearFormData();
			forecast.clearHistory();
			forecast.clearCache(true);
			forecast.loadDataWithBaseURL("file:///android_res/drawable/", str, "text/html", "utf-8", null);
		});
	}

	void forceCurrentRefresh(String str)
	{
		Common.LogMessage("forceCurrentRefresh(): str=" + str);

		current.post(() ->
		{
			Common.LogMessage("forceCurrentRefresh(): str=" + str);
			current.clearFormData();
			current.clearHistory();
			current.clearCache(true);
			current.loadDataWithBaseURL("file:///android_res/drawable/", str, "text/html", "utf-8", null);
		});
	}

	private void forceRefresh()
	{
		Common.LogMessage("forceRefresh()");
		common.getWeather();
		reloadWebView(true);
		reloadForecast(true);
	}

	private void loadWebView()
	{
		Common.LogMessage("loadWebView()");
		Thread t = new Thread(() ->
		{
			try
			{
				// Sleep needed to stop frames dropping while loading
				Thread.sleep(500);
			} catch (Exception e) {
				Common.doStackOutput(e);
			}

			final StringBuilder sb = new StringBuilder();

			if (common.GetBoolPref("radarforecast", true))
			{
				switch (common.GetStringPref("radtype", "image"))
				{
					case "image" ->
					{
						String radar = common.context.getFilesDir() + "/radar.gif";

						File myFile = new File(radar);
						Common.LogMessage("myFile == " + myFile.getAbsolutePath());
						Common.LogMessage("myFile.exists() == " + myFile.exists());

						if (!myFile.exists() || common.GetStringPref("RADAR_URL", "").isEmpty())
						{
							sb.append(Common.current_html_headers)
									.append(getString(R.string.radar_url_not_set))
									.append(Common.html_footer);
						} else {
							sb.append(Common.current_html_headers);

							try
							{
								File f = new File(radar);
								try (FileInputStream imageInFile = new FileInputStream(f))
								{
									byte[] imageData = new byte[(int) f.length()];
									if (imageInFile.read(imageData) > 0)
										radar = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
								} catch (Exception e) {
									Common.doStackOutput(e);
								}
							} catch (Exception e)
							{
								Common.doStackOutput(e);
							}

							sb.append("\t<img style='margin:0px;padding:0px;border:0px;text-align:center;")
									.append("max-width:100%;width:auto;height:auto;'\n\tsrc='")
									.append(radar).append("'>");
							sb.append(Common.html_footer);
						}

						forceForecastRefresh(sb.toString());

						return;
					}
					case "webpage" ->
					{
						String radar_url = common.GetStringPref("RADAR_URL", "");
						Common.LogMessage("Loading RADAR_URL -> " + radar_url);
						forecast.loadUrl(radar_url);

						return;
					}
					default ->
					{
						sb.append(Common.current_html_headers);
						sb.append("Radar URL not set or is still downloading. You can go to settings to change.");
						sb.append(Common.html_footer);
					}
				}

				forceForecastRefresh(sb.toString());

				return;
			} else {
				String fctype = common.GetStringPref("fctype", "Yahoo");
				String data = common.GetStringPref("forecastData", "");

				if(data.isEmpty())
				{
					sb.append(Common.current_html_headers);
					sb.append("Forecast URL not set or is still downloading. You can go to settings to change.");
					sb.append(Common.html_footer);
					return;
				}

				switch (fctype.toLowerCase(Locale.ENGLISH))
				{
					case "yahoo" ->
					{
						String[] content = common.processYahoo(data);
						if (content == null || content.length == 0)
						{
							stopRefreshing();
							return;
						}

						String logo = "<img src='purple.png' height='29px'/>";
						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>").append(logo)
								.append("</div>").append(content[0]);
						sb.append(Common.html_footer);
					}
					case "weatherzone" ->
					{
						String[] content = common.processWZ(data);
						if (content == null || content.length == 0)
						{
							stopRefreshing();
							return;
						}

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='wz.png' height='29px'/>")
								.append("</div>").append(content[0]);
						sb.append(Common.html_footer);
					}
					case "yr.no" ->
					{
						String[] content = common.processYR(data);
						if (content == null || content.length == 0)
						{
							stopRefreshing();
							return;
						}

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='yrno.png' height='29px'/>")
								.append("</div>").append(content[0]);
						sb.append(Common.html_footer);
					}
					case "met.no" ->
					{
						String[] content = common.processMetNO(data);
						if (content == null || content.length == 0)
						{
							stopRefreshing();
							return;
						}

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='met_no.png' height='29px'/>")
								.append(content[0])
								.append("</div>");
						sb.append(Common.html_footer);
					}
					case "bom.gov.au" ->
					{
						String[] content = common.processBOM(data);
						if (content == null || content.length == 0)
						{
							stopRefreshing();
							return;
						}

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='bom.png' height='29px'/>")
								.append("</div>").append(content[0]);
						sb.append(Common.html_footer);
					}
					case "wmo.int" ->
					{
						String[] content = common.processWMO(data);
						if (content == null || content.length == 0)
						{
							stopRefreshing();
							return;
						}

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='wmo.png' height='29px'/>")
								.append("</div>").append(content[0]);
						sb.append(Common.html_footer);
					}
					case "weather.gov" ->
					{
						String[] content = common.processWGOV(data);
						if (content == null || content.length == 0)
						{
							stopRefreshing();
							return;
						}

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='wgov.png' height='29px'/>")
								.append("</div>").append(content[0]);
						sb.append(Common.html_footer);
					}
					case "weather.gc.ca" ->
					{
						String[] content = common.processWCA(data);
						if (content == null || content.length == 0)
						{
							stopRefreshing();
							return;
						}

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='wca.png' height='29px'/>")
								.append("</div>").append(content[0]);
						sb.append(Common.html_footer);
					}
					case "weather.gc.ca-fr" ->
					{
						String[] content = common.processWCAF(data);
						if (content == null || content.length == 0)
						{
							stopRefreshing();
							return;
						}

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='wca.png' height='29px'/>")
								.append(content[0]).append("</div><");
						sb.append(Common.html_footer);
					}
					case "metoffice.gov.uk" ->
					{
						String[] content = common.processMET(data);
						if (content == null || content.length == 0)
						{
							stopRefreshing();
							return;
						}

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='met.png' height='29px'/>")
								.append("</div>").append(content[0]);
					}
					case "bom2" ->
					{
						String[] content = common.processBOM2(data);
						if (content == null || content.length == 0)
						{
							stopRefreshing();
							return;
						}

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='bom.png' height='29px'/>")
								.append("</div>").append(content[0]);
						sb.append(Common.html_footer);

					}
					case "bom3" ->
					{
						String[] content = common.processBOM3(data);
						if (content == null || content.length == 0)
						{
							stopRefreshing();
							return;
						}

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='bom.png' height='29px'/>")
								.append("</div>").append(content[0]);
						sb.append(Common.html_footer);
					}
					case "aemet.es" ->
					{
						String[] content = common.processAEMET(data);
						if (content == null || content.length == 0)
						{
							stopRefreshing();
							return;
						}

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='aemet.png' height='29px'/>")
								.append("</div>").append(content[0]);
						sb.append(Common.html_footer);
					}
					case "dwd.de" ->
					{
						String[] content = common.processDWD(data);
						if (content == null || content.length == 0)
						{
							stopRefreshing();
							return;
						}

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='dwd.png' height='29px'/>")
								.append("</div>").append(content[0]);
						sb.append(Common.html_footer);
					}
					case "metservice.com" ->
					{
						String[] content = common.processMetService(data);
						if (content == null || content.length == 0)
						{
							stopRefreshing();
							return;
						}

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='metservice.png' height='29px'/>")
								.append("</div>").append(content[0]);
						sb.append(Common.html_footer);
					}
					case "openweathermap.org" ->
					{
						String[] content = common.processOWM(data);
						if (content == null || content.length == 0)
							return;

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='owm.png' height='29px'/>")
								.append("</div>").append(content[0]);
						sb.append(Common.html_footer);
					}
					case "weather.com" ->
					{
						String[] content = common.processWCOM(data);
						if (content == null || content.length == 0)
							return;

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='weather_com.png' height='29px'/>")
								.append("</div>").append(content[0]);
						sb.append(Common.html_footer);
					}
					case "met.ie" ->
					{
						String[] content = common.processMETIE(data);
						if (content == null || content.length == 0)
							return;

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='met_ie.png' height='29px'/>")
								.append("</div>").append(content[0]);
						sb.append(Common.html_footer);
					}
					case "tempoitalia.it" ->
					{
						String[] content = common.processTempoItalia(data);
						if (content == null || content.length == 0)
							return;

						sb.append(Common.current_html_headers);
						sb.append("<div style='text-align:center'>")
								.append("<img src='tempoitalia_it.png' height='29px'/>")
								.append("</div>").append(content[0]);
						sb.append(Common.html_footer);
					}
				}
			}

			forceForecastRefresh(sb.toString());
		});

		t.start();
	}

	private void stopRefreshing()
	{
		Common.LogMessage("stopRefreshing()");
		if(!swipeLayout.isRefreshing())
			return;

		Handler mHandler = new Handler(Looper.getMainLooper());
		mHandler.post(() -> swipeLayout.setRefreshing(false));

		forecast.post(() -> forecast.reload());
		current.post(() -> current.reload());
	}

	private void reloadForecast(boolean force)
	{
		Common.LogMessage("reloadForecast()");

		if(common.GetBoolPref("radarforecast", true) || (!common.checkConnection() && !force))
		{
			stopRefreshing();
			return;
		}

		final String forecast_url = common.GetStringPref("FORECAST_URL", "");

		if(forecast_url.isEmpty())
		{
			final String html = Common.current_html_headers + "Forecast URL not set. Edit inigo-settings.txt to change." + Common.html_footer;
			forceForecastRefresh(html);

			return;
		}

		Common.LogMessage("forecast checking: " + forecast_url);

		Thread t = new Thread(() ->
		{
			try
			{
				long current_time = round(System.currentTimeMillis() / 1000.0);

				if(common.GetStringPref("forecastData", "").isEmpty() ||
						common.GetLongPref("rssCheck", 0) + 7190 < current_time ||
						force)
				{
					Common.LogMessage("no forecast data or cache is more than 2 hour old or was forced...");

					String tmp = common.downloadForecast();
					if(tmp != null)
					{
						Common.LogMessage("updating rss cache");
						common.SetLongPref("rssCheck", current_time);
						common.SetStringPref("forecastData", tmp);

						drawEverything();
					}
				}
			} catch (Exception e) {
				Common.doStackOutput(e);
			}
		});

		t.start();
	}

	private void reloadWebView(boolean force)
	{
		Common.LogMessage("reloadWebView()");

		if(!common.GetBoolPref("radarforecast", true))
		{
			stopRefreshing();
			return;
		}

		if(!common.checkConnection() && !force)
		{
			Common.LogMessage("Not on wifi and not a forced refresh");
			stopRefreshing();
			return;
		}

		Common.LogMessage("reload radar...");
		final String radar = common.GetStringPref("RADAR_URL", "");

		if(radar.isEmpty())
		{
			loadWebView();
			return;
		}

		Thread t = new Thread(() ->
		{
			try
			{
				Common.LogMessage("starting to download image from: " + radar);
				File f = common.downloadRADAR(radar);
				Common.LogMessage("done downloading " + f.getAbsolutePath() + ", prompt handler to draw to movie");
				File f2 = new File(common.context.getFilesDir(), "/radar.gif");
				if(f.renameTo(f2))
					stopRefreshing();
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
		Common.LogMessage("weather.java -- adding a new filter");
	}

	public void onPause()
	{
		super.onPause();
		Common.NotificationManager.getNotificationLiveData().removeObserver(notificationObserver);
		Common.LogMessage("weather.java -- unregisterReceiver");
	}

	private final Observer<String> notificationObserver = str ->
	{
		Common.LogMessage("notificationObserver == " + str);

		if (str.equals(Common.UPDATE_INTENT) || str.equals(Common.REFRESH_INTENT))
		{
			reloadWebView(false);
			reloadForecast(false);
			drawEverything();
		}

		if(str.equals(Common.EXIT_INTENT))
			onPause();
	};
}