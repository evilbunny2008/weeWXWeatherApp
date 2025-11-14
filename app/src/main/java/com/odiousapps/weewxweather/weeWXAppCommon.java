package com.odiousapps.weewxweather;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Base64;
import android.util.Log;

import com.github.evilbunny2008.xmltojson.XmlToJson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.LiveData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings({"unused", "SameParameterValue", "ApplySharedPref", "ConstantConditions",
		"SameReturnValue", "BooleanMethodIsAlwaysInverted",
		"SetTextI18n"})
class weeWXAppCommon
{
	private final static String PREFS_NAME = "WeeWxWeatherPrefs";
	final static boolean debug_on = false;
	final static boolean web_debug_on = false;

	final static String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";

	static final String EXIT_INTENT = "com.odiousapps.weewxweather.EXIT_INTENT";
	static final String FAILED_INTENT = "com.odiousapps.weewxweather.FAILED_INTENT";
	static final String INIGO_INTENT = "com.odiousapps.weewxweather.INIGO_UPDATE";

	static final String REFRESH_WEATHER_FORECAST_INTENT = "com.odiousapps.weewxweather.REFRESH_WEATHER_FORECAST_INTENT";
	static final String REFRESH_FORECAST_INTENT = "com.odiousapps.weewxweather.REFRESH_FORECAST_INTENT";
	static final String REFRESH_RADAR_INTENT = "com.odiousapps.weewxweather.REFRESH_RADAR_INTENT";
	static final String REFRESH_WEATHER_INTENT = "com.odiousapps.weewxweather.REFRESH_WEATHER_INTENT";
	static final String REFRESH_WEBCAM_INTENT = "com.odiousapps.weewxweather.REFRESH_WEBCAM_INTENT";

	static final String WIDGET_UPDATE = "com.odiousapps.weewxweather.WIDGET_UPDATE";

	static final String WIDGET_THEME_MODE = "widget_theme_mode";

	public static final long icon_version = 12;
	private static final String icon_url = "https://github.com/evilbunny2008/InigoPlugin/releases/download/1.0.0/icons.zip";

	private static final BitmapFactory.Options options = new BitmapFactory.Options();

	private static JSONObject nws = null;

	private static Typeface tf_bold = null;

	private final static ExecutorService executor = Executors.newFixedThreadPool(4);

	private static Future<?> forecastTask, radarTask, weatherTask, webcamTask;

	private static long ftStart, rtStart, wcStart, wtStart;

	static final float[] NEGATIVE = {
			-1.0f, 0, 0, 0, 255, // red
			0, -1.0f, 0, 0, 255, // green
			0, 0, -1.0f, 0, 255, // blue
			0, 0, 0, 1.0f, 0  // alpha
	};

	static
	{
		try
		{
			System.setProperty("http.agent", UA);
		} catch(Exception ignored) {
		}

		try
		{
			tf_bold = Typeface.create("sans-serif", Typeface.BOLD);
		} catch(Exception ignored) {
		}

		options.inJustDecodeBounds = false;
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
	}

	/** @noinspection CallToPrintStackTrace*/
	static void doStackOutput(Exception e)
	{
		e.printStackTrace();
	}

	public static boolean isEmpty(StringBuilder sb)
	{
		return sb == null || sb.length() == 0;
	}

	static long[] getPeriod()
	{
		long[] def = {0, 0};

		int pos = GetIntPref("updateInterval", weeWXApp.updateInterval_default);
		if(pos < 0)
			return def;

		long period;

		switch(pos)
		{
			case 1 -> period = 5 * 60_000L;
			case 2 -> period = 10 * 60_000L;
			case 3 -> period = 15 * 60_000L;
			case 4 -> period = 30 * 60_000L;
			case 5 -> period = 60 * 60_000L;
			default ->
			{
				return def;
			}
		}

		return new long[]{period, 45_000L};
	}

	static void LogMessage(String value)
	{
		LogMessage(value, false);
	}

	static void LogMessage(String value, boolean showAnyway)
	{
		if(debug_on || showAnyway)
		{
			int len = value.indexOf("\n");
			if(len <= 0)
				len = value.length();
			Log.i("weeWXApp", "message='" + value.substring(0, len) + "'");
		}
	}

	private static Context getApplicationContext()
	{
		return weeWXApp.getInstance();
	}

	private static SharedPreferences getPrefSettings()
	{
		return getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	}

	private static SharedPreferences getPrefSettings(Context context)
	{
		return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	}

	static void SetStringPref(String name, String value)
	{
		SharedPreferences.Editor editor = getPrefSettings().edit();
		editor.putString(name, value);
		editor.apply();
	}

	static void RemovePref(String name)
	{
		SharedPreferences.Editor editor = getPrefSettings().edit();
		editor.putString(name, "");
		editor.remove(name);
		editor.apply();
	}

	static void clearPref()
	{
		getPrefSettings().edit().clear().apply();
	}

	static void commitPref()
	{
		SharedPreferences.Editor editor = getPrefSettings().edit();
		editor.commit();
	}

	static boolean isPrefSet(String name)
	{
		return getPrefSettings().contains(name);
	}

	static boolean fixTypes(Context context)
	{
		SharedPreferences prefs = getPrefSettings(context);

		Map<String, ?> all = prefs.getAll();
		SharedPreferences.Editor editor = prefs.edit();
		boolean changed = false;

		for(Map.Entry<String, ?> entry : all.entrySet())
		{
			String key = entry.getKey();
			Object value = entry.getValue();

			if(!(value instanceof String))
				continue; // only convert strings

			String s = ((String)value).trim();
			Number parsed = parseNumber(s);

			if(parsed == null)
				continue; // not numeric

			// Choose best numeric type
			Object newValue = compressNumber(parsed);

			// If re-writing as exact same type/string, skip
			if(value.equals(newValue))
				continue;

			// Persist as correct numeric type
			switch(newValue)
			{
				case Integer i -> editor.putInt(key, i);
				case Long l -> editor.putLong(key, l);
				case Float v -> editor.putFloat(key, v);
				case Double v -> editor.putString(key, Double.toString(v));
				case null, default ->
				{
					continue;
				}
			}

			changed = true;
		}

		if(changed)
			editor.apply();

		return changed;
	}

	static Number parseNumber(String s)
	{
		if(s == null || s.isEmpty())
			return null;

		try
		{
			if(s.contains("."))
				return Double.parseDouble(s);
			else
				return Long.parseLong(s);
		} catch (Exception e) {
			return null;
		}
	}

	static Object compressNumber(Number number)
	{
		if(number instanceof Long)
		{
			long l = number.longValue();
			if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE)
				return (int)l;
			return l;
		}

		if(number instanceof Double)
		{
			double d = number.doubleValue();
			float f = (float)d;

			// If float is lossless, use float
			if((double)f == d)
				return f;

			// Otherwise keep double, stored as string
			return d;
		}

