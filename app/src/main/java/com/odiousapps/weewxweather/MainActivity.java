package com.odiousapps.weewxweather;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.window.OnBackInvokedDispatcher;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URLDecoder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.github.evilbunny2008.colourpicker.CPEditText;

import org.json.JSONObject;


import static androidx.core.view.WindowCompat.enableEdgeToEdge;

import static com.github.evilbunny2008.colourpicker.Common.parseHexToColour;
import static com.github.evilbunny2008.colourpicker.Common.to_ARGB_hex;

import static com.odiousapps.weewxweather.weeWXApp.getAndroidString;
import static com.odiousapps.weewxweather.weeWXApp.getEnglishAndroidString;
import static com.odiousapps.weewxweather.weeWXAppCommon.checkURL;
import static com.odiousapps.weewxweather.weeWXAppCommon.doStackOutput;
import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;
import static com.odiousapps.weewxweather.weeWXAppCommon.is_valid_url;
import static com.odiousapps.weewxweather.weeWXAppCommon.json_keys;
import static com.odiousapps.weewxweather.weeWXAppCommon.json_labels;
import static com.odiousapps.weewxweather.weeWXAppCommon.loadOrDownloadImage;
import static com.odiousapps.weewxweather.weeWXAppCommon.mergeJsonObjects;
import static com.odiousapps.weewxweather.weeWXAppCommon.str2Float;

@SuppressWarnings({"unused", "FieldCanBeLocal", "UnspecifiedRegisterReceiverFlag",
		"UnsafeIntentLaunch", "NotifyDataSetChanged", "SourceLockedOrientationActivity",
		"ConstantConditions", "SameParameterValue", "SequencedCollectionMethodCanBeUsed"})
public class MainActivity extends FragmentActivity
{
	private static MainActivity instance;
	private boolean hasStarted = false;

	private static final int NOTIFICATION_PERMISSION_CODE = 1001;

	private TabLayout tabLayout;
	private DrawerLayout mDrawerLayout;
	private TextInputLayout fgtil, bgtil;
	private TextInputEditText settingsURL, customURL;
	private CPEditText widgetBG, widgetFG;
	private MaterialButton b1, b2, b3, b4;
	private MaterialAutoCompleteTextView s1, s2, s3, s4, s5;
	private MaterialSwitch wifi_only, show_indoor, metric_forecasts, rain_in_inches,
			use_exact_alarm, save_app_debug_logs, next_moon, force_dark_mode,
			morning_temp_alert, afternoon_temp_alert, rainfall_alert,
			rainrate_alert_watch, rainrate_alert_warning, rainrate_alert_severe;
	private MaterialRadioButton showRadar, showForecast;
	private ViewPager2 mViewPager;
	private Slider sliderMorningTemp, sliderAfternoonTemp, sliderRainfall;

	private LinearLayout settingLayout, aboutLayout, morning_temp_setting, afternoon_temp_setting, rainfall_setting;

	private ScrollView scrollView;

	private SectionsStateAdapter mSectionsPagerAdapter;

	private final String utf8 = "UTF-8";

	private int UpdateFrequency, DayNightMode, widget_theme_mode, UpdateInterval, webcamInterval;

	private int appInitialLeft, appInitialRight, appInitialTop, appInitialBottom;
	private int cdInitialLeft, cdInitialRight, cdInitialTop, cdInitialBottom;
	private int dlInitialLeft, dlInitialRight, dlInitialTop, dlInitialBottom;
	private int rvInitialLeft, rvInitialRight, rvInitialTop, rvInitialBottom;

	private ImageButton hamburger;
	private boolean gestureNav = false;

	private int extraPx;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private Future<?> backgroundTask;

	private long bgStart = 0;

	private int theme;

	private record Setting(String name, int ResId) {}

	private final List<Setting> screen_elements = new ArrayList<>();

	ColorStateList strokeColors;

	private AlertDialog dialog;

	private int MorningTemp = 230;
	private int AfternoonTemp = 260;

	private int RainfallLimit = 2500;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		instance = this;

		super.onCreate(savedInstanceState);

		if(KeyValue.webview_major_version < 83)
		{
			Intent intent = new Intent(this, WebViewTooOldActivity.class);
			startActivity(intent);
			finish();
			return;
		}

		theme = (int)KeyValue.readVar("theme", weeWXApp.theme_default);

		setTheme(theme);

		if(weeWXApp.theme_default == R.style.AppTheme_weeWXApp_Light_Common)
			LogMessage("MainActivity.onCreate() weeWXApp.theme_default: R.style.AppTheme_weeWXApp_Light_Common");
		else if(weeWXApp.theme_default == R.style.AppTheme_weeWXApp_Dark_Common)
			LogMessage("MainActivity.onCreate() weeWXApp.theme_default: R.style.AppTheme_weeWXApp_Dark_Common");
		else if(weeWXApp.theme_default == R.style.AppTheme_weeWXApp_Common)
			LogMessage("MainActivity.onCreate() weeWXApp.theme_default: R.style.AppTheme_weeWXApp_Common");
		else
			LogMessage("MainActivity.onCreate() theme: " + weeWXApp.theme_default);

		if(theme == R.style.AppTheme_weeWXApp_Light_Common)
			LogMessage("MainActivity.onCreate() theme: R.style.AppTheme_weeWXApp_Light_Common");
		else if(theme == R.style.AppTheme_weeWXApp_Dark_Common)
			LogMessage("MainActivity.onCreate() theme: R.style.AppTheme_weeWXApp_Dark_Common");
		else if(theme == R.style.AppTheme_weeWXApp_Common)
			LogMessage("MainActivity.onCreate() theme: R.style.AppTheme_weeWXApp_Common");
		else
			LogMessage("MainActivity.onCreate() theme: " + theme);

		screen_elements.add(new Setting("about_the_app", R.id.about_the_app));
		screen_elements.add(new Setting("aboutText", R.id.aboutText));
		screen_elements.add(new Setting("bgTextInputLayout", R.id.bgTextInputLayout));
		screen_elements.add(new Setting("customURL", R.id.customURL));
		screen_elements.add(new Setting("fgTextInputLayout", R.id.fgTextInputLayout));
		screen_elements.add(new Setting("force_dark_mode", R.id.force_dark_mode));
		screen_elements.add(new Setting("metric_forecasts", R.id.metric_forecasts));
		screen_elements.add(new Setting("mtv1", R.id.mtv1));
		screen_elements.add(new Setting("mtv2", R.id.mtv2));
		screen_elements.add(new Setting("next_moon", R.id.next_moon));
		screen_elements.add(new Setting("rain_in_inches", R.id.rain_in_inches));
		screen_elements.add(new Setting("save_app_debug_logs", R.id.save_app_debug_logs));
		screen_elements.add(new Setting("settings", R.id.settings));
		screen_elements.add(new Setting("showRadar", R.id.showRadar));
		screen_elements.add(new Setting("showForecast", R.id.showForecast));
		screen_elements.add(new Setting("show_indoor", R.id.show_indoor));
		screen_elements.add(new Setting("spinner1", R.id.spinner1));
		screen_elements.add(new Setting("spinner2", R.id.spinner2));
		screen_elements.add(new Setting("spinner3", R.id.spinner3));
		screen_elements.add(new Setting("spinner4", R.id.spinner4));
		screen_elements.add(new Setting("spinner5", R.id.spinner5));
		screen_elements.add(new Setting("til1", R.id.til1));
		screen_elements.add(new Setting("til2", R.id.til2));
		screen_elements.add(new Setting("til3", R.id.til3));
		screen_elements.add(new Setting("til4", R.id.til4));
		screen_elements.add(new Setting("til5", R.id.til5));
		screen_elements.add(new Setting("til6", R.id.til6));
		screen_elements.add(new Setting("til7", R.id.til7));
		screen_elements.add(new Setting("use_exact_alarm", R.id.use_exact_alarm));
		screen_elements.add(new Setting("widgetBG", R.id.widgetBG));
		screen_elements.add(new Setting("widgetFG", R.id.widgetFG));
		screen_elements.add(new Setting("wifi_only", R.id.wifi_only));

		screen_elements.add(new Setting("morning_temp_alert", R.id.morning_temp_alert));
		screen_elements.add(new Setting("morning_temp_setting", R.id.morning_temp_setting));

		screen_elements.add(new Setting("tvMorningTempLabel", R.id.tvMorningTempLabel));
		screen_elements.add(new Setting("tvMorningTempValue", R.id.tvMorningTempValue));
		screen_elements.add(new Setting("sliderMorningTemp", R.id.sliderMorningTemp));
		screen_elements.add(new Setting("minMorningTemp", R.id.minMorningTemp));
		screen_elements.add(new Setting("maxMorningTemp", R.id.maxMorningTemp));

		screen_elements.add(new Setting("afternoon_temp_alert", R.id.afternoon_temp_alert));
		screen_elements.add(new Setting("afternoon_temp_setting", R.id.afternoon_temp_setting));

		screen_elements.add(new Setting("tvAfternoonTempLabel", R.id.tvAfternoonTempLabel));
		screen_elements.add(new Setting("tvAfternoonTempValue", R.id.tvAfternoonTempValue));
		screen_elements.add(new Setting("sliderAfternoonTemp", R.id.sliderAfternoonTemp));
		screen_elements.add(new Setting("minAfternoonTemp", R.id.minAfternoonTemp));
		screen_elements.add(new Setting("maxAfternoonTemp", R.id.maxAfternoonTemp));

		screen_elements.add(new Setting("rainfall_alert", R.id.rainfall_alert));
		screen_elements.add(new Setting("rainfall_setting", R.id.rainfall_setting));

		screen_elements.add(new Setting("tvRainfallLimitLabel", R.id.tvRainfallLimitLabel));
		screen_elements.add(new Setting("tvRainfallLimitValue", R.id.tvRainfallLimitValue));
		screen_elements.add(new Setting("sliderRainfallLimit", R.id.sliderRainfallLimit));
		screen_elements.add(new Setting("minRainfallLimit", R.id.minRainfallLimit));
		screen_elements.add(new Setting("maxRainfallLimit", R.id.maxRainfallLimit));

		screen_elements.add(new Setting("rainrate_alert_watch", R.id.rainrate_alert_watch));
		screen_elements.add(new Setting("rainrate_alert_warning", R.id.rainrate_alert_warning));
		screen_elements.add(new Setting("rainrate_alert_severe", R.id.rainrate_alert_severe));

		LogMessage("MainActivity.onCreate() started...");

		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

		LogMessage("MainActivity.onCreate() smallestScreenWidthDp: " + weeWXApp.smallestScreenWidth());
		LogMessage("MainActivity.onCreate() minWidth=" + weeWXApp.getWidth() +
		                          ", minHeight=" + weeWXApp.getHeight());

		extraPx = (int)(75 * weeWXApp.getInstance().getResources().getDisplayMetrics().density + 0.5f);

		if(!weeWXApp.isTablet())
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		setContentView(R.layout.main_activity);

		Window window = getWindow();
		WindowInsetsControllerCompat controller =
				WindowCompat.getInsetsController(window, window.getDecorView());

		controller.setAppearanceLightStatusBars(false);
		controller.setAppearanceLightNavigationBars(false);

		// Show the status bar...
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
		{
			WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
			insetsController.show(WindowInsetsCompat.Type.systemBars());
		}

		mDrawerLayout = findViewById(R.id.drawer_layout);
		mDrawerLayout.addDrawerListener(handleDrawerListener);

