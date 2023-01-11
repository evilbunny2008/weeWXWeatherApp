package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.evilbunny2008.androidmaterialcolorpickerdialog.ColorPicker;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener
{
    private TabLayout tabLayout;
    private Common common;
    private DrawerLayout mDrawerLayout;
	private EditText settingsURL;
	private EditText customURL;
	private EditText fgColour;
	private EditText bgColour;
	private Button b1;
	private Button b2;
	private Button b3;
	private boolean showSettings = true;
	private Spinner s1;
	private Spinner s2;
	private SwitchCompat show_indoor, metric_forecasts;
	private TextView tv;

	private ProgressDialog dialog;

	private static int pos;
	private static int theme;

	@SuppressLint("WrongConstant")
	@Override
    protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		common = new Common(this);

		mDrawerLayout = findViewById(R.id.drawer_layout);

		tabLayout = findViewById(R.id.tabs);

	    SectionsStateAdapter mSectionsPagerAdapter = new SectionsStateAdapter(getSupportFragmentManager(), getLifecycle());
	    mSectionsPagerAdapter.addFragment(new Weather(common));
	    mSectionsPagerAdapter.addFragment(new Stats(common));
	    mSectionsPagerAdapter.addFragment(new Forecast(common));
	    mSectionsPagerAdapter.addFragment(new Webcam(common));
	    mSectionsPagerAdapter.addFragment(new Custom(common));

		ViewPager2 mViewPager = findViewById(R.id.container);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		String[] tabTitles;
		if(common.GetBoolPref("radarforecast", true))
			tabTitles = new String[]{getString(R.string.weather2), getString(R.string.stats2), getString(R.string.radar), getString(R.string.webcam2), getString(R.string.custom2)};
		else
			tabTitles = new String[]{getString(R.string.weather2), getString(R.string.stats2), getString(R.string.forecast2), getString(R.string.webcam2), getString(R.string.custom2)};
		new TabLayoutMediator(tabLayout, mViewPager, ((tab, position) -> tab.setText(tabTitles[position]))).attach();



		if(!common.GetBoolPref("radarforecast", true))
			Objects.requireNonNull(tabLayout.getTabAt(2)).setText(R.string.radar);

		try
		{
			if(common.GetStringPref("BASE_URL", "").equals(""))
				mDrawerLayout.openDrawer(Gravity.START);
		} catch (Exception e) {
			e.printStackTrace();
		}

	    settingsURL = findViewById(R.id.settings);
	    customURL = findViewById(R.id.customURL);
	    s1 = findViewById(R.id.spinner1);

	    metric_forecasts = findViewById(R.id.metric_forecasts);
	    show_indoor = findViewById(R.id.show_indoor);
	    s2 = findViewById(R.id.spinner2);

	    b1 = findViewById(R.id.button);
	    b2 = findViewById(R.id.deleteData);
	    b3 = findViewById(R.id.aboutButton);

	    fgColour = findViewById(R.id.fgPicker);
	    bgColour = findViewById(R.id.bgPicker);

	    tv = findViewById(R.id.aboutText);

	    Thread t = new Thread(() ->
	    {
		    try
		    {
			    // Sleep needed to stop frames dropping while loading
			    Thread.sleep(500);
		    } catch (Exception e) {
			    e.printStackTrace();
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
		d.setIcon(R.drawable.ic_launcher_foreground);
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
	    adapter.setDropDownViewResource(R.layout.spinner_layout);
	    s1.setAdapter(adapter);
	    s1.setOnItemSelectedListener(this);
	    pos = common.GetIntPref("updateInterval", 1);
	    s1.setSelection(pos);

	    SwitchCompat wifi_only = findViewById(R.id.wifi_only);
	    wifi_only.setChecked(common.GetBoolPref("onlyWIFI", false));
	    SwitchCompat use_icons = findViewById(R.id.use_icons);
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
	    adapter2.setDropDownViewResource(R.layout.spinner_layout);
	    s2.setAdapter(adapter2);
	    s2.setOnItemSelectedListener(this);
	    theme = common.GetIntPref("dark_theme", 2);
	    s2.setSelection(theme);

	    RadioButton showRadar = findViewById(R.id.showRadar);
	    showRadar.setChecked(common.GetBoolPref("radarforecast", true));
	    RadioButton showForecast = findViewById(R.id.showForecast);
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
		    dialog = ProgressDialog.show(common.context, getString(R.string.testing_urls), getString(R.string.please_wait_verify_url), false);
		    dialog.show();

		    processSettings();
	    });

	    b2.setOnClickListener(arg0 -> checkReally());

	    b3.setOnClickListener(arg0 ->
	    {
		    if(showSettings)
		    {
			    showSettings = false;
			    b1.setVisibility(View.INVISIBLE);
			    b2.setVisibility(View.INVISIBLE);
			    b3.setText(R.string.settings2);

			    LinearLayout settingsLayout = findViewById(R.id.settingsLayout);
			    settingsLayout.setVisibility(View.GONE);
			    LinearLayout aboutLayout = findViewById(R.id.aboutLayout);
			    aboutLayout.setVisibility(View.VISIBLE);
		    } else {
			    showSettings = true;
			    b1.setVisibility(View.VISIBLE);
			    b2.setVisibility(View.VISIBLE);
			    b3.setText(R.string.about2);

			    LinearLayout aboutLayout = findViewById(R.id.aboutLayout);
			    aboutLayout.setVisibility(View.GONE);
			    LinearLayout settingsLayout = findViewById(R.id.settingsLayout);
			    settingsLayout.setVisibility(View.VISIBLE);
		    }

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
			    "Forecasts by" +
			    "<a href='https://www.yahoo.com/?ilc=401'>Yahoo!</a>, " +
			    "<a href='https://weatherzone.com.au'>weatherzone</a>, " +
			    "<a href='https://hjelp.yr.no/hc/en-us/articles/360001940793-Free-weather-data-service-from-Yr'>yr.no</a>, " +
			    "<a href='https://bom.gov.au'>Bureau of Meteorology</a>, " +
			    "<a href='https://www.weather.gov'>Weather.gov</a>, " +
			    "<a href='https://worldweather.wmo.int/en/home.html'>World Meteorology Organisation</a>, " +
			    "<a href='https://weather.gc.ca'>Environment Canada</a>, " +
			    "<a href='https://www.metoffice.gov.uk'>UK Met Office</a>, " +
			    "<a href='https://www.aemet.es'>La Agencia Estatal de Meteorología</a>, " +
			    "<a href='https://www.dwd.de'>Deutscher Wetterdienst</a>, " +
			    "<a href='https://metservice.com'>MetService.com</a>, " +
			    "<a href='https://meteofrance.com'>MeteoFrance.com</a>, " +
			    "<br><br>" +
			    "weeWX Weather App v" + common.getAppversion() + " is by <a href='https://odiousapps.com'>OdiousApps</a>.</body</html>";

	    tv.setText(Html.fromHtml(lines));
	    tv.setMovementMethod(LinkMovementMethod.getInstance());

	    // https://github.com/Pes8/android-material-color-picker-dialog

	    String hex = "#" + Integer.toHexString(common.GetIntPref("fgColour", 0xFF000000)).toUpperCase();
	    fgColour.setText(hex);
	    fgColour.setOnClickListener(v -> showPicker(common.GetIntPref("fgColour", 0xFF000000),true));

	    hex = "#" + Integer.toHexString(common.GetIntPref("bgColour", 0xFFFFFFFF)).toUpperCase();
	    bgColour.setText(hex);
	    bgColour.setOnClickListener(v -> showPicker(common.GetIntPref("bgColour", 0xFFFFFFFF),false));
    }

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

	private void hideKeyboard(View view)
	{
		InputMethodManager inputMethodManager = (InputMethodManager)common.context.getSystemService(Activity.INPUT_METHOD_SERVICE);
		if(inputMethodManager != null)
			inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	public void onItemSelected(AdapterView<?> parent, View v, int position, long id)
	{
		Common.LogMessage("Before -> pos="+pos+",theme="+theme);

		if(parent.getId() == R.id.spinner1)
			pos = position;
		else if(parent.getId() == R.id.spinner2)
			theme = position;

		Common.LogMessage("After -> pos="+pos+",theme="+theme);
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) { }

	private void checkReally()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(common.context);
		builder.setMessage(getString(R.string.remove_all_data)).setCancelable(false)
				.setPositiveButton(getString(R.string.ok), (dialoginterface, i) ->
				{
					Common.LogMessage("trash all data");

					//common.RemovePref("SETTINGS_URL");
					common.RemovePref("updateInterval");
					common.RemovePref("BASE_URL");
					common.RemovePref("radtype");
					common.RemovePref("RADAR_URL");
					common.RemovePref("FORECAST_URL");
					common.RemovePref("fctype");
					common.RemovePref("WEBCAM_URL");
					common.RemovePref("CUSTOM_URL");
					common.RemovePref("custom_url");
					common.RemovePref("metric");
					common.RemovePref("showIndoor");
					common.RemovePref("rssCheck");
					common.RemovePref("forecastData");
					common.RemovePref("LastDownload");
					common.RemovePref("LastDownloadTime");
					common.RemovePref("radarforecast");
					common.RemovePref("seekBar");
					common.RemovePref("fgColour");
					common.RemovePref("bgColour");
					common.RemovePref("bomtown");
					common.RemovePref("metierev");
					common.RemovePref("dark_theme");
					common.RemovePref("use_icons");
					common.commit();

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

					dialoginterface.cancel();

					System.exit(0);
				}).setNegativeButton(getString(R.string.no), (dialoginterface, i) -> dialoginterface.cancel());

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

			SwitchCompat metric_forecasts = findViewById(R.id.metric_forecasts);
			SwitchCompat show_indoor = findViewById(R.id.show_indoor);
			SwitchCompat wifi_only = findViewById(R.id.wifi_only);
			SwitchCompat use_icons = findViewById(R.id.use_icons);

			RadioButton showRadar = findViewById(R.id.showRadar);
			long curtime = Math.round(System.currentTimeMillis() / 1000.0);

			if(use_icons.isChecked() && (common.GetLongPref("icon_version", 0) < Common.icon_version || !common.checkForImages()))
			{
				try
				{
					if (!common.downloadIcons())
					{
						common.SetStringPref("lastError", getString(R.string.icons_failed_to_download));
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

			if (settingsURL.getText().toString().equals("https://example.com/weewx/inigo-settings.txt") || settingsURL.getText().toString().equals(""))
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
					mb[0] = mb[0].trim().toLowerCase();
					if (mb[0].equals("data"))
						data = mb[1];
					if (mb[0].equals("radtype"))
						radtype = mb[1].toLowerCase();
					if (mb[0].equals("radar"))
						radar = mb[1];
					if (mb[0].equals("fctype"))
						fctype = mb[1].toLowerCase();
					if (mb[0].equals("forecast"))
						forecast = mb[1];
					if (mb[0].equals("webcam"))
						webcam = mb[1];
					if (mb[0].equals("custom"))
						custom = mb[1];
				}

				if(fctype.equals(""))
					fctype = "Yahoo";

				if(radtype.equals(""))
					radtype = "image";

				validURL = true;
			} catch (Exception e) {
				common.SetStringPref("lastError", e.toString());
				e.printStackTrace();
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

			if (data.equals(""))
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
					e.printStackTrace();
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

			if (!radar.equals("") && !radar.equals(oldradar))
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
					e.printStackTrace();
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

			if(!forecast.equals(""))
			{
				try
				{
					switch (fctype.toLowerCase())
					{
						case "yahoo":
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
							if(!forecast.startsWith("http"))
							{
								common.SetStringPref("lastError", "Yahoo API recently changed, you need to update your settings.");
								runOnUiThread(() -> {
									b1.setEnabled(true);
									b2.setEnabled(true);
									dialog.dismiss();
									new AlertDialog.Builder(common.context)
											.setTitle("Wasn't able to connect or download the forecast.")
											.setMessage(common.GetStringPref("lastError", getString(R.string.unknown_error_occurred)))
											.setPositiveButton(getString(R.string.ill_fix_and_try_again), (dialog, which) ->
											{
											}).show();
								});
								return;
							}
							break;
						case "weatherzone":
							forecast = "https://rss.weatherzone.com.au/?u=12994-1285&lt=aploc&lc=" + forecast + "&obs=0&fc=1&warn=0";
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
							break;
						case "yr.no":
						case "met.no":
						case "weather.gc.ca":
						case "weather.gc.ca-fr":
						case "metoffice.gov.uk":
						case "bom2":
						case "aemet.es":
						case "dwd.de":
						case "tempoitalia.it":
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
							break;
						case "bom.gov.au":
							common.SetStringPref("lastError", "Forecast type " + fctype + " is no longer supported due to ftp support being dropped in Android. Use bom2 forecasts instead, check the wiki for details.");
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
						case "wmo.int":
							if(!forecast.startsWith("http"))
								forecast = "https://worldweather.wmo.int/en/json/" + forecast.trim() + "_en.xml";
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
							break;
						case "weather.gov":
							String lat = "", lon = "";

							if(forecast.contains("?"))
								forecast = forecast.split("\\?", 2)[1].trim();

							if(forecast.contains("lat") && forecast.contains("lon"))
							{
								String[] tmp = forecast.split("&");
								for(String line : tmp)
								{
									if(line.split("=", 2)[0].equals("lat"))
										lat = line.split("=", 2)[1].trim();
									if(line.split("=", 2)[0].equals("lon"))
										lon = line.split("=", 2)[1].trim();
								}
							} else {
								lat = forecast.split(",")[0].trim();
								lon = forecast.split(",")[1].trim();
							}

							forecast = "https://forecast.weather.gov/MapClick.php?lat=" + lat + "&lon=" + lon + "&unit=0&lg=english&FcstType=json";
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
							break;
						case "bom3":
							forecast = "https://api.weather.bom.gov.au/v1/locations/" + forecast.trim() + "/forecasts/daily";
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
							break;
						case "metservice.com":
							forecast = "https://www.metservice.com/publicData/localForecast" + forecast;
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
							break;
						case "openweathermap.org":
							if(metric_forecasts.isChecked())
								forecast += "&units=metric";
							else
								forecast += "&units=imperial";
							forecast += "&lang=" + Locale.getDefault().getLanguage();
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
							break;
						case "weather.com":
							forecast = "https://api.weather.com/v3/wx/forecast/daily/5day?geocode=" + forecast + "&format=json&apiKey=d522aa97197fd864d36b418f39ebb323";
							//forecast = "https://api.weather.com/v2/turbo/vt1dailyForecast?apiKey=d522aa97197fd864d36b418f39ebb323&format=json&geocode=" + forecast + "&language=en-US";
							if(metric_forecasts.isChecked())
								forecast += "&units=m";
							else
								forecast += "&units=e";
							forecast += "&language=" + Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry();
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
							break;
						case "met.ie":
							metierev = "https://prodapi.metweb.ie/location/reverse/" + forecast.replaceAll(",", "/");
							forecast = "https://prodapi.metweb.ie/weather/daily/" + forecast.replaceAll(",", "/") + "/10";
							if(common.GetStringPref("metierev", "").equals("") || !forecast.equals(oldforecast))
							{
								metierev = common.downloadForecast(fctype, metierev, null);
								JSONObject jobj = new JSONObject(metierev);
								metierev = jobj.getString("city") + ", Ireland";
								common.SetStringPref("metierev", metierev);
							}
							Common.LogMessage("forecast=" + forecast);
							Common.LogMessage("fctype=" + fctype);
							Common.LogMessage("metierev=" + metierev);
							break;
						default:
							common.SetStringPref("lastError", String.format(getString(R.string.forecast_type_is_invalid), fctype));
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
				} catch (Exception e) {
					common.SetStringPref("lastError", e.toString());
					e.printStackTrace();
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

			if (!forecast.equals("") && !oldforecast.equals(forecast))
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
						common.SetLongPref("rssCheck", curtime);
						common.SetStringPref("forecastData", tmp);
					}
				} catch (Exception e) {
					common.SetStringPref("lastError", e.toString());
					e.printStackTrace();
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

			if (!webcam.equals("") && !webcam.equals(oldwebcam))
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

			custom_url = customURL.getText().toString();

			if(custom_url.equals(""))
			{
				if (!custom.equals("") && !custom.equals("https://example.com/mobile.html") && !custom.equals(oldcustom))
				{
					try
					{
						if(common.checkURL(custom))
							validURL5 = true;
						else
							common.RemovePref("custom_url");
					} catch (Exception e) {
						common.SetStringPref("lastError", e.toString());
						e.printStackTrace();
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
						e.printStackTrace();
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

			if(forecast.equals(""))
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

	@Override
    public void onBackPressed()
    {
	    if(mDrawerLayout.isDrawerOpen(GravityCompat.START))
	    {
		    mDrawerLayout.closeDrawer(GravityCompat.START);
	    } else {
		    super.onBackPressed();
		    Common.LogMessage("finishing up.");
		    finish();
	    }
    }

    @Override
    public void onPause()
    {
	    unregisterReceiver(serviceReceiver);
	    super.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

	    IntentFilter filter = new IntentFilter();
	    filter.addAction(Common.UPDATE_INTENT);
	    filter.addAction(Common.FAILED_INTENT);
	    filter.addAction(Common.TAB0_INTENT);
	    filter.addAction(Common.INIGO_INTENT);
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

				if(action != null && action.equals(Common.UPDATE_INTENT))
	            {
		            String hex = "#" + Integer.toHexString(common.GetIntPref("fgColour", 0xFF000000)).toUpperCase();
		            fgColour.setText(hex);
		            hex = "#" + Integer.toHexString(common.GetIntPref("bgColour", 0xFFFFFFFF)).toUpperCase();
		            bgColour.setText(hex);
	            }

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
                e.printStackTrace();
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