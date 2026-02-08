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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;

public class Stats extends Fragment
{
	private Slider mySlider;
	private int currZoom = 0;
	private View rootView;
	private SafeWebView wv;
	private SwipeRefreshLayout swipeLayout;
	private final ViewTreeObserver.OnScrollChangedListener scl = () -> swipeLayout.setEnabled(wv.getScrollY() == 0);

	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
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
			weeWXAppCommon.getWeather(true, false, false);
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
			wv = weeWXApp.getInstance().wvpl.getWebView();

		if(wv.getParent() != null)
			((ViewGroup)wv.getParent()).removeView(wv);

		FrameLayout fl = rootView.findViewById(R.id.webViewFrameLayout);
		fl.removeAllViews();
		fl.addView(wv);

		wv.getViewTreeObserver().addOnScrollChangedListener(scl);

		wv.setOnPageFinishedListener((v, url) ->
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
		weeWXAppCommon.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);

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

		weeWXAppCommon.NotificationManager.getNotificationLiveData().removeObservers(getViewLifecycleOwner());

		if(wv != null)
		{
			ViewParent parent = wv.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(wv);

			wv.getViewTreeObserver().removeOnScrollChangedListener(scl);

			weeWXApp.getInstance().wvpl.recycleWebView(wv);

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

		if(zoom > 199)
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

				    if(value1 == null || value1.isBlank() || value1.equals("null") || value1.equals("-1"))
				    {
					    handler.postDelayed(this, 150);
						return;
				    }

					Float f = weeWXAppCommon.str2Float(value1);
					if(f == null)
					{
						LogMessage("f is null!", KeyValue.i);

						handler.postDelayed(this, 150);
						return;
					}

					LogMessage("f == " + f, KeyValue.i);

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

							handler.postDelayed(this, 150);
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

	private String convert(String cur)
	{
		cur = cur.strip();

		//LogMessage("Old cur: " + cur);

		String[] time = cur.split(":");
		cur = time[0];
		if(time.length > 1)
			cur += ":" + time[1];
		if(cur.length() > 1 && cur.startsWith("0"))
			cur = cur.substring(1);

		//LogMessage("New cur: " + cur);

		return cur;
	}

	private String getTimeMonth(String str)
	{
		if(str.length() > 2)
			return weeWXAppCommon.getDaySuffix((int)Float.parseFloat(weeWXAppCommon.getDateFromString(str).substring(0, 2)));

		return str;
	}

	private String getTimeYear(String str)
	{
		str = getAllTime(str);

		if(str.length() > 2)
			return str.substring(0, str.length() - 2);

		return str;
	}

	private String getAllTime(String str)
	{
		str = weeWXAppCommon.getDateFromString(str);
		if(str.length() > 1 && str.startsWith("0"))
			str = str.substring(1);
		return str;
	}

	private String createRowLeft()
	{
		return createRowLeft(null, weeWXApp.emptyField, weeWXApp.emptyField);
	}

	private String createRowLeft(String class1, String str1, String str2)
	{
		String icon = "";
		if(class1 != null && !class1.isBlank())
			icon = weeWXAppCommon.cssToSVG(class1);

		return "\t\t<div class='statsDataRow'>\n" +
		       "\t\t\t<div class='statsDataCell left'>" + icon + "</div>\n" +
		       weeWXApp.currentSpacer +
		       "\t\t\t<div class='statsDataCell midleft'>" + str1 + "</div>\n" +
		       weeWXApp.currentSpacer +
		       "\t\t\t<div class='statsDataCell midleft'>" + str2 + "</div>\n";
	}

	private String createRowRight()
	{
		return createRowRight(null, weeWXApp.emptyField, weeWXApp.emptyField);
	}

	private String createRowRight(String class2, String str3, String str4)
	{
		String icon = "";
		if(class2 != null && !class2.isBlank())
			icon = weeWXAppCommon.cssToSVG(class2);

		return "\t\t\t<div class='statsDataCell midright'>" + str3 + "</div>\n" +
		       weeWXApp.currentSpacer +
		       "\t\t\t<div class='statsDataCell midright'>" + str4 + "</div>\n" +
		       weeWXApp.currentSpacer +
		       "\t\t\t<div class='statsDataCell right'>" + icon + "</div>\n" +
		       "\t\t</div>\n\n";
	}

	private String createRow(String class1, String class2, String str1,
	                         String str2, String str3, String str4)
	{
		if((str1 == null || str1.isBlank()) &&
		   (str2 == null || str2.isBlank()) &&
		   (str3 == null || str3.isBlank()) &&
		   (str4 == null || str4.isBlank()))
			return "";

		return createRowLeft(class1, str1, str2) +
		       weeWXApp.currentSpacer +
		       createRowRight(class2, str3, str4);
	}

	private String createRowLeft(String class1, String str1)
	{
		return "\t\t<div class='statsDataRow'>\n" +
		       "\t\t\t<div class='statsDataCell left'>" +
		       weeWXAppCommon.fiToSVG("flaticon-windy") +
		       "</div>\n\t\t\t" + weeWXApp.currentSpacer +
		       "\t\t\t<div class='statsDataCell " + class1 + "'>" + str1 + "</div>\n";
	}

	private String createRowRight(String class2, String str2)
	{
		return "\t\t\t<div class='statsDataCell " + class2 + "'>" +
		       str2 +
		       "</div>\n\t\t\t" + weeWXApp.currentSpacer +
		       "\t\t\t<div class='statsDataCell right'>" +
		       weeWXAppCommon.cssToSVG("wi-umbrella") +
		       "</div>\n\t\t</div>\n\n";
	}

	private String createRow(String str1, String str2)
	{
		return createRowLeft("Wind", str1) +
		       weeWXApp.currentSpacer +
		       createRowRight("Rain", str2);
	}

	private String createRow2(String str1, String str2)
	{
		return createRowLeft("Wind2", str1) + createRowRight("Rain2", str2);
	}

	private String createSolarUV(String[] bits, int uv, int uvWhen, int solar,
	                             int solarWhen, int timeMode, String which)
	{
		if(bits.length <= Math.max(Math.max(uv, uvWhen), Math.max(solar, solarWhen)))
		{
			LogMessage("No solar or UV data, skipping...");
			return "";
		}

		String UV = weeWXAppCommon.getElement(bits, uv).strip();
		if(UV.contains("N/A"))
			UV = "";

		if(weeWXAppCommon.getElement(bits, uvWhen).strip().isBlank())
			UV = "";

		String SOLAR = weeWXAppCommon.getElement(bits, solar).strip();
		if(SOLAR.contains("N/A"))
			SOLAR = "";

		if(weeWXAppCommon.getElement(bits, solarWhen).strip().isBlank())
			SOLAR = "";


		if(UV.isBlank() && SOLAR.isBlank())
		{
			LogMessage("No solar and UV data, skipping...");
			return "";
		}

		String className = weeWXAppCommon.fiToSVG("flaticon-women-sunglasses");
		String out = "";

		if(!UV.isBlank())
		{
			String dateTimeStr = getDateTimeStr(bits, uvWhen, timeMode, which);
			out += createRowLeft(className, UV + "UVI", dateTimeStr);
		} else {
			out += createRowLeft();
		}

		out += weeWXApp.currentSpacer;

		if(!SOLAR.isBlank())
		{
			String dateTimeStr = getDateTimeStr(bits, solarWhen, timeMode, which);
			out += createRowRight(className, dateTimeStr, SOLAR + "W/m²");
		} else {
			out += createRowRight();
		}

		return out;
	}

	private String showIndoor(int appendId, String[] bits, int max,
	                          int maxWhen, int min, int minWhen,
	                          int timeMode, String which)
	{
		if(bits.length < maxWhen || weeWXAppCommon.getElement(bits, maxWhen).isBlank() ||
		   weeWXAppCommon.getElement(bits, minWhen).isBlank())
			return "";

		String maxDateTimeStr = getDateTimeStr(bits, maxWhen, timeMode, which);
		String minDateTimeStr = getDateTimeStr(bits, minWhen, timeMode, which);
		if((maxDateTimeStr == null || maxDateTimeStr.isBlank()) &&
		   (minDateTimeStr == null || minDateTimeStr.isBlank()))
			return "";

		return createRow(weeWXAppCommon.fiToSVG("flaticon-home-page"),
				weeWXAppCommon.fiToSVG("flaticon-home-page"),
				weeWXAppCommon.getElement(bits, max) + weeWXAppCommon.getElement(bits, appendId),
				maxDateTimeStr, minDateTimeStr,
				weeWXAppCommon.getElement(bits, min) + weeWXAppCommon.getElement(bits, appendId));
	}

	private String getDateTimeStr(String[] bits, int when, int timeMode, String which)
	{
		String dateTimeStr = "";

		if(timeMode == 0)
			dateTimeStr = convert(weeWXAppCommon.getElement(bits, when));
		else if(timeMode == 1)
			dateTimeStr = getTimeMonth(weeWXAppCommon.getElement(bits, when));
		else if(timeMode == 2)
			dateTimeStr = getTimeYear(weeWXAppCommon.getElement(bits, when));
		else if(timeMode == 3)
			dateTimeStr = getTimeSection(which, weeWXAppCommon.getElement(bits, when));
		else if(timeMode == 4)
			dateTimeStr = getAllTime(weeWXAppCommon.getElement(bits, when));

		return dateTimeStr;
	}

	private String getTimeSection(String which, String str)
	{
		if(which.equals("Year"))
			return getAllTime(str);

		return getTimeYear(str);
	}

	private String generateTodaysSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t\t<div class='statsHeader'>\n\t\t\t");
		sb.append(weeWXApp.getAndroidString(header));
		sb.append("\n\t\t</div>\n\n");

		sb.append(createRow(weeWXAppCommon.fiToSVG("flaticon-temperature"),
				weeWXAppCommon.fiToSVG("flaticon-temperature"),
				weeWXAppCommon.getElement(bits, 3) + weeWXAppCommon.getElement(bits, 60),
				convert(weeWXAppCommon.getElement(bits, 4)),
				convert(weeWXAppCommon.getElement(bits, 2)),
				weeWXAppCommon.getElement(bits, 1) + weeWXAppCommon.getElement(bits, 60)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-raindrop"),
				weeWXAppCommon.cssToSVG("wi-raindrop"),
				weeWXAppCommon.getElement(bits, 15) + weeWXAppCommon.getElement(bits, 60),
				convert(weeWXAppCommon.getElement(bits, 16)),
				convert(weeWXAppCommon.getElement(bits, 14)),
				weeWXAppCommon.getElement(bits, 13) + weeWXAppCommon.getElement(bits, 60)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-humidity"),
				weeWXAppCommon.cssToSVG("wi-humidity"),
				weeWXAppCommon.getElement(bits, 9) + weeWXAppCommon.getElement(bits, 64),
				convert(weeWXAppCommon.getElement(bits, 10)),
				convert(weeWXAppCommon.getElement(bits, 8)),
				weeWXAppCommon.getElement(bits, 6) + weeWXAppCommon.getElement(bits, 64)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-barometer"),
				weeWXAppCommon.cssToSVG("wi-barometer"),
				weeWXAppCommon.getElement(bits, 39) + weeWXAppCommon.getElement(bits, 63),
				convert(weeWXAppCommon.getElement(bits, 40)),
				convert(weeWXAppCommon.getElement(bits, 42)),
				weeWXAppCommon.getElement(bits, 41) + weeWXAppCommon.getElement(bits, 63)));

		if((boolean)KeyValue.readVar("showIndoor", weeWXApp.showIndoor_default))
		{
			if(bits.length > 165 && !bits[163].isBlank() && !bits[165].isBlank())
				sb.append(showIndoor(60, bits, 164, 165, 162, 163, 0, null));

			if(bits.length > 170 && !bits[170].isBlank() && !bits[168].isBlank())
				sb.append(showIndoor(64, bits, 169, 170, 167, 168, 0, null));
		}

		sb.append(createSolarUV(bits, 205, 206, 207, 208, 0, null));

		String rain = weeWXAppCommon.getElement(bits, 20);
		String since = weeWXApp.getAndroidString(R.string.since) + " mn";

		if(bits.length > 158 && !bits[158].isBlank())
		{
			rain = bits[158];

			if(bits.length > 160 && !bits[160].isBlank())
				since = weeWXApp.getAndroidString(R.string.since) + " " + weeWXAppCommon.getElement(bits, 160);
		}

		sb.append(createRow(weeWXAppCommon.getElement(bits, 19) + weeWXAppCommon.getElement(bits, 61) +
                " " + weeWXAppCommon.getElement(bits, 32) + " " + convert(weeWXAppCommon.getElement(bits, 33)),
				since + " " + rain + weeWXAppCommon.getElement(bits, 62)));

		return sb.toString();
	}

