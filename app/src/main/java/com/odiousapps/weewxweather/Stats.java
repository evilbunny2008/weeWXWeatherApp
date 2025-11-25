package com.odiousapps.weewxweather;

import android.os.Bundle;
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

public class Stats extends Fragment
{
	private Slider mySlider;
	private int currZoom;
	private View rootView;
	private SafeWebView wv;
	private SwipeRefreshLayout swipeLayout;
	private final ViewTreeObserver.OnScrollChangedListener scl = () -> swipeLayout.setEnabled(wv.getScrollY() == 0);

	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		weeWXAppCommon.LogMessage("Stats.onCreateView()");
		super.onCreateView(inflater, container, savedInstanceState);

		rootView = inflater.inflate(R.layout.fragment_stats, container, false);

		LinearLayout ll = rootView.findViewById(R.id.stats_ll);
		ll.setBackgroundColor(KeyValue.bgColour);

		swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
		swipeLayout.setOnRefreshListener(() ->
		{
			swipeLayout.setRefreshing(true);
			weeWXAppCommon.LogMessage("weeWXAppCommon.getWeather(true, false)...", true);
			weeWXAppCommon.getWeather(true, false);

		});

		mySlider = rootView.findViewById(R.id.pageZoom);
		mySlider.setBackgroundColor(KeyValue.bgColour);
		mySlider.addOnChangeListener((slider, value, fromUser) ->
		{
			weeWXAppCommon.LogMessage("Current Slider zoom =" + (int)mySlider.getValue() + "%");
			weeWXAppCommon.LogMessage("New Slider zoom =" + value + "%");

			if(fromUser && currZoom != (int)value)
			{
				currZoom = (int)value;
				weeWXAppCommon.SetIntPref("mySlider", currZoom);
				setZoom(currZoom);
			}
		});

		return rootView;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		weeWXAppCommon.LogMessage("Stats.onViewCreated()");
		super.onViewCreated(view, savedInstanceState);

		if(wv == null)
			wv = WebViewPreloader.getInstance().getWebView(requireContext());

		if(wv.getParent() != null)
			((ViewGroup)wv.getParent()).removeView(wv);

		FrameLayout fl = view.findViewById(R.id.webViewFrameLayout);
		fl.removeAllViews();
		fl.addView(wv);

		wv.getViewTreeObserver().addOnScrollChangedListener(scl);

		swipeLayout.setRefreshing(true);
		currZoom = sanitiseZoom(weeWXAppCommon.GetIntPref("mySlider", weeWXApp.mySlider_default));
		if(currZoom != (int)mySlider.getValue())
		{
			setZoom(currZoom);
			weeWXAppCommon.LogMessage("currZoom: " + currZoom + "%");
		}

		updateFields();

