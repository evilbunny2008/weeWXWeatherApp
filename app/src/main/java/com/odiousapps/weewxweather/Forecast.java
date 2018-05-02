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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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
		    Common.LogMessage("already unregistered");
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
		    doYahoo(data);
	    else if(fctype.toLowerCase().equals("weatherzone"))
		    doWZ(data);

	    if(dialog != null)
	    {
		    dialog.dismiss();
		    dialog = null;
	    }
    }

	private void doWZ(String data)
	{
		try
		{
			String desc = "", content = "", pubDate = "";

			String[] bits = data.split("<title>");
			if(bits.length >= 2)
				desc = bits[1].split("</title>")[0].trim();

			bits = data.split("<description>");
			if(bits.length >= 3)
			{
				String s = bits[2].split("</description>")[0];
				content = s.substring(9, s.length() - 4).trim();
			}

			bits = data.split("<pubDate>");
			if(bits.length >= 2)
				pubDate = bits[1].split("</pubDate>")[0].trim();

			SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault());
			long mdate = sdf.parse(pubDate).getTime();
			sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
			pubDate = sdf.format(mdate);

			content = content.replace("http://www.weatherzone.com.au/images/icons/fcast_30/", "file:///android_res/drawable/wz")
						.replace(".gif", ".png");

			content = "<div style='font-size:16pt;'>" + pubDate + "</div><br/><br/>" + content;

			Common.LogMessage("content="+content);

			updateForecast(content, desc);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    private void doYahoo(String data)
    {
	    JSONObject json;

    	try
	    {
		    json = new JSONObject(data);

		    Common.LogMessage("starting JSON Parsing");

		    JSONObject query = json.getJSONObject("query");
		    JSONObject results = query.getJSONObject("results");
		    JSONObject channel = results.getJSONObject("channel");
		    JSONObject item = channel.getJSONObject("item");
		    JSONObject units = channel.getJSONObject("units");
		    String temp = units.getString("temperature");
		    final String desc = channel.getString("description").substring(19);
		    JSONArray forecast = item.getJSONArray("forecast");
		    Common.LogMessage("ended JSON Parsing");

		    StringBuilder str = new StringBuilder();

		    Calendar calendar = Calendar.getInstance();
		    int hour = calendar.get(Calendar.HOUR_OF_DAY);

		    int start = 0;
		    if (hour >= 15)
			    start = 1;

		    JSONObject tmp = forecast.getJSONObject(start);
		    int code = tmp.getInt("code");
		    String stmp;

		    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
		    long rssCheck = common.GetIntPref("rssCheck", 0);
		    rssCheck *= 1000;
		    Date resultdate = new Date(rssCheck);


		    stmp = "<table style='width:100%;border:0px;'>";
		    str.append(stmp);
		    stmp = "<tr><td style='width:50%;font-size:16pt;'>" + tmp.getString("date") + "</td>";
		    str.append(stmp);
		    stmp = "<td style='width:50%;text-align:right;' rowspan='2'><img width='80px' src='file:///android_res/drawable/yahoo" + code + "'><br/>" +
				    sdf.format(resultdate) + "</td></tr>";
		    str.append(stmp);

		    stmp = "<tr><td style='width:50%;font-size:48pt;'>" + tmp.getString("high") + "&deg;" + temp + "</td></tr>";
		    str.append(stmp);

		    stmp = "<tr><td style='font-size:16pt;'>" + tmp.getString("low") + "&deg;" + temp + "</td>";
		    str.append(stmp);

		    stmp = "<td style='text-align:right;font-size:16pt;'>" + tmp.getString("text") + "</td></tr></table><br>";
		    str.append(stmp);

		    stmp = "<table style='width:100%;border:0px;'>";
		    str.append(stmp);

		    for (int i = start + 1; i <= start + 5; i++)
		    {
			    tmp = forecast.getJSONObject(i);
			    code = tmp.getInt("code");

			    stmp = "<tr><td style='width:10%;' rowspan='2'>" + "<img width='40px' src='file:///android_res/drawable/yahoo" + code + "'></td>";
			    str.append(stmp);

			    stmp = "<td style='width:45%;'><b>" + tmp.getString("day") + ", " + tmp.getString("date") + "</b></td>";
			    str.append(stmp);

			    stmp = "<td style='width:45%;text-align:right;'><b>" + tmp.getString("high") + "&deg;" + temp + "</b></td></tr>";
			    str.append(stmp);

			    stmp = "<tr><td>" + tmp.getString("text") + "</td>";
			    str.append(stmp);

			    stmp = "<td style='text-align:right;'>" + tmp.getString("low") + "&deg;" + temp + "</td></tr>";
			    str.append(stmp);

			    stmp = "<tr><td style='font-size:10pt;' colspan='5'>&nbsp;</td></tr>";
			    str.append(stmp);
		    }

		    stmp = "</table>";
		    str.append(stmp);

		    Common.LogMessage("finished building forecast: " + str.toString());
		    updateForecast(str.toString(), desc);
	    } catch (Exception e) {
    		e.printStackTrace();
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