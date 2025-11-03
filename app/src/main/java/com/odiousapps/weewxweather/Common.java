package com.odiousapps.weewxweather;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
import android.net.Uri;
import android.os.Build;
import android.os.LocaleList;
import android.util.Base64;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;

import com.github.evilbunny2008.colourpicker.CPEditText;
import com.github.evilbunny2008.xmltojson.XmlToJson;

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
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.LiveData;
import okhttp3.ConnectionSpec;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings({"unused", "SameParameterValue", "ApplySharedPref",
		"SameReturnValue", "BooleanMethodIsAlwaysInverted",
		"SetTextI18n"})
public class Common
{
	private final static String PREFS_NAME = "WeeWxWeatherPrefs";
	final static boolean debug_on = false;
	final static boolean web_debug_on = false;

	final static String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";

	static final String WIDGET_UPDATE = "com.odiousapps.weewxweather.WIDGET_UPDATE";
	static final String UPDATE_INTENT = "com.odiousapps.weewxweather.UPDATE_INTENT";
	static final String REFRESH_INTENT = "com.odiousapps.weewxweather.REFRESH_INTENT";
	static final String TAB0_INTENT = "com.odiousapps.weewxweather.TAB0_INTENT";
	static final String EXIT_INTENT = "com.odiousapps.weewxweather.EXIT_INTENT";
	static final String INIGO_INTENT = "com.odiousapps.weewxweather.INIGO_UPDATE";
	static final String FAILED_INTENT = "com.odiousapps.weewxweather.FAILED_INTENT";

	static final String WIDGET_THEME_MODE = "widget_theme_mode";

	private static final long inigo_version = 4000;
	public static final long icon_version = 12;
	private static final String icon_url = "https://github.com/evilbunny2008/weeWXWeatherApp/releases/download/1.0.3/icons.zip";

	private static final BitmapFactory.Options options = new BitmapFactory.Options();

	private static Thread t = null;
	private static JSONObject nws = null;

	private static Typeface tf_bold = null;

	static final float[] NEGATIVE = {
			-1.0f, 0, 0, 0, 255, // red
			0, -1.0f, 0, 0, 255, // green
			0, 0, -1.0f, 0, 255, // blue
			0, 0, 0, 1.0f, 0  // alpha
	};

	static Colours colours;
	static String current_html_headers;

