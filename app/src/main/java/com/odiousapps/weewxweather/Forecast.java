package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.rongi.rotate_layout.layout.RotateLayout;

import java.io.File;

class Forecast
{
    private Common common;
    private View rootView;
    private WebView wv1, wv2;
    private SwipeRefreshLayout swipeLayout;
	private TextView forecast;
	private ImageView im;
	private RotateLayout rl;
	private boolean dark_theme;

	Forecast(Common common)
    {
        this.common = common;
	    dark_theme = common.GetBoolPref("dark_theme", false);
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
			    swipeLayout.setRefreshing(true);
			    Common.LogMessage("rootview long press");
			    reloadWebView(true);
			    getForecast(true);
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
			    reloadWebView(true);
			    getForecast(true);
		    }
	    });

	    rl = rootView.findViewById(R.id.rotateWeb);
	    wv1 = rootView.findViewById(R.id.webView1);
	    wv2 = rootView.findViewById(R.id.webView2);

	    wv1.getSettings().setUserAgentString(Common.UA);
	    wv1.getSettings().setJavaScriptEnabled(true);

	    wv1.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener()
	    {
		    @Override
		    public void onScrollChanged()
		    {
			    if (wv1.getScrollY() == 0)
			    {
				    swipeLayout.setEnabled(true);
			    } else {
				    swipeLayout.setEnabled(false);
			    }
		    }
	    });

	    wv1.setWebChromeClient(new WebChromeClient()
	    {
		    @Override
		    public boolean onConsoleMessage(ConsoleMessage cm)
		    {
			    return true;
		    }
	    });

	    wv1.setOnLongClickListener(new View.OnLongClickListener()
	    {
		    @Override
		    public boolean onLongClick(View v)
		    {
			    Vibrator vibrator = (Vibrator) common.context.getSystemService(Context.VIBRATOR_SERVICE);
			    if (vibrator != null)
				    vibrator.vibrate(250);
			    swipeLayout.setRefreshing(true);
			    Common.LogMessage("webview long press");
			    reloadWebView(true);
			    getForecast(true);
			    return true;
		    }
	    });

	    wv1.setWebViewClient(new WebViewClient()
	    {
		    @Override
		    public boolean shouldOverrideUrlLoading(WebView view, String url)
		    {
			    return false;
		    }
	    });

	    wv1.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener()
	    {
		    @Override
		    public void onScrollChanged()
		    {
			    if (wv1.getScrollY() == 0)
			    {
				    swipeLayout.setEnabled(true);
			    } else
			    {
				    swipeLayout.setEnabled(false);
			    }
		    }
	    });

	    wv2.getSettings().setUserAgentString(Common.UA);
	    wv2.getSettings().setJavaScriptEnabled(true);

	    wv2.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener()
	    {
		    @Override
		    public void onScrollChanged()
		    {
			    if (wv2.getScrollY() == 0)
			    {
				    swipeLayout.setEnabled(true);
			    } else {
				    swipeLayout.setEnabled(false);
			    }
		    }
	    });

	    wv2.setWebChromeClient(new WebChromeClient()
	    {
		    @Override
		    public boolean onConsoleMessage(ConsoleMessage cm)
		    {
			    return true;
		    }
	    });

	    wv2.setOnLongClickListener(new View.OnLongClickListener()
	    {
		    @Override
		    public boolean onLongClick(View v)
		    {
			    Vibrator vibrator = (Vibrator) common.context.getSystemService(Context.VIBRATOR_SERVICE);
			    if (vibrator != null)
				    vibrator.vibrate(250);
			    swipeLayout.setRefreshing(true);
			    Common.LogMessage("webview long press");
			    reloadWebView(true);
			    getForecast(true);
			    return true;
		    }
	    });

	    wv2.setWebViewClient(new WebViewClient()
	    {
		    @Override
		    public boolean shouldOverrideUrlLoading(WebView view, String url)
		    {
			    return false;
		    }
	    });

	    wv2.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener()
	    {
		    @Override
		    public void onScrollChanged()
		    {
			    if (wv2.getScrollY() == 0)
			    {
				    swipeLayout.setEnabled(true);
			    } else
			    {
				    swipeLayout.setEnabled(false);
			    }
		    }
	    });

	    forecast = rootView.findViewById(R.id.forecast);
	    im = rootView.findViewById(R.id.logo);

	    updateScreen();

	    File f2 = new File(common.context.getFilesDir(), "/radar.gif");
	    long[] period = common.getPeriod();

	    if(!common.GetStringPref("RADAR_URL", "").equals("") && f2.lastModified() + period[0] < System.currentTimeMillis())
		    reloadWebView(false);

	    int curtime = Math.round(System.currentTimeMillis() / 1000);
	    if(!common.GetBoolPref("radarforecast", true) && common.GetIntPref("rssCheck", 0) + 7190 < curtime)
		    getForecast(false);

        return rootView;
    }

	private void loadWebView()
	{
		if(common.GetBoolPref("radarforecast", true))
			return;

		swipeLayout.setRefreshing(true);

		if (common.GetStringPref("radtype", "image").equals("image"))
		{
			String radar = common.context.getFilesDir() + "/radar.gif";
			File rf = new File(radar);

			if(!radar.equals("") && !rf.exists() && !common.GetStringPref("RADAR_URL", "").equals("") && common.checkConnection())
			{
				reloadWebView(true);
				return;
			}

			if (radar.equals("") || !rf.exists() || common.GetStringPref("RADAR_URL", "").equals(""))
			{
				String html = "<html>";
				if (dark_theme)
					html += "<head><style>body{color: #fff; background-color: #000;}</style></head>";
				html += "<body>Radar URL not set or is still downloading. You can go to settings to change.</body></html>";
				wv1.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null);
				swipeLayout.setRefreshing(false);
				return;
			}

			int height = Math.round((float) Resources.getSystem().getDisplayMetrics().widthPixels / Resources.getSystem().getDisplayMetrics().scaledDensity * 0.955f);
			int width = Math.round((float) Resources.getSystem().getDisplayMetrics().heightPixels / Resources.getSystem().getDisplayMetrics().scaledDensity * 0.955f);

			String html = "<!DOCTYPE html>\n" +
					"<html>\n" +
					"  <head>\n" +
					"    <meta charset='utf-8'>\n" +
					"    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n";
			if (dark_theme)
				html += "<style>body{color: #fff; background-color: #000;}</style>";
			html += "  </head>\n" +
					"  <body>\n" +
					"\t<div style='text-align:center;'>\n" +
					"\t<img style='margin:0px;padding:0px;border:0px;text-align:center;max-height:" + height + "px;max-width:" + width + "px;width:auto;height:auto;'\n" +
					"\tsrc='file://" + radar + "'>\n" +
					"\t</div>\n" +
					"  </body>\n" +
					"</html>";
			wv1.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null);
			rl.setVisibility(View.VISIBLE);
			wv2.setVisibility(View.GONE);
			swipeLayout.setRefreshing(false);
		} else if (common.GetStringPref("radtype", "image").equals("webpage") && !common.GetStringPref("RADAR_URL", "").equals("")) {
			rl.setVisibility(View.GONE);
			wv2.setVisibility(View.VISIBLE);

			if(common.checkConnection())
			{
				wv2.clearCache(true);
				wv2.clearHistory();
				wv2.clearFormData();
				WebSettings webSettings = wv2.getSettings();
				webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
			}

			wv2.loadUrl(common.GetStringPref("RADAR_URL", ""));

			swipeLayout.setRefreshing(false);
		}

		swipeLayout.setRefreshing(false);
	}

	@SuppressWarnings("SameParameterValue")
	private void reloadWebView(boolean force)
	{
		if(common.GetBoolPref("radarforecast", true))
			return;

		Common.LogMessage("reload radar...");
		final String radar = common.GetStringPref("RADAR_URL", "");

		if(radar.equals("") || common.GetStringPref("radtype", "image").equals("webpage"))
		{
			loadWebView();
			swipeLayout.setRefreshing(false);
			return;
		}

		if(!common.checkConnection() && !force)
		{
			Common.LogMessage("Not on wifi and not a forced refresh");
			swipeLayout.setRefreshing(false);
			return;
		}

		swipeLayout.setRefreshing(true);

		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Common.LogMessage("starting to download image from: " + radar);
					File f = common.downloadRADAR(radar);
					Common.LogMessage("done downloading " + f.getAbsolutePath() + ", prompt handler to draw to movie");
					File f2 = new File(common.context.getFilesDir(), "/radar.gif");
					if(f.renameTo(f2))
						handlerDone.sendEmptyMessage(0);
				} catch (Exception e) {
					e.printStackTrace();
				}

				Handler mHandler = new Handler(Looper.getMainLooper());
				mHandler.post(new Runnable()
				{
					@Override
					public void run()
					{
						swipeLayout.setRefreshing(false);
					}
				});
			}
		});

		t.start();
	}

	void doResume()
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction(Common.UPDATE_INTENT);
		filter.addAction(Common.REFRESH_INTENT);
		filter.addAction(Common.EXIT_INTENT);
		common.context.registerReceiver(serviceReceiver, filter);
		Common.LogMessage("forecast.java -- registerReceiver");
	}

	void doPause()
    {
	    try
	    {
		    common.context.unregisterReceiver(serviceReceiver);
	    } catch (Exception e) {
		    e.printStackTrace();
	    }
	    Common.LogMessage("forecast.java -- unregisterReceiver");
    }

    private void updateScreen()
    {
	    if (common.GetBoolPref("radarforecast", true))
	    {
		    Common.LogMessage("Displaying forecast");
		    getForecast(false);
		    forecast.setVisibility(View.VISIBLE);
		    im.setVisibility(View.VISIBLE);
		    rl.setVisibility(View.GONE);
		    wv2.setVisibility(View.VISIBLE);
	    } else {
		    Common.LogMessage("Displaying radar");
		    if (common.GetStringPref("radtype", "image").equals("image"))
		    {
			    forecast.setVisibility(View.GONE);
			    im.setVisibility(View.GONE);
			    rl.setVisibility(View.VISIBLE);
			    wv2.setVisibility(View.GONE);
		    } else {
			    forecast.setVisibility(View.GONE);
			    im.setVisibility(View.GONE);
			    rl.setVisibility(View.GONE);
			    wv2.setVisibility(View.VISIBLE);
		    }
		    loadWebView();
	    }

	    swipeLayout.setRefreshing(false);
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
                if(action != null && (action.equals(Common.UPDATE_INTENT) || action.equals(Common.REFRESH_INTENT)))
                {
	                dark_theme = common.GetBoolPref("dark_theme", false);
	                updateScreen();
                } else if(action != null && action.equals(Common.EXIT_INTENT)) {
                    doPause();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void getForecast(boolean force)
    {
	    if(!common.GetBoolPref("radarforecast", true))
		    return;

	    final String forecast_url = common.GetStringPref("FORECAST_URL", "");

	    if(forecast_url.equals(""))
	    {
		    final String html = "<html><body>Forecast URL not set. Edit inigo-settings.txt to change.</body></html>";

		    Handler mHandler = new Handler(Looper.getMainLooper());
		    mHandler.post(new Runnable()
		    {
			    @Override
			    public void run()
			    {
				    wv2.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
				    swipeLayout.setRefreshing(false);
			    }
		    });

		    return;
	    }

	    if(!common.checkConnection() && !force)
	    {
		    Common.LogMessage("Not on wifi and not a forced refresh");
		    handlerDone.sendEmptyMessage(0);
		    return;
	    }

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

	                    String tmp = common.downloadForecast();

	                    Common.LogMessage("updating rss cache");
	                    common.SetIntPref("rssCheck", curtime);
	                    common.SetStringPref("forecastData", tmp);
	                    handlerDone.sendEmptyMessage(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
	        common.SendRefresh();
	        swipeLayout.setRefreshing(false);
        }
    };

    private void generateForecast()
    {
    	Thread t = new Thread(new Runnable()
	    {
		    @Override
		    public void run()
		    {
			    Handler mHandler = new Handler(Looper.getMainLooper());
			    mHandler.post(new Runnable()
			    {
				    @Override
				    public void run()
				    {
					    swipeLayout.setRefreshing(true);
				    }
			    });

			    Common.LogMessage("getting json data");
		        String data;
		        String fctype = common.GetStringPref("fctype", "Yahoo");

			    data = common.GetStringPref("forecastData", "");
			    if(data.equals(""))
			    {
				    String tmp = "<html>";
				    if(dark_theme)
					    tmp += "<head><style>body{color: #fff; background-color: #000;}</style></head>";
				    tmp += "<body>Forecast URL not set, edit settings.txt to change</body></html>";

				    final String html = tmp;

				    mHandler.post(new Runnable()
				    {
					    @Override
					    public void run()
					    {
						    wv1.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
						    swipeLayout.setRefreshing(false);
					    }
				    });
				    return;
			    }

			    switch(fctype.toLowerCase())
			    {
				    case "yahoo":
				    {
					    String[] content = common.processYahoo(data, true);
					    if(content != null && content.length >= 2)
						    updateForecast(content[0], content[1]);
					    break;
				    }
				    case "weatherzone":
				    {
					    String[] content = common.processWZ(data, true);
					    if(content != null && content.length >= 2)
						    updateForecast(content[0], content[1]);
					    break;
				    }
				    case "yr.no":
				    {
					    String[] content = common.processYR(data, true);
					    if(content != null && content.length >= 2)
						    updateForecast(content[0], content[1]);
					    break;
				    }
				    case "bom.gov.au":
				    {
					    String[] content = common.processBOM(data, true);
					    if(content != null && content.length >= 2)
						    updateForecast(content[0], content[1]);
					    break;
				    }
				    case "wmo.int":
				    {
						String[] content = common.processWMO(data, true);
					    if(content != null && content.length >= 2)
						    updateForecast(content[0], content[1]);
					    break;
				    }
				    case "weather.gov":
				    {
					    String[] content = common.processWGOV(data, true);
					    if(content != null && content.length >= 2)
						    updateForecast(content[0], content[1]);
					    break;
				    }
				    case "weather.gc.ca":
				    {
					    String[] content = common.processWCA(data, true);
					    if(content != null && content.length >= 2)
						    updateForecast(content[0], content[1]);
					    break;
				    }
				    case "weather.gc.ca-fr":
				    {
					    String[] content = common.processWCAF(data, true);
					    if(content != null && content.length >= 2)
						    updateForecast(content[0], content[1]);
					    break;
				    }
				    case "metoffice.gov.uk":
				    {
					    String[] content = common.processMET(data, true);
					    if(content != null && content.length >= 2)
						    updateForecast(content[0], content[1]);
					    break;
				    }
				    case "bom2":
				    {
					    String[] content = common.processBOM2(data, true);
					    if(content != null && content.length >= 2)
						    updateForecast(content[0], content[1]);
					    break;
				    }
				    case "aemet.es":
				    {
					    String[] content = common.processAEMET(data, true);
					    if(content != null && content.length >= 2)
						    updateForecast(content[0], content[1]);
					    break;
				    }
				    case "dwd.de":
				    {
					    String[] content = common.processDWD(data, true);
					    if(content != null && content.length >= 2)
						    updateForecast(content[0], content[1]);
					    break;
				    }
				    case "metservice.com":
				    {
					    String[] content = common.processMetService(data, true);
					    if(content != null && content.length >= 2)
						    updateForecast(content[0], content[1]);
					    break;
				    }
				    case "meteofrance.com":
				    {
					    String[] content = common.processMF(data, true);
					    if(content != null && content.length >= 2)
						    updateForecast(content[0], content[1]);
					    break;
				    }
				    case "smn.gob.ar":
				    {
					    String[] content = common.processSMN(data, true);
					    if(content != null && content.length >= 2)
						    updateForecast(content[0], content[1]);
					    break;
				    }
				    case "darksky.net":
				    {
					    String[] content = common.processDarkSky(data, true);
					    if(content != null && content.length >= 2)
						    updateForecast(content[0], content[1]);
					    break;
				    }
			    }

			    mHandler.post(new Runnable()
			    {
				    @Override
				    public void run()
				    {
					    swipeLayout.setRefreshing(false);
				    }
			    });
		    }
	    });

    	t.start();
    }

    private void updateForecast(final String bits, final String desc)
    {
	    Handler mHandler = new Handler(Looper.getMainLooper());
	    mHandler.post(new Runnable()
	    {
		    @Override
		    public void run()
		    {
			    String tmpfc = "<html>";
			    if (dark_theme)
				    tmpfc += "<head><style>body{color: #fff; background-color: #000;}</style>" + Common.ssheader + "</head>";
			    else
			    	tmpfc += "<head>" + Common.ssheader + "</head>";
			    tmpfc += "<body style='text-align:center'>" + bits + "</body></html>";

			    final String fc = tmpfc;

			    Handler mHandler = new Handler(Looper.getMainLooper());
			    mHandler.post(new Runnable()
			    {
				    @Override
				    public void run()
				    {
					    wv2.loadDataWithBaseURL("file:///android_res/drawable/", fc, "text/html", "utf-8", null);
				    }
			    });

			    TextView tv1 = rootView.findViewById(R.id.forecast);
			    if (!dark_theme)
			    {
				    tv1.setTextColor(0xff000000);
				    tv1.setBackgroundColor(0xffffffff);
				    im.setBackgroundColor(0xffffffff);
			    } else
			    {
				    tv1.setTextColor(0xffffffff);
				    tv1.setBackgroundColor(0xff000000);
				    im.setBackgroundColor(0xff000000);
			    }
			    tv1.setText(desc);

			    switch (common.GetStringPref("fctype", "yahoo").toLowerCase())
			    {
				    case "yahoo":
					    im.setImageResource(R.drawable.purple);
					    break;
				    case "weatherzone":
					    im.setImageResource(R.drawable.wz);
					    break;
				    case "yr.no":
					    im.setImageResource(R.drawable.yrno);
					    break;
				    case "bom.gov.au":
					    im.setImageResource(R.drawable.bom);
					    break;
				    case "wmo.int":
					    im.setImageResource(R.drawable.wmo);
					    break;
				    case "weather.gov":
					    im.setImageResource(R.drawable.wgov);
					    break;
				    case "weather.gc.ca":
					    im.setImageResource(R.drawable.wca);
					    break;
				    case "weather.gc.ca-fr":
					    im.setImageResource(R.drawable.wca);
					    break;
				    case "metoffice.gov.uk":
					    im.setImageResource(R.drawable.met);
					    break;
				    case "bom2":
					    im.setImageResource(R.drawable.bom);
					    break;
				    case "aemet.es":
					    im.setImageResource(R.drawable.aemet);
					    break;
				    case "dwd.de":
					    im.setImageResource(R.drawable.dwd);
					    break;
				    case "metservice.com":
					    im.setImageResource(R.drawable.metservice);
					    break;
				    case "meteofrance.com":
					    im.setImageResource(R.drawable.mf);
					    break;
				    case "smn.gob.ar":
					    im.setImageResource(R.drawable.smn);
					    break;
				    case "darksky.net":
					    im.setImageResource(R.drawable.darksky);
					    break;
			    }
		    }
	    });
    }
}