	private String generateYesterdaysSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t\t<div class='statsHeader'>\n\t\t\t");
		sb.append(weeWXApp.getAndroidString(header));
		sb.append("\n\t\t</div>\n\n");

		sb.append(createRow(weeWXAppCommon.fiToSVG("flaticon-temperature"),
				weeWXAppCommon.fiToSVG("flaticon-temperature"),
				weeWXAppCommon.getElement(bits, 67) + weeWXAppCommon.getElement(bits, 60),
				convert(weeWXAppCommon.getElement(bits, 68)),
				convert(weeWXAppCommon.getElement(bits, 66)),
				weeWXAppCommon.getElement(bits, 65) + weeWXAppCommon.getElement(bits, 60)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-raindrop"), weeWXAppCommon.cssToSVG("wi-raindrop"),
				weeWXAppCommon.getElement(bits, 78) + weeWXAppCommon.getElement(bits, 60),
				convert(weeWXAppCommon.getElement(bits, 79)),
				convert(weeWXAppCommon.getElement(bits, 77)),
				weeWXAppCommon.getElement(bits, 76) + weeWXAppCommon.getElement(bits, 64)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-humidity"), weeWXAppCommon.cssToSVG("wi-humidity"),
				weeWXAppCommon.getElement(bits, 82) + weeWXAppCommon.getElement(bits, 64),
				convert(weeWXAppCommon.getElement(bits, 83)),
				convert(weeWXAppCommon.getElement(bits, 81)),
				weeWXAppCommon.getElement(bits, 80) + weeWXAppCommon.getElement(bits, 64)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-barometer"), weeWXAppCommon.cssToSVG("wi-barometer"),
				weeWXAppCommon.getElement(bits, 84) + weeWXAppCommon.getElement(bits, 63),
				convert(weeWXAppCommon.getElement(bits, 85)),
				convert(weeWXAppCommon.getElement(bits, 87)),
				weeWXAppCommon.getElement(bits, 86) + weeWXAppCommon.getElement(bits, 63)));

