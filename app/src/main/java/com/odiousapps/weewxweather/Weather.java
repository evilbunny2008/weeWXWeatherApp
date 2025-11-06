package com.odiousapps.weewxweather;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class Weather extends Fragment implements View.OnClickListener
{
	private boolean isVisible;
	private String lastURL;
	private Thread forecastThread, radarThread;
	private long ftStart, rtStart;
	private WebView current, forecast;
	private SwipeRefreshLayout swipeLayout;
	private MaterialCheckBox floatingCheckBox;
	private LinearLayout fll;
	private boolean disableSwipeOnRadar;
	private boolean disabledSwipe;
	private MainActivity activity;
	private MaterialTextView tv1, tv2;
	private final ViewTreeObserver.OnScrollChangedListener scl =
			() -> swipeLayout.setEnabled(current.getScrollY() == 0);

	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		Common.LogMessage("Weather.onCreateView()");

		activity = (MainActivity)getActivity();

		View rootView = inflater.inflate(R.layout.fragment_weather, container, false);

		fll = rootView.findViewById(R.id.floating_linear_layout);
		LinearLayout currentll = rootView.findViewById(R.id.currentLinearLayout);
		currentll.setBackgroundColor(KeyValue.bgColour);

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

		return rootView;
	}

	private WebView loadWebview(WebView webView, View view, int viewid,
	                            boolean doSwipe, boolean dynamicSizing)
	{
		boolean wasNull = webView == null;

		if(wasNull)
			webView = WebViewPreloader.getInstance().getWebView(requireContext());

		if(webView.getParent() != null)
			((ViewGroup)webView.getParent()).removeView(webView);

		FrameLayout frameLayout = view.findViewById(viewid);
		frameLayout.removeAllViews();
		frameLayout.addView(webView);

		if(doSwipe)
			webView.getViewTreeObserver().addOnScrollChangedListener(scl);

		webView.setWebViewClient(new WebViewClient()
		{
			@Override
			public void onPageFinished(WebView wv, String url)
			{
				super.onPageFinished(wv, url);

				if(dynamicSizing)
				{
					Common.LogMessage("dynamicSizing is true...");
					// Post a Runnable to make sure contentHeight is available
					view.postDelayed(() ->
					{
						float density =  weeWXApp.getDensity();
						int contentHeightPx = tv1.getHeight() + tv2.getHeight() +
						                      (int)(wv.getContentHeight() * density);
						ViewGroup.LayoutParams params = swipeLayout.getLayoutParams();
						params.height = contentHeightPx; // - (int)(5 * density);
						swipeLayout.setLayoutParams(params);
						Common.LogMessage("New Height: " + contentHeightPx);
					}, 100); // 100ms delay lets the page finish rendering
				}

				rtStart = ftStart = 0;

				stopRefreshing();
			}
		});

		if(wasNull)
			return webView;

		return null;
	}

	void doInitialLoad(View view, Bundle savedInstanceState)
	{
		Common.LogMessage("Weather.doInitialLoad()");

		boolean dynamicSizing = weeWXApp.getHeight() > weeWXApp.getWidth() && weeWXApp.getWidth() < 1100;

		if(current == null)
			current = loadWebview(null, view, R.id.current, true, dynamicSizing);
		else
			loadWebview(current, view, R.id.current, true, dynamicSizing);

		if(forecast == null)
			forecast = loadWebview(null, view, R.id.forecast, false, false);
		else
			loadWebview(forecast, view, R.id.forecast, false, false);

		if(savedInstanceState != null)
		{
			Common.LogMessage("Weather.doInitialLoad() loading from savedInstanceState...");
			current.restoreState(savedInstanceState);
			forecast.restoreState(savedInstanceState);
		}

		Common.LogMessage("Weather.doInitialLoad() doing full load...");
		drawEverything();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		Common.LogMessage("Weather.onViewCreated()");
		super.onViewCreated(view, savedInstanceState);

		swipeLayout.setRefreshing(true);

		Common.LogMessage("Weather.onViewCreated()-- adding notification manager...");
		Common.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);

		doInitialLoad(view, savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState)
	{
		Common.LogMessage("Weather.onSaveInstanceState()");
		super.onSaveInstanceState(outState);

		if(current != null)
			current.saveState(outState);

		if(forecast != null)
			forecast.saveState(outState);
	}

	@Override
	public void onDestroyView()
	{
		Common.LogMessage("Weather.onDestroyView()");
		super.onDestroyView();

		Common.LogMessage("Weather.onDestroyView()-- removing notification manager...");
		Common.NotificationManager.getNotificationLiveData().removeObserver(notificationObserver);

		if(current != null)
		{
			ViewParent parent = current.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(current);

			current.getViewTreeObserver().removeOnScrollChangedListener(scl);

			WebViewPreloader.getInstance().recycleWebView(current);

			Common.LogMessage("Weather.onDestroyView() recycled current...");
		}

		if(forecast != null)
		{
			ViewParent parent = forecast.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(forecast);

			WebViewPreloader.getInstance().recycleWebView(forecast);

			Common.LogMessage("Weather.onDestroyView() recycled forecast...");
		}
	}

	private void checkFields(TextView tv, String txt)
	{
		if(!tv.getText().toString().equals(txt))
			tv.post(() -> tv.setText(txt));
	}

	void drawEverything()
	{
		Common.LogMessage("drawEverything()");

		if(Common.GetBoolPref("radarforecast", true))
		{
			String radtype = Common.GetStringPref("radtype", "image");
			String radarURL = Common.GetStringPref("RADAR_URL", "");
			if(radarURL == null || radarURL.isBlank() || radtype == null || radtype.isBlank())
			{
				Common.LogMessage("radarURL: " + radarURL + ", radtype: " + radtype);
				return;
			}

			if(radtype.equals("image"))
			{
				Common.LogMessage("Update the radar image...");
				long[] perioda = Common.getPeriod();
				long period = Math.round(perioda[0] / 1000.0);

				int pos = Common.GetIntPref("updateInterval", 1);
				if(pos < 0)
					return;

				File f2 = Common.getFile("radar.gif");
				long current_time = Math.round(System.currentTimeMillis() / 1000.0);
				reloadWebView(Common.getModifiedTime(f2) + period < current_time);
			} else {
				Common.LogMessage("Update the radar webview page...");
				loadWebViewURL(radarURL, false);
			}
		} else {
			Common.LogMessage("Show the forecast...");
			reloadForecast(false);
		}

		Common.LogMessage("updateFields()", true);
		String lastDownload = Common.GetStringPref("LastDownload",null);
		if(lastDownload == null || lastDownload.isBlank())
		{
			Common.LogMessage("lastDownload is null or blank...", true);
			return;
		}

		String[] bits = lastDownload.split("\\|");
		if(bits.length < 65)
		{
			Common.LogMessage("bits.length < 65");
			return;
		}

		Common.LogMessage("bits.length: " + bits.length, true);

		checkFields(tv1, bits[56]);
		checkFields(tv2, bits[54] + " " + bits[55]);

		final String currentSpacerLeft = "<span class='currentSpacer left'></span>";
		final String currentSpacerRight = "<span class='currentSpacer right'></span>";

		final StringBuilder sb = new StringBuilder();
		sb.append("\n<div class='todayCurrent'>\n");
		sb.append("\t<div class='topRowCurrent'>\n");
		sb.append("\t\t<div class='mainTemp'>");
		sb.append(bits[0]).append(bits[60]);
		sb.append("</div>\n");

		sb.append("\t\t<div class='apparentTemp'>AT:<br/>");
		if(bits.length > 203)
			sb.append(bits[203]).append(bits[60]);
		else
			sb.append(Common.emptyField);
		sb.append("</div>\n\t</div>\n\n");

		sb.append("\t<div class='dataTableCurrent'>\n");

		sb.append("\t\t<div class='dataRowCurrent'>\n");

		sb.append("\t\t\t<div class='dataCellCurrent'><i class='flaticon-windy icon'></i>")
				.append(currentSpacerLeft)
				.append(bits[25]).append(bits[61]).append("</div>\n");
		sb.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(bits[37]).append(bits[63])
				.append(currentSpacerRight)
				.append("<i class='wi wi-barometer icon'></i></div>\n");

		sb.append("\t\t</div>\n");

		sb.append("\t\t<div class='dataRowCurrent'>\n");

		sb.append("\t\t\t<div class='dataCellCurrent'><i class='wi wi-wind wi-towards-")
				.append(bits[30].toLowerCase(Locale.ENGLISH))
				.append(" icon'></i>")
				.append(currentSpacerLeft)
				.append(bits[30]).append("</div>\n");
		sb.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(bits[6]).append(bits[64])
				.append(currentSpacerRight)
				.append("<i class='wi wi-humidity icon'></i></div>\n");

		sb.append("\t\t</div>\n");

		sb.append("\t\t<div class='dataRowCurrent'>\n");

		String rain = bits[20] + bits[62] + " " +
		              weeWXApp.getAndroidString(R.string.since) + " mn";
		if(bits.length > 160 && !bits[160].isBlank())
			rain = bits[158] + bits[62] + " " +
			       weeWXApp.getAndroidString(R.string.since) + " " + bits[160];

		sb.append("\t\t\t<div class='dataCellCurrent'><i class='wi wi-umbrella icon'></i>")
				.append(currentSpacerLeft)
				.append(rain).append("</div>\n");
		sb.append("\t\t\t<div class='dataCellCurrent right'>")
				.append(bits[12]).append(bits[60])
				.append(currentSpacerRight)
				.append("<i class='wi wi-raindrop icon' style='font-size:24px;'></i></div>");

		sb.append("\n\t\t</div>\n");

		if(!bits[45].isBlank() || !bits[43].isBlank())
		{
			sb.append("\t\t<div class='dataRowCurrent'>\n")
					.append("\t\t\t<div class='dataCellCurrent'>\n\t\t\t\t");

			if(!bits[45].isBlank())
			{
				sb.append("<i class='flaticon-women-sunglasses icon'></i>")
						.append(currentSpacerLeft)
						.append(bits[45])
						.append(" UVI");
			} else {
				sb.append(Common.emptyField);
			}

			sb.append("\n\t\t\t</div>\n\t\t\t<div class='dataCellCurrent right'>\n\t\t\t\t");

			if(!bits[43].isBlank())
			{
				sb.append(bits[43]).append(" W/m²")
						.append(currentSpacerRight)
						.append("<i class='flaticon-women-sunglasses icon'></i>");
			} else {
				sb.append(Common.emptyField);
			}

			sb.append("\n\t\t\t</div>\n\t\t</div>\n");
		}

		if(bits.length > 166 && (!bits[161].isBlank() || !bits[166].isBlank()) &&
		   Common.GetBoolPref("showIndoor", false))
		{
			sb.append("\t\t<div class='dataRowCurrent'>\n")
					.append("\t\t\t<div class='dataCellCurrent'>");

			if(!bits[161].isBlank())
			{
				sb.append("<i class='flaticon-home-page icon'></i>")
						.append(currentSpacerLeft)
						.append(bits[161])
						.append(bits[60]);
			} else {
				sb.append(Common.emptyField);
			}

			sb.append("</div>\n\t\t\t<div class='dataCellCurrent right'>");

			if(!bits[166].isBlank())
			{
				sb.append(bits[166])
						.append(bits[64])
						.append(currentSpacerRight)
						.append("<i class='flaticon-home-page icon'></i>");
			} else {
				sb.append(Common.emptyField);
			}

			sb.append("</div>\n\t\t</div>\n");
		}

		sb.append("\t\t<div class='dataRowCurrent'>\n");

		sb.append("\t\t\t<div class='dataCellCurrent'><i class='wi wi-sunrise icon'></i>")
				.append(currentSpacerLeft)
				.append(bits[57]).append("</div>\n");
		sb.append("\t\t\t<div class='dataCellCurrent right'>").append(bits[58])
				.append(currentSpacerRight)
				.append("<i class='wi wi-sunset icon'></i></div>\n");

		sb.append("\t\t</div>\n");

		sb.append("\t\t<div class='dataRowCurrent'>\n");

		sb.append("\t\t\t<div class='dataCellCurrent'><i class='wi wi-moonrise icon'></i>")
				.append(currentSpacerLeft)
				.append(bits[47]).append("</div>\n");
		sb.append("\t\t\t<div class='dataCellCurrent right'>").append(bits[48])
				.append(currentSpacerRight)
				.append("<i class='wi wi-moonset icon'></i></div>\n");

		sb.append("\t\t</div>\n\n");

		sb.append("\t</div>\n</div>\n");

		CustomDebug.writeDebug("current.html",
				Common.current_html_headers + sb + Common.html_footer);

		forceCurrentRefresh(sb.toString());
	}

	void loadWebViewURL(String url, boolean force)
	{
		Common.LogMessage("Line 413 loadWebViewURL()  url: " + url);

		if(forecast == null)
		{
			Common.LogMessage("Line 417 loadWebViewURL() forecast == null, skipping...");
			return;
		}

		if(url.equals(lastURL))
		{
			Common.LogMessage("Line 432 loadWebViewURL() url == lastURL...");
			if(force)
			{
				forecast.post(() -> forecast.reload());
				Common.LogMessage("Line 444 loadWebViewURL() loadWebViewURL() forecast.reload()");
			}

			return;
		}

		lastURL = url;

		Common.LogMessage("Line 455 loadWebViewURL() post lastURL check...");

		forecast.post(() -> forecast.loadUrl(url));
		Common.LogMessage("Line 469 loadWebViewURL() url: " + url + " should have loaded...");
		stopRefreshing();
	}

	void loadWebViewContent(int resId)
	{
		Common.LogMessage("loadWebViewContent() resId: " + resId);

		loadWebViewContent(Common.current_html_headers +
								weeWXApp.getAndroidString(resId) +
								Common.html_footer);
	}

	void loadWebViewContent(String text)
	{
		Common.LogMessage("text: " + text.replaceAll("\n", "")
				.replaceAll("\r", ""));

		if(forecast == null)
		{
			stopRefreshing();
			return;
		}

		Common.LogMessage("loadWebviewContent()");

		forecast.post(() -> forecast.loadDataWithBaseURL("file:///android_res/", text,
					"text/html", "utf-8", null));
		stopRefreshing();
	}

	void forceCurrentRefresh(String body)
	{
		Common.LogMessage("Line 463 forceCurrentRefresh() body: " + body.replaceAll("\n", "")
				.replaceAll("\r", ""));

		if(current == null)
		{
			Common.LogMessage("Weather.forceCurrentRefresh() current == null", true);
			return;
		}

		Common.LogMessage("Line 472 forceCurrentRefresh()", true);

		String str = Common.current_html_headers + body;

		if(Common.web_debug_on)
			str += Common.debug_html;

		str += Common.html_footer;

		String html_str = str;

		current.post(() ->
		{
			current.loadDataWithBaseURL("file:///android_res/", html_str,
					"text/html", "utf-8", null);
			current.invalidate();
			Common.LogMessage("Line 493 forceCurrentRefresh() current.post()", true);
		});
	}

	private void forceRefresh()
	{
		Common.LogMessage("forceRefresh()");
		Common.getWeather();

		if(Common.GetBoolPref("radarforecast", true))
		{
			String radtype = Common.GetStringPref("radtype", "image");
			if(radtype != null && radtype.equals("image"))
			{
				reloadWebView(true);
			} else {
				String radarURL = Common.GetStringPref("RADAR_URL", "image");
				if(radarURL == null || radarURL.isBlank())
				{
					String html = Common.current_html_headers +
					              weeWXApp.getAndroidString(R.string.radar_url_not_set) +
					              Common.html_footer;
					loadWebViewContent(html);
					return;
				}

				loadWebViewURL(radarURL, true);
			}
		} else {
			reloadForecast(true);
		}
	}

	private void loadWebView()
	{
		Common.LogMessage("loadWebView()");

		String fctype = Common.GetStringPref("fctype", "Yahoo");

		String forecastData = Common.GetStringPref("forecastData", null);

		loadWebView(fctype, forecastData);
	}


	private void loadWebView(String fctype, String forecastData)
	{
		Common.LogMessage("loadWebView(fctype, forecastData)");

		boolean radarForecast = Common.GetBoolPref("radarforecast", true);
		String radtype = Common.GetStringPref("radtype", "image");
		String radarURL = Common.GetStringPref("RADAR_URL", "");

		loadWebView(radarForecast, radtype, radarURL, fctype, forecastData);
	}

	private void loadWebView(boolean radarForecast, String radtype, String radarURL, String fctype, String forecastData)
	{
		Common.LogMessage("loadWebView(forecast)");

		final StringBuilder sb = new StringBuilder();

		sb.append(Common.current_html_headers);

		if(radarForecast)
		{
			if(radtype == null || radtype.isBlank() || radarURL == null || radarURL.isBlank())
			{
				updateFLL(View.VISIBLE);
				loadWebViewContent(R.string.radar_url_not_set);
				return;
			}

			if(radtype.equals("webpage"))
			{
				updateFLL(View.VISIBLE);
				Common.LogMessage("Loading RADAR_URL -> " + radarURL);
				loadWebViewURL(radarURL, false);
			} else {
				updateFLL(View.GONE);

				long current_time = Math.round(System.currentTimeMillis() / 1000.0);

				if(radarThread != null)
				{
					if(radarThread.isAlive())
					{
						if(rtStart + 30 > current_time)
						{
							Common.LogMessage("rtStart is less than 30s old, we'll skip this attempt...");
							loadWebViewContent(R.string.radar_still_downloading);
							return;
						}

						Common.LogMessage("rtStart is 30+s old, we'll interrupt it...");
						radarThread.interrupt();
					}

					radarThread = null;
				}

				rtStart = current_time;

				radarThread = new Thread(() ->
				{
					File myFile = Common.getFile("radar.gif");

					Common.LogMessage("myFile == " + myFile.getAbsolutePath());
					Common.LogMessage("myFile.exists() == " + myFile.exists());

					Bitmap bm = null;

					try
					{
						bm = Common.loadOrDownloadImage(radarURL, "radar.gif", false);
					} catch(Exception ignored) {}

					if(bm == null)
					{
						loadWebViewContent(R.string.radar_download_failed);
						rtStart = 0;
						return;
					}

					sb.append("\n\t<img class='radarImage' alt='Radar Image' src='")
							.append(Common.toBase64(Common.bitmapToBytes(bm)))
							.append("' />\n\n");
					sb.append(Common.html_footer);
					loadWebViewContent(sb.toString());
					rtStart = 0;
				});

				radarThread.start();
			}

			return;
		} else {
			updateFLL(View.GONE);

			if(weeWXApp.getWidth() > weeWXApp.getHeight())
				sb.append("\n<div style='margin-top:10px'></div>\n\n");

			if(fctype == null || fctype.isBlank())
			{
				loadWebViewContent(R.string.forecast_type_is_invalid);
				return;
			}

			if(forecastData == null || forecastData.isBlank())
			{
				String forecastURL = Common.GetStringPref("FORECAST_URL", null);
				forecastURL = Common.reallyGetForecast(forecastURL);
				if(forecastURL == null || forecastURL.isBlank())
				{
					loadWebViewContent(R.string.wasnt_able_to_connect_forecast);
					return;
				}
			}

			String baseURL = "drawable/";

			switch(fctype.toLowerCase(Locale.ENGLISH))
			{
				case "yahoo" ->
				{
					String[] content = Common.processYahoo(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(baseURL).append("purple.png' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "weatherzone" ->
				{
					String[] content = Common.processWZ(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(baseURL).append("wz.png' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "yr.no" ->
				{
					String[] content = Common.processYR(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(baseURL).append("yrno.png' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "met.no" ->
				{
					String[] content = Common.processMetNO(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(baseURL).append("met_no.png' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "wmo.int" ->
				{
					String[] content = Common.processWMO(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(baseURL).append("wmo.png' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "weather.gov" ->
				{
					String[] content = Common.processWGOV(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(baseURL).append("wgov.png' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "weather.gc.ca" ->
				{
					String[] content = Common.processWCA(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(baseURL).append("wca.png' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "weather.gc.ca-fr" ->
				{
					String[] content = Common.processWCAF(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(baseURL).append("wca.png' height='29px'/>")
							.append("</div><")
							.append(content[0]);
				}
				case "metoffice.gov.uk" ->
				{
					String[] content = Common.processMET(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(baseURL).append("met.png' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "bom2", "bom3" ->
				{
					String[] content;

					Common.LogMessage("doing bom2/bom3...");

					if(fctype.toLowerCase(Locale.ENGLISH).equals("bom3"))
						content = Common.processBOM3(forecastData);
					else if(fctype.toLowerCase(Locale.ENGLISH).equals("bom2"))
						content = Common.processBOM2(forecastData);
					else
						return;

					Common.LogMessage("content: " + Arrays.toString(content));

					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("\n<div style='text-align:center'>\n\t");
					if(KeyValue.theme == R.style.AppTheme_weeWXApp_Dark_Common)
						sb.append("<img src='").append(baseURL).append("bom.png' style='filter:invert(1);' height='29px' />");
					else
						sb.append("<img src='").append(baseURL).append("bom.png' height='29px' />");
					sb.append("\n</div>\n").append(content[0]);
				}
				case "aemet.es" ->
				{
					String[] content = Common.processAEMET(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(baseURL).append("aemet.png' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "dwd.de" ->
				{
					String[] content = Common.processDWD(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(baseURL).append("dwd.png' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "metservice.com" ->
				{
					String[] content = Common.processMetService(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(baseURL).append("metservice.png' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "openweathermap.org" ->
				{
					String[] content = Common.processOWM(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(baseURL).append("owm.png' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "weather.com" ->
				{
					String[] content = Common.processWCOM(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(baseURL).append("weather_com.png' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "met.ie" ->
				{
					String[] content = Common.processMETIE(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(baseURL).append("met_ie.png' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
				case "tempoitalia.it" ->
				{
					String[] content = Common.processTempoItalia(forecastData);
					if(content == null || content.length == 0)
					{
						loadWebViewContent(R.string.failed_to_process_forecast_data);
						return;
					}

					sb.append("<div style='text-align:center'>")
							.append("<img src='").append(baseURL).append("tempoitalia_it.png' height='29px'/>")
							.append("</div>\n")
							.append(content[0]);
				}
			}
		}

		sb.append(Common.html_footer);

		CustomDebug.writeDebug("loadWebView.html", sb.toString());


		loadWebViewContent(sb.toString());
	}

	private void stopRefreshing()
	{
		Common.LogMessage("stopRefreshing()");
		if(!swipeLayout.isRefreshing())
			return;

		swipeLayout.post(() -> swipeLayout.setRefreshing(false));
	}

	private void reloadForecast(boolean force)
	{
		Common.LogMessage("reloadForecast()");

		if(Common.GetBoolPref("radarforecast", true))
		{
			Common.LogMessage("reloadForecast() radarforecast is true, skipping... This line shouldn't be hit...");
			stopRefreshing();
			return;
		}

		if(!Common.checkConnection() && !force)
		{
			Common.LogMessage("Not on wifi and not a forced refresh");
			stopRefreshing();
			return;
		}

		String forecast_url = Common.GetStringPref("FORECAST_URL", null);
		if(forecast_url == null || forecast_url.isBlank())
		{
			loadWebViewContent(R.string.forecast_url_not_set);
			return;
		}

		Common.LogMessage("forecast checking: " + forecast_url);

		long current_time = Math.round(System.currentTimeMillis() / 1000.0);

		if(forecastThread != null)
		{
			if(forecastThread.isAlive())
			{
				if(ftStart + 30 > current_time)
				{
					Common.LogMessage("forecastThread is less than 30s old, we'll skip this attempt...");
					return;
				}

				Common.LogMessage("forecastThread is 30+s old or not alive, we'll interrupt it...");
				forecastThread.interrupt();
			}

			forecastThread = null;
		}

		ftStart = current_time;

		forecastThread = new Thread(() ->
		{
			try
			{
				String forecastData = Common.GetStringPref("forecastData", null);

				Common.LogMessage("forecastData: " + forecastData);

				if(force || forecastData == null || forecastData.isBlank() ||
						Common.GetLongPref("rssCheck", 0) + 7190 < current_time)
				{
					Common.LogMessage("No forecast data or cache is more than 2 hour old or was forced...");

					forecastData = Common.reallyGetForecast(forecast_url);
					if(forecastData != null && !forecastData.isBlank())
					{
						String fctype = Common.GetStringPref("fctype", null);
						if(fctype == null || fctype.isBlank())
						{
							loadWebViewContent(R.string.forecast_type_is_invalid);
							ftStart = 0;
							return;
						}

						loadWebView(fctype, forecastData);
						ftStart = 0;
						return;
					}

					Common.LogMessage("Forecast is null or blank, spitting out error to the webview...");
					loadWebViewContent(R.string.wasnt_able_to_connect_or_download);
					ftStart = 0;
					return;
				}

				Common.LogMessage("forecastThread is about to call loadWebView()...");
				loadWebView();
			} catch(Exception e) {
				Common.doStackOutput(e);
			}

			ftStart = 0;
		});

		forecastThread.start();
	}

	private void reloadWebView(boolean force)
	{
		Common.LogMessage("reloadWebView()");

		if(!Common.GetBoolPref("radarforecast", true))
		{
			Common.LogMessage("This shouldn't have happened!");
			return;
		}

		if(!Common.checkConnection() && !force)
		{
			Common.LogMessage("Not on wifi and not a forced refresh");
			return;
		}

		Common.LogMessage("Reload radar...");
		final String radar = Common.GetStringPref("RADAR_URL", "");
		if(radar == null || radar.isBlank())
		{
			Common.LogMessage("radar_url is null or blank...");
			return;
		}

		String radtype = Common.GetStringPref("radtype", "image");
		if(radtype == null || !radtype.equals("image"))
		{
			Common.LogMessage("This shouldn't have happened! radtype is null or blank...");
			return;
		}

		updateFLL(View.GONE);

		long current_time = Math.round(System.currentTimeMillis() / 1000.0);

		if(radarThread != null)
		{
			if(radarThread.isAlive())
			{
				if(rtStart + 30 > current_time)
				{
					Common.LogMessage("ftStart is less than 30s old, we'll skip this attempt...");
					return;
				}

				Common.LogMessage("ftStart is 30+s old, we'll interrupt it...");
				radarThread.interrupt();
			}

			radarThread = null;
		}

		rtStart = current_time;

		radarThread = new Thread(() ->
		{
			try
			{
				Common.LogMessage("Starting to download image from: " + radar);

				File file = Common.getFile("radar.gif");

				if(!Common.downloadToFile(file, radar))
				{
					String html = Common.current_html_headers +
					              weeWXApp.getAndroidString(R.string.radar_download_failed) +
					              Common.html_footer;
					Common.LogMessage("Failed to download radar image");
					loadWebViewContent(html);
					rtStart = 0;
					return;
				}

				Common.LogMessage("done downloading " + file.getAbsolutePath() + ", prompt handler to draw to movie");
				loadWebView();
			} catch(Exception e) {
				Common.doStackOutput(e);
			}

			rtStart = 0;
		});

		radarThread.start();
	}

	public void onResume()
	{
		super.onResume();

		Common.LogMessage("Weather.onResume()");

		if(isVisible)
			return;

		isVisible = true;

		Common.LogMessage("Weather.onResume() -- updating the value of the floating checkbox...");
		updateFLL();

		if(Common.GetBoolPref("radarforecast", true))
		{
			String radtype = Common.GetStringPref("radtype", "image");
			if(radtype == null || !radtype.equals("image"))
				loadWebView();
			else
				reloadWebView(false);
		} else {
			reloadForecast(false);
		}
	}

	public void onPause()
	{
		super.onPause();

		Common.LogMessage("Weather.onPause()");

		if(!isVisible)
			return;

		isVisible = false;

		if(disabledSwipe)
		{
			disabledSwipe = false;
			Common.LogMessage("Enabling swipe between screens...");
			activity.setUserInputPager(true);
		}
	}

	private final Observer<String> notificationObserver = str ->
	{
		Common.LogMessage("notificationObserver == " + str);

		if(str.equals(Common.UPDATE_INTENT) || str.equals(Common.REFRESH_INTENT))
			drawEverything();

		if(str.equals(Common.EXIT_INTENT))
			onPause();
	};

	@Override
	public void onClick(View view)
	{
		disableSwipeOnRadar = floatingCheckBox.isChecked();
		Common.SetBoolPref("disableSwipeOnRadar", disableSwipeOnRadar);
		updateFLL();
		Common.LogMessage("Forecast.onClick() finished...");
	}

	private void updateFLL()
	{
		activity.setUserInputPager(fll.getVisibility() != View.VISIBLE || !disableSwipeOnRadar);
	}

	private void updateFLL(int visibility)
	{
		if(fll.getVisibility() != visibility)
		{
			requireActivity().runOnUiThread(() -> fll.setVisibility(visibility));
			updateFLL();
		}
	}
}