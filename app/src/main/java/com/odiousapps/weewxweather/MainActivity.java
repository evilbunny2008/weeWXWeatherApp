package com.odiousapps.weewxweather;

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
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.TextView;

import com.pes.androidmaterialcolorpickerdialog.ColorPicker;
import com.pes.androidmaterialcolorpickerdialog.ColorPickerCallback;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
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

	private ProgressDialog dialog;

	private static int pos;
	private static final String[] paths = {"Manual Updates", "Every 5 Minutes", "Every 10 Minutes", "Every 15 Minutes", "Every 30 Minutes", "Every Hour"};

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

        if(myService.singleton == null)
            startService(new Intent(this, myService.class));

        IntentFilter filter = new IntentFilter();
        filter.addAction(myService.UPDATE_INTENT);
        filter.addAction(myService.TAB0_INTENT);
        filter.addAction(myService.INIGO_INTENT);
        registerReceiver(serviceReceiver, filter);

        doSettings();
    }

    private void showUpdateAvailable()
	{
		final AlertDialog.Builder d = new AlertDialog.Builder(this);
		d.setTitle("weeWx Weather App");
		d.setMessage("This app has been updated but the server you are connecting to hasn't updated the Inigo Plugin for weeWx. Fields may not show up properly until weeWx is updated.");
		d.setPositiveButton("OK", null);
		d.setIcon(R.drawable.ic_launcher_foreground);
		d.show();
	}

    private void doSettings()
    {
	    settingsURL = findViewById(R.id.settings);
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

	    customURL = findViewById(R.id.customURL);
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

	    Spinner s1 = findViewById(R.id.spinner1);
	    ArrayAdapter<String> adapter = new ArrayAdapter<>(common.context, R.layout.spinner_layout, paths);
	    adapter.setDropDownViewResource(R.layout.spinner_layout);
	    s1.setAdapter(adapter);
	    s1.setSelection(common.GetIntPref("updateInterval", 1));
	    s1.setOnItemSelectedListener(this);

	    CheckBox cb1 = findViewById(R.id.cb1);
	    if(!common.GetBoolPref("bgdl", true))
		    cb1.setChecked(false);

	    boolean metric = common.GetBoolPref("metric", true);
	    CheckBox cb2 = findViewById(R.id.cb2);
	    if(!metric)
		    cb2.setChecked(false);

	    boolean showIndoor = common.GetBoolPref("showIndoor", false);
	    CheckBox cb3 = findViewById(R.id.showIndoor);
	    if(!showIndoor)
		    cb3.setChecked(false);

	    boolean radarforecast = common.GetBoolPref("radarforecast", true);
	    RadioButton showForecast = findViewById(R.id.showForecast);
	    if(!radarforecast)
		    showForecast.setChecked(true);

	    b1 = findViewById(R.id.button);
	    b2 = findViewById(R.id.deleteData);
	    b3 = findViewById(R.id.aboutButton);

	    LinearLayout settingsLayout = findViewById(R.id.settingsLayout);
	    settingsLayout.setVisibility(View.VISIBLE);
	    LinearLayout aboutLayout = findViewById(R.id.aboutLayout);
	    aboutLayout.setVisibility(View.GONE);

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

	    TextView tv = findViewById(R.id.aboutText);

	    String lines = "<html><body>Big thanks to the <a href='http://weewx.com'>weeWx project</a>, as this app " +
			    "wouldn't be possible otherwise.<br><br>" +
			    "Weather Icons from <a href='https://www.flaticon.com/'>FlatIcon</a> and " +
			    "is licensed under <a href='http://creativecommons.org/licenses/by/3.0/'>CC 3.0 BY</a><br><br>" +
			    "weeWx Weather App v" + common.getAppversion() + " is by <a href='https://odiousapps.com'>OdiousApps</a>.</body</html>";

	    tv.setText(Html.fromHtml(lines));
	    tv.setMovementMethod(LinkMovementMethod.getInstance());

	    fgColour = findViewById(R.id.fgPicker);
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

	    bgColour = findViewById(R.id.bgPicker);
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

			    myService.singleton.SendIntents();

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
						common.RemovePref("bgdl");
						common.RemovePref("rssCheck");
						common.RemovePref("forecastData");
						common.RemovePref("LastDownload");
						common.RemovePref("LastDownloadTime");
						common.RemovePref("radarforecast");
						common.RemovePref("seekBar");
						common.RemovePref("fgColour");
						common.RemovePref("bgColour");
						common.commit();

						common.context.stopService(new Intent(common.context, myService.class));

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

				String olddata = common.GetStringPref("BASE_URL", "");
				String oldradar = common.GetStringPref("RADAR_URL", "");
				String oldforecast = common.GetStringPref("FORECAST_URL", "");
				String oldwebcam = common.GetStringPref("WEBCAM_URL", "");
				String oldcustom = common.GetStringPref("CUSTOM_URL", "");
				String oldcustom_url = common.GetStringPref("custom_url", "");

				String data = "", radtype = "", radar = "", forecast = "", webcam = "", custom = "", custom_url, fctype = "";

				CheckBox cb1 = findViewById(R.id.cb1);
				CheckBox cb2 = findViewById(R.id.cb2);
				CheckBox cb3 = findViewById(R.id.showIndoor);

				RadioButton showRadar = findViewById(R.id.showRadar);
				int curtime = Math.round(System.currentTimeMillis() / 1000);

				if (settingsURL.getText().toString().equals("https://example.com/weewx/inigo-settings.txt") || settingsURL.getText().toString().equals(""))
				{
					handlerSettings.sendEmptyMessage(0);
					return;
				}

				try
				{
					Uri uri = Uri.parse(settingsURL.getText().toString());
					Common.LogMessage("inigo-settings.txt == " + settingsURL.getText().toString());
					common.SetStringPref("SETTINGS_URL", settingsURL.getText().toString());
					if (uri.getUserInfo() != null && uri.getUserInfo().contains(":"))
					{
						final String[] UC = uri.getUserInfo().split(":");
						Common.LogMessage("uri username = " + uri.getUserInfo());

						if (UC != null && UC.length > 1)
						{
							Authenticator.setDefault(new Authenticator()
							{
								protected PasswordAuthentication getPasswordAuthentication()
								{
									return new PasswordAuthentication(UC[0], UC[1].toCharArray());
								}
							});
						}
					}

					URL url = new URL(settingsURL.getText().toString());
					URLConnection conn = url.openConnection();
					conn.setDoOutput(true);
					conn.connect();

					BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = in.readLine()) != null)
					{
						line += "\n";
						sb.append(line);
					}
					in.close();

					String[] bits = sb.toString().replaceAll("[^\\p{ASCII}]", "").trim().split("\\n");

					for (String bit : bits)
					{
						String[] mb = bit.split("=", 2);
						mb[0] = mb[0].trim().toLowerCase();
						if (mb[0].equals("data"))
							data = mb[1];
						if(mb[0].equals("radtype"))
							radtype = mb[1].toLowerCase();
						if (mb[0].equals("radar"))
							radar = mb[1];
						if(mb[0].equals("fctype"))
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
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (!validURL)
				{
					handlerSettings.sendEmptyMessage(0);
					return;
				}

				if (data.equals(""))
				{
					handlerDATA.sendEmptyMessage(0);
					return;
				}

				if (!data.equals(olddata))
				{
					try
					{
						Uri uri = Uri.parse(data);
						if (uri.getUserInfo() != null && uri.getUserInfo().contains(":"))
						{
							final String[] UC = uri.getUserInfo().split(":");
							Common.LogMessage("uri username = " + uri.getUserInfo());

							if (UC != null && UC.length > 1)
							{
								Authenticator.setDefault(new Authenticator()
								{
									protected PasswordAuthentication getPasswordAuthentication()
									{
										return new PasswordAuthentication(UC[0], UC[1].toCharArray());
									}
								});
							}
						}

						URL url = new URL(data);
						HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
						urlConnection.setRequestMethod("GET");
						urlConnection.setDoOutput(true);
						urlConnection.connect();

						BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

						StringBuilder sb = new StringBuilder();
						String line;
						while ((line = in.readLine()) != null)
							sb.append(line);
						in.close();

						common.SetStringPref("LastDownload", sb.toString().trim());
						common.SetLongPref("LastDownloadTime", curtime);
						validURL1 = true;
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (Exception e) {
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
							Common.LogMessage("starting to download image from: " + radar);
							URL url = new URL(radar);

							InputStream ins = url.openStream();
							File file = new File(common.context.getFilesDir(), "/radar.gif");
							FileOutputStream out = null;

							try
							{
								out = new FileOutputStream(file);
								final byte[] b = new byte[2048];
								int length;
								while ((length = ins.read(b)) != -1)
									out.write(b, 0, length);
								validURL2 = true;
							} catch (Exception e) {
								e.printStackTrace();
							} finally {
								try
								{
									if (ins != null)
										ins.close();
									if (out != null)
										out.close();
								} catch (IOException e)
								{
									e.printStackTrace();
								}
							}
						} else if(radtype.equals("webpage")) {
							try
							{
								Common.LogMessage("checking: " + radar);
								URL url = new URL(radar);
								URLConnection conn = url.openConnection();
								conn.connect();

								validURL2 = true;
							} catch (MalformedURLException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} catch (Exception e) {
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
						switch (fctype)
						{
							case "yahoo":
								forecast = URLEncoder.encode(forecast, "utf-8");
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);

								if (cb2.isChecked())
									forecast = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20woeid%20in%20(select%20woeid%20from%20geo.places(1)%20where%20text%3D%22" + forecast + "%22)%20and%20u%3D'c'&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
								else
									forecast = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20woeid%20in%20(select%20woeid%20from%20geo.places(1)%20where%20text%3D%22" + forecast + "%22)%20and%20u%3D'f'&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
								break;
							case "weatherzone":
								forecast = "http://rss.weatherzone.com.au/?u=12994-1285&lt=aploc&lc=" + forecast + "&obs=0&fc=1&warn=0";
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							default:
								handlerForecast.sendEmptyMessage(0);
								return;
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				if (!forecast.equals("") && !forecast.equals(oldforecast))
				{
					Common.LogMessage("forecast checking: " + forecast);

					try
					{
						Common.LogMessage("checking: " + forecast);

						URL url = new URL(forecast);
						URLConnection conn = url.openConnection();
						conn.setDoOutput(true);
						conn.connect();
						BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

						String line;
						StringBuilder sb = new StringBuilder();
						while ((line = in.readLine()) != null)
						{
							line = line.trim();
							if (line.length() > 0)
								sb.append(line);
						}
						in.close();

						Common.LogMessage("updating rss cache");
						common.SetIntPref("rssCheck", curtime);
						common.SetStringPref("forecastData", sb.toString().trim());

						validURL3 = true;
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (Exception e) {
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
							Common.LogMessage("checking: " + custom);
							URL url = new URL(custom);
							URLConnection conn = url.openConnection();
							conn.connect();
							common.RemovePref("custom_url");

							validURL5 = true;
						} catch (MalformedURLException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} catch (Exception e) {
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
							Common.LogMessage("checking: " + custom_url);
							URL url = new URL(custom_url);
							URLConnection conn = url.openConnection();
							conn.connect();

							validURL5 = true;
						} catch (MalformedURLException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} catch (Exception e) {
							e.printStackTrace();
						}

						if (!validURL5)
						{
							handlerCUSTOM_URL.sendEmptyMessage(0);
							return;
						}
					}
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
				common.SetBoolPref("metric", cb2.isChecked());
				common.SetBoolPref("showIndoor", cb3.isChecked());
				common.SetBoolPref("bgdl", cb1.isChecked());
				common.SetBoolPref("radarforecast", showRadar.isChecked());

				if(myService.singleton == null)
					startService(new Intent(common.context, myService.class));
				else
					myService.singleton.stopTimer();

				myService.singleton.startTimer();

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
					.setTitle("Invalid URL")
					.setMessage("Wasn't able to connect or download the settings from your server")
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
					.setTitle("Invalid URL")
					.setMessage("Wasn't able to connect or download data.txt from your server")
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
					.setTitle("Invalid URL")
					.setMessage("Wasn't able to connect or download radar= image from the internet")
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
					.setTitle("Invalid URL")
					.setMessage("Wasn't able to connect or download the forecast from Yahoo.")
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
					.setTitle("Invalid URL")
					.setMessage("Wasn't able to connect or download a webcam image from your server")
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
					.setTitle("Invalid URL")
					.setMessage("Wasn't able to connect or download from the custom URL specified")
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
					.setTitle("Invalid URL")
					.setMessage("Wasn't able to connect or download from the custom URL specified")
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
	    if (mDrawerLayout.isDrawerOpen(GravityCompat.START))
	    {
		    mDrawerLayout.closeDrawer(GravityCompat.START);
	    } else {
		    super.onBackPressed();
		    if(common.GetBoolPref("bgdl", true))
		    {
			    Common.LogMessage("Moving task to background");
			    moveTaskToBack(true);
			    Common.LogMessage("app should now be in the bg.");
		    } else {
			    Common.LogMessage("finishing up.");
			    finish();
		    }
	    }
    }

    @Override
    public void onDestroy()
    {
	    super.onDestroy();

	    unregisterReceiver(serviceReceiver);

	    if(!common.GetBoolPref("bgdl", true))
	    {
		    stopService(new Intent(this, myService.class));
		    System.exit(0);
	    }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if(myService.singleton != null)
        {
            Common.LogMessage("pausing app updates");
            myService.singleton.doUpdate = false;
	        if(!common.GetBoolPref("bgdl", true))
	            stopService(new Intent(common.context, myService.class));
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

	    Common.LogMessage("resuming app updates");

	    if(myService.singleton == null)
        {
	        startService(new Intent(common.context, myService.class));
        } else {
	        myService.singleton.doUpdate = true;
	        myService.singleton.SendIntents();
        }
    }

    public void getWeather()
    {
        if(myService.singleton != null)
            myService.singleton.getWeather();
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
                if(action != null && action.equals(myService.TAB0_INTENT))
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

	            if(action != null && action.equals(myService.UPDATE_INTENT))
	            {
		            String hex = "#" + Integer.toHexString(common.GetIntPref("fgColour", 0xFF000000)).toUpperCase();
		            fgColour.setText(hex);
		            hex = "#" + Integer.toHexString(common.GetIntPref("bgColour", 0xFFFFFFFF)).toUpperCase();
		            bgColour.setText(hex);
	            }

	            if(action != null && action.equals(myService.INIGO_INTENT))
	            {
		            showUpdateAvailable();
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
        public void onDestroyView()
        {
            super.onDestroyView();

            switch(lastPos)
            {
                case 1:
                    weather.doStop();
                    break;
                case 2:
                    stats.doStop();
                    break;
                case 3:
                    forecast.doStop();
                    break;
                case 4:
                    webcam.doStop();
                    break;
                case 5:
                    custom.doStop();
                    break;
                case 6:
                    //about.doStop();
                    break;
            }

            Common.LogMessage("onDestroyView() has been called lastpos ="+lastPos);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
        	if(inflater == null || container == null)
        		return null;

	        Common common = new Common(getContext());

	        lastPos = getArguments().getInt(ARG_SECTION_NUMBER);

		    if(getArguments().getInt(ARG_SECTION_NUMBER) == 1) {
                weather = new Weather(common);
                return weather.myWeather(inflater, container);
            } else if(getArguments().getInt(ARG_SECTION_NUMBER) == 2) {
                stats = new Stats(common);
                return stats.myStats(inflater, container);
            } else if(getArguments().getInt(ARG_SECTION_NUMBER) == 3) {
                forecast = new Forecast(common);
                return forecast.myForecast(inflater, container);
            } else if(getArguments().getInt(ARG_SECTION_NUMBER) == 4) {
                webcam = new Webcam(common);
                return webcam.myWebcam(inflater, container);
            } else if(getArguments().getInt(ARG_SECTION_NUMBER) == 5) {
			    custom = new Custom(common);
			    return custom.myCustom(inflater, container);
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