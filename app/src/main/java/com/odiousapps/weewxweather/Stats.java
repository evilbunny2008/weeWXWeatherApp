package com.odiousapps.weewxweather;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.slider.Slider;

import java.util.Date;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import static com.odiousapps.weewxweather.weeWXApp.getAndroidString;
import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;
import static com.odiousapps.weewxweather.weeWXAppCommon.is_blank;
import static com.odiousapps.weewxweather.weeWXAppCommon.weeWXNotificationManager;
import static com.odiousapps.weewxweather.weeWXAppCommon.cssToSVG;
import static com.odiousapps.weewxweather.weeWXAppCommon.deg2Str;
import static com.odiousapps.weewxweather.weeWXAppCommon.fiToSVG;
import static com.odiousapps.weewxweather.weeWXAppCommon.formatString;
import static com.odiousapps.weewxweather.weeWXAppCommon.getDateTimeStr;
import static com.odiousapps.weewxweather.weeWXAppCommon.getJson;
import static com.odiousapps.weewxweather.weeWXAppCommon.getSinceHour;
import static com.odiousapps.weewxweather.weeWXAppCommon.hasElement;
import static com.odiousapps.weewxweather.weeWXAppCommon.processUpdateInBG;
import static com.odiousapps.weewxweather.weeWXAppCommon.str2Float;

public class Stats extends Fragment
{
	private Slider mySlider;
	private int currZoom = 0;
	private View rootView;
	private SafeWebView wv;
	private SwipeRefreshLayout swipeLayout;
	private final ViewTreeObserver.OnScrollChangedListener scl = () -> swipeLayout.setEnabled(wv.getScrollY() == 0);

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		LogMessage("Stats.onCreateView()");
		super.onCreateView(inflater, container, savedInstanceState);

		int bgColour = weeWXApp.getColours().bgColour;

		rootView = inflater.inflate(R.layout.fragment_stats, container, false);

		LinearLayout ll = rootView.findViewById(R.id.stats_ll);
		ll.setBackgroundColor(bgColour);

		swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
		swipeLayout.setOnRefreshListener(() ->
		{
			swipeLayout.setRefreshing(true);
			LogMessage("weeWXAppCommon.getWeather(true, false)...");
			processUpdateInBG(true, false, false, true,
					true, false, false, false);
		});

		mySlider = rootView.findViewById(R.id.pageZoom);
		mySlider.setBackgroundColor(bgColour);
		mySlider.addOnChangeListener((slider, value, fromUser) ->
		{
			LogMessage("Current Slider zoom = " + (int)mySlider.getValue() + "%");
			LogMessage("New Slider zoom = " + value + "%");

			if(fromUser && currZoom != (int)value)
			{
				currZoom = (int)value;
				KeyValue.putVar("mySlider", currZoom);
				setZoom(currZoom, true);
			}
		});

		if(wv == null)
			wv = new SafeWebView(weeWXApp.getInstance());

		FrameLayout fl = rootView.findViewById(R.id.webViewFrameLayout);
		fl.addView(wv);

		wv.getViewTreeObserver().addOnScrollChangedListener(scl);

		wv.setOnCustomPageFinishedListener((v, url) ->
		{
			LogMessage("Stats.setOnPageFinishedListener()");

			if(currZoom == 0)
			{
				currZoom = sanitiseZoom((int)KeyValue.readVar("mySlider", weeWXApp.mySlider_default));
				LogMessage("Stats.setOnPageFinishedListener() currZoom: " + currZoom + "%", KeyValue.d);
				wv.postDelayed(() -> setZoom(currZoom, false), 50);
			}

			stopRefreshing();
		}, false);

		swipeLayout.setRefreshing(true);

		updateFields();

		LogMessage("Stats.onViewCreated()-- adding notification manager...");
		weeWXNotificationManager.observeNotifications(getViewLifecycleOwner(), notificationObserver);

