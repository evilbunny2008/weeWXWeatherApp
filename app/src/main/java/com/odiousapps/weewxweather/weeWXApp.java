package com.odiousapps.weewxweather;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.os.Build;
import android.os.LocaleList;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.caverock.androidsvg.PreserveAspectRatio;
import com.caverock.androidsvg.SVG;
import com.github.evilbunny2008.colourpicker.CPEditText;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

@SuppressWarnings({"unused", "SameParameterValue"})
public class weeWXApp extends Application
{
	private static final String html_header = """
	<!DOCTYPE html>
	<html lang='CURRENT_LANG'>
		<head>
			<meta charset='utf-8'>
			<meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no'>
			<meta name='color-scheme' content='light dark'>
	""";

	static final String script_header = """
			<script>
				document.addEventListener("DOMContentLoaded", function()
				{
					const btn = document.getElementById("scrollToTop");

					window.addEventListener("scroll", () =>
					{
						// At the top? Hide the button
						if(document.documentElement.scrollTop > 100)
							btn.classList.add("show");
						else
							btn.classList.remove("show");
					});

					btn.addEventListener("click", () =>
					{
						window.scrollTo(
						{
							top: 0,
							behavior: "smooth"
						});
					});
				});
			</script>
	""";

	static final String html_header_rest = """
		</head>
		<body>
	""";

	static final String inline_arrow = """
			<!-- Floating scroll-to-top button -->
			<div id="scrollToTop">
				<svg viewBox="0 0 24 24">
					<path d="M12 4l-7 8h4v8h6v-8h4z"/>
				</svg>
			</div>
	""";

	static final String html_footer = """
		</body>
	</html>
	""";

	static final String about_blurb = """
		Big thanks to the <a href='https://weewx.com'>weeWX project</a>, as this app
		wouldn't be possible otherwise.<br><br>
		Weather Icons from <a href='https://www.flaticon.com/'>FlatIcon</a> and
		is licensed under <a href='https://creativecommons.org/licenses/by/3.0/'>CC 3.0 BY</a> and
		<a href='https://github.com/erikflowers/weather-icons'>Weather Font</a> by Erik Flowers<br><br>
		weeWX Weather App v
	""" + BuildConfig.VERSION_NAME + """
	    is by <a href='https://odiousapps.com'>OdiousApps</a>.
	""";

	final static String debug_html = """
			<div id='widthDisplay'
				style='position: fixed; top: 10px; right: 10px;
				background: rgba(0,0,0,0.7); color: #fff;
				padding: 5px 10px; border-radius: 5px;
				font-family: monospace; z-index: 9999;'>
			</div>

			<script>
				const display = document.getElementById('widthDisplay');

				function updateWidth() {
					display.textContent = 'Width: ' + window.innerWidth + 'px ' +
					'x Height: ' + window.innerHeight + 'px';
				}

				// Update immediately
				updateWidth();

				// Update on resize
				window.addEventListener('resize', updateWidth);
			</script>
	""";

	private static final String dialog_html_header = """
	<!doctype html>
	<html lang="REPLACE_WITH_LANG">
	<head>
		<meta charset="utf-8" />
		<meta name="viewport" content="width=device-width,initial-scale=1" />
		<title>Warning UI Mock</title>
	""";

	private static final String dialog_html_header_rest = """
	</head>
	<body>
		<!-- Example: full "modal-like" warning -->
		<div class="warn-wrap" role="alertdialog" aria-labelledby="w-title" aria-describedby="w-desc" tabindex="0">
			<div class="warn-accent" aria-hidden="true"></div>
			<div class="warn-icon" aria-hidden="true">
				<!-- inline SVG warning icon -->
				<svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true" focusable="false">
					<path d="M11.03 3.5c.37-.9 1.64-.9 2.01 0l7.04 17.06A1.5 1.5 0 0 1 19.67 22H4.33a1.5 1.5 0 0 1-1.41-1.44L10 3.5z" fill="currentColor" opacity="0.12"/>
					<path d="M12 8.25c-.41 0-.75.34-.75.75v4.5c0 .41.34.75.75.75s.75-.34.75-.75v-4.5c0-.41-.34-.75-.75-.75zm0 8.5a.9.9 0 1 1 0 1.8.9.9 0 0 1 0-1.8z" fill="currentColor"/>
				</svg>
			</div>
			<div class="warn-body">
			<p id="w-desc" class="warn-desc">
				WARNING_BODY
			</p>
			</div>
		</div>
	</body>
	</html>
	""";

