package com.odiousapps.weewxweather;

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
	public void onReceive(Context context, Intent intent)
	{
		Common.LogMessage("onReceive() called..");
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = intent.getIntArrayExtra("appWidgetIds");

		if(appWidgetIds != null && appWidgetIds.length > 0)
			onUpdate(context, appWidgetManager, appWidgetIds);
	}

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        common = new Common(context);
	    common.setAlarm("WidgetProvider");

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
    }
}