		if((boolean)KeyValue.readVar("showIndoor", weeWXApp.showIndoor_default))
		{
			if(bits.length > 174 && !bits[174].isBlank() && !bits[172].isBlank())
				sb.append(showIndoor(60, bits, 173, 174, 171, 172, 0, null));

			if(bits.length > 178 && !bits[178].isBlank() && !bits[176].isBlank())
				sb.append(showIndoor(64, bits, 177, 178, 175, 176, 0, null));
		}

		sb.append(createSolarUV(bits, 209, 210, 211, 212, 0, null));

		String rain = bits[21];
		String before = weeWXApp.getAndroidString(R.string.before) + " mn";

		if(bits.length > 159 && !bits[159].isBlank())
		{
			rain = bits[159];

			if(bits.length > 160 && !bits[160].isBlank())
				before = weeWXApp.getAndroidString(R.string.before) + " " + weeWXAppCommon.getElement(bits, 160);
		}

		sb.append(createRow(weeWXAppCommon.getElement(bits, 69) + weeWXAppCommon.getElement(bits, 61) + " " +
		                    weeWXAppCommon.getElement(bits, 70) + " " + convert(weeWXAppCommon.getElement(bits, 71)),
							before + " " + rain + weeWXAppCommon.getElement(bits, 62)));

