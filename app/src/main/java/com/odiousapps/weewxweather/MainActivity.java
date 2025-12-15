package com.odiousapps.weewxweather;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
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

import com.github.evilbunny2008.colourpicker.CPEditText;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
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


import static androidx.core.view.WindowCompat.enableEdgeToEdge;
import static com.github.evilbunny2008.colourpicker.Common.parseHexToColour;
import static com.github.evilbunny2008.colourpicker.Common.to_ARGB_hex;

@SuppressWarnings({"unused", "FieldCanBeLocal", "UnspecifiedRegisterReceiverFlag",
		"UnsafeIntentLaunch", "NotifyDataSetChanged", "SourceLockedOrientationActivity",
		"ConstantConditions", "SameParameterValue"})
public class MainActivity extends FragmentActivity
{
	private TabLayout tabLayout;
	private DrawerLayout mDrawerLayout;
	private TextInputLayout fgtil, bgtil;
	private TextInputEditText settingsURL, customURL;
	private CPEditText fgColour, bgColour;
	private MaterialButton b1, b2, b3, b4;
	private MaterialAutoCompleteTextView s1, s2, s3;
	private MaterialSwitch wifi_only, show_indoor, metric_forecasts, use_exact_alarm,
			save_app_debug_logs, next_moon, force_dark_mode;
	private MaterialRadioButton showRadar, showForecast;
	private static ViewPager2 mViewPager;

	private AlertDialog dialog;

	private LinearLayout settingLayout, aboutLayout;

	private ScrollView scrollView;

	private SectionsStateAdapter mSectionsPagerAdapter;

	private static int UpdateFrequency, DayNightMode, widget_theme_mode;

	private int appInitialLeft, appInitialRight, appInitialTop, appInitialBottom;
	private int cdInitialLeft, cdInitialRight, cdInitialTop, cdInitialBottom;
	private int dlInitialLeft, dlInitialRight, dlInitialTop, dlInitialBottom;
	private int rvInitialLeft, rvInitialRight, rvInitialTop, rvInitialBottom;

	private ImageButton hamburger;
	private boolean gestureNav = false;

	private int extraPx;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private Future<?> backgroundTask;

	private long bgStart;

	private final int[] screen_elements = new int[]
	{
		R.id.about_the_app,
		R.id.aboutText,
		R.id.til1,
		R.id.settings,
		R.id.show_indoor,
		R.id.metric_forecasts,
		R.id.wifi_only,
		R.id.til2,
		R.id.spinner1,
		R.id.til3,
		R.id.spinner2,
		R.id.til4,
		R.id.spinner3,
		R.id.fgTextInputLayout,
		R.id.fg_Picker,
		R.id.bgTextInputLayout,
		R.id.bg_Picker,
		R.id.mtv1,
		R.id.mtv2,
		R.id.showRadar,
		R.id.showForecast,
		R.id.til5,
		R.id.customURL,
		R.id.use_exact_alarm,
		R.id.save_app_debug_logs,
		R.id.next_moon,
		R.id.force_dark_mode
	};

	ColorStateList strokeColors;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		setTheme(KeyValue.theme);

		super.onCreate(savedInstanceState);

		weeWXAppCommon.LogMessage("MainActivity.ocCreate() started...");

		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

		weeWXAppCommon.LogMessage("smallestScreenWidthDp: " + weeWXApp.smallestScreenWidth());
		weeWXAppCommon.LogMessage("minWidth=" + weeWXApp.getWidth() +
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
			weeWXAppCommon.LogMessage("setupBackHandling() setting getOnBackPressedDispatcher() SDK < TIRAMISU");
			getOnBackPressedDispatcher().addCallback(this, obpc);
		} else {
			// Android 13+ predictive back gestures
			// Only intercept the back if keyboard is visible or drawer is open
			weeWXAppCommon.LogMessage("setupBackHandling() setting getOnBackInvokedDispatcher() SDK >= TIRAMISU");
			Api33BackHandler.setup(this);
		}

		myLinearLayout cd = findViewById(R.id.custom_drawer);
		cd.setBackgroundColor(KeyValue.bgColour);
		cd.setOnTouchedListener((v) ->
		{
			weeWXAppCommon.LogMessage("cd.TouchedListener()");
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
				weeWXAppCommon.LogMessage("Error! e: " + e, true);
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
		if(weeWXAppCommon.isPrefSet("radarforecast") &&
		   weeWXAppCommon.GetBoolPref("radarforecast", weeWXApp.radarforecast_default) == weeWXApp.RadarOnHomeScreen)
			tabTitles = new String[]{weeWXApp.getAndroidString(R.string.weather2),
			                         weeWXApp.getAndroidString(R.string.stats2),
			                         weeWXApp.getAndroidString(R.string.forecast2),
			                         weeWXApp.getAndroidString(R.string.webcam2),
			                         weeWXApp.getAndroidString(R.string.custom2)};
		else
			tabTitles = new String[]{weeWXApp.getAndroidString(R.string.weather2),
			                         weeWXApp.getAndroidString(R.string.stats2),
			                         weeWXApp.getAndroidString(R.string.radar),
			                         weeWXApp.getAndroidString(R.string.webcam2),
			                         weeWXApp.getAndroidString(R.string.custom2)};

		new TabLayoutMediator(tabLayout, mViewPager,
				((tab, position) -> tab.setText(tabTitles[position]))).attach();

		if(savedInstanceState != null)
		{
			int page = savedInstanceState.getInt("page", 0);
			mViewPager.setCurrentItem(page, false);
		}

		try
		{
			String baseURL = weeWXAppCommon.GetStringPref("BASE_URL", weeWXApp.BASE_URL_default);
			if(baseURL == null || baseURL.isBlank())
				mDrawerLayout.openDrawer(GravityCompat.START);
		} catch(Exception e) {
			weeWXAppCommon.doStackOutput(e);
		}

		settingLayout = findViewById(R.id.settingLayout);
		aboutLayout = findViewById(R.id.aboutLayout);

		settingsURL = findViewById(R.id.settings);
		customURL = findViewById(R.id.customURL);

		metric_forecasts = findViewById(R.id.metric_forecasts);
		show_indoor = findViewById(R.id.show_indoor);

		use_exact_alarm = findViewById(R.id.use_exact_alarm);
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
			weeWXAppCommon.LogMessage("New UpdateFrequency: " + UpdateFrequency);
		});

