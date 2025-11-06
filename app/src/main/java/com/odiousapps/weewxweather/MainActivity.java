package com.odiousapps.weewxweather;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import com.github.evilbunny2008.colourpicker.CPEditText;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;

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
		"ConstantConditions"})
public class MainActivity extends FragmentActivity
{
	private TabLayout tabLayout;
	private DrawerLayout mDrawerLayout;
	private OnBackInvokedCallback backCallback;
	private TextInputLayout fgtil, bgtil;
	private TextInputEditText settingsURL, customURL;
	private CPEditText fgColour, bgColour;
	private MaterialButton b1, b2, b3, b4;
	private AutoCompleteTextView s1, s2, s3;
	private MaterialSwitch wifi_only, use_icons, show_indoor, metric_forecasts;
	private MaterialRadioButton showRadar, showForecast;
	private static ViewPager2 mViewPager;

	private AlertDialog dialog;

	private LinearLayout settingLayout, aboutLayout;

	private ScrollView scrollView;

	private SectionsStateAdapter mSectionsPagerAdapter;

	private static int UpdateFrequency, DayNightMode, widget_theme_mode;

	private String[] updateOptions, themeOptions, widgetThemeOptions;

	private int appInitialLeft, appInitialRight, appInitialTop, appInitialBottom;
	private int cdInitialLeft, cdInitialRight, cdInitialTop, cdInitialBottom;
	private int dlInitialLeft, dlInitialRight, dlInitialTop, dlInitialBottom;
	private int rvInitialLeft, rvInitialRight, rvInitialTop, rvInitialBottom;

	private ImageButton hamburger;
	private boolean gestureNav = false;

	private Thread mainactivityThread;
	private long maStart;

	private int extraPx;

	private static final int[] screen_elements = new int[]
	{
		R.id.til1,
		R.id.settings,
		R.id.show_indoor,
		R.id.metric_forecasts,
		R.id.wifi_only,
		R.id.use_icons,
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
		R.id.showRadar,
		R.id.showForecast,
		R.id.til5,
		R.id.customURL
	};

	ColorStateList strokeColors;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		setTheme(KeyValue.theme);

		super.onCreate(savedInstanceState);

		Common.LogMessage("MainActivity.ocCreate() started...");

		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

