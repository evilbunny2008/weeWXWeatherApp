package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

class Weather
{
    private Common common;
    private View rootView;
    private WebView wv;

    Weather(Common common)
    {
        this.common = common;
    }

    private void checkFields(TextView tv, String txt)
    {
        if(!tv.getText().toString().equals(txt))
            tv.setText(txt);
    }

    private View updateFields()
    {
        Common.LogMessage("updateFields()");
        String bits[] = common.GetStringPref("LastDownload","").split("\\|");
        if(bits.length < 65)
            return rootView;

        checkFields((TextView)rootView.findViewById(R.id.textView), bits[56]);
        checkFields((TextView)rootView.findViewById(R.id.textView2), bits[54] + " " + bits[55]);
        checkFields((TextView)rootView.findViewById(R.id.textView3), bits[0] + bits[60]);

	    if(bits.length > 203)
		    checkFields((TextView)rootView.findViewById(R.id.appTemp), "App Temp: " + bits[203] + bits[60]);

        checkFields((TextView)rootView.findViewById(R.id.textView4), bits[25] + bits[61]);
        checkFields((TextView)rootView.findViewById(R.id.textView5), bits[37] + bits[63]);
        checkFields((TextView)rootView.findViewById(R.id.textView6), bits[29]);
        checkFields((TextView)rootView.findViewById(R.id.textView7), bits[6] + bits[64]);

	    String rain = bits[20] + bits[62] + " since mn";
	    if(bits.length > 160 && !bits[160].equals(""))
		    rain = bits[158] + bits[62] + " since " + bits[160];

        checkFields((TextView)rootView.findViewById(R.id.textView8), rain);
        checkFields((TextView)rootView.findViewById(R.id.textView9), bits[12] + bits[60]);
        checkFields((TextView)rootView.findViewById(R.id.textView10), bits[45] + "UVI");
        checkFields((TextView)rootView.findViewById(R.id.textView11), bits[43] + "W/m\u00B2");

        checkFields((TextView)rootView.findViewById(R.id.textView12), bits[57]);
        checkFields((TextView)rootView.findViewById(R.id.textView13), bits[58]);
        checkFields((TextView)rootView.findViewById(R.id.textView14), bits[47]);
        checkFields((TextView)rootView.findViewById(R.id.textView15), bits[48]);

	    LinearLayout homerow = rootView.findViewById(R.id.homerow);
        if(common.GetBoolPref("showIndoor", false))
			homerow.setVisibility(View.VISIBLE);
        else
	        homerow.setVisibility(View.GONE);

	    if(bits.length > 166)
	    {
		    checkFields((TextView) rootView.findViewById(R.id.hometemp), bits[161] + bits[60]);
		    checkFields((TextView) rootView.findViewById(R.id.homehumid), bits[166] + bits[64]);
	    }

        return rootView;
    }

