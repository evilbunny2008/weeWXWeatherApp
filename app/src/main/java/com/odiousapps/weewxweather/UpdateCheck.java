package com.odiousapps.weewxweather;

import android.app.AlarmManager;
import android.app.PendingIntent;
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

		Common common = new Common(c);
		common.setAlarm("UpdateCheck");

		if(common.GetBoolPref("onlyWIFI") && !common.checkWifiOnAndConnected())
		{
			Common.LogMessage("Skipping update due to wifi setting.");
			return;
		}

		common.getWeather();
		Common.LogMessage("UpdateCheck.java finished.");
	}
}