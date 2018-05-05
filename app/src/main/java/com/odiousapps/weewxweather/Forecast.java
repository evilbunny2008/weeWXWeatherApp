package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
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
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class Forecast
{
    private Common common;
    private View rootView;
    private WebView wv;
    private SwipeRefreshLayout swipeLayout;
	private ProgressDialog dialog;

	Forecast(Common common)
    {
        this.common = common;
    }

    View myForecast(LayoutInflater inflater, ViewGroup container)
    {
        rootView = inflater.inflate(R.layout.fragment_forecast, container, false);
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

	    swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
	    swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
	    {
		    @Override
		    public void onRefresh()
		    {
			    swipeLayout.setRefreshing(true);
			    Common.LogMessage("onRefresh();");
			    forceRefresh();
		    }
	    });

        wv = rootView.findViewById(R.id.webView1);
	    wv.getSettings().setUserAgentString(Common.UA);
        wv.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator != null)
                    vibrator.vibrate(250);
                Common.LogMessage("webview long press");
                forceRefresh();
                return true;
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

	    IntentFilter filter = new IntentFilter();
	    filter.addAction(myService.UPDATE_INTENT);
	    filter.addAction(myService.EXIT_INTENT);
	    common.context.registerReceiver(serviceReceiver, filter);

	    getForecast();

        return rootView;
    }

    public void doStop()
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
                if(action != null && action.equals(myService.UPDATE_INTENT))
                {
                    getForecast();
                } else if(action != null && action.equals(myService.EXIT_INTENT)) {
                    common.context.unregisterReceiver(serviceReceiver);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void forceRefresh()
    {
        Common.LogMessage("wiping rss cache");
        common.SetIntPref("rssCheck", 0);
        common.SetStringPref("forecastData", "");
        getForecast();
    }

    private void getForecast()
    {
        final String rss = common.GetStringPref("FORECAST_URL", "");
        if(rss.equals(""))
        {
	        wv.loadDataWithBaseURL(null, "<html><body>Forecast URL not set, go to settings to change</body></html>", "text/html", "utf-8", null);
	        return;
        }

	    swipeLayout.setRefreshing(false);

	    dialog = ProgressDialog.show(common.context, "Updating Forecast", "Please wait while we get some fresh data", false);
	    dialog.show();

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

                    if(common.GetStringPref("forecastData", "").equals("") || common.GetIntPref("rssCheck", 0) + 3600 < curtime)
                    {
                        Common.LogMessage("no forecast data or cache is more than 3 hour old");
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

    @SuppressLint("HandlerLeak")
    private Handler handlerDone = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            generateForecast();
        }
    };

    private void generateForecast()
    {
        Common.LogMessage("getting json data");
        String data;
        String fctype = common.GetStringPref("fctype", "Yahoo");

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

	    if(dialog != null)
	    {
		    dialog.dismiss();
		    dialog = null;
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

	    ImageView im = rootView.findViewById(R.id.logo);
	    if(common.GetStringPref("fctype", "yahoo").toLowerCase().equals("yahoo"))
	    {
		    im.setImageResource(R.drawable.purple);
	    } else if(common.GetStringPref("fctype", "yahoo").toLowerCase().equals("weatherzone")) {
	        im.setImageResource(R.drawable.wz);
        }
    }
}