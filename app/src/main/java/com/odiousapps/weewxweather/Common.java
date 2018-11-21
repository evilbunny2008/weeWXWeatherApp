package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import fr.arnaudguyon.xmltojsonlib.XmlToJson;

import static java.lang.Math.round;

class Common
{
	private final static String PREFS_NAME = "WeeWxWeatherPrefs";
	private final static boolean debug_on = true;
	private String appversion = "0.0.0";
	Context context;

	final static String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36";

	static String UPDATE_INTENT = "com.odiousapps.weewxweather.UPDATE_INTENT";
	static String TAB0_INTENT = "com.odiousapps.weewxweather.TAB0_INTENT";
	static String EXIT_INTENT = "com.odiousapps.weewxweather.EXIT_INTENT";
	static String INIGO_INTENT = "com.odiousapps.weewxweather.INIGO_UPDATE";
	static String FAILED_INTENT = "com.odiousapps.weewxweather.FAILED_INTENT";

	private static final long inigo_version = 4000;

	private Thread t = null;

	Common(Context c)
	{
		System.setProperty("http.agent", UA);
		this.context = c;

		try
		{
			PackageManager pm = c.getPackageManager();
			PackageInfo version = pm.getPackageInfo("com.odiousapps.weewxweather", 0);
			appversion = version.versionName;
			LogMessage("appversion=" + appversion);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private long[] getPeriod()
	{
		long[] def = {0, 0};

		int pos = GetIntPref("updateInterval", 1);
		if (pos <= 0)
			return def;

		long period;

		switch (pos)
		{
			case 1:
				period = 5 * 60000;
				break;
			case 2:
				period = 10 * 60000;
				break;
			case 3:
				period = 15 * 60000;
				break;
			case 4:
				period = 30 * 60000;
				break;
			case 5:
				period = 60 * 60000;
				break;
			default:
				return def;
		}

		return new long[]{period, 45000};
	}

	void setAlarm(String from)
	{
		long[] ret = getPeriod();
		long period = ret[0];
		long wait = ret[1];
		if(period <= 0)
			return;

		long start = Math.round((double)System.currentTimeMillis() / (double)period) * period + wait;

		if(start < System.currentTimeMillis())
			start += period;

		LogMessage(from + " - start == " + start);
		LogMessage(from + " - period == " + period);
		LogMessage(from + " - wait == " + wait);

		AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent myIntent = new Intent(context, UpdateCheck.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		if(mgr != null)
			mgr.setExact(AlarmManager.RTC_WAKEUP, start, pi);
	}

	String getAppversion()
	{
		return appversion;
	}

	static void LogMessage(String value)
	{
		LogMessage(value, false);
	}

	static void LogMessage(String value, boolean showAnyway)
	{
		if (debug_on || showAnyway)
			Log.i("weeWX Weather", "message='" + value + "'");
	}

	void SetStringPref(String name, String value)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(name, value);
		editor.apply();

		LogMessage("Updating '" + name + "'='" + value + "'");
	}

	void RemovePref(String name)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove(name);
		editor.apply();

		LogMessage("Removing '" + name + "'");
	}

	@SuppressLint("ApplySharedPref")
	void commit()
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.commit();
	}

	String GetStringPref(String name, String defval)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		String value;

		try
		{
			value = settings.getString(name, defval);
		} catch (ClassCastException cce)
		{
			cce.printStackTrace();
			//SetStringPref(name, defval);
			return defval;
		} catch (Exception e)
		{
			LogMessage("GetStringPref(" + name + ", " + defval + ") Err: " + e.toString());
			e.printStackTrace();
			return defval;
		}

		LogMessage(name + "'='" + value + "'");