	private static final String html_header = """
            <!DOCTYPE html>
            <html lang="CURRENT_LANG">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
              <meta name="color-scheme" content="light dark">
              <link rel='stylesheet' href='file:///android_asset/weathericons.css'>
              <link rel='stylesheet' href='file:///android_asset/weathericons_wind.css'>
              <link rel='stylesheet' type='text/css' href='file:///android_asset/flaticon.css'>
              <style>
              :root {
                padding: 0;
                margin: 0 0 15px 0;;
                color: FG_HEX;
                background-color: BG_HEX;
                --font-icon-big: 1.5rem;
                --font-smaller: 1rem;
                --font-small: 1.1rem;
                --font-average: 1.25rem;
                --font-large: 1.35rem;
                --font-larger: 2rem;
                --font-big: 2rem;
                --font-huge: 4rem;
                text-size: var(--font-average);
              }
              a {
                color: ACCENT_HEX;
              }
              hr {
                border: 1px solid GRAY_HEX;
                width: 90%;
                margin: 15px auto;
              }
              html, body {
                font-family: sans-serif;
                font-size: var(--font-smaller);
                padding: 0;
                margin: 0;
                width: 100%;
                max-width: 100%;
                overflow-x: hidden;
                display: flex;
                flex-direction: column;
                justify-content: center;
                align-items: center;
              }
              * { box-sizing: border-box; }
              .header {
                text-align: center;
                font-size: var(--font-large);
              }
              .today {
                display: flex;
                flex-direction: column;
                margin: 0 10px 10px 10px;
              }
              .topRow {
                display: flex;
                flex-direction: row;
                align-items: center;
                justify-content: space-between;
              }
              .iconBig img, i {
                width: 80px;
                height: auto;
                line-height: 1;
                font-size: 5rem;
                align-self: start;
                margin-bottom: 4px;
              }
              .wordToday {
                font-size: var(--font-larger);
                font-weight: bold;
                text-align: center;
              }
              .bigTemp {
                display: flex;
                flex-direction: column;
                padding-left: 5px;
                align-items: flex-end;
                font-size: var(--font-big);
                font-weight: bold;
                text-align: center;
                justify-self: center;
              }
              .minTemp {
                font-size: var(--font-large);
              }
              .maxTemp, .forecastMax {
                color: #d32f2f;
              }
              .minTemp, .forecastMin {
                color: #1976d2;
              }
              .forecastMax, .forecastMin {
                font-size: var(--font-small);
              }
              .bigForecastText {
                text-align: justify;
                text-justify: inter-word;
                text-align-last: left;
                font-size: var(--font-large);
                max-width: 1000px;
              }
              .forecast {
                display: flex;
                flex-direction: row;
                flex-wrap: wrap;
                max-width: 1200px;
                margin: 0 5px 10px 5px;
                justify-content: center;
                column-gap: 100px;
              }
              .day {
                display: flex;
                flex: 1 1 500px;
                flex-direction: column;
                justify-content: space-evenly;
                margin: 5px 0 10px 0;
                max-width: 500px;
              }
              .dayTopRow {
                display: flex;
                flex-direction: row;
                width: 100%;
                align-items: center;
                justify-content: space-between;
                font-size: var(--font-average);
                font-weight: bold;
              }
              .iconSmall img, i {
                align-self: start;
                font-size: var(--font-large);
                width: 40px;
                height: auto;
                margin-right: 8px;
              }
              .dayTitle {
                font-weight: bold;
                font-size: var(--font-average);
                margin: 0 0 2px 0;
                text-align: center;
                align-items: center;
              }
              .desc {
                font-size: var(--font-smaller);
                text-align: justify;
                text-justify: inter-word;
              }
              .radarImage {
                display: block;
                background: transparent;
                max-width: 100vw;
                max-height: 100vh;
                width: 100%;
                height: auto;
                object-fit: contain;
                margin: 0;
                padding: 0;
                border: none;
              }
              .mainTemp {
                font-size: var(--font-huge);
                text-align: left;
                margin: 0 0 0 10px;
              }
              .apparentTemp {
                font-size: var(--font-larger);
                text-align: right;
                margin: 0 10px 0 0;
              }
              .icon {
                font-size: var(--font-average);
                width: 20px;
                text-align: center;
                margin: 0 4px 0 4px;
              }
              .todayCurrent {
                display: flex;
                flex-direction: column;
                width: 100%;
                max-width: 500px;
                padding: 0;
                margin: 0;
              }
              .topRowCurrent {
                display: flex;
                flex-direction: row;
                align-items: center;
                justify-content: space-between;
              }
              .dataTableCurrent {
                display: flex;
                flex-direction: column;
                padding: 0 5px 0 5px;
                margin: 0;
                width: 100%;
              }
              .dataRowCurrent {
                width: 100%;
                display: flex;
                justify-content: space-between;
                align-items: center;
                font-size: var(--font-small);
              }
              .dataCellCurrent {
                width: 100%;
                display: flex;
                align-items: center;
                white-space: nowrap;
              }
              .dataCellCurrent.right {
                justify-content: flex-end;
              }
              .statsLayout {
                display: flex;
                flex-direction: row;
                flex-wrap: wrap;
                max-width: 1200px;
                column-gap: 2px;
                justify-content: space-evenly;
                padding: 0 5px 0 5px;
              }
              .statsHeader {
                font-weight: bold;
                font-size: var(--font-large);
                text-align: center;
              }
              .statsSection {
                display: flex;
                flex-direction: column;
                width: 100%;
                max-width: 500px;
              }
              .statsDataRow {
                display: grid;
                grid-template-columns: minmax(100px, 2fr)
                                       minmax(55px, 1fr)
                                       minmax(3px, 5px)
                                       minmax(55px, 1fr)
                                       minmax(100px, 2fr);
              }
              .statsDataCell {
                align-self: center;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
              }
              .statsDataCell.left, .statsDataCell.midleft,
               .statsDataCell.Wind, .statsDataCell.Wind2 {
                text-align: left;
                justify-self: start;
              }
              .statsDataCell.right, .statsDataCell.midright,
               .statsDataCell.Rain, .statsDataCell.Rain2 {
                text-align: right;
                justify-self: end;
              }
              .statsDataCell.Wind, .statsDataCell.Rain {
                grid-column: span 2;
              }
              .statsDataCell.Wind2 {
                grid-column: span 4;
              }
              .statsDataCell.Rain2 {
                grid-column: span 1;
              }
              .statsSpacer {
                width: 1px;
                height: 100%;
                margin-left: 2px;
                margin-right: 2px;
              }
              .currentSpacer {
                width: 7px;
                height: 100%;
                margin-left: 2px;
                margin-right: 2px;
              }
              .currentSpacer.right {
                width: 3px;
              }
              @media (min-width: 500px) {
                .statsDataRow {
                  display: grid;
                  grid-template-columns: minmax(100px, 2fr)
                                         minmax(55px, 1fr)
                                         minmax(3px, 1fr)
                                         minmax(55px, 1fr)
                                         minmax(100px, 2fr);
                }
              }
              </style>
            </head>
            <body>
            """;