		hamburger = findViewById(R.id.hamburger);
		hamburger.setOnClickListener(arg0 ->
		{
			if(mDrawerLayout.isDrawerOpen(GravityCompat.START))
				mDrawerLayout.closeDrawer(GravityCompat.START);
			else
				mDrawerLayout.openDrawer(GravityCompat.START);
		});

		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
		{
			// Legacy back handling for Android < 13
			LogMessage("MainActivity.onCreate() setupBackHandling() setting getOnBackPressedDispatcher() SDK < TIRAMISU");
			getOnBackPressedDispatcher().addCallback(this, obpc);
		} else {
			// Android 13+ predictive back gestures
			// Only intercept the back if keyboard is visible or drawer is open
			LogMessage("MainActivity.onCreate() setupBackHandling() setting getOnBackInvokedDispatcher() SDK >= TIRAMISU");
			getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
		}

		myLinearLayout cd = findViewById(R.id.custom_drawer);
		cd.setBackgroundColor(weeWXApp.getColours().bgColour);
		cd.setOnTouchedListener((v) ->
		{
			//LogMessage("MainActivity.onCreate() cd.TouchedListener()");
			handleTouch();
		});

		AppBarLayout abl = findViewById(R.id.appbar);
		FrameLayout rv = findViewById(R.id.root_view);
		scrollView = findViewById(R.id.sv1);

		appInitialLeft = abl.getPaddingLeft();
		appInitialTop = abl.getPaddingTop();
		appInitialRight = abl.getPaddingRight();
		appInitialBottom = abl.getPaddingBottom();

		cdInitialLeft = cd.getPaddingLeft();
		cdInitialTop = cd.getPaddingTop();
		cdInitialRight = cd.getPaddingRight();
		cdInitialBottom = cd.getPaddingBottom();

		dlInitialLeft = mDrawerLayout.getPaddingLeft();
		dlInitialTop = mDrawerLayout.getPaddingTop();
		dlInitialRight = mDrawerLayout.getPaddingRight();
		dlInitialBottom = mDrawerLayout.getPaddingBottom();

		rvInitialLeft = rv.getPaddingLeft();
		rvInitialTop = rv.getPaddingTop();
		rvInitialRight = rv.getPaddingRight();
		rvInitialBottom = rv.getPaddingBottom();

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
		{
			try
			{
				gestureNav = Settings.Secure.getInt(getContentResolver(),
						"navigation_mode", 0) == 2;
			} catch(Exception e) {
				LogMessage("MainActivity.onCreate() Error! e: " + e.getMessage(), true, KeyValue.e);
			}
		}

		ViewCompat.setOnApplyWindowInsetsListener(abl, (v, insets) ->
		{
			Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
			int top = appInitialTop + sb.top;
			v.setPadding(appInitialLeft, top, appInitialRight, appInitialBottom);
			return insets;
		});

		ViewCompat.setOnApplyWindowInsetsListener(cd, (v, insets) ->
		{
			Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
			Insets nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
			Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
			int top = cdInitialTop + sb.top;
			int bottom = cdInitialBottom + Math.max(nb.bottom, ime.bottom);
			v.setPadding(cdInitialLeft, top, cdInitialRight, bottom);
			return insets;
		});

		ViewCompat.setOnApplyWindowInsetsListener(mDrawerLayout, (view, insets) ->
		{
			Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
			Insets nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
			Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

			int top = dlInitialTop + sb.top;
			int bottom = dlInitialBottom + Math.max(nb.bottom, ime.bottom);
			view.setBackgroundColor(weeWXApp.getColour(R.color.MyAppNavBarColour));
			view.setPadding(dlInitialLeft, top, dlInitialRight, bottom);
			return insets;
		});

		ViewCompat.setOnApplyWindowInsetsListener(rv, (view, insets) ->
		{
			Insets nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
			Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
			int bottom = rvInitialBottom + Math.max(nb.bottom, ime.bottom);
			view.setBackgroundColor(weeWXApp.getColour(R.color.MyAppNavBarColour));
			view.setPadding(rvInitialLeft, rvInitialTop, rvInitialRight, bottom);

			if(!gestureNav)
			{
				boolean oldNav = gestureNav;

				Insets gestureInsets = insets.getInsets(WindowInsetsCompat.Type.systemGestures());

				gestureNav = gestureInsets.left > 0 || gestureInsets.right > 0;

				if(oldNav != gestureNav)
					updateHamburger();
			}

			return insets;
		});

		tabLayout = findViewById(R.id.tabs);

		mSectionsPagerAdapter = new SectionsStateAdapter(getSupportFragmentManager(), getLifecycle());
		mSectionsPagerAdapter.addFragment(new Weather());
		mSectionsPagerAdapter.addFragment(new Stats());
		mSectionsPagerAdapter.addFragment(new Forecast());
		mSectionsPagerAdapter.addFragment(new Webcam());
		mSectionsPagerAdapter.addFragment(new Custom());

		mViewPager = findViewById(R.id.container);
		mViewPager.setOffscreenPageLimit(mSectionsPagerAdapter.getItemCount());
		mViewPager.setAdapter(mSectionsPagerAdapter);
		reduceViewPagerSwipeSensitivity(mViewPager);

		String[] tabTitles;
		if(KeyValue.isPrefSet("radarforecast") &&
		   (boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default) == weeWXApp.RadarOnHomeScreen)
			tabTitles = new String[]{getAndroidString(R.string.weather2),
			                         getAndroidString(R.string.stats2),
			                         getAndroidString(R.string.forecast2),
			                         getAndroidString(R.string.webcam2),
			                         getAndroidString(R.string.custom2)};
		else
			tabTitles = new String[]{getAndroidString(R.string.weather2),
			                         getAndroidString(R.string.stats2),
			                         getAndroidString(R.string.radar),
			                         getAndroidString(R.string.webcam2),
			                         getAndroidString(R.string.custom2)};

		new TabLayoutMediator(tabLayout, mViewPager,
				((tab, position) -> tab.setText(tabTitles[position]))).attach();

		if(savedInstanceState != null)
		{
			int page = savedInstanceState.getInt("page", 0);
			mViewPager.setCurrentItem(page, false);
		}

		if(!KeyValue.isPrefSet(json_keys[0] + "_url"))
			mDrawerLayout.openDrawer(GravityCompat.START);

		settingLayout = findViewById(R.id.settingLayout);
		aboutLayout = findViewById(R.id.aboutLayout);

		settingsURL = findViewById(R.id.settings);
		customURL = findViewById(R.id.customURL);

		metric_forecasts = findViewById(R.id.metric_forecasts);
		metric_forecasts.setOnCheckedChangeListener((buttonView, isChecked) ->
		{
			if(hasStarted)
			{
				LogMessage("RainfallLimit: " + RainfallLimit);

				if(isChecked)
				{
					MorningTemp = Math.round(weeWXAppCommon.F2C(MorningTemp / 10f) * 10);
					AfternoonTemp = Math.round(weeWXAppCommon.F2C(AfternoonTemp / 10f) * 10f);
					RainfallLimit = Math.round(weeWXAppCommon.in2mm(RainfallLimit));
				} else {
					MorningTemp = Math.round(weeWXAppCommon.C2F(MorningTemp / 10f) * 10);
					AfternoonTemp = Math.round(weeWXAppCommon.C2F(AfternoonTemp / 10f) * 10);
					RainfallLimit = Math.round(weeWXAppCommon.mm2in(RainfallLimit));
				}

				LogMessage("RainfallLimit: " + RainfallLimit);

				updateSliders(isChecked, MorningTemp, AfternoonTemp);
				updateSliders2(isChecked, RainfallLimit);
			}
		});

		rain_in_inches = findViewById(R.id.rain_in_inches);
		show_indoor = findViewById(R.id.show_indoor);

		use_exact_alarm = findViewById(R.id.use_exact_alarm);
		use_exact_alarm.setOnCheckedChangeListener((buttonView, isChecked) ->
		{
			if(isChecked)
				UpdateCheck.promptForExact(this);
		});

		save_app_debug_logs = findViewById(R.id.save_app_debug_logs);
		next_moon = findViewById(R.id.next_moon);
		force_dark_mode = findViewById(R.id.force_dark_mode);

		b1 = findViewById(R.id.saveButton);
		b2 = findViewById(R.id.deleteData);
		b3 = findViewById(R.id.aboutButton);
		b4 = findViewById(R.id.settingsButton);

		s1 = findViewById(R.id.spinner1);
		s1.setOnItemClickListener((parent, view, position, id) ->
		{
			UpdateFrequency = position;
			LogMessage("MainActivity.onCreate() New UpdateFrequency: " + UpdateFrequency);
		});

		s2 = findViewById(R.id.spinner2);
		s2.setOnItemClickListener((parent, view, position, id) ->
		{
			DayNightMode = position;
			LogMessage("MainActivity.onCreate() New DayNightMode: " + DayNightMode);
		});

		s3 = findViewById(R.id.spinner3);
		s3.setOnItemClickListener((parent, view, position, id) ->
		{
			widget_theme_mode = position;
			if(widget_theme_mode == 4)
			{
				fgtil.post(() -> fgtil.setVisibility(View.VISIBLE));
				bgtil.post(() -> bgtil.setVisibility(View.VISIBLE));
			} else {
				fgtil.post(() -> fgtil.setVisibility(View.GONE));
				bgtil.post(() -> bgtil.setVisibility(View.GONE));
			}

			LogMessage("MainActivity.onCreate() New widget_theme_mode: " + widget_theme_mode);
		});

		s4 = findViewById(R.id.spinner4);
		s4.setOnItemClickListener((parent, view, position, id) ->
		{
			UpdateInterval = position;
			LogMessage("MainActivity.onCreate() New UpdateInterval: " + UpdateInterval);
		});

		s5 = findViewById(R.id.spinner5);
		s5.setOnItemClickListener((parent, view, position, id) ->
		{
			webcamInterval = position;
			LogMessage("MainActivity.onCreate() New webcamInterval: " + webcamInterval);
		});

		widgetBG = findViewById(R.id.widgetBG);
		widgetFG = findViewById(R.id.widgetFG);

		wifi_only = findViewById(R.id.wifi_only);

		showRadar = findViewById(R.id.showRadar);
		showForecast = findViewById(R.id.showForecast);

		sliderMorningTemp = findViewById(R.id.sliderMorningTemp);
		TextView tvMorningTempValue = findViewById(R.id.tvMorningTempValue);

		sliderMorningTemp.addOnChangeListener((s, value, fromUser) ->
		{
			String tempUnit = "C";
			if(!metric_forecasts.isChecked())
				tempUnit = "F";

			MorningTemp = Math.round(value);

			tvMorningTempValue.setText(String.format(Locale.getDefault(), "%.1f°" + tempUnit, (value / 10f)));
		});

		sliderAfternoonTemp = findViewById(R.id.sliderAfternoonTemp);
		TextView tvAfternoonTempValue = findViewById(R.id.tvAfternoonTempValue);

		sliderAfternoonTemp.addOnChangeListener((s, value, fromUser) ->
		{
			String tempUnit = "C";
			if(!metric_forecasts.isChecked())
				tempUnit = "F";

			AfternoonTemp = Math.round(value);

			tvAfternoonTempValue.setText(String.format(Locale.getDefault(), "%.1f°" + tempUnit, (value / 10f)));
		});

