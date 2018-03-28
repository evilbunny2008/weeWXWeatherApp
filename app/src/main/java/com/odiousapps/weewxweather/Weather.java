package com.odiousapps.weewxweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

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

        checkFields((TextView)rootView.findViewById(R.id.textView4), bits[25] + bits[61]);
        checkFields((TextView)rootView.findViewById(R.id.textView5), bits[37] + bits[63]);
        checkFields((TextView)rootView.findViewById(R.id.textView6), bits[29]);
        checkFields((TextView)rootView.findViewById(R.id.textView7), bits[6] + bits[64]);
        checkFields((TextView)rootView.findViewById(R.id.textView8), bits[20] + bits[62]);
        checkFields((TextView)rootView.findViewById(R.id.textView9), bits[12] + bits[60]);
        checkFields((TextView)rootView.findViewById(R.id.textView10), bits[45] + "UVI");
        checkFields((TextView)rootView.findViewById(R.id.textView11), bits[43] + "W/m\u00B2");

        checkFields((TextView)rootView.findViewById(R.id.textView12), bits[57]);
        checkFields((TextView)rootView.findViewById(R.id.textView13), bits[58]);
        checkFields((TextView)rootView.findViewById(R.id.textView14), bits[47]);
        checkFields((TextView)rootView.findViewById(R.id.textView15), bits[48]);

        return rootView;
    }

    View myWeather(LayoutInflater inflater, ViewGroup container)
    {
        rootView = inflater.inflate(R.layout.fragment_weather, container, false);
        rootView.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator != null)
                    vibrator.vibrate(150);
                Common.LogMessage("rootview long press");
                forceRefresh();
                return true;
            }
        });

        wv = rootView.findViewById(R.id.webView1);
        wv.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator != null)
                    vibrator.vibrate(150);
                Common.LogMessage("webview long press");
                reloadWebView();
                return true;
            }
        });
        reloadWebView();

        IntentFilter filter = new IntentFilter();
        filter.addAction(myService.UPDATE_INTENT);
        filter.addAction(myService.EXIT_INTENT);
        common.context.registerReceiver(serviceReceiver, filter);

        return updateFields();
    }

    private void forceRefresh()
    {
        if(myService.singleton != null)
            myService.singleton.getWeather();
    }

    private void reloadWebView()
    {
        Common.LogMessage("reload radar...");
        wv.getSettings().setAppCacheEnabled(false);
        wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        wv.getSettings().setUserAgentString(Common.UA);
        wv.clearCache(true);
        String radar = common.GetStringPref("RADAR_URL", "");

        if (radar != null && !radar.equals(""))
        {
            String html = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "  <head>\n" +
                    "    <meta charset='utf-8'>\n" +
                    "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                    "  </head>\n" +
                    "  <body>\n" +
                    "\t<br>\n" +
                    "\t<img style='margin:0px;padding:0px;border:0px;text-align:center;max-width:100%;width:auto;height:auto;'\n" +
                    "\tsrc='" + radar + "'>\n" +
                    "  </body>\n" +
                    "</html>";
            wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
        } else {
            String html = "<html><body>Radar URL not set, go to settings to change</body></html>";
            wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
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
                    updateFields();
                    reloadWebView();
                } else if(action != null && action.equals(myService.EXIT_INTENT)) {
                    common.context.unregisterReceiver(serviceReceiver);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}