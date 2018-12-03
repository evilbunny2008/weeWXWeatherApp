package com.odiousapps.weewxweather;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.pes.androidmaterialcolorpickerdialog.ColorPicker;
import com.pes.androidmaterialcolorpickerdialog.ColorPickerCallback;

import java.io.File;
import java.net.URLEncoder;

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
	private Switch show_indoor, metric_forecasts, dark_theme;
	private TextView tv;

	private ProgressDialog dialog;

	private static int pos;
	private static final String[] paths = {"Manual Updates", "Every 5 Minutes", "Every 10 Minutes", "Every 15 Minutes", "Every 30 Minutes", "Every Hour"};

	private boolean hasPermission = false;
	int permsRequestCode = 200;

	@Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        common = new Common(this);

	    mDrawerLayout = findViewById(R.id.drawer_layout);

        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        tabLayout = findViewById(R.id.tabs);

	    if(!common.GetBoolPref("radarforecast", true))
		    //noinspection ConstantConditions
		    tabLayout.getTabAt(2).setText(R.string.radar);

	    mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

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
	    ArrayAdapter<String> adapter = new ArrayAdapter<>(common.context, R.layout.spinner_layout, paths);
	    adapter.setDropDownViewResource(R.layout.spinner_layout);
	    s1.setAdapter(adapter);
	    s1.setOnItemSelectedListener(this);

	    metric_forecasts = findViewById(R.id.metric_forecasts);
	    show_indoor = findViewById(R.id.show_indoor);
	    dark_theme = findViewById(R.id.dark_theme);

	    b1 = findViewById(R.id.button);
	    b2 = findViewById(R.id.deleteData);
	    b3 = findViewById(R.id.aboutButton);

	    fgColour = findViewById(R.id.fgPicker);
	    bgColour = findViewById(R.id.bgPicker);

	    b1.setOnClickListener(new View.OnClickListener()
	    {
		    public void onClick(View arg0)
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
			    dialog = ProgressDialog.show(common.context, "Testing submitted URLs", "Please wait while we verify the URL you submitted.", false);
			    dialog.show();

			    processSettings();
		    }
	    });

	    b2.setOnClickListener(new View.OnClickListener()
	    {
		    public void onClick(View arg0)
		    {
			    checkReally();
		    }
	    });

	    b3.setOnClickListener(new View.OnClickListener()
	    {
		    public void onClick(View arg0)
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

		    }
	    });

	    tv = findViewById(R.id.aboutText);
		doSettings();
		doPerms();

	    common.setAlarm("MainActivity");
    }

	private void doPerms()
	{
		if(Build.VERSION.SDK_INT < 23)
		{
			hasPermission = true;
		} else {
			if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
			{
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, permsRequestCode);
			} else {
				hasPermission = true;
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		hasPermission = false;
		if (requestCode == permsRequestCode)
		{
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
			{
				hasPermission = true;
			}
		}

		if (!hasPermission)
		{
			Toast.makeText(this, "Can't continue without being able to access shared storage.", Toast.LENGTH_LONG).show();
			finish();
		}
	}

	private void showUpdateAvailable()
	{
		final AlertDialog.Builder d = new AlertDialog.Builder(this);
		d.setTitle("weeWX Weather App");
		d.setMessage("This app has been updated but the server you are connecting to hasn't updated the Inigo Plugin for weeWX. Fields may not show up properly until weeWX is updated.");
		d.setPositiveButton("OK", null);
		d.setIcon(R.drawable.ic_launcher_foreground);
		d.show();
	}

    private void doSettings()
    {
	    settingsURL.setText(common.GetStringPref("SETTINGS_URL", "https://example.com/weewx/inigo-settings.txt"));
	    settingsURL.setOnFocusChangeListener(new View.OnFocusChangeListener()
	    {
		    @Override
		    public void onFocusChange(View v, boolean hasFocus)
		    {
			    if (!hasFocus)
				    hideKeyboard(v);
		    }
	    });

	    customURL.setText(common.GetStringPref("custom_url", ""));
	    customURL.setOnFocusChangeListener(new View.OnFocusChangeListener()
	    {
		    @Override
		    public void onFocusChange(View v, boolean hasFocus)
		    {
			    if (!hasFocus)
				    hideKeyboard(v);
		    }
	    });

	    pos = common.GetIntPref("updateInterval", 1);
	    s1.setSelection(pos);

	    metric_forecasts.setChecked(common.GetBoolPref("metric", true));
	    show_indoor.setChecked(common.GetBoolPref("showIndoor", false));
	    dark_theme.setChecked(common.GetBoolPref("dark_theme", false));

	    Switch wifi_only = findViewById(R.id.wifi_only);
	    wifi_only.setChecked(common.GetBoolPref("onlyWIFI", false));

	    boolean radarforecast = common.GetBoolPref("radarforecast", true);
	    RadioButton showForecast = findViewById(R.id.showForecast);
	    if(!radarforecast)
		    showForecast.setChecked(true);

	    LinearLayout settingsLayout = findViewById(R.id.settingsLayout);
	    settingsLayout.setVisibility(View.VISIBLE);
	    LinearLayout aboutLayout = findViewById(R.id.aboutLayout);
	    aboutLayout.setVisibility(View.GONE);

	    String lines = "<html><body>Big thanks to the <a href='http://weewx.com'>weeWX project</a>, as this app " +
			    "wouldn't be possible otherwise.<br><br>" +
			    "Weather Icons from <a href='https://www.flaticon.com/'>FlatIcon</a> and " +
			    "is licensed under <a href='http://creativecommons.org/licenses/by/3.0/'>CC 3.0 BY</a><br><br>" +
			    "Forecasts supplied by <a href='https://www.yahoo.com/?ilc=401'>Yahoo!</a>, <a href='https://weatherzone.com.au'>weatherzone</a>, " +
			    "<a href='https://hjelp.yr.no/hc/en-us/articles/360001940793-Free-weather-data-service-from-Yr'>yr.no</a>, " +
			    "<a href='https://bom.gov.au'>Bureau of Meteorology</a>, <a href='https://www.weather.gov'>Weather.gov</a>, " +
			    "<a href='https://worldweather.wmo.int/en/home.html'>World Meteorology Organisation</a> and " +
			    "<a href='https://weather.gc.ca'>Environment Canada</a>" +
			    "<br><br>" +
			    "weeWX Weather App v" + common.getAppversion() + " is by <a href='https://odiousapps.com'>OdiousApps</a>.</body</html>";

	    tv.setText(Html.fromHtml(lines));
	    tv.setMovementMethod(LinkMovementMethod.getInstance());

	    // https://github.com/Pes8/android-material-color-picker-dialog

	    String hex = "#" + Integer.toHexString(common.GetIntPref("fgColour", 0xFF000000)).toUpperCase();
	    fgColour.setText(hex);
	    fgColour.setOnClickListener(new View.OnClickListener()
	    {
		    @Override
		    public void onClick(View v)
		    {
			    showPicker(common.GetIntPref("fgColour", 0xFF000000),true);
		    }
	    });

	    hex = "#" + Integer.toHexString(common.GetIntPref("bgColour", 0xFFFFFFFF)).toUpperCase();
	    bgColour.setText(hex);
	    bgColour.setOnClickListener(new View.OnClickListener()
	    {
		    @Override
		    public void onClick(View v)
		    {
			    showPicker(common.GetIntPref("bgColour", 0xFFFFFFFF),false);
		    }
	    });
    }

	private void showPicker(int col, final boolean fgColour)
    {
	    final ColorPicker cp = new ColorPicker(MainActivity.this, col >> 24 & 255, col >> 16 & 255, col >> 8 & 255, col & 255);

	    cp.setCallback(new ColorPickerCallback()
	    {
		    @Override
		    public void onColorChosen(@ColorInt int colour)
		    {
			    Common.LogMessage("Pure Hex" + Integer.toHexString(colour));

			    if(fgColour)
				    common.SetIntPref("fgColour", colour);
			    else
				    common.SetIntPref("bgColour", colour);

			    common.SendIntents();

			    cp.dismiss();
		    }
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
		pos = position;
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) { }

	private void checkReally()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(common.context);
		builder.setMessage("Are you sure you want to remove all data?").setCancelable(false)
				.setPositiveButton("Yes", new android.content.DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialoginterface, int i)
					{
						Common.LogMessage("trash all data");

						common.RemovePref("SETTINGS_URL");
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
						common.RemovePref("dark_theme");
						common.commit();

						File file = new File(common.context.getFilesDir(), "webcam.jpg");
						if(file.exists() && file.canWrite())
							if(!file.delete())
								Common.LogMessage("couldn't delete webcam.jpg");

						file = new File(common.context.getFilesDir(), "radar.gif");
						if(file.exists() && file.canWrite())
							if(!file.delete())
								Common.LogMessage("couldn't delete radar.gif");

						RemoteViews remoteViews = common.buildUpdate(common.context);
						ComponentName thisWidget = new ComponentName(common.context, WidgetProvider.class);
						AppWidgetManager manager = AppWidgetManager.getInstance(common.context);
						manager.updateAppWidget(thisWidget, remoteViews);
						Common.LogMessage("widget intent broadcasted");

						dialoginterface.cancel();

						System.exit(0);
					}
				}).setNegativeButton("No", new android.content.DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialoginterface, int i)
			{
				dialoginterface.cancel();
			}
		});

		builder.create().show();

	}

	private void processSettings()
	{
		Thread t = new Thread(new Runnable()
		{
			@SuppressWarnings("ConstantConditions")
			@Override
			public void run()
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

				String data = "", radtype = "", radar = "", forecast = "", webcam = "", custom = "", custom_url, fctype = "", bomtown = "";

				Switch metric_forecasts = findViewById(R.id.metric_forecasts);
				Switch show_indoor = findViewById(R.id.show_indoor);
				Switch wifi_only = findViewById(R.id.wifi_only);

				RadioButton showRadar = findViewById(R.id.showRadar);
				int curtime = Math.round(System.currentTimeMillis() / 1000);

				if (settingsURL.getText().toString().equals("https://example.com/weewx/inigo-settings.txt") || settingsURL.getText().toString().equals(""))
				{
					common.SetStringPref("lastError", "URL was set to the default or was empty");
					handlerSettings.sendEmptyMessage(0);
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

					if(fctype == null || fctype.equals(""))
						fctype = "Yahoo";

					if(radtype == null || radtype.equals(""))
						radtype = "image";

					validURL = true;
				} catch (Exception e) {
					common.SetStringPref("lastError", e.toString());
					e.printStackTrace();
				}

				if (!validURL)
				{
					handlerSettings.sendEmptyMessage(0);
					return;
				}

				Common.LogMessage("data == " + data);

				if (data.equals(""))
				{
					common.SetStringPref("lastError", "Data url was blank");
					handlerDATA.sendEmptyMessage(0);
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
					handlerDATA.sendEmptyMessage(0);
					return;
				}

				if (!radar.equals("") && !radar.equals(oldradar))
				{
					try
					{
						if(radtype.equals("image"))
						{
							File f = common.downloadRADAR(radar);
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
						handlerRADAR.sendEmptyMessage(0);
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
								forecast = URLEncoder.encode(forecast, "utf-8");
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);

								if (metric_forecasts.isChecked())
									forecast = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20woeid%20in%20(select%20woeid%20from%20geo.places(1)%20where%20text%3D%22" + forecast + "%22)%20and%20u%3D'c'&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
								else
									forecast = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20woeid%20in%20(select%20woeid%20from%20geo.places(1)%20where%20text%3D%22" + forecast + "%22)%20and%20u%3D'f'&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
								break;
							case "weatherzone":
								forecast = "https://rss.weatherzone.com.au/?u=12994-1285&lt=aploc&lc=" + forecast + "&obs=0&fc=1&warn=0";
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "yr.no":
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "bom.gov.au":
								bomtown = forecast.split(",")[1].trim();
								common.SetStringPref("bomtown", bomtown);
								forecast = "ftp://ftp.bom.gov.au/anon/gen/fwo/" + forecast.split(",")[0].trim() + ".xml";
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								Common.LogMessage("bomtown=" + bomtown);
								break;
							case "wmo.int":
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
									String[] tmp = forecast.split("&", 2);
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
							case "weather.gc.ca":
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "metoffice.gov.uk":
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "bom2":
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							default:
								common.SetStringPref("lastError", "forecast type " + fctype + " is invalid, check your settings file and try again.");
								handlerForecast.sendEmptyMessage(0);
								return;
						}
					} catch (Exception e) {
						common.SetStringPref("lastError", e.toString());
						e.printStackTrace();
					}
				}

				Common.LogMessage("line 654");

				if (!forecast.equals("") && !forecast.equals(oldforecast))
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
							common.SetIntPref("rssCheck", curtime);
							common.SetStringPref("forecastData", tmp);
						}
					} catch (Exception e) {
						common.SetStringPref("lastError", e.toString());
						e.printStackTrace();
					}

					if (!validURL3)
					{
						handlerForecast.sendEmptyMessage(0);
						return;
					}
				}

				if (!webcam.equals("") && !webcam.equals(oldwebcam))
				{
					Common.LogMessage("checking: " + webcam);

					if (!Webcam.downloadWebcam(webcam, common.context.getFilesDir()))
					{
						handlerWEBCAM.sendEmptyMessage(0);
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
							handlerCUSTOM.sendEmptyMessage(0);
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
							handlerCUSTOM_URL.sendEmptyMessage(0);
							return;
						}
					}
				}

				if(forecast.equals(""))
				{
					common.SetIntPref("rssCheck", 0);
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
				common.SetBoolPref("dark_theme", dark_theme.isChecked());
				common.SetBoolPref("onlyWIFI", wifi_only.isChecked());

				common.SendRefresh();
				handlerDone.sendEmptyMessage(0);
			}
		});

		t.start();
	}

	@SuppressLint("HandlerLeak")
	private Handler handlerDone = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
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
		}
	};

	@SuppressLint("HandlerLeak")
	private Handler handlerSettings = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			b1.setEnabled(true);
			b2.setEnabled(true);
			dialog.dismiss();
			new AlertDialog.Builder(common.context)
					.setTitle("Wasn't able to connect or download the settings from your server")
					.setMessage(common.GetStringPref("lastError", "Unknown error occurred"))
					.setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
						}
					}).show();
		}
	};

	@SuppressLint("HandlerLeak")
	private Handler handlerDATA = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			b1.setEnabled(true);
			b2.setEnabled(true);
			dialog.dismiss();
			new AlertDialog.Builder(common.context)
					.setTitle("Wasn't able to connect or download data.txt from your server")
					.setMessage(common.GetStringPref("lastError", "Unknown error occurred"))
					.setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
						}
					}).show();
		}
	};

	@SuppressLint("HandlerLeak")
	private Handler handlerRADAR = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			b1.setEnabled(true);
			b2.setEnabled(true);
			dialog.dismiss();
			new AlertDialog.Builder(common.context)
					.setTitle("Wasn't able to connect or download radar= image from the internet")
					.setMessage(common.GetStringPref("lastError", "Unknown error occurred"))
					.setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
						}
					}).show();
		}
	};

	@SuppressLint("HandlerLeak")
	private Handler handlerForecast = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			b1.setEnabled(true);
			b2.setEnabled(true);
			dialog.dismiss();
			new AlertDialog.Builder(common.context)
					.setTitle("Wasn't able to connect or download the forecast from Yahoo.")
					.setMessage(common.GetStringPref("lastError", "Unknown error occurred"))
					.setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
						}
					}).show();
		}
	};

	@SuppressLint("HandlerLeak")
	private Handler handlerWEBCAM = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			b1.setEnabled(true);
			b2.setEnabled(true);
			dialog.dismiss();
			new AlertDialog.Builder(common.context)
					.setTitle("Wasn't able to connect or download a webcam image from your server")
					.setMessage(common.GetStringPref("lastError", "Unknown error occurred"))
					.setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
						}
					}).show();
		}
	};

	@SuppressLint("HandlerLeak")
	private Handler handlerCUSTOM = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			b1.setEnabled(true);
			b2.setEnabled(true);
			dialog.dismiss();
			new AlertDialog.Builder(common.context)
					.setTitle("Wasn't able to connect or download from the custom URL specified")
					.setMessage(common.GetStringPref("lastError", "Unknown error occurred"))
					.setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
						}
					}).show();
		}
	};

	@SuppressLint("HandlerLeak")
	private Handler handlerCUSTOM_URL = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			b1.setEnabled(true);
			b2.setEnabled(true);
			dialog.dismiss();
			new AlertDialog.Builder(common.context)
					.setTitle("Wasn't able to connect or download from the custom URL specified")
					.setMessage(common.GetStringPref("lastError", "Unknown error occurred"))
					.setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
						}
					}).show();
		}
	};

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

                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
	                        //noinspection ConstantConditions
	                        tabLayout.getTabAt(0).select();
                        }
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
		            runOnUiThread(new Runnable()
		            {
			            @Override
			            public void run()
			            {
				            //swipeLayout.setRefreshing(false);
				            new AlertDialog
						            .Builder(common.context)
						            .setTitle("An error occurred while attempting to update usage")
						            .setMessage(common.GetStringPref("lastError", "Unknown error occurred"))
						            .setPositiveButton("Ok", new DialogInterface.OnClickListener()
						            {
							            @Override
							            public void onClick(DialogInterface dialog, int which)
							            {
							            }
						            }).show();
			            }
		            });

	            }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public static class PlaceholderFragment extends Fragment
    {
        private static final String ARG_SECTION_NUMBER = "section_number";
        private int lastPos = 0;
        private Weather weather;
        private Stats stats;
        private Forecast forecast;
        private Webcam webcam;
        private Custom custom;

        public PlaceholderFragment() {}

        public static PlaceholderFragment newInstance(int sectionNumber)
        {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onPause()
        {
        	super.onPause();
	        switch(lastPos)
	        {
		        case 1:
			        weather.doPause();
			        break;
		        case 2:
			        stats.doPause();
			        break;
		        case 3:
			        forecast.doPause();
			        break;
		        case 4:
			        webcam.doPause();
			        break;
		        case 5:
			        custom.doPause();
			        break;
	        }

	        Common.LogMessage("onPause() has been called lastpos ="+lastPos);
        }

        @Override
	    public void onResume()
	    {
		    super.onResume();
		    switch(lastPos)
		    {
			    case 1:
				    weather.doResume();
				    break;
			    case 2:
				    stats.doResume();
				    break;
			    case 3:
				    forecast.doResume();
				    break;
			    case 4:
				    webcam.doResume();
				    break;
			    case 5:
				    custom.doResume();
				    break;
		    }

		    Common.LogMessage("onResume() has been called lastpos ="+lastPos);
	    }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
        	if(container == null)
        		return null;

	        Common common = new Common(getContext());

	        Bundle args = getArguments();

	        if(args != null)
	        {
		        lastPos = args.getInt(ARG_SECTION_NUMBER);

		        if (args.getInt(ARG_SECTION_NUMBER) == 1)
		        {
			        weather = new Weather(common);
			        return weather.myWeather(inflater, container);
		        } else if (args.getInt(ARG_SECTION_NUMBER) == 2) {
			        stats = new Stats(common);
			        return stats.myStats(inflater, container);
		        } else if (args.getInt(ARG_SECTION_NUMBER) == 3) {
			        forecast = new Forecast(common);
			        return forecast.myForecast(inflater, container);
		        } else if (args.getInt(ARG_SECTION_NUMBER) == 4) {
			        webcam = new Webcam(common);
			        return webcam.myWebcam(inflater, container);
		        } else if (args.getInt(ARG_SECTION_NUMBER) == 5) {
			        custom = new Custom(common);
			        return custom.myCustom(inflater, container);
		        }
	        }
            return null;
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount()
        {
            return 5;
        }
    }
}