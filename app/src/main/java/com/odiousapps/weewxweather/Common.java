package com.odiousapps.weewxweather;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import fr.arnaudguyon.xmltojsonlib.XmlToJson;
import okhttp3.ConnectionSpec;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static java.lang.Math.round;

class Common
{
	private final static String PREFS_NAME = "WeeWxWeatherPrefs";
	private final static boolean debug_on = true;
	private String appversion = "0.0.0";
	final Context context;

	final static String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";

	static final String UPDATE_INTENT = "com.odiousapps.weewxweather.UPDATE_INTENT";
	static final String REFRESH_INTENT = "com.odiousapps.weewxweather.REFRESH_INTENT";
	static final String TAB0_INTENT = "com.odiousapps.weewxweather.TAB0_INTENT";
	static final String EXIT_INTENT = "com.odiousapps.weewxweather.EXIT_INTENT";
	static final String INIGO_INTENT = "com.odiousapps.weewxweather.INIGO_UPDATE";
	static final String FAILED_INTENT = "com.odiousapps.weewxweather.FAILED_INTENT";

	private static final long inigo_version = 4000;
	static final long icon_version = 12;
	private static final String icon_url = "https://github.com/evilbunny2008/weeWXWeatherApp/releases/download/1.0.3/icons.zip";

	private Thread t = null;
	private JSONObject nws = null;

	private final Typeface tf_bold;

	static final String ssheader = "<link rel='stylesheet' href='file:///android_asset/weathericons.css'>" +
								"<link rel='stylesheet' href='file:///android_asset/weathericons_wind.css'>" +
								"<link rel='stylesheet' type='text/css' href='file:///android_asset/flaticon.css'>";

	static final float[] NEGATIVE = {
			-1.0f, 0, 0, 0, 255, // red
			0, -1.0f, 0, 0, 255, // green
			0, 0, -1.0f, 0, 255, // blue
			0, 0, 0, 1.0f, 0  // alpha
	};

	Common(Context c)
	{
		System.setProperty("http.agent", UA);
		this.context = c;
		tf_bold = Typeface.create("sans-serif", Typeface.BOLD);

		try
		{
			PackageManager pm = c.getPackageManager();
			PackageInfo version = pm.getPackageInfo("com.odiousapps.weewxweather", 0);
			appversion = version.versionName;
			LogMessage("appversion=" + appversion);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	long[] getPeriod()
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
		if (period <= 0)
			return;

		long start = Math.round((double) System.currentTimeMillis() / (double) period) * period + wait;

		if (start < System.currentTimeMillis())
			start += period;

		LogMessage(from + " - start == " + start, true);
		LogMessage(from + " - period == " + period, true);
		LogMessage(from + " - wait == " + wait, true);

		Intent myAlarm = new Intent(context.getApplicationContext(), UpdateCheck.class);
		PendingIntent recurringAlarm = PendingIntent.getBroadcast(context.getApplicationContext(), 0, myAlarm, PendingIntent.FLAG_IMMUTABLE);
		AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarms.setExact(AlarmManager.RTC_WAKEUP, start, recurringAlarm);
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
		{
			int len = value.indexOf("\n");
			if (len <= 0)
				len = value.length();
			Log.i("weeWX Weather", "message='" + value.substring(0, len) + "'");
		}
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

	void commit()
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.apply();
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
			return defval;
		} catch (Exception e)
		{
			LogMessage("GetStringPref(" + name + ", " + defval + ") Err: " + e);
			e.printStackTrace();
			return defval;
		}

		LogMessage(name + "'='" + value + "'");

		return value;
	}

	void SetLongPref(String name, long value)
	{
		SetStringPref(name, "" + value);
	}

	@SuppressWarnings({"SameParameterValue"})
	long GetLongPref(String name, long defval)
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

	@SuppressWarnings({"SameParameterValue"})
	boolean GetBoolPref(String name, boolean defval)
	{
		String value = "0";
		if (defval)
			value = "1";

		String val = GetStringPref(name, value);
		return val.equals("1");
	}

	RemoteViews buildUpdate(Context context, int dark_theme)
	{
		int bgColour, fgColour;

		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
		Bitmap myBitmap = Bitmap.createBitmap(600, 440, Bitmap.Config.ARGB_4444);
		Canvas myCanvas = new Canvas(myBitmap);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setSubpixelText(true);

		if(dark_theme == 0)
			bgColour = GetIntPref("bgColour", 0xFFFFFFFF);
		else
			bgColour = GetIntPref("bgColour", 0xFF000000);
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(bgColour);

		RectF rectF = new RectF(0, 0, myCanvas.getWidth(), myCanvas.getHeight());
		int cornersRadius = 25;
		myCanvas.drawRoundRect(rectF, cornersRadius, cornersRadius, paint);

		if(dark_theme == 0)
			fgColour = GetIntPref("fgColour", 0xFF000000);
		else
			fgColour = GetIntPref("fgColour", 0xFFFFFFFF);

		if(bgColour == 0xFF000000 && fgColour == 0xFF000000)
			fgColour = 0xFFFFFFFF;
		else if(bgColour == 0xFFFFFFFF && fgColour == 0xFFFFFFFF)
			fgColour = 0xFF000000;

		paint.setStyle(Paint.Style.FILL);
		paint.setColor(fgColour);
		paint.setTextAlign(Paint.Align.CENTER);

		String[] bits = GetStringPref("LastDownload", "").split("\\|");
		if (bits.length > 110)
		{
			paint.setTextSize(64);
			myCanvas.drawText(bits[56], Math.round(myCanvas.getWidth() / 2.0), 80, paint);
			paint.setTextSize(48);
			myCanvas.drawText(bits[55], Math.round(myCanvas.getWidth() / 2.0), 140, paint);
			paint.setTextSize(200);
			myCanvas.drawText(bits[0] + bits[60], Math.round(myCanvas.getWidth() / 2.0), 310, paint);

			paint.setTextAlign(Paint.Align.LEFT);
			paint.setTextSize(64);
			myCanvas.drawText(bits[25] + bits[61], 20, 400, paint);

			paint.setTextAlign(Paint.Align.RIGHT);
			paint.setTextSize(64);

			String rain = bits[20];
			if (bits.length > 158 && !bits[158].equals(""))
				rain = bits[158];

			myCanvas.drawText(rain + bits[62], myCanvas.getWidth() - 20, 400, paint);
		} else {
			paint.setTextSize(200);
			myCanvas.drawText("Error!", Math.round(myCanvas.getWidth() / 2.0), 300, paint);
		}

		views.setImageViewBitmap(R.id.widget, myBitmap);
		return views;
	}

	private String generateForecast(List<Day> days, long timestamp, boolean showHeader)
	{
		if(days == null || days.size() <= 0)
			return null;

		StringBuilder sb = new StringBuilder();
		String tmp;

		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
		String ftime = sdf.format(timestamp);

		tmp = "<div style='font-size:12pt;'>" + ftime + "</div>";
		sb.append(tmp);

		if(showHeader)
		{
			tmp = "<table style='width:100%;border:0px;'>";
			sb.append(tmp);

			if(days.get(0).max.equals("&deg;C") || days.get(0).max.equals("&deg;F"))
				tmp = "<tr><td style='width:50%;font-size:48pt;'>&nbsp;</td>";
			else
				tmp = "<tr><td style='width:50%;font-size:48pt;'>" + days.get(0).max + "</td>";
			sb.append(tmp);

			if(!days.get(0).icon.startsWith("file:///") && !days.get(0).icon.startsWith("data:image"))
				tmp = "<td style='width:50%;text-align:right;'><i style='font-size:80px;' class='" + days.get(0).icon + "'></i></td></tr>";
			else
				tmp = "<td style='width:50%;text-align:right;'><img width='80px' src='" + days.get(0).icon + "'></td></tr>";
			sb.append(tmp);

			if(days.get(0).min.equals("&deg;C") || days.get(0).min.equals("&deg;F"))
			{
				tmp = "<tr><td style='text-align:right;font-size:16pt;' colspan='2'>" + days.get(0).text + "</td></tr></table><br />";
			} else {
				tmp = "<tr><td style='font-size:16pt;'>" + days.get(0).min + "</td>";
				sb.append(tmp);
				tmp = "<td style='text-align:right;font-size:16pt;'>" + days.get(0).text + "</td></tr></table><br />";
			}
			sb.append(tmp);

			tmp = "<table style='width:100%;border:0px;'>";
			sb.append(tmp);

			for(int i = 1; i < days.size(); i++)
			{
				Day day = days.get(i);
				if(!day.icon.startsWith("file:///") && !day.icon.startsWith("data:image"))
					tmp = "<tr><td style='width:10%;vertical-align:top;' rowspan='2'><i style='font-size:30px;' class='" + day.icon + "'></i></td>";
				else
					tmp = "<tr><td style='width:10%;vertical-align:top;' rowspan='2'><img width='40px' src='" + day.icon + "'></td>";
				sb.append(tmp);

				tmp = "<td style='width:65%;'><b>" + day.day + "</b></td>";
				sb.append(tmp);

				if(day.max.equals("&deg;C") || day.max.equals("&deg;F"))
					tmp = "<td style='width:25%;text-align:right;vertical-align:top;'>&nbsp;</td></tr>";
				else
					tmp = "<td style='width:25%;text-align:right;vertical-align:top;'><b>" + day.max + "</b></td></tr>";
				sb.append(tmp);

				if(day.min.equals("&deg;C") || day.min.equals("&deg;F"))
				{
					tmp = "<tr><td colspan='2'>" + day.text + "</td></tr>";
				} else {
					tmp = "<tr><td>" + day.text + "</td>";
					sb.append(tmp);
					tmp = "<td style='width:10%;text-align:right;vertical-align:top;'>" + day.min + "</td></tr>";
				}
				sb.append(tmp);

				tmp = "<tr><td style='font-size:4pt;' colspan='5'>&nbsp;</td></tr>";
				sb.append(tmp);
			}

		} else {
			tmp = "<table style='width:100%;'>\n";
			sb.append(tmp);

			for (Day day : days)
			{
				if(!day.icon.startsWith("file:///") && !day.icon.startsWith("data:image"))
					tmp = "<tr><td style='width:10%;vertical-align:top;' rowspan='2'><i style='font-size:30px;' class='" + day.icon + "'></i></td>";
				else
					tmp = "<tr><td style='width:10%;vertical-align:top;' rowspan='2'><img width='40px' src='" + day.icon + "'></td>";
				sb.append(tmp);

				tmp = "<td style='width:80%;'><b>" + day.day + "</b></td>";
				sb.append(tmp);

				if(!day.max.equals("&deg;C") && !day.max.equals("&deg;F"))
					tmp = "<td style='width:10%;text-align:right;vertical-align:top;'><b>" + day.max + "</b></td></tr>";
				else
					tmp = "<td style='width:10%;'><b>&nbsp;</b></td></tr>";
				sb.append(tmp);

				if(day.min.equals("&deg;C") || day.min.equals("&deg;F"))
				{
					tmp = "<tr><td colspan='2'>" + day.text + "</td></tr>";
				} else {
					tmp = "<tr><td>" + day.text + "</td>";
					sb.append(tmp);
					tmp = "<td style='width:10%;text-align:right;vertical-align:top;'>" + day.min + "</td></tr>";
				}
				sb.append(tmp);
			}

		}
		sb.append("</table>");
		return sb.toString();
	}