	static final String html_footer = "\n</body>\n</html>\n";

	static final String about_blurb = "Big thanks to the <a href='https://weewx.com'>weeWX project</a>, as this app " +
		"wouldn't be possible otherwise.<br><br>" +
		"Weather Icons from <a href='https://www.flaticon.com/'>FlatIcon</a> and " +
		"is licensed under <a href='https://creativecommons.org/licenses/by/3.0/'>CC 3.0 BY</a> and " +
		"<a href='https://github.com/erikflowers/weather-icons'>Weather Font</a> by Erik Flowers" +
		"<br><br>" +
		"weeWX Weather App v" + getAppVersion() + " is by <a href='https://odiousapps.com'>OdiousApps</a>.";

	static void replaceHex6String(String html_tag, int colour)
	{
		String hex = String.format(CPEditText.getFixedChar() + "%06X", 0xFFFFFF & colour);
		current_html_headers = current_html_headers.replaceAll(html_tag, hex);
	}

	static void replaceHex8String(String html_tag, int colour)
	{
		String hex = CPEditText.getFixedChar() + String.format("%08X", colour);
		current_html_headers = current_html_headers.replaceAll(html_tag, hex);
	}

	static void replaceHTMLString(String html_tag, String replacement)
	{
		current_html_headers = current_html_headers.replaceAll(html_tag, replacement);
	}

	static void reload()
	{
		Context context = getContext();
		if(context == null)
			return;

		current_html_headers = html_header;
		colours = new Colours();
		getDayNightMode();
		replaceHex6String("FG_HEX", KeyValue.fgColour);
		replaceHex6String("BG_HEX", KeyValue.bgColour);
		if(KeyValue.theme == R.style.AppTheme_weeWxWeatherApp_Dark_Common)
		{
			replaceHex6String("ACCENT_HEX", colours.DarkBlueAccent);
			replaceHex6String("GRAY_HEX", colours.DarkGray);
		} else {
			replaceHex6String("ACCENT_HEX", colours.LightBlueAccent);
			replaceHex6String("GRAY_HEX", colours.LightGray);
		}

		String lang = Locale.getDefault().getLanguage();
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
		{
			LocaleList locales = weeWXApp.getLocales();
			if(!locales.isEmpty())
				lang = locales.get(0).toLanguageTag();
		}

		lang = lang.split("-")[0].trim();
		replaceHTMLString("CURRENT_LANG", lang);
		LogMessage("Current app language: " + lang);
	}

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

		reload();

		options.inJustDecodeBounds = false;
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;

		LogMessage("app_version=" + getAppVersion());
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

		int pos = GetIntPref("updateInterval", 1);
		if(pos <= 0)
			return def;

		long period;

		switch (pos)
		{
			case 1 -> period = 5 * 60000;
			case 2 -> period = 10 * 60000;
			case 3 -> period = 15 * 60000;
			case 4 -> period = 30 * 60000;
			case 5 -> period = 60 * 60000;
			default ->
			{
				return def;
			}
		}

