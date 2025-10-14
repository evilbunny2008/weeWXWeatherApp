package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;

import com.github.evilbunny2008.androidmaterialcolorpickerdialog.ColorPicker;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener
{
	private TabLayout tabLayout;
	private Common common;
	private DrawerLayout mDrawerLayout;
	private TextInputEditText settingsURL;
	private TextInputEditText customURL;
	//private TextInputEditText fgColour;
	//private TextInputEditText bgColour;
	private MaterialButton b1;
	private MaterialButton b2;
	private MaterialButton b3;
	private MaterialButton b4;
	private AutoCompleteTextView s1;
	private AutoCompleteTextView s2;
	private MaterialSwitch show_indoor, metric_forecasts;
	private MaterialTextView tv;

	private AlertDialog dialog;

	private LinearLayout settingsLayout;
	private LinearLayout aboutLayout;

	private static int pos;
	private static int theme;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		for (java.lang.reflect.Method m : ColorPicker.class.getMethods())
			Log.d("evilbunny CP_METHOD", m.getName());

		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

		final CoordinatorLayout rootView = findViewById(R.id.main_content);

		ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) ->
		{
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			{
				WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
				insetsController.show(WindowInsetsCompat.Type.systemBars());
			}

			Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());

			// Preserve existing padding
			int left = view.getPaddingLeft();
			int right = view.getPaddingRight();

			// Add system top inset to the existing top padding
			int top = view.getPaddingTop() + systemInsets.top;
			int bottom = view.getPaddingBottom() + systemInsets.bottom;

			view.setPadding(left, top, right, bottom);

			return insets;
		});

		common = new Common(this);

		mDrawerLayout = findViewById(R.id.drawer_layout);

		OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
		onBackPressedDispatcher.addCallback(this, onBackPressedCallback);

		tabLayout = findViewById(R.id.tabs);

		SectionsStateAdapter mSectionsPagerAdapter = new SectionsStateAdapter(getSupportFragmentManager(), getLifecycle());
		mSectionsPagerAdapter.addFragment(new Weather(common));
		mSectionsPagerAdapter.addFragment(new Stats(common));
		mSectionsPagerAdapter.addFragment(new Forecast(common));
		mSectionsPagerAdapter.addFragment(new Webcam(common));
		mSectionsPagerAdapter.addFragment(new Custom(common));

		ViewPager2 mViewPager = findViewById(R.id.container);
		//mViewPager.setUserInputEnabled(false);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		String[] tabTitles;
		if(common.GetBoolPref("radarforecast", true))
			tabTitles = new String[]{getString(R.string.weather2), getString(R.string.stats2), getString(R.string.forecast2), getString(R.string.webcam2), getString(R.string.custom2)};
		else
			tabTitles = new String[]{getString(R.string.weather2), getString(R.string.stats2), getString(R.string.radar), getString(R.string.webcam2), getString(R.string.custom2)};
		new TabLayoutMediator(tabLayout, mViewPager, ((tab, position) -> tab.setText(tabTitles[position]))).attach();

		try
		{
			if(Common.isEmpty(common.GetStringPref("BASE_URL", "")))
				mDrawerLayout.openDrawer(GravityCompat.START);
		} catch (Exception e) {
			Common.doStackOutput(e);
		}

		settingsLayout = findViewById(R.id.settingsLayout);
		aboutLayout = findViewById(R.id.aboutLayout);

		settingsURL = findViewById(R.id.settings);
		customURL = findViewById(R.id.customURL);
		s1 = findViewById(R.id.spinner1);

		metric_forecasts = findViewById(R.id.metric_forecasts);
		show_indoor = findViewById(R.id.show_indoor);
		s2 = findViewById(R.id.spinner2);

		b1 = findViewById(R.id.saveButton);
		b2 = findViewById(R.id.deleteData);
		b3 = findViewById(R.id.aboutButton);
		b4 = findViewById(R.id.settingsButton);

		//fgColour = findViewById(R.id.fg_Picker);
	//	bgColour = findViewById(R.id.bg_Picker);

		tv = findViewById(R.id.aboutText);

		Thread t = new Thread(() ->
		{
			try
			{
				// Sleep needed to stop frames dropping while loading
				Thread.sleep(500);
			} catch (Exception e) {
				Common.doStackOutput(e);
			}

			Handler mHandler = new Handler(Looper.getMainLooper());
			mHandler.post(this::doSettings);

			common.setAlarm("MainActivity");
		});

		t.start();
	}

	private void showUpdateAvailable()
	{
		final AlertDialog.Builder d = new AlertDialog.Builder(this);
		d.setTitle(getString(R.string.app_name));
		d.setMessage(getString(R.string.inigo_needs_updating));
		d.setPositiveButton(getString(R.string.ok), null);
		d.setIcon(R.mipmap.ic_launcher_foreground);
		d.show();
	}

	private void doSettings()
	{
		String[] paths = new String[]
		{
			getString(R.string.manual_update),
			getString(R.string.every_5_minutes),
			getString(R.string.every_10_minutes),
			getString(R.string.every_15_minutes),
			getString(R.string.every_30_minutes),
			getString(R.string.every_hour),
		};

		ArrayAdapter<String> adapter = new ArrayAdapter<>(common.context, R.layout.spinner_layout, paths);
		s1.setAdapter(adapter);
		s1.setOnItemClickListener(this);
		pos = common.GetIntPref("updateInterval", 1);
		if(pos >= 0 && pos < paths.length)
			s1.setText(paths[pos], false);
		else
			s1.setText(getString(R.string.every_5_minutes));

		MaterialSwitch wifi_only = findViewById(R.id.wifi_only);
		wifi_only.setChecked(common.GetBoolPref("onlyWIFI", false));
		MaterialSwitch use_icons = findViewById(R.id.use_icons);
		use_icons.setChecked(common.GetBoolPref("use_icons", false));
		metric_forecasts.setChecked(common.GetBoolPref("metric", true));
		show_indoor.setChecked(common.GetBoolPref("showIndoor", false));

		String[] themeString = new String[]
		{
			getString(R.string.use_light_theme),
			getString(R.string.use_dark_theme2),
			getString(R.string.use_system_default)
		};

		ArrayAdapter<String> adapter2 = new ArrayAdapter<>(common.context, R.layout.spinner_layout, themeString);
		s2.setAdapter(adapter2);
		s2.setOnItemClickListener(this);
		theme = common.GetIntPref("dark_theme", 2);
		if (theme >= 0 && theme < themeString.length)
			s2.setText(themeString[theme], false);
		else
			s2.setText(getString(R.string.use_system_default));

		MaterialRadioButton showRadar = findViewById(R.id.showRadar);
		showRadar.setChecked(common.GetBoolPref("radarforecast", true));
		MaterialRadioButton showForecast = findViewById(R.id.showForecast);
		showForecast.setChecked(!common.GetBoolPref("radarforecast", true));

		b1.setOnClickListener(arg0 ->
		{
			b1.setEnabled(false);
			b2.setEnabled(false);
			InputMethodManager mgr = (InputMethodManager)common.context.getSystemService(Context.INPUT_METHOD_SERVICE);
			if(mgr != null)
			{
				mgr.hideSoftInputFromWindow(settingsURL.getWindowToken(), 0);
				mgr.hideSoftInputFromWindow(customURL.getWindowToken(), 0);
			}

			Common.LogMessage("show dialog");
			AlertDialog.Builder builder = new AlertDialog.Builder(common.context);
			builder.setCancelable(false);
			builder.setView(R.layout.layout_loading_dialog);
			dialog = builder.create();
			dialog.show();
			processSettings();
		});

		b2.setOnClickListener(arg0 -> checkReally());

		b3.setOnClickListener(arg0 ->
		{
			settingsLayout.setVisibility(View.GONE);
			aboutLayout.setVisibility(View.VISIBLE);
		});

		b4.setOnClickListener(arg0 ->
		{
			aboutLayout.setVisibility(View.GONE);
			settingsLayout.setVisibility(View.VISIBLE);
		});

		settingsURL.setText(common.GetStringPref("SETTINGS_URL", "https://example.com/weewx/inigo-settings.txt"));
		settingsURL.setOnFocusChangeListener((v, hasFocus) ->
		{
			if (!hasFocus)
				hideKeyboard(v);
		});

		customURL.setText(common.GetStringPref("custom_url", ""));
		customURL.setOnFocusChangeListener((v, hasFocus) ->
		{
			if (!hasFocus)
				hideKeyboard(v);
		});

		LinearLayout settingsLayout = findViewById(R.id.settingsLayout);
		settingsLayout.setVisibility(View.VISIBLE);
		LinearLayout aboutLayout = findViewById(R.id.aboutLayout);
		aboutLayout.setVisibility(View.GONE);

		String lines = "<html><body>Big thanks to the <a href='http://weewx.com'>weeWX project</a>, as this app " +
				"wouldn't be possible otherwise.<br><br>" +
				"Weather Icons from <a href='https://www.flaticon.com/'>FlatIcon</a> and " +
				"is licensed under <a href='http://creativecommons.org/licenses/by/3.0/'>CC 3.0 BY</a> and " +
				"<a href='https://github.com/erikflowers/weather-icons'>Weather Font</a> by Erik Flowers" +
				"<br><br>" +
				"weeWX Weather App v" + common.getAppVersion() + " is by <a href='https://odiousapps.com'>OdiousApps</a>.</body</html>" +
				"<br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br>";

		tv.setText(HtmlCompat.fromHtml(lines, HtmlCompat.FROM_HTML_MODE_LEGACY));
		tv.setMovementMethod(LinkMovementMethod.getInstance());

		// https://github.com/Pes8/android-material-color-picker-dialog

		//String hex = "#" + Integer.toHexString(common.GetIntPref("fgColour", 0xFF000000)).toUpperCase(Locale.ENGLISH);
		//fgColour.setText(hex);
//		fgColour.setOnClickListener(v -> showPicker(common.GetIntPref("fgColour", 0xFF000000),true));

		//hex = "#" + Integer.toHexString(common.GetIntPref("bgColour", 0xFFFFFFFF)).toUpperCase(Locale.ENGLISH);
		//bgColour.setText(hex);
//		bgColour.setOnClickListener(v -> showPicker(common.GetIntPref("bgColour", 0xFFFFFFFF),false));
	}
