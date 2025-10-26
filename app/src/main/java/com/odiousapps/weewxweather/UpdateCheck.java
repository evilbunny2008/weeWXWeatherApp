package com.odiousapps.weewxweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpdateCheck extends BroadcastReceiver
{
	@Override
	public void onReceive(final Context c, Intent i)
	{
		Common.LogMessage("UpdateCheck.java started.", true);
		if(c == null)
		{
			Common.LogMessage("UpdateCheck.java failed, c == null", true);
			return;
		}

		Common.setAlarm("UpdateCheck");

		if(!Common.checkConnection())
		{
			Common.LogMessage("Skipping update due to wifi setting.", true);
			return;
		}

		Common.reload();
		WidgetProvider.updateAppWidget();
		Common.getWeather();
		Common.getForecast();
		Common.LogMessage("UpdateCheck.java finished.", true);
	}
}