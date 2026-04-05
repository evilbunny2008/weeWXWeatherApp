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
import android.icu.text.MessageFormat;
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
import android.util.Xml;
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
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.jsoup.Jsoup;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xmlpull.v1.XmlPullParser;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
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


import static com.odiousapps.weewxweather.WidgetProvider.updateAppWidget;
import static com.odiousapps.weewxweather.weeWXApp.getAndroidString;
import static com.odiousapps.weewxweather.weeWXApp.hasBootedFully;

@SuppressWarnings({"unused", "SameParameterValue", "ApplySharedPref", "ConstantConditions",
                   "SameReturnValue", "BooleanMethodIsAlwaysInverted", "SetTextI18n",
                   "ConstantLocale", "CallToPrintStackTrace", "SequencedCollectionMethodCanBeUsed"})
class weeWXAppCommon
{
	static final float[] NEGATIVE = {
			-1.0f, 0, 0, 0, 255, // red
			0, -1.0f, 0, 0, 255, // green
			0, 0, -1.0f, 0, 255, // blue
			0, 0, 0, 1.0f, 0  // alpha
	};

	private final static String PREFS_NAME = "WeeWxWeatherPrefs";
	final static String LOGTAG = "weeWXApp";
	static int debug_level = KeyValue.i;
	final static boolean debug_html = false;
	final static boolean web_debug_on = false;
	private final static int maxLogLength = 5_000;

	static final String UTF8_BOM = "\uFEFF";

	static final int default_timeout = 5_000;
	static final int default_webview_timeout = 90_000;
	static final int default_wait_on_boot = 2_500;
	static final int maximum_retries = 3;
	static final int retry_sleep_time = 1_000;

	private static Uri logFileUri = null;

	static final String EXIT_INTENT = "com.odiousapps.weewxweather.EXIT_INTENT";
	static final String INIGO_INTENT = "com.odiousapps.weewxweather.INIGO_UPDATE";
	static final String PROCESSING_ERRORS = "com.odiousapps.weewxweather.PROCESSING_ERRORS";

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

