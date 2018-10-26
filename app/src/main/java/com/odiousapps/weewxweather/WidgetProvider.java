package com.odiousapps.weewxweather;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class WidgetProvider extends AppWidgetProvider
{
    Common common = null;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        common = new Common(context);

        final int count = appWidgetIds.length;

        for(int i = 0; i < count; i++)
        {
            int widgetId = appWidgetIds[i];

            Common.LogMessage("appWidgetsIds["+i+"] = " + appWidgetIds[i]);
            RemoteViews remoteViews = common.buildUpdate(context);

            Intent launchActivity = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchActivity, 0);
            remoteViews.setOnClickPendingIntent(R.id.widget, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }

	    long[] ret = common.getPeriod();
	    long period = ret[0];
	    long wait = ret[1];
	    if(period <= 0)
		    return;

	    long start = Math.round((double)System.currentTimeMillis() / (double)period) * period + wait;

	    Common.LogMessage("WidgetProvider - weewxstart == " + start);
	    Common.LogMessage("WidgetProvider - weewxperiod == " + period);
	    Common.LogMessage("WidgetProvider - weewxwait == " + wait);

	    AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
	    Intent myIntent = new Intent(context, UpdateCheck.class);
	    PendingIntent pi = PendingIntent.getBroadcast(context, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);

	    if(mgr != null)
		    mgr.setExact(AlarmManager.RTC_WAKEUP, start, pi);
    }
}