		s2 = findViewById(R.id.spinner2);
		s2.setOnItemClickListener((parent, view, position, id) ->
		{
			DayNightMode = position;
			weeWXAppCommon.LogMessage("New DayNightMode: " + DayNightMode);
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

			weeWXAppCommon.LogMessage("New widget_theme_mode: " + widget_theme_mode);
		});

		fgColour = findViewById(R.id.fg_Picker);
		bgColour = findViewById(R.id.bg_Picker);

		wifi_only = findViewById(R.id.wifi_only);

		showRadar = findViewById(R.id.showRadar);
		showForecast = findViewById(R.id.showForecast);

		fgtil = findViewById(R.id.fgTextInputLayout);
		bgtil = findViewById(R.id.bgTextInputLayout);

		int fg, bg;
		boolean wo, met, si, sr, sf, uea, sadl, nm, fdm;

		UpdateFrequency = weeWXAppCommon.GetIntPref("updateInterval", 1);
		DayNightMode = weeWXAppCommon.GetIntPref("DayNightMode", weeWXApp.DayNightMode_default);
		KeyValue.widget_theme_mode = widget_theme_mode =
				weeWXAppCommon.GetIntPref(weeWXAppCommon.WIDGET_THEME_MODE, weeWXApp.widget_theme_mode_dfault);

		fg = weeWXAppCommon.GetIntPref("fgColour", weeWXApp.fgColour_default);
		bg = weeWXAppCommon.GetIntPref("bgColour", weeWXApp.bgColour_default);

		wo = weeWXAppCommon.GetBoolPref("onlyWIFI", weeWXApp.onlyWIFI_default);
		met = weeWXAppCommon.GetBoolPref("metric", weeWXApp.metric_default);
		si = weeWXAppCommon.GetBoolPref("showIndoor", weeWXApp.showIndoor_default);
		sr = weeWXAppCommon.GetBoolPref("radarforecast", weeWXApp.radarforecast_default);

		uea = weeWXAppCommon.GetBoolPref("use_exact_alarm", weeWXApp.use_exact_alarm_default);
		sadl = KeyValue.save_app_debug_logs;
		nm = weeWXAppCommon.GetBoolPref("next_moon", weeWXApp.next_moon_default);
		fdm = weeWXAppCommon.GetBoolPref("next_moon", weeWXApp.force_dark_mode_default);

		if(savedInstanceState != null)
		{
			weeWXAppCommon.LogMessage("MainActivity.onCreate() Reading current settings that were saved in a bundle....");
			UpdateFrequency = savedInstanceState.getInt("UpdateFrequency", UpdateFrequency);
			DayNightMode = savedInstanceState.getInt("DayNightMode", DayNightMode);
			widget_theme_mode = savedInstanceState.getInt("widget_theme_mode", widget_theme_mode);

			weeWXAppCommon.LogMessage("UpdateFrequency: " + UpdateFrequency);
			weeWXAppCommon.LogMessage("DayNightMode: " + DayNightMode);
			weeWXAppCommon.LogMessage("widget_theme_mode: " + widget_theme_mode);

			fg = savedInstanceState.getInt("fg", fg);
			bg = savedInstanceState.getInt("bg", bg);

			wo = savedInstanceState.getBoolean("wo", wo);
			met = savedInstanceState.getBoolean("met", met);
			si = savedInstanceState.getBoolean("si", si);
			sr = savedInstanceState.getBoolean("sr", sr);
			uea = savedInstanceState.getBoolean("uea", uea);
			sadl = savedInstanceState.getBoolean("sadl", sadl);
			nm = savedInstanceState.getBoolean("nm", nm);
			fdm = savedInstanceState.getBoolean("fdm", fdm);
		}

		// https://github.com/Pes8/android-material-color-picker-dialog
		String hex = CPEditText.getFixedChar() + String.format("%08X", fg).toUpperCase();
		weeWXAppCommon.LogMessage("Line223 Setting fgColour to " + to_ARGB_hex(hex));
		final String fghex = hex;
		fgColour.setText(fghex);
		fgColour.setOnTouchListener((v, event) ->
		{
			if(event.getAction() == MotionEvent.ACTION_UP)
				handleTouch();

			return false;
		});

		hex = CPEditText.getFixedChar() + String.format("%08X", bg).toUpperCase();
		weeWXAppCommon.LogMessage("Line229 Setting bgColour to " + to_ARGB_hex(hex));
		final String bghex = hex;
		bgColour.setText(bghex);
		bgColour.setOnTouchListener((v, event) ->
		{
			if(event.getAction() == MotionEvent.ACTION_UP)
				handleTouch();

			return false;
		});