		sliderRainfall = findViewById(R.id.sliderRainfallLimit);
		TextView tvRainfallValue = findViewById(R.id.tvRainfallLimitValue);

		sliderRainfall.addOnChangeListener((s, value, fromUser) ->
		{
			RainfallLimit = Math.round(value);

			if(metric_forecasts.isChecked() && !rain_in_inches.isChecked())
				tvRainfallValue.setText(String.format(Locale.getDefault(), "%.1fmm", (value / 100)));
			else
				tvRainfallValue.setText(String.format(Locale.getDefault(), "%.2fin", (value / 100)));
		});

		morning_temp_setting = findViewById(R.id.morning_temp_setting);
		morning_temp_alert = findViewById(R.id.morning_temp_alert);
		morning_temp_alert.setOnCheckedChangeListener((buttonView, isChecked) ->
		{
		    if(isChecked)
		    {
				requestNotificationPermission();
				morning_temp_setting.setVisibility(View.VISIBLE);
		    } else {
				morning_temp_setting.setVisibility(View.GONE);
		    }
		});

		afternoon_temp_setting = findViewById(R.id.afternoon_temp_setting);
		afternoon_temp_alert = findViewById(R.id.afternoon_temp_alert);
		afternoon_temp_alert.setOnCheckedChangeListener((buttonView, isChecked) ->
		{
		    if(isChecked)
		    {
				requestNotificationPermission();
				afternoon_temp_setting.setVisibility(View.VISIBLE);
		    } else {
				afternoon_temp_setting.setVisibility(View.GONE);
		    }
		});

		rainfall_setting = findViewById(R.id.rainfall_setting);
		rainfall_alert = findViewById(R.id.rainfall_alert);
		rainfall_alert.setOnCheckedChangeListener((buttonView, isChecked) ->
		{
		    if(isChecked)
		    {
				requestNotificationPermission();
				rainfall_setting.setVisibility(View.VISIBLE);
		    } else {
				rainfall_setting.setVisibility(View.GONE);
		    }
		});

		rainrate_alert_watch = findViewById(R.id.rainrate_alert_watch);
		rainrate_alert_watch.setOnCheckedChangeListener((buttonView, isChecked) ->
		{
		    if(isChecked)
				requestNotificationPermission();
		});

		rainrate_alert_warning = findViewById(R.id.rainrate_alert_warning);
		rainrate_alert_warning.setOnCheckedChangeListener((buttonView, isChecked) ->
		{
		    if(isChecked)
				requestNotificationPermission();
		});

		rainrate_alert_severe = findViewById(R.id.rainrate_alert_severe);
		rainrate_alert_severe.setOnCheckedChangeListener((buttonView, isChecked) ->
		{
		    if(isChecked)
				requestNotificationPermission();
		});

		fgtil = findViewById(R.id.fgTextInputLayout);
		bgtil = findViewById(R.id.bgTextInputLayout);

		int fg, bg;
		boolean wo, met, rii, si, sr, sf, uea, sadl, nm, fdm, mta, ata, rfa, rra_watch, rra_warning, rra_severe;
		int mt, at, rfl;

		boolean newDataUrlSet = KeyValue.isPrefSet(json_keys[0] + "_url");
		if(newDataUrlSet)
		{
			String dataURL = (String)KeyValue.readVar(json_keys[0] + "_url", "");
			if(dataURL == null || dataURL.isBlank())
				newDataUrlSet = false;
		}

		boolean lastDownloadSet = KeyValue.isPrefSet("LastDownload");
		if(lastDownloadSet)
		{
			String lastDownload = (String)KeyValue.readVar("LastDownload", "");
			if(lastDownload == null || lastDownload.isBlank())
				lastDownloadSet = false;
		}

		if(lastDownloadSet)
		{
			if(!newDataUrlSet)
			{
				LogMessage("MainActivity.onCreate() showUpdateAvailable2() triggered because no JSON URL detected.");
				showUpdateAvailable2();
			} else {
				KeyValue.putVar("LastDownload", null);
			}
		}

		UpdateFrequency = (int)KeyValue.readVar("UpdateFrequency", weeWXApp.UpdateFrequency_default);
		UpdateInterval = (int)KeyValue.readVar("UpdateInterval", weeWXApp.UpdateInterval_default);
		webcamInterval = (int)KeyValue.readVar("webcamInterval", weeWXApp.webcamInterval_default);
		DayNightMode = (int)KeyValue.readVar("DayNightMode", weeWXApp.DayNightMode_default);
		widget_theme_mode =	(int)KeyValue.readVar(weeWXAppCommon.WIDGET_THEME_MODE, weeWXApp.widget_theme_mode_default);

		LogMessage("MainActivity.onCreate() DayNightMode: " + DayNightMode);

		bg = (int)KeyValue.readVar("widgetBG", weeWXApp.widgetBG_default);
		fg = (int)KeyValue.readVar("widgetFG", weeWXApp.widgetFG_default);

		wo = (boolean)KeyValue.readVar("onlyWIFI", weeWXApp.onlyWIFI_default);
		met = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		rii = (boolean)KeyValue.readVar("rainInInches", weeWXApp.rain_in_inches_default);
		si = (boolean)KeyValue.readVar("showIndoor", weeWXApp.showIndoor_default);
		sr = (boolean)KeyValue.readVar("radarforecast", weeWXApp.radarforecast_default);

		uea = (boolean)KeyValue.readVar("use_exact_alarm", weeWXApp.use_exact_alarm_default);
		sadl = (boolean)KeyValue.readVar("save_app_debug_logs", weeWXApp.save_app_debug_logs_default);
		nm = (boolean)KeyValue.readVar("next_moon", weeWXApp.next_moon_default);
		fdm = (boolean)KeyValue.readVar("force_dark_mode", weeWXApp.force_dark_mode_default);

		mta = (boolean)KeyValue.readVar("morning_temp_alert", weeWXApp.morning_temp_alert_default);
		mt = (int)KeyValue.readVar("MorningTemp", weeWXApp.MorningTemp_default);

		ata = (boolean)KeyValue.readVar("afternoon_temp_alert", weeWXApp.afternoon_temp_alert_default);
		at = (int)KeyValue.readVar("AfternoonTemp", weeWXApp.AfternoonTemp_default);

		rfa = (boolean)KeyValue.readVar("rainfall_alert", weeWXApp.rainfall_alert_default);
		rfl = (int)KeyValue.readVar("RainfallLimit", weeWXApp.RainfallLimit_default);

		rra_watch = (boolean)KeyValue.readVar("rainrate_alert_watch", weeWXApp.rainrate_alert_watch_default);
		rra_warning = (boolean)KeyValue.readVar("rainrate_alert_warning", weeWXApp.rainrate_alert_warning_default);
		rra_severe = (boolean)KeyValue.readVar("rainrate_alert_severe", weeWXApp.rainrate_alert_severe_default);

		if(savedInstanceState != null)
		{
			LogMessage("MainActivity.onCreate() Reading current settings that were saved in a bundle....");
			UpdateFrequency = savedInstanceState.getInt("UpdateFrequency", UpdateFrequency);
			UpdateInterval = savedInstanceState.getInt("UpdateInterval", UpdateInterval);
			webcamInterval = savedInstanceState.getInt("webcamInterval", webcamInterval);
			DayNightMode = savedInstanceState.getInt("DayNightMode", DayNightMode);
			widget_theme_mode = savedInstanceState.getInt("widget_theme_mode", widget_theme_mode);

			LogMessage("MainActivity.onCreate() UpdateFrequency: " + UpdateFrequency);
			LogMessage("MainActivity.onCreate() UpdateInterval: " + UpdateInterval);
			LogMessage("MainActivity.onCreate() webcamInterval: " + webcamInterval);
			LogMessage("MainActivity.onCreate() DayNightMode: " + DayNightMode);
			LogMessage("MainActivity.onCreate() widget_theme_mode: " + widget_theme_mode);

			bg = savedInstanceState.getInt("bg", bg);
			fg = savedInstanceState.getInt("fg", fg);

			wo = savedInstanceState.getBoolean("wo", wo);
			met = savedInstanceState.getBoolean("met", met);
			rii = savedInstanceState.getBoolean("rii", rii);
			si = savedInstanceState.getBoolean("si", si);
			sr = savedInstanceState.getBoolean("sr", sr);
			uea = savedInstanceState.getBoolean("uea", uea);
			sadl = savedInstanceState.getBoolean("sadl", sadl);
			nm = savedInstanceState.getBoolean("nm", nm);
			fdm = savedInstanceState.getBoolean("fdm", fdm);

			mta = savedInstanceState.getBoolean("mta", mta);
			mt = savedInstanceState.getInt("mt", mt);

			ata = savedInstanceState.getBoolean("ata", ata);
			at = savedInstanceState.getInt("at", at);

			rfa = savedInstanceState.getBoolean("rfa", rfa);
			rfl = savedInstanceState.getInt("rfl", rfl);

			rra_watch = savedInstanceState.getBoolean("rra_watch", rra_watch);
			rra_warning = savedInstanceState.getBoolean("rra_warning", rra_warning);
			rra_severe = savedInstanceState.getBoolean("rra_severe", rra_severe);

			LogMessage("MainActivity.onCreate() DayNightMode: " + DayNightMode);
		}

		// https://github.com/Pes8/android-material-color-picker-dialog
		String hex = CPEditText.getFixedChar() + String.format("%08X", bg).toUpperCase();
		LogMessage("MainActivity.onCreate() Setting widgetBG to " + to_ARGB_hex(hex));
		final String bghex = hex;
		widgetBG.setText(bghex);
		widgetBG.setOnTouchListener((v, event) ->
		{
			if(event.getAction() == MotionEvent.ACTION_UP)
				handleTouch();

			return false;
		});

		hex = CPEditText.getFixedChar() + String.format("%08X", fg).toUpperCase();
		LogMessage("MainActivity.onCreate() Setting widgetFG to " + to_ARGB_hex(hex));
		final String fghex = hex;
		widgetFG.setText(fghex);
		widgetFG.setOnTouchListener((v, event) ->
		{
			if(event.getAction() == MotionEvent.ACTION_UP)
				handleTouch();

			return false;
		});

		wifi_only.setChecked(wo);
		metric_forecasts.setChecked(met);
		rain_in_inches.setChecked(rii);
		show_indoor.setChecked(si);
		use_exact_alarm.setChecked(uea);
		save_app_debug_logs.setChecked(sadl);
		next_moon.setChecked(nm);
		force_dark_mode.setChecked(fdm);

		morning_temp_alert.setChecked(mta);
		MorningTemp = mt;

		afternoon_temp_alert.setChecked(ata);
		AfternoonTemp = at;

		updateSliders(met, mt, at);

		rainfall_alert.setChecked(rfa);
		RainfallLimit = rfl;

		rainrate_alert_watch.setChecked(rra_watch);
		rainrate_alert_warning.setChecked(rra_warning);
		rainrate_alert_severe.setChecked(rra_severe);

		updateSliders2(met, rfl);

		LogMessage("MainActivity.onCreate() DayNightMode: " + DayNightMode);

		showRadar.setChecked(sr);
		showForecast.setChecked(!sr);

		settingLayout.setVisibility(View.VISIBLE);
		aboutLayout.setVisibility(View.GONE);