	final static String[] json_labels = {"JSON Data", "JSON Dicts", "JSON Last"};
	final static String[] json_keys = {"json_data", "json_dicts", "json_last"};

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
	static final SimpleDateFormat sdf15 = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
	static final SimpleDateFormat sdf16 = new SimpleDateFormat("EEE d, HH:mm", Locale.getDefault());
	static final SimpleDateFormat sdf17 = new SimpleDateFormat("EEE d, h:mm a", Locale.getDefault());
	static final SimpleDateFormat sdf18 = new SimpleDateFormat("EEE d, MMMM yyyy h:mm a", Locale.getDefault());
	static final SimpleDateFormat sdf19 = new SimpleDateFormat("h:mm a", Locale.getDefault());
	static final SimpleDateFormat sdf20 = new SimpleDateFormat("h:mma", Locale.getDefault());
	static final SimpleDateFormat sdf21 = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault());

	private static final BitmapFactory.Options options = new BitmapFactory.Options();

	private static JSONObject nws = null;

	private static Typeface tf_bold = null;

	private final static ExecutorService executor = Executors.newFixedThreadPool(5);
	private static final ExecutorService prefsExec = Executors.newSingleThreadExecutor();

	private static Future<?> forecastTask, radarTask, weatherTask, webcamTask;

	private static long lastUpdateCheck = 0;
	private static long ftStart = lastUpdateCheck, rtStart = lastUpdateCheck,
			wcStart = lastUpdateCheck, wtStart = lastUpdateCheck;

	private static final Set<String> processedMissingIcons = new HashSet<>();

	private static int wriCounter = 0;
	private static long wriStart = 0;

	record Result(List<Day> days, String desc, long timestamp, boolean isDaily) {}
	record Result2(String[] forecast_text, String desc, int rc) {}
	record TempResult(float CurrTemp, float minObservedTemp, float maxObservedTemp, int[] outTemp_trend_count,
	                  int[] outTemp_trend_signal, long[] outTemp_trend_ts) {}
	record NPWSLL(long nowTime, long periodTime, long waitTime, long startTime, long lastStart, long report_time) {}

	private static final String utf8 = "utf-8";

	// Period indices
	private static final int PERIOD_10MIN = 0;
	private static final int PERIOD_30MIN = 1;
	private static final int PERIOD_1HR   = 2;
	private static final int PERIOD_6HR   = 3;
	private static final int PERIOD_24HR  = 4;

	// Alert level indices
	private static final int LEVEL_WATCH   = 0;
	private static final int LEVEL_WARNING = 1;
	private static final int LEVEL_SEVERE  = 2;

	// Thresholds in mm
	private static final int[][] FLOOD_THRESHOLDS_MM = {
	    { 600,   1000,  2000  },  // 10 min
	    { 1500,  2500,  5000  },  // 30 min
	    { 2500,  5000,  10000 },  // 1 hr
	    { 6000,  11000, 20000 },  // 6 hr
	    { 10000, 20000, 30000 }   // 24 hr
	};

	// Thresholds in 1/100ths inch
	private static final int[][] FLOOD_THRESHOLDS_IN = {
	    { 24,  39,  79   },  // 10 min
	    { 59,  98,  197  },  // 30 min
	    { 98,  197, 394  },  // 1 hr
	    { 236, 433, 787  },  // 6 hr
	    { 394, 787, 1181 }   // 24 hr
	};

	private static final int[] warning_delays = {
		600, 1800, 3600, 21600, 86400
	};

	final static String[] direction_labels = new String[]{"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
															"S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};

	static
	{
		if(weeWXApp.DEBUG)
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

		int pos = (int)KeyValue.readVar("UpdateFrequency", weeWXApp.UpdateFrequency_default);
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
			LogMessage("MainActivity.onCreate() colorSurface of " + name + ": #" + Integer.toHexString(colour));
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

	static void setVar(String var, Object val)
	{
		if(val == null)
		{
			if(isPrefSet(var))
				RemovePref(var);

			return;
		}

		switch(val)
		{
			case Boolean b ->
			{
				try
				{
					getPrefSettings().edit().putBoolean(var, (boolean)val).commit();
				} catch(Exception ignored) {}
			}
			case Float v ->
			{
				try
				{
					getPrefSettings().edit().putFloat(var, (float)val).commit();
				} catch(Exception ignored) {}
			}
			case Integer i ->
			{
				try
				{
					getPrefSettings().edit().putInt(var, (int)val).commit();
				} catch(Exception ignored) {}
			}
			case Long l ->
			{
				try
				{
					getPrefSettings().edit().putLong(var, (long)val).commit();
				} catch(Exception ignored) {}
			}
			case String s ->
			{
				try
				{
					getPrefSettings().edit().putString(var, s).commit();
				} catch(Exception ignored) {}
			}
			default ->
			{
			}
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
		SharedPreferences prefs = getPrefSettings();
		if(prefs == null)
			return false;

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

			// Choose the best numeric type
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

	static long getRSSms()
	{
		LogMessage("Checking for var named 'rssCheck'");

		if(!KeyValue.isPrefSet("rssCheck"))
			return 0L;

		long rssTime = (long)KeyValue.readVar("rssCheck", 0L);

		LogMessage("getRSSms() Before: rssTime: " + rssTime);

		while(rssTime < 10_000_000_000L)
			rssTime *= 1_000L;

		LogMessage("getRSSms() After: rssTime: " + rssTime);

		return rssTime;
	}

	static long getRSSsecs()
	{
		long rssTime = getRSSms();
		while(rssTime > 10_000_000_000L)
			rssTime = Math.round(rssTime / 1_000D);

		return rssTime;
	}

	static long getLDTms()
	{
		LogMessage("Checking for var named '" + json_keys[0] + "_time'");

		if(!isPrefSet(json_keys[0] + "_time"))
			return 0L;

		long LastDownloadTime = (long)KeyValue.readVar(json_keys[0] + "_time", 0L);

		LogMessage("getLDTms() Before: LastDownloadTime: " + LastDownloadTime);

		while(LastDownloadTime < 10_000_000_000L)
			LastDownloadTime *= 1_000L;

		LogMessage("getLDTms() After: LastDownloadTime: " + LastDownloadTime);

		return LastDownloadTime;
	}

	static long getLAFDms()
	{
		//LogMessage("Checking for var named 'lastAttemptedForecastDownloadTime'");

		if(!isPrefSet("lastAttemptedForecastDownloadTime"))
			return 0L;

		long lastAttemptedForecastDownloadTime = (long)KeyValue.readVar("lastAttemptedForecastDownloadTime", 0L);
		while(lastAttemptedForecastDownloadTime < 10_000_000_000L)
			lastAttemptedForecastDownloadTime *= 1_000L;

		return lastAttemptedForecastDownloadTime;
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

	static String generateForecast(List<Day> days, long timestamp, boolean showHeader, boolean daily)
	{
		LogMessage("Starting generateForecast()");
//		LogMessage("days: " + days);

		if(days.isEmpty())
		{
			LogMessage("generateForecast() Was sent an empty days variable, skipping...", true, KeyValue.w);
			return null;
		}

		Calendar cal = Calendar.getInstance();

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
					sb.append("\t\t\t<img style='height:100px;width:auto;' alt='weather icon' src='").append(removeWS(first.icon)).append("' />\n");

				sb.append("\t\t</div>\n");
			} else {
				sb.append(weeWXApp.currentSpacer);
			}

			sb.append("\t\t<div class='wordToday'>").append(getAndroidString(R.string.today)).append("</div>\n");

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
					sb.append("\t\t\t<img style='height:75px;width:auto;' alt='weather icon' src='")
							.append(removeWS(day.icon))
							.append("' />\n");

				sb.append("\t\t</div>\n");
			} else {
				sb.append(weeWXApp.currentSpacer);
			}

			sb.append("\t\t\t<div class='dayTitle'>");

			if(daily)
			{
				if(i == 0)
					sb.append(getAndroidString(R.string.today));
				else
					sb.append(weeWXAppCommon.sdf2.format(day.timestamp));
			} else {
				cal.setTimeInMillis(day.timestamp);
				int hour = cal.get(Calendar.HOUR_OF_DAY);
				if(hour == 0)
					sb.append(weeWXAppCommon.sdf2.format(day.timestamp));
				else
					sb.append(weeWXAppCommon.sdf17.format(day.timestamp));
			}

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
		if(timestamp <= 0)
			return;

		while(timestamp < 10_000_000_000L)
			timestamp *= 1_000L;

		long rssCheck = getRSSms();

		LogMessage("rssCheck: " + rssCheck);
		LogMessage("timestamp: " + timestamp);

		if(timestamp != rssCheck)
			KeyValue.putVar("rssCheck", timestamp);
	}

	static Day getDayByDate(List<Day> days, long timestamp)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestamp);

		Calendar cal1 = Calendar.getInstance();

		for(int i = 0; i < days.size(); i++)
		{
			Day day = days.get(i);

			cal1.setTimeInMillis(day.timestamp);

			if(cal.get(Calendar.YEAR) == cal1.get(Calendar.YEAR) &&
			   cal.get(Calendar.DAY_OF_YEAR) == cal1.get(Calendar.DAY_OF_YEAR))
				return day;
		}

		return null;
	}

	static Result processBOM3(int UpdateInterval, String data, String url) throws IOException, InterruptedException
	{
		LogMessage("processBOM3()");

		int modhour = getIntervalTime(UpdateInterval)[1];
		long now = System.currentTimeMillis();

		Result r1 = null;
		long bomDailyLastUpdate = (long)KeyValue.readVar("bomDailyLastUpdate", 0L);
		if(now - bomDailyLastUpdate < 10_800_000L)
		{
			String bomDailyGson = (String)KeyValue.readVar("bomDailyGson", "");
			Gson gson = new Gson();
			GsonHelper gh = gson.fromJson(bomDailyGson, GsonHelper.class);
			if(gh != null && gh.days != null && !gh.days.isEmpty())
				r1 = new Result(gh.days, gh.desc, gh.timestamp, gh.isDaily);
		}

		if(r1 == null)
		{
			String url2 = url.replace("/hourly", "/daily");

			String fcdata = "";
			if(!url2.isBlank() && !url2.equals(url))
			{
				LogMessage("processBOM3(): Getting BoM daily");
				fcdata = downloadString(url2, false);
			}

			if(fcdata != null && !fcdata.isBlank())
			{
				LogMessage("processBOM3(): Processing BoM daily");
				r1 = processBOM3Daily(fcdata, false);
				if(r1 != null)
				{
					GsonHelper gh = new GsonHelper();
					gh.days = r1.days();
					gh.desc = r1.desc();
					gh.timestamp = r1.timestamp() > 0 ? r1.timestamp() : System.currentTimeMillis();
					gh.isDaily = r1.isDaily();
					Gson gson = new Gson();
					String forecastGson = gson.toJson(gh);

					KeyValue.putVar("bomDailyGson", forecastGson);
					KeyValue.putVar("bomDailyLastUpdate", gh.timestamp);
				}
			}
		}

		Calendar cal = Calendar.getInstance();
		Calendar today = Calendar.getInstance();

		LogMessage("processBOM3(): Processing BoM hourly");
		Result r2 = processBOM3Hourly(data);

		if(r1 == null || r1.days.isEmpty())
			return r2;

		LogMessage("processBOM3(): Merging BoM daily + hourly");

		long lasttimestamp = 0;
		boolean not_set_text_yet = false;
		for(int i = 0; i < r2.days.size(); i++)
		{
			Day day = r2.days.get(i);
			cal.setTimeInMillis(day.timestamp);
			int hour = cal.get(Calendar.HOUR_OF_DAY);

			lasttimestamp = day.timestamp;

			boolean isToday = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
						   cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);

			Day r1day = getDayByDate(r1.days, day.timestamp);
			if(r1day == null)
				continue;

//			if(!not_set_text_yet)
//			{
//				LogMessage("cal.get(Calendar.YEAR): " + cal.get(Calendar.YEAR));
//				LogMessage("today.get(Calendar.YEAR): " + today.get(Calendar.YEAR));
//				LogMessage("cal.get(Calendar.DAY_OF_YEAR): " + cal.get(Calendar.DAY_OF_YEAR));
//				LogMessage("today.get(Calendar.DAY_OF_YEAR): " + today.get(Calendar.DAY_OF_YEAR));
//
//				LogMessage("hour: " + hour);
//				LogMessage("modhour: " + modhour);
//				LogMessage("hour % modhour: " + hour % modhour);
//			}

			if(isToday && hour % modhour == 0 && !not_set_text_yet)
			{
				not_set_text_yet = true;
				day.text = r1day.text;
				day.min = r1day.min;
				day.max = r1day.max;
				r2.days.set(i, day);
			}

			if(hour == 0)
			{
				day.text = r1day.text;
				day.min = r1day.min;
				day.max = r1day.max;
				r2.days.set(i, day);
			}
		}

		for(int i = 0; i < r1.days.size(); i++)
		{
			Day r1day = r1.days.get(i);
			if(r1day.timestamp < lasttimestamp)
				continue;

			r2.days.add(r1day);
		}

		LogMessage("processBOM3(): Successfully merged BoM daily + hourly");

		return r2;
	}

	static Result processBOM3Hourly(String data)
	{
		String missing = null;

		LogMessage("processBOM3Hourly(): Starting processBOM3Hourly()");

		if(data.isBlank())
		{
			LogMessage("processBOM3Hourly(): data is blank, skipping...", true, KeyValue.w);
			return null;
		}

		//LogMessage("data: " + data);

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);

		String desc = KeyValue.bomLocation;
		LogMessage("processBOM3Hourly(): desc: " + desc);

		List<Day> days = new ArrayList<>();
		long timestamp = 0;

		try
		{
			JSONObject jobj = new JSONObject(data);

			String tmp = jobj.getJSONObject("metadata").getString("issue_time");

			Date df = sdf1.parse(tmp);
			if(df != null)
				timestamp = df.getTime();

			if(timestamp <= 0)
				timestamp = System.currentTimeMillis();

			if(timestamp > 0)
				updateCacheTime(timestamp);

			Date date = new Date(timestamp);
			LogMessage("Last updated forecast: " + sdf5.format(date));

			JSONArray myhours = jobj.getJSONArray("data");
			for(int i = 0; i < myhours.length(); i++)
			{
				Day day = new Day();

				day.timestamp = 0;
				df = sdf1.parse(myhours.getJSONObject(i).getString("time"));
				if(df != null)
					day.timestamp = df.getTime();

				if(metric)
				{
					try
					{
						day.max = myhours.getJSONObject(i).getInt("temp") + "&deg;C";
					} catch(Exception ignored) {}

					try
					{
						day.min = myhours.getJSONObject(i).getInt("temp_feels_like") + "&deg;C";
					} catch(Exception ignored) {}
				} else {
					try
					{
						day.max = Math.round(C2F(myhours.getJSONObject(i).getInt("temp"))) + "&deg;F";
					} catch(Exception ignored) {}

					try
					{
						day.min = Math.round(C2F(myhours.getJSONObject(i).getInt("temp_feels_like"))) + "&deg;F";
					} catch(Exception ignored) {}
				}

				String fileName = bomlookup(myhours.getJSONObject(i).getString("icon_descriptor"));
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
					String forecaseURL = (String)KeyValue.readVar("FORECAST_URL", "");

					LogMessage("Unable to locate SVG: " + missing, KeyValue.d);

					weeWXAppCommon.uploadMissingIcon(Map.of(
							"svgName", missing,
							"svgURL", forecaseURL)
					);
				}

				days.add(day);
			}
		} catch(Exception e) {
			LogMessage("Error! e: " + e, true, KeyValue.e);
			Activity act = getActivity();
			if(act == null)
				return null;

			act.finish();
		}

		return new Result(days, desc, timestamp, false);
	}

	static Result processBOM3Daily(String data, boolean updateCacheTime)
	{
		String missing = null;

		LogMessage("Starting processBOM3Daily()");

		if(data.isBlank())
		{
			LogMessage("processBOM3Daily() data is blank, skipping...", true, KeyValue.w);
			return null;
		}

		//LogMessage("data: " + data);

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		String desc = KeyValue.bomLocation;
		List<Day> days = new ArrayList<>();
		long timestamp = 0;

		try
		{
			JSONObject jobj = new JSONObject(data);

			//desc = jobj.getJSONObject("metadata").getString("forecast_region");
			String tmp = jobj.getJSONObject("metadata").getString("issue_time");

			Date df = sdf1.parse(tmp);
			if(df != null)
				timestamp = df.getTime();

			if(timestamp <= 0)
				timestamp = System.currentTimeMillis();

			if(timestamp > 0 && updateCacheTime)
				updateCacheTime(timestamp);

			Date date = new Date(timestamp);
			LogMessage("Last updated forecast: " + sdf5.format(date));

			JSONArray mydays = jobj.getJSONArray("data");
			for(int i = 0; i < mydays.length(); i++)
			{
				Day day = new Day();

				day.timestamp = 0;
				df = sdf1.parse(mydays.getJSONObject(i).getString("date"));
				if(df != null)
					day.timestamp = df.getTime();

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
					String forecaseURL = (String)KeyValue.readVar("FORECAST_URL", "");

					LogMessage("Unable to locate SVG: " + missing, KeyValue.d);

					weeWXAppCommon.uploadMissingIcon(Map.of(
							"svgName", missing,
							"svgURL", forecaseURL)
					);
				}

				if(day.icon != null && !day.icon.isBlank())
					days.add(day);
			}
		} catch(Exception e) {
			LogMessage("Error! e: " + e, true, KeyValue.e);
			Activity act = getActivity();
			if(act == null)
				return null;

			act.finish();
		}

		return new Result(days, desc, timestamp, true);
	}

	private static String bomlookup(String icon)
	{
		icon = icon.replace("-", "_");

		return switch(icon)
		{
			case "dusty" -> "dust";
			case "hazy" -> "haze";
			case "light_shower" -> "light_showers";
			case "mostly_sunny" -> "partly_cloudy";
			case "shower" -> "showers";
			case "storm" -> "storms";
			case "windy" -> "wind";
			default -> icon;
		};

//		LogMessage("BoM Old Icon: " + icon, KeyValue.d);
//		LogMessage("BoM New Icon: " + newicon, KeyValue.d);
//		return newicon;
	}

	static Result processMET(String data)
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

			String icon = "https://beta.metoffice.gov.uk" + forecasts[i].split("<img class='icon'")[1].split("src='")[1].split("'>")[0].strip();
			StringBuilder fileName = new StringBuilder(icon.substring(icon.lastIndexOf("/") + 1).replaceAll("\\.svg$", ".png"));
			day.min = forecasts[i].split("<span class='tab-temp-low'", 2)[1].split("'>")[1].split("</span>")[0].strip();
			day.max = forecasts[i].split("<span class='tab-temp-high'", 2)[1].split("'>")[1].split("</span>")[0].strip();
			day.text = forecasts[i].split("<div class='summary-text", 2)[1].split("'>", 3)[2]
					.split("</div>", 2)[0].replace("</span>", "").replace("<span>", "");

			day.min = day.min.substring(0, day.min.length() - 5) + "&deg;C";
			day.max = day.max.substring(0, day.max.length() - 5) + "&deg;C";

			fileName.insert(0, "met");

			day.icon = "file:///android_asset/icons/met/" + fileName;

			if(!metric)
			{
				day.max = C2Fdeground(str2Float(day.max));
				day.min = C2Fdeground(str2Float(day.min));
			}

			days.add(day);
		}

		return new Result(days, desc, timestamp, true);
	}

	// Thanks goes to the https://saratoga-weather.org folk for the base NOAA icons and code for dualimage.php
	static Result processWGOV(String data)
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

				if(iconLink.getString(i).toLowerCase(Locale.ENGLISH).strip().startsWith("http"))
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

				day.max = temperature.getString(i) + "&deg;F";

				if(metric)
					day.max = F2Cdeground(str2Float(temperature.getString(i)));

				day.text = weather.getString(i);
				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new Result(days, desc, timestamp, true);
	}

	static Result processWMO(String data)
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

				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new Result(days, desc, timestamp, true);
	}

	static Result processMetService(String data)
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
					day.max = C2Fdeground(str2Float(day.max));
					day.min = C2Fdeground(str2Float(day.min));
				}

				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new Result(days, desc, timestamp, true);
	}

	static Result processDWD(String data)
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
				String icon, temp, dayName;
				if(bit.split("<td ><b>", 2).length > 1)
					dayName = bit.split("<td ><b>", 2)[1].split("</b></td>", 2)[0].strip();
				else
					dayName = bit.split("<td><b>", 2)[1].split("</b></td>", 2)[0].strip();

				Locale locale = new Locale.Builder().setLanguage("de").setRegion("DE").build();
				day.timestamp = convertDaytoTS(dayName, locale, lastTS);

				if(bit.split("<td ><img name='piktogramm' src='", 2).length > 1)
					icon = bit.split("<td ><img name='piktogramm' src='", 2)[1].split("' width='50' alt='", 2)[0].strip();
				else
					icon = bit.split("<td><img name='piktogramm' src='", 2)[1].split("' width='50' alt='", 2)[0].strip();

				if(bit.split("'></td>\r\n<td >", 2).length > 1)
					temp = bit.split("'></td>\r\n<td >", 2)[1].split("Grad <abbr title='Celsius'>C</abbr></td>\r\n", 2)[0].strip();
				else
					temp = bit.split("'></td>\r\n<td>", 2)[1].split("Grad <abbr title='Celsius'>C</abbr></td>\r\n", 2)[0].strip();

				icon = icon.replace("/DE/wetter/_functions/piktos/vhs_", "").replace("?__blob=normal", "").strip();

				String fileName = "dwd_" + icon.replaceAll("[ -]", "_");
				String url = "https://www.dwd.de/DE/wetter/_functions/piktos/" + icon + "?__blob=normal";

				day.icon = "file:///android_asset/icons/dwd/" + fileName;

				day.max = temp + "&deg;C";
				day.min = "&deg;C";
				if(!metric)
					day.max = C2Fdeground(str2Float(temp));

				days.add(day);
				lastTS = day.timestamp;
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new Result(days, desc, timestamp, true);
	}

	static Result processAEMET(String data)
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
					day.max = C2Fdeground(str2Float(day.max));
					day.min = C2Fdeground(str2Float(day.min));
				}

				day.text = estado_cielo.getString("descripcion");
				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new Result(days, desc, timestamp, true);
	}

	static Result processWCOM(String data)
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
				day.text = phrase.getString(i);
				day.icon = icons.getString(i);
				day.max = day_temp.getString(i);
				day.min = night_temp.getString(i);

				day.icon = String.format(Locale.US, "%02d", Integer.parseInt(day.icon));
				day.icon = "icons/wcom/" + day.icon + ".svg";
				String content = weeWXApp.loadFileFromAssets(day.icon);
				if(content != null && !content.isBlank())
					day.icon = "file:///android_asset/" + day.icon;
				else
					day.icon = null;

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

		return new Result(days, desc, timestamp, true);
	}

	static Result processMETIE(String data)
	{
		if(data.isBlank())
			return null;

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		long timestamp = getRSSms();
		List<Day> days = new ArrayList<>();
		String desc;

		Date firstTimeFrom = null, secondTimeFrom = null;
		Date secondTimeTo = null;

		String temp = null, rain = null, symbolNo = null;

		float maxTemp = -999.9f;
		float possRainTotal = 0.0f;

		String today = null, tonight = null, tomorrow = null, outlook = null;

		boolean doneToday = false, doneTomorrow = false, doneOutlook = false;

		int firstDay = 0, secondDay = 0;

		Calendar calendar = Calendar.getInstance();
		Calendar UTCcal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

		long now = System.currentTimeMillis();
		String fcText = (String)KeyValue.readVar("textFC", "");
		if(fcText != null && !fcText.isBlank())
		{
			try
			{
				JSONArray jarr = new JSONObject(fcText)
						.getJSONArray("forecasts")
						.getJSONObject(0)
						.getJSONArray("regions");

				for(int i = 0;  i < jarr.length(); i++)
				{
					JSONObject jobj = jarr.getJSONObject(i);

					if(jobj.has("today"))
						today = jobj.getString("today");

					if(jobj.has("tonight"))
						tonight = jobj.getString("tonight");

					if(jobj.has("tomorrow"))
						tomorrow = jobj.getString("tomorrow");

					if(jobj.has("outlook"))
						outlook = jobj.getString("outlook");
				}

				LogMessage("jarr: " + jarr, true, KeyValue.i);
			} catch(JSONException ignored) {}
		}

		try
		{
			desc = (String)KeyValue.readVar("forecastLocationName", "");

			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(new StringReader(data));

			boolean foundProduct = false;
			int eventType = parser.getEventType();
			while(eventType != XmlPullParser.END_DOCUMENT)
			{
				String tag = parser.getName();
				if(tag != null && !tag.isBlank() && tag.equals("product") && eventType == XmlPullParser.START_TAG)
					break;

				eventType = parser.next();
			}

			while(eventType != XmlPullParser.END_DOCUMENT)
			{
				eventType = parser.next();

				if(eventType == XmlPullParser.END_TAG)
					continue;

				String tag = parser.getName();
				if(tag != null && !tag.isBlank())
				{
					if(tag.equals("location") || (tag.equals("time") && eventType == XmlPullParser.END_TAG))
						continue;

					if(tag.equals("time"))
					{
						if(firstTimeFrom == null)
						{
							firstTimeFrom = sdf1.parse(parser.getAttributeValue(null, "from"));
						} else
						{
							secondTimeFrom = sdf1.parse(parser.getAttributeValue(null, "from"));
							secondTimeTo = sdf1.parse(parser.getAttributeValue(null, "to"));
						}
					}

					if(tag.equals("temperature"))
						temp = parser.getAttributeValue(null, "value");

					if(tag.equals("precipitation"))
						rain = parser.getAttributeValue(null, "value");

					if(tag.equals("symbol"))
						symbolNo = parser.getAttributeValue(null, "number");

					if(symbolNo == null)
						continue;

					if(secondTimeFrom.getTime() < now)
					{
						firstTimeFrom = secondTimeFrom = secondTimeTo = null;
						temp = rain = symbolNo = null;

						continue;
					}

					calendar.setTime(secondTimeFrom);
					UTCcal.setTime(secondTimeFrom);

					int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
					int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
					int UTChourOfDay = UTCcal.get(Calendar.HOUR_OF_DAY);
					long utctimestamp = secondTimeFrom.getTime() - 86_400_000L;

					float tmpTemp = str2Float(temp);
					if(maxTemp < tmpTemp)
						maxTemp = tmpTemp;

					possRainTotal += str2Float(rain);

					if(UTChourOfDay != 0)
					{
						LogMessage(UTChourOfDay + " != 0, skipping...");

						firstTimeFrom = secondTimeFrom = secondTimeTo = null;
						temp = rain = symbolNo = null;

						continue;
					}

					LogMessage(UTChourOfDay + " == 0, processing....");

					Day day = new Day();

					LogMessage("secondTimeFrom: " + sdf8.format(secondTimeFrom));
					LogMessage("secondTimeTo: " + sdf8.format(secondTimeTo));

					if(days.isEmpty())
						day.timestamp = now;
					else
						day.timestamp = secondTimeFrom.getTime() - 86_400_000L;

					if(!doneToday)
					{
						doneToday = true;
						if(UTCcal.get(Calendar.HOUR_OF_DAY) >= 17)
							day.text = tonight;
						else
							day.text = today;
						LogMessage("day.text: " + day.text, true, KeyValue.i);
					} else if(!doneTomorrow) {
						doneTomorrow = true;
						day.text = tomorrow;
						LogMessage("Tomorrow: " + tomorrow, true, KeyValue.i);
					} else if(!doneOutlook) {
						doneOutlook = true;
						day.text = outlook;
						LogMessage("Outlook: " + outlook, true, KeyValue.i);
					}

					symbolNo = String.format(Locale.US, "%02d", Integer.parseInt(symbolNo));

					day.icon = "icons/metie/y" + symbolNo + "d.png";
					String content = weeWXApp.loadFileFromAssets(day.icon);
					if(content != null && !content.isBlank())
						day.icon = "file:///android_asset/" + day.icon;
					else
						day.icon = null;

					day.max = maxTemp + "&deg;C";
					if(!metric)
						day.max = C2Fdeg(maxTemp);

					boolean rainInInches = (boolean)KeyValue.readVar("rainInInches", weeWXApp.rain_in_inches_default);
					if(!metric || rainInInches)
						day.min = mm2in(str2Float(rain)) + "in";
					else
						day.min = rain + "mm";

					days.add(day);

					maxTemp = -999.9f;
					possRainTotal = 0.0f;
					firstTimeFrom = secondTimeFrom = secondTimeTo = null;
					temp = rain = symbolNo = null;
				}
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new Result(days, desc, timestamp, true);
	}

	static Result processOWM(String data)
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

				JSONObject temp = j.getJSONObject("temp");
				int min = (int)Math.round(Double.parseDouble(temp.getString("min")));
				int max = (int)Math.round(Double.parseDouble(temp.getString("max")));
				JSONObject weather = j.getJSONArray("weather").getJSONObject(0);

				int id = weather.getInt("id");
				String text = weather.getString("description");
				String icon = weather.getString("icon");

				LogMessage("icon: " + icon);

				day.icon = "icons/owm/" + icon + "_t@4x.png";
				String content = weeWXApp.loadFileFromAssets(day.icon);
				if(content != null && !content.isBlank())
					day.icon = "file:///android_asset/" + day.icon;
				else
					day.icon = null;

				LogMessage("day.icon: " + day.icon);

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

		return new Result(days, desc, timestamp, true);
	}

	static Result processMetNO(String data)
	{
		if(data.isBlank())
			return null;

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		boolean rainInInches = (boolean)KeyValue.readVar("rainInInches", weeWXApp.rain_in_inches_default);
		long timestamp = 0;
		List<Day> days = new ArrayList<>();

		String desc = (String)KeyValue.readVar("forecastLocationName", "");
		if(desc == null || desc.isBlank())
			desc = "";

		int modhour = getIntervalTime()[1];

		try
		{
			JSONObject jobj = new JSONObject(data);
			if(jobj == null)
				return null;

			jobj = jobj.getJSONObject("properties");

			JSONObject meta = jobj.getJSONObject("meta");
			String updated_at = meta.getString("updated_at");
			JSONArray timeseries = jobj.getJSONArray("timeseries");

			Date df = sdf1.parse(updated_at);
			if(df != null)
			{
				timestamp = df.getTime();
				if(timestamp > 0)
					updateCacheTime(timestamp);
			}

			for(int i = 0; i < timeseries.length(); i++)
			{
				JSONObject jobj2 = timeseries.getJSONObject(i);
				String time = jobj2.getString("time");
				boolean isMidnight = time.endsWith("T00:00:00Z");

				Day day = buildMetNODay(jobj2, metric, rainInInches, modhour);
				days.add(day);
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new Result(days, desc, timestamp, false);
	}

	private static Day buildMetNODay(JSONObject jobj2, boolean metric, boolean rainInInches, int modhour)
	{
		try
		{
			Day day = new Day();
			JSONObject tsdata = jobj2.getJSONObject("data");

			String icon = "";
			if(modhour < 24)
			{
				// Prefer shorter interval icons for hourly/sub-daily views
				if(tsdata.has("next_1_hours"))
					icon = tsdata.getJSONObject("next_1_hours").getJSONObject("summary").getString("symbol_code");
				else if(tsdata.has("next_6_hours"))
					icon = tsdata.getJSONObject("next_6_hours").getJSONObject("summary").getString("symbol_code");
				else if(tsdata.has("next_12_hours"))
					icon = tsdata.getJSONObject("next_12_hours").getJSONObject("summary").getString("symbol_code");
			} else {
				// Prefer longer interval icons for daily view
				if(tsdata.has("next_12_hours"))
					icon = tsdata.getJSONObject("next_12_hours").getJSONObject("summary").getString("symbol_code");
				else if(tsdata.has("next_6_hours"))
					icon = tsdata.getJSONObject("next_6_hours").getJSONObject("summary").getString("symbol_code");
				else if(tsdata.has("next_1_hours"))
					icon = tsdata.getJSONObject("next_1_hours").getJSONObject("summary").getString("symbol_code");
			}

			// Temperature from instant data (compact API doesn't have air_temperature_max)
			JSONObject instant = tsdata.getJSONObject("instant").getJSONObject("details");
			if(instant.has("air_temperature"))
			{
				day.max = instant.getDouble("air_temperature") + "&deg;C";
				if(!metric)
					day.max = C2Fdeg(str2Float(day.max));
			}

			// Rain - prefer next_1_hours for short intervals, next_6_hours for daily
			boolean gotRain = false;
			if(modhour < 24 && tsdata.has("next_1_hours"))
			{
				try
				{
					JSONObject details = tsdata.getJSONObject("next_1_hours").getJSONObject("details");
					double precip = details.getDouble("precipitation_amount");
					if(!metric || rainInInches)
						day.min = round(precip / 25.4, 1) + "in";
					else
						day.min = precip + "mm";
					gotRain = true;
				} catch(Exception ignored) {}
			}

			if(!gotRain && tsdata.has("next_6_hours"))
			{
				JSONObject details = tsdata.getJSONObject("next_6_hours")
						.getJSONObject("details");

				try
				{
					double precip = details.getDouble("precipitation_amount");
					if(!metric || rainInInches)
						day.min = round(precip / 25.4, 1) + "in";
					else
						day.min = precip + "mm";
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

			Date df = sdf1.parse(jobj2.getString("time"));
			if(df != null)
				day.timestamp = df.getTime();
			else
				day.timestamp = System.currentTimeMillis();

			day.icon = "icons/metno/" + icon + ".svg";
			String content = weeWXApp.loadFileFromAssets(day.icon);
			if(content != null && !content.isBlank())
				day.icon = "file:///android_asset/" + day.icon;
			else
				day.icon = null;

			return day;
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}
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
			if(dayName.toLowerCase(Locale.ENGLISH).strip().startsWith(string_time.toLowerCase(Locale.ENGLISH)))
				return lastTS;

			lastTS += 86_400_000L;

			if(lastTS > startTS + 86_400_000L)
				return 0;
		}
	}

	static Result processWZ(String url, String data)
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
			for(String i : items)
			{
				Day day = new Day();
				String[] tmp = i.split("</b>", 2);
				Locale locale = new Locale.Builder().setLanguage("en").setRegion("AU").build();
				day.timestamp = convertDaytoTS(tmp[0], locale, lastTS);

				if(tmp.length == 1)
					continue;

				String[] mybits = tmp[1].split("<br />");
				String fileName = mybits[1].strip().replace("<img src='https://www.weatherzone.com.au/images/icons/fcast_30/", "")
						.replace("'>", "").replace(".gif", "");

				fileName = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.length() - 2);
				fileName = JsoupHelper.wzTitle2Filename(url, fileName, null, false);

				if(fileName != null && !fileName.isBlank())
					day.icon = "file:///android_asset/icons/wz/" + fileName;
				else
					day.icon = null;

				String mydesc = mybits[2].strip();
				String[] range = mybits[3].split(" - ", 2);

				day.max = range[1];
				day.min = range[0];

				if(!metric)
				{
					day.max = C2Fdeground(str2Float(day.max)) + "&deg;F";
					day.min = C2Fdeground(str2Float(day.min)) + "&deg;F";
				}

				day.text = mydesc;
				days.add(day);
				lastTS = day.timestamp;
			}
		} catch(Exception e) {
			doStackOutput(e);
			return null;
		}

		return new Result(days, desc, timestamp, false);
	}

	static String convertRGB2Hex(String svg)
	{
		// rgb(var(--uds-spectrum-color-purple-4))
		for(KeyValue.KV kv : KeyValue.yahoo)
		{
			LogMessage("Checking for rgb(var(" + kv.Key + ")) -> " + kv.Val);
			svg = svg.replace("rgb(var(" + kv.Key + "))", kv.Val);
		}

		Pattern p = Pattern.compile("rgb\\(var\\((.*?)\\)\\)");
		Matcher m = p.matcher(svg);
		while(m.find())
			LogMessage("Found unconverted RGB in SVG: " + m.group(1), KeyValue.d);

		return svg;
	}

	static void SendIntent(String action)
	{
		if(action.equals(REFRESH_WEATHER_INTENT))
		{
			long now = System.currentTimeMillis();

			wriCounter++;

			if(wriStart == 0)
				wriStart = now;

			if(wriStart + 5 > now && wriCounter > 5)
				return;

			if(wriStart + 5 < now)
			{
				wriStart = now;
				wriCounter = 0;
			}
		}

		NotificationManager.updateNotificationMessage(action);
	}

	static Boolean passesRegularCheck(boolean forced, boolean has_json_combined)
	{
		if(!forced && !KeyValue.isPrefSet("UpdateFrequency"))
		{
			if(has_json_combined)
			{
				LogMessage("passesRegularCheck() lastJsonDownload is null or blank...", KeyValue.d);
				KeyValue.putVar("LastWeatherError", getAndroidString(R.string.update_set_to_manual_but_no_content_cached));
				return false;
			}

			LogMessage("passesRegularCheck() Not forced and set to manual updates...");
			return true;
		}

		int pos = (int)KeyValue.readVar("UpdateFrequency", weeWXApp.UpdateFrequency_default);
		if(pos < 0 || pos >= weeWXApp.updateOptions.length)
			pos = weeWXApp.UpdateFrequency_default;

		LogMessage("passesRegularCheck() pos: " + pos + ", update interval set to: " +
		           weeWXApp.updateOptions[pos] + ", forced set to: " + forced);

		if(!forced && pos == 0)
		{
			if(has_json_combined)
			{
				LogMessage("passesRegularCheck() lastJsonDownload is null or blank...", KeyValue.d);
				KeyValue.putVar("LastWeatherError", getAndroidString(R.string.update_set_to_manual_but_no_content_cached));
				return false;
			}

			LogMessage("passesRegularCheck() Not forced and set to manual updates...");
			return true;
		}

		NPWSLL npwsll = getNPWSLL();
		if(!forced && npwsll.periodTime <= 0)
		{
			LogMessage("passesRegularCheck() Skipping, period is invalid or set to manual refresh only...", KeyValue.d);

			if(has_json_combined)
			{
				KeyValue.putVar("LastWeatherError", getAndroidString(R.string.update_set_to_manual_but_no_content_cached));
				return false;
			}

			return true;
		}

		if(!forced && npwsll.report_time == 0)
		{
			LogMessage("passesRegularCheck() Skipping, report_time == 0, app hasn't been setup...", KeyValue.d);

			if(has_json_combined)
			{
				KeyValue.putVar("LastWeatherError", getAndroidString(R.string.no_download_or_app_not_setup));
				return false;
			}

			return true;
		}

		LogMessage("passesRegularCheck() Last updated check, is " + npwsll.report_time + " > " + npwsll.lastStart + "?");
		if(!forced && npwsll.report_time > npwsll.lastStart)
		{
			LogMessage("passesRegularCheck() !forced && " + npwsll.report_time + " > " + npwsll.lastStart + "...");
			if(has_json_combined)
			{
				LogMessage("passesRegularCheck() lastJsonDownload != null && !lastJsonDownload.isBlank()... Skipping...", KeyValue.d);
				return true;
			}
		}

		return null;
	}

	static boolean getWeather(boolean forced, boolean calledFromweeWXApp, boolean runningInBG)
	{
		LogMessage("getWeather()...");

		long now = System.currentTimeMillis();

		boolean has_json_combined = false;

		JSONObject jsonObject = getJson();
		if(jsonObject != null && jsonObject.length() != 0)
			has_json_combined = true;

		String[] json_url = new String[json_keys.length];
		for(int i = 0; i < json_keys.length; i++)
		{
			if(!KeyValue.isPrefSet(json_keys[i] + "_url"))
			{
				LogMessage("getWeather() json_keys[" + i + "]_url isn't set...");
				KeyValue.putVar("LastWeatherError", getAndroidString(R.string.data_url_was_blank));
				return false;
			}

			json_url[i] = (String)KeyValue.readVar(json_keys[i] + "_url", "");
			if(json_url[i] == null || json_url[i].isBlank())
			{
				LogMessage("getWeather() json_keys[" + i + "]_url == null || json_keys[" + i + "]_url.isBlank()...");
				KeyValue.putVar("LastWeatherError", getAndroidString(R.string.data_url_was_blank));
				return false;
			}

			if(!forced && !checkConnection())
			{
				if(!has_json_combined)
				{
					LogMessage("getWeather() has_json_combined is false...");
					KeyValue.putVar("LastWeatherError", getAndroidString(R.string.wifi_not_available));
					return false;
				}

				LogMessage("getWeather() Not forced and WiFi needed but not available");
				return true;
			}

			long secDiff = (now - lastUpdateCheck) / 1000;
			if(!forced && secDiff < 30 && secDiff > 0)
			{
				LogMessage("getWeather() !forced && and updated less than 30s ago (" +
				           secDiff + "s)");

				if(!has_json_combined)
				{
					LogMessage("getWeather() has_json_combined is false...");
					KeyValue.putVar("LastWeatherError", getAndroidString(R.string.wifi_not_available));
					return false;
				}

				LogMessage("getWeather() json_str[" + i + "] is not null or blank...");
				return true;
			}
		}

		if(has_json_combined)
		{
			Boolean passes = passesRegularCheck(forced, has_json_combined);
			if(passes != null)
			{
				LogMessage("getWeather() passesRegularCheck(): " + passes);
				return passes;
			}
		}

		if(!forced && !hasBootedFully && !calledFromweeWXApp)
		{
			LogMessage("getWeather() Hasn't booted fully and wasn't called by weeWXApp and wasn't forced, skipping...");
			KeyValue.putVar("LastWeatherError", getAndroidString(R.string.attempting_to_download_data_txt));
			return false;
		}

		if(weatherTask != null && !weatherTask.isDone())
		{
			if((now - wtStart) / 1000 < 30)
			{
				LogMessage("getWeather() weatherTask is less than 30s old (" + (now - wtStart) / 1000 +
				           "s), we'll skip this attempt...");
				return true;
			}

			LogMessage("getWeather() weatherTask was more than 30s old (" + (now - wtStart) / 1000 +
			           "s) cancelling and restarting...");

			weatherTask.cancel(true);
			weatherTask = null;
		}

		LogMessage("getWeather() Creating a weatherTask...");

		lastUpdateCheck = now;
		wtStart = now;

		if(!runningInBG)
		{
			weatherTask = executor.submit(() ->
			{
				handleWeatherUpdate(json_url);
				wtStart = 0;
			});

			return true;
		} else {
			boolean ret = handleWeatherUpdate(json_url);
			wtStart = 0;
			return ret;
		}
	}

	static boolean handleWeatherUpdate(String[] json_url)
	{
		boolean processDicts = false;
		for(int i = 0; i < json_url.length; i++)
		{
			LogMessage("handleWeatherUpdate() Weather checking: " + json_url[i]);

			try
			{
				Boolean ret = reallyGetWeather(i, json_url[i], false);
				if(i == 1 && ret == null)
				{
					processDicts = true;
				} else if(!ret) {
					LogMessage("handleWeatherUpdate() SendIntent(STOP_WEATHER_INTENT)");
					SendIntent(STOP_WEATHER_INTENT);
					return false;
				}
			} catch(Exception e) {
				LogMessage("handleWeatherUpdate() Error! e: " + e, true, KeyValue.e);
				KeyValue.putVar("LastWeatherError", e.getLocalizedMessage());
				SendIntent(STOP_WEATHER_INTENT);
				return false;
			}
		}

		if(processDicts && !KeyValue.parseDicts())
		{
			LogMessage("handleWeatherUpdate() KeyValue.parseDicts() == false");
			return false;
		}

		if(!mergeJsonObjects())
		{
			LogMessage("handleWeatherUpdate() mergeJsonObjects() == false");
			return false;
		}

		LogMessage("handleWeatherUpdate() Update the widget");
		updateAppWidget();

		checkTempAlerts();
		checkRainfallAlert();
		checkRainrateAlert();

		LogMessage("handleWeatherUpdate() SendIntent(REFRESH_WEATHER_INTENT)");
		SendIntent(REFRESH_WEATHER_INTENT);

		return true;
	}

	static boolean mergeJsonObjects()
	{
		JSONObject json_data, json_last, json_combined;

		//LogMessage("mergeJsonObjects() Loading " + json_keys[0] + "_str from SharedPrefs");
		String json_data_str = (String)KeyValue.readVar(json_keys[0] + "_str", "");
		if(json_data_str == null || json_data_str.isBlank())
		{
			LogMessage("mergeJsonObjects() json_data_str == null || json_data_str.isBlank()");
			return false;
		}

		try
		{
			json_data = new JSONObject(json_data_str);
		} catch(JSONException je) {
			LogMessage("mergeJsonObjects() Failed turing json_data_str into json_data, je: " + je.getMessage());
			return false;
		}

		if(json_data == null || json_data.length() == 0)
		{
			LogMessage("mergeJsonObjects() json_data == null || json_data.length() == 0");
			return false;
		}

		//LogMessage("mergeJsonObjects() Loading " + json_keys[2] + "_str from SharedPrefs");
		String json_last_str = (String)KeyValue.readVar(json_keys[2] + "_str", "");
		if(json_last_str == null || json_last_str.isBlank())
		{
			LogMessage("mergeJsonObjects() json_last_str == null || json_last_str.isBlank()");
			return false;
		}

		try
		{
			json_last = new JSONObject(json_last_str);
		} catch(JSONException je) {
			LogMessage("mergeJsonObjects() Failed turing json_last_str into json_last, je: " + je.getMessage());
			return false;
		}

		if(json_last == null || json_last.length() == 0)
		{
			LogMessage("mergeJsonObjects() json_last == null || json_last.length() == 0");
			return false;
		}

		try
		{
		    json_combined = json_last;
		    Iterator<String> keys = json_data.keys();
		    while(keys.hasNext())
			{
		        String key = keys.next();
				Object value = json_data.get(key);
				//LogMessage("mergeJsonObjects() putting key: " + key + ", value: " + value);
		        json_combined.put(key, value);
		    }
		} catch(JSONException je) {
			LogMessage("mergeJsonObjects() Failed to merge json_data with json_last, je: + " + je.getMessage());
			return false;
		}

		if(json_combined == null || json_combined.length() == 0)
		{
			LogMessage("mergeJsonObjects() json_combined == null || json_combined.length() == 0");
			return false;
		}

		String json_combined_str = json_combined.toString();

		//LogMessage("mergeJsonObjects() json_combined_str: " + json_combined_str);
		KeyValue.putVar("json_combined_str", json_combined_str);

		return true;
	}

	static void checkRainfallAlert()
	{
		boolean rainfall_alert = (boolean)KeyValue.readVar("rainfall_alert", weeWXApp.rainfall_alert_default);

		Calendar cal = Calendar.getInstance();
		Calendar cal1 = Calendar.getInstance();

		if(!rainfall_alert)
		{
			LogMessage("checkRainfallAlert() rainfall_alert set to false");
			return;
		}

		LogMessage("checkRainfallAlert() rainfall_alert set to true");

		long last_rainfall_alert = (long)KeyValue.readVar("LastRainfallAlert", 0L);
		cal1.setTimeInMillis(last_rainfall_alert);

		if(cal.get(Calendar.YEAR) == cal1.get(Calendar.YEAR) &&
		   cal.get(Calendar.DAY_OF_YEAR) == cal1.get(Calendar.DAY_OF_YEAR))
		{
			LogMessage("checkRainfallAlert() Rainfall notification already triggered today");
			return;
		}

		float rainfall = (float)getJson("day_rain", 0f);

		int since_hour = (int)getJson("since_hour", 0);
		if(since_hour < 0 || since_hour > 23)
			since_hour = 0;

		KeyValue.since_hour = since_hour;

		if(since_hour > 0)
			rainfall = (float)getJson("since_rain_today", 0f);

		float rainfall_limit = (int)KeyValue.readVar("RainfallLimit", weeWXApp.RainfallLimit_default) / 100f;

		if(rainfall >= rainfall_limit)
		{
			KeyValue.putVar("LastRainfallAlert", System.currentTimeMillis());
			weeWXApp.sendRainfallAlert(rainfall, rainfall_limit);
			LogMessage("checkRainfallAlert() rainfall (" + rainfall + ") >= rainfall_limit (" + rainfall_limit + ") notification triggered");
		} else {
			LogMessage("checkRainfallAlert() rainfall (" + rainfall + ") < rainfall_limit (" + rainfall_limit + ") no notification triggered");
		}
	}

	static void checkRainrateAlert()
	{
		boolean[] rainrate_alerts = {
			(boolean)KeyValue.readVar("rainrate_alert_watch", weeWXApp.rainrate_alert_watch_default),
			(boolean)KeyValue.readVar("rainrate_alert_warning", weeWXApp.rainrate_alert_warning_default),
			(boolean)KeyValue.readVar("rainrate_alert_severe", weeWXApp.rainrate_alert_severe_default),
		};

		if(!rainrate_alerts[0] && !rainrate_alerts[1] && !rainrate_alerts[2])
		{
			LogMessage("checkRainrateAlert() rainrate_alert set to false");
			return;
		}

		LogMessage("checkRainrateAlert() At least one rainrate alert is true");

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default) &&
		                 !(boolean)KeyValue.readVar("rainInInches", weeWXApp.rain_in_inches_default);

		float rainrate = (float)getJson("rainRate", 0f);

		int[] totals = {
			Math.round((float)getJson("rain_600", 0f) * 100),
			Math.round((float)getJson("rain_1800", 0f) * 100),
			Math.round((float)getJson("rain_3600", 0f) * 100),
			Math.round((float)getJson("rain_21600", 0f) * 100),
			Math.round((float)getJson("rain_86400", 0f) * 100),
		};

		long now = System.currentTimeMillis();
		long last_rainrate_alert = (long)KeyValue.readVar("LastRainrateAlert", 0L);
		int last_rainrate_level = (int)KeyValue.readVar("LastRainrateLevel", -1);

		String unit = "mm";
		int[][] FLOOD_THRESHOLDS = FLOOD_THRESHOLDS_MM;
		if(!metric)
		{
			unit = "in";
			FLOOD_THRESHOLDS = FLOOD_THRESHOLDS_IN;
		}

		int[] alert = getAlertLevel(totals, FLOOD_THRESHOLDS, rainrate_alerts);
		int level = alert[0];
		int period = alert[1];

		if(level < 0 || period < 0)
		{
			LogMessage("checkRainrateAlert() No current rainrate notification needed");
			return;
		}

		String debugunit = "minutes";
		String timelen_unit = getAndroidString(R.string.minutes);
		int timelen = warning_delays[period] % 60;
		if(warning_delays[period] == 3_600)
		{
			debugunit = "hour";
			timelen_unit = getAndroidString(R.string.hour);
			timelen = 1;
		} else if(warning_delays[period] > 3_600) {
			debugunit = "hours";
			timelen_unit = getAndroidString(R.string.hours);
			timelen = warning_delays[period] % 3_600;
		}

		int time_ago = Math.round((now - last_rainrate_alert) / 1000f);

		if(time_ago >= warning_delays[period] || level > last_rainrate_level)
		{
			KeyValue.putVar("LastRainrateAlert", now);
			KeyValue.putVar("LastRainrateLevel", alert[0]);
			weeWXApp.sendRainrateAlert(totals[period], level, timelen, timelen_unit, metric);
			LogMessage("checkRainrateAlert() rainfall (" + totals[period] + unit +
			           ") >= rainfall_limit (" + FLOOD_THRESHOLDS[period][level] + unit + ") " +
			           "fell in " + timelen + " " + debugunit + " so notification triggered");
		} else {
			LogMessage("checkRainrateAlert() No threshold reached, so no notification triggered");
		}
	}

	private static int[] getAlertLevel(int[] totals, int[][] thresholds, boolean[] rainrate_alerts)
	{
		for(int level = LEVEL_SEVERE; level >= LEVEL_WATCH; level--)
		{
			if(!rainrate_alerts[level])
				continue;

			for(int period = 0; period < totals.length; period++)
			{
				if(totals[period] >= thresholds[period][level])
				{
					return new int[]{level, period};
				}
			}
		}

		return new int[]{-1, -1}; // no alert
	}

	static void checkTempAlerts()
	{
		boolean morning_temp_alert = (boolean)KeyValue.readVar("morning_temp_alert", weeWXApp.morning_temp_alert_default);

		Calendar cal = Calendar.getInstance();
		Calendar cal1 = Calendar.getInstance();

		TempResult temps = getTempResult();

		// Don't trigger temp alerts before 6am
		if(cal.get(Calendar.HOUR_OF_DAY) < 6)
			return;

		if(cal.get(Calendar.HOUR_OF_DAY) < 12 && morning_temp_alert)
		{
			LogMessage("checkTempAlerts() morning_temp_alert set to true");

			long last_morning_alert = (long)KeyValue.readVar("LastMorningTempAlert", 0L);
			cal1.setTimeInMillis(last_morning_alert);

			if(cal.get(Calendar.YEAR) == cal1.get(Calendar.YEAR) &&
			   cal.get(Calendar.DAY_OF_YEAR) == cal1.get(Calendar.DAY_OF_YEAR))
			{
				LogMessage("checkTempAlerts() Notification already triggered this morning");
				return;
			}

			float morning_temp_limit = (int)KeyValue.readVar("MorningTemp", weeWXApp.MorningTemp_default) / 10f;

			float minObservedTemp = Math.min(temps.minObservedTemp, temps.CurrTemp);

			LogMessage("checkTempAlerts() minForecastTemp: " + minObservedTemp);

			boolean hasBottomedOut = temps.CurrTemp > minObservedTemp;

			LogMessage("checkTempAlerts() hasBottomedOut: " + hasBottomedOut);

			boolean hasWarmedUp = hasBottomedOut && temps.CurrTemp >= morning_temp_limit && minObservedTemp < morning_temp_limit;

			LogMessage("checkTempAlerts() hasWarmedUp: " + hasWarmedUp);

			if(hasWarmedUp)
			{
				KeyValue.putVar("LastMorningTempAlert", System.currentTimeMillis());
				weeWXApp.sendTemperatureAlert(temps.CurrTemp, morning_temp_limit, false);
				LogMessage("checkTempAlerts() CurrTemp (" + temps.CurrTemp + ") >= morning_temp_limit (" +
				           morning_temp_limit + ") notification triggered");
			} else {
				LogMessage("checkTempAlerts() CurrTemp (" + temps.CurrTemp + ") < morning_temp_limit (" +
				           morning_temp_limit + ") no notification triggered");
			}

			return;
		}

		if(cal.get(Calendar.HOUR_OF_DAY) < 12)
		{
			LogMessage("checkTempAlerts() morning_temp_alert set to false");
			return;
		}

		boolean afternoon_temp_alert = (boolean)KeyValue.readVar("afternoon_temp_alert", weeWXApp.afternoon_temp_alert_default);

		if(afternoon_temp_alert)
		{
			LogMessage("checkTempAlerts() afternoon_temp_alert set to true");

			long last_afternoon_alert = (long)KeyValue.readVar("LastAfternoonTempAlert", 0L);
			cal1.setTimeInMillis(last_afternoon_alert);

			if(cal.get(Calendar.YEAR) == cal1.get(Calendar.YEAR) &&
			   cal.get(Calendar.DAY_OF_YEAR) == cal1.get(Calendar.DAY_OF_YEAR))
			{
				LogMessage("checkTempAlerts() Notification already triggered this afternoon");
				return;
			}

			float afternoon_temp_limit = (int)KeyValue.readVar("AfternoonTemp", weeWXApp.AfternoonTemp_default) / 10f;

			float maxObservedTemp = Math.max(temps.CurrTemp, temps.maxObservedTemp);

			for(int i = 0; i < temps.outTemp_trend_ts.length; i++)
			{
				LogMessage("checkTempAlerts() outTemp_trend_ts[" + i + "]: " + sdf10.format(new Date(temps.outTemp_trend_ts[i])));
				LogMessage("checkTempAlerts() outTemp_trend_signal[" + i + "]: " + temps.outTemp_trend_signal[i]);
				LogMessage("checkTempAlerts() outTemp_trend_count[" + i + "]: " + temps.outTemp_trend_count[i]);
			}

			if(temps.outTemp_trend_signal.length == 0)
			{
				LogMessage("checkTempAlerts() temps.outTemp_trend_signal.length == 0, skipping temp limit checks...");
				return;
			}

			long last_ts = temps.outTemp_trend_ts[0];
			int last_signal = temps.outTemp_trend_signal[0];
			int last_count = temps.outTemp_trend_count[0];

			boolean hasPeaked = (last_signal == -1 || cal.get(Calendar.HOUR_OF_DAY) >= 16) && temps.CurrTemp < maxObservedTemp;

			LogMessage("checkTempAlerts() hasPeaked: " + hasPeaked);

			boolean hasCooledOffEnough = hasPeaked && temps.CurrTemp <= afternoon_temp_limit && maxObservedTemp > afternoon_temp_limit;

			LogMessage("checkTempAlerts() hasCooledOffEnough: " + hasCooledOffEnough);

			if(hasCooledOffEnough)
			{
				KeyValue.putVar("LastAfternoonTempAlert", System.currentTimeMillis());
				weeWXApp.sendTemperatureAlert(temps.CurrTemp, afternoon_temp_limit, true);
				LogMessage("checkTempAlerts() CurrTemp (" + temps.CurrTemp + ") <= afternoon_temp_limit (" +
				           afternoon_temp_limit + ") notification triggered");
			} else {
				LogMessage("checkTempAlerts() CurrTemp (" + temps.CurrTemp + ") > afternoon_temp_limit (" +
				           afternoon_temp_limit + ") no notification triggered");
			}
		} else {
			LogMessage("checkTempAlerts() afternoon_temp_alert set to false");
		}
	}

	static TempResult getTempResult()
	{
		int[] emptyIntArr = new int[]{};
		long[] emptyLongArr = new long[]{};

		return new TempResult(
				(float)getJson("current_outTemp", 0f),
				(float)getJson("day_outTemp_min", 0f),
				(float)getJson("day_outTemp_max", 0f),
				(int[])getJson("outTemp_trend_count", emptyIntArr),
				(int[])getJson("outTemp_trend_signal", emptyIntArr),
				(long[])getJson("outTemp_trend_ts", emptyLongArr, true));
	}

	static Object getElement(String element, JSONObject jsonObject, Object defaultValue, boolean timeStamps)
	{
		if(jsonObject == null || jsonObject.length() == 0)
			return defaultValue;

		switch(defaultValue)
		{
			case Boolean b ->
			{
				return jsonObject.optBoolean(element, b);
			}
			case Double d ->
			{
				return jsonObject.optDouble(element, d);
			}
			case Float f ->
			{
				return (float)jsonObject.optDouble(element, f);
			}
			case Integer i ->
			{
				return jsonObject.optInt(element, i);
			}
			case int[] iarr ->
			{
				if(!jsonObject.has(element))
					return iarr;

				JSONArray temparr = jsonObject.optJSONArray(element);
				if(temparr == null || temparr.length() == 0)
					return iarr;

				int[] numbers = new int[temparr.length()];
				for(int i = 0; i < temparr.length(); ++i)
					numbers[i] = temparr.optInt(i);

				return numbers;
			}
			case Long l ->
			{
				return jsonObject.optLong(element, l);
			}
			case long[] larr ->
			{
				long multiplier = 1L;
				if(timeStamps)
					multiplier = 1_000L;

				if(!jsonObject.has(element))
					return larr;

				JSONArray temparr = jsonObject.optJSONArray(element);
				if(temparr == null || temparr.length() == 0)
					return larr;

				long[] numbers = new long[temparr.length()];
				for(int i = 0; i < temparr.length(); ++i)
					numbers[i] = temparr.optLong(i) * multiplier;

				return numbers;
			}
			case String s ->
			{
				return jsonObject.optString(element, s).strip();
			}

			default -> LogMessage("Unknown Object type " + defaultValue.getClass());
		}

		return defaultValue;
	}

	static String getDateTimeStr(long when, int timeMode)
	{
		String dateTimeStr = "";

		if(timeMode == 0)
			dateTimeStr = getHourMin(when);
		else if(timeMode == 1)
			dateTimeStr = getTimeMonth(when);
		else if(timeMode == 2)
			dateTimeStr = getTimeYear(when);
		else if(timeMode == 3)
			dateTimeStr = getAllTime(when);
		else if(timeMode == 4)
			dateTimeStr = getHourMinNext(when);

		return dateTimeStr;
	}

	static String getHourMin(long when)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
		return sdf.format(when);
	}

	static String getHourMinNext(long when)
	{
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal2.setTimeInMillis(when);

		String str = sdf20.format(when);

		if(cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR) ||
		   cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR))
			str += "+1";

		return str;
	}

	static String getOrdinal(int day)
	{
		Object[] testArgs = {day};
		MessageFormat mf = new MessageFormat("{0,ordinal}", Locale.getDefault());
		return mf.format(testArgs);
	}

	static String getTimeMonth(long when)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("d", Locale.getDefault());
		int day = str2Int(sdf.format(when));
		return getOrdinal(day);
	}

	static String widgetTime(long when)
	{
		return sdf19.format(new Date(when)) + " " + getTimeMonth(when) + " " + getShortMonth(when);
	}

	static String getShortMonth(long when)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("MMM", Locale.getDefault());
		String mon = sdf.format(when);
		if(mon.length() > 3)
			mon = mon.substring(0, 3);

		return mon;
	}

	static String getTimeYear(long when)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("d", Locale.getDefault());
		return sdf.format(when) + " " + getShortMonth(when);
	}

	static String getAllTime(long when)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("MMM", Locale.getDefault());
		String mon = sdf.format(when);
		if(mon.length() > 3)
			mon = mon.substring(0, 3);

		sdf = new SimpleDateFormat("yy", Locale.getDefault());
		return mon + " '" + sdf.format(when);
	}

	static String formatString(String element)
	{
		String key = KeyValue.getKeyFromName(element);
		if(key == null || key.isBlank())
		{
			LogMessage("element '" + element + "' returned no key");
			return null;
		}

		float f = (float)getJson(element, 0f);

		String fmt = KeyValue.getFormat(key);
		if(fmt == null || fmt.isBlank())
		{
			LogMessage("fmt was null or blank");
			LogMessage("element: " + element);
			LogMessage("f: " + f);
			LogMessage("key: " + key);
			return null;
		}

		String fmt_str = String.format(fmt, f);
		if(fmt_str == null || fmt_str.isBlank())
		{
			LogMessage("fmt_str was null or blank");
			LogMessage("element: " + element);
			LogMessage("f: " + f);
			LogMessage("key: " + key);
			LogMessage("fmt: " + fmt);
			return null;
		}

//		if(element.toLowerCase(Locale.ENGLISH).contains("_intemp_"))
//		{
//			LogMessage("element: " + element);
//			LogMessage("f: " + f);
//			LogMessage("key: " + key);
//			LogMessage("fmt: " + fmt);
//			LogMessage("fmt_str: " + fmt_str);
//		}

		return fmt_str;
	}

	static String getSinceHour(int since_hour, int since_str)
	{
		if(since_hour == 0)
			return getAndroidString(since_str) + " mn";

		SimpleDateFormat sdf = new SimpleDateFormat("ha", Locale.getDefault());
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, since_hour);
		return getAndroidString(since_str) + " " + sdf.format(cal.getTimeInMillis());
	}

	static boolean hasElement(String element)
	{
		JSONObject jsonObject = getJson();
		if(jsonObject == null || jsonObject.length() == 0)
			return false;

		return jsonObject.has(element);
	}

	static Object getJson(String element, Object defaultValue)
	{
		return getJson(element, defaultValue, false);
	}

	static Object getJson(String element, Object defaultValue, boolean timestamps)
	{
		JSONObject jsonObject = getJson();
		return getElement(element, jsonObject, defaultValue, timestamps);
	}

	static JSONObject getJson()
	{
		if(!KeyValue.isPrefSet("json_combined_str"))
			return null;

		String line = (String)KeyValue.readVar("json_combined_str", "");
		if(line != null && !line.isBlank())
		{
			try
			{
				return new JSONObject(line);
			} catch(Exception ignored) {}
		}

		return null;
	}

	static boolean is_valid_url(String url)
	{
		return url != null && !url.isBlank() && (url.startsWith("http://") || url.startsWith("https://"));
	}

	static Boolean reallyGetWeather(int id, String url, boolean force) throws InterruptedException, IOException
	{
		LogMessage("reallyGetWeather(" + id + ") url: " + url);

		long now = System.currentTimeMillis();

		long lastJsonDataDownloadAttempt = (long)KeyValue.readVar("lastJsonDataDownloadAttempt_" + id, 0L);

		if(lastJsonDataDownloadAttempt + 10_000L > now)
		{
			LogMessage("reallyGetWeather() lastJsonDataDownloadAttempt_" + id + " (" +
			           lastJsonDataDownloadAttempt + ") + 10_000L > now (" + now + ")");
			return false;
		}

		KeyValue.putVar("lastJsonDataDownloadAttempt_" + id, now);

		if(!force)
		{
			long json_time = (long)KeyValue.readVar(json_keys[id] + "_time", 0L);
			if(id == 1 && json_time > 0)
				return true;

			if(id == 2 && json_time > 0)
			{
				Calendar cal1 = Calendar.getInstance();
				Calendar cal2 = Calendar.getInstance();
				cal2.setTimeInMillis(json_time);

				if(cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
				   cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR))
					return true;
			}
		}

		String line = downloadString(url, true);
		if(line == null || line.isBlank())
			return false;

		JSONObject jsonObject;
		try
		{
			jsonObject = new JSONObject(line);
		} catch(Exception e) {
			LogMessage("reallyGetWeather() Error! e: " + e.getMessage());
			return false;
		}

		if(jsonObject == null || jsonObject.length() == 0 || !jsonObject.has("version"))
		{
			LogMessage("reallyGetWeather() jsonObject == null || jsonObject.length() == 0 || jsonObject didn't have a version");
			return false;
		}

		int version = jsonObject.optInt("version", 0);
		if(version < weeWXApp.minimum_inigo_version)
		{
			LogMessage("reallyGetWeather() sendAlert() triggered because version (" + version +
			           ") < weeWXApp.minimum_inigo_version (" + weeWXApp.minimum_inigo_version + ")");
			sendAlert();
			return false;
		}

		if(jsonObject.has("processingErrors"))
		{
			JSONArray jarr = jsonObject.optJSONArray("processingErrors");
			if(jarr != null && jarr.length() > 0)
			{
				KeyValue.putVar("ProcessingErrorCount", jarr.length());
				KeyValue.putVar("ProcessingErrorID", id);

				for(int i = 0; i < jarr.length(); i++)
					LogMessage("reallyGetWeather() Error in " + json_labels[id] + ": " + jarr.optString(i), KeyValue.e);

				SendIntent(PROCESSING_ERRORS);
				return false;
			}
		}


		KeyValue.putVar(json_keys[id] + "_time", now);
		KeyValue.putVar(json_keys[id] + "_str", jsonObject.toString());
		KeyValue.putVar("LastWeatherError", null);

		LogMessage("reallyGetWeather() Last Server Update Time: " + sdf14.format(jsonObject.optInt("report_time") * 1_000L));
		LogMessage("reallyGetWeather() LastDownloadTime: " + sdf14.format(now));

		if(!force && id == 1)
			return null;

		return true;
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

		String cfg = downloadString(url, true);

		if(cfg == null)
			return null;

		if(cfg.startsWith(UTF8_BOM))
			cfg = cfg.substring(1).strip();

		return cfg;
	}

	static boolean checkURL(String url, boolean nocache) throws InterruptedException, IOException
	{
		if(!is_valid_url(url))
			return false;

		OkHttpClient client = NetworkClient.getInstance(url, nocache);

		return reallyCheckURL(client, url, 0);
	}

	private static boolean reallyCheckURL(OkHttpClient client, String url, int retries) throws InterruptedException, IOException
	{
		LogMessage("reallyCheckURL() checking if url  " + url + " is valid, attempt " + (retries + 1));

		Request request = NetworkClient.getRequest(true, url, false);

		try(Response response = client.newCall(request).execute())
		{
			return response.isSuccessful();
		} catch(IOException ioe) {
			if(retries < maximum_retries)
			{
				retries++;

//				LogMessage("reallyCheckURL() Error! ioe: " + ioe.getMessage() + ", retry: " + retries +
//				           ", will sleep " + retry_sleep_time + " seconds and retry...", true);

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

//			LogMessage("reallyCheckURL() Error! e: " + ioe.getMessage(), true, KeyValue.e);

			doStackOutput(ioe);

			throw ioe;
		}
	}

	static String downloadString(String url, boolean nocache) throws InterruptedException, IOException
	{
		if(!is_valid_url(url))
			return null;

		OkHttpClient client = NetworkClient.getInstance(url, nocache);

		return reallyDownloadString(client, url, 0, nocache);
	}

	private static String reallyDownloadString(OkHttpClient client, String url, int retries, boolean nocache) throws InterruptedException, IOException
	{
		LogMessage("reallyDownloadString() checking if url  " + url + " is valid, attempt " + (retries + 1));
		Request request = NetworkClient.getRequest(false, url, nocache);

		if(request == null)
			throw new IOException("Failed to build request for URL: " + url);

		try(Response response = client.newCall(request).execute())
		{
			String bodyStr = response.body().string();

			LogMessage("response: " + response);
			//LogMessage("Returned string: " + bodyStr);
			//LogMessage("response.code(): " + response.code());

			if(!response.isSuccessful())
			{
				String error = "HTTP error " + response;
				if(bodyStr != null && !bodyStr.isBlank())
					error += ", body: " + bodyStr;
				LogMessage("reallyDownloadString() Error! error: " + error, true, KeyValue.w);
				throw new IOException(error);
			}

			return bodyStr;
		} catch(Exception e) {
			doStackOutput(e);

			if(false)
			{
				if(retries < maximum_retries)
				{
					retries++;

	//				LogMessage("reallyDownloadString() Error! e: " + e.getMessage() + ", retry: " + retries +
	//				           ", will sleep " + Math.round(retry_sleep_time / 1_000D) + " seconds and retry...", true);

					try
					{
						Thread.sleep(retry_sleep_time);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						LogMessage("reallyDownloadString() Error! ie: " + ie.getMessage(), true, KeyValue.e);
						throw ie;
					}

					return reallyDownloadString(client, url, retries, nocache);
				}

	//			LogMessage("reallyDownloadString() Error! e: " + e.getMessage(), true, KeyValue.e);

				doStackOutput(e);
			}

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

		fb.add("appName", weeWXApp.APPLICATION_ID);
		fb.add("appVersion", weeWXApp.VERSION_NAME);

		RequestBody requestBody = fb.build();

		OkHttpClient client = NetworkClient.getInstance(url, true);

		return reallyDownloadString(client, requestBody, url, 0, true);
	}

	private static String reallyDownloadString(OkHttpClient client, RequestBody requestBody,
                       String url, int retries, boolean noCache) throws InterruptedException, IOException
	{
		LogMessage("reallyDownloadString() checking if url  " + url + " is valid, attempt " + (retries + 1));

		Request request = NetworkClient.getRequest(false, url, noCache)
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

			if(false)
			{
				if(retries < maximum_retries)
				{
					retries++;

	//				LogMessage("reallyDownloadString() Error! e: " + e.getMessage() + ", retry: " + retries +
	//				           ", will sleep " + retry_sleep_time + " seconds and retry...", true);

					try
					{
						Thread.sleep(retry_sleep_time);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw ie;
					}

					return reallyDownloadString(client, requestBody, url, retries, noCache);
				}
			}

			//LogMessage("reallyDownloadString(url, args) Error! e: " + e.getMessage(), true, KeyValue.e);

			doStackOutput(e);
			throw e;
		}

		return null;
	}

	static void uploadMissingIcon(Map<String,String> args)
	{
		String url = weeWXApp.missingIconURL;

		FormBody.Builder fb = new FormBody.Builder();

		LogMessage("uploadMissingIcon() Adding appName and appVersion to FormBody");

		fb.add("appName", weeWXApp.APPLICATION_ID);
		fb.add("appVersion", weeWXApp.VERSION_NAME);

		for(Map.Entry<String, String> arg : args.entrySet())
		{
			LogMessage("uploadMissingIcon() Adding " + arg.getKey() + "=" + arg.getValue() + " to fb...");

			if(arg.getKey().equals("svgName"))
				if(processedMissingIcons.contains(arg.getValue()))
					return;
				else
					processedMissingIcons.add(arg.getValue());

			if(!arg.getKey().equalsIgnoreCase("appName") &&
			   !arg.getKey().equalsIgnoreCase("appVersion"))
				fb.add(arg.getKey(), arg.getValue());
		}

		executor.submit(() ->
		{
			RequestBody requestBody = fb.build();

			OkHttpClient client = NetworkClient.getInstance(url, false);

			LogMessage("uploadMissingIcon() Sending everything to reallyUploadMissingIcon()");

			reallyUploadMissingIcon(client, requestBody, url, 0);
		});
	}

	private static void reallyUploadMissingIcon(OkHttpClient client, RequestBody requestBody, String url, int retries)
	{
		Exception lastException = null;

		LogMessage("reallyUploadString() checking if url " + url + " is valid, attempt: #" + (retries + 1));

		Request request = NetworkClient.getRequest(false, url, false)
				.newBuilder().post(requestBody).build();

		try(Response response = client.newCall(request).execute())
		{
			String bodyStr = response.body().string();
			if(response.isSuccessful())
			{
				if(bodyStr != null && bodyStr.equals("OK"))
				{
					LogMessage("reallyUploadString() Map uploaded successfully...");
					return;
				}

				LogMessage("reallyUploadString() Error from server: " + bodyStr + ", retry: " + retries +
				           ", will sleep " + retry_sleep_time + " seconds and retry...", true, KeyValue.w);
			} else
				LogMessage("reallyUploadString() Failed to upload something... response code: " + response.code() +
				           ", body: " + bodyStr, true, KeyValue.w);
		} catch(Exception e) {
			doStackOutput(e);
//			lastException = e;
		}

		if(lastException != null)
		{
			if(retries < maximum_retries)
			{
				retries++;

				if(lastException != null)
					LogMessage("reallyUploadString() Error! lastException: " + lastException.getMessage() + ", retry: #" + retries +
					           ", will sleep " + Math.round(retry_sleep_time / 1_000D) + "s and then retry...", true, KeyValue.w);

				try
				{
					Thread.sleep(retry_sleep_time);
				} catch (InterruptedException ie) {
					LogMessage("reallyUploadString() Error! ie: " + ie.getMessage(), true, KeyValue.e);
					Thread.currentThread().interrupt();
					return;
				}

				reallyUploadMissingIcon(client, requestBody, url, retries);
				return;
			}
		}

		if(lastException != null)
		{
			LogMessage("reallyUploadString() Error! lastException: " + lastException.getMessage(), true, KeyValue.e);
			doStackOutput(lastException);
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

	static boolean getForecast(boolean forced, boolean calledFromweeWXApp, boolean runningInBG)
	{
		String fctype = (String)KeyValue.readVar("fctype", "");

		if(fctype == null || fctype.isBlank())
		{
			LogMessage("getForecast() fctype == null || fctype.isBlank(), skipping...", KeyValue.d);
			String fmtErr = String.format(getAndroidString(R.string.forecast_type_is_invalid), fctype);
			KeyValue.putVar("LastForecastError", fmtErr);
			return false;
		}

		ForecastDefaults fcDef = weeWXApp.getFCdefs(fctype);
		if(fcDef == null)
		{
			LogMessage("UpdateCheck.java Unable to get forecast defaults for fctype: " + fctype, KeyValue.w);
			KeyValue.putVar("LastForecastError", "Unable to get forecast defaults for fctype: " + fctype);
			return false;
		}

		LogMessage("getForecast() fctype: " + fctype);

		if(fctype.equals("weatherzone3") || fctype.equals("metservice2"))
		{
			LogMessage("getForecast() fctype == weatherzone3 || metservice2, skipping...", KeyValue.d);
			KeyValue.putVar("LastForecastError", getAndroidString(R.string.forecast_type_is_invalid));
			return false;
		}

		String forecast_url = (String)KeyValue.readVar("FORECAST_URL", "");
		if(forecast_url == null || forecast_url.isBlank())
		{
			LogMessage("getForecast() FORECAST_URL == null || isBlank(), skipping...", KeyValue.w);
			KeyValue.putVar("LastForecastError", getAndroidString(R.string.forecast_url_not_set));
			return false;
		}

		String forecastGson = (String)KeyValue.readVar("forecastGsonEncoded", "");
		boolean hasForecastGson = forecastGson != null && forecastGson.length() > 128;
		if(hasForecastGson)
			LogMessage("forecastGson.length(): " + forecastGson.length());

		if(!checkConnection() && !forced)
		{
			LogMessage("getForecast() Not on wifi and not a forced refresh, skipping...", KeyValue.d);
			if(!hasForecastGson)
			{
				LogMessage("getForecast() hasForecastGson is false, skipping...");
				KeyValue.putVar("LastForecastError", getAndroidString(R.string.wifi_not_available));
				return false;
			}

			LogMessage("getForecast() hasForecastGson is true, skipping...");
			return true;
		}

		int pos = (int)KeyValue.readVar("UpdateFrequency", weeWXApp.UpdateFrequency_default);
		if(!forced && pos == 0)
		{
			LogMessage("getForecast() Set to manual update and not forced...", KeyValue.d);

			if(!hasForecastGson)
			{
				LogMessage("getForecast() hasForecastGson is false, skipping...");
				KeyValue.putVar("LastForecastError", getAndroidString(R.string.update_set_to_manual_but_no_content_cached));
				return false;
			}

			LogMessage("getForecast() hasForecastGson is true, skipping...");
			return true;
		}

		long now = System.currentTimeMillis();
		LogMessage("getForecast() current_time: " + now);

		long rssCheckTime = getRSSms();
		LogMessage("getForecast() rssCheckTime: " + rssCheckTime);

		long lastAttemptedForecastDownload = getLAFDms();

		if(rssCheckTime == 0)
		{
			LogMessage("getForecast() Bad rssCheckTime, skipping...", KeyValue.d);

			if(!hasForecastGson)
			{
				LogMessage("getForecast() hasForecastGson is false, skipping...");
				KeyValue.putVar("LastForecastError", getAndroidString(R.string.still_downloading_forecast_data));
				return false;
			}

			LogMessage("getForecast() hasForecastGson is true, skipping...");
			return true;
		}

		long dur = (now - lastAttemptedForecastDownload) / 1000;
		if(!forced && lastAttemptedForecastDownload > 0 && dur < fcDef.delay_before_downloading)
		{
			LogMessage("getForecast() !forced and last attempt was less than " + fcDef.delay_before_downloading +
			           "s ago (" + dur + "s ago)");
			if(hasForecastGson)
				return true;

			LogMessage("getForecast() hasForecastGson is false, skipping...");
			KeyValue.putVar("LastForecastError", getAndroidString(R.string.still_downloading_forecast_data));
			return false;
		}

		dur = (now - getRSSms()) / 1000;
		if(!forced && hasForecastGson && dur < fcDef.default_forecast_refresh)
		{
			LogMessage("getForecast() !forced and hasForecastGson and cache isn't more than " +
			           fcDef.default_forecast_refresh + "s old (" + dur + "s ago), skipping...");
			return true;
		}

		if(!hasBootedFully && !calledFromweeWXApp && !forced)
		{
			LogMessage("getForecast() not fully booted and not called from weeWXApp.class and not forced...", KeyValue.d);

			if(hasForecastGson)
				return true;

			LogMessage("getForecast() hasForecastGson is false, skipping...");
			KeyValue.putVar("LastForecastError", getAndroidString(R.string.still_downloading_forecast_data));
			return false;
		}

		int wait_time = fcDef.default_wait_before_killing_executor;

		dur = (now - ftStart) / 1000;
		if(forecastTask != null && !forecastTask.isDone())
		{
			if(dur < wait_time)
			{
				LogMessage("getForecast() forecastTask is less than " + wait_time + "s old (" +
				           dur + "s), we'll skip this attempt...", KeyValue.d);

				if(hasForecastGson)
				{
					LogMessage("getForecast() fctype is weatherzone2 and hasForecastGson is true, skipping...");
					return true;
				}

				LogMessage("getForecast() fctype is weatherzone2 and hasForecastGson is false, skipping...");
				KeyValue.putVar("LastForecastError", getAndroidString(R.string.still_downloading_forecast_data));
				return false;
			}

			forecastTask.cancel(true);
			forecastTask = null;
		}

		dur = (now - rssCheckTime) / 1000;
		LogMessage("getForecast() RSSCache_period_default: " + fcDef.default_forecast_refresh);
		LogMessage("getForecast() current_time: " + now);
		LogMessage("getForecast() rssCheckTime: " + rssCheckTime);
		LogMessage("getForecast() Was forced or no forecast data or cache is more than " +
		           fcDef.default_forecast_refresh + "s old (" + dur + "s)");

		ftStart = now;

		int intervalpos = getIntervalTime()[0];

		if(!runningInBG)
		{
			forecastTask = executor.submit(() ->
			{
				LogMessage("getForecast() Forecast checking: " + forecast_url);
				boolean ret = handleUpdate(fctype, forecast_url, intervalpos);
				if(ret)
					SendIntent(REFRESH_FORECAST_INTENT);
				else
					SendIntent(STOP_FORECAST_INTENT);

				ftStart = 0;
			});

			if(hasForecastGson)
			{
				LogMessage("getForecast() hasForecastGson is true...");
				return true;
			}

			LogMessage("getForecast() hasForecastGson is false...", KeyValue.d);
			KeyValue.putVar("LastForecastError", getAndroidString(R.string.still_downloading_forecast_data));

			return true;
		} else {
			LogMessage("getForecast() Forecast checking: " + forecast_url);
			boolean ret = handleUpdate(fctype, forecast_url, intervalpos);
			if(ret)
				SendIntent(REFRESH_FORECAST_INTENT);
			else
				SendIntent(STOP_FORECAST_INTENT);

			ftStart = 0;

			return ret;
		}
	}

	private static boolean handleUpdate(String fctype, String forecast_url, int intervalpos)
	{
		try
		{
			if(reallyGetForecast(fctype, forecast_url, intervalpos))
			{
				LogMessage("getForecast() Successfully updated local forecast cache...");
				return true;
			}
		} catch(Exception e) {
			LogMessage("getForecast() Error! e: " + e, true, KeyValue.e);
			KeyValue.putVar("LastForecastError", e.getLocalizedMessage());
			return true;
		}

		LogMessage("getForecast() Failed to successfully update local forecast cache...", KeyValue.d);
		return false;
	}

	static int getNextRandom(int min, int max)
	{
		int random;

		do
		{
			random = ThreadLocalRandom.current().nextInt(max);
		} while(random < min || random > max);

		return random;
	}

	static String downloadWZHTML(String url, boolean addDelay, boolean fcString) throws IOException
	{
		if(addDelay)
		{
			try
			{
				int delay = getNextRandom(5_000, 10_000);
				Thread.sleep(delay);
			} catch(Exception ignored) {}
		}

		WebViewPreloader wvpl = new WebViewPreloader();

		if(fcString)
			return wvpl.getHTML(
				url,
				new String[]{"Districts"},
				new String[]{"Page Not found", "Oops! We can't find the page you're looking for. Please check the URL you entered or"},
				new String[]{"Oops! Unfortunately, there was an error on the page.", "Oops! We can't find the page you're looking for."}
			);

		return wvpl.getHTML(
			url,
			new String[]{"UPDATED", "Daily Forecast", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"},
			new String[]{"Page Not found", "Oops! We can't find the page you're looking for. Please check the URL you entered or"},
			new String[]{"Oops! Unfortunately, there was an error on the page.", "Oops! We can't find the page you're looking for."}
		);
	}

	static String getWZHTML(String url, boolean fcString) throws IOException
	{
		String wzHTML;
		boolean addDelay = false;
		int attempt = 1;
		int attempts = 10;
		do
		{
			LogMessage("Starting attempt #" + attempt + " of " + attempts);
			wzHTML = downloadWZHTML(url, addDelay, fcString);
			if(wzHTML == null || wzHTML.startsWith("error|"))
				break;
			addDelay = true;
		} while((wzHTML == null || wzHTML.length() < 10_000) && attempt++ < attempts);

		if(wzHTML != null)
			LogMessage("wzHTML.length(): " + wzHTML.length());

		return wzHTML;
	}

	static String prettyHTML(String html)
	{
		return Jsoup.parse(html).outputSettings(new org.jsoup.nodes.Document.OutputSettings()
				.indentAmount(2).prettyPrint(true)).outerHtml();
	}

	static GsonHelper String2Gson()
	{
		int modhour = getIntervalTime()[1];
		String forecastGson = (String)KeyValue.readVar("forecastGsonEncoded", "");
		return String2Gson(forecastGson, modhour);
	}

	static GsonHelper String2Gson(String forecastGson, int modhour)
	{
		boolean hasForecastGson = forecastGson != null && forecastGson.length() > 128;

		if(!hasForecastGson)
			return null;

		LogMessage("weeWXAppCommon.String2Gson() forecastGson.length(): " + forecastGson.length());

		Gson gson = new Gson();
		GsonHelper gh = gson.fromJson(forecastGson, GsonHelper.class);
		if(gh == null || gh.days == null || gh.days.isEmpty())
		{
			LogMessage("weeWXAppCommon.String2Gson() #2 Failed to process WZ forecast data...");
			return null;
		}

		if(!gh.isDaily)
			gh.days = filterByInterval(gh.days, modhour);

		LogMessage("weeWXAppCommon.String2Gson() displayDays.size(): " + gh.days.size());

		return gh;
	}

	static String[] getGsonContent(String forecastGson, boolean showHeader)
	{
		int modhour = getIntervalTime()[1];

		GsonHelper gh = String2Gson(forecastGson, modhour);
		if(gh == null)
			return new String[]{"error", getAndroidString(R.string.failed_to_process_forecast_data)};

		String content = generateForecast(gh.days, gh.timestamp, showHeader, gh.isDaily || modhour == 24);
		if(content == null || content.isBlank())
		{
			LogMessage("weeWXAppCommon.getGsonContent() #3 Failed to process forecast data...");
			return new String[]{"error", getAndroidString(R.string.failed_to_process_forecast_data)};
		}

		LogMessage("weeWXAppCommon.getGsonContent() content.length(): " + content.length());
//		LogMessage("weeWXAppCommon.getGsonContent() content: " + content);
		return new String[]{null, content, gh.desc != null ? gh.desc : ""};
	}

	private static List<Day> filterByInterval(List<Day> allEntries, int modhour)
	{
//		LogMessage("filterByInterval(): modhour: " + modhour);
//		LogMessage("filterByInterval(): allEntries: " + allEntries);

		if(allEntries == null || allEntries.isEmpty())
			return allEntries;

		List<Day> filtered = new ArrayList<>();
		Calendar cal = Calendar.getInstance();
		for(int i = 0; i < allEntries.size(); i++)
		{
			Day day = allEntries.get(i);
			cal.setTimeInMillis(day.timestamp);

//			LogMessage("filterByInterval(): DAY_OF_MONTH: " + cal.get(Calendar.DAY_OF_MONTH));
//			LogMessage("filterByInterval(): DAY_OF_YEAR: " + cal.get(Calendar.DAY_OF_YEAR));
//
//			LogMessage("filterByInterval(): HOUR_OF_DAY: " + cal.get(Calendar.HOUR_OF_DAY));
//			LogMessage("filterByInterval(): MINUTE: " + cal.get(Calendar.MINUTE));
//			LogMessage("filterByInterval(): SECOND: " + cal.get(Calendar.SECOND));

			int hour = cal.get(Calendar.HOUR_OF_DAY);
			if(hour % modhour == 0)
				filtered.add(day);
		}

//		LogMessage("filterByInterval(): filtered: " + filtered);

		return filtered;
	}

	static boolean reallyGetForecast(String fctype, String url, int UpdateInterval) throws InterruptedException, IOException
	{
		if(fctype.equals("metservice2") || fctype.equals("weatherzone3"))
			return false;

		LogMessage("reallyGetForecast() forcecastURL: " + url);

		if(url == null || url.isBlank())
		{
			KeyValue.putVar("LastForecastError", getAndroidString(R.string.forecast_url_not_set));
			return false;
		}

		long now = System.currentTimeMillis();

		long lastAttemptedForecastDownloadTime = (long)KeyValue.readVar("lastAttemptedForecastDownloadTime", 0L);

		if(lastAttemptedForecastDownloadTime + 10_000L > now)
		{
			LogMessage("reallyGetWeather() lastAttemptedForecastDownloadTime (" + lastAttemptedForecastDownloadTime + ") + 10_000L > now (" + now + ")");
			return false;
		}

		KeyValue.putVar("lastAttemptedForecastDownloadTime", now);

		String forecastData = null;
		Result r1 = null;
		if(fctype.equals("weatherzone2"))
		{
			String forecast_text_url = url.substring(0, url.lastIndexOf("/"));
			LogMessage("reallyGetForecast() Trying secondary URL for forecasts strings: " + forecast_text_url);

			int attempts = 0;
			Result2 r2 = null;
			String wzHTML = null;
			boolean r1fromfile = false, r2fromfile = false;

			if(weeWXApp.DEBUG)
			{
				try
				{
					wzHTML = CustomDebug.readDebug("weeWX", "R2_body.html");
					LogMessage("reallyGetForecast() wzHTML: " + wzHTML);
					if(wzHTML != null && wzHTML.length() > 128)
					{
						r2 = JsoupHelper.processWZ2GetForecastStrings(wzHTML);
						if(r2 != null && r2.rc() > 0)
							r2fromfile = true;
					}
				} catch(Exception e) {
//					LogMessage("Error! e: " + e.getMessage(), true, KeyValue.e);
//					doStackOutput(e);
//					return false;
				}
			}

//			if(weeWXApp.DEBUG)
//				return true;

			if(r2 == null || r2.rc() == 0)
			{
				do
				{
					wzHTML = getWZHTML(forecast_text_url, true);
					if(wzHTML == null)
						break;

					if(wzHTML.isBlank())
						continue;

					if(wzHTML.startsWith("error|"))
						break;

					if(wzHTML.length() > 128 && !r2fromfile && KeyValue.debugging_on())
						CustomDebug.writeDebug("weeWX", "R2_body.html", wzHTML);

					LogMessage("reallyGetForecast() Got data from WZ, let's try to find forecast strings in it...");
					r2 = JsoupHelper.processWZ2GetForecastStrings(wzHTML);
				} while(attempts++ < 3 && (r2 == null || r2.rc() == 0));

				if(wzHTML == null || wzHTML.isBlank())
				{
					LogMessage("Nothing substantial was returned from WZ...", KeyValue.w);
					KeyValue.putVar("LastForecastError", "Nothing substantial was returned from WZ...");
					return false;
				}

				if(wzHTML.startsWith("error|"))
				{
					String[] errors = wzHTML.strip().split("\\|", 2);
					LogMessage("Error! errors[1]: " + errors[1], KeyValue.w);
					KeyValue.putVar("LastForecastError", errors[1]);
					return false;
				}
			}

			if(r2 == null || r2.rc() == 0)
			{
				if(wzHTML != null && !wzHTML.isBlank() && !r2fromfile)
					CustomDebug.writeDebug("weeWX", "R2_body.html", wzHTML);

				LogMessage("reallyGetForecast() Nothing substantial was returned from WZ...", KeyValue.w);
				KeyValue.putVar("LastForecastError", "Nothing substantial was returned from WZ...");
				return false;
			}

			LogMessage("reallyGetForecast() Got forecast strings from WZ... rc: " + r2.rc());

			if(weeWXApp.DEBUG && KeyValue.debugging_on())
			{
				try
				{
					forecastData = CustomDebug.readDebug("weeWX", "R1_body.html");
					LogMessage("reallyGetForecast() forecastData: " + forecastData);
					if(forecastData != null && forecastData.length() > 128)
					{
						r1 = JsoupHelper.processWZ2Forecasts(url, forecastData, r2);
						if(r1 != null && r1.days() != null && !r1.days().isEmpty())
							r1fromfile = true;
					}
				} catch(Exception e) {
//					LogMessage("Error! e: " + e.getMessage(), true, KeyValue.e);
//					doStackOutput(e);
//					return false;
				}
			}

//			if(weeWXApp.DEBUG)
//				return true;

			if(r1 == null || r1.days() == null || r1.days().isEmpty())
			{
				attempts = 0;
				do
				{
					forecastData = getWZHTML(url, false);
					if(forecastData == null)
						break;

					if(forecastData.isBlank())
						continue;

					if(forecastData.startsWith("error|"))
						break;

					if(forecastData.length() > 128 && !r1fromfile && KeyValue.debugging_on())
						CustomDebug.writeDebug("weeWX", "R1_body.html", forecastData);

					LogMessage("reallyGetForecast() Got data from WZ, let's try to find forecast blocks in it...");
					r1 = JsoupHelper.processWZ2Forecasts(url, forecastData, r2);
				} while(attempts++ < 3 && (r1 == null || r1.days() == null || r1.days().isEmpty()));

				if(forecastData == null || forecastData.isBlank())
				{
					LogMessage("Nothing substantial was returned from WZ...", KeyValue.w);
					KeyValue.putVar("LastForecastError", "Nothing substantial was returned from WZ...");
					return false;
				}

				if(forecastData.startsWith("error|"))
				{
					String[] errors = forecastData.strip().split("\\|", 2);
					LogMessage("Error! errors[1]: " + errors[1], KeyValue.w);
					KeyValue.putVar("LastForecastError", errors[1]);
					return false;
				}
			}

			if(r1 == null || r1.days() == null || r1.days().isEmpty())
			{
				if(forecastData != null && !forecastData.isBlank() && !r1fromfile)
					CustomDebug.writeDebug("weeWX", "R1_body.html", forecastData);

				LogMessage("reallyGetForecast() Failed to find any forecast blocks, giving up...", KeyValue.w);
				KeyValue.putVar("LastForecastError", "Nothing substantial was returned from WZ...");
				return false;
			}
		} else {
			forecastData = downloadString(url, false);

			if(forecastData == null || forecastData.isBlank())
			{
				LogMessage("reallyGetForecast() Failed to get any forecast blocks after 3 attempts... giving up...", KeyValue.w);
				KeyValue.putVar("LastForecastError", getAndroidString(R.string.failed_to_process_forecast_data));
				return false;
			}

			if(forecastData.length() > 128 && KeyValue.debugging_on())
			{
				try
				{
					CustomDebug.writeDebug("weeWX", "forecast.html", forecastData);
				} catch(Exception e) {
					LogMessage("reallyGetForecast() Debug write failed: " + e.getMessage(), KeyValue.w);
				}
			}

//			LogMessage("reallyGetForecast() forcecastData: " + forecastData);
		}

		if(KeyValue.countyName != null && !KeyValue.countyName.isBlank())
		{
			String textFC = downloadString("https://www.met.ie/Open_Data/json/" + KeyValue.countyName + ".json", false);
			KeyValue.putVar("textFC", textFC);
		}

		switch(fctype)
		{
			case "aemet.es" -> r1 = processAEMET(forecastData);
			case "bom2" -> r1 = JsoupHelper.processBoM2(forecastData);
			case "bom3daily" -> r1 = processBOM3Daily(forecastData, true);
			case "bom3hourly" -> r1 = processBOM3(UpdateInterval, forecastData, url);
			case "dwd.de" -> r1 = processDWD(forecastData);
			case "tempoitalia.it" -> r1 = JsoupHelper.processTempoItalia(forecastData);
			case "met.ie" -> r1 = processMETIE(forecastData);
			case "met.no" -> r1 = processMetNO(forecastData);
			case "metoffice.gov.uk" -> r1 = processMET(forecastData);
			case "metservice.com" -> r1 = processMetService(forecastData);
			case "openweathermap.org" -> r1 = processOWM(forecastData);
			case "weather.com" -> r1 = processWCOM(forecastData);
			case "weather.gc.ca" -> r1 = JsoupHelper.processWCA(forecastData);
			case "weather.gc.ca-fr" -> r1 = JsoupHelper.processWCAF(forecastData);
			case "weather.gov" -> r1 = processWGOV(forecastData);
			case "weatherzone" -> r1 = processWZ(url, forecastData);
			case "weatherzone2" -> {}
			case "wmo.int" -> r1 = processWMO(forecastData);
			case "yahoo" -> r1 = JsoupHelper.processYahoo(forecastData);
			default ->
			{
				LogMessage("reallyGetForecast() Failed to process forecast, fctype is invalid: " + fctype);
				String fmtStr = String.format(getAndroidString(R.string.forecast_type_is_invalid), fctype);
				KeyValue.putVar("LastForecastError", fmtStr);
				return false;
			}
		}

		if(r1 == null || r1.days() == null || r1.days().isEmpty())
		{
//			if(forecastData != null && !forecastData.isBlank())
//				CustomDebug.writeDebug("weeWX", "forecast.html", forecastData);

			LogMessage("reallyGetForecast() Failed to process forecast data, giving up...", KeyValue.w);
			KeyValue.putVar("LastForecastError", getAndroidString(R.string.failed_to_process_forecast_data));
			return false;
		}

		GsonHelper gh = new GsonHelper();
		gh.days = r1.days();
		gh.desc = r1.desc();
		gh.timestamp = r1.timestamp() > 0 ? r1.timestamp() : System.currentTimeMillis();
		gh.isDaily = r1.isDaily();

		Gson gson = new Gson();
		String forecastGson = gson.toJson(gh);

		LogMessage("reallyGetForecast() Updating forecast cache, forecastGson.length(): " + forecastGson.length());
		KeyValue.putVar("forecastGsonEncoded", forecastGson);
		KeyValue.putVar("rssCheck", gh.timestamp);
		KeyValue.putVar("LastForecastError", null);

		return true;
	}

	static Bitmap getRadarImage(boolean forced, boolean calledFromweeWXApp, boolean noCache, boolean runningInBG)
	{
		Bitmap bm = null;

		try
		{
			if(checkForImage(weeWXApp.radarFilename))
				bm = getImage(weeWXApp.radarFilename);
		} catch(Exception e) {
			LogMessage("getRadarImage() Error! e: " + e, true, KeyValue.e);
		}

		if(!checkConnection() && !forced)
		{
			if(bm == null)
				return weeWXApp.textToBitmap(R.string.wifi_not_available);

			return bm;
		}

		int pos = (int)KeyValue.readVar("UpdateFrequency", weeWXApp.UpdateFrequency_default);
		LogMessage("getRadarImage() pos: " + pos + ", update interval set to: " + weeWXApp.updateOptions[pos] + ", forced set to: " + forced);
		if(pos < 0 || pos >= weeWXApp.updateOptions.length)
			pos = weeWXApp.UpdateFrequency_default;

		if(!forced && pos == 0)
		{
			if(bm == null)
				return weeWXApp.textToBitmap(R.string.update_set_to_manual_but_no_content_cached);

			return bm;
		}

		LogMessage("getRadarImage() Reloading radar image...");

		String radarURL = (String)KeyValue.readVar("RADAR_URL", "");
		if(radarURL == null || radarURL.isBlank())
			return weeWXApp.textToBitmap(R.string.radar_url_not_set);

		String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
		if(radtype == null || (!radtype.equals("image") && !radtype.equals("webpage")))
		{
			String tmp = String.format(getAndroidString(R.string.radar_type_is_invalid), radtype);
			return weeWXApp.textToBitmap(tmp);
		}

		if(!hasBootedFully && !calledFromweeWXApp && !forced)
		{
			if(bm != null)
				return bm;
			else
				return weeWXApp.textToBitmap(R.string.radar_still_downloading);
		}

		long now = System.currentTimeMillis();
		long lastRadarDownload = (long)KeyValue.readVar("lastRadarDownload", 0L);

		NPWSLL npwsll = getNPWSLL();
		long dur = npwsll.report_time - lastRadarDownload;
		if(!forced && bm != null && dur < npwsll.periodTime)
		{
			LogMessage("getRadarImage() Not forced and bm != null and less than " + npwsll.periodTime +
			           "ms (" + dur + "ms)...", KeyValue.d);
			return bm;
		}

		LogMessage("getRadarImage() Reload radar...");

		dur = (now - rtStart) / 1000;
		if(radarTask != null && !radarTask.isDone())
		{
			if(dur < 30)
			{
				LogMessage("getRadarImage() radarTask is less than 30s old (" + dur +
				                          "s), we'll skip this attempt...", KeyValue.d);
				return bm;
			}

			radarTask.cancel(true);
			radarTask = null;
		}

		LogMessage("getRadarImage() Staring attempt to get a new radar image...");

		rtStart = now;

		if(!runningInBG)
		{
			radarTask = executor.submit(() ->
			{
				LogMessage("getRadarImage() Radar checking: " + radarURL);

				try
				{
					LogMessage("getRadarImage() Starting to download radar image from: " + radarURL);
					if(loadOrDownloadImage(radarURL, weeWXApp.radarFilename, noCache) != null)
					{
						KeyValue.putVar("lastRadarDownload", System.currentTimeMillis());
						SendIntent(REFRESH_RADAR_INTENT);
						rtStart = 0;
						return;
					}
				} catch(Exception e) {
					LogMessage("getRadarImage() Error! e: " + e, true, KeyValue.e);
				}

				SendIntent(STOP_RADAR_INTENT);
				rtStart = 0;
			});

			return bm;
		} else {
			LogMessage("getRadarImage() Radar checking: " + radarURL);

			try
			{
				LogMessage("getRadarImage() Starting to download radar image from: " + radarURL);
				bm = loadOrDownloadImage(radarURL, weeWXApp.radarFilename, noCache);
				if(bm != null)
				{
					KeyValue.putVar("lastRadarDownload", System.currentTimeMillis());
					SendIntent(REFRESH_RADAR_INTENT);
					rtStart = 0;
					return bm;
				}
			} catch(Exception e) {
				LogMessage("getRadarImage() Error! e: " + e, true, KeyValue.e);
			}

			SendIntent(STOP_RADAR_INTENT);
			rtStart = 0;
			return null;
		}
	}

	static Bitmap getWebcamImage(boolean forced, boolean calledFromweeWXApp, boolean noCache, boolean runningInBG)
	{
		return getWebcamImage(forced, calledFromweeWXApp, noCache, runningInBG, true);
	}

	static Bitmap getWebcamImage(boolean forced, boolean calledFromweeWXApp, boolean noCache, boolean runningInBG, boolean requireIntent)
	{
		Bitmap bm = null;
		long now = System.currentTimeMillis();

		try
		{
			if(checkForImage(weeWXApp.webcamFilename))
				bm = getImage(weeWXApp.webcamFilename);
		} catch(Exception e) {
			LogMessage("getWebcamImage() Error! e: " + e, true, KeyValue.e);
		}

		long lastDownloadWebcamAttempt = (long)KeyValue.readVar("lastDownloadWebcamAttempt", 0L);

		if(lastDownloadWebcamAttempt + 5_000L > now)
		{
			LogMessage("getWebcamImage() lastDownloadWebcamAttempt (" + lastDownloadWebcamAttempt + ") + 5_000L > now (" + now + ")");
			if(requireIntent)
				SendIntent(STOP_WEBCAM_INTENT);
			return bm;
		}

		KeyValue.putVar("lastDownloadWebcamAttempt", now);

		if(!checkConnection() && !forced)
		{
			if(bm == null)
				return weeWXApp.textToBitmap(R.string.wifi_not_available);
			if(requireIntent)
				SendIntent(STOP_WEBCAM_INTENT);
			return bm;
		}

		if(!KeyValue.isPrefSet("webcamInterval"))
		{
			LogMessage("getWebcamImage() Invalid update frequency...");
			if(requireIntent)
				SendIntent(STOP_WEBCAM_INTENT);
			return weeWXApp.textToBitmap(R.string.invalid_update_interval);
		}

		int pos = (int)KeyValue.readVar("webcamInterval", weeWXApp.webcamInterval_default);
		if(pos < 0 || pos >= weeWXApp.webcamRefreshOptions.length)
			pos = weeWXApp.webcamInterval_default;

		LogMessage("getWebcamImage() pos: " + pos + ", update interval set to: " +
		           weeWXApp.webcamRefreshOptions[pos] + ", forced set to: " + forced);

		if(!forced && pos == 0)
		{
			LogMessage("getWebcamImage() Not forced and set to manual updates...");

			if(bm == null)
				return weeWXApp.textToBitmap(R.string.update_set_to_manual_but_no_content_cached);
			if(requireIntent)
				SendIntent(STOP_WEBCAM_INTENT);
			return bm;
		}

		LogMessage("getWebcamImage() Reloading webcam...");

		String webcamURL = (String)KeyValue.readVar("WEBCAM_URL", "");
		if(webcamURL == null || webcamURL.isBlank())
		{
			LogMessage("getWebcamImage() Webcam URL not set...");
			if(requireIntent)
				SendIntent(STOP_WEBCAM_INTENT);
			return weeWXApp.textToBitmap(R.string.webcam_url_url_not_set);
		}

		long dur = (now - wcStart) / 1000;
		if(!forced && dur < 30)
		{
			if(bm == null)
			{
				LogMessage("getWebcamImage() Not forced and not 30s old (" + dur + "s) and bm == null...");
				if(requireIntent)
					SendIntent(STOP_WEBCAM_INTENT);
				return weeWXApp.textToBitmap(R.string.webcam_still_downloading);

			} else {
				LogMessage("getWebcamImage() Not forced and not 30s old (" + dur + "s) and bm != null...");
				if(requireIntent)
					SendIntent(STOP_WEBCAM_INTENT);
				return bm;
			}
		}

		if(!forced && !hasBootedFully && !calledFromweeWXApp)
		{
			LogMessage("getWebcamImage() Not forced and hasBootedFully is false and calledFromweeWXApp is false...");

			if(requireIntent)
				SendIntent(STOP_WEBCAM_INTENT);

			if(bm != null)
				return bm;
			else
				return weeWXApp.textToBitmap(R.string.webcam_still_downloading);
		}

		NPWSLL npwsll = getNPWSLL();
		long lastWebcamDownload = (long)KeyValue.readVar("lastWebcamDownload", 0L);
		dur = npwsll.report_time - lastWebcamDownload;
		if(!forced && bm != null && dur < npwsll.periodTime)
		{
			LogMessage("getWebcamImage() Not forced and bm != null and less than " + npwsll.periodTime + "ms (" + dur + "ms)...");
			if(requireIntent)
				SendIntent(STOP_WEBCAM_INTENT);
			return bm;
		}

		LogMessage("getWebcamImage() Reload webcam...");

		dur = (now - wcStart) / 1000;
		if(webcamTask != null && !webcamTask.isDone())
		{
			if(dur < 30)
			{
				LogMessage("getWebcamImage() webcamTask is less than 30s old (" + dur + "s), we'll skip this attempt...");
				if(requireIntent)
					SendIntent(STOP_WEBCAM_INTENT);
				return bm;
			}

			webcamTask.cancel(true);
			webcamTask = null;
		}

		wcStart = now;

		if(!runningInBG)
		{
			webcamTask = executor.submit(() ->
			{
				LogMessage("getWebcamImage() Webcam checking: " + webcamURL);

				try
				{
					LogMessage("getWebcamImage() Starting to download webcam image from: " + webcamURL);
					if(loadOrDownloadImage(webcamURL, weeWXApp.webcamFilename, noCache) != null)
					{
						KeyValue.putVar("lastWebcamDownload", npwsll.lastStart);
						LogMessage("getWebcamImage() Webcam image downloaded successfully...");
						SendIntent(REFRESH_WEBCAM_INTENT);
						wcStart = 0;
						return;
					}
				} catch(Exception e) {
					LogMessage("getWebcamImage() Error! e: " + e, true, KeyValue.e);
				}

				LogMessage("getWebcamImage() Webcam image failed to download successfully...", KeyValue.w);
				SendIntent(STOP_WEBCAM_INTENT);
				wcStart = 0;
			});

			if(bm == null)
			{
				if(requireIntent)
					SendIntent(STOP_WEBCAM_INTENT);
				return weeWXApp.textToBitmap(R.string.webcam_still_downloading);
			}

			if(requireIntent)
				SendIntent(STOP_WEBCAM_INTENT);

			return bm;
		} else {
			LogMessage("getWebcamImage() Webcam checking: " + webcamURL);

			try
			{
				LogMessage("getWebcamImage() Starting to download webcam image from: " + webcamURL);
				bm = loadOrDownloadImage(webcamURL, weeWXApp.webcamFilename, noCache);
				if(bm != null)
				{
					KeyValue.putVar("lastWebcamDownload", npwsll.lastStart);
					LogMessage("getWebcamImage() Webcam image downloaded successfully...");
					SendIntent(REFRESH_WEBCAM_INTENT);
					wcStart = 0;
					return bm;
				}
			} catch(Exception e) {
				LogMessage("getWebcamImage() Error! e: " + e, true, KeyValue.e);
			}

			LogMessage("getWebcamImage() Webcam image failed to download successfully...", KeyValue.w);
			SendIntent(STOP_WEBCAM_INTENT);
			wcStart = 0;
			return null;
		}
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
		return str.replaceAll("[\n\r\t]", "").strip();
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

	static void delImage(String filename)
	{
		File file = getDataDir();
		file = new File(file, filename);
		if(file.exists() && !file.delete())
			LogMessage("delImage() Failed to delete " + filename);
	}

	static Bitmap loadOrDownloadImage(String url, String filename, boolean noCache) throws InterruptedException, IOException
	{
		Bitmap bm = null;
		File file = getFile(filename);

		LogMessage("Starting to download image from: " + url);
		if(url.toLowerCase(Locale.ENGLISH).strip().endsWith(".mjpeg") ||
		   url.toLowerCase(Locale.ENGLISH).strip().endsWith(".mjpg"))
		{
			LogMessage("Trying to get a frame from a MJPEG stream and set bm to it...");
			bm = grabMjpegFrame(url, false);
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
			if(downloadToFile(file, url, noCache))
				bm = loadImage(file);
		}

		LogMessage("Finished downloading from webURL: " + url);
		return bm;
	}

	static byte[] downloadContent(String url, boolean noCache) throws InterruptedException, IOException
	{
		if(url == null || url.isBlank())
		{
			LogMessage("url is null or blank, bailing out...", KeyValue.d);
			throw new IOException("url is null or blank, bailing out...");
		}

		OkHttpClient client = NetworkClient.getInstance(url, noCache);

		return reallyDownloadContent(client, url, 0, noCache);
	}

	private static byte[] reallyDownloadContent(OkHttpClient client, String url, int retries, boolean noCache) throws InterruptedException, IOException
	{
		LogMessage("reallyDownloadContent() checking if url  " + url + " is valid, attempt " + (retries + 1));

		Request request = NetworkClient.getRequest(false, url, noCache);

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

				return reallyDownloadContent(client, url, retries, noCache);
			}

//			LogMessage("reallyCheckURL() Error! e: " + e.getMessage(), true, KeyValue.e);

			doStackOutput(e);

			throw e;
		}
	}

	static boolean downloadToFile(File file, String url, boolean noCache) throws InterruptedException, IOException
	{
		LogMessage("Downloading from url: " + url);
		byte[] body = downloadContent(url, noCache);
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

	static Bitmap grabMjpegFrame(String url, boolean noCache) throws InterruptedException, IOException
	{
		if(url == null || url.isBlank())
		{
			LogMessage("url is null or blank, bailing out...", KeyValue.d);
			throw new IOException("url is null or blank, bailing out...");
		}

		OkHttpClient client = NetworkClient.getStream(url);

		return reallyGrabMjpegFrame(client, url, 0, noCache);
	}

	static Bitmap reallyGrabMjpegFrame(OkHttpClient client, String url, int retries, boolean noCache) throws InterruptedException, IOException
	{
		Exception lastException = null;
		Bitmap bm;
		InputStream urlStream = null;

		LogMessage("reallyGrabMjpegFrame() checking if url  " + url + " is valid, attempt " + (retries + 1));

		Request request = NetworkClient.getRequest(false, url, noCache);

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

			//lastException = new IOException("Failed to successfully grab a frame from a mjpeg stream...");
		} catch(IOException e) {
			doStackOutput(e);
			//lastException = e;
			throw e;
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

			return reallyGrabMjpegFrame(client, url, retries, noCache);
		}

		if(lastException != null)
		{
			LogMessage("reallyCheckURL() Error! lastException: " + lastException.getMessage(), true, KeyValue.e);
			try
			{
				throw lastException;
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}

		return null;
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

	static String C2Fdeground(float C)
	{
		return C2F(C) + "&deg;F";
	}

	static String F2Cdeground(float F)
	{
		return F2C(F) + "&deg;C";
	}

	static int str2Int(String f)
	{
		return Math.round(str2Float(f));
	}

	static float str2Float(String f)
	{
		String tmp = f.replaceAll("[^0-9-.]", "").strip();
		if(tmp == null || tmp.isBlank())
			return 0;

		try
		{
			return (float)Double.parseDouble(tmp);
		} catch(Exception ignored) {}

		return 0;
	}

	static float mm2in(float mm)
	{
		return mm / 25.4f;
	}

	static float in2mm(float in)
	{
		return in * 25.4f;
	}

	static float mps2kmph(float mps)
	{
		return round(mps * 3.6f, 1);
	}

	static float mps2mph(float mps)
	{
		return round(mps * 2.236936f, 1);
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

	static void dumpString(String str)
	{
		for(int i = 0; i < Math.min(str.length(), 10); i++)
			LogMessage("char at " + i + ": " + ((int)str.charAt(i)));
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

	static int[] getIntervalTime()
	{
		int pos = (int)KeyValue.readVar("UpdateInterval", weeWXApp.UpdateInterval_default);
		return getIntervalTime(pos);
	}

	static int[] getIntervalTime(int pos)
	{
		int IntervalTime;

		switch(pos)
		{
			case 1 -> IntervalTime = 43_200;
			case 2 -> IntervalTime = 21_600;
			case 3 -> IntervalTime = 10_800;
			case 4 -> IntervalTime = 3_600;
			default -> IntervalTime = 86_400;
		}

		//LogMessage("IntervalTime: " + IntervalTime + "s");

		int modhour = Math.round(IntervalTime / 3_600f);

		if(modhour < 1 || modhour > 24)
			modhour = 24;

		return new int[]{pos, modhour, IntervalTime};
	}

	static NPWSLL getNPWSLL()
	{
		long now = System.currentTimeMillis();

//		Log.i(LOGTAG, Log.getStackTraceString(new Throwable()));

		String string_time = sdf8.format(now);
		LogMessage("getNPWSLL() now: " + string_time);

		long[] ret = getPeriod();

		LogMessage("Got the following: " + Arrays.toString(ret));

		long period = ret[0];
		long wait = ret[1];

		//LogMessage("Here1");

		long report_time = getLDTms() + wait;

		string_time = sdf8.format(report_time);
		LogMessage("getNPWSLL() report_time: " + string_time);

		if(period <= 0)
			return new NPWSLL(now, period, wait, 0L, 0L, report_time);

		//LogMessage("Here2");

		long start = Math.round((double)now / (double)period) * period;

		string_time = sdf8.format(start);
		LogMessage("getNPWSLL() start: " + string_time);

		start += wait;

		string_time = sdf8.format(start);
		LogMessage("getNPWSLL() start+wait: " + string_time);

		while(start < now + 15_000L)
			start += period;

		string_time = sdf8.format(start);
		LogMessage("getNPWSLL() next start: " + string_time);

		long lastStart = start - period;

		string_time = sdf8.format(lastStart);
		LogMessage("getNPWSLL() lastStart: " + string_time);

		return new NPWSLL(now, period, wait, start, lastStart, report_time);
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

	static String deg2Str(String degree_element, String speed_element)
	{
		if(!hasElement(degree_element) || !hasElement(speed_element))
			return "N/A";

		float speed = (float)getJson(speed_element, 0f);

		if(speed <= 0)
			return "N/A";

		float degrees = (float)getJson(degree_element, 0f);

		int index = (int)Math.round(degrees / 22.5) % 16;
		return direction_labels[index];
	}
}