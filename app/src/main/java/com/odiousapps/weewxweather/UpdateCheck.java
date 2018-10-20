package com.odiousapps.weewxweather;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpdateCheck extends BroadcastReceiver
{
	private Context context;
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

		context = c;
		common = new Common(context);
		common.getWeather();

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

				int pos = common.GetIntPref("updateInterval", 1);
				if(pos <= 0)
					return;

				long period;

				switch(pos)
				{
					case 1:
						period = 5 * 60000;
						break;
					case 2:
						period = 10 * 60000;
						break;
					case 3:
						period = 15 * 60000;
						break;
					case 4:
						period = 30 * 60000;
						break;
					case 5:
						period = 60 * 60000;
						break;
					default:
						return;
				}

				AlarmManager mgr = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
				Intent myInent = new Intent(c, UpdateCheck.class);

				if(mgr != null)
				{
					long start = Math.round((double)System.currentTimeMillis() / (double)period) * period + period + 45000;
					Common.LogMessage("weewxstart == " + start);
					Common.LogMessage("weewxperiod == " + period);

					PendingIntent pi = PendingIntent.getBroadcast(c, 0, myInent, PendingIntent.FLAG_UPDATE_CURRENT);
					//mgr.setInexactRepeating(AlarmManager.RTC, start, period, pi);
					mgr.setExact(AlarmManager.RTC_WAKEUP, start, pi);
				}

				Common.LogMessage("UpdateCheck.java finished.");
			}
		});

		t.start();
	}
}