	static String current_html_headers;

	static String current_dialog_html;

	final static String emptyField = "<div style='span:3;'>\u00A0</div>\n";
	final static String currentSpacer = "\t\t\t<div class='currentSpacer'>\u00A0</div>\n";

	private static weeWXApp instance = null;
	private Colours colours;
	private static int lastNightMode = -1;

	final static int minimum_inigo_version = 4000;

	final static boolean radarforecast_default = false;
	final static boolean disableSwipeOnRadar_default = false;
	final static boolean onlyWIFI_default = false;
	final static boolean metric_default = true;
	final static boolean showIndoor_default = true;
	final static boolean use_exact_alarm_default = false;
	final static boolean save_app_debug_logs = false;
	final static boolean next_moon_default = false;
	final static boolean force_dark_mode_default = false;

	final static int fgColour_default = 0xFFFFFFFF;
	final static int bgColour_default = 0x00000000;
	final static int updateInterval_default = -1;
	final static int mySlider_default = 100;
	final static int DayNightMode_default = 2;
	final static int widget_theme_mode_dfault = 4;

	final static long LastDownloadTime_default = 0;
	final static long RSSCache_period_default = 7190;

	final static String LastDownload_default = null;
	final static String BASE_URL_default = null;
	final static String RADAR_URL_default = null;
	final static String FORECAST_URL_default = null;
	final static String WEBCAM_URL_default = null;
	final static String CUSTOM_URL_default = null;
	final static String custom_url_default = null;
	final static String metierev_default = null;
	final static String forecastData_default = null;
	final static String fctype_default = null;
	final static String radtype_default = null;
	final static String SETTINGS_URL_default = "https://example.com/weewx/inigo-settings.txt";
	final static String CustomURL_default = "https://example.com/mobile.html";

	final static String radarFilename = "radar.gif";
	final static String webcamFilename = "webcam.jpg";
	final static String debug_filename = "weeWXApp_Debug.txt.gz";

	final static boolean RadarOnHomeScreen = true;
	final static boolean ForecastOnHomeScreen = !RadarOnHomeScreen;
	final static boolean RadarOnForecastScreen = !RadarOnHomeScreen;
	final static boolean ForecastOnForecastScreen = RadarOnHomeScreen;

	static String[] updateOptions, themeOptions, widgetThemeOptions;

	static boolean hasBootedFully = false;

	final static String charset = StandardCharsets.UTF_8.toString();

	@Override
	public void onCreate()
	{
		instance = this;

		super.onCreate();

		setStrings();

		KeyValue.save_app_debug_logs = weeWXAppCommon.GetBoolPref("save_app_debug_logs", weeWXApp.save_app_debug_logs);

		if(KeyValue.save_app_debug_logs)
			weeWXAppCommon.LogMessage("Debug logging enabled...");
		else
			weeWXAppCommon.LogMessage("Debug logging disabled...");

		weeWXAppCommon.LogMessage("weeWXApp.java app_version: " + BuildConfig.VERSION_NAME + " starting...");

		if(weeWXAppCommon.fixTypes())
			weeWXAppCommon.LogMessage("weeWXApp.java successfully converted preference object types...");
		else
			weeWXAppCommon.LogMessage("weeWXApp.java didn't need to convert preference object types...");

		colours = new Colours();

		KeyValue.loadYahooRGB2SVGTable();

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
			WebViewPreloader.getInstance().init(6);

		applyTheme(false);

		weeWXAppCommon.LogMessage("weeWXApp.java UpdateCheck.cancelAlarm();");
		UpdateCheck.cancelAlarm();

		weeWXAppCommon.LogMessage("weeWXApp.java UpdateCheck.setNextAlarm();");
		UpdateCheck.setNextAlarm();

		weeWXAppCommon.LogMessage("weeWXApp.java UpdateCheck.runInTheBackground(false, true);");
		UpdateCheck.runInTheBackground(false, true);

		if(KeyValue.rssCheck == 0 && KeyValue.forecastData == null)
		{
			KeyValue.fctype = weeWXAppCommon.GetStringPref("fctype", null);
			KeyValue.forecastData = weeWXAppCommon.GetStringPref("forecastData", null);
			KeyValue.rssCheck = weeWXAppCommon.GetLongPref("rssCheck", 0);
		}

		if(KeyValue.LastDownloadTime == 0 && KeyValue.LastDownload == null)
		{
			KeyValue.LastDownload = weeWXAppCommon.GetStringPref("LastDownload", null);
			KeyValue.LastDownloadTime = weeWXAppCommon.GetLongPref("LastDownloadTime", 0);
		}
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);