		return number;
	}

	static String GetStringPref(String name, String default_value)
	{
		return getPrefSettings().getString(name, default_value);
	}

	static void SetLongPref(String name, long value)
	{
		SharedPreferences.Editor editor = getPrefSettings().edit();
		editor.putLong(name, value);
		editor.apply();
	}

	static long GetLongPref(String name, long default_value)
	{
		try
		{
			return getPrefSettings().getLong(name, default_value);
		} catch(ClassCastException e) {
			return getPrefSettings().getInt(name, (int)default_value);
		}
	}

	static void SetFloatPref(String name, float value)
	{
		SharedPreferences.Editor editor = getPrefSettings().edit();
		editor.putFloat(name, value);
		editor.apply();
	}

	static float GetFloatPref(String name, float default_value)
	{
		return getPrefSettings().getFloat(name, default_value);
	}

	static void SetIntPref(String name, int value)
	{
		SharedPreferences.Editor editor = getPrefSettings().edit();
		editor.putInt(name, value);
		editor.apply();
	}

	static int GetIntPref(String name, int default_value)
	{
		return getPrefSettings().getInt(name, default_value);
	}

	static void SetBoolPref(String name, boolean value)
	{
		SharedPreferences.Editor editor = getPrefSettings().edit();
		editor.putBoolean(name, value);
		editor.apply();
	}

	static boolean GetBoolPref(String name, boolean default_value)
	{
		try
		{
			return getPrefSettings().getBoolean(name, default_value);
		} catch(ClassCastException e) {
			int def = 0;
			if(default_value)
				def = 1;

			int i = getPrefSettings().getInt(name, def);
			return i == 1;
		}
	}

	private static Day getFirstDay(List<Day> days)
	{
		Day first = null;
		if(days != null && !days.isEmpty())
		{
			Iterator<Day> it = days.iterator();
			if(it.hasNext())
				first = it.next();
		}

		return first;
	}

	private static String generateForecast(List<Day> days, long timestamp, boolean showHeader)
	{
		LogMessage("Starting generateForecast()");
		LogMessage("days: "+days);

		if(days.isEmpty())
			return null;

		StringBuilder sb = new StringBuilder();
		int start = 0;

		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
		String string_time = sdf.format(timestamp);

		sb.append("\n<div class='header'>").append(string_time).append("</div>\n\n");

		if(showHeader)
		{
			start = 1;
			Day first = getFirstDay(days);

			sb.append("<div class='today'>\n\t<div class='topRow'>\n");
			sb.append("\t\t<div class='iconBig'>\n");
			if(!first.icon.startsWith("file:///") && !first.icon.startsWith("data:image"))
				sb.append("\t\t\t<i class='").append(first.icon).append(" />\n");
			else
				sb.append("\t\t\t<img alt='weather icon' src='").append(first.icon.replaceAll("\n", "").replaceAll("\r", "")).append("' />\n");
			sb.append("\t\t</div>\n");

			sb.append("\t\t<div class='wordToday'>").append(weeWXApp.getAndroidString(R.string.today)).append("</div>\n");

			sb.append("\t\t<div class='bigTemp'>\n");

			sb.append("\t\t\t<div class='maxTemp'>");
			if(first.max.isBlank() || first.max.equals("&deg;C") || first.max.equals("&deg;F"))
				sb.append(weeWXApp.emptyField);
			else
				sb.append(first.max);
			sb.append("</div>\n");

			sb.append("\t\t\t<div class='minTemp'>");
			if(first.min.isBlank() || first.min.equals("&deg;C") || first.min.equals("&deg;F"))
				sb.append(weeWXApp.emptyField);
			else
				sb.append(first.min);
			sb.append("</div>\n\t\t</div>\n\t</div>\n");

			sb.append("\t<div class='bigForecastText'>\n\t\t");
			sb.append(first.text);
			sb.append("\n\t</div>\n</div>\n\n");

			sb.append("<hr />\n\n");
		}

		sb.append("<div class='forecast'>\n\n");

		for(int i = start; i < days.size(); i++)
		{
			Day day = days.get(i);

			sb.append("\t<div class='day'>\n\t\t<div class='dayTopRow'>\n");

			String tmpstr = day.icon.replaceAll("\n", "")
					.replaceAll("\r", "")
					.replaceAll("\t", "");

			sb.append("\t\t\t<div class='iconSmall'>\n");
			if(!day.icon.startsWith("file:///") && !day.icon.startsWith("data:image"))
				sb.append("\t\t\t\t<i class='").append(day.icon).append("'></i>\n");
			else
				sb.append("\t\t\t\t<img alt='weather icon' src='")
						.append(tmpstr).append("' />\n");
			sb.append("\t\t\t</div>\n");

			sb.append("\t\t\t<div class='dayTitle'>");

			if(i == 0)
				sb.append(weeWXApp.getAndroidString(R.string.today));

			if(i != 0)
				sb.append(day.day);

			sb.append("</div>\n");

			sb.append("\t\t\t<div class='smallTemp'>\n");
			sb.append("\t\t\t\t<div class='forecastMax'>");
			if(day.max.equals("&deg;C") || day.max.equals("&deg;F"))
				sb.append("N/A");
			else
				sb.append(day.max);
			sb.append("</div>\n");

			sb.append("\t\t\t\t<div class='forecastMin'>");
			if(day.min.equals("&deg;C") || day.min.equals("&deg;F"))
				sb.append("N/A");
			else
				sb.append(day.min);
			sb.append("</div>\n\t\t\t</div>\n\t\t</div>\n");

			sb.append("\t\t<div class='desc'>\n\t\t\t");
			sb.append(day.text).append("\n\t\t</div>\n");

			if(i < days.size() - 1)
				sb.append("\t\t<hr />\n");

			sb.append("\t</div>\n\n");
		}

		if((days.size() - start) % 2 == 1)
			sb.append("<div class='day'></div>\n\n");


		sb.append("</div>\n");

		//CustomDebug.writeDebug("common.html", sb.toString());

		return sb.toString();
	}

	static String[] processBOM2(String data)
	{
		return processBOM2(data, false);
	}

	static String[] processBOM2(String data, boolean showHeader)
	{

		if(data.isBlank())
			return null;

		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);
		boolean use_icons = GetBoolPref("useIcons", weeWXApp.useIcons_default);
		String desc;
		List<Day> days = new ArrayList<>();
		long timestamp = 0;

		try
		{
			Document doc = Jsoup.parse(data);
			desc = doc.title().split(" - Bureau of Meteorology")[0].trim();
			String fcdiv = doc.select("div.forecasts").html();
			String obs = doc.select("span").html().split("issued at ")[1].split("\\.", 2)[0].trim();

			int i = 0, j = obs.indexOf(":");
			String hour = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String minute = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String am_pm = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			//String TZ = obs.substring(i, j);
			i = j + 5;
			j = obs.indexOf(" ", i);
			//String DOW = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String date = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String month = obs.substring(i, j);
			i = j + 1;
			j = obs.length();
			String year = obs.substring(i, j);

			obs = hour + ":" + minute + " " + am_pm + " " + date + " " + month + " " + year;

			SimpleDateFormat sdf = new SimpleDateFormat("h:mm aa d MMMM yyyy", Locale.getDefault());
			Date df = sdf.parse(obs);
			if(df != null)
				timestamp = df.getTime();

			String[] bits = fcdiv.split("<dl class='forecast-summary'>");
			String bit = bits[1];
			Day day = new Day();

			day.day = bit.split("<a href='", 2)[1].split("'>", 2)[0].split("/forecast/detailed/#d", 2)[1].trim();
			sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

			day.timestamp = 0;
			df = sdf.parse(day.day);
			if(df != null)
				day.timestamp = df.getTime();

			sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
			day.day = sdf.format(day.timestamp);

			day.icon = "https://reg.bom.gov.au" + bit.split("<img src='", 2)[1].split("' alt='", 2)[0].trim();

			if(bit.contains("<dd class='max'>"))
				day.max = bit.split("<dd class='max'>")[1].split("</dd>")[0].trim();

			if(bit.contains("<dd class='min'>"))
				day.min = bit.split("<dd class='min'>")[1].split("</dd>")[0].trim();

			day.text = bit.split("<dd class='summary'>")[1].split("</dd>")[0].trim();

			String fileName =  day.icon.substring(day.icon.lastIndexOf('/') + 1, day.icon.length() - 4);

			if(!use_icons)
			{
				if(!fileName.equals("frost"))
					day.icon = "wi wi-bom-" + fileName;
				else
					day.icon = "flaticon-thermometer";
			} else {
				fileName = "bom2" + day.icon.substring(day.icon.lastIndexOf('/') + 1).replaceAll("-", "_");
				fileName = checkImage(fileName, day.icon);
				if(fileName == null)
					return null;

				File f = new File(fileName);
				try(FileInputStream imageInFile = new FileInputStream(f))
				{
					byte[] imageData = new byte[(int) f.length()];
					if(imageInFile.read(imageData) > 0)
						day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
				} catch(Exception e) {
					doStackOutput(e);
					doStackOutput(e);
				}
			}

			day.max = day.max.replaceAll("°C", "").trim();
			day.min = day.min.replaceAll("°C", "").trim();

			if(metric)
			{
				day.max += "&deg;C";
				day.min += "&deg;C";
			} else {
				if(!day.max.isBlank())
					day.max += Math.round((Double.parseDouble(day.max) * 9.0 / 5.0) + 32.0) + "&deg;F";
				if(!day.min.isBlank())
					day.min += Math.round((Double.parseDouble(day.min) * 9.0 / 5.0) + 32.0) + "&deg;F";
			}

			if(day.max.isBlank() || day.max.startsWith("&deg;"))
				day.max = "N/A";

			days.add(day);

			for(i = 2; i < bits.length; i++)
			{
				day = new Day();
				bit = bits[i];
				day.day = bit.split("<a href='", 2)[1].split("'>", 2)[0].split("/forecast/detailed/#d", 2)[1].trim();
				sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
				day.timestamp = 0;
				df = sdf.parse(day.day);
				if(df != null)
					day.timestamp = df.getTime();

				sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
				day.day = sdf.format(day.timestamp);

				day.icon = "https://reg.bom.gov.au" + bit.split("<img src='", 2)[1].split("' alt='", 2)[0].trim();
				day.max = bit.split("<dd class='max'>")[1].split("</dd>")[0].trim();
				day.min = bit.split("<dd class='min'>")[1].split("</dd>")[0].trim();
				day.text = bit.split("<dd class='summary'>")[1].split("</dd>")[0].trim();

				fileName = day.icon.substring(day.icon.lastIndexOf('/') + 1, day.icon.length() - 4);

				if(!use_icons)
				{
					if(!fileName.equals("frost"))
						day.icon = "wi wi-bom-" + fileName;
					else
						day.icon = "flaticon-thermometer";
				} else {
					fileName = "bom2" + day.icon.substring(day.icon.lastIndexOf('/') + 1).replaceAll("-", "_");
					fileName = checkImage(fileName, day.icon);
					if(fileName == null)
						return null;

					File f = new File(fileName);
					try(FileInputStream imageInFile = new FileInputStream(f))
					{
						byte[] imageData = new byte[(int) f.length()];
						if(imageInFile.read(imageData) > 0)
							day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
					} catch(Exception e) {
						doStackOutput(e);
					}
				}

				day.max = day.max.replaceAll("°C", "").trim();
				day.min = day.min.replaceAll("°C", "").trim();

				if(metric)
				{
					day.max = day.max + "&deg;C";
					day.min = day.min + "&deg;C";
				} else {
					day.max = Math.round((Double.parseDouble(day.max) * 9.0 / 5.0) + 32.0) + "&deg;F";
					day.min = Math.round((Double.parseDouble(day.min) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}

				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	static String[] processBOM3(String data)
	{
		return processBOM3(data, false);
	}

	static String[] processBOM3(String data, boolean showHeader)
	{
		LogMessage("Starting processBOM3()");

		if(data.isBlank())
		{
			LogMessage("data is blank, skipping...");
			return null;
		}

		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);
		boolean use_icons = GetBoolPref("useIcons", weeWXApp.useIcons_default);
		String desc = "";
		List<Day> days = new ArrayList<>();
		long timestamp = 0;

		try
		{
			LogMessage("Line 1178");

			JSONObject jobj = new JSONObject(data);

			desc = jobj.getJSONObject("metadata").getString("forecast_region");
			String tmp = jobj.getJSONObject("metadata").getString("issue_time");

			LogMessage("Line 1185");

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

			Date df = sdf.parse(tmp);
			if(df != null)
				timestamp = df.getTime();

			LogMessage("Line 1192");

			JSONArray mydays = jobj.getJSONArray("data");
			for(int i = 0; i < mydays.length(); i++)
			{
				Day day = new Day();

				day.timestamp = 0;
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
				df = sdf.parse(mydays.getJSONObject(i).getString("date"));
				if(df != null)
					day.timestamp = df.getTime();

				sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
				day.day = sdf.format(day.timestamp);

				LogMessage("Line 1207");

				if(metric)
				{
					try
					{
						day.max = mydays.getJSONObject(i).getInt("temp_max") + "&deg;C";
					} catch(Exception e) {
						// ignore errors
					}

					try
					{
						day.min = mydays.getJSONObject(i).getInt("temp_min") + "&deg;C";
					} catch(Exception e) {
						// ignore errors
					}
				} else {
					try
					{
						day.max = Math.round((mydays.getJSONObject(i).getInt("temp_max") * 9.0 / 5.0) + 32.0) + "&deg;F";
					} catch(Exception e) {
						// ignore errors
					}

					try
					{
						day.min = Math.round((mydays.getJSONObject(i).getInt("temp_min") * 9.0 / 5.0) + 32.0) + "&deg;F";
					} catch(Exception e) {
						// ignore errors
					}
				}

				LogMessage("Line 1240");

				if(!mydays.getJSONObject(i).getString("extended_text").equals("null"))
					day.text = mydays.getJSONObject(i).getString("extended_text");

				LogMessage("Line 1246");

				String fileName = bomlookup(mydays.getJSONObject(i).getString("icon_descriptor"));
				if(!fileName.equals("null"))
				{
					if(!use_icons)
					{
						LogMessage("Line 1254");

						if(!fileName.equals("frost"))
							day.icon = "wi wi-bom-" + fileName;
						else
							day.icon = "flaticon-thermometer";
					} else {
						LogMessage("Line 1260");

						fileName = checkImage("bom2" + fileName + ".png", null);
						if(fileName != null)
						{
							File f = new File(fileName);
							try(FileInputStream imageInFile = new FileInputStream(f))
							{
								byte[] imageData = new byte[(int)f.length()];
								if(imageInFile.read(imageData) > 0)
									day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
							} catch(Exception e) {
								doStackOutput(e);
							}
						}
					}
				}

				days.add(day);

				LogMessage("Line 1273");
			}
		} catch(Exception e) {
			LogMessage("Error! " + e);
			Activity act = getActivity();
			if(act == null)
				return new String[]{"Error!", e.toString()};

			act.finish();
		}

		LogMessage("Forecast data has been processed, sending for layout...");
		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	private static String bomlookup(String icon)
	{
		icon = icon.replace("-", "_");

		return switch(icon)
		{
			case "shower" -> "showers";
			case "dusty" -> "dust";
			case "mostly_sunny" -> "partly_cloudy";
			case "light_shower" -> "light_showers";
			case "windy" -> "wind";
			default -> icon;
		};
	}

	static String[] processMET(String data)
	{
		return processMET(data, false);
	}

	static String[] processMET(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		try
		{
			return processMET(data, showHeader);
		} catch(Exception e) {
			doStackOutput(e);
		}

		return null;
	}

	private static String[] reallyProcessMET(String data, boolean showHeader) throws IOException, ParseException
	{
		if(data.isBlank())
			return null;

		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);
		boolean use_icons = GetBoolPref("useIcons", weeWXApp.useIcons_default);
		long timestamp = GetLongPref("rssCheck", 0) * 1_000L;
		String desc;
		List<Day> days = new ArrayList<>();

		desc = data.split("<title>", 2)[1].split(" weather - Met Office</title>",2)[0].trim();

		String[] forecasts = data.split("<ul id='dayNav'", 2)[1].split("</ul>", 2)[0].split("<li");
		for(int i = 1; i < forecasts.length; i++)
		{
			Day day = new Day();
			String date = forecasts[i].split("data-tab-id='", 2)[1].split("'")[0].trim();

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

			day.timestamp = 0;
			Date df = sdf.parse(date);
			if(df != null)
				day.timestamp = df.getTime();

			sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
			day.day = sdf.format(day.timestamp);

			String icon = "https://beta.metoffice.gov.uk" + forecasts[i].split("<img class='icon'")[1].split("src='")[1].split("'>")[0].trim();
			String fileName = icon.substring(icon.lastIndexOf('/') + 1).replaceAll("\\.svg$", ".png");
			day.min = forecasts[i].split("<span class='tab-temp-low'", 2)[1].split("'>")[1].split("</span>")[0].trim();
			day.max = forecasts[i].split("<span class='tab-temp-high'", 2)[1].split("'>")[1].split("</span>")[0].trim();
			day.text = forecasts[i].split("<div class='summary-text", 2)[1].split("'>", 3)[2]
								.split("</div>", 2)[0].replaceAll("</span>", "").replaceAll("<span>", "");

			day.min = day.min.substring(0, day.min.length() - 5);
			day.max = day.max.substring(0, day.max.length() - 5);

			if(!use_icons)
			{
				day.icon = "wi wi-metoffice-" + fileName.substring(0, fileName.lastIndexOf("."));
			} else {
				fileName = checkImage("met" + fileName, null);
				if(fileName == null)
					return null;

				File f = new File(fileName);
				try(FileInputStream imageInFile = new FileInputStream(f))
				{
					byte[] imageData = new byte[(int) f.length()];
					if(imageInFile.read(imageData) > 0)
						day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
				} catch(Exception e) {
					doStackOutput(e);
				}
			}

			if(metric)
			{
				day.max += "&deg;C";
				day.min += "&deg;C";
			} else {
				day.max = Math.round((Double.parseDouble(day.max) * 9.0 / 5.0) + 32.0) + "&deg;F";
				day.min = Math.round((Double.parseDouble(day.min) * 9.0 / 5.0) + 32.0) + "&deg;F";
			}

			days.add(day);
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	static String[] processWCA(String data)
	{
		return processWCA(data, false);
	}

	static String[] processWCA(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);
		boolean use_icons = GetBoolPref("useIcons", weeWXApp.useIcons_default);
		String desc;
		List<Day> days = new ArrayList<>();
		long timestamp, lastTS;

		try
		{
			String obs = data.split("Forecast issued: ", 2)[1].trim();
			obs = obs.split("</span>", 2)[0].trim();

			int i = 0, j = obs.indexOf(":");
			String hour = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String minute = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String am_pm = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			//String TZ = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			//String DOW = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String date = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String month = obs.substring(i, j);
			i = j + 1;
			j = obs.length();
			String year = obs.substring(i, j);

			obs = hour + ":" + minute + " " + am_pm + " " + date + " " + month + " " + year;

			SimpleDateFormat sdf = new SimpleDateFormat("h:mm aa d MMMM yyyy", Locale.getDefault());

			lastTS = timestamp = 0;
			Date df = sdf.parse(obs);
			if(df != null)
				lastTS = timestamp = df.getTime();

			desc = data.split("<dt>Observed at:</dt>", 2)[1].split("<dd class='mrgn-bttm-0'>")[1].split("</dd>")[0].trim();

			data = data.split("<div class='div-table'>", 2)[1].trim();
			data = data.split("<section><details open='open' class='wxo-detailedfore'>")[0].trim();
			data = data.substring(0, data.length() - 7).trim();

			String[] bits = data.split("<div class='div-column'>");

			for(i = 1; i < bits.length; i++)
			{
				Document doc = Jsoup.parse(bits[i].trim());
				Elements div = doc.select("div");
				for (j = 0; j < div.size(); j++)
				{
					String text = "", img_url = "", temp = "", pop = "";
					Day day = new Day();

					if(div.get(j).className().contains("greybkgrd"))
					{
						j++;
						continue;
					}

					if(div.get(j).toString().contains("div-row-head"))
					{
						if(div.get(j).select("div").html().contains("Tonight"))
						{
							date = "Tonight";
							day.timestamp = lastTS;
						} else if(div.get(j).select("div").html().contains("Night")) {
							date = "Night";
							day.timestamp = lastTS;
						} else {
							date = div.get(j).html().split("<strong title='", 2)[1].split("'>", 2)[0].trim();
							day.timestamp = convertDaytoTS(date, Locale.CANADA, lastTS);
						}
					} else
						continue;

					j++;

					if(j >= div.size())
					{
						LogMessage("continue");
						continue;
					}

					try
					{
						if(div.outerHtml().contains("div-row-data"))
						{
							if(metric)
								temp = div.get(j).select("div").select("span").html().split("<abbr")[0].trim() + "C";
							else
								temp = div.get(j).select("div").select("span").html().split("</abbr>")[1].split("<abbr")[0].trim() + "F";
							text = div.get(j).select("div").select("img").outerHtml().split("alt='", 2)[1].split("'", 2)[0].trim();
							img_url = "https://www.weather.gc.ca" + div.get(j).select("div").select("img").outerHtml().split("src='", 2)[1].split("'", 2)[0].trim();
							pop = div.get(j).select("div").select("small").html().trim();
						}
					} catch(Exception e) {
						LogMessage("hmmm 2 == " + div.html());
						doStackOutput(e);
					}

					String fileName = img_url.substring(img_url.lastIndexOf('/') + 1).replaceAll("\\.gif$", "");

					if(!use_icons)
					{
						if(!fileName.equals("26"))
							day.icon = "wi wi-weather-gc-ca-" + fileName;
						else
							day.icon = "flaticon-thermometer";
					} else {
						fileName = checkImage("wca" + fileName + ".png", null);
						if(fileName == null)
							return null;

						File f = new File(fileName);
						try(FileInputStream imageInFile = new FileInputStream(f))
						{
							byte[] imageData = new byte[(int) f.length()];
							if(imageInFile.read(imageData) > 0)
								day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
						} catch(Exception e) {
							doStackOutput(e);
						}
					}

					day.day = date;
					day.max = temp;
					day.text = text;
					day.min = pop;

					days.add(day);

					lastTS = day.timestamp;
				}
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	static String[] processWCAF(String data)
	{
		return processWCAF(data, false);
	}

	static String[] processWCAF(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);
		boolean use_icons = GetBoolPref("useIcons", weeWXApp.useIcons_default);
		String desc;
		List<Day> days = new ArrayList<>();
		long timestamp, lastTS;

		try
		{
			String obs = data.split("Prévisions émises à : ", 2)[1].trim();
			obs = obs.split("</span>", 2)[0].trim();

			int i = 0, j = obs.indexOf("h");
			String hour = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String minute = obs.substring(i, j);

			String[] bits = obs.split(" ");
			String date = bits[bits.length - 3];
			String month = bits[bits.length - 2];
			String year = bits[bits.length - 1];

			obs = hour + ":" + minute + " " + date + " " + month + " " + year;

			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm d MMMM yyyy", Locale.CANADA_FRENCH);

			lastTS = timestamp = 0;
			Date df = sdf.parse(obs);
			if(df != null)
				lastTS = timestamp = df.getTime();

			desc = data.split("<dt>Enregistrées à :</dt>", 2)[1].split("<dd class='mrgn-bttm-0'>")[1].split("</dd>")[0].trim();

			data = data.split("<div class='div-table'>", 2)[1].trim();
			data = data.split("<section><details open='open' class='wxo-detailedfore'>")[0].trim();
			data = data.substring(0, data.length() - 7).trim();

			bits = data.split("<div class='div-column'>");

			for(i = 1; i < bits.length; i++)
			{
				Document doc = Jsoup.parse(bits[i].trim());
				Elements div = doc.select("div");
				for (j = 0; j < div.size(); j++)
				{
					Day day = new Day();
					String text = "", img_url = "", temp = "", pop = "";

					if(div.get(j).className().contains("greybkgrd"))
					{
						j++;
						continue;
					}

					if(div.get(j).toString().contains("div-row-head"))
					{
						if(div.get(j).select("div").html().contains("Ce soir et cette nuit"))
						{
							date = "Cette nuit";
							day.timestamp = lastTS;
						} else if(div.get(j).select("div").html().contains("Nuit")) {
							date = "Nuit";
							day.timestamp = lastTS;
						} else {
							date = div.get(j).html().split("<strong title='", 2)[1].split("'>", 2)[0].trim();
							day.timestamp = convertDaytoTS(date, Locale.CANADA_FRENCH, lastTS);
						}
					} else
						continue;

					j++;

					if(j >= div.size())
					{
						LogMessage("continue");
						continue;
					}

					try
					{
						if(div.outerHtml().contains("div-row-data"))
						{
							if(metric)
								temp = div.get(j).select("div").select("span").html().split("<abbr")[0].trim() + "C";
							else
								temp = div.get(j).select("div").select("span").html().split("</abbr>")[1].split("<abbr")[0].trim() + "F";
							text = div.get(j).select("div").select("img").outerHtml().split("alt='", 2)[1].split("'", 2)[0].trim();
							img_url = "https://www.weather.gc.ca" + div.get(j).select("div").select("img").outerHtml().split("src='", 2)[1].split("'", 2)[0].trim();
							pop = div.get(j).select("div").select("small").html().trim();
						}
					} catch(Exception e) {
						LogMessage("hmmm 2 == " + div.html());
						doStackOutput(e);
					}

					String fileName = img_url.substring(img_url.lastIndexOf('/') + 1).replaceAll("\\.gif$", "");

					if(!use_icons)
					{
						if(!fileName.equals("26"))
							day.icon = "wi wi-weather-gc-ca-" + fileName;
						else
							day.icon = "flaticon-thermometer";
					} else {
						fileName = checkImage("wca" + fileName + ".png", null);
						if(fileName == null)
							return null;

						File f = new File(fileName);
						try(FileInputStream imageInFile = new FileInputStream(f))
						{
							byte[] imageData = new byte[(int) f.length()];
							if(imageInFile.read(imageData) > 0)
								day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
						} catch(Exception e) {
							doStackOutput(e);
						}
					}

					day.day = date;
					day.max = temp;
					day.text = text;
					day.min = pop;
					days.add(day);

					lastTS = day.timestamp;
				}
			}

		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	// Thanks goes to the https://saratoga-weather.org folk for the base NOAA icons and code for dualimage.php

	static String[] processWGOV(String data)
	{
		return processWGOV(data, false);
	}

	static String[] processWGOV(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);
		List<Day> days = new ArrayList<>();
		long timestamp;
		String desc;

		try
		{
			JSONObject jobj = new JSONObject(data);

			desc = jobj.getJSONObject("currentobservation").getString("name");
			String tmp = jobj.getString("creationDate");

			SimpleDateFormat sdf;
			sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

			timestamp = 0;
			Date df =sdf.parse(tmp);
			if(df != null)
				timestamp = df.getTime();

			JSONArray periodName = jobj.getJSONObject("time").getJSONArray("startPeriodName");
			JSONArray validTime = jobj.getJSONObject("time").getJSONArray("startValidTime");
			JSONArray weather = jobj.getJSONObject("data").getJSONArray("weather");
			final JSONArray iconLink = jobj.getJSONObject("data").getJSONArray("iconLink");
			JSONArray temperature = jobj.getJSONObject("data").getJSONArray("temperature");
			for(int i = 0; i < periodName.length(); i++)
			{
				Day day = new Day();
				String fimg = "", simg = "", fper = "", sper = "", number;
				iconLink.put(i, iconLink.getString(i).replace("http://", "https://"));
				String url = iconLink.getString(i);

				String fn = "wgov" + url.substring(url.lastIndexOf('/') + 1).replace(".png", ".jpg");
				if(fn.startsWith("wgovDualImage.php"))
				{
					tmp = url.split("\\?", 2)[1].trim();
					String[] lines = tmp.split("&");
					for(String line : lines)
					{
						String trim_line = line.trim();
						String[] bits = trim_line.split("=", 2);
						if(bits[0].trim().equals("i"))
							fimg = "wgov" + bits[1].trim();
						if(bits[0].trim().equals("j"))
							simg = "wgov" + bits[1].trim();
						if(bits[0].trim().equals("ip"))
							fper = bits[1].trim();
						if(bits[0].trim().equals("jp"))
							sper = bits[1].trim();
					}

					Bitmap bmp1 = loadImage(fimg + ".jpg");
					Bitmap bmp2 = loadImage(simg + ".jpg");

					if(bmp1 != null && bmp2 != null)
					{
						if(!fimg.equals(simg))
						{
							Bitmap bmp = combineImages(bmp1, bmp2, fimg.substring(4), simg.substring(4), fper + "%", sper + "%");
							if(bmp == null)
							{
								LogMessage("continue1, bmp is null");
								continue;
							}

							byte[] byteArray = bitmapToBytes(bmp);
							bmp.recycle();

							// https://stackoverflow.com/questions/9224056/android-bitmap-to-base64-string

							String encoded = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
							iconLink.put(i, encoded);
						} else {
							Bitmap bmp = combineImage(bmp1, fper + "%", sper + "%");
							if(bmp == null)
							{
								LogMessage("continue2, bmp is null");
								continue;
							}

							byte[] byteArray = bitmapToBytes(bmp);
							bmp.recycle();

							String encoded = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
							iconLink.put(i, encoded);
						}
					}
				} else {
					Pattern p = Pattern.compile("\\d");
					number = p.matcher(fn).replaceAll("");
					if(!number.isBlank())
					{
						fn = fn.replaceAll("\\d{2,3}\\.jpg$", ".jpg");
						Bitmap bmp3 = loadImage(fn);

						if(bmp3 != null)
						{
							Bitmap bmp4 = loadImage("wgovoverlay.jpg");
							if(bmp4 == null)
								return null;

							bmp3 = overlay(bmp3, bmp4, number + "%");

							byte[] byteArray = bitmapToBytes(bmp3);
							bmp3.recycle();
							bmp4.recycle();

							// https://stackoverflow.com/questions/9224056/android-bitmap-to-base64-string

							String encoded = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
							iconLink.put(i, encoded);
						}
					}
				}

				if(iconLink.getString(i).toLowerCase(Locale.ENGLISH).startsWith("http"))
				{
					String fileName = "wgov" + iconLink.getString(i).substring(iconLink.getString(i).lastIndexOf("/") + 1).trim().replaceAll("\\.png$", ".jpg");
					fileName = checkImage(fileName, iconLink.getString(i));
					if(fileName == null)
						return null;

					File f = new File(fileName);
					try(FileInputStream imageInFile = new FileInputStream(f))
					{
						byte[] imageData = new byte[(int) f.length()];
						if(imageInFile.read(imageData) > 0)
							day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
					} catch(Exception e) {
						doStackOutput(e);
					}
				} else {
					day.icon = iconLink.getString(i);
				}

				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

				day.timestamp = 0;
				df = sdf.parse(validTime.getString(i));
				if(df != null)
					day.timestamp = df.getTime();

				day.day = periodName.getString(i);

				if(!metric)
					day.max = temperature.getString(i) + "&deg;F";
				else
					day.max = Math.round((Double.parseDouble(temperature.getString(i)) - 32.0) * 5.0 / 9.0)  + "&deg;C";
				day.text = weather.getString(i);
				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	static String[] processWMO(String data)
	{
		return processWMO(data, false);
	}

	static String[] processWMO(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);
		String desc;
		long timestamp;
		List<Day> days = new ArrayList<>();

		try
		{
			JSONObject jobj = new JSONObject(data);

			desc = jobj.getJSONObject("city").getString("cityName") + ", " + jobj.getJSONObject("city").getJSONObject("member").getString("memName");
			String tmp = jobj.getJSONObject("city").getJSONObject("forecast").getString("issueDate");

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

			timestamp = 0;
			Date df = sdf.parse(tmp);
			if(df != null)
				timestamp = df.getTime();

			JSONArray jarr = jobj.getJSONObject("city").getJSONObject("forecast").getJSONArray("forecastDay");
			for(int i = 0; i < jarr.length(); i++)
			{
				Day day = new Day();
				JSONObject j = jarr.getJSONObject(i);

				String date = j.getString("forecastDate").trim();
				sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

				day.timestamp = 0;
				df = sdf.parse(date);
				if(df != null)
					day.timestamp = df.getTime();

				sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
				date = sdf.format(day.timestamp);

				day.text = j.getString("weather");
				day.max = j.getString("maxTemp") + "&deg;C";
				day.min = j.getString("minTemp") + "&deg;C";
				if(!metric)
				{
					day.max = j.getString("maxTempF") + "&deg;F";
					day.min = j.getString("minTempF") + "&deg;F";
				}

				String code = j.getString("weatherIcon");
				code = code.substring(0, code.length() - 2);

				if((Integer.parseInt(code) >= 1 && Integer.parseInt(code) <= 27) || code.equals("31") || code.equals("35"))
					day.icon = "wi wi-wmo-" + code;
				else if(code.equals("28"))
					day.icon = "flaticon-cactus";
				else if(code.equals("29") || code.equals("30"))
					day.icon = "flaticon-thermometer";
				else if(code.equals("32"))
					day.icon = "flaticon-cold";
				else if(code.equals("33"))
					day.icon = "flaticon-warm";
				else if(code.equals("34"))
					day.icon = "flaticon-cool";

				day.day = date;
				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	static String[] processBOM(String data)
	{
		return processBOM(data, false);
	}

	static String[] processBOM(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);
		boolean use_icons = GetBoolPref("useIcons", weeWXApp.useIcons_default);
		String desc;
		long timestamp;
		List<Day> days = new ArrayList<>();

		try
		{
			JSONObject jobj = new JSONObject(data);
			desc = jobj.getString("description") + ", Australia";

			String tmp = jobj.getString("content");
			SimpleDateFormat sdf;
			sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

			timestamp = 0;
			Date df = sdf.parse(tmp);
			if(df != null)
				timestamp = df.getTime();

			JSONArray jarr = jobj.getJSONArray("forecast-period");
			for(int i = 0; i < jarr.length(); i++)
			{
				Day day = new Day();
				String code = "";

				JSONObject j = jarr.getJSONObject(i);
				for(int x = 0; x < j.getJSONArray("text").length(); x++)
				{
					if(j.getJSONArray("text").getJSONObject(x).getString("type").equals("precis"))
					{
						day.text = j.getJSONArray("text").getJSONObject(x).getString("content");
						break;
					}
				}

				try
				{
					JSONArray jarr2 = j.getJSONArray("element");
					for (int x = 0; x < jarr2.length(); x++)
					{
						if(jarr2.getJSONObject(x).getString("type").equals("forecast_icon_code"))
							code = jarr2.getJSONObject(x).getString("content");

						if(jarr2.getJSONObject(x).getString("type").equals("air_temperature_minimum"))
							day.min = jarr2.getJSONObject(x).getString("content");

						if(jarr2.getJSONObject(x).getString("type").equals("air_temperature_maximum"))
							day.max = jarr2.getJSONObject(x).getString("content");
					}
				} catch(JSONException e) {
					code = j.getJSONObject("element").getString("content");
				}

				String date = j.getString("start-time-local").trim();
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

				day.timestamp = 0;
				df = sdf.parse(date);
				if(df != null)
					day.timestamp = df.getTime();

				sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
				day.day = sdf.format(day.timestamp);

				if(!use_icons)
				{
					if(!code.equals("14"))
						day.icon = "wi wi-bom-ftp-" + code;
					else
						day.icon = "flaticon-thermometer";
				} else {
					String fileName = checkImage("bom" + code + ".png", null);
					if(fileName == null)
						return null;

					File f = new File(fileName);
					try(FileInputStream imageInFile = new FileInputStream(f))
					{
						byte[] imageData = new byte[(int) f.length()];
						if(imageInFile.read(imageData) > 0)
							day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
					} catch(Exception e) {
						doStackOutput(e);
					}
				}

				if(metric)
				{
					day.max += "&deg;C";
					day.min += "&deg;C";
				} else {
					day.max = Math.round((Double.parseDouble(day.max) * 9.0 / 5.0) + 32.0) + "&deg;F";
					day.min = Math.round((Double.parseDouble(day.min) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}

				if(day.max.equals("&deg;C") || day.max.equals("&deg;F"))
					day.max = "N/A";

				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	static String[] processMetService(String data)
	{
		return processMetService(data, false);
	}

	static String[] processMetService(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);
		boolean use_icons = GetBoolPref("useIcons", weeWXApp.useIcons_default);
		List<Day> days = new ArrayList<>();
		String desc;
		long timestamp = 0;
		long tmp_timestamp = 0;

		try
		{
			LogMessage(data);
			JSONObject jobj = new JSONObject(data);
			JSONArray loop = jobj.getJSONArray("days");
			String string_time = loop.getJSONObject(0).getString("issuedAtISO");
			desc = jobj.getString("locationWASP") + ", New Zealand";

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

			Date df = sdf.parse(string_time);
			if(df != null)
				timestamp = df.getTime();

			for(int i = 0; i < loop.length(); i++)
			{
				Day day = new Day();
				JSONObject jtmp = loop.getJSONObject(i);
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

				df = sdf.parse(jtmp.getString("dateISO"));
				if(df != null)
					tmp_timestamp = df.getTime();

				day.timestamp = tmp_timestamp;
				day.day = jtmp.getString("dow");
				day.text = jtmp.getString("forecast");
				day.max = jtmp.getString("max");
				day.min = jtmp.getString("min");
				if(jtmp.has("partDayData"))
					day.icon = jtmp.getJSONObject("partDayData").getJSONObject("afternoon").getString("forecastWord");
				else
					day.icon = jtmp.getString("forecastWord");

				day.icon = day.icon.toLowerCase(Locale.ENGLISH).replaceAll(" ", "-").trim();

				if(!use_icons)
				{
					if(!day.icon.equals("frost"))
						day.icon = "wi wi-metservice-" + day.icon;
					else
						day.icon = "flaticon-thermometer";
				} else {
					day.icon = day.icon.replaceAll("-", "_");
					String fileName = checkImage("ms_" + day.icon + ".png", null);
					if(fileName == null)
						return null;

					File f = new File(fileName);
					try(FileInputStream imageInFile = new FileInputStream(f))
					{
						byte[] imageData = new byte[(int) f.length()];
						if(imageInFile.read(imageData) > 0)
							day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
					} catch(Exception e) {
						doStackOutput(e);
					}
				}

				if(metric)
				{
					day.max += "&deg;C";
					day.min += "&deg;C";
				} else {
					day.max = Math.round((Double.parseDouble(day.max) * 9.0 / 5.0) + 32.0) + "&deg;F";
					day.min = Math.round((Double.parseDouble(day.min) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}

				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	static String[] processDWD(String data)
	{
		return processDWD(data, false);
	}

	static String[] processDWD(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);
		boolean use_icons = GetBoolPref("useIcons", weeWXApp.useIcons_default);
		List<Day> days = new ArrayList<>();
		String desc = "";
		long timestamp = 0;
		long lastTS = 0;

		try
		{
			String[] bits = data.split("<title>");
			if(bits.length >= 2)
				desc = bits[1].split("</title>")[0];
			desc = desc.substring(desc.lastIndexOf(" - ") + 3).trim();
			String string_time = data.split("<tr class='headRow'>", 2)[1].split("</tr>", 2)[0].trim();
			String date = string_time.split("<td width='30%' class='stattime'>", 2)[1].split("</td>", 2)[0].trim();
			string_time = date + " " + string_time.split("<td width='40%' class='stattime'>", 2)[1].split(" Uhr</td>", 2)[0].trim();

			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy' 'HH", Locale.getDefault());
			Date df = sdf.parse(string_time);
			if(df != null)
				lastTS = timestamp = df.getTime();

			data = data.split("<td width='40%' class='statwert'>Vorhersage</td>", 2)[1].split("</table>", 2)[0].trim();
			bits = data.split("<tr");
			for(int i = 1; i < bits.length; i++)
			{
				Day day = new Day();
				String bit = bits[i];
				String icon, temp;
				if(bit.split("<td ><b>", 2).length > 1)
					day.day = bit.split("<td ><b>", 2)[1].split("</b></td>", 2)[0].trim();
				else
					day.day = bit.split("<td><b>", 2)[1].split("</b></td>", 2)[0].trim();

				Locale locale = new Locale.Builder().setLanguage("de").setRegion("DE").build();
				day.timestamp = convertDaytoTS(day.day, locale, lastTS);
				if(day.timestamp != 0)
				{
					sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
					day.day = sdf.format(day.timestamp) + " " + day.day.substring(day.day.lastIndexOf(" ") + 1);
				}

				if(bit.split("<td ><img name='piktogramm' src='", 2).length > 1)
					icon = bit.split("<td ><img name='piktogramm' src='", 2)[1].split("' width='50' alt='", 2)[0].trim();
				else
					icon = bit.split("<td><img name='piktogramm' src='", 2)[1].split("' width='50' alt='", 2)[0].trim();

				if(bit.split("'></td>\r\n<td >", 2).length > 1)
					temp = bit.split("'></td>\r\n<td >", 2)[1].split("Grad <abbr title='Celsius'>C</abbr></td>\r\n", 2)[0].trim();
				else
					temp = bit.split("'></td>\r\n<td>", 2)[1].split("Grad <abbr title='Celsius'>C</abbr></td>\r\n", 2)[0].trim();

				icon = icon.replaceAll("/DE/wetter/_functions/piktos/vhs_", "").replaceAll("\\?__blob=normal", "").trim();
				String fileName = "dwd_" + icon.replaceAll("-", "_");
				String url = "https://www.dwd.de/DE/wetter/_functions/piktos/" + icon + "?__blob=normal";
				fileName = checkImage(fileName, url);
				if(fileName == null)
					return null;

				if(!use_icons)
				{
					icon = icon.replaceAll("_", "-");
					icon = icon.substring(0, icon.lastIndexOf("."));
					if(!icon.equals("pic-48") && !icon.equals("pic-66") && !icon.equals("pic67"))
						day.icon = "wi wi-dwd-" + icon;
					else
						day.icon = "flaticon-thermometer";
				} else {
					File f = new File(fileName);
					try(FileInputStream imageInFile = new FileInputStream(f))
					{
						byte[] imageData = new byte[(int) f.length()];
						if(imageInFile.read(imageData) > 0)
							day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
					} catch(Exception e) {
						doStackOutput(e);
					}
				}

				day.min = "&deg;C";
				if(metric)
					day.max = temp + "&deg;C";
				else
					day.max = Math.round((Double.parseDouble(temp) * 9.0 / 5.0) + 32.0) + "&deg;F";

				days.add(day);
				lastTS = day.timestamp;
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	static String[] processTempoItalia(String data)
	{
		return processTempoItalia(data, false);
	}

	static String[] processTempoItalia(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);
		boolean use_icons = GetBoolPref("useIcons", weeWXApp.useIcons_default);
		List<Day> days = new ArrayList<>();
		String desc;
		long timestamp = 0;
		long lastTS = 0;

		try
		{
			String stuff = data.split("<div id='weatherDayNavigator'>", 2)[1].trim();
			stuff = stuff.split("<h2>", 2)[1].trim();
			desc = stuff.split(" <span class='day'>")[0].trim();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
			String string_time = sdf.format(System.currentTimeMillis());
			sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
			Date df = sdf.parse(string_time);
			if(df != null)
				lastTS = timestamp = df.getTime();

			data = data.split("<tbody>")[1].trim();
			String[] bits = data.split("<tr>");
			for(int i = 1; i < bits.length; i++)
			{
				Day day = new Day();
				String bit = bits[i].trim();
				String icon;
				day.day = bit.split("<td class='timeweek'>")[1].split("'>")[1].split("</a></td>", 2)[0].trim();

				Locale locale = new Locale.Builder().setLanguage("it").setRegion("IT").build();
				day.timestamp = convertDaytoTS(day.day, locale, lastTS);
				if(day.timestamp != 0)
				{
					sdf = new SimpleDateFormat("EEE dd", Locale.getDefault());
					day.day = sdf.format(day.timestamp) + " " + day.day.substring(day.day.lastIndexOf(" ") + 1);
				}

				icon = bit.split("<td class='skyIcon'><img src='", 2)[1].split("' alt='",2)[0].trim();
				String[] ret = checkFilesIt(icon);
				if(ret[0] != null)
				{
					File f = new File(ret[1]);
					try(FileInputStream imageInFile = new FileInputStream(f))
					{
						byte[] imageData = new byte[(int) f.length()];
						if(imageInFile.read(imageData) > 0)
							day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
					} catch(Exception e) {
						doStackOutput(e);
					}
				} else
					return ret;
//                LogMessage("day.icon=" + day.icon);

				day.max = bit.split("<td class='tempmax'>", 2)[1].split("°C</td>", 2)[0].trim();
				day.min = bit.split("<td class='tempmin'>", 2)[1].split("°C</td>", 2)[0].trim();

				day.text = bit.split("<td class='skyDesc'>")[1].split("</td>")[0].trim();

				if(!use_icons)
				{
					String filename = new File(icon).getName();
					day.icon = "wi wi-tempoitalia-" + filename.substring(0, filename.length() - 4);
				}

				LogMessage("day.icon=" + day.icon);

				if(metric)
				{
					day.max += "&deg;C";
					day.min += "&deg;C";
				} else {
					day.max = Math.round((Double.parseDouble(day.max) * 9.0 / 5.0) + 32.0) + "&deg;F";
					day.min = Math.round((Double.parseDouble(day.min) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}

				days.add(day);
				lastTS = day.timestamp;
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	static String[] processAEMET(String data)
	{
		return processAEMET(data, false);
	}

	static String[] processAEMET(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);
		boolean use_icons = GetBoolPref("useIcons", weeWXApp.useIcons_default);
		List<Day> days = new ArrayList<>();
		String desc;
		long timestamp = 0;

		try
		{
			JSONObject jobj = new XmlToJson.Builder(data).build().toJson();
			if(jobj == null)
				return null;

			jobj = jobj.getJSONObject("root");
			desc = jobj.getString("nombre") + ", " + jobj.getString("provincia");

			String elaborado = jobj.getString("elaborado");

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
			Date df = sdf.parse(elaborado);
			if(df != null)
				timestamp = df.getTime();

			JSONArray dates = jobj.getJSONObject("prediccion").getJSONArray("dia");
			for(int i = 0; i < dates.length(); i++)
			{
				Day day = new Day();
				JSONObject jtmp = dates.getJSONObject(i);
				String fecha = jtmp.getString("fecha");
				sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

				day.timestamp = 0;
				df = sdf.parse(fecha);
				if(df != null)
					day.timestamp = df.getTime();

				sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
				day.day = sdf.format(day.timestamp);

				JSONObject estado_cielo = null;

				Object v = jtmp.get("estado_cielo");
				if(v instanceof JSONObject)
				{
					estado_cielo = jtmp.getJSONObject("estado_cielo");
				} else if(v instanceof JSONArray)
				{
					JSONArray jarr = jtmp.getJSONArray("estado_cielo");
					for (int j = 0; j < jarr.length(); j++)
					{
						if(!jarr.getJSONObject(j).getString("descripcion").isBlank())
						{
							estado_cielo = jarr.getJSONObject(j);
							break;
						}
					}
				}

				if(estado_cielo == null)
					return null;

				JSONObject temperatura = jtmp.getJSONObject("temperatura");

				String code = estado_cielo.getString("content");
				if(!use_icons)
				{
					if(!code.startsWith("7"))
						day.icon = "wi wi-aemet-" + code;
					else
						day.icon = "flaticon-thermometer";
				} else
				{
					String url = "https://www.aemet.es/imagenes/png/estado_cielo/" + code + "_g.png";
					String fileName = "aemet_" + code + "_g.png";
					fileName = checkImage(fileName, url);
					if(fileName == null)
						return null;

					File f = new File(fileName);
					try(FileInputStream imageInFile = new FileInputStream(f))
					{
						byte[] imageData = new byte[(int) f.length()];
						if(imageInFile.read(imageData) > 0)
							day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
					} catch(Exception e) {
						doStackOutput(e);
					}
				}

				day.max = temperatura.getString("maxima");
				day.min = temperatura.getString("minima");
				if(metric)
				{
					day.max += "&deg;C";
					day.min += "&deg;C";
				} else {
					day.max = Math.round((Double.parseDouble(day.max) * 9.0 / 5.0) + 32.0) + "&deg;F";
					day.min = Math.round((Double.parseDouble(day.min) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}

				day.text = estado_cielo.getString("descripcion");
				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	static String[] processWCOM(String data)
	{
		return processWCOM(data, false);
	}

	static String[] processWCOM(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);
		boolean use_icons = GetBoolPref("useIcons", weeWXApp.useIcons_default);
		long timestamp = GetLongPref("rssCheck", 0) * 1_000L;

		List<Day> days = new ArrayList<>();
		String desc;

		try
		{
			JSONObject jobj = new JSONObject(data);
			desc = "TODO";

			JSONArray validDate = jobj.getJSONArray("dayOfWeek");
			JSONArray timestamps = jobj.getJSONArray("validTimeUtc");
			JSONObject daypart = jobj.getJSONArray("daypart").getJSONObject(0);
			JSONArray icons = daypart.getJSONArray("iconCode");
			JSONArray phrase = daypart.getJSONArray("wxPhraseLong");
			JSONArray day_temp = jobj.getJSONArray("temperatureMax");
			JSONArray night_temp = jobj.getJSONArray("temperatureMin");
			JSONArray dayname = jobj.getJSONArray("dayOfWeek");

			for(int i = 0; i < validDate.length(); i++)
			{
				if(icons.getString(i) == null || icons.getString(i).equals("null"))
					continue;

				Day day = new Day();
				day.timestamp = timestamps.getLong(i);
				day.day = dayname.getString(i);
				day.text = phrase.getString(i);
				day.icon = icons.getString(i);
				day.max = day_temp.getString(i);
				day.min = night_temp.getString(i);

				if(!use_icons)
				{
					day.icon = "wi wi-yahoo-" + day.icon;
				} else {
					String fileName = checkImage(day.icon + ".png", null);
					if(fileName == null)
						return null;

					LogMessage("yahoo filename = " + fileName);

					File f = new File(fileName);
					try(FileInputStream imageInFile = new FileInputStream(f))
					{
						byte[] imageData = new byte[(int) f.length()];
						if(imageInFile.read(imageData) > 0)
							day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
					} catch(Exception e) {
						doStackOutput(e);
					}
				}

				if(metric)
				{
					day.max += "&deg;C";
					day.min += "&deg;C";
				} else {
					day.max += "&deg;F";
					day.min += "&deg;F";
				}

				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	static String[] processMETIE(String data)
	{
		return processMETIE(data, false);
	}

	static String[] processMETIE(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);
		boolean use_icons = GetBoolPref("useIcons", weeWXApp.useIcons_default);
		long timestamp = GetLongPref("rssCheck", 0) * 1_000L;

		List<Day> days = new ArrayList<>();
		String desc;

		try
		{
			desc = GetStringPref("metierev", weeWXApp.metierev_default);

			SimpleDateFormat dayname = new SimpleDateFormat("EEEE d", Locale.getDefault());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
			JSONArray jarr = new JSONArray(data);
			for(int i = 0; i < jarr.length(); i++)
			{
				JSONObject jobj = jarr.getJSONObject(i);
				Day day = new Day();
				String tmpDay = jobj.getString("date") + " " + jobj.getString("time");
				day.timestamp = 0;
				Date df = sdf.parse(tmpDay);
				if(df != null)
					day.timestamp = df.getTime();

				day.day = dayname.format(day.timestamp);
				day.max = jobj.getString("temperature");
				day.icon = jobj.getString("weatherNumber");

				if(!use_icons)
				{
					day.icon = "wi wi-met-ie-" + day.icon;
				} else {
					String fileName = checkImage("y" + day.icon + ".png", null);
					if(fileName == null)
						return null;

					File f = new File(fileName);
					try(FileInputStream imageInFile = new FileInputStream(f))
					{
						byte[] imageData = new byte[(int) f.length()];
						if(imageInFile.read(imageData) > 0)
							day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
					} catch(Exception e) {
						doStackOutput(e);
					}
				}

				day.text = jobj.getString("weatherDescription");

				if(metric)
					day.max += "&deg;C";
				else
					day.max = Math.round((Double.parseDouble(day.max) * 9.0 / 5.0) + 32.0) + "&deg;F";

				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	static String[] processOWM(String data) { return processOWM(data, false); }

	static String[] processOWM(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		List<Day> days = new ArrayList<>();
		String desc;

		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);
		long timestamp = GetLongPref("rssCheck", 0) * 1_000L;

		try
		{
			JSONObject jobj = new JSONObject(data);
			desc = jobj.getJSONObject("city").getString("name") + ", " + jobj.getJSONObject("city").getString("country");
			JSONArray jarr = jobj.getJSONArray("list");
			SimpleDateFormat sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
			for(int i = 0; i < jarr.length(); i++)
			{
				Day day = new Day();
				JSONObject j = jarr.getJSONObject(i);
				day.timestamp = j.getLong("dt") * 1_000L;
				day.day = sdf.format(day.timestamp);

				JSONObject temp = j.getJSONObject("temp");
				int min = (int)Math.round(Double.parseDouble(temp.getString("min")));
				int max = (int)Math.round(Double.parseDouble(temp.getString("max")));
				JSONObject weather = j.getJSONArray("weather").getJSONObject(0);

				int id = weather.getInt("id");
				String text = weather.getString("description");
				String icon = weather.getString("icon");

				if(!icon.endsWith("n"))
					day.icon = "wi wi-owm-day-" + id;
				else
					day.icon = "wi wi-owm-night-" + id;

				if(metric)
					day.max = max + "&deg;C";
				else
					day.max = max + "&deg;F";
				day.text = text;

				if(metric)
					day.min = min + "&deg;C";
				else
					day.min = min + "&deg;F";
				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	static String[] processYR(String data)
	{
		return processYR(data, false);
	}

	static String[] processYR(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		boolean use_icons = GetBoolPref("useIcons", weeWXApp.useIcons_default);
		List<Day> days = new ArrayList<>();
		String desc;
		long timestamp;

		try
		{
			JSONObject jobj = new XmlToJson.Builder(data).build().toJson();
			if(jobj == null)
				return null;

			jobj = jobj.getJSONObject("weatherdata");
			JSONObject location = jobj.getJSONObject("location");
			desc = location.getString("name") + ", " + location.getString("country");

			timestamp = GetLongPref("rssCheck", 0) * 1_000L;

			JSONArray jarr = jobj.getJSONObject("forecast")
					.getJSONObject("tabular")
					.getJSONArray("time");

			for(int i = 0; i < jarr.length(); i++)
			{
				Day day = new Day();
				String from = jarr.getJSONObject(i).getString("from");
				String to = jarr.getJSONObject(i).getString("to");
				//String period = jarr.getJSONObject(i).getString("period");
				JSONObject symbol = jarr.getJSONObject(i).getJSONObject("symbol");
				JSONObject precipitation = jarr.getJSONObject(i).getJSONObject("precipitation");
				JSONObject temperature = jarr.getJSONObject(i).getJSONObject("temperature");
				JSONObject windDirection = jarr.getJSONObject(i).getJSONObject("windDirection");
				JSONObject windSpeed = jarr.getJSONObject(i).getJSONObject("windSpeed");

				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
				day.timestamp = 0;
				Date df = sdf.parse(from);
				if(df != null)
					day.timestamp = df.getTime();

				sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
				String date = sdf.format(day.timestamp);

				sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
				from = sdf.format(day.timestamp);

				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
				long mdate = 0;
				df = sdf.parse(to);
				if(df != null)
					mdate = df.getTime();

				sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
				to = sdf.format(mdate);

				day.day = date + ": " + from + "-" + to;

				String code = symbol.getString("var");

				if(!use_icons)
				{
					day.icon = "wi wi-yrno-" + code;
				} else {
					String fileName = checkImage("yrno" + code + ".png", null);
					if(fileName == null)
						return null;

					File f = new File(fileName);
					try(FileInputStream imageInFile = new FileInputStream(f))
					{
						byte[] imageData = new byte[(int) f.length()];
						if(imageInFile.read(imageData) > 0)
							day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
					} catch(Exception e) {
						doStackOutput(e);
					}
				}

				day.max = temperature.getString("value") + "&deg;C";
				day.min = precipitation.getString("value") + "mm";
				day.text = windSpeed.getString("name") + ", " + windSpeed.get("mps") + "m/s from the " + windDirection.getString("name");
				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	static String[] processMetNO(String data)
	{
		return processMetNO(data, false);
	}

	static String[] processMetNO(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);
		boolean use_icons = GetBoolPref("useIcons", weeWXApp.useIcons_default);
		List<Day> days = new ArrayList<>();
		String desc = "";
		long timestamp;

		try
		{
//            desc = location.getString("name") + ", " + location.getString("country");

			JSONObject jobj = new JSONObject(data);
			jobj = jobj.getJSONObject("properties");
			String updated_at = jobj.getJSONObject("meta").getString("updated_at");
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

			timestamp = 0;
			Date df = sdf.parse(updated_at);
			if(df != null)
				timestamp = df.getTime();

			JSONArray jarr = jobj.getJSONArray("timeseries");

			for(int i = 0; i < jarr.length(); i++)
			{
				Day day = new Day();
				String time = jarr.getJSONObject(i).getString("time");

				day.timestamp = 0;
				df = sdf.parse(time);
				if(df != null)
					day.timestamp = df.getTime();

				JSONObject tsdata = jarr.getJSONObject(i).getJSONObject("data");
				double temp = tsdata.getJSONObject("instant").getJSONObject("details").getDouble("air_temperature");
				if(metric)
					day.max = Math.round(temp) + "&deg;C";
				else
					day.max = Math.round((temp * 9.0 / 5.0) + 32.0) + "&deg;F";

				String icon;

				try
				{
					try
					{
						double precip = tsdata.getJSONObject("next_1_hours").getJSONObject("details").getDouble("precipitation_amount");
						if(metric)
							day.min = precip + "mm";
						else
							day.min = (Math.round(precip / 2_540D) / 100D) + "in";

						icon = tsdata.getJSONObject("next_1_hours").getJSONObject("summary").getString("symbol_code");
					} catch(Exception e) {
						double precip = tsdata.getJSONObject("next_6_hours").getJSONObject("details").getDouble("precipitation_amount");
						if(metric)
							day.min = precip + "mm";
						else
							day.min = (Math.round(precip / 2_540D) / 100D) + "in";

						icon = tsdata.getJSONObject("next_6_hours").getJSONObject("summary").getString("symbol_code");
					}
				} catch(Exception e) {
					continue;
				}

				double windSpeed = tsdata.getJSONObject("instant").getJSONObject("details").getDouble("wind_speed");
				double windDir = tsdata.getJSONObject("instant").getJSONObject("details").getDouble("wind_from_direction");
				if(metric)
					day.text = windSpeed + "m/s from the " + degtoname(windDir);
				else
					day.text = (Math.round(windSpeed * 22.36936) / 10.0) + "mph from the " + degtoname(windDir);

				SimpleDateFormat sdf2 = new SimpleDateFormat("EEEE d, HH:mm", Locale.getDefault());
				day.day = sdf2.format(day.timestamp);

				LogMessage("time == " + time);
				LogMessage("icon == " + icon);
				String code = getCode(icon);

				if(!use_icons)
				{
					day.icon = "wi wi-yrno-" + code;
				} else {
					String fileName = checkImage("yrno" + code + ".png", null);
					if(fileName == null)
						return null;

					File f = new File(fileName);
					try(FileInputStream imageInFile = new FileInputStream(f))
					{
						byte[] imageData = new byte[(int) f.length()];
						if(imageInFile.read(imageData) > 0)
							day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
					} catch(Exception e) {
						doStackOutput(e);
					}
				}

				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	private static String degtoname(double deg)
	{
		if(deg <= 22.5)
			return "North";
		else if(deg <= 67.5)
			return "North East";
		else if(deg <= 112.5)
			return "East";
		else if(deg <= 157.5)
			return "South East";
		else if(deg <= 202.5)
			return "South";
		else if(deg <= 247.5)
			return "South West";
		else if(deg <= 292.5)
			return "West";
		else if(deg <= 337.5)
			return "North West";
		return "North";
	}

	private static String getCode(String icon)
	{
		return switch (icon)
				{
					case "clearsky_night" -> "01n";
					case "clearsky_polartwilight" -> "01m";
					case "fair_day" -> "02d";
					case "fair_night" -> "02n";
					case "fair_polartwilight" -> "02m";
					case "partlycloudy_day" -> "03d";
					case "partlycloudy_night" -> "03n";
					case "partlycloudy_polartwilight" -> "03m";
					case "cloudy" -> "04";
					case "rainshowers_day" -> "05d";
					case "rainshowers_night" -> "05n";
					case "rainshowers_polartwilight" -> "05m";
					case "rainshowersandthunder_day" -> "06d";
					case "rainshowersandthunder_night" -> "06n";
					case "rainshowersandthunder_polartwilight" -> "06m";
					case "sleetshowers_day" -> "07d";
					case "sleetshowers_night" -> "07n";
					case "sleetshowers_polartwilight" -> "07m";
					case "snowshowers_day" -> "08d";
					case "snowshowers_night" -> "08n";
					case "snowshowers_polartwilight" -> "08m";
					case "rain" -> "09";
					case "heavyrain" -> "10";
					case "heavyrainandthunder" -> "11";
					case "sleet" -> "12";
					case "snow" -> "13";
					case "snowandthunder" -> "14";
					case "fog" -> "15";
					case "sleetshowersandthunder_day" -> "20d";
					case "sleetshowersandthunder_night" -> "20n";
					case "sleetshowersandthunder_polartwilight" -> "20m";
					case "snowshowersandthunder_day" -> "21d";
					case "snowshowersandthunder_night" -> "21n";
					case "snowshowersandthunder_polartwilight" -> "21m";
					case "rainandthunder" -> "22";
					case "sleetandthunder" -> "23";
					case "lightrainshowersandthunder_day" -> "24d";
					case "lightrainshowersandthunder_night" -> "24n";
					case "lightrainshowersandthunder_polartwilight" -> "24m";
					case "heavyrainshowersandthunder_day" -> "25d";
					case "heavyrainshowersandthunder_night" -> "25n";
					case "heavyrainshowersandthunder_polartwilight" -> "25m";
					case "lightssleetshowersandthunder_day" -> "26d";
					case "lightssleetshowersandthunder_night" -> "26n";
					case "lightssleetshowersandthunder_polartwilight" -> "26m";
					case "heavysleetshowersandthunder_day" -> "27d";
					case "heavysleetshowersandthunder_night" -> "27n";
					case "heavysleetshowersandthunder_polartwilight" -> "27m";
					case "lightssnowshowersandthunder_day" -> "28d";
					case "lightssnowshowersandthunder_night" -> "28n";
					case "lightssnowshowersandthunder_polartwilight" -> "28m";
					case "heavysnowshowersandthunder_day" -> "29d";
					case "heavysnowshowersandthunder_night" -> "29n";
					case "heavysnowshowersandthunder_polartwilight" -> "29m";
					case "lightrainandthunder" -> "30";
					case "lightsleetandthunder" -> "31";
					case "heavysleetandthunder" -> "32";
					case "lightsnowandthunder" -> "33";
					case "heavysnowandthunder" -> "34";
					case "lightrainshowers_day" -> "40d";
					case "lightrainshowers_night" -> "40n";
					case "lightrainshowers_polartwilight" -> "40m";
					case "heavyrainshowers_day" -> "41d";
					case "heavyrainshowers_night" -> "41n";
					case "heavyrainshowers_polartwilight" -> "41m";
					case "lightsleetshowers_day" -> "42d";
					case "lightsleetshowers_night" -> "42n";
					case "lightsleetshowers_polartwilight" -> "42m";
					case "heavysleetshowers_day" -> "43d";
					case "heavysleetshowers_night" -> "43n";
					case "heavysleetshowers_polartwilight" -> "43m";
					case "lightsnowshowers_day" -> "44d";
					case "lightsnowshowers_night" -> "44n";
					case "lightsnowshowers_polartwilight" -> "44m";
					case "heavysnowshowers_day" -> "45d";
					case "heavysnowshowers_night" -> "45n";
					case "heavysnowshowers_polartwilight" -> "45m";
					case "lightrain" -> "46";
					case "lightsleet" -> "47";
					case "heavysleet" -> "48";
					case "lightsnow" -> "49";
					case "heavysnow" -> "50";
					default -> "01d";
				};

	}

	private static long convertDaytoTS(String dayName, Locale locale, long lastTS)
	{
		long startTS = lastTS;

		dayName = HtmlCompat.fromHtml(dayName, HtmlCompat.FROM_HTML_MODE_LEGACY).toString();

		while(true)
		{
			SimpleDateFormat sdf = new SimpleDateFormat("EEEE", locale);
			String string_time = sdf.format(lastTS);
			//LogMessage("string_time == " + string_time + ", dayName == " + dayName);
			if(dayName.toLowerCase(Locale.ENGLISH).startsWith(string_time.toLowerCase(Locale.ENGLISH)))
				return lastTS;

			lastTS += 86_400_000L;

			if(lastTS > startTS + 86_400_000L)
				return 0;
		}
	}

	static String[] processWZ(String data)
	{
		return processWZ(data, false);
	}

	static String[] processWZ(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		boolean use_icons = GetBoolPref("useIcons", weeWXApp.useIcons_default);
		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);
		List<Day> days = new ArrayList<>();
		long timestamp = 0;
		long lastTS = 0;
		String desc;

		try
		{
			JSONObject jobj = new XmlToJson.Builder(data).build().toJson();
			if(jobj == null)
				return null;

			jobj = jobj.getJSONObject("rss").getJSONObject("channel");
			desc = jobj.getString("title");
			String pubDate = jobj.getString("pubDate");

			SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault());
			Date df = sdf.parse(pubDate);
			if(df != null)
				lastTS = timestamp = df.getTime();

			JSONObject item = jobj.getJSONArray("item").getJSONObject(0);
			String[] items = item.getString("description").trim().split("<b>");
			for (String i : items)
			{
				Day day = new Day();
				String[] tmp = i.split("</b>", 2);
				Locale locale = new Locale.Builder().setLanguage("en").setRegion("AU").build();
				day.timestamp = convertDaytoTS(day.day, locale, lastTS);
				if(day.timestamp != 0)
				{
					sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
					day.day = sdf.format(day.timestamp);
				} else {
					day.day = tmp[0];
				}

				if(tmp.length == 1)
					continue;

				String[] mybits = tmp[1].split("<br />");
				String myimg = mybits[1].trim().replaceAll("<img src='https://www.weatherzone.com.au/images/icons/fcast_30/", "")
									.replaceAll("'>", "").replaceAll(".gif", "").replaceAll("_", "-").trim();
				String mydesc = mybits[2].trim();
				String[] range = mybits[3].split(" - ", 2);

				if(!use_icons)
				{
					if(!myimg.equals("frost-then-sunny"))
						day.icon = "wi wi-weatherzone-" + myimg;
					else
						day.icon = "flaticon-thermometer";
				} else {
					String fileName = "wz" + myimg.replaceAll("-", "_") + ".png";
					fileName = checkImage(fileName, null);
					if(fileName == null)
						return null;

					File f = new File(fileName);
					try(FileInputStream imageInFile = new FileInputStream(f))
					{
						byte[] imageData = new byte[(int) f.length()];
						if(imageInFile.read(imageData) > 0)
							day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
					} catch(Exception e) {
						doStackOutput(e);
					}
				}

				day.max = range[1];
				day.min = range[0];
				if(!metric)
				{
					day.max = Math.round(Double.parseDouble(range[1].substring(0, range[1].length() - 7)) * 9.0 / 5.0 + 32.0) + "&deg;F";
					day.min = Math.round((Double.parseDouble(range[0].substring(0, range[0].length() - 7)) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}
				day.text = mydesc;
				days.add(day);
				lastTS = day.timestamp;
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	private static String[] checkFiles(String url) throws Exception
	{
		String filename = "yahoo-" + new File(url).getName();
		File f = new File(getExtDir("icons"), filename);
		if(!f.exists())
		{
			LogMessage("File not found: " + "yahoo-" + filename);
			LogMessage("downloading...");

			String new_url = "https://delungra.com/weewx/yahoo-missing.php?filename=" + filename;
			if(!downloadToFile(f, new_url))
				return new String[]{null, "f is invalid."};

			if(f.exists())
				return new String[]{"", f.getAbsolutePath()};
		} else {
			LogMessage(filename);
			return new String[]{"", f.getAbsolutePath()};
		}

		return new String[]{null, "Failed to load or download icon: " + filename};
	}

	private static String[] checkFilesIt(String url) throws Exception
	{
		String filename = "tempoitalia-" + new File(url).getName();
		File f = new File(getExtDir("icons"), filename);
		if(!f.exists())
		{
			LogMessage("File not found: " + "tempoitalia-" + filename);
			LogMessage("downloading...");

			String new_url = "https://delungra.com/weewx/TempoItalia-missing.php?filename=" + filename;
			if(!downloadToFile(f, new_url))
				return new String[]{null, "f is invalid."};

			if(f.exists())
				return new String[]{"", f.getAbsolutePath()};
		} else {
			//LogMessage(filename);
			return new String[]{"", f.getAbsolutePath()};
		}

		return new String[]{null, "Failed to load or download icon: " + filename};
	}

	static String[] processYahoo(String data)
	{
		return processYahoo(data, false);
	}

	static String[] processYahoo(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		boolean metric = GetBoolPref("metric", weeWXApp.metric_default);

		List<Day> days = new ArrayList<>();
		long timestamp = GetLongPref("rssCheck", 0) * 1_000L;
		String desc;
		Document doc;

		try
		{
			String[] bits = data.split("data-reactid='7'>", 8);
			String[] b = bits[7].split("</h1>", 2);
			String town = b[0];
			String rest = b[1];
			b = rest.split("data-reactid='8'>", 2)[1].split("</div>", 2);
			String country = b[0];
			rest = b[1];

			desc = town.trim() + ", " + country.trim();
			int[] daynums = new int[]{196, 221, 241, 261, 281, 301, 321, 341, 361, 381};
			for (int startid : daynums)
			{
				Day myday = new Day();

				int endid;
				long last_ts = System.currentTimeMillis();

				if(startid == 196)
					endid = startid + 24;
				else
					endid = startid + 19;

				String tmpstr = rest.split("<span data-reactid='" + startid + "'>", 2)[1];
				bits = tmpstr.split("data-reactid='" + endid + "'>", 2);
				tmpstr = bits[0];
				rest = bits[1];
				String dow = tmpstr.split("</span>", 2)[0].trim();
				Locale locale = new Locale.Builder().setLanguage("en").setRegion("US").build();
				myday.timestamp = convertDaytoTS(dow, locale, last_ts);
				SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
				myday.day = sdf.format(myday.timestamp);

				myday.text = tmpstr.split("<img alt='", 2)[1].split("'", 2)[0].trim();
				myday.icon = tmpstr.split("<img alt='", 2)[1].split("'", 2)[1];
				myday.icon = myday.icon.split("src='", 2)[1].split("'", 2)[0].trim();

				myday.max = tmpstr.split("data-reactid='" + (startid + 10) + "'>", 2)[1];
				myday.max = myday.max.split("</span>", 2)[0].trim();
				myday.min = tmpstr.split("data-reactid='" + (startid + 13) + "'>", 2)[1];
				myday.min = myday.min.split("</span>", 2)[0].trim();

				doc = Jsoup.parse(myday.max.trim());
				myday.max = doc.text();

				doc = Jsoup.parse(myday.min.trim());
				myday.min = doc.text();

				myday.max = myday.max.substring(0, myday.max.length() - 1);
				myday.min = myday.min.substring(0, myday.min.length() - 1);

				if(metric)
				{
					myday.max = Math.round((Double.parseDouble(myday.max) - 32.0) * 5.0 / 9.0) + "&deg;C";
					myday.min = Math.round((Double.parseDouble(myday.min) - 32.0) * 5.0 / 9.0) + "&deg;C";
				} else
				{
					myday.max += "&deg;F";
					myday.min += "&deg;F";
				}

				String[] ret = checkFiles(myday.icon);
				if(ret[0] != null)
				{
					File f = new File(ret[1]);
					try(FileInputStream imageInFile = new FileInputStream(f))
					{
						byte[] imageData = new byte[(int) f.length()];
						if(imageInFile.read(imageData) > 0)
							myday.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
					} catch(Exception e) {
						doStackOutput(e);
					}
				} else
					return ret;

				LogMessage(myday.toString());
				days.add(myday);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	static void SendWeatherForecastRefreshIntent()
	{
		NotificationManager.updateNotificationMessage(REFRESH_WEATHER_FORECAST_INTENT);
		LogMessage("SendWeatherForecastRefreshIntent() Broadcasted");
	}

	static void SendForecastRefreshIntent()
	{
		String caller = new Exception().getStackTrace()[1].getClassName();
		NotificationManager.updateNotificationMessage(REFRESH_FORECAST_INTENT);
		LogMessage("Refresh forecast intent broadcasted by " + caller);
	}

	static void SendRadarRefreshIntent()
	{
		WidgetProvider.updateAppWidget();
		NotificationManager.updateNotificationMessage(REFRESH_RADAR_INTENT);
		LogMessage("Refresh radar intent broadcasted");
	}

	static void SendWeatherRefreshIntent()
	{
		WidgetProvider.updateAppWidget();
		NotificationManager.updateNotificationMessage(REFRESH_WEATHER_INTENT);
		LogMessage("Refresh weather intent broadcasted");
	}

	static void SendWebcamRefreshIntent()
	{
		WidgetProvider.updateAppWidget();
		NotificationManager.updateNotificationMessage(REFRESH_WEBCAM_INTENT);
		LogMessage("Refresh weather intent broadcasted");
	}

	static String[] getWeather(boolean forced)
	{
		long current_time = getCurrTime();

		String baseURL = GetStringPref("BASE_URL", weeWXApp.BASE_URL_default);
		if(baseURL == null || baseURL.isBlank())
			return new String[]{"error", weeWXApp.getAndroidString(R.string.data_url_was_blank)};

		String lastDownload = GetStringPref("LastDownload", weeWXApp.LastDownload_default);
		if(!forced && (lastDownload == null || lastDownload.isBlank()))
		{
			LogMessage("lastDownload is null or blank...");

			if(!checkConnection())
				return new String[]{"error", weeWXApp.getAndroidString(R.string.wifi_not_available)};
		}

		long lastDownloadTime = GetLongPref("LastDownloadTime", weeWXApp.LastDownloadTime_default);
		long[] ret = getPeriod();
		long period = Math.round(ret[0] / 1_000D);

		if(!forced && current_time < lastDownloadTime + period && lastDownload != null && !lastDownload.isBlank())
			return new String[]{"ok", lastDownload};

		if(weatherTask != null && !weatherTask.isDone())
		{
			if(wtStart + 30 > current_time)
			{
				LogMessage("weatherTask is less than 30s old (" + (current_time - wtStart) +
				                          "s), we'll skip this attempt...");
				return new String[]{"ok", lastDownload};
			}

			LogMessage("weatherTask was more than 30s old (" +
			           (current_time - wtStart) + "s) cancelling and restarting...");

			weatherTask.cancel(true);
		}

		LogMessage("Creating a weatherTask...");

		wtStart = current_time;

		weatherTask = executor.submit(() ->
		{
			LogMessage("Weather checking: " + baseURL);

			String tmpForecastData = null;

			try
			{
				if(reallyGetWeather(baseURL))
					SendWeatherRefreshIntent();
			} catch(Exception ignored) {}

			wtStart = 0;
		});

		return new String[]{"ok", lastDownload};
	}

	static boolean reallyGetWeather(String url) throws IOException
	{
		weeWXAppCommon.LogMessage("url: " + url);
		String line = downloadString(url);
		weeWXAppCommon.LogMessage("Got output, line: " + line);
		if(!line.isBlank() && line.contains("|"))
		{
			String[] parts = line.split("\\|", 2);
			int version = (int)Float.parseFloat(parts[0]);
			if(version < weeWXApp.minimum_inigo_version || parts.length < 2)
			{
				sendAlert();
				return false;
			}

			line = parts[1];

			SetStringPref("LastDownload", line);
			SetLongPref("LastDownloadTime", getCurrTime());
			return true;
		}

		return false;
	}

	//    https://stackoverflow.com/questions/3841317/how-do-i-see-if-wi-fi-is-connected-on-android
	static boolean checkConnection()
	{
		if(!GetBoolPref("onlyWIFI", weeWXApp.onlyWIFI_default))
			return true;

		ConnectivityManager connMgr = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		if(connMgr == null)
			return false;

		Network network = connMgr.getActiveNetwork();
		if(network == null)
			return false;

		NetworkCapabilities capabilities = connMgr.getNetworkCapabilities(network);
		return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
	}

	private static void sendAlert()
	{
		NotificationManager.updateNotificationMessage(INIGO_INTENT);
		LogMessage("Send user note about upgrading the Inigo Plugin");
	}

	// https://stackoverflow.com/questions/8710515/reading-an-image-file-into-bitmap-from-sdcard-why-am-i-getting-a-nullpointerexc

	private static String checkImage(String fileName, String icon) throws IOException
	{
		File f = new File(getExtDir("icons"), fileName);
		LogMessage("f = " + f.getAbsolutePath());
		if(f.exists() && f.isFile() && f.canRead() && f.length() > 0)
			return f.getAbsolutePath();

		// Icon is missing we should probably ask to send information as feedback
		if(icon != null)
			if(!downloadToFile(fileName, icon))
				throw new IOException("Failed to download " + fileName);

		return icon;
	}

	private static Bitmap combineImage(Bitmap bmp1, String fnum, String snum)
	{
		Context context = getApplicationContext();

		try
		{
			int x1 = bmp1.getHeight();
			int y1 = bmp1.getWidth();

			Bitmap bmp = bmp1.copy(Bitmap.Config.ARGB_8888, true);

			Paint paint = new Paint();
			paint.setAntiAlias(true);
			Canvas comboImage = new Canvas(bmp);
			comboImage.drawBitmap(bmp1, 0f, 0f, null);

			if(!fnum.equals("%") || !snum.equals("%"))
			{
				Bitmap bmp2 = loadImage("wgovoverlay.jpg");
				if(bmp2 == null)
					return null;

				paint = new Paint();
				paint.setAlpha(100);
				comboImage.drawBitmap(bmp2, 0f, bmp1.getHeight() - bmp2.getHeight(), paint);
			}

			if(!fnum.equals("%") && !snum.equals("%"))
			{
				// Draw arrow
				paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(ContextCompat.getColor(context, R.color.LightPrussianBlue));
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth(1);
				comboImage.drawLine( Math.round(x1 / 2.0) + 5, y1 - 9, Math.round(x1 / 2.0) + 10, y1 - 7, paint);
				comboImage.drawLine( Math.round(x1 / 2.0) - 10, y1 - 7, Math.round(x1 / 2.0) + 10, y1 - 7, paint);
				comboImage.drawLine( Math.round(x1 / 2.0) + 5, y1 - 5, Math.round(x1 / 2.0) + 10, y1 - 7, paint);
			}

			if(!fnum.equals("%"))
			{
				paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(ContextCompat.getColor(context, R.color.LightPrussianBlue));
				paint.setTextSize(13);
				paint.setTypeface(tf_bold);

				comboImage.drawText(fnum, 0, y1 - 2, paint);
			}

			if(!snum.equals("%"))
			{
				paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(ContextCompat.getColor(context, R.color.LightPrussianBlue));
				paint.setTextSize(13);
				paint.setTypeface(tf_bold);

				Rect textBounds = new Rect();
				paint.getTextBounds(snum, 0, snum.length(), textBounds);
				int width = (int) paint.measureText(snum);

				comboImage.drawText(snum, x1 - width, y1 - 2, paint);
			}

			return bmp;
		} catch(Exception e) {
			doStackOutput(e);
		}

		return null;
	}

	private static Bitmap combineImages(Bitmap bmp1, Bitmap bmp2, String fimg, String simg, String fnum, String snum)
	{
		Context context = getApplicationContext();

		try
		{
			int x1 = bmp1.getHeight();
			int y1 = bmp1.getWidth();

			int x2 = bmp2.getHeight();
			int y2 = bmp2.getWidth();

			if(nws == null)
			{
				loadNWS();
				LogMessage("Loaded NWS");
			}

			String[] bits = nws.getString(fimg).split("\\|");
			switch (bits[1].trim())
			{
				case "L" -> bmp1 = Bitmap.createBitmap(bmp1, 0, 0, x1 / 2, y1);
				case "R" -> bmp1 = Bitmap.createBitmap(bmp1, x1 / 2, 0, x1, y1);
				default -> bmp1 = Bitmap.createBitmap(bmp1, x1 / 4, 0, x1 * 3 / 4, y1);
			}

			bits = nws.getString(simg).split("\\|");
			switch (bits[1].trim())
			{
				case "L" -> bmp2 = Bitmap.createBitmap(bmp2, 0, 0, x2 / 2, y2);
				case "R" -> bmp2 = Bitmap.createBitmap(bmp2, x2 / 2, 0, x2, y2);
				default -> bmp2 = Bitmap.createBitmap(bmp2, x2 / 4, 0, x2 * 3 / 4, y2);
			}

			Bitmap bmp = Bitmap.createBitmap(x1, y1, Bitmap.Config.ARGB_8888);
			bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);

			Paint paint = new Paint();
			paint.setAntiAlias(true);
			Canvas comboImage = new Canvas(bmp);
			comboImage.drawBitmap(bmp1, 0f, 0f, null);
			comboImage.drawBitmap(bmp2, Math.round(x1 / 2.0), 0f, null);

			paint.setColor(ContextCompat.getColor(context, R.color.Black));
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(4);
			comboImage.drawLine( Math.round(x1 / 2.0), 0, Math.round(x1 / 2.0), y1, paint);

			paint.setColor(ContextCompat.getColor(context, R.color.White));
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(2);
			comboImage.drawLine( Math.round(x1 / 2.0), 0, Math.round(x1 / 2.0), y1, paint);

			if(!fnum.equals("%") || !snum.equals("%"))
			{
				Bitmap bmp4 = loadImage("wgovoverlay.jpg");
				if(bmp4 == null)
					return null;

				paint = new Paint();
				paint.setAlpha(100);
				comboImage.drawBitmap(bmp4, 0f, bmp1.getHeight() - bmp4.getHeight(), paint);
			}

			if(!fnum.equals("%") && !snum.equals("%"))
			{
				// Draw arrow
				paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(ContextCompat.getColor(context, R.color.LightPrussianBlue));
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth(1);
				comboImage.drawLine( Math.round(x1 / 2.0) + 5, y1 - 9, Math.round(x1 / 2.0) + 10, y1 - 7, paint);
				comboImage.drawLine( Math.round(x1 / 2.0) - 10, y1 - 7, Math.round(x1 / 2.0) + 10, y1 - 7, paint);
				comboImage.drawLine( Math.round(x1 / 2.0) + 5, y1 - 5, Math.round(x1 / 2.0) + 10, y1 - 7, paint);
			}

			if(!fnum.equals("%"))
			{
				paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(ContextCompat.getColor(context, R.color.LightPrussianBlue));
				paint.setTextSize(13);
				paint.setTypeface(tf_bold);

				comboImage.drawText(fnum, 2, y1 - 2, paint);
			}

			if(!snum.equals("%"))
			{
				paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(ContextCompat.getColor(context, R.color.LightPrussianBlue));
				paint.setTextSize(13);
				paint.setTypeface(tf_bold);

				Rect textBounds = new Rect();
				paint.getTextBounds(snum, 0, snum.length(), textBounds);
				int width = (int) paint.measureText(snum);

				comboImage.drawText(snum, x1 - width - 2, y1 - 2, paint);
			}

			return bmp;
		} catch(Exception e) {
			doStackOutput(e);
		}

		return null;
	}

	// https://stackoverflow.com/questions/1540272/android-how-to-overlay-a-bitmap-draw-over-a-bitmap

	private static Bitmap overlay(Bitmap bmp1, Bitmap bmp2, String s)
	{
		Paint paint = new Paint();
		paint.setAlpha(100);

		Bitmap.Config btmp = bmp1.getConfig();
		if(btmp == null)
			btmp = Bitmap.Config.ARGB_8888;

		Bitmap bmp3 = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), btmp);
		Canvas canvas = new Canvas(bmp3);
		canvas.drawBitmap(bmp1, 0f, 0f, null);
		canvas.drawBitmap(bmp2, 0f, bmp1.getHeight() - bmp2.getHeight(), paint);
		paint.setAntiAlias(true);
		paint.setColor(ContextCompat.getColor(getApplicationContext(), R.color.LightPrussianBlue));
		paint.setTextSize(13);
		paint.setTypeface(tf_bold);
		Rect textBounds = new Rect();
		paint.getTextBounds(s, 0, s.length(), textBounds);
		int width = (int)paint.measureText(s);

		canvas.drawText(s, bmp1.getWidth() - width, bmp1.getHeight() - 5, paint);

		return bmp3;
	}

	// https://stackoverflow.com/questions/19945411/android-java-how-can-i-parse-a-local-json-file-from-assets-folder-into-a-listvi

	private static void loadNWS()
	{
		try
		{
			InputStream is = weeWXApp.openRawResource(R.raw.nws);
			int size = is.available();
			byte[] buffer = new byte[size];
			if(is.read(buffer) > 0)
				nws = new JSONObject(new String(buffer, StandardCharsets.UTF_8));
			is.close();
		} catch(Exception e) {
			doStackOutput(e);
		}
	}

	static String downloadSettings(String url) throws IOException
	{
		String UTF8_BOM = "\uFEFF";

		SetStringPref("SETTINGS_URL", url);

		String cfg = downloadString(url);

		if(cfg.startsWith(UTF8_BOM))
			cfg = cfg.substring(1).trim();

		return cfg;
	}

	static boolean checkURL(String url) throws IOException
	{
		LogMessage("checking if url  " + url + " is valid...");
		if(url == null || url.isBlank())
			return false;

		OkHttpClient client = NetworkClient.getInstance(url);
		Request request = NetworkClient.newRequest(true, url);

		try(Response response = client.newCall(request).execute())
		{
			return response.isSuccessful();
		}
	}

	static String downloadString(String url) throws IOException
	{
		LogMessage("downloading text from " + url);
		OkHttpClient client = NetworkClient.getInstance(url);
		Request request = NetworkClient.newRequest(url);

		try(Response response = client.newCall(request).execute())
		{
			if(!response.isSuccessful())
				throw new IOException("HTTP error " + response);

			return response.body().string();
		}
	}

	private static void writeFile(String fileName, String data) throws Exception
	{
		File dir = getDir("weeWX");
		if(!dir.exists())
			if(!dir.mkdirs())
				return;

		File f = new File(dir, fileName);

		OutputStream outputStream = new FileOutputStream(f);
		outputStream.write(data.getBytes());
		outputStream.flush();
		outputStream.close();

		publish(f);
	}

	private static void publish(File f)
	{
		LogMessage("wrote to " + f.getAbsolutePath());
		if(f.exists())
			MediaScannerConnection.scanFile(getApplicationContext(),
					new String[]{f.getAbsolutePath()}, null, null);
	}

	static boolean downloadIcons() throws IOException
	{
		File dir = getExtDir("icons");

		if(!dir.exists() && !dir.mkdirs())
			throw new IOException("There was a problem making the icons directory, you will need to try again.");

		if(!dir.exists() && dir.mkdirs())
			publish(dir);

		File f = new File(dir, "icon.zip");
		if(!downloadToFile(f, icon_url))
			throw new IOException("There was a problem downloading icons, you will need to try again.");

		if(!dir.exists())
			throw new IOException("There was a problem making the icons directory, you will need to try again.");

		unzip(f, dir);
		if(!f.delete())
			throw new IOException("There was a problem cleaning up the zip file.");

		return true;
	}

	private static void unzip(File zipFilePath, File destDir) throws IOException
	{
		LogMessage("dsetDir: " + destDir.getAbsolutePath());
		byte[] buffer = new byte[1024];
		FileInputStream fis = new FileInputStream(zipFilePath);
		ZipInputStream zis = new ZipInputStream(fis);
		ZipEntry ze = zis.getNextEntry();
		while(ze != null)
		{
			String fileName = ze.getName();
			File newFile = new File(destDir, fileName);
			String canonicalPath = newFile.getCanonicalPath();
			if(!canonicalPath.startsWith(destDir.toString()))
			{
				LogMessage("File '" + canonicalPath + "' is a security problem, skipping.");
				continue;
			}
			LogMessage("Unzipping to " + newFile.getAbsolutePath());
			if(newFile.getParent() != null)
			{
				File dir = new File(newFile.getParent());
				if(!dir.exists() && !dir.mkdirs())
					throw new IOException("There was a problem creating 1 or more directories on external storage");

				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
				while((len = zis.read(buffer)) > 0)
					fos.write(buffer, 0, len);
				fos.flush();
				fos.close();

				publish(newFile);
				//close this ZipEntry
				zis.closeEntry();
				ze = zis.getNextEntry();
			}
		}

		zis.closeEntry();
		zis.close();
		fis.close();
	}

	static boolean checkForImages(String filename)
	{
		try
		{
			File dir = getFilesDir();
			File f = new File(dir, filename);
			return f.exists() && f.isFile() && f.canRead();
		} catch(Exception ignored) {}

		return false;
	}

	static boolean checkForIcons()
	{
		String[] files = new String[]{"aemet_11_g.png", "apixu_113.png", "bom1.png", "bom2clear.png", "dwd_pic_0_8.png",
										"i1.png", "ilmeteo_ss1.png", "met0.png", "mf_j_w1_0_n_2.png", "ms_cloudy.png",
										"wca00.png", "wgovbkn.jpg", "wzclear.png", "y01d.png", "yahoo0.gif", "yrno01d.png"};

		try
		{
			File dir = getExtDir("icons");

			for(String file: files)
			{
				File f = new File(dir, file);
				if(!f.exists() || !f.isFile() || !f.canRead())
					return false;
			}

			return true;
		} catch(Exception ignored) {}

		return false;
	}

	static String[] getForecast()
	{
		return getForecast(false);
	}

	static String[] getForecast(boolean force)
	{
		String fctype = GetStringPref("fctype", weeWXApp.fctype_default);
		if(fctype == null || fctype.isBlank())
		{
			String finalErrorStr = String.format(weeWXApp.getAndroidString(R.string.forecast_type_is_invalid), fctype);
			return new String[]{"error", finalErrorStr, fctype};
		}

		LogMessage("weeWXAppCommon.getForecast() fctype: " + fctype);

		String forecast_url = GetStringPref("FORECAST_URL", weeWXApp.FORECAST_URL_default);
		if(forecast_url == null || forecast_url.isBlank())
			return new String[]{"error", weeWXApp.getAndroidString(R.string.forecast_url_not_set), fctype};

		String forecastData = GetStringPref("forecastData", weeWXApp.forecastData_default);

		if(!checkConnection() && !force)
		{
			LogMessage("Not on wifi and not a forced refresh");
			if(forecastData == null || forecastData.isBlank())
				return new String[]{"error", weeWXApp.getAndroidString(R.string.wifi_not_available), fctype};

			return new String[]{"ok", forecastData, fctype};
		}

		final long current_time = getCurrTime();
		long rssCheckTime = GetLongPref("rssCheck", 0);

		if(!force && rssCheckTime + weeWXApp.RSSCache_period_default > current_time && forecastData != null && !forecastData.isBlank())
		{
			LogMessage("Cache isn't more than " + weeWXApp.RSSCache_period_default + " seconds old (" +
			           (rssCheckTime + weeWXApp.RSSCache_period_default) + "s)", true);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
			String date = sdf.format(GetLongPref("rssCheck", 0) * 1_000L);
			LogMessage("rsscheck: " + date);
			date = sdf.format(current_time * 1_000L);
			LogMessage("current_time: " + date);
			return new String[]{"ok", forecastData, fctype};
		}

		if(forecastTask != null && !forecastTask.isDone())
		{
			if(ftStart + 30 > current_time)
			{
				LogMessage("forecastTask is less than 30s old, we'll skip this attempt...", true);
				return new String[]{"ok", forecastData, fctype};
			}

			forecastTask.cancel(true);
		}

		LogMessage("Was forced or no forecast data or cache is more than " + weeWXApp.RSSCache_period_default +
		           " seconds old (" + (current_time - rssCheckTime) + "s)", true);

		ftStart = current_time;

		forecastTask = executor.submit(() ->
		{
			LogMessage("Forecast checking: " + forecast_url, true);

			String tmpForecastData = null;

			try
			{
				tmpForecastData = reallyGetForecast(forecast_url);
			} catch(Exception ignored) {}

			if(tmpForecastData != null && !tmpForecastData.isBlank())
				SendForecastRefreshIntent();

			ftStart = 0;
		});

		return new String[]{"ok", forecastData, fctype};
	}

	static String reallyGetForecast(String url) throws IOException
	{
		if(url == null || url.isBlank())
			return null;

		String forecastData = downloadString(url);
		if(forecastData.isBlank())
			return null;

		long current_time = getCurrTime();

		LogMessage("updating rss cache");
		SetLongPref("rssCheck", current_time);
		SetStringPref("forecastData", forecastData);

		return forecastData;
	}

	static Bitmap getRadarImage(boolean forced)
	{
		Bitmap bm = null;

		try
		{
			if(checkForImages(weeWXApp.radarFilename))
				bm = getImage(weeWXApp.radarFilename);
		} catch(Exception ignored) {}

		if(!checkConnection() && !forced)
		{
			if(bm == null)
				return weeWXApp.textToBitmap(R.string.wifi_not_available);

			return bm;
		}

		LogMessage("Reloading radar...");

		String radarURL = GetStringPref("RADAR_URL", weeWXApp.RADAR_URL_default);
		if(radarURL == null || radarURL.isBlank())
			return weeWXApp.textToBitmap(R.string.radar_url_not_set);

		String radtype = GetStringPref("radtype", weeWXApp.radtype_default);
		if(radtype == null || radtype.equals("image"))
			return weeWXApp.textToBitmap(R.string.radar_type_is_invalid);

		LogMessage("Reload radar...");

		long current_time = getCurrTime();

		if(radarTask != null && !radarTask.isDone())
		{
			if(rtStart + 30 > current_time)
			{
				LogMessage("radarTask is less than 30s old (" + (current_time - rtStart) +
				                          "s), we'll skip this attempt...");
				return bm;
			}

			radarTask.cancel(true);
		}

		rtStart = current_time;

		radarTask = executor.submit(() ->
		{
			LogMessage("Radar checking: " + radarURL);

			String tmpForecastData = null;

			try
			{
				LogMessage("Starting to download radar image from: " + radarURL);
				if(loadOrDownloadImage(radarURL, weeWXApp.radarFilename) != null)
					SendRadarRefreshIntent();
			} catch(Exception ignored) {}

			rtStart = 0;
		});

		return bm;
	}

	static Bitmap getWebcamImage(boolean forced)
	{
		Bitmap bm = null;
		long current_time = getCurrTime();

		try
		{
			if(checkForImages(weeWXApp.webcamFilename))
				bm = getImage(weeWXApp.webcamFilename);
		} catch(Exception ignored) {}

		if(!checkConnection() && !forced)
		{
			if(bm == null)
				return weeWXApp.textToBitmap(R.string.wifi_not_available);

			return bm;
		}

		LogMessage("Reloading webcam...");

		String webcamURL = GetStringPref("WEBCAM_URL", weeWXApp.WEBCAM_URL_default);
		if(webcamURL == null || webcamURL.isBlank())
			return weeWXApp.textToBitmap(R.string.wasnt_able_to_connect_webcam_url);

		if(!forced && wcStart + 60 > current_time && bm != null)
			return bm;

		LogMessage("Reload webcam...");

		if(webcamTask != null && !webcamTask.isDone())
		{
			if(wcStart + 30 > current_time)
			{
				LogMessage("webcamTask is less than 30s old (" + (current_time - wcStart) +
				                          "s), we'll skip this attempt...");
				return bm;
			}

			webcamTask.cancel(true);
		}

		wcStart = current_time;

		webcamTask = executor.submit(() ->
		{
			LogMessage("Webcam checking: " + webcamURL);

			try
			{
				LogMessage("Starting to download webcam image from: " + webcamURL);
				if(loadOrDownloadImage(webcamURL, weeWXApp.webcamFilename) != null)
					SendWebcamRefreshIntent();
			} catch(Exception ignored) {}
		});

		return bm;
	}

	static String getDateFromString(String str)
	{
		str = str.trim();

		if(!str.contains(" "))
			return str;

		try
		{
			return str.split(" ", 2)[0];
		} catch(Exception e) {
			doStackOutput(e);
		}

		return str;
	}

	static String getDaySuffix(int day)
	{
		String suffix;
		if(day >= 11 && day <= 13)
		{
			suffix = "th";
		} else {
			suffix = switch(day % 10)
			{
				case 1 -> "st";
				case 2 -> "nd";
				case 3 -> "rd";
				default -> "th";
			};
		}

		return day + suffix;
	}

	static byte[] bitmapToBytes(Bitmap bm)
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.JPEG, 75, stream);
		byte[] byteArray = stream.toByteArray();
		bm.recycle();

		return byteArray;
	}

	static String toBase64(byte[] in)
	{
		return "data:image/jpeg;base64," + Base64.encodeToString(in, Base64.DEFAULT)
				.replaceAll("\n", "")
				.replaceAll("\r", "")
				.replaceAll("\t", "");
	}

	static File getExtDir(String dir) throws IOException
	{
		File extdir = weeWXApp.getInstance().getExternalFilesDir("");
		if(extdir == null)
			throw new IOException("Unable to locate the external files directory...");

		if(!extdir.exists())
			throw new IOException("Unable to locate " + extdir.getAbsolutePath());

		if(!extdir.isDirectory())
			throw new IOException(extdir.getAbsolutePath() + " isn't a directory...");

		if(!extdir.canWrite())
			throw new IOException("Can't write to " + extdir.getAbsolutePath());

		File newdir = new File(extdir, dir);

		if(newdir.exists() && !newdir.isDirectory())
			throw new IOException(newdir.getAbsolutePath() + " isn't a directory...");

		if(!newdir.exists() && !newdir.mkdirs())
			throw new IOException("Can't create the requested directory " + newdir.getAbsolutePath());

		return newdir;
	}

	static File getFilesDir()
	{
		return weeWXApp.getInstance().getFilesDir();
	}

	static File getDir(String dir)
	{
		return new File(getFilesDir(), dir);
	}

	static boolean renameTo(File oldfile, File newfile) throws IOException
	{
		try
		{
			Files.move(oldfile.toPath(), newfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch(IOException ignored) {
			try(InputStream in = new FileInputStream(oldfile);
				OutputStream out = new FileOutputStream(newfile))
			{
				byte[] buf = new byte[8192];
				int len;
				while((len = in.read(buf)) > 0)
					out.write(buf, 0, len);

				return oldfile.delete();
			}
		}
	}

	static boolean deleteFile(String filename)
	{
		return deleteFile(getFilesDir(), filename);
	}

	static boolean deleteFile(String directory, String filename)
	{
		File dir = getDir(directory);
		return deleteFile(dir, filename);
	}

	static boolean deleteFile(File dir, String filename)
	{
		try
		{
			File file = new File(dir, filename);
			if(file.exists() && file.canWrite())
			{
				if(!file.delete())
				{
					LogMessage("Couldn't delete " + file.getAbsolutePath());
					return false;
				} else {
					LogMessage("Deleted " + file.getAbsolutePath());
					return true;

				}
			} else {
				LogMessage(file.getAbsolutePath() + " doesn't exist, skipping...");
				return true;
			}
		} catch(Exception ignored) {}

		return false;
	}

	static long getModifiedTime(File file)
	{
		try
		{
			if(file.exists())
				return Math.round(file.lastModified() / 1_000D);
		} catch(Exception ignored) {}

		return 0;
	}

	static File getFile(String filename)
	{
		return getFile(getFilesDir(), filename);
	}

	static File getFile(File dir, String filename)
	{
		return new File(dir, filename);
	}

	static Bitmap loadImage(String fileName) throws IOException
	{
		File f = getExtDir("icons");
		f = new File(f, fileName);

		return loadImage(f);
	}

	static Bitmap loadImage(File file)
	{
		return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
	}

	static Bitmap getImage(String filename)
	{
		File file = getFilesDir();
		file = new File(file, filename);
		return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
	}

	static Bitmap loadOrDownloadImage(String url, String filename) throws IOException
	{
		Bitmap bm = null;
		File file = getFile(filename);

		LogMessage("Starting to download image from: " + url);
		if(url.toLowerCase(Locale.ENGLISH).endsWith(".mjpeg") ||
		   url.toLowerCase(Locale.ENGLISH).endsWith(".mjpg"))
		{
			LogMessage("Trying to get a frame from a MJPEG stream and set bm to it...");
			bm = grabMjpegFrame(url);
			LogMessage("Saving frame to storage...");
			try(FileOutputStream out = new FileOutputStream(file))
			{
				LogMessage("Attempting to save to " + file.getAbsoluteFile());
				bm.compress(Bitmap.CompressFormat.JPEG, 85, out);
				LogMessage("Got past the save... ");
			} catch(Exception e) {
				LogMessage("Error! " + e);
				throw new IOException(e);
			}

			LogMessage("Frame saved successfully to storage...");
		} else {
			LogMessage("Trying to download a JPEG file and set bm to it...");
			if(downloadToFile(file, url))
				bm = loadImage(file);
		}

		LogMessage("Finished downloading from webURL: " + url);
		return bm;
	}

	static byte[] downloadContent(String url) throws IOException
	{
		if(url == null || url.isBlank())
		{
			LogMessage("url is null or blank, bailing out...");
			throw new IOException("url is null or blank, bailing out...");
		}

		OkHttpClient client = NetworkClient.getInstance(url);
		Request request = NetworkClient.newRequest(url);

		try(Response response = client.newCall(request).execute())
		{
			if(!response.isSuccessful())
				throw new IOException("HTTP error " + response);

			return response.body().bytes();
		}
	}

	static boolean downloadToFile(String filename, String url) throws IOException
	{
		return downloadToFile(getFile(filename), url);
	}

	static boolean downloadToFile(File file, String url) throws IOException
	{
		byte[] body = downloadContent(url);
		if(body == null || body.length == 0)
			throw new IOException("Download content was null or empty");

		File tmpfile = Files.createTempFile("weeWXApp_", ".tmp").toFile();

		if(tmpfile.exists() && !tmpfile.delete())
		{
			LogMessage("tmpfile " + tmpfile.getAbsolutePath() + " exists, but can't be deleted, bailing out...");
			throw new IOException(tmpfile.getAbsolutePath() + " exists, but can't be deleted, bailing out...");
		}

		FileOutputStream fos = new FileOutputStream(tmpfile);
		fos.write(body);
		fos.close();

		if(!renameTo(tmpfile, file))
			throw new IOException("Failed to rename tmpfile " + tmpfile.getAbsolutePath() + " to desination file " +
								  file.getAbsolutePath() + ", bailing out...");

		return true;
	}

	public static Bitmap grabMjpegFrame(String url) throws IOException
	{
		LogMessage("Requesting a MJPEG frame from " + url);

		Bitmap bm;
		InputStream urlStream = null;

		OkHttpClient client = NetworkClient.getStream(url);
		Request request = NetworkClient.newRequest(url);

		try(Response response = client.newCall(request).execute())
		{
			if(!response.isSuccessful())
			{
				LogMessage("Error! " + response);
				throw new IOException("HTTP error " + response);
			}

			LogMessage("Successfully connected to server, now to grab a frame...");

			urlStream = response.body().byteStream();

			BufferedReader reader = new BufferedReader(new InputStreamReader(urlStream, StandardCharsets.US_ASCII));

			while(true)
			{

				String line;
				int contentLength = -1;

				while((line = reader.readLine()) != null)
				{
					if(line.isEmpty() && contentLength > 0)
						break;

					if(line.startsWith("Content-Length:"))
						contentLength = Integer.parseInt(line.substring(15).trim());
				}

				LogMessage("contentLength: " + contentLength);

				byte[] imageBytes = new byte[contentLength];
				int offset = 0;
				while(offset < contentLength)
				{
					int read = urlStream.read(imageBytes, offset, contentLength - offset);
					if(read == -1)
					{
						LogMessage("Stream ended prematurely");
						throw new EOFException("Stream ended prematurely");
					}
					offset += read;
				}

				BitmapFactory.Options options = new BitmapFactory.Options();
				bm = BitmapFactory.decodeStream(new ByteArrayInputStream(imageBytes), null, options);
				if(bm != null)
				{
					LogMessage("Got an image... wooo!");
					break;
				} else {
					LogMessage("bm is null, let's go again...");
				}
			}
		} finally {
			if(urlStream != null)
				urlStream.close();
		}

		return bm;
	}

	static long getCurrTime()
	{
		return Math.round(System.currentTimeMillis() / 1_000D);
	}

	static Activity getActivity()
	{
		Context context = getApplicationContext();

		while(context instanceof ContextWrapper)
		{
			if(context instanceof Activity)
				return (Activity)context;

			context = ((ContextWrapper)context).getBaseContext();
		}

		return null;
	}

	static class NotificationLiveData extends LiveData<String>
	{
		public void setNotification(String message)
		{
			postValue(message);
		}
	}

	static class NotificationManager
	{

		private static final NotificationLiveData notificationLiveData = new NotificationLiveData();

		public static void updateNotificationMessage(String message)
		{
			notificationLiveData.setNotification(message);
		}

		public static NotificationLiveData getNotificationLiveData()
		{
			return notificationLiveData;
		}
	}
}