		b1.setOnClickListener(arg0 ->
		{
			LogMessage("MainActivity.onCreate() Starting save settings!");
			b1.setEnabled(false);
			b2.setEnabled(false);
			closeKeyboard();

			int layout = R.layout.layout_loading_dialog_dark;
			if(KeyValue.theme == R.style.AppTheme_weeWXApp_Light_Common)
				layout = R.layout.layout_loading_dialog_light;

			LogMessage("MainActivity.onCreate() show dialog");
			dialog = new AlertDialog.Builder(this)
					.setCancelable(false)
					.setView(layout)
					.create();

			dialog.show();

			LogMessage("MainActivity.onCreate() Process settings!");
			processSettings();
		});

		b2.setOnClickListener(arg0 -> checkReally());

		b3.setOnClickListener(arg0 ->
		{
			loadAboutText();
			settingLayout.setVisibility(View.GONE);
			aboutLayout.setVisibility(View.VISIBLE);
			scrollView.smoothScrollTo(0, 0);
		});

		b4.setOnClickListener(arg0 ->
		{
			aboutLayout.setVisibility(View.GONE);
			settingLayout.setVisibility(View.VISIBLE);
			scrollView.smoothScrollTo(0, 0);
		});

		settingsURL.setText((String)KeyValue.readVar("SETTINGS_URL", weeWXApp.SETTINGS_URL_default));
		settingsURL.setOnFocusChangeListener((v, hasFocus) ->
		{
			if(!hasFocus)
				closeKeyboard(v);
		});

		customURL.setText((String)KeyValue.readVar("custom_url", ""));
		customURL.setOnFocusChangeListener((v, hasFocus) ->
		{
			LogMessage("MainActivity.onCreate() CustomURL has a focus change event...");

			if(!hasFocus)
			{
				LogMessage("MainActivity.onCreate() CustomURL lost focus change event...");
				closeKeyboard(v);
				return;
			}

			LogMessage("MainActivity.onCreate() CustomURL gained focus change event...");

			final View root = getWindow().getDecorView().getRootView();
			final ViewTreeObserver.OnGlobalLayoutListener listener = new ViewTreeObserver.OnGlobalLayoutListener()
			{
				@Override
				public void onGlobalLayout()
				{
					// Remove listener immediately — only want to run once
					root.getViewTreeObserver().removeOnGlobalLayoutListener(this);

					// Now the layout has resized, scroll to the EditText
					scrollView.postDelayed(() ->
					{
						LogMessage("MainActivity.onCreate() Old scrollY: " + scrollView.getScrollY());
						int scrollY = scrollView.getScrollY() + extraPx;
						LogMessage("MainActivity.onCreate() New scrollY: " + scrollY);
						scrollView.smoothScrollTo(0, Math.max(scrollY, 0));
					}, 200);
				}
			};

			root.getViewTreeObserver().addOnGlobalLayoutListener(listener);
		});

		enableEdgeToEdge(window);

		setStrings();
		updateHamburger();
		updateDropDowns();
		updateColours();
		WidgetProvider.updateAppWidget();

		LogMessage("MainActivity.onCreate() loading NotificationManager...");
		weeWXAppCommon.NotificationManager.getNotificationLiveData().observe(this, notificationObserver);

		if(!screen_elements.isEmpty())
		{
			Setting s = screen_elements.get(0);
			weeWXAppCommon.LogColour(findViewById(s.ResId()), s.name());
		}

