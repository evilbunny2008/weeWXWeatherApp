package com.odiousapps.weewxweather;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@SuppressWarnings("unused")
public class UpdateCheck extends BroadcastReceiver
{
	private final static ExecutorService executor = Executors.newSingleThreadExecutor();

	private static Future<?> backgroundTask;

	private static long bgStart;

	@Override
	public void onReceive(Context context, Intent i)
	{
		weeWXAppCommon.LogMessage("UpdateCheck.java onReceive() intent.getAction(): " + i.getAction());

		if(context == null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java failed, context == null");
			return;
		}

		if(!weeWXAppCommon.checkConnection())
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Skipping update due to wifi setting.");
			return;
		}

		long current_time = weeWXAppCommon.getCurrTime();

		long lastDownloadTime = weeWXAppCommon.GetLongPref("LastDownloadTime", weeWXApp.LastDownloadTime_default);
		if(lastDownloadTime == 0)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java failed, lastDownloadTime == 0");
			return;
		}
/*
		long[] ret = weeWXAppCommon.getPeriod();
		long period = Math.round(ret[0] / 1_000D);

		weeWXAppCommon.LogMessage("lastDownloadTime: " + lastDownloadTime);
		weeWXAppCommon.LogMessage("period: " + period);
		weeWXAppCommon.LogMessage("current_time: " + current_time);
		weeWXAppCommon.LogMessage("diff: " + (current_time - lastDownloadTime));

		if(BuildConfig.DEBUG && lastDownloadTime + period - 30 > current_time)
			return;
*/
		weeWXAppCommon.LogMessage("UpdateCheck.java started.");

		runInTheBackground(context, true);

		weeWXAppCommon.LogMessage("UpdateCheck.java finished.");
	}

	private static PendingIntent getPendingIntent(Context context)
	{
		return getPendingIntent(context, false);
	}

	private static PendingIntent getPendingIntent(Context context, boolean doNoCreate)
	{
		Intent intent = new Intent(context, UpdateCheck.class);
		intent.setAction("com.odiousapps.weewxweather.UPDATECHECK");

		int flags = PendingIntent.FLAG_IMMUTABLE;

		if(doNoCreate)
			flags |= PendingIntent.FLAG_NO_CREATE;

		return PendingIntent.getBroadcast(context, 0, intent, flags);
	}

	static void setAlarm(Context context)
	{
		weeWXAppCommon.LogMessage("UpdateCheck.java Attempting to set the reoccurring alarm...");

		if(context == null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java failed, context == null");
			return;
		}

		long lastDownloadTime = weeWXAppCommon.GetLongPref("LastDownloadTime", weeWXApp.LastDownloadTime_default);
		if(lastDownloadTime == 0)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java failed, lastDownloadTime == 0");
			return;
		}

		if(getPendingIntent(context, true) != null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Reoccurring alarm already set, skipping...");
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
		String string_time = sdf.format(start);

		weeWXAppCommon.LogMessage("UpdateCheck.java now: " + now, true);
		weeWXAppCommon.LogMessage("UpdateCheck.java start: " + string_time, true);
		weeWXAppCommon.LogMessage("UpdateCheck.java period: " + period, true);
		weeWXAppCommon.LogMessage("UpdateCheck.java wait: " + wait, true);

		AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, start, period, getPendingIntent(context));

		weeWXAppCommon.LogMessage("UpdateCheck.java Successfully set the reoccurring alarm...");

		runInTheBackground(context, false);
	}

	static void cancelAlarm(Context context)
	{
		weeWXAppCommon.LogMessage("UpdateCheck.java Attempting to cancel the reoccurring alarm...");

		PendingIntent pi = getPendingIntent(context, true);

		if(pi == null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Reoccurring alarm wasn't set, skipping...");
			return;
		}

		pi.cancel();
		pi = getPendingIntent(context, false);

		if(backgroundTask != null && !backgroundTask.isDone())
			backgroundTask.cancel(true);

		AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pi);
		pi.cancel();
		weeWXAppCommon.LogMessage("UpdateCheck.java Successfully cancelled the reoccurring alarm...");
	}

	private static void runInTheBackground(Context context, boolean forced)
	{
		weeWXAppCommon.LogMessage("UpdateCheck.java runInTheBackground() running the background updates...", true);

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
		}

		bgStart = current_time;

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "weeWXApp::MyWakelockTag");
		wl.acquire(600_000L);

		backgroundTask = executor.submit(() ->
		{
			try
			{
				if(forced)
				{
					long now = System.currentTimeMillis();
					long[] ret = weeWXAppCommon.getPeriod();
					long period = ret[0];
					long wait = ret[1];

					if(period <= 0)
						return;

					long nextstart = Math.round((double)now / (double)period) * period;
					nextstart += wait;
					if(nextstart < now)
						nextstart += period;

					SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault());
					String string_time = sdf.format(nextstart);

					long delayms = nextstart - now;

					if(delayms > 540_000L)
					{
						weeWXAppCommon.LogMessage("UpdateCheck.java executor.submit() can't delay for more " +
						                          "than about 10minutes, skipping delay sleep...", true);
					} else {
						weeWXAppCommon.LogMessage("UpdateCheck.java executor.submit() Executor started... Next Start: " +
						                          string_time + ", delayms: " + delayms, true);
						try
						{
							Thread.sleep(delayms);
						} catch(InterruptedException e) {
							weeWXAppCommon.doStackOutput(e);
						}
					}
				}

				weeWXAppCommon.LogMessage("UpdateCheck.java executor.submit() weeWXAppCommon.getForecast(false);...", true);
				weeWXAppCommon.getForecast(false);

				String radtype = weeWXAppCommon.GetStringPref("radtype", weeWXApp.radtype_default);
				if(radtype != null && !radtype.isBlank() && radtype.equals("image"))
				{
					String radarURL = weeWXAppCommon.GetStringPref("RADAR_URL", weeWXApp.RADAR_URL_default);
					if(radarURL != null && !radarURL.isBlank())
					{
						weeWXAppCommon.LogMessage("UpdateCheck.java executor.submit() weeWXAppCommon.getRadarImage(false);...", true);
						weeWXAppCommon.getRadarImage(false);
					}
				}

				weeWXAppCommon.LogMessage("UpdateCheck.java executor.submit() weeWXAppCommon.getWeather(" + forced + ");...", true);
				weeWXAppCommon.getWeather(forced);
			} catch(Exception ignored) {}

			weeWXAppCommon.LogMessage("UpdateCheck.java executor.submit() finished running the background updates, " +
			                          "about to release the wake lock...", true);

			wl.release();
		});
	}
}
