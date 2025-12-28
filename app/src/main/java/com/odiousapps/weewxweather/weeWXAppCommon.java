package com.odiousapps.weewxweather;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.Cursor;
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
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.github.evilbunny2008.xmltojson.XmlToJson;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONArray;
import org.json.JSONObject;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.LiveData;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings({"unused", "SameParameterValue", "ApplySharedPref", "ConstantConditions",
		"SameReturnValue", "BooleanMethodIsAlwaysInverted", "SetTextI18n", "ConstantLocale",
		"CallToPrintStackTrace"})
class weeWXAppCommon
{
	private final static String PREFS_NAME = "WeeWxWeatherPrefs";
	final static String LOGTAG = "weeWXApp";
	static int debug_level = KeyValue.i;
	final static boolean debug_html = false;
	final static boolean web_debug_on = false;
	private final static int maxLogLength = 5_000;

	static final String UTF8_BOM = "\uFEFF";

	static final int default_timeout = 5_000;
	static final int maximum_retries = 3;
	static final int retry_sleep_time = 1_000;

	private static Uri logFileUri = null;

	static final String EXIT_INTENT = "com.odiousapps.weewxweather.EXIT_INTENT";
	static final String INIGO_INTENT = "com.odiousapps.weewxweather.INIGO_UPDATE";

	static final String UPDATECHECK = "com.odiousapps.weewxweather.UPDATECHECK";

	static final String REFRESH_DARKMODE_INTENT = "com.odiousapps.weewxweather.REFRESH_DARKMODE_INTENT";
	static final String REFRESH_FORECAST_INTENT = "com.odiousapps.weewxweather.REFRESH_FORECAST_INTENT";
	static final String REFRESH_RADAR_INTENT = "com.odiousapps.weewxweather.REFRESH_RADAR_INTENT";
	static final String REFRESH_WEATHER_INTENT = "com.odiousapps.weewxweather.REFRESH_WEATHER_INTENT";
	static final String REFRESH_WEBCAM_INTENT = "com.odiousapps.weewxweather.REFRESH_WEBCAM_INTENT";

	static final String STOP_FORECAST_INTENT = "com.odiousapps.weewxweather.STOP_FORECAST_INTENT";
	static final String STOP_RADAR_INTENT = "com.odiousapps.weewxweather.STOP_RADAR_INTENT";
	static final String STOP_WEATHER_INTENT = "com.odiousapps.weewxweather.STOP_WEATHER_INTENT";
	static final String STOP_WEBCAM_INTENT = "com.odiousapps.weewxweather.STOP_WEBCAM_INTENT";

	static final String WIDGET_THEME_MODE = "widget_theme_mode";

