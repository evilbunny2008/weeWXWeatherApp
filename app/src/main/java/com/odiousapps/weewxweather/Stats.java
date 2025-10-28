package com.odiousapps.weewxweather;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.evilbunny2008.colourpicker.CPSlider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class Stats extends Fragment
{
	private boolean isVisible = false;
	private boolean isRunning = false;
	private CPSlider mySlider;
	private View rootView;
	private WebView wv;
	private SwipeRefreshLayout swipeLayout;

	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		rootView = inflater.inflate(R.layout.fragment_stats, container, false);

		LinearLayout ll = rootView.findViewById(R.id.stats_ll);
		ll.setBackgroundColor(KeyValue.bgColour);

		mySlider = rootView.findViewById(R.id.pageZoom);
		mySlider.setBackgroundColor(KeyValue.bgColour);
		wv = rootView.findViewById(R.id.webView1);
		Common.setWebview(wv);

		swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
		swipeLayout.setOnRefreshListener(() ->
		{
			swipeLayout.setRefreshing(true);
			Common.LogMessage("onRefresh();");
			forceRefresh();
		});

		wv.getViewTreeObserver().addOnScrollChangedListener(() ->
				swipeLayout.setEnabled(wv.getScrollY() == 0));

		int default_zoom = sanitiseZoom(Common.GetIntPref("mySlider", 100));
		mySlider.setValue(default_zoom);
		mySlider.addOnChangeListener((slider, value, fromUser) ->
		{
			Common.LogMessage("Slider zoom =" + value + "%");

			if(fromUser)
			{
				Common.SetIntPref("mySlider", (int)value);
				setZoom((int)value);
			}
		});

		return rootView;
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
		final int finalZoom = sanitiseZoom(zoom);

		Common.LogMessage("new zoom value = " + finalZoom + "%");

		mySlider.post(() -> mySlider.setValue(finalZoom));
		wv.post(() -> wv.getSettings().setTextZoom(finalZoom));
	}

	public void onResume()
	{
		super.onResume();

		Common.LogMessage("Stats.onResume()");

		if(isVisible)
			return;

		isVisible = true;

		Common.LogMessage("Stats.onResume()-- adding notification manager...", true);
		Common.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);

		if(isRunning)
			return;

		isRunning = true;
		swipeLayout.setRefreshing(true);
		updateFields();
	}

	public void onPause()
	{
		super.onPause();
		Common.NotificationManager.getNotificationLiveData().removeObserver(notificationObserver);
		Common.LogMessage("stats.java -- unregisterReceiver");
	}

	private final Observer<String> notificationObserver = str ->
	{
		Common.LogMessage("notificationObserver == " + str);

		if(str.equals(Common.UPDATE_INTENT) || str.equals(Common.REFRESH_INTENT))
			updateFields();

		if(str.equals(Common.EXIT_INTENT))
			onPause();
	};

	private void forceRefresh()
	{
		Common.getWeather();
	}

	private void checkFields(final TextView tv, final String txt)
	{
		if(!tv.getText().toString().equals(txt))
			tv.post(() -> tv.setText(txt));
	}

	private String convert(String cur)
	{
		cur = cur.trim();
/*
		String[] bits = null;

		if(!cur.contains(" "))
			return cur;

		try
		{
			//cur = "9:34:00 PM";
			bits = cur.trim().split(" ");
		} catch(Exception e) {
			Common.doStackOutput(e);
		}

		if(bits == null || bits.length < 2)
			return cur;

		String[] time = null;
		try
		{
			time = bits[0].trim().split(":");
		} catch(Exception e) {
			Common.doStackOutput(e);
		}

		if(time == null || time.length < 3)
			return cur;

		int hours = (int)Float.parseFloat(time[0]);
		int mins = (int)Float.parseFloat(time[1]);
		//int secs = (int)Float.parseFloat(time[2]);

		boolean pm = bits[1].trim().equalsIgnoreCase("pm");

		Common.LogMessage("pm == '" + bits[1] + "'", true);

		if(!pm && hours == 12)
			hours = 0;
		else if(pm && hours != 12)
			hours = hours + 12;

		//return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, mins, secs);
		//return String.format(Locale.getDefault(), "%d:%02d", hours, mins);
		//String str = String.format("%tR", hours, mins);
*/
		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat("hh:mm", Locale.getDefault());
			Date dt = sdf.parse(cur);
			if(dt != null)
			{
				sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
				String str = sdf.format(dt);
				Common.LogMessage("str == '" + str + "'", true);
				return str;
			}
		} catch(Exception ignored) {}

		return cur;
	}

	private String getTime(String str)
	{
		str = str.trim();

		if(!str.contains(" "))
			return str;

		try
		{
			return str.split(" ", 2)[0];
		} catch(Exception e) {
			Common.doStackOutput(e);
		}

		return str;
	}

	private String getTimeMonth(String str)
	{
		str = getTime(str);
		str = Common.dayOfMonth((int)Float.parseFloat(str.substring(0, 2)));
		return str;
	}

	private String getTimeYear(String str)
	{
		str = getTime(str);
		if(str.startsWith("0"))
			str = str.substring(1);
		str = str.substring(0, str.length() - 2);
		return str;
	}

	private String getAllTime(String str)
	{
		str = getTime(str);
		if(str.startsWith("0"))
			str = str.substring(1);
		return str;
	}

	private String createRow(String class1, String class2, String str1, String str2, String str3, String str4)
	{
		return "\t<div class='dataRow'>\n" +
		       "\t\t<div class='dataCell'><i class='" + class1 + " icon'></i>" + str1 + "</div>\n" +
	           "\t\t<div class='dataCell'>" + str2 + "</div>\n" +
	           "\t\t<div class='datacell'>" + str3 + "</div>\n" +
	           "\t\t<div class='dataCell right'>" + str4 + "<i class='" + class2 + " icon'></i></div>\n" +
	           "\t</div>\n\n";
	}

	private String createRow(String str1, String str2)
	{
		return "\t<div class='dataRow'>\n" +
		       "\t\t<div class='dataCell'><i class='flaticon-windy icon'></i>" + str1 + "</div>\n" +
		       "\t\t<div></div><div></div>\n" +
		       "\t\t<div class='dataCell right'>" + str2 +
		       "<i class='wi wi-umbrella icon'></i></div>\n\t</div>\n\n";
	}

	private String createRow(String str2)
	{
		return "\t<div class='dataRow'>\n" +
		       "\t\t<div></div><div></div><div></div>\n" +
		       "\t\t<div class='dataCell right'>" + str2 + "</div>\n" +
		       "\t</div>\n\n";
	}

	private String generateTodaysSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t<div class='statsHeader'>\n\t\t");
		sb.append(getString(header));
		sb.append("\n\t</div>\n\n");

		sb.append(createRow("flaticon-temperature", "flaticon-temperature",
				bits[3] + bits[60], convert(bits[4]), convert(bits[2]),
				bits[1] + bits[60]));

		sb.append(createRow("wi wi-raindrop", "wi wi-raindrop",
				bits[15] + bits[60], convert(bits[16]), convert(bits[14]),
				bits[13] + bits[60]));

		sb.append(createRow("wi wi-humidity", "wi wi-humidity",
				bits[9] + bits[64], convert(bits[10]), convert(bits[8]),
				bits[6] + bits[64]));

		sb.append(createRow("wi wi-barometer", "wi wi-barometer",
				bits[39] + bits[63], convert(bits[40]), convert(bits[42]),
				bits[41] + bits[63]));

		if(bits.length > 202 && Common.GetBoolPref("showIndoor", false))
		{
			sb.append(createRow("flaticon-home-page", "flaticon-home-page",
					bits[164] + bits[60], convert(bits[165]), convert(bits[163]),
					bits[162] + bits[60]));

			sb.append(createRow("flaticon-home-page", "flaticon-home-page",
					bits[169] + bits[64], convert(bits[170]), convert(bits[168]),
					bits[167] + bits[64]));
		}

		if(bits.length > 205 && !bits[205].isEmpty())
		{
			sb.append(createRow("flaticon-women-sunglasses", "flaticon-women-sunglasses",
					bits[205] + "UVI", convert(bits[206]), convert(bits[208]),
					bits[207] + "W/m²"));
		}

		String rain = bits[20];
		String since = getString(R.string.since) + " mm";

		if(bits.length > 160 && !bits[160].isEmpty())
			rain = bits[158];

		if(bits.length > 160 && !bits[158].isEmpty() && !bits[160].isEmpty())
			since = getString(R.string.since) + " " + bits[160];

		sb.append(createRow(bits[19] + bits[61] + " " + bits[32] + " " + convert(bits[33]),
				rain + bits[62]));

		sb.append(createRow(since));

		return sb.toString();
	}

	private String generateYesterdaysSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t<div class='statsHeader'>\n\t\t");
		sb.append(getString(header));
		sb.append("\n\t</div>\n\n");

		sb.append(createRow("flaticon-temperature", "flaticon-temperature",
				bits[67] + bits[60], convert(bits[68]), convert(bits[66]),
				bits[65] + bits[60]));

		sb.append(createRow("wi wi-raindrop", "wi wi-raindrop",
				bits[78] + bits[60], convert(bits[79]), convert(bits[77]),
				bits[76] + bits[64]));

		sb.append(createRow("wi wi-humidity", "wi wi-humidity",
				bits[82] + bits[64], convert(bits[83]), convert(bits[81]),
				bits[80] + bits[64]));

		sb.append(createRow("wi wi-barometer", "wi wi-barometer",
				bits[84] + bits[63], convert(bits[85]), convert(bits[87]),
				bits[86] + bits[63]));

		if(bits.length > 202 && Common.GetBoolPref("showIndoor", false))
		{
			sb.append(createRow("flaticon-home-page", "flaticon-home-page",
					bits[173] + bits[60], convert(bits[174]), convert(bits[172]),
					bits[171] + bits[60]));

			sb.append(createRow("flaticon-home-page", "flaticon-home-page",
					bits[177] + bits[64], convert(bits[178]), convert(bits[176]),
					bits[175] + bits[64]));
		}

		if(bits.length > 209 && !bits[209].isEmpty())
		{
			sb.append(createRow("flaticon-women-sunglasses", "flaticon-women-sunglasses",
					bits[209] + "UVI", convert(bits[210]), convert(bits[212]),
					bits[211] + "W/m²"));
		}

		String rain = bits[21];
		String before = getString(R.string.before) + " mm";

		if(bits.length > 160 && !bits[159].isEmpty())
			rain = bits[159];

		sb.append(createRow(bits[69] + bits[61] + " " + bits[70] + " " + convert(bits[71]),
				rain + bits[62]));

		if(bits.length > 160 && !bits[159].isEmpty() && !bits[160].isEmpty())
			before = getString(R.string.before) + " " + bits[160];

		sb.append(createRow(before));

		return sb.toString();
	}

	private String generateThisMonthsSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t<div class='statsHeader'>\n\t\t");
		sb.append(getString(header));
		sb.append("\n\t</div>\n\n");

		sb.append(createRow("flaticon-temperature", "flaticon-temperature",
				bits[90] + bits[60], getTimeMonth(bits[91]), getTimeMonth(bits[89]),
				bits[88] + bits[60]));

		sb.append(createRow("wi wi-raindrop", "wi wi-raindrop",
				bits[101] + bits[60], getTimeMonth(bits[102]), getTimeMonth(bits[100]),
				bits[99] + bits[64]));

		sb.append(createRow("wi wi-humidity", "wi wi-humidity",
				bits[105] + bits[64], getTimeMonth(bits[106]), getTimeMonth(bits[104]),
				bits[103] + bits[64]));

		sb.append(createRow("wi wi-barometer", "wi wi-barometer",
				bits[107] + bits[63], getTimeMonth(bits[108]), getTimeMonth(bits[110]),
				bits[109] + bits[63]));

		if(bits.length > 202 && Common.GetBoolPref("showIndoor", false))
		{
			sb.append(createRow("flaticon-home-page", "flaticon-home-page",
					bits[181] + bits[60], getTimeMonth(bits[182]), getTimeMonth(bits[180]),
					bits[179] + bits[60]));

			sb.append(createRow("flaticon-home-page", "flaticon-home-page",
					bits[185] + bits[64], getTimeMonth(bits[186]), getTimeMonth(bits[184]),
					bits[183] + bits[64]));
		}

		if(bits.length > 213 && !bits[213].isEmpty())
		{
			sb.append(createRow("flaticon-women-sunglasses", "flaticon-women-sunglasses",
					bits[213] + "UVI", getTimeMonth(bits[214]), getTimeMonth(bits[216]),
					bits[215] + "W/m²"));
		}

		sb.append(createRow(bits[92] + bits[61] + " " + bits[93] + " " + getTimeMonth(bits[94]),
				bits[22] + bits[62]));

		return sb.toString();
	}

	private String generateThisYearsSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t<div class='statsHeader'>\n\t\t");
		sb.append(getString(header));
		sb.append("\n\t</div>\n\n");

		sb.append(createRow("flaticon-temperature", "flaticon-temperature",
				bits[113] + bits[60], getTimeYear(bits[114]), getTimeYear(bits[112]),
				bits[111] + bits[60]));

		sb.append(createRow("wi wi-raindrop", "wi wi-raindrop",
				bits[124] + bits[60], getTimeYear(bits[125]), getTimeYear(bits[123]),
				bits[122] + bits[60]));

		sb.append(createRow("wi wi-humidity", "wi wi-humidity",
				bits[128] + bits[64], getTimeYear(bits[129]), getTimeYear(bits[127]),
				bits[126] + bits[64]));

		sb.append(createRow("wi wi-barometer", "wi wi-barometer",
				bits[130] + bits[63], getTimeYear(bits[131]), getTimeYear(bits[133]),
				bits[132] + bits[63]));

		if(bits.length > 202 && Common.GetBoolPref("showIndoor", false))
		{
			sb.append(createRow("flaticon-home-page", "flaticon-home-page",
					bits[189] + bits[60], getTimeYear(bits[190]), getTimeYear(bits[188]),
					bits[187] + bits[60]));

			sb.append(createRow("flaticon-home-page", "flaticon-home-page",
					bits[193] + bits[64], getTimeYear(bits[194]), getTimeYear(bits[192]),
					bits[191] + bits[64]));
		}

		if(bits.length > 217 && !bits[217].isEmpty())
		{
			sb.append(createRow("flaticon-women-sunglasses", "flaticon-women-sunglasses",
					bits[217] + "UVI", getTimeYear(bits[218]), getTimeYear(bits[220]),
					bits[219] + "W/m²"));
		}

		sb.append(createRow(bits[115] + bits[61] + " " + bits[116] + " " + getTimeYear(bits[117]),
				bits[23] + bits[62]));

		return sb.toString();
	}

	private String generateAllTimeSection(int header, String[] bits)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\t<div class='statsHeader'>\n\t\t");
		sb.append(getString(header));
		sb.append("\n\t</div>\n\n");

		sb.append(createRow("flaticon-temperature", "flaticon-temperature",
				bits[136] + bits[60], getAllTime(bits[137]), getAllTime(bits[135]),
				bits[134] + bits[60]));

		sb.append(createRow("wi wi-raindrop", "wi wi-raindrop",
				bits[147] + bits[60], getAllTime(bits[148]), getAllTime(bits[146]),
				bits[145] + bits[60]));

		sb.append(createRow("wi wi-humidity", "wi wi-humidity",
				bits[151] + bits[64], getAllTime(bits[152]), getAllTime(bits[150]),
				bits[149] + bits[64]));

		sb.append(createRow("wi wi-barometer", "wi wi-barometer",
				bits[153] + bits[63], getAllTime(bits[154]), getAllTime(bits[156]),
				bits[155] + bits[63]));

		if(bits.length > 202 && Common.GetBoolPref("showIndoor", false))
		{
			sb.append(createRow("flaticon-home-page", "flaticon-home-page",
					bits[197] + bits[60], getAllTime(bits[198]), getAllTime(bits[196]),
					bits[195] + bits[60]));

			sb.append(createRow("flaticon-home-page", "flaticon-home-page",
					bits[201] + bits[64], getAllTime(bits[202]), getAllTime(bits[200]),
					bits[199] + bits[64]));
		}

		if(bits.length > 221 && !bits[221].isEmpty())
		{
			sb.append(createRow("flaticon-women-sunglasses", "flaticon-women-sunglasses",
					bits[221] + "UVI", getAllTime(bits[222]), getAllTime(bits[224]),
					bits[223] + "W/m²"));
		}

		sb.append(createRow(bits[138] + bits[61] + " " + bits[139] + " " + getAllTime(bits[140]),
				bits[157] + bits[62]));

		return sb.toString();
	}

	private void updateFields()
	{
		Thread t = new Thread(() ->
		{
			try
			{
				// Sleep needed to stop frames dropping while loading
				Thread.sleep(500);
			} catch(Exception e) {
				Common.doStackOutput(e);
			}

			String tmpStr = Common.GetStringPref("LastDownload", "");
			if(tmpStr == null)
			{
				stopRefreshing();
				return;
			}

			String[] bits = tmpStr.split("\\|");

			if(bits.length < 65)
			{
				stopRefreshing();
				return;
			}

			// Today Stats
			checkFields(rootView.findViewById(R.id.textView), bits[56]);
			checkFields(rootView.findViewById(R.id.textView2), bits[54] + " " + bits[55]);

			final StringBuilder sb = new StringBuilder();

			sb.append(Common.current_html_headers);

			sb.append("\n<div class='todayCurrent'>\n\n");

			sb.append(generateTodaysSection(R.string.todayStats, bits));

			if(bits.length > 87 && !bits[87].isEmpty())
			{
				sb.append("\t<hr />\n\n");
				sb.append(generateYesterdaysSection(R.string.yesterdayStats, bits));
			}

			if(bits.length > 110 && !bits[110].isEmpty())
			{
				sb.append("\t<hr />\n\n");
				sb.append(generateThisMonthsSection(R.string.this_months_stats, bits));
			}

			if(bits.length > 133 && !bits[133].isEmpty())
			{
				sb.append("\t<hr />\n\n");
				sb.append(generateThisYearsSection(R.string.this_year_stats, bits));
			}

			if(bits.length > 157 && !bits[157].isEmpty())
			{
				sb.append("\t<hr />\n\n");
				sb.append(generateAllTimeSection(R.string.all_time_stats, bits));
			}

			sb.append("</div>\n\n<div style='margin-bottom:5px'></div>\n");

			sb.append(Common.html_footer);

			String html = sb.toString();

			//CustomDebug.writeDebug("stats_weewx.html", html);

			wv.post(() -> wv.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null));
			stopRefreshing();
		});

		t.start();
	}
}