	String[] processBOM2(String data)
	{
		return processBOM2(data, false);
	}

	String[] processBOM2(String data, boolean showHeader)
	{
		if(data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
		String desc;
		List<Day> days = new ArrayList<>();
		long timestamp;

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
			String ampm = obs.substring(i, j);
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

			obs = hour + ":" + minute + " " + ampm + " " + date + " " + month + " " + year;

			SimpleDateFormat sdf = new SimpleDateFormat("h:mm aa d MMMM yyyy", Locale.getDefault());
			timestamp = Objects.requireNonNull(sdf.parse(obs)).getTime();

			String[] bits = fcdiv.split("<dl class=\"forecast-summary\">");
			String bit = bits[1];
			Day day = new Day();

			day.day = bit.split("<a href=\"", 2)[1].split("\">", 2)[0].split("/forecast/detailed/#d", 2)[1].trim();
			sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
			day.timestamp = Objects.requireNonNull(sdf.parse(day.day)).getTime();
			sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
			day.day = sdf.format(day.timestamp);

			day.icon = "http://www.bom.gov.au" + bit.split("<img src=\"", 2)[1].split("\" alt=\"", 2)[0].trim();

			if(bit.contains("<dd class=\"max\">"))
				day.max = bit.split("<dd class=\"max\">")[1].split("</dd>")[0].trim();

			if(bit.contains("<dd class=\"min\">"))
				day.min = bit.split("<dd class=\"min\">")[1].split("</dd>")[0].trim();

			day.text = bit.split("<dd class=\"summary\">")[1].split("</dd>")[0].trim();

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
				File f = new File(fileName);
				FileInputStream imageInFile = new FileInputStream(f);
				byte[] imageData = new byte[(int) f.length()];
				if(imageInFile.read(imageData) > 0)
					day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
			}

			day.max = day.max.replaceAll("°C", "").trim();
			day.min = day.min.replaceAll("°C", "").trim();

			if(metric)
			{
				day.max += "&deg;C";
				day.min += "&deg;C";
			} else {
				if(!day.max.equals(""))
					day.max += round((Double.parseDouble(day.max) * 9.0 / 5.0) + 32.0) + "&deg;F";
				if(!day.min.equals(""))
					day.min += round((Double.parseDouble(day.min) * 9.0 / 5.0) + 32.0) + "&deg;F";
			}

			if(day.max.equals("") || day.max.startsWith("&deg;"))
				day.max = "N/A";

			days.add(day);

			for(i = 2; i < bits.length; i++)
			{
				day = new Day();
				bit = bits[i];
				day.day = bit.split("<a href=\"", 2)[1].split("\">", 2)[0].split("/forecast/detailed/#d", 2)[1].trim();
				sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
				day.timestamp = Objects.requireNonNull(sdf.parse(day.day)).getTime();
				sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
				day.day = sdf.format(day.timestamp);

				day.icon = "http://www.bom.gov.au" + bit.split("<img src=\"", 2)[1].split("\" alt=\"", 2)[0].trim();
				day.max = bit.split("<dd class=\"max\">")[1].split("</dd>")[0].trim();
				day.min = bit.split("<dd class=\"min\">")[1].split("</dd>")[0].trim();
				day.text = bit.split("<dd class=\"summary\">")[1].split("</dd>")[0].trim();

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
					File f = new File(fileName);
					FileInputStream imageInFile = new FileInputStream(f);
					byte[] imageData = new byte[(int) f.length()];
					if(imageInFile.read(imageData) > 0)
						day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
				}

				day.max = day.max.replaceAll("°C", "").trim();
				day.min = day.min.replaceAll("°C", "").trim();

				if(metric)
				{
					day.max = day.max + "&deg;C";
					day.min = day.min + "&deg;C";
				} else {
					day.max = round((Double.parseDouble(day.max) * 9.0 / 5.0) + 32.0) + "&deg;F";
					day.min = round((Double.parseDouble(day.min) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}

				days.add(day);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	String[] processBOM3(String data)
	{
		return processBOM3(data, false);
	}

	String[] processBOM3(String data, boolean showHeader)
	{
		if(data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
		String desc;
		List<Day> days = new ArrayList<>();
		long timestamp;

		try
		{
			JSONObject jobj = new JSONObject(data);

			desc = jobj.getJSONObject("metadata").getString("forecast_region");
			String tmp = jobj.getJSONObject("metadata").getString("issue_time");

			SimpleDateFormat sdf;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
			{
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
			} else {
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
			}
			timestamp = Objects.requireNonNull(sdf.parse(tmp)).getTime();

			JSONArray mydays = jobj.getJSONArray("data");
			for(int i = 0; i < mydays.length(); i++)
			{
				Day day = new Day();
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
				{
					sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
				} else {
					sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
				}
				day.timestamp = Objects.requireNonNull(sdf.parse(mydays.getJSONObject(i).getString("date"))).getTime();
				sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
				day.day = sdf.format(day.timestamp);

				if(metric)
				{
					try
					{
						day.max = mydays.getJSONObject(i).getInt("temp_max") + "&deg;C";
					} catch (Exception e) {
						// ignore errors
					}

					try
					{
						day.min = mydays.getJSONObject(i).getInt("temp_min") + "&deg;C";
					} catch (Exception e) {
						// ignore errors
					}
				} else {
					try
					{
						day.max = round((mydays.getJSONObject(i).getInt("temp_max") * 9.0 / 5.0) + 32.0) + "&deg;F";
					} catch (Exception e) {
						// ignore errors
					}

					try
					{
						day.min = round((mydays.getJSONObject(i).getInt("temp_min") * 9.0 / 5.0) + 32.0) + "&deg;F";
					} catch (Exception e) {
						// ignore errors
					}
				}

				if(!mydays.getJSONObject(i).getString("extended_text").equals("null"))
					day.text = mydays.getJSONObject(i).getString("extended_text");

				String fileName = bomlookup(mydays.getJSONObject(i).getString("icon_descriptor"));
				if(!fileName.equals("null"))
				{
					if (!use_icons)
					{
						if (!fileName.equals("frost"))
							day.icon = "wi wi-bom-" + fileName;
						else
							day.icon = "flaticon-thermometer";
					} else {
						fileName = checkImage("bom2" + fileName + ".png", null);
						File f = new File(fileName);
						FileInputStream imageInFile = new FileInputStream(f);
						byte[] imageData = new byte[(int) f.length()];
						if(imageInFile.read(imageData) > 0)
							day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
					}
				}

				days.add(day);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	private String bomlookup(String icon)
	{
		icon = icon.replace("-", "_");

		switch(icon)
		{
			case "shower":
				return "showers";
			case "dusty":
				return "dust";
			case "mostly_sunny":
				return "partly_cloudy";
			case "light_shower":
				return "light_showers";
			case "windy":
				return "wind";
		}

		return icon;
	}

	String[] processMET(String data)
	{
		return processMET(data, false);
	}

	String[] processMET(String data, boolean showHeader)
	{
		if(data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
		long timestamp = GetLongPref("rssCheck", 0) * 1000;
		String desc;
		List<Day> days = new ArrayList<>();

		try
		{
			desc = data.split("<title>", 2)[1].split(" weather - Met Office</title>",2)[0].trim();

			String[] forecasts = data.split("<ul id=\"dayNav\"", 2)[1].split("</ul>", 2)[0].split("<li");
			for(int i = 1; i < forecasts.length; i++)
			{
				Day day = new Day();
				String date = forecasts[i].split("data-tab-id=\"", 2)[1].split("\"")[0].trim();

				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
				day.timestamp = Objects.requireNonNull(sdf.parse(date)).getTime();
				sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
				day.day = sdf.format(day.timestamp);

				String icon = "https://beta.metoffice.gov.uk" + forecasts[i].split("<img class=\"icon\"")[1].split("src=\"")[1].split("\">")[0].trim();
				String fileName =  icon.substring(icon.lastIndexOf('/') + 1).replaceAll("\\.svg$", "\\.png");
				day.min = forecasts[i].split("<span class=\"tab-temp-low\"", 2)[1].split("\">")[1].split("</span>")[0].trim();
				day.max = forecasts[i].split("<span class=\"tab-temp-high\"", 2)[1].split("\">")[1].split("</span>")[0].trim();
				day.text = forecasts[i].split("<div class=\"summary-text", 2)[1].split("\">", 3)[2]
									.split("</div>", 2)[0].replaceAll("</span>", "").replaceAll("<span>", "");

				day.min = day.min.substring(0, day.min.length() - 5);
				day.max = day.max.substring(0, day.max.length() - 5);

				if(!use_icons)
				{
					day.icon = "wi wi-metoffice-" + fileName.substring(0, fileName.lastIndexOf("."));
				} else {
					fileName = checkImage("met" + fileName, null);
					File f = new File(fileName);
					FileInputStream imageInFile = new FileInputStream(f);
					byte[] imageData = new byte[(int) f.length()];
					if(imageInFile.read(imageData) > 0)
						day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
				}

				if(metric)
				{
					day.max += "&deg;C";
					day.min += "&deg;C";
				} else {
					day.max = round((Double.parseDouble(day.max) * 9.0 / 5.0) + 32.0) + "&deg;F";
					day.min = round((Double.parseDouble(day.min) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}

				days.add(day);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	String[] processWCA(String data)
	{
		return processWCA(data, false);
	}

	String[] processWCA(String data, boolean showHeader)
	{
		if(data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
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
			String ampm = obs.substring(i, j);
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

			obs = hour + ":" + minute + " " + ampm + " " + date + " " + month + " " + year;

			SimpleDateFormat sdf = new SimpleDateFormat("h:mm aa d MMMM yyyy", Locale.getDefault());
			lastTS = timestamp = Objects.requireNonNull(sdf.parse(obs)).getTime();

			desc = data.split("<dt>Observed at:</dt>", 2)[1].split("<dd class=\"mrgn-bttm-0\">")[1].split("</dd>")[0].trim();

			data = data.split("<div class=\"div-table\">", 2)[1].trim();
			data = data.split("<section><details open=\"open\" class=\"wxo-detailedfore\">")[0].trim();
			data = data.substring(0, data.length() - 7).trim();

			String[] bits = data.split("<div class=\"div-column\">");

			for(i = 1; i < bits.length; i++)
			{
				Document doc = Jsoup.parse(bits[i].trim());
				Elements div = doc.select("div");
				for (j = 0; j < div.size(); j++)
				{
					String text = "", img_url = "", temp = "", pop = "";
					Day day = new Day();

					if (div.get(j).className().contains("greybkgrd"))
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
							date = div.get(j).html().split("<strong title=\"", 2)[1].split("\">", 2)[0].trim();
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
						if (div.outerHtml().contains("div-row-data"))
						{
							if (metric)
								temp = div.get(j).select("div").select("span").html().split("<abbr")[0].trim() + "C";
							else
								temp = div.get(j).select("div").select("span").html().split("</abbr>")[1].split("<abbr")[0].trim() + "F";
							text = div.get(j).select("div").select("img").outerHtml().split("alt=\"", 2)[1].split("\"", 2)[0].trim();
							img_url = "https://www.weather.gc.ca" + div.get(j).select("div").select("img").outerHtml().split("src=\"", 2)[1].split("\"", 2)[0].trim();
							pop = div.get(j).select("div").select("small").html().trim();
						}
					} catch (Exception e) {
						LogMessage("hmmm 2 == " + div.html());
						e.printStackTrace();
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
						File f = new File(fileName);
						FileInputStream imageInFile = new FileInputStream(f);
						byte[] imageData = new byte[(int) f.length()];
						if(imageInFile.read(imageData) > 0)
							day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
					}

					day.day = date;
					day.max = temp;
					day.text = text;
					day.min = pop;

					days.add(day);

					lastTS = day.timestamp;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	String[] processWCAF(String data)
	{
		return processWCAF(data, false);
	}

	String[] processWCAF(String data, boolean showHeader)
	{
		if(data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
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
			lastTS = timestamp = Objects.requireNonNull(sdf.parse(obs)).getTime();

			desc = data.split("<dt>Enregistrées à :</dt>", 2)[1].split("<dd class=\"mrgn-bttm-0\">")[1].split("</dd>")[0].trim();

			data = data.split("<div class=\"div-table\">", 2)[1].trim();
			data = data.split("<section><details open=\"open\" class=\"wxo-detailedfore\">")[0].trim();
			data = data.substring(0, data.length() - 7).trim();

			bits = data.split("<div class=\"div-column\">");

			for(i = 1; i < bits.length; i++)
			{
				Document doc = Jsoup.parse(bits[i].trim());
				Elements div = doc.select("div");
				for (j = 0; j < div.size(); j++)
				{
					Day day = new Day();
					String text = "", img_url = "", temp = "", pop = "";

					if (div.get(j).className().contains("greybkgrd"))
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
						} else if (div.get(j).select("div").html().contains("Nuit")) {
							date = "Nuit";
							day.timestamp = lastTS;
						} else {
							date = div.get(j).html().split("<strong title=\"", 2)[1].split("\">", 2)[0].trim();
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
						if (div.outerHtml().contains("div-row-data"))
						{
							if (metric)
								temp = div.get(j).select("div").select("span").html().split("<abbr")[0].trim() + "C";
							else
								temp = div.get(j).select("div").select("span").html().split("</abbr>")[1].split("<abbr")[0].trim() + "F";
							text = div.get(j).select("div").select("img").outerHtml().split("alt=\"", 2)[1].split("\"", 2)[0].trim();
							img_url = "https://www.weather.gc.ca" + div.get(j).select("div").select("img").outerHtml().split("src=\"", 2)[1].split("\"", 2)[0].trim();
							pop = div.get(j).select("div").select("small").html().trim();
						}
					} catch (Exception e) {
						LogMessage("hmmm 2 == " + div.html());
						e.printStackTrace();
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
						File f = new File(fileName);
						FileInputStream imageInFile = new FileInputStream(f);
						byte[] imageData = new byte[(int) f.length()];
						if(imageInFile.read(imageData) > 0)
							day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
					}

					day.day = date;
					day.max = temp;
					day.text = text;
					day.min = pop;
					days.add(day);

					lastTS = day.timestamp;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	// Thanks goes to the https://saratoga-weather.org folk for the base NOAA icons and code for dualimage.php

	String[] processWGOV(String data)
	{
		return processWGOV(data, false);
	}

	String[] processWGOV(String data, boolean showHeader)
	{
		if(data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		List<Day> days = new ArrayList<>();
		long timestamp;
		String desc;

		try
		{
			JSONObject jobj = new JSONObject(data);

			desc = jobj.getJSONObject("currentobservation").getString("name");
			String tmp = jobj.getString("creationDate");

			SimpleDateFormat sdf;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
			{
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
			} else {
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
			}
			timestamp = Objects.requireNonNull(sdf.parse(tmp)).getTime();

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
						line = line.trim();
						String[] bits = line.split("=", 2);
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
							if (bmp == null)
							{
								LogMessage("continue1, bmp is null");
								continue;
							}

							ByteArrayOutputStream stream = new ByteArrayOutputStream();
							bmp.compress(Bitmap.CompressFormat.JPEG, 75, stream);
							byte[] byteArray = stream.toByteArray();
							bmp.recycle();

							// https://stackoverflow.com/questions/9224056/android-bitmap-to-base64-string

							String encoded = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
							iconLink.put(i, encoded);
						} else {
							Bitmap bmp = combineImage(bmp1, fper + "%", sper + "%");
							if (bmp == null)
							{
								LogMessage("continue2, bmp is null");
								continue;
							}

							ByteArrayOutputStream stream = new ByteArrayOutputStream();
							bmp.compress(Bitmap.CompressFormat.JPEG, 75, stream);
							byte[] byteArray = stream.toByteArray();
							bmp.recycle();

							String encoded = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
							iconLink.put(i, encoded);
						}
					}
				} else {
					Pattern p = Pattern.compile("[0-9]");
					number = p.matcher(fn).replaceAll("");
					if(!number.equals(""))
					{
						fn = fn.replaceAll("\\d{2,3}\\.jpg$", "\\.jpg");
						Bitmap bmp3 = loadImage(fn);

						if(bmp3 != null)
						{
							Bitmap bmp4 = loadImage("wgovoverlay.jpg");
							bmp3 = overlay(bmp3, bmp4, number + "%");

							ByteArrayOutputStream stream = new ByteArrayOutputStream();
							bmp3.compress(Bitmap.CompressFormat.JPEG, 75, stream);
							byte[] byteArray = stream.toByteArray();
							bmp3.recycle();
							bmp4.recycle();

							// https://stackoverflow.com/questions/9224056/android-bitmap-to-base64-string

							String encoded = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
							iconLink.put(i, encoded);
						}
					}
				}

				if(iconLink.getString(i).toLowerCase().startsWith("http"))
				{
					String fileName = "wgov" + iconLink.getString(i).substring(iconLink.getString(i).lastIndexOf("/") + 1).trim().replaceAll("\\.png$", "\\.jpg");
					fileName = checkImage(fileName, iconLink.getString(i));
					File f = new File(fileName);
					FileInputStream imageInFile = new FileInputStream(f);
					byte[] imageData = new byte[(int) f.length()];
					if(imageInFile.read(imageData) > 0)
						day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
				} else {
					day.icon = iconLink.getString(i);
				}

				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
				{
					sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
				} else {
					sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
				}
				day.timestamp = Objects.requireNonNull(sdf.parse(validTime.getString(i))).getTime();
				day.day = periodName.getString(i);

				if(!metric)
					day.max = temperature.getString(i) + "&deg;F";
				else
					day.max = round((Double.parseDouble(temperature.getString(i)) - 32.0) * 5.0 / 9.0)  + "&deg;C";
				day.text = weather.getString(i);
				days.add(day);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	String[] processWMO(String data)
	{
		return processWMO(data, false);
	}

	String[] processWMO(String data, boolean showHeader)
	{
		if(data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		String desc;
		long timestamp;
		List<Day> days = new ArrayList<>();

		try
		{
			JSONObject jobj = new JSONObject(data);

			desc = jobj.getJSONObject("city").getString("cityName") + ", " + jobj.getJSONObject("city").getJSONObject("member").getString("memName");
			String tmp = jobj.getJSONObject("city").getJSONObject("forecast").getString("issueDate");

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
			timestamp = Objects.requireNonNull(sdf.parse(tmp)).getTime();

			JSONArray jarr = jobj.getJSONObject("city").getJSONObject("forecast").getJSONArray("forecastDay");
			for(int i = 0; i < jarr.length(); i++)
			{
				Day day = new Day();
				JSONObject j = jarr.getJSONObject(i);

				String date = j.getString("forecastDate").trim();
				sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
				day.timestamp = Objects.requireNonNull(sdf.parse(date)).getTime();
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
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	String[] processBOM(String data)
	{
		return processBOM(data, false);
	}

	String[] processBOM(String data, boolean showHeader)
	{
		if(data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
		String desc;
		long timestamp;
		List<Day> days = new ArrayList<>();

		try
		{
			JSONObject jobj = new JSONObject(data);
			desc = jobj.getString("description") + ", Australia";

			String tmp = jobj.getString("content");
			SimpleDateFormat sdf;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
			{
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
			} else {
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
			}
			timestamp = Objects.requireNonNull(sdf.parse(tmp)).getTime();

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
						if (jarr2.getJSONObject(x).getString("type").equals("forecast_icon_code"))
							code = jarr2.getJSONObject(x).getString("content");

						if (jarr2.getJSONObject(x).getString("type").equals("air_temperature_minimum"))
							day.min = jarr2.getJSONObject(x).getString("content");

						if (jarr2.getJSONObject(x).getString("type").equals("air_temperature_maximum"))
							day.max = jarr2.getJSONObject(x).getString("content");
					}
				} catch (JSONException e) {
					code = j.getJSONObject("element").getString("content");
				}

				String date = j.getString("start-time-local").trim();
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
				{
					sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
				} else {
					sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
				}
				day.timestamp = Objects.requireNonNull(sdf.parse(date)).getTime();
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
					File f = new File(fileName);
					FileInputStream imageInFile = new FileInputStream(f);
					byte[] imageData = new byte[(int) f.length()];
					if(imageInFile.read(imageData) > 0)
						day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
				}

				if(metric)
				{
					day.max += "&deg;C";
					day.min += "&deg;C";
				} else {
					day.max = round((Double.parseDouble(day.max) * 9.0 / 5.0) + 32.0) + "&deg;F";
					day.min = round((Double.parseDouble(day.min) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}

				if(day.max.equals("&deg;C") || day.max.equals("&deg;F"))
					day.max = "N/A";

				days.add(day);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	String[] processMetService(String data)
	{
		return processMetService(data, false);
	}

	String[] processMetService(String data, boolean showHeader)
	{
		if (data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
		List<Day> days = new ArrayList<>();
		String desc;
		long timestamp;

		try
		{
			JSONObject jobj = new JSONObject(data);
			JSONArray loop = jobj.getJSONArray("days");
			String ftime = loop.getJSONObject(0).getString("issuedAtISO");
			desc = jobj.getString("locationWASP") + ", New Zealand";

			SimpleDateFormat sdf;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
			{
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
			} else {
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
			}
			timestamp = Objects.requireNonNull(sdf.parse(ftime)).getTime();

			for(int i = 0; i < loop.length(); i++)
			{
				Day day = new Day();
				JSONObject jtmp = loop.getJSONObject(i);
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
				{
					sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
				} else {
					sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
				}
				day.timestamp = Objects.requireNonNull(sdf.parse(jtmp.getString("dateISO"))).getTime();
				day.day = jtmp.getString("dow");
				day.text = jtmp.getString("forecast");
				day.max = jtmp.getString("max");
				day.min = jtmp.getString("min");
				if(jtmp.has("partDayData"))
					day.icon = jtmp.getJSONObject("partDayData").getJSONObject("afternoon").getString("forecastWord");
				else
					day.icon = jtmp.getString("forecastWord");

				day.icon = day.icon.toLowerCase().replaceAll(" ", "-").trim();

				if(!use_icons)
				{
					if(!day.icon.equals("frost"))
						day.icon = "wi wi-metservice-" + day.icon;
					else
						day.icon = "flaticon-thermometer";
				} else {
					day.icon = day.icon.replaceAll("-", "_");
					String fileName = checkImage("ms_" + day.icon + ".png", null);
					File f = new File(fileName);
					FileInputStream imageInFile = new FileInputStream(f);
					byte[] imageData = new byte[(int) f.length()];
					if(imageInFile.read(imageData) > 0)
						day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
				}

				if(metric)
				{
					day.max += "&deg;C";
					day.min += "&deg;C";
				} else {
					day.max = round((Double.parseDouble(day.max) * 9.0 / 5.0) + 32.0) + "&deg;F";
					day.min = round((Double.parseDouble(day.min) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}

				days.add(day);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	String[] processDWD(String data)
	{
		return processDWD(data, false);
	}

	String[] processDWD(String data, boolean showHeader)
	{
		if (data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
		List<Day> days = new ArrayList<>();
		String desc = "";
		long timestamp, lastTS;

		try
		{
			String[] bits = data.split("<title>");
			if (bits.length >= 2)
				desc = bits[1].split("</title>")[0];
			desc = desc.substring(desc.lastIndexOf(" - ") + 3).trim();
			String ftime = data.split("<tr class=\"headRow\">", 2)[1].split("</tr>", 2)[0].trim();
			String date = ftime.split("<td width=\"30%\" class=\"stattime\">", 2)[1].split("</td>", 2)[0].trim();
			ftime = date + " " + ftime.split("<td width=\"40%\" class=\"stattime\">", 2)[1].split(" Uhr</td>", 2)[0].trim();

			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy' 'HH", Locale.getDefault());
			lastTS = timestamp = Objects.requireNonNull(sdf.parse(ftime)).getTime();

			data = data.split("<td width=\"40%\" class=\"statwert\">Vorhersage</td>", 2)[1].split("</table>", 2)[0].trim();
			bits = data.split("<tr");
			for(int i = 1; i < bits.length; i++)
			{
				Day day = new Day();
				String bit = bits[i];
				String icon, temp;
				if (bit.split("<td ><b>", 2).length > 1)
					day.day = bit.split("<td ><b>", 2)[1].split("</b></td>", 2)[0].trim();
				else
					day.day = bit.split("<td><b>", 2)[1].split("</b></td>", 2)[0].trim();

				day.timestamp = convertDaytoTS(day.day, new Locale("de", "DE"), lastTS);
				if (day.timestamp != 0)
				{
					sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
					day.day = sdf.format(day.timestamp) + " " + day.day.substring(day.day.lastIndexOf(" ") + 1);
				}

				if (bit.split("<td ><img name=\"piktogramm\" src=\"", 2).length > 1)
					icon = bit.split("<td ><img name=\"piktogramm\" src=\"", 2)[1].split("\" width=\"50\" alt=\"", 2)[0].trim();
				else
					icon = bit.split("<td><img name=\"piktogramm\" src=\"", 2)[1].split("\" width=\"50\" alt=\"", 2)[0].trim();

				if (bit.split("\"></td>\r\n<td >", 2).length > 1)
					temp = bit.split("\"></td>\r\n<td >", 2)[1].split("Grad <abbr title=\"Celsius\">C</abbr></td>\r\n", 2)[0].trim();
				else
					temp = bit.split("\"></td>\r\n<td>", 2)[1].split("Grad <abbr title=\"Celsius\">C</abbr></td>\r\n", 2)[0].trim();

				icon = icon.replaceAll("/DE/wetter/_functions/piktos/vhs_", "").replaceAll("\\?__blob=normal", "").trim();
				String fileName = "dwd_" + icon.replaceAll("-", "_");
				String url = "https://www.dwd.de/DE/wetter/_functions/piktos/" + icon + "?__blob=normal";
				fileName = checkImage(fileName, url);

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
					FileInputStream imageInFile = new FileInputStream(f);
					byte[] imageData = new byte[(int) f.length()];
					if(imageInFile.read(imageData) > 0)
						day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
				}

				day.min = "&deg;C";
				if(metric)
					day.max = temp + "&deg;C";
				else
					day.max = round((Double.parseDouble(temp) * 9.0 / 5.0) + 32.0) + "&deg;F";

				days.add(day);
				lastTS = day.timestamp;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	String[] processTempoItalia(String data)
	{
		return processTempoItalia(data, false);
	}

	String[] processTempoItalia(String data, boolean showHeader)
	{
		if (data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
		List<Day> days = new ArrayList<>();
		String desc;
		long timestamp, lastTS;

		try
		{
			String stuff = data.split("<div id=\"weatherDayNavigator\">", 2)[1].trim();
			stuff = stuff.split("<h2>", 2)[1].trim();
			desc = stuff.split(" <span class=\"day\">")[0].trim();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
			String ftime = sdf.format(System.currentTimeMillis());
			sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
			lastTS = timestamp = Objects.requireNonNull(sdf.parse(ftime)).getTime();

			data = data.split("<tbody>")[1].trim();
			String[] bits = data.split("<tr>");
			for(int i = 1; i < bits.length; i++)
			{
				Day day = new Day();
				String bit = bits[i].trim();
				String icon;
				day.day = bit.split("<td class=\"timeweek\">")[1].split("\">")[1].split("</a></td>", 2)[0].trim();

				day.timestamp = convertDaytoTS(day.day, new Locale("it", "IT"), lastTS);
				if(day.timestamp != 0)
				{
					sdf = new SimpleDateFormat("EEE dd", Locale.getDefault());
					day.day = sdf.format(day.timestamp) + " " + day.day.substring(day.day.lastIndexOf(" ") + 1);
				}

				icon = bit.split("<td class=\"skyIcon\"><img src=\"", 2)[1].split("\" alt=\"",2)[0].trim();
				String[] ret = checkFilesIt(icon);
				if(ret[0] != null)
				{
					File f = new File(ret[1]);
					FileInputStream imageInFile = new FileInputStream(f);
					byte[] imageData = new byte[(int) f.length()];
					if(imageInFile.read(imageData) > 0)
						day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
				} else
					return ret;
//				LogMessage("day.icon=" + day.icon);

				day.max = bit.split("<td class=\"tempmax\">", 2)[1].split("°C</td>", 2)[0].trim();
				day.min = bit.split("<td class=\"tempmin\">", 2)[1].split("°C</td>", 2)[0].trim();

				day.text = bit.split("<td class=\"skyDesc\">")[1].split("</td>")[0].trim();

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
					day.max = round((Double.parseDouble(day.max) * 9.0 / 5.0) + 32.0) + "&deg;F";
					day.min = round((Double.parseDouble(day.min) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}

				days.add(day);
				lastTS = day.timestamp;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	String[] processAEMET(String data)
	{
		return processAEMET(data, false);
	}

	String[] processAEMET(String data, boolean showHeader)
	{
		if (data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
		List<Day> days = new ArrayList<>();
		String desc;
		long timestamp;

		try
		{
			JSONObject jobj = new XmlToJson.Builder(data).build().toJson();
			if (jobj == null)
				return null;

			jobj = jobj.getJSONObject("root");
			desc = jobj.getString("nombre") + ", " + jobj.getString("provincia");

			String elaborado = jobj.getString("elaborado");

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
			timestamp = Objects.requireNonNull(sdf.parse(elaborado)).getTime();

			JSONArray dates = jobj.getJSONObject("prediccion").getJSONArray("dia");
			for(int i = 0; i < dates.length(); i++)
			{
				Day day = new Day();
				JSONObject jtmp = dates.getJSONObject(i);
				String fecha = jtmp.getString("fecha");
				sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
				day.timestamp = Objects.requireNonNull(sdf.parse(fecha)).getTime();
				sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
				day.day = sdf.format(day.timestamp);

				JSONObject estado_cielo = null;

				Object v = jtmp.get("estado_cielo");
				if(v instanceof JSONObject)
				{
					estado_cielo = jtmp.getJSONObject("estado_cielo");
				} else if(v instanceof JSONArray) {
					JSONArray jarr = jtmp.getJSONArray("estado_cielo");
					for(int j = 0; j < jarr.length(); j++)
					{
						if(!jarr.getJSONObject(j).getString("descripcion").equals(""))
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
				} else {
					String url = "http://www.aemet.es/imagenes/png/estado_cielo/" + code + "_g.png";
					String fileName = "aemet_" + code + "_g.png";
					fileName = checkImage(fileName, url);
					File f = new File(fileName);
					FileInputStream imageInFile = new FileInputStream(f);
					byte[] imageData = new byte[(int) f.length()];
					if(imageInFile.read(imageData) > 0)
						day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);				}

				day.max = temperatura.getString("maxima");
				day.min = temperatura.getString("minima");
				if(metric)
				{
					day.max += "&deg;C";
					day.min += "&deg;C";
				} else {
					day.max = round((Double.parseDouble(day.max) * 9.0 / 5.0) + 32.0) + "&deg;F";
					day.min = round((Double.parseDouble(day.min) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}

				day.text = estado_cielo.getString("descripcion");
				days.add(day);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	String[] processWCOM(String data)
	{
		return processWCOM(data, false);
	}

	String[] processWCOM(String data, boolean showHeader)
	{
		if (data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
		long timestamp = GetLongPref("rssCheck", 0) * 1000;

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
				if (icons.getString(i) == null || icons.getString(i).equals("null"))
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
					Common.LogMessage("yahoo filename = " + fileName);

					File f = new File(fileName);
					FileInputStream imageInFile = new FileInputStream(f);
					byte[] imageData = new byte[(int) f.length()];
					if(imageInFile.read(imageData) > 0)
						day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
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
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	String[] processMETIE(String data)
	{
		return processMETIE(data, false);
	}

	String[] processMETIE(String data, boolean showHeader)
	{
		if (data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
		long timestamp = GetLongPref("rssCheck", 0) * 1000;

		List<Day> days = new ArrayList<>();
		String desc;

		try
		{
			desc = GetStringPref("metierev", "");

			SimpleDateFormat dayname = new SimpleDateFormat("EEEE d", Locale.getDefault());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
			JSONArray jarr = new JSONArray(data);
			for(int i = 0; i < jarr.length(); i++)
			{
				JSONObject jobj = jarr.getJSONObject(i);
				Day day = new Day();
				String tmpDay = jobj.getString("date") + " " + jobj.getString("time");
				day.timestamp = Objects.requireNonNull(sdf.parse(tmpDay)).getTime();
				day.day = dayname.format(day.timestamp);
				day.max = jobj.getString("temperature");
				day.icon = jobj.getString("weatherNumber");

				if(!use_icons)
				{
					day.icon = "wi wi-met-ie-" + day.icon;
				} else {
					String fileName = checkImage("y" + day.icon + ".png", null);
					File f = new File(fileName);
					FileInputStream imageInFile = new FileInputStream(f);
					byte[] imageData = new byte[(int) f.length()];
					if(imageInFile.read(imageData) > 0)
						day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
				}

				day.text = jobj.getString("weatherDescription");

				if(metric)
					day.max += "&deg;C";
				else
					day.max = round((Double.parseDouble(day.max) * 9.0 / 5.0) + 32.0) + "&deg;F";

				days.add(day);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	String[] processOWM(String data) { return processOWM(data, false); }

	String[] processOWM(String data, boolean showHeader)
	{
		if (data == null || data.equals(""))
			return null;

		List<Day> days = new ArrayList<>();
		String desc;

		boolean metric = GetBoolPref("metric", true);
		long timestamp = GetLongPref("rssCheck", 0) * 1000;

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
				day.timestamp = j.getLong("dt") * 1000;
				day.day = sdf.format(day.timestamp);

				JSONObject temp = j.getJSONObject("temp");
				int min = (int)round(Double.parseDouble(temp.getString("min")));
				int max = (int)round(Double.parseDouble(temp.getString("max")));
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
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	String[] processYR(String data)
	{
		return processYR(data, false);
	}

	String[] processYR(String data, boolean showHeader)
	{
		if(data == null || data.equals(""))
			return null;

		boolean use_icons = GetBoolPref("use_icons", false);
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

			timestamp = GetLongPref("rssCheck", 0) * 1000;

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
				day.timestamp = Objects.requireNonNull(sdf.parse(from)).getTime();
				sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
				String date = sdf.format(day.timestamp);

				sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
				from = sdf.format(day.timestamp);

				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
				long mdate = Objects.requireNonNull(sdf.parse(to)).getTime();
				sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
				to = sdf.format(mdate);

				day.day = date + ": " + from + "-" + to;

				String code = symbol.getString("var");

				if(!use_icons)
				{
					day.icon = "wi wi-yrno-" + code;
				} else {
					String fileName = checkImage("yrno" + code + ".png", null);
					File f = new File(fileName);
					FileInputStream imageInFile = new FileInputStream(f);
					byte[] imageData = new byte[(int) f.length()];
					if(imageInFile.read(imageData) > 0)
						day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
				}

				day.max = temperature.getString("value") + "&deg;C";
				day.min = precipitation.getString("value") + "mm";
				day.text = windSpeed.getString("name") + ", " + windSpeed.get("mps") + "m/s from the " + windDirection.getString("name");
				days.add(day);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	String[] processMetNO(String data)
	{
		return processMetNO(data, false);
	}

	String[] processMetNO(String data, boolean showHeader)
	{
		if(data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
		List<Day> days = new ArrayList<>();
		String desc = "";
		long timestamp;

		try
		{
//			desc = location.getString("name") + ", " + location.getString("country");

			JSONObject jobj = new JSONObject(data);
			jobj = jobj.getJSONObject("properties");
			String updated_at = jobj.getJSONObject("meta").getString("updated_at");
			SimpleDateFormat sdf;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
			{
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
			} else {
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
			}
			timestamp = Objects.requireNonNull(sdf.parse(updated_at)).getTime();
			JSONArray jarr = jobj.getJSONArray("timeseries");

			for(int i = 0; i < jarr.length(); i++)
			{
				Day day = new Day();
				String time = jarr.getJSONObject(i).getString("time");
				day.timestamp = Objects.requireNonNull(sdf.parse(time)).getTime();

				JSONObject tsdata = jarr.getJSONObject(i).getJSONObject("data");
				double temp = tsdata.getJSONObject("instant").getJSONObject("details").getDouble("air_temperature");
				if(metric)
					day.max = round(temp) + "&deg;C";
				else
					day.max = round((temp * 9.0 / 5.0) + 32.0) + "&deg;F";

				String icon;

				try
				{
					try
					{
						double precip = tsdata.getJSONObject("next_1_hours").getJSONObject("details").getDouble("precipitation_amount");
						if(metric)
							day.min = precip + "mm";
						else
							day.min = (round(precip / 25.4 * 1000.0) / 1000.0) + "in";

						icon = tsdata.getJSONObject("next_1_hours").getJSONObject("summary").getString("symbol_code");
					} catch (Exception e) {
						double precip = tsdata.getJSONObject("next_6_hours").getJSONObject("details").getDouble("precipitation_amount");
						if(metric)
							day.min = precip + "mm";
						else
							day.min = (round(precip / 25.4 * 1000.0) / 1000.0) + "in";

						icon = tsdata.getJSONObject("next_6_hours").getJSONObject("summary").getString("symbol_code");
					}
				} catch (Exception e) {
					continue;
				}

				double windSpeed = tsdata.getJSONObject("instant").getJSONObject("details").getDouble("wind_speed");
				double windDir = tsdata.getJSONObject("instant").getJSONObject("details").getDouble("wind_from_direction");
				if(metric)
					day.text = windSpeed + "m/s from the " + degtoname(windDir);
				else
					day.text = (round(windSpeed * 22.36936) / 10.0) + "mph from the " + degtoname(windDir);

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
					File f = new File(fileName);
					FileInputStream imageInFile = new FileInputStream(f);
					byte[] imageData = new byte[(int) f.length()];
					if(imageInFile.read(imageData) > 0)
						day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
				}

				days.add(day);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	private String degtoname(double deg)
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

	private String getCode(String icon)
	{
		switch(icon)
		{
			case "clearsky_day":
				return "01d";
			case "clearsky_night":
				return "01n";
			case "clearsky_polartwilight":
				return "01m";
			case "fair_day":
				return "02d";
			case "fair_night":
				return "02n";
			case "fair_polartwilight":
				return "02m";
			case "partlycloudy_day":
				return "03d";
			case "partlycloudy_night":
				return "03n";
			case "partlycloudy_polartwilight":
				return "03m";
			case "cloudy":
				return "04";
			case "rainshowers_day":
				return "05d";
			case "rainshowers_night":
				return "05n";
			case "rainshowers_polartwilight":
				return "05m";
			case "rainshowersandthunder_day":
				return "06d";
			case "rainshowersandthunder_night":
				return "06n";
			case "rainshowersandthunder_polartwilight":
				return "06m";
			case "sleetshowers_day":
				return "07d";
			case "sleetshowers_night":
				return "07n";
			case "sleetshowers_polartwilight":
				return "07m";
			case "snowshowers_day":
				return "08d";
			case "snowshowers_night":
				return "08n";
			case "snowshowers_polartwilight":
				return "08m";
			case "rain":
				return "09";
			case "heavyrain":
				return "10";
			case "heavyrainandthunder":
				return "11";
			case "sleet":
				return "12";
			case "snow":
				return "13";
			case "snowandthunder":
				return "14";
			case "fog":
				return "15";

			case "sleetshowersandthunder_day":
				return "20d";
			case "sleetshowersandthunder_night":
				return "20n";
			case "sleetshowersandthunder_polartwilight":
				return "20m";
			case "snowshowersandthunder_day":
				return "21d";
			case "snowshowersandthunder_night":
				return "21n";
			case "snowshowersandthunder_polartwilight":
				return "21m";
			case "rainandthunder":
				return "22";
			case "sleetandthunder":
				return "23";
			case "lightrainshowersandthunder_day":
				return "24d";
			case "lightrainshowersandthunder_night":
				return "24n";
			case "lightrainshowersandthunder_polartwilight":
				return "24m";
			case "heavyrainshowersandthunder_day":
				return "25d";
			case "heavyrainshowersandthunder_night":
				return "25n";
			case "heavyrainshowersandthunder_polartwilight":
				return "25m";
			case "lightssleetshowersandthunder_day":
				return "26d";
			case "lightssleetshowersandthunder_night":
				return "26n";
			case "lightssleetshowersandthunder_polartwilight":
				return "26m";
			case "heavysleetshowersandthunder_day":
				return "27d";
			case "heavysleetshowersandthunder_night":
				return "27n";
			case "heavysleetshowersandthunder_polartwilight":
				return "27m";
			case "lightssnowshowersandthunder_day":
				return "28d";
			case "lightssnowshowersandthunder_night":
				return "28n";
			case "lightssnowshowersandthunder_polartwilight":
				return "28m";
			case "heavysnowshowersandthunder_day":
				return "29d";
			case "heavysnowshowersandthunder_night":
				return "29n";
			case "heavysnowshowersandthunder_polartwilight":
				return "29m";
			case "lightrainandthunder":
				return "30";
			case "lightsleetandthunder":
				return "31";
			case "heavysleetandthunder":
				return "32";
			case "lightsnowandthunder":
				return "33";
			case "heavysnowandthunder":
				return "34";

			case "lightrainshowers_day":
				return "40d";
			case "lightrainshowers_night":
				return "40n";
			case "lightrainshowers_polartwilight":
				return "40m";
			case "heavyrainshowers_day":
				return "41d";
			case "heavyrainshowers_night":
				return "41n";
			case "heavyrainshowers_polartwilight":
				return "41m";
			case "lightsleetshowers_day":
				return "42d";
			case "lightsleetshowers_night":
				return "42n";
			case "lightsleetshowers_polartwilight":
				return "42m";
			case "heavysleetshowers_day":
				return "43d";
			case "heavysleetshowers_night":
				return "43n";
			case "heavysleetshowers_polartwilight":
				return "43m";
			case "lightsnowshowers_day":
				return "44d";
			case "lightsnowshowers_night":
				return "44n";
			case "lightsnowshowers_polartwilight":
				return "44m";
			case "heavysnowshowers_day":
				return "45d";
			case "heavysnowshowers_night":
				return "45n";
			case "heavysnowshowers_polartwilight":
				return "45m";
			case "lightrain":
				return "46";
			case "lightsleet":
				return "47";
			case "heavysleet":
				return "48";
			case "lightsnow":
				return "49";
			case "heavysnow":
				return "50";
		}

		return "01d";
	}

	private long convertDaytoTS(String dayName, Locale locale, long lastTS)
	{
		long startTS = lastTS;

		dayName = Html.fromHtml(dayName).toString();

		while(true)
		{
			SimpleDateFormat sdf = new SimpleDateFormat("EEEE", locale);
			String ftime = sdf.format(lastTS);
			//LogMessage("ftime == " + ftime + ", dayName == " + dayName);
			if(dayName.toLowerCase().startsWith(ftime.toLowerCase()))
				return lastTS;

			lastTS += 86400000;

			if(lastTS > startTS + 864000000)
				return 0;
		}
	}

	String[] processWZ(String data)
	{
		return processWZ(data, false);
	}

	String[] processWZ(String data, boolean showHeader)
	{
		if(data == null || data.equals(""))
			return null;

		boolean use_icons = GetBoolPref("use_icons", false);
		boolean metric = GetBoolPref("metric", true);
		List<Day> days = new ArrayList<>();
		long timestamp, lastTS;
		String desc;

		try
		{
			JSONObject jobj = new XmlToJson.Builder(data).build().toJson();
			if (jobj == null)
				return null;

			jobj = jobj.getJSONObject("rss").getJSONObject("channel");
			desc = jobj.getString("title");
			String pubDate = jobj.getString("pubDate");

			SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault());
			timestamp = Objects.requireNonNull(sdf.parse(pubDate)).getTime();
			lastTS = timestamp;

			JSONObject item = jobj.getJSONArray("item").getJSONObject(0);
			String[] items = item.getString("description").trim().split("<b>");
			for (String i : items)
			{
				Day day = new Day();
				String[] tmp = i.split("</b>", 2);
				day.timestamp = convertDaytoTS(tmp[0], new Locale("en", "AU"), lastTS);
				if(day.timestamp != 0)
				{
					sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
					day.day = sdf.format(day.timestamp);
				} else {
					day.day = tmp[0];
				}

				if (tmp.length <= 1)
					continue;

				String[] mybits = tmp[1].split("<br />");
				String myimg = mybits[1].trim().replaceAll("<img src=\"http://www.weatherzone.com.au/images/icons/fcast_30/", "")
									.replaceAll("\">", "").replaceAll(".gif", "").replaceAll("_", "-").trim();
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
					File f = new File(fileName);
					FileInputStream imageInFile = new FileInputStream(f);
					byte[] imageData = new byte[(int) f.length()];
					if(imageInFile.read(imageData) > 0)
						day.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
				}

				day.max = range[1];
				day.min = range[0];
				if(!metric)
				{
					day.max = round(Double.parseDouble(range[1].substring(0, range[1].length() - 7)) * 9.0 / 5.0 + 32.0) + "&deg;F";
					day.min = round((Double.parseDouble(range[0].substring(0, range[0].length() - 7)) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}
				day.text = mydesc;
				days.add(day);
				lastTS = day.timestamp;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	private String[] checkFiles(String url) throws Exception
	{
		String filename = "yahoo-" + new File(url).getName();
		File f = new File(context.getExternalFilesDir(""), "weeWX");
		f = new File(f,"icons");
		f = new File(f, filename);
		if(!f.exists())
		{
			LogMessage("File not found: " + "yahoo-" + filename);
			LogMessage("downloading...");

			String new_url = "https://delungra.com/weewx/yahoo-missing.php?filename=" + filename;
			f = downloadBinary(f, new_url);
			if(f == null)
				return new String[]{null, "f is invalid."};

			if(f.exists())
				return new String[]{"", f.getAbsolutePath()};
		} else {
			LogMessage(filename);
			return new String[]{"", f.getAbsolutePath()};
		}

		return new String[]{null, "Failed to load or download icon: " + filename};
	}

	private String[] checkFilesIt(String url) throws Exception
	{
		String filename = "tempoitalia-" + new File(url).getName();
		File f = new File(context.getExternalFilesDir(""), "weeWX");
		f = new File(f,"icons");
		f = new File(f, filename);
		if(!f.exists())
		{
			LogMessage("File not found: " + "tempoitalia-" + filename);
			LogMessage("downloading...");

			String new_url = "https://delungra.com/weewx/TempoItalia-missing.php?filename=" + filename;
			f = downloadBinary(f, new_url);
			if(f == null)
				return new String[]{null, "f is invalid."};

			if(f.exists())
				return new String[]{"", f.getAbsolutePath()};
		} else {
			//LogMessage(filename);
			return new String[]{"", f.getAbsolutePath()};
		}

		return new String[]{null, "Failed to load or download icon: " + filename};
	}

	String[] processYahoo(String data)
	{
		return processYahoo(data, false);
	}

    String[] processYahoo(String data, boolean showHeader)
    {
	    if(data == null || data.equals(""))
		    return null;

	    boolean metric = GetBoolPref("metric", true);

		List<Day> days = new ArrayList<>();
		long timestamp = GetLongPref("rssCheck", 0) * 1000;
		String desc;
	    Document doc;

	    try
	    {
		    String[] bits = data.split("data-reactid=\"7\">", 8);
		    String[] b = bits[7].split("</h1>", 2);
		    String town = b[0];
		    String rest = b[1];
		    b = rest.split("data-reactid=\"8\">", 2)[1].split("</div>", 2);
		    String country = b[0];
		    rest = b[1];

		    desc = town.trim() + ", " + country.trim();
		    int[] daynums = new int[]{196, 221, 241, 261, 281, 301, 321, 341, 361, 381};
		    for (int startid : daynums)
		    {
			    Day myday = new Day();

			    int endid;
			    long last_ts = System.currentTimeMillis();

			    if (startid == 196)
				    endid = startid + 24;
			    else
				    endid = startid + 19;

			    String tmpstr = rest.split("<span data-reactid=\"" + startid + "\">", 2)[1];
			    bits = tmpstr.split("data-reactid=\"" + endid + "\">", 2);
			    tmpstr = bits[0];
			    rest = bits[1];
			    String dow = tmpstr.split("</span>", 2)[0].trim();
			    myday.timestamp = convertDaytoTS(dow, new Locale("en", "US"), last_ts);
			    SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
			    myday.day = sdf.format(myday.timestamp);

			    myday.text = tmpstr.split("<img alt=\"", 2)[1].split("\"", 2)[0].trim();
			    myday.icon = tmpstr.split("<img alt=\"", 2)[1].split("\"", 2)[1];
			    myday.icon = myday.icon.split("src=\"", 2)[1].split("\"", 2)[0].trim();

			    myday.max = tmpstr.split("data-reactid=\"" + (startid + 10) + "\">", 2)[1];
			    myday.max = myday.max.split("</span>", 2)[0].trim();
			    myday.min = tmpstr.split("data-reactid=\"" + (startid + 13) + "\">", 2)[1];
			    myday.min = myday.min.split("</span>", 2)[0].trim();

			    doc = Jsoup.parse(myday.max.trim());
			    myday.max = doc.text();

			    doc = Jsoup.parse(myday.min.trim());
			    myday.min = doc.text();

			    myday.max = myday.max.substring(0, myday.max.length() - 1);
			    myday.min = myday.min.substring(0, myday.min.length() - 1);

			    if (metric)
			    {
				    myday.max = round((Double.parseDouble(myday.max) - 32.0) * 5.0 / 9.0) + "&deg;C";
				    myday.min = round((Double.parseDouble(myday.min) - 32.0) * 5.0 / 9.0) + "&deg;C";
			    } else
			    {
				    myday.max += "&deg;F";
				    myday.min += "&deg;F";
			    }

			    String[] ret = checkFiles(myday.icon);
				if (ret[0] != null)
			    {
				    File f = new File(ret[1]);
				    FileInputStream imageInFile = new FileInputStream(f);
				    byte[] imageData = new byte[(int) f.length()];
				    if(imageInFile.read(imageData) > 0)
				    	myday.icon = "data:image/jpeg;base64," + Base64.encodeToString(imageData, Base64.DEFAULT);
			    } else
				    return ret;

				LogMessage(myday.toString());
			    days.add(myday);
		    }
	    } catch (Exception e) {
		    e.printStackTrace();
		    return null;
	    }

	    return new String[]{generateForecast(days, timestamp, showHeader), desc};
    }

	void SendIntents()
	{
		Intent intent = new Intent();
		intent.setAction(Common.UPDATE_INTENT);
		context.sendBroadcast(intent);
		Common.LogMessage("update_intent broadcast.");

		intent = new Intent(context, WidgetProvider.class);
		intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
		int[] ids = AppWidgetManager.getInstance(context.getApplicationContext()).getAppWidgetIds(new ComponentName(context.getApplicationContext(), WidgetProvider.class));
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
		context.sendBroadcast(intent);
		Common.LogMessage("widget intent broadcasted");
	}

	void SendRefresh()
	{
		Intent intent = new Intent();
		intent.setAction(Common.REFRESH_INTENT);
		context.sendBroadcast(intent);
		Common.LogMessage("refresh_intent broadcast.");

		intent = new Intent(context, WidgetProvider.class);
		intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
		int[] ids = AppWidgetManager.getInstance(context.getApplicationContext()).getAppWidgetIds(new ComponentName(context.getApplicationContext(), WidgetProvider.class));
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

		t = new Thread(() ->
		{
			try
			{
				String fromURL = GetStringPref("BASE_URL", "");
				if (fromURL.equals(""))
					return;

				reallyGetWeather(fromURL);
				SendRefresh();
			} catch (InterruptedException | InterruptedIOException ie) {
				ie.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
				SetStringPref("lastError", e.toString());
				SendFailedIntent();
			}
		});

		t.start();
	}

	void reallyGetWeather(String fromURL) throws Exception
	{
		String line = downloadString(fromURL);
		if(!line.equals(""))
		{
			String[] bits = line.split("\\|");
			if (Double.parseDouble(bits[0]) < inigo_version)
			{
				if(GetLongPref("inigo_version", 0) < Common.inigo_version)
				{
					SetLongPref("inigo_version", Common.inigo_version);
					sendAlert();
				}
			}

			if (Double.parseDouble(bits[0]) >= 4000)
			{
				StringBuilder sb = new StringBuilder();
				for (int i = 1; i < bits.length; i++)
				{
					if (sb.length() > 0)
						sb.append("|");
					sb.append(bits[i]);
				}

				line = sb.toString().trim();
			}

			SetStringPref("LastDownload", line);
			SetLongPref("LastDownloadTime", Math.round(System.currentTimeMillis() / 1000.0));
		}
	}

	//	https://stackoverflow.com/questions/3841317/how-do-i-see-if-wi-fi-is-connected-on-android
	boolean checkConnection()
	{
		if(GetBoolPref("onlyWIFI", false))
		{
			WifiManager wifiMgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

			if (wifiMgr.isWifiEnabled())
			{
				WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
				return wifiInfo.getNetworkId() != -1;
			}

			return false;
		}

		return true;
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

	// https://stackoverflow.com/questions/8710515/reading-an-image-file-into-bitmap-from-sdcard-why-am-i-getting-a-nullpointerexc

	private Bitmap loadImage(String fileName)
	{
		File f = new File(context.getExternalFilesDir(""), "weeWX");
		f = new File(f, "icons");
		f = new File(f, fileName);

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = false;
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		return BitmapFactory.decodeFile(f.getAbsolutePath(), options);
	}

	private String checkImage(String fileName, String icon)
	{
		File f = new File(context.getExternalFilesDir(""), "weeWX");
		f = new File(f, "icons");
		f = new File(f, fileName);
		//LogMessage("f = " + f.getAbsolutePath());
		if(f.exists() && f.isFile() && f.length() > 0)
			return f.getAbsolutePath();

		// Icon is missing we should probably ask to send information as feedback
		if(icon != null)
			downloadImage(fileName, icon);

		return icon;
	}

	@SuppressWarnings("unused")
	private void downloadImage(final String fileName, final String imageURL)
	{
		Thread t = new Thread(() ->
		{
			if (fileName == null || fileName.equals("") || imageURL == null || imageURL.equals(""))
				return;

			Common.LogMessage("checking: " + imageURL);

			try
			{
				File f = new File(context.getExternalFilesDir(""), "weeWX");
				f = new File(f, "icons");
				if (!f.exists())
					if (!f.mkdirs())
						return;

				f = new File(f, fileName);

				if(f.exists() && f.isFile() && f.length() > 0)
					return;

				LogMessage("f == " + f.getAbsolutePath());
				LogMessage("imageURL == " + imageURL);

				downloadJSOUP(f, imageURL);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		t.start();
	}

	private Bitmap combineImage(Bitmap bmp1, String fnum, String snum)
	{
		try
		{
			int x1 = bmp1.getHeight();
			int y1 = bmp1.getWidth();

			Bitmap bmp = bmp1.copy(Bitmap.Config.ARGB_8888, true);

			Paint paint = new Paint();
			paint.setAntiAlias(true);
			Canvas comboImage = new Canvas(bmp);
			comboImage.drawBitmap(bmp1, 0f, 0f, null);

			if (!fnum.equals("%") || !snum.equals("%"))
			{
				Bitmap bmp2 = loadImage("wgovoverlay.jpg");
				paint = new Paint();
				paint.setAlpha(100);
				comboImage.drawBitmap(bmp2, 0f, bmp1.getHeight() - bmp2.getHeight(), paint);
			}

			if (!fnum.equals("%") && !snum.equals("%"))
			{
				// Draw arrow
				paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(0xff00487b);
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
				paint.setColor(0xff00487b);
				paint.setTextSize(13);
				paint.setTypeface(tf_bold);

				comboImage.drawText(fnum, 0, y1 - 2, paint);
			}

			if(!snum.equals("%"))
			{
				paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(0xff00487b);
				paint.setTextSize(13);
				paint.setTypeface(tf_bold);

				Rect textBounds = new Rect();
				paint.getTextBounds(snum, 0, snum.length(), textBounds);
				int width = (int) paint.measureText(snum);

				comboImage.drawText(snum, x1 - width, y1 - 2, paint);
			}

			return bmp;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private Bitmap combineImages(Bitmap bmp1, Bitmap bmp2, String fimg, String simg, String fnum, String snum)
	{
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
				case "L":
					bmp1 = Bitmap.createBitmap(bmp1, 0, 0, x1 / 2, y1);
					break;
				case "R":
					bmp1 = Bitmap.createBitmap(bmp1, x1 / 2, 0, x1, y1);
					break;
				default:
					bmp1 = Bitmap.createBitmap(bmp1, x1 / 4, 0, x1 * 3 / 4, y1);
					break;
			}

			bits = nws.getString(simg).split("\\|");
			switch (bits[1].trim())
			{
				case "L":
					bmp2 = Bitmap.createBitmap(bmp2, 0, 0, x2 / 2, y2);
					break;
				case "R":
					bmp2 = Bitmap.createBitmap(bmp2, x2 / 2, 0, x2, y2);
					break;
				default:
					bmp2 = Bitmap.createBitmap(bmp2, x2 / 4, 0, x2 * 3 / 4, y2);
					break;
			}

			Bitmap bmp = Bitmap.createBitmap(x1, y1, Bitmap.Config.ARGB_8888);
			bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);

			Paint paint = new Paint();
			paint.setAntiAlias(true);
			Canvas comboImage = new Canvas(bmp);
			comboImage.drawBitmap(bmp1, 0f, 0f, null);
			comboImage.drawBitmap(bmp2, Math.round(x1 / 2.0), 0f, null);

			paint.setColor(0xff000000);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(4);
			comboImage.drawLine( Math.round(x1 / 2.0), 0, Math.round(x1 / 2.0), y1, paint);

			paint.setColor(0xffffffff);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(2);
			comboImage.drawLine( Math.round(x1 / 2.0), 0, Math.round(x1 / 2.0), y1, paint);

			if(!fnum.equals("%") || !snum.equals("%"))
			{
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = false;
				Bitmap bmp4 = loadImage("wgovoverlay.jpg");
				paint = new Paint();
				paint.setAlpha(100);
				comboImage.drawBitmap(bmp4, 0f, bmp1.getHeight() - bmp4.getHeight(), paint);
			}

			if (!fnum.equals("%") && !snum.equals("%"))
			{
				// Draw arrow
				paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(0xff00487b);
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
				paint.setColor(0xff00487b);
				paint.setTextSize(13);
				paint.setTypeface(tf_bold);

				comboImage.drawText(fnum, 2, y1 - 2, paint);
			}

			if(!snum.equals("%"))
			{
				paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(0xff00487b);
				paint.setTextSize(13);
				paint.setTypeface(tf_bold);

				Rect textBounds = new Rect();
				paint.getTextBounds(snum, 0, snum.length(), textBounds);
				int width = (int) paint.measureText(snum);

				comboImage.drawText(snum, x1 - width - 2, y1 - 2, paint);
			}

			return bmp;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	// https://stackoverflow.com/questions/1540272/android-how-to-overlay-a-bitmap-draw-over-a-bitmap

	private Bitmap overlay(Bitmap bmp1, Bitmap bmp2, String s)
	{
		Paint paint = new Paint();
		paint.setAlpha(100);

		Bitmap bmp3 = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
		Canvas canvas = new Canvas(bmp3);
		canvas.drawBitmap(bmp1, 0f, 0f, null);
		canvas.drawBitmap(bmp2, 0f, bmp1.getHeight() - bmp2.getHeight(), paint);
		paint.setAntiAlias(true);
		paint.setColor(0xff00487b);
		paint.setTextSize(13);
		paint.setTypeface(tf_bold);
		Rect textBounds = new Rect();
		paint.getTextBounds(s, 0, s.length(), textBounds);
		int width = (int)paint.measureText(s);

		canvas.drawText(s, bmp1.getWidth() - width, bmp1.getHeight() - 5, paint);

		return bmp3;
	}

	// https://stackoverflow.com/questions/19945411/android-java-how-can-i-parse-a-local-json-file-from-assets-folder-into-a-listvi

	@SuppressWarnings("CharsetObjectCanBeUsed")
	private void loadNWS()
	{
		try
		{
			InputStream is = context.getResources().openRawResource(R.raw.nws);
			int size = is.available();
			byte[] buffer = new byte[size];
			if(is.read(buffer) > 0)
				nws = new JSONObject(new String(buffer, "UTF-8"));
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	String downloadSettings(String url) throws Exception
	{
		String UTF8_BOM = "\uFEFF";

		SetStringPref("SETTINGS_URL", url);
		String cfg = downloadString(url).trim();
		if(cfg.startsWith(UTF8_BOM))
			cfg = cfg.substring(1).trim();
		return cfg;
	}

	File downloadRADAR(String radar) throws Exception
	{
		LogMessage("starting to download image from: " + radar);
		File file = new File(context.getFilesDir(), "/radar.gif.tmp");
		return downloadBinary(file, radar);
	}

	@SuppressWarnings("SameReturnValue")
	boolean checkURL(String setting) throws Exception
	{
		Common.LogMessage("checking: " + setting);
		URL url = new URL(setting);
		URLConnection conn = url.openConnection();
		conn.connect();

		return true;
	}

	String downloadForecast() throws Exception
	{
		return downloadForecast(GetStringPref("fctype", "Yahoo"), GetStringPref("FORECAST_URL", ""), GetStringPref("bomtown", ""));
	}

	String downloadForecast(String fctype, String forecast, String bomtown) throws Exception
	{
		String tmp;

		if(!fctype.equals("bom2"))
			tmp = downloadString(forecast);
		else
			tmp = downloadString2(forecast);

		if(fctype.equals("bom.gov.au"))
		{
			boolean found = false;

			JSONObject jobj = Objects.requireNonNull(new XmlToJson.Builder(tmp).build().toJson()).getJSONObject("product");
			String content = jobj.getJSONObject("amoc").getJSONObject("issue-time-local").getString("content");
			JSONArray area = jobj.getJSONObject("forecast").getJSONArray("area");

			for(int i = 0; i < area.length(); i++)
			{
				JSONObject o = area.getJSONObject(i);
				if(o.getString("description").equals(bomtown))
				{
					o.put("content", content);
					tmp = o.toString();
					found = true;
					break;
				}
			}

			if(!found)
			{
				SetStringPref("lastError", "Unable to match '" + bomtown + "'. Make sure you selected the right state file ID and a town where the BoM produces forecasts.");
				throw new Exception("Unable to match '" + bomtown + "'. Make sure you selected the right state file ID and a town where the BoM produces forecasts.");
			}
		}

		return tmp;
	}

	private String downloadString2(String fromURL) throws Exception
	{
		Connection.Response resultResponse = Jsoup.connect(fromURL)
													.userAgent(UA)
													.referrer("http://www.bom.gov.au")
													.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
													.header("Cache-Control", "max-age=0")
													.header("Accept-Language", "en-au")
													.header("Upgrade-Insecure-Requests", "1")
													.header("Accept-Encoding", "deflate")
													.header("Connection", "keep-alive")
													.maxBodySize(Integer.MAX_VALUE)
													.ignoreContentType(true).execute();
		return resultResponse.body();
	}

	private int responseCount(Response response)
	{
		int result = 1;
		while ((response = response.priorResponse()) != null)
			result++;
		return result;
	}

	private String downloadString(String fromURL) throws Exception
	{
		OkHttpClient client;
		Request request;

		Uri uri = Uri.parse(fromURL);
		if(uri.getUserInfo() != null && uri.getUserInfo().contains(":"))
		{
			String[] UC = uri.getUserInfo().split(":");
			LogMessage("uri username = " + UC[0]);

			client = new OkHttpClient.Builder()
				.connectTimeout(60, TimeUnit.SECONDS)
				.writeTimeout(60, TimeUnit.SECONDS)
				.readTimeout(60, TimeUnit.SECONDS)
				.connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS))
				.authenticator((route, response) ->
				{
					if (responseCount(response) >= 3)
						return null;

					String credential = Credentials.basic(UC[0], UC[1]);
					return response.request().newBuilder().header("Authorization", credential).build();
				})
				.build();
		} else {
			client = new OkHttpClient.Builder()
					.connectTimeout(60, TimeUnit.SECONDS)
					.writeTimeout(60, TimeUnit.SECONDS)
					.readTimeout(60, TimeUnit.SECONDS)
					.connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS))
					.build();
		}

		request = new Request.Builder().url(fromURL)
				.addHeader("Referer", fromURL)
				.addHeader("User-Agent", UA)
				.build();

		try (Response response = client.newCall(request).execute()) {
			assert response.body() != null;
			String rb = Objects.requireNonNull(response.body()).string().trim();
			LogMessage(rb);
			return rb;
		}
	}

	@SuppressWarnings({"unused", "SameParameterValue"})
	private void writeFile(String fileName, String data) throws Exception
	{
		File dir = new File(context.getExternalFilesDir(""), "weeWX");
		if (!dir.exists())
			if (!dir.mkdirs())
				return;

		File f = new File(dir, fileName);

		OutputStream outputStream = new FileOutputStream(f);
		outputStream.write(data.getBytes());
		outputStream.flush();
		outputStream.close();

		publish(f);
	}

	@SuppressWarnings({"SameParameterValue"})
	File downloadJSOUP(File f, String fromURL) throws Exception
	{
		File dir = f.getParentFile();
		assert dir != null;
		if (!dir.exists())
			if (!dir.mkdirs())
				return null;

		Connection.Response resultResponse = Jsoup.connect(fromURL).userAgent(UA).maxBodySize(Integer.MAX_VALUE).ignoreContentType(true).execute();
		FileOutputStream out = new FileOutputStream(f);
		out.write(resultResponse.bodyAsBytes());
		out.flush();
		out.close();
		publish(f);

		return f;
	}

	private File downloadBinary(File f, String fromURL) throws Exception
	{
		File dir = f.getParentFile();
		assert dir != null;
		if (!dir.exists())
			if (!dir.mkdirs())
				return null;

		OkHttpClient client;
		Request request;
		OutputStream outputStream = new FileOutputStream(f);

		Uri uri = Uri.parse(fromURL);
		if(uri.getUserInfo() != null && uri.getUserInfo().contains(":"))
		{
			String[] UC = uri.getUserInfo().split(":");
			LogMessage("uri username = " + UC[0]);

			client = new OkHttpClient.Builder()
					.connectTimeout(60, TimeUnit.SECONDS)
					.writeTimeout(60, TimeUnit.SECONDS)
					.readTimeout(60, TimeUnit.SECONDS)
					.connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS))
					.authenticator((route, response) ->
					{
						if (responseCount(response) >= 3)
							return null;

						String credential = Credentials.basic(UC[0], UC[1]);
						return response.request().newBuilder().header("Authorization", credential).build();
					})
					.build();
		} else {
			client = new OkHttpClient.Builder()
					.connectTimeout(60, TimeUnit.SECONDS)
					.writeTimeout(60, TimeUnit.SECONDS)
					.readTimeout(60, TimeUnit.SECONDS)
					.connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS))
					.build();
		}

		request = new Request.Builder().url(fromURL)
				.addHeader("Referer", fromURL)
				.addHeader("User-Agent", UA)
				.build();

		try (Response response = client.newCall(request).execute()) {
			assert response.body() != null;
			outputStream.write(Objects.requireNonNull(response.body()).bytes());
			outputStream.flush();
			outputStream.close();
			publish(f);
			return f;
		}
	}

	private void publish(File f)
	{
		LogMessage("wrote to " + f.getAbsolutePath());
		if (f.exists() && f.isFile())
		{
			MediaScannerConnection.scanFile(context.getApplicationContext(), new String[]{f.getAbsolutePath()}, null, null);
			context.getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(f)));
		}

		if(f.exists() && f.isDirectory())
		{
			MediaScannerConnection.scanFile(context.getApplicationContext(), new String[] {f.getAbsolutePath()}, null, null);
		}
	}

	@SuppressWarnings("SameReturnValue")
	boolean downloadIcons() throws Exception
	{
		File f = new File(context.getExternalFilesDir(""), "weeWX");
		File dir = f;

		if(!dir.exists() && !dir.mkdirs())
			throw new Exception("There was a problem making the icons directory, you will need to try again.");

		if(!dir.exists() && dir.mkdirs())
			publish(dir);

		f = new File(f, "icon.zip");
		if(downloadJSOUP(f, icon_url) == null)
			throw new Exception("There was a problem downloading icons, you will need to try again.");

		dir = new File(dir, "icons");
		if(!dir.exists() && dir.mkdirs())
			publish(dir);

		if(!dir.exists())
			throw new Exception("There was a problem making the icons directory, you will need to try again.");

		unzip(f, dir);
		if(!f.delete())
			throw new Exception("There was a problem cleaning up the zip file.");

		return true;
	}

	private void unzip(File zipFilePath, File destDir) throws Exception
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
			if (!canonicalPath.startsWith(destDir.toString()))
			{
				LogMessage("File '" + canonicalPath + "' is a security problem, skipping.");
				continue;
			}
			LogMessage("Unzipping to " + newFile.getAbsolutePath());
			File dir = new File(Objects.requireNonNull(newFile.getParent()));
			if(!dir.exists() && !dir.mkdirs())
				throw new Exception("There was a problem creating 1 or more directories on external storage");

			FileOutputStream fos = new FileOutputStream(newFile);
			int len;
			while ((len = zis.read(buffer)) > 0)
				fos.write(buffer, 0, len);
			fos.flush();
			fos.close();

			publish(newFile);
			//close this ZipEntry
			zis.closeEntry();
			ze = zis.getNextEntry();
		}

		zis.closeEntry();
		zis.close();
		fis.close();
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	boolean checkForImages()
	{
		File dir = new File(context.getExternalFilesDir(""), "weeWX");
		dir = new File(dir, "icons");
		if(!dir.exists() || !dir.isDirectory())
			return false;

		String[] files = new String[]{"aemet_11_g.png", "apixu_113.png", "bom1.png", "bom2clear.png", "dwd_pic_0_8.png",
										"i1.png", "ilmeteo_ss1.png", "met0.png", "mf_j_w1_0_n_2.png", "ms_cloudy.png",
										"wca00.png", "wgovbkn.jpg", "wzclear.png", "y01d.png", "yahoo0.gif", "yrno01d.png"};
		for(String file : files)
		{
			File f = new File(dir, file);
			if (!f.exists() || !f.isFile())
				return false;
		}

		return true;
	}

	void getForecast()
	{
		getForecast(false);
	}

	@SuppressWarnings({"WeakerAccess", "SameParameterValue"})
	void getForecast(boolean force)
	{
		if(GetBoolPref("radarforecast", true))
		{
			return;
		}

		final String forecast_url = GetStringPref("FORECAST_URL", "");

		if(forecast_url.equals(""))
		{
			return;
		}

		if(!checkConnection() && !force)
		{
			LogMessage("Not on wifi and not a forced refresh");
			return;
		}

		final long curtime = Math.round(System.currentTimeMillis() / 1000.0);

		if(GetStringPref("forecastData", "").equals("") || GetLongPref("rssCheck", 0) + 7190 < curtime)
		{
			LogMessage("no forecast data or cache is more than 2 hour old");
		} else {
			LogMessage("cache isn't more than 2 hour old");
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
			String date = sdf.format(GetLongPref("rssCheck", 0) * 1000);
			LogMessage("rsscheck == " + date);
			date = sdf.format(curtime * 1000);
			LogMessage("curtime == " + date);
			return;
		}

		LogMessage("forecast checking: " + forecast_url);

		Thread t = new Thread(() ->
		{
			try
			{
				String tmp = downloadForecast();
				if(tmp != null)
				{
					LogMessage("updating rss cache");
					SetLongPref("rssCheck", curtime);
					SetStringPref("forecastData", tmp);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		t.start();
	}

	int getSystemTheme()
	{
		switch (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
		{
			case Configuration.UI_MODE_NIGHT_NO:
				Common.LogMessage("Night mode off");
				return 0;
			case Configuration.UI_MODE_NIGHT_YES:
				Common.LogMessage("Night mode on");
				return 1;
			case Configuration.UI_MODE_NIGHT_UNDEFINED:
				Common.LogMessage("Night mode not set, default to off");
				return 0;
		}

		return 0;
	}
}