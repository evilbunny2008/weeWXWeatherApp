package com.odiousapps.weewxweather;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RNAutoStart extends BroadcastReceiver
{
    public void onReceive(Context c, Intent i)
    {
        Common.LogMessage("Broadcast intent detected " + i.getAction());

        Common.LogMessage("RNAutostart: i=" + i.toString());
        Common common = new Common(c);
	    common.getWeather();

	    if(common.GetBoolPref("bgdl", true))
        {
        	try
	        {
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
		        Intent myIntent = new Intent(c, UpdateCheck.class);

		        if(mgr != null)
		        {
			        PendingIntent pi = PendingIntent.getBroadcast(c, 0, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			        mgr.cancel(pi);

			        long start = Math.round((double)System.currentTimeMillis() / (double)period) * period + 45000;
			        Common.LogMessage("weewxstart == " + start);
			        Common.LogMessage("weewxperiod == " + period);

			        pi = PendingIntent.getBroadcast(c, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			        //mgr.setInexactRepeating(AlarmManager.RTC, start, period, pi);
			        mgr.setExact(AlarmManager.RTC_WAKEUP, start, pi);
		        }

		        Common.LogMessage("onReceive() end");
	        } catch (Exception e) {
        		e.printStackTrace();
	        }
        }
    }
}