/*
	private void showPicker(int col, final boolean fgColour)
	{
		final ColorPicker cp = new ColorPicker(MainActivity.this, col >> 24 & 255, col >> 16 & 255, col >> 8 & 255, col & 255);

		cp.setCallback(colour ->
		{
			Common.LogMessage("Pure Hex" + Integer.toHexString(colour));

			if(fgColour)
				common.SetIntPref("fgColour", colour);
			else
				common.SetIntPref("bgColour", colour);

			common.SendIntents();

			cp.dismiss();
		});

		cp.show();
	}
*/
	private void hideKeyboard(View view)
	{
		InputMethodManager inputMethodManager = (InputMethodManager)common.context.getSystemService(Activity.INPUT_METHOD_SERVICE);
		if(inputMethodManager != null)
			inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	public void onItemClick(AdapterView<?> parent, View v, int position, long id)
	{
		Common.LogMessage("Before -> pos="+pos+",theme="+theme);

		if(parent.getId() == R.id.spinner1)
			pos = position;
		else if(parent.getId() == R.id.spinner2)
			theme = position;

		Common.LogMessage("After -> pos="+pos+",theme="+theme);
	}

	private void checkReally()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(common.context);
		builder.setMessage(getString(R.string.remove_all_data)).setCancelable(false)
				.setPositiveButton(getString(R.string.ok), (dialog_interface, i) ->
				{
					Common.LogMessage("trash all data");

					common.clearPref();
					common.commitPref();

					File file = new File(common.context.getFilesDir(), "webcam.jpg");
					if(file.exists() && file.canWrite())
						if(!file.delete())
							Common.LogMessage("couldn't delete webcam.jpg");

					file = new File(common.context.getFilesDir(), "radar.gif");
					if(file.exists() && file.canWrite())
						if(!file.delete())
							Common.LogMessage("couldn't delete radar.gif");

					RemoteViews remoteViews = common.buildUpdate(common.context, theme);
					ComponentName thisWidget = new ComponentName(common.context, WidgetProvider.class);
					AppWidgetManager manager = AppWidgetManager.getInstance(common.context);
					manager.updateAppWidget(thisWidget, remoteViews);
					Common.LogMessage("widget intent broadcasted");

					dialog_interface.cancel();

					System.exit(0);
				}).setNegativeButton(getString(R.string.no), (dialog_interface, i) -> dialog_interface.cancel());

		builder.create().show();
	}

	private void processSettings()
	{
		Thread t = new Thread(() ->
		{
			boolean validURL = false;
			boolean validURL1 = false;
			boolean validURL2 = false;
			boolean validURL3 = false;
			boolean validURL5 = false;

			common.SetStringPref("lastError", "");

			String olddata = common.GetStringPref("BASE_URL", "");
			String oldradar = common.GetStringPref("RADAR_URL", "");
			String oldforecast = common.GetStringPref("FORECAST_URL", "");
			String oldwebcam = common.GetStringPref("WEBCAM_URL", "");
			String oldcustom = common.GetStringPref("CUSTOM_URL", "");
			String oldcustom_url = common.GetStringPref("custom_url", "");

			String data = "", radtype = "", radar = "", forecast = "", webcam = "", custom = "", custom_url, fctype = "", bomtown = "", metierev;

			MaterialSwitch metric_forecasts = findViewById(R.id.metric_forecasts);
			MaterialSwitch show_indoor = findViewById(R.id.show_indoor);
			MaterialSwitch wifi_only = findViewById(R.id.wifi_only);
			MaterialSwitch use_icons = findViewById(R.id.use_icons);

			MaterialRadioButton showRadar = findViewById(R.id.showRadar);
			long current_time = Math.round(System.currentTimeMillis() / 1000.0);

			if(use_icons.isChecked() && (common.GetLongPref("icon_version", 0) < Common.icon_version || !common.checkForImages()))
			{
				try
				{
					if (!common.downloadIcons())
					{
						common.SetStringPref("lastError", getString(R.string.icons_failed_to_download));
						runOnUiThread(() ->
						{
							b1.setEnabled(true);
							b2.setEnabled(true);
							dialog.dismiss();
							new AlertDialog.Builder(common.context)
									.setTitle(getString(R.string.wasnt_able_to_detect_icons))
									.setMessage(common.GetStringPref("lastError", getString(R.string.unknown_error_occurred)))
									.setPositiveButton(getString(R.string.ill_fix_and_try_again), (dialog, which) ->
									{
									}).show();
						});
						return;
					}
				} catch (Exception e) {
					common.SetStringPref("lastError", e.toString());
					runOnUiThread(() -> {
						b1.setEnabled(true);
						b2.setEnabled(true);
						dialog.dismiss();
						new AlertDialog.Builder(common.context)
								.setTitle(getString(R.string.wasnt_able_to_detect_icons))
								.setMessage(common.GetStringPref("lastError", getString(R.string.unknown_error_occurred)))
								.setPositiveButton(getString(R.string.ill_fix_and_try_again), (dialog, which) ->
								{
								}).show();
					});
					return;
				}

				common.SetLongPref("icon_version", Common.icon_version);
			}

			String settings_url = settingsURL.getText() != null ? settingsURL.getText().toString().trim() : "";
			if(Common.isEmpty(settings_url) || settings_url.equals("https://example.com/weewx/inigo-settings.txt"))
			{
				common.SetStringPref("lastError", getString(R.string.url_was_default_or_empty));
				runOnUiThread(() -> {
					b1.setEnabled(true);
					b2.setEnabled(true);
					dialog.dismiss();
					new AlertDialog.Builder(common.context)
							.setTitle(getString(R.string.wasnt_able_to_connect_settings))
							.setMessage(common.GetStringPref("lastError", getString(R.string.unknown_error_occurred)))
							.setPositiveButton(getString(R.string.ill_fix_and_try_again), (dialog, which) ->
							{
							}).show();
				});

				return;
			}

			try
			{
				String[] bits = common.downloadSettings(settingsURL.getText().toString()).split("\\n");
				for (String bit : bits)
				{
					String[] mb = bit.split("=", 2);
					mb[0] = mb[0].trim().toLowerCase(Locale.ENGLISH);
					if (mb[0].equals("data"))
						data = mb[1];
					if (mb[0].equals("radtype"))
						radtype = mb[1].toLowerCase(Locale.ENGLISH);
					if (mb[0].equals("radar"))
						radar = mb[1];
					if (mb[0].equals("fctype"))
						fctype = mb[1].toLowerCase(Locale.ENGLISH);
					if (mb[0].equals("forecast"))
						forecast = mb[1];
					if (mb[0].equals("webcam"))
						webcam = mb[1];
					if (mb[0].equals("custom"))
						custom = mb[1];
				}

				if(Common.isEmpty(fctype))
					fctype = "Yahoo";

				if(Common.isEmpty(radtype))
					radtype = "image";

				validURL = true;
			} catch (Exception e) {
				common.SetStringPref("lastError", e.toString());
				Common.doStackOutput(e);
			}

			if (!validURL)
			{
				runOnUiThread(() -> {
					b1.setEnabled(true);
					b2.setEnabled(true);
					dialog.dismiss();
					new AlertDialog.Builder(common.context)
							.setTitle(getString(R.string.wasnt_able_to_connect_settings))
							.setMessage(common.GetStringPref("lastError", getString(R.string.unknown_error_occurred)))
							.setPositiveButton(getString(R.string.ill_fix_and_try_again), (dialog, which) ->
							{
							}).show();
				});
				return;
			}

			Common.LogMessage("data == " + data);

			if (Common.isEmpty(data))
			{
				common.SetStringPref("lastError", getString(R.string.data_url_was_blank));
				runOnUiThread(() -> {
					b1.setEnabled(true);
					b2.setEnabled(true);
					dialog.dismiss();
					new AlertDialog.Builder(common.context)
							.setTitle(getString(R.string.wasnt_able_to_connect_data_txt))
							.setMessage(common.GetStringPref("lastError", getString(R.string.unknown_error_occurred)))
							.setPositiveButton(getString(R.string.ill_fix_and_try_again), (dialog, which) ->
							{
							}).show();
				});
				return;
			}

			if (!data.equals(olddata))
			{
				try
				{
					common.reallyGetWeather(data);
					validURL1 = true;
				} catch (Exception e) {
					common.SetStringPref("lastError", e.toString());
					Common.doStackOutput(e);
				}
			} else
				validURL1 = true;

			if (!validURL1)
			{
				runOnUiThread(() -> {
					b1.setEnabled(true);
					b2.setEnabled(true);
					dialog.dismiss();
					new AlertDialog.Builder(common.context)
							.setTitle(getString(R.string.wasnt_able_to_connect_radar_image))
							.setMessage(common.GetStringPref("lastError", getString(R.string.unknown_error_occurred)))
							.setPositiveButton(getString(R.string.ill_fix_and_try_again), (dialog, which) ->
							{
							}).show();
				});
				return;
			}

			if (!Common.isEmpty(radar) && !radar.equals(oldradar))
			{
				try
				{
					if(radtype.equals("image"))
					{
						File file = new File(getFilesDir(), "/radar.gif.tmp");
						File f = common.downloadJSOUP(file, radar);
						validURL2 = f.exists();
					} else if(radtype.equals("webpage")) {
						validURL2 = common.checkURL(radar);
					}
				} catch (Exception e) {
					common.SetStringPref("lastError", e.toString());
					Common.doStackOutput(e);
				}

				if (!validURL2)
				{
					runOnUiThread(() -> {
						b1.setEnabled(true);
						b2.setEnabled(true);
						dialog.dismiss();
						new AlertDialog.Builder(common.context)
								.setTitle(getString(R.string.wasnt_able_to_connect_radar_image))
								.setMessage(common.GetStringPref("lastError", getString(R.string.unknown_error_occurred)))
								.setPositiveButton(getString(R.string.ill_fix_and_try_again), (dialog, which) ->
								{
								}).show();
					});
					return;
				}
			}

			if(!Common.isEmpty(forecast))
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
								common.SetStringPref("lastError", "Yahoo API recently changed, you need to update your settings.");
								runOnUiThread(() ->
								{
									b1.setEnabled(true);
									b2.setEnabled(true);
									dialog.dismiss();
									new AlertDialog.Builder(common.context)
											.setTitle(getString(R.string.wasnt_able_to_connect_or_download))
											.setMessage(common.GetStringPref("lastError", getString(R.string.unknown_error_occurred)))
											.setPositiveButton(getString(R.string.ill_fix_and_try_again), (dialog, which) ->
											{
											}).show();
								});
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
						case "bom.gov.au" ->
						{
							common.SetStringPref("lastError", "Forecast type " + fctype + " is no longer supported due to ftp support being dropped in Android. Use bom2 forecasts instead, check the wiki for details.");
							runOnUiThread(() ->
							{
								b1.setEnabled(true);
								b2.setEnabled(true);
								dialog.dismiss();
								new AlertDialog.Builder(common.context)
										.setTitle(getString(R.string.wasnt_able_to_connect_forecast))
										.setMessage(common.GetStringPref("lastError", getString(R.string.unknown_error_occurred)))
										.setPositiveButton(getString(R.string.ill_fix_and_try_again), (dialog, which) ->
										{
										}).show();
							});
							return;
						}
						case "wmo.int" ->
						{
							if (!forecast.startsWith("http"))
								forecast = "https://worldweather.wmo.int/en/json/" + forecast.trim() + "_en.xml";
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
						}
						case "weather.gov" ->
						{
							String lat = "", lon = "";
							if (forecast.contains("?"))
								forecast = forecast.split("\\?", 2)[1].trim();
							if (forecast.contains("lat") && forecast.contains("lon"))
							{
								String[] tmp = forecast.split("&");
								for (String line : tmp)
								{
									if (line.split("=", 2)[0].equals("lat"))
										lat = line.split("=", 2)[1].trim();
									if (line.split("=", 2)[0].equals("lon"))
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
							if (metric_forecasts.isChecked())
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
							if (metric_forecasts.isChecked())
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
							if(Common.isEmpty(common.GetStringPref("metierev", "")) || !forecast.equals(oldforecast))
							{
								metierev = common.downloadForecast(fctype, metierev, null);
								JSONObject jobj = new JSONObject(metierev);
								metierev = jobj.getString("city") + ", Ireland";
								common.SetStringPref("metierev", metierev);
							}
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
							Common.LogMessage("metierev=" + metierev);
						}
						default ->
						{
							common.SetStringPref("lastError", String.format(getString(R.string.forecast_type_is_invalid), fctype));
							runOnUiThread(() ->
							{
								b1.setEnabled(true);
								b2.setEnabled(true);
								dialog.dismiss();
								new AlertDialog.Builder(common.context)
										.setTitle(getString(R.string.wasnt_able_to_connect_forecast))
										.setMessage(common.GetStringPref("lastError", getString(R.string.unknown_error_occurred)))
										.setPositiveButton(getString(R.string.ill_fix_and_try_again), (dialog, which) ->
										{
										}).show();
							});
							return;
						}
					}
				} catch (Exception e) {
					common.SetStringPref("lastError", e.toString());
					Common.doStackOutput(e);
				}
			}

			Common.LogMessage("line 742");

			if((fctype.equals("weather.gov") || fctype.equals("yahoo")) && !common.checkForImages() && !use_icons.isChecked())
			{
				common.SetStringPref("lastError", String.format(getString(R.string.forecast_type_needs_icons), fctype));
				runOnUiThread(() -> {
					b1.setEnabled(true);
					b2.setEnabled(true);
					dialog.dismiss();
					new AlertDialog.Builder(common.context)
							.setTitle(getString(R.string.wasnt_able_to_detect_forecast_icons))
							.setMessage(common.GetStringPref("lastError", getString(R.string.unknown_error_occurred)))
							.setPositiveButton(getString(R.string.ill_fix_and_try_again), (dialog, which) ->
							{
							}).show();
				});
				return;
			}

			if (!Common.isEmpty(forecast) && !oldforecast.equals(forecast))
			{
				Common.LogMessage("forecast checking: " + forecast);

				try
				{
					Common.LogMessage("checking: " + forecast);
					String tmp = common.downloadForecast(fctype, forecast, bomtown);
					if(tmp != null)
					{
						validURL3 = true;
						Common.LogMessage("updating rss cache");
						common.SetLongPref("rssCheck", current_time);
						common.SetStringPref("forecastData", tmp);
					}
				} catch (Exception e) {
					common.SetStringPref("lastError", e.toString());
					Common.doStackOutput(e);
				}

				if (!validURL3)
				{
					runOnUiThread(() -> {
						b1.setEnabled(true);
						b2.setEnabled(true);
						dialog.dismiss();
						new AlertDialog.Builder(common.context)
								.setTitle(getString(R.string.wasnt_able_to_connect_forecast))
								.setMessage(common.GetStringPref("lastError", getString(R.string.unknown_error_occurred)))
								.setPositiveButton(getString(R.string.ill_fix_and_try_again), (dialog, which) ->
								{
								}).show();
					});
					return;
				}
			}

			if (!Common.isEmpty(webcam) && !webcam.equals(oldwebcam))
			{
				Common.LogMessage("checking: " + webcam);

				if (!Webcam.downloadWebcam(webcam, common.context.getFilesDir()))
				{
					runOnUiThread(() -> {
						b1.setEnabled(true);
						b2.setEnabled(true);
						dialog.dismiss();
						new AlertDialog.Builder(common.context)
								.setTitle(getString(R.string.wasnt_able_to_connect_webcam_url))
								.setMessage(common.GetStringPref("lastError", getString(R.string.unknown_error_occurred)))
								.setPositiveButton(getString(R.string.ill_fix_and_try_again), (dialog, which) ->
								{
								}).show();
					});
					return;
				}
			}

			custom_url = customURL.getText() != null ? customURL.getText().toString().trim() : "";
			if(Common.isEmpty(custom_url))
			{
				if (!Common.isEmpty(custom) && !custom.equals("https://example.com/mobile.html") && !custom.equals(oldcustom))
				{
					try
					{
						if(common.checkURL(custom))
							validURL5 = true;
						else
							common.RemovePref("custom_url");
					} catch (Exception e) {
						common.SetStringPref("lastError", e.toString());
						Common.doStackOutput(e);
					}

					if (!validURL5)
					{
						runOnUiThread(() -> {
							b1.setEnabled(true);
							b2.setEnabled(true);
							dialog.dismiss();
							new AlertDialog.Builder(common.context)
									.setTitle(getString(R.string.wasnt_able_to_connect_custom_url))
									.setMessage(common.GetStringPref("lastError", getString(R.string.unknown_error_occurred)))
									.setPositiveButton(getString(R.string.ill_fix_and_try_again), (dialog, which) ->
									{
									}).show();
						});
						return;
					}
				}
			} else {
				if (!custom_url.equals(oldcustom_url))
				{
					try
					{
						if(common.checkURL(custom_url))
							validURL5 = true;
					} catch (Exception e) {
						common.SetStringPref("lastError", e.toString());
						Common.doStackOutput(e);
					}

					if (!validURL5)
					{
						runOnUiThread(() -> {
							b1.setEnabled(true);
							b2.setEnabled(true);
							dialog.dismiss();
							new AlertDialog.Builder(common.context)
									.setTitle(getString(R.string.wasnt_able_to_connect_custom_url))
									.setMessage(common.GetStringPref("lastError", getString(R.string.unknown_error_occurred)))
									.setPositiveButton(getString(R.string.ill_fix_and_try_again), (dialog, which) ->
									{
									}).show();
						});
						return;
					}
				}
			}

			if(Common.isEmpty(forecast))
			{
				common.SetLongPref("rssCheck", 0);
				common.SetStringPref("forecastData", "");
			}

			common.SetStringPref("SETTINGS_URL", settingsURL.getText().toString());
			common.SetIntPref("updateInterval", pos);
			common.SetStringPref("BASE_URL", data);
			common.SetStringPref("radtype", radtype);
			common.SetStringPref("RADAR_URL", radar);
			common.SetStringPref("FORECAST_URL", forecast);
			common.SetStringPref("fctype", fctype);
			common.SetStringPref("WEBCAM_URL", webcam);
			common.SetStringPref("CUSTOM_URL", custom);
			common.SetStringPref("custom_url", custom_url);
			common.SetBoolPref("radarforecast", showRadar.isChecked());

			common.SetBoolPref("metric", metric_forecasts.isChecked());
			common.SetBoolPref("showIndoor", show_indoor.isChecked());
			common.SetIntPref("dark_theme", theme);
			common.SetBoolPref("onlyWIFI", wifi_only.isChecked());
			common.SetBoolPref("use_icons", use_icons.isChecked());

			common.SendRefresh();
			runOnUiThread(() -> {
				if(!common.GetBoolPref("radarforecast", true))
					//noinspection ConstantConditions
					tabLayout.getTabAt(2).setText(R.string.radar);
				else
					//noinspection ConstantConditions
					tabLayout.getTabAt(2).setText(R.string.forecast2);

				b1.setEnabled(true);
				b2.setEnabled(true);
				dialog.dismiss();
				mDrawerLayout.closeDrawer(GravityCompat.START);
			});
		});

		t.start();
	}

	OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true)
	{
		@Override
		public void handleOnBackPressed()
		{
			if(mDrawerLayout.isDrawerOpen(GravityCompat.START))
			{
				mDrawerLayout.closeDrawer(GravityCompat.START);
			} else {
				Common.LogMessage("finishing up.");
				finish();
			}
		}
	};

	@Override
	public void onPause()
	{
		unregisterReceiver(serviceReceiver);
		super.onPause();
	}

	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	@Override
	protected void onResume()
	{
		super.onResume();

		IntentFilter filter = new IntentFilter();
		filter.addAction(Common.UPDATE_INTENT);
		filter.addAction(Common.FAILED_INTENT);
		filter.addAction(Common.TAB0_INTENT);
		filter.addAction(Common.INIGO_INTENT);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
			registerReceiver(serviceReceiver, filter, RECEIVER_NOT_EXPORTED);
		else
			registerReceiver(serviceReceiver, filter);

		Common.LogMessage("resuming app updates");
		common.SendIntents();
	}

	public void getWeather()
	{
		common.getWeather();
	}

	private final BroadcastReceiver serviceReceiver = new BroadcastReceiver()
	{
		@SuppressLint("SuspiciousIndentation")
		@Override
		public void onReceive(Context context, Intent intent)
		{
			try
			{
				Common.LogMessage("We have a hit, so we should probably update the screen.");
				String action = intent.getAction();
				if(action != null && action.equals(Common.TAB0_INTENT))
				{
					getWeather();

					runOnUiThread(() ->
					{
						//noinspection ConstantConditions
						tabLayout.getTabAt(0).select();
					});
				}

	//			if(action != null && action.equals(Common.UPDATE_INTENT))
	//			{
					//String hex = "#" + Integer.toHexString(common.GetIntPref("fgColour", 0xFF000000)).toUpperCase(Locale.ENGLISH);
					//fgColour.setText(hex);
					//hex = "#" + Integer.toHexString(common.GetIntPref("bgColour", 0xFFFFFFFF)).toUpperCase(Locale.ENGLISH);
					//bgColour.setText(hex);
	//			}

				if(action != null && action.equals(Common.INIGO_INTENT))
				{
					showUpdateAvailable();
				}

				if(action != null && action.equals(Common.FAILED_INTENT))
				{
					runOnUiThread(() -> new AlertDialog
							.Builder(common.context)
							.setTitle(getString(R.string.error_occurred_while_attempting_to_update))
							.setMessage(common.GetStringPref("lastError", getString(R.string.unknown_error_occurred)))
							.setPositiveButton("Ok", (dialog, which) ->
							{
							}).show());

				}
			} catch (Exception e) {
				Common.doStackOutput(e);
			}
		}
	};

	public static class SectionsStateAdapter extends FragmentStateAdapter
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