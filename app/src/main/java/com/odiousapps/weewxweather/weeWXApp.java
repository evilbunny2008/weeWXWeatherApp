package com.odiousapps.weewxweather;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.LocaleList;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.github.evilbunny2008.colourpicker.CPEditText;

import java.io.InputStream;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

@SuppressWarnings({"unused", "SameParameterValue"})
public class weeWXApp extends Application
{
	private static final String html_header =
			"""
			<!DOCTYPE html>
			<html lang='CURRENT_LANG'>
				<head>
					<meta charset='utf-8'>
					<meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no'>
					<meta name='color-scheme' content='light dark'>
					<link rel='stylesheet' href='file:///android_asset/weathericons.css'>
					<link rel='stylesheet' href='file:///android_asset/weathericons_wind.css'>
					<link rel='stylesheet' type='text/css' href='file:///android_asset/flaticon.css'>
					<style>
						:root
						{
							padding: 0;
							margin: 0 0 15px 0;
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
						a
						{
							color: ACCENT_HEX;
						}
						hr
						{
							border: 1px solid GRAY_HEX;
							width: 90%;
							margin: 15px auto;
						}
						html, body
						{
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
						*
						{
							box-sizing: border-box;
						}
						.header
						{
							text-align: center;
							font-size: var(--font-large);
						}
						.today
						{
							display: flex;
							flex-direction: column;
							margin: 0 10px 10px 10px;
						}
						.topRow
						{
							display: flex;
							flex-direction: row;
							align-items: center;
							justify-content: space-between;
						}
						.iconBig img, i
						{
							width: 80px;
							height: auto;
							line-height: 1;
							font-size: 5rem;
							align-self: start;
							margin-bottom: 4px;
						}
						.wordToday
						{
							font-size: var(--font-larger);
							font-weight: bold;
							text-align: center;
						}
						.bigTemp
						{
							display: flex;
							flex-direction: column;
							padding-left: 5px;
							align-items: flex-end;
							font-size: var(--font-big);
							font-weight: bold;
							text-align: center;
							justify-self: center;
						}
						.minTemp
						{
							font-size: var(--font-large);
						}
						.maxTemp, .forecastMax
						{
							color: #d32f2f;
						}
						.minTemp, .forecastMin
						{
							color: #1976d2;
						}
						.forecastMax, .forecastMin
						{
							font-size: var(--font-small);
						}
						.bigForecastText
						{
							text-align: justify;
							text-justify: inter-word;
							text-align-last: left;
							font-size: var(--font-large);
							max-width: 1000px;
						}
						.forecast
						{
							display: flex;
							flex-direction: row;
							flex-wrap: wrap;
							max-width: 1200px;
							margin: 0 5px 10px 5px;
							justify-content: center;
							column-gap: 100px;
						}
						.day
						{
							display: flex;
							flex: 1 1 500px;
							flex-direction: column;
							justify-content: space-evenly;
							margin: 5px 0 10px 0;
							max-width: 500px;
						}
						.dayTopRow
						{
							display: flex;
							flex-direction: row;
							width: 100%;
							align-items: center;
							justify-content: space-between;
							font-size: var(--font-average);
							font-weight: bold;
						}
						.iconSmall img, i
						{
							align-self: start;
							font-size: var(--font-large);
							width: 40px;
							height: auto;
							margin-right: 8px;
						}
						.dayTitle
						{
							font-weight: bold;
							font-size: var(--font-average);
							margin: 0 0 2px 0;
							text-align: center;
							align-items: center;
						}
						.desc
						{
							font-size: var(--font-smaller);
							text-align: justify;
							text-justify: inter-word;
						}
						.radarImage
						{
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
						.mainTemp
						{
							font-size: var(--font-huge);
							text-align: left;
							margin: 0 0 0 10px;
						}
						.apparentTemp
						{
							font-size: var(--font-larger);
							text-align: right;
							margin: 0 10px 0 0;
						}
						.icon
						{
							font-size: var(--font-average);
							width: 20px;
							text-align: center;
							margin: 0 4px 0 4px;
						}
						.todayCurrent
						{
							display: flex;
							flex-direction: column;
							width: 100%;
							max-width: 500px;
							padding: 0;
							margin: 0;
						}
						.topRowCurrent
						{
							display: flex;
							flex-direction: row;
							align-items: center;
							justify-content: space-between;
						}
						.dataTableCurrent
						{
							display: flex;
							flex-direction: column;
							padding: 0 5px 0 5px;
							margin: 0;
							width: 100%;
						}
						.dataRowCurrent
						{
							width: 100%;
							display: flex;
							justify-content: space-between;
							align-items: center;
							font-size: var(--font-small);
						}
						.dataCellCurrent
						{
							width: 100%;
							display: flex;
							align-items: center;
							white-space: nowrap;
						}
						.dataCellCurrent.right
						{
							justify-content: flex-end;
						}
						.statsLayout
						{
							display: flex;
							flex-direction: row;
							flex-wrap: wrap;
							max-width: 1200px;
							column-gap: 2px;
							justify-content: space-evenly;
							padding: 0 5px 0 5px;
						}
						.statsHeader
						{
							font-weight: bold;
							font-size: var(--font-large);
							text-align: center;
						}
						.statsSection
						{
							display: flex;
							flex-direction: column;
							width: 100%;
							max-width: 500px;
						}
						.statsDataRow
						{
							display: grid;
							grid-template-columns: minmax(100px, 2fr)
							minmax(55px, 1fr)
							minmax(3px, 5px)
							minmax(55px, 1fr)
							minmax(100px, 2fr);
						}
						.statsDataCell
						{
							align-self: center;
							white-space: nowrap;
							overflow: hidden;
							text-overflow: ellipsis;
						}
						.statsDataCell.left, .statsDataCell.midleft,
						.statsDataCell.Wind, .statsDataCell.Wind2
						{
							text-align: left;
							justify-self: start;
						}
						.statsDataCell.right, .statsDataCell.midright,
						.statsDataCell.Rain, .statsDataCell.Rain2
						{
							text-align: right;
							justify-self: end;
						}
						.statsDataCell.Wind, .statsDataCell.Rain
						{
							grid-column: span 2;
						}
						.statsDataCell.Wind2
						{
							grid-column: span 4;
						}
						.statsDataCell.Rain2
						{
							grid-column: span 1;
						}
						.statsSpacer
						{
							width: 1px;
							height: 100%;
							margin-left: 2px;
							margin-right: 2px;
						}
						.currentSpacer
						{
							width: 7px;
							height: 100%;
							margin-left: 2px;
							margin-right: 2px;
						}
						.currentSpacer.right
						{
							width: 3px;
						}
						@media (min-width: 500px)
						{
							.statsDataRow
							{
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

	final static String about_blurb =
			"""
			Big thanks to the <a href='https://weewx.com'>weeWX project</a>, as this app
			wouldn't be possible otherwise.<br><br>
			Weather Icons from <a href='https://www.flaticon.com/'>FlatIcon</a> and
			is licensed under <a href='https://creativecommons.org/licenses/by/3.0/'>CC 3.0 BY</a> and
			<a href='https://github.com/erikflowers/weather-icons'>Weather Font</a> by Erik Flowers
			<br><br>
			weeWX Weather App v""" + BuildConfig.VERSION_NAME + """
									is by <a href='https://odiousapps.com'>OdiousApps</a>.
									""";

	final static String debug_html =
			"""
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

