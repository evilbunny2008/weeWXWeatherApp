package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class WidgetProvider extends AppWidgetProvider
{
    Common common = null;
	private int dark_theme;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Common.LogMessage("onReceive() called..");
		common = new Common(context);

		dark_theme = common.GetIntPref("dark_theme", 2);
		if(dark_theme == 2)
			dark_theme = common.getSystemTheme();

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = intent.getIntArrayExtra("appWidgetIds");

		if(appWidgetIds != null && appWidgetIds.length > 0)
			onUpdate(context, appWidgetManager, appWidgetIds);
	}

    @SuppressLint("UnspecifiedImmutableFlag")
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
		common = new Common(context);
		common.setAlarm("WidgetProvider");

	    dark_theme = common.GetIntPref("dark_theme", 2);
	    if(dark_theme == 2)
		    dark_theme = common.getSystemTheme();

        final int count = appWidgetIds.length;

        for(int i = 0; i < count; i++)
        {
            int widgetId = appWidgetIds[i];

            Common.LogMessage("appWidgetsIds["+i+"] = " + appWidgetIds[i]);
            RemoteViews remoteViews = common.buildUpdate(context, dark_theme);

            Intent launchActivity = new Intent(context, SplashScreen.class);

	        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchActivity, PendingIntent.FLAG_IMMUTABLE);
            remoteViews.setOnClickPendingIntent(R.id.widget, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }
}