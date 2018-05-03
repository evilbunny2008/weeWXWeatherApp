package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RemoteViews;
import android.widget.Spinner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

class Settings implements AdapterView.OnItemSelectedListener
{
    private Common common;
    private EditText et1;
	private Button b1;

	private ProgressDialog dialog;

    private static int pos;
    private static final String[] paths = {"Manual Updates", "Every 5 Minutes", "Every 10 Minutes", "Every 15 Minutes", "Every 30 Minutes", "Every Hour"};

    Settings(Common common)
    {
        this.common = common;
    }

    View mySettings(LayoutInflater inflater, ViewGroup container)
    {
        final View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        et1 = rootView.findViewById(R.id.settings);
        et1.setText(common.GetStringPref("SETTINGS_URL", "https://example.com/weewx/settings.txt"));
        et1.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
		    @Override
		    public void onFocusChange(View v, boolean hasFocus)
		    {
			    if (!hasFocus)
				    hideKeyboard(v);
		    }
		});

        Spinner s1 = rootView.findViewById(R.id.spinner1);
        ArrayAdapter<String>adapter = new ArrayAdapter<>(common.context, R.layout.spinner_layout, paths);
        adapter.setDropDownViewResource(R.layout.spinner_layout);
        s1.setAdapter(adapter);
        s1.setSelection(common.GetIntPref("updateInterval", 1));
        s1.setOnItemSelectedListener(this);

        boolean bgdl = common.GetBoolPref("bgdl", true);
        CheckBox cb1 = rootView.findViewById(R.id.cb1);
        if(!bgdl)
            cb1.setChecked(false);

        boolean metric = common.GetBoolPref("metric", true);
        CheckBox cb2 = rootView.findViewById(R.id.cb2);
        if(!metric)
            cb2.setChecked(false);

        boolean radarforecast = common.GetBoolPref("radarforecast", true);
	    RadioButton showForecast = rootView.findViewById(R.id.showForecast);
        if(!radarforecast)
        	showForecast.setChecked(true);

        b1 = rootView.findViewById(R.id.button);
        b1.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View arg0)
            {
                b1.setEnabled(false);
                InputMethodManager mgr = (InputMethodManager)common.context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if(mgr != null)
                    mgr.hideSoftInputFromWindow(et1.getWindowToken(), 0);

                Common.LogMessage("show dialog");
                dialog = ProgressDialog.show(common.context, "Testing submitted URLs", "Please wait while we verify the URL you submitted.", false);
                dialog.show();

                processSettings(rootView);
            }
        });

	    Button b2 = rootView.findViewById(R.id.deleteData);
	    b2.setOnClickListener(new View.OnClickListener()
	    {
		    public void onClick(View arg0)
		    {
		    	checkReally();
		    }
	    });

	    return rootView;
    }

	private void hideKeyboard(View view)
	{
		InputMethodManager inputMethodManager =(InputMethodManager)common.context.getSystemService(Activity.INPUT_METHOD_SERVICE);
		if(inputMethodManager != null)
			inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

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
				    common.RemovePref("RADAR_URL");
				    common.RemovePref("FORECAST_URL");
				    common.RemovePref("fctype");
				    common.RemovePref("WEBCAM_URL");
				    common.RemovePref("CUSTOM_URL");
				    common.RemovePref("metric");
				    common.RemovePref("bgdl");
				    common.RemovePref("rssCheck");
				    common.RemovePref("forecastData");
				    common.RemovePref("LastDownload");
				    common.RemovePref("radarforecast");
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

    private void processSettings(final View rootView)
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

			    String data = "", radar = "", forecast = "", webcam = "", custom = "", fctype = "";

			    CheckBox cb1 = rootView.findViewById(R.id.cb1);
			    CheckBox cb2 = rootView.findViewById(R.id.cb2);

			    RadioButton showRadar = rootView.findViewById(R.id.showRadar);

			    if (et1.getText().toString().equals("https://example.com/weewx/settings.txt") || et1.getText().toString().equals(""))
			    {
				    handlerSettings.sendEmptyMessage(0);
				    return;
			    }

			    try
			    {
				    Common.LogMessage("checking: " + et1.getText().toString());
				    URL url = new URL(et1.getText().toString());
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
					    if (mb[0].equals("data"))
						    data = mb[1];
					    if (mb[0].equals("radar"))
						    radar = mb[1];
					    if(mb[0].equals("fctype"))
						    fctype = mb[1];
					    if (mb[0].equals("forecast"))
						    forecast = mb[1];
					    if (mb[0].equals("webcam"))
						    webcam = mb[1];
					    if (mb[0].equals("custom"))
						    custom = mb[1];
				    }

				    if(fctype == null || fctype.equals(""))
				    	fctype = "Yahoo";

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
				    handlerDATA.sendEmptyMessage(0);
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
					    Common.LogMessage("checking: " + data);
					    URL url = new URL(data);
					    URLConnection conn = url.openConnection();
					    conn.connect();
					    InputStream in = conn.getInputStream();
					    in.close();
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
					    Common.LogMessage("checking: " + radar);
					    URL url = new URL(radar);
					    URLConnection conn = url.openConnection();
					    conn.connect();
					    InputStream in = conn.getInputStream();
					    in.close();
					    validURL2 = true;
				    } catch (MalformedURLException e) {
					    e.printStackTrace();
				    } catch (IOException e) {
					    e.printStackTrace();
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
					    forecast = URLEncoder.encode(forecast, "utf-8");
					    Common.LogMessage("forecast=" + forecast);
					    Common.LogMessage("fctype=" + fctype);

					    if(fctype.toLowerCase().equals("yahoo"))
					    {
						    if (cb2.isChecked())
							    forecast = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20woeid%20in%20(select%20woeid%20from%20geo.places(1)%20where%20text%3D%22" + forecast + "%22)%20and%20u%3D'c'&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
						    else
							    forecast = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20woeid%20in%20(select%20woeid%20from%20geo.places(1)%20where%20text%3D%22" + forecast + "%22)%20and%20u%3D'f'&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
					    } else if(fctype.toLowerCase().equals("weatherzone")) {
					    	forecast = "http://rss.weatherzone.com.au/?u=12994-1285&lt=aploc&lc=" + forecast + "&obs=0&fc=1&warn=0";
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
					    int curtime = Math.round(System.currentTimeMillis() / 1000);

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
					    	if(line.length() > 0)
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

			    if (!custom.equals("") && !custom.equals("https://example.com/mobile.html") && !custom.equals(oldcustom))
			    {
				    try
				    {
					    Common.LogMessage("checking: " + custom);
					    URL url = new URL(custom);
					    URLConnection conn = url.openConnection();
					    conn.connect();
					    InputStream in = conn.getInputStream();
					    in.close();
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

			    common.SetStringPref("SETTINGS_URL", et1.getText().toString());
			    common.SetIntPref("updateInterval", pos);
			    common.SetStringPref("BASE_URL", data);
			    common.SetStringPref("RADAR_URL", radar);
			    common.SetStringPref("FORECAST_URL", forecast);
			    common.SetStringPref("fctype", fctype);
			    common.SetStringPref("WEBCAM_URL", webcam);
			    common.SetStringPref("CUSTOM_URL", custom);
			    common.SetBoolPref("metric", cb2.isChecked());
			    common.SetBoolPref("bgdl", cb1.isChecked());
			    common.SetBoolPref("radarforecast", showRadar.isChecked());

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
            Common.LogMessage("sending intents");
            dialog.dismiss();
            Intent intent = new Intent();
            intent.putExtra("urlChanged", true);
            intent = new Intent();
            intent.setAction(myService.TAB0_INTENT);
            common.context.sendBroadcast(intent);
            Common.LogMessage("sent intents");
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handlerSettings = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            b1.setEnabled(true);
            dialog.dismiss();
            new AlertDialog.Builder(common.context)
                .setTitle("Invalid URL")
                .setMessage("Wasn't able to connect or download settings.txt from your server")
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

    public void onItemSelected(AdapterView<?> parent, View v, int position, long id)
    {
        pos = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView)
    {
    }
}