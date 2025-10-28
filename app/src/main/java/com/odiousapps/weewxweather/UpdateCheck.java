package com.odiousapps.weewxweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpdateCheck extends BroadcastReceiver
{
	@Override
	public void onReceive(final Context c, Intent i)
	{
		Common.LogMessage("UpdateCheck.java started.");
		if(c == null)
		{
			Common.LogMessage("UpdateCheck.java failed, c == null");
			return;
		}

		Common.setAlarm("UpdateCheck");

		if(!Common.checkConnection())
		{
			Common.LogMessage("Skipping update due to wifi setting.");
			return;
		}

		Common.reload();
		WidgetProvider.updateAppWidget();
		Common.getWeather();
		Common.getForecast();
		Common.LogMessage("UpdateCheck.java finished.");
	}
}