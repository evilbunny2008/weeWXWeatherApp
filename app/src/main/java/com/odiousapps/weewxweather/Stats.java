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
import static com.odiousapps.weewxweather.weeWXAppCommon.cssToSVG;
import static com.odiousapps.weewxweather.weeWXAppCommon.fiToSVG;
import static com.odiousapps.weewxweather.weeWXAppCommon.getDateFromString;
import static com.odiousapps.weewxweather.weeWXAppCommon.getDaySuffix;
import static com.odiousapps.weewxweather.weeWXAppCommon.getElement;
import static com.odiousapps.weewxweather.weeWXAppCommon.getInt;
import static com.odiousapps.weewxweather.weeWXAppCommon.str2Float;

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
			wv = new SafeWebView(weeWXApp.getInstance());

		FrameLayout fl = rootView.findViewById(R.id.webViewFrameLayout);
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

				    if(value1 == null || value1.isBlank() || value1.equals("null") || value1.equals("-1"))
				    {
						handler.postDelayed(this, 150);
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
			return getDaySuffix(getInt(getDateFromString(str).substring(0, 2)));

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
		str = getDateFromString(str);
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
			icon = cssToSVG(class1);

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
			icon = cssToSVG(class2);

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
		       fiToSVG("flaticon-windy") +
		       "</div>\n\t\t\t" + weeWXApp.currentSpacer +
		       "\t\t\t<div class='statsDataCell " + class1 + "'>" + str1 + "</div>\n";
	}

	private String createRowRight(String class2, String str2)
	{
		return "\t\t\t<div class='statsDataCell " + class2 + "'>" +
		       str2 +
		       "</div>\n\t\t\t" + weeWXApp.currentSpacer +
		       "\t\t\t<div class='statsDataCell right'>" +
		       cssToSVG("wi-umbrella") +
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

		String UV = getElement(uv, bits);
		if(UV.contains("N/A"))
			UV = "";

		if(getElement(uvWhen, bits).isBlank())
			UV = "";

		String SOLAR = getElement(solar, bits);
		if(SOLAR.contains("N/A"))
			SOLAR = "";

		if(getElement(solarWhen, bits).strip().isBlank())
			SOLAR = "";


		if(UV.isBlank() && SOLAR.isBlank())
		{
			LogMessage("No solar and UV data, skipping...");
			return "";
		}

		String className = fiToSVG("flaticon-women-sunglasses");
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
		if(bits.length < maxWhen || getElement(maxWhen, bits).isBlank() ||
		   getElement(minWhen, bits).isBlank())
			return "";

		String maxDateTimeStr = getDateTimeStr(bits, maxWhen, timeMode, which);
		String minDateTimeStr = getDateTimeStr(bits, minWhen, timeMode, which);
		if((maxDateTimeStr == null || maxDateTimeStr.isBlank()) &&
		   (minDateTimeStr == null || minDateTimeStr.isBlank()))
			return "";

		return createRow(fiToSVG("flaticon-home-page"),
				fiToSVG("flaticon-home-page"),
				getElement(max, bits) + getElement(appendId, bits),
				maxDateTimeStr, minDateTimeStr,
				getElement(min, bits) + getElement(appendId, bits));
	}

	private String getDateTimeStr(String[] bits, int when, int timeMode, String which)
	{
		String dateTimeStr = "";

		if(timeMode == 0)
			dateTimeStr = convert(getElement(when, bits));
		else if(timeMode == 1)
			dateTimeStr = getTimeMonth(getElement(when, bits));
		else if(timeMode == 2)
			dateTimeStr = getTimeYear(getElement(when, bits));
		else if(timeMode == 3)
			dateTimeStr = getTimeSection(which, getElement(when, bits));
		else if(timeMode == 4)
			dateTimeStr = getAllTime(getElement(when, bits));

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

		String unitSym = getElement(60, bits);

		sb.append(createRow(fiToSVG("flaticon-temperature"),
				fiToSVG("flaticon-temperature"),
				getElement(3, bits) + unitSym,
				convert(getElement(4, bits)),
				convert(getElement(2, bits)),
				getElement(1, bits) + unitSym));

		sb.append(createRow(cssToSVG("wi-raindrop"),
				cssToSVG("wi-raindrop"),
				getElement(15, bits) + unitSym,
				convert(getElement(16, bits)),
				convert(getElement(14, bits)),
				getElement(13, bits) + unitSym));

		sb.append(createRow(cssToSVG("wi-humidity"),
				cssToSVG("wi-humidity"),
				getElement(9, bits) + getElement(64, bits),
				convert(getElement(10, bits)),
				convert(getElement(8, bits)),
				getElement(6, bits) + getElement(64, bits)));

		sb.append(createRow(cssToSVG("wi-barometer"),
				cssToSVG("wi-barometer"),
				getElement(39, bits) + getElement(63, bits),
				convert(getElement(40, bits)),
				convert(getElement(42, bits)),
				getElement(41, bits) + getElement(63, bits)));

		if((boolean)KeyValue.readVar("showIndoor", weeWXApp.showIndoor_default))
		{
			if(!getElement(163, bits).isBlank() && !getElement(165, bits).isBlank())
				sb.append(showIndoor(60, bits, 164, 165, 162, 163, 0, null));

			if(!getElement(168, bits).isBlank() && !getElement(170, bits).isBlank())
				sb.append(showIndoor(64, bits, 169, 170, 167, 168, 0, null));
		}

		sb.append(createSolarUV(bits, 205, 206, 207, 208, 0, null));

		String rain = getElement(20, bits);
		String since = weeWXApp.getAndroidString(R.string.since) + " mn";

		String str = getElement(158, bits);
		if(!str.isBlank())
		{
			rain = str;

			str = getElement(160, bits);
			if(!str.isBlank())
				since = weeWXApp.getAndroidString(R.string.since) + " " + str;
		}

		sb.append(createRow(getElement(19, bits) + getElement(61, bits) +
                " " + getElement(32, bits) + " " + convert(getElement(33, bits)),
				since + " " + rain + getElement(62, bits)));

		return sb.toString();
	}

	private String generateYesterdaysSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t\t<div class='statsHeader'>\n\t\t\t");
		sb.append(weeWXApp.getAndroidString(header));
		sb.append("\n\t\t</div>\n\n");

		String unitSym = getElement(60, bits);

		sb.append(createRow(fiToSVG("flaticon-temperature"),
				fiToSVG("flaticon-temperature"),
				getElement(67, bits) + unitSym,
				convert(getElement(68, bits)),
				convert(getElement(66, bits)),
				getElement(65, bits) + unitSym));

		sb.append(createRow(cssToSVG("wi-raindrop"), cssToSVG("wi-raindrop"),
				getElement(78, bits) + unitSym,
				convert(getElement(79, bits)),
				convert(getElement(77, bits)),
				getElement(76, bits) + getElement(64, bits)));

		sb.append(createRow(cssToSVG("wi-humidity"), cssToSVG("wi-humidity"),
				getElement(82, bits) + getElement(64, bits),
				convert(getElement(83, bits)),
				convert(getElement(81, bits)),
				getElement(80, bits) + getElement(64, bits)));

		sb.append(createRow(cssToSVG("wi-barometer"), cssToSVG("wi-barometer"),
				getElement(84, bits) + getElement(63, bits),
				convert(getElement(85, bits)),
				convert(getElement(87, bits)),
				getElement(86, bits) + getElement(63, bits)));

		if((boolean)KeyValue.readVar("showIndoor", weeWXApp.showIndoor_default))
		{
			if(!getElement(172, bits).isBlank() && !getElement(174, bits).isBlank())
				sb.append(showIndoor(60, bits, 173, 174, 171, 172, 0, null));

			if(!getElement(176, bits).isBlank() && !getElement(178, bits).isBlank())
				sb.append(showIndoor(64, bits, 177, 178, 175, 176, 0, null));
		}

		sb.append(createSolarUV(bits, 209, 210, 211, 212, 0, null));

		String rain = getElement(21, bits);
		String before = weeWXApp.getAndroidString(R.string.before) + " mn";

		String str1 = getElement(159, bits);
		if(!str1.isBlank())
		{
			rain = str1;

			String str2 = getElement(160, bits);
			if(!str2.isBlank())
				before = weeWXApp.getAndroidString(R.string.before) + " " + str2;
		}

		sb.append(createRow(getElement(69, bits) + getElement(61, bits) + " " +
		                    getElement(70, bits) + " " + convert(getElement(71, bits)),
							before + " " + rain + getElement(62, bits)));

		return sb.toString();
	}

	private String generateThisMonthsSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t\t<div class='statsHeader'>\n\t\t\t");
		sb.append(weeWXApp.getAndroidString(header));
		sb.append("\n\t\t</div>\n\n");

		String unitSym = getElement(60, bits);

		sb.append(createRow(fiToSVG("flaticon-temperature"),
				fiToSVG("flaticon-temperature"),
				getElement(90, bits) + unitSym,
				getTimeMonth(getElement(91, bits)),
				getTimeMonth(getElement(89, bits)),
				getElement(88, bits) + unitSym));

		sb.append(createRow(cssToSVG("wi-raindrop"), cssToSVG("wi-raindrop"),
				getElement(101, bits) + unitSym,
				getTimeMonth(getElement(102, bits)),
				getTimeMonth(getElement(100, bits)),
				getElement(99, bits) + getElement(64, bits)));

		sb.append(createRow(cssToSVG("wi-humidity"), cssToSVG("wi-humidity"),
				getElement(105, bits) + getElement(64, bits),
				getTimeMonth(getElement(106, bits)),
				getTimeMonth(getElement(104, bits)),
				getElement(103, bits) + getElement(64, bits)));

		sb.append(createRow(cssToSVG("wi-barometer"), cssToSVG("wi-barometer"),
				getElement(107, bits) + getElement(63, bits),
				getTimeMonth(getElement(108, bits)),
				getTimeMonth(getElement(110, bits)),
				getElement(109, bits) + getElement(63, bits)));

		if((boolean)KeyValue.readVar("showIndoor", weeWXApp.showIndoor_default))
		{
			if(!getElement(180, bits).isBlank() && !getElement(182, bits).isBlank())
				sb.append(showIndoor(60, bits, 181, 182, 179, 180, 1, null));

			if(!getElement(184, bits).isBlank() && !getElement(186, bits).isBlank())
				sb.append(showIndoor(64, bits, 185, 186, 183, 184, 1, null));
		}

		sb.append(createSolarUV(bits, 213, 214, 215, 216, 1, null));

		sb.append(createRow2(getElement(92, bits) + getElement(61, bits) + " " +
		                     getElement(93, bits) + " " + getTimeMonth(getElement(94, bits)),
							 getElement(22, bits) + getElement(62, bits)));

		return sb.toString();
	}

	private String generateThisYearsSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t\t<div class='statsHeader'>\n\t\t\t");
		sb.append(weeWXApp.getAndroidString(header));
		sb.append("\n\t\t</div>\n\n");

		String unitSym = getElement(60, bits);

		sb.append(createRow(fiToSVG("flaticon-temperature"),
				fiToSVG("flaticon-temperature"),
				getElement(113, bits) + unitSym,
				getTimeYear(getElement(114, bits)),
				getTimeYear(getElement(112, bits)),
				getElement(111, bits) + unitSym));

		sb.append(createRow(cssToSVG("wi-raindrop"), cssToSVG("wi-raindrop"),
				getElement(124, bits) + unitSym,
				getTimeYear(getElement(125, bits)),
				getTimeYear(getElement(123, bits)),
				getElement(122, bits) + unitSym));

		sb.append(createRow(cssToSVG("wi-humidity"), cssToSVG("wi-humidity"),
				getElement(128, bits) + getElement(64, bits),
				getTimeYear(getElement(129, bits)),
				getTimeYear(getElement(127, bits)),
				getElement(126, bits) + getElement(64, bits)));

		sb.append(createRow(cssToSVG("wi-barometer"), cssToSVG("wi-barometer"),
				getElement(130, bits) + getElement(63, bits),
				getTimeYear(getElement(131, bits)),
				getTimeYear(getElement(133, bits)),
				getElement(132, bits) + getElement(63, bits)));

		if((boolean)KeyValue.readVar("showIndoor", weeWXApp.showIndoor_default))
		{
			if(!getElement(188, bits).isBlank() && !getElement(190, bits).isBlank())
				sb.append(showIndoor(60, bits, 189, 190, 187, 188, 2, null));

			if(!getElement(192, bits).isBlank() && !getElement(194, bits).isBlank())
				sb.append(showIndoor(64, bits, 193, 194, 191, 192, 2, null));
		}

		sb.append(createSolarUV(bits, 217, 218, 219, 220, 2, null));

		sb.append(createRow2(getElement(115, bits) + getElement(61, bits) + " " +
		                     getElement(116, bits) + " " + getTimeYear(getElement(117, bits)),
							 getElement(23, bits) + getElement(62, bits)));

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

		String unitSym = getElement(60, bits);

		sb.append(createRow(fiToSVG("flaticon-temperature"),
				fiToSVG("flaticon-temperature"),
				getElement(start + 2, bits) + unitSym,
				getTimeSection(which, getElement(start + 3, bits)),
				getTimeSection(which, getElement(start + 1, bits)),
				getElement(start, bits) + unitSym));

		sb.append(createRow(cssToSVG("wi-raindrop"), cssToSVG("wi-raindrop"),
				getElement(start + 13, bits) + unitSym,
				getTimeSection(which, getElement(start + 14, bits)),
				getTimeSection(which, getElement(start + 12, bits)),
				getElement(start + 11, bits) + unitSym));

		sb.append(createRow(cssToSVG("wi-humidity"), cssToSVG("wi-humidity"),
				getElement(start + 17, bits) + getElement(64, bits),
				getTimeSection(which, getElement(start + 18, bits)),
				getTimeSection(which, getElement(start + 16, bits)),
				getElement(start + 15, bits) + getElement(64, bits)));

		sb.append(createRow(cssToSVG("wi-barometer"), cssToSVG("wi-barometer"),
				getElement(start + 7, bits) + getElement(63, bits),
				getTimeSection(which, getElement(start + 8, bits)),
				getTimeSection(which, getElement(start + 10, bits)),
				getElement(start + 9, bits) + getElement(63, bits)));

		if((boolean)KeyValue.readVar("showIndoor", weeWXApp.showIndoor_default))
		{
			if(!getElement(start + 21, bits).isBlank() && !getElement(start + 23, bits).isBlank())
				sb.append(showIndoor(60, bits,
						start + 22, start + 23, start + 20, start + 21, 3, which));

			if(!getElement(start + 25, bits).isBlank() && !getElement(start + 27, bits).isBlank())
				sb.append(showIndoor(64, bits,
						start + 26, start + 27, start + 24, start + 25, 3, which));
		}

		sb.append(createSolarUV(bits, start + 28, start + 29, start + 30, start + 31, 3, which));

		sb.append(createRow2(getElement(start + 4, bits) + getElement(61, bits) + " " +
		                     getElement(start + 5, bits) + " " + getTimeSection(which, getElement(start + 6, bits)),
							 getElement(start + 19, bits) + getElement(62, bits)));

		return sb.toString();
	}

	private String generateAllTimeSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t\t<div class='statsHeader'>\n\t\t\t");
		sb.append(weeWXApp.getAndroidString(header));
		sb.append("\n\t\t</div>\n\n");

		String unitSym = getElement(60, bits);

		sb.append(createRow(fiToSVG("flaticon-temperature"),
				fiToSVG("flaticon-temperature"),
				getElement(136, bits) + unitSym,
				getAllTime(getElement(137, bits)),
				getAllTime(getElement(135, bits)),
				getElement(134, bits) + unitSym));

		sb.append(createRow(cssToSVG("wi-raindrop"),
				cssToSVG("wi-raindrop"),
				getElement(147, bits) + unitSym,
				getAllTime(getElement(148, bits)),
				getAllTime(getElement(146, bits)),
				getElement(145, bits) + unitSym));

		sb.append(createRow(cssToSVG("wi-humidity"),
				cssToSVG("wi-humidity"),
				getElement(151, bits) + getElement(64, bits),
				getAllTime(getElement(152, bits)),
				getAllTime(getElement(150, bits)),
				getElement(149, bits) + getElement(64, bits)));

		sb.append(createRow(cssToSVG("wi-barometer"),
				cssToSVG("wi-barometer"),
				getElement(153, bits) + getElement(63, bits),
				getAllTime(getElement(154, bits)),
				getAllTime(getElement(156, bits)),
				getElement(155, bits) + getElement(63, bits)));

		if((boolean)KeyValue.readVar("showIndoor", weeWXApp.showIndoor_default))
		{
			if(!getElement(196, bits).isBlank() && !getElement(198, bits).isBlank())
				sb.append(showIndoor(60, bits,
						197, 198, 195, 196, 4, null));

			if(!getElement(200, bits).isBlank() && !getElement(202, bits).isBlank())
				sb.append(showIndoor(64, bits,
						201, 202, 199, 200, 4, null));
		}

		sb.append(createSolarUV(bits, 221, 222, 223, 224, 4, null));

		sb.append(createRow2(getElement(138, bits) + getElement(61, bits) + " " +
		                     getElement(139, bits) + " " + getAllTime(getElement(140, bits)),
							 getElement(157, bits) + getElement(62, bits)));

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

			//setZoom(currZoom, true);

			stopRefreshing();
			return;
		}

		String[] bits = ((String)KeyValue.readVar("LastDownload", "")).split("\\|");

		if(bits.length < 100)
		{
			wv.post(() -> wv.loadDataWithBaseURL(null,
					weeWXApp.getAndroidString(R.string.unknown_error_occurred),
					"text/html", "utf-8", null));

			//setZoom(currZoom, true);

			stopRefreshing();
			return;
		}

		// Today Stats
		checkFields(rootView.findViewById(R.id.textView),
				getElement(56, bits));
		checkFields(rootView.findViewById(R.id.textView2),
				getElement(54, bits) + " " + getElement(55, bits));

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
		sb.append(generateTodaysSection(R.string.todayStats, bits));
		sb.append("\t\t<hr />\n");
		sb.append("\t</div>\n\n");

		// Do stats for yesterday...
		sb.append("\t<div class='statsSection'>\n");
		sb.append(generateYesterdaysSection(R.string.yesterdayStats, bits));
		sb.append("\t\t<hr />\n");
		sb.append("\t</div>\n\n");

		String str = getElement(110, bits);
		if(!str.isBlank())
		{
			// Do stats for this month
			sb.append("\t<div class='statsSection'>\n");
			sb.append(generateThisMonthsSection(R.string.this_months_stats, bits));
			sb.append("\t\t<hr />\n");
			sb.append("\t</div>\n\n");
		}

		str = getElement(268, bits);
		if(!str.isBlank())
		{
			// Do last month's stats
			sb.append("\t<div class='statsSection'>\n");
			sb.append(generateLastSection(bits, 258, "Month"));
			sb.append("\t\t<hr />\n");
			sb.append("\t</div>\n\n");
		}

		str = getElement(133, bits);
		if(!str.isBlank())
		{
			// Do stats for this year
			sb.append("\t<div class='statsSection'>\n");
			sb.append(generateThisYearsSection(R.string.this_year_stats, bits));
			sb.append("\t\t<hr />\n");
			sb.append("\t</div>\n\n");
		}

		str = getElement(236, bits);
		if(!str.isBlank())
		{
			// Do last year's stats
			sb.append("\t<div class='statsSection'>\n");
			sb.append(generateLastSection(bits, 226, "Year"));
			sb.append("\t\t<hr />\n");
			sb.append("\t</div>\n\n");
		}

		str = getElement(156, bits);
		if(!str.isBlank())
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

		if(weeWXAppCommon.debug_html)
			CustomDebug.writeOutput(requireContext(), "stats", sb.toString(), isVisible(), requireActivity());

		wv.post(() -> wv.loadDataWithBaseURL("file:///android_asset/",
				sb.toString(), "text/html", "utf-8", null));

		stopRefreshing();
	}
}