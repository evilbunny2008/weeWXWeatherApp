package com.odiousapps.weewxweather;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpdateCheck extends BroadcastReceiver
{
	private Common common;

	@Override
	public void onReceive(final Context c, Intent i)
	{
		Common.LogMessage("UpdateCheck.java started.");
		if(c == null)
		{
			Common.LogMessage("UpdateCheck.java failed, c == null");
			return;
		}

		common = new Common(c);

		final long[] ret = common.getPeriod();
		final long period = ret[0];
		final long wait = ret[1];
		if(period <= 0)
			return;

		final long start = Math.round((double)System.currentTimeMillis() / (double)period) * period + period + wait;

		Common.LogMessage("weewxstart == " + start);
		Common.LogMessage("weewxperiod == " + period);
		Common.LogMessage("weewxwait == " + wait);

		common.getWeather();
		AlarmManager mgr = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
		Intent myInent = new Intent(c, UpdateCheck.class);

		if(mgr != null)
		{
			PendingIntent pi = PendingIntent.getBroadcast(c, 0, myInent, 0);
			mgr.setExact(AlarmManager.RTC_WAKEUP, start, pi);
		}

		Common.LogMessage("UpdateCheck.java finished.");
	}
}