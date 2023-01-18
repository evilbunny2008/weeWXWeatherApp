package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static java.lang.Math.round;

public class Weather extends Fragment
{
	private final Common common;
	private View rootView;
	private WebView forecast;
	private int dark_theme;
	private SwipeRefreshLayout swipeLayout;

	Weather(Common common)
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
		rootView = inflater.inflate(R.layout.fragment_weather, container, false);

		swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
		swipeLayout.setOnRefreshListener(() ->
		{
			swipeLayout.setRefreshing(true);
			Common.LogMessage("onRefresh();");
			forceRefresh();
		});
		swipeLayout.setRefreshing(true);

		forecast = rootView.findViewById(R.id.forecast);

		forecast.getSettings().setJavaScriptEnabled(true);
		forecast.getSettings().setUserAgentString(Common.UA);

		forecast.setWebViewClient(new WebViewClient()
		{
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				return false;
			}
		});

		forecast.getViewTreeObserver().addOnScrollChangedListener(() -> swipeLayout.setEnabled(forecast.getScrollY() == 0));

		forecast.setWebChromeClient(new WebChromeClient()
		{
			@Override
			public boolean onConsoleMessage(ConsoleMessage cm)
			{
				return true;
			}
		});

		loadWebView();

		File f2 = new File(common.context.getFilesDir(), "/radar.gif");
		long[] period = common.getPeriod();

		if(!common.GetStringPref("RADAR_URL", "").equals("") && f2.lastModified() + period[0] < System.currentTimeMillis())
			reloadWebView(false);

		long curtime = Math.round(System.currentTimeMillis() / 1000.0);
		if(!common.GetBoolPref("radarforecast", true) && common.GetLongPref("rssCheck", 0) + 7190 < curtime)
			reloadForecast(false);

		return updateFields();
	}

	private void checkFields(TextView tv, String txt)
	{
		if(!tv.getText().toString().equals(txt))
			tv.setText(txt);

		if(dark_theme == 0)
		{
			tv.setTextColor(0xff000000);
			tv.setBackgroundColor(0xffffffff);
		} else {
			tv.setTextColor(0xffffffff);
			tv.setBackgroundColor(0xff000000);
		}
	}

	private View updateFields()
	{
		int iw = 17;

		Common.LogMessage("updateFields()");
		String[] bits = common.GetStringPref("LastDownload","").split("\\|");
		if(bits.length < 65)
			return rootView;

		checkFields(rootView.findViewById(R.id.textView), bits[56]);
		checkFields(rootView.findViewById(R.id.textView2), bits[54] + " " + bits[55]);

		final WebView current = rootView.findViewById(R.id.current);
		current.getSettings().setUserAgentString(Common.UA);
		current.setWebViewClient(new WebViewClient()
		{
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				return false;
			}
		});
		current.setWebChromeClient(new WebChromeClient()
		{
			@Override
			public boolean onConsoleMessage(ConsoleMessage cm)
			{
				return true;
			}
		});

		String stmp;
		final StringBuilder sb = new StringBuilder();

		String header;
		if(dark_theme == 0)
			header = "<html><head>" + Common.ssheader + "</head><body>";
		else
			header = "<html><head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head><body>";
		sb.append(header);

		Common.LogMessage("header == " + header);

		String footer = "</body></html>";

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

		stmp = "<tr><td><i style='font-size:" + iw + "px;' class='wi wi-wind wi-towards-" + bits[30].toLowerCase() + "'></i></td><td>" + bits[30] + "</td>" +
				"<td style='text-align:right;'>" + bits[6] + bits[64] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-humidity'></i></td></tr>";
		sb.append(stmp);

		String rain = bits[20] + bits[62] + " " + common.context.getString(R.string.since) + " mn";
		if(bits.length > 160 && !bits[160].equals(""))
			rain = bits[158] + bits[62] + " " + common.context.getString(R.string.since) + " " + bits[160];

		stmp = "<tr><td><i style='font-size:" + iw + "px;' class='wi wi-umbrella'></i></td><td>" + rain + "</td>" +
				"<td style='text-align:right;'>" + bits[12] + bits[60] + "</td><td><i style='font-size:" + round(iw * 1.4) + "px;' class='wi wi-raindrop'></i></td></tr>";
		sb.append(stmp);

		stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-women-sunglasses'></i></td><td>" + bits[45] + "UVI</td>" +
				"<td style='text-align:right;'>" + bits[43] + "W/m\u00B2</td><td><i style='font-size:" + iw + "px;' class='flaticon-women-sunglasses'></i></td></tr>";
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

		sb.append(footer);

		current.setWebChromeClient(new WebChromeClient()
		{
			@Override
			public boolean onConsoleMessage(ConsoleMessage cm)
			{
				Common.LogMessage(cm.message());
				return super.onConsoleMessage(cm);
			}
		});

		Handler mHandler = new Handler(Looper.getMainLooper());
		mHandler.post(() ->
		{
			current.loadDataWithBaseURL("file:///android_res/drawable/", sb.toString().trim(), "text/html", "utf-8", null);
			swipeLayout.setRefreshing(false);
		});

		return rootView;
	}

	private void forceRefresh()
	{
		swipeLayout.setRefreshing(true);
		common.getWeather();
		reloadWebView(true);
		reloadForecast(true);
		swipeLayout.setRefreshing(false);
	}

	private void loadWebView()
	{
		swipeLayout.setRefreshing(true);

		Thread t = new Thread(() ->
		{
			try
			{
				// Sleep needed to stop frames dropping while loading
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}

			final StringBuffer sb = new StringBuffer();
			String tmp;

			Handler mHandler = new Handler(Looper.getMainLooper());
			mHandler.post(() -> swipeLayout.setRefreshing(true));

			if (common.GetBoolPref("radarforecast", true))
			{
				switch (common.GetStringPref("radtype", "image"))
				{
					case "image":
					{
						String radar = common.context.getFilesDir() + "/radar.gif";

						File myFile = new File(radar);
						Common.LogMessage("myFile == " + myFile.getAbsolutePath());
						Common.LogMessage("myFile.exists() == " + myFile.exists());

						if(!myFile.exists() || common.GetStringPref("RADAR_URL", "").equals(""))
						{
							sb.append("<html><body>").append(getString(R.string.radar_url_not_set)).append("</body></html>");
						} else {
							sb.append("<!DOCTYPE html>\n" +
									"<html>\n" +
									"  <head>\n" +
									"	<meta charset='utf-8'>\n" +
									"	<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
							if (dark_theme == 1)
								sb.append("<style>body{color: #fff; background-color: #000;}</style>");

							try
							{
								File f = new File(radar);
								FileInputStream imageInFile = new FileInputStream(f);
								byte[] imageData = new byte[(int) f.length()];
								if(imageInFile.read(imageData) > 0)
									radar = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
							} catch (Exception e) {
								e.printStackTrace();
							}

							tmp = "  </head>\n" +
									"  <body>\n" +
									"\t<img style='margin:0px;padding:0px;border:0px;text-align:center;max-width:100%;width:auto;height:auto;'\n" +
									"\tsrc='" + radar + "'>\n" +
									"  </body>\n" +
									"</html>";
							sb.append(tmp);
						}
						mHandler.post(() ->
						{
							forecast.loadDataWithBaseURL(null, sb.toString(), "text/html", "utf-8", null);
							swipeLayout.setRefreshing(false);
						});
						return;
					}
					case "webpage":
						mHandler.post(() ->
						{
							String radar_url = common.GetStringPref("RADAR_URL", "");
							Common.LogMessage("Loading RADAR_URL -> " + radar_url);
							forecast.loadUrl(radar_url);
							swipeLayout.setRefreshing(false);
						});
						return;
					default:
						sb.append("<html>");
						if (dark_theme == 1)
							sb.append("<head><style>body{color: #fff; background-color: #000;}</style></head>");
						sb.append("<body>Radar URL not set or is still downloading. You can go to settings to change.</body></html>");
						break;
				}

				mHandler.post(() ->
				{
					forecast.loadDataWithBaseURL("file:///android_res/drawable/", sb.toString(), "text/html", "utf-8", null);
					swipeLayout.setRefreshing(false);
				});
			} else {
				String fctype = common.GetStringPref("fctype", "Yahoo");
				String data = common.GetStringPref("forecastData", "");

				if (data.equals(""))
				{
					sb.append("<html>");
					if (dark_theme == 1)
						sb.append("<head><style>body{color: #fff; background-color: #000;}</style></head>");
					tmp = "<body>Forecast URL not set or is still downloading. You can go to settings to change.</body></html>";
					sb.append(tmp);
					return;
				}

				switch (fctype.toLowerCase())
				{
					case "yahoo":
					{
						String[] content = common.processYahoo(data);
						if (content == null || content.length <= 0)
						{
							stopRefreshing();
							return;
						}

						String logo = "<img src='purple.png' height='29px'/><br/>";
						sb.append("<html>");
						if (dark_theme == 1)
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);
						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);
						break;
					}
					case "weatherzone":
					{
						String[] content = common.processWZ(data);
						if (content == null || content.length <= 0)
						{
							stopRefreshing();
							return;
						}

						String logo = "<img src='wz.png' height='29px'/><br/>";
						sb.append("<html>");
						if (dark_theme == 1)
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);
						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);
						break;
					}
					case "yr.no":
					{
						String[] content = common.processYR(data);
						if (content == null || content.length <= 0)
						{
							stopRefreshing();
							return;
						}

						String logo = "<img src='yrno.png' height='29px'/><br/>";
						sb.append("<html>");
						if (dark_theme == 1)
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);
						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);
						break;
					}
					case "met.no":
					{
						String[] content = common.processMetNO(data);
						if (content == null || content.length <= 0)
						{
							stopRefreshing();
							return;
						}

						String logo = "<img src='met_no.png' height='29px'/><br/>";
						sb.append("<html>");
						if (dark_theme == 1)
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);
						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);
						break;
					}
					case "bom.gov.au":
					{
						String[] content = common.processBOM(data);
						if (content == null || content.length <= 0)
						{
							stopRefreshing();
							return;
						}

						String logo = "<img src='bom.png' height='29px'/><br/>";
						sb.append("<html>");
						if (dark_theme == 1)
						{
							logo = "<img src='bom.png' style='filter:invert(100%);' height='29px'/><br/>";
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						} else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);

						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);
						break;
					}
					case "wmo.int":
					{
						String[] content = common.processWMO(data);
						if (content == null || content.length <= 0)
						{
							stopRefreshing();
							return;
						}

						String logo = "<img src='wmo.png' height='29px'/><br/>";
						sb.append("<html>");
						if (dark_theme == 1)
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);
						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);
						break;
					}
					case "weather.gov":
					{
						String[] content = common.processWGOV(data);
						if (content == null || content.length <= 0)
						{
							stopRefreshing();
							return;
						}

						String logo = "<img src='wgov.png' height='29px'/><br/>";
						sb.append("<html>");
						if (dark_theme == 1)
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);
						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);
						break;
					}
					case "weather.gc.ca":
					{
						String[] content = common.processWCA(data);
						if (content == null || content.length <= 0)
						{
							stopRefreshing();
							return;
						}

						String logo = "<img src='wca.png' height='29px'/><br/>";
						sb.append("<html>");
						if (dark_theme == 1)
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);
						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);
						break;
					}
					case "weather.gc.ca-fr":
					{
						String[] content = common.processWCAF(data);
						if (content == null || content.length <= 0)
						{
							stopRefreshing();
							return;
						}

						String logo = "<img src='wca.png' height='29px'/><br/>";
						sb.append("<html>");
						if (dark_theme == 1)
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);
						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);
						break;
					}
					case "metoffice.gov.uk":
					{
						String[] content = common.processMET(data);
						if (content == null || content.length <= 0)
						{
							stopRefreshing();
							return;
						}

						String logo = "<img src='met.png' height='29px'/><br/>";
						sb.append("<html>");
						if (dark_theme == 1)
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);
						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);
						break;
					}
					case "bom2":
					{
						String[] content = common.processBOM2(data);
						if (content == null || content.length <= 0)
						{
							stopRefreshing();
							return;
						}

						String logo = "<img src='bom.png' height='29px'/><br/>";
						sb.append("<html>");
						if(dark_theme == 1)
						{
							logo = "<img src='bom.png' style='filter:invert(100%);' height='29px'/><br/>";
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						} else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);
						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);

						break;
					}
					case "bom3":
					{
						String[] content = common.processBOM3(data);
						if (content == null || content.length <= 0)
						{
							stopRefreshing();
							return;
						}

						String logo = "<img src='bom.png' height='29px'/><br/>";
						sb.append("<html>");
						if(dark_theme == 1)
						{
							logo = "<img src='bom.png' style='filter:invert(100%);' height='29px'/><br/>";
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						} else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);
						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);

						break;
					}
					case "aemet.es":
					{
						String[] content = common.processAEMET(data);
						if (content == null || content.length <= 0)
						{
							stopRefreshing();
							return;
						}

						String logo = "<img src='aemet.png' height='29px'/><br/>";
						sb.append("<html>");
						if (dark_theme == 1)
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);
						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);
						break;
					}
					case "dwd.de":
					{
						String[] content = common.processDWD(data);
						if (content == null || content.length <= 0)
						{
							stopRefreshing();
							return;
						}

						String logo = "<img src='dwd.png' height='29px'/><br/>";
						sb.append("<html>");
						if (dark_theme == 1)
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);
						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);
						break;
					}
					case "metservice.com":
					{
						String[] content = common.processMetService(data);
						if (content == null || content.length <= 0)
						{
							stopRefreshing();
							return;
						}

						String logo = "<img src='metservice.png' height='29px'/><br/>";
						sb.append("<html>");

						if(dark_theme == 1)
						{
							logo = "<img src='metservice.png' style='filter:invert(100%);' height='29px'/><br/>";
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						} else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);
						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);
						break;
					}
					case "openweathermap.org":
					{
						String[] content = common.processOWM(data);
						if (content == null || content.length <= 0)
							return;

						String logo = "<img src='owm.png' height='29px'/><br/>";
						sb.append("<html>");
						if(dark_theme == 1)
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);

						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);
						break;
					}
					case "weather.com":
					{
						String[] content = common.processWCOM(data);
						if (content == null || content.length <= 0)
							return;

						String logo = "<img src='weather_com.png' height='29px'/><br/>";
						sb.append("<html>");
						if(dark_theme == 1)
						{
							logo = "<img src='weather_com.png' style='filter:invert(100%);' height='29px'/><br/>";
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						} else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);

						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);
						break;
					}
					case "met.ie":
					{
						String[] content = common.processMETIE(data);
						if (content == null || content.length <= 0)
							return;

						String logo = "<img src='met_ie.png' height='29px'/><br/>";
						sb.append("<html>");
						if(dark_theme == 1)
						{
							logo = "<img src='met_ie.png' style='filter:invert(100%);' height='29px'/><br/>";
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						} else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);

						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);
						break;
					}
					case "tempoitalia.it":
					{
						String[] content = common.processTempoItalia(data);
						if (content == null || content.length <= 0)
							return;

						String logo = "<img src='tempoitalia_it.png' height='29px'/><br/>";
						sb.append("<html>");
						if(dark_theme == 1)
							tmp = "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
						else
							tmp = "<head>" + Common.ssheader + "</head>";
						sb.append(tmp);

						tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						sb.append(tmp);
						break;
					}
				}
			}

			mHandler.post(() ->
			{
				forecast.clearFormData();
				forecast.clearHistory();
				forecast.clearCache(true);
				forecast.loadDataWithBaseURL("file:///android_res/drawable/", sb.toString(), "text/html", "utf-8", null);
				swipeLayout.setRefreshing(false);
			});
		});

		t.start();
	}

	private void stopRefreshing()
	{
		Handler mHandler = new Handler(Looper.getMainLooper());
		mHandler.post(() -> swipeLayout.setRefreshing(false));
	}

	private void reloadForecast(boolean force)
	{
		if(common.GetBoolPref("radarforecast", true))
		{
			stopRefreshing();
			return;
		}

		final String forecast_url = common.GetStringPref("FORECAST_URL", "");

		if(forecast_url.equals(""))
		{
			final String html = "<html><body>Forecast URL not set. Edit inigo-settings.txt to change.</body></html>";
			Handler mHandler = new Handler(Looper.getMainLooper());
			mHandler.post(() ->
			{
				forecast.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
				swipeLayout.setRefreshing(false);
			});

			return;
		}

		if(!common.checkConnection() && !force)
		{
			Common.LogMessage("Not on wifi and not a forced refresh");
			if(swipeLayout.isRefreshing())
				swipeLayout.setRefreshing(false);
			return;
		}

		if(!swipeLayout.isRefreshing())
			swipeLayout.setRefreshing(true);

		Common.LogMessage("forecast checking: " + forecast_url);

		Thread t = new Thread(() ->
		{
			try
			{
				long curtime = round(System.currentTimeMillis() / 1000.0);

				if(common.GetStringPref("forecastData", "").equals("") || common.GetLongPref("rssCheck", 0) + 7190 < curtime)
				{
					Common.LogMessage("no forecast data or cache is more than 2 hour old");

					String tmp = common.downloadForecast();
					if(tmp != null)
					{
						Common.LogMessage("updating rss cache");
						common.SetLongPref("rssCheck", curtime);
						common.SetStringPref("forecastData", tmp);

						Handler mHandler1 = new Handler(Looper.getMainLooper());
						mHandler1.post(() ->
						{
							swipeLayout.setRefreshing(false);
							common.SendRefresh();
						});
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		t.start();
	}

	private void reloadWebView(boolean force)
	{
		if(!common.GetBoolPref("radarforecast", true))
			return;

		Common.LogMessage("reload radar...");
		final String radar = common.GetStringPref("RADAR_URL", "");

		if(radar.equals(""))
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

		if(!swipeLayout.isRefreshing())
			swipeLayout.setRefreshing(true);

		Thread t = new Thread(() ->
		{
			try
			{
				Common.LogMessage("starting to download image from: " + radar);
				File f = common.downloadRADAR(radar);
				Common.LogMessage("done downloading " + f.getAbsolutePath() + ", prompt handler to draw to movie");
				File f2 = new File(common.context.getFilesDir(), "/radar.gif");
				if(f.renameTo(f2))
				{
					Handler mHandler1 = new Handler(Looper.getMainLooper());
					mHandler1.post(() ->
					{
						swipeLayout.setRefreshing(false);
						common.SendRefresh();
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		t.start();
	}

	public void onResume()
	{
		super.onResume();
		updateFields();
		loadWebView();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Common.UPDATE_INTENT);
		filter.addAction(Common.EXIT_INTENT);
		filter.addAction(Common.REFRESH_INTENT);
		common.context.registerReceiver(serviceReceiver, filter);
		Common.LogMessage("weather.java -- adding a new filter");
	}

	public void onPause()
	{
		super.onPause();
		try
		{
			common.context.unregisterReceiver(serviceReceiver);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		Common.LogMessage("weather.java -- unregisterReceiver");
	}

	private final BroadcastReceiver serviceReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			try
			{
				String action = intent.getAction();
				if(action != null && action.equals(Common.UPDATE_INTENT))
				{
					Common.LogMessage("Weather() We have a update_intent, so we should probably update the screen.");
					dark_theme = common.GetIntPref("dark_theme", 2);
					if(dark_theme == 2)
						dark_theme = common.getSystemTheme();
					updateFields();
					reloadWebView(false);
					reloadForecast(false);
				} else if(action != null && action.equals(Common.REFRESH_INTENT)) {
					Common.LogMessage("Weather() We have a refresh_intent, so we should probably update the screen.");
					dark_theme = common.GetIntPref("dark_theme", 2);
					if(dark_theme == 2)
						dark_theme = common.getSystemTheme();
					updateFields();
					loadWebView();
				} else if(action != null && action.equals(Common.EXIT_INTENT)) {
					onPause();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
}