		return new long[]{period, 45000};
	}

	static void setAlarm(String from)
	{
		Context context = getContext();
		if(context == null)
			return;

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

		Intent myAlarm = new Intent(context.getApplicationContext(), UpdateCheck.class);
		PendingIntent recurringAlarm = PendingIntent.getBroadcast(context.getApplicationContext(),
				0, myAlarm, PendingIntent.FLAG_IMMUTABLE);

		AlarmManager alarms = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		alarms.set(AlarmManager.RTC_WAKEUP, start, recurringAlarm);
	}

	static String getAppVersion()
	{
		return BuildConfig.VERSION_NAME;
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

	static void SetStringPref(String name, String value)
	{
		Context context = getContext();
		if(context == null)
			return;

		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(name, value);
		editor.apply();

		//LogMessage("Updating '" + name + "'='" + value + "'");
	}

	static void RemovePref(String name)
	{
		Context context = getContext();
		if(context == null)
			return;

		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();

		// Wipe the entry...
		editor.putString(name, "");
		editor.remove(name);
		editor.apply();

		//LogMessage("Removing '" + name + "'");
	}

	static void clearPref()
	{
		Context context = getContext();
		if(context == null)
			return;

		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		prefs.edit().clear().apply();

		//LogMessage("Clearing Prefs");
	}

	static void commitPref()
	{
		Context context = getContext();
		if(context == null)
			return;

		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.commit();

		//LogMessage("Commiting Prefs");
	}

	static boolean isPrefSet(String name)
	{
		Context context = getContext();
		if(context == null)
			return false;

		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		String default_value = "unknown";

		try
		{
			String value = settings.getString(name, default_value).trim();
			if(!value.equals(default_value))
			{
				LogMessage("Pref '" + name + "'was set to '" + value + "'.");
				return true;
			}
		} catch(Exception ignored) {
		}

		//LogMessage("Pref '" + name + "'was not set.");
		return false;
	}

	static String GetStringPref(String name, String default_value)
	{
		Context context = getContext();
		if(context == null)
			return null;

		SharedPreferences settings;
		String value;

		try
		{
			settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
			value = settings.getString(name, default_value);
		} catch(ClassCastException cce) {
			doStackOutput(cce);
			return default_value;
		} catch(Exception e) {
			//LogMessage("GetStringPref(" + name + ", " + default_value + ") Err: " + e);
			doStackOutput(e);
			return default_value;
		}

		//LogMessage(name + "'='" + value + "'");

		return value;
	}

	static void SetLongPref(String name, long value)
	{
		SetStringPref(name, String.valueOf(value));
	}

	static long GetLongPref(String name, long default_value)
	{
		String val = GetStringPref(name, String.valueOf(default_value));
		if(val == null)
			return default_value;

		return Long.parseLong(val);
	}

	static void SetFloatPref(String name, float value)
	{
		SetStringPref(name, String.valueOf(value));
	}

	static float GetFloatPref(String name, float default_value)
	{
		String str = GetStringPref(name, String.valueOf(default_value));
		if(str == null)
			return default_value;

		String val = str.trim();
		if(val.isBlank())
			return 0.0f;

		return Float.parseFloat(val);
	}

	static void SetIntPref(String name, int value)
	{
		SetStringPref(name, String.valueOf(value));
	}

	static int GetIntPref(String name, int default_value)
	{
		String val = GetStringPref(name, String.valueOf(default_value));
		if(val == null)
			return default_value;
		return (int)Float.parseFloat(val);
	}

	static void SetBoolPref(String name, boolean value)
	{
		String val = "0";
		if(value)
			val = "1";

		SetStringPref(name, val);
	}

	static boolean GetBoolPref(String name, boolean default_value)
	{
		String value = "0";
		if(default_value)
			value = "1";

		String val = GetStringPref(name, value);
		if(val == null)
			return default_value;

		return val.equals("1");
	}

	static File getFilesDir()
	{
		Context context = getContext();
		if(context == null)
			return null;

		return context.getFilesDir();
	}

	static final class myWebChromeClient extends WebChromeClient
	{
		@Override
		public boolean onConsoleMessage(ConsoleMessage cm)
		{
			return true;
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
				sb.append("&nbsp;");
			else
				sb.append(first.max);
			sb.append("</div>\n");

			sb.append("\t\t\t<div class='minTemp'>");
			if(first.min.isBlank() || first.min.equals("&deg;C") || first.min.equals("&deg;F"))
				sb.append("&nbsp;");
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

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
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

			day.icon = "https://www.bom.gov.au" + bit.split("<img src='", 2)[1].split("' alt='", 2)[0].trim();

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

				day.icon = "https://www.bom.gov.au" + bit.split("<img src='", 2)[1].split("' alt='", 2)[0].trim();
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
			sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

			timestamp = 0;
			Date df = sdf.parse(tmp);
			if(df != null)
				timestamp = df.getTime();

			JSONArray mydays = jobj.getJSONArray("data");
			for(int i = 0; i < mydays.length(); i++)
			{
				Day day = new Day();
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

				day.timestamp = 0;
				df = sdf.parse(mydays.getJSONObject(i).getString("date"));
				if(df != null)
					day.timestamp = df.getTime();

				sdf = new SimpleDateFormat("EEEE d", Locale.getDefault());
				day.day = sdf.format(day.timestamp);

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

				if(!mydays.getJSONObject(i).getString("extended_text").equals("null"))
					day.text = mydays.getJSONObject(i).getString("extended_text");

				String fileName = bomlookup(mydays.getJSONObject(i).getString("icon_descriptor"));
				if(!fileName.equals("null"))
				{
					if(!use_icons)
					{
						if(!fileName.equals("frost"))
							day.icon = "wi wi-bom-" + fileName;
						else
							day.icon = "flaticon-thermometer";
					} else {
						fileName = checkImage("bom2" + fileName + ".png", null);
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
				}

				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	private static String bomlookup(String icon)
	{
		icon = icon.replace("-", "_");

		return switch (icon)
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

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
		long timestamp = GetLongPref("rssCheck", 0) * 1000;
		String desc;
		List<Day> days = new ArrayList<>();

		try
		{
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
		} catch(Exception e) {
			doStackOutput(e);
			return null;
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

							ByteArrayOutputStream stream = new ByteArrayOutputStream();
							bmp.compress(Bitmap.CompressFormat.JPEG, 75, stream);
							byte[] byteArray = stream.toByteArray();
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

							ByteArrayOutputStream stream = new ByteArrayOutputStream();
							bmp.compress(Bitmap.CompressFormat.JPEG, 75, stream);
							byte[] byteArray = stream.toByteArray();
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
							if(bmp3 == null)
								return null;

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

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
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

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
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

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
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
//				LogMessage("day.icon=" + day.icon);

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

		boolean metric = GetBoolPref("metric", true);
		boolean use_icons = GetBoolPref("use_icons", false);
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
							day.min = (Math.round(precip / 25.4 * 1000.0) / 1000.0) + "in";

						icon = tsdata.getJSONObject("next_1_hours").getJSONObject("summary").getString("symbol_code");
					} catch(Exception e) {
						double precip = tsdata.getJSONObject("next_6_hours").getJSONObject("details").getDouble("precipitation_amount");
						if(metric)
							day.min = precip + "mm";
						else
							day.min = (Math.round(precip / 25.4 * 1000.0) / 1000.0) + "in";

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

			lastTS += 86400000;

			if(lastTS > startTS + 864000000)
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

		boolean use_icons = GetBoolPref("use_icons", false);
		boolean metric = GetBoolPref("metric", true);
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
		Context context = getContext();
		if(context == null)
			return new String[]{null, "Failed to load or download icon from url: " + url};

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

	private static String[] checkFilesIt(String url) throws Exception
	{
		Context context = getContext();
		if(context == null)
			return new String[]{null, "Failed to load or download from url: " + url};

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

	static String[] processYahoo(String data)
	{
		return processYahoo(data, false);
	}

	static String[] processYahoo(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		boolean metric = GetBoolPref("metric", true);

		List<Day> days = new ArrayList<>();
		long timestamp = GetLongPref("rssCheck", 0) * 1000;
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

	static void SendIntents()
	{
		getWeather();
		getForecast();
		WidgetProvider.updateAppWidget();
		NotificationManager.updateNotificationMessage(UPDATE_INTENT);
		LogMessage("update intent broadcasted");
	}

	private static void SendRefresh()
	{
		WidgetProvider.updateAppWidget();
		NotificationManager.updateNotificationMessage(REFRESH_INTENT);
		LogMessage("refresh intent broadcasted");
	}

	static void getWeather()
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
				if(fromURL == null || fromURL.isBlank())
					return;

				reallyGetWeather(fromURL);
				SendRefresh();
			} catch(Exception e) {
				doStackOutput(e);
				SetStringPref("lastError", e.toString());
				SendFailedIntent();
			}
		});

		t.start();
	}

	static void reallyGetWeather(String fromURL)
	{
		String line = downloadString(fromURL);
		if(line != null && !line.isBlank())
		{
			String[] bits = line.split("\\|");
			if(Double.parseDouble(bits[0]) < inigo_version)
			{
				if(GetLongPref("inigo_version", 0) < inigo_version)
				{
					SetLongPref("inigo_version", inigo_version);
					sendAlert();
				}
			}

			if(Double.parseDouble(bits[0]) >= 4000)
			{
				StringBuilder sb = new StringBuilder();
				for (int i = 1; i < bits.length; i++)
				{
					if(!isEmpty(sb))
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
	static boolean checkConnection()
	{
		Context context = getContext();
		if(context == null)
			return false;

		if(!GetBoolPref("onlyWIFI", false))
			return true;

		ConnectivityManager connMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
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

	private static void SendFailedIntent()
	{
		NotificationManager.updateNotificationMessage(FAILED_INTENT);
		LogMessage("failed_intent broadcast.");
	}

	// https://stackoverflow.com/questions/8710515/reading-an-image-file-into-bitmap-from-sdcard-why-am-i-getting-a-nullpointerexc

	static Bitmap loadImage(String fileName)
	{
		Context context = getContext();
		if(context == null)
			return null;

		File f = new File(context.getExternalFilesDir(""), "weeWX");
		f = new File(f, "icons");
		f = new File(f, fileName);

		return loadImage(f);
	}

	static Bitmap loadImage(File file)
	{
		return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
	}

	private static String checkImage(String fileName, String icon)
	{
		Context context = getContext();
		if(context == null)
			return null;

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

	private static void downloadImage(final String fileName, final String imageURL)
	{
		Context context = getContext();
		if(context == null)
			return;

		Thread t = new Thread(() ->
		{
			if(fileName.isBlank() || imageURL.isBlank())
				return;

			LogMessage("checking: " + imageURL);

			try
			{
				File f = new File(context.getExternalFilesDir(""), "weeWX");
				f = new File(f, "icons");
				if(!f.exists())
					if(!f.mkdirs())
						return;

				f = new File(f, fileName);

				if(f.exists() && f.isFile() && f.length() > 0)
					return;

				LogMessage("f == " + f.getAbsolutePath());
				LogMessage("imageURL == " + imageURL);

				downloadJSOUP(f, imageURL);
			} catch(Exception e) {
				doStackOutput(e);
			}
		});

		t.start();
	}

	private static Bitmap combineImage(Bitmap bmp1, String fnum, String snum)
	{
		Context context = getContext();
		if(context == null)
			return null;

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
		Context context = getContext();
		if(context == null)
			return null;

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
		Context context = getContext();
		if(context == null)
			return null;

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
		paint.setColor(ContextCompat.getColor(context, R.color.LightPrussianBlue));
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
		Context context = getContext();
		if(context == null)
			return;

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

	static String downloadSettings(String url)
	{
		String UTF8_BOM = "\uFEFF";

		SetStringPref("SETTINGS_URL", url);

		String cfg = downloadString(url);

		if(cfg == null)
			return null;

		if(cfg.startsWith(UTF8_BOM))
			cfg = cfg.substring(1).trim();

		return cfg;
	}

	static File downloadRADAR(String radar) throws Exception
	{
		Context context = getContext();
		if(context == null)
			return null;

		LogMessage("starting to download image from: " + radar);
		File file = new File(context.getFilesDir(), "/radar.gif.tmp");
		return downloadBinary(file, radar);
	}

	static boolean checkURL(String setting) throws Exception
	{
		LogMessage("checking: " + setting);
		URL url = new URL(setting);
		URLConnection conn = url.openConnection();
		conn.connect();

		return true;
	}

	static String downloadForecast() throws Exception
	{
		String str1 = GetStringPref("fctype", "Yahoo");
		if(str1 == null)
			return null;

		String str2 = GetStringPref("FORECAST_URL", "");
		if(str2 == null)
			return null;

		String str3 = GetStringPref("bomtown", "");
		if(str3 == null)
			return null;

		return downloadForecast(str1, str2, str3);
	}

	static String downloadForecast(String fctype, String forecast, String bomtown) throws Exception
	{
		String tmp;

		if(!fctype.equals("bom2"))
			tmp = downloadString(forecast);
		else
			tmp = downloadString2(forecast);

		if(tmp == null)
			return null;

		if(fctype.equals("bom.gov.au"))
		{
			boolean found = false;

			JSONObject jobj = new JSONObject();
			JSONObject tmp_result = new XmlToJson.Builder(tmp).build().toJson();

			if(tmp_result != null)
				jobj = tmp_result.getJSONObject("product");

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

	private static String downloadString2(String fromURL) throws Exception
	{
		Connection.Response resultResponse = Jsoup.connect(fromURL)
													.userAgent(UA)
													.referrer("https://www.bom.gov.au")
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

	private static int responseCount(Response response)
	{
		int result = 1;
		while ((response = response.priorResponse()) != null)
			result++;
		return result;
	}

	private static String downloadString(String fromURL)
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
					if(responseCount(response) >= 3)
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

		try(Response response = client.newCall(request).execute())
		{
			String rb = response.body().string().trim();
			LogMessage(rb);
			return rb;
		} catch(Exception ignored) {}

		return null;
	}

	private static void writeFile(String fileName, String data) throws Exception
	{
		Context context = getContext();
		if(context == null)
			return;

		File dir = new File(context.getExternalFilesDir(""), "weeWX");
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

	static File downloadJSOUP(File f, String fromURL) throws Exception
	{
		File dir = f.getParentFile();
		assert dir != null;
		if(!dir.exists())
			if(!dir.mkdirs())
				return null;

		Connection.Response resultResponse = Jsoup.connect(fromURL).userAgent(UA).maxBodySize(Integer.MAX_VALUE).ignoreContentType(true).execute();
		FileOutputStream out = new FileOutputStream(f);
		out.write(resultResponse.bodyAsBytes());
		out.flush();
		out.close();
		publish(f);

		return f;
	}

	static File downloadBinary(File f, String fromURL) throws Exception
	{
		File dir = f.getParentFile();
		assert dir != null;
		if(!dir.exists())
			if(!dir.mkdirs())
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
						if(responseCount(response) >= 3)
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

		try (Response response = client.newCall(request).execute())
		{
			outputStream.write(response.body().bytes());
			outputStream.flush();
			outputStream.close();
			publish(f);
			return f;
		}
	}

	private static void publish(File f)
	{
		Context context = getContext();
		if(context == null)
			return;

		LogMessage("wrote to " + f.getAbsolutePath());
		if(f.exists())
			MediaScannerConnection.scanFile(context, new String[]{f.getAbsolutePath()}, null, null);
	}

	static boolean downloadIcons() throws Exception
	{
		Context context = getContext();
		if(context == null)
			return false;

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

	private static void unzip(File zipFilePath, File destDir) throws Exception
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
		}

		zis.closeEntry();
		zis.close();
		fis.close();
	}

	static boolean checkForImages()
	{
		Context context = getContext();
		if(context == null)
			return false;

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
			if(!f.exists() || !f.isFile())
				return false;
		}

		return true;
	}

	static Context getContext()
	{
		if(weeWXApp.getInstance() == null)
			return null;

		return weeWXApp.getInstance().getApplicationContext();
	}

	static void getForecast()
	{
		getForecast(false);
	}

	static void getForecast(boolean force)
	{
		if(GetBoolPref("radarforecast", true))
		{
			return;
		}

		final String forecast_url = GetStringPref("FORECAST_URL", "");
		if(forecast_url == null || forecast_url.isBlank())
			return;

		if(!checkConnection() && !force)
		{
			LogMessage("Not on wifi and not a forced refresh");
			return;
		}

		final long current_time = Math.round(System.currentTimeMillis() / 1000.0);
		String str1 = GetStringPref("forecastData", "");
		long long1 = GetLongPref("rssCheck", 0);

		if(str1 == null || str1.isBlank() || long1 + 7190 < current_time)
		{
			LogMessage("no forecast data or cache is more than 2 hour old");
		} else {
			LogMessage("cache isn't more than 2 hour old");
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
			String date = sdf.format(GetLongPref("rssCheck", 0) * 1000);
			LogMessage("rsscheck == " + date);
			date = sdf.format(current_time * 1000);
			LogMessage("current_time == " + date);
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
					SetLongPref("rssCheck", current_time);
					SetStringPref("forecastData", tmp);
				}
			} catch(Exception e) {
				doStackOutput(e);
			}
		});

		t.start();
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
			Common.doStackOutput(e);
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

	static void getDayNightMode()
	{
		Context context = getContext();
		if(context == null)
			return;

		int current_mode, current_theme, bgColour, fgColour;
		boolean isWidgetSet = false;
		boolean prefSet = Common.isPrefSet("DayNightMode");
		int nightDaySetting = getAppDayNightSetting();
		int nightModeFlags = weeWXApp.getUImode() & Configuration.UI_MODE_NIGHT_MASK;
		KeyValue.widget_theme_mode = GetIntPref(WIDGET_THEME_MODE, 0);

		current_mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
		current_theme = R.style.AppTheme_weeWxWeatherApp_Light_Common;
		bgColour = ContextCompat.getColor(context, R.color.White);
		fgColour = ContextCompat.getColor(context, R.color.Black);

		if(prefSet)
		{
			if(nightDaySetting == 0)
			{
				LogMessage("Night mode off...");
				current_mode = AppCompatDelegate.MODE_NIGHT_NO;
				current_theme = R.style.AppTheme_weeWxWeatherApp_Light_Common;
				bgColour = ContextCompat.getColor(context, R.color.White);
				fgColour = ContextCompat.getColor(context, R.color.Black);
			} else if(nightDaySetting == 1) {
				LogMessage("Night mode on...");
				current_mode = AppCompatDelegate.MODE_NIGHT_NO;
				current_theme = R.style.AppTheme_weeWxWeatherApp_Dark_Common;
				bgColour = ContextCompat.getColor(context, R.color.Black);
				fgColour = ContextCompat.getColor(context, R.color.White);
			}

			if(KeyValue.widget_theme_mode == 1)
			{
				isWidgetSet = true;
				KeyValue.widgetBG = bgColour;
				KeyValue.widgetFG = fgColour;
			}
		}

		if(!prefSet || (nightDaySetting != 0 && nightDaySetting != 1))
		{
			if(nightModeFlags == Configuration.UI_MODE_NIGHT_NO)
			{
				LogMessage("Night mode off...");
				bgColour = ContextCompat.getColor(context, R.color.White);
				fgColour = ContextCompat.getColor(context, R.color.Black);
				current_theme = R.style.AppTheme_weeWxWeatherApp_Light_Common;
			} else {
				LogMessage("Night mode on...");
				bgColour = ContextCompat.getColor(context, R.color.Black);
				fgColour = ContextCompat.getColor(context, R.color.White);
				current_theme = R.style.AppTheme_weeWxWeatherApp_Dark_Common;
			}
		}

		if(KeyValue.widget_theme_mode == 2 || (KeyValue.widget_theme_mode == 0 && nightModeFlags == Configuration.UI_MODE_NIGHT_NO))
		{
			isWidgetSet = true;
			KeyValue.widgetBG = ContextCompat.getColor(context, R.color.White);
			KeyValue.widgetFG = ContextCompat.getColor(context, R.color.Black);
		}

		if(KeyValue.widget_theme_mode == 3 || (KeyValue.widget_theme_mode == 0 && nightModeFlags == Configuration.UI_MODE_NIGHT_YES))
		{
			isWidgetSet = true;
			KeyValue.widgetBG = ContextCompat.getColor(context, R.color.Black);
			KeyValue.widgetFG = ContextCompat.getColor(context, R.color.White);
		}

		if(KeyValue.widget_theme_mode == 4 || !isWidgetSet)
		{
			KeyValue.widgetBG = Common.GetIntPref("bgColour", 0x00000000);
			KeyValue.widgetFG = Common.GetIntPref("fgColour", 0xFFFFFFFF);
		}

		KeyValue.theme = current_theme;
		KeyValue.mode = current_mode;
		KeyValue.bgColour = bgColour;
		KeyValue.fgColour = fgColour;
	}

	static int getAppDayNightSetting()
	{
		return GetIntPref("DayNightMode", 2);
	}

	static Activity getActivity()
	{
		Context context = getContext();
		if(context == null)
			return null;

		while(context instanceof ContextWrapper)
		{
			if(context instanceof Activity)
				return (Activity) context;

			context = ((ContextWrapper)context).getBaseContext();
		}

		return null;
	}

	public static class NotificationLiveData extends LiveData<String>
	{
		public void setNotification(String message)
		{
			postValue(message);
		}
	}

	public static class NotificationManager
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

	static class Colours
	{
		public int widgetBG = 0x00000000;
		public int widgetFG = 0xFFFFFFFF;

		public int White = 0xFFFFFFFF;
		public int Black = 0xFF000000;

		public int AlmostBlack = 0xFF121212;
		public int LightBlueAccent = 0xFF82B1FF;
		public int DarkBlueAccent = 0xFF1E88E5;
		public int DarkGray = 0xFF333333;
		public int LightGray = 0xFFE0E0E0;

		public Colours()
		{
			Context context = getContext();
			if(context != null)
			{
				widgetBG = ContextCompat.getColor(context, R.color.widgetBackgroundColour);
				widgetFG = ContextCompat.getColor(context, R.color.widgetTextColour);

				White = ContextCompat.getColor(context, R.color.White);
				Black = ContextCompat.getColor(context, R.color.Black);

				AlmostBlack = ContextCompat.getColor(context, R.color.AlmostBlack);
				LightBlueAccent = ContextCompat.getColor(context, R.color.LightBlueAccent);
				DarkBlueAccent = ContextCompat.getColor(context, R.color.DarkBlueAccent);
				DarkGray = ContextCompat.getColor(context, R.color.DarkGray);
				LightGray = ContextCompat.getColor(context, R.color.LightGray);
			}
		}
	}
}