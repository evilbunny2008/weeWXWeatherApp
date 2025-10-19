package com.odiousapps.weewxweather;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class WidgetProvider extends AppWidgetProvider
{
	Common common = null;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Common.LogMessage("onReceive() called..");
		common = new Common(context);

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = intent.getIntArrayExtra("appWidgetIds");

		if(appWidgetIds != null && appWidgetIds.length > 0)
			onUpdate(context, appWidgetManager, appWidgetIds);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
	{
		common = new Common(context);
		common.setAlarm("WidgetProvider.onUpdate()");
		common.buildUpdate(context);
	}
}