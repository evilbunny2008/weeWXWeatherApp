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
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

class Forecast
{
    private Common common;
    private View rootView;
    private WebView wv;
    private SwipeRefreshLayout swipeLayout;
	private TextView forecast;
	private ImageView im;

	Forecast(Common common)
    {
        this.common = common;
    }

    @SuppressLint("SetJavaScriptEnabled")
    View myForecast(LayoutInflater inflater, ViewGroup container)
    {
	    rootView = inflater.inflate(R.layout.fragment_forecast, container, false);
	    rootView.setOnLongClickListener(new View.OnLongClickListener()
	    {
		    @Override
		    public boolean onLongClick(View v)
		    {
			    Vibrator vibrator = (Vibrator) common.context.getSystemService(Context.VIBRATOR_SERVICE);
			    if (vibrator != null)
				    vibrator.vibrate(250);
			    Common.LogMessage("rootview long press");
			    forceRefresh();
			    reloadWebView(true);
			    return true;
		    }
	    });

	    swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
	    swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
	    {
		    @Override
		    public void onRefresh()
		    {
			    swipeLayout.setRefreshing(true);
			    Common.LogMessage("onRefresh();");
			    forceRefresh();
			    reloadWebView(true);
		    }
	    });

	    wv = rootView.findViewById(R.id.webView1);

	    wv.getSettings().setUserAgentString(Common.UA);
	    wv.getSettings().setJavaScriptEnabled(true);

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

