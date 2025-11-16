package com.odiousapps.weewxweather;

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
import android.widget.Toast;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
	private WebView current, forecast;
	private SwipeRefreshLayout swipeLayout;
	private MaterialCheckBox floatingCheckBox;
	private LinearLayout fll;
	private MainActivity activity;
	private MaterialTextView tv1, tv2;
	private final ViewTreeObserver.OnScrollChangedListener scl =
			() -> swipeLayout.setEnabled(current.getScrollY() == 0);

	private long lastRunForecast, lastRunRadar; //, lastRunWeather;

	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		weeWXAppCommon.LogMessage("Weather.onCreateView()");

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
			weeWXAppCommon.LogMessage("onRefresh();");
			forceRefresh();
		});

		floatingCheckBox = rootView.findViewById(R.id.floatingCheckBoxMain);
		floatingCheckBox.setOnClickListener(this);

		boolean disableSwipeOnRadar = weeWXAppCommon.GetBoolPref("disableSwipeOnRadar", weeWXApp.disableSwipeOnRadar_default);
		floatingCheckBox.setChecked(disableSwipeOnRadar);

		return rootView;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		weeWXAppCommon.LogMessage("Weather.onViewCreated()");
		super.onViewCreated(view, savedInstanceState);

		swipeLayout.setRefreshing(true);

		weeWXAppCommon.LogMessage("Weather.onViewCreated()-- adding notification manager...");
		weeWXAppCommon.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);

		if(current == null)
			current = loadWebview(null, view, R.id.current, true);
		else
			loadWebview(current, view, R.id.current, true);

		if(forecast == null)
			forecast = loadWebview(null, view, R.id.forecast, false);
		else
			loadWebview(forecast, view, R.id.forecast, false);

		if(weeWXAppCommon.isPrefSet("radarforecast"))
		{
			weeWXAppCommon.LogMessage("Weather.onViewCreated() doing full load...");
			if(weeWXAppCommon.GetBoolPref("radarforecast", weeWXApp.radarforecast_default) == weeWXApp.RadarOnHomeScreen)
				drawRadar();
			else
				drawForecast();

			drawWeather();

			loadWebView();
		}
	}

	@Override
	public void onDestroyView()
	{
		weeWXAppCommon.LogMessage("Weather.onDestroyView()");
		super.onDestroyView();

		if(current != null)
		{
			ViewParent parent = current.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(current);

			current.getViewTreeObserver().removeOnScrollChangedListener(scl);

			WebViewPreloader.getInstance().recycleWebView(current);

			weeWXAppCommon.LogMessage("Weather.onDestroyView() recycled current...");
		}

		if(forecast != null)
		{
			ViewParent parent = forecast.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(forecast);

			WebViewPreloader.getInstance().recycleWebView(forecast);

			weeWXAppCommon.LogMessage("Weather.onDestroyView() recycled forecast...");
		}
	}

	private WebView loadWebview(WebView webView, View view, int viewid, boolean doSwipe)
	{
		StackTraceElement caller = new Exception().getStackTrace()[1];
		String callerClass  = caller.getClassName();
		String callerMethod = caller.getMethodName();
		weeWXAppCommon.LogMessage("Weather.java loadWebview() " + callerClass + "." + callerMethod);

		boolean dynamicSizing = viewid == R.id.current && weeWXApp.getWidth() < 1100;

		//weeWXAppCommon.LogMessage("Weather.java onViewCreated() height: " + weeWXApp.getHeight());
		//weeWXAppCommon.LogMessage("Weather.java onViewCreated() width: " + weeWXApp.getWidth());
		//weeWXAppCommon.LogMessage("Weather.java onViewCreated() density: " + weeWXApp.getDensity());

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
					weeWXAppCommon.LogMessage("dynamicSizing is true...");
					// Post a Runnable to make sure contentHeight is available
					view.postDelayed(() ->
					{
						float density =  weeWXApp.getDensity();
						int contentHeightPx = tv1.getHeight() + tv2.getHeight() +
						                      (int)(wv.getContentHeight() * density);
						ViewGroup.LayoutParams params = swipeLayout.getLayoutParams();
						params.height = contentHeightPx; // - (int)(5 * density);
						swipeLayout.setLayoutParams(params);
						weeWXAppCommon.LogMessage("New Height: " + contentHeightPx);
					}, 100); // 100ms delay lets the page finish rendering
				} else if(viewid == R.id.current) {
					weeWXAppCommon.LogMessage("dynamicSizing is false...");
				}

				stopRefreshing();
			}
		});

		if(wasNull)
			return webView;

		return null;
	}

	private void checkFields(TextView tv, String txt)
	{
		if(!tv.getText().toString().equals(txt))
			tv.post(() -> tv.setText(txt));
	}

	private void drawRadar()
	{
		weeWXAppCommon.LogMessage("drawRadar()");

		long current_time = weeWXAppCommon.getCurrTime();

		if(lastRunRadar + 5 > current_time)
		{
			weeWXAppCommon.LogMessage("We already ran less than 5s ago, skipping...");
			stopRefreshing();
			return;
		}

		lastRunRadar = current_time;

		if(!weeWXAppCommon.isPrefSet("radarforecast") ||
		   weeWXAppCommon.GetBoolPref("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.RadarOnHomeScreen)
		{
			stopRefreshing();
			return;
		}

		String radtype = weeWXAppCommon.GetStringPref("radtype", weeWXApp.radtype_default);
		if(radtype == null || radtype.isBlank())
		{
			weeWXAppCommon.LogMessage("radtype: " + radtype);
			String tmp = String.format(weeWXApp.getAndroidString(R.string.radar_type_is_invalid), radtype);
			loadWebViewContent(tmp);
			return;
		}

		if(radtype.equals("image"))
		{
			weeWXAppCommon.LogMessage("Update the radar iamge from cache...");
			loadOrReloadRadarImage();
		} else {
			String radarURL = weeWXAppCommon.GetStringPref("RADAR_URL", weeWXApp.RADAR_URL_default);
			if(radarURL == null || radarURL.isBlank())
			{
				weeWXAppCommon.LogMessage("radarURL: " + radarURL);
				loadWebViewContent(R.string.radar_url_not_set);
				return;
			}

			weeWXAppCommon.LogMessage("Update the radar webview page...");
			loadWebViewURL(radarURL);
		}
	}

	void drawForecast()
	{
		weeWXAppCommon.LogMessage("drawForecast()");

		if(weeWXAppCommon.GetBoolPref("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.ForecastOnHomeScreen)
			return;

		weeWXAppCommon.LogMessage("Show the forecast...");
		reloadForecast();
	}

	void drawWeather()
	{
		weeWXAppCommon.LogMessage("drawWeather()");

		String lastDownload = weeWXAppCommon.GetStringPref("LastDownload", weeWXApp.LastDownload_default);
		if(lastDownload == null || lastDownload.isBlank())
		{
			forceCurrentRefresh(R.string.attempting_to_download_data_txt);
			stopRefreshing();
			return;
		}

		String[] bits = lastDownload.split("\\|");

		weeWXAppCommon.LogMessage("bits.length: " + bits.length);

		if(bits.length < 65)
		{
			weeWXAppCommon.LogMessage("bits.length < 65");
			forceCurrentRefresh(R.string.error_occurred_while_attempting_to_update);
			stopRefreshing();
			return;
		}

		checkFields(tv1, bits[56]);
		checkFields(tv2, bits[54] + " " + bits[55]);

		String currentSpacerLeft = "<span class='currentSpacer left'></span>";
		String currentSpacerRight = "<span class='currentSpacer right'></span>";

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
			sb.append(weeWXApp.emptyField);
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

		bits[43] = bits[43].strip();

		if(bits[43].contains("N/A"))
			bits[43] = "";

		bits[45] = bits[45].strip();

		if(bits[45].contains("N/A"))
			bits[45] = "";

		if(!bits[43].isBlank() || !bits[45].isBlank())
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
				sb.append(weeWXApp.emptyField);
			}

			sb.append("\n\t\t\t</div>\n\t\t\t<div class='dataCellCurrent right'>\n\t\t\t\t");

			if(!bits[43].isBlank())
			{
				sb.append(bits[43]).append(" W/m²")
						.append(currentSpacerRight)
						.append("<i class='flaticon-women-sunglasses icon'></i>");
			} else {
				sb.append(weeWXApp.emptyField);
			}

			sb.append("\n\t\t\t</div>\n\t\t</div>\n");
		}

		if(bits.length > 166 && (!bits[161].isBlank() || !bits[166].isBlank()) &&
		   weeWXAppCommon.GetBoolPref("showIndoor", weeWXApp.showIndoor_default))
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
				sb.append(weeWXApp.emptyField);
			}

			sb.append("</div>\n\t\t\t<div class='dataCellCurrent right'>");

			if(!bits[166].isBlank())
			{
				sb.append(bits[166])
						.append(bits[64])
						.append(currentSpacerRight)
						.append("<i class='flaticon-home-page icon'></i>");
			} else {
				sb.append(weeWXApp.emptyField);
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

		forceCurrentRefresh(sb.toString());
	}

	private void loadAndShowWebView(WebView wv, String text, String url)
	{
		if(text != null && url != null)
			return;

		wv.post(() ->
		{
			if(text != null)
				wv.loadDataWithBaseURL("file:///android_res/", text,
						"text/html", "utf-8", null);
			else if(url != null)
				wv.loadUrl(url);
			else
				wv.reload();

			//wv.invalidate();
		});
	}

	void loadWebViewURL(String url)
	{
		long current_time = weeWXAppCommon.getCurrTime();

		weeWXAppCommon.LogMessage("Line 464 loadWebViewURL() url: " + url);

		if(forecast == null)
		{
			weeWXAppCommon.LogMessage("Line 468 loadWebViewURL() forecast == null, skipping...");
			stopRefreshing();
			return;
		}

		if(weeWXAppCommon.GetBoolPref("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.RadarOnHomeScreen ||
		   weeWXAppCommon.GetStringPref("radtype", weeWXApp.radtype_default).equals("image"))
		{
			weeWXAppCommon.LogMessage("Line 474 loadWebViewURL() loadWebViewURL() " +
			                          "radarforecast != weeWXApp.RadarOnHomeScreen or " +
			                          "radtype == image...");
			stopRefreshing();
			return;
		}

		if(lastRunForecast + 5 > current_time)
		{
			weeWXAppCommon.LogMessage("Line 474 loadWebViewURL() loadWebViewURL() ran less than 5s ago, skipping...");
			stopRefreshing();
			return;
		}

		lastRunForecast = current_time;

		if(url.equals(lastURL))
		{
			weeWXAppCommon.LogMessage("Line 474 loadWebViewURL() loadWebViewURL() forecast.reload()");
			loadAndShowWebView(forecast, null, null);
			return;
		}

		lastURL = url;

		weeWXAppCommon.LogMessage("Line 481 loadWebViewURL() post lastURL check...");

		loadAndShowWebView(forecast, null, url);
		weeWXAppCommon.LogMessage("Line 484 loadWebViewURL() url: " + url + " should have loaded...");
		stopRefreshing();
	}

	void loadWebViewContent(int resId)
	{
		weeWXAppCommon.LogMessage("loadWebViewContent() resId: " + resId);

		loadWebViewContent(weeWXApp.current_html_headers + weeWXApp.script_header +
		                   weeWXApp.html_header_rest + weeWXApp.inline_arrow +
		                   weeWXApp.getAndroidString(resId) +
		                   weeWXApp.html_footer);
	}

	void loadWebViewContent(String text)
	{
		if(forecast == null)
		{
			stopRefreshing();
			return;
		}

		weeWXAppCommon.LogMessage("loadWebviewContent()");

		loadAndShowWebView(forecast, text, null);
		stopRefreshing();
	}

	void forceCurrentRefresh(int resId)
	{
		forceCurrentRefresh(weeWXApp.getAndroidString(resId));
	}

	void forceCurrentRefresh(String body)
	{
		if(current == null)
		{
			weeWXAppCommon.LogMessage("Weather.forceCurrentRefresh() current == null");
			return;
		}

		weeWXAppCommon.LogMessage("Line 472 forceCurrentRefresh()");

		body = weeWXAppCommon.indentNonBlankLines(body, 2) + "\n\n";

		String str = weeWXApp.current_html_headers + weeWXApp.html_header_rest + body;

		if(weeWXAppCommon.web_debug_on)
			str += weeWXApp.debug_html;

		str += weeWXApp.html_footer;

		if(weeWXAppCommon.debug_html)
		{
			String filename = "weeWX_current_conditions_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".html";

			File outFile = null;

			try
			{
				outFile = weeWXAppCommon.getExtFile("weeWX", filename);
				CustomDebug.writeDebug(outFile, str);
				String theOutFile = outFile.getAbsolutePath();

				weeWXAppCommon.LogMessage("Wrote debug html to " + theOutFile);

				if(isAdded())
				{
					requireActivity().runOnUiThread(() ->
							Toast.makeText(requireContext(), "Wrote debug html to " + theOutFile, Toast.LENGTH_LONG).show());
				}
			} catch(Exception e) {
				weeWXAppCommon.LogMessage("Attempted to write to " + filename + " but failed with the following error: " + e);

				if(isAdded())
				{
					if(outFile != null)
					{
						String theOutFile = outFile.getAbsolutePath();
						requireActivity().runOnUiThread(() ->
								Toast.makeText(requireContext(), "Failed to output debug html to " + theOutFile, Toast.LENGTH_LONG).show());
					} else {
						requireActivity().runOnUiThread(() ->
								Toast.makeText(requireContext(), "Failed to output debug html to " + filename, Toast.LENGTH_LONG).show());
					}
				}
			}
		}

		loadAndShowWebView(current, str, null);
		weeWXAppCommon.LogMessage("Line 426 forceCurrentRefresh() calling loadAndShowWebView()");
	}

	private void forceRefresh()
	{
		weeWXAppCommon.LogMessage("forceRefresh()");

		if(weeWXAppCommon.GetBoolPref("radarforecast", weeWXApp.radarforecast_default) == weeWXApp.RadarOnHomeScreen)
		{
			String radtype = weeWXAppCommon.GetStringPref("radtype", weeWXApp.radtype_default);
			if(radtype != null && radtype.equals("image"))
			{
				weeWXAppCommon.getRadarImage(true, false);
			} else {
				String radarURL = weeWXAppCommon.GetStringPref("RADAR_URL", weeWXApp.RADAR_URL_default);
				if(radarURL == null || radarURL.isBlank())
				{
					String html = weeWXApp.current_html_headers + weeWXApp.html_header_rest +
					              weeWXApp.getAndroidString(R.string.radar_url_not_set) +
					              weeWXApp.html_footer;
					loadWebViewContent(html);
					return;
				}

				loadWebViewURL(radarURL);
			}
		} else {
			weeWXAppCommon.LogMessage("Let's force download of fresh forecast data...");
			weeWXAppCommon.getForecast(true, false);
		}

		UpdateCheck.runInTheBackground(true, false, false);
	}

	private void loadWebView()
	{
		weeWXAppCommon.LogMessage("loadWebView()");

		String fctype = weeWXAppCommon.GetStringPref("fctype", weeWXApp.fctype_default);

		String forecastData = weeWXAppCommon.GetStringPref("forecastData", weeWXApp.forecastData_default);

		if(forecastData != null && !forecastData.isBlank())
			loadWebView(fctype, forecastData);
	}

	private void loadWebView(String fctype, String forecastData)
	{
		weeWXAppCommon.LogMessage("loadWebView(fctype, forecastData)");

		boolean radarForecast = weeWXAppCommon.GetBoolPref("radarforecast", weeWXApp.radarforecast_default);
		String radtype = weeWXAppCommon.GetStringPref("radtype", weeWXApp.radtype_default);
		String radarURL = weeWXAppCommon.GetStringPref("RADAR_URL", weeWXApp.RADAR_URL_default);

		loadWebView(radarForecast, radtype, radarURL, fctype, forecastData);
	}

	private void loadWebView(boolean radarForecast, String radtype, String radarURL, String fctype, String forecastData)
	{
		weeWXAppCommon.LogMessage("loadWebView(forecast)");

		final StringBuilder sb = new StringBuilder();

		sb.append(weeWXApp.current_html_headers);
		sb.append(weeWXApp.script_header);
		sb.append(weeWXApp.html_header_rest);
		sb.append(weeWXApp.inline_arrow);

		if(radarForecast == weeWXApp.RadarOnHomeScreen)
		{
			if(radtype == null || radtype.isBlank())
			{
				updateFLL(View.GONE);
				String tmp = String.format(weeWXApp.getAndroidString(R.string.radar_type_is_invalid), radtype);
				loadWebViewContent(tmp);
				stopRefreshing();
				return;
			}

			if(radarURL == null || radarURL.isBlank())
			{
				updateFLL(View.GONE);
				loadWebViewContent(R.string.radar_url_not_set);
				stopRefreshing();
				return;
			}

			if(radtype.equals("webpage"))
			{
				updateFLL(View.VISIBLE);
				weeWXAppCommon.LogMessage("Loading RADAR_URL -> " + radarURL);
				loadWebViewURL(radarURL);
			} else {
				loadOrReloadRadarImage();
			}

			stopRefreshing();
		} else {
			updateFLL(View.GONE);

			if(weeWXApp.getWidth() > weeWXApp.getHeight())
				sb.append("\n<div style='margin-top:10px'></div>\n\n");

			if(fctype == null || fctype.isBlank())
			{
				String finalErrorStr = String.format(weeWXApp.getAndroidString(R.string.forecast_type_is_invalid), fctype);
				loadWebViewContent(finalErrorStr);
				return;
			}

			if(forecastData == null || forecastData.isBlank())
			{
				forecastData = weeWXAppCommon.GetStringPref("forecastData", weeWXApp.forecastData_default);
				if(forecastData == null || forecastData.isBlank())
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
					String[] content = weeWXAppCommon.processYahoo(forecastData);
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
					String[] content = weeWXAppCommon.processWZ(forecastData);
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
					String[] content = weeWXAppCommon.processYR(forecastData);
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
					String[] content = weeWXAppCommon.processMetNO(forecastData);
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
					String[] content = weeWXAppCommon.processWMO(forecastData);
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
					String[] content = weeWXAppCommon.processWGOV(forecastData);
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
					String[] content = weeWXAppCommon.processWCA(forecastData);
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
					String[] content = weeWXAppCommon.processWCAF(forecastData);
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
					String[] content = weeWXAppCommon.processMET(forecastData);
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

					weeWXAppCommon.LogMessage("doing bom2/bom3...");

					if(fctype.toLowerCase(Locale.ENGLISH).equals("bom3"))
						content = weeWXAppCommon.processBOM3(forecastData);
					else if(fctype.toLowerCase(Locale.ENGLISH).equals("bom2"))
						content = weeWXAppCommon.processBOM2(forecastData);
					else
						return;

					weeWXAppCommon.LogMessage("content: " + Arrays.toString(content));

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
					String[] content = weeWXAppCommon.processAEMET(forecastData);
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
					String[] content = weeWXAppCommon.processDWD(forecastData);
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
					String[] content = weeWXAppCommon.processMetService(forecastData);
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
					String[] content = weeWXAppCommon.processOWM(forecastData);
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
					String[] content = weeWXAppCommon.processWCOM(forecastData);
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
					String[] content = weeWXAppCommon.processMETIE(forecastData);
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
					String[] content = weeWXAppCommon.processTempoItalia(forecastData);
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
				default ->
				{
					String finalErrorStr = String.format(weeWXApp.getAndroidString(R.string.forecast_type_is_invalid), fctype);
					loadWebViewContent(finalErrorStr);
					return;
				}
			}

			sb.append(weeWXApp.html_footer);

			loadWebViewContent(sb.toString());
			stopRefreshing();
		}
	}

	private void stopRefreshing()
	{
		weeWXAppCommon.LogMessage("stopRefreshing()");
		if(!swipeLayout.isRefreshing())
			return;

		swipeLayout.post(() -> swipeLayout.setRefreshing(false));
	}

	private void reloadForecast()
	{
		weeWXAppCommon.LogMessage("reloadForecast()");

		updateFLL(View.GONE);

		if(weeWXAppCommon.GetBoolPref("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.ForecastOnHomeScreen)
		{
			weeWXAppCommon.LogMessage("Weather.java reloadForecast() weeWXApp.ForecastOnHomeScreenis not true, skipping... " +
			                          "This line shouldn't be hit ever...",true);
			loadWebViewContent(R.string.unknown_error_occurred);
			stopRefreshing();
			return;
		}

		String[] ret = weeWXAppCommon.getForecast(false, false);
		String forecastData = ret[1];
		String fctype = ret[2];

		weeWXAppCommon.LogMessage("fctype: " + fctype);
		weeWXAppCommon.LogMessage("forecastData: " + forecastData);

		if(ret[0].equals("error"))
		{
			if(forecastData != null && !forecastData.isBlank())
			{
				weeWXAppCommon.LogMessage("getForecast returned the following error: " + forecastData);
				loadWebViewContent(weeWXApp.current_html_headers + weeWXApp.html_header_rest + forecastData + weeWXApp.html_footer);
			} else {
				weeWXAppCommon.LogMessage("getForecast returned an unknown error...");
				loadWebViewContent(weeWXApp.current_html_headers + weeWXApp.html_header_rest +
				                   weeWXApp.getAndroidString(R.string.unknown_error_occurred) +
				                   weeWXApp.html_footer);
			}
		}

		weeWXAppCommon.LogMessage("getForecast returned some content...");
		loadWebView(fctype, forecastData);
	}

	private void loadOrReloadRadarImage()
	{
		weeWXAppCommon.LogMessage("loadOrReloadRadarImage()");

		updateFLL(View.GONE);

		if(weeWXAppCommon.GetBoolPref("radarforecast", weeWXApp.radarforecast_default) != weeWXApp.RadarOnHomeScreen)
		{
			weeWXAppCommon.LogMessage("This shouldn't have happened! weeWXApp.RadarOnHomeScreen is false...");
			stopRefreshing();
			return;
		}

		String html;

		if(weeWXAppCommon.getRadarImage(false, false) != null)
		{
			weeWXAppCommon.LogMessage("done downloading radar.gif, prompt to show");
			loadWebView();
			stopRefreshing();
			return;
		}

		html = weeWXApp.current_html_headers + weeWXApp.html_header_rest +
		       weeWXApp.getAndroidString(R.string.radar_still_downloading) +
		       weeWXApp.html_footer;
		weeWXAppCommon.LogMessage("Failed to download radar image");
		loadWebViewContent(html);
		stopRefreshing();
	}

	public void onResume()
	{
		super.onResume();

		weeWXAppCommon.LogMessage("Weather.onResume()");

		if(isVisible)
			return;

		isVisible = true;

		weeWXAppCommon.LogMessage("Weather.onResume() -- updating the value of the floating checkbox...");
		updateFLL();
	}

	public void onPause()
	{
		super.onPause();

		weeWXAppCommon.LogMessage("Weather.onPause()");

		if(!isVisible)
			return;

		isVisible = false;

		weeWXAppCommon.LogMessage("Enabling swipe between screens...");
		activity.setUserInputPager(true);
	}

	private final Observer<String> notificationObserver = str ->
	{
		weeWXAppCommon.LogMessage("Weather.java notificationObserver: " + str);

		if(str.equals(weeWXAppCommon.REFRESH_FORECAST_INTENT))
			drawForecast();

		if(str.equals(weeWXAppCommon.REFRESH_RADAR_INTENT))
			drawRadar();

		if(str.equals(weeWXAppCommon.REFRESH_WEATHER_INTENT))
			drawWeather();

		if(str.equals(weeWXAppCommon.EXIT_INTENT))
			onPause();
	};

	@Override
	public void onClick(View view)
	{
		weeWXAppCommon.SetBoolPref("disableSwipeOnRadar", floatingCheckBox.isChecked());
		updateFLL();
		weeWXAppCommon.LogMessage("Forecast.onClick() finished...");
	}

	private void updateFLL()
	{
		activity.setUserInputPager(fll.getVisibility() != View.VISIBLE || !floatingCheckBox.isChecked() || !isVisible());
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