package com.odiousapps.weewxweather;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
import android.util.Log;

import com.caverock.androidsvg.PreserveAspectRatio;
import com.caverock.androidsvg.SVG;
import com.github.evilbunny2008.colourpicker.CPEditText;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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

	private static final String inline_arrow_light = """
			<!-- Floating scroll-to-top button -->
			<div id="scrollToTop">
				<svg viewBox="0 0 24 24">
					<path fill="#fff"
					      d="M12 4l-7 8h4v8h6v-8h4z"/>
				</svg>
			</div>
	""";

	private static final String inline_arrow_dark = """
			<!-- Floating scroll-to-top button -->
			<div id="scrollToTop">
				<svg viewBox="0 0 24 24">
					<path fill="#000"
					      d="M12 4l-7 8h4v8h6v-8h4z"/>
				</svg>
			</div>
	""";

	static String inline_arrow = inline_arrow_light;

	static final String html_footer = """
		</body>
	</html>
	""";

	private static final String about_blurb = """
		Big thanks to the <a href='https://weewx.com'>weeWX project</a>, as this app
		wouldn't be possible otherwise.<br><br>
		Weather Icons from <a href='https://www.flaticon.com/'>FlatIcon</a> and
		is licensed under <a href='https://creativecommons.org/licenses/by/3.0/'>CC 3.0 BY</a> and
		<a href='https://github.com/erikflowers/weather-icons'>Weather Font</a> by Erik Flowers
		<br><br>
		Current WebView Library Version: WEBVIEWVER
		<br/><br/>
		weeWX Weather App vAPPVERSION
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

	static String current_about_blurb = about_blurb;

	final static String emptyField = "<div style='span:3;'>\u00A0</div>\n";
	final static String currentSpacer = "\t\t\t<div class='currentSpacer'>\u00A0</div>\n";

	private static weeWXApp instance = null;
	private static Colours colours;
	private static int lastNightMode = -1;

	final static int minimum_inigo_version = 4000;

	final static boolean radarforecast_default = false;
	final static boolean disableSwipeOnRadar_default = false;
	final static boolean onlyWIFI_default = false;
	final static boolean metric_default = true;
	final static boolean showIndoor_default = true;
	final static boolean use_exact_alarm_default = false;
	final static boolean next_moon_default = false;
	final static boolean force_dark_mode_default = false;
	final static boolean save_app_debug_logs_default = false;

	final static int bgColour_default = 0xFFFFFFFF;
	final static int fgColour_default = 0xFF000000;

	final static int widgetBG_default = 0x00000000;
	final static int widgetFG_default = 0xFFFFFFFF;
	final static int widgetBG_default2 = 0x00000000;
	final static int widgetFG_default2 = 0xFF000000;

	final static int updateInterval_default = 1;
	final static int mySlider_default = 100;
	final static int DayNightMode_default = 2;
	final static int widget_theme_mode_default = 4;
	final static int theme_default = R.style.AppTheme_weeWXApp_Light_Common;
	final static int mode_default = AppCompatDelegate.MODE_NIGHT_NO;

	final static long LastDownloadTime_default = 0;
	final static long RSSCache_period_default = 7_140L;

	final static String LastDownload_default = "";
	final static String BASE_URL_default = "";
	final static String RADAR_URL_default = "";
	final static String FORECAST_URL_default = "";
	final static String WEBCAM_URL_default = "";
	final static String CUSTOM_URL_default = "";
	final static String custom_url_default = "";
	final static String forecastData_default = "";
	final static String fctype_default = "";
	final static String radtype_default = "image";
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

	private static Integer currentTheme = null;

	final static String charset = StandardCharsets.UTF_8.toString();

	record Setting(String Key, Object Val) {}

	@Override
	public void onCreate()
	{
		instance = this;

		try
		{
			// Preload the BuildConfig, weeWXAppCommon and KeyValue classes
			Class.forName("com.odiousapps.weewxweather.BuildConfig");
			Class.forName("com.odiousapps.weewxweather.weeWXAppCommon");
			Class.forName("com.odiousapps.weewxweather.KeyValue");
		} catch(ClassNotFoundException ignored) {}

		super.onCreate();

		int major = 0;
		try
		{
			PackageManager pm = weeWXApp.getInstance().getPackageManager();
			String[] possibleWebViews = {
					"com.google.android.webview",
					"com.android.webview",
					"com.android.chrome"
			};

			for (String pkg : possibleWebViews) {
				try {
					Log.i(weeWXAppCommon.LOGTAG, "Checking for " + pkg);
					PackageInfo info = pm.getPackageInfo(pkg, 0);
					KeyValue.currWebViewVer = info.versionName;
					Log.i(weeWXAppCommon.LOGTAG, pkg + " version: " + KeyValue.currWebViewVer);
					break;
				} catch (PackageManager.NameNotFoundException ignored) {}
			}

			if(KeyValue.currWebViewVer != null && !KeyValue.currWebViewVer.isBlank())
			{
				String[] parts = KeyValue.currWebViewVer.split("\\.");
				KeyValue.webview_major_version = (int)Float.parseFloat(parts[0]);
			}
		} catch(Exception e) {
			Log.e(weeWXAppCommon.LOGTAG, "Error! e: " + e.getMessage(), e);
		}

		// Let's assume no value is actually ok and the package name has changed or something similar...
		if(KeyValue.webview_major_version <= 0)
			KeyValue.webview_major_version = 83;

		if(KeyValue.webview_major_version < 83)
			return;

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

		if((boolean)KeyValue.readVar("save_app_debug_logs", save_app_debug_logs_default))
			weeWXAppCommon.LogMessage("Debug logging enabled...", true, KeyValue.i);
		else
			weeWXAppCommon.LogMessage("Debug logging disabled...", true, KeyValue.i);

		weeWXAppCommon.LogMessage("weeWXApp.java app_version: " + com.odiousapps.weewxweather.BuildConfig.VERSION_NAME + " starting...", KeyValue.i);

		if(weeWXAppCommon.fixTypes())
			weeWXAppCommon.LogMessage("weeWXApp.java successfully converted preference object types...", KeyValue.d);
		else
			weeWXAppCommon.LogMessage("weeWXApp.java didn't need to convert preference object types...", KeyValue.d);

		colours = new Colours();

		KeyValue.loadYahooRGB2SVGTable();
		KeyValue.fillCounties();

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
			WebViewPreloader.getInstance().init(6);

		applyTheme(false);

		weeWXAppCommon.LogMessage("weeWXApp.java UpdateCheck.cancelAlarm()");
		UpdateCheck.cancelAlarm();

		weeWXAppCommon.LogMessage("weeWXApp.java UpdateCheck.setNextAlarm()");
		UpdateCheck.setNextAlarm();

		weeWXAppCommon.LogMessage("weeWXApp.java UpdateCheck.runInTheBackground(false, true)");
		UpdateCheck.runInTheBackground(false, true);

		updateAboutBlurb();

		KeyValue.countyName = (String)KeyValue.readVar("CountyName", null);
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
			weeWXAppCommon.LogMessage("newConfig.uiMode changed, update the theme and mode!", KeyValue.d);
			applyTheme(true);
		}
	}

	static void updateAboutBlurb()
	{
		if(KeyValue.currWebViewVer != null)
		{
			current_about_blurb = about_blurb.replaceAll("WEBVIEWVER", KeyValue.currWebViewVer)
					.replaceAll("APPVERSION", com.odiousapps.weewxweather.BuildConfig.VERSION_NAME);
		}
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
				weeWXAppCommon.LogMessage("File not found: " + filename, KeyValue.d);
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
		int theme = (int)KeyValue.readVar("theme", theme_default);
		int mode = mode_default;

		//if(DynamicColors.isDynamicColorAvailable())
		//	DynamicColors.applyToActivitiesIfAvailable(this);

		colours.initOrReinit();

		List<Setting> settings = getDayNightMode();

		for(Setting s : settings)
		{
			if(s.Key.equals("theme"))
				theme = (int)s.Val();

			if(s.Key.equals("mode"))
				mode = (int)s.Val();
		}

		if(theme == R.style.AppTheme_weeWXApp_Light_Common)
			Log.i(weeWXAppCommon.LOGTAG, "weeWXApp.onCreate() theme: R.style.AppTheme_weeWXApp_Light_Common");
		else if(theme == R.style.AppTheme_weeWXApp_Dark_Common)
			Log.i(weeWXAppCommon.LOGTAG, "weeWXApp.onCreate() theme: R.style.AppTheme_weeWXApp_Dark_Common");
		else if(theme == R.style.AppTheme_weeWXApp_Common)
			Log.i(weeWXAppCommon.LOGTAG, "weeWXApp.onCreate() theme: R.style.AppTheme_weeWXApp_Common");
		else
			Log.i(weeWXAppCommon.LOGTAG, "weeWXApp.onCreate() theme: " + theme);

		weeWXAppCommon.LogMessage("Setting mode to: " + mode, true, KeyValue.w);

		if(AppCompatDelegate.getDefaultNightMode() != mode)
			AppCompatDelegate.setDefaultNightMode(mode);

		if(currentTheme == null || currentTheme != theme)
		{
			currentTheme = theme;
			instance.setTheme(theme);
		}

		String main_css = loadFileFromAssets("main.css");
		if(main_css != null && !main_css.isBlank())
		{
			if(theme == R.style.AppTheme_weeWXApp_Light_Common)
				main_css = main_css.replaceAll("ARROW_COLOUR", "#fff")
						.replaceAll("ARROW_BG_COLOUR", "#333");
			else
				main_css = main_css.replaceAll("ARROW_COLOUR", "#000000")
						.replaceAll("ARROW_BG_COLOUR", "#CCCCCC");
		}

		current_html_headers = html_header +
		                       "<style>\n" +
		                       main_css+
		                       "\n</style>\n";

		current_dialog_html = dialog_html_header +
		                      "<style>\n" +
		                      loadFileFromAssets("secondary.css") +
		                      "\n</style>\n" +
		                      dialog_html_header_rest;

		if(theme == R.style.AppTheme_weeWXApp_Light_Common)
			inline_arrow = inline_arrow_light;
		else
			inline_arrow = inline_arrow_dark;

		replaceHex6String("BG_HEX", colours.bgColour);
		replaceHex6String("FG_HEX", colours.fgColour);
		if((int)KeyValue.readVar("theme", theme_default) == R.style.AppTheme_weeWXApp_Dark_Common)
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

		weeWXAppCommon.LogMessage("Current app language: " + lang, KeyValue.d);

		if(forced)
		{
			weeWXAppCommon.SendIntent(weeWXAppCommon.STOP_FORECAST_INTENT);
			weeWXAppCommon.SendIntent(weeWXAppCommon.STOP_RADAR_INTENT);
			weeWXAppCommon.SendIntent(weeWXAppCommon.STOP_WEATHER_INTENT);
			weeWXAppCommon.SendIntent(weeWXAppCommon.STOP_WEBCAM_INTENT);
		}

		weeWXAppCommon.LogMessage("Theme should have updated!");

		weeWXAppCommon.LogMessage("DayNightMode == " + AppCompatDelegate.getDefaultNightMode(), true, KeyValue.w);

		weeWXAppCommon.SendIntent(weeWXAppCommon.REFRESH_DARKMODE_INTENT);
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
			canvas.drawColor(weeWXApp.getColours().bgColour);

			// 3️⃣ Prepare the paint
			TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
			paint.setColor(weeWXApp.getColours().fgColour);
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
		return colours;
	}

	static List<Setting> getDayNightMode()
	{
		List<Setting> settings = new ArrayList<>();

		int Black = ContextCompat.getColor(instance, R.color.Black);
		int White = ContextCompat.getColor(instance, R.color.White);

		boolean prefSet = weeWXAppCommon.isPrefSet("DayNightMode");

		int current_theme = (int)KeyValue.readVar("theme", theme_default);
		int current_mode = (int)KeyValue.readVar("mode", mode_default);
		int widget_theme_mode = (int)KeyValue.readVar("widget_theme_mode", widget_theme_mode_default);
		int current_bgColour = (int)KeyValue.readVar("bgColour", bgColour_default);
		int current_fgColour = (int)KeyValue.readVar("fgColour", fgColour_default);
		int current_widgetBG = (int)KeyValue.readVar("widgetBG", widgetBG_default);
		int current_widgetFG = (int)KeyValue.readVar("widgetFG", widgetFG_default);

		int nightDaySetting = (int)KeyValue.readVar("DayNightMode", DayNightMode_default);
		int nightModeFlags = getUImode() & Configuration.UI_MODE_NIGHT_MASK;

		int theme = current_theme;
		int mode = current_mode;
		int bgColour = current_bgColour;
		int fgColour = current_fgColour;
		int widgetBG = current_widgetBG;
		int widgetFG = current_widgetFG;

		if(prefSet && nightDaySetting == 0)
		{
			weeWXAppCommon.LogMessage("Set to Light...");
			theme = R.style.AppTheme_weeWXApp_Light_Common;
			mode = AppCompatDelegate.MODE_NIGHT_NO;
			bgColour = White;
			fgColour = Black;
		}

		if(prefSet && nightDaySetting == 1)
		{
			weeWXAppCommon.LogMessage("Set to Dark...");
			theme = R.style.AppTheme_weeWXApp_Dark_Common;
			mode = AppCompatDelegate.MODE_NIGHT_NO;
			bgColour = Black;
			fgColour = White;
		}

		if(!prefSet || nightDaySetting == 2)
		{
			if(nightDaySetting == 2 && nightModeFlags == Configuration.UI_MODE_NIGHT_NO)
			{
				weeWXAppCommon.LogMessage("Pref not set or set to follow system and dark mode off...");
				theme = R.style.AppTheme_weeWXApp_Light_Common;
				mode = AppCompatDelegate.MODE_NIGHT_NO;
				bgColour = White;
				fgColour = Black;
			} else {
				weeWXAppCommon.LogMessage("Pref not set or set to follow system and dark mode on...");
				theme = R.style.AppTheme_weeWXApp_Dark_Common;
				mode = AppCompatDelegate.MODE_NIGHT_NO;
				bgColour = Black;
				fgColour = White;
			}
		}

		if(!prefSet || widget_theme_mode == 0)
		{
			if(nightModeFlags == Configuration.UI_MODE_NIGHT_NO)
			{
				weeWXAppCommon.LogMessage("Pref set to follow system and dark mode isn't on...");
				widgetBG = widgetBG_default;
				widgetFG = widgetFG_default;
			} else {
				weeWXAppCommon.LogMessage("Pref set to follow system and dark mode is on...");
				widgetBG = widgetBG_default2;
				widgetFG = widgetFG_default2;
			}
		}

		if(prefSet && widget_theme_mode == 1)
		{
			weeWXAppCommon.LogMessage("Pref set to follow the app...");
			widgetBG = bgColour;
			widgetFG = fgColour;
		}

		if(prefSet && widget_theme_mode == 2)
		{
			weeWXAppCommon.LogMessage("Pref set to Light...");
			widgetBG = widgetBG_default2;
			widgetFG = widgetFG_default2;
		}

		if(prefSet && widget_theme_mode == 3)
		{
			weeWXAppCommon.LogMessage("Pref set to Dark...");
			widgetBG = widgetBG_default;
			widgetFG = widgetFG_default;
		}

		if(prefSet && widget_theme_mode == 4)
		{
			weeWXAppCommon.LogMessage("Pref set to Custom...");
		}

		KeyValue.theme = theme;
		KeyValue.mode = mode;
		colours.bgColour = bgColour;
		colours.fgColour = fgColour;
		colours.widgetBG = widgetBG;
		colours.widgetFG = widgetFG;

		if(prefSet)
		{
			if(current_theme != theme)
				KeyValue.putVar("theme", theme);

			if(current_mode != mode)
				KeyValue.putVar("mode", mode);

			if(current_bgColour != bgColour)
				KeyValue.putVar("bgColour", bgColour);

			if(current_fgColour != fgColour)
				KeyValue.putVar("fgColour", fgColour);

			if(current_widgetBG != widgetBG)
				KeyValue.putVar("widgetBG", widgetBG);

			if(current_widgetFG != widgetFG)
				KeyValue.putVar("widgetFG", widgetFG);
		}

		settings.add(new Setting("theme", theme));
		settings.add(new Setting("mode", mode));
		settings.add(new Setting("widget_theme_mode", widget_theme_mode));

		settings.add(new Setting("bgColour", bgColour));
		settings.add(new Setting("fgColour", fgColour));
		settings.add(new Setting("widgetBG", widgetBG));
		settings.add(new Setting("widgetFG", widgetFG));

		Log.w(weeWXAppCommon.LOGTAG, "weeWXApp.getDayNightMode() bgColour: #" + Integer.toHexString(bgColour));
		Log.w(weeWXAppCommon.LOGTAG, "weeWXApp.getDayNightMode() fgColour: #" + Integer.toHexString(fgColour));
		Log.w(weeWXAppCommon.LOGTAG, "weeWXApp.getDayNightMode() widgetBG: #" + Integer.toHexString(widgetBG));
		Log.w(weeWXAppCommon.LOGTAG, "weeWXApp.getDayNightMode() widgetFG: #" + Integer.toHexString(widgetFG));

		return settings;
	}
}