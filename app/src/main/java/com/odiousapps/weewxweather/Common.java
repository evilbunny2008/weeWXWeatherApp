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
import android.os.Environment;
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
	static String REFRESH_INTENT = "com.odiousapps.weewxweather.REFRESH_INTENT";
	static String TAB0_INTENT = "com.odiousapps.weewxweather.TAB0_INTENT";
	static String EXIT_INTENT = "com.odiousapps.weewxweather.EXIT_INTENT";
	static String INIGO_INTENT = "com.odiousapps.weewxweather.INIGO_UPDATE";
	static String FAILED_INTENT = "com.odiousapps.weewxweather.FAILED_INTENT";

	private static final long inigo_version = 4000;
	static final long icon_version = 4;
	private static final String icon_url = "https://github.com/evilbunny2008/weeWXWeatherApp/releases/download/0.7.11/icons.zip";

	private Thread t = null;
	private JSONObject nws = null;
	private JSONArray conditions = null;

	private Typeface tf;
	private Map<String, String> lookupTable = new HashMap<>();

	static String ssheader = "<link rel='stylesheet' href='file:///android_asset/weathericons.css'>" +
								"<link rel='stylesheet' href='file:///android_asset/weathericons_wind.css'>" +
								"<link rel='stylesheet' type='text/css' href='file:///android_asset/flaticon.css'>";

	static final float[] NEGATIVE = {
		-1.0f,     0,     0,    0, 255, // red
			0, -1.0f,     0,    0, 255, // green
			0,     0, -1.0f,    0, 255, // blue
			0,     0,     0, 1.0f,   0  // alpha
	};

	Common(Context c)
	{
		System.setProperty("http.agent", UA);
		this.context = c;
		tf = Typeface.create("Arial", Typeface.NORMAL);

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
		{
			int len = value.indexOf("\n");
			if(len <= 0)
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
	void SetLongPref(String name, long value)
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

	@SuppressWarnings({"SameParameterValue", "unused"})
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

	String[] processDarkSky(String data)
	{
		return processDarkSky(data, false);
	}

	String[] processDarkSky(String data, boolean showHeader)
	{
		if (data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		StringBuilder out = new StringBuilder();
		String tmp;
		String desc;

		long mdate = (long)GetIntPref("rssCheck", 0) * 1000;
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
		String ftime = sdf.format(mdate);

		tmp = "<div style='font-size:12pt;'>" + ftime + "</div>";
		out.append(tmp);
		tmp = "<table style='width:100%;'>\n";
		out.append(tmp);

		try
		{
			JSONObject jobj = new JSONObject(data);
			desc = jobj.getString("latitude") + ", " + jobj.getString("longitude");
			JSONObject daily = jobj.getJSONObject("daily");
			JSONArray  jarr = daily.getJSONArray("data");
			for(int i = 0; i < jarr.length(); i++)
			{
				JSONObject day = jarr.getJSONObject(i);
				String icon = day.getString("icon");
				sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
				ftime = sdf.format(((long)day.getInt("time") * 1000));

				String max = Integer.toString(round(day.getInt("temperatureHigh")));
				String min = Integer.toString(round(day.getInt("temperatureLow")));

				if(metric)
				{
					max += "&deg;C";
					min += "&deg;C";
				} else {
					max += "&deg;F";
					min += "&deg;F";
				}

				tmp = "<tr><td style='width:10%; vertical-align:top;' rowspan='2'><i style='font-size:30px;' class='wi wi-forecast-io-" + icon + "'></i></td>";
				out.append(tmp);

				tmp = "<td style='width:80%;'><b>" + ftime + "</b></td>";
				out.append(tmp);

				tmp = "<td style='width:10%;text-align:right;'><b>" + max + "</b></td></tr>";
				out.append(tmp);

				tmp = "<tr><td>" + day.getString("summary") + "</td>";
				out.append(tmp);

				tmp = "<td style='width:10%;text-align:right;'>" + min + "</td></tr>";
				out.append(tmp);

				if(showHeader)
				{
					tmp = "<tr><td style='font-size:10pt;' colspan='5'>&nbsp;</td></tr>";
					out.append(tmp);
				}
			}
			tmp = "</table>";
			out.append(tmp);
			return new String[]{out.toString(), desc};
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	String[] processSMN(String data)
	{
		return processSMN(data, false);
	}

	String[] processSMN(String data, boolean showHeader)
	{
		if (data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
		StringBuilder out = new StringBuilder();
		String tmp;
		String desc;

		try
		{
			data = data.split("<div id=\"block_city_detail\"", 2)[1]; //.split("</div></div>", 2)[0].trim();
			desc = data.split("<h1>", 2)[1].split("</h1>", 2)[0].trim();

			String ftime = data.split("<h3 class=\"infoTitle\">Actualizado: ", 2)[1].split("hs.</h3>", 2)[0].trim();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
			ftime = sdf.format(System.currentTimeMillis()) + " " + ftime;

			sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
			long mdate = sdf.parse(ftime).getTime();
			sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
			ftime = sdf.format(mdate);

			tmp = "<div style='font-size:12pt;'>" + ftime + "</div>";
			out.append(tmp);
			tmp = "<table style='width:100%;'>\n";
			out.append(tmp);

			data = data.split("<div class=\"panels-flexible-row panels-flexible-row-base2cols-1 clearfix", 2)[0]
					   .split("</div></div>")[0].trim();

			String[] bits = data.split("extendedForecastDay");
			for (int i = 1; i < bits.length; i++)
			{
				String line = bits[i].trim();
				String day = line.split("<h3>", 2)[1].split("</h3>", 2)[0].trim();
				String min = line.split("<p>Min ", 2)[1].split("ºC", 2)[0].trim();
				String max = line.split("Max ",2)[1].split("ºC", 2)[0].trim();
				String morning = line.split("<p><b>")[1].split("</b>", 2)[0].trim();
				String night = line.split("<p><b>")[2].split("</b>", 2)[0].trim();
				String icon1 = line.split("<i class=\"wi ")[1].split("\"></i>", 2)[0].trim();
				String icon2 = line.split("<i class=\"wi ")[2].split("\"></i>", 2)[0].trim();
				String text1 = line.split("<div class=\"col col-md-8\">")[1].split("</div>", 2)[0].trim();
				String text2 = line.split("<div class=\"col col-md-8\">")[2].split("</div>", 2)[0].trim();

				if(!use_icons)
				{
					tmp = "<tr><td style='width:10%; vertical-align:top;' rowspan='2'><i style='font-size:30px;' class='wi " + icon1 + "'></i></td>";
				} else {
					String icon = "https://www.smn.gob.ar/sites/all/themes/smn/images/weather-icons/big-" + icon1 + ".png";
					String fileName = "smn_" + icon1.replaceAll("-", "_") + ".png";
					fileName = checkImage(fileName, icon);
					tmp = "<tr><td style='width:10%; vertical-align:top;' rowspan='2'><img width='40px' src='file://" + fileName + "'></td>";
				}
				out.append(tmp);

				tmp = "<td style='width:80%;'><b>" + day + " - " + morning + "</b></td>";
				out.append(tmp);

				if(metric)
				{
					max = max + "&deg;C";
					min = min + "&deg;C";
				} else {
					max = round((Double.parseDouble(max) * 9.0 / 5.0) + 32.0) + "&deg;F";
					min = round((Double.parseDouble(min) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}

				if(!max.equals("&deg;C"))
					tmp = "<td style='width:10%;text-align:right;'><b>" + max + "</b></td></tr>";
				else
					tmp = "<td style='width:10%;text-align:right;'><b>&nbsp;</b></td></tr>";
				out.append(tmp);

				tmp = "<tr><td colspan='2'>" + text1 + "</td></tr>";
				out.append(tmp);

				if(!use_icons)
				{
					tmp = "<tr><td style='width:10%; vertical-align:top;' rowspan='2'><i style='font-size:30px;' class='wi " + icon2 + "'></i></td>";
				} else {
					String icon = "https://www.smn.gob.ar/sites/all/themes/smn/images/weather-icons/big-" + icon2 + ".png";
					String fileName = "smn_" + icon2.replaceAll("-", "_") + ".png";
					fileName = checkImage(fileName, icon);
					tmp = "<tr><td style='width:10%; vertical-align:top;' rowspan='2'><img width='40px' src='file://" + fileName + "'></td>";
				}
				out.append(tmp);

				tmp = "<td style='width:80%;'><b>" + day + " - " + night + "</b></td>";
				out.append(tmp);

				if(!min.equals("&deg;C"))
					tmp = "<td style='width:10%;text-align:right;'><b>" + min + "</b></td></tr>";
				else
					tmp = "<td style='width:10%;text-align:right;'><b>&nbsp;</b></td></tr>";
				out.append(tmp);

				tmp = "<tr><td colspan='2'>" + text2 + "</td></tr>";
				out.append(tmp);

				if(showHeader)
				{
					tmp = "<tr><td style='font-size:10pt;' colspan='5'>&nbsp;</td></tr>";
					out.append(tmp);
				}
			}

			tmp = "</table>";
			out.append(tmp);
			return new String[]{out.toString(), desc};
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	String[] processMF(String data)
	{
		return processMF(data, false);
	}

	String[] processMF(String data, boolean showHeader)
	{
		if (data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		StringBuilder out = new StringBuilder();
		String tmp;
		String desc;

		try
		{
			desc = data.split("<h1>", 2)[1].split("</h1>", 2)[0].trim();

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
			String ftime = sdf.format(System.currentTimeMillis());
			ftime += " " + data.split("<p class=\"heure-de-prevision\">Prévisions météo actualisées à ", 2)[1].split("</p>", 2)[0].trim();

			sdf = new SimpleDateFormat("yyyy-MM-dd HH'h'mm", Locale.getDefault());
			long mdate = sdf.parse(ftime).getTime();
			sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.FRANCE);
			ftime = sdf.format(mdate);

			tmp = "<div style='font-size:12pt;'>" + ftime + "</div>";
			out.append(tmp);
			tmp = "<table style='width:100%;'>\n";
			out.append(tmp);

			data = data.split("<!-- LISTE JOURS -->", 2)[1].split("<!-- LISTE JOURS/ -->", 2)[0].trim();

			if(lookupTable.size() <= 0)
			{
				makeTable();
				LogMessage("Loaded lookup table.");
			}

			String[] bits = data.split("title=\"");
			for(int i = 1; i < bits.length; i++)
			{
				String bit = bits[i].trim();

				String text = bit.split("\">", 2)[0].trim();

				sdf = new SimpleDateFormat("-MM-yyyy", Locale.getDefault());
				String day = bit.split("<a>", 2)[1].split("</a>", 2)[0].trim() + sdf.format(System.currentTimeMillis());
				String min = bit.split("class=\"min-temp\">", 2)[1].split("°C Minimale", 2)[0].trim();
				String max = bit.split("class=\"max-temp\">", 2)[1].split("°C Maximale", 2)[0].trim();
				String icon = bit.split("<dd class=\"pic40 ", 2)[1].split("\">",2)[0].trim();

				sdf = new SimpleDateFormat("EEE dd-MM-yyyy", Locale.FRANCE);
				mdate = sdf.parse(day).getTime();
				sdf = new SimpleDateFormat("EEEE", Locale.FRANCE);
				day = sdf.format(mdate);

				while(lookupTable.get(icon) == null && icon.length() > 0)
				{
					icon = icon.substring(0, icon.length() - 1);
					LogMessage("icon == " + icon);
				}

				if(lookupTable.get(icon) != null)
					tmp = "<tr><td style='width:10%;' rowspan='2'><img width='40px' src='" + lookupTable.get(icon) + "'></td>";
				else
					tmp = "<tr><td style='width:10%;' rowspan='2'>N/A</td>";
				out.append(tmp);

				tmp = "<td style='width:80%;'><b>" + day + "</b></td>";
				out.append(tmp);

				if(metric)
				{
					max = max + "&deg;C";
					min = min + "&deg;C";
				} else {
					max = round((Double.parseDouble(max) * 9.0 / 5.0) + 32.0) + "&deg;F";
					min = round((Double.parseDouble(min) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}

				if(!max.equals("&deg;C"))
					tmp = "<td style='width:10%;text-align:right;'><b>" + max + "</b></td></tr>";
				else
					tmp = "<td style='width:10%;text-align:right;'><b>&nbsp;</b></td></tr>";
				out.append(tmp);

				tmp = "<tr><td>" + text + "</td>";
				out.append(tmp);

				if(!min.equals("&deg;C"))
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

			tmp = "</table>";
			out.append(tmp);
			return new String[]{out.toString(), desc};
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
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
		String tmp;
		StringBuilder out = new StringBuilder();

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
			String day = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String month = obs.substring(i, j);
			i = j + 1;
			j = obs.length();
			String year = obs.substring(i, j);

			obs = hour + ":" + minute + " " + ampm + " " + day + " " + month + " " + year;

			SimpleDateFormat sdf = new SimpleDateFormat("h:mm aa d MMMM yyyy", Locale.getDefault());
			long mdate = sdf.parse(obs).getTime();
			sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
			obs = sdf.format(mdate);

			tmp = "<div style='font-size:12pt;'>" + obs + "</div>";
			out.append(tmp);
			tmp = "<table style='width:100%;'>\n";
			out.append(tmp);

			String[] bits = fcdiv.split("<dl class=\"forecast-summary\">");
			String bit = bits[1];
			day = bit.split("<a href=\"", 2)[1].split("\">", 2)[1].split("</a>", 2)[0].trim();
			String icon = "http://www.bom.gov.au" + bit.split("<img src=\"", 2)[1].split("\" alt=\"", 2)[0].trim();
			String min = "", max = "";

			if(bit.contains("<dd class=\"max\">"))
				max = bit.split("<dd class=\"max\">")[1].split("</dd>")[0].trim();

			if(bit.contains("<dd class=\"min\">"))
				min = bit.split("<dd class=\"min\">")[1].split("</dd>")[0].trim();

			String text = bit.split("<dd class=\"summary\">")[1].split("</dd>")[0].trim();

			String fileName =  icon.substring(icon.lastIndexOf('/') + 1, icon.length() - 4);

			if(!use_icons)
			{
				if(!fileName.equals("frost"))
					tmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='wi wi-bom-" + fileName + "'></i></td>";
				else
					tmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='flaticon-thermometer'></i></td>";
			} else {
				fileName = "bom2" + icon.substring(icon.lastIndexOf('/') + 1, icon.length()).replaceAll("-", "_");
				fileName = checkImage(fileName, icon);
				tmp = "<tr><td style='width:10%;' rowspan='2'><img src='file://" + fileName + "'></td>";
			}
			out.append(tmp);

			tmp = "<td style='width:80%;'><b>" + day + "</b></td>";
			out.append(tmp);

			max = max.replaceAll("°C", "").trim();
			min = min.replaceAll("°C", "").trim();

			if(metric)
			{
				max = max + "&deg;C";
				min = min + "&deg;C";
			} else {
				max = round((Double.parseDouble(max) * 9.0 / 5.0) + 32.0) + "&deg;F";
				min = round((Double.parseDouble(min) * 9.0 / 5.0) + 32.0) + "&deg;F";
			}

			if(!max.equals("&deg;C"))
				tmp = "<td style='width:10%;text-align:right;'><b>" + max + "</b></td></tr>";
			else
				tmp = "<td style='width:10%;text-align:right;'><b>&nbsp;</b></td></tr>";
			out.append(tmp);

			tmp = "<tr><td>" + text + "</td>";
			out.append(tmp);

			if(!min.equals("&deg;C"))
				tmp = "<td style='text-align:right;'>" + min + "</td></tr>";
			else
				tmp = "<td style='text-align:right;'>&nbsp;</td></tr>";
			out.append(tmp);

			if(showHeader)
			{
				tmp = "<tr><td style='font-size:10pt;' colspan='5'>&nbsp;</td></tr>";
				out.append(tmp);
			}

			for(i = 2; i < bits.length; i++)
			{
				bit = bits[i];
				day = bit.split("<a href=\"", 2)[1].split("\">", 2)[1].split("</a>", 2)[0].trim();
				icon = "http://www.bom.gov.au" + bit.split("<img src=\"", 2)[1].split("\" alt=\"", 2)[0].trim();
				max = bit.split("<dd class=\"max\">")[1].split("</dd>")[0].trim();
				min = bit.split("<dd class=\"min\">")[1].split("</dd>")[0].trim();
				text = bit.split("<dd class=\"summary\">")[1].split("</dd>")[0].trim();

				fileName =  icon.substring(icon.lastIndexOf('/') + 1, icon.length() - 4);

				if(!use_icons)
				{
					if(!fileName.equals("frost"))
						tmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='wi wi-bom-" + fileName + "'></i></td>";
					else
						tmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='flaticon-thermometer'></i></td>";
				} else {
					fileName = "bom2" + icon.substring(icon.lastIndexOf('/') + 1, icon.length()).replaceAll("-", "_");
					fileName = checkImage(fileName, icon);
					tmp = "<tr><td style='width:10%;' rowspan='2'><img src='file://" + fileName + "'></td>";
				}
				out.append(tmp);

				tmp = "<td style='width:80%;'><b>" + day + "</b></td>";
				out.append(tmp);

				max = max.replaceAll("°C", "").trim();
				min = min.replaceAll("°C", "").trim();

				if(metric)
				{
					max = max + "&deg;C";
					min = min + "&deg;C";
				} else {
					max = round((Double.parseDouble(max) * 9.0 / 5.0) + 32.0) + "&deg;F";
					min = round((Double.parseDouble(min) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}

				if(!max.equals("&deg;C"))
					tmp = "<td style='width:10%;text-align:right;'><b>" + max + "</b></td></tr>";
				else
					tmp = "<td style='width:10%;text-align:right;'><b>&nbsp;</b></td></tr>";
				out.append(tmp);

				tmp = "<tr><td>" + text + "</td>";
				out.append(tmp);

				if(!min.equals("&deg;C"))
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

	String[] processMET(String data)
	{
		return processMET(data, false);
	}

	String[] processMET(String data, boolean showHeader)
	{
		if(data == null || data.equals(""))
			return null;

		//boolean metric = GetBoolPref("metric", true);
		long mdate = (long)GetIntPref("rssCheck", 0) * 1000;
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
		String lastupdate = sdf.format(mdate);

		String tmp;
		StringBuilder out = new StringBuilder();
		String desc;

		try
		{
			desc = data.split("<title>", 2)[1].split(" weather - Met Office</title>",2)[0].trim();

			tmp = "<div style='font-size:12pt;'>" + lastupdate + "</div>";
			out.append(tmp);
			tmp = "<table style='width:100%;'>\n";
			out.append(tmp);

			String[] forecasts = data.split("<ul id=\"dayNav\"", 2)[1].split("</ul>", 2)[0].split("<li");
			for(int i = 1; i < forecasts.length; i++)
			{
				String date = forecasts[i].split("data-tab-id=\"", 2)[1].split("\"")[0].trim();

				sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
				mdate = sdf.parse(date).getTime();
				sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
				date = sdf.format(mdate);

				String icon = "https://beta.metoffice.gov.uk" + forecasts[i].split("<img class=\"icon\"")[1].split("src=\"")[1].split("\">")[0].trim();
				String fileName =  "met" + icon.substring(icon.lastIndexOf('/') + 1, icon.length()).replaceAll("\\.svg$", "\\.png");
				String min = forecasts[i].split("<span class=\"tab-temp-low\"", 2)[1].split("\">")[1].split("</span>")[0].trim();
				String max = forecasts[i].split("<span class=\"tab-temp-high\"", 2)[1].split("\">")[1].split("</span>")[0].trim();
				String text = forecasts[i].split("<div class=\"summary-text", 2)[1].split("\">", 3)[2]
									.split("</div>", 2)[0].replaceAll("</span>", "").replaceAll("<span>", "");

				tmp = "<tr><td style='width:10%;' rowspan='2'><img width='40px' src='" + fileName + "'></td>";
				out.append(tmp);

				tmp = "<td style='width:80%;'><b>" + date + "</b></td>";
				out.append(tmp);

				tmp = "<td style='width:10%;text-align:right;'><b>" + max + "</b></td></tr>";
				out.append(tmp);

				tmp = "<tr><td style='width:80%;'>" + text + "</td>";
				out.append(tmp);

				tmp = "<td style='text-align:right;vertical-align:top;'>" + min + "</td></tr>";
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

	String[] processWCA(String data)
	{
		return processWCA(data, false);
	}

	String[] processWCA(String data, boolean showHeader)
	{
		if(data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		String tmp;
		StringBuilder out = new StringBuilder();
		String desc;

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
			String day = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String month = obs.substring(i, j);
			i = j + 1;
			j = obs.length();
			String year = obs.substring(i, j);

			obs = hour + ":" + minute + " " + ampm + " " + day + " " + month + " " + year;

			SimpleDateFormat sdf = new SimpleDateFormat("h:mm aa d MMMM yyyy", Locale.getDefault());
			long mdate = sdf.parse(obs).getTime();
			sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
			obs = sdf.format(mdate);

			tmp = "<div style='font-size:12pt;'>" + obs + "</div>";
			out.append(tmp);
			tmp = "<table style='width:100%;'>\n";
			out.append(tmp);

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
					String date, text = "", img_url = "", temp = "", pop = "";

					if (div.get(j).className().contains("greybkgrd"))
					{
						j++;
						continue;
					}

					if(div.get(j).toString().contains("div-row-head"))
					{
						if(div.get(j).select("div").html().contains("Tonight"))
							date = "Tonight";
						else if(div.get(j).select("div").html().contains("Night"))
							date = "Night";
						else
							date = div.get(j).html().split("<strong title=\"", 2)[1].split("\">", 2)[0].trim();
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

					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inJustDecodeBounds = false;

					String fileName = "wca" + img_url.substring(img_url.lastIndexOf('/') + 1, img_url.length()).replaceAll("\\.gif$", "\\.png");

					tmp = "<tr><td style='width:10%;' rowspan='2'><img width='40px' src='" + fileName + "'></td>";
					out.append(tmp);

					tmp = "<td style='width:80%;'><b>" + date + "</b></td>";
					out.append(tmp);

					tmp = "<td style='width:10%;text-align:right;'><b>" + temp + "</b></td></tr>";
					out.append(tmp);

					tmp = "<tr><td style='width:80%;'>" + text + "</td>";
					out.append(tmp);

					if(pop.equals(""))
						tmp = "<td>&nbsp;</td></tr>";
					else
						tmp = "<td style='text-align:right;'>" + pop + "</td></tr>";
					out.append(tmp);

					if(showHeader)
					{
						tmp = "<tr><td style='font-size:10pt;' colspan='5'>&nbsp;</td></tr>";
						out.append(tmp);
					}
				}
			}

			out.append("</table>");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{out.toString(), desc};
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
		String tmp;
		StringBuilder out = new StringBuilder();
		String desc;

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
			String day = bits[bits.length - 3];
			String month = bits[bits.length - 2];
			String year = bits[bits.length - 1];

			obs = hour + ":" + minute + " " + day + " " + month + " " + year;

			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm d MMMM yyyy", Locale.CANADA_FRENCH);
			long mdate = sdf.parse(obs).getTime();
			sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.CANADA_FRENCH);
			obs = sdf.format(mdate);

			tmp = "<div style='font-size:12pt;'>" + obs + "</div>";
			out.append(tmp);
			tmp = "<table style='width:100%;'>\n";
			out.append(tmp);

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
					String date, text = "", img_url = "", temp = "", pop = "";

					if (div.get(j).className().contains("greybkgrd"))
					{
						j++;
						continue;
					}

					if(div.get(j).toString().contains("div-row-head"))
					{
						if(div.get(j).select("div").html().contains("Ce soir et cette nuit"))
							date = "Cette nuit";
						else if(div.get(j).select("div").html().contains("Nuit"))
							date = "Nuit";
						else
							date = div.get(j).html().split("<strong title=\"", 2)[1].split("\">", 2)[0].trim();
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

					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inJustDecodeBounds = false;

					String fileName = "wca" + img_url.substring(img_url.lastIndexOf('/') + 1, img_url.length()).replaceAll("\\.gif$", "\\.png");

					tmp = "<tr><td style='width:10%;' rowspan='2'><img width='40px' src='" + fileName + "'></td>";
					out.append(tmp);

					tmp = "<td style='width:80%;'><b>" + date + "</b></td>";
					out.append(tmp);

					tmp = "<td style='width:10%;text-align:right;'><b>" + temp + "</b></td></tr>";
					out.append(tmp);

					tmp = "<tr><td style='width:80%;'>" + text + "</td>";
					out.append(tmp);

					if(pop.equals(""))
						tmp = "<td>&nbsp;</td></tr>";
					else
						tmp = "<td style='text-align:right;'>" + pop + "</td></tr>";
					out.append(tmp);

					if(showHeader)
					{
						tmp = "<tr><td style='font-size:10pt;' colspan='5'>&nbsp;</td></tr>";
						out.append(tmp);
					}
				}
			}

			out.append("</table>");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String[]{out.toString(), desc};
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
		String desc;
		StringBuilder out = new StringBuilder();

		try
		{
			JSONObject jobj = new JSONObject(data);

			desc = jobj.getJSONObject("currentobservation").getString("name");
			String tmp = jobj.getString("creationDate");

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
			long mdate = sdf.parse(tmp).getTime();
			sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
			String date = sdf.format(mdate);

			tmp = "<div style='font-size:12pt;'>" + date + "</div>";
			out.append(tmp);
			tmp = "<table style='width:100%;'>\n";
			out.append(tmp);

			JSONArray periodName = jobj.getJSONObject("time").getJSONArray("startPeriodName");
			JSONArray weather = jobj.getJSONObject("data").getJSONArray("weather");
			final JSONArray iconLink = jobj.getJSONObject("data").getJSONArray("iconLink");
			JSONArray temperature = jobj.getJSONObject("data").getJSONArray("temperature");
			for(int i = 0; i < periodName.length(); i++)
			{
				String fimg = "", simg = "", fper = "", sper = "", number;
				iconLink.put(i, iconLink.getString(i).replace("http://", "https://"));
				final String url = iconLink.getString(i);

				String fn = "wgov" + url.substring(url.lastIndexOf('/') + 1, url.length()).replace(".png", ".jpg");
				if(fn.startsWith("wgovDualImage.php"))
				{
					//fn = "wgov_" + convertToHex(genSHA(fn.substring(4, fn.length()))) + ".jpg";
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

					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inJustDecodeBounds = false;
					Bitmap bmp1 = null, bmp2 = null;

					try
					{
						Class res = R.drawable.class;
						Field field = res.getField(fimg);
						int drawableId = field.getInt(null);
						if(drawableId != 0)
							bmp1 = BitmapFactory.decodeResource(context.getResources(), drawableId, options);
					} catch (Exception e) {
						//LogMessage("Failure to get drawable id.");
					}

					try
					{
						Class res = R.drawable.class;
						Field field = res.getField(simg);
						int drawableId = field.getInt(null);
						if(drawableId != 0)
							bmp2 = BitmapFactory.decodeResource(context.getResources(), drawableId, options);
					} catch (Exception e) {
						//LogMessage("Failure to get drawable id.");
					}

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
					Pattern p = Pattern.compile("[^0-9]");
					number = p.matcher(fn).replaceAll("");
					if(!number.equals(""))
					{
						Bitmap bmp3 = null;
						BitmapFactory.Options options = new BitmapFactory.Options();
						options.inJustDecodeBounds = false;

						fn = fn.replaceAll("\\d{2,3}\\.jpg$", "\\.jpg");

						try
						{
							Class res = R.drawable.class;
							Field field = res.getField(fn.substring(0, fn.length() - 4));
							int drawableId = field.getInt(null);
							if(drawableId != 0)
								bmp3 = BitmapFactory.decodeResource(context.getResources(), drawableId, options);
						} catch (Exception e) {
							LogMessage("Failure to get drawable id for: " + fn);
						}

						if(bmp3 != null)
						{
							Bitmap bmp4 = BitmapFactory.decodeResource(context.getResources(), R.drawable.wgovoverlay, options);
							bmp3 = overlay(bmp3, bmp4, number + "%");

							ByteArrayOutputStream stream = new ByteArrayOutputStream();
							bmp3.compress(Bitmap.CompressFormat.JPEG, 75, stream);
							byte[] byteArray = stream.toByteArray();
							bmp3.recycle();
							bmp4.recycle();

							// https://stackoverflow.com/questions/9224056/android-bitmap-to-base64-string

							String encoded = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
							iconLink.put(i, encoded);

							LogMessage("wrote " + fn + " to " + iconLink.getString(i).substring(0, 100));
						}
					}
				}

				tmp = "<tr><td style='width:10%;' rowspan='2'><img width='40px' src='" + iconLink.getString(i) + "'></td>";
				out.append(tmp);

				tmp = "<td style='width:80%;'><b>" + periodName.getString(i) + "</b></td>";
				out.append(tmp);

				tmp = "<td style='width:10%;text-align:right;'><b>" + temperature.getString(i) + "&deg;F</b></td></tr>";
				if(metric)
					tmp = "<td style='width:10%;text-align:right;'><b>" + round((Double.parseDouble(temperature.getString(i)) - 32.0) * 5.0 / 9.0)  + "&deg;C</b></td></tr>";
				out.append(tmp);

				tmp = "<tr><td style='width:80%;'>" + weather.getString(i) + "</td>";
				out.append(tmp);

				tmp = "<td>&nbsp;</td></tr>";
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

	String[] processWMO(String data)
	{
		return processWMO(data, false);
	}

	String[] processWMO(String data, boolean showHeader)
	{
		if(data == null || data.equals(""))
			return null;

		String desc;
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
				code = code.substring(0, code.length() - 2);

				if((Integer.parseInt(code) >= 1 && Integer.parseInt(code) <= 27) || code.equals("31") || code.equals("35"))
					tmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='wi wi-wmo-" + code + "'></i></td>";
				else if(code.equals("28"))
					tmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='flaticon-cactus'></i></td>";
				else if(code.equals("29") || code.equals("30"))
					tmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='flaticon-thermometer'></i></td>";
				else if(code.equals("32"))
					tmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='flaticon-cold'></i></td>";
				else if(code.equals("33"))
					tmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='flaticon-warm'></i></td>";
				else if(code.equals("34"))
					tmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='flaticon-cool'></i></td>";
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
		if(data == null || data.equals(""))
			return null;

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
		String desc;
		StringBuilder out = new StringBuilder();

		try
		{
			JSONObject jobj = new JSONObject(data);
			desc = jobj.getString("description") + ", Australia";

			String tmp = jobj.getString("content");
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
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
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
				mdate = sdf.parse(date).getTime();
				sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
				date = sdf.format(mdate);

				if(!use_icons)
				{
					if(!code.equals("14"))
						tmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='wi wi-bom-ftp-" + code + "'></i></td>";
					else
						tmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='flaticon-thermometer'></i></td>";
				} else {
					String fileName = checkImage("bom" + code + ".png", null);
					tmp = "<tr><td style='width:10%;' rowspan='2'><img width='40px' src='file://" + fileName + "'></td>";
				}
				out.append(tmp);

				tmp = "<td style='width:80%;'><b>" + date + "</b></td>";
				out.append(tmp);

				if(metric)
				{
					max = max + "&deg;C";
					min = min + "&deg;C";
				} else {
					max = round((Double.parseDouble(max) * 9.0 / 5.0) + 32.0) + "&deg;F";
					min = round((Double.parseDouble(min) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}

				if(!max.equals("&deg;C"))
					tmp = "<td style='width:10%;text-align:right;'><b>" + max + "</b></td></tr>";
				else
					tmp = "<td style='width:10%;text-align:right;'><b>&nbsp;</b></td></tr>";
				out.append(tmp);

				tmp = "<tr><td>" + text + "</td>";
				out.append(tmp);

				if(!min.equals("&deg;C"))
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
		StringBuilder out = new StringBuilder();
		String tmp;
		String desc;

		try
		{
			JSONObject jobj = new JSONObject(data);
			JSONArray days = jobj.getJSONArray("days");
			String ftime = days.getJSONObject(0).getString("issuedAtISO");
			desc = jobj.getString("locationECWasp") + ", New Zealand";

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
			long mdate = sdf.parse(ftime).getTime();
			sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
			ftime = sdf.format(mdate);

			tmp = "<div style='font-size:12pt;'>" + ftime + "</div>";
			out.append(tmp);
			tmp = "<table style='width:100%;'>\n";
			out.append(tmp);

			for(int i = 0; i < days.length(); i++)
			{
				JSONObject jtmp = days.getJSONObject(i);
				String dow = jtmp.getString("dow");
				String text = jtmp.getString("forecast");
				String max = jtmp.getString("max");
				String min = jtmp.getString("min");
				String icon;
				if(jtmp.has("partDayData"))
					icon = jtmp.getJSONObject("partDayData").getJSONObject("afternoon").getString("forecastWord");
				else
					icon = jtmp.getString("forecastWord");

				icon = icon.toLowerCase().replaceAll(" ", "-").trim();

				if(!use_icons)
				{
					if(!icon.equals("frost"))
						tmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='wi wi-metservice-" + icon + "'></i></td>";
					else
						tmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='flaticon-thermometer'></i></td>";
				} else {
					icon = icon.replaceAll("-", "_");
					String fileName = checkImage("ms_" + icon + ".png", null);
					tmp = "<tr><td style='width:10%;' rowspan='2'><img width='40px' src='file://" + fileName + "'></td>";
				}
				out.append(tmp);

				tmp = "<td style='width:80%;'><b>" + dow + "</b></td>";
				out.append(tmp);

				if(metric)
				{
					max = max + "&deg;C";
					min = min + "&deg;C";
				} else {
					max = round((Double.parseDouble(max) * 9.0 / 5.0) + 32.0) + "&deg;F";
					min = round((Double.parseDouble(min) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}

				if(!max.equals("&deg;C"))
					tmp = "<td style='width:10%;text-align:right;'><b>" + max + "</b></td></tr>";
				else
					tmp = "<td style='width:10%;text-align:right;'><b>&nbsp;</b></td></tr>";
				out.append(tmp);

				tmp = "<tr><td>" + text + "</td>";
				out.append(tmp);

				if(!min.equals("&deg;C"))
					tmp = "<td style='text-align:right;vertical-align:top;'>" + min + "</td></tr>";
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
			return new String[]{out.toString(), desc};
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
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
//		boolean use_icons = GetBoolPref("use_icons", false);
		StringBuilder out = new StringBuilder();
		String tmp;
		String desc = "";

		try
		{
			String[] bits = data.split("<title>");
			if (bits.length >= 2)
				desc = bits[1].split("</title>")[0];
			desc = desc.substring(desc.lastIndexOf(" - ") + 3, desc.length()).trim();
			String ftime = data.split("<tr class=\"headRow\">", 2)[1].split("</tr>", 2)[0].trim();
			String date = ftime.split("<td width=\"30%\" class=\"stattime\">", 2)[1].split("</td>", 2)[0].trim();
			ftime = date + " " + ftime.split("<td width=\"40%\" class=\"stattime\">", 2)[1].split(" Uhr</td>", 2)[0].trim();

			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy' 'HH", Locale.getDefault());
			long mdate = sdf.parse(ftime).getTime();
			sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
			ftime = sdf.format(mdate);

			tmp = "<div style='font-size:12pt;'>" + ftime + "</div>";
			out.append(tmp);
			tmp = "<table style='width:100%;'>\n";
			out.append(tmp);

			data = data.split("<td width=\"40%\" class=\"statwert\">Vorhersage</td>", 2)[1].split("</table>", 2)[0].trim();

			bits = data.split("<tr");
			for(int i = 1; i < bits.length; i++)
			{
				String bit = bits[i];
				String day, icon, temp;
				if(bit.split("<td ><b>", 2).length > 1)
					day = bit.split("<td ><b>", 2)[1].split("</b></td>", 2)[0].trim();
				else
					day = bit.split("<td><b>", 2)[1].split("</b></td>", 2)[0].trim();

				if(bit.split("<td ><img name=\"piktogramm\" src=\"", 2).length > 1)
					icon = bit.split("<td ><img name=\"piktogramm\" src=\"", 2)[1].split("\" width=\"50\" alt=\"",2)[0].trim();
				else
					icon = bit.split("<td><img name=\"piktogramm\" src=\"", 2)[1].split("\" width=\"50\" alt=\"",2)[0].trim();

				if(bit.split("\"></td>\n<td >", 2).length > 1)
					temp = bit.split("\"></td>\n<td >", 2)[1].split("Grad <abbr title=\"Celsius\">C</abbr></td>\n", 2)[0].trim();
				else
					temp = bit.split("\"></td>\n<td>", 2)[1].split("Grad <abbr title=\"Celsius\">C</abbr></td>\n", 2)[0].trim();

				icon = icon.replaceAll("/DE/wetter/_functions/piktos/vhs_", "").replaceAll("\\?__blob=normal", "").trim();
				String fileName = "dwd_" + icon.replaceAll("-", "_");
				String url = "https://www.dwd.de/DE/wetter/_functions/piktos/" + icon + "?__blob=normal";
				fileName = checkImage(fileName, url);

				tmp = "<tr><td style='width:10%;' rowspan='2'><img width='40px' src='" + fileName + "'></td>";
				out.append(tmp);

				tmp = "<td style='width:80%;'><b>" + day + "</b></td>";
				out.append(tmp);

				if(metric)
					temp = temp + "&deg;C";
				else
					temp = round((Double.parseDouble(temp) * 9.0 / 5.0) + 32.0) + "&deg;F";

				if(!temp.equals("&deg;C"))
					tmp = "<td style='width:10%;text-align:right;'><b>" + temp + "</b></td></tr>";
				else
					tmp = "<td style='width:10%;text-align:right;'><b>&nbsp;</b></td></tr>";
				out.append(tmp);

				tmp = "<tr><td>&nbsp;</td>";
				out.append(tmp);

				tmp = "<td style='text-align:right;'>&nbsp;</td></tr>";
				out.append(tmp);

				if(showHeader)
				{
					tmp = "<tr><td style='font-size:10pt;' colspan='5'>&nbsp;</td></tr>";
					out.append(tmp);
				}
			}

			out.append("</table>");
			return new String[]{out.toString(), desc};
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
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
		StringBuilder out = new StringBuilder();
		String tmp;
		String desc;

		try
		{
			JSONObject jobj = new XmlToJson.Builder(data).build().toJson();
			if (jobj == null)
				return null;

			jobj = jobj.getJSONObject("root");
			desc = jobj.getString("nombre") + ", " + jobj.getString("provincia");

			String elaborado = jobj.getString("elaborado");

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
			long mdate = sdf.parse(elaborado).getTime();
			sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
			elaborado = sdf.format(mdate);

			tmp = "<div style='font-size:12pt;'>" + elaborado + "</div>";
			out.append(tmp);
			tmp = "<table style='width:100%;'>\n";
			out.append(tmp);

			JSONArray days = jobj.getJSONObject("prediccion").getJSONArray("dia");
			for(int i = 0; i < days.length(); i++)
			{
				JSONObject jtmp = days.getJSONObject(i);
				String fecha = jtmp.getString("fecha");
				sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
				mdate = sdf.parse(fecha).getTime();
				sdf = new SimpleDateFormat("EEEE", new Locale("es", "ES"));
				fecha = sdf.format(mdate);

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
				String url = "http://www.aemet.es/imagenes/png/estado_cielo/" + code + "_g.png";

				String fileName = "aemet_" + code + "_g.png";
				fileName = checkImage(fileName, url);

				tmp = "<tr><td style='width:10%;' rowspan='2'><img width='40px' src='" + fileName + "'></td>";
				out.append(tmp);

				tmp = "<td style='width:80%;'><b>" + fecha + "</b></td>";
				out.append(tmp);

				String max = temperatura.getString("maxima");
				String min = temperatura.getString("minima");
				if(metric)
				{
					max = max + "&deg;C";
					min = min + "&deg;C";
				} else {
					max = round((Double.parseDouble(max) * 9.0 / 5.0) + 32.0) + "&deg;F";
					min = round((Double.parseDouble(min) * 9.0 / 5.0) + 32.0) + "&deg;F";
				}

				if(!max.equals("&deg;C"))
					tmp = "<td style='width:10%;text-align:right;'><b>" + max + "</b></td></tr>";
				else
					tmp = "<td style='width:10%;text-align:right;'><b>&nbsp;</b></td></tr>";
				out.append(tmp);

				tmp = "<tr><td>" + estado_cielo.getString("descripcion") + "</td>";
				out.append(tmp);

				if(!min.equals("&deg;C"))
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
			return new String[]{out.toString(), desc};
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	String[] processAPIXU(String data)
	{
		return processAPIXU(data, false);
	}

	String[] processAPIXU(String data, boolean showHeader)
	{
		if (data == null || data.equals(""))
			return null;

		StringBuilder out = new StringBuilder();
		String tmp;
		String desc;

		boolean metric = GetBoolPref("metric", true);

		if(conditions == null)
		{
			loadConditions();
			LogMessage("Loaded Conditions");
		}

		try
		{
			JSONObject jobj = new JSONObject(data);

			long mdate = jobj.getJSONObject("location").getLong("localtime_epoch") * 1000;
			SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
			String date = sdf.format(mdate);

			tmp = "<div style='font-size:12pt;'>" + date + "</div>";
			out.append(tmp);
			tmp = "<table style='width:100%;'>\n";
			out.append(tmp);

			desc = jobj.getJSONObject("location").getString("name") + ", " + jobj.getJSONObject("location").getString("country");
			JSONArray jarr = jobj.getJSONObject("forecast").getJSONArray("forecastday");
			sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
			for(int i = 0; i < jarr.length(); i++)
			{
				JSONObject j = jarr.getJSONObject(i);
				long mtime = j.getLong("date_epoch") * 1000;
				date = sdf.format(mtime);

				JSONObject day = j.getJSONObject("day");

				String min, max;

				if(metric)
				{
					min = (int)round(day.getDouble("mintemp_c")) + "&deg;C";
					max = (int)round(day.getDouble("maxtemp_c")) + "&deg;C";
				} else {
					min = (int)round(day.getDouble("mintemp_f")) + "&deg;F";
					max = (int)round(day.getDouble("maxtemp_f")) + "&deg;F";
				}

				int code = day.getJSONObject("condition").getInt("code");

				String text = "", icon = "";

				for(int k = 0; k < conditions.length(); k++)
				{
					JSONObject cond = conditions.getJSONObject(k);
					if(cond.getInt("code") == code)
					{
						icon = String.valueOf(cond.getInt("icon"));
						if(Locale.getDefault().getLanguage().equals("en"))
						{
							text = cond.getString("day");
						} else {
							for(int l = 0; l < cond.getJSONArray("languages").length(); l++)
							{
								JSONObject lang = cond.getJSONArray("languages").getJSONObject(l);
								if(lang.getString("lang_iso").equals(Locale.getDefault().getLanguage()))
								{
									text = lang.getString("day_text");
									break;
								}
							}
						}
						break;
					}
				}

				tmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='wi wi-apixu-" + icon + "'></i></td>";
				out.append(tmp);

				tmp = "<td style='width:80%;'><b>" + date + "</b></td>";
				out.append(tmp);

				tmp = "<td style='width:10%;text-align:right;'><b>" + max + "</b></td></tr>";
				out.append(tmp);

				tmp = "<tr><td style='width:80%;'>" + text + "</td>";
				out.append(tmp);

				tmp = "<td style='text-align:right;'>" + min + "</td></tr>";
				out.append(tmp);

				if(showHeader)
				{
					tmp = "<tr><td style='font-size:10pt;' colspan='5'>&nbsp;</td></tr>";
					out.append(tmp);
				}
			}

			out.append("</table>");
			return new String[]{out.toString(), desc};
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	String[] processOWM(String data)
	{
		return processOWM(data, false);
	}

	String[] processOWM(String data, boolean showHeader)
	{
		if (data == null || data.equals(""))
			return null;

		StringBuilder out = new StringBuilder();
		String tmp;
		String desc;

		boolean metric = GetBoolPref("metric", true);
		long mdate = (long)GetIntPref("rssCheck", 0) * 1000;
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
		String date = sdf.format(mdate);

		tmp = "<div style='font-size:12pt;'>" + date + "</div>";
		out.append(tmp);
		tmp = "<table style='width:100%;'>\n";
		out.append(tmp);

		try
		{
			JSONObject jobj = new JSONObject(data);
			desc = jobj.getJSONObject("city").getString("name") + ", " + jobj.getJSONObject("city").getString("country");
			JSONArray jarr = jobj.getJSONArray("list");
			sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
			for(int i = 0; i < jarr.length(); i++)
			{
				JSONObject j = jarr.getJSONObject(i);
				long mtime = (long)j.getInt("dt") * 1000;
				date = sdf.format(mtime);

				JSONObject temp = j.getJSONObject("temp");
				int min = (int)round(Double.parseDouble(temp.getString("min")));
				int max = (int)round(Double.parseDouble(temp.getString("max")));
				JSONObject weather = j.getJSONArray("weather").getJSONObject(0);

				int id = weather.getInt("id");
				String text = weather.getString("description");
				String icon = weather.getString("icon");

				if(!icon.endsWith("n"))
					tmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='wi wi-owm-day-" + id + "'></i></td>";
				else
					tmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='wi wi-owm-night-" + id + "'></i></td>";
				out.append(tmp);

				tmp = "<td style='width:80%;'><b>" + date + "</b></td>";
				out.append(tmp);

				if(metric)
					tmp = "<td style='width:10%;text-align:right;'><b>" + max + "&deg;C</b></td></tr>";
				else
					tmp = "<td style='width:10%;text-align:right;'><b>" + max + "&deg;F</b></td></tr>";
				out.append(tmp);

				tmp = "<tr><td style='width:80%;'>" + text + "</td>";
				out.append(tmp);

				if(metric)
					tmp = "<td style='text-align:right;'>" + min + "&deg;C</td></tr>";
				else
					tmp = "<td style='text-align:right;'>" + min + "&deg;F</td></tr>";
				out.append(tmp);

				if(showHeader)
				{
					tmp = "<tr><td style='font-size:10pt;' colspan='5'>&nbsp;</td></tr>";
					out.append(tmp);
				}
			}

			out.append("</table>");
			return new String[]{out.toString(), desc};
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	String[] processYR(String data)
	{
		return processYR(data, false);
	}

	String[] processYR(String data, boolean showHeader)
	{
		if(data == null || data.equals(""))
			return null;

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
				JSONObject windSpeed = jarr.getJSONObject(i).getJSONObject("windSpeed");

				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
				mdate = sdf.parse(from).getTime();
				sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
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

	String[] processWZ(String data)
	{
		return processWZ(data, false);
	}

	String[] processWZ(String data, boolean showHeader)
	{
		boolean metric = GetBoolPref("metric", true);
		if(data == null || data.equals(""))
			return null;

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
	    if(data == null || data.equals(""))
		    return null;

	    JSONObject json;
	    boolean use_icons = GetBoolPref("use_icons", false);

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

			    if(!use_icons)
			    {
				    stmp = "<td style='width:50%;text-align:right;'><i style='font-size:60px;' class='wi wi-yahoo-" + code + "'></i></td></tr>";
			    } else {
				    String fileName = checkImage("yahoo" + code + ".gif", null);
				    stmp = "<td style='width:50%;text-align:right;'><img width='80px' src='file://" + fileName + "'></td></tr>";
			    }
			    LogMessage(stmp);
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

				    if(!use_icons)
				    {
					    stmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='wi wi-yahoo-" + code + "'></i></td>";
				    } else {
					    String fileName = checkImage("yahoo" + code + ".gif", null);
					    stmp = "<tr><td style='width:10%;' rowspan='2'><img width='30px' src='file://" + fileName + "'></td>";
				    }
				    str.append(stmp);

				    stmp = "<td style='width:45%;'><b>" + tmp.getString("day") + ", " + tmp.getString("date") + "</b></td>";
				    str.append(stmp);

				    stmp = "<td style='width:45%;text-align:right;'><b>" + tmp.getString("high") + "&deg;" + temp + "</b></td>";
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

				    if(!use_icons)
				    {
					    stmp = "<tr><td style='width:10%;' rowspan='2'><i style='font-size:30px;' class='wi wi-yahoo-" + code + "'></i></td>";
				    } else {
					    String fileName = checkImage("yahoo" + code + ".gif", null);
					    stmp = "<tr><td style='width:10%;' rowspan='2'><img width='30px' src='file://" + fileName + "'></td>";
				    }
				    LogMessage(stmp);
				    str.append(stmp);

				    stmp = "<td style='width:45%;'><b>" + tmp.getString("day") + ", " + tmp.getString("date") + "</b></td>";
				    str.append(stmp);

				    stmp = "<td style='width:45%;text-align:right;'><b>" + tmp.getString("high") + "&deg;" + temp + "</b></td></tr>";
				    str.append(stmp);

				    stmp = "<tr><td>" + tmp.getString("text") + "</td>";
				    str.append(stmp);

				    stmp = "<td style='text-align:right;'>" + tmp.getString("low") + "&deg;" + temp + "</td></tr>";
				    str.append(stmp);
			    }

			    stmp = "</table>";
			    str.append(stmp);
		    }

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

	void SendRefresh()
	{
		Intent intent = new Intent();
		intent.setAction(Common.REFRESH_INTENT);
		context.sendBroadcast(intent);
		Common.LogMessage("refresh_intent broadcast.");
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
					String fromURL = GetStringPref("BASE_URL", "");
					if (fromURL.equals(""))
						return;

					reallyGetWeather(fromURL);
					SendRefresh();
				} catch (Exception e) {
					e.printStackTrace();
					SetStringPref("lastError", e.toString());
					SendFailedIntent();
				}
			}
		});

		t.start();
	}

	void reallyGetWeather(String fromURL) throws Exception
	{
		String line = downloadString(fromURL);
		if(!line.equals(""))
		{
			String bits[] = line.split("\\|");
			if (Double.valueOf(bits[0]) < inigo_version)
			{
				if(GetLongPref("inigo_version", 0) < Common.inigo_version)
				{
					SetLongPref("inigo_version", Common.inigo_version);
					sendAlert();
				}
			}

			if (Double.valueOf(bits[0]) >= 4000)
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
			SetLongPref("LastDownloadTime", round(System.currentTimeMillis() / 1000));
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

	private String checkImage(String fileName, String icon)
	{
		File f = new File(Environment.getExternalStorageDirectory(), "weeWX");
		f = new File(f, "icons");
		f = new File(f, fileName);
		if(f.exists() && f.isFile() && f.length() > 0)
			return f.getAbsolutePath();

		try
		{
			Class res = R.drawable.class;
			Field field = res.getField(fileName.substring(0, fileName.lastIndexOf(".") - 1));
			int drawableId = field.getInt(null);
			if(drawableId != 0)
				return fileName;
		} catch (Exception e) {
			//LogMessage("Failure to get drawable id.");
		}

		//if(icon != null)
			//downloadImage(fileName, icon);

		return fileName;
	}

	private void downloadImage(final String fileName, final String imageURL)
	{
		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				if (fileName == null || fileName.equals("") || imageURL == null || imageURL.equals(""))
					return;

				Common.LogMessage("checking: " + imageURL);

				try
				{
					File f = new File(Environment.getExternalStorageDirectory(), "weeWX");
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
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = false;
				Bitmap bmp2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.wgovoverlay, options);
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
				paint.setStrokeWidth(5);
				comboImage.drawLine( 100, bmp1.getHeight() - 20, 140, bmp1.getHeight() - 20, paint);
				comboImage.drawLine( 125, bmp1.getHeight() - 33, 140, bmp1.getHeight() - 20, paint);
				comboImage.drawLine( 125, bmp1.getHeight() - 7, 140, bmp1.getHeight() - 20, paint);
			}

			if(!fnum.equals("%"))
			{
				paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(0xff00487b);
				paint.setTextSize(50);
				paint.setTypeface(Typeface.create(tf, Typeface.BOLD));

				comboImage.drawText(fnum, 5, y1 - 5, paint);
			}

			if(!snum.equals("%"))
			{
				paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(0xff00487b);
				paint.setTextSize(50);
				paint.setTypeface(Typeface.create(tf, Typeface.BOLD));

				Rect textBounds = new Rect();
				paint.getTextBounds(snum, 0, snum.length(), textBounds);
				int width = (int) paint.measureText(snum);

				comboImage.drawText(snum, x1 - width - 5, y1 - 5, paint);
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
					bmp1 = Bitmap.createBitmap(bmp1, x1 / 2, 0, x1 - x1 / 2, y1);
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
					bmp2 = Bitmap.createBitmap(bmp2, x2 / 2, 0, x2 - x2 / 2, y2);
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
			comboImage.drawBitmap(bmp2, x1 / 2, 0f, null);

			paint.setColor(0xff000000);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(10);
			comboImage.drawLine( x1 / 2, 0, x1 / 2, y1, paint);

			paint.setColor(0xffffffff);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(6);
			comboImage.drawLine( x1 / 2, 0, x1 / 2, y1, paint);

			if(!fnum.equals("%") || !snum.equals("%"))
			{
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = false;
				Bitmap bmp4 = BitmapFactory.decodeResource(context.getResources(), R.drawable.wgovoverlay, options);
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
				paint.setStrokeWidth(5);
				comboImage.drawLine( 100, bmp1.getHeight() - 20, 140, bmp1.getHeight() - 20, paint);
				comboImage.drawLine( 125, bmp1.getHeight() - 33, 140, bmp1.getHeight() - 20, paint);
				comboImage.drawLine( 125, bmp1.getHeight() - 7, 140, bmp1.getHeight() - 20, paint);
			}

			if(!fnum.equals("%"))
			{
				paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(0xff00487b);
				paint.setTextSize(50);
				paint.setTypeface(Typeface.create(tf, Typeface.BOLD));

				comboImage.drawText(fnum, 5, y1 - 5, paint);
			}

			if(!snum.equals("%"))
			{
				paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(0xff00487b);
				paint.setTextSize(50);
				paint.setTypeface(Typeface.create(tf, Typeface.BOLD));

				Rect textBounds = new Rect();
				paint.getTextBounds(snum, 0, snum.length(), textBounds);
				int width = (int) paint.measureText(snum);

				comboImage.drawText(snum, x1 - width - 5, y1 - 5, paint);
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
		paint.setTextSize(50);
		paint.setTypeface(Typeface.create(tf, Typeface.BOLD));
		Rect textBounds = new Rect();
		paint.getTextBounds(s, 0, s.length(), textBounds);
		int width = (int)paint.measureText(s);

		canvas.drawText(s, bmp1.getWidth() - width, bmp1.getHeight() - 5, paint);

		return bmp3;
	}

	// https://stackoverflow.com/questions/19945411/android-java-how-can-i-parse-a-local-json-file-from-assets-folder-into-a-listvi

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

	private void loadConditions()
	{
		try
		{
			InputStream is = context.getResources().openRawResource(R.raw.conditions);
			int size = is.available();
			byte[] buffer = new byte[size];
			if(is.read(buffer) > 0)
				conditions = new JSONArray(new String(buffer, "UTF-8"));
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	String downloadSettings(String url) throws Exception
	{
		SetStringPref("SETTINGS_URL", url);
		return downloadString(url).replaceAll("[^\\p{ASCII}]+$", "").trim();
	}

	File downloadRADAR(String radar) throws Exception
	{
		LogMessage("starting to download image from: " + radar);
		File file = new File(context.getFilesDir(), "/radar.gif.tmp");
		return downloadBinary(file, radar);
	}

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
		String tmp = downloadString(forecast);

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

	private String downloadString(String fromURL) throws Exception
	{
		Uri uri = Uri.parse(fromURL);
		Common.LogMessage("fromURL == " + fromURL);
		if (uri.getUserInfo() != null && uri.getUserInfo().contains(":"))
		{
			final String[] UC = uri.getUserInfo().split(":");
			Common.LogMessage("uri username = " + uri.getUserInfo());

			if (UC != null && UC.length > 1)
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

		URL url = new URL(fromURL);
		URLConnection conn = url.openConnection();
		conn.setConnectTimeout(60000);
		conn.setReadTimeout(60000);
		conn.setDoOutput(true);
		conn.connect();
		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

		String line;
		StringBuilder sb = new StringBuilder();
		while ((line = in.readLine()) != null)
		{
			line = line.trim() + "\n";
			if (line.length() > 0)
				sb.append(line);
		}
		in.close();

		return sb.toString().trim();
	}

	@SuppressWarnings("unused")
	void writeFile(String fileName, String data) throws Exception
	{
		File dir = new File(Environment.getExternalStorageDirectory(), "weeWX");
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
	private File downloadJSOUP(File f, String fromURL) throws Exception
	{
		File dir = f.getParentFile();
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
		if (!dir.exists())
			if (!dir.mkdirs())
				return null;

		Uri uri = Uri.parse(fromURL);
		LogMessage("fromURL == " + fromURL);
		if (uri.getUserInfo() != null && uri.getUserInfo().contains(":"))
		{
			final String[] UC = uri.getUserInfo().split(":");
			LogMessage("uri username = " + uri.getUserInfo());

			if (UC != null && UC.length > 1)
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

		OutputStream outputStream = new FileOutputStream(f);

		URL url = new URL(fromURL);
		URLConnection conn = url.openConnection();
		conn.setConnectTimeout(60000);
		conn.setReadTimeout(60000);
		conn.setDoOutput(true);
		conn.connect();
		InputStream input = conn.getInputStream();

		int count;
		byte data[] = new byte[1024];
		while ((count = input.read(data)) != -1)
			outputStream.write(data, 0, count);

		input.close();
		outputStream.flush();
		outputStream.close();

		publish(f);
		return f;
	}

	private void makeTable()
	{
		lookupTable.put("N_W1_0-N_0", "mf_n_w2_1.png");
		lookupTable.put("N_W2_1", "mf_n_w2_1.png");
		lookupTable.put("N_W1_0-N_7", "mf_n_w2_1.png");
		lookupTable.put("J_W1_0-N", "mf_n_w2_1.png");

		lookupTable.put("J_W1_0-N_5", "mf_j_w1_0_n_5.png");

		lookupTable.put("N_W1_0-N_5", "mf_n_w1_0_n_5.png");

		lookupTable.put("J_W2_2", "mf_j_w1_0_n_2.png");
		lookupTable.put("J_W1_0-N_1", "mf_j_w1_0_n_2.png");
		lookupTable.put("J_W1_0-N_2", "mf_j_w1_0_n_2.png");
		lookupTable.put("J_W1_0-N_4", "mf_j_w1_0_n_2.png");
		lookupTable.put("J_W1_0-N_6", "mf_j_w1_0_n_2.png");

		lookupTable.put("N_W2_2", "mf_n_w2_2.png");
		lookupTable.put("N_W1_0-N_1", "mf_n_w2_2.png");
		lookupTable.put("N_W1_0-N_2", "mf_n_w2_2.png");
		lookupTable.put("N_W1_0-N_4", "mf_n_w2_2.png");
		lookupTable.put("N_W1_0-N_6", "mf_n_w2_2.png");

		lookupTable.put("J_W1_0-N_3", "mf_j_w2_3.png");
		lookupTable.put("N_W1_0-N_3", "mf_j_w2_3.png");
		lookupTable.put("J_W2_3", "mf_j_w2_3.png");
		lookupTable.put("N_W2_3", "mf_j_w2_3.png");

		lookupTable.put("J_W1_1-N", "mf_j_w1_1_n.png");
		lookupTable.put("J_W1_2-N", "mf_j_w1_1_n.png");
		lookupTable.put("J_W1_33-N", "mf_j_w1_1_n.png");

		lookupTable.put("N_W1_1-N", "mf_n_w1_1_n.png");
		lookupTable.put("N_W1_2-N", "mf_n_w1_1_n.png");
		lookupTable.put("N_W1_33-N", "mf_n_w1_1_n.png");

		lookupTable.put("N_W1_1-N_3", "mf_n_w1_1_n_3.png");
		lookupTable.put("J_W1_1-N_3", "mf_n_w1_1_n_3.png");
		lookupTable.put("N_W1_2-N_3", "mf_n_w1_1_n_3.png");
		lookupTable.put("J_W1_2-N_3", "mf_n_w1_1_n_3.png");
		lookupTable.put("N_W1_33-N_3", "mf_n_w1_1_n_3.png");
		lookupTable.put("J_W1_33-N_3", "mf_n_w1_1_n_3.png");

		lookupTable.put("J_W1_3-N", "mf_j_w2_4.png");
		lookupTable.put("N_W1_3-N", "mf_j_w2_4.png");
		lookupTable.put("J_W2_4", "mf_j_w2_4.png");
		lookupTable.put("N_W2_4", "mf_j_w2_4.png");

		lookupTable.put("J_W1_4-N", "mf_j_w2_5.png");
		lookupTable.put("J_W1_5-N", "mf_j_w2_5.png");
		lookupTable.put("J_W1_6-N", "mf_j_w2_5.png");
		lookupTable.put("J_W2_5", "mf_j_w2_5.png");
		lookupTable.put("N_W2_5", "mf_j_w2_5.png");
		lookupTable.put("N_W1_4-N", "mf_j_w2_5.png");
		lookupTable.put("N_W1_5-N", "mf_j_w2_5.png");
		lookupTable.put("N_W1_6-N", "mf_j_w2_5.png");

		lookupTable.put("J_W1_7-N", "mf_j_w1_7_n.png");
		lookupTable.put("N_W1_7-N", "mf_j_w1_7_n.png");

		lookupTable.put("J_W1_8-N", "mf_j_w1_8_n.png");

		lookupTable.put("N_W1_8-N", "mf_n_w1_8_n.png");

		lookupTable.put("N_W1_8-N_3", "mf_n_w1_8_n_3.png");
		lookupTable.put("J_W1_8-N_3", "mf_n_w1_8_n_3.png");

		lookupTable.put("J_W1_9-N", "mf_j_w2_6.png");
		lookupTable.put("J_W1_18-N", "mf_j_w2_6.png");
		lookupTable.put("J_W1_30-N", "mf_j_w2_6.png");
		lookupTable.put("J_W2_6", "mf_j_w2_6.png");
		lookupTable.put("J_W2_12", "mf_j_w2_6.png");

		lookupTable.put("N_W1_9-N", "mf_n_w2_6.png");
		lookupTable.put("N_W1_18-N", "mf_n_w2_6.png");
		lookupTable.put("N_W1_30-N", "mf_n_w2_6.png");
		lookupTable.put("N_W2_6", "mf_n_w2_6.png");
		lookupTable.put("N_W2_12", "mf_n_w2_6.png");

		lookupTable.put("J_W1_9-N_3", "mf_j_w1_9_n_3.png");
		lookupTable.put("N_W1_9-N_3", "mf_j_w1_9_n_3.png");
		lookupTable.put("J_W1_18-N_3", "mf_j_w1_9_n_3.png");
		lookupTable.put("N_W1_18-N_3", "mf_j_w1_9_n_3.png");
		lookupTable.put("J_W1_30-N_3", "mf_j_w1_9_n_3.png");
		lookupTable.put("N_W1_30-N_3", "mf_j_w1_9_n_3.png");

		//lookupTable.put("J_W1_19-N", "mf_large_10_a.png");
		lookupTable.put("J_W2_8", "mf_j_w2_8.png");
		lookupTable.put("J_W2_14", "mf_j_w2_8.png");

		//lookupTable.put("N_W1_19-N", "mf_large_10_b.png");
		lookupTable.put("N_W2_8", "mf_n_w2_8.png");
		lookupTable.put("N_W2_14", "mf_n_w2_8.png");

		lookupTable.put("J_W1_10-N", "mf_j_w1_10_n.png");
		lookupTable.put("J_W1_19-N", "mf_j_w1_10_n.png");
		lookupTable.put("N_W1_10-N", "mf_j_w1_10_n.png");
		lookupTable.put("N_W1_19-N", "mf_j_w1_10_n.png");

		lookupTable.put("J_W1_11-N", "mf_j_w2_9.png");
		lookupTable.put("N_W1_11-N", "mf_j_w2_9.png");
		lookupTable.put("J_W2_9", "mf_j_w2_9.png");
		lookupTable.put("N_W2_9", "mf_j_w2_9.png");

		lookupTable.put("J_W1_32", "mf_j_w1_32.png");
		lookupTable.put("J_W2_16", "mf_j_w1_32.png");

		lookupTable.put("N_W1_32", "mf_n_w1_32.png");
		lookupTable.put("N_W2_16", "mf_n_w1_32.png");

		lookupTable.put("J_W1_12", "mf_j_w1_12.png");
		lookupTable.put("N_W1_12", "mf_j_w1_12.png");
		lookupTable.put("J_W1_32-N_3", "mf_j_w1_12.png");
		lookupTable.put("N_W1_32-N_3", "mf_j_w1_12.png");
		lookupTable.put("J_W2_17", "mf_j_w1_12.png");
		lookupTable.put("N_W2_17", "mf_j_w1_12.png");

		lookupTable.put("J_W1_13", "mf_j_w2_13.png");
		lookupTable.put("J_W1_21", "mf_j_w2_13.png");
		lookupTable.put("J_W2_7", "mf_j_w2_13.png");
		lookupTable.put("J_W2_13", "mf_j_w2_13.png");

		lookupTable.put("N_W1_13", "mf_n_w1_13.png");
		lookupTable.put("N_W1_21", "mf_n_w1_13.png");
		lookupTable.put("N_W2_7", "mf_n_w1_13.png");
		lookupTable.put("N_W2_13", "mf_n_w1_13.png");

		lookupTable.put("J_W1_13-N_3", "mf_j_w_w1_13_n_3.png");
		lookupTable.put("N_W1_13-N_3", "mf_j_w_w1_13_n_3.png");
		lookupTable.put("J_W1_21-N_3", "mf_j_w_w1_13_n_3.png");
		lookupTable.put("N_W1_21-N_3", "mf_j_w_w1_13_n_3.png");

		lookupTable.put("J_W1_14", "mf_j_w1_14.png");
		lookupTable.put("J_W1_20", "mf_j_w1_14.png");

		lookupTable.put("N_W1_14", "mf_n_w1_14.png");
		lookupTable.put("N_W1_20", "mf_n_w1_14.png");

		lookupTable.put("J_W1_20-N_3", "mf_j_w1_14_n_3.png");
		lookupTable.put("N_W1_20-N_3", "mf_j_w1_14_n_3.png");
		lookupTable.put("N_W1_14-N_3", "mf_j_w1_14_n_3.png");
		lookupTable.put("J_W1_14-N_3", "mf_j_w1_14_n_3.png");

		lookupTable.put("J_W1_15", "mf_j_w2_10.png");
		lookupTable.put("J_W1_22", "mf_j_w2_10.png");
		lookupTable.put("J_W2_10", "mf_j_w2_10.png");
		lookupTable.put("J_W2_15", "mf_j_w2_10.png");
		lookupTable.put("J_W2_19", "mf_j_w2_10.png");

		lookupTable.put("N_W1_15", "mf_n_w1_15.png");
		lookupTable.put("N_W1_22", "mf_n_w1_15.png");
		lookupTable.put("N_W2_10", "mf_n_w1_15.png");
		lookupTable.put("N_W2_15", "mf_n_w1_15.png");
		lookupTable.put("N_W2_19", "mf_n_w1_15.png");

		lookupTable.put("J_W1_22-N_3", "mf_j_w1_16_n_3.png");
		lookupTable.put("N_W1_22-N_3", "mf_j_w1_16_n_3.png");
		lookupTable.put("J_W1_15-N_3", "mf_j_w1_16_n_3.png");
		lookupTable.put("N_W1_15-N_3", "mf_j_w1_16_n_3.png");
		lookupTable.put("W1_16", "mf_j_w1_16_n_3.png");
		lookupTable.put("J_W1_16-N", "mf_j_w1_16_n_3.png");
		lookupTable.put("N_W1_16-N", "mf_j_w1_16_n_3.png");

		lookupTable.put("J_W1_17-N", "mf_n_w2_11.png");
		lookupTable.put("N_W1_17-N", "mf_n_w2_11.png");
		lookupTable.put("N_W2_11", "mf_n_w2_11.png");
		lookupTable.put("J_W2_11", "mf_n_w2_11.png");

		lookupTable.put("J_W1_23-N", "mf_j_w1_29-n.png");
		lookupTable.put("J_W1_28-N", "mf_j_w1_29-n.png");
		lookupTable.put("J_W1_29-N", "mf_j_w1_29-n.png");

		lookupTable.put("N_W1_23-N", "mf_n_w1_28_n.png");
		lookupTable.put("N_W1_28-N", "mf_n_w1_28_n.png");
		lookupTable.put("N_W1_29-N", "mf_n_w1_28_n.png");

		lookupTable.put("J_W1_23-N_3", "mf_n_w1_23_n_3.png");
		lookupTable.put("N_W1_23-N_3", "mf_n_w1_23_n_3.png");
		lookupTable.put("J_W1_28-N_3", "mf_n_w1_23_n_3.png");
		lookupTable.put("N_W1_28-N_3", "mf_n_w1_23_n_3.png");
		lookupTable.put("J_W1_29-N_3", "mf_n_w1_23_n_3.png");
		lookupTable.put("N_W1_29-N_3", "mf_n_w1_23_n_3.png");

		lookupTable.put("J_W1_24-N", "mf_j_w2_18.png");
		lookupTable.put("J_W1_26-N", "mf_j_w2_18.png");
		lookupTable.put("J_W1_31-N", "mf_j_w2_18.png");
		lookupTable.put("J_W2_18", "mf_j_w2_18.png");

		lookupTable.put("N_W1_24-N", "mf_n_w2_18.png");
		lookupTable.put("N_W1_26-N", "mf_n_w2_18.png");
		lookupTable.put("N_W1_31-N", "mf_n_w2_18.png");
		lookupTable.put("N_W2_18", "mf_n_w2_18.png");

		lookupTable.put("J_W1_24-N_3", "mf_j_w1_24_n_3.png");
		lookupTable.put("J_W1_31-N_3", "mf_j_w1_24_n_3.png");
		lookupTable.put("J_W1_26-N_3", "mf_j_w1_24_n_3.png");
		lookupTable.put("N_W1_26-N_3", "mf_j_w1_24_n_3.png");
		lookupTable.put("N_W1_24-N_3", "mf_j_w1_24_n_3.png");
		lookupTable.put("N_W1_31-N_3", "mf_j_w1_24_n_3.png");

		lookupTable.put("J_W1_25-N", "mf_j_w1_27_n.png");
		lookupTable.put("J_W1_27-N", "mf_j_w1_27_n.png");

		lookupTable.put("N_W1_25-N", "mf_n_w1_27_n.png");
		lookupTable.put("N_W1_27-N", "mf_n_w1_27_n.png");

		lookupTable.put("J_W1_25-N_3", "mf_n_w1_27_n_3.png");
		lookupTable.put("N_W1_25-N_3", "mf_n_w1_27_n_3.png");
		lookupTable.put("J_W1_27-N_3", "mf_n_w1_27_n_3.png");
		lookupTable.put("N_W1_27-N_3", "mf_n_w1_27_n_3.png");

		lookupTable.put("J_W1_32-N_2", "mf_j_w1_32_n_2.png");
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

	boolean downloadIcons() throws Exception
	{
		File f = new File(Environment.getExternalStorageDirectory(), "weeWX");
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
			File newFile = new File(destDir + File.separator + fileName);
			LogMessage("Unzipping to " + newFile.getAbsolutePath());
			File dir = new File(newFile.getParent());
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
}