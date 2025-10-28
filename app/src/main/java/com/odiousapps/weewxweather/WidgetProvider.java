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
		Common.LogMessage("WidgetProvider.onReceive() called.. intent.getAction()=" + intent.getAction());

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = intent.getIntArrayExtra("appWidgetIds");
		if(appWidgetIds != null && appWidgetIds.length > 0)
			updateAppWidget(context, appWidgetManager, appWidgetIds);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
	{
		Common.LogMessage("WidgetProvider.onUpdate() called..");
		Common.setAlarm("WidgetProvider.onUpdate()");
		updateAppWidget(context, appWidgetManager, appWidgetIds);
	}

	@Override
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions)
	{
		Common.setAlarm("WidgetProvider.onAppWidgetOptionsChanged()");
		Common.LogMessage("onAppWidgetOptionsChanged() called..");

		KeyValue.widgetMinWidth.put(appWidgetId, newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));
		KeyValue.widgetMinHeight.put(appWidgetId, newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
		KeyValue.widgetMaxWidth.put(appWidgetId, newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH));
		KeyValue.widgetMaxHeight.put(appWidgetId, newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));

		updateAppWidget(context, appWidgetManager, new int[]{appWidgetId});
	}

	static void updateAppWidget()
	{
		Context context = weeWXApp.getInstance().getApplicationContext();
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		ComponentName widgets = new ComponentName(context, WidgetProvider.class);
		int[] widgetIds = manager.getAppWidgetIds(widgets);
		updateAppWidget(context, manager, widgetIds);
	}

	public static void updateAppWidget(Context context, AppWidgetManager manager, int[] widgetIds)
	{
		Common.LogMessage("WidgetProvider.updateAppWidget() called..");
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
		Common.LogMessage("RemoteViews built: " + views);

		int bgColour = KeyValue.widgetBG;
		int fgColour = KeyValue.widgetFG;

		Common.LogMessage("fgColour = " + to_ARGB_hex(fgColour));
		Common.LogMessage("bgColour = " + to_ARGB_hex(bgColour));

		views.setInt(R.id.widget_frame, "setBackgroundColor", bgColour);

		views.setTextColor(R.id.widget_location, fgColour);
		views.setTextColor(R.id.widget_time, fgColour);
		views.setTextColor(R.id.widget_temperature, fgColour);
		views.setTextColor(R.id.widget_wind, fgColour);
		views.setTextColor(R.id.widget_rain, fgColour);

		views.setTextViewText(R.id.widget_location, "");
		views.setTextViewText(R.id.widget_time, "");
		views.setTextViewText(R.id.widget_temperature, "");
		views.setTextViewText(R.id.widget_wind, "");
		views.setTextViewText(R.id.widget_rain, "");

		String lastDownload = Common.GetStringPref("LastDownload", "");
		if(lastDownload != null && !lastDownload.isEmpty())
		{
			String[] bits = lastDownload.split("\\|");
			if(bits.length > 110)
			{
				String tmp = bits[20];
				if (bits.length > 158 && !bits[158].isEmpty())
					tmp = bits[158];

				tmp += bits[62];

				views.setTextViewText(R.id.widget_location, bits[56]);
				views.setTextViewText(R.id.widget_time, bits[55]);
				views.setTextViewText(R.id.widget_temperature, bits[0] + bits[60]);
				views.setTextViewText(R.id.widget_wind, bits[25] + bits[61]);
				views.setTextViewText(R.id.widget_rain, tmp);
				Common.LogMessage("Temperature set to " + bits[0] + bits[60]);
			} else {
				Common.LogMessage("Temperature set to Error!");
			}
		} else {
			Common.LogMessage("Temperature set to Error!");
		}

		Intent launchActivity = new Intent(context, MainActivity.class);
		launchActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchActivity, PendingIntent.FLAG_IMMUTABLE);
		views.setOnClickPendingIntent(R.id.widget_frame, pendingIntent);

		for(int widgetId : widgetIds)
		{
			if(KeyValue.widgetMinHeight.isEmpty())
			{
				Common.LogMessage("weeWXApp.widgetMinHeight isEmpty adding an entry...");
				KeyValue.widgetMinHeight.put(widgetId, 128);
			}

			try
			{
				Integer rawHeight = KeyValue.widgetMinHeight.get(widgetId);
				float textSize = 128 / 3f;

				if(rawHeight != null)
					textSize = rawHeight / 3f;

				views.setFloat(R.id.widget_temperature, "setTextSize", textSize);
				Common.LogMessage("Updating widgetId = " + widgetId + "'s textSize to " + textSize);

				manager.updateAppWidget(widgetId, views);
			} catch (Exception e) {
				Common.LogMessage("Widget update failed for id=" + widgetId + ", e=" + e);
				Common.doStackOutput(e);
			}
		}
	}
}