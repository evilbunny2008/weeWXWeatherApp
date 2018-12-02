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
import android.widget.TextView;

import java.io.File;

class Weather
{
    private Common common;
    private View rootView;
    private WebView wv;
    private boolean dark_theme;
	private SwipeRefreshLayout swipeLayout;

    Weather(Common common)
    {
        this.common = common;
	    dark_theme = common.GetBoolPref("dark_theme", false);
    }

    private void checkFields(TextView tv, String txt)
    {
        if(!tv.getText().toString().equals(txt))
            tv.setText(txt);

        if(!dark_theme)
        {
	        tv.setTextColor(0xff000000);
	        tv.setBackgroundColor(0xffffffff);
	        Common.LogMessage("no dark theme");
        } else {
	        tv.setTextColor(0xffffffff);
	        tv.setBackgroundColor(0xff000000);
	        Common.LogMessage("dark theme");
        }
    }

    private View updateFields()
    {
	    int iw = 17;

        Common.LogMessage("updateFields()");
        String bits[] = common.GetStringPref("LastDownload","").split("\\|");
        if(bits.length < 65)
            return rootView;

        checkFields((TextView)rootView.findViewById(R.id.textView), bits[56]);
        checkFields((TextView)rootView.findViewById(R.id.textView2), bits[54] + " " + bits[55]);

	    final WebView current = rootView.findViewById(R.id.current);
	    current.getSettings().setUserAgentString(Common.UA);
	    current.setOnLongClickListener(new View.OnLongClickListener()
	    {
		    @Override
		    public boolean onLongClick(View v)
		    {
			    Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
			    if(vibrator != null)
				    vibrator.vibrate(250);
			    Common.LogMessage("current long press");
			    updateFields();
			    return true;
		    }
	    });

	    current.setWebViewClient(new WebViewClient()
	    {
		    @Override
		    public boolean shouldOverrideUrlLoading(WebView view, String url)
		    {
			    return false;
		    }
	    });
	    current.setWebChromeClient(new WebChromeClient()
	    {
		    @Override
		    public boolean onConsoleMessage(ConsoleMessage cm)
		    {
			    return true;
		    }
	    });

	    String stmp;
	    StringBuilder sb = new StringBuilder();

	    String header;
	    if(!dark_theme)
	    	header = "<html><body>";
	    else
	    	header = "<html><head><style>body{color: #fff; background-color: #000;}</style></head><body>";
	    String footer = "</body></html>";
	    sb.append(header);

	    sb.append("<table style='width:100%;border:0px;'>");

	    stmp = "<tr><td style='font-size:36pt;text-align:right;'>" + bits[0] + bits[60] + "</td>";
	    if(bits.length > 203)
		    stmp += "<td style='font-size:18pt;text-align:right;vertical-align:bottom;'>AT: " + bits[203] + bits[60] +"</td></tr></table>";
	    else
	    	stmp += "<td>&nbsp</td></tr></table>";

	    sb.append(stmp);
	    sb.append("<table style='width:100%;border:0px;'>");

	    stmp = "<tr><td><img style='width:" + iw + "px' src='windsock.png'></td><td>" + bits[25] + bits[61] + "</td>" +
			    "<td style='text-align:right;'>" + bits[1] + bits[60] + "</td><td><img style='width:" + iw + "px' src='barometer.png'></td></tr>";
	    sb.append(stmp);

	    stmp = "<tr><td><img style='width:" + iw + "px' src='compass.png'></td><td>" + bits[37] + bits[63] + "</td>" +
			    "<td style='text-align:right;'>" + bits[6] + bits[64] + "</td><td><img style='width:" + iw + "px' src='humidity.png'></td></tr>";
	    sb.append(stmp);

	    String rain = bits[20] + bits[62] + " since mn";
	    if(bits.length > 160 && !bits[160].equals(""))
		    rain = bits[158] + bits[62] + " since " + bits[160];

	    stmp = "<tr><td><img style='width:" + iw + "px' src='umbrella.png'></td><td>" + rain + "</td>" +
			    "<td style='text-align:right;'>" + bits[12] + bits[60] + "</td><td><img style='width:" + iw + "px' src='droplet.png'></td></tr>";
	    sb.append(stmp);

	    stmp = "<tr><td><img style='width:" + iw + "px' src='sunglasses.png'></td><td>" + bits[45] + "UVI</td>" +
			    "<td style='text-align:right;'>" + bits[43] + "W/m\u00B2</td><td><img style='width:" + iw + "px' src='sunglasses.png'></td></tr>";
	    sb.append(stmp);

	    if(bits.length > 202 && common.GetBoolPref("showIndoor", false))
	    {
		    stmp = "<tr><td><img style='width:" + iw + "px' src='home.png'></td><td>" + bits[161] + bits[60] + "</td>" +
				    "<td style='text-align:right;'>" + bits[166] + bits[64] + "</td><td><img style='width:" + iw + "px' src='home.png'></td></tr>";
		    sb.append(stmp);
	    }

	    stmp = "</table>";
	    sb.append(stmp);

	    sb.append("<table style='width:100%;border:0px;'>");

	    stmp = "<tr>" +
			    "<td><img style='width:" + iw + "px' src='sunrise.png'></td><td>" + bits[57] + "</td>" +
			    "<td><img style='width:" + iw + "px' src='sunset.png'></td><td>" + bits[58] + "</td>" +
			    "<td><img style='width:" + iw + "px' src='moonrise.png'></td><td>" + bits[47] + "</td>" +
	            "<td><img style='width:" + iw + "px' src='moonset.png'></td><td>" + bits[48] + "</td></tr>";
	    sb.append(stmp);

	    stmp = "</table>";
	    sb.append(stmp);

	    sb.append(footer);
	    final String html = sb.toString().trim();

	    current.post(new Runnable()
	    {
		    @Override
		    public void run()
		    {
			    current.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null);
		    }
	    });

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
	            forceRefresh();
	            reloadWebView(true);
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
            reloadWebView(false);
        if(!common.GetBoolPref("radarforecast", true))
        	reloadForecast(false);

