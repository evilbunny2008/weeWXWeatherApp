package com.odiousapps.weewxweather;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;


import static com.github.evilbunny2008.colourpicker.Common.to_ARGB_hex;

public class WidgetProvider extends AppWidgetProvider
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		weeWXAppCommon.LogMessage("WidgetProvider.onReceive() called.. intent.getAction()=" +
		                          intent.getAction());

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = intent.getIntArrayExtra("appWidgetIds");
		if(appWidgetIds != null && appWidgetIds.length > 0)
			updateAppWidget(context, appWidgetManager, appWidgetIds);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
	                     int[] appWidgetIds)
	{
		weeWXAppCommon.LogMessage("WidgetProvider.onUpdate() called..");
		updateAppWidget(context, appWidgetManager, appWidgetIds);
	}

	@Override
	public void onAppWidgetOptionsChanged(Context context,
	                                      AppWidgetManager appWidgetManager,
	                                      int appWidgetId, Bundle newOptions)
	{
		weeWXAppCommon.LogMessage("onAppWidgetOptionsChanged() called..");

		KeyValue.widgetMinWidth.put(appWidgetId,
				newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));
		KeyValue.widgetMinHeight.put(appWidgetId,
				newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
		KeyValue.widgetMaxWidth.put(appWidgetId,
				newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH));
		KeyValue.widgetMaxHeight.put(appWidgetId,
				newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));

		updateAppWidget(context, appWidgetManager, new int[]{appWidgetId});
	}

	static void setDefaultColoursAndText(Context context, RemoteViews views, int widgetBG, int widgetFG)
	{
		weeWXAppCommon.LogMessage("widgetBG = " + to_ARGB_hex(widgetBG));
		weeWXAppCommon.LogMessage("widgetFG = " + to_ARGB_hex(widgetFG));

		views.setInt(R.id.widget_frame, "setBackgroundColor", widgetBG);

		views.setTextColor(R.id.widget_location, widgetFG);
		views.setTextColor(R.id.widget_time, widgetFG);
		views.setTextColor(R.id.widget_temperature, widgetFG);
		views.setTextColor(R.id.widget_wind, widgetFG);
		views.setTextColor(R.id.widget_rain, widgetFG);

		views.setTextViewText(R.id.widget_location, "");
		views.setTextViewText(R.id.widget_time, "");
		views.setTextViewText(R.id.widget_wind, "");
		views.setTextViewText(R.id.widget_rain, "");
		views.setTextViewText(R.id.widget_temperature, "");

		Intent launchActivity = new Intent(context, MainActivity.class);
		launchActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
		                        Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(context,
				0, launchActivity, PendingIntent.FLAG_IMMUTABLE);
		views.setOnClickPendingIntent(R.id.widget_frame, pendingIntent);
	}

	static void resetAppWidget()
	{
		Context context = weeWXApp.getInstance();
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		ComponentName widgets = new ComponentName(context, WidgetProvider.class);
		int[] widgetIds = manager.getAppWidgetIds(widgets);
		resetAppWidget(context, manager, widgetIds);
	}

	static void resetAppWidget(Context context, AppWidgetManager manager, int[] widgetIds)
	{
		weeWXAppCommon.LogMessage("WidgetProvider.resetAppWidget() called..");

		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

		int widgetBG = weeWXApp.widgetBG_default;
		int widgetFG = weeWXApp.widgetFG_default;

		setDefaultColoursAndText(context, views, widgetBG, widgetFG);

		views.setTextViewText(R.id.widget_temperature, "Error!");

		for(int widgetId : widgetIds)
			manager.updateAppWidget(widgetId, views);
	}

	static void updateAppWidget()
	{
		Context context = weeWXApp.getInstance();
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		ComponentName widgets = new ComponentName(context, WidgetProvider.class);
		int[] widgetIds = manager.getAppWidgetIds(widgets);
		updateAppWidget(context, manager, widgetIds);
	}

	static void updateAppWidget(Context context, AppWidgetManager manager, int[] widgetIds)
	{
		weeWXAppCommon.LogMessage("WidgetProvider.updateAppWidget() called..");
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

		int widgetBG = (int)KeyValue.readVar("widgetBG", weeWXApp.widgetBG_default);
		int widgetFG = (int)KeyValue.readVar("widgetFG", weeWXApp.widgetFG_default);

		setDefaultColoursAndText(context, views, widgetBG, widgetFG);

		String tempText;
		//float approxCharWidthDp = 8f;

		String lastDownload = (String)KeyValue.readVar("LastDownload", null);
		if(lastDownload != null && !lastDownload.isBlank())
		{
			String[] bits = lastDownload.split("\\|");
			String rain = bits[20];
			if(bits.length > 158 && !bits[158].isBlank())
				rain = bits[158];

			rain += bits[62];

			tempText = bits[0] + bits[60];
			weeWXAppCommon.LogMessage("Temperature set to " + tempText);

			views.setTextViewText(R.id.widget_location, bits[56]);
			views.setTextViewText(R.id.widget_time, bits[55]);
			views.setTextViewText(R.id.widget_wind, bits[25] + bits[61]);
			views.setTextViewText(R.id.widget_rain, rain);
		} else {
			weeWXAppCommon.LogMessage("Temperature set to Error!");
			tempText = "Error!";
		}

		views.setTextViewText(R.id.widget_temperature, tempText);

		for(int widgetId: widgetIds)
			manager.updateAppWidget(widgetId, views);
	}
}