    @SuppressLint("SetJavaScriptEnabled")
    View myWeather(LayoutInflater inflater, ViewGroup container)
    {
    	if(inflater == null || container == null)
    		return null;

        rootView = inflater.inflate(R.layout.fragment_weather, container, false);
        rootView.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator != null)
                    vibrator.vibrate(250);
                Common.LogMessage("rootview long press");
                forceRefresh();
                return true;
            }
        });

	    final SwipeRefreshLayout swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
	    swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
	    {
		    @Override
		    public void onRefresh()
		    {
		    	swipeLayout.setRefreshing(true);
		    	Common.LogMessage("onRefresh();");
			    forceRefresh();
			    reloadWebView();
			    reloadForecast();
			    swipeLayout.setRefreshing(false);
		    }
	    });

        wv = rootView.findViewById(R.id.radar);

	    wv.getSettings().setJavaScriptEnabled(true);
	    wv.getSettings().setUserAgentString(Common.UA);
        wv.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator != null)
                    vibrator.vibrate(250);
                Common.LogMessage("wv long press");
                loadWebView();
                return true;
            }
        });

	    wv.setWebViewClient(new WebViewClient()
	    {
		    @Override
		    public boolean shouldOverrideUrlLoading(WebView view, String url)
		    {
			    return false;
		    }
	    });

	    wv.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener()
	    {
		    @Override
		    public void onScrollChanged()
		    {
			    if (wv.getScrollY() == 0)
			    {
				    swipeLayout.setEnabled(true);
			    } else {
				    swipeLayout.setEnabled(false);
			    }
		    }
	    });

	    wv.setWebChromeClient(new WebChromeClient()
	    {
		    @Override
		    public boolean onConsoleMessage(ConsoleMessage cm)
		    {
			    return true;
		    }
	    });

        loadWebView();

        if(!common.GetStringPref("RADAR_URL", "").equals(""))
            reloadWebView();
        if(!common.GetBoolPref("radarforecast", true))
        	reloadForecast();

        Common.LogMessage("weather.java -- adding a new filter");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Common.UPDATE_INTENT);
        filter.addAction(Common.EXIT_INTENT);
        common.context.registerReceiver(serviceReceiver, filter);

        return updateFields();
    }

    private void forceRefresh()
    {
        common.getWeather();
	    wipeForecast();
	    loadWebView();
    }

    private void loadWebView()
    {
    	if(common.GetBoolPref("radarforecast", true))
	    {
		    switch (common.GetStringPref("radtype", "image"))
		    {
			    case "image":
				    String radar = common.context.getFilesDir() + "/radar.gif";

				    if (radar.equals("") || !new File(radar).exists() || common.GetStringPref("RADAR_URL", "").equals(""))
				    {
					    String html = "<html><body>Radar URL not set or is still downloading. You can go to settings to change.</body></html>";
					    wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
					    return;
				    }

				    String html = "<!DOCTYPE html>\n" +
						    "<html>\n" +
						    "  <head>\n" +
						    "    <meta charset='utf-8'>\n" +
						    "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
						    "  </head>\n" +
						    "  <body>\n" +
						    "\t<img style='margin:0px;padding:0px;border:0px;text-align:center;max-width:100%;width:auto;height:auto;'\n" +
						    "\tsrc='file://" + radar + "'>\n" +
						    "  </body>\n" +
						    "</html>";
				    wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
				    break;
			    case "webpage":
				    wv.loadUrl(common.GetStringPref("RADAR_URL", ""));
				    break;
			    default:
				    String tmphtml = "<html><body>Radar URL not set or is still downloading. You can go to settings to change.</body></html>";
				    wv.loadDataWithBaseURL(null, tmphtml, "text/html", "utf-8", null);
				    break;
		    }
	    } else {
		    String fctype = common.GetStringPref("fctype", "Yahoo");
		    String data = common.GetStringPref("forecastData", "");
		    if(fctype.toLowerCase().equals("yahoo"))
		    {
			    String[] content = common.processYahoo(data);
			    if(content == null || content.length <= 0)
			    	return;
			    String yahoo = "<img src='purple.png' height='29px'/><br/>";
			    final String fc = "<html><body style='text-align:center'>" + yahoo + content[0] + "</body></html>";
			    wv.post(new Runnable()
			    {
				    @Override
				    public void run()
				    {
					    wv.loadDataWithBaseURL("file:///android_res/drawable/", fc, "text/html", "utf-8", null);
				    }
			    });

		    } else if(fctype.toLowerCase().equals("weatherzone")) {
			    String[] content = common.processWZ(data);
			    if(content == null || content.length <= 0)
				    return;

			    String wz = "<img src='wz.png' height='29px'/><br/>";
			    final String fc = "<html><body style='text-align:center'>" + wz + content[0] + "</body></html>";

			    wv.post(new Runnable()
			    {
				    @Override
				    public void run()
				    {
					    wv.loadDataWithBaseURL("file:///android_res/drawable/", fc, "text/html", "utf-8", null);
				    }
			    });

			    /*
			    	    if(common.GetStringPref("fctype", "yahoo").toLowerCase().equals("yahoo"))
		    im.setImageResource(R.drawable.purple);
	    else if(common.GetStringPref("fctype", "yahoo").toLowerCase().equals("weatherzone"))
	        im.setImageResource(R.drawable.wz);
			     */

		    }
	    }
    }

	private void wipeForecast()
	{
		Common.LogMessage("wiping rss cache");
		common.SetIntPref("rssCheck", 0);
		common.SetStringPref("forecastData", "");
		reloadForecast();
	}

	private void reloadForecast()
    {
	    if(common.GetBoolPref("radarforecast", true))
		    return;

	    final String rss = common.GetStringPref("FORECAST_URL", "");

	    Common.LogMessage("forecast checking: " + rss);

	    Thread t = new Thread(new Runnable()
	    {
		    @Override
		    public void run()
		    {
			    try
			    {
				    int curtime = Math.round(System.currentTimeMillis() / 1000);

				    if(common.GetStringPref("forecastData", "").equals("") || common.GetIntPref("rssCheck", 0) + 7190 < curtime)
				    {
					    Common.LogMessage("no forecast data or cache is more than 2 hour old");
					    URL url = new URL(rss);
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
				    }

				    handlerDone.sendEmptyMessage(0);
			    } catch (Exception e) {
				    e.printStackTrace();
			    }
		    }
	    });

	    t.start();
    }

    private void reloadWebView()
    {
	    if(!common.GetBoolPref("radarforecast", true))
	    	return;

        Common.LogMessage("reload radar...");
        final String radar = common.GetStringPref("RADAR_URL", "");

        if(radar.equals(""))
        {
            loadWebView();
            return;
        }

        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Common.LogMessage("starting to download image from: " + radar);
                    URL url = new URL(radar);

                    InputStream ins = url.openStream();
                    File file = new File(common.context.getFilesDir(), "/radar.gif");

	                int curtime = Math.round(System.currentTimeMillis() / 1000);

                    if(Math.round(file.lastModified() / 1000) + 590 > curtime)
						return;

                    FileOutputStream out = null;

                    try
                    {
                        out = new FileOutputStream(file);
                        final byte[] b = new byte[2048];
                        int length;
                        while ((length = ins.read(b)) != -1)
                            out.write(b, 0, length);
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

                    Common.LogMessage("done downloading, prompt handler to draw to movie");
                } catch (Exception e) {
                    e.printStackTrace();
                }

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
            try
            {
                Common.LogMessage(common.context.getFilesDir() + "/radar.gif");
                loadWebView();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public void doStop()
    {
        Common.LogMessage("weather.java -- unregisterReceiver");
	    try
	    {
		    common.context.unregisterReceiver(serviceReceiver);
	    } catch (Exception e) {
			//TODO: ignore this exception...
	    }
    }

    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            try
            {
                String action = intent.getAction();
                if(action != null && action.equals(Common.UPDATE_INTENT))
                {
                    Common.LogMessage("Weather() We have a hit, so we should probably update the screen.");
                    updateFields();
                    reloadWebView();
                    reloadForecast();
                } else if(action != null && action.equals(Common.EXIT_INTENT)) {
                    Common.LogMessage("weather.java -- unregisterReceiver");
                    common.context.unregisterReceiver(serviceReceiver);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}