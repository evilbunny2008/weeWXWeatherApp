package com.odiousapps.weewxweather;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class myService extends Service
{
    static myService singleton = null;
    private Common common = null;
    Timer timer = null;
    Thread t = null;
    LockScreenReceiver lockScreenReceiver;

    static String UPDATE_INTENT = "com.odiousapps.weewxweather.UPDATE_INTENT";
    static String TAB0_INTENT = "com.odiousapps.weewxweather.TAB0_INTENT";
    static String EXIT_INTENT = "com.odiousapps.weewxweather.EXIT_INTENT";

    boolean doUpdate = true;

    public void onCreate()
    {
        super.onCreate();
        singleton = this;
        common = new Common(this);

        Reminder();
        Common.LogMessage("myService started.");

        lockScreenReceiver = new LockScreenReceiver();
        IntentFilter lockFilter = new IntentFilter();
        lockFilter.addAction(Intent.ACTION_SCREEN_ON);
        lockFilter.addAction(Intent.ACTION_SCREEN_OFF);
        lockFilter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(lockScreenReceiver, lockFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Common.LogMessage("WeeWXService marked START_STICKY");
        return Service.START_STICKY;
    }

    public boolean Update()
    {
        return doUpdate;
    }

    public void Reminder()
    {
        if(timer == null)
            timer = new Timer();

        timer.schedule(new myTimer(), 1,60000);
        Common.LogMessage("New timer set to repeat every 60000ms");
        getWeather();
        Common.LogMessage("Running getWeather();");
    }

    public void onDestroy()
    {
        super.onDestroy();

        Intent intent = new Intent();
        intent.setAction(EXIT_INTENT);
        sendBroadcast(intent);
        Common.LogMessage("myService Fired off exit broadcast.");

        unregisterReceiver(lockScreenReceiver);

        if (timer != null)
        {
            Common.LogMessage("I'm melting... MELTING...");
            timer.cancel();
            timer.purge();
            timer = null;
        }

        if(t != null)
        {
            if(t.isAlive())
                t.interrupt();
            t = null;
        }

        singleton = null;
        Common.LogMessage("myService stopped.");
    }

    class myTimer extends TimerTask
    {
        public void run()
        {
            Calendar calendar = Calendar.getInstance();
            int mins = calendar.get(Calendar.MINUTE);
            int secs = calendar.get(Calendar.SECOND);

            Common.LogMessage("mins:secs == "+mins+":"+secs);

            int pos = common.GetIntPref("updateInterval", 1);

            if(pos == 0)
                return;

            if(pos == 1)
            {
                switch (mins)
                {
                    case 1:
                    case 6:
                    case 11:
                    case 16:
                    case 21:
                    case 26:
                    case 31:
                    case 36:
                    case 41:
                    case 46:
                    case 51:
                    case 56:
                        break;
                    default:
                        return;
                }
            } else if(pos == 2) {
                switch (mins)
                {
                    case 1:
                    case 11:
                    case 21:
                    case 31:
                    case 41:
                    case 51:
                        break;
                    default:
                        return;
                }
            } else if(pos == 3) {
                switch (mins)
                {
                    case 1:
                    case 16:
                    case 31:
                    case 46:
                        break;
                    default:
                        return;
                }
            } else if(pos == 4) {
                switch (mins)
                {
                    case 1:
                    case 31:
                        break;
                    default:
                        return;
                }
            } else if(pos == 5) {
                switch (mins)
                {
                    case 1:
                        break;
                    default:
                        return;
                }
            }

            Common.LogMessage("Running getWeather();");
            getWeather();
        }
    }

    public void getWeather()
    {
        if(t != null)
        {
            if(t.isAlive())
                t.interrupt();
            t = null;
        }

        t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    URL url = new URL(common.GetStringPref("BASE_URL", ""));
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
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

                    if(doUpdate)
                        SendIntents();
                } catch (Exception e) {
                    Common.LogMessage(e.toString());
                }
            }
        });

        t.start();
    }

    private void SendIntents()
    {
        Intent intent = new Intent();
        intent.setAction(UPDATE_INTENT);
        sendBroadcast(intent);
        Common.LogMessage("Fired off first update broadcast.");

        RemoteViews remoteViews = common.buildUpdate(this);
        ComponentName thisWidget = new ComponentName(this, WidgetProvider.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        manager.updateAppWidget(thisWidget, remoteViews);
        Common.LogMessage("widget intent broadcasted");
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    public class LockScreenReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent != null && intent.getAction() != null)
            {
                switch(intent.getAction())
                {
                    case Intent.ACTION_SCREEN_ON:
                        Common.LogMessage("ACTION_SCREEN_ON");
                        return;
                    case Intent.ACTION_SCREEN_OFF:
                        Common.LogMessage("ACTION_SCREEN_OFF");
                        doUpdate = false;
                        // stop updating until the ACTION_USER_PRESENT is seen
                        return;
                    case Intent.ACTION_USER_PRESENT:
                        doUpdate = true;
                        SendIntents();
                        Common.LogMessage("ACTION_USER_PRESENT");
                }
            }
        }
    }
}