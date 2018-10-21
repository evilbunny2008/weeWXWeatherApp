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

		Thread t = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					Common.LogMessage("UpdateCheck.java try");
				} catch (Exception e) {
					Common.LogMessage("UpdateCheck.java exception: " + e.toString());
				}

				long[] ret = common.getPeriod();
				long period = ret[0];
				long wait = ret[1];
				if(period <= 0)
					return;

				common.getWeather();


				AlarmManager mgr = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
				Intent myInent = new Intent(c, UpdateCheck.class);

				if(mgr != null)
				{
					long start = Math.round((double)System.currentTimeMillis() / (double)period) * period + period + wait;
					Common.LogMessage("weewxstart == " + start);
					Common.LogMessage("weewxperiod == " + period);
					Common.LogMessage("weewxwait == " + wait);
					PendingIntent pi = PendingIntent.getBroadcast(c, 0, myInent, PendingIntent.FLAG_UPDATE_CURRENT);
					mgr.setExact(AlarmManager.RTC_WAKEUP, start, pi);
				}

				Common.LogMessage("UpdateCheck.java finished.");
			}
		});

		t.start();
	}
}