		LogMessage("MainActivity.onCreate() has finished...");
		hasStarted = true;
	}

	private void updateSliders(boolean met, float morning, float afternoon)
	{
		LogMessage("met: " + met);
		LogMessage("morning: " + morning);
		LogMessage("afternoon: " + afternoon);

		MaterialTextView minMorningTemp = findViewById(R.id.minMorningTemp);
		MaterialTextView maxMorningTemp = findViewById(R.id.maxMorningTemp);
		MaterialTextView minAfternoonTemp = findViewById(R.id.minAfternoonTemp);
		MaterialTextView maxAfternoonTemp = findViewById(R.id.maxAfternoonTemp);

		int minTemp = 160;
		int maxTemp = 300;
		String tempUnit = "°C";

		if(!met)
		{
			minTemp = Math.round(weeWXAppCommon.C2F(minTemp / 10f) * 10);
			maxTemp = Math.round(weeWXAppCommon.C2F(maxTemp / 10f) * 10);
			tempUnit = "°F";
		}

		sliderMorningTemp.setValueFrom(-999);
		sliderMorningTemp.setValueTo(999);

		morning = Math.round(morning);

		if(morning < minTemp || morning > maxTemp)
			morning = weeWXApp.MorningTemp_default;

		sliderMorningTemp.setValue(morning);
		sliderMorningTemp.setValueFrom(minTemp);
		sliderMorningTemp.setValueTo(maxTemp);

		sliderAfternoonTemp.setValueFrom(-999);
		sliderAfternoonTemp.setValueTo(999);

		afternoon = Math.round(afternoon);

		if(afternoon < minTemp || afternoon > maxTemp)
			afternoon = weeWXApp.AfternoonTemp_default;

		sliderAfternoonTemp.setValue(afternoon);
		sliderAfternoonTemp.setValueFrom(minTemp);
		sliderAfternoonTemp.setValueTo(maxTemp);

		String str = (minTemp / 10) + tempUnit;
		minMorningTemp.setText(str);
		minAfternoonTemp.setText(str);

		str = (maxTemp / 10) + tempUnit;
		maxMorningTemp.setText(str);
		maxAfternoonTemp.setText(str);
	}

	private void updateSliders2(boolean met, float rainfall)
	{
		LogMessage("met: " + met);
		LogMessage("rainfall: " + rainfall);

		MaterialTextView minRainfallLimit = findViewById(R.id.minRainfallLimit);
		MaterialTextView maxRainfallLimit = findViewById(R.id.maxRainfallLimit);

		int step = 20;
		int lowerlimit = step;
		int upperLimit = 25000;

		if(!met)
		{
			step = 1;
			lowerlimit = step;
			upperLimit = 10000;
		}

		sliderRainfall.setStepSize(step);
		sliderRainfall.setValueTo(99999);

		rainfall = Math.round(rainfall);

		if(met)
			rainfall = Math.round(rainfall / step) * step;

		if(rainfall < lowerlimit || rainfall > upperLimit)
			rainfall = weeWXApp.RainfallLimit_default;

		sliderRainfall.setValue(rainfall);
		sliderRainfall.setValueFrom(lowerlimit);
		sliderRainfall.setValueTo(upperLimit);

		String str = String.format(Locale.getDefault(), "%.1fmm", (lowerlimit / 100f));
		if(!met)
			str = String.format(Locale.getDefault(), "%.2fin", (lowerlimit / 100f));

		minRainfallLimit.setText(str);

		str = String.format(Locale.getDefault(), "%.1fmm", (upperLimit / 100f));
		if(!met)
			str = String.format(Locale.getDefault(), "%.2fin", (upperLimit / 100f));

		maxRainfallLimit.setText(str);
	}

	private void reduceViewPagerSwipeSensitivity(ViewPager2 viewPager)
	{
		try
		{
			Field recyclerViewField = ViewPager2.class.getDeclaredField("mRecyclerView");
			recyclerViewField.setAccessible(true);
			RecyclerView recyclerView = (RecyclerView) recyclerViewField.get(viewPager);

			Field touchSlopField = RecyclerView.class.getDeclaredField("mTouchSlop");
			touchSlopField.setAccessible(true);
			int touchSlop = (int) touchSlopField.get(recyclerView);
			touchSlopField.set(recyclerView, touchSlop * 3);
		} catch(Exception e) {
			doStackOutput(e);
		}
	}

	@Override
	public void onDestroy()
	{
		LogMessage("MainActivity.onDestroy()");
		super.onDestroy();

		weeWXAppCommon.NotificationManager.getNotificationLiveData().removeObservers(this);
	}

	private void loadAboutText()
	{
		ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
		ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
		am.getMemoryInfo(memInfo);

		// App's own memory usage
		Runtime runtime = Runtime.getRuntime();
		long usedMemory = runtime.totalMemory() - runtime.freeMemory();
		long maxMemory = runtime.maxMemory();

		float USEDMEMORY = usedMemory / 1_048_576f;
		float MAXMEMORY = maxMemory / 1_048_576f;

		MaterialTextView tv1 = findViewById(R.id.aboutText);
		String about_blurb = weeWXApp.current_about_blurb
				.replace("USEDMEMORY", String.format(Locale.ENGLISH, "%.1f MB", USEDMEMORY))
				.replace("MAXMEMORY", String.format(Locale.ENGLISH, "%.1f MB", MAXMEMORY));

		tv1.setText(HtmlCompat.fromHtml(about_blurb, HtmlCompat.FROM_HTML_MODE_COMPACT));
		tv1.setMovementMethod(LinkMovementMethod.getInstance());
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if(KeyValue.isVisible)
			return;

		KeyValue.isVisible = true;

		if(!UpdateCheck.canSetExact(this) && use_exact_alarm.isChecked())
			use_exact_alarm.setChecked(false);

		if(ActivityCompat.checkSelfPermission(instance, Manifest.permission.POST_NOTIFICATIONS)
				   != PackageManager.PERMISSION_GRANTED && !KeyValue.hasNotificationPerm)
		{
			if(morning_temp_alert.isChecked())
				morning_temp_alert.setChecked(false);

			if(afternoon_temp_alert.isChecked())
				afternoon_temp_alert.setChecked(false);

			if(rainfall_alert.isChecked())
				rainfall_alert.setChecked(false);

			if(rainrate_alert_watch.isChecked())
				rainrate_alert_watch.setChecked(false);

			if(rainrate_alert_warning.isChecked())
				rainrate_alert_warning.setChecked(false);

			if(rainrate_alert_severe.isChecked())
				rainrate_alert_severe.setChecked(false);
		}

		UpdateCheck.cancelAlarm();

		UpdateCheck.setNextAlarm();

		UpdateCheck.runInTheBackground(false, false);
	}

	@Override
	protected void onPause()
	{
		KeyValue.isVisible = false;

		super.onPause();
	}

	private void updateColours()
	{
		int fgColour = weeWXApp.getColours().fgColour;

		LogMessage("MainActivity.onCreate() fgColour: #" + Integer.toHexString(fgColour));

		for(Setting s : screen_elements)
		{
			View view = findViewById(s.ResId());
			switch(view)
			{
				case TextInputLayout v ->
				{
					v.setBoxStrokeColorStateList(strokeColors);
					v.setDefaultHintTextColor(strokeColors);
					v.setPlaceholderTextColor(strokeColors);
					v.setHintTextColor(strokeColors);
					v.setHelperTextColor(strokeColors);
				}
				case MaterialTextView v ->
				{
					v.setTextColor(fgColour);
					v.setHintTextColor(fgColour);
				}
				case TextInputEditText v ->
				{
					v.setTextColor(fgColour);
					v.setHintTextColor(fgColour);
				}
				case MaterialSwitch v ->
				{
					v.setTextColor(fgColour);
					v.setHintTextColor(fgColour);
				}
				case MaterialAutoCompleteTextView v ->
				{
					v.setTextColor(fgColour);
					v.setHintTextColor(fgColour);
				}
				case MaterialRadioButton v ->
				{
					v.setTextColor(fgColour);
					v.setHintTextColor(fgColour);
				}
				case TextView v ->
				{
					v.setTextColor(fgColour);
					v.setHintTextColor(fgColour);
				}
				case Slider v -> LogMessage("MainActivity.updateColours(): Slider detected");
				case LinearLayout v -> LogMessage("MainActivity.updateColours(): LinearLayout detected");
				default -> LogMessage("MainActivity.updateColours() Uncaught view type: " + view, KeyValue.w);
			}
		}
	}

	private void updateHamburger()
	{
		if(gestureNav)
		{
			LogMessage("MainActivity.updateHamburger() gestureNav == true, show the hamburger menu...");
			if(hamburger.getVisibility() != View.VISIBLE)
				hamburger.setVisibility(View.VISIBLE);
		} else {
			LogMessage("MainActivity.updateHamburger() gestureNav == false, hide the hamburger menu...");
			if(hamburger.getVisibility() != View.GONE)
				hamburger.setVisibility(View.GONE);
		}
	}

	final DrawerLayout.SimpleDrawerListener handleDrawerListener = new DrawerLayout.SimpleDrawerListener()
	{
		@Override
		public void onDrawerOpened(@NonNull View drawerView)
		{
			//LogMessage("MainActivity.handleDrawerListener() Detected a back press in the DrawerLayout...");

			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			View focus = getCurrentFocus();
			if(imm != null && focus != null && imm.isAcceptingText())
				closeKeyboard(drawerView, imm);
		}
	};

	final OnBackPressedCallback obpc = new OnBackPressedCallback(true)
	{
		@Override
		public void handleOnBackPressed()
		{
			//LogMessage("MainActivity.handleOnBackPressed()");
			handleBack();
		}
	};

	void handleBack()
	{
		//LogMessage("MainActivity.handleBack() Detected an application back press...");
		View focus = getCurrentFocus();
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if(focus != null && imm != null && imm.isAcceptingText())
		{
			//LogMessage("MainActivity.handleBack() Let's hide the on screen keyboard and clearFocus()...");
			closeKeyboard(focus, imm);
			return;
		}

		if(mDrawerLayout.isDrawerOpen(GravityCompat.START))
		{
			//LogMessage("MainActivity.handleBack() Let's shut the drawer...");
			closeDrawer();
			return;
		}

		if(mViewPager.getCurrentItem() > 0)
		{
			LogMessage("MainActivity.handleBack() Cycle through tabs until we hit tab 0");
			mViewPager.post(() -> mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1));
			return;
		}

		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
		{
			LogMessage("MainActivity.handleBack() Let's end now... SDK < TIRAMISU");
			obpc.setEnabled(false);
			finish();
		} else {
			LogMessage("MainActivity.handleBack() SDK >= TIRAMISU... Let the system do it's thing...");
			getOnBackPressedDispatcher().onBackPressed();
		}
	}

	void handleTouch()
	{
		if(settingsURL != null && settingsURL.isFocused())
		{
			closeKeyboard();
			if(settingsURL.isFocused())
				settingsURL.clearFocus();
		}

		if(customURL != null && customURL.isFocused())
		{
			closeKeyboard();
			if(customURL.isFocused())
				customURL.clearFocus();
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState)
	{
		super.onSaveInstanceState(outState);

		LogMessage("MainActivity.onSaveInstanceState() Stashing current settings into a bundle....");
		outState.putInt("page", mViewPager.getCurrentItem());
		outState.putInt("UpdateFrequency", UpdateFrequency);
		outState.putInt("UpdateInterval", UpdateInterval);
		outState.putInt("webcamInterval", webcamInterval);
		outState.putInt("DayNightMode", DayNightMode);
		outState.putInt("widget_theme_mode", widget_theme_mode);

		LogMessage("MainActivity.onSaveInstanceState() UpdateFrequency: " + UpdateFrequency);
		LogMessage("MainActivity.onSaveInstanceState() UpdateInterval: " + UpdateInterval);
		LogMessage("MainActivity.onSaveInstanceState() webcamInterval: " + webcamInterval);
		LogMessage("MainActivity.onSaveInstanceState() DayNightMode: " + DayNightMode);
		LogMessage("MainActivity.onSaveInstanceState() widget_theme_mode: " + widget_theme_mode);

		Editable edit = widgetBG.getText();
		if(edit != null && edit.length() > 0)
		{
			int bg = parseHexToColour(edit.toString());
			outState.putInt("bg", bg);
		} else {
			outState.putInt("bg", weeWXApp.widgetBG_default);
		}

		edit = widgetFG.getText();
		if(edit != null && edit.length() > 0)
		{
			int fg = parseHexToColour(edit.toString());
			outState.putInt("fg", fg);
		} else {
			outState.putInt("fg", weeWXApp.widgetFG_default);
		}

		outState.putBoolean("wo", wifi_only.isChecked());
		outState.putBoolean("met", metric_forecasts.isChecked());
		outState.putBoolean("rii", rain_in_inches.isChecked());
		outState.putBoolean("si", show_indoor.isChecked());
		outState.putBoolean("sr", showRadar.isChecked());
		outState.putBoolean("uea", use_exact_alarm.isChecked());
		outState.putBoolean("sadl", save_app_debug_logs.isChecked());
		outState.putBoolean("nm", next_moon.isChecked());
		outState.putBoolean("fdm", force_dark_mode.isChecked());

		outState.putBoolean("mta", morning_temp_alert.isChecked());
		outState.putInt("mt", (int)sliderMorningTemp.getValue());

		outState.putBoolean("ata", afternoon_temp_alert.isChecked());
		outState.putInt("at", (int)sliderAfternoonTemp.getValue());

		outState.putBoolean("rfa", rainfall_alert.isChecked());
		outState.putInt("rfl", RainfallLimit);

		outState.putBoolean("rra_watch", rainrate_alert_watch.isChecked());
		outState.putBoolean("rra_warning", rainrate_alert_warning.isChecked());
		outState.putBoolean("rra_severe", rainrate_alert_severe.isChecked());
	}

	private void setStrings()
	{
		int fgColour = weeWXApp.getColours().fgColour;

		int disabled = weeWXApp.getColours().LightGray;
		if(theme == R.style.AppTheme_weeWXApp_Dark_Common)
			disabled = weeWXApp.getColours().DarkGray;

		strokeColors = new ColorStateList(new int[][]{new int[]{android.R.attr.state_enabled},      // default enabled
		                                              new int[]{-android.R.attr.state_enabled},     // disabled
		                                              new int[]{android.R.attr.state_focused},      // focused
		                                              new int[]{-android.R.attr.state_focused},     // not focused
		}, new int[]{fgColour,  // default
		             disabled,  // disabled
		             fgColour,  // focused
		             fgColour   // unfocused
		});
	}

	private ArrayAdapter<String> newArrayAdapter(int resId, String[] strings)
	{
		return new ArrayAdapter<>(this, resId, strings)
		{
			@NonNull
			@Override
			public View getView(int position, View convertView, @NonNull ViewGroup parent)
			{
				MaterialTextView view = (MaterialTextView)super.getView(position, convertView, parent);
				view.setBackgroundColor(weeWXApp.getColours().bgColour);
				view.setTextColor(weeWXApp.getColours().fgColour);
				return view;
			}

			@Override
			public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent)
			{
				MaterialTextView view = (MaterialTextView)super.getDropDownView(position, convertView, parent);
				view.setBackgroundColor(weeWXApp.getColours().bgColour);
				view.setTextColor(weeWXApp.getColours().fgColour);
				return view;
			}
		};
	}

	private void updateDropDowns()
	{
		ArrayAdapter<String> adapter1 = newArrayAdapter(R.layout.spinner_layout, weeWXApp.updateOptions);
		ArrayAdapter<String> adapter2 = newArrayAdapter(R.layout.spinner_layout, weeWXApp.themeOptions);
		ArrayAdapter<String> adapter3 = newArrayAdapter(R.layout.spinner_layout, weeWXApp.widgetThemeOptions);
		ArrayAdapter<String> adapter4 = newArrayAdapter(R.layout.spinner_layout, weeWXApp.updateInterval);
		ArrayAdapter<String> adapter5 = newArrayAdapter(R.layout.spinner_layout, weeWXApp.webcamRefreshOptions);

		adapter1.notifyDataSetChanged();
		adapter2.notifyDataSetChanged();
		adapter3.notifyDataSetChanged();

		if(UpdateFrequency < 0 || UpdateFrequency >= weeWXApp.updateOptions.length)
			UpdateFrequency = 1;

		if(UpdateInterval < 0 || UpdateInterval >= weeWXApp.updateInterval.length)
			UpdateInterval = 0;

		if(webcamInterval < 0 || webcamInterval >= weeWXApp.webcamRefreshOptions.length)
			webcamInterval = 0;

		final int uf = UpdateFrequency;
		final int ui = UpdateInterval;
		final int wi = webcamInterval;
		final int dnm = DayNightMode;
		final int wtm = widget_theme_mode;

		runOnUiThread(() ->
		{
			LogMessage("MainActivity.updateDropDowns() UpdateFrequency: " + uf);
			LogMessage("MainActivity.updateDropDowns() UpdateInterval: " + ui);
			LogMessage("MainActivity.updateDropDowns() webcamInterval: " + wi);
			LogMessage("MainActivity.updateDropDowns() DayNightMode: " + dnm);
			LogMessage("MainActivity.updateDropDowns() widget_theme_mode: " + wtm);

			s1.setAdapter(adapter1);
			s1.setText(weeWXApp.updateOptions[uf], false);

			s2.setAdapter(adapter2);
			s2.setText(weeWXApp.themeOptions[dnm], false);

			if(wtm == 4)
			{
				fgtil.setVisibility(View.VISIBLE);
				bgtil.setVisibility(View.VISIBLE);
			} else {
				fgtil.setVisibility(View.GONE);
				bgtil.setVisibility(View.GONE);
			}

			s3.setAdapter(adapter3);
			s3.setText(weeWXApp.widgetThemeOptions[wtm], false);

			s4.setAdapter(adapter4);
			s4.setText(weeWXApp.updateInterval[ui], false);

			s5.setAdapter(adapter5);
			s5.setText(weeWXApp.webcamRefreshOptions[wi], false);
		});
	}

	void closeKeyboard()
	{
		View focus = getCurrentFocus();
		closeKeyboard(focus);
	}

	void closeKeyboard(View view)
	{
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if(view != null && imm != null && imm.isAcceptingText())
			closeKeyboard(view, imm);
	}

	void closeKeyboard(View focus, InputMethodManager imm)
	{
		if(focus != null && imm != null && imm.isAcceptingText())
		{
			LogMessage("MainActivity.closeKeyboard() Let's hide the on screen keyboard...");
			imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
			focus.clearFocus();
		}
	}

	void closeDrawer()
	{
		if(mDrawerLayout.isDrawerOpen(GravityCompat.START))
		{
			LogMessage("MainActivity.closeDrawer() Let's shut the drawer...");
			mDrawerLayout.closeDrawer(GravityCompat.START);
		}
	}

	private void showUpdateAvailable()
	{
		int updateVer = Math.max(weeWXApp.minimum_inigo_version, weeWXApp.minimum_inigo_version);

		if((boolean)KeyValue.readVar("shownUpdate_" + updateVer, false))
			return;

		KeyValue.putVar("shownUpdate_" + updateVer, true);

		new AlertDialog.Builder(this)
				.setCancelable(false)
				.setIcon(R.mipmap.ic_launcher_foreground)
				.setTitle(getAndroidString(R.string.app_name))
				.setMessage(getAndroidString(R.string.inigo_needs_updating))
				.setPositiveButton(getAndroidString(R.string.ok), null)
				.create()
				.show();
	}

	private void showUpdateAvailable2()
	{
		LogMessage("Will now show update2 dialog now...");
		String str = getAndroidString(R.string.json_formatted_data);
		str = String.format(str, "weeWX App", "JSON formatted", "weeWX", "Inigo Plugin", "inigo-settings.txt", "GitHub.com");
		new AlertDialog.Builder(this)
				.setIcon(R.mipmap.ic_launcher_foreground)
				.setTitle(getAndroidString(R.string.app_name))
				.setMessage(str)
				.setPositiveButton(getAndroidString(R.string.ok), (dialog_interface, i) ->
				{
					String url = "https://github.com/evilbunny2008/weeWXWeatherApp/wiki/JSON-formatted-data-file";
					Intent urlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					startActivity(urlIntent);
				})
				.setNegativeButton(getAndroidString(R.string.skip), null)
				.create()
				.show();

		LogMessage("update2 dialog should now be visible...");
	}

	private void checkReally()
	{
		new AlertDialog.Builder(this)
				.setMessage(getAndroidString(R.string.remove_all_data)).setCancelable(false)
				.setPositiveButton(getAndroidString(R.string.ok), (dialog_interface, i) ->
				{
					String settings_url = settingsURL.getText() != null && !settingsURL.getText().toString().isBlank() ? settingsURL.getText().toString().strip() : "";

					LogMessage("MainActivity.checkReally() Reset any widgets...");
					WidgetProvider.resetAppWidget();

					LogMessage("MainActivity.checkReally() trash the webcam and radar images if they exist...");
					weeWXAppCommon.delImage(weeWXApp.webcamFilename);
					weeWXAppCommon.delImage(weeWXApp.radarFilename);

					LogMessage("MainActivity.checkReally() trash all data and exit cleanly...");
					((ActivityManager)getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();

					if(settings_url != null && !settings_url.isBlank())
					{
						LogMessage("MainActivity.checkReally() Save the settings URL before exitting...");
						KeyValue.putVar("SETTINGS_URL", settings_url);
					}
				})
				.setNegativeButton(getAndroidString(R.string.no), (dialog_interface, i) -> dialog_interface.cancel())
				.create()
				.show();
	}

	private void resetScreen()
	{
		LogMessage("MainActivity.resetScreen() Do some stuff here...");

		closeKeyboard();
		closeDrawer();

		scrollView.scrollTo(0, 0);
		dialog.dismiss();
	}

	private void processSettings()
	{
		LogMessage("MainActivity.java processSettings() running the background updates...");

		long now = System.currentTimeMillis();

		UpdateCheck.cancelAlarm();

		long dur = (now - bgStart) / 1000;
		if(backgroundTask != null && !backgroundTask.isDone())
		{
			if(dur < 30)
			{
				LogMessage("processSettings() executor is still running and is less than 30s old (" +
				           dur + "s), skipping...",	true, KeyValue.w);
				return;
			}

			LogMessage("processSettings() Cancelling the current background executor...");
			backgroundTask.cancel(true);
			backgroundTask = null;
		}

		bgStart = now;
		backgroundTask = executor.submit(() ->
		{
			LogMessage("processSettings() bg executor started...");

			String tmpStr;
			String forecastLocationName = KeyValue.countyName = null;

			boolean validURL = false;
			boolean validURL2;
			boolean validURL3;
			boolean validURL5 = false;

			String oldData = null, jsonData = null, radtype = "", radarURL = "", forecastURL = "", webcamURL = "",
					CustomURL = "", appCustomURL, fctype = "", bomtown = "";
			String[] json_urls = new String[json_keys.length];

			String settings_url = settingsURL.getText() != null ? settingsURL.getText().toString().strip() : "";
			LogMessage("processSettings() settings_url: " + settings_url);

			if(settings_url.isBlank() || settings_url.equals(weeWXApp.SETTINGS_URL_default))
			{
				errorDialog(R.string.url_was_default_or_empty);
				return;
			}

			try
			{
				String settingsData = weeWXAppCommon.downloadSettings(settings_url).strip();
				//LogMessage("processSettings() settingsData: " + settingsData);

				if(settingsData == null || settingsData.isBlank() || settingsData.length() < 128)
				{
					errorDialog(R.string.wasnt_able_to_connect_settings);
					return;
				}

				String[] bits = settingsData.split("\\n");
				if(bits != null && bits.length > 1)
				{
					for(String bit : bits)
					{
						String line = bit.strip();
						if(line.isBlank() || line.startsWith("#") || !line.contains("="))
							continue;

						String[] mb = line.split("=", 2);
						mb[0] = mb[0].toLowerCase(Locale.ENGLISH).strip();
						mb[1] = mb[1].strip();

						LogMessage("processSettings() mb[0]: " + mb[0], KeyValue.d);
						LogMessage("processSettings() mb[1]: " + mb[1], KeyValue.d);

						switch(mb[0])
						{
							case "data" -> oldData = mb[1].strip();
							case "jsondata" -> jsonData = mb[1].strip();
							case "json-data" -> json_urls[0] = mb[1].strip();
							case "json-dicts" -> json_urls[1] = mb[1].strip();
							case "json-last" -> json_urls[2] = mb[1].strip();
							case "radar" -> radarURL = mb[1].strip();
							case "radtype" -> radtype = mb[1].toLowerCase(Locale.ENGLISH).strip();
							case "forecast" -> forecastURL = mb[1].strip();
							case "fctype" -> fctype = mb[1].toLowerCase(Locale.ENGLISH).strip();
							case "webcam" -> webcamURL = mb[1].strip();
							case "custom" -> CustomURL = mb[1].strip();
							default -> LogMessage("processSettings() Invalid setting: " + mb[0] +
							                                     "=" + mb[1] + ", skipping...", true, KeyValue.w);
						}
					}
				}

				LogMessage("processSettings() here0!");
			} catch(Exception e) {
				LogMessage("processSettings() Error! e: " + e.getMessage(), true, KeyValue.e);
				doStackOutput(e);
				errorDialog(e);
				return;
			}

			LogMessage("processSettings() here0a!");

			if(!is_valid_url(json_urls[0]) || !is_valid_url(json_urls[1]) || !is_valid_url(json_urls[2]))
			{
				if(is_valid_url(jsonData))
				{
					json_urls[0] = jsonData;
					json_urls[1] = jsonData.replace("inigo-data", "inigo-dicts");
					json_urls[2] = jsonData.replace("inigo-data", "inigo-last");
				} else if(is_valid_url(oldData)) {
					oldData = oldData.replace("inigo-data.txt", "inigo-data.json");
					json_urls[0] = oldData;
					json_urls[1] = oldData.replace("inigo-data", "inigo-dicts");
					json_urls[2] = oldData.replace("inigo-data", "inigo-last");
				}
			}

			LogMessage("processSettings() here0b!");

			for(int i = 0; i < json_urls.length; i++)
			{
				LogMessage("processSettings() json_urls[" + i + "]: " + json_urls[i]);
				if(!is_valid_url(json_urls[i]))
				{
					errorDialog(R.string.wasnt_able_to_download_url_is_blank, json_labels[i]);
					return;
				}

				boolean validURL1;
				try
				{
					LogMessage("processSettings() Checking " + json_labels[i] + ": " + json_urls[i]);
					validURL1 = weeWXAppCommon.reallyGetWeather(i, json_urls[i], true);
				} catch(Exception e) {
					doStackOutput(e);
					errorDialog(e);
					return;
				}

				if(!validURL1)
				{
					errorDialog(R.string.wasnt_able_to_connect_or_download, json_labels[i], json_urls[i]);
					return;
				}
			}

			LogMessage("processSettings() here1!");

			if(!mergeJsonObjects())
			{
				errorDialog(R.string.failed_to_merge_weather_data, json_labels[0], json_labels[2]);
				return;
			}

			LogMessage("processSettings() here2!");

			if(!KeyValue.parseDicts())
			{
				errorDialog(R.string.failed_to_process_weather_data, json_labels[1]);
				return;
			}

			LogMessage("processSettings() here3!");

			if(radtype == null || radtype.isBlank())
			{
				errorDialog(R.string.radar_type_is_invalid);
				return;
			}

			LogMessage("processSettings() here4!");

			if(!is_valid_url(radarURL))
			{
				errorDialog(R.string.wasnt_able_to_connect_radar_image);
				return;
			}

			LogMessage("processSettings() here5!");

			if(fctype == null || fctype.isBlank())
			{
				errorDialog(R.string.forecast_type_is_invalid);
				return;
			}

			LogMessage("processSettings() here6!");

			if(forecastURL == null || forecastURL.isBlank())
			{
				errorDialog(R.string.wasnt_able_to_connect_forecast);
				return;
			}

			LogMessage("processSettings() here7!");

			for(int i = 0; i < json_urls.length; i++)
				LogMessage("processSettings() " + json_labels[i] + ": " + json_urls[i]);

			LogMessage("processSettings() radarURL: " + radarURL);
			LogMessage("processSettings() radtype: " + radtype);
			LogMessage("processSettings() forecastURL: " + forecastURL);
			LogMessage("processSettings() fctype: " + fctype);
			LogMessage("processSettings() webcamURL: " + webcamURL);
			LogMessage("processSettings() CustomURL: " + CustomURL);
			LogMessage("processSettings() forecastURL: " + forecastURL);

			try
			{
				if(fctype.toLowerCase(Locale.ENGLISH).strip().equals("bom3"))
				{
					forecastURL = forecastURL.strip();

					if(forecastURL.length() > 6)
						forecastURL = forecastURL.substring(0, 6);

					if(UpdateInterval > 0)
						fctype = "bom3hourly";
					else
						fctype = "bom3daily";
				}

				switch(fctype.toLowerCase(Locale.ENGLISH).strip())
				{
					case "weatherzone3" ->
					{
						LogMessage("processSettings() fctype: " + fctype);

						String[] URLs;

						if(forecastURL.contains(","))
							URLs = forecastURL.split(",");
						else
							URLs = new String[]{forecastURL};

						if(URLs != null && URLs.length > 1)
						{
							URLs = Arrays.stream(URLs)
									.distinct()
									.toArray(String[]::new);
						} else {
							runOnUiThread(() ->
							{
								b1.setEnabled(true);
								b2.setEnabled(true);
								dialog.dismiss();
							});

							bgStart = 0;
							return;
						}

						String[] allURLs = new String[URLs.length * 7];
						Calendar cal = Calendar.getInstance();
						Date d;
						int rc = 0;
						for(String url : URLs)
						{
							for(int i = 0; i < 7; i++)
							{
								long futureWhen = now + 86_400_000 * i;
								LocalDate date = Instant.ofEpochMilli(futureWhen)
										.atZone(ZoneId.systemDefault())
										.toLocalDate();

								allURLs[rc] = url;
								if(i > 0)
								{
									allURLs[rc] += String.format(Locale.getDefault(), "?date=%04d", date.getYear());
									allURLs[rc] += String.format(Locale.getDefault(), "-%02d", date.getMonthValue());
									allURLs[rc] += String.format(Locale.getDefault(), "-%02d", date.getDayOfMonth());
								}

								rc++;
							}
						}

						Collections.shuffle(Arrays.asList(allURLs));

						int loops = 1;
						for(String url : allURLs)
						{
							forecastURL = url;

							LogMessage("processSettings() URL #" + loops + "/" + allURLs.length);
							LogMessage("processSettings() forecastURL: " + forecastURL);

							String wzHTML;
							boolean addDelay = false;
							int attempt = 1;
							do
							{
								wzHTML = weeWXAppCommon.downloadWZHTML(forecastURL, addDelay, true);
								addDelay = true;
							} while((wzHTML == null || wzHTML.length() < 10_000) && attempt++ <= 3);

							if(wzHTML == null || wzHTML.length() < 10_000)
								continue;

							LogMessage("processSettings() wzHTML.length(): " + wzHTML.length());
							JsoupHelper.processWZhtml(url, wzHTML);

							if(++loops <= allURLs.length)
							{
								int random = weeWXAppCommon.getNextRandom(5_000, 10_000);
								LogMessage("processSettings() Sleeping for " + random + "ms...");
								Thread.sleep(random);
							}
						}

						runOnUiThread(() ->
						{
							b1.setEnabled(true);
							b2.setEnabled(true);
							dialog.dismiss();
						});

						bgStart = 0;
						return;
					}
					case "metservice.com2" ->
					{
						LogMessage("processSettings() fctype: " + fctype);
						LogMessage("processSettings() forecastURL:" + forecastURL);
/*
						String metService = weeWXApp.wvpl.getHTML(forecastURL);
						if(metService != null && metService.length() > 1024)
						{
							String pretty = Jsoup.parse(metService).outputSettings(
									new Document.OutputSettings().indentAmount(2).prettyPrint(true)
							).outerHtml();

							//CustomDebug.writeDebug("weeWX", "metservice.com.7day.html", pretty);
							//LogMessage("processSettings() wrote content to metservice.com.7day.html");
						}
*/
						runOnUiThread(() ->
						{
							b1.setEnabled(true);
							b2.setEnabled(true);
							dialog.dismiss();
							closeDrawer();
						});

						bgStart = 0;
						return;
					}
					case "weatherzone" ->
					{
						forecastURL = "https://rss.weatherzone.com.au/?u=12994-1285&lt=aploc&lc=" + forecastURL + "&obs=0&fc=1&warn=0";
						LogMessage("processSettings() forecastURL: " + forecastURL);
						LogMessage("processSettings() fctype: " + fctype);
					}
					case "met.no" ->
					{
						LogMessage("processSettings() forecastURL: " + forecastURL);
						LogMessage("processSettings() fctype: " + fctype);

						float lat = 0, lon = 0;

						if(forecastURL.startsWith("http"))
						{
							// Full URL provided, extract lat/lon from query params
							URI uri = URI.create(forecastURL);

							if(uri.getQuery() != null)
							{
								for(String pair : uri.getQuery().split("&"))
								{
									int idx = pair.indexOf('=');
									String key = URLDecoder.decode(pair.substring(0, idx), utf8);
									String val = URLDecoder.decode(pair.substring(idx + 1), utf8);

									if(key.equals("lat"))
										lat = str2Float(val);

									if(key.equals("lon"))
										lon = str2Float(val);
								}
							}
						} else if(forecastURL.contains(",")) {
							// Coordinates format: lat,lon
							String[] llbits = forecastURL.split(",", 2);
							lat = str2Float(llbits[0].strip());
							lon = str2Float(llbits[1].strip());
						}

						if(lat != 0 && lon != 0)
						{
							forecastURL = "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=" + lat + "&lon=" + lon;

							String url = "https://odiousapps.com/get-location-name-by-ll.php";

							forecastLocationName = weeWXAppCommon.downloadString(url, Map.of(
									"lat", "" + lat,
									"lon", "" + lon));

							LogMessage("processSettings() forecastLocationName: " + forecastLocationName);
						}

						LogMessage("processSettings() met.no forecastURL: " + forecastURL);
					}
					case "yahoo", "weather.gc.ca", "weather.gc.ca-fr", "metoffice.gov.uk",
					     "bom2", "aemet.es", "dwd.de", "tempoitalia.it", "weatherzone2" ->
					{
						LogMessage("processSettings() forecastURL: " + forecastURL);
						LogMessage("processSettings() fctype: " + fctype);
					}
					case "wmo.int" ->
					{
						if(!forecastURL.startsWith("http"))
							forecastURL = "https://worldweather.wmo.int/en/json/" + forecastURL.strip() + "_en.xml";
						LogMessage("processSettings() forecastURL: " + forecastURL);
						LogMessage("processSettings() fctype: " + fctype);
					}
					case "weather.gov" ->
					{
						String lat = "", lon = "";
						if(forecastURL.contains("?"))
							forecastURL = forecastURL.split("\\?", 2)[1].strip();
						if(forecastURL.contains("lat") && forecastURL.contains("lon"))
						{
							String[] tmp = forecastURL.split("&");
							for (String line : tmp)
							{
								if(line.split("=", 2)[0].equals("lat"))
									lat = line.split("=", 2)[1].strip();
								if(line.split("=", 2)[0].equals("lon"))
									lon = line.split("=", 2)[1].strip();
							}
						} else {
							lat = forecastURL.split(",")[0].strip();
							lon = forecastURL.split(",")[1].strip();
						}

						forecastURL = "https://forecast.weather.gov/MapClick.php?lat=" + lat + "&lon=" + lon + "&unit=0&lg=english&FcstType=json";
						LogMessage("processSettings() forecastURL: " + forecastURL);
						LogMessage("processSettings() fctype: " + fctype);
					}
					case "bom3hourly" ->
					{
						boolean needUpdate = KeyValue.bomGeohash == null || KeyValue.bomGeohash.isBlank() ||
						                     !KeyValue.bomGeohash.equals(forecastURL.strip()) ||
						                     KeyValue.bomLocation == null || KeyValue.bomLocation.isBlank();

						if(needUpdate)
						{
							KeyValue.bomGeohash = forecastURL.strip();
							String newurl = "https://api.weather.bom.gov.au/v1/locations/" + forecastURL.strip();
							String text = weeWXAppCommon.downloadString(newurl, false);
							LogMessage("processSettings(): text: " + text);

							JSONObject jobj = new JSONObject(text);
							if(jobj.has("data"))
							{
								jobj = jobj.getJSONObject("data");
								if(jobj.has("name") && jobj.has("state"))
								{
									KeyValue.bomLocation = jobj.getString("name") + ", " + jobj.getString("state");
									LogMessage("processSettings() BoM Location: " + KeyValue.bomLocation);
								}
							}
						}

						forecastURL = "https://api.weather.bom.gov.au/v1/locations/" + forecastURL.strip() + "/forecasts/hourly";
						LogMessage("processSettings() forecastURL: " + forecastURL);
						LogMessage("processSettings() fctype: " + fctype);
					}
					case "bom3daily" ->
					{
						forecastURL = "https://api.weather.bom.gov.au/v1/locations/" + forecastURL.strip() + "/forecasts/daily";
						LogMessage("processSettings() forecastURL: " + forecastURL);
						LogMessage("processSettings() fctype: " + fctype);
					}
					case "metservice.com" ->
					{
						if(!forecastURL.startsWith("http"))
							forecastURL = "https://www.metservice.com/publicData/localForecast" + forecastURL.strip();
						LogMessage("processSettings() forecastURL: " + forecastURL);
						LogMessage("processSettings() fctype: " + fctype);
					}
					case "openweathermap.org" ->
					{
						if(metric_forecasts.isChecked())
							forecastURL += "&units=metric";
						else
							forecastURL += "&units=imperial";
						forecastURL += "&lang=" + Locale.getDefault().getLanguage();
						forecastURL += "&mode=json&cnt=16";

						LogMessage("processSettings() forecastURL: " + forecastURL);
						LogMessage("processSettings() fctype: " + fctype);
					}
					case "weather.com" ->
					{
						forecastURL = "https://api.weather.com/v3/wx/forecast/daily/10day?geocode=" + forecastURL + "&format=json&apiKey" +
						              "=71f92ea9dd2f4790b92ea9dd2f779061";
						if(metric_forecasts.isChecked())
							forecastURL += "&units=m";
						else
							forecastURL += "&units=e";
						forecastURL += "&language=" + Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry();
						LogMessage("processSettings() forecastURL: " + forecastURL);
						LogMessage("processSettings() fctype: " + fctype);
					}
					case "met.ie" ->
					{
						String[] llbits = forecastURL.split(",", 2);

						float lat = str2Float(llbits[0]);
						float lon = str2Float(llbits[1]);

						if(lat != 0 && lon != 0)
						{
							forecastURL = "http://openaccess.pf.api.met.ie/metno-wdb2ts/locationforecast?" +
							              "lat=" + lat + ";long=" + lon;

							String url = "https://odiousapps.com/get-location-name-by-ll.php";

							forecastLocationName = weeWXAppCommon.downloadString(url, Map.of(
									"lat", "" + lat,
									"lon", "" + lon));

							LogMessage("forecastLocationName: " + forecastLocationName);

							Object[] o = KeyValue.closestCounty(lat, lon);

							if(o != null && o.length == 2)
							{
								KeyValue.countyName = ((KeyValue.County)o[0]).name;
								LogMessage("processSettings() County Name: " + KeyValue.countyName);
								LogMessage("processSettings() Distance: " + (float)o[1]);
							}
						}

						LogMessage("processSettings() forecastURL: " + forecastURL);
						LogMessage("processSettings() fctype: " + fctype);
					}
					default ->
					{
						LogMessage("processSettings() No forecast information...", KeyValue.w);
						errorDialog(R.string.forecast_type_is_invalid, fctype);
						return;
					}
				}
			} catch(Exception e) {
				doStackOutput(e);
				errorDialog(e);
				return;
			}

			LogMessage("processSettings() forecast checking: " + forecastURL);
			LogMessage("processSettings() fctype: " + fctype);
			LogMessage("processSettings() UpdateInterval: " + UpdateInterval);
			LogMessage("processSettings() webcamInterval: " + webcamInterval);

			try
			{
				validURL3 = weeWXAppCommon.reallyGetForecast(fctype, forecastURL, UpdateInterval);
				//LogMessage("processSettings() tmpStr: " + tmpStr);
			} catch(Exception e) {
				//doStackOutput(e);
				errorDialog(e);
				return;
			}

			if(!validURL3)
			{
				errorDialog(R.string.wasnt_able_to_connect_forecast);
				return;
			}

			try
			{
				if(radtype.equals("image"))
					validURL2 = loadOrDownloadImage(radarURL, weeWXApp.radarFilename, false) != null;
				else if(radtype.equals("webpage"))
					validURL2 = checkURL(radarURL, false);
				else
					validURL2 = false;
			} catch(Exception e) {
				doStackOutput(e);
				errorDialog(e);
				return;
			}

			if(!validURL2)
			{
				errorDialog(R.string.wasnt_able_to_connect_radar_image);
				return;
			}

			LogMessage("processSettings() checking: " + webcamURL);

			Bitmap bm;

			try
			{
				bm = loadOrDownloadImage(webcamURL, weeWXApp.webcamFilename, true);
			} catch(Exception e) {
				LogMessage("processSettings() Error! e: " + e.getMessage(), true, KeyValue.e);
				//doStackOutput(e);
				errorDialog(e);
				return;
			}

			if(bm == null)
			{
				LogMessage("processSettings() bm is null!", true, KeyValue.w);
				errorDialog(R.string.wasnt_able_to_connect_webcam_url);
				return;
			}

			appCustomURL = customURL.getText() != null ? customURL.getText().toString().strip() : "";
			try
			{
				if(appCustomURL.isBlank())
				{
					if(!CustomURL.isBlank() && !CustomURL.equals(weeWXApp.CustomURL_default))
					{
						LogMessage("processSettings() Checking url: " + CustomURL);
						validURL5 = checkURL(CustomURL, false);
						if(!validURL5)
							KeyValue.putVar("custom_url", null);
					}
				} else {
					LogMessage("processSettings() Checking url: " + appCustomURL);
					validURL5 = checkURL(appCustomURL, false);
				}
			} catch(Exception e) {
				doStackOutput(e);
				errorDialog(e);
				return;
			}

			if(!validURL5)
			{
				errorDialog(R.string.wasnt_able_to_connect_custom_url);
				return;
			}

			if(KeyValue.countyName != null && KeyValue.countyName.isBlank())
				KeyValue.countyName = null;

			KeyValue.putVar("CountyName", KeyValue.countyName);

			if(KeyValue.bomLocation != null && KeyValue.bomLocation.isBlank())
				KeyValue.bomLocation = null;

			if(KeyValue.bomGeohash != null && KeyValue.bomGeohash.isBlank())
				KeyValue.bomGeohash = null;

			KeyValue.putVar("bomLocation", KeyValue.bomLocation);
			KeyValue.putVar("bomGeohash", KeyValue.bomGeohash);

			KeyValue.putVar("SETTINGS_URL", settingsURL.getText().toString());
			KeyValue.putVar("UpdateFrequency", UpdateFrequency);
			KeyValue.putVar("UpdateInterval", UpdateInterval);
			KeyValue.putVar("webcamInterval", webcamInterval);

			for(int i = 0; i < json_urls.length; i++)
				KeyValue.putVar(json_keys[i] + "_url", json_urls[i]);

			if(forecastURL == null || forecastURL.isBlank())
			{
				KeyValue.putVar("rssCheck", null);
				KeyValue.putVar("forecastData", null);
				KeyValue.putVar("forecastGsonEncoded", null);
				KeyValue.putVar("lastAttemptedForecastDownloadTime", null);
				KeyValue.putVar("FORECAST_URL", null);
				KeyValue.putVar("fctype", null);
				KeyValue.putVar("forecastLocationName", null);
			} else {
				KeyValue.putVar("FORECAST_URL", forecastURL);
				KeyValue.putVar("fctype", fctype);

				if(forecastLocationName == null || forecastLocationName.isBlank())
					forecastLocationName = null;

				KeyValue.putVar("forecastLocationName", forecastLocationName);
			}

			if(radarURL == null || radarURL.isBlank())
			{
				KeyValue.putVar("radtype", null);
				KeyValue.putVar("RADAR_URL", null);
			} else {
				KeyValue.putVar("radtype", radtype);
				KeyValue.putVar("RADAR_URL", radarURL);
			}

			if(webcamURL == null || webcamURL.isBlank())
				KeyValue.putVar("WEBCAM_URL", null);
			else
				KeyValue.putVar("WEBCAM_URL", webcamURL);

			if(CustomURL == null || CustomURL.isBlank())
				KeyValue.putVar("CUSTOM_URL", null);
			else
				KeyValue.putVar("CUSTOM_URL", CustomURL);

			if(appCustomURL == null || appCustomURL.isBlank())
				KeyValue.putVar("custom_url", null);
			else
				KeyValue.putVar("custom_url", appCustomURL);

			KeyValue.putVar("morning_temp_alert", morning_temp_alert.isChecked());
			KeyValue.putVar("MorningTemp", (int)sliderMorningTemp.getValue());

			KeyValue.putVar("afternoon_temp_alert", afternoon_temp_alert.isChecked());
			KeyValue.putVar("AfternoonTemp", (int)sliderAfternoonTemp.getValue());

			KeyValue.putVar("rainfall_alert", rainfall_alert.isChecked());
			KeyValue.putVar("RainfallLimit", (int)sliderRainfall.getValue());

			KeyValue.putVar("rainrate_alert_watch", rainrate_alert_watch.isChecked());
			KeyValue.putVar("rainrate_alert_warning", rainrate_alert_warning.isChecked());
			KeyValue.putVar("rainrate_alert_severe", rainrate_alert_severe.isChecked());

			KeyValue.putVar("metric", metric_forecasts.isChecked());
			KeyValue.putVar("rainInInches", rain_in_inches.isChecked());
			KeyValue.putVar("showIndoor", show_indoor.isChecked());
			KeyValue.putVar("DayNightMode", DayNightMode);
			KeyValue.putVar("onlyWIFI", wifi_only.isChecked());
			KeyValue.putVar("radarforecast", showRadar.isChecked());
			KeyValue.putVar("use_exact_alarm", use_exact_alarm.isChecked());
			KeyValue.putVar("save_app_debug_logs", save_app_debug_logs.isChecked());
			KeyValue.putVar("next_moon", next_moon.isChecked());
			KeyValue.putVar("force_dark_mode", force_dark_mode.isChecked());

			KeyValue.putVar(weeWXAppCommon.WIDGET_THEME_MODE, widget_theme_mode);

			Editable edit = widgetBG.getText();
			if(edit != null && edit.length() > 0)
			{
				int bg = parseHexToColour(edit.toString());
				KeyValue.putVar("widgetBG", bg);
				LogMessage("processSettings() Saved widget bg colour: " + to_ARGB_hex(bg));
			} else {
				KeyValue.putVar("widgetBG", null);
			}

			edit = widgetFG.getText();
			if(edit != null && edit.length() > 0)
			{
				int fg = parseHexToColour(edit.toString());
				KeyValue.putVar("widgetFG", fg);
				LogMessage("processSettings() Saved widget fg colour: " + to_ARGB_hex(fg));
			} else {
				KeyValue.putVar("widgetFG", null);
			}

			LogMessage("processSettings() Refresh widgets if at least one exists...");
			WidgetProvider.updateAppWidget();

			LogMessage("processSettings() Set the alarm...");
			UpdateCheck.setNextAlarm();

			runOnUiThread(() ->
			{
				resetScreen();

				LogMessage("processSettings() Apply the new theme");
				setTheme((int)KeyValue.readVar("theme", weeWXApp.theme_default));
				weeWXApp.applyTheme(false);

				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
				{
				    overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0);
				    overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0);
				}

				LogMessage("processSettings() Recreate the activity...");
				finish();
				startActivity(getIntent());
			});
		});
	}

	void errorDialog(Exception e)
	{
		errorDialog(e.getLocalizedMessage(), e.getMessage());
	}

	void errorDialog(String errorStr, String logStr)
	{
		LogMessage("errorDialog(): logStr: " + logStr);
		bgStart = 0;
		runOnUiThread(() ->
		{
			b1.setEnabled(true);
			b2.setEnabled(true);
			dialog.dismiss();
			new AlertDialog.Builder(this)
					.setTitle(getAndroidString(R.string.error))
					.setMessage(errorStr)
					.setPositiveButton(getAndroidString(R.string.ill_fix_and_try_again), null)
					.create()
					.show();
		});
	}

	void errorDialog(int strid, String... vars)
	{
		String errorStr = String.format(Locale.getDefault(), getAndroidString(strid), (Object[]) vars);
		String logStr = String.format(Locale.ENGLISH, getEnglishAndroidString(strid), (Object[]) vars);
		errorDialog(errorStr, logStr);
	}

	static void runDelayed(long delayMs, Runnable task)
	{
		new Handler(Looper.getMainLooper()).postDelayed(task, delayMs);
	}

	private final Observer<String> notificationObserver = str ->
	{
		LogMessage("notificationObserver: " + str);

		if(str.equals(weeWXAppCommon.INIGO_INTENT))
			showUpdateAvailable();
	};

	public boolean isViewPagerNull()
	{
		return mViewPager == null;
	}

	public void setUserInputPager(boolean b)
	{
		LogMessage("MainActivity.setUserInputPager()");
		if(isViewPagerNull())
			return;

		mViewPager.post(() -> mViewPager.setUserInputEnabled(b));
	}

	static MainActivity getInstance()
	{
		return instance;
	}

	static class SectionsStateAdapter extends FragmentStateAdapter
	{
		private final ArrayList<Fragment> arrayList = new ArrayList<>();

		public SectionsStateAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle)
		{
			super(fragmentManager, lifecycle);
		}

		public void addFragment(Fragment fragment)
		{
			arrayList.add(fragment);
		}

		@NonNull
		@Override
		public Fragment createFragment(int position)
		{
			return arrayList.get(position);
		}

		@Override
		public int getItemCount()
		{
			return arrayList.size();
		}
	}

	private void requestNotificationPermission()
	{
		LogMessage("requestNotificationPermission()");

		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
	       ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
		{
			KeyValue.hasNotificationPerm = true;
			return;
		}

		LogMessage("requestNotificationPermission() Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU");

		boolean hasAsked = (boolean)KeyValue.readVar("hasAsked", false);

		LogMessage("requestNotificationPermission() Permission not granted");

		if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS))
		{
			LogMessage("requestNotificationPermission() User previously denied, show rational pop-up");

			KeyValue.putVar("hasAsked", true);

	        // show explanation dialog first
			new AlertDialog.Builder(this)
					.setCancelable(false)
					.setTitle(getAndroidString(R.string.notification_permission))
					.setMessage(getAndroidString(R.string.notifications_needed))
					.setPositiveButton(getAndroidString(R.string.ok), (dialog, which) ->
					{
					    LogMessage("requestNotificationPermission() User choose to retry notification permission");
					    ActivityCompat.requestPermissions(this,
					        new String[]{Manifest.permission.POST_NOTIFICATIONS},
					        NOTIFICATION_PERMISSION_CODE);
					})
					.setNegativeButton(getAndroidString(R.string.no), (dialog, which) ->
					{
					    LogMessage("requestNotificationPermission() User choose not to retry notification permission");
					    disableAlerts();
					})
					.create()
					.show();

			return;
	    }

		if(!hasAsked)
		{
			// first time
			LogMessage("requestNotificationPermission() Showing user notification permission");
			ActivityCompat.requestPermissions(this,
				new String[]{Manifest.permission.POST_NOTIFICATIONS},
				NOTIFICATION_PERMISSION_CODE);

			return;
		}

		LogMessage("requestNotificationPermission() Permission not granted, but already asked twice.");

		new AlertDialog.Builder(this)
				.setTitle(getAndroidString(R.string.notification_permission))
				.setMessage(getAndroidString(R.string.notifications_needed2))
				.setPositiveButton(getAndroidString(R.string.ok), (dialog, which) ->
				{
					LogMessage("requestNotificationPermission() User choose to open settings");
					Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
					intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
					startActivity(intent);
				})
				.setNegativeButton(getAndroidString(R.string.no), (dialog, which) ->
				{
					LogMessage("requestNotificationPermission() User choose to decline opening settings");
					disableAlerts();
				})
				.create()
				.show();
	}

	void disableAlerts()
	{
		morning_temp_alert.setChecked(false);
		afternoon_temp_alert.setChecked(false);
		rainfall_alert.setChecked(false);
		rainrate_alert_watch.setChecked(false);
		rainrate_alert_warning.setChecked(false);
		rainrate_alert_severe.setChecked(false);
		KeyValue.hasNotificationPerm = false;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
	    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

	    if(requestCode == NOTIFICATION_PERMISSION_CODE)
			KeyValue.hasNotificationPerm = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
	    else
		    disableAlerts();
	}
}