		int newNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
		boolean isModeChanged = (lastNightMode != -1 && lastNightMode != newNightMode);

		lastNightMode = newNightMode;

		if(isModeChanged)
		{
			weeWXAppCommon.LogMessage("newConfig.uiMode changed, update the theme and mode!");
			applyTheme(true);
		}
	}

	static void setStrings()
	{
		updateOptions = new String[]
		{
			getAndroidString(R.string.manual_update),
			getAndroidString(R.string.every_5_minutes),
			getAndroidString(R.string.every_10_minutes),
			getAndroidString(R.string.every_15_minutes),
			getAndroidString(R.string.every_30_minutes),
			getAndroidString(R.string.every_hour),
			getAndroidString(R.string.update_while_running),
		};

		themeOptions = new String[]
		{
			getAndroidString(R.string.light_theme),
			getAndroidString(R.string.dark_theme),
			getAndroidString(R.string.system_default)
		};

		widgetThemeOptions = new String[]
		{
			getAndroidString(R.string.system_default),
			getAndroidString(R.string.match_app),
			getAndroidString(R.string.light_theme),
			getAndroidString(R.string.dark_theme),
			getAndroidString(R.string.custom_setting)
		};
	}

	static Drawable loadSVGFromAssets(String filename)
	{
		String svgStr = loadFileFromAssets(filename);

		if(svgStr == null || svgStr.isBlank())
			return null;

		weeWXAppCommon.LogMessage("svgStr: " + svgStr);

		try
		{
			SVG svg = SVG.getFromString(svgStr);
			svg.setDocumentPreserveAspectRatio(PreserveAspectRatio.FULLSCREEN);
			return new PictureDrawable(svg.renderToPicture());
		} catch(Exception e) {
			weeWXAppCommon.doStackOutput(e);
		}

		return null;
	}

	static Bitmap loadBitmapFromAssets(String filename)
	{
		byte[] imgBytes = loadBinaryFromAssets(filename);

		if(imgBytes == null)
			return null;

		return BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
	}

	static byte[] loadBinaryFromAssets(String filename)
	{
		try
		{
			InputStream is = instance.getAssets().open(filename);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			byte[] buffer = new byte[1024];
			int len;

			while((len = is.read(buffer)) != -1)
				baos.write(buffer, 0, len);

			is.close();

			return baos.toByteArray();
		} catch (Exception e) {
			weeWXAppCommon.doStackOutput(e);
			return null;
		}
	}

	static String loadFileFromAssets(String filename)
	{
		String path = "", nameonly;

		try
		{
			AssetManager assetManager = instance.getAssets();

			if(filename.lastIndexOf('/') >= 0)
			{
				path = filename.substring(0, filename.lastIndexOf('/'));
				nameonly = filename.substring(filename.lastIndexOf('/') + 1);
			} else {
				nameonly = filename;
			}


			String[] files = assetManager.list(path);
			if(files == null || files.length == 0)
				return null;

			boolean exists = false;
			for(String f : files)
			{
				if(f.equals(nameonly))
				{
					exists = true;
					break;
				}
			}

			if(!exists)
			{
				//weeWXAppCommon.LogMessage("File not found: " + filename, true);
				return null;
			}

			InputStream is = instance.getAssets().open(filename);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			byte[] buffer = new byte[1024];
			int len;

			while((len = is.read(buffer)) != -1)
				baos.write(buffer, 0, len);

			is.close();

			return baos.toString(charset);
		} catch (Exception e) {
			weeWXAppCommon.doStackOutput(e);
		}

		return null;
	}

	static void applyTheme(boolean forced)
	{
		//if(DynamicColors.isDynamicColorAvailable())
		//	DynamicColors.applyToActivitiesIfAvailable(this);

		instance.colours.initOrReinit();

		getDayNightMode();

		if(AppCompatDelegate.getDefaultNightMode() != KeyValue.mode)
			AppCompatDelegate.setDefaultNightMode(KeyValue.mode);

		current_html_headers = html_header +
		                       "<style>\n" +
		                       loadFileFromAssets("main.css") +
		                       "\n</style>\n";

		current_dialog_html = dialog_html_header +
		                      "<style>\n" +
		                      loadFileFromAssets("secondary.css") +
		                      "\n</style>\n" +
		                      dialog_html_header_rest;

		replaceHex6String("FG_HEX", KeyValue.fgColour);
		replaceHex6String("BG_HEX", KeyValue.bgColour);
		if(KeyValue.theme == R.style.AppTheme_weeWXApp_Dark_Common)
		{
			replaceHex6String("ACCENT_HEX", instance.colours.DarkBlueAccent);
			replaceHex6String("GRAY_HEX", instance.colours.DarkGray);
		} else {
			replaceHex6String("ACCENT_HEX", instance.colours.LightBlueAccent);
			replaceHex6String("GRAY_HEX", instance.colours.LightGray);
		}

		String lang = Locale.getDefault().getLanguage();
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
		{
			LocaleList locales = getLocales();
			if(!locales.isEmpty())
				lang = locales.get(0).toLanguageTag();
		}

		if(lang.contains("-"))
			lang = lang.split("-", 2)[0].strip();

		if(!lang.isEmpty())
			lang = lang.strip();

		if(lang.isEmpty())
			lang = "en";

		current_html_headers = replaceHTMLString(current_html_headers, "CURRENT_LANG", lang);
		current_dialog_html = replaceHTMLString(current_dialog_html, "CURRENT_LANG", lang);
		weeWXAppCommon.LogMessage("Current app language: " + lang);

		instance.setTheme(KeyValue.theme);

		weeWXAppCommon.LogMessage("DayNightMode == " + AppCompatDelegate.getDefaultNightMode());

		if(forced)
		{
			weeWXAppCommon.SendIntent(weeWXAppCommon.STOP_FORECAST_INTENT);
			weeWXAppCommon.SendIntent(weeWXAppCommon.STOP_RADAR_INTENT);
			weeWXAppCommon.SendIntent(weeWXAppCommon.STOP_WEATHER_INTENT);
			weeWXAppCommon.SendIntent(weeWXAppCommon.STOP_WEBCAM_INTENT);
		}

		weeWXAppCommon.LogMessage("Theme should have updated!");
	}

	static void replaceHex6String(String html_tag, int colour)
	{
		String hex = java.lang.String.format(CPEditText.getFixedChar() + "%06X", 0xFFFFFF & colour);
		current_html_headers = current_html_headers.replaceAll(html_tag, hex);
	}

	static void replaceHex8String(String html_tag, int colour)
	{
		String hex = CPEditText.getFixedChar() + java.lang.String.format("%08X", colour);
		current_html_headers = current_html_headers.replaceAll(html_tag, hex);
	}

	static String replaceHTMLString(String base_html, String html_tag, String replacement)
	{
		return base_html.replaceAll(html_tag, replacement);
	}

	static weeWXApp getInstance()
	{
		return instance;
	}

	static int getHeight()
	{
		return instance.getResources().getConfiguration().screenHeightDp;
	}

	static int getWidth()
	{
		return instance.getResources().getConfiguration().screenWidthDp;
	}

	static boolean isTablet()
	{
		return instance.getResources().getConfiguration().smallestScreenWidthDp >= 600;
	}

	static LocaleList getLocales()
	{
		return instance.getResources().getConfiguration().getLocales();
	}

	static InputStream openRawResource(int resId)
	{
		return instance.getResources().openRawResource(resId);
	}

	static String getAndroidString(int resId)
	{
		return instance.getString(resId);
	}

	static int smallestScreenWidth()
	{
		return instance.getResources().getConfiguration().smallestScreenWidthDp;
	}

	static int getUImode()
	{
		return instance.getResources().getConfiguration().uiMode;
	}

	static float getDensity()
	{
		return instance.getResources().getDisplayMetrics().density;
	}

	static int getColour(int resId)
	{
		return instance.getColor(resId);
	}

	static Drawable getAndroidDrawable(int resId)
	{
		return AppCompatResources.getDrawable(instance, resId);
	}

	private static StaticLayout newStaticLayout(String text, TextPaint paint, int width)
	{
		return StaticLayout.Builder
				.obtain(text, 0, text.length(), paint, width)
				.setAlignment(Layout.Alignment.ALIGN_CENTER)
				.setLineSpacing(0f, 1f)
				.setIncludePad(true)
				.build();
	}

	static Bitmap textToBitmap(int resId)
	{
		return textToBitmap(getAndroidString(resId));
	}

	static Bitmap textToBitmap(String text)
	{
		// 1️⃣ Load the reference drawable
		Drawable drawable = getAndroidDrawable(R.drawable.nowebcam);
		if(drawable == null)
			return null;

		int width = drawable.getIntrinsicWidth();
		int height = drawable.getIntrinsicHeight();

		// 2️⃣ Create a new bitmap with same size
		Bitmap bitmap;

		if(text != null && !text.isBlank())
		{
			float textSize = height * 0.8f;

			bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawColor(KeyValue.bgColour);

			// 3️⃣ Prepare the paint
			TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
			paint.setColor(KeyValue.fgColour);
			paint.setTextAlign(Paint.Align.LEFT);
			paint.setTextSize(textSize);

			StaticLayout staticLayout = newStaticLayout(text, paint, width);

			while(textSize > 5f)
			{
				paint.setTextSize(textSize);

				staticLayout = newStaticLayout(text, paint, width);

				if(staticLayout.getHeight() <= height)
					break;

				textSize -= 1f;
			}

			float textY = (height - staticLayout.getHeight()) / 2f;

			// 5️⃣ Draw the text centered
			canvas.save();
			canvas.translate(0, textY);
			staticLayout.draw(canvas);
			canvas.restore();
		} else {
			bitmap = ((BitmapDrawable)drawable).getBitmap();
		}

		return bitmap;
	}

	static Colours getColours()
	{
		return instance.colours;
	}

	static void getDayNightMode()
	{
		Context context = instance;

		int current_mode, current_theme, bgColour, fgColour;
		boolean isWidgetSet = false;
		boolean prefSet = weeWXAppCommon.isPrefSet("DayNightMode");
		int nightDaySetting = getAppDayNightSetting();
		int nightModeFlags = getUImode() & Configuration.UI_MODE_NIGHT_MASK;
		KeyValue.widget_theme_mode = weeWXAppCommon.GetIntPref(weeWXAppCommon.WIDGET_THEME_MODE, 0);

		current_mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
		current_theme = R.style.AppTheme_weeWXApp_Light_Common;
		bgColour = ContextCompat.getColor(context, R.color.White);
		fgColour = ContextCompat.getColor(context, R.color.Black);

		if(prefSet)
		{
			if(nightDaySetting == 0)
			{
				weeWXAppCommon.LogMessage("Night mode off...");
				current_mode = AppCompatDelegate.MODE_NIGHT_NO;
				current_theme = R.style.AppTheme_weeWXApp_Light_Common;
				bgColour = ContextCompat.getColor(context, R.color.White);
				fgColour = ContextCompat.getColor(context, R.color.Black);
			} else if(nightDaySetting == 1) {
				weeWXAppCommon.LogMessage("Night mode on...");
				current_mode = AppCompatDelegate.MODE_NIGHT_NO;
				current_theme = R.style.AppTheme_weeWXApp_Dark_Common;
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
				weeWXAppCommon.LogMessage("Night mode off...");
				bgColour = ContextCompat.getColor(context, R.color.White);
				fgColour = ContextCompat.getColor(context, R.color.Black);
				current_theme = R.style.AppTheme_weeWXApp_Light_Common;
			} else {
				weeWXAppCommon.LogMessage("Night mode on...");
				bgColour = ContextCompat.getColor(context, R.color.Black);
				fgColour = ContextCompat.getColor(context, R.color.White);
				current_theme = R.style.AppTheme_weeWXApp_Dark_Common;
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
			KeyValue.widgetBG = weeWXAppCommon.GetIntPref("bgColour", bgColour_default);
			KeyValue.widgetFG = weeWXAppCommon.GetIntPref("fgColour", fgColour_default);
		}

		KeyValue.theme = current_theme;
		KeyValue.mode = current_mode;
		KeyValue.bgColour = bgColour;
		KeyValue.fgColour = fgColour;
	}

	static int getAppDayNightSetting()
	{
		return weeWXAppCommon.GetIntPref("DayNightMode", DayNightMode_default);
	}
}