        return updateFields();
    }

    private void forceRefresh()
    {
        common.getWeather();
	    wipeForecast();
	    reloadWebView(true);
    }

    private void loadWebView()
    {
    	if(common.GetBoolPref("radarforecast", true))
	    {
		    switch (common.GetStringPref("radtype", "image"))
		    {
			    case "image":
				    String radar = common.context.getFilesDir() + "/radar.gif";

				    File myFile = new File(radar);
				    Common.LogMessage("myFile == " + myFile.getAbsolutePath());
				    Common.LogMessage("myFile.exists() == " + myFile.exists());

				    if (radar.equals("") || !myFile.exists() || common.GetStringPref("RADAR_URL", "").equals(""))
				    {
					    final String html = "<html><body>Radar URL not set or is still downloading. You can go to settings to change.</body></html>";
					    wv.post(new Runnable()
					    {
						    @Override
						    public void run()
						    {
							    wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
						    }
					    });
					    return;
				    }

				    String tmphtml = "<!DOCTYPE html>\n" +
						    "<html>\n" +
						    "  <head>\n" +
						    "    <meta charset='utf-8'>\n" +
						    "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n";
				    if(dark_theme)
					    tmphtml += "<style>body{color: #fff; background-color: #000;}</style>";

				    tmphtml += "  </head>\n" +
						    "  <body>\n" +
						    "\t<img style='margin:0px;padding:0px;border:0px;text-align:center;max-width:100%;width:auto;height:auto;'\n" +
						    "\tsrc='file://" + radar + "'>\n" +
						    "  </body>\n" +
						    "</html>";
				    final String html = tmphtml;
				    wv.post(new Runnable()
				    {
					    @Override
					    public void run()
					    {
						    wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
					    }
				    });
				    break;
			    case "webpage":
				    wv.post(new Runnable()
				    {
					    @Override
					    public void run()
					    {
					    	wv.loadUrl(common.GetStringPref("RADAR_URL", ""));
					    }
				    });
				    break;
			    default:
				    tmphtml = "<html>";
				    if(dark_theme)
				    	tmphtml = "<head><style>body{color: #fff; background-color: #000;}</style></head>";
				    tmphtml += "<body>Radar URL not set or is still downloading. You can go to settings to change.</body></html>";
				    final String shtml = tmphtml;
				    wv.post(new Runnable()
				    {
					    @Override
					    public void run()
					    {
						    wv.loadDataWithBaseURL(null, shtml, "text/html", "utf-8", null);
					    }
				    });
				    break;
		    }
	    } else {
		    String fctype = common.GetStringPref("fctype", "Yahoo");
		    String data = common.GetStringPref("forecastData", "");

		    if(data.equals(""))
		    {
			    String tmphtml = "<html>";
			    if (dark_theme)
				    tmphtml = "<head><style>body{color: #fff; background-color: #000;}</style></head>";
			    tmphtml += "<body>Forecast URL not set or is still downloading. You can go to settings to change.</body></html>";
			    final String shtml = tmphtml;
			    wv.post(new Runnable()
			    {
				    @Override
				    public void run()
				    {
					    wv.loadDataWithBaseURL(null, shtml, "text/html", "utf-8", null);
				    }
			    });

			    return;
		    }

		    switch (fctype.toLowerCase())
		    {
			    case "yahoo":
			    {
				    String[] content = common.processYahoo(data);
				    if (content == null || content.length <= 0)
					    return;
				    String yahoo = "<img src='purple.png' height='29px'/><br/>";

				    String tmpfc = "<html>";
				    if (dark_theme)
					    tmpfc += "<head><style>body{color: #fff; background-color: #000;}</style></head>";
				    tmpfc += "<body style='text-align:center'>" + yahoo + content[0] + "</body></html>";

				    final String fc = tmpfc;

				    wv.post(new Runnable()
				    {
					    @Override
					    public void run()
					    {
						    wv.loadDataWithBaseURL("file:///android_res/drawable/", fc, "text/html", "utf-8", null);
					    }
				    });
				    break;
			    }
			    case "weatherzone":
			    {
				    String[] content = common.processWZ(data);
				    if (content == null || content.length <= 0)
					    return;

				    String wz = "<img src='wz.png' height='29px'/><br/>";
				    String tmpfc = "<html>";
				    if (dark_theme)
					    tmpfc += "<head><style>body{color: #fff; background-color: #000;}</style></head>";
				    tmpfc += "<body style='text-align:center'>" + wz + content[0] + "</body></html>";
				    final String fc = tmpfc;

				    wv.post(new Runnable()
				    {
					    @Override
					    public void run()
					    {
						    wv.loadDataWithBaseURL("file:///android_res/drawable/", fc, "text/html", "utf-8", null);
					    }
				    });
				    break;
			    }
			    case "yr.no":
			    {
				    String[] content = common.processYR(data);
				    if (content == null || content.length <= 0)
					    return;

				    String yrno = "<img src='yrno.png' height='29px'/><br/>";
				    String tmpfc = "<html>";
				    if (dark_theme)
					    tmpfc += "<head><style>body{color: #fff; background-color: #000;}</style></head>";
				    tmpfc += "<body style='text-align:center'>" + yrno + content[0] + "</body></html>";

				    final String fc = tmpfc;

				    wv.post(new Runnable()
				    {
					    @Override
					    public void run()
					    {
						    wv.loadDataWithBaseURL("file:///android_res/drawable/", fc, "text/html", "utf-8", null);
					    }
				    });
				    break;
			    }
			    case "bom.gov.au":
			    {
				    String[] content = common.processBOM(data);
				    if (content == null || content.length <= 0)
					    return;

				    String bom = "<img src='bom.png' height='29px'/><br/>";
				    String tmpfc = "<html>";
				    if (dark_theme)
					    tmpfc += "<head><style>body{color: #fff; background-color: #000;}</style></head>";
				    tmpfc += "<body style='text-align:center'>" + bom + content[0] + "</body></html>";

				    final String fc = tmpfc;

				    wv.post(new Runnable()
				    {
					    @Override
					    public void run()
					    {
						    wv.loadDataWithBaseURL("file:///android_res/drawable/", fc, "text/html", "utf-8", null);
					    }
				    });
				    break;
			    }
			    case "wmo.int":
			    {
				    String[] content = common.processWMO(data);
				    if (content == null || content.length <= 0)
					    return;

				    String wmo = "<img src='wmo.png' height='29px'/><br/>";
				    String tmpfc = "<html>";
				    if (dark_theme)
					    tmpfc += "<head><style>body{color: #fff; background-color: #000;}</style></head>";
				    tmpfc += "<body style='text-align:center'>" + wmo + content[0] + "</body></html>";

				    final String fc = tmpfc;

				    wv.post(new Runnable()
				    {
					    @Override
					    public void run()
					    {
						    wv.loadDataWithBaseURL("file:///android_res/drawable/", fc, "text/html", "utf-8", null);
					    }
				    });
				    break;
			    }
			    case "weather.gov":
			    {
				    String[] content = common.processWGOV(data);
				    if (content == null || content.length <= 0)
					    return;

				    String wgov = "<img src='wgov.png' height='29px'/><br/>";
				    String tmpfc = "<html>";
				    if (dark_theme)
					    tmpfc += "<head><style>body{color: #fff; background-color: #000;}</style></head>";
				    tmpfc += "<body style='text-align:center'>" + wgov + content[0] + "</body></html>";

				    final String fc = tmpfc;

				    wv.post(new Runnable()
				    {
					    @Override
					    public void run()
					    {
						    wv.loadDataWithBaseURL("file:///android_res/drawable/", fc, "text/html", "utf-8", null);
					    }
				    });
				    break;
			    }
			    case "weather.gc.ca":
			    {
				    String[] content = common.processWCA(data);
				    if (content == null || content.length <= 0)
					    return;

				    String wca = "<img src='wca.png' height='29px'/><br/>";
				    String tmpfc = "<html>";
				    if (dark_theme)
					    tmpfc += "<head><style>body{color: #fff; background-color: #000;}</style></head>";
				    tmpfc += "<body style='text-align:center'>" + wca + content[0] + "</body></html>";
				    final String fc = tmpfc;

				    wv.post(new Runnable()
				    {
					    @Override
					    public void run()
					    {
						    wv.loadDataWithBaseURL("file:///android_res/drawable/", fc, "text/html", "utf-8", null);
					    }
				    });
				    break;
			    }
			    case "metoffice.gov.uk":
			    {
				    String[] content = common.processMET(data);
				    if (content == null || content.length <= 0)
					    return;

				    String logo = "<img src='met.png' height='29px'/><br/>";
				    String tmpfc = "<html>";
				    if (dark_theme)
					    tmpfc += "<head><style>body{color: #fff; background-color: #000;}</style></head>";
				    tmpfc += "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
				    final String fc = tmpfc;

				    wv.post(new Runnable()
				    {
					    @Override
					    public void run()
					    {
						    wv.loadDataWithBaseURL("file:///android_res/drawable/", fc, "text/html", "utf-8", null);
					    }
				    });
				    break;
			    }
			    case "bom2":
			    {
				    String[] content = common.processBOM2(data);
				    if (content == null || content.length <= 0)
					    return;

				    String bom = "<img src='bom.png' height='29px'/><br/>";
				    String tmpfc = "<html>";
				    if (dark_theme)
					    tmpfc += "<head><style>body{color: #fff; background-color: #000;}</style></head>";
				    tmpfc += "<body style='text-align:center'>" + bom + content[0] + "</body></html>";

				    final String fc = tmpfc;

				    wv.post(new Runnable()
				    {
					    @Override
					    public void run()
					    {
						    wv.loadDataWithBaseURL("file:///android_res/drawable/", fc, "text/html", "utf-8", null);
					    }
				    });
				    break;
			    }
		    }
	    }
    }

	private void wipeForecast()
	{
		Common.LogMessage("wiping rss cache");
		common.SetIntPref("rssCheck", 0);
		common.SetStringPref("forecastData", "");
		reloadForecast(true);
	}

	private void reloadForecast(boolean force)
    {
	    if(common.GetBoolPref("radarforecast", true))
		    return;

	    final String forecast_url = common.GetStringPref("FORECAST_URL", "");

	    if(forecast_url.equals(""))
	    {
		    final String html = "<html><body>Forecast URL not set. Edit inigo-settings.txt to change.</body></html>";
		    wv.post(new Runnable()
		    {
			    @Override
			    public void run()
			    {
				    wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
			    }
		    });

		    return;
	    }

	    if(!common.checkWifiOnAndConnected() && !force)
	    {
	    	Common.LogMessage("Not on wifi and not a forced refresh");
		    if(swipeLayout.isRefreshing())
			    swipeLayout.setRefreshing(false);
		    return;
	    }

	    if(!swipeLayout.isRefreshing())
		    swipeLayout.setRefreshing(true);

	    Common.LogMessage("forecast checking: " + forecast_url);

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
						if(tmp != null)
						{
							Common.LogMessage("updating rss cache");
							common.SetIntPref("rssCheck", curtime);
							common.SetStringPref("forecastData", tmp);
						}
				    }
			    } catch (Exception e) {
				    e.printStackTrace();
			    }

			    handlerDone.sendEmptyMessage(0);
		    }
	    });

	    t.start();
    }

    private void reloadWebView(boolean force)
    {
	    if(!common.GetBoolPref("radarforecast", true))
	    	return;

        Common.LogMessage("reload radar...");
        final String radar = common.GetStringPref("RADAR_URL", "");

        if(radar.equals(""))
        {
	        loadWebView();
	        handlerDone.sendEmptyMessage(0);
            return;
        }

	    if(!common.checkWifiOnAndConnected() && !force)
	    {
		    Common.LogMessage("Not on wifi and not a forced refresh");
		    if(swipeLayout.isRefreshing())
			    swipeLayout.setRefreshing(false);
		    return;
	    }

	    if(!swipeLayout.isRefreshing())
		    swipeLayout.setRefreshing(true);

        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Common.LogMessage("starting to download image from: " + radar);
                    String fn = common.downloadRADAR(radar);
                    Common.LogMessage("done downloading " + fn + ", prompt handler to draw to movie");
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
			common.SendRefresh();
	        swipeLayout.setRefreshing(false);
        }
    };

    void doResume()
    {
	    IntentFilter filter = new IntentFilter();
	    filter.addAction(Common.UPDATE_INTENT);
	    filter.addAction(Common.EXIT_INTENT);
	    filter.addAction(Common.REFRESH_INTENT);
	    common.context.registerReceiver(serviceReceiver, filter);
	    Common.LogMessage("weather.java -- adding a new filter");
    }

    void doPause()
    {
	    try
	    {
		    common.context.unregisterReceiver(serviceReceiver);
	    } catch (Exception e) {
			//TODO: ignore this exception...
	    }
	    Common.LogMessage("weather.java -- unregisterReceiver");
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
	                dark_theme = common.GetBoolPref("dark_theme", false);
	                updateFields();
	                reloadWebView(false);
	                reloadForecast(false);
                } else if(action != null && action.equals(Common.REFRESH_INTENT)) {
	                Common.LogMessage("Weather() We have a hit, so we should probably update the screen.");
	                dark_theme = common.GetBoolPref("dark_theme", false);
	                loadWebView();
                } else if(action != null && action.equals(Common.EXIT_INTENT)) {
                    doPause();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}