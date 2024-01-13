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
			Common.LogMessage("UpdateCheck.java failed, c == null");
			return;
		}

		Common common = new Common(c);
		common.setAlarm("UpdateCheck");

		if(!common.checkConnection())
		{
			Common.LogMessage("Skipping update due to wifi setting.", true);
			return;
		}

		common.getWeather();
		common.getForecast();
		Common.LogMessage("UpdateCheck.java finished.", true);
	}
}