		weeWXAppCommon.LogMessage("Stats.onViewCreated()-- adding notification manager...");
		weeWXAppCommon.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);
	}

	@Override
	public void onDestroyView()
	{
		weeWXAppCommon.LogMessage("Stats.onDestroyView()");
		super.onDestroyView();

		if(wv != null)
		{
			ViewParent parent = wv.getParent();
			if(parent instanceof ViewGroup)
				((ViewGroup)parent).removeView(wv);

			wv.getViewTreeObserver().removeOnScrollChangedListener(scl);

			WebViewPreloader.getInstance().recycleWebView(wv);

			weeWXAppCommon.LogMessage("Stats.onDestroyView() recycled wv...");
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
			zoom = 50;

		if(zoom > 200)
			zoom = 200;

		return zoom;
	}

	void setZoom(int zoom)
	{
		if(mySlider == null || wv == null)
			return;

		final int finalZoom = sanitiseZoom(zoom);

		weeWXAppCommon.LogMessage("new zoom value = " + finalZoom + "%");

		mySlider.post(() -> mySlider.setValue(finalZoom));
		wv.post(() -> wv.getSettings().setTextZoom(finalZoom));
	}

	private final Observer<String> notificationObserver = str ->
	{
		weeWXAppCommon.LogMessage("notificationObserver: " + str);

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
				tv.setBackgroundColor(KeyValue.bgColour);
				tv.setTextColor(KeyValue.fgColour);
				tv.setText(txt);
			});
	}

	private String convert(String cur)
	{
		cur = cur.strip();

		weeWXAppCommon.LogMessage("Old cur: " + cur);

		String[] time = cur.split(":");
		cur = time[0];
		if(time.length > 1)
			cur += ":" + time[1];
		if(cur.length() > 1 && cur.startsWith("0"))
			cur = cur.substring(1);

		weeWXAppCommon.LogMessage("New cur: " + cur);

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
			icon = "<i class='" + class1 + " icon'></i>";

		return "\t\t<div class='statsDataRow'>\n" +
		       "\t\t\t<div class='statsDataCell left'>" + icon + str1 + "</div>\n" +
		       "\t\t\t<div class='statsDataCell midleft'>" + str2 + "</div>\n";
	}

	private String createRowMiddle()
	{
		return "\t\t\t<div class='statsSpacer'>" + weeWXApp.emptyField + "</div>\n";
	}

	private String createRowRight()
	{
		return createRowRight(null, weeWXApp.emptyField, weeWXApp.emptyField);
	}

	private String createRowRight(String class2, String str3, String str4)
	{
		String icon = "";
		if(class2 != null && !class2.isBlank())
			icon = "<i class='" + class2 + " icon'></i>";

		return "\t\t\t<div class='statsDataCell midright'>" + str3 + "</div>\n" +
		       "\t\t\t<div class='statsDataCell right'>" + str4 + icon + "</div>\n" +
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
		       createRowMiddle() +
		       createRowRight(class2, str3, str4);
	}

	private String createRowLeft(String class1, String str1)
	{
		return "\t\t<div class='statsDataRow'>\n" +
		       "\t\t\t<div class='statsDataCell " +
		       class1 + "'><i class='flaticon-windy icon'></i>" +
		       str1 + "</div>\n";
	}

	private String createRowRight(String class2, String str2)
	{
		return "\t\t\t<div class='statsDataCell " +
		       class2 + "'>" + str2 +
		       "<i class='wi wi-umbrella icon'></i></div>\n\t\t</div>\n\n";
	}

	private String createRow(String str1, String str2)
	{
		return createRowLeft("Wind", str1) + createRowMiddle() + createRowRight("Rain", str2);
	}

	private String createRow2(String str1, String str2)
	{
		return createRowLeft("Wind2", str1) + createRowRight("Rain2", str2);
	}

	private String getElement(String[] bits, int element)
	{
		try
		{
			if(bits.length > element)
				return bits[element].strip();
		} catch(Exception e) {
			weeWXAppCommon.LogMessage("Error! e: " + e, true);
		}

		return "";
	}	

	private String createSolarUV(String[] bits, int uv, int uvWhen, int solar, int solarWhen, int timeMode, String which)
	{
		if(bits.length <= Math.max(Math.max(uv, uvWhen), Math.max(solar, solarWhen)))
		{
			weeWXAppCommon.LogMessage("No solar or UV data, skipping...");
			return "";
		}

		String UV = getElement(bits, uv).strip();
		if(UV.contains("N/A"))
			UV = "";

		if(getElement(bits, uvWhen).strip().isBlank())
			UV = "";

		String SOLAR = getElement(bits, solar).strip();
		if(SOLAR.contains("N/A"))
			SOLAR = "";

		if(getElement(bits, solarWhen).strip().isBlank())
			SOLAR = "";


		if(UV.isBlank() && SOLAR.isBlank())
		{
			weeWXAppCommon.LogMessage("No solar and UV data, skipping...");
			return "";
		}

		String className = "flaticon-women-sunglasses";
		String out = "";

		if(!UV.isBlank())
		{
			String dateTimeStr = getDateTimeStr(bits, uvWhen, timeMode, which);
			out += createRowLeft(className, UV + "UVI", dateTimeStr);
		} else {
			out += createRowLeft();
		}

		out += createRowMiddle();

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
		if(bits.length < maxWhen || getElement(bits, maxWhen).isBlank() || getElement(bits, minWhen).isBlank())
			return "";

		String maxDateTimeStr = getDateTimeStr(bits, maxWhen, timeMode, which);
		String minDateTimeStr = getDateTimeStr(bits, minWhen, timeMode, which);
		if((maxDateTimeStr == null || maxDateTimeStr.isBlank()) && (minDateTimeStr == null || minDateTimeStr.isBlank()))
			return "";

		return createRow("flaticon-home-page", "flaticon-home-page",
				getElement(bits, max) + getElement(bits, appendId),
				maxDateTimeStr, minDateTimeStr,
				getElement(bits, min) + getElement(bits, appendId));
	}

	private String getDateTimeStr(String[] bits, int when, int timeMode, String which)
	{
		String dateTimeStr = "";

		if(timeMode == 0)
			dateTimeStr = convert(getElement(bits, when));
		else if(timeMode == 1)
			dateTimeStr = getTimeMonth(getElement(bits, when));
		else if(timeMode == 2)
			dateTimeStr = getTimeYear(getElement(bits, when));
		else if(timeMode == 3)
			dateTimeStr = getTimeSection(which, getElement(bits, when));
		else if(timeMode == 4)
			dateTimeStr = getAllTime(getElement(bits, when));

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

		sb.append(createRow("flaticon-temperature", "flaticon-temperature",
				getElement(bits, 3) + getElement(bits, 60),
				convert(getElement(bits, 4)),
				convert(getElement(bits, 2)),
				getElement(bits, 1) + getElement(bits, 60)));

		sb.append(createRow("wi wi-raindrop", "wi wi-raindrop",
				getElement(bits, 15) + getElement(bits, 60),
				convert(getElement(bits, 16)),
				convert(getElement(bits, 14)),
				getElement(bits, 13) + getElement(bits, 60)));

		sb.append(createRow("wi wi-humidity", "wi wi-humidity",
				getElement(bits, 9) + getElement(bits, 64),
				convert(getElement(bits, 10)),
				convert(getElement(bits, 8)),
				getElement(bits, 6) + getElement(bits, 64)));

		sb.append(createRow("wi wi-barometer", "wi wi-barometer",
				getElement(bits, 39) + getElement(bits, 63),
				convert(getElement(bits, 40)),
				convert(getElement(bits, 42)),
				getElement(bits, 41) + getElement(bits, 63)));

		if(weeWXAppCommon.GetBoolPref("showIndoor", weeWXApp.showIndoor_default))
		{
			if(bits.length > 165 && !bits[163].isBlank() && !bits[165].isBlank())
				sb.append(showIndoor(60, bits, 164, 165, 162, 163, 0, null));

			if(bits.length > 170 && !bits[170].isBlank() && !bits[168].isBlank())
				sb.append(showIndoor(64, bits, 169, 170, 167, 168, 0, null));
		}

		sb.append(createSolarUV(bits, 205, 206, 207, 208, 0, null));

		String rain = getElement(bits, 20);
		String since = weeWXApp.getAndroidString(R.string.since) + " mn";

		if(bits.length > 158 && !bits[158].isBlank())
		{
			rain = bits[158];

			if(bits.length > 160 && !bits[160].isBlank())
				since = weeWXApp.getAndroidString(R.string.since) + " " + getElement(bits, 160);
		}

		sb.append(createRow(getElement(bits, 19) + getElement(bits, 61) +
                " " + getElement(bits, 32) + " " + convert(getElement(bits, 33)),
				since + " " + rain + getElement(bits, 62)));

		return sb.toString();
	}

	private String generateYesterdaysSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t\t<div class='statsHeader'>\n\t\t\t");
		sb.append(weeWXApp.getAndroidString(header));
		sb.append("\n\t\t</div>\n\n");

		sb.append(createRow("flaticon-temperature", "flaticon-temperature",
				getElement(bits, 67) + getElement(bits, 60),
				convert(getElement(bits, 68)),
				convert(getElement(bits, 66)),
				getElement(bits, 65) + getElement(bits, 60)));

		sb.append(createRow("wi wi-raindrop", "wi wi-raindrop",
				getElement(bits, 78) + getElement(bits, 60),
				convert(getElement(bits, 79)),
				convert(getElement(bits, 77)),
				getElement(bits, 76) + getElement(bits, 64)));

		sb.append(createRow("wi wi-humidity", "wi wi-humidity",
				getElement(bits, 82) + getElement(bits, 64),
				convert(getElement(bits, 83)),
				convert(getElement(bits, 81)),
				getElement(bits, 80) + getElement(bits, 64)));

		sb.append(createRow("wi wi-barometer", "wi wi-barometer",
				getElement(bits, 84) + getElement(bits, 63),
				convert(getElement(bits, 85)),
				convert(getElement(bits, 87)),
				getElement(bits, 86) + getElement(bits, 63)));

		if(weeWXAppCommon.GetBoolPref("showIndoor", weeWXApp.showIndoor_default))
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
				before = weeWXApp.getAndroidString(R.string.before) + " " + getElement(bits, 160);
		}

		sb.append(createRow(getElement(bits, 69) + getElement(bits, 61) + " " +
		                    getElement(bits, 70) + " " + convert(getElement(bits, 71)),
							before + " " + rain + getElement(bits, 62)));

		return sb.toString();
	}

	private String generateThisMonthsSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t\t<div class='statsHeader'>\n\t\t\t");
		sb.append(weeWXApp.getAndroidString(header));
		sb.append("\n\t\t</div>\n\n");

		sb.append(createRow("flaticon-temperature", "flaticon-temperature",
				getElement(bits, 90) + getElement(bits, 60),
				getTimeMonth(getElement(bits, 91)),
				getTimeMonth(getElement(bits, 89)),
				getElement(bits, 88) + getElement(bits, 60)));

		sb.append(createRow("wi wi-raindrop", "wi wi-raindrop",
				getElement(bits, 101) + getElement(bits, 60),
				getTimeMonth(getElement(bits, 102)),
				getTimeMonth(getElement(bits, 100)),
				getElement(bits, 99) + getElement(bits, 64)));

		sb.append(createRow("wi wi-humidity", "wi wi-humidity",
				getElement(bits, 105) + getElement(bits, 64),
				getTimeMonth(getElement(bits, 106)),
				getTimeMonth(getElement(bits, 104)),
				getElement(bits, 103) + getElement(bits, 64)));

		sb.append(createRow("wi wi-barometer", "wi wi-barometer",
				getElement(bits, 107) + getElement(bits, 63),
				getTimeMonth(getElement(bits, 108)),
				getTimeMonth(getElement(bits, 110)),
				getElement(bits, 109) + getElement(bits, 63)));

		if(weeWXAppCommon.GetBoolPref("showIndoor", weeWXApp.showIndoor_default))
		{
			if(bits.length > 182 && !bits[182].isBlank() && !bits[180].isBlank())
				sb.append(showIndoor(60, bits, 181, 182, 179, 180, 1, null));

			if(bits.length > 186 && !bits[186].isBlank() && !bits[184].isBlank())
				sb.append(showIndoor(64, bits, 185, 186, 183, 184, 1, null));
		}

		sb.append(createSolarUV(bits, 213, 214, 215, 216, 1, null));

		sb.append(createRow2(getElement(bits, 92) + getElement(bits, 61) + " " +
		                     getElement(bits, 93) + " " + getTimeMonth(getElement(bits, 94)),
							 getElement(bits, 22) + getElement(bits, 62)));

		return sb.toString();
	}

	private String generateThisYearsSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t\t<div class='statsHeader'>\n\t\t\t");
		sb.append(weeWXApp.getAndroidString(header));
		sb.append("\n\t\t</div>\n\n");

		sb.append(createRow("flaticon-temperature", "flaticon-temperature",
				getElement(bits, 113) + getElement(bits, 60),
				getTimeYear(getElement(bits, 114)),
				getTimeYear(getElement(bits, 112)),
				getElement(bits, 111) + getElement(bits, 60)));

		sb.append(createRow("wi wi-raindrop", "wi wi-raindrop",
				getElement(bits, 124) + getElement(bits, 60),
				getTimeYear(getElement(bits, 125)),
				getTimeYear(getElement(bits, 123)),
				getElement(bits, 122) + getElement(bits, 60)));

		sb.append(createRow("wi wi-humidity", "wi wi-humidity",
				getElement(bits, 128) + getElement(bits, 64),
				getTimeYear(getElement(bits, 129)),
				getTimeYear(getElement(bits, 127)),
				getElement(bits, 126) + getElement(bits, 64)));

		sb.append(createRow("wi wi-barometer", "wi wi-barometer",
				getElement(bits, 130) + getElement(bits, 63),
				getTimeYear(getElement(bits, 131)),
				getTimeYear(getElement(bits, 133)),
				getElement(bits, 132) + getElement(bits, 63)));

		if(weeWXAppCommon.GetBoolPref("showIndoor", weeWXApp.showIndoor_default))
		{
			if(bits.length > 190 && !bits[190].isBlank() && !bits[188].isBlank())
				sb.append(showIndoor(60, bits, 189, 190, 187, 188, 2, null));

			if(bits.length > 194 && !bits[194].isBlank() && !bits[192].isBlank())
				sb.append(showIndoor(64, bits, 193, 194, 191, 192, 2, null));
		}

		sb.append(createSolarUV(bits, 217, 218, 219, 220, 2, null));

		sb.append(createRow2(getElement(bits, 115) + getElement(bits, 61) + " " +
		                     getElement(bits, 116) + " " + getTimeYear(getElement(bits, 117)),
							 getElement(bits, 23) + getElement(bits, 62)));

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

		sb.append(createRow("flaticon-temperature", "flaticon-temperature",
				bits[start + 2] + getElement(bits, 60),
				getTimeSection(which, bits[start + 3]),
				getTimeSection(which, bits[start + 1]),
				bits[start] + getElement(bits, 60)));

		sb.append(createRow("wi wi-raindrop", "wi wi-raindrop",
				bits[start + 13] + getElement(bits, 60),
				getTimeSection(which, bits[start + 14]),
				getTimeSection(which, bits[start + 12]),
				bits[start + 11] + getElement(bits, 60)));

		sb.append(createRow("wi wi-humidity", "wi wi-humidity",
				bits[start + 17] + getElement(bits, 64),
				getTimeSection(which, bits[start + 18]),
				getTimeSection(which, bits[start + 16]),
				bits[start + 15] + getElement(bits, 64)));

		sb.append(createRow("wi wi-barometer", "wi wi-barometer",
				bits[start + 7] + getElement(bits, 63),
				getTimeSection(which, bits[start + 8]),
				getTimeSection(which, bits[start + 10]),
				bits[start + 9] + getElement(bits, 63)));

		if(weeWXAppCommon.GetBoolPref("showIndoor", weeWXApp.showIndoor_default))
		{
			if(bits.length > start + 23 && !bits[start + 23].isBlank() && !bits[start + 21].isBlank())
				sb.append(showIndoor(60, bits,
						start + 22, start + 23, start + 20, start + 21, 3, which));

			if(bits.length > start + 27 && !bits[start + 27].isBlank() && !bits[start + 25].isBlank())
				sb.append(showIndoor(64, bits,
						start + 26, start + 27, start + 24, start + 25, 3, which));
		}

		sb.append(createSolarUV(bits, start + 28, start + 29, start + 30, start + 31, 3, which));

		sb.append(createRow2(bits[start + 4] + getElement(bits, 61) + " " +
		                     bits[start + 5] + " " + getTimeSection(which, bits[start + 6]),
							 bits[start + 19] + getElement(bits, 62)));

		return sb.toString();
	}

	private String generateAllTimeSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t\t<div class='statsHeader'>\n\t\t\t");
		sb.append(weeWXApp.getAndroidString(header));
		sb.append("\n\t\t</div>\n\n");

		sb.append(createRow("flaticon-temperature", "flaticon-temperature",
				getElement(bits, 136) + getElement(bits, 60),
				getAllTime(getElement(bits, 137)),
				getAllTime(getElement(bits, 135)),
				getElement(bits, 134) + getElement(bits, 60)));

		sb.append(createRow("wi wi-raindrop", "wi wi-raindrop",
				getElement(bits, 147) + getElement(bits, 60),
				getAllTime(getElement(bits, 148)),
				getAllTime(getElement(bits, 146)),
				getElement(bits, 145) + getElement(bits, 60)));

		sb.append(createRow("wi wi-humidity", "wi wi-humidity",
				getElement(bits, 151) + getElement(bits, 64),
				getAllTime(getElement(bits, 152)),
				getAllTime(getElement(bits, 150)),
				getElement(bits, 149) + getElement(bits, 64)));

		sb.append(createRow("wi wi-barometer", "wi wi-barometer",
				getElement(bits, 153) + getElement(bits, 63),
				getAllTime(getElement(bits, 154)),
				getAllTime(getElement(bits, 156)),
				getElement(bits, 155) + getElement(bits, 63)));

		if(weeWXAppCommon.GetBoolPref("showIndoor", weeWXApp.showIndoor_default))
		{
			if(bits.length > 198 && !bits[198].isBlank() && !bits[196].isBlank())
				sb.append(showIndoor(60, bits,
						197, 198, 195, 196, 4, null));

			if(bits.length > 202 && !bits[202].isBlank() && !bits[200].isBlank())
				sb.append(showIndoor(64, bits,
						201, 202, 199, 200, 4, null));
		}

		sb.append(createSolarUV(bits, 221, 222, 223, 224, 4, null));

		sb.append(createRow2(getElement(bits, 138) + getElement(bits, 61) + " " +
		                     getElement(bits, 139) + " " + getAllTime(getElement(bits, 140)),
							 getElement(bits, 157) + getElement(bits, 62)));

		return sb.toString();
	}

	private void updateFields()
	{
		weeWXAppCommon.LogMessage("Stats.java updateFields()");
		String[] ret = weeWXAppCommon.getWeather(false, false);
		String lastDownload = ret[1];

		if(ret[0].equals("error"))
		{
			if(lastDownload != null && !lastDownload.isBlank())
				wv.post(() -> wv.loadDataWithBaseURL("file:///android_res/",
						lastDownload, "text/html", "utf-8", null));
			else
				wv.post(() -> wv.loadDataWithBaseURL("file:///android_res/",
						weeWXApp.getAndroidString(R.string.unknown_error_occurred),
						"text/html", "utf-8", null));

			stopRefreshing();
			return;
		}

		String[] bits = lastDownload.split("\\|");

		if(bits.length < 65)
		{
			wv.post(() -> wv.loadDataWithBaseURL("file:///android_res/",
					weeWXApp.getAndroidString(R.string.unknown_error_occurred),
					"text/html", "utf-8", null));
			stopRefreshing();
			return;
		}

		// Today Stats
		checkFields(rootView.findViewById(R.id.textView),
				getElement(bits, 56));
		checkFields(rootView.findViewById(R.id.textView2),
				getElement(bits, 54) + " " + getElement(bits, 55));

		final StringBuilder sb = new StringBuilder();

		sb.append(weeWXApp.current_html_headers);
		sb.append(weeWXApp.script_header);
		sb.append(weeWXApp.html_header_rest);
		sb.append(weeWXApp.inline_arrow);

		sb.append("\n<div class='statsLayout'>\n\n");

		// Show today's stats
		sb.append("\t<div class='statsSection'>\n");
		sb.append(generateTodaysSection(R.string.todayStats, bits));
		sb.append("\t\t<hr />\n\n");
		sb.append("\t</div>\n\n");

		if(bits.length > 87 && !bits[87].isBlank())
		{
			// Do stats for yesterday...
			sb.append("\t<div class='statsSection'>\n");
			sb.append(generateYesterdaysSection(R.string.yesterdayStats, bits));
			sb.append("\t<hr />\n\n");
			sb.append("\t\t</div>\n\n");
		}

		if(bits.length > 110 && !bits[110].isBlank())
		{
			// Do stats for this month
			sb.append("\t<div class='statsSection'>\n");
			sb.append(generateThisMonthsSection(R.string.this_months_stats, bits));
			sb.append("\t\t<hr />\n\n");
			sb.append("\t</div>\n\n");
		}

		if(bits.length > 268 && !bits[267].isBlank())
		{
			// Do last month's stats
			sb.append("\t<div class='statsSection'>\n");
			sb.append(generateLastSection(bits, 258, "Month"));
			sb.append("\t\t<hr />\n\n");
			sb.append("\t</div>\n\n");
		}

		if(bits.length > 133 && !bits[133].isBlank())
		{
			// Do stats for this year
			sb.append("\t<div class='statsSection'>\n");
			sb.append(generateThisYearsSection(R.string.this_year_stats, bits));
			sb.append("\t\t<hr />\n\n");
			sb.append("\t</div>\n\n");
		}

		if(bits.length > 236 && !bits[236].isBlank())
		{
			// Do last year's stats
			sb.append("\t<div class='statsSection'>\n");
			sb.append(generateLastSection(bits, 226, "Year"));
			sb.append("\t\t<hr />\n\n");
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

		wv.post(() -> wv.loadDataWithBaseURL("file:///android_res/",
				sb.toString(), "text/html", "utf-8", null));

		stopRefreshing();
	}
}