		return rootView;
	}

	public void onResume()
	{
		super.onResume();

		currZoom = sanitiseZoom((int)KeyValue.readVar("mySlider", weeWXApp.mySlider_default));

		LogMessage("Stats.onResume() currZoom: " + currZoom + "%", KeyValue.d);

		wv.postDelayed(() -> setZoom(currZoom, false), 50);
	}

	@Override
	public void onDestroyView()
	{
		LogMessage("Stats.onDestroyView()");
		super.onDestroyView();

		weeWXNotificationManager.removeNotificationObserver(notificationObserver);

		if(wv != null)
		{
			ViewParent parent = wv.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(wv);

			wv.getViewTreeObserver().removeOnScrollChangedListener(scl);

			wv.destroy();

			LogMessage("Stats.onDestroyView() recycled wv...");
		}
	}

	void stopRefreshing()
	{
		if(!swipeLayout.isRefreshing())
			return;

		swipeLayout.post(() -> swipeLayout.setRefreshing(false));
	}

	int sanitiseZoom(int zoom)
	{
		//zoom = Math.round(Math.round(zoom * 10.0f) * 10.0f);

		if(zoom < 50)
			zoom = 100;

		if(zoom > 200)
			zoom = 100;

		return zoom;
	}

	void setZoom(int zoom, boolean fromUser)
	{
		if(mySlider == null || wv == null)
			return;

		if(!fromUser && (int)mySlider.getValue() == zoom)
			return;

		final int finalZoom = sanitiseZoom(zoom);
		final float finalZoomDec = finalZoom / 100.0f;

		String jsCommon = """
			(function()
			{
				if (!document || !document.body || !document.body.style) return -1;

			""";

		String jsBottom = "})();";

		String js1 = jsCommon + "var z = window.getComputedStyle(document.body).zoom;" +
								"return z ? z : -1;" + jsBottom;

		LogMessage("Get Zoom JS: " + js1.replaceAll("[\n\r\t]", " ")
				.replaceAll("\\s+", " "), KeyValue.d);

		String js2 = jsCommon + "document.body.style.zoom = " + finalZoomDec + "; return 'OK';" + jsBottom;

		LogMessage("Set Zoom JS: " + js2.replaceAll("[\n\r\t]", " ")
				.replaceAll("\\s+", " "), KeyValue.d);

		Handler handler = new Handler(Looper.getMainLooper());
		Runnable poll = new Runnable()
		{
			@Override
			public void run()
			{
				wv.evaluateJavascript(js1, value1 ->
				{
					LogMessage("value1: " + value1, KeyValue.i);

					if(is_blank(value1) || value1.equals("null") || value1.equals("-1"))
					{
						handler.postDelayed(this, 100);
						return;
					}

					float f = str2Float(value1);

					LogMessage("f: " + f + ", finalZoomDec: " + finalZoomDec, KeyValue.i);

					if(f == finalZoomDec)
					{
						LogMessage("Current page zoom: " + finalZoom + "%, no change required", KeyValue.d);
						mySlider.setValue(finalZoom);
						return;
					}

					wv.post(() -> wv.evaluateJavascript(js2, value2 ->
					{
						LogMessage("Stats.evaluateJavascript() returned value: " + value2);
						if(!value2.equals("\"OK\""))
						{
							LogMessage("value != OK: " + value2);

							handler.postDelayed(this, 100);
							return;
						}

						LogMessage("New zoom set to value: " + finalZoom + "%", KeyValue.d);
						mySlider.setValue(finalZoom);
					}));
				});
			}
		};

		handler.post(poll);
	}

	private final Observer<String> notificationObserver = str ->
	{
		LogMessage("Stats.notificationObserver: " + str);

		if(str.equals(weeWXAppCommon.REFRESH_WEATHER_INTENT))
			updateFields();

		if(str.equals(weeWXAppCommon.STOP_WEATHER_INTENT))
			stopRefreshing();
	};

	private void checkFields(final TextView tv, final String txt)
	{
		if(!tv.getText().toString().equals(txt))
			tv.post(() ->
			{
				tv.setBackgroundColor(weeWXApp.getColours().bgColour);
				tv.setTextColor(weeWXApp.getColours().fgColour);
				tv.setText(txt);
			});
	}

	private String createRowLeft()
	{
		return createRowLeft(null, weeWXApp.emptyField, weeWXApp.emptyField);
	}

	private String createRowLeft(String class1, String str1, String str2)
	{
		String icon = "";
		if(!is_blank(class1))
			icon = cssToSVG(class1);

		return createRowLeft2(icon, str1, str2);
	}

	private String createRowLeft2(String icon, String str1, String str2)
	{
		return "\t\t<div class='statsDataRow'>\n" +
			   "\t\t\t<div class='statsDataCell left'>" + icon + "</div>\n" +
			   weeWXApp.currentSpacer +
			   "\t\t\t<div class='statsDataCell midleft'>" + str1 + "</div>\n" +
			   weeWXApp.currentSpacer +
			   "\t\t\t<div class='statsDataCell midright'>" + str2 + "</div>\n";
	}

	private String createRowRight()
	{
		return createRowRight(null, weeWXApp.emptyField, weeWXApp.emptyField);
	}

	private String createRowRight2(String icon, String str3, String str4)
	{
		return "\t\t\t<div class='statsDataCell midleft'>" + str3 + "</div>\n" +
			   weeWXApp.currentSpacer +
			   "\t\t\t<div class='statsDataCell midright'>" + str4 + "</div>\n" +
			   weeWXApp.currentSpacer +
			   "\t\t\t<div class='statsDataCell right'>" + icon + "</div>\n" +
			   "\t\t</div>\n\n";
	}

	private String createRowRight(String class2, String str3, String str4)
	{
		String icon = "";
		if(!is_blank(class2))
			icon = cssToSVG(class2);

		return createRowRight2(icon, str3, str4);
	}

	private String createRow(String class1, String class2, String str1,
							 String str2, String str3, String str4)
	{
		if(is_blank(str1) && is_blank(str2) && is_blank(str3) && is_blank(str4))
			return "";

		return createRowLeft(class1, str1, str2) +
			   weeWXApp.currentSpacer +
			   createRowRight(class2, str3, str4);
	}

	private String createRowLeft(int degree, String str1, String dateTime)
	{
		return "\t\t<div class='statsDataRow'>\n" +
			   "\t\t\t<div class='statsDataCell left'>" +
			   cssToSVG("wi-wind-deg", degree) + "</div>\n" +
			   weeWXApp.currentSpacer +
			   "\t\t\t<div class='statsDataCell midleft'>" + str1 + "</div>\n" +
			   weeWXApp.currentSpacer +
			   "\t\t\t<div class='statsDataCell midright'>" + dateTime + "</div>\n";
	}

	private String createRowLeft(int degree, String str1)
	{
		return "\t\t<div class='statsDataRow'>\n" +
			   "\t\t\t<div class='statsDataCell left'>" +
			   cssToSVG("wi-wind-deg", degree) +
			   "</div>\n\t\t\t" + weeWXApp.currentSpacer +
			   "\t\t\t<div class='statsDataCell Wind2'>" + str1 + "</div>\n";
	}

	private String createRowRight(String str2)
	{
		return "\t\t\t<div class='statsDataCell Rain2'>" +
			   str2 +
			   "</div>\n\t\t\t" + weeWXApp.currentSpacer +
			   "\t\t\t<div class='statsDataCell right'>" +
			   cssToSVG("wi-umbrella") +
			   "</div>\n\t\t</div>\n\n";
	}

	private String createRow(int degree, String str1, String dateTime, String since, String str2)
	{
		return createRowLeft(degree, str1, dateTime) +
			   weeWXApp.currentSpacer +
			   createRowRight("wi-umbrella", since, str2);
	}

	private String createRow2(int degree, String str1, String str2)
	{
		return createRowLeft(degree, str1) + createRowRight(str2);
	}

	private String createSolarUV(String uv, String uvWhen, String solar, String solarWhen, int timeMode)
	{
		long UVWhen = Math.round((double)getJson(uvWhen, 0D) * 1_000L);
		long SolarWhen = Math.round((double)getJson(solarWhen, 0D) * 1_000L);

//		LogMessage("createSolarUV() solar: " + solar);
//		LogMessage("createSolarUV() solarWhen: " + solarWhen);
//		LogMessage("createSolarUV() SolarWhen: " + SolarWhen);

		if(UVWhen == 0 && SolarWhen == 0)
		{
			LogMessage("No solar or UV data, skipping...");
			return "";
		}

		String UV = formatString(uv);
		String SOLAR = formatString(solar);

		String className = fiToSVG("flaticon-women-sunglasses");
		String out = "";

		if(UVWhen != 0)
		{
			String dateTimeStr = getDateTimeStr(UVWhen, timeMode);
			out += createRowLeft(className, UV + KeyValue.getLabel(uv, "UVI").strip(), dateTimeStr);
		} else {
			out += createRowLeft();
		}

		out += weeWXApp.currentSpacer;

		if(SolarWhen != 0)
		{
			String dateTimeStr = getDateTimeStr(SolarWhen, timeMode);
			out += createRowRight(className, dateTimeStr, SOLAR + KeyValue.getLabel(solar, "W/m²"));
		} else {
			out += createRowRight();
		}

		return out;
	}

	private String generateSection(int header, String timeperiod)
	{
		int timeMode = 0;
		if(timeperiod.contains("month"))
			timeMode = 1;
		if(timeperiod.contains("year"))
			timeMode = 2;
		if(timeperiod.equals("alltime"))
			timeMode = 3;

		StringBuilder sb = new StringBuilder();

		sb.append("\t\t<div class='statsHeader'>\n\t\t\t");
		sb.append(getAndroidString(header));
		sb.append("\n\t\t</div>\n\n");

		String tempSym = KeyValue.getLabel("outTemp", "°C");
		String humSym = KeyValue.getLabel("outHumidity", "%");
		String pressSym = KeyValue.getLabel("barometer", "hPa");
		String speedSym = KeyValue.getLabel("wind", "km/h");
		String rainSym = KeyValue.getLabel("rain", "mm");

		String[] loop = {"outTemp", "dewpoint", "outHumidity", "barometer"};
		String[] syms = {tempSym, tempSym, humSym, pressSym};
		String[] css = {fiToSVG("flaticon-temperature"),
						cssToSVG("wi-raindrop"),
						cssToSVG("wi-humidity"),
						cssToSVG("wi-barometer")};

		for(int i = 0; i < loop.length; i++)
		{
			sb.append(createRow(css[i], css[i],
					formatString(timeperiod + "_" + loop[i] + "_max") + syms[i],
					getDateTimeStr(Math.round((double)getJson(timeperiod + "_" + loop[i] + "_maxtime", 0D) * 1_000L), timeMode),
					getDateTimeStr(Math.round((double)getJson(timeperiod + "_" + loop[i] + "_mintime", 0D) * 1_000L), timeMode),
					formatString(timeperiod + "_" + loop[i] + "_min") + syms[i]));
		}

		if((boolean)KeyValue.readVar("showIndoor", weeWXApp.showIndoor_default))
		{
			long maxtime = Math.round((double)getJson(timeperiod + "_inTemp_maxtime", 0D) * 1_000L);
			long mintime = Math.round((double)getJson(timeperiod + "_inTemp_mintime", 0D) * 1_000L);
			if(mintime > 0 && maxtime > 0)
				sb.append(createRow(fiToSVG("flaticon-home-page"), fiToSVG("flaticon-home-page"),
									formatString(timeperiod + "_inTemp_max") + tempSym,
									getDateTimeStr(maxtime, timeMode), getDateTimeStr(mintime, timeMode),
									formatString(timeperiod + "_inTemp_min") + tempSym));

			maxtime = Math.round((double)getJson(timeperiod + "_inHumidity_maxtime", 0D) * 1_000L);
			mintime = Math.round((double)getJson(timeperiod + "_inHumidity_mintime", 0D) * 1_000L);
			if(mintime > 0 && maxtime > 0)
				sb.append(createRow(fiToSVG("flaticon-home-page"), fiToSVG("flaticon-home-page"),
									formatString(timeperiod + "_inHumidity_max") + humSym,
									getDateTimeStr(maxtime, timeMode), getDateTimeStr(mintime, timeMode),
									formatString(timeperiod + "_inHumidity_min") + humSym));
		}

		sb.append(createSolarUV(timeperiod + "_UV_max", timeperiod + "_UV_maxtime",
				timeperiod + "_radiation_max", timeperiod + "_radiation_maxtime", timeMode));

		int since_hour = (int)getJson("since_hour", 0);
		String since = "";
		String rain = formatString(timeperiod + "_rain_sum");

		if(timeperiod.equals("day"))
		{
			if(since_hour > 0)
				rain = formatString("since_today");

			since = getSinceHour(since_hour, R.string.since);
		} else if(timeperiod.equals("yesterday")) {
			if(since_hour > 0)
				rain = formatString("since_yesterday");

			since = getSinceHour(since_hour, R.string.since);
		}

		boolean hasVec = hasElement(timeperiod + "_wind_vecdir");
		boolean hasET = hasElement(timeperiod + "_ET_sum");

		if(hasVec || hasET)
		{
			if(hasVec)
				sb.append(createRowLeft2(cssToSVG("wi-wind-deg",
						Math.round((float)getJson(timeperiod + "_wind_vecdir", 0f))),
						formatString(timeperiod + "_wind_avg") + speedSym +
						" " + deg2Str(timeperiod + "_wind_vecdir"),
						getAndroidString(R.string.avg)));
			else
				sb.append(createRowLeft());

			sb.append(weeWXApp.currentSpacer);

			if(hasET)
				sb.append(createRowRight2(cssToSVG("evaporation"), "ET",
						formatString(timeperiod + "_ET_sum") + rainSym));
			else
				sb.append(createRowRight());
		}

		if(timeMode == 0 || timeMode == 1)
			sb.append(createRow(Math.round((float)getJson(timeperiod + "_wind_maxdir", 0f)),
					formatString(timeperiod + "_wind_max") + speedSym +
					" " + deg2Str(timeperiod + "_wind_maxdir", timeperiod + "_wind_max"),
					getDateTimeStr(Math.round((double)getJson(timeperiod + "_wind_maxtime", 0D) * 1_000L), timeMode),
					since, rain + rainSym));
		else
			sb.append(createRow2(Math.round((float)getJson(timeperiod + "_wind_maxdir", 0f)),
					formatString(timeperiod + "_wind_max") + speedSym +
					" " + deg2Str(timeperiod + "_wind_maxdir", timeperiod + "_wind_max") +
					" " + getDateTimeStr(Math.round((double)getJson(timeperiod + "_wind_maxtime", 0D) * 1_000L), timeMode),
					rain + rainSym));

		return sb.toString();
	}

	private void updateFields()
	{
		LogMessage("Stats.java updateFields()");

		long report_time = Math.round((double)getJson("report_time", 0D) * 1_000L);

		// Today Stats
		checkFields(rootView.findViewById(R.id.textView), (String)getJson("station_location", ""));
		checkFields(rootView.findViewById(R.id.textView2), weeWXApp.getInstance().sdf18.format(new Date(report_time)));

		final StringBuilder sb = new StringBuilder();

		sb.append(weeWXApp.current_html_headers);

		if(currZoom != 100)
		{
			float newZoom = (float)Math.round(currZoom / 10f) / 10;

			sb.append("\n\t\t<style>\n\t\t\tbody\n\t\t\t{\n\t\t\t\tzoom: ").append(newZoom).append(";\n\t\t\t}\n\t\t</style>\n");
		}

		sb.append(weeWXApp.script_header)
				.append(weeWXApp.html_header_rest)
				.append(weeWXApp.inline_arrow);

		sb.append("\n<div class='statsLayout'>\n\n");

		// Show today's stats
		sb.append("\t<div class='statsSection'>\n");
		sb.append(generateSection(R.string.todayStats, "day"));
		sb.append("\t\t<hr />\n");
		sb.append("\t</div>\n\n");

		// Do stats for yesterday...
		sb.append("\t<div class='statsSection'>\n");
		sb.append(generateSection(R.string.yesterdayStats, "yesterday"));
		sb.append("\t\t<hr />\n");
		sb.append("\t</div>\n\n");

		// Do stats for this month
		sb.append("\t<div class='statsSection'>\n");
		sb.append(generateSection(R.string.this_months_stats, "month"));
		sb.append("\t\t<hr />\n");
		sb.append("\t</div>\n\n");

		// Do last month's stats
		sb.append("\t<div class='statsSection'>\n");
		sb.append(generateSection(R.string.last_month_stats, "last_month"));
		sb.append("\t\t<hr />\n");
		sb.append("\t</div>\n\n");

		// Do stats for this year
		sb.append("\t<div class='statsSection'>\n");
		sb.append(generateSection(R.string.this_year_stats, "year"));
		sb.append("\t\t<hr />\n");
		sb.append("\t</div>\n\n");

		// Do last year's stats
		sb.append("\t<div class='statsSection'>\n");
		sb.append(generateSection(R.string.last_year_stats, "last_year"));
		sb.append("\t\t<hr />\n");
		sb.append("\t</div>\n\n");

		// Do all time stats
		sb.append("\t<div class='statsSection'>\n");
		sb.append(generateSection(R.string.all_time_stats, "alltime"));
		sb.append("\t</div>\n\n");

		sb.append("</div>\n\n<div style='margin-bottom:5px'></div>\n");

		if(weeWXAppCommon.web_debug_on)
			sb.append(weeWXApp.debug_html);

		sb.append(weeWXApp.html_footer);

		if(weeWXAppCommon.debug_html)
			CustomDebug.writeOutput(requireContext(), "stats", sb.toString(), isVisible(), requireActivity());

		wv.post(() -> wv.loadDataWithBaseURL("file:///android_asset/",
				sb.toString(), "text/html", "utf-8", null));

		stopRefreshing();
	}
}