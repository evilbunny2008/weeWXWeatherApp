package com.odiousapps.weewxweather;

import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static java.lang.Math.round;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Weather extends Fragment implements View.OnClickListener
{
	private WebView forecast;
	private WebView current;
	private SwipeRefreshLayout swipeLayout;
	private MaterialCheckBox floatingCheckBox;
	private LinearLayout fll;
	private boolean isVisible = false;
	private boolean isRunning = false;
	private boolean disableSwipeOnRadar;
	private boolean disabledSwipe;
	private MainActivity activity;
	private MaterialTextView tv1, tv2;

	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		activity = (MainActivity)getActivity();

		View rootView = inflater.inflate(R.layout.fragment_weather, container, false);

		fll = rootView.findViewById(R.id.floating_linear_layout);

		tv1 = rootView.findViewById(R.id.textView);
		tv2 = rootView.findViewById(R.id.textView2);

		tv1.setTextColor(KeyValue.fgColour);
		tv2.setTextColor(KeyValue.fgColour);

		swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
		swipeLayout.setBackgroundColor(KeyValue.bgColour);
		fll.setBackgroundColor(KeyValue.bgColour);

		swipeLayout.setOnRefreshListener(() ->
		{
			swipeLayout.setRefreshing(true);
			Common.LogMessage("onRefresh();");
			forceRefresh();
		});

		floatingCheckBox = rootView.findViewById(R.id.floatingCheckBoxMain);
		floatingCheckBox.setOnClickListener(this);

		forecast = rootView.findViewById(R.id.forecast);
		Common.setWebview(forecast);

		current = rootView.findViewById(R.id.current);
		Common.setWebview(current);

		current.getViewTreeObserver().addOnScrollChangedListener(() ->
				swipeLayout.setEnabled(current.getScrollY() == 0));

		current.setWebViewClient(new WebViewClient()
		{
			@Override
			public void onPageFinished(WebView view, String url)
			{
				super.onPageFinished(view, url);
				stopRefreshing();

				// Post a Runnable to make sure contentHeight is available
				view.postDelayed(() ->
				{
					int contentHeightPx = (int)(current.getContentHeight() *
							current.getResources().getDisplayMetrics().density) +
							tv1.getHeight() + tv2.getHeight();
					ViewGroup.LayoutParams params = swipeLayout.getLayoutParams();
					params.height = contentHeightPx; // - (int)(5 * current.getResources().getDisplayMetrics().density);
					swipeLayout.setLayoutParams(params);
					Common.LogMessage("New Height: " + contentHeightPx);
					//view.setLayoutParams(params);
				}, 100); // 100ms delay lets the page finish rendering
			}
		});

		loadWebView();
		drawEverything();

		return rootView;
	}

	private void checkFields(TextView tv, String txt)
	{
		if(!tv.getText().toString().equals(txt))
			tv.post(() -> tv.setText(txt));
	}

	void drawEverything()
	{
		String[] bits;

		Common.LogMessage("drawEverything()");

		File f2 = new File(Common.getFilesDir(), "/radar.gif");
		long[] period = Common.getPeriod();

		String radarURL = Common.GetStringPref("RADAR_URL", "");
		if(radarURL == null)
			return;

		if(!radarURL.isEmpty() && f2.lastModified() + period[0] < System.currentTimeMillis())
			reloadWebView(false);

		long current_time = Math.round(System.currentTimeMillis() / 1000.0);
		if(!Common.GetBoolPref("radarforecast", true) && Common.GetLongPref("rssCheck", 0) + 7190 < current_time)
			reloadForecast(false);

		Common.LogMessage("updateFields()");
		String lastDownload = Common.GetStringPref("LastDownload","");
		if(lastDownload != null && !lastDownload.isEmpty())
		{
			bits = lastDownload.split("\\|");
			if (bits.length < 65)
				return;
		} else
			return;

		checkFields(tv1, bits[56]);
		checkFields(tv2, bits[54] + " " + bits[55]);

		final StringBuilder sb = new StringBuilder();

		sb.append(Common.current_html_headers);

		sb.append("\n<div class='todayCurrent'>\n");
		sb.append("\t<div class='topRowCurrent'>\n");
		sb.append("\t\t<div class='mainTemp'>");
		sb.append(bits[0]).append(bits[60]);
		sb.append("</div>\n");

		sb.append("\t\t<div class='apparentTemp'>AT: ");
		if(bits.length > 203)
			sb.append(bits[203]).append(bits[60]);
		else
			sb.append("&nbsp;");
		sb.append("</div>\n\t</div>\n\n");

		sb.append("\t<div class='dataTable'>\n");

		sb.append("\t\t<div class='dataRowCurrent'>\n");

		sb.append("\t\t\t<div class='dataCell'><i class='flaticon-windy icon'></i>")
				.append(bits[25]).append(bits[61]).append("</div>\n");
		sb.append("\t\t\t<div class='dataCell right'>")
				.append(bits[37]).append(bits[63]).append("<i class='wi wi-barometer icon'></i></div>\n");

		sb.append("\t\t</div>\n");

		sb.append("\t\t<div class='dataRowCurrent'>\n");

		sb.append("\t\t\t<div class='dataCell'><i class='wi wi-wind wi-towards-").append(bits[30].toLowerCase(Locale.ENGLISH))
				.append(" icon'></i>").append(bits[30]).append("</div>\n");
		sb.append("\t\t\t<div class='dataCell right'>").append(bits[6]).append(bits[64])
				.append("<i class='wi wi-humidity icon'></i></div>\n");

		sb.append("\t\t</div>\n");

		sb.append("\t\t<div class='dataRowCurrent'>\n");

		String rain = bits[20] + bits[62] + " " + Common.getString(R.string.since) + " mm";
		if(bits.length > 160 && !bits[160].isEmpty())
			rain = bits[158] + bits[62] + " " + Common.getString(R.string.since) + " " + bits[160];

		sb.append("\t\t\t<div class='dataCell'><i class='wi wi-umbrella icon'></i>")
				.append(rain).append("</div>\n");
		sb.append("\t\t\t<div class='dataCell right'>").append(bits[12]).append(bits[60])
				.append("<i class='wi wi-raindrop icon' style='font-size:24px;'></i></div>");

		sb.append("\t\t</div>\n");

		sb.append("\t\t<div class='dataRowCurrent'>\n");

		sb.append("\t\t\t<div class='dataCell'><i class='flaticon-women-sunglasses icon'></i>")
				.append(bits[45]).append(" UVI</div>\n");
		sb.append("\t\t\t<div class='dataCell right'>").append(bits[43])
				.append(" W/m²<i class='flaticon-women-sunglasses icon'></i></div>");

		sb.append("\t\t</div>\n");

		if(bits.length > 202 && Common.GetBoolPref("showIndoor", false))
		{
			sb.append("\t\t<div class='dataRowCurrent'>\n");

			sb.append("\t\t\t<div class='dataCell'><i class='flaticon-home-page icon'></i>")
					.append(bits[161]).append(bits[60]).append("</div>\n");
			sb.append("\t\t\t<div class='dataCell right'>").append(bits[166]).append(bits[64])
					.append(" <i class='flaticon-home-page icon'></i></div>\n");

			sb.append("\t\t</div>\n");
		}

		sb.append("\t</div>\n\n");

		sb.append("\t<div class='dataRowCurrent'>\n");

		sb.append("\t\t<div class='dataCell'><i class='wi wi-sunrise icon'></i> ").append(bits[57]).append("</div>\n");
		sb.append("\t\t<div class='dataCell right'>").append(bits[58]).append(" <i class='wi wi-sunset icon'></i></div>\n");

		sb.append("\t</div>\n\n");

		sb.append("\t<div class='dataRowCurrent'>\n");

		sb.append("\t\t<div class='dataCell'><i class='wi wi-moonrise icon'></i> ").append(bits[47]).append("</div>\n");
		sb.append("\t\t<div class='dataCell right'>").append(bits[48]).append(" <i class='wi wi-moonset icon'></i></div>\n");

		sb.append("\t</div>\n</div>\n");

		sb.append(Common.html_footer);

		//CustomDebug.writeDebug("current_weewx.html", sb.toString());

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
			forecast.loadDataWithBaseURL("file:///android_res/drawable/", str,
					"text/html", "utf-8", null);
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
			current.loadDataWithBaseURL("file:///android_res/drawable/", str,
					"text/html", "utf-8", null);
		});
	}

	private void forceRefresh()
	{
		Common.LogMessage("forceRefresh()");
		Common.getWeather();
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
			sb.append(Common.current_html_headers);

			if(Common.GetBoolPref("radarforecast", true))
			{
				if(fll.getVisibility() != View.VISIBLE)
					fll.post(() -> fll.setVisibility(View.VISIBLE));
				updateSwipe();

				String radtype = Common.GetStringPref("radtype", "image");
				if(radtype == null)
				{
					stopRefreshing();
					return;
				}

				switch(radtype)
				{
					case "webpage" ->
					{
						String radar_url = Common.GetStringPref("RADAR_URL", "");
						if(radar_url == null)
						{
							stopRefreshing();
							return;
						}

						Common.LogMessage("Loading RADAR_URL -> " + radar_url);
						forecast.post(() -> forecast.loadUrl(radar_url));
						stopRefreshing();
						return;
					}
					case "image" ->
					{
						String radarURL = Common.GetStringPref("RADAR_URL", "");
						if(radarURL == null)
						{
							stopRefreshing();
							return;
						}

						String radar = Common.getFilesDir() + "/radar.gif";

						File myFile = new File(radar);
						Common.LogMessage("myFile == " + myFile.getAbsolutePath());
						Common.LogMessage("myFile.exists() == " + myFile.exists());

						if(!myFile.exists() || radarURL.isEmpty())
						{
							sb.append(getString(R.string.radar_url_not_set));
						} else {
							try
							{
								File f = new File(radar);
								try (FileInputStream imageInFile = new FileInputStream(f))
								{
									byte[] imageData = new byte[(int) f.length()];
									if(imageInFile.read(imageData) > 0)
										radar = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
								} catch (Exception e) {
									Common.doStackOutput(e);
								}
							} catch (Exception e)
							{
								Common.doStackOutput(e);
							}

							sb.append("\t<img class='radarImage' alt='Radar Image' src='").append(radar).append("' />\n\n");
						}
					}
					default -> sb.append("Radar URL not set or is still downloading. You can go to settings to change.");
				}
			} else {
				if(fll.getVisibility() != View.GONE)
					fll.post(() -> fll.setVisibility(View.GONE));
				updateSwipe();
				String fctype = Common.GetStringPref("fctype", "Yahoo");
				if(fctype == null || fctype.isEmpty())
				{
					stopRefreshing();
					return;
				}

				String data = Common.GetStringPref("forecastData", "");

				if(data == null || data.isEmpty())
				{
					sb.append("Forecast URL not set or is still downloading. You can go to settings to change.");
				} else
				{
					switch(fctype.toLowerCase(Locale.ENGLISH))
					{
						case "yahoo" ->
						{
							String[] content = Common.processYahoo(data);
							if(content == null || content.length == 0)
							{
								stopRefreshing();
								return;
							}

							sb.append("<div style='text-align:center'>").append("<img src='purple.png' height='29px'/>").append("</div>\n").append(content[0]);
						}
						case "weatherzone" ->
						{
							String[] content = Common.processWZ(data);
							if(content == null || content.length == 0)
							{
								stopRefreshing();
								return;
							}

							sb.append("<div style='text-align:center'>").append("<img src='wz.png' height='29px'/>").append("</div>\n").append(content[0]);
						}
						case "yr.no" ->
						{
							String[] content = Common.processYR(data);
							if(content == null || content.length == 0)
							{
								stopRefreshing();
								return;
							}

							sb.append("<div style='text-align:center'>").append("<img src='yrno.png' height='29px'/>").append("</div>\n").append(content[0]);
						}
						case "met.no" ->
						{
							String[] content = Common.processMetNO(data);
							if(content == null || content.length == 0)
							{
								stopRefreshing();
								return;
							}

							sb.append("<div style='text-align:center'>").append("<img src='met_no.png' height='29px'/>").append("</div>\n").append(content[0]);
						}
						case "wmo.int" ->
						{
							String[] content = Common.processWMO(data);
							if(content == null || content.length == 0)
							{
								stopRefreshing();
								return;
							}

							sb.append("<div style='text-align:center'>").append("<img src='wmo.png' height='29px'/>").append("</div>\n").append(content[0]);
						}
						case "weather.gov" ->
						{
							String[] content = Common.processWGOV(data);
							if(content == null || content.length == 0)
							{
								stopRefreshing();
								return;
							}

							sb.append("<div style='text-align:center'>").append("<img src='wgov.png' height='29px'/>").append("</div>\n").append(content[0]);
						}
						case "weather.gc.ca" ->
						{
							String[] content = Common.processWCA(data);
							if(content == null || content.length == 0)
							{
								stopRefreshing();
								return;
							}

							sb.append("<div style='text-align:center'>").append("<img src='wca.png' height='29px'/>").append("</div>\n").append(content[0]);
						}
						case "weather.gc.ca-fr" ->
						{
							String[] content = Common.processWCAF(data);
							if(content == null || content.length == 0)
							{
								stopRefreshing();
								return;
							}

							sb.append("<div style='text-align:center'>").append("<img src='wca.png' height='29px'/>").append("</div><").append(content[0]);
						}
						case "metoffice.gov.uk" ->
						{
							String[] content = Common.processMET(data);
							if(content == null || content.length == 0)
							{
								stopRefreshing();
								return;
							}

							sb.append("<div style='text-align:center'>").append("<img src='met.png' height='29px'/>").append("</div>\n").append(content[0]);
						}
						case "bom.gov.au", "bom2", "bom3" ->
						{
							String[] content = Common.processBOM3(data);
							if(content == null || content.length == 0)
							{
								stopRefreshing();
								return;
							}

							sb.append("\n<div style='text-align:center'>\n\t");
							if(KeyValue.theme == R.style.AppTheme_weeWxWeatherApp_Dark_Common)
								sb.append("<img src='bom.png' style='filter:invert(1);' height='29px' />");
							else
								sb.append("<img src='bom.png' height='29px' />");
							sb.append("\n</div>\n").append(content[0]);
						}
						case "aemet.es" ->
						{
							String[] content = Common.processAEMET(data);
							if(content == null || content.length == 0)
							{
								stopRefreshing();
								return;
							}

							sb.append("<div style='text-align:center'>").append("<img src='aemet.png' height='29px'/>").append("</div>\n").append(content[0]);
						}
						case "dwd.de" ->
						{
							String[] content = Common.processDWD(data);
							if(content == null || content.length == 0)
							{
								stopRefreshing();
								return;
							}

							sb.append("<div style='text-align:center'>").append("<img src='dwd.png' height='29px'/>").append("</div>\n").append(content[0]);
						}
						case "metservice.com" ->
						{
							String[] content = Common.processMetService(data);
							if(content == null || content.length == 0)
							{
								stopRefreshing();
								return;
							}

							sb.append("<div style='text-align:center'>").append("<img src='metservice.png' height='29px'/>").append("</div>\n").append(content[0]);
						}
						case "openweathermap.org" ->
						{
							String[] content = Common.processOWM(data);
							if(content == null || content.length == 0)
								return;

							sb.append("<div style='text-align:center'>").append("<img src='owm.png' height='29px'/>").append("</div>\n").append(content[0]);
						}
						case "weather.com" ->
						{
							String[] content = Common.processWCOM(data);
							if(content == null || content.length == 0)
							{
								stopRefreshing();
								return;
							}

							sb.append("<div style='text-align:center'>").append("<img src='weather_com.png' height='29px'/>").append("</div>\n").append(content[0]);
						}
						case "met.ie" ->
						{
							String[] content = Common.processMETIE(data);
							if(content == null || content.length == 0)
							{
								stopRefreshing();
								return;
							}

							sb.append("<div style='text-align:center'>").append("<img src='met_ie.png' height='29px'/>").append("</div>\n").append(content[0]);
						}
						case "tempoitalia.it" ->
						{
							String[] content = Common.processTempoItalia(data);
							if(content == null || content.length == 0)
							{
								stopRefreshing();
								return;
							}

							sb.append("<div style='text-align:center'>").append("<img src='tempoitalia_it.png' height='29px'/>").append("</div>\n").append(content[0]);
						}
					}
				}
			}

			sb.append(Common.html_footer);

			String str = sb.toString();
			//CustomDebug.writeDebug("webview_weewx.html", str);

			forceForecastRefresh(str);
		});

		t.start();
	}

	private void stopRefreshing()
	{
		Common.LogMessage("stopRefreshing()");
		if(!swipeLayout.isRefreshing())
			return;

		forecast.post(() -> forecast.reload());
		current.post(() -> current.reload());
		swipeLayout.post(() -> swipeLayout.setRefreshing(false));
	}

	private void reloadForecast(boolean force)
	{
		Common.LogMessage("reloadForecast()");

		if(Common.GetBoolPref("radarforecast", true) || (!Common.checkConnection() && !force))
		{
			stopRefreshing();
			return;
		}

		final String forecast_url = Common.GetStringPref("FORECAST_URL", "");

		if(forecast_url == null || forecast_url.isEmpty())
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
				String forecastData = Common.GetStringPref("forecastData", "");

				long current_time = round(System.currentTimeMillis() / 1000.0);

				if(forecastData == null || forecastData.isEmpty() ||
						Common.GetLongPref("rssCheck", 0) + 7190 < current_time ||
						force)
				{
					Common.LogMessage("no forecast data or cache is more than 2 hour old or was forced...");

					String tmp = Common.downloadForecast();
					if(tmp != null)
					{
						Common.LogMessage("updating rss cache");
						Common.SetLongPref("rssCheck", current_time);
						Common.SetStringPref("forecastData", tmp);

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

		if(!Common.GetBoolPref("radarforecast", true))
		{
			stopRefreshing();
			return;
		}

		if(!Common.checkConnection() && !force)
		{
			Common.LogMessage("Not on wifi and not a forced refresh");
			stopRefreshing();
			return;
		}

		Common.LogMessage("reload radar...");
		final String radar = Common.GetStringPref("RADAR_URL", "");

		if(radar == null || radar.isEmpty())
		{
			loadWebView();
			return;
		}

		Thread t = new Thread(() ->
		{
			if(fll.getVisibility() != View.VISIBLE)
				fll.post(() -> fll.setVisibility(View.VISIBLE));
			updateSwipe();

			try
			{
				Common.LogMessage("starting to download image from: " + radar);
				File f = Common.downloadRADAR(radar);
				if(f == null || f.length() == 0)
				{
					Common.LogMessage("failed to download radar image");
					stopRefreshing();
					return;
				}

				Common.LogMessage("done downloading " + f.getAbsolutePath() + ", prompt handler to draw to movie");
				File f2 = new File(Common.getFilesDir(), "/radar.gif");
				f.renameTo(f2);
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

		Common.LogMessage("Weather.onResume()");

		if(isVisible)
			return;

		isVisible = true;

		Common.LogMessage("Weather.onResume()-- adding notification manager...");
		Common.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);

		Common.LogMessage("Weather.onResume() -- updating the value of the floating checkbox...");
		disableSwipeOnRadar = Common.GetBoolPref("disableSwipeOnRadar", false);
		floatingCheckBox.setChecked(disableSwipeOnRadar);
		updateSwipe();

		if(isRunning)
			return;

		isRunning = true;
		swipeLayout.setRefreshing(true);
	}

	public void onPause()
	{
		super.onPause();

		Common.LogMessage("Weather.onPause()");

		if(!isVisible)
			return;

		isVisible = false;

		Common.NotificationManager.getNotificationLiveData().removeObserver(notificationObserver);
		doPause();

		Common.LogMessage("Weather.onPause()-- removing notification manager...");
	}

	private final Observer<String> notificationObserver = str ->
	{
		Common.LogMessage("notificationObserver == " + str);

		if(str.equals(Common.UPDATE_INTENT) || str.equals(Common.REFRESH_INTENT))
		{
			reloadWebView(false);
			reloadForecast(false);
			drawEverything();
		}

		if(str.equals(Common.EXIT_INTENT))
			onPause();
	};

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
	void updateSwipe()
	{
		if(!isVisible)
			return;

		if((disabledSwipe && disableSwipeOnRadar) || (!disabledSwipe && !disableSwipeOnRadar))
			return;

		if(!disabledSwipe && disableSwipeOnRadar && fll.getVisibility() == View.VISIBLE)
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
		updateSwipe();
		Common.LogMessage("Forecast.onClick() finished...");
	}
}