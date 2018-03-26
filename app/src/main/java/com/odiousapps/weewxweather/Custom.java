package com.odiousapps.weewxweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class Custom
{
    private Common common;
    private WebView wv;

    Custom(Common common)
    {
        this.common = common;
    }

    View myCustom(LayoutInflater inflater, ViewGroup container)
    {
        View rootView = inflater.inflate(R.layout.fragment_webcam, container, false);
        wv = rootView.findViewById(R.id.webcam);
        wv.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(150);
                Common.LogMessage("long press");
                reloadWebView();
                return true;
            }
        });
        reloadWebView();

        IntentFilter filter = new IntentFilter();
        filter.addAction(myService.UPDATE_INTENT);
        filter.addAction(myService.EXIT_INTENT);
        common.context.registerReceiver(serviceReceiver, filter);

        return rootView;
    }

    protected void reloadWebView()
    {
        Common.LogMessage("reload custom...");

        String custom = common.GetStringPref("CUSTOM_URL", "");

        if (custom == null || custom.equals(""))
            return;

        wv.loadUrl(custom);
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