	static final SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
	static final SimpleDateFormat sdf2 = new SimpleDateFormat("EEEE d", Locale.getDefault());
	static final SimpleDateFormat sdf3 = new SimpleDateFormat("h:mm aa d MMMM yyyy", Locale.getDefault());
	static final SimpleDateFormat sdf4 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
	static final SimpleDateFormat sdf5 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
	static final SimpleDateFormat sdf6 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault());
	static final SimpleDateFormat sdf7 = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
	static final SimpleDateFormat sdf8 = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault());
	static final SimpleDateFormat sdf9 = new SimpleDateFormat("HH:mm d MMMM yyyy", Locale.CANADA_FRENCH);
	static final SimpleDateFormat sdf10 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
	static final SimpleDateFormat sdf11 = new SimpleDateFormat("dd.MM.yyyy' 'HH", Locale.getDefault());
	static final SimpleDateFormat sdf12 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
	static final SimpleDateFormat sdf13 = new SimpleDateFormat("dd MMM yyyy HH:mm:ss.SSS", Locale.getDefault());
	static final SimpleDateFormat sdf14 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS XXX", Locale.getDefault());

	private static final BitmapFactory.Options options = new BitmapFactory.Options();

	private static JSONObject nws = null;

	private static Typeface tf_bold = null;

	private final static ExecutorService executor = Executors.newFixedThreadPool(5);
	private static final ExecutorService prefsExec = Executors.newSingleThreadExecutor();

	private static Future<?> forecastTask, radarTask, weatherTask, webcamTask;

	private static long ftStart, rtStart, wcStart, wtStart; //, lastWeatherRefresh = 0;

	static final float[] NEGATIVE = {
			-1.0f, 0, 0, 0, 255, // red
			0, -1.0f, 0, 0, 255, // green
			0, 0, -1.0f, 0, 255, // blue
			0, 0, 0, 1.0f, 0  // alpha
	};

	static
	{
		if(com.odiousapps.weewxweather.BuildConfig.DEBUG)
			debug_level =  KeyValue.v;

		try
		{
			System.setProperty("http.agent", NetworkClient.UA);
		} catch(Exception e) {
			LogMessage("Error! e: " + e, true, KeyValue.e);
		}

		try
		{
			tf_bold = Typeface.create("sans-serif", Typeface.BOLD);
		} catch(Exception e) {
			LogMessage("Error! e: " + e, true, KeyValue.e);
		}

		options.inJustDecodeBounds = false;
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
	}

	static void doStackOutput(Throwable t)
	{
		t.printStackTrace();
	}

	static long[] getPeriod()
	{
		long[] def = new long[]{0L, 0L};

		int pos = (int)KeyValue.readVar("updateInterval", weeWXApp.updateInterval_default);
		if(pos <= 0)
			return def;

		long period = 60_000L;

		switch(pos)
		{
			case 1, 6 -> period *= 5;
			case 2 -> period *= 10;
			case 3 -> period *= 15;
			case 4 -> period *= 30;
			case 5 -> period *= 60;
			default ->
			{
				return def;
			}
		}

		return new long[]{period, 45_000L};
	}

	static void LogColour(View view, String name)
	{
		Integer colour = null;

		switch(view)
		{
			case TextInputLayout v ->
			{
				EditText editText = v.getEditText();
				if(editText != null)
					colour = editText.getCurrentTextColor();
			}
			case MaterialTextView v -> colour = v.getCurrentTextColor();
			case TextInputEditText v -> colour = v.getCurrentTextColor();
			case MaterialSwitch v -> colour = v.getCurrentTextColor();
			case MaterialAutoCompleteTextView v -> colour = v.getCurrentTextColor();
			case MaterialRadioButton v -> colour = v.getCurrentTextColor();
			case TextView v -> colour = v.getCurrentTextColor();
			default -> LogMessage("Uncaught view type: " + view, KeyValue.w);
		}

		if(colour != null)
			Log.w(LOGTAG, "MainActivity.onCreate() colorSurface of " + name + ": #" + Integer.toHexString(colour));
	}

	static void LogMessage(String text)
	{
		LogMessage(text, false, KeyValue.v);
	}

	static void LogMessage(String text, int level)
	{
		LogMessage(text, false, level);
	}

	static void LogMessage(String text, boolean showAnyway)
	{
		LogMessage(text, showAnyway, KeyValue.i);
	}

	static void LogMessage(String text, boolean showAnyway, int level)
	{
		Context context = weeWXApp.getInstance();
		if(context == null)
			return;

		if(text == null || text.isBlank())
			return;

		if((boolean)KeyValue.readVar("save_app_debug_logs", weeWXApp.save_app_debug_logs_default))
		{
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
				appendWithMediaStore(text, level);
			else
				appendLegacy(text, level);
		}

		if(level <= debug_level || showAnyway)
		{
			text = removeWS(text);
			if(text.length() > maxLogLength)
				text = "[String truncated to " + maxLogLength + " bytes] " + text.substring(0, maxLogLength);

			if(level == KeyValue.e)
				Log.e(LOGTAG, "message='" + text + "'");
			else if(level == KeyValue.w)
				Log.w(LOGTAG, "message='" + text + "'");
			else if(level == KeyValue.i)
				Log.i(LOGTAG, "message='" + text + "'");
			else if(level == KeyValue.d)
				Log.d(LOGTAG, "message='" + text + "'");
			else
				Log.v(LOGTAG, "message='" + text + "'");
		} else
			Log.v(LOGTAG, "hidden message='" + text + "'");
	}

	static byte[] gzipToBytes(String text) throws IOException
	{
		if(text == null || text.isBlank())
			return null;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzip = new GZIPOutputStream(baos);
		gzip.write(text.getBytes(StandardCharsets.UTF_8));
		gzip.close(); // flush + finish

		return baos.toByteArray();
	}

	static String levelToName(int level)
	{
		String name = "INVALID";

		switch(level)
		{
			case KeyValue.e -> name = "ERR";
			case KeyValue.w -> name = "WARN";
			case KeyValue.i -> name = "INFO";
			case KeyValue.d -> name = "DEBUG";
			case KeyValue.v -> name = "VERBOSE";
		}

		return name;
	}

	private static void appendLegacy(String text, int level)
	{
		try
		{
			String string_time = sdf13.format(System.currentTimeMillis());

			String tmpStr = string_time + " " + levelToName(level) + ": " + text.strip() + "\n";

			File file = getExtFile("weeWX", weeWXApp.debug_filename);
			boolean needsPublishing = !file.exists();
			FileOutputStream fos = new FileOutputStream(file, true);

			if(weeWXApp.debug_filename.endsWith(".gz"))
				fos.write(gzipToBytes(text));
			else if(weeWXApp.debug_filename.endsWith(".txt"))
				fos.write(text.getBytes());

			fos.close();

			if(needsPublishing)
				publish(file);
		} catch (IOException e) {
			doStackOutput(e);
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.Q)
	private static void appendWithMediaStore(String text, int level)
	{
		Context context = weeWXApp.getInstance();
		String folderName = Environment.DIRECTORY_DOWNLOADS + "/weeWX/";

		// ================
		// 1. Ensure weeWX folder exists
		// ================
		Uri filesCollection = MediaStore.Files.getContentUri("external");

		// --- 1. Verify that cached URI still exists ---
		if(logFileUri != null)
		{
			try(Cursor c = context.getContentResolver().query(
					logFileUri,
					new String[]{ MediaStore.Files.FileColumns._ID },
					null, null, null))
			{
				if(c == null || !c.moveToFirst())
				{
					// The file was deleted -> reset cached URI
					logFileUri = null;
				}
			} catch (Exception e) {
				// If any error occurs, assume file is gone
				logFileUri = null;
			}
		}

		// --- 2. If logFileUri is null, attempt to find or create it ---
		if(logFileUri == null)
		{
			// First try to find it
			String[] projection = { MediaStore.Files.FileColumns._ID };
			String selection = MediaStore.Files.FileColumns.DISPLAY_NAME + "=? AND " +
			                   MediaStore.Files.FileColumns.RELATIVE_PATH + "=?";
			String[] args = { weeWXApp.debug_filename, folderName };

			try(Cursor cursor = context.getContentResolver().query(filesCollection, projection, selection, args, null))
			{
				if(cursor != null && cursor.moveToFirst())
				{
					long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID));
					logFileUri = ContentUris.withAppendedId(filesCollection, id);
				}
			} catch (Exception e) {
				doStackOutput(e);
			}

			// If not found, create it
			if(logFileUri == null && weeWXApp.debug_filename.endsWith(".gz"))
			{
				ContentValues values = new ContentValues();
				values.put(MediaStore.Files.FileColumns.DISPLAY_NAME, weeWXApp.debug_filename);
				values.put(MediaStore.Files.FileColumns.MIME_TYPE, "application/gzip");
				values.put(MediaStore.Files.FileColumns.RELATIVE_PATH, folderName);

				logFileUri = context.getContentResolver().insert(filesCollection, values);
				if(logFileUri == null)
					throw new RuntimeException("Failed to create log file in MediaStore.Files");
			} else if(logFileUri == null && weeWXApp.debug_filename.endsWith(".txt")) {
				ContentValues values = new ContentValues();
				values.put(MediaStore.Files.FileColumns.DISPLAY_NAME, weeWXApp.debug_filename);
				values.put(MediaStore.Files.FileColumns.MIME_TYPE, "text/plain");
				values.put(MediaStore.Files.FileColumns.RELATIVE_PATH, folderName);

				logFileUri = context.getContentResolver().insert(filesCollection, values);
				if(logFileUri == null)
				{
					String warning = "Failed to create log file in MediaStore.Files";
					Log.w(LOGTAG, warning);
					throw new RuntimeException(warning);
				}
			}
		}

		if(logFileUri == null)
		{
			String warning = "Failed to open debug file, skipping...";
			Log.w(LOGTAG, warning);
			throw new RuntimeException(warning);
		}

		// ================
		// 3. Append text to the log file
		// ================
		String timestamp = sdf13.format(System.currentTimeMillis());

		String tmpStr = timestamp + " " + levelToName(level) + ": " + text.strip() + "\n";

		try(OutputStream os = context.getContentResolver().openOutputStream(logFileUri, "wa"))
		{
			if(weeWXApp.debug_filename.endsWith(".gz"))
				os.write(gzipToBytes(tmpStr));
			else
				os.write(tmpStr.getBytes());
		} catch (IOException e) {
			doStackOutput(e);
		}
	}

	private static SharedPreferences getPrefSettings()
	{
		return weeWXApp.getInstance().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	}

	private static SharedPreferences getPrefSettings(Context context)
	{
		return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	}

	static void setVar(String var, Object val)
	{
		if(val == null && isPrefSet(var))
		{
			RemovePref(var);
			return;
		}

		if(val instanceof Boolean)
		{
			try
			{
				getPrefSettings().edit().putBoolean(var, (boolean)val).commit();
			} catch(Exception ignored) {}
		} else if(val instanceof Integer) {
			try
			{
				getPrefSettings().edit().putInt(var, (int)val).commit();
			} catch(Exception ignored) {}
		} else if(val instanceof Float) {
			try
			{
				getPrefSettings().edit().putFloat(var, (float)val).commit();
			} catch(Exception ignored) {}
		} else if(val instanceof Long) {
			try
			{
				getPrefSettings().edit().putLong(var, (long)val).commit();
			} catch(Exception ignored) {}
		} else if(val instanceof String) {
			try
			{
				getPrefSettings().edit().putString(var, (String)val).commit();
			} catch(Exception ignored) {}
		}
	}

	static Object readVar(String var, Object defVal)
	{
		if(var == null || var.isBlank())
			return null;

		if(!isPrefSet(var))
			return defVal;

		if(defVal == null)
			defVal = "";

		if(defVal instanceof Boolean)
		{
			try
			{
				return getPrefSettings().getBoolean(var, (boolean)defVal);
			} catch(Exception ignored) {}
		}

		if(defVal instanceof Integer)
		{
			try
			{
				return getPrefSettings().getInt(var, (int)defVal);
			} catch(Exception ignored) {}
		}

		if(defVal instanceof Float)
		{
			try
			{
				return getPrefSettings().getFloat(var, (float)defVal);
			} catch(Exception ignored) {}
		}

		if(defVal instanceof Long)
		{
			try
			{
				return getPrefSettings().getLong(var, (long)defVal);
			} catch(Exception ignored) {}
		}

		if(defVal instanceof String)
		{
			try
			{
				return getPrefSettings().getString(var, (String)defVal);
			} catch(Exception ignored) {}
		}

		return null;
	}

	static void RemovePref(String name)
	{
		SharedPreferences.Editor editor = getPrefSettings().edit();
		editor.putString(name, "");
		editor.remove(name);
		editor.apply();
	}

	static boolean isPrefSet(String name)
	{
		return getPrefSettings().contains(name);
	}

	static boolean fixTypes()
	{
		Context context = weeWXApp.getInstance();
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

			String s = ((String)value).strip();
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
		try
		{
			return getPrefSettings().getInt(name, default_value);
		} catch(ClassCastException e) {
			return (int)getPrefSettings().getLong(name, default_value);
		}
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

	static long getRSSms()
	{
		long rssTime = (long)KeyValue.readVar("rssCheck", 0L);
		while(rssTime < 10_000_000_000L)
			rssTime *= 1_000L;

		return rssTime;
	}

	static long getRSSsecs()
	{
		long rssTime = (long)KeyValue.readVar("rssCheck", 0L);
		while(rssTime > 10_000_000_000L)
			rssTime = Math.round(rssTime / 1_000D);

		return rssTime;
	}

	static long getLDms()
	{
		LogMessage("Checking for var named 'LastDownloadTime'");

		if(!isPrefSet("LastDownloadTime"))
			return 0L;

		long LastDownloadTime = (long)KeyValue.readVar("LastDownloadTime", 0L);

		LogMessage("LastDownloadTime: " + LastDownloadTime, true, KeyValue.d);

		while(LastDownloadTime < 10_000_000_000L)
			LastDownloadTime *= 1_000L;

		return LastDownloadTime;
	}

	static long getLDsecs()
	{
		LogMessage("Checking for var named 'LastDownloadTime'");

		if(!isPrefSet("LastDownloadTime"))
			return 0L;

		long LastDownloadTime = (long)KeyValue.readVar("LastDownloadTime", 0L);
		while(LastDownloadTime > 10_000_000_000L)
			LastDownloadTime = Math.round(LastDownloadTime / 1_000D);

		return LastDownloadTime;
	}

	static long getLFDms()
	{
		LogMessage("Checking for var named 'lastForecastDownloadTime'");

		if(!isPrefSet("lastForecastDownloadTime"))
			return 0L;

		long lastForecastDownloadTime = (long)KeyValue.readVar("lastForecastDownloadTime", 0L);
		while(lastForecastDownloadTime < 10_000_000_000L)
			lastForecastDownloadTime *= 1_000L;

		return lastForecastDownloadTime;
	}

	static long getLFDsecs()
	{
		LogMessage("Checking for var named 'lastForecastDownloadTime'");

		if(!isPrefSet("lastForecastDownloadTime"))
			return 0L;

		long lastForecastDownloadTime = (long)KeyValue.readVar("lastForecastDownloadTime", 0L);
		while(lastForecastDownloadTime > 10_000_000_000L)
			lastForecastDownloadTime = Math.round(lastForecastDownloadTime / 1_000D);

		return lastForecastDownloadTime;
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
		LogMessage("days: " + days);

		if(days.isEmpty())
		{
			LogMessage("generateForecast() Was sent an empty days variable, skipping...", true, KeyValue.w);
			return null;
		}

		StringBuilder sb = new StringBuilder();
		int start = 0;

		String string_time = sdf7.format(timestamp);

		sb.append("\n<div class='header'>").append(string_time).append("</div>\n\n");

		if(showHeader)
		{
			start = 1;
			Day first = getFirstDay(days);

			sb.append("<div class='today'>\n\t<div class='topRow'>\n");

			if(first.icon != null && !first.icon.isBlank())
			{
				sb.append("\t\t<div class='iconBig'>\n");

				if(!first.icon.startsWith("file:///") && !first.icon.startsWith("data:image"))
					sb.append("\t\t\t").append(cssToSVG(first.icon)).append("\n");
				else
					sb.append("\t\t\t<img alt='weather icon' src='").append(removeWS(first.icon)).append("' />\n");

				sb.append("\t\t</div>\n");
			} else {
				sb.append(weeWXApp.currentSpacer);
			}

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

			if(day.icon != null && !day.icon.isBlank())
			{
				sb.append("\t\t<div class='iconSmall'>\n");

				if(!day.icon.startsWith("file:///") && !day.icon.startsWith("data:image"))
					sb.append("\t\t\t").append(cssToSVG(day.icon)).append("\n");
				else
					sb.append("\t\t\t<img alt='weather icon' src='").append(removeWS(day.icon)).append("' />\n");

				sb.append("\t\t</div>\n");
			} else {
				sb.append(weeWXApp.currentSpacer);
			}

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

	static void updateCacheTime(long timestamp)
	{
		long last_update = 0;

		if(timestamp > 10_000_000_000L)
			last_update = Math.round(timestamp / 1_000D);

		long rssCheck = getRSSsecs();

		LogMessage("rssCheck: " + rssCheck);
		LogMessage("last_update: " + last_update);

		if(last_update > 0 && last_update != rssCheck)
			SetLongPref("rssCheck", last_update);
	}

	static String[] processBOM2(String data)
	{
		return processBOM2(data, false);
	}

	static String[] processBOM2(String data, boolean showHeader)
	{

		if(data.isBlank())
			return null;

		JsoupHelper.Result ret = JsoupHelper.processBoM2(data);

		if(ret == null || ret.days() == null)
			return null;

		return new String[]{generateForecast(ret.days(), ret.timestamp(), showHeader), ret.desc()};
	}

	static String[] processBOM3(String data)
	{
		return processBOM3(data, false);
	}

	static String[] processBOM3(String data, boolean showHeader)
	{
		String missing = null;

		LogMessage("Starting processBOM3()");

		if(data.isBlank())
		{
			LogMessage("processBOM3() data is blank, skipping...", true, KeyValue.w);
			return null;
		}

		LogMessage("data: " + data);

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		String desc = "";
		List<Day> days = new ArrayList<>();
		long timestamp = 0;

		try
		{
			JSONObject jobj = new JSONObject(data);

			desc = jobj.getJSONObject("metadata").getString("forecast_region");
			String tmp = jobj.getJSONObject("metadata").getString("issue_time");

			Date df = sdf1.parse(tmp);
			if(df != null)
				timestamp = df.getTime();

			if(timestamp > 0)
				updateCacheTime(timestamp);

//			timestamp = System.currentTimeMillis();
			Date date = new Date(timestamp);
			LogMessage("Last updated forecast: " + sdf3.format(date));

			JSONArray mydays = jobj.getJSONArray("data");
			for(int i = 0; i < mydays.length(); i++)
			{
				Day day = new Day();

				day.timestamp = 0;
				df = sdf1.parse(mydays.getJSONObject(i).getString("date"));
				if(df != null)
					day.timestamp = df.getTime();

				day.day = sdf2.format(day.timestamp);

				if(metric)
				{
					try
					{
						day.max = mydays.getJSONObject(i).getInt("temp_max") + "&deg;C";
					} catch(Exception ignored) {}

					try
					{
						day.min = mydays.getJSONObject(i).getInt("temp_min") + "&deg;C";
					} catch(Exception ignored) {}
				} else {
					try
					{
						day.max = Math.round(C2F(mydays.getJSONObject(i).getInt("temp_max"))) + "&deg;F";
					} catch(Exception ignored) {}

					try
					{
						day.min = Math.round(C2F(mydays.getJSONObject(i).getInt("temp_min"))) + "&deg;F";
					} catch(Exception ignored) {}
				}

				if(!mydays.getJSONObject(i).getString("extended_text").equals("null"))
					day.text = mydays.getJSONObject(i).getString("extended_text");

				String fileName = bomlookup(mydays.getJSONObject(i).getString("icon_descriptor"));
				if(fileName != null && !fileName.equals("null") && !fileName.isBlank())
				{
					day.icon = "icons/bom/" + fileName + ".svg";

					String content = weeWXApp.loadFileFromAssets(day.icon);
					if(content != null && !content.isBlank())
					{
						missing = null;
						day.icon = "file:///android_asset/" + day.icon;
					} else {
						missing = day.icon;
						day.icon = null;
					}
				}

				if(missing != null)
				{
					String base_url = "https://odiousapps.com/bom-missing-svg.php";

					String forecaseURL = (String)KeyValue.readVar("FORECAST_URL", null);

					weeWXAppCommon.LogMessage("Unable to locate SVG: " + missing, KeyValue.d);

					weeWXAppCommon.uploadString(base_url, Map.of(
							"svgMissingName", missing,
							"svgMissingURL", forecaseURL,
							"appName", com.odiousapps.weewxweather.BuildConfig.APPLICATION_ID,
							"appVersion", com.odiousapps.weewxweather.BuildConfig.VERSION_NAME));

				}

				days.add(day);
			}
		} catch(Exception e) {
			LogMessage("Error! e: " + e, true, KeyValue.e);
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

		String newicon = switch(icon)
		{
			case "shower" -> "showers";
			case "dusty" -> "dust";
			case "mostly_sunny" -> "partly_cloudy";
			case "light_shower" -> "light_showers";
			case "windy" -> "wind";
			default -> icon;
		};

		LogMessage("BoM Old Icon: " + icon, KeyValue.d);
		LogMessage("BoM New Icon: " + newicon, KeyValue.d);
		return newicon;
	}

	static String[] processMET(String data, boolean showHeader)
	{
		if(data == null || data.isBlank())
			return null;

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		long timestamp = getRSSms();

		String desc;
		List<Day> days = new ArrayList<>();

		desc = data.split("<title>", 2)[1].split(" weather - Met Office</title>",2)[0].strip();

		String[] forecasts = data.split("<ul id='dayNav'", 2)[1].split("</ul>", 2)[0].split("<li");
		for(int i = 1; i < forecasts.length; i++)
		{
			Day day = new Day();
			String date = forecasts[i].split("data-tab-id='", 2)[1].split("'")[0].strip();

			day.timestamp = 0;
			try
			{
				Date df = sdf4.parse(date);
				if(df != null)
					day.timestamp = df.getTime();
			} catch(Exception ignored) {}

			day.day = sdf2.format(day.timestamp);

			String icon = "https://beta.metoffice.gov.uk" + forecasts[i].split("<img class='icon'")[1].split("src='")[1].split("'>")[0].strip();
			StringBuilder fileName = new StringBuilder(icon.substring(icon.lastIndexOf('/') + 1).replaceAll("\\.svg$", ".png"));
			day.min = forecasts[i].split("<span class='tab-temp-low'", 2)[1].split("'>")[1].split("</span>")[0].strip();
			day.max = forecasts[i].split("<span class='tab-temp-high'", 2)[1].split("'>")[1].split("</span>")[0].strip();
			day.text = forecasts[i].split("<div class='summary-text", 2)[1].split("'>", 3)[2]
					.split("</div>", 2)[0].replaceAll("</span>", "").replaceAll("<span>", "");

			day.min = day.min.substring(0, day.min.length() - 5) + "&deg;C";
			day.max = day.max.substring(0, day.max.length() - 5) + "&deg;C";

			fileName.insert(0, "met");

			day.icon = "file:///android_asset/icons/met/" + fileName;

			if(!metric)
			{
				day.max = C2Fdeg((int)Float.parseFloat(day.max));
				day.min = C2Fdeg((int)Float.parseFloat(day.min));
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

		JsoupHelper.Result ret = JsoupHelper.processWCA(data);

		if(ret == null || ret.days() == null)
			return null;

		return new String[]{generateForecast(ret.days(), ret.timestamp(), showHeader), ret.desc()};
	}

	static String[] processWCAF(String data)
	{
		return processWCAF(data, false);
	}

	static String[] processWCAF(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		JsoupHelper.Result ret = JsoupHelper.processWCA(data);

		if(ret == null || ret.days() == null)
			return null;

		return new String[]{generateForecast(ret.days(), ret.timestamp(), showHeader), ret.desc()};
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

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		List<Day> days = new ArrayList<>();
		long timestamp;
		String desc;

		try
		{
			JSONObject jobj = new JSONObject(data);

			desc = jobj.getJSONObject("currentobservation").getString("name");
			String tmp = jobj.getString("creationDate");

			timestamp = 0;
			Date df = sdf1.parse(tmp);
			if(df != null)
				timestamp = df.getTime();

			if(timestamp > 0)
				updateCacheTime(timestamp);

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
					tmp = url.split("\\?", 2)[1].strip();
					String[] lines = tmp.split("&");
					for(String line : lines)
					{
						String trim_line = line.strip();
						String[] bits = trim_line.split("=", 2);
						if(bits[0].strip().equals("i"))
							fimg = "wgov" + bits[1].strip();
						if(bits[0].strip().equals("j"))
							simg = "wgov" + bits[1].strip();
						if(bits[0].strip().equals("ip"))
							fper = bits[1].strip();
						if(bits[0].strip().equals("jp"))
							sper = bits[1].strip();
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
								LogMessage("continue1, bmp is null", KeyValue.w);
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
								LogMessage("continue2, bmp is null", KeyValue.w);
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
					String fileName = "wgov" + iconLink.getString(i).substring(iconLink.getString(i).lastIndexOf("/") + 1).strip().replaceAll("\\.png$", ".jpg");

					day.icon = "file:///android_asset/icons/wgov/" + fileName;
				} else {
					day.icon = iconLink.getString(i);
				}

				day.timestamp = 0;
				df = sdf1.parse(validTime.getString(i));
				if(df != null)
					day.timestamp = df.getTime();

				day.day = periodName.getString(i);

				day.max = temperature.getString(i) + "&deg;F";

				if(metric)
					day.max = F2Cdeg((int)Float.parseFloat(temperature.getString(i)));

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

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		String desc;
		long timestamp;
		List<Day> days = new ArrayList<>();

		try
		{
			JSONObject jobj = new JSONObject(data);

			desc = jobj.getJSONObject("city").getString("cityName") + ", " + jobj.getJSONObject("city").getJSONObject("member").getString("memName");
			String tmp = jobj.getJSONObject("city").getJSONObject("forecast").getString("issueDate");


			timestamp = 0;
			Date df = sdf10.parse(tmp);
			if(df != null)
				timestamp = df.getTime();

			if(timestamp > 0)
				updateCacheTime(timestamp);

			JSONArray jarr = jobj.getJSONObject("city").getJSONObject("forecast").getJSONArray("forecastDay");
			for(int i = 0; i < jarr.length(); i++)
			{
				Day day = new Day();
				JSONObject j = jarr.getJSONObject(i);

				String date = j.getString("forecastDate").strip();

				day.timestamp = 0;
				df = sdf4.parse(date);
				if(df != null)
					day.timestamp = df.getTime();

				date = sdf2.format(day.timestamp);

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
					day.icon = cssToSVG("wi-wmo-" + code);
				else if(code.equals("28"))
					day.icon = fiToSVG("flaticon-cactus");
				else if(code.equals("29") || code.equals("30"))
					day.icon = fiToSVG("flaticon-thermometer");
				else if(code.equals("32"))
					day.icon = fiToSVG("flaticon-cold");
				else if(code.equals("33"))
					day.icon = fiToSVG("flaticon-warm");
				else if(code.equals("34"))
					day.icon = fiToSVG("flaticon-cool");

				day.day = date;
				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new String[]{generateForecast(days, timestamp, showHeader), desc};
	}

	static String[] processMetService(String data, boolean showHeader)
	{
		if(data == null || data.isBlank())
			return null;

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		List<Day> days = new ArrayList<>();
		String desc;
		long timestamp;
		long tmp_timestamp = 0;

		try
		{
			LogMessage("processMetService() data: " + data);
			JSONObject jobj = new JSONObject(data);
			JSONArray loop = jobj.getJSONArray("days");
			timestamp = loop.getJSONObject(0).getLong("issuedAtRaw");
			desc = loop.getJSONObject(0).getJSONObject("riseSet").getString("location") + ", New Zealand";

			if(timestamp > 0)
				updateCacheTime(timestamp);

			for(int i = 0; i < loop.length(); i++)
			{
				Day day = new Day();
				JSONObject jtmp = loop.getJSONObject(i);

				Date df = sdf1.parse(jtmp.getString("dateISO"));
				if(df != null)
					tmp_timestamp = df.getTime();

				day.timestamp = tmp_timestamp;
				day.day = jtmp.getString("dow");
				day.text = jtmp.getString("forecast");
				day.max = jtmp.getString("max") + "&deg;C";
				day.min = jtmp.getString("min") + "&deg;C";
				if(jtmp.has("partDayData"))
					day.icon = jtmp.getJSONObject("partDayData").getJSONObject("afternoon").getString("forecastWord");
				else
					day.icon = jtmp.getString("forecastWord");

				day.icon = day.icon.toLowerCase(Locale.ENGLISH).strip()
						.replaceAll("[ -]", "_");

				if(day.icon.equals("wind_and_rain"))
					day.icon = "wind_rain";

				if(day.icon.equals("windy"))
					day.icon = "wind";

				day.icon = "icons/metservice/condition_" + day.icon + ".svg";

				String content = weeWXApp.loadFileFromAssets(day.icon);
				if(content != null && !content.isBlank())
					day.icon = "file:///android_asset/" + day.icon;
				else
					day.icon = null;

				LogMessage("day.icon: " + day.icon);

				if(!metric)
				{
					day.max = C2Fdeg((int)Float.parseFloat(day.max));
					day.min = C2Fdeg((int)Float.parseFloat(day.min));
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

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		List<Day> days = new ArrayList<>();
		String desc = "";
		long timestamp = 0;
		long lastTS = 0;

		try
		{
			String[] bits = data.split("<title>");
			if(bits.length >= 2)
				desc = bits[1].split("</title>")[0];
			desc = desc.substring(desc.lastIndexOf(" - ") + 3).strip();
			String string_time = data.split("<tr class='headRow'>", 2)[1].split("</tr>", 2)[0].strip();
			String date = string_time.split("<td width='30%' class='stattime'>", 2)[1].split("</td>", 2)[0].strip();
			string_time = date + " " + string_time.split("<td width='40%' class='stattime'>", 2)[1].split(" Uhr</td>", 2)[0].strip();

			Date df = sdf11.parse(string_time);
			if(df != null)
				lastTS = timestamp = df.getTime();

			if(timestamp > 0)
				updateCacheTime(timestamp);

			data = data.split("<td width='40%' class='statwert'>Vorhersage</td>", 2)[1].split("</table>", 2)[0].strip();
			bits = data.split("<tr");
			for(int i = 1; i < bits.length; i++)
			{
				Day day = new Day();
				String bit = bits[i];
				String icon, temp;
				if(bit.split("<td ><b>", 2).length > 1)
					day.day = bit.split("<td ><b>", 2)[1].split("</b></td>", 2)[0].strip();
				else
					day.day = bit.split("<td><b>", 2)[1].split("</b></td>", 2)[0].strip();

				Locale locale = new Locale.Builder().setLanguage("de").setRegion("DE").build();
				day.timestamp = convertDaytoTS(day.day, locale, lastTS);
				if(day.timestamp != 0)
				{
					day.day = sdf2.format(day.timestamp) + " " + day.day.substring(day.day.lastIndexOf(" ") + 1);
				}

				if(bit.split("<td ><img name='piktogramm' src='", 2).length > 1)
					icon = bit.split("<td ><img name='piktogramm' src='", 2)[1].split("' width='50' alt='", 2)[0].strip();
				else
					icon = bit.split("<td><img name='piktogramm' src='", 2)[1].split("' width='50' alt='", 2)[0].strip();

				if(bit.split("'></td>\r\n<td >", 2).length > 1)
					temp = bit.split("'></td>\r\n<td >", 2)[1].split("Grad <abbr title='Celsius'>C</abbr></td>\r\n", 2)[0].strip();
				else
					temp = bit.split("'></td>\r\n<td>", 2)[1].split("Grad <abbr title='Celsius'>C</abbr></td>\r\n", 2)[0].strip();

				icon = icon.replaceAll("/DE/wetter/_functions/piktos/vhs_", "").replaceAll("\\?__blob=normal", "").strip();

				String fileName = "dwd_" + icon.replaceAll("-", "_");
				String url = "https://www.dwd.de/DE/wetter/_functions/piktos/" + icon + "?__blob=normal";

				day.icon = "file:///android_asset/icons/dwd/" + fileName;

				day.max = temp + "&deg;C";
				day.min = "&deg;C";
				if(!metric)
					day.max = C2Fdeg((int)Float.parseFloat(temp));

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

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		List<Day> days = new ArrayList<>();
		String desc;
		long timestamp = 0;
		long lastTS = 0;

		try
		{
			String stuff = data.split("<div id='weatherDayNavigator'>", 2)[1].strip();
			stuff = stuff.split("<h2>", 2)[1].strip();
			desc = stuff.split(" <span class='day'>")[0].strip();
			String string_time = sdf7.format(System.currentTimeMillis());
			Date df = sdf7.parse(string_time);
			if(df != null)
				lastTS = timestamp = df.getTime();

			if(timestamp > 0)
				updateCacheTime(timestamp);

			data = data.split("<tbody>")[1].strip();
			String[] bits = data.split("<tr>");
			for(int i = 1; i < bits.length; i++)
			{
				Day day = new Day();
				String bit = bits[i].strip();
				day.day = bit.split("<td class='timeweek'>")[1].split("'>")[1].split("</a></td>", 2)[0].strip();

				Locale locale = new Locale.Builder().setLanguage("it").setRegion("IT").build();
				day.timestamp = convertDaytoTS(day.day, locale, lastTS);
				if(day.timestamp != 0)
					day.day = sdf2.format(day.timestamp);

				String url = bit.split("<td class='skyIcon'><img src='", 2)[1].split("' alt='",2)[0].strip();
				String icon = new File(url).getName();

				String fileName = cssToSVG("wi-tempoitalia-" + icon);

				day.icon = "file:///android_asset/icons/tempoitalia/" + fileName;

				day.max = bit.split("<td class='tempmax'>", 2)[1].split("°C</td>", 2)[0].strip() + "&deg;C";
				day.min = bit.split("<td class='tempmin'>", 2)[1].split("°C</td>", 2)[0].strip() + "&deg;C";

				day.text = bit.split("<td class='skyDesc'>")[1].split("</td>")[0].strip();

				LogMessage("day.icon=" + day.icon);

				if(metric)
				{
					day.max = C2Fdeg((int)Float.parseFloat(day.max));
					day.min = C2Fdeg((int)Float.parseFloat(day.min));
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

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
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

			Date df = sdf12.parse(elaborado);
			if(df != null)
				timestamp = df.getTime();

			if(timestamp > 0)
				updateCacheTime(timestamp);

			JSONArray dates = jobj.getJSONObject("prediccion").getJSONArray("dia");
			for(int i = 0; i < dates.length(); i++)
			{
				Day day = new Day();
				JSONObject jtmp = dates.getJSONObject(i);
				String fecha = jtmp.getString("fecha");

				day.timestamp = 0;
				df = sdf4.parse(fecha);
				if(df != null)
					day.timestamp = df.getTime();

				day.day = sdf2.format(day.timestamp);

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
				String url = "https://www.aemet.es/imagenes/png/estado_cielo/" + code + "_g.png";
				String fileName = "aemet_" + code + "_g.png";

				day.icon = "file:///android_asset/icons/aemet/" + fileName;

				day.max = temperatura.getString("maxima") + "&deg;C";
				day.min = temperatura.getString("minima") + "&deg;C";

				if(!metric)
				{
					day.max = C2Fdeg((int)Float.parseFloat(day.max));
					day.min = C2Fdeg((int)Float.parseFloat(day.min));
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

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		long timestamp = getRSSms();
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

				String fileName = day.icon + ".png";

				day.icon = "file:///android_asset/icons/wcom/" + fileName;

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

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		long timestamp = getRSSms();
		List<Day> days = new ArrayList<>();
		String desc;

		try
		{
			desc = (String)KeyValue.readVar("metierev", weeWXApp.metierev_default);

			JSONArray jarr = new JSONArray(data);
			for(int i = 0; i < jarr.length(); i++)
			{
				JSONObject jobj = jarr.getJSONObject(i);
				Day day = new Day();
				String tmpDay = jobj.getString("date") + " " + jobj.getString("time");
				day.timestamp = 0;
				Date df = sdf7.parse(tmpDay);
				if(df != null)
					day.timestamp = df.getTime();

				day.day = sdf2.format(day.timestamp);
				day.max = jobj.getString("temperature") + "&deg;C";
				day.icon = jobj.getString("weatherNumber");

				String fileName = "y" + day.icon + ".png";

				day.icon = "file:///android_asset/icons/metie/" + fileName;

				day.text = jobj.getString("weatherDescription");

				if(!metric)
					day.max = C2Fdeg((int)Float.parseFloat(day.max));

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

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		long timestamp = getRSSms();

		try
		{
			JSONObject jobj = new JSONObject(data);
			desc = jobj.getJSONObject("city").getString("name") + ", " + jobj.getJSONObject("city").getString("country");
			JSONArray jarr = jobj.getJSONArray("list");
			for(int i = 0; i < jarr.length(); i++)
			{
				Day day = new Day();
				JSONObject j = jarr.getJSONObject(i);
				day.timestamp = j.getLong("dt") * 1_000L;
				day.day = sdf7.format(day.timestamp);

				JSONObject temp = j.getJSONObject("temp");
				int min = (int)Math.round(Double.parseDouble(temp.getString("min")));
				int max = (int)Math.round(Double.parseDouble(temp.getString("max")));
				JSONObject weather = j.getJSONArray("weather").getJSONObject(0);

				int id = weather.getInt("id");
				String text = weather.getString("description");
				String icon = weather.getString("icon");

				if(!icon.endsWith("n"))
					day.icon = cssToSVG("wi-owm-day-" + id);
				else
					day.icon = cssToSVG("wi-owm-night-" + id);

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

	static String[] processMetNO(String data)
	{
		return processMetNO(data, false);
	}

	static String[] processMetNO(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		long timestamp = 0;
		List<Day> days = new ArrayList<>();

		Calendar cal = Calendar.getInstance();

		String desc = (String)KeyValue.readVar("forecastLocationName", null);
		if(desc == null || desc.isBlank())
			desc = "";

		try
		{
			JSONObject jobj = new JSONObject(data);
			if(jobj == null)
				return null;

			jobj = jobj.getJSONObject("properties");

			//CustomDebug.writeDebug("weeWX", "processMetNO.json", jobj.toString(4));

			JSONObject meta = jobj.getJSONObject("meta");
			JSONObject units = meta.getJSONObject("units");
			String updated_at = meta.getString("updated_at");
			JSONArray timeseries = jobj.getJSONArray("timeseries");

			Date df = sdf1.parse(updated_at);
			if(df != null)
			{
				timestamp = df.getTime();
				if(timestamp > 0)
					updateCacheTime(timestamp);
			}

			int count = 0;
			for(int i = 0; i < timeseries.length(); i++)
			{
				Day day = new Day();

				JSONObject jobj2 = timeseries.getJSONObject(i);

//				df = sdf1.parse(jobj2.getString("time"));
//				cal.setTime(df);
//				if(cal.get(Calendar.HOUR_OF_DAY) != 0)
//					continue;

				if(i != 0 && !jobj2.getString("time").endsWith("T00:00:00Z"))
					continue;

				JSONObject tsdata = jobj2.getJSONObject("data");
				JSONObject details;

				String icon = "";

				if(tsdata.has("next_12_hours"))
				{
					icon = tsdata.getJSONObject("next_12_hours")
							.getJSONObject("summary")
							.getString("symbol_code");
				} else if(tsdata.has("next_6_hours")) {
					icon = tsdata.getJSONObject("next_6_hours")
							.getJSONObject("summary")
							.getString("symbol_code");
				}

				if(tsdata.has("next_6_hours"))
				{
					details = tsdata.getJSONObject("next_6_hours")
							.getJSONObject("details");

					if(details.has("air_temperature_max"))
					{
						day.max = details.getDouble("air_temperature_max") + "&deg;C";
						if(!metric)
							day.max = C2Fdeg(round(Float.parseFloat(day.max), 1));
					} else {
						CustomDebug.writeDebug("weeWX", "processMetNO" + count++ + ".json", details.toString(4));
					}

					try
					{
						double precip = details.getDouble("precipitation_amount");
						day.min = precip + "mm";
						if(!metric)
							day.min = round(precip / 25.4, 1) + "in";
					} catch(Exception ignored) {}
				}

				JSONObject details2 = tsdata.getJSONObject("instant")
						.getJSONObject("details");
				if(details2.has("wind_speed"))
				{
					double windSpeed = details2.getDouble("wind_speed");
					double windDir = details2.getDouble("wind_from_direction");

					if(metric)
						day.text = Math.round(windSpeed * 3.6) + "kph from the " + degtoname(windDir);
					else
						day.text = Math.round(windSpeed * 2.236936) + "mph from the " + degtoname(windDir);
				}

				day.timestamp = 0;
				df = sdf1.parse(jobj2.getString("time"));
				if(df != null)
				{
					day.timestamp = df.getTime();
					day.day = sdf2.format(day.timestamp);
				} else {
					day.day = sdf2.format(System.currentTimeMillis());
				}

				day.icon = "icons/metno/" + icon + ".svg";
				String content = weeWXApp.loadFileFromAssets(day.icon);
				if(content != null && !content.isBlank())
				{
					day.icon = "file:///android_asset/" + day.icon;
					//CustomDebug.writeDebug("weeWX", "processMetNO" + count++ + ".json", jobj2.toString(4));
				} else {
					day.icon = null;
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

	static long convertDaytoTS(String dayName, Locale locale, long lastTS)
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

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
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

			Date df = sdf6.parse(pubDate);
			if(df != null)
				lastTS = timestamp = df.getTime();

			JSONObject item = jobj.getJSONArray("item").getJSONObject(0);
			String[] items = item.getString("description").strip().split("<b>");
			for (String i : items)
			{
				Day day = new Day();
				String[] tmp = i.split("</b>", 2);
				Locale locale = new Locale.Builder().setLanguage("en").setRegion("AU").build();
				day.timestamp = convertDaytoTS(day.day, locale, lastTS);
				if(day.timestamp != 0)
					day.day = sdf2.format(day.timestamp);
				else
					day.day = tmp[0];

				if(tmp.length == 1)
					continue;

				String[] mybits = tmp[1].split("<br />");
				String myimg = mybits[1].strip().replaceAll("<img src='https://www.weatherzone.com.au/images/icons/fcast_30/", "")
									.replaceAll("'>", "").replaceAll(".gif", "").replaceAll("_", "-").strip();
				String mydesc = mybits[2].strip();
				String[] range = mybits[3].split(" - ", 2);

				String fileName = myimg.replaceAll("-", "_");

				fileName = "wz" + fileName.substring(fileName.lastIndexOf('/') + 1, fileName.length() - 2) + ".png";

				day.icon = "file:///android_asset/icons/wz/" + fileName;

				LogMessage("WZ URI: " + fileName);

				day.max = range[1];
				day.min = range[0];

				if(!metric)
				{
					day.max = C2Fdeg((int)Float.parseFloat(day.max));
					day.min = C2Fdeg((int)Float.parseFloat(day.min));
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

	static String convertRGB2Hex(String svg)
	{
		// rgb(var(--uds-spectrum-color-purple-4))
		for(KeyValue.KV kv : KeyValue.yahoo)
		{
			LogMessage("Checking for rgb(var(" + kv.Key + ")) -> " + kv.Val);
			svg = svg.replaceAll("rgb\\(var\\(" + kv.Key + "\\)\\)", kv.Val);
		}

		Pattern p = Pattern.compile("rgb\\(var\\((.*?)\\)\\)");
		Matcher m = p.matcher(svg);
		while(m.find())
			LogMessage("Found unconverted RGB in SVG: " + m.group(1), KeyValue.d);

		return svg;
	}

	static String[] processYahoo(String data)
	{
		return processYahoo(data, false);
	}

	static String[] processYahoo(String data, boolean showHeader)
	{
		if(data.isBlank())
			return null;

		JsoupHelper.Result ret = JsoupHelper.processBoM2(data);

		if(ret == null || ret.days() == null)
			return null;

		return new String[]{generateForecast(ret.days(), ret.timestamp(), showHeader), ret.desc()};
	}

	static void SendIntent(String action)
	{
//		if(action.equals(REFRESH_WEATHER_INTENT) && lastWeatherRefresh + 5 > getCurrTime())
//				return;

//		if(action.equals(REFRESH_WEATHER_INTENT))
//			lastWeatherRefresh = getCurrTime();

		NotificationManager.updateNotificationMessage(action);
	}

	static boolean getWeather(boolean forced, boolean calledFromweeWXApp)
	{
		long current_time = getCurrTime();

		String baseURL = (String)KeyValue.readVar("BASE_URL", weeWXApp.BASE_URL_default);
		if(baseURL == null || baseURL.isBlank())
		{
			LogMessage("baseURL == null || baseURL.isBlank()...", KeyValue.d);
			KeyValue.putVar("LastWeatherError", weeWXApp.getAndroidString(R.string.data_url_was_blank));
			return false;
		}

		String lastDownload = (String)KeyValue.readVar("LastDownload", "");
		if(!forced && !checkConnection())
		{
			if(lastDownload == null || lastDownload.isBlank())
			{
				LogMessage("lastDownload is null or blank...", KeyValue.d);
				KeyValue.putVar("LastWeatherError", weeWXApp.getAndroidString(R.string.wifi_not_available));
				return false;
			} else {
				LogMessage("Not forced and WiFi needed but not available", KeyValue.d);
				return true;
			}
		}

		int pos = (int)KeyValue.readVar("updateInterval", weeWXApp.updateInterval_default);
		LogMessage("getWeather() pos: " + pos + ", update interval set to: " +
		           weeWXApp.updateOptions[pos] + ", forced set to: " + forced);

		if(pos < 0)
		{
			LogMessage("Invalid update frequency...", KeyValue.d);
			KeyValue.putVar("LastWeatherError", weeWXApp.getAndroidString(R.string.invalid_update_interval));
			return false;
		}

		if(!forced && pos == 0)
		{
			if(lastDownload == null || lastDownload.isBlank())
			{
				LogMessage("lastDownload is null or blank...", KeyValue.d);
				KeyValue.putVar("LastWeatherError", weeWXApp.getAndroidString(R.string.update_set_to_manual_but_no_content_cached));
				return false;
			} else {
				LogMessage("Not forced and set to manual updates...");
				return true;
			}
		}

		long[] npwsll = getNPWSLL();
		if(!forced && npwsll[1] <= 0)
		{
			LogMessage("weeWXCommon.java Skipping, period is invalid or set to manual refresh only...", KeyValue.d);

			if(lastDownload == null || lastDownload.isBlank())
			{
				KeyValue.putVar("LastWeatherError", weeWXApp.getAndroidString(R.string.update_set_to_manual_but_no_content_cached));
				return false;
			}

			return true;
		}

		if(!forced && npwsll[5] == 0)
		{
			LogMessage("weeWXCommon.java Skipping, lastDownloadTime == 0, app hasn't been setup...", KeyValue.d);

			if(lastDownload == null || lastDownload.isBlank())
			{
				KeyValue.putVar("LastWeatherError", weeWXApp.getAndroidString(R.string.no_download_or_app_not_setup));
				return false;
			}

			return true;
		}

		if(!forced && Math.round((npwsll[5] + npwsll[1]) / 1_000D) > current_time)
		{
			LogMessage("!forced && " + Math.round((npwsll[5] + npwsll[1]) / 1_000D) + " > " + current_time + "...");
			if(lastDownload != null && !lastDownload.isBlank())
			{
				LogMessage("lastDownload != null && !lastDownload.isBlank()... Skipping...", KeyValue.d);
				return true;
			}
		}

		if(!forced && !weeWXApp.hasBootedFully && !calledFromweeWXApp)
		{
			LogMessage("Hasn't booted fully and wasn't called by weeWXApp and wasn't forced, skipping...", KeyValue.d);

			if(lastDownload != null && !lastDownload.isBlank())
			{
				KeyValue.putVar("LastWeatherError", lastDownload);
				return true;
			}

			KeyValue.putVar("LastWeatherError", weeWXApp.getAndroidString(R.string.attempting_to_download_data_txt));
			return false;
		}

		if(weatherTask != null && !weatherTask.isDone())
		{
			if(wtStart + 30 > current_time)
			{
				LogMessage("weatherTask is less than 30s old (" + (current_time - wtStart) + "s), we'll skip this attempt...", KeyValue.d);
				return true;
			}

			LogMessage("weatherTask was more than 30s old (" + (current_time - wtStart) + "s) cancelling and restarting...", KeyValue.d);

			weatherTask.cancel(true);
			weatherTask = null;
		}

		LogMessage("Creating a weatherTask...");

		wtStart = current_time;

		weatherTask = executor.submit(() ->
		{
			LogMessage("Weather checking: " + baseURL);

			try
			{
				if(reallyGetWeather(baseURL))
				{
					LogMessage("Update the widget");
					WidgetProvider.updateAppWidget();

					LogMessage("weatherTask.SendIntent(REFRESH_WEATHER_INTENT)");
					SendIntent(REFRESH_WEATHER_INTENT);
					wtStart = 0;
					return;
				}
			} catch(Exception e) {
				LogMessage("getWeather() Error! e: " + e, true, KeyValue.e);
			}

			LogMessage("weatherTask.SendIntent(STOP_WEATHER_INTENT)");
			SendIntent(STOP_WEATHER_INTENT);
			wtStart = 0;
		});

		return true;
	}

	static String getElement(String[] bits, int element)
	{
		try
		{
			if(bits.length > element)
				return bits[element].strip();
		} catch(Exception e) {
			weeWXAppCommon.LogMessage("Error! e: " + e, true, KeyValue.e);
		}

		return "";
	}

	static boolean reallyGetWeather(String url) throws InterruptedException, IOException
	{
		LogMessage("reallyGetWeather() url: " + url);
		String line = downloadString(url);
		LogMessage("reallyGetWeather() Got output, line: " + line);
		if(!line.isBlank() && line.contains("|"))
		{
			String[] bits = line.split("\\|");
			int version = (int)Float.parseFloat(bits[0]);
			if(version < weeWXApp.minimum_inigo_version || bits.length <= 100)
			{
				sendAlert();
				return false;
			}

			bits = Arrays.copyOfRange(bits, 1, bits.length);
			line = String.join("|", bits);
			//LogMessage("reallyGetWeather() new line: " + line);

			long LastDownloadTime = Math.round(System.currentTimeMillis() / 1_000D);
			String ret = getElement(bits, 225);
			if(ret != null && !ret.isBlank())
				LastDownloadTime = (long)Double.parseDouble(bits[225]);

			KeyValue.putVar("LastDownload", line);
			KeyValue.putVar("LastDownloadTime", LastDownloadTime);
			KeyValue.putVar("LastWeatherError", null);

			LogMessage("Last Server Update Time: " + sdf14.format(LastDownloadTime * 1_000L));
			LogMessage("LastDownloadTime: " + sdf14.format(System.currentTimeMillis()));

			return true;
		}

		return false;
	}

	//    https://stackoverflow.com/questions/3841317/how-do-i-see-if-wi-fi-is-connected-on-android
	static boolean checkConnection()
	{
		if(!(boolean)KeyValue.readVar("onlyWIFI", weeWXApp.onlyWIFI_default))
			return true;

		ConnectivityManager connMgr = (ConnectivityManager)weeWXApp.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
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
		LogMessage("Send user note about upgrading the Inigo Plugin", KeyValue.d);
	}

	// https://stackoverflow.com/questions/8710515/reading-an-image-file-into-bitmap-from-sdcard-why-am-i-getting-a-nullpointerexc

	private static Bitmap combineImage(Bitmap bmp1, String fnum, String snum)
	{
		Context context = weeWXApp.getInstance();

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
		Context context = weeWXApp.getInstance();

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
			switch (bits[1].strip())
			{
				case "L" -> bmp1 = Bitmap.createBitmap(bmp1, 0, 0, x1 / 2, y1);
				case "R" -> bmp1 = Bitmap.createBitmap(bmp1, x1 / 2, 0, x1, y1);
				default -> bmp1 = Bitmap.createBitmap(bmp1, x1 / 4, 0, x1 * 3 / 4, y1);
			}

			bits = nws.getString(simg).split("\\|");
			switch (bits[1].strip())
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
		paint.setColor(ContextCompat.getColor(weeWXApp.getInstance(), R.color.LightPrussianBlue));
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

	static String downloadSettings(String url) throws InterruptedException, IOException
	{
		KeyValue.putVar("SETTINGS_URL", url);

		String cfg = downloadString(url);

		if(cfg == null)
			return null;

		if(cfg.startsWith(UTF8_BOM))
			cfg = cfg.substring(1).strip();

		return cfg;
	}

	static boolean checkURL(String url) throws InterruptedException, IOException
	{
		if(url == null || url.isBlank())
			return false;

		OkHttpClient client = NetworkClient.getInstance(url);

		return reallyCheckURL(client, url, 0);
	}

	private static boolean reallyCheckURL(OkHttpClient client, String url, int retries) throws InterruptedException, IOException
	{
		LogMessage("reallyCheckURL() checking if url  " + url + " is valid, attempt " + (retries + 1));

		Request request = NetworkClient.getRequest(true, url);

		try(Response response = client.newCall(request).execute())
		{
			return response.isSuccessful();
		} catch(Exception e) {
			if(retries < maximum_retries)
			{
				retries++;

				LogMessage("reallyCheckURL() Error! e: " + e.getMessage() + ", retry: " + retries +
				           ", will sleep " + retry_sleep_time + " seconds and retry...", true);

				try
				{
					Thread.sleep(retry_sleep_time);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					LogMessage("reallyCheckURL() Error! ie: " + ie.getMessage(), true, KeyValue.e);

					throw ie;
				}

				return reallyCheckURL(client, url, retries);
			}

			LogMessage("reallyCheckURL() Error! e: " + e.getMessage(), true, KeyValue.e);
			throw e;
		}
	}

	static String downloadString(String url) throws InterruptedException, IOException
	{
		OkHttpClient client = NetworkClient.getInstance(url);

		return reallyDownloadString(client, url, 0);
	}

	private static String reallyDownloadString(OkHttpClient client, String url, int retries) throws InterruptedException, IOException
	{
		LogMessage("reallyDownloadString() checking if url  " + url + " is valid, attempt " + (retries + 1));
		Request request = NetworkClient.getRequest(false, url);

		try(Response response = client.newCall(request).execute())
		{
			String bodyStr = response.body().string();

			if(!response.isSuccessful())
			{
				String error = "HTTP error " + response;
				if(bodyStr != null && !bodyStr.isBlank())
					error += ", body: " + bodyStr;
				LogMessage("reallyDownloadString() Error! error: " + error, true, KeyValue.w);
				throw new IOException(error);
			}

			LogMessage("response: " + response);
			LogMessage("Returned string: " + bodyStr);
			return bodyStr;
		} catch(Exception e) {
			if(retries < maximum_retries)
			{
				retries++;

				LogMessage("reallyDownloadString() Error! e: " + e.getMessage() + ", retry: " + retries +
				           ", will sleep " + retry_sleep_time + " seconds and retry...", true);

				try
				{
					Thread.sleep(retry_sleep_time);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					LogMessage("reallyDownloadString() Error! ie: " + ie.getMessage(), true, KeyValue.e);
					throw ie;
				}

				return reallyDownloadString(client, url, retries);
			}

			LogMessage("reallyDownloadString() Error! e: " + e.getMessage(), true, KeyValue.e);
			throw e;
		}
	}

	static String downloadString(String url, Map<String, String> args) throws InterruptedException, IOException
	{
		if(url == null || url.isBlank() || args == null || args.isEmpty())
		{
			LogMessage("downloadString(url, args) Attempted uploading nothing, skipping...", true, KeyValue.d);
			return null;
		}

		FormBody.Builder fb = new FormBody.Builder();

		for(Map.Entry<String, String> arg: args.entrySet())
			fb.add(arg.getKey(), arg.getValue());

		RequestBody requestBody = fb.build();

		OkHttpClient client = NetworkClient.getInstance(url);

		return reallyDownloadString(client, requestBody, url, 0);
	}

	private static String reallyDownloadString(OkHttpClient client, RequestBody requestBody,
                       String url, int retries) throws InterruptedException, IOException
	{
		LogMessage("reallyDownloadString() checking if url  " + url + " is valid, attempt " + (retries + 1));

		Request request = NetworkClient.getRequest(false, url)
				.newBuilder().post(requestBody).build();

		try(Response response = client.newCall(request).execute())
		{
			String bodyStr = response.body().string();
			if(response.isSuccessful())
			{
				LogMessage("reallyDownloadString(url, args) Successfully uploaded something... response: " + bodyStr);
				return bodyStr;
			} else {
				LogMessage("reallyDownloadString(url, args) Failed to upload something... response: " + bodyStr, true, KeyValue.w);
			}
		} catch(Exception e) {
			if(retries < maximum_retries)
			{
				retries++;

				LogMessage("reallyDownloadString() Error! e: " + e.getMessage() + ", retry: " + retries +
				           ", will sleep " + retry_sleep_time + " seconds and retry...", true);

				try
				{
					Thread.sleep(retry_sleep_time);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw ie;
				}

				return reallyDownloadString(client, requestBody, url, retries);
			}

			LogMessage("reallyDownloadString(url, args) Error! e: " + e.getMessage(), true, KeyValue.e);
			throw e;
		}

		return null;
	}

	static void uploadString(String url, Map<String,String> args)
	{
		if(url == null || url.isBlank() || args == null || args.isEmpty())
		{
			LogMessage("uploadString() Attempted uploading nothing, skipping...", KeyValue.d);
			return;
		}

		executor.submit(() ->
		{
			FormBody.Builder fb = new FormBody.Builder();

			for(Map.Entry<String, String> arg: args.entrySet())
				fb.add(arg.getKey(), arg.getValue());

			RequestBody requestBody = fb.build();

			OkHttpClient client = NetworkClient.getInstance(url);

			reallyUploadString(client, requestBody, url, 0);
		});
	}

	private static void reallyUploadString(OkHttpClient client, RequestBody requestBody, String url, int retries)
	{
		LogMessage("reallyUploadString() checking if url  " + url + " is valid, attempt " + (retries + 1));

		Request request = NetworkClient.getRequest(false, url)
				.newBuilder().post(requestBody).build();

		try(Response response = client.newCall(request).execute())
		{
			String bodyStr = response.body().string();
			if(response.isSuccessful())
				LogMessage("reallyUploadString() Successfully uploaded something... response: " + bodyStr);
			else
				LogMessage("reallyUploadString() Failed to upload something... response: " + bodyStr, true, KeyValue.d);
		} catch(Exception e) {
			if(retries < maximum_retries)
			{
				retries++;

				LogMessage("reallyUploadString() Error! e: " + e.getMessage() + ", retry: " + retries +
				           ", will sleep " + retry_sleep_time + " seconds and retry...", true);

				try
				{
					Thread.sleep(retry_sleep_time);
				} catch (InterruptedException ie) {
					LogMessage("reallyUploadString() Error! ie: " + ie.getMessage(), true, KeyValue.e);
					Thread.currentThread().interrupt();
					return;
				}

				reallyUploadString(client, requestBody, url, retries);
				return;
			}

			LogMessage("reallyUploadString() Error! e: " + e.getMessage(), true, KeyValue.e);
			doStackOutput(e);
		}
	}

	static void publish(File f)
	{
		if(f.exists())
		{
			LogMessage("Let's tell Android about: " + f.getAbsolutePath());

			MediaScannerConnection.scanFile(weeWXApp.getInstance(),
					new String[]{f.getAbsolutePath()}, null, null);
		} else
			LogMessage(f.getAbsolutePath() + " doesn't exist, skipping...");
	}

	static boolean checkForImage(String filename)
	{
		try
		{
			File dir = getDataDir();
			File f = new File(dir, filename);
			return f.exists() && f.isFile() && f.canRead();
		} catch(Exception e) {
			LogMessage("Error! e: " + e, true, KeyValue.e);
		}

		return false;
	}

	static String nodeToString(Node node) throws TransformerException
	{
		Transformer tf = TransformerFactory.newInstance().newTransformer();

		tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		tf.setOutputProperty(OutputKeys.INDENT, "no");

		StringWriter writer = new StringWriter();
		tf.transform(new DOMSource(node), new StreamResult(writer));

		return writer.toString();
	}

	static Element symbolToSvg(Document doc, Node symbolNode)
	{
		Element symbol = (Element)symbolNode;

		// 1. Create new <svg> element in SVG namespace
		Element svg = doc.createElementNS("http://www.w3.org/2000/svg", "svg");

		// 2. Copy attributes (except id, optional)
		NamedNodeMap attrs = symbol.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			Attr attr = (Attr) attrs.item(i);
			if(!"id".equals(attr.getName()))
				svg.setAttribute(attr.getName(), attr.getValue());
		}

		// 3. Move all children
		while(symbol.hasChildNodes())
			svg.appendChild(symbol.getFirstChild());

		return svg;
	}

	static boolean getForecast(boolean forced, boolean calledFromweeWXApp)
	{
		String fctype = (String)KeyValue.readVar("fctype", "");
		if(fctype == null || fctype.isBlank())
		{
			LogMessage("fctype == null || fctype.isBlank(), skipping...", KeyValue.d);
			KeyValue.putVar("LastForecastError", String.format(
					weeWXApp.getAndroidString(R.string.forecast_type_is_invalid), fctype));
			return false;
		}

		LogMessage("getForecast() fctype: " + fctype);

		String forecast_url = (String)KeyValue.readVar("FORECAST_URL", weeWXApp.FORECAST_URL_default);
		if(forecast_url == null || forecast_url.isBlank())
		{
			KeyValue.putVar("LastForecastError", weeWXApp.getAndroidString(R.string.forecast_url_not_set));
			return false;
		}

		if(!checkConnection() && !forced)
		{
			LogMessage("Not on wifi and not a forced refresh", KeyValue.d);
			String forecastData = (String)KeyValue.readVar("forecastData", "");
			if(forecastData == null || forecastData.isBlank())
			{
				KeyValue.putVar("LastForecastError", weeWXApp.getAndroidString(R.string.wifi_not_available));
				return false;
			}

			LogMessage("forecastData != null && !forecastData.isBlank()");
			return true;
		}

		int pos = (int)KeyValue.readVar("updateInterval", weeWXApp.updateInterval_default);
		LogMessage("getForecast() pos: " + pos + ", update interval set to: " +
		           weeWXApp.RSSCache_period_default + "s, forced set to: " + forced);
		if(pos < 0)
		{
			LogMessage("Invalid update frequency...", KeyValue.d);
			KeyValue.putVar("LastForecastError", weeWXApp.getAndroidString(R.string.invalid_update_interval));
			return false;
		}

		String forecastData = (String)KeyValue.readVar("forecastData", "");
		if(!forced && pos == 0)
		{
			LogMessage("Set to manual update and not forced...", KeyValue.d);

			if(forecastData == null || forecastData.isBlank())
			{
				KeyValue.putVar("LastForecastError", weeWXApp.getAndroidString(R.string.wifi_not_available));
				return false;
			}

			LogMessage("forecastData != null && !forecastData.isBlank()");
			return true;
		}

		long current_time = getCurrTime();
		LogMessage("current_time: " + current_time);
		String dateStr = sdf7.format(current_time * 1_000L);
		LogMessage("current_time: " + dateStr);

		long rssCheckTime = getRSSsecs();
		LogMessage("rssCheckTime: " + rssCheckTime);
		dateStr = sdf7.format(rssCheckTime * 1_000L);
		LogMessage("rsscheck: " + dateStr);

		if(rssCheckTime == 0)
		{
			LogMessage("Bad rssCheckTime, skipping...", KeyValue.d);
			if(forecastData == null || forecastData.isBlank())
			{
				KeyValue.putVar("LastForecastError", weeWXApp.getAndroidString(R.string.still_downloading_forecast_data));
				return false;
			}

			return true;
		}

		long lastForecastDownloadTime = getLFDsecs();
		if(!forced && lastForecastDownloadTime + weeWXApp.RSSCache_period_default > current_time &&
		   forecastData != null && !forecastData.isBlank())
		{
			LogMessage("Cache isn't more than " + weeWXApp.RSSCache_period_default + "s old (" +
			           (current_time - lastForecastDownloadTime) + "s old)");
			return true;
		}

		if(!weeWXApp.hasBootedFully && !calledFromweeWXApp && !forced)
		{
			LogMessage("not fully booted or not called from weeWX App class or not forced...", KeyValue.d);

			if(forecastData != null && !forecastData.isBlank())
				return true;

			KeyValue.putVar("LastForecastError", weeWXApp.getAndroidString(R.string.still_downloading_forecast_data));
			return false;
		}

		if(forecastTask != null && !forecastTask.isDone())
		{
			if(ftStart + 30 > current_time)
			{
				LogMessage("forecastTask is less than 30s old (" + (current_time - ftStart) + "s), we'll skip this attempt...", KeyValue.d);
				return true;
			}

			forecastTask.cancel(true);
			forecastTask = null;
		}

		LogMessage("RSSCache_period_default: " + weeWXApp.RSSCache_period_default);
		LogMessage("current_time: " + current_time);
		LogMessage("rssCheckTime: " + rssCheckTime);
		LogMessage("Was forced or no forecast data or cache is more than " + weeWXApp.RSSCache_period_default +
		           "s old (" + (current_time - rssCheckTime) + "s)");

		ftStart = current_time;

		forecastTask = executor.submit(() ->
		{
			LogMessage("Forecast checking: " + forecast_url);

			String tmpForecastData;

			try
			{
				tmpForecastData = reallyGetForecast(forecast_url);
			} catch(Exception e) {
				LogMessage("Error! e: " + e, true, KeyValue.e);
				return;
			}

			if(tmpForecastData != null && !tmpForecastData.isBlank())
			{
				LogMessage("Successfully updated forecast data...");
				SendIntent(REFRESH_FORECAST_INTENT);
				ftStart = 0;
				return;
			}

			LogMessage("Failed to successfully update forecast data...", KeyValue.d);
			SendIntent(STOP_FORECAST_INTENT);
			ftStart = 0;
		});

		if(forecastData != null && !forecastData.isBlank())
		{
			LogMessage("forecastData != null and !isBlank()...");
			return true;
		}

		LogMessage("forecastData == null or isBlank()...", KeyValue.d);
		KeyValue.putVar("LastForecastError", weeWXApp.getAndroidString(R.string.still_downloading_forecast_data));
		return false;
	}

	static String reallyGetForecast(String url) throws InterruptedException, IOException
	{
		LogMessage("reallyGetForecast() forcecastURL: " + url);

		if(url == null || url.isBlank())
			return null;

		String forecastData = downloadString(url);
		if(forecastData.isBlank())
			return null;

		LogMessage("reallyGetForecast() forcecastData: " + forecastData);

		try
		{
			JSONObject jobj = new JSONObject(forecastData);
			JSONObject jo = jobj.getJSONObject("metadata");

			LogMessage("issue_time: " + jo.getString("issue_time"));
/*
			jo.put("issue_time", Instant.ofEpochSecond(getCurrTime()).toString());
			jobj.put("metadata", jo);
			forecastData = jobj.toString();

			jobj = new JSONObject(forecastData);
			jo = jobj.getJSONObject("metadata");

			LogMessage("New issue_time: " + jo.getString("issue_time"));
*/
		} catch(Exception ignored) {}

		long lastForecastDownloadTime = getCurrTime();

		LogMessage("updating rss cache");
		KeyValue.putVar("forecastData", forecastData);

		KeyValue.putVar("rssCheck", lastForecastDownloadTime);
		KeyValue.putVar("lastForecastDownloadTime", lastForecastDownloadTime);
		KeyValue.putVar("LastForecastError", "");

		return forecastData;
	}

	static Bitmap getRadarImage(boolean forced, boolean calledFromweeWXApp)
	{
		Bitmap bm = null;

		try
		{
			if(checkForImage(weeWXApp.radarFilename))
				bm = getImage(weeWXApp.radarFilename);
		} catch(Exception e) {
			LogMessage("Error! e: " + e, true, KeyValue.e);
		}

		if(!checkConnection() && !forced)
		{
			if(bm == null)
				return weeWXApp.textToBitmap(R.string.wifi_not_available);

			return bm;
		}

		int pos = (int)KeyValue.readVar("updateInterval", weeWXApp.updateInterval_default);
		LogMessage("getRadarImage() pos: " + pos + ", update interval set to: " + weeWXApp.updateOptions[pos] + ", forced set to: " + forced);
		if(pos < 0)
		{
			LogMessage("Invalid update frequency...", KeyValue.d);
			return weeWXApp.textToBitmap(R.string.invalid_update_interval);
		}

		if(!forced && pos == 0)
		{
			if(bm == null)
				return weeWXApp.textToBitmap(R.string.update_set_to_manual_but_no_content_cached);

			return bm;
		}

		LogMessage("Reloading radar image...");

		String radarURL = (String)KeyValue.readVar("RADAR_URL", weeWXApp.RADAR_URL_default);
		if(radarURL == null || radarURL.isBlank())
			return weeWXApp.textToBitmap(R.string.radar_url_not_set);

		String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
		if(radtype == null || (!radtype.equals("image") && !radtype.equals("webpage")))
		{
			String tmp = String.format(weeWXApp.getAndroidString(R.string.radar_type_is_invalid), radtype);
			return weeWXApp.textToBitmap(tmp);
		}

		if(!weeWXApp.hasBootedFully && !calledFromweeWXApp && !forced)
		{
			if(bm != null)
				return bm;
			else
				return weeWXApp.textToBitmap(R.string.radar_still_downloading);
		}

		LogMessage("Reload radar...");

		long current_time = getCurrTime();

		if(radarTask != null && !radarTask.isDone())
		{
			if(rtStart + 30 > current_time)
			{
				LogMessage("radarTask is less than 30s old (" + (current_time - rtStart) +
				                          "s), we'll skip this attempt...", KeyValue.d);
				return bm;
			}

			radarTask.cancel(true);
			radarTask = null;
		}

		LogMessage("Staring attempt to get a new radar image...");

		rtStart = current_time;

		radarTask = executor.submit(() ->
		{
			LogMessage("Radar checking: " + radarURL);

			try
			{
				LogMessage("Starting to download radar image from: " + radarURL);
				if(loadOrDownloadImage(radarURL, weeWXApp.radarFilename) != null)
				{
					SendIntent(REFRESH_RADAR_INTENT);
					rtStart = 0;
					return;
				}
			} catch(Exception e) {
				LogMessage("Error! e: " + e, true, KeyValue.e);
			}

			SendIntent(STOP_RADAR_INTENT);
			rtStart = 0;
		});

		return bm;
	}

	static Bitmap getWebcamImage(boolean forced, boolean calledFromweeWXApp)
	{
		Bitmap bm = null;
		long current_time = getCurrTime();

		try
		{
			if(checkForImage(weeWXApp.webcamFilename))
				bm = getImage(weeWXApp.webcamFilename);
		} catch(Exception e) {
			LogMessage("Error! e: " + e, true, KeyValue.e);
		}

		if(!checkConnection() && !forced)
		{
			if(bm == null)
				return weeWXApp.textToBitmap(R.string.wifi_not_available);

			return bm;
		}

		int pos = (int)KeyValue.readVar("updateInterval", weeWXApp.updateInterval_default);
		LogMessage("getWebcamImage() pos: " + pos + ", update interval set to: " +
		           weeWXApp.updateOptions[pos] + ", forced set to: " + forced);
		if(pos < 0)
		{
			LogMessage("Invalid update frequency...", KeyValue.d);
			return weeWXApp.textToBitmap(R.string.invalid_update_interval);
		}

		if(!forced && pos == 0)
		{
			LogMessage("Not forced and set to manual updates...", KeyValue.d);

			if(bm == null)
				return weeWXApp.textToBitmap(R.string.update_set_to_manual_but_no_content_cached);

			return bm;
		}

		LogMessage("Reloading webcam...");

		String webcamURL = (String)KeyValue.readVar("WEBCAM_URL", weeWXApp.WEBCAM_URL_default);
		if(webcamURL == null || webcamURL.isBlank())
		{
			LogMessage("Webcam URL not set...", KeyValue.d);
			return weeWXApp.textToBitmap(R.string.webcam_url_url_not_set);
		}

		if(!forced && wcStart + 60 > current_time)
		{
			if(bm == null)
			{
				LogMessage("Not forced and wcStart + 60 > current_time and bm == null...", KeyValue.d);
				return weeWXApp.textToBitmap(R.string.webcam_still_downloading);

			} else {
				LogMessage("Not forced and wcStart + 60 > current_time and bm != null...", KeyValue.d);
				return bm;
			}
		}

		if(!forced && !weeWXApp.hasBootedFully && !calledFromweeWXApp)
		{
			LogMessage("Not forced and hasBootedFully is false and calledFromweeWXApp is false...", KeyValue.d);

			if(bm != null)
				return bm;
			else
				return weeWXApp.textToBitmap(R.string.webcam_still_downloading);
		}

		LogMessage("Reload webcam...");

		if(webcamTask != null && !webcamTask.isDone())
		{
			if(wcStart + 30 > current_time)
			{
				LogMessage("webcamTask is less than 30s old (" + (current_time - wcStart) + "s), we'll skip this attempt...", KeyValue.d);
				return bm;
			}

			webcamTask.cancel(true);
			webcamTask = null;
		}

		wcStart = current_time;

		webcamTask = executor.submit(() ->
		{
			LogMessage("Webcam checking: " + webcamURL);

			try
			{
				LogMessage("Starting to download webcam image from: " + webcamURL);
				if(loadOrDownloadImage(webcamURL, weeWXApp.webcamFilename) != null)
				{
					LogMessage("Webcam image downloaded successfully...");
					SendIntent(REFRESH_WEBCAM_INTENT);
					return;
				}
			} catch(Exception e) {
				LogMessage("Error! e: " + e, true, KeyValue.e);
			}

			LogMessage("Webcam image failed to download successfully...", KeyValue.w);
			SendIntent(STOP_WEBCAM_INTENT);
		});

		if(bm == null)
			return weeWXApp.textToBitmap(R.string.webcam_still_downloading);

		return bm;
	}

	static String getDateFromString(String str)
	{
		str = str.strip();

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

	static String doMoon(String str)
	{
		str = str.strip();

		if(!str.contains(" "))
			return str;

		if(str.length() > 0 && str.startsWith("0"))
			str = str.substring(1);

		try
		{
			String[] bits = str.split(" ", 3);
			if(bits.length < 2 || bits[1].isBlank())
				return str;

			int day = (int)Float.parseFloat(bits[1].strip());
			if(day > 31 || day < 1)
				return str;

			return bits[0] + " " + getDaySuffix(day);
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
		return "data:image/jpeg;base64," + removeWS(Base64.encodeToString(in, Base64.DEFAULT));
	}

	static String removeWS(String str)
	{
		return str
				.replaceAll("\n", "")
				.replaceAll("\r", "")
				.replaceAll("\t", "")
				.strip();
	}

	static String indentNonBlankLines(String s, int numberOfTabs)
	{
		String indents = String.join("", Collections.nCopies(numberOfTabs, "\t"));
		StringBuilder out = new StringBuilder();
		try(BufferedReader br = new BufferedReader(new StringReader(s)))
		{
			String line;
			boolean first = true;
			while((line = br.readLine()) != null)
			{
				if (!first) out.append('\n');
				first = false;

				if(line.trim().isEmpty())
					out.append(line);  // keep empty lines unchanged
				else
					out.append(indents).append(line);
			}
		} catch (IOException e) {
			LogMessage("Error! e: " + e, true, KeyValue.e);
		}

		return out.toString();
	}

	static File getExtFile(String finalDir, String filename) throws IOException
	{
		File dir = new File(Environment.getExternalStorageDirectory(), "Download");
		dir = new File(dir, finalDir);

		if(dir.exists() && !dir.isDirectory())
		{
			LogMessage("Something called '" + finalDir + "' already exist, but it isn't a directory...", KeyValue.w);
			throw new IOException("There is already something named " + finalDir + " but it's not a directory");
		}

		if(!dir.exists())
		{
			if(!dir.mkdirs())
			{
				LogMessage("Can't make '" + dir.getAbsoluteFile() + "' dir...", KeyValue.w);
				throw new IOException("Tried to create " + dir.getAbsoluteFile() + " but failed to create the directory");
			}

			publish(dir);
		}

		return new File(dir, filename);
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

		if(!newdir.exists())
		{
			if(!newdir.mkdirs())
				throw new IOException("Can't create the requested directory " + newdir.getAbsolutePath());

			publish(newdir);
		}

		return newdir;
	}

	static File getDataDir()
	{
		LogMessage("filesDir: " + weeWXApp.getInstance().getFilesDir().getAbsolutePath());
		return weeWXApp.getInstance().getFilesDir();
	}

	static File getDir(String dir)
	{
		return new File(getDataDir(), dir);
	}

	static boolean renameTo(File oldfile, File newfile) throws IOException
	{
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			try
			{
				Files.move(oldfile.toPath(), newfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				return true;
			} catch(IOException e) {
				LogMessage("Error! e: " + e, true, KeyValue.e);
			}
		}

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

	static File getFile(String filename)
	{
		return getFile(getDataDir(), filename);
	}

	static File getFile(File dir, String filename)
	{
		return new File(dir, filename);
	}

	static Bitmap loadImage(String fileName)
	{
		File f = new File(getDir("icons"), fileName);

		return loadImage(f);
	}

	static Bitmap loadImage(File file)
	{
		return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
	}

	static Bitmap getImage(String filename)
	{
		File file = getDataDir();
		file = new File(file, filename);
		return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
	}

	static Bitmap loadOrDownloadImage(String url, String filename) throws InterruptedException, IOException
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
				LogMessage("Error! e: " + e, true, KeyValue.e);
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

	static byte[] downloadContent(String url) throws InterruptedException, IOException
	{
		if(url == null || url.isBlank())
		{
			LogMessage("url is null or blank, bailing out...", KeyValue.d);
			throw new IOException("url is null or blank, bailing out...");
		}

		OkHttpClient client = NetworkClient.getInstance(url);

		return reallyDownloadContent(client, url, 0);
	}

	private static byte[] reallyDownloadContent(OkHttpClient client, String url, int retries) throws InterruptedException, IOException
	{
		LogMessage("reallyDownloadContent() checking if url  " + url + " is valid, attempt " + (retries + 1));

		Request request = NetworkClient.getRequest(false, url);

		try(Response response = client.newCall(request).execute())
		{
			if(!response.isSuccessful())
			{
				String warning = "HTTP Error: " + response;
				LogMessage(warning, true, KeyValue.w);
				throw new IOException(warning);
			}

			return response.body().bytes();
		} catch(Exception e) {
			if(retries < maximum_retries)
			{
				retries++;

				LogMessage("reallyCheckURL() Error! e: " + e.getMessage() + ", retry: " + retries +
				           ", will sleep " + retry_sleep_time + " seconds and retry...", true);

				try
				{
					Thread.sleep(retry_sleep_time);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					LogMessage("reallyCheckURL() Error! ie: " + ie.getMessage(), true, KeyValue.e);

					throw ie;
				}

				return reallyDownloadContent(client, url, retries);
			}

			LogMessage("reallyCheckURL() Error! e: " + e.getMessage(), true, KeyValue.e);
			throw e;
		}
	}

	static boolean downloadToFile(File file, String url) throws InterruptedException, IOException
	{
		LogMessage("Downloading from url: " + url);
		byte[] body = downloadContent(url);
		if(body == null || body.length == 0)
		{
			String warning = "Download content was null or empty";
			LogMessage(warning, true, KeyValue.w);
			throw new IOException(warning);
		}

		File tmpfile = File.createTempFile("weeWXApp_", ".tmp");

		if(tmpfile.exists() && !tmpfile.delete())
		{
			String warning = tmpfile.getAbsolutePath() + " exists, but can't be deleted, bailing out...";
			LogMessage(warning, true, KeyValue.w);
			throw new IOException(warning);
		}

		FileOutputStream fos = new FileOutputStream(tmpfile);
		fos.write(body);
		fos.close();

		LogMessage("Successfully saved " + body.length + " bytes of data to: " + tmpfile.getAbsoluteFile());

		if(!renameTo(tmpfile, file))
		{
			String warning = "Failed to rename tmpfile " + tmpfile.getAbsolutePath() + " to desination file " +
			                 file.getAbsolutePath() + ", bailing out...";
			LogMessage(warning, true, KeyValue.w);
			throw new IOException(warning);
		}

		LogMessage("Renamed " + tmpfile.getAbsolutePath() + " to " + file.getAbsoluteFile() + " successfully...");

		return true;
	}

	static Bitmap grabMjpegFrame(String url) throws InterruptedException, IOException
	{
		if(url == null || url.isBlank())
		{
			LogMessage("url is null or blank, bailing out...", KeyValue.d);
			throw new IOException("url is null or blank, bailing out...");
		}

		OkHttpClient client = NetworkClient.getStream(url);

		return reallyGrabMjpegFrame(client, url, 0);
	}

	static Bitmap reallyGrabMjpegFrame(OkHttpClient client, String url, int retries) throws InterruptedException, IOException
	{
		Exception lastException;
		Bitmap bm;
		InputStream urlStream = null;

		LogMessage("reallyGrabMjpegFrame() checking if url  " + url + " is valid, attempt " + (retries + 1));

		Request request = NetworkClient.getRequest(false, url);

		try(Response response = client.newCall(request).execute())
		{
			if(!response.isSuccessful())
			{
				String warning = "Error! response: " + response;
				LogMessage(warning, true, KeyValue.w);
				throw new IOException(warning);
			}

			LogMessage("Successfully connected to server, now to grab a frame...");

			urlStream = response.body().byteStream();

			BufferedReader reader = new BufferedReader(new InputStreamReader(urlStream, StandardCharsets.US_ASCII));

			String line;
			int contentLength = -1;

			while((line = reader.readLine()) != null)
			{
				if(line.isEmpty() && contentLength > 0)
					break;

				if(line.startsWith("Content-Length:"))
					contentLength = Integer.parseInt(line.substring(15).strip());
			}

			LogMessage("contentLength: " + contentLength);

			byte[] imageBytes = new byte[contentLength];
			int offset = 0;
			while(offset < contentLength)
			{
				int read = urlStream.read(imageBytes, offset, contentLength - offset);
				if(read == -1)
				{
					String warning = "Stream ended prematurely";
					LogMessage(warning, true, KeyValue.w);
					throw new IOException(warning);
				}
				offset += read;
			}

			BitmapFactory.Options options = new BitmapFactory.Options();
			bm = BitmapFactory.decodeStream(new ByteArrayInputStream(imageBytes), null, options);
			if(bm != null)
			{
				LogMessage("Got an image... wooo!");
				return bm;
			}

			lastException = new IOException("Failed to successfully grab a frame from a mjpeg stream...");
		} catch(IOException e) {
			lastException = e;
		} finally {
			if(urlStream != null)
				urlStream.close();
		}

		if(lastException == null)
			lastException = new IOException("Something bad happened... Not sure what though...");

		if(retries < maximum_retries)
		{
			retries++;

			LogMessage("reallyCheckURL() Error! e: " + lastException.getMessage() + ", retry: " + retries +
			           ", will sleep " + retry_sleep_time + " seconds and retry...", true);

			try
			{
				Thread.sleep(retry_sleep_time);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				LogMessage("reallyCheckURL() Error! ie: " + ie.getMessage(), true, KeyValue.e);

				throw ie;
			}

			return reallyGrabMjpegFrame(client, url, retries);
		}

		LogMessage("reallyCheckURL() Error! lastException: " + lastException.getMessage(), true, KeyValue.e);
		try
		{
			throw lastException;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	static long getCurrTime()
	{
		return Math.round(System.currentTimeMillis() / 1_000D);
	}

	static Activity getActivity()
	{
		Context context = weeWXApp.getInstance();

		while(context instanceof ContextWrapper)
		{
			if(context instanceof Activity)
				return (Activity)context;

			context = ((ContextWrapper)context).getBaseContext();
		}

		return null;
	}

	static String fiToSVG(String cssname)
	{
		if(cssname.startsWith("<img "))
			return cssname;

		String tmpStr = "<img class='wi'";

		if((int)KeyValue.readVar("theme", weeWXApp.theme_default) == R.style.AppTheme_weeWXApp_Dark_Common)
			tmpStr += " style='filter:invert(1);'";

		tmpStr += " src='";

		String tmpImg = "";

		switch(cssname)
		{
			case "flaticon-temperature" -> tmpImg = "glyphs/uniF100.svg";
			case "flaticon-home-page" -> tmpImg = "glyphs/uniF101.svg";
			case "flaticon-women-sunglasses" -> tmpImg = "glyphs/uniF102.svg";
			case "flaticon-windy" -> tmpImg = "glyphs/uniF103.svg";
			case "flaticon-thermometer" -> tmpImg = "glyphs/uniF104.svg";
			case "flaticon-cactus" -> tmpImg = "glyphs/uniF105.svg";
			case "flaticon-cool" -> tmpImg = "glyphs/uniF106.svg";
			case "flaticon-cold" -> tmpImg = "glyphs/uniF107.svg";
			case "flaticon-warm" -> tmpImg = "glyphs/uniF108.svg";
			default -> LogMessage("Invalid FlatIcon String: " + cssname, true, KeyValue.w);
		}

		if(!tmpImg.isBlank())
			tmpImg= "file:///android_asset/" + tmpImg;

		return tmpStr + tmpImg + "'/>";
	}

	static int getDirection(String direction)
	{
		int dir;

		switch(direction)
		{
			case "nne" -> dir = 23;
			case "ne" -> dir = 45;
			case "ene" -> dir = 68;
			case "e" -> dir = 90;
			case "ese" -> dir = 113;
			case "se" -> dir = 135;
			case "sse" -> dir = 158;
			case "s" -> dir = 180;
			case "ssw" -> dir = 203;
			case "sw" -> dir = 225;
			case "wsw" -> dir = 248;
			case "w" -> dir = 270;
			case "wnw" -> dir = 293;
			case "nw" -> dir = 313;
			case "nnw" -> dir = 336;
			default -> dir = 0;
		}

		return dir;
	}

	static String cssToSVG(String cssname)
	{
		if(cssname == null || cssname.isBlank())
			return null;

		return cssToSVG(cssname, null);
	}

	static float C2F(float C)
	{
		return C * 9.0f / 5.0f + 32.0f;
	}

	static float F2C(float F)
	{
		return (F - 32.0f) / 1.8f;
	}

	static String C2Fdeg(int C)
	{
		return Math.round(C2F(C)) + "&deg;F";
	}

	static String F2Cdeg(int F)
	{
		return Math.round(F2C(F)) + "&deg;C";
	}

	static String C2Fdeg(float C)
	{
		return (Math.round(C2F(C) * 10.0) / 10.0) + "&deg;F";
	}

	static String F2Cdeg(float F)
	{
		return (Math.round(F2C(F) * 10.0) / 10.0) + "&deg;C";
	}

	static float round(float num, int dp)
	{
		return BigDecimal.valueOf(num)
				.setScale(dp, RoundingMode.HALF_UP)
				.floatValue();
	}

	static double round(double num, int dp)
	{
		return BigDecimal.valueOf(num)
				.setScale(dp, RoundingMode.HALF_UP)
				.doubleValue();
	}

	static String cssToSVG(String cssname, Integer Angle)
	{
		if(cssname == null || cssname.isBlank())
			return null;

		int dir;

		if(cssname.startsWith("<img "))
			return cssname;

		String tmpStr = "<img class='wi'";

		int theme = (int)KeyValue.readVar("theme", weeWXApp.theme_default);

		if((cssname.equals("wi-wind-deg") && Angle != null) || theme == R.style.AppTheme_weeWXApp_Dark_Common)
		{
			tmpStr += " style='";

			if(theme == R.style.AppTheme_weeWXApp_Dark_Common)
				tmpStr += "filter:invert(1);";

			if(cssname.equals("wi-wind-deg") && Angle instanceof Integer angle)
			{
				dir = angle;

				if(dir < 0 || dir > 359)
					dir = 0;

				tmpStr += "transform:rotate(" + dir + "deg);display: inline-block;";
			}

			tmpStr += "'";
		}

		tmpStr += " src='file:///android_asset/glyphs/" + cssname + ".svg'/>";

		return tmpStr;
	}

	static long[] getNPWSLL()
	{
		long now = System.currentTimeMillis();

//		Log.i(LOGTAG, Log.getStackTraceString(new Throwable()));

		String string_time = sdf8.format(now);
		LogMessage("getNPWSLL() now: " + string_time);

		long[] ret = getPeriod();

		LogMessage("Got the following: " + Arrays.toString(ret));

		long period = ret[0];
		long wait = ret[1];

		if(period <= 0)
			return new long[]{now, period, wait, 0L, 0L};

		LogMessage("Here1");

		long lastDownloadTime = getLDms();

		LogMessage("Here2");

		string_time = sdf8.format(lastDownloadTime);
		LogMessage("getNPWSLL() lastDownloadTime: " + string_time);

		long start = Math.round((double)now / (double)period) * period;

		string_time = sdf8.format(start);
		LogMessage("getNPWSLL() start: " + string_time);

		start += wait;

		string_time = sdf8.format(start);
		LogMessage("getNPWSLL() start+wait: " + string_time);

		while(start < now)
			start += period;

		string_time = sdf8.format(start);
		LogMessage("getNPWSLL() next start: " + string_time);

		long lastStart = start - period;

		string_time = sdf8.format(lastStart);
		LogMessage("getNPWSLL() lastStart: " + string_time);

		return new long[]{now, period, wait, start, lastStart, lastDownloadTime};
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