		wifi_only.setChecked(wo);
		metric_forecasts.setChecked(met);
		show_indoor.setChecked(si);
		use_exact_alarm.setChecked(uea);
		save_app_debug_logs.setChecked(sadl);
		next_moon.setChecked(nm);
		force_dark_mode.setChecked(fdm);

		showRadar.setChecked(sr);
		showForecast.setChecked(!sr);

		settingLayout.setVisibility(View.VISIBLE);
		aboutLayout.setVisibility(View.GONE);

		MaterialTextView tv1 = findViewById(R.id.aboutText);
		tv1.setText(HtmlCompat.fromHtml(weeWXApp.about_blurb, HtmlCompat.FROM_HTML_MODE_COMPACT));
		tv1.setMovementMethod(LinkMovementMethod.getInstance());

		b1.setOnClickListener(arg0 ->
		{
			b1.setEnabled(false);
			b2.setEnabled(false);
			closeKeyboard();

			weeWXAppCommon.LogMessage("show dialog");
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setCancelable(false);
			builder.setView(R.layout.layout_loading_dialog);
			dialog = builder.create();
			dialog.show();
			processSettings();
		});

		b2.setOnClickListener(arg0 -> checkReally());

		b3.setOnClickListener(arg0 ->
		{
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

		settingsURL.setText(weeWXAppCommon.GetStringPref("SETTINGS_URL",
				weeWXApp.SETTINGS_URL_default));
		settingsURL.setOnFocusChangeListener((v, hasFocus) ->
		{
			if(!hasFocus)
				closeKeyboard(v);
		});

		customURL.setText(weeWXAppCommon.GetStringPref("custom_url", ""));
		customURL.setOnFocusChangeListener((v, hasFocus) ->
		{
			weeWXAppCommon.LogMessage("CustomURL has a focus change event...");

			if(!hasFocus)
			{
				weeWXAppCommon.LogMessage("CustomURL lost focus change event...");
				closeKeyboard(v);
				return;
			}

			weeWXAppCommon.LogMessage("CustomURL gained focus change event...");

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
						weeWXAppCommon.LogMessage("Old scrollY: " + scrollView.getScrollY());
						int scrollY = scrollView.getScrollY() + extraPx;
						weeWXAppCommon.LogMessage("New scrollY: " + scrollY);
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

		weeWXAppCommon.LogMessage("MainActivity.onCreate() loading NotificationManager...");
		weeWXAppCommon.NotificationManager.getNotificationLiveData().observe(this, notificationObserver);

		weeWXAppCommon.LogMessage("MainActivity.onCreate() has finished...");
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
		} catch (Exception e) {
			weeWXAppCommon.doStackOutput(e);
		}
	}

	@Override
	public void onDestroy()
	{
		weeWXAppCommon.LogMessage("MainActivity.onDestroy()");
		super.onDestroy();

		weeWXAppCommon.NotificationManager.getNotificationLiveData().removeObservers(this);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		KeyValue.isVisible = true;

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
		for(int i : screen_elements)
		{
			View view = findViewById(i);
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
					v.setTextColor(KeyValue.fgColour);
					v.setHintTextColor(KeyValue.fgColour);
				}
				case TextInputEditText v ->
				{
					v.setTextColor(KeyValue.fgColour);
					v.setHintTextColor(KeyValue.fgColour);
				}
				case MaterialSwitch v ->
				{
					v.setTextColor(KeyValue.fgColour);
					v.setHintTextColor(KeyValue.fgColour);
				}
				case MaterialAutoCompleteTextView v ->
				{
					v.setTextColor(KeyValue.fgColour);
					v.setHintTextColor(KeyValue.fgColour);
				}
				case MaterialRadioButton v ->
				{
					v.setTextColor(KeyValue.fgColour);
					v.setHintTextColor(KeyValue.fgColour);
				}
				case TextView v ->
				{
					v.setTextColor(KeyValue.fgColour);
					v.setHintTextColor(KeyValue.fgColour);
				}
				default -> weeWXAppCommon.LogMessage("Uncaught view type: " + view);
			}
		}
	}

	private void updateHamburger()
	{
		if(gestureNav)
		{
			weeWXAppCommon.LogMessage("gestureNav == true, show the hamburger menu...");
			if(hamburger.getVisibility() != View.VISIBLE)
				hamburger.setVisibility(View.VISIBLE);
		} else {
			weeWXAppCommon.LogMessage("gestureNav == false, hide the hamburger menu...");
			if(hamburger.getVisibility() != View.GONE)
				hamburger.setVisibility(View.GONE);
		}
	}

	final DrawerLayout.SimpleDrawerListener handleDrawerListener = new DrawerLayout.SimpleDrawerListener()
	{
		@Override
		public void onDrawerOpened(@NonNull View drawerView)
		{
			weeWXAppCommon.LogMessage("Detected a back press in the DrawerLayout...");

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
			weeWXAppCommon.LogMessage("handleOnBackPressed()");
			handleBack();
		}
	};