		return value;
	}

	@SuppressWarnings({"unused", "SameParameterValue"})
	private void SetLongPref(String name, long value)
	{
		SetStringPref(name, "" + value);
	}

	@SuppressWarnings("unused")
	long GetLongPref(String name)
	{
		return GetLongPref(name, 0);
	}

	@SuppressWarnings("WeakerAccess")
	long GetLongPref(String name, @SuppressWarnings("SameParameterValue") long defval)
	{
		String val = GetStringPref(name, "" + defval);
		if (val == null)
			return defval;
		return Long.parseLong(val);
	}

	void SetIntPref(String name, int value)
	{
		SetStringPref(name, "" + value);
	}

	@SuppressWarnings("unused")
	int GetIntPref(String name)
	{
		return GetIntPref(name, 0);
	}

	int GetIntPref(String name, int defval)
	{
		String val = GetStringPref(name, "" + defval);
		if (val == null)
			return defval;
		return Integer.parseInt(val);
	}

	void SetBoolPref(String name, boolean value)
	{
		String val = "0";
		if (value)
			val = "1";

		SetStringPref(name, val);
	}

	@SuppressWarnings("SameParameterValue")
	boolean GetBoolPref(String name)
	{
		return GetBoolPref(name, false);
	}

	boolean GetBoolPref(String name, boolean defval)
	{
		String value = "0";
		if (defval)
			value = "1";

		String val = GetStringPref(name, value);
		return val.equals("1");
	}

	RemoteViews buildUpdate(Context context)
	{
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
		Bitmap myBitmap = Bitmap.createBitmap(600, 440, Bitmap.Config.ARGB_4444);
		Canvas myCanvas = new Canvas(myBitmap);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setSubpixelText(true);

		int bgColour = GetIntPref("bgColour", 0xFFFFFFFF);
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(bgColour);

		RectF rectF = new RectF(0, 0, myCanvas.getWidth(), myCanvas.getHeight());
		int cornersRadius = 25;
		myCanvas.drawRoundRect(rectF, cornersRadius, cornersRadius, paint);

		int fgColour = GetIntPref("fgColour", 0xFF000000);

		paint.setStyle(Paint.Style.FILL);
		paint.setColor(fgColour);
		paint.setTextAlign(Paint.Align.CENTER);

		String bits[] = GetStringPref("LastDownload", "").split("\\|");
		if (bits.length > 110)
		{
			paint.setTextSize(64);
			myCanvas.drawText(bits[56], myCanvas.getWidth() / 2, 80, paint);
			paint.setTextSize(48);
			myCanvas.drawText(bits[55], myCanvas.getWidth() / 2, 140, paint);
			paint.setTextSize(200);
			myCanvas.drawText(bits[0] + bits[60], myCanvas.getWidth() / 2, 310, paint);

			paint.setTextAlign(Paint.Align.LEFT);
			paint.setTextSize(64);
			myCanvas.drawText(bits[25] + bits[61], 20, 400, paint);

			paint.setTextAlign(Paint.Align.RIGHT);
			paint.setTextSize(64);

			String rain = bits[20];
			if (bits.length > 158 && !bits[158].equals(""))
				rain = bits[158];

			myCanvas.drawText(rain + bits[62], myCanvas.getWidth() - 20, 400, paint);
		} else
		{
			paint.setTextSize(200);
			myCanvas.drawText("Error!", myCanvas.getWidth() / 2, 300, paint);
		}

		views.setImageViewBitmap(R.id.widget, myBitmap);
		return views;
	}

	String[] processWMO(String data)
	{
		return processWMO(data, false);
	}

	String[] processWMO(String data, boolean showHeader)
	{
		if(data == null || data.equals(""))
			return null;

		String desc = "";
		StringBuilder out = new StringBuilder();
		boolean metric = GetBoolPref("metric", true);

		try
		{
			JSONObject jobj = new JSONObject(data);

			desc = jobj.getJSONObject("city").getString("cityName") + ", " + jobj.getJSONObject("city").getJSONObject("member").getString("memName");
			String tmp = jobj.getJSONObject("city").getJSONObject("forecast").getString("issueDate");

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
			long mdate = sdf.parse(tmp).getTime();
			sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
			String date = sdf.format(mdate);

			tmp = "<div style='font-size:12pt;'>" + date + "</div>";
			out.append(tmp);
			tmp = "<table style='width:100%;'>\n";
			out.append(tmp);

			JSONArray jarr = jobj.getJSONObject("city").getJSONObject("forecast").getJSONArray("forecastDay");
			for(int i = 0; i < jarr.length(); i++)
			{
				JSONObject j = jarr.getJSONObject(i);

				date = j.getString("forecastDate").trim();
				sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
				mdate = sdf.parse(date).getTime();
				sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
				date = sdf.format(mdate);

				String text = j.getString("weather");
				String max = j.getString("maxTemp") + "&deg;C";
				String min = j.getString("minTemp") + "&deg;C";
				if(!metric)
				{
					max = j.getString("maxTempF") + "&deg;F";
					min = j.getString("minTempF") + "&deg;F";
				}

				String code = j.getString("weatherIcon");
				code = code.substring(0, 2);

				tmp = "<tr><td style='width:10%;' rowspan='2'>" + "<img width='40px' src='file:///android_res/drawable/i" + code + ".png'></td>";
				out.append(tmp);

				tmp = "<td style='width:80%;'><b>" + date + "</b></td>";
				out.append(tmp);

				if(!max.equals(""))
					tmp = "<td style='width:10%;text-align:right;'><b>" + max + "</b></td></tr>";
				else
					tmp = "<td style='width:10%;text-align:right;'><b>&nbsp;</b></td></tr>";
				out.append(tmp);

				tmp = "<tr><td>" + text + "</td>";
				out.append(tmp);

				if(!min.equals(""))
					tmp = "<td style='text-align:right;'>" + min + "</td></tr>";
				else
					tmp = "<td style='text-align:right;'>&nbsp;</td></tr>";
				out.append(tmp);

				if(showHeader)
				{
					tmp = "<tr><td style='font-size:10pt;' colspan='5'>&nbsp;</td></tr>";
					out.append(tmp);
				}
			}

			out.append("</table>");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{out.toString(), desc};
	}

	String[] processBOM(String data)
	{
		return processBOM(data, false);
	}

	String[] processBOM(String data, boolean showHeader)
	{
		String desc;
		StringBuilder out = new StringBuilder();

		if(data == null || data.equals(""))
			return null;

		try
		{
			JSONObject jobj = new JSONObject(data);
			desc = jobj.getString("description") + ", Australia";

			String tmp = jobj.getString("content");
			tmp = tmp.substring(0, tmp.length() - 6);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
			long mdate = sdf.parse(tmp).getTime();
			sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
			String date = sdf.format(mdate);

			tmp = "<div style='font-size:12pt;'>" + date + "</div>";
			out.append(tmp);
			tmp = "<table style='width:100%;'>\n";
			out.append(tmp);

			JSONArray jarr = jobj.getJSONArray("forecast-period");
			for(int i = 0; i < jarr.length(); i++)
			{
				String text = "";
				String code = "";
				String min = "";
				String max = "";
				JSONObject j = jarr.getJSONObject(i);
				for(int x = 0; x < j.getJSONArray("text").length(); x++)
				{
					if(j.getJSONArray("text").getJSONObject(x).getString("type").equals("precis"))
					{
						text = j.getJSONArray("text").getJSONObject(x).getString("content");
						break;
					}
				}

				try
				{
					JSONArray jarr2 = j.getJSONArray("element");
					for (int x = 0; x < jarr2.length(); x++)
					{
						if (jarr2.getJSONObject(x).getString("type").equals("forecast_icon_code"))
							code = jarr2.getJSONObject(x).getString("content");

						if (jarr2.getJSONObject(x).getString("type").equals("air_temperature_minimum"))
							min = jarr2.getJSONObject(x).getString("content");

						if (jarr2.getJSONObject(x).getString("type").equals("air_temperature_maximum"))
							max = jarr2.getJSONObject(x).getString("content");
					}
				} catch (JSONException e) {
					code = j.getJSONObject("element").getString("content");
				}

				date = j.getString("start-time-local").trim();
				date = date.substring(0, date.length() - 6);
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
				mdate = sdf.parse(date).getTime();
				sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
				date = sdf.format(mdate);

				tmp = "<tr><td style='width:10%;' rowspan='2'>" + "<img width='40px' src='file:///android_res/drawable/bom" + code + ".png'></td>";
				out.append(tmp);

				tmp = "<td style='width:80%;'><b>" + date + "</b></td>";
				out.append(tmp);

				if(!max.equals(""))
					tmp = "<td style='width:10%;text-align:right;'><b>" + max + "&deg;C</b></td></tr>";
				else
					tmp = "<td style='width:10%;text-align:right;'><b>&nbsp;</b></td></tr>";
				out.append(tmp);

				tmp = "<tr><td>" + text + "</td>";
				out.append(tmp);

				if(!min.equals(""))
					tmp = "<td style='text-align:right;'>" + min + "&deg;C</td></tr>";
				else
					tmp = "<td style='text-align:right;'>&nbsp;</td></tr>";
				out.append(tmp);

				if(showHeader)
				{
					tmp = "<tr><td style='font-size:10pt;' colspan='5'>&nbsp;</td></tr>";
					out.append(tmp);
				}
				//LogMessage(jarr.getJSONObject(i).toString(2));
			}

			out.append("</table>");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{out.toString(), desc};
	}

	String[] processYR(String data)
	{
		//boolean metric = GetBoolPref("metric", true);
		StringBuilder out = new StringBuilder();
		String desc;

		try
		{
			JSONObject jobj = new XmlToJson.Builder(data).build().toJson();
			if(jobj == null)
				return null;

			jobj = jobj.getJSONObject("weatherdata");
			JSONObject location = jobj.getJSONObject("location");
			desc = location.getString("name") + ", " + location.getString("country");

			long mdate = (long)GetIntPref("rssCheck", 0) * 1000;
			SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
			String lastupdate = sdf.format(mdate);

			JSONArray jarr = jobj.getJSONObject("forecast")
					.getJSONObject("tabular")
					.getJSONArray("time");

			if(jarr == null)
				return null;

			String tmp = "<div style='font-size:12pt;'>" + lastupdate + "</div>";
			out.append(tmp);
			tmp = "<table style='width:100%;'>\n";
			out.append(tmp);

			String olddate = "";
			for(int i = 0; i < jarr.length(); i++)
			{
				String from = jarr.getJSONObject(i).getString("from");
				String to = jarr.getJSONObject(i).getString("to");
				String period = jarr.getJSONObject(i).getString("period");
				JSONObject symbol = jarr.getJSONObject(i).getJSONObject("symbol");
				JSONObject precipitation = jarr.getJSONObject(i).getJSONObject("precipitation");
				JSONObject temperature = jarr.getJSONObject(i).getJSONObject("temperature");
				JSONObject windDirection = jarr.getJSONObject(i).getJSONObject("windDirection");
				//JSONObject pressure = jarr.getJSONObject(i).getJSONObject("pressure");
				JSONObject windSpeed = jarr.getJSONObject(i).getJSONObject("windSpeed");

				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
				mdate = sdf.parse(from).getTime();
				sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
				String date = sdf.format(mdate);

				sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
				from = sdf.format(mdate);

				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
				mdate = sdf.parse(to).getTime();
				sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
				to = sdf.format(mdate);

				if(i == 0 || Integer.parseInt(period) == 0)
				{
					if(!olddate.equals("") && olddate.equals(date))
						break;

					olddate = date;

					tmp = "<tr><th colspan='5' style='text-align:left;'><strong>" + date + "</strong></th></tr>\n";
					out.append(tmp);
					tmp = "<tr><th>Time</th><th>FCast</th><th>Temp</th><th>Rain</th><th>Wind</th></tr>\n";
					out.append(tmp);
				}

				tmp = "<tr>" +
						"<td>" + from + "-" + to + "</td>";
				out.append(tmp);

				String code = symbol.getString("var");

				tmp = "<td><img width='40px' src='file:///android_res/drawable/yrno" + code + ".png'></td>";
				out.append(tmp);

				tmp = "<td>" + temperature.getString("value") + "&deg;C</td>";
				out.append(tmp);

				tmp = "<td>" + precipitation.getString("value") + "mm</td>";
				out.append(tmp);

				tmp = "<td>" + windSpeed.getString("name") + ", " + windSpeed.get("mps") + "m/s from the " + windDirection.getString("name") + "</td>";
				out.append(tmp);

				out.append("</tr>\n");
			}

			out.append("</table>");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{out.toString(), desc};
	}

	String[] processWZ(String data)
	{
		return processWZ(data, false);
	}

	String[] processWZ(String data, boolean showHeader)
	{
		boolean metric = GetBoolPref("metric", true);

		try
		{
			String desc = "", content = "", pubDate = "";

			String[] bits = data.split("<title>");
			if (bits.length >= 2)
				desc = bits[1].split("</title>")[0].trim();

			bits = data.split("<description>");
			if (bits.length >= 3)
			{
				String s = bits[2].split("</description>")[0];
				content = s.substring(9, s.length() - 3).trim();
			}

			bits = data.split("<pubDate>");
			if (bits.length >= 2)
				pubDate = bits[1].split("</pubDate>")[0].trim();

			if (pubDate.equals(""))
				return null;

			StringBuilder str = new StringBuilder();
			String stmp;

			SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault());
			long mdate = sdf.parse(pubDate).getTime();
			sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
			pubDate = sdf.format(mdate);

			if(showHeader)
			{
				content = content.replace("src=\"http://www.weatherzone.com.au/images/icons/fcast_30/", "width=\"50px\" src=\"file:///android_res/drawable/wz")
						.replace(".gif", ".png");

				String[] days = content.split("<b>");
				String day = days[1];
				String[] tmp = day.split("</b>", 2);

				String[] mybits = tmp[1].split("<br />");
				String myimg = mybits[1];
				String mydesc = mybits[2];
				String[] range = mybits[3].split(" - ", 2);


				stmp = "<table style='width:100%;border:0px;'>";
				str.append(stmp);

				stmp = "<tr><td style='width:50%;font-size:48pt;'>" + scrubTemp(range[1], metric) + "</td>";
				str.append(stmp);

				stmp = "<td style='width:50%;text-align:right;'>" + myimg.replace("50px", "80px") + "</td></tr>";
				str.append(stmp);

				stmp = "<tr><td style='font-size:16pt;'>" + scrubTemp(range[0], metric) + "</td>";
				str.append(stmp);

				stmp = "<td style='text-align:right;font-size:16pt;'>" +mydesc + "</td></tr></table><br />";
				str.append(stmp);

				stmp = "<table style='width:100%;border:0px;'>";
				str.append(stmp);

				for(int i = 2; i < days.length; i++)
				{
					day = days[i];
					tmp = day.split("</b>", 2);
					String dayName = tmp[0];

					if (tmp.length <= 1)
						continue;

					mybits = tmp[1].split("<br />");
					myimg = mybits[1];
					mydesc = mybits[2];
					range = mybits[3].split(" - ", 2);

					stmp = "<tr><td style='width:10%;' rowspan='2'>" + myimg + "</td>";
					str.append(stmp);

					stmp = "<td style='width:65%;'><b>" + dayName + "</b></td>";
					str.append(stmp);

					stmp = "<td style='width:25%;text-align:right;'><b>" + scrubTemp(range[1], metric) + "</b></td></tr>";
					str.append(stmp);

					stmp = "<tr><td>" + mydesc + "</td>";
					str.append(stmp);

					stmp = "<td style='text-align:right;'>" + scrubTemp(range[0], metric) + "</td></tr>";
					str.append(stmp);

					stmp = "<tr><td style='font-size:4pt;' colspan='5'>&nbsp;</td></tr>";
					str.append(stmp);
				}

				stmp = "</table>";
				str.append(stmp);
			} else {
				content = content.replace("src=\"http://www.weatherzone.com.au/images/icons/fcast_30/", "width=\"40px\" src=\"file:///android_res/drawable/wz")
						.replace(".gif", ".png");

				stmp = "<table style='width:100%;border:0px;'>";
				str.append(stmp);

				String[] days = content.split("<b>");
				for (String day : days)
				{
					String[] tmp = day.split("</b>", 2);
					String dayName = tmp[0];

					if (tmp.length <= 1)
						continue;

					String[] mybits = tmp[1].split("<br />");
					String myimg = mybits[1];
					String mydesc = mybits[2];
					String[] range = mybits[3].split(" - ", 2);

					stmp = "<tr><td style='width:10%;' rowspan='2'>" + myimg + "</td>";
					str.append(stmp);

					stmp = "<td style='width:65%;'><b>" + dayName + "</b></td>";
					str.append(stmp);

					stmp = "<td style='width:25%;text-align:right;'><b>" + scrubTemp(range[1], metric) + "</b></td></tr>";
					str.append(stmp);

					stmp = "<tr><td>" + mydesc + "</td>";
					str.append(stmp);

					stmp = "<td style='text-align:right;'>" + scrubTemp(range[0], metric) + "</td></tr>";
					str.append(stmp);

					stmp = "<tr><td style='font-size:4pt;' colspan='5'>&nbsp;</td></tr>";
					str.append(stmp);
				}

				stmp = "</table>";
				str.append(stmp);
			}

			content = "<div style='font-size:12pt;'>" + pubDate + "</div>" + str.toString();

			Common.LogMessage("content=" + content);

			return new String[]{content, desc};
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private String scrubTemp(String temp, boolean metric)
	{
		if(temp.length() <= 7 || metric)
			return temp;

		float i = Float.parseFloat(temp.substring(0, temp.length() - 7));
		LogMessage("i == " + i);
		int f = round(9.0f / 5.0f * i + 32.0f);
		LogMessage("f == " + f);
		return String.valueOf(f) + "&#176;F";
	}

	String[] processYahoo(String data)
	{
		return processYahoo(data, false);
	}

    String[] processYahoo(String data, boolean showHeader)
    {
	    JSONObject json;

	    try
	    {
		    json = new JSONObject(data);

		    Common.LogMessage("starting JSON Parsing");

		    JSONObject query = json.getJSONObject("query");
		    JSONObject results = query.getJSONObject("results");
		    JSONObject channel = results.getJSONObject("channel");
		    String pubdate = channel.getString("lastBuildDate");
		    JSONObject item = channel.getJSONObject("item");
		    JSONObject units = channel.getJSONObject("units");
		    String temp = units.getString("temperature");
		    final String desc = channel.getString("description").substring(19);
		    JSONArray forecast = item.getJSONArray("forecast");
		    Common.LogMessage("ended JSON Parsing");

		    StringBuilder str = new StringBuilder();
		    String stmp;

		    Calendar calendar = Calendar.getInstance();
		    int hour = calendar.get(Calendar.HOUR_OF_DAY);

		    int start = 0;
		    if (hour >= 15)
			    start = 1;

		    JSONObject tmp = forecast.getJSONObject(start);
		    int code = tmp.getInt("code");

		    if(showHeader)
		    {
			    stmp = "<div style='font-size:12pt;'>" + pubdate + "</div>";
			    str.append(stmp);

			    stmp = "<table style='width:100%;border:0px;'>";
			    str.append(stmp);

			    stmp = "<tr><td style='width:50%;font-size:48pt;'>" + tmp.getString("high") + "&deg;" + temp + "</td>";
			    str.append(stmp);

			    stmp = "<td style='width:50%;text-align:right;'><img width='80px' src='file:///android_res/drawable/yahoo" + code + ".png'></td></tr>";
			    str.append(stmp);

			    stmp = "<tr><td style='font-size:16pt;'>" + tmp.getString("low") + "&deg;" + temp + "</td>";
			    str.append(stmp);

			    stmp = "<td style='text-align:right;font-size:16pt;'>" + tmp.getString("text") + "</td></tr></table><br />";
			    str.append(stmp);

			    stmp = "<table style='width:100%;border:0px;'>";
			    str.append(stmp);

			    for (int i = start + 1; i <= start + 5; i++)
			    {
				    tmp = forecast.getJSONObject(i);
				    code = tmp.getInt("code");

				    stmp = "<tr><td style='width:10%;' rowspan='2'>" + "<img width='40px' src='file:///android_res/drawable/yahoo" + code + ".png'></td>";
				    str.append(stmp);

				    stmp = "<td style='width:45%;'><b>" + tmp.getString("day") + ", " + tmp.getString("date") + "</b></td>";
				    str.append(stmp);

				    stmp = "<td style='width:45%;text-align:right;'><b>" + tmp.getString("high") + "&deg;" + temp + "</b></td></tr>";
				    str.append(stmp);

				    stmp = "<tr><td>" + tmp.getString("text") + "</td>";
				    str.append(stmp);

				    stmp = "<td style='text-align:right;'>" + tmp.getString("low") + "&deg;" + temp + "</td></tr>";
				    str.append(stmp);

				    stmp = "<tr><td style='font-size:10pt;' colspan='5'>&nbsp;</td></tr>";
				    str.append(stmp);
			    }

			    stmp = "</table>";
			    str.append(stmp);
		    } else {
			    stmp = "<div style='font-size:12pt;'>" + pubdate + "</div>";
			    str.append(stmp);

			    stmp = "<table style='width:100%;border:0px;'>";
			    str.append(stmp);

			    for (int i = start; i <= start + 5; i++)
			    {
				    tmp = forecast.getJSONObject(i);
				    code = tmp.getInt("code");

				    stmp = "<tr><td style='width:10%;' rowspan='2'>" + "<img width='40px' src='file:///android_res/drawable/yahoo" + code + "'></td>";
				    str.append(stmp);

				    stmp = "<td style='width:45%;'><b>" + tmp.getString("day") + ", " + tmp.getString("date") + "</b></td>";
				    str.append(stmp);

				    stmp = "<td style='width:45%;text-align:right;'><b>" + tmp.getString("high") + "&deg;" + temp + "</b></td></tr>";
				    str.append(stmp);

				    stmp = "<tr><td>" + tmp.getString("text") + "</td>";
				    str.append(stmp);

				    stmp = "<td style='text-align:right;'>" + tmp.getString("low") + "&deg;" + temp + "</td></tr>";
				    str.append(stmp);

				    stmp = "<tr><td style='font-size:5pt;' colspan='5'>&nbsp;</td></tr>";
				    str.append(stmp);
			    }

			    stmp = "</table>";
			    str.append(stmp);
		    }

		    Common.LogMessage("finished building forecast: " + str.toString());
		    return new String[]{str.toString(), desc};
	    } catch (Exception e) {
		    e.printStackTrace();
	    }

	    return null;
    }

	void SendIntents()
	{
		Intent intent = new Intent();
		intent.setAction(Common.UPDATE_INTENT);
		context.sendBroadcast(intent);
		Common.LogMessage("update_intent broadcast.");

		intent = new Intent(context, WidgetProvider.class);
		intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
		int ids[] = AppWidgetManager.getInstance(context.getApplicationContext()).getAppWidgetIds(new ComponentName(context.getApplicationContext(), WidgetProvider.class));
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
		context.sendBroadcast(intent);
		Common.LogMessage("widget intent broadcasted");
	}

	void getWeather()
	{
		if(t != null)
		{
			if(t.isAlive())
				t.interrupt();
			t = null;
		}

		t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					String data = GetStringPref("BASE_URL", "");
					if (data.equals(""))
						return;

					reallyGetWeather(data);
					SendIntents();
				} catch (Exception e) {
					e.printStackTrace();
					SetStringPref("lastError", e.toString());
					SendFailedIntent();
				}
			}
		});

		t.start();
	}

	void reallyGetWeather(String data) throws Exception
	{
		Uri uri = Uri.parse(data);
		if (uri.getUserInfo() != null && uri.getUserInfo().contains(":"))
		{
			final String[] UC = uri.getUserInfo().split(":");
			Common.LogMessage("uri username = " + uri.getUserInfo());

			if (UC.length > 1)
			{
				Authenticator.setDefault(new Authenticator()
				{
					protected PasswordAuthentication getPasswordAuthentication()
					{
						return new PasswordAuthentication(UC[0], UC[1].toCharArray());
					}
				});
			}
		}

		URL url = new URL(data);
		HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
		urlConnection.setConnectTimeout(60000);
		urlConnection.setReadTimeout(60000);
		urlConnection.setRequestMethod("GET");
		urlConnection.setDoOutput(true);
		urlConnection.connect();

		StringBuilder sb = new StringBuilder();
		String line;

		InterruptThread it = new InterruptThread(Thread.currentThread(), urlConnection);
		Thread myThread = new Thread(it);

		myThread.start();
		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		while ((line = in.readLine()) != null)
			sb.append(line);
		in.close();

		it.interupt = false;
		myThread.interrupt();

		line = sb.toString().trim();
		if(!line.equals(""))
		{
			String bits[] = line.split("\\|");
			if (Double.valueOf(bits[0]) < Common.inigo_version)
			{
				if(GetLongPref("inigo_version", 0) < Common.inigo_version)
				{
					SetLongPref("inigo_version", Common.inigo_version);
					sendAlert();
				}
			}

			if (Double.valueOf(bits[0]) >= 4000)
			{
				sb = new StringBuilder();
				for (int i = 1; i < bits.length; i++)
				{
					if (sb.length() > 0)
						sb.append("|");
					sb.append(bits[i]);
				}

				line = sb.toString().trim();
			}

			SetStringPref("LastDownload", line);
			SetLongPref("LastDownloadTime", round(System.currentTimeMillis() / 1000));
		}
	}

	class InterruptThread implements Runnable
	{
		Thread parent;
		HttpURLConnection con;
		boolean interupt;

		InterruptThread(Thread parent, HttpURLConnection con)
		{
			this.parent = parent;
			this.con = con;
			this.interupt = true;
		}

		public void run()
		{
			try
			{
				Thread.sleep(15000);
				if(interupt)
				{
					Common.LogMessage("Timer thread forcing parent to quit connection");
					con.disconnect();
					parent.interrupt();
				}
			} catch (InterruptedException e) {
				// TODO: ignore this.
			}
		}
	}

	//	https://stackoverflow.com/questions/3841317/how-do-i-see-if-wi-fi-is-connected-on-android
	boolean checkWifiOnAndConnected()
	{
		WifiManager wifiMgr = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

		if (wifiMgr.isWifiEnabled())
		{ // Wi-Fi adapter is ON
			WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
			return wifiInfo.getNetworkId() != -1;
		} else {
			return false; // Wi-Fi adapter is OFF
		}
	}

	private void sendAlert()
	{
		Intent intent = new Intent();
		intent.setAction(Common.INIGO_INTENT);
		context.sendBroadcast(intent);
		Common.LogMessage("Send user note about upgrading the Inigo Plugin");
	}

	private void SendFailedIntent()
	{
		Intent intent = new Intent();
		intent.setAction(FAILED_INTENT);
		context.sendBroadcast(intent);
		LogMessage("failed_intent broadcast.");
	}
}