		return sb.toString();
	}

	private String generateThisMonthsSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t\t<div class='statsHeader'>\n\t\t\t");
		sb.append(weeWXApp.getAndroidString(header));
		sb.append("\n\t\t</div>\n\n");

		sb.append(createRow(weeWXAppCommon.fiToSVG("flaticon-temperature"),
				weeWXAppCommon.fiToSVG("flaticon-temperature"),
				weeWXAppCommon.getElement(bits, 90) + weeWXAppCommon.getElement(bits, 60),
				getTimeMonth(weeWXAppCommon.getElement(bits, 91)),
				getTimeMonth(weeWXAppCommon.getElement(bits, 89)),
				weeWXAppCommon.getElement(bits, 88) + weeWXAppCommon.getElement(bits, 60)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-raindrop"), weeWXAppCommon.cssToSVG("wi-raindrop"),
				weeWXAppCommon.getElement(bits, 101) + weeWXAppCommon.getElement(bits, 60),
				getTimeMonth(weeWXAppCommon.getElement(bits, 102)),
				getTimeMonth(weeWXAppCommon.getElement(bits, 100)),
				weeWXAppCommon.getElement(bits, 99) + weeWXAppCommon.getElement(bits, 64)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-humidity"), weeWXAppCommon.cssToSVG("wi-humidity"),
				weeWXAppCommon.getElement(bits, 105) + weeWXAppCommon.getElement(bits, 64),
				getTimeMonth(weeWXAppCommon.getElement(bits, 106)),
				getTimeMonth(weeWXAppCommon.getElement(bits, 104)),
				weeWXAppCommon.getElement(bits, 103) + weeWXAppCommon.getElement(bits, 64)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-barometer"), weeWXAppCommon.cssToSVG("wi-barometer"),
				weeWXAppCommon.getElement(bits, 107) + weeWXAppCommon.getElement(bits, 63),
				getTimeMonth(weeWXAppCommon.getElement(bits, 108)),
				getTimeMonth(weeWXAppCommon.getElement(bits, 110)),
				weeWXAppCommon.getElement(bits, 109) + weeWXAppCommon.getElement(bits, 63)));

		if((boolean)KeyValue.readVar("showIndoor", weeWXApp.showIndoor_default))
		{
			if(bits.length > 182 && !bits[182].isBlank() && !bits[180].isBlank())
				sb.append(showIndoor(60, bits, 181, 182, 179, 180, 1, null));

			if(bits.length > 186 && !bits[186].isBlank() && !bits[184].isBlank())
				sb.append(showIndoor(64, bits, 185, 186, 183, 184, 1, null));
		}

		sb.append(createSolarUV(bits, 213, 214, 215, 216, 1, null));

		sb.append(createRow2(weeWXAppCommon.getElement(bits, 92) + weeWXAppCommon.getElement(bits, 61) + " " +
		                     weeWXAppCommon.getElement(bits, 93) + " " + getTimeMonth(weeWXAppCommon.getElement(bits, 94)),
							 weeWXAppCommon.getElement(bits, 22) + weeWXAppCommon.getElement(bits, 62)));

		return sb.toString();
	}

	private String generateThisYearsSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t\t<div class='statsHeader'>\n\t\t\t");
		sb.append(weeWXApp.getAndroidString(header));
		sb.append("\n\t\t</div>\n\n");

		sb.append(createRow(weeWXAppCommon.fiToSVG("flaticon-temperature"),
				weeWXAppCommon.fiToSVG("flaticon-temperature"),
				weeWXAppCommon.getElement(bits, 113) + weeWXAppCommon.getElement(bits, 60),
				getTimeYear(weeWXAppCommon.getElement(bits, 114)),
				getTimeYear(weeWXAppCommon.getElement(bits, 112)),
				weeWXAppCommon.getElement(bits, 111) + weeWXAppCommon.getElement(bits, 60)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-raindrop"), weeWXAppCommon.cssToSVG("wi-raindrop"),
				weeWXAppCommon.getElement(bits, 124) + weeWXAppCommon.getElement(bits, 60),
				getTimeYear(weeWXAppCommon.getElement(bits, 125)),
				getTimeYear(weeWXAppCommon.getElement(bits, 123)),
				weeWXAppCommon.getElement(bits, 122) + weeWXAppCommon.getElement(bits, 60)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-humidity"), weeWXAppCommon.cssToSVG("wi-humidity"),
				weeWXAppCommon.getElement(bits, 128) + weeWXAppCommon.getElement(bits, 64),
				getTimeYear(weeWXAppCommon.getElement(bits, 129)),
				getTimeYear(weeWXAppCommon.getElement(bits, 127)),
				weeWXAppCommon.getElement(bits, 126) + weeWXAppCommon.getElement(bits, 64)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-barometer"), weeWXAppCommon.cssToSVG("wi-barometer"),
				weeWXAppCommon.getElement(bits, 130) + weeWXAppCommon.getElement(bits, 63),
				getTimeYear(weeWXAppCommon.getElement(bits, 131)),
				getTimeYear(weeWXAppCommon.getElement(bits, 133)),
				weeWXAppCommon.getElement(bits, 132) + weeWXAppCommon.getElement(bits, 63)));

		if((boolean)KeyValue.readVar("showIndoor", weeWXApp.showIndoor_default))
		{
			if(bits.length > 190 && !bits[190].isBlank() && !bits[188].isBlank())
				sb.append(showIndoor(60, bits, 189, 190, 187, 188, 2, null));

			if(bits.length > 194 && !bits[194].isBlank() && !bits[192].isBlank())
				sb.append(showIndoor(64, bits, 193, 194, 191, 192, 2, null));
		}

		sb.append(createSolarUV(bits, 217, 218, 219, 220, 2, null));

		sb.append(createRow2(weeWXAppCommon.getElement(bits, 115) + weeWXAppCommon.getElement(bits, 61) + " " +
		                     weeWXAppCommon.getElement(bits, 116) + " " + getTimeYear(weeWXAppCommon.getElement(bits, 117)),
							 weeWXAppCommon.getElement(bits, 23) + weeWXAppCommon.getElement(bits, 62)));

		return sb.toString();
	}

	private String generateLastSection(String[] bits, int start, String which)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t\t<div class='statsHeader'>\n\t\t\t");

		if(which.equals("Year"))
			sb.append(weeWXApp.getAndroidString(R.string.last_year_stats));
		else
			sb.append(weeWXApp.getAndroidString(R.string.last_month_stats));

		sb.append("\n\t\t</div>\n\n");

		sb.append(createRow(weeWXAppCommon.fiToSVG("flaticon-temperature"),
				weeWXAppCommon.fiToSVG("flaticon-temperature"),
				bits[start + 2] + weeWXAppCommon.getElement(bits, 60),
				getTimeSection(which, bits[start + 3]),
				getTimeSection(which, bits[start + 1]),
				bits[start] + weeWXAppCommon.getElement(bits, 60)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-raindrop"), weeWXAppCommon.cssToSVG("wi-raindrop"),
				bits[start + 13] + weeWXAppCommon.getElement(bits, 60),
				getTimeSection(which, bits[start + 14]),
				getTimeSection(which, bits[start + 12]),
				bits[start + 11] + weeWXAppCommon.getElement(bits, 60)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-humidity"), weeWXAppCommon.cssToSVG("wi-humidity"),
				bits[start + 17] + weeWXAppCommon.getElement(bits, 64),
				getTimeSection(which, bits[start + 18]),
				getTimeSection(which, bits[start + 16]),
				bits[start + 15] + weeWXAppCommon.getElement(bits, 64)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-barometer"), weeWXAppCommon.cssToSVG("wi-barometer"),
				bits[start + 7] + weeWXAppCommon.getElement(bits, 63),
				getTimeSection(which, bits[start + 8]),
				getTimeSection(which, bits[start + 10]),
				bits[start + 9] + weeWXAppCommon.getElement(bits, 63)));

		if((boolean)KeyValue.readVar("showIndoor", weeWXApp.showIndoor_default))
		{
			if(bits.length > start + 23 && !bits[start + 23].isBlank() && !bits[start + 21].isBlank())
				sb.append(showIndoor(60, bits,
						start + 22, start + 23, start + 20, start + 21, 3, which));

			if(bits.length > start + 27 && !bits[start + 27].isBlank() && !bits[start + 25].isBlank())
				sb.append(showIndoor(64, bits,
						start + 26, start + 27, start + 24, start + 25, 3, which));
		}

		sb.append(createSolarUV(bits, start + 28, start + 29, start + 30, start + 31, 3, which));

		sb.append(createRow2(bits[start + 4] + weeWXAppCommon.getElement(bits, 61) + " " +
		                     bits[start + 5] + " " + getTimeSection(which, bits[start + 6]),
							 bits[start + 19] + weeWXAppCommon.getElement(bits, 62)));

		return sb.toString();
	}

	private String generateAllTimeSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t\t<div class='statsHeader'>\n\t\t\t");
		sb.append(weeWXApp.getAndroidString(header));
		sb.append("\n\t\t</div>\n\n");

		sb.append(createRow(weeWXAppCommon.fiToSVG("flaticon-temperature"),
				weeWXAppCommon.fiToSVG("flaticon-temperature"),
				weeWXAppCommon.getElement(bits, 136) + weeWXAppCommon.getElement(bits, 60),
				getAllTime(weeWXAppCommon.getElement(bits, 137)),
				getAllTime(weeWXAppCommon.getElement(bits, 135)),
				weeWXAppCommon.getElement(bits, 134) + weeWXAppCommon.getElement(bits, 60)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-raindrop"),
				weeWXAppCommon.cssToSVG("wi-raindrop"),
				weeWXAppCommon.getElement(bits, 147) + weeWXAppCommon.getElement(bits, 60),
				getAllTime(weeWXAppCommon.getElement(bits, 148)),
				getAllTime(weeWXAppCommon.getElement(bits, 146)),
				weeWXAppCommon.getElement(bits, 145) + weeWXAppCommon.getElement(bits, 60)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-humidity"),
				weeWXAppCommon.cssToSVG("wi-humidity"),
				weeWXAppCommon.getElement(bits, 151) + weeWXAppCommon.getElement(bits, 64),
				getAllTime(weeWXAppCommon.getElement(bits, 152)),
				getAllTime(weeWXAppCommon.getElement(bits, 150)),
				weeWXAppCommon.getElement(bits, 149) + weeWXAppCommon.getElement(bits, 64)));

		sb.append(createRow(weeWXAppCommon.cssToSVG("wi-barometer"),
				weeWXAppCommon.cssToSVG("wi-barometer"),
				weeWXAppCommon.getElement(bits, 153) + weeWXAppCommon.getElement(bits, 63),
				getAllTime(weeWXAppCommon.getElement(bits, 154)),
				getAllTime(weeWXAppCommon.getElement(bits, 156)),
				weeWXAppCommon.getElement(bits, 155) + weeWXAppCommon.getElement(bits, 63)));

		if((boolean)KeyValue.readVar("showIndoor", weeWXApp.showIndoor_default))
		{
			if(bits.length > 198 && !bits[198].isBlank() && !bits[196].isBlank())
				sb.append(showIndoor(60, bits,
						197, 198, 195, 196, 4, null));

			if(bits.length > 202 && !bits[202].isBlank() && !bits[200].isBlank())
				sb.append(showIndoor(64, bits,
						201, 202, 199, 200, 4, null));
		}

		sb.append(createSolarUV(bits, 221, 222, 223, 224, 4, null));

		sb.append(createRow2(weeWXAppCommon.getElement(bits, 138) + weeWXAppCommon.getElement(bits, 61) + " " +
		                     weeWXAppCommon.getElement(bits, 139) + " " + getAllTime(weeWXAppCommon.getElement(bits, 140)),
							 weeWXAppCommon.getElement(bits, 157) + weeWXAppCommon.getElement(bits, 62)));

		return sb.toString();
	}

	private void updateFields()
	{
		LogMessage("Stats.java updateFields()");

		boolean ret = weeWXAppCommon.getWeather(false, false, false);
		if(!ret)
		{
			String LastWeatherError = (String)KeyValue.readVar("LastWeatherError", "");
			if(LastWeatherError != null && !LastWeatherError.isBlank())
				wv.post(() -> wv.loadDataWithBaseURL(null, LastWeatherError,
						"text/html", "utf-8", null));
			else
				wv.post(() -> wv.loadDataWithBaseURL(null,
						weeWXApp.getAndroidString(R.string.unknown_error_occurred),
						"text/html", "utf-8", null));

			KeyValue.putVar("LastWeatherError", null);

			setZoom(currZoom, true);

			stopRefreshing();
			return;
		}

		String[] bits = ((String)KeyValue.readVar("LastDownload", "")).split("\\|");

		if(bits.length < 100)
		{
			wv.post(() -> wv.loadDataWithBaseURL(null,
					weeWXApp.getAndroidString(R.string.unknown_error_occurred),
					"text/html", "utf-8", null));

			setZoom(currZoom, true);

			stopRefreshing();
			return;
		}

		// Today Stats
		checkFields(rootView.findViewById(R.id.textView),
				weeWXAppCommon.getElement(bits, 56));
		checkFields(rootView.findViewById(R.id.textView2),
				weeWXAppCommon.getElement(bits, 54) + " " + weeWXAppCommon.getElement(bits, 55));

		final StringBuilder sb = new StringBuilder();

		sb.append(weeWXApp.current_html_headers)
				.append(weeWXApp.script_header)
				.append(weeWXApp.html_header_rest)
				.append(weeWXApp.inline_arrow);

		sb.append("\n<div class='statsLayout'>\n\n");

		// Show today's stats
		sb.append("\t<div class='statsSection'>\n");
		sb.append(generateTodaysSection(R.string.todayStats, bits));
		sb.append("\t\t<hr />\n");
		sb.append("\t</div>\n\n");

		// Do stats for yesterday...
		sb.append("\t<div class='statsSection'>\n");
		sb.append(generateYesterdaysSection(R.string.yesterdayStats, bits));
		sb.append("\t\t<hr />\n");
		sb.append("\t</div>\n\n");

		if(bits.length > 110 && !bits[110].isBlank())
		{
			// Do stats for this month
			sb.append("\t<div class='statsSection'>\n");
			sb.append(generateThisMonthsSection(R.string.this_months_stats, bits));
			sb.append("\t\t<hr />\n");
			sb.append("\t</div>\n\n");
		}

		if(bits.length > 268 && !bits[267].isBlank())
		{
			// Do last month's stats
			sb.append("\t<div class='statsSection'>\n");
			sb.append(generateLastSection(bits, 258, "Month"));
			sb.append("\t\t<hr />\n");
			sb.append("\t</div>\n\n");
		}

		if(bits.length > 133 && !bits[133].isBlank())
		{
			// Do stats for this year
			sb.append("\t<div class='statsSection'>\n");
			sb.append(generateThisYearsSection(R.string.this_year_stats, bits));
			sb.append("\t\t<hr />\n");
			sb.append("\t</div>\n\n");
		}

		if(bits.length > 236 && !bits[236].isBlank())
		{
			// Do last year's stats
			sb.append("\t<div class='statsSection'>\n");
			sb.append(generateLastSection(bits, 226, "Year"));
			sb.append("\t\t<hr />\n");
			sb.append("\t</div>\n\n");
		}

		if(bits.length > 156 && !bits[156].isBlank())
		{
			// Do all time stats
			sb.append("\t<div class='statsSection'>\n");
			sb.append(generateAllTimeSection(R.string.all_time_stats, bits));
			sb.append("\t</div>\n\n");
		}

		sb.append("</div>\n\n<div style='margin-bottom:5px'></div>\n");

		if(weeWXAppCommon.web_debug_on)
			sb.append(weeWXApp.debug_html);

		sb.append(weeWXApp.html_footer);

		//if(weeWXAppCommon.debug_html)
		//	CustomDebug.writeOutput(requireContext(), "stats", sb.toString(), isVisible(), requireActivity());

		wv.post(() -> wv.loadDataWithBaseURL("file:///android_asset/",
				sb.toString(), "text/html", "utf-8", null));

		setZoom(currZoom, true);

		stopRefreshing();
	}
}