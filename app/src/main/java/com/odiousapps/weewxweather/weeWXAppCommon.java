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
import org.jsoup.nodes.Document;
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

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.LifecycleOwner;

import androidx.lifecycle.Observer;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.odiousapps.weewxweather.WidgetProvider.updateAppWidget;
import static com.odiousapps.weewxweather.weeWXApp.CONTENT_TYPE;
import static com.odiousapps.weewxweather.weeWXApp.DEBUG;
import static com.odiousapps.weewxweather.weeWXApp.ERROR_E;
import static com.odiousapps.weewxweather.weeWXApp.FAILED_TO_CREATE_LOG_FILE_IN_MEDIA_STORE_FILES;
import static com.odiousapps.weewxweather.weeWXApp.RSS_CHECK;
import static com.odiousapps.weewxweather.weeWXApp.SAVE_APP_DEBUG_LOGS;
import static com.odiousapps.weewxweather.weeWXApp.TIME_EXT;
import static com.odiousapps.weewxweather.weeWXApp.getAndroidString;
import static com.odiousapps.weewxweather.weeWXApp.getEnglishAndroidString;

@SuppressWarnings({"CallToPrintStackTrace"})
class weeWXAppCommon
{
	private final static String PREFS_NAME = "WeeWxWeatherPrefs";
	final static String LOGTAG = "weeWXApp";
	static final String MESSAGE = "message='";
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
	static Uri logFileUri;
	static final String EXIT_INTENT = "com.odiousapps.weewxweather.EXIT_INTENT";
	static final String INIGO_INTENT = "com.odiousapps.weewxweather.INIGO_UPDATE";
	static final String PROCESSING_ERRORS = "com.odiousapps.weewxweather.PROCESSING_ERRORS";
	static final String FAILED_TO_MERGE = "com.odiousapps.weewxweather.FAILED_TO_MERGE";
	static final String UPDATE_ERRORS = "com.odiousapps.weewxweather.UPDATE_ERRORS";

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

	private static final int[] emptyIntArr = new int[]{};
	private static final long[] emptyLongArr = new long[]{};

	private static final BitmapFactory.Options options = new BitmapFactory.Options();

	private static JSONObject nws;

	private static Typeface tf_bold;

	private static final ExecutorService executor = Executors.newFixedThreadPool(5);

	private static final Collection<String> processedMissingIcons;

	record Result(List<Day> days, String desc, long timestamp, boolean isDaily) {}
	record Result2(String[] forecast_text, String desc, int rc) {}
	record Result3(boolean succeeded, String error, Result result) {}
	record TempResult(float CurrTemp, float minObservedTemp, float maxObservedTemp, int[] outTemp_trend_count,
					  int[] outTemp_trend_signal, long[] outTemp_trend_ts) {}
	record NPWSLL(long nowTime, long periodTime, long waitTime, long startTime, long lastStart, long report_time) {}

	static ParallelDownloader downloader;

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

	private static final int[] levels = {
			LEVEL_SEVERE, LEVEL_WARNING, LEVEL_WATCH
	};

	private static final int[] periods = {
			PERIOD_10MIN, PERIOD_30MIN, PERIOD_1HR, PERIOD_6HR, PERIOD_24HR
	};

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

	final static String[] direction_labels = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
									          "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};

