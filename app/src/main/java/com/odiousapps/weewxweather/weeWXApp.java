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
	private static final String html_header = """
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
						/* Scroll-to-top button */
						#scrollToTop
						{
							position: fixed;
							bottom: 20px;
							right: 20px;
							width: 48px;
							height: 48px;
							background: #333;
							color: #fff;
							border-radius: 50%;
							display: flex;
							align-items: center;
							justify-content: center;
							cursor: pointer;
							opacity: 0;
							pointer-events: none;
							transition: opacity 0.25s ease;
							z-index: 999;
						}
						#scrollToTop.show
						{
							opacity: 0.9;
							pointer-events: auto;
						}
						#scrollToTop svg
						{
							width: 24px;
							height: 24px;
							fill: #fff;
						}
					</style>
			""";

	static final String script_header =
					"""
					<script>
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

	static final String about_blurb = "Big thanks to the <a href='https://weewx.com'>weeWX project</a>, as this app " +
					"wouldn't be possible otherwise.<br><br>\n" +
					"Weather Icons from <a href='https://www.flaticon.com/'>FlatIcon</a> and " +
					"is licensed under <a href='https://creativecommons.org/licenses/by/3.0/'>CC 3.0 BY</a> and " +
					"<a href='https://github.com/erikflowers/weather-icons'>Weather Font</a> by Erik Flowers<br><br>\n" +
					"weeWX Weather App v" + BuildConfig.VERSION_NAME + " is by <a href='https://odiousapps.com'>OdiousApps</a>.\n\n";

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

	private static final String dialog_html = """
           <!doctype html>
           <html lang="REPLACE_WITH_LANG">
           <head>
           <meta charset="utf-8" />
           <meta name="viewport" content="width=device-width,initial-scale=1" />
           <title>Warning UI Mock</title>
           <style>
             :root{
               --bg: BG_HEX;
               --panel: #fff;
               --text: FG_HEX;
               --muted: #586069;
               --accent: #b45309; /* amber-700 */
               --danger: #b91c1c;
               --shadow: 0 12px 30px rgba(16,24,40,0.12);
               --radius: 14px;
               --glass: rgba(255,255,255,0.6);
               --maxw: 520px;
             }
             @media (prefers-color-scheme: dark){
               :root{
                 --bg: #0b0b0c;
                 --panel: linear-gradient(180deg, rgba(255,255,255,0.02), rgba(255,255,255,0.01));
                 --text: #e6edf3;
                 --muted: #9aa4b2;
                 --accent: #f59e0b;
                 --danger: #ef4444;
                 --shadow: 0 16px 40px rgba(2,6,23,0.6);
                 --glass: rgba(255,255,255,0.03);
               }
             }
             /* page background (useful when loaded full-screen in WebView) */
             html,body{
               height:100%;
               margin:0;
               font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial;
               background: linear-gradient(180deg,var(--bg), color-mix(in srgb, var(--bg) 85%, black 15%));
               color:var(--text);
               -webkit-font-smoothing:antialiased;
               -moz-osx-font-smoothing:grayscale;
               display:flex;
               align-items:center;
               justify-content:center;
               padding:24px;
             }
             /* container that looks like a dialog */
             .warn-wrap{
               width:100%;
               max-width:var(--maxw);
               background: var(--panel);
               border-radius:var(--radius);
               box-shadow:var(--shadow);
               padding:20px;
               display:flex;
               gap:16px;
               align-items:flex-start;
               position:relative;
               overflow:hidden;
               border: 1px solid color-mix(in srgb, var(--text) 6%, transparent);
               animation: drop-in .28s cubic-bezier(.16,.84,.34,1);
             }
             /* small visual accent bar (left) */
             .warn-accent{
               width:6px;
               border-radius:6px;
               background: linear-gradient(180deg,var(--accent), color-mix(in srgb, var(--accent) 70%, black 30%));
               flex:0 0 6px;
             }
             /* icon & content layout */
             .warn-icon{
               flex:0 0 48px;
               height:48px;
               display:flex;
               align-items:center;
               justify-content:center;
               border-radius:10px;
               background:var(--glass);
               margin-top:2px;
               box-shadow: inset 0 -2px 6px rgba(0,0,0,0.06);
             }
             .warn-body{
               flex:1 1 auto;
               min-width:0;
             }
             .warn-title{
               font-size:1.05rem;
               font-weight:700;
               margin:0 0 6px 0;
               display:flex;
               gap:8px;
               align-items:center;
             }
             .warn-desc{
               margin:0;
               color:var(--muted);
               font-size:0.95rem;
               line-height:1.45;
               word-break:break-word;
             }
             .warn-actions{
               display:flex;
               gap:10px;
               margin-top:14px;
               flex-wrap:wrap;
             }
             .btn{
               border:0;
               padding:8px 12px;
               border-radius:10px;
               font-weight:600;
               font-size:0.95rem;
               cursor:pointer;
               min-height:38px;
               display:inline-flex;
               align-items:center;
               gap:8px;
             }
             .btn-primary{
               background: linear-gradient(180deg,var(--accent), color-mix(in srgb, var(--accent) 60%, black 20%));
               color:white;
               box-shadow: 0 6px 18px rgba(0,0,0,0.08);
             }
             .btn-ghost{
               background:transparent;
               color:var(--muted);
               border:1px solid color-mix(in srgb, var(--text) 6%, transparent);
             }
             .btn-danger{
               background: linear-gradient(180deg,var(--danger), color-mix(in srgb, var(--danger) 60%, black 20%));
               color:white;
             }
             /* close button top-right (small 'x') */
             .close-btn{
               position:absolute;
               top:10px;
               right:10px;
               width:36px;
               height:36px;
               display:inline-flex;
               align-items:center;
               justify-content:center;
               border-radius:9px;
               border:0;
               background:transparent;
               color:var(--muted);
               cursor:pointer;
               font-weight:700;
               font-size:14px;
             }
             .close-btn:active{ transform:scale(.98) }
             /* subtle "pill" badge for severity */
             .severity{
               font-size:0.8rem;
               padding:4px 8px;
               border-radius:999px;
               background: color-mix(in srgb, var(--accent) 12%, transparent);
               color: color-mix(in srgb, var(--accent) 95%, white 5%);
               display:inline-block;
               margin-left:auto;
             }
             /* compact variant (useful for inline banners) */
             .warn-compact{
               max-width:100%;
               border-radius:10px;
               padding:12px;
               gap:12px;
             }
             /* small animation */
             @keyframes drop-in {
               from{ transform: translateY(14px) scale(.995); opacity:0 }
               to{ transform: translateY(0) scale(1); opacity:1 }
             }
             /* responsiveness */
             @media (max-width:420px){
               .warn-wrap{ padding:14px; gap:12px; border-radius:12px; }
               .warn-icon{ flex-basis:40px; height:40px; }
               .btn{ min-height:36px; padding:8px 10px; font-size:0.9rem }
             }
             /* subtle focus styles for keyboard nav (accessible in WebView if focusable) */
             .btn:focus, .close-btn:focus { outline: 3px solid color-mix(in srgb, var(--accent) 28%, transparent); outline-offset:2px }
           </style>
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

	final static String emptyField = "<span class='field'>\u00A0</span>";

	private static weeWXApp instance = null;
	private Colours colours;
	private static int lastNightMode = -1;

	final static int minimum_inigo_version = 4000;

	final static boolean radarforecast_default = false;
	final static boolean disableSwipeOnRadar_default = false;
	final static boolean onlyWIFI_default = false;
	final static boolean useIcons_default = true;
	final static boolean metric_default = true;
	final static boolean showIndoor_default = true;
	final static boolean use_exact_alarm_default = false;
	final static boolean save_app_debug_logs = false;
	final static boolean next_moon_default = false;

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
	final static String debug_filename = "weeWXApp_Debug.log.gz";

	final static boolean RadarOnHomeScreen = true;
	final static boolean ForecastOnHomeScreen = !RadarOnHomeScreen;
	final static boolean RadarOnForecastScreen = !RadarOnHomeScreen;
	final static boolean ForecastOnForecastScreen = RadarOnHomeScreen;

	static String[] updateOptions, themeOptions, widgetThemeOptions;

	static boolean hasBootedFully = false;

	@Override
	public void onCreate()
	{
		instance = this;

		super.onCreate();

		setStrings();

		KeyValue.save_app_debug_logs = weeWXAppCommon.GetBoolPref("save_app_debug_logs", weeWXApp.save_app_debug_logs);

		if(KeyValue.save_app_debug_logs)
			weeWXAppCommon.LogMessage("Debug logging enabled...", true);
		else
			weeWXAppCommon.LogMessage("Debug logging disabled...", true);

		weeWXAppCommon.LogMessage("weeWXApp.java app_version: " + BuildConfig.VERSION_NAME + " starting...", true);

		if(weeWXAppCommon.fixTypes())
			weeWXAppCommon.LogMessage("weeWXApp.java successfully converted preference object types...");
		else
			weeWXAppCommon.LogMessage("weeWXApp.java didn't need to convert preference object types...");

		colours = new Colours();

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
			WebViewPreloader.getInstance().init(6);

		applyTheme(false);

		weeWXAppCommon.LogMessage("weeWXApp.java UpdateCheck.cancelAlarm();", true);
		UpdateCheck.cancelAlarm();

		weeWXAppCommon.LogMessage("weeWXApp.java UpdateCheck.setNextAlarm();", true);
		UpdateCheck.setNextAlarm();

		weeWXAppCommon.LogMessage("weeWXApp.java UpdateCheck.runInTheBackground(false, true);", true);
		UpdateCheck.runInTheBackground(false, true);
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
			getAndroidString(R.string.every_hour)
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

	static void applyTheme(boolean forced)
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

		if(forced)
		{
			weeWXAppCommon.SendIntent(weeWXAppCommon.STOP_FORECAST_INTENT);
			weeWXAppCommon.SendIntent(weeWXAppCommon.STOP_RADAR_INTENT);
			weeWXAppCommon.SendIntent(weeWXAppCommon.STOP_WEATHER_INTENT);
			weeWXAppCommon.SendIntent(weeWXAppCommon.STOP_WEBCAM_INTENT);
		}

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
		current_dialog_html = replaceHTMLString(dialog_html, "CURRENT_LANG", lang);
		weeWXAppCommon.LogMessage("Current app language: " + lang);
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