	void handleBack()
	{
		weeWXAppCommon.LogMessage("Line 694 handleBack() Detected an application back press...");
		View focus = getCurrentFocus();
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if(focus != null && imm != null && imm.isAcceptingText())
		{
			weeWXAppCommon.LogMessage("Line 699 handleBack() Let's hide the on screen keyboard and clearFocus()...");
			closeKeyboard(focus, imm);
			return;
		}

		if(mDrawerLayout.isDrawerOpen(GravityCompat.START))
		{
			weeWXAppCommon.LogMessage("Line 713 handleBack() Let's shut the drawer...");
			closeDrawer();
			return;
		}

		if(mViewPager.getCurrentItem() > 0)
		{
			weeWXAppCommon.LogMessage("Line 708 handleBack() Cycle through tabs until we hit tab 0");
			mViewPager.post(() -> mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1));
			return;
		}

		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
		{
			weeWXAppCommon.LogMessage("Line 716 handleBack() Let's end now... SDK < TIRAMISU");
			obpc.setEnabled(false);
			finish();
		} else {
			weeWXAppCommon.LogMessage("Line 720 handleBack() SDK >= TIRAMISU... Let the system do it's thing...");
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

		weeWXAppCommon.LogMessage("MainActivity.onSaveInstanceState() Stashing current settings into a bundle....");
		outState.putInt("page", mViewPager.getCurrentItem());
		outState.putInt("UpdateFrequency", UpdateFrequency);
		outState.putInt("DayNightMode", DayNightMode);
		outState.putInt("widget_theme_mode", widget_theme_mode);

		weeWXAppCommon.LogMessage("UpdateFrequency: " + UpdateFrequency);
		weeWXAppCommon.LogMessage("DayNightMode: " + DayNightMode);
		weeWXAppCommon.LogMessage("widget_theme_mode: " + widget_theme_mode);

		Editable edit = fgColour.getText();
		if(edit != null && edit.length() > 0)
		{
			int fg = parseHexToColour(edit.toString());
			outState.putInt("fg", fg);
		} else {
			outState.putInt("fg", weeWXApp.fgColour_default);
		}

		edit = bgColour.getText();
		if(edit != null && edit.length() > 0)
		{
			int bg = parseHexToColour(edit.toString());
			outState.putInt("bg", bg);
		} else {
			outState.putInt("bg", weeWXApp.bgColour_default);
		}

		outState.putBoolean("wo", wifi_only.isChecked());
		outState.putBoolean("met", metric_forecasts.isChecked());
		outState.putBoolean("si", show_indoor.isChecked());
		outState.putBoolean("sr", showRadar.isChecked());
		outState.putBoolean("uea", use_exact_alarm.isChecked());
		outState.putBoolean("sadl", save_app_debug_logs.isChecked());
		outState.putBoolean("nm", next_moon.isChecked());
		outState.putBoolean("fdm", force_dark_mode.isChecked());
	}