	    wv.setOnLongClickListener(new View.OnLongClickListener()
	    {
		    @Override
		    public boolean onLongClick(View v)
		    {
			    Vibrator vibrator = (Vibrator) common.context.getSystemService(Context.VIBRATOR_SERVICE);
			    if (vibrator != null)
				    vibrator.vibrate(250);
			    Common.LogMessage("webview long press");
			    if(common.GetStringPref("radtype", "image").equals("image"))
			    {
				    forceRefresh();
				    reloadWebView(true);
			    } else if(common.GetStringPref("radtype", "image").equals("webpage")) {
				    wv.reload();
			    }
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
			    } else
			    {
				    swipeLayout.setEnabled(false);
			    }
		    }
	    });

	    IntentFilter filter = new IntentFilter();
	    filter.addAction(Common.UPDATE_INTENT);
	    filter.addAction(Common.EXIT_INTENT);
	    common.context.registerReceiver(serviceReceiver, filter);

	    forecast = rootView.findViewById(R.id.forecast);
	    im = rootView.findViewById(R.id.logo);

	    if (common.GetBoolPref("radarforecast", true))
	    {
	    	Common.LogMessage("Displaying forecast");
		    getForecast();
		    forecast.setVisibility(View.VISIBLE);
		    im.setVisibility(View.VISIBLE);
	    } else {
		    Common.LogMessage("Displaying radar");
		    if(common.GetStringPref("fctype", "yahoo").equals("yahoo") || common.GetStringPref("fctype", "yahoo").equals("weatherzone"))
		    {
			    loadWebView();
		        reloadWebView(false);

		    } else if(common.GetStringPref("radtype", "image").equals("webpage") && !common.GetStringPref("RADAR_URL", "").equals(""))
		    	wv.loadUrl(common.GetStringPref("RADAR_URL", ""));
		    forecast.setVisibility(View.GONE);
		    im.setVisibility(View.GONE);
        }

        return rootView;
    }

	private void loadWebView()
	{
		if(common.GetStringPref("radtype", "image").equals("image"))
		{
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
		} else if(common.GetStringPref("radtype", "image").equals("webpage") && !common.GetStringPref("RADAR_URL", "").equals("")) {
			wv.loadUrl(common.GetStringPref("RADAR_URL", ""));
		}
	}

	private void reloadWebView(final boolean force)
	{
		if(common.GetBoolPref("radarforecast", true))
			return;

		Common.LogMessage("reload radar...");
		final String radar = common.GetStringPref("RADAR_URL", "");

		if(radar.equals("") || common.GetStringPref("radtype", "image").equals("webpage"))
		{
			loadWebView();
			handlerDone.sendEmptyMessage(0);
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

					if(!force && Math.round(file.lastModified() / 1000) + 590 > curtime)
					{
						handlerDone.sendEmptyMessage(0);
						return;
					}

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


	void doStop()
    {
        Common.LogMessage("forecast.java -- unregisterReceiver");
	    try
	    {
		    common.context.unregisterReceiver(serviceReceiver);
	    } catch (Exception e) {
		    e.printStackTrace();
	    }
    }

    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            try
            {
                Common.LogMessage("Weather() We have a hit, so we should probably update the screen.");
                String action = intent.getAction();
                if(action != null && action.equals(Common.UPDATE_INTENT))
                {
	                if (common.GetBoolPref("radarforecast", true))
	                {
		                Common.LogMessage("Displaying forecast");
		                getForecast();
		                forecast.setVisibility(View.VISIBLE);
		                im.setVisibility(View.VISIBLE);
	                } else {
		                Common.LogMessage("Displaying radar");
		                loadWebView();
		                reloadWebView(true);
		                forecast.setVisibility(View.GONE);
		                im.setVisibility(View.GONE);
	                }
                } else if(action != null && action.equals(Common.EXIT_INTENT)) {
                    common.context.unregisterReceiver(serviceReceiver);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void forceRefresh()
    {
	    if(!common.GetBoolPref("radarforecast", true))
		    return;

        Common.LogMessage("wiping rss cache");
        common.SetIntPref("rssCheck", 0);
        common.SetStringPref("forecastData", "");
        getForecast();
    }

    private void getForecast()
    {
	    if(!common.GetBoolPref("radarforecast", true))
		    return;

        final String rss = common.GetStringPref("FORECAST_URL", "");
        if(rss.equals(""))
        {
	        wv.loadDataWithBaseURL(null, "<html><body>Forecast URL not set, go to settings to change</body></html>", "text/html", "utf-8", null);
	        return;
        }

        if(!swipeLayout.isRefreshing())
	        swipeLayout.setRefreshing(true);

	    if(!common.GetStringPref("forecastData", "").equals(""))
		    generateForecast();

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
	        if (common.GetBoolPref("radarforecast", true))
	        	generateForecast();
	        else
	        	loadWebView();

	        swipeLayout.setRefreshing(false);
        }
    };

    private void generateForecast()
    {
        Common.LogMessage("getting json data");
        String data;
        String fctype = common.GetStringPref("fctype", "Yahoo");

	    swipeLayout.setRefreshing(false);

        try
        {
	        data = common.GetStringPref("forecastData", "");
	        while (data.equals(""))
	        {
		        Thread.sleep(1000);
		        data = common.GetStringPref("forecastData", "");
	        }
        } catch (Exception e) {
        	e.printStackTrace();
        	return;
        }

	    if(fctype.toLowerCase().equals("yahoo"))
	    {
		    String[] content = common.processYahoo(data);
		    if(content != null && content.length >= 2)
			    updateForecast(content[0], content[1]);
	    } else if(fctype.toLowerCase().equals("weatherzone")) {
		    String[] content = common.processWZ(data);
		    if(content != null && content.length >= 2)
			    updateForecast(content[0], content[1]);
	    }
    }

    private void updateForecast(String bits, String desc)
    {
        final String fc = "<html><body style='text-align:center'>"  + bits + "</body></html>";

        wv.post(new Runnable()
        {
            @Override
            public void run()
            {
                wv.loadDataWithBaseURL(null, fc, "text/html", "utf-8", null);
            }
        });

        TextView tv1 = rootView.findViewById(R.id.forecast);
        tv1.setText(desc);

	    if(common.GetStringPref("fctype", "yahoo").toLowerCase().equals("yahoo"))
		    im.setImageResource(R.drawable.purple);
	    else if(common.GetStringPref("fctype", "yahoo").toLowerCase().equals("weatherzone"))
	        im.setImageResource(R.drawable.wz);
    }
}