	static String current_html_headers;

	final static String emptyField = "<span class='field'>\u00A0</span>";

	private static weeWXApp instance;
	private Colours colours;
	private static int lastNightMode = -1;

	final static int minimum_inigo_version = 4000;

	final static boolean radarforecast_default = false;
	final static boolean disableSwipeOnRadar_default = false;
	final static boolean onlyWIFI_default = false;
	final static boolean useIcons_default = true;
	final static boolean metric_default = true;
	final static boolean showIndoor_default = true;

	final static int fgColour_default = 0xFFFFFFFF;
	final static int bgColour_default = 0x00000000;
	final static int updateInterval_default = 1;
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

	final static boolean RadarOnHomeScreen = true;
	final static boolean ForecastOnHomeScreen = !RadarOnHomeScreen;
	final static boolean RadarOnForecastScreen = !RadarOnHomeScreen;
	final static boolean ForecastOnForecastScreen = RadarOnHomeScreen;

	@Override
	public void onCreate()
	{
		super.onCreate();

		weeWXAppCommon.LogMessage("weeWXApp.java app_version: " + BuildConfig.VERSION_NAME);

		instance = this;
		colours = new Colours(this);
		WebViewPreloader.getInstance().init(this, 6);
		applyTheme();

		weeWXAppCommon.LogMessage("weeWXApp.java UpdateCheck.setAlarm(this);");
		UpdateCheck.cancelAlarm(this);
		UpdateCheck.setAlarm(this);
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
			applyTheme();
		}
	}

	static void applyTheme()
	{
		//if(DynamicColors.isDynamicColorAvailable())
		//	DynamicColors.applyToActivitiesIfAvailable(this);

		instance.colours.initOrReinit();

		getDayNightMode();

		loadOrReload();

		if(AppCompatDelegate.getDefaultNightMode() != KeyValue.mode)
			AppCompatDelegate.setDefaultNightMode(KeyValue.mode);

		instance.setTheme(KeyValue.theme);

		weeWXAppCommon.LogMessage("DayNightMode == " + AppCompatDelegate.getDefaultNightMode());

		weeWXAppCommon.SendForecastRefreshIntent();
		weeWXAppCommon.SendWeatherRefreshIntent();
		weeWXAppCommon.LogMessage("Theme should have updated!");
	}

	private static void loadOrReload()
	{
		current_html_headers = html_header;

		getDayNightMode();

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
			LocaleList locales = weeWXApp.getLocales();
			if(!locales.isEmpty())
				lang = locales.get(0).toLanguageTag();
		}

		lang = lang.split("-")[0].trim();
		replaceHTMLString("CURRENT_LANG", lang);
		weeWXAppCommon.LogMessage("Current app language: " + lang);
	}

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
		Context context = instance.getApplicationContext();

		int current_mode, current_theme, bgColour, fgColour;
		boolean isWidgetSet = false;
		boolean prefSet = weeWXAppCommon.isPrefSet("DayNightMode");
		int nightDaySetting = getAppDayNightSetting();
		int nightModeFlags = weeWXApp.getUImode() & Configuration.UI_MODE_NIGHT_MASK;
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
			KeyValue.widgetBG = weeWXAppCommon.GetIntPref("bgColour", weeWXApp.bgColour_default);
			KeyValue.widgetFG = weeWXAppCommon.GetIntPref("fgColour", weeWXApp.fgColour_default);
		}

		KeyValue.theme = current_theme;
		KeyValue.mode = current_mode;
		KeyValue.bgColour = bgColour;
		KeyValue.fgColour = fgColour;
	}

	static int getAppDayNightSetting()
	{
		return weeWXAppCommon.GetIntPref("DayNightMode", weeWXApp.DayNightMode_default);
	}
}