	static
	{
		processedMissingIcons = new HashSet<>();
		if(DEBUG)
			debug_level = KeyValue.v;

		try
		{
			System.setProperty("http.agent", NetworkClient.UA);
		} catch(Exception e)
		{
			LogMessage(ERROR_E + e, true, KeyValue.e);
		}

		try
		{
			tf_bold = Typeface.create("sans-serif", Typeface.BOLD);
		} catch(Exception e)
		{
			LogMessage(ERROR_E + e, true, KeyValue.e);
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
		long[] def = {0L, 0L};

		int pos = (int)KeyValue.readVar(weeWXApp.UPDATE_FREQUENCY, weeWXApp.UpdateFrequency_default);
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

		if(is_blank(text))
			return;

		if((boolean)KeyValue.readVar(SAVE_APP_DEBUG_LOGS, weeWXApp.save_app_debug_logs_default))
		{
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
			{
				try
				{
					appendWithMediaStore(text, level);
				} catch(Exception ignored) {}
			} else
				appendLegacy(text);
		}

		if(level <= debug_level || showAnyway)
		{
			String textStr = removeWS(text);
			if(textStr.length() > maxLogLength)
				textStr = "[String truncated to " + maxLogLength + " bytes] " + text.substring(0, maxLogLength);

			switch(level)
			{
				case KeyValue.e -> Log.e(LOGTAG, MESSAGE + textStr + "'");
				case KeyValue.w -> Log.w(LOGTAG, MESSAGE + textStr + "'");
				case KeyValue.i -> Log.i(LOGTAG, MESSAGE + textStr + "'");
				case KeyValue.d -> Log.d(LOGTAG, MESSAGE + textStr + "'");
				default -> Log.v(LOGTAG, MESSAGE + textStr + "'");
			}
		} else
			Log.v(LOGTAG, "hidden message='" + text + "'");
	}

	@Nullable
	static byte[] gzipToBytes(String text) throws IOException
	{
		if(is_blank(text))
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

	@SuppressWarnings("ConstantValue")
    private static void appendLegacy(String text)
	{
		try
		{
			File file = getExtFile(weeWXApp.WEEWX_DIR, weeWXApp.debug_filename);
			boolean needsPublishing = !file.exists();
			FileOutputStream fos = new FileOutputStream(file, true);

			if(weeWXApp.debug_filename.endsWith(".gz"))
				fos.write(gzipToBytes(text));
			else if(weeWXApp.debug_filename.endsWith(".txt"))
				fos.write(text.getBytes(StandardCharsets.UTF_8));

			fos.close();

			if(needsPublishing)
				publish(file);
		} catch (IOException e) {
			doStackOutput(e);
		}
	}

	@SuppressWarnings("ConstantValue")
    @RequiresApi(api = Build.VERSION_CODES.Q)
	private static void appendWithMediaStore(String text, int level) throws IOException
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
			try(Cursor cursor = context.getContentResolver().query(
					logFileUri,
					new String[]{ MediaStore.Files.FileColumns._ID },
					null, null, null))
			{
				if(cursor == null || !cursor.moveToFirst())
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
					throw new IOException(FAILED_TO_CREATE_LOG_FILE_IN_MEDIA_STORE_FILES);
			} else if(logFileUri == null && weeWXApp.debug_filename.endsWith(".txt")) {
				ContentValues values = new ContentValues();
				values.put(MediaStore.Files.FileColumns.DISPLAY_NAME, weeWXApp.debug_filename);
				values.put(MediaStore.Files.FileColumns.MIME_TYPE, CONTENT_TYPE);
				values.put(MediaStore.Files.FileColumns.RELATIVE_PATH, folderName);

				logFileUri = context.getContentResolver().insert(filesCollection, values);
				if(logFileUri == null)
				{
					String warning = FAILED_TO_CREATE_LOG_FILE_IN_MEDIA_STORE_FILES;
					Log.w(LOGTAG, warning);
					throw new IOException(warning);
				}
			}
		}

		// ================
		// 3. Append text to the log file
		// ================
		String timestamp = weeWXApp.getInstance().sdf13.format(System.currentTimeMillis());

		String tmpStr = timestamp + " " + levelToName(level) + ": " + text.strip() + "\n";

		try(OutputStream os = context.getContentResolver().openOutputStream(logFileUri, "wa"))
		{
			if(os == null)
				return;

			if(weeWXApp.debug_filename.endsWith(".gz"))
				os.write(gzipToBytes(tmpStr));
			else
				os.write(tmpStr.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			doStackOutput(e);
		}
	}

	private static SharedPreferences getPrefSettings()
	{
		return weeWXApp.getInstance().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	}

	static void setVar(String string, Object val)
	{
		if(val == null)
		{
			if(isPrefSet(string))
				RemovePref(string);

			return;
		}

		switch(val)
		{
			case Boolean aBoolean ->
			{
				try
				{
					getPrefSettings().edit().putBoolean(string, aBoolean).apply();
				} catch(Exception ignored) {}
			}
			case Float f ->
			{
				try
				{
					getPrefSettings().edit().putFloat(string, f).apply();
				} catch(Exception ignored) {}
			}
			case Integer i ->
			{
				try
				{
					getPrefSettings().edit().putInt(string, i).apply();
				} catch(Exception ignored) {}
			}
			case Long l ->
			{
				try
				{
					getPrefSettings().edit().putLong(string, l).apply();
				} catch(Exception ignored) {}
			}
			case String s ->
			{
				try
				{
					getPrefSettings().edit().putString(string, s).apply();
				} catch(Exception ignored) {}
			}
			default ->
			{
			}
		}
	}

	@Nullable
	static Object readVar(String string, Object defVal)
	{
		if(is_blank(string))
			return null;

		if(!isPrefSet(string))
			return defVal;

		if(defVal == null)
			defVal = "";

		if(defVal instanceof Boolean)
		{
			try
			{
				return getPrefSettings().getBoolean(string, (boolean)defVal);
			} catch(Exception ignored) {}
		}

		if(defVal instanceof Integer)
		{
			try
			{
				return getPrefSettings().getInt(string, (int)defVal);
			} catch(Exception ignored) {}
		}

		if(defVal instanceof Float)
		{
			try
			{
				return getPrefSettings().getFloat(string, (float)defVal);
			} catch(Exception ignored) {}
		}

		if(defVal instanceof Long)
		{
			try
			{
				return getPrefSettings().getLong(string, (long)defVal);
			} catch(Exception ignored) {}
		}

		if(defVal instanceof String)
		{
			try
			{
				return getPrefSettings().getString(string, (String)defVal);
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

	static boolean fixtypes()
	{
		SharedPreferences prefs = getPrefSettings();
		if(prefs == null)
			return false;

		Map<String, ?> all = prefs.getAll();
		SharedPreferences.Editor editor = prefs.edit();
		boolean changed = false;

		for(Map.Entry<String, ?> entry: all.entrySet())
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

	@Nullable
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
		if(number instanceof Long l)
		{
			if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE)
				return l.intValue();

			return l;
		} else if(number instanceof Double d) {
			float f = d.floatValue();

			// If float is lossless, use float
			if(f == d)
				return f;

			// Otherwise keep double, stored as string
			return d;
		}

		return number;
	}

	static long getRSSms()
	{
		LogMessage("Checking for var named 'rssCheck'");

		if(!KeyValue.isPrefSet(RSS_CHECK))
			return 0L;

		long rssTime = (long)KeyValue.readVar(RSS_CHECK, 0L);
		if(rssTime == 0)
		    return 0L;

		LogMessage("getRSSms() Before: rssTime: " + rssTime);

		while(rssTime < 10_000_000_000L)
			rssTime *= 1_000L;

		LogMessage("getRSSms() After: rssTime: " + rssTime);

		return rssTime;
	}

	static long getLDTms()
	{
		LogMessage("Checking for var named '" + json_keys[0] + TIME_EXT + "'");

		if(!isPrefSet(json_keys[0] + TIME_EXT))
			return 0L;

		long lastDownloadTime = (long)KeyValue.readVar(json_keys[0] + TIME_EXT, 0L);
		if(lastDownloadTime == 0)
			return 0L;

		LogMessage("getLDTms() Before: lastDownloadTime: " + lastDownloadTime);

		while(lastDownloadTime < 10_000_000_000L)
			lastDownloadTime *= 1_000L;

		LogMessage("getLDTms() After: lastDownloadTime: " + lastDownloadTime);

		return lastDownloadTime;
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

		String string_time = weeWXApp.getInstance().sdf7.format(timestamp);

		sb.append("\n<div class='header'>").append(string_time).append("</div>\n\n");

		if(showHeader)
		{
			start = 1;
			Day first = getFirstDay(days);

			sb.append("<div class='today'>\n\t<div class='topRow'>\n");

			if(!is_blank(first.icon))
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
			if(is_blank(first.max) || first.max.equals("&deg;C") || first.max.equals("&deg;F"))
				sb.append(weeWXApp.emptyField);
			else
				sb.append(first.max);
			sb.append("</div>\n");

			sb.append("\t\t\t<div class='minTemp'>");
			if(is_blank(first.min) || first.min.equals("&deg;C") || first.min.equals("&deg;F"))
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

			if(!is_blank(day.icon))
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
					sb.append(weeWXApp.getInstance().sdf2.format(day.timestamp));
			} else {
				cal.setTimeInMillis(day.timestamp);
				int hour = cal.get(Calendar.HOUR_OF_DAY);
				if(hour == 0)
					sb.append(weeWXApp.getInstance().sdf2.format(day.timestamp));
				else
					sb.append(weeWXApp.getInstance().sdf17.format(day.timestamp));
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
		LogMessage("weeWXAppCommon.updateCacheTime(" + timestamp + ")");

		if(timestamp <= 0)
			return;

		while(timestamp < 10_000_000_000L)
			timestamp *= 1_000L;

		long rssCheck = getRSSms();

		LogMessage("rssCheck: " + rssCheck);
		LogMessage("timestamp: " + timestamp);

		if(timestamp != rssCheck)
			KeyValue.putVar(RSS_CHECK, timestamp);
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

	static Result processBOM3(int modhour, String data, String url) throws IOException
	{
		LogMessage("processBOM3()");

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
			if(!is_blank(url2) && !url2.equals(url))
			{
				LogMessage("processBOM3(): Getting BoM daily");
				fcdata = downloadString(url2);
			}

			if(!is_blank(fcdata))
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

		if(r2 == null)
			return null;

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

		if(is_blank(data))
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

			Date df = weeWXApp.getInstance().sdf1.parse(tmp);
			if(df != null)
				timestamp = df.getTime();

			if(timestamp <= 0)
				timestamp = System.currentTimeMillis();

			if(timestamp > 0)
				updateCacheTime(timestamp);

			Date date = new Date(timestamp);
			LogMessage("Last updated forecast: " + weeWXApp.getInstance().sdf5.format(date));

			JSONArray myhours = jobj.getJSONArray("data");
			for(int i = 0; i < myhours.length(); i++)
			{
				Day day = new Day();

				day.timestamp = 0;
				df = weeWXApp.getInstance().sdf1.parse(myhours.getJSONObject(i).getString("time"));
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
				if(!is_blank(fileName))
				{
					day.icon = "icons/bom/" + fileName + ".svg";

					String content = weeWXApp.loadFileFromAssets(day.icon);
					if(!is_blank(content))
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
			LogMessage(ERROR_E + e, true, KeyValue.e);
			Activity act = getActivity();
			if(act == null)
				return null;

			act.finish();
		}

		return new Result(days, desc, timestamp, false);
	}

	static Result processBOM3Daily(String data, boolean updateCTime)
	{
		String missing = null;

		LogMessage("Starting processBOM3Daily()");

		if(is_blank(data))
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

			Date df = weeWXApp.getInstance().sdf1.parse(tmp);
			if(df != null)
				timestamp = df.getTime();

			if(timestamp <= 0)
				timestamp = System.currentTimeMillis();

			if(timestamp > 0 && updateCTime)
				updateCacheTime(timestamp);

			Date date = new Date(timestamp);
			LogMessage("Last updated forecast: " + weeWXApp.getInstance().sdf5.format(date));

			JSONArray mydays = jobj.getJSONArray("data");
			for(int i = 0; i < mydays.length(); i++)
			{
				Day day = new Day();

				day.timestamp = 0;
				df = weeWXApp.getInstance().sdf1.parse(mydays.getJSONObject(i).getString("date"));
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
				if(!is_blank(fileName))
				{
					day.icon = "icons/bom/" + fileName + ".svg";

					String content = weeWXApp.loadFileFromAssets(day.icon);
					if(!is_blank(content))
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

				if(!is_blank(day.icon))
					days.add(day);
			}
		} catch(Exception e) {
			LogMessage(ERROR_E + e, true, KeyValue.e);
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
		if(is_blank(data))
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
				Date df = weeWXApp.getInstance().sdf4.parse(date);
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
		if(is_blank(data))
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
			Date df = weeWXApp.getInstance().sdf1.parse(tmp);
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
					if(is_blank(number))
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
				df = weeWXApp.getInstance().sdf1.parse(validTime.getString(i));
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
		if(is_blank(data))
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
			Date df = weeWXApp.getInstance().sdf10.parse(tmp);
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
				df = weeWXApp.getInstance().sdf4.parse(date);
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
		if(is_blank(data))
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

				Date df = weeWXApp.getInstance().sdf1.parse(jtmp.getString("dateISO"));
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
				if(!is_blank(content))
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
		if(is_blank(data))
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

			Date df = weeWXApp.getInstance().sdf11.parse(string_time);
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
		if(is_blank(data))
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

			Date df = weeWXApp.getInstance().sdf12.parse(elaborado);
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
				df = weeWXApp.getInstance().sdf4.parse(fecha);
				if(df != null)
					day.timestamp = df.getTime();

				JSONObject estado_cielo = null;

				Object v = jtmp.get("estado_cielo");
				if(v instanceof JSONObject)
				{
					estado_cielo = jtmp.getJSONObject("estado_cielo");
				} else if(v instanceof JSONArray) {
					JSONArray jarr = jtmp.getJSONArray("estado_cielo");
					for (int j = 0; j < jarr.length(); j++)
					{
						if(!is_blank(jarr.getJSONObject(j).getString("descripcion")))
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
		if(is_blank(data))
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
				if(!is_blank(content))
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
		if(is_blank(data))
			return null;

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		long timestamp = getRSSms();
		List<Day> days = new ArrayList<>();
		String desc;

		Date firstTimeFrom = null, secondTimeFrom = null;
		Date secondTimeTo = null;

		String temp = null, rain = null, symbolNo = null;

		float maxTemp = -999.9f;

		String today = null, tonight = null, tomorrow = null, outlook = null;

		boolean doneToday = false, doneTomorrow = false, doneOutlook = false;

        Calendar calendar = Calendar.getInstance();
		Calendar UTCcal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

		long now = System.currentTimeMillis();
		String fcText = (String)KeyValue.readVar("textFC", "");
		if(!is_blank(fcText))
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

            int eventType = parser.getEventType();
			while(eventType != XmlPullParser.END_DOCUMENT)
			{
				String tag = parser.getName();
				if(!is_blank(tag) && tag.equals("product") && eventType == XmlPullParser.START_TAG)
					break;

				eventType = parser.next();
			}

			while(eventType != XmlPullParser.END_DOCUMENT)
			{
				eventType = parser.next();

				if(eventType == XmlPullParser.END_TAG)
					continue;

				String tag = parser.getName();
				if(!is_blank(tag))
				{
					if(tag.equals("location"))
						continue;

					if(tag.equals("time"))
					{
						if(firstTimeFrom == null)
						{
							firstTimeFrom = weeWXApp.getInstance().sdf1.parse(parser.getAttributeValue(null, "from"));
						} else {
							secondTimeFrom = weeWXApp.getInstance().sdf1.parse(parser.getAttributeValue(null, "from"));
							secondTimeTo = weeWXApp.getInstance().sdf1.parse(parser.getAttributeValue(null, "to"));
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

					if(secondTimeFrom != null && secondTimeFrom.getTime() < now)
					{
						firstTimeFrom = secondTimeFrom = secondTimeTo = null;
						temp = rain = symbolNo = null;

						continue;
					}

					if(secondTimeFrom != null)
					{
						calendar.setTime(secondTimeFrom);
						UTCcal.setTime(secondTimeFrom);
					}

					int UTChourOfDay = UTCcal.get(Calendar.HOUR_OF_DAY);

					float tmpTemp = str2Float(temp);
					if(maxTemp < tmpTemp)
						maxTemp = tmpTemp;

					if(UTChourOfDay != 0)
					{
						LogMessage(UTChourOfDay + " != 0, skipping...");

						firstTimeFrom = null;
						if(secondTimeFrom != null)
							secondTimeFrom = null;

						if(secondTimeTo != null)
							secondTimeTo = null;

						temp = rain = symbolNo = null;

						continue;
					}

					LogMessage(UTChourOfDay + " == 0, processing....");

					Day day = new Day();

					if(secondTimeFrom != null)
						LogMessage("secondTimeFrom: " + weeWXApp.getInstance().sdf8.format(secondTimeFrom));

					if(secondTimeTo != null)
						LogMessage("secondTimeTo: " + weeWXApp.getInstance().sdf8.format(secondTimeTo));

					if(days.isEmpty())
						day.timestamp = now;
					else if(secondTimeFrom != null)
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
					if(!is_blank(content))
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
					firstTimeFrom = null;

					if(secondTimeFrom != null)
						secondTimeFrom = null;

					if(secondTimeTo != null)
						secondTimeTo = null;

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
		if(is_blank(data))
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

				String text = weather.getString("description");
				String icon = weather.getString("icon");

				LogMessage("icon: " + icon);

				day.icon = "icons/owm/" + icon + "_t@4x.png";
				String content = weeWXApp.loadFileFromAssets(day.icon);
				if(!is_blank(content))
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
		if(is_blank(data))
			return null;

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		boolean rainInInches = (boolean)KeyValue.readVar("rainInInches", weeWXApp.rain_in_inches_default);
		long timestamp = 0;
		List<Day> days = new ArrayList<>();

		String desc = (String)KeyValue.readVar("forecastLocationName", "");
		if(is_blank(desc))
			desc = "";

		int modhour = getIntervalTime()[1];

		try
		{
			JSONObject jobj = new JSONObject(data);
			jobj = jobj.getJSONObject("properties");

			JSONObject meta = jobj.getJSONObject("meta");
			String updated_at = meta.getString("updated_at");
			JSONArray timeseries = jobj.getJSONArray("timeseries");

			Date df = weeWXApp.getInstance().sdf1.parse(updated_at);
			if(df != null)
			{
				timestamp = df.getTime();
				if(timestamp > 0)
					updateCacheTime(timestamp);
			}

			for(int i = 0; i < timeseries.length(); i++)
			{
				JSONObject jobj2 = timeseries.getJSONObject(i);
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

			Date df = weeWXApp.getInstance().sdf1.parse(jobj2.getString("time"));
			if(df != null)
				day.timestamp = df.getTime();
			else
				day.timestamp = System.currentTimeMillis();

			day.icon = "icons/metno/" + icon + ".svg";
			String content = weeWXApp.loadFileFromAssets(day.icon);
			if(!is_blank(content))
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
		if(is_blank(data))
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

			Date df = weeWXApp.getInstance().sdf6.parse(pubDate);
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

				if(!is_blank(fileName))
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

	static Boolean passesRegularCheck(boolean forced, boolean has_json_combined)
	{
		if(!forced && !KeyValue.isPrefSet(weeWXApp.UPDATE_FREQUENCY))
		{
			if(has_json_combined)
			{
				LogMessage("passesRegularCheck() lastJsonDownload is null or blank...", KeyValue.d);
				KeyValue.putVar("LastWeatherError", getAndroidString(R.string.update_set_to_manual_but_no_content_cached));
				return false;
			}

			LogMessage("passesRegularCheck() Not forced and set to manual updates...");
			return false;
		}

		int pos = (int)KeyValue.readVar(weeWXApp.UPDATE_FREQUENCY, weeWXApp.UpdateFrequency_default);
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
			return false;
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

			return false;
		}

		if(!forced && npwsll.report_time == 0)
		{
			LogMessage("passesRegularCheck() Skipping, report_time == 0, app hasn't been setup...", KeyValue.d);

			if(has_json_combined)
			{
				KeyValue.putVar("LastWeatherError", getAndroidString(R.string.no_download_or_app_not_setup));
				return false;
			}

			return false;
		}

		LogMessage("passesRegularCheck() Last updated check, is " + npwsll.report_time + " < " + npwsll.lastStart + "?");
		if(!forced && npwsll.report_time > npwsll.lastStart)
		{
			LogMessage("passesRegularCheck() !forced && " + npwsll.report_time + " < " + npwsll.lastStart + "...");
			if(has_json_combined)
			{
				LogMessage("passesRegularCheck() lastJsonDownload != null && !isBlank()... Skipping...", KeyValue.d);
				return false;
			}
		}

		return true;
	}

	static boolean notMergeJsonObjects()
	{
		JSONObject json_data, json_last, json_combined;

		//LogMessage("mergeJsonObjects() Loading " + json_keys[0] + "_str from SharedPrefs");
		String json_data_str = (String)KeyValue.readVar(json_keys[0] + "_str", "");
		if(is_blank(json_data_str))
		{
			LogMessage("mergeJsonObjects() json_data_str == null or isBlank()");
			return true;
		}

		try
		{
			json_data = new JSONObject(json_data_str);
		} catch(JSONException je) {
			LogMessage("mergeJsonObjects() Failed turing json_data_str into json_data, je: " + je.getMessage());
			return true;
		}

		if(json_data.length() == 0)
		{
			LogMessage("mergeJsonObjects() json_data.length() == 0");
			return true;
		}

		//LogMessage("mergeJsonObjects() Loading " + json_keys[2] + "_str from SharedPrefs");
		String json_last_str = (String)KeyValue.readVar(json_keys[2] + "_str", "");
		if(is_blank(json_last_str))
		{
			LogMessage("mergeJsonObjects() json_last_str == null or isBlank()");
			return true;
		}

		try
		{
			json_last = new JSONObject(json_last_str);
		} catch(JSONException je) {
			LogMessage("mergeJsonObjects() Failed turing json_last_str into json_last, je: " + je.getMessage());
			return true;
		}

		if(json_last.length() == 0)
		{
			LogMessage("mergeJsonObjects() json_last.length() == 0");
			return true;
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
			return true;
		}

		if(json_combined.length() == 0)
		{
			LogMessage("mergeJsonObjects() json_combined.length() == 0");
			return true;
		}

		String json_combined_str = json_combined.toString();

		//LogMessage("mergeJsonObjects() json_combined_str: " + json_combined_str);
		KeyValue.putVar("json_combined_str", json_combined_str);

		return false;
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
			(boolean)KeyValue.readVar(weeWXApp.RAINRATE_ALERT_WATCH, weeWXApp.rainrate_alert_watch_default),
			(boolean)KeyValue.readVar(weeWXApp.RAINRATE_ALERT_WARNING, weeWXApp.rainrate_alert_warning_default),
			(boolean)KeyValue.readVar(weeWXApp.RAINRATE_ALERT_SEVERE, weeWXApp.rainrate_alert_severe_default),
		};

		if(!rainrate_alerts[0] && !rainrate_alerts[1] && !rainrate_alerts[2])
		{
			LogMessage("checkRainrateAlert() rainrate_alert set to false");
			return;
		}

		LogMessage("checkRainrateAlert() At least one rainrate alert is true");

		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default) &&
						 !(boolean)KeyValue.readVar("rainInInches", weeWXApp.rain_in_inches_default);

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
		int timelen = 0;
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

			String tmpStr = String.format(Locale.getDefault(), "%.1f", totals[period] / 100f) + "mm";
			if(!metric)
				tmpStr = String.format(Locale.getDefault(), "%.2f", totals[period] / 100f) + "in";

			weeWXApp.sendRainrateAlert(tmpStr, level, timelen, timelen_unit);
			LogMessage("checkRainrateAlert() rainfall (" + totals[period] + unit +
					   ") >= rainfall_limit (" + FLOOD_THRESHOLDS[period][level] + unit + ") " +
					   "fell in " + timelen + " " + debugunit + " so notification triggered");
		} else {
			LogMessage("checkRainrateAlert() No threshold reached, so no notification triggered");
		}
	}

	private static int[] getAlertLevel(int[] totals, int[][] thresholds, boolean[] rainrate_alerts)
	{
		for(int level : levels)
		{
			if(!rainrate_alerts[level])
				continue;

			for(int period : periods)
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

//			for(int i = 0; i < temps.outTemp_trend_ts.length; i++)
//			{
//				LogMessage("checkTempAlerts() outTemp_trend_ts[" + i + "]: " + weeWXApp.getInstance().sdf10.format(new Date(temps.outTemp_trend_ts[i])));
//				LogMessage("checkTempAlerts() outTemp_trend_signal[" + i + "]: " + temps.outTemp_trend_signal[i]);
//				LogMessage("checkTempAlerts() outTemp_trend_count[" + i + "]: " + temps.outTemp_trend_count[i]);
//			}
//
//			if(temps.outTemp_trend_signal.length == 0)
//			{
//				LogMessage("checkTempAlerts() temps.outTemp_trend_signal.length == 0, skipping temp limit checks...");
//				return;
//			}

			//long last_ts = temps.outTemp_trend_ts[0];
			int last_signal = temps.outTemp_trend_signal[0];
			//int last_count = temps.outTemp_trend_count[0];

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

		String str = weeWXApp.getInstance().sdf20.format(when);

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
		return weeWXApp.getInstance().sdf19.format(new Date(when)) + " " + getTimeMonth(when) + " " + getShortMonth(when);
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
		if(is_blank(key))
		{
			LogMessage("element '" + element + "' returned no key");
			return null;
		}

		float f = (float)getJson(element, 0f);

		String fmt = KeyValue.getFormat(key);
		if(is_blank(fmt))
		{
			LogMessage("fmt was null or blank");
			LogMessage("element: " + element);
			LogMessage("f: " + f);
			LogMessage("key: " + key);
			return null;
		}

		String fmt_str = String.format(fmt, f);
		if(is_blank(fmt_str))
		{
			LogMessage("fmt_str was null or blank");
			LogMessage("element: " + element);
			LogMessage("f: " + f);
			LogMessage("key: " + key);
			LogMessage("fmt: " + fmt);
			return null;
		}

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
		if(!is_blank(line))
		{
			try
			{
				return new JSONObject(line);
			} catch(Exception ignored) {}
		}

		return null;
	}

	static JSONArray getJSONerrors()
	{
		JSONArray jsonArray;

		String line = (String)KeyValue.readVar("JSONerrors", "");
		if(!is_blank(line))
		{
			try
			{
				jsonArray = new JSONArray(line);
			} catch(Exception e) {
				jsonArray = new JSONArray();
			}
		} else
			jsonArray = new JSONArray();

		return jsonArray;
	}

	static void saveJSONerrors(@NonNull JSONArray jsonArray)
	{
		KeyValue.putVar("JSONerrors", jsonArray.toString());
	}

	static boolean is_valid_url(String url)
	{
		return !is_blank(url) && (url.startsWith("http://") || url.startsWith("https://"));
	}

	static boolean is_blank(String str)
	{
		return str == null || str.isBlank();
	}

	static void noteError(Exception e)
	{
		LogMessage("UpdateCheck.noteError() Error! " + e.getMessage(), KeyValue.e);
		noteError(e.getLocalizedMessage());
	}

	static void noteError(int resId)
	{
		noteError(getAndroidString(resId));
	}

	static void noteError(String error)
	{
		JSONArray jsonArray = getJSONerrors();
		jsonArray.put(error);
		saveJSONerrors(jsonArray);
	}

	static void noteError(int resId, Object[] vars)
	{
		LogMessage("UpdateCheck.noteError() Error! " + String.format(Locale.ENGLISH, getEnglishAndroidString(resId), vars), KeyValue.e);
		noteError(String.format(Locale.getDefault(), getAndroidString(resId), vars));
	}

	static int errorCount()
	{
		JSONArray jsonArray = getJSONerrors();
		return jsonArray.length();
	}

	static void processUpdateInBG(boolean forced, boolean onReceivedUpdate, boolean onAppStart, boolean sendIntents,
	                               boolean weather, boolean forecast, boolean radar, boolean webcam)
	{
		new Thread(() -> processUpdates(forced, onReceivedUpdate, onAppStart, sendIntents, weather, forecast, radar, webcam)).start();
	}

	static void processUpdates(boolean forced, boolean onReceivedUpdate, boolean onAppStart, boolean sendIntents,
                               boolean weather, boolean forecast, boolean radar, boolean webcam)
	{
		if(notCheckConnection() && !forced)
		{
			LogMessage("weeWXAppCommon.processUpdates() Not on wifi and not a forced refresh, skipping...", KeyValue.d);
			if(sendIntents)
			{
				if(weather)
					weeWXNotificationManager.updateNotificationMessage(STOP_WEATHER_INTENT);

				if(forecast)
					weeWXNotificationManager.updateNotificationMessage(STOP_FORECAST_INTENT);

				if(radar)
					weeWXNotificationManager.updateNotificationMessage(STOP_RADAR_INTENT);

				if(webcam)
					weeWXNotificationManager.updateNotificationMessage(STOP_WEBCAM_INTENT);
			}

			return;
		}

		if(downloader != null && downloader.isRunning() && downloader.startTime + 5_000L > System.currentTimeMillis())
		{
			LogMessage("weeWXAppCommon.processUpdates() downloader is running already, skipping update...", KeyValue.d);
			if(sendIntents)
			{
				if(weather)
					weeWXNotificationManager.updateNotificationMessage(STOP_WEATHER_INTENT);

				if(forecast)
					weeWXNotificationManager.updateNotificationMessage(STOP_FORECAST_INTENT);

				if(radar)
					weeWXNotificationManager.updateNotificationMessage(STOP_RADAR_INTENT);

				if(webcam)
					weeWXNotificationManager.updateNotificationMessage(STOP_WEBCAM_INTENT);
			}

			return;
		}

		if(forced && downloader != null && downloader.isRunning())
			downloader.shutdown();

		if(downloader != null && !downloader.isRunning())
			downloader = null;

		try
		{
			List<Integer> idtype = new ArrayList<>();
			List<String> urls = new ArrayList<>();
			List<String> contentTypes = new ArrayList<>();
			List<Object> PossibleErrors = new ArrayList<>();

			String fctype = "", forecastURL = "";
			boolean hasForecastGson = false;

			ForecastDefaults fcDef = null;

			if(weather)
			{
				boolean has_json_combined = false;

				JSONObject jsonObject = getJson();
				if(jsonObject != null && jsonObject.length() != 0)
					has_json_combined = true;

				if(has_json_combined)
				{
					Boolean passes = passesRegularCheck(forced, true);
					if(!passes)
					{
						LogMessage("getWeather() passesRegularCheck(): false");
						weather = false;
					}
				}
			}

			if(weather)
			{
				for(int i = 0; i < 3; i++)
				{
					String JSONurl = (String)KeyValue.readVar(json_keys[i] + "_url", "");
					if(is_valid_url(JSONurl))
					{
						Calendar cal1 = Calendar.getInstance();
						Calendar cal2 = Calendar.getInstance();
						cal2.setTimeInMillis((long)KeyValue.readVar(json_keys[i] + TIME_EXT, 0L));

						if(i == 1 && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR))
							continue;

						if(i == 2 && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
						   cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR))
							continue;

						idtype.add(i);
						urls.add(JSONurl);
						contentTypes.add("JSON");
						PossibleErrors.add(new Object[]{R.string.wasnt_able_to_connect_or_download, new Object[]{json_labels[i], JSONurl}});
					}
				}
			}

			if(forecast)
			{
				fctype = (String)KeyValue.readVar(weeWXApp.FCTYPE, "");
				if(is_blank(fctype))
				{
					LogMessage("UpdateCheck.java Unable to get forecast defaults for fctype: " + fctype, KeyValue.w);
					forecast = false;
				}
			}

			if(forecast)
			{
				fcDef = weeWXApp.getFCdefs(fctype);
				if(fcDef == null)
				{
					LogMessage("UpdateCheck.java Unable to get forecast defaults for fctype: " + fctype, KeyValue.w);
					noteError("Unable to get forecast defaults for fctype: " + fctype);
					forecast = false;
				}
			}

			if(forecast)
			{
				LogMessage("weeWXAppCommon.processUpdates() fctype: " + fctype);

				if(fctype.equals("weatherzone3") || fctype.equals("metservice2"))
				{
					LogMessage("weeWXAppCommon.processUpdates() fctype == weatherzone3 || metservice2, skipping...", KeyValue.d);
					noteError(R.string.forecast_type_is_invalid, new Object[]{fctype});
					forecast = false;
				}
			}

			if(forecast)
			{
				String forecast_url = (String)KeyValue.readVar("FORECAST_URL", "");
				if(is_blank(forecast_url))
				{
					LogMessage("weeWXAppCommon.processUpdates() FORECAST_URL == null || isBlank(), skipping...", KeyValue.e);
					noteError(R.string.forecast_url_not_set, new Object[]{"inigo-settings.txt"});
					forecast = false;
				}
			}

			if(forecast)
			{
				int pos = (int)KeyValue.readVar(weeWXApp.UPDATE_FREQUENCY, weeWXApp.UpdateFrequency_default);
				if(!forced && pos == 0)
				{
					if(!hasForecastGson)
						LogMessage("weeWXAppCommon.processUpdates() hasForecastGson is false, skipping...", KeyValue.w);
					else
						LogMessage("weeWXAppCommon.processUpdates() hasForecastGson is true, skipping...", KeyValue.w);

					forecast = false;
				}
			}

			long now = System.currentTimeMillis();
			LogMessage("weeWXAppCommon.processUpdates() current_time: " + now);

			long rssCheckTime = getRSSms();
			LogMessage("weeWXAppCommon.processUpdates() rssCheckTime: " + rssCheckTime);

			long lastAttemptedForecastDownload = getLAFDms();

			if(forecast)
			{
				if(rssCheckTime == 0)
				{
					if(!hasForecastGson)
						LogMessage("weeWXAppCommon.processUpdates() hasForecastGson is false, skipping...", KeyValue.e);
					else
						LogMessage("weeWXAppCommon.processUpdates() hasForecastGson is true, skipping...", KeyValue.e);

					forecast = false;
				}
			}

			if(forecast)
			{
				long dur = (now - lastAttemptedForecastDownload) / 1000;
				if(!forced && lastAttemptedForecastDownload > 0 && dur < fcDef.delay_before_downloading)
				{
					LogMessage("weeWXAppCommon.processUpdates() !forced and last attempt was less than " + fcDef.delay_before_downloading +
							   "s ago (" + dur + "s ago)");
					forecast = false;
				}
			}

			if(forecast)
			{
				long dur = (now - rssCheckTime) / 1000;
				if(!forced && dur < fcDef.default_forecast_refresh)
				{
					LogMessage("weeWXAppCommon.processUpdates() !forced and cache isn't more than " +
							   fcDef.default_forecast_refresh + "s old (" + dur + "s ago), skipping...");
					forecast = false;
				}
			}

			if(forecast)
			{
				forecastURL = (String)KeyValue.readVar("FORECAST_URL", "");
				if(is_valid_url(forecastURL))
				{
					idtype.add(3);
					urls.add(forecastURL);
					contentTypes.add("HTML");
					PossibleErrors.add(R.string.wasnt_able_to_connect_forecast);
					KeyValue.putVar("lastAttemptedForecastDownloadTime", now);
				}
			}

			if(radar)
			{
				String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
				if(radtype != null && radtype.equals("image"))
				{
					String radarURL = (String)KeyValue.readVar("RADAR_URL", "");
					if(!is_blank(radarURL))
					{
						idtype.add(4);
						urls.add(radarURL);
						contentTypes.add("IMAGE");
						PossibleErrors.add(R.string.wasnt_able_to_connect_radar_image);
					}
				}
			}

			if(webcam)
			{
				String webcam_url = (String)KeyValue.readVar("WEBCAM_URL", "");
				if(!is_blank(webcam_url))
				{
					idtype.add(5);
					urls.add(webcam_url);
					contentTypes.add("IMAGE");
					PossibleErrors.add(R.string.wasnt_able_to_connect_webcam_url);
				}
			}

			if(urls.isEmpty())
			{
				LogMessage("weeWXAppCommon.processUpdates() No jobs to run...", KeyValue.w);
				if(sendIntents)
				{
					if(weather)
						weeWXNotificationManager.updateNotificationMessage(STOP_WEATHER_INTENT);

					if(forecast)
						weeWXNotificationManager.updateNotificationMessage(STOP_FORECAST_INTENT);

					if(radar)
						weeWXNotificationManager.updateNotificationMessage(STOP_RADAR_INTENT);

					if(webcam)
						weeWXNotificationManager.updateNotificationMessage(STOP_WEBCAM_INTENT);
				}

				return;
			}

			boolean updatedWeather = false;
			boolean updatedForecast = false;
			boolean updatedRadar = false;
			boolean updatedWebcam = false;

			downloader = new ParallelDownloader(urls.size());
			List<ParallelDownloader.DownloadResult> results = downloader.downloadAll(idtype, urls, contentTypes);

			boolean allOk = results.stream().allMatch(ParallelDownloader.DownloadResult::success);
			if(!allOk)
			{
				List<ParallelDownloader.DownloadResult> failed = results.stream().filter(r -> !r.success()).toList();
				for(ParallelDownloader.DownloadResult r : failed)
				{
					LogMessage("MainActivity.processSettings(" + r.id() + ") Error! " + r.error(), KeyValue.e);

					Object obj = PossibleErrors.get(r.id());
					if(obj instanceof Object[] objects)
					{
						noteError((int)objects[0], (Object[])objects[1]);

					} else if(obj instanceof Integer errorId)
						noteError(errorId);
				}
			} else {
				int UpdateInterval = (int)KeyValue.readVar("UpdateInterval", 0);
				int modhour = getIntervalTime(UpdateInterval)[1];

				boolean needToMerge = false;
				boolean Processing_Errors = false;
				List<ParallelDownloader.DownloadResult> succeeded = results.stream().toList();
				for(ParallelDownloader.DownloadResult r : succeeded)
				{
					LogMessage("processUpdates() r.id: " + r.id());
					if(0 <= r.id() && r.id() <= 2)
					{
						Boolean ret = processWeather(r.id(), r.string());
						if(Boolean.TRUE.equals(ret))
						{
							if(r.id() != 1)
							{
								updatedWeather = true;
								needToMerge = true;
							} else if(KeyValue.parseDicts()) {
								LogMessage("Failed to process inigo-dicts.json file", KeyValue.e);
								noteError(R.string.failed_to_process_units, new Object[]{json_labels[r.id()]});
							}
						} else if(Boolean.FALSE.equals(ret)) {
							LogMessage("Failed to process " + json_labels[r.id()] + " file", KeyValue.w);
						} else if(ret == null) {
								LogMessage("Failed to process " + json_labels[r.id()] + " file", KeyValue.e);
								noteError(R.string.failed_to_process_weather_data, new Object[]{json_labels[r.id()]});
						}
					}

					if(r.id() == 3)
					{
						Result3 r3 = processForecast(modhour, fctype, r.string(), forecastURL);

						if(!r3.succeeded())
							noteError(r3.error());
						else
							updatedForecast = true;
					}

					if(r.id() == 4 && r.contentType().equals("IMAGE"))
					{
						Bitmap bm = r.bm();
						File file = getFile(weeWXApp.radarFilename);
						try(FileOutputStream out = new FileOutputStream(file))
						{
							LogMessage("Attempting to save to " + file.getAbsoluteFile());
							bm.compress(Bitmap.CompressFormat.JPEG, 85, out);
							LogMessage("Got past the save... ");
							updatedRadar = true;
						} catch(Exception e) {
							LogMessage(ERROR_E + e, true, KeyValue.e);
							noteError(e);
						}
					}

					if(r.id() == 5)
					{
						Bitmap bm = r.bm();
						File file = getFile(weeWXApp.webcamFilename);
						try(FileOutputStream out = new FileOutputStream(file))
						{
							LogMessage("Attempting to save to " + file.getAbsoluteFile());
							bm.compress(Bitmap.CompressFormat.JPEG, 85, out);
							LogMessage("Got past the save... ");
							updatedWebcam = true;
						} catch(Exception e) {
							LogMessage(ERROR_E + e, true, KeyValue.e);
							noteError(e);
						}
					}
				}

				if(needToMerge && notMergeJsonObjects())
					noteError(R.string.failed_to_merge_weather_data, new Object[]{json_labels[0], json_labels[2]});

				if(updatedWeather)
				{
					checkTempAlerts();
					checkRainfallAlert();
					checkRainrateAlert();
					updateAppWidget();
				}

				if(sendIntents)
				{
					if(errorCount() > 0)
					{
						LogMessage("sending UPDATE_ERRORS intent...");
						weeWXNotificationManager.updateNotificationMessage(UPDATE_ERRORS);
						LogMessage("sent UPDATE_ERRORS intent...");
					} else {
						if(updatedWeather)
						{
							LogMessage("sending REFRESH_WEATHER_INTENT intent...");
							weeWXNotificationManager.updateNotificationMessage(REFRESH_WEATHER_INTENT);
							LogMessage("sent REFRESH_WEATHER_INTENT intent...");
						}

						if(updatedForecast)
						{
							LogMessage("sending REFRESH_FORECAST_INTENT intent...");
							weeWXNotificationManager.updateNotificationMessage(REFRESH_FORECAST_INTENT);
							LogMessage("sent REFRESH_FORECAST_INTENT intent...");
						}

						if(updatedRadar)
						{
							LogMessage("sending REFRESH_RADAR_INTENT intent...");
							weeWXNotificationManager.updateNotificationMessage(REFRESH_RADAR_INTENT);
							LogMessage("sent REFRESH_RADAR_INTENT intent...");
						}

						if(updatedWebcam)
						{
							LogMessage("sending REFRESH_WEBCAM_INTENT intent...");
							weeWXNotificationManager.updateNotificationMessage(REFRESH_WEBCAM_INTENT);
							LogMessage("sent REFRESH_WEBCAM_INTENT intent...");
						}
					}
				}
			}
		} catch(Exception e) {
			LogMessage("UpdateCheck.runInTheBackground() Error! e: " + e.getMessage(), KeyValue.e);
		}
	}

	static Boolean processWeather(int id, String weatherStr) throws JSONException
	{
		if(is_blank(weatherStr))
			return false;

		JSONObject jsonObject = new JSONObject(weatherStr);

		if(jsonObject.length() == 0 || !jsonObject.has("version"))
		{
			LogMessage("processWeather() jsonObject.length() == 0 || jsonObject didn't have a version");
			return false;
		}

		int version = jsonObject.optInt("version", 0);
		if(version < weeWXApp.minimum_inigo_version)
		{
			LogMessage("processWeather() sendAlert() triggered because version (" + version +
					   ") < weeWXApp.minimum_inigo_version (" + weeWXApp.minimum_inigo_version + ")");
			weeWXNotificationManager.updateNotificationMessage(INIGO_INTENT);
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
					LogMessage("processWeather() Error in " + json_labels[id] + ": " + jarr.optString(i), KeyValue.e);

				return null;
			}
		}

		long now = System.currentTimeMillis();

		KeyValue.putVar(json_keys[id] + TIME_EXT, now);
		KeyValue.putVar(json_keys[id] + "_str", jsonObject.toString());
		KeyValue.putVar("LastWeatherError", null);

		LogMessage("processWeather() Last Server Update Time: " + weeWXApp.getInstance().sdf14.format(jsonObject.optInt("report_time") * 1_000L));
		LogMessage("processWeather() LastDownloadTime: " + weeWXApp.getInstance().sdf14.format(now));

		return true;
	}

	static Boolean reallyGetWeather(int id, String url, boolean force) throws IOException, JSONException
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
			long json_time = (long)KeyValue.readVar(json_keys[id] + TIME_EXT, 0L);
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

		String line = downloadString(url);
		Boolean ret = processWeather(id, line);
		if(ret == null)
		{
			weeWXNotificationManager.updateNotificationMessage(PROCESSING_ERRORS);
			return null;
		}

		return ret;
	}

	//	https://stackoverflow.com/questions/3841317/how-do-i-see-if-wi-fi-is-connected-on-android
	static boolean notCheckConnection()
	{
		if(!(boolean)KeyValue.readVar("onlyWIFI", weeWXApp.onlyWIFI_default))
			return false;

		ConnectivityManager connMgr = (ConnectivityManager)weeWXApp.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
		if(connMgr == null)
			return true;

		Network network = connMgr.getActiveNetwork();
		if(network == null)
			return true;

		NetworkCapabilities capabilities = connMgr.getNetworkCapabilities(network);
		return capabilities == null || !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
	}

	// https://stackoverflow.com/questions/8710515/reading-an-image-file-into-bitmap-from-sdcard-why-am-i-getting-a-nullpointerexc
	private static Bitmap combineImage(Bitmap bmp1, String fnum, String snum)
	{
		Context context = weeWXApp.getInstance();
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

	static String downloadSettings(String url) throws IOException
	{
		KeyValue.putVar("SETTINGS_URL", url);

		String cfg = downloadString(url);

		if(cfg == null)
			return null;

		if(cfg.startsWith(UTF8_BOM))
			cfg = cfg.substring(1).strip();

		return cfg;
	}

	static String downloadString(String url) throws IOException
	{
		if(!is_valid_url(url))
			return null;

		OkHttpClient client = NetworkClient.getInstance(url);

		return reallyDownloadString(client, url, 0);
	}

	private static String reallyDownloadString(OkHttpClient client, String url, int retries) throws IOException
	{
		LogMessage("reallyDownloadString() checking if url  " + url + " is valid, attempt " + (retries + 1));
		Request request = NetworkClient.getRequest(false, url);

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
				if(!is_blank(bodyStr))
					error += ", body: " + bodyStr;
				LogMessage("reallyDownloadString() Error! error: " + error, true, KeyValue.w);
				throw new IOException(error);
			}

			return bodyStr;
		}
	}

	static String downloadString(String url, Map<String, String> args) throws IOException
	{
		if(is_blank(url) || args == null || args.isEmpty())
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

		OkHttpClient client = NetworkClient.getInstance(url);

		return reallyDownloadString(client, requestBody, url, 0);
	}

	private static String reallyDownloadString(OkHttpClient client, RequestBody requestBody, String url, int retries) throws IOException
	{
		LogMessage("reallyDownloadString() checking if url  " + url + " is valid, attempt " + (retries + 1));

		Request request = NetworkClient.getRequest(false, url);
		if(request == null)
			return null;

		request = request.newBuilder().post(requestBody).build();

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

//			if(false)
//			{
//				if(retries < maximum_retries)
//				{
//					retries++;
//
//	//				LogMessage("reallyDownloadString() Error! e: " + e.getMessage() + ", retry: " + retries +
//	//						   ", will sleep " + retry_sleep_time + " seconds and retry...", true);
//
//					try
//					{
//						Thread.sleep(retry_sleep_time);
//					} catch (InterruptedException ie) {
//						Thread.currentThread().interrupt();
//						throw ie;
//					}
//
//					return reallyDownloadString(client, requestBody, url, retries);
//				}
//			}

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

			OkHttpClient client = NetworkClient.getInstance(url);

			LogMessage("uploadMissingIcon() Sending everything to reallyUploadMissingIcon()");

			reallyUploadMissingIcon(client, requestBody, url, 0);
		});
	}

	private static void reallyUploadMissingIcon(OkHttpClient client, RequestBody requestBody, String url, int retries)
	{
		Exception lastException = null;

		LogMessage("reallyUploadString() checking if url " + url + " is valid, attempt: #" + (retries + 1));

		Request request = NetworkClient.getRequest(false, url);
		if(request == null)
			return;

		request = request.newBuilder().post(requestBody).build();

		try(Response response = client.newCall(request).execute())
		{
			String bodyStr = response.body().string();
			if(response.isSuccessful())
			{
				if(bodyStr.equals("OK"))
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
			lastException = e;
		}

		if(lastException != null)
		{
			if(retries < maximum_retries)
			{
				retries++;

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
			LogMessage(ERROR_E + e, true, KeyValue.e);
		}

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
		} while(wzHTML.length() < 10_000 && attempt++ < attempts);

		if(wzHTML != null)
			LogMessage("wzHTML.length(): " + wzHTML.length());

		return wzHTML;
	}

	@SuppressWarnings("unused")
    static String prettyHTML(String html)
	{
		return Jsoup.parse(html).outputSettings(new Document.OutputSettings()
				.indentAmount(2).prettyPrint(true)).outerHtml();
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
		if(is_blank(content))
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

	static Result3 processWZ2(String url) throws IOException
	{
		String forecast_text_url = url.substring(0, url.lastIndexOf("/"));
		LogMessage("reallyGetForecast() Trying secondary URL for forecasts strings: " + forecast_text_url);

		int attempts = 0;
		Result r1 = null;
		Result2 r2 = null;
		String wzHTML = null;

        String forecastData = null;
		if(DEBUG)
		{
			try
			{
				wzHTML = CustomDebug.readDebug("R2_body.html");
				LogMessage("reallyGetForecast() wzHTML: " + wzHTML);
				if(wzHTML != null && wzHTML.length() > 128)
				{
					r2 = JsoupHelper.processWZ2GetForecastStrings(wzHTML);
				}
			} catch(Exception e) {
				LogMessage(ERROR_E + e.getMessage(), true, KeyValue.e);
				doStackOutput(e);
				return new Result3(false, e.getLocalizedMessage(), null);
			}
		}

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

				if(wzHTML.length() > 128 && KeyValue.debugging_on())
					CustomDebug.writeDebug("R2_body.html", wzHTML);

				LogMessage("reallyGetForecast() Got data from WZ, let's try to find forecast strings in it...");
				r2 = JsoupHelper.processWZ2GetForecastStrings(wzHTML);
			} while(attempts++ < 3 && (r2 == null || r2.rc() == 0));

			if(is_blank(wzHTML))
			{
				LogMessage("Nothing substantial was returned from WZ...", KeyValue.w);
				return new Result3(false, "Nothing substantial was returned from WZ...", null);
			}

			if(wzHTML.startsWith("error|"))
			{
				String[] errors = wzHTML.strip().split("\\|", 2);
				LogMessage("Error! errors[1]: " + errors[1], KeyValue.w);
				return new Result3(false, errors[1], null);
			}
		}

		if(r2 == null || r2.rc() == 0)
		{
			if(!is_blank(wzHTML))
				CustomDebug.writeDebug("R2_body.html", wzHTML);

			LogMessage("reallyGetForecast() Nothing substantial was returned from WZ...", KeyValue.w);
			return new Result3(false, "Nothing substantial was returned from WZ...", null);
		}

		LogMessage("reallyGetForecast() Got forecast strings from WZ... rc: " + r2.rc());

		if(DEBUG && KeyValue.debugging_on())
		{
			try
			{
				forecastData = CustomDebug.readDebug("R1_body.html");
				LogMessage("reallyGetForecast() forecastData: " + forecastData);
				if(forecastData != null && forecastData.length() > 128)
					r1 = JsoupHelper.processWZ2Forecasts(url, forecastData, r2);
			} catch(Exception e) {
				LogMessage(ERROR_E + e.getMessage(), true, KeyValue.e);
				doStackOutput(e);
				return new Result3(false, e.getLocalizedMessage(), null);
			}
		}

		if(r1 == null || r1.days() == null || r1.days().isEmpty())
		{
			attempts = 0;
			do
			{
				forecastData = getWZHTML(url, false);
				if(forecastData == null)
					break;

				if(is_blank(forecastData))
					continue;

				if(forecastData.startsWith("error|"))
					break;

				if(forecastData.length() > 128 && KeyValue.debugging_on())
					CustomDebug.writeDebug("R1_body.html", forecastData);

				LogMessage("reallyGetForecast() Got data from WZ, let's try to find forecast blocks in it...");
				r1 = JsoupHelper.processWZ2Forecasts(url, forecastData, r2);
			} while(attempts++ < 3 && (r1 == null || r1.days() == null || r1.days().isEmpty()));

			if(is_blank(forecastData))
			{
				LogMessage("Nothing substantial was returned from WZ...", KeyValue.w);
				return new Result3(false, "Nothing substantial was returned from WZ...", null);
			}

			if(forecastData.startsWith("error|"))
			{
				String[] errors = forecastData.strip().split("\\|", 2);
				LogMessage("Error! errors[1]: " + errors[1], KeyValue.w);
				KeyValue.putVar("LastForecastError", errors[1]);
				return new Result3(false, errors[1], null);
			}
		}

		if(r1 == null || r1.days() == null || r1.days().isEmpty())
		{
			if(is_blank(forecastData))
				CustomDebug.writeDebug("R1_body.html", forecastData);

			LogMessage("reallyGetForecast() Failed to find any forecast blocks, giving up...", KeyValue.w);
			return new Result3(false, "Nothing substantial was returned from WZ...", null);
		}

		return new Result3(true, null, r1);
	}

	static Result3 processForecast(int modhour, String fctype, String forecastData, String url) throws IOException
	{
		Result r1 = null;
		if(fctype.equals("weatherzone2"))
		{
			Result3 r3 = processWZ2(url);
			if(!r3.succeeded())
				return r3;

			r1 = r3.result();
		} else {
			if(is_blank(forecastData))
			{
				LogMessage("reallyGetForecast() Failed to get any forecast blocks after 3 attempts... giving up...", KeyValue.w);
				return new Result3(false, getAndroidString(R.string.failed_to_process_forecast_data), null);
			}

			if(forecastData.length() > 128 && KeyValue.debugging_on())
			{
				try
				{
					CustomDebug.writeDebug("forecast.html", forecastData);
				} catch(Exception e) {
					LogMessage("reallyGetForecast() Debug write failed: " + e.getMessage(), KeyValue.w);
				}
			}
		}

		switch(fctype)
		{
			case "aemet.es" -> r1 = processAEMET(forecastData);
			case "bom2" -> r1 = JsoupHelper.processBoM2(forecastData);
			case "bom3daily" -> r1 = processBOM3Daily(forecastData, true);
			case "bom3hourly" -> r1 = processBOM3(modhour, forecastData, url);
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
				return new Result3(false, fmtStr, null);
			}
		}

		if(r1 == null || r1.days() == null || r1.days().isEmpty())
		{
			LogMessage("reallyGetForecast() Failed to process forecast data, giving up...", KeyValue.w);
			return new Result3(false, getAndroidString(R.string.failed_to_process_forecast_data), null);
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
		KeyValue.putVar(RSS_CHECK, gh.timestamp);
		KeyValue.putVar("LastForecastError", null);

		return new Result3(true, null, r1);
	}

	static boolean reallyGetForecast(String fctype, String url, int modhour) throws IOException
	{
		if(fctype.equals("metservice2") || fctype.equals("weatherzone3"))
			return false;

		LogMessage("reallyGetForecast() forcecastURL: " + url);

		if(is_blank(url))
		{
			String tmpStr = getAndroidString(R.string.forecast_url_not_set);
			tmpStr = String.format(Locale.getDefault(), tmpStr, "inigo-settings.txt");
			KeyValue.putVar("LastForecastError", tmpStr);
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
		if(!fctype.equals("weatherzone2"))
		{
			forecastData = downloadString(url);

			if(is_blank(forecastData))
			{
				LogMessage("reallyGetForecast() Failed to get any forecast blocks after 3 attempts... giving up...", KeyValue.w);
				KeyValue.putVar("LastForecastError", getAndroidString(R.string.failed_to_process_forecast_data));
				return false;
			}

			if(forecastData.length() > 128 && KeyValue.debugging_on())
			{
				try
				{
					CustomDebug.writeDebug("forecast.html", forecastData);
				} catch(Exception e) {
					LogMessage("reallyGetForecast() Debug write failed: " + e.getMessage(), KeyValue.w);
				}
			}
		}

		if(is_blank(KeyValue.countyName))
		{
			String textFC = downloadString("https://www.met.ie/Open_Data/json/" + KeyValue.countyName + ".json");
			KeyValue.putVar("textFC", textFC);
		}

		Result3 r3 = processForecast(modhour, fctype, forecastData, url);
		return r3.succeeded();
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
			LogMessage(ERROR_E + e, true, KeyValue.e);
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
				LogMessage(ERROR_E + e, true, KeyValue.e);
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

	static Bitmap loadOrDownloadImage(String url, String filename) throws InterruptedException, IOException
	{
		Bitmap bm = null;
		File file = getFile(filename);

		LogMessage("Starting to download image from: " + url);
		if(url.toLowerCase(Locale.ENGLISH).strip().endsWith(".mjpeg") ||
		   url.toLowerCase(Locale.ENGLISH).strip().endsWith(".mjpg"))
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
				LogMessage(ERROR_E + e, true, KeyValue.e);
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
		if(is_blank(url))
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
		if(request == null)
			return null;

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

//			LogMessage("reallyCheckURL() Error! e: " + e.getMessage(), true, KeyValue.e);

			doStackOutput(e);

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
		if(is_blank(url))
		{
			LogMessage("url is null or blank, bailing out...", KeyValue.d);
			throw new IOException("url is null or blank, bailing out...");
		}

		OkHttpClient client = NetworkClient.getStream(url);

		return reallyGrabMjpegFrame(client, url, 0);
	}

	static Bitmap reallyGrabMjpegFrame(OkHttpClient client, String url, int retries) throws InterruptedException, IOException
	{
		Bitmap bm;
		InputStream urlStream = null;

		LogMessage("reallyGrabMjpegFrame() checking if url  " + url + " is valid, attempt " + (retries + 1));

		Request request = NetworkClient.getRequest(false, url);
		if(request == null)
			return null;

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

		if(retries < maximum_retries)
		{
			retries++;

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

		if(!is_blank(tmpImg))
			tmpImg= "file:///android_asset/" + tmpImg;

		return tmpStr + tmpImg + "'/>";
	}

	@SuppressWarnings("unused")
    static int getDirection(String direction)
	{
		int dir;

		switch(direction.toLowerCase(Locale.ENGLISH))
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
		if(is_blank(cssname))
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
		if(is_blank(f))
			return 0;

		String tmp = f.replaceAll("[^0-9-.]", "").strip();

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

	@SuppressWarnings("unused")
    static float mps2kmph(float mps)
	{
		return round(mps * 3.6f, 1);
	}

	@SuppressWarnings("unused")
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

	static String cssToSVG(String cssname, Integer Angle)
	{
		if(is_blank(cssname))
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

		String string_time = weeWXApp.getInstance().sdf8.format(now);
		LogMessage("getNPWSLL() now: " + string_time);

		long[] ret = getPeriod();

		LogMessage("Got the following: " + Arrays.toString(ret));

		long period = ret[0];
		long wait = ret[1];

		//LogMessage("Here1");

		long report_time = getLDTms() + wait;

		string_time = weeWXApp.getInstance().sdf8.format(report_time);
		LogMessage("getNPWSLL() report_time: " + string_time);

		if(period <= 0)
			return new NPWSLL(now, period, wait, 0L, 0L, report_time);

		//LogMessage("Here2");

		long start = Math.round((double)now / (double)period) * period;

		string_time = weeWXApp.getInstance().sdf8.format(start);
		LogMessage("getNPWSLL() start: " + string_time);

		start += wait;

		string_time = weeWXApp.getInstance().sdf8.format(start);
		LogMessage("getNPWSLL() start+wait: " + string_time);

		while(start < now + 15_000L)
			start += period;

		string_time = weeWXApp.getInstance().sdf8.format(start);
		LogMessage("getNPWSLL() next start: " + string_time);

		long lastStart = start - period;

		string_time = weeWXApp.getInstance().sdf8.format(lastStart);
		LogMessage("getNPWSLL() lastStart: " + string_time);

		return new NPWSLL(now, period, wait, start, lastStart, report_time);
	}

	static class weeWXNotificationManager
	{
	    private static final EventBroadcaster<String> broadcaster = new EventBroadcaster<>();

	    public static void updateNotificationMessage(String message)
		{
	        broadcaster.broadcast(message);
	    }

	    public static void observeNotifications(LifecycleOwner owner, Observer<String> observer)
		{
	        broadcaster.observe(owner, observer);
	    }

	    public static void removeNotificationObserver(Observer<String> observer)
		{
	        broadcaster.removeObserver(observer);
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

	static String deg2Str(String degree_element)
	{
		if(!hasElement(degree_element))
			return "N/A";

		float degrees = (float)getJson(degree_element, 0f);

		int index = (int)Math.round(degrees / 22.5) % 16;
		return direction_labels[index];
	}
}