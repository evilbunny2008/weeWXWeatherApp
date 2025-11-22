package com.odiousapps.weewxweather;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

@SuppressWarnings("unused")
public class UpdateCheck extends BroadcastReceiver
{
	private final static ExecutorService executor = Executors.newSingleThreadExecutor();

	private static Future<?> backgroundTask;

	private static long bgStart;

	@Override
	public void onReceive(Context context, Intent i)
	{
		weeWXAppCommon.LogMessage("UpdateCheck.java onReceive() i.getAction(): " + i.getAction(), true);

		if(context == null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java failed, context == null", true);
			return;
		}

		if(!weeWXAppCommon.UPDATECHECK.equals(i.getAction()))
		{
			weeWXAppCommon.LogMessage(i.getAction() + " != " + weeWXAppCommon.UPDATECHECK + ", skipping...", true);
			return;
		}

		if(!weeWXAppCommon.checkConnection())
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Skipping update due to wifi setting.", true);
			return;
		}

		int pos = weeWXAppCommon.GetIntPref("updateInterval", weeWXApp.updateInterval_default);
		if(pos <= 0)
			return;

		long lastDownloadTime = weeWXAppCommon.GetLongPref("LastDownloadTime", weeWXApp.LastDownloadTime_default);
		if(lastDownloadTime == 0)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java failed, lastDownloadTime == 0", true);
			return;
		}

		weeWXAppCommon.LogMessage("UpdateCheck.java started.", true);

		setNextAlarm();

		runInTheBackground(true, false);

		weeWXAppCommon.LogMessage("UpdateCheck.java finished.", true);
	}

	private static PendingIntent getPendingIntent(Context context, boolean doNoCreate)
	{
		Intent intent = new Intent(context, UpdateCheck.class);
		intent.setAction(weeWXAppCommon.UPDATECHECK);

		int flags = PendingIntent.FLAG_IMMUTABLE;

		if(doNoCreate)
			flags |= PendingIntent.FLAG_NO_CREATE;

		return PendingIntent.getBroadcast(context, 0, intent, flags);
	}

	static void setNextAlarm()
	{
		weeWXAppCommon.LogMessage("UpdateCheck.java Attempting to set the reoccurring alarm...");

		Context context = weeWXApp.getInstance();
		if(context == null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java failed, context == null");
			return;
		}

		if(getPendingIntent(context, true) != null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Reoccurring alarm already set, did you forget to call cancel first? Skipping...");
			return;
		}

		long lastDownloadTime = weeWXAppCommon.GetLongPref("LastDownloadTime", weeWXApp.LastDownloadTime_default);
		if(lastDownloadTime == 0)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java failed, lastDownloadTime == 0");
			return;
		}

		long now = System.currentTimeMillis();
		long[] ret = weeWXAppCommon.getPeriod();
		long period = ret[0];
		long wait = ret[1];

		if(period <= 0)
			return;

		long start = Math.round((double)now / (double)period) * period;

