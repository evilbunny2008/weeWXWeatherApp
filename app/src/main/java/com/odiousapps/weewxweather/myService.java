package com.odiousapps.weewxweather;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.IBinder;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
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

    boolean widgetUpdate = true;
    boolean doUpdate = true;

    public void onCreate()
    {
        super.onCreate();
        singleton = this;
        common = new Common(this);

        Common.LogMessage("myService started.");

        lockScreenReceiver = new LockScreenReceiver();
        IntentFilter lockFilter = new IntentFilter();
        lockFilter.addAction(Intent.ACTION_SCREEN_ON);
        lockFilter.addAction(Intent.ACTION_SCREEN_OFF);
        lockFilter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(lockScreenReceiver, lockFilter);

        startTimer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
	    if(common.GetBoolPref("bgdl", false))
	    {
		    Common.LogMessage("weeWXService marked START_STICKY");
		    return Service.START_STICKY;
	    }

	    return Service.START_NOT_STICKY;
    }

    void stopTimer()
    {
        if (timer != null)
        {
            Common.LogMessage("Stopping timer thread");
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    public void startTimer()
    {
        if(timer == null)
            timer = new Timer();

        int pos = common.GetIntPref("updateInterval", 1);
        if(pos <= 0)
            return;

        int period;

        Calendar date = Calendar.getInstance();

        switch(pos)
        {
            case 1:
                date.set(Calendar.MINUTE, 5);
                period = 5 * 60000;
                break;
            case 2:
                date.set(Calendar.MINUTE, 10);
                period = 10 * 60000;
                break;
            case 3:
                date.set(Calendar.MINUTE, 15);
                period = 15 * 60000;
                break;
            case 4:
                date.set(Calendar.MINUTE, 15);
                period = 30 * 60000;
                break;
            case 5:
                date.set(Calendar.MINUTE, 60);
                period = 60 * 60000;
                break;
            default:
                return;
        }

        date.set(Calendar.SECOND, 30);
        date.set(Calendar.MILLISECOND, 0);


        timer.scheduleAtFixedRate(new myTimer(), date.getTime(), period);
        Common.LogMessage("New timer set to repeat every " + period + "ms");
    }

    public void onDestroy()
    {
        super.onDestroy();

        Intent intent = new Intent();
        intent.setAction(EXIT_INTENT);
        sendBroadcast(intent);
        Common.LogMessage("myService Fired off exit broadcast.");

        unregisterReceiver(lockScreenReceiver);

        stopTimer();

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
            int hours = calendar.get(Calendar.HOUR_OF_DAY);
            int mins = calendar.get(Calendar.MINUTE);
            int secs = calendar.get(Calendar.SECOND);

            Common.LogMessage("hour:min:sec == "+ hours + ":" + mins + ":" + secs);

            int pos = common.GetIntPref("updateInterval", 1);

            if(pos == 0)
                return;

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
                	String data = common.GetStringPref("BASE_URL", "");
                	if(data.equals(""))
                		return;

	                Uri uri = Uri.parse(data);
	                if (uri.getUserInfo() != null && uri.getUserInfo().contains(":"))
	                {
		                final String[] UC = uri.getUserInfo().split(":");
		                Common.LogMessage("uri username = " + uri.getUserInfo());

		                if (UC.length > 1)
		                {
			                Authenticator.setDefault(new Authenticator()
			                {
				                protected PasswordAuthentication getPasswordAuthentication()
				                {
					                return new PasswordAuthentication(UC[0], UC[1].toCharArray());
				                }
			                });
		                }
	                }

                    URL url = new URL(data);
                    HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
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
                    common.SetLongPref("LastDownloadTime", Math.round(System.currentTimeMillis() / 1000));
                    SendIntents();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        t.start();
    }

    void SendIntents()
    {
        if(doUpdate)
        {
            Intent intent = new Intent();
            intent.setAction(UPDATE_INTENT);
            sendBroadcast(intent);
            Common.LogMessage("update_intent broadcast.");
        }

        if(widgetUpdate)
        {
            RemoteViews remoteViews = common.buildUpdate(this);
            ComponentName thisWidget = new ComponentName(this, WidgetProvider.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, remoteViews);
            Common.LogMessage("widget intent broadcasted");
        }
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
                        widgetUpdate = false;
                        return;
                    case Intent.ACTION_USER_PRESENT:
                        Common.LogMessage("ACTION_USER_PRESENT");
                        widgetUpdate = true;
                        SendIntents();
                }
            }
        }
    }
}