	private void setStrings()
	{
		int disabled = weeWXApp.getColours().LightGray;
		if(KeyValue.theme == R.style.AppTheme_weeWXApp_Dark_Common)
			disabled = weeWXApp.getColours().DarkGray;

		strokeColors = new ColorStateList(new int[][]{new int[]{android.R.attr.state_enabled},      // default enabled
		                                              new int[]{-android.R.attr.state_enabled},     // disabled
		                                              new int[]{android.R.attr.state_focused},      // focused
		                                              new int[]{-android.R.attr.state_focused},     // not focused
		}, new int[]{KeyValue.fgColour,  // default
		             disabled,  // disabled
		             KeyValue.fgColour,  // focused
		             KeyValue.fgColour   // unfocused
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
				view.setBackgroundColor(KeyValue.bgColour);
				view.setTextColor(KeyValue.fgColour);
				return view;
			}

			@Override
			public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent)
			{
				MaterialTextView view = (MaterialTextView)super.getDropDownView(position, convertView, parent);
				view.setBackgroundColor(KeyValue.bgColour);
				view.setTextColor(KeyValue.fgColour);
				return view;
			}
		};
	}

	private void updateDropDowns()
	{
		ArrayAdapter<String> adapter1 = newArrayAdapter(R.layout.spinner_layout, weeWXApp.updateOptions);
		ArrayAdapter<String> adapter2 = newArrayAdapter(R.layout.spinner_layout, weeWXApp.themeOptions);
		ArrayAdapter<String> adapter3 = newArrayAdapter(R.layout.spinner_layout, weeWXApp.widgetThemeOptions);

		adapter1.notifyDataSetChanged();
		adapter2.notifyDataSetChanged();
		adapter3.notifyDataSetChanged();

		final int uf = UpdateFrequency;
		final int dnm = DayNightMode;
		final int wtm = widget_theme_mode;

		runOnUiThread(() ->
		{
			weeWXAppCommon.LogMessage("UpdateFrequency: " + uf);
			weeWXAppCommon.LogMessage("DayNightMode: " + dnm);
			weeWXAppCommon.LogMessage("widget_theme_mode: " + wtm);

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
			weeWXAppCommon.LogMessage("Line 886 closeKeyboard() Let's hide the on screen keyboard...");
			imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
			focus.clearFocus();
		}
	}

	void closeDrawer()
	{
		if(mDrawerLayout.isDrawerOpen(GravityCompat.START))
		{
			weeWXAppCommon.LogMessage("Let's shut the drawer...");
			mDrawerLayout.closeDrawer(GravityCompat.START);
		}
	}

	private void showUpdateAvailable()
	{
		final AlertDialog.Builder d = new AlertDialog.Builder(this);
		d.setTitle(weeWXApp.getAndroidString(R.string.app_name));
		d.setMessage(weeWXApp.getAndroidString(R.string.inigo_needs_updating));
		d.setPositiveButton(weeWXApp.getAndroidString(R.string.ok), null);
		d.setIcon(R.mipmap.ic_launcher_foreground);
		d.show();
	}

	private void checkReally()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(weeWXApp.getAndroidString(R.string.remove_all_data)).setCancelable(false)
		.setPositiveButton(weeWXApp.getAndroidString(R.string.ok), (dialog_interface, i) ->
		{
			weeWXAppCommon.LogMessage("Reset any widgets...");
			WidgetProvider.resetAppWidget();

			weeWXAppCommon.LogMessage("trash all data and exit cleanly...");
			((ActivityManager)getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
		}).setNegativeButton(weeWXApp.getAndroidString(R.string.no), (dialog_interface, i) -> dialog_interface.cancel());

		builder.create().show();
	}

	private void resetScreen()
	{
		weeWXAppCommon.LogMessage("Do some stuff here...");

		closeKeyboard();
		closeDrawer();

		scrollView.scrollTo(0, 0);
		dialog.dismiss();
	}

	private void processSettings()
	{
		weeWXAppCommon.LogMessage("MainActivity.java processSettings() running the background updates...");

		if(use_exact_alarm.isChecked() && !UpdateCheck.canSetExact(this))
		{
			weeWXAppCommon.LogMessage("Need to prompt user to allow exact alarms...");

			UpdateCheck.promptForExact(this);

			//resetScreen();
			b1.setEnabled(true);
			b2.setEnabled(true);
			b3.setEnabled(true);
			b4.setEnabled(true);

			dialog.dismiss();

			return;
		}

		long current_time = weeWXAppCommon.getCurrTime();

		UpdateCheck.cancelAlarm();

		UpdateCheck.setNextAlarm();

		//UpdateCheck.runInTheBackground(false, false);

		if(backgroundTask != null && !backgroundTask.isDone())
		{
			if(bgStart + 30 > current_time)
			{
				weeWXAppCommon.LogMessage("MainActivity.java processSettings() executor is still running and is less than 30s old (" +
				                          (current_time - bgStart) + "s), skipping...",	true);
				return;
			}

			weeWXAppCommon.LogMessage("MainActivity.java processSettings() Cancelling the current background executor...");
			backgroundTask.cancel(true);
			backgroundTask = null;
		}

		bgStart = current_time;

		backgroundTask = executor.submit(() ->
		{
			weeWXAppCommon.LogMessage("MainActivity.java bg started...");

			String tmpStr, errorStr = "";
			boolean validURL;
			boolean validURL1;
			boolean validURL2;
			boolean validURL3;
			boolean validURL5;

			String baseURL = "", radtype = "", radarURL = "", forecastURL = "", webcamURL = "",
					CustomURL = "", appCustomURL, fctype = "", bomtown = "", metierev;

			String settings_url = settingsURL.getText() != null ? settingsURL.getText().toString().strip() : "";
			if(settings_url.isBlank() || settings_url.equals(weeWXApp.SETTINGS_URL_default))
			{
				runOnUiThread(() ->
				{
					b1.setEnabled(true);
					b2.setEnabled(true);
					dialog.dismiss();
					new AlertDialog.Builder(this)
							.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_settings))
							.setMessage(weeWXApp.getAndroidString(R.string.url_was_default_or_empty))
							.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
									(dialog, which) -> {}).show();
				});

				bgStart = 0;
				return;
			}

			try
			{
				String settingsData = weeWXAppCommon.downloadSettings(settings_url).strip();
				if(settingsData == null || settingsData.isBlank())
				{
					validURL = false;
				} else {
					String[] bits = settingsData.split("\\n");
					if(bits.length > 1)
					{
						for(String bit: bits)
						{
							if(bit.isBlank() || bit.startsWith("#") || !bit.contains("="))
								continue;

							String[] mb = bit.split("=", 2);
							mb[0] = mb[0].toLowerCase(Locale.ENGLISH).strip();
							mb[1] = mb[1].strip();

							weeWXAppCommon.LogMessage("mb[0]: " + mb[0]);
							weeWXAppCommon.LogMessage("mb[1]: " + mb[1]);

							switch(mb[0])
							{
								case "data" -> baseURL = mb[1];
								case "radar" -> radarURL = mb[1];
								case "radtype" -> radtype = mb[1].toLowerCase(Locale.ENGLISH);
								case "forecast" -> forecastURL = mb[1];
								case "fctype" -> fctype = mb[1].toLowerCase(Locale.ENGLISH);
								case "webcam" -> webcamURL = mb[1];
								case "custom" -> CustomURL = mb[1];
							}
						}

						validURL = true;
					} else {
						validURL = false;
					}
				}
			} catch(Exception e) {
				weeWXAppCommon.doStackOutput(e);
				errorStr = e.toString();
				validURL = false;
			}

			if(!validURL)
			{
				String finalErrorStr = errorStr;
				runOnUiThread(() ->
				{
					b1.setEnabled(true);
					b2.setEnabled(true);
					dialog.dismiss();
					new AlertDialog.Builder(this)
							.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_settings))
							.setMessage(finalErrorStr)
							.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
									(dialog, which) -> {}).show();
				});

				bgStart = 0;
				return;
			}

			weeWXAppCommon.LogMessage("baseURL " + baseURL);

			if(baseURL == null || baseURL.isBlank())
			{
				runOnUiThread(() ->
				{
					b1.setEnabled(true);
					b2.setEnabled(true);
					dialog.dismiss();
					new AlertDialog.Builder(this)
							.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_data_txt))
							.setMessage(weeWXApp.getAndroidString(R.string.data_url_was_blank))
							.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
									(dialog, which) -> {}).show();
				});

				bgStart = 0;
				return;
			}

			try
			{
				weeWXAppCommon.LogMessage("Checking baseURL: " + baseURL);
				validURL1 = weeWXAppCommon.reallyGetWeather(baseURL);
			} catch(Exception e) {
				weeWXAppCommon.doStackOutput(e);
				errorStr = e.toString();
				validURL1 = false;
			}

			if(!validURL1)
			{
				String finalErrorStr = errorStr;
				runOnUiThread(() ->
				{
					b1.setEnabled(true);
					b2.setEnabled(true);
					dialog.dismiss();
					new AlertDialog.Builder(this)
							.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_radar_image))
							.setMessage(finalErrorStr)
							.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
									(dialog, which) -> {}).show();
				});

				bgStart = 0;
				return;
			}

			if(!radarURL.isBlank())
			{
				try
				{
					if(radtype.equals("image"))
						validURL2 = weeWXAppCommon.loadOrDownloadImage(radarURL, weeWXApp.radarFilename) != null;
					else if(radtype.equals("webpage"))
						validURL2 = weeWXAppCommon.checkURL(radarURL);
					else
						validURL2 = false;
				} catch(Exception e) {
					weeWXAppCommon.doStackOutput(e);
					errorStr = e.toString();
					validURL2 = false;
				}

				if(!validURL2)
				{
					String finalErrorStr = errorStr;
					runOnUiThread(() ->
					{
						b1.setEnabled(true);
						b2.setEnabled(true);
						dialog.dismiss();
						new AlertDialog.Builder(this)
								.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_radar_image))
								.setMessage(finalErrorStr)
								.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
										(dialog, which) -> {}).show();
					});

					bgStart = 0;
					return;
				}
			}

			if(!forecastURL.isBlank())
			{
				try
				{
					switch(fctype.toLowerCase(Locale.ENGLISH))
					{
/*
						case "metservice.com2" ->
						{
							weeWXAppCommon.LogMessage("fctype: " + fctype);
							weeWXAppCommon.LogMessage("forecast:" + forecastURL);
							String metService = WebViewPreloader.getHTML(forecastURL, 10_000);
							if(metService != null && metService.length() > 1024)
							{
								String pretty = Jsoup.parse(metService).outputSettings(
										new Document.OutputSettings().indentAmount(2).prettyPrint(true)
								).outerHtml();

								CustomDebug.writeDebug("weeWX", "metservice.com.7day.html", pretty);
								weeWXAppCommon.LogMessage("wrote content to metservice.com.7day.html");
							}

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
*/
						case "yahoo" ->
						{
							weeWXAppCommon.LogMessage("forecast: " + forecastURL);
							weeWXAppCommon.LogMessage("fctype: " + fctype);
							if(!forecastURL.startsWith("http"))
							{
								String finalErrorStr = "Yahoo API recently changed, you need to update your settings.";
								runOnUiThread(() ->
								{
									b1.setEnabled(true);
									b2.setEnabled(true);
									dialog.dismiss();
									new AlertDialog.Builder(this)
											.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_or_download))
											.setMessage(finalErrorStr)
											.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
													(dialog, which) -> {}).show();
								});

								bgStart = 0;
								return;
							}
						}
						case "weatherzone" ->
						{
							forecastURL = "https://rss.weatherzone.com.au/?u=12994-1285&lt=aploc&lc=" + forecastURL + "&obs=0&fc=1&warn=0";
							weeWXAppCommon.LogMessage("forecast: " + forecastURL);
							weeWXAppCommon.LogMessage("fctype: " + fctype);
						}
						case "yr.no", "met.no", "weather.gc.ca", "weather.gc.ca-fr", "metoffice.gov.uk",
						     "bom2", "aemet.es", "dwd.de", "tempoitalia.it" ->
						{
							weeWXAppCommon.LogMessage("forecast: " + forecastURL);
							weeWXAppCommon.LogMessage("fctype: " + fctype);
						}
						case "wmo.int" ->
						{
							if(!forecastURL.startsWith("http"))
								forecastURL = "https://worldweather.wmo.int/en/json/" + forecastURL.strip() + "_en.xml";
							weeWXAppCommon.LogMessage("forecast: " + forecastURL);
							weeWXAppCommon.LogMessage("fctype: " + fctype);
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
							weeWXAppCommon.LogMessage("forecast: " + forecastURL);
							weeWXAppCommon.LogMessage("fctype: " + fctype);
						}
						case "bom3" ->
						{
							forecastURL = "https://api.weather.bom.gov.au/v1/locations/" + forecastURL.strip() + "/forecasts/daily";
							weeWXAppCommon.LogMessage("forecast: " + forecastURL);
							weeWXAppCommon.LogMessage("fctype: " + fctype);
						}
						case "metservice.com" ->
						{
							forecastURL = "https://www.metservice.com/publicData/localForecast" + forecastURL;
							weeWXAppCommon.LogMessage("forecast: " + forecastURL);
							weeWXAppCommon.LogMessage("fctype: " + fctype);
						}
						case "openweathermap.org" ->
						{
							if(metric_forecasts.isChecked())
								forecastURL += "&units=metric";
							else
								forecastURL += "&units=imperial";
							forecastURL += "&lang=" + Locale.getDefault().getLanguage();
							weeWXAppCommon.LogMessage("forecast: " + forecastURL);
							weeWXAppCommon.LogMessage("fctype: " + fctype);
						}
						case "weather.com" ->
						{
							forecastURL = "https://api.weather.com/v3/wx/forecast/daily/5day?geocode=" + forecastURL + "&format=json&apiKey" +
							    "=71f92ea9dd2f4790b92ea9dd2f779061";
							if(metric_forecasts.isChecked())
								forecastURL += "&units=m";
							else
								forecastURL += "&units=e";
							forecastURL += "&language=" + Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry();
							weeWXAppCommon.LogMessage("forecast=" + forecastURL);
							weeWXAppCommon.LogMessage("fctype=" + fctype);
						}
						case "met.ie" ->
						{
							metierev = "https://prodapi.metweb.ie/location/reverse/" + forecastURL.replaceAll(",", "/");
							forecastURL = "https://prodapi.metweb.ie/weather/daily/" + forecastURL.replaceAll(",", "/") + "/10";

							tmpStr = weeWXAppCommon.GetStringPref("metierev", weeWXApp.metierev_default);
							if(tmpStr == null || tmpStr.isBlank())
							{
								metierev = weeWXAppCommon.downloadString(metierev);
								if(metierev == null)
								{
									bgStart = 0;
									return;
								}

								JSONObject jobj = new JSONObject(metierev);
								metierev = jobj.getString("city") + ", Ireland";
								weeWXAppCommon.SetStringPref("metierev", metierev);
							}
							weeWXAppCommon.LogMessage("forecast: " + forecastURL);
							weeWXAppCommon.LogMessage("fctype: " + fctype);
							weeWXAppCommon.LogMessage("metierev: " + metierev);
						}
						default ->
						{
							String finalErrorStr = String.format(weeWXApp.getAndroidString(R.string.forecast_type_is_invalid), fctype);
							runOnUiThread(() ->
							{
								b1.setEnabled(true);
								b2.setEnabled(true);
								dialog.dismiss();
								new AlertDialog.Builder(this)
										.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_forecast))
										.setMessage(finalErrorStr)
										.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again), (dialog, which) ->
										{
										}).show();
							});

							bgStart = 0;
							return;
						}
					}
				} catch(Exception e) {
					weeWXAppCommon.doStackOutput(e);
					errorStr = e.toString();
				}
			}

			if(!forecastURL.isBlank())
			{
				weeWXAppCommon.LogMessage("forecast checking: " + forecastURL);

				tmpStr = weeWXAppCommon.reallyGetForecast(forecastURL);
				weeWXAppCommon.LogMessage("tmpStr: " + tmpStr);
				validURL3 = tmpStr != null && !tmpStr.isBlank();

				if(!validURL3 && errorStr != null && !errorStr.isBlank())
				{
					String finalErrorStr = errorStr;
					runOnUiThread(() ->
					{
						b1.setEnabled(true);
						b2.setEnabled(true);
						dialog.dismiss();
						new AlertDialog.Builder(this)
								.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_forecast))
								.setMessage(finalErrorStr)
								.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
										(dialog, which) -> {}).show();
					});

					bgStart = 0;
					return;
				}
			}

			if(!webcamURL.isBlank())
			{
				weeWXAppCommon.LogMessage("checking: " + webcamURL);

				try
				{
					Bitmap bm = null;

					try
					{
						bm = weeWXAppCommon.loadOrDownloadImage(webcamURL, weeWXApp.webcamFilename);
					} catch(Exception e) {
						weeWXAppCommon.doStackOutput(e);
					}

					if(bm == null)
					{
						weeWXAppCommon.LogMessage("bm is null!");

						runOnUiThread(() ->
						{
							b1.setEnabled(true);
							b2.setEnabled(true);
							dialog.dismiss();
							new AlertDialog.Builder(this)
									.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_webcam_url))
									.setMessage(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_webcam_url))
									.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
											(dialog, which) -> {}).show();
						});

						bgStart = 0;
						return;
					}
				} catch(Exception e) {
					weeWXAppCommon.doStackOutput(e);
				}
			}

			appCustomURL = customURL.getText() != null ? customURL.getText().toString().strip() : "";
			if(appCustomURL.isBlank())
			{
				if(!CustomURL.isBlank() && !CustomURL.equals(weeWXApp.CustomURL_default))
				{
					try
					{
						if(weeWXAppCommon.checkURL(CustomURL))
						{
							validURL5 = true;
						} else {
							weeWXAppCommon.RemovePref("custom_url");
							validURL5 = false;
						}
					} catch(Exception e) {
						weeWXAppCommon.doStackOutput(e);
						errorStr = e.toString();
						validURL5 = false;
					}

					if(!validURL5)
					{
						String finalErrorStr = errorStr;
						runOnUiThread(() ->
						{
							b1.setEnabled(true);
							b2.setEnabled(true);
							dialog.dismiss();
							new AlertDialog.Builder(this)
									.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_custom_url))
									.setMessage(finalErrorStr)
									.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
											(dialog, which) -> {}).show();
						});

						bgStart = 0;
						return;
					}
				}
			} else {
				try
				{
					validURL5 = weeWXAppCommon.checkURL(appCustomURL);
				} catch(Exception e) {
					weeWXAppCommon.doStackOutput(e);
					errorStr = e.toString();
					validURL5 = false;
				}

				if(!validURL5)
				{
					String finalErrorStr = errorStr;
					runOnUiThread(() ->
					{
						b1.setEnabled(true);
						b2.setEnabled(true);
						dialog.dismiss();
						new AlertDialog.Builder(this)
								.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_custom_url))
								.setMessage(finalErrorStr)
								.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
										(dialog, which) -> {}).show();
					});

					bgStart = 0;
					return;
				}
			}

			weeWXAppCommon.SetStringPref("SETTINGS_URL", settingsURL.getText().toString());
			weeWXAppCommon.SetIntPref("updateInterval", UpdateFrequency);
			weeWXAppCommon.SetStringPref("BASE_URL", baseURL);

			if(forecastURL == null || forecastURL.isBlank())
			{
				weeWXAppCommon.RemovePref("rssCheck");
				weeWXAppCommon.RemovePref("forecastData");
				weeWXAppCommon.RemovePref("fctype");
			} else {
				weeWXAppCommon.SetStringPref("FORECAST_URL", forecastURL);
				weeWXAppCommon.SetStringPref("fctype", fctype);
			}

			if(radarURL == null || radarURL.isBlank())
			{
				weeWXAppCommon.RemovePref("radtype");
				weeWXAppCommon.RemovePref("RADAR_URL");
			} else {
				weeWXAppCommon.SetStringPref("radtype", radtype);
				weeWXAppCommon.SetStringPref("RADAR_URL", radarURL);
			}

			if(webcamURL == null || webcamURL.isBlank())
				weeWXAppCommon.RemovePref("WEBCAM_URL");
			else
				weeWXAppCommon.SetStringPref("WEBCAM_URL", webcamURL);

			if(CustomURL == null || CustomURL.isBlank())
				weeWXAppCommon.RemovePref("CUSTOM_URL");
			else
				weeWXAppCommon.SetStringPref("CUSTOM_URL", CustomURL);

			if(appCustomURL == null || appCustomURL.isBlank())
				weeWXAppCommon.RemovePref("custom_url");
			else
				weeWXAppCommon.SetStringPref("custom_url", appCustomURL);

			KeyValue.save_app_debug_logs = save_app_debug_logs.isChecked();

			weeWXAppCommon.SetBoolPref("metric", metric_forecasts.isChecked());
			weeWXAppCommon.SetBoolPref("showIndoor", show_indoor.isChecked());
			weeWXAppCommon.SetIntPref("DayNightMode", DayNightMode);
			weeWXAppCommon.SetBoolPref("onlyWIFI", wifi_only.isChecked());
			weeWXAppCommon.SetBoolPref("radarforecast", showRadar.isChecked());
			weeWXAppCommon.SetBoolPref("use_exact_alarm", use_exact_alarm.isChecked());
			weeWXAppCommon.SetBoolPref("save_app_debug_logs", KeyValue.save_app_debug_logs);
			weeWXAppCommon.SetBoolPref("next_moon", next_moon.isChecked());
			weeWXAppCommon.SetBoolPref("force_dark_mode", force_dark_mode.isChecked());

			weeWXAppCommon.SetIntPref(weeWXAppCommon.WIDGET_THEME_MODE, widget_theme_mode);
			KeyValue.widget_theme_mode = widget_theme_mode;

			Editable edit = fgColour.getText();
			if(edit != null && edit.length() > 0)
			{
				int fg = parseHexToColour(edit.toString());
				weeWXAppCommon.SetIntPref("fgColour", fg);
				weeWXAppCommon.LogMessage("Saved widget fg colour: " + to_ARGB_hex(fg));
			} else {
				weeWXAppCommon.RemovePref("fgColour");
			}

			edit = bgColour.getText();
			if(edit != null && edit.length() > 0)
			{
				int bg = parseHexToColour(edit.toString());
				weeWXAppCommon.SetIntPref("bgColour", bg);
				weeWXAppCommon.LogMessage("Saved widget bg colour: " + to_ARGB_hex(bg));
			} else {
				weeWXAppCommon.RemovePref("bgColour");
			}

			weeWXAppCommon.LogMessage("Refresh widgets if at least one exists...");
			WidgetProvider.updateAppWidget();

			weeWXAppCommon.LogMessage("Restart the alarm...");

			runOnUiThread(() ->
			{
				resetScreen();

				weeWXAppCommon.LogMessage("Apply the new theme");
				weeWXApp.applyTheme(false);
				setTheme(KeyValue.theme);
				WidgetProvider.updateAppWidget();

				runDelayed(500L, () ->
				{
					weeWXAppCommon.LogMessage("Recreate the activity...");
					recreate();

					weeWXAppCommon.LogMessage("Make sure the hamburger and dropdowns etc are remade...");

					setStrings();

					updateHamburger();
					updateDropDowns();
					updateColours();

					bgStart = 0;
				});
			});
		});
	}

	static void runDelayed(long delayMs, Runnable task)
	{
		new Handler(Looper.getMainLooper()).postDelayed(task, delayMs);
	}

	private final Observer<String> notificationObserver = str ->
	{
		weeWXAppCommon.LogMessage("notificationObserver: " + str);

		if(str.equals(weeWXAppCommon.INIGO_INTENT))
			showUpdateAvailable();
	};

	public boolean isViewPagerNull()
	{
		return mViewPager == null;
	}

	public void setUserInputPager(boolean b)
	{
		weeWXAppCommon.LogMessage("MainActivity.setUserInputPager()");
		if(isViewPagerNull())
			return;

		mViewPager.post(() -> mViewPager.setUserInputEnabled(b));
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
}