/*
		if(BuildConfig.DEBUG)
		{
			start = now;
			period = 30_000L;
			wait = 5_000L;
		}
*/
		start += wait;

		while(start < now)
			start += period;

		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault());
		String string_time = sdf.format(now);

		weeWXAppCommon.LogMessage("UpdateCheck.java now: " + string_time, true);

		string_time = sdf.format(start);
		weeWXAppCommon.LogMessage("UpdateCheck.java start: " + string_time, true);
		weeWXAppCommon.LogMessage("UpdateCheck.java period: " + Math.round(period / 1_000D) + "s", true);
		weeWXAppCommon.LogMessage("UpdateCheck.java wait: " + Math.round(wait / 1_000D) + "s", true);

		weeWXAppCommon.LogMessage("UpdateCheck.java secs to next start: " + Math.round((start - now) / 1_000D) + "s", true);

		AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		if(alarm != null)
		{
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			{
				if(!alarm.canScheduleExactAlarms())
				{
					Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
					intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
					intent.setData(Uri.parse("package:" + context.getPackageName()));
					context.startActivity(intent);
				} else {
					alarm.setExact(AlarmManager.RTC_WAKEUP, start, getPendingIntent(context, false));
					weeWXAppCommon.LogMessage("UpdateCheck.java Successfully set the alarm...", true);
				}
			} else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				if(!alarm.canScheduleExactAlarms())
				{
					Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
					intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent);
				} else {
					alarm.setExact(AlarmManager.RTC_WAKEUP, start, getPendingIntent(context, false));
					weeWXAppCommon.LogMessage("UpdateCheck.java Successfully set the alarm...", true);
				}
			} else {
				alarm.setExact(AlarmManager.RTC_WAKEUP, start, getPendingIntent(context, false));
				weeWXAppCommon.LogMessage("UpdateCheck.java Successfully set the alarm...", true);
			}
		}
	}

	static void cancelAlarm()
	{
		weeWXAppCommon.LogMessage("UpdateCheck.java Attempting to cancel the current alarm...");

		Context context = weeWXApp.getInstance();
		if(context == null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java failed, context == null");
			return;
		}

		PendingIntent pi = getPendingIntent(context, true);

		if(pi == null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java an alarm wasn't set, skipping...");
			return;
		}

		pi.cancel();
		pi = getPendingIntent(context, false);

		if(backgroundTask != null && !backgroundTask.isDone())
		{
			backgroundTask.cancel(true);
			backgroundTask = null;
		}

		AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pi);
		pi.cancel();

		weeWXAppCommon.LogMessage("UpdateCheck.java Successfully cancelled the reoccurring alarm...");
	}

	static void runInTheBackground(boolean onReceivedUpdate, boolean onAppStart)
	{
		weeWXAppCommon.LogMessage("UpdateCheck.java runInTheBackground() running the background updates...");

		Context context = weeWXApp.getInstance();
		if(context == null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java failed, context == null");
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		int pos = weeWXAppCommon.GetIntPref("updateInterval", weeWXApp.updateInterval_default);
		if(pos < 0)
		{
			weeWXAppCommon.LogMessage("Invalid update frequency...");
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		if(pos == 0)
		{
			weeWXAppCommon.LogMessage("update interval set to manual update... skipping...");
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		if(!weeWXAppCommon.checkConnection())
		{
			weeWXAppCommon.LogMessage("WiFi needed but unavailable... skipping...");
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		weeWXAppCommon.LogMessage("update interval set to: " + weeWXApp.updateOptions[pos], true);

		if(!onReceivedUpdate)
		{
			long now = System.currentTimeMillis();
			long[] ret = weeWXAppCommon.getPeriod();
			long period = ret[0];
			long wait = ret[1];

			if(period <= 0)
				return;

			long start = Math.round((double)now / (double)period) * period;

			start += wait;

			while(start < now)
				start += period;

			long lastStart = start - period;

			long lastDownloadTime = weeWXAppCommon.GetLongPref("LastDownloadTime", weeWXApp.LastDownloadTime_default) * 1_000L;

			SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault());

			String string_time = sdf.format(lastDownloadTime);
			weeWXAppCommon.LogMessage("UpdateCheck.java lastDownloadTime: " + string_time, true);

			string_time = sdf.format(lastStart);
			weeWXAppCommon.LogMessage("UpdateCheck.java lastStart: " + string_time, true);

			if(lastDownloadTime >= lastStart)
			{
				weeWXAppCommon.LogMessage("Updated since lastStart time... skipping...", true);
				if(!weeWXApp.hasBootedFully)
					weeWXApp.hasBootedFully = true;
				return;
			}
		}

		long current_time = weeWXAppCommon.getCurrTime();

		if(backgroundTask != null && !backgroundTask.isDone())
		{
			if(bgStart + 30 > current_time)
			{
				weeWXAppCommon.LogMessage("UpdateCheck.java runInTheBackground() executor is still running and is less than 30s old (" +
				                          (current_time - bgStart) + "s), skipping...");
				return;
			}

			weeWXAppCommon.LogMessage("UpdateCheck.java runInTheBackground() Cancelling the current background executor...");
			backgroundTask.cancel(true);
			backgroundTask = null;
		}

		bgStart = current_time;

		PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "weeWXApp::MyWakelockTag");
		wl.acquire(600_000L);

		backgroundTask = executor.submit(() ->
		{
			try
			{
				if(onAppStart)
				{
					try
					{
						Thread.sleep(5_000L);
					} catch(InterruptedException e) {
						//weeWXAppCommon.doStackOutput(e);
						return;
					}
				}

				weeWXAppCommon.LogMessage("UpdateCheck.java executor.submit() weeWXAppCommon.getForecast(false, " +
				                          onAppStart + ")...", true);
				weeWXAppCommon.getForecast(false, onAppStart);

				String radtype = weeWXAppCommon.GetStringPref("radtype", weeWXApp.radtype_default);
				if(radtype != null && radtype.equals("image"))
				{
					String radarURL = weeWXAppCommon.GetStringPref("RADAR_URL", weeWXApp.RADAR_URL_default);
					if(radarURL != null && !radarURL.isBlank())
					{
						weeWXAppCommon.LogMessage("UpdateCheck.java executor.submit() weeWXAppCommon.getRadarImage(false, " +
						                          onAppStart + ")...", true);
						weeWXAppCommon.getRadarImage(false, onAppStart);
					}
				}

				weeWXAppCommon.LogMessage("UpdateCheck.java executor.submit() weeWXAppCommon.getWeather(false, " +
				                          onAppStart + ")...", true);
				weeWXAppCommon.getWeather(false, onAppStart);

				weeWXAppCommon.LogMessage("UpdateCheck.java executor.submit() weeWXAppCommon.getWebcam(false, " +
				                          onAppStart + ")...", true);
				weeWXAppCommon.getWebcamImage(false, onAppStart);
			} catch(Exception e) {
				weeWXAppCommon.LogMessage("Error! e: " + e, true);
			}

			if(onAppStart && !weeWXApp.hasBootedFully)
			{
				weeWXApp.hasBootedFully = true;
				weeWXAppCommon.LogMessage("weeWXApp.hasBootedFully is now true...");
			}

			weeWXAppCommon.LogMessage("UpdateCheck.java executor.submit() finished running the background updates, " +
			                          "about to release the wake lock...", true);

			wl.release();
		});
	}
}