		Common.LogMessage("smallestScreenWidthDp: " + weeWXApp.smallestScreenWidth());
		Common.LogMessage("minWidth=" + weeWXApp.getWidth() +
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
			WindowInsetsControllerCompat insetsController =
					WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
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

		setupBackHandling();

		myLinearLayout cd = findViewById(R.id.custom_drawer);
		cd.setBackgroundColor(KeyValue.bgColour);
		cd.setOnTouchedListener((v) ->
		{
			Common.LogMessage("cd.TouchedListener()");
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
			} catch(Exception ignored) {}
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
		if(Common.GetBoolPref("radarforecast", true))
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
			String baseURL = Common.GetStringPref("BASE_URL", "");
			if(baseURL == null || baseURL.isBlank())
				mDrawerLayout.openDrawer(GravityCompat.START);
		} catch(Exception e) {
			Common.doStackOutput(e);
		}

		settingLayout = findViewById(R.id.settingLayout);
		aboutLayout = findViewById(R.id.aboutLayout);

		settingsURL = findViewById(R.id.settings);
		customURL = findViewById(R.id.customURL);

		metric_forecasts = findViewById(R.id.metric_forecasts);
		show_indoor = findViewById(R.id.show_indoor);

		b1 = findViewById(R.id.saveButton);
		b2 = findViewById(R.id.deleteData);
		b3 = findViewById(R.id.aboutButton);
		b4 = findViewById(R.id.settingsButton);

		s1 = findViewById(R.id.spinner1);
		s1.setOnItemClickListener((parent, view, position, id) ->
		{
			UpdateFrequency = position;
			Common.LogMessage("New UpdateFrequency: " + UpdateFrequency);
		});

		s2 = findViewById(R.id.spinner2);
		s2.setOnItemClickListener((parent, view, position, id) ->
		{
			DayNightMode = position;
			Common.LogMessage("New DayNightMode: " + DayNightMode);
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

			Common.LogMessage("New widget_theme_mode: " + widget_theme_mode);
		});

		fgColour = findViewById(R.id.fg_Picker);
		bgColour = findViewById(R.id.bg_Picker);

		wifi_only = findViewById(R.id.wifi_only);
		use_icons = findViewById(R.id.use_icons);

		showRadar = findViewById(R.id.showRadar);
		showForecast = findViewById(R.id.showForecast);

		fgtil = findViewById(R.id.fgTextInputLayout);
		bgtil = findViewById(R.id.bgTextInputLayout);

		int fg, bg;
		boolean wo, ui, met, si, sr, sf;

		UpdateFrequency = Common.GetIntPref("updateInterval", 1);
		DayNightMode = Common.GetIntPref("DayNightMode", 2);
		KeyValue.widget_theme_mode = widget_theme_mode = Common.GetIntPref(Common.WIDGET_THEME_MODE, 0);

		fg = Common.GetIntPref("fgColour", 0xFFFFFFFF);
		bg = Common.GetIntPref("bgColour", 0x00000000);

		wo = Common.GetBoolPref("onlyWIFI", false);
		ui = Common.GetBoolPref("use_icons", false);
		met = Common.GetBoolPref("metric", true);
		si = Common.GetBoolPref("showIndoor", false);
		sr = Common.GetBoolPref("radarforecast", true);

		if(savedInstanceState != null)
		{
			Common.LogMessage("MainActivity.onCreate() Reading current settings that were saved in a bundle....");
			UpdateFrequency = savedInstanceState.getInt("UpdateFrequency", UpdateFrequency);
			DayNightMode = savedInstanceState.getInt("DayNightMode", DayNightMode);
			widget_theme_mode = savedInstanceState.getInt("widget_theme_mode", widget_theme_mode);

			Common.LogMessage("UpdateFrequency: " + UpdateFrequency);
			Common.LogMessage("DayNightMode: " + DayNightMode);
			Common.LogMessage("widget_theme_mode: " + widget_theme_mode);

			fg = savedInstanceState.getInt("fg", fg);
			bg = savedInstanceState.getInt("bg", bg);

			wo = savedInstanceState.getBoolean("wo", wo);
			ui = savedInstanceState.getBoolean("ui", ui);
			met = savedInstanceState.getBoolean("met", met);
			si = savedInstanceState.getBoolean("si", si);
			sr = savedInstanceState.getBoolean("sr", sr);
		}

		// https://github.com/Pes8/android-material-color-picker-dialog
		String hex = CPEditText.getFixedChar() + String.format("%08X", fg).toUpperCase();
		Common.LogMessage("Line223 Setting fgColour to "+ to_ARGB_hex(hex));
		final String fghex = hex;
		fgColour.setText(fghex);
		fgColour.setOnTouchListener((v, event) ->
		{
			if(event.getAction() == MotionEvent.ACTION_UP)
				handleTouch();

			return false;
		});

		hex = CPEditText.getFixedChar() + String.format("%08X", bg).toUpperCase();
		Common.LogMessage("Line229 Setting bgColour to "+ to_ARGB_hex(hex));
		final String bghex = hex;
		bgColour.setText(bghex);
		bgColour.setOnTouchListener((v, event) ->
		{
			if(event.getAction() == MotionEvent.ACTION_UP)
				handleTouch();

			return false;
		});

		wifi_only.setChecked(wo);
		use_icons.setChecked(ui);
		metric_forecasts.setChecked(met);
		show_indoor.setChecked(si);

		showRadar.setChecked(sr);
		showForecast.setChecked(!sr);

		settingLayout.setVisibility(View.VISIBLE);
		aboutLayout.setVisibility(View.GONE);

		MaterialTextView tv1 = findViewById(R.id.aboutText);
		tv1.setText(HtmlCompat.fromHtml(Common.about_blurb, HtmlCompat.FROM_HTML_MODE_COMPACT));
		tv1.setMovementMethod(LinkMovementMethod.getInstance());

		b1.setOnClickListener(arg0 ->
		{
			b1.setEnabled(false);
			b2.setEnabled(false);
			closeKeyboard();

			Common.LogMessage("show dialog");
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

		settingsURL.setText(Common.GetStringPref("SETTINGS_URL",
				"https://example.com/weewx/inigo-settings.txt"));
		settingsURL.setOnFocusChangeListener((v, hasFocus) ->
		{
			if(!hasFocus)
				closeKeyboard(v);
		});

		customURL.setText(Common.GetStringPref("custom_url", ""));
		customURL.setOnFocusChangeListener((v, hasFocus) ->
		{
			Common.LogMessage("CustomURL has a focus change event...");

			if(!hasFocus)
			{
				Common.LogMessage("CustomURL lost focus change event...");
				closeKeyboard(v);
				return;
			}

			Common.LogMessage("CustomURL gained focus change event...");

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
						Common.LogMessage("Old scrollY: " + scrollView.getScrollY());
						int scrollY = scrollView.getScrollY() + extraPx;
						Common.LogMessage("New scrollY: " + scrollY);
						scrollView.smoothScrollTo(0, Math.max(scrollY, 0));
					}, 200);
				}
			};

			root.getViewTreeObserver().addOnGlobalLayoutListener(listener);
		});

		enableEdgeToEdge(window);

		setStrings();
		updateHamburger();
		WidgetProvider.updateAppWidget();

		Common.NotificationManager.getNotificationLiveData().observe(this, notificationObserver);

		Common.LogMessage("MainActivity.onCreate() has finished...");
		Common.setAlarm("MainActivity.onCreate()");
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
			Common.doStackOutput(e);
		}
	}

	@Override
	public void onDestroy()
	{
		Common.LogMessage("MainActivity.onDestroy()");
		super.onDestroy();

		Common.NotificationManager.getNotificationLiveData().removeObserver(notificationObserver);
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
				case AutoCompleteTextView v ->
				{
					v.setTextColor(KeyValue.fgColour);
					v.setHintTextColor(KeyValue.fgColour);
				}
				case MaterialRadioButton v ->
				{
					v.setTextColor(KeyValue.fgColour);
					v.setHintTextColor(KeyValue.fgColour);
				}
				default -> Common.LogMessage("Uncaught view type: " + view);
			}
		}

	}

	private void updateHamburger()
	{
		if(gestureNav)
		{
			Common.LogMessage("gestureNav == true, show the hamburger menu...");
			if(hamburger.getVisibility() != View.VISIBLE)
				hamburger.setVisibility(View.VISIBLE);
		} else {
			Common.LogMessage("gestureNav == false, hide the hamburger menu...");
			if(hamburger.getVisibility() != View.GONE)
				hamburger.setVisibility(View.GONE);
		}
	}

	final DrawerLayout.SimpleDrawerListener handleDrawerListener = new DrawerLayout.SimpleDrawerListener()
	{
		@Override
		public void onDrawerOpened(@NonNull View drawerView)
		{
			Common.LogMessage("Detected a back press in the DrawerLayout...");

			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			View focus = getCurrentFocus();
			if(imm != null && focus != null && imm.isAcceptingText())
				closeKeyboard(drawerView, imm);
		}
	};

	private void setupBackHandling()
	{
		// Legacy back handling for Android < 13
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
		{
			Common.LogMessage("setupBackHandling() setting getOnBackPressedDispatcher() SDK < TIRAMISU");
			getOnBackPressedDispatcher().addCallback(this, obpc);
		} else {
			// Android 13+ predictive back gestures
			// Only intercept the back if keyboard is visible or drawer is open
			Common.LogMessage("setupBackHandling() setting getOnBackInvokedDispatcher() SDK >= TIRAMISU");
			getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
					OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
		}
	}

	final OnBackPressedCallback obpc = new OnBackPressedCallback(true)
	{
		@Override
		public void handleOnBackPressed()
		{
			Common.LogMessage("handleOnBackPressed()");
			handleBack();
		}
	};

	private void handleBack()
	{
		Common.LogMessage("Line 694 handleBack() Detected an application back press...");
		View focus = getCurrentFocus();
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if(focus != null && imm != null && imm.isAcceptingText())
		{
			Common.LogMessage("Line 699 handleBack() Let's hide the on screen keyboard and clearFocus()...");
			closeKeyboard(focus, imm);
			return;
		}

		if(mDrawerLayout.isDrawerOpen(GravityCompat.START))
		{
			Common.LogMessage("Line 713 handleBack() Let's shut the drawer...");
			closeDrawer();
			return;
		}

		if(mViewPager.getCurrentItem() > 0)
		{
			Common.LogMessage("Line 708 handleBack() Cycle through tabs until we hit tab 0");
			mViewPager.post(() -> mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1));
			return;
		}


		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
		{
			Common.LogMessage("Line 716 handleBack() Let's end now... SDK < TIRAMISU");
			obpc.setEnabled(false);
			finish();
		} else {
			Common.LogMessage("Line 720 handleBack() SDK >= TIRAMISU... Let the system do it's thing...");
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

		Common.LogMessage("MainActivity.onSaveInstanceState() Stashing current settings into a bundle....");
		outState.putInt("page", mViewPager.getCurrentItem());
		outState.putInt("UpdateFrequency", UpdateFrequency);
		outState.putInt("DayNightMode", DayNightMode);
		outState.putInt("widget_theme_mode", widget_theme_mode);

		Common.LogMessage("UpdateFrequency: " + UpdateFrequency);
		Common.LogMessage("DayNightMode: " + DayNightMode);
		Common.LogMessage("widget_theme_mode: " + widget_theme_mode);

		Editable edit = fgColour.getText();
		if(edit != null && edit.length() > 0)
		{
			int fg = parseHexToColour(edit.toString());
			outState.putInt("fg", fg);
		} else {
			outState.putInt("fg", 0xFFFFFFFF);
		}

		edit = bgColour.getText();
		if(edit != null && edit.length() > 0)
		{
			int bg = parseHexToColour(edit.toString());
			outState.putInt("bg", bg);
		} else {
			outState.putInt("fg", 0x00000000);
		}

		outState.putBoolean("wo", wifi_only.isChecked());
		outState.putBoolean("ui", use_icons.isChecked());
		outState.putBoolean("met", metric_forecasts.isChecked());
		outState.putBoolean("si", show_indoor.isChecked());
		outState.putBoolean("sr", showRadar.isChecked());
	}

	private void setStrings()
	{
		updateOptions = new String[]
		{
			weeWXApp.getAndroidString(R.string.manual_update),
			weeWXApp.getAndroidString(R.string.every_5_minutes),
			weeWXApp.getAndroidString(R.string.every_10_minutes),
			weeWXApp.getAndroidString(R.string.every_15_minutes),
			weeWXApp.getAndroidString(R.string.every_30_minutes),
			weeWXApp.getAndroidString(R.string.every_hour),
		};

		themeOptions = new String[]
		{
			weeWXApp.getAndroidString(R.string.light_theme),
			weeWXApp.getAndroidString(R.string.dark_theme),
			weeWXApp.getAndroidString(R.string.system_default)
		};

		widgetThemeOptions = new String[]
		{
			weeWXApp.getAndroidString(R.string.system_default),
			weeWXApp.getAndroidString(R.string.match_app),
			weeWXApp.getAndroidString(R.string.light_theme),
			weeWXApp.getAndroidString(R.string.dark_theme),
			weeWXApp.getAndroidString(R.string.custom_setting)
		};

		int disabled = Common.colours.LightGray;
		if(KeyValue.theme == R.style.AppTheme_weeWXApp_Dark_Common)
			disabled = Common.colours.DarkGray;

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
		ArrayAdapter<String> adapter1 = newArrayAdapter(R.layout.spinner_layout, updateOptions);
		ArrayAdapter<String> adapter2 = newArrayAdapter(R.layout.spinner_layout, themeOptions);
		ArrayAdapter<String> adapter3 = newArrayAdapter(R.layout.spinner_layout, widgetThemeOptions);

		final int uf = UpdateFrequency;
		final int dnm = DayNightMode;
		final int wtm = widget_theme_mode;

		runOnUiThread(() ->
		{
			Common.LogMessage("UpdateFrequency: " + uf);
			Common.LogMessage("DayNightMode: " + dnm);
			Common.LogMessage("widget_theme_mode: " + wtm);

			s1.setAdapter(adapter1);
			s1.setText(updateOptions[uf], false);

			s2.setAdapter(adapter2);
			s2.setText(themeOptions[dnm], false);

			if(wtm == 4)
			{
				fgtil.setVisibility(View.VISIBLE);
				bgtil.setVisibility(View.VISIBLE);
			} else {
				fgtil.setVisibility(View.GONE);
				bgtil.setVisibility(View.GONE);
			}

			s3.setAdapter(adapter3);
			s3.setText(widgetThemeOptions[wtm], false);
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
			Common.LogMessage("Line 886 closeKeyboard() Let's hide the on screen keyboard...");
			imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
			focus.clearFocus();
		}
	}

	void closeDrawer()
	{
		if(mDrawerLayout.isDrawerOpen(GravityCompat.START))
		{
			Common.LogMessage("Let's shut the drawer...");
			mDrawerLayout.closeDrawer(GravityCompat.START);
		}
	}

	private void resetActivity()
	{
		Common.LogMessage("Resetting mSectionsPagerAdapter");
		Common.reload();

		((weeWXApp)getApplication()).applyTheme();

		recreate();

		closeKeyboard();
		closeDrawer();

		WidgetProvider.updateAppWidget();

		updateHamburger();
		updateDropDowns();
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
			Common.LogMessage("trash all data");

			Common.clearPref();
			Common.commitPref();

			if(!Common.deleteFile("webcam.jpg"))
				Common.LogMessage("Failed to delete webcam.jpg, or it no longer exists...");

			if(!Common.deleteFile("radar.gif"))
				Common.LogMessage("Failed to delete radar.gif, or it no longer exists...");

			WidgetProvider.updateAppWidget();

			dialog_interface.cancel();

			System.exit(0);
		}).setNegativeButton(weeWXApp.getAndroidString(R.string.no), (dialog_interface, i) -> dialog_interface.cancel());

		builder.create().show();
	}

	private void processSettings()
	{
		long current_time = Math.round(System.currentTimeMillis() / 1000.0);

		if(mainactivityThread != null)
		{
			if(mainactivityThread.isAlive())
			{
				if(maStart + 30 > current_time)
				{
					Common.LogMessage("rtStart is less than 30s old, we'll skip this attempt...");
					return;
				}

				Common.LogMessage("rtStart is 30+s old, we'll interrupt it...");
				mainactivityThread.interrupt();
			}

			mainactivityThread = null;
		}

		maStart = current_time;

		mainactivityThread = new Thread(() ->
		{
			Common.LogMessage("mainactivityThread started...");

			String tmpStr;
			boolean validURL;
			boolean validURL1 = false;
			boolean validURL2 = false;
			boolean validURL3;
			boolean validURL5 = false;

			Common.SetStringPref("lastError", "");

			String olddata = Common.GetStringPref("BASE_URL", "");
			String oldradar = Common.GetStringPref("RADAR_URL", "");
			String oldforecast = Common.GetStringPref("FORECAST_URL", "");
			String oldwebcam = Common.GetStringPref("WEBCAM_URL", "");
			String oldcustom = Common.GetStringPref("CUSTOM_URL", "");
			String oldcustom_url = Common.GetStringPref("custom_url", "");

			String data = "", radtype = "", radar = "", forecast = "", webcam = "",
					custom = "", custom_url, fctype = "", bomtown = "", metierev;

			boolean icons_exist = Common.checkForImages();

			if(use_icons.isChecked() && (Common.GetLongPref("icon_version",
					0) < Common.icon_version || !icons_exist))
			{
				try
				{
					if(!Common.downloadIcons())
					{
						Common.SetStringPref("lastError",
								weeWXApp.getAndroidString(R.string.icons_failed_to_download));
						runOnUiThread(() ->
						{
							b1.setEnabled(true);
							b2.setEnabled(true);
							dialog.dismiss();
							new AlertDialog.Builder(this)
									.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_detect_icons))
									.setMessage(Common.GetStringPref("lastError",
											weeWXApp.getAndroidString(R.string.unknown_error_occurred)))
									.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
											(dialog, which) -> {}).show();
						});

						maStart = 0;
						return;
					}
				} catch(Exception e) {
					Common.SetStringPref("lastError", e.toString());
					runOnUiThread(() ->
					{
						b1.setEnabled(true);
						b2.setEnabled(true);
						dialog.dismiss();
						new AlertDialog.Builder(this)
								.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_detect_icons))
								.setMessage(Common.GetStringPref("lastError",
										weeWXApp.getAndroidString(R.string.unknown_error_occurred)))
								.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
										(dialog, which) -> {}).show();
					});

					maStart = 0;
					return;
				}

				Common.SetLongPref("icon_version", Common.icon_version);
			}

			String settings_url = settingsURL.getText() != null ? settingsURL.getText().toString().trim() : "";
			if(settings_url.isBlank() || settings_url.equals("https://example.com/weewx/inigo-settings.txt"))
			{
				Common.SetStringPref("lastError",
						weeWXApp.getAndroidString(R.string.url_was_default_or_empty));
				runOnUiThread(() ->
				{
					b1.setEnabled(true);
					b2.setEnabled(true);
					dialog.dismiss();
					new AlertDialog.Builder(this)
							.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_settings))
							.setMessage(Common.GetStringPref("lastError",
									weeWXApp.getAndroidString(R.string.unknown_error_occurred)))
							.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
									(dialog, which) -> {}).show();
				});

				maStart = 0;
				return;
			}

			try
			{
				String settingsData = Common.downloadSettings(settingsURL.getText().toString()).trim();
				if(settingsData == null || settingsData.isBlank())
				{
					maStart = 0;
					return;
				}

				String[] bits = settingsData.split("\\n");
				for(String bit : bits)
				{
					String[] mb = bit.split("=", 2);
					mb[0] = mb[0].trim().toLowerCase(Locale.ENGLISH);
					if(mb[0].equals("data"))
						data = mb[1];
					if(mb[0].equals("radtype"))
						radtype = mb[1].toLowerCase(Locale.ENGLISH);
					if(mb[0].equals("radar"))
						radar = mb[1];
					if(mb[0].equals("fctype"))
						fctype = mb[1].toLowerCase(Locale.ENGLISH);
					if(mb[0].equals("forecast"))
						forecast = mb[1];
					if(mb[0].equals("webcam"))
						webcam = mb[1];
					if(mb[0].equals("custom"))
						custom = mb[1];
				}

				if(fctype.isBlank())
					fctype = "Yahoo";

				if(radtype.isBlank())
					radtype = "image";

				validURL = true;
			} catch(Exception e) {
				Common.SetStringPref("lastError", e.toString());
				Common.doStackOutput(e);
				validURL = false;
			}

			if(!validURL)
			{
				runOnUiThread(() ->
				{
					b1.setEnabled(true);
					b2.setEnabled(true);
					dialog.dismiss();
					new AlertDialog.Builder(this)
							.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_settings))
							.setMessage(Common.GetStringPref("lastError",
									weeWXApp.getAndroidString(R.string.unknown_error_occurred)))
							.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
									(dialog, which) -> {}).show();
				});

				maStart = 0;
				return;
			}

			Common.LogMessage("data == " + data);

			if(data.isBlank())
			{
				Common.SetStringPref("lastError",
						weeWXApp.getAndroidString(R.string.data_url_was_blank));
				runOnUiThread(() ->
				{
					b1.setEnabled(true);
					b2.setEnabled(true);
					dialog.dismiss();
					new AlertDialog.Builder(this)
							.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_data_txt))
							.setMessage(Common.GetStringPref("lastError",
									weeWXApp.getAndroidString(R.string.unknown_error_occurred)))
							.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
									(dialog, which) -> {}).show();
				});

				maStart = 0;
				return;
			}

			if(!data.equals(olddata))
			{
				try
				{
					Common.reallyGetWeather(data);
					validURL1 = true;
				} catch(Exception e) {
					Common.SetStringPref("lastError", e.toString());
					Common.doStackOutput(e);
				}
			} else
				validURL1 = true;

			if(!validURL1)
			{
				runOnUiThread(() ->
				{
					b1.setEnabled(true);
					b2.setEnabled(true);
					dialog.dismiss();
					new AlertDialog.Builder(this)
							.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_radar_image))
							.setMessage(Common.GetStringPref("lastError",
									weeWXApp.getAndroidString(R.string.unknown_error_occurred)))
							.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
									(dialog, which) -> {}).show();
				});

				maStart = 0;
				return;
			}

			if(!radar.isBlank() && !radar.equals(oldradar))
			{
				try
				{
					if(radtype.equals("image"))
					{
						validURL2 = Common.downloadToFile("radar.gif", radar);
					} else if(radtype.equals("webpage")) {
						validURL2 = Common.checkURL(radar);
					}
				} catch(Exception e) {
					Common.SetStringPref("lastError", e.toString());
					Common.doStackOutput(e);
				}

				if(!validURL2)
				{
					runOnUiThread(() ->
					{
						b1.setEnabled(true);
						b2.setEnabled(true);
						dialog.dismiss();
						new AlertDialog.Builder(this)
								.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_radar_image))
								.setMessage(Common.GetStringPref("lastError",
										weeWXApp.getAndroidString(R.string.unknown_error_occurred)))
								.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
										(dialog, which) -> {}).show();
					});

					maStart = 0;
					return;
				}
			}

			if(!forecast.isBlank())
			{
				try
				{
					switch (fctype.toLowerCase(Locale.ENGLISH))
					{
						case "yahoo" ->
						{
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
							if(!forecast.startsWith("http"))
							{
								Common.SetStringPref("lastError", "Yahoo API recently changed, you need to update your settings.");
								runOnUiThread(() ->
								{
									b1.setEnabled(true);
									b2.setEnabled(true);
									dialog.dismiss();
									new AlertDialog.Builder(this)
											.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_or_download))
											.setMessage(Common.GetStringPref("lastError",
													weeWXApp.getAndroidString(R.string.unknown_error_occurred)))
											.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
													(dialog, which) -> {}).show();
								});

								maStart = 0;
								return;
							}
						}
						case "weatherzone" ->
						{
							forecast = "https://rss.weatherzone.com.au/?u=12994-1285&lt=aploc&lc=" + forecast + "&obs=0&fc=1&warn=0";
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
						}
						case "yr.no", "met.no", "weather.gc.ca", "weather.gc.ca-fr", "metoffice.gov.uk", "bom2", "aemet.es", "dwd.de", "tempoitalia.it" ->
						{
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
						}
						case "wmo.int" ->
						{
							if(!forecast.startsWith("http"))
								forecast = "https://worldweather.wmo.int/en/json/" + forecast.trim() + "_en.xml";
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
						}
						case "weather.gov" ->
						{
							String lat = "", lon = "";
							if(forecast.contains("?"))
								forecast = forecast.split("\\?", 2)[1].trim();
							if(forecast.contains("lat") && forecast.contains("lon"))
							{
								String[] tmp = forecast.split("&");
								for (String line : tmp)
								{
									if(line.split("=", 2)[0].equals("lat"))
										lat = line.split("=", 2)[1].trim();
									if(line.split("=", 2)[0].equals("lon"))
										lon = line.split("=", 2)[1].trim();
								}
							} else
							{
								lat = forecast.split(",")[0].trim();
								lon = forecast.split(",")[1].trim();
							}
							forecast = "https://forecast.weather.gov/MapClick.php?lat=" + lat + "&lon=" + lon + "&unit=0&lg=english&FcstType=json";
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
						}
						case "bom3" ->
						{
							forecast = "https://api.weather.bom.gov.au/v1/locations/" + forecast.trim() + "/forecasts/daily";
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
						}
						case "metservice.com" ->
						{
							forecast = "https://www.metservice.com/publicData/localForecast" + forecast;
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
						}
						case "openweathermap.org" ->
						{
							if(metric_forecasts.isChecked())
								forecast += "&units=metric";
							else
								forecast += "&units=imperial";
							forecast += "&lang=" + Locale.getDefault().getLanguage();
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
						}
						case "weather.com" ->
						{
							forecast = "https://api.weather.com/v3/wx/forecast/daily/5day?geocode=" + forecast + "&format=json&apiKey=d522aa97197fd864d36b418f39ebb323";
							if(metric_forecasts.isChecked())
								forecast += "&units=m";
							else
								forecast += "&units=e";
							forecast += "&language=" + Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry();
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
						}
						case "met.ie" ->
						{
							metierev = "https://prodapi.metweb.ie/location/reverse/" + forecast.replaceAll(",", "/");
							forecast = "https://prodapi.metweb.ie/weather/daily/" + forecast.replaceAll(",", "/") + "/10";

							tmpStr = Common.GetStringPref("metierev", "");
							if(tmpStr == null)
							{
								maStart = 0;
								return;
							}

							if(tmpStr.isBlank() || !forecast.equals(oldforecast))
							{
								metierev = Common.downloadString(metierev);
								if(metierev == null)
								{
									maStart = 0;
									return;
								}

								JSONObject jobj = new JSONObject(metierev);
								metierev = jobj.getString("city") + ", Ireland";
								Common.SetStringPref("metierev", metierev);
							}
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
							Common.LogMessage("metierev=" + metierev);
						}
						default ->
						{
							Common.SetStringPref("lastError", String.format(weeWXApp.getAndroidString(R.string.forecast_type_is_invalid), fctype));
							runOnUiThread(() ->
							{
								b1.setEnabled(true);
								b2.setEnabled(true);
								dialog.dismiss();
								new AlertDialog.Builder(this)
										.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_forecast))
										.setMessage(Common.GetStringPref("lastError", weeWXApp.getAndroidString(R.string.unknown_error_occurred)))
										.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again), (dialog, which) ->
										{
										}).show();
							});

							maStart = 0;
							return;
						}
					}
				} catch(Exception e) {
					Common.SetStringPref("lastError", e.toString());
					Common.doStackOutput(e);
				}
			}

			Common.LogMessage("line 1380");

			if((fctype.equals("weather.gov") || fctype.equals("yahoo")) && !icons_exist && !use_icons.isChecked())
			{
				Common.SetStringPref("lastError",
						String.format(weeWXApp.getAndroidString(R.string.forecast_type_needs_icons), fctype));
				runOnUiThread(() ->
				{
					use_icons.setChecked(true);
					b1.setEnabled(true);
					b2.setEnabled(true);
					dialog.dismiss();
					new AlertDialog.Builder(this)
						.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_detect_forecast_icons))
						.setMessage(Common.GetStringPref("lastError",
								weeWXApp.getAndroidString(R.string.unknown_error_occurred)))
						.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
								(dialog, which) -> {}).show();
				});

				maStart = 0;
				return;
			}

			if(!forecast.isBlank() && oldforecast != null && !oldforecast.equals(forecast))
			{
				Common.LogMessage("forecast checking: " + forecast);

				validURL3 = Common.reallyGetForecast(forecast) != null;

				if(!validURL3)
				{
					runOnUiThread(() ->
					{
						b1.setEnabled(true);
						b2.setEnabled(true);
						dialog.dismiss();
						new AlertDialog.Builder(this)
								.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_forecast))
								.setMessage(Common.GetStringPref("lastError",
										weeWXApp.getAndroidString(R.string.unknown_error_occurred)))
								.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
										(dialog, which) -> {}).show();
					});

					maStart = 0;
					return;
				}
			}

			if(!webcam.isBlank() && !webcam.equals(oldwebcam))
			{
				Common.LogMessage("checking: " + webcam);

				try
				{
					Bitmap bm = null;

					try
					{
						bm = Common.loadOrDownloadImage(webcam, "webcam.jpg", true);
					} catch(Exception e) {
						Common.LogMessage("Error! " + e);
					}

					if(bm == null)
					{
						Common.LogMessage("bm is null!");

						runOnUiThread(() ->
						{
							b1.setEnabled(true);
							b2.setEnabled(true);
							dialog.dismiss();
							new AlertDialog.Builder(this)
									.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_webcam_url))
									.setMessage(Common.GetStringPref("lastError",
											weeWXApp.getAndroidString(R.string.unknown_error_occurred)))
									.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
											(dialog, which) -> {}).show();
						});

						maStart = 0;
						return;
					}
				} catch(Exception e) {
					Common.doStackOutput(e);
				}
			}

			custom_url = customURL.getText() != null ? customURL.getText().toString().trim() : "";
			if(custom_url.isBlank())
			{
				if(!custom.isBlank() && !custom.equals("https://example.com/mobile.html") && !custom.equals(oldcustom))
				{
					try
					{
						if(Common.checkURL(custom))
							validURL5 = true;
						else
							Common.RemovePref("custom_url");
					} catch(Exception e) {
						Common.SetStringPref("lastError", e.toString());
						Common.doStackOutput(e);
					}

					if(!validURL5)
					{
						runOnUiThread(() ->
						{
							b1.setEnabled(true);
							b2.setEnabled(true);
							dialog.dismiss();
							new AlertDialog.Builder(this)
									.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_custom_url))
									.setMessage(Common.GetStringPref("lastError",
											weeWXApp.getAndroidString(R.string.unknown_error_occurred)))
									.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
											(dialog, which) -> {}).show();
						});

						maStart = 0;
						return;
					}
				}
			} else {
				if(!custom_url.equals(oldcustom_url))
				{
					try
					{
						if(Common.checkURL(custom_url))
							validURL5 = true;
					} catch(Exception e) {
						Common.SetStringPref("lastError", e.toString());
						Common.doStackOutput(e);
					}

					if(!validURL5)
					{
						runOnUiThread(() ->
						{
							b1.setEnabled(true);
							b2.setEnabled(true);
							dialog.dismiss();
							new AlertDialog.Builder(this)
									.setTitle(weeWXApp.getAndroidString(R.string.wasnt_able_to_connect_custom_url))
									.setMessage(Common.GetStringPref("lastError",
											weeWXApp.getAndroidString(R.string.unknown_error_occurred)))
									.setPositiveButton(weeWXApp.getAndroidString(R.string.ill_fix_and_try_again),
											(dialog, which) -> {}).show();
						});

						maStart = 0;
						return;
					}
				}
			}

			if(forecast == null || forecast.isBlank())
			{
				Common.SetLongPref("rssCheck", 0);
				Common.SetStringPref("forecastData", null);
			}

			Common.SetStringPref("SETTINGS_URL", settingsURL.getText().toString());
			Common.SetIntPref("updateInterval", UpdateFrequency);
			Common.SetStringPref("BASE_URL", data);
			Common.SetStringPref("radtype", radtype);
			Common.SetStringPref("RADAR_URL", radar);
			Common.SetStringPref("FORECAST_URL", forecast);
			Common.SetStringPref("fctype", fctype);
			Common.SetStringPref("WEBCAM_URL", webcam);
			Common.SetStringPref("CUSTOM_URL", custom);
			Common.SetStringPref("custom_url", custom_url);
			Common.SetBoolPref("radarforecast", showRadar.isChecked());

			Common.SetBoolPref("metric", metric_forecasts.isChecked());
			Common.SetBoolPref("showIndoor", show_indoor.isChecked());
			Common.SetIntPref("DayNightMode", DayNightMode);
			Common.SetBoolPref("onlyWIFI", wifi_only.isChecked());
			Common.SetBoolPref("use_icons", use_icons.isChecked());

			Common.SetIntPref(Common.WIDGET_THEME_MODE, widget_theme_mode);
			KeyValue.widget_theme_mode = widget_theme_mode;

			Editable edit = fgColour.getText();
			if(edit != null && edit.length() > 0)
			{
				int fg = parseHexToColour(edit.toString());
				Common.SetIntPref("fgColour", fg);
				Common.LogMessage("Saved widget fg colour: " + to_ARGB_hex(fg));
			}

			edit = bgColour.getText();
			if(edit != null && edit.length() > 0)
			{
				int bg = parseHexToColour(edit.toString());
				Common.SetIntPref("bgColour", bg);
				Common.LogMessage("Saved widget bg colour: " + to_ARGB_hex(bg));
			}

			runOnUiThread(() ->
			{
				Common.LogMessage("Do some stuff here...");
				scrollView.scrollTo(0, 0);
				dialog.dismiss();
				resetActivity();
			});

			updateHamburger();
			updateDropDowns();
			maStart = 0;
		});

		mainactivityThread.start();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		Common.LogMessage("Resuming app updates");

		updateHamburger();
		updateDropDowns();
		updateColours();
	}

	private final Observer<String> notificationObserver = str ->
	{
		Common.LogMessage("notificationObserver: " + str);

		if(str.equals(Common.TAB0_INTENT))
		{
			Common.getWeather();
			runOnUiThread(() ->
			{
				TabLayout.Tab tab = tabLayout.getTabAt(0);
				if(tab != null)
					tab.select();
			});
		}

		if(str.equals(Common.INIGO_INTENT))
			showUpdateAvailable();

		if(str.equals(Common.FAILED_INTENT))
		{
			runOnUiThread(() -> new AlertDialog
					.Builder(this)
					.setTitle(weeWXApp.getAndroidString(R.string.error_occurred_while_attempting_to_update))
					.setMessage(Common.GetStringPref("lastError", weeWXApp.getAndroidString(R.string.unknown_error_occurred)))
					.setPositiveButton("Ok", (dialog, which) ->
					{
					}).show());
		}
	};

	public boolean isViewPagerNull()
	{
		return mViewPager == null;
	}

	public void setUserInputPager(boolean b)
	{
		Common.LogMessage("MainActivity.setUserInputPager()");
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