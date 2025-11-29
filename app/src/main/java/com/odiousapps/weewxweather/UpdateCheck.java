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

		long[] npwsll = weeWXAppCommon.getNPWSLL();
		if(npwsll[1] <= 0)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Skipping, period is invalid or set to manual refresh only...", true);
			return;
		}

		if(npwsll[5] == 0)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Skipping, lastDownloadTime == 0, app hasn't been setup...", true);
			return;
		}

		weeWXAppCommon.LogMessage("UpdateCheck.java started.", true);

		cancelAlarm();

		setNextAlarm();

		runInTheBackground(true, false, false);

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
		weeWXAppCommon.LogMessage("UpdateCheck.java Attempting to set the alarm...", true);

		Context context = weeWXApp.getInstance();
		if(context == null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java failed, context == null", true);
			return;
		}

		if(!canSetExact(context))
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java canSetExactAlarm is false, Skipping...", true);
			return;
		}

		long[] npwsll = weeWXAppCommon.getNPWSLL();
		if(npwsll[1] <= 0)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Skipping, period is invalid " +
			                          "or set to manual refresh only...", true);
			return;
		}

		if(npwsll[5] == 0)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Skipping, lastDownloadTime == 0, " +
			                          "app hasn't been setup...", true);
			return;
		}

		if(getPendingIntent(context, true) != null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java An alarm is already set, did you forget " +
			                          "to call cancel first? Skipping...", true);
			return;
		}

		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault());
		String string_time = sdf.format(npwsll[0]);

		weeWXAppCommon.LogMessage("UpdateCheck.java now: " + string_time, true);

		string_time = sdf.format(npwsll[3]);
		weeWXAppCommon.LogMessage("UpdateCheck.java start: " +
		                          string_time, true);
		weeWXAppCommon.LogMessage("UpdateCheck.java period: " +
		                          Math.round(npwsll[1] / 1_000D) + "s", true);
		weeWXAppCommon.LogMessage("UpdateCheck.java wait: " +
		                          Math.round(npwsll[2] / 1_000D) + "s", true);

		weeWXAppCommon.LogMessage("UpdateCheck.java secs to next start: " +
		                          Math.round((npwsll[3] - npwsll[0]) / 1_000D) + "s", true);

		AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

		if(canSetExact(context))
			setExactAlarm(context, alarm, npwsll[3]);
		else
			setInexactAlarm(context, alarm, npwsll[3]);
	}

	static void promptForExact(Context context)
	{
		AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		if(alarm != null)
		{
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !alarm.canScheduleExactAlarms())
			{
				Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
				intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
				intent.setData(Uri.parse("package:" + context.getPackageName()));
				context.startActivity(intent);
			} else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
				Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
				intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
			}
		}

	}

	private static void setExactAlarm(Context context, AlarmManager alarm, long start)
	{
		long current_time = weeWXAppCommon.getCurrTime();
		alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, start, getPendingIntent(context, false));
		weeWXAppCommon.LogMessage("UpdateCheck.java Successfully set the exact alarm for " +
		                          (Math.round(start / 1_000D) - current_time) + "s time...", true);
	}

	private static void setInexactAlarm(Context context, AlarmManager alarm, long start)
	{
		long current_time = weeWXAppCommon.getCurrTime();
		alarm.set(AlarmManager.RTC_WAKEUP, start, getPendingIntent(context, false));
		weeWXAppCommon.LogMessage("UpdateCheck.java Successfully set the inexact alarm for " +
		                          (Math.round(start / 1_000D) - current_time) + "s time...", true);
	}

	static boolean canSetExact(Context context)
	{
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Build.VERSION.SDK_INT < Build.VERSION_CODES.S " +
			                          "so setting exact alarm allowed...", true);
			return true;
		}

		AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		if(alarm != null && alarm.canScheduleExactAlarms())
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java alarm.canScheduleExactAlarms() is true...", true);
			return true;
		}

		weeWXAppCommon.LogMessage("UpdateCheck.java alarm.canScheduleExactAlarms() is false...", true);
		return false;
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

	static void runInTheBackground(boolean onReceivedUpdate, boolean onAppStart, boolean onResume)
	{
		boolean forced = weeWXAppCommon.isPrefSet("radarforecast") && onResume &&
		                 weeWXAppCommon.GetBoolPref("update_on_resume", weeWXApp.update_on_resume_default);

		weeWXAppCommon.LogMessage("UpdateCheck.java runInTheBackground() running the background updates...", true);

		Context context = weeWXApp.getInstance();
		if(context == null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java failed, context == null", true);
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		int pos = weeWXAppCommon.GetIntPref("updateInterval", weeWXApp.updateInterval_default);
		if(pos < 0)
		{
			weeWXAppCommon.LogMessage("Invalid update frequency...", true);
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		if(pos == 0 && !forced)
		{
			weeWXAppCommon.LogMessage("update interval set to manual update and not set to update on start... skipping...", true);
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		if(!weeWXAppCommon.checkConnection())
		{
			weeWXAppCommon.LogMessage("WiFi needed but unavailable... skipping...", true);
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		weeWXAppCommon.LogMessage("update interval set to: " + weeWXApp.updateOptions[pos], true);

		if(onAppStart)
		{
			long[] npwsll = weeWXAppCommon.getNPWSLL();

			if(npwsll[1] <= 0)
			{
				weeWXAppCommon.LogMessage("Period is invalid or set to manual update only... skipping...", true);
				if(!weeWXApp.hasBootedFully)
					weeWXApp.hasBootedFully = true;
				return;
			}

			SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault());

			String string_time = sdf.format(npwsll[5]);
			weeWXAppCommon.LogMessage("UpdateCheck.java lastDownloadTime: " + string_time, true);

			string_time = sdf.format(npwsll[4]);
			weeWXAppCommon.LogMessage("UpdateCheck.java lastStart: " + string_time, true);

			if(npwsll[5] >= npwsll[4])
			{
				weeWXAppCommon.LogMessage("Updated since lastStart time... skipping...", true);
				if(!weeWXApp.hasBootedFully)
					weeWXApp.hasBootedFully = true;
				return;
			}
		}

		weeWXAppCommon.LogMessage("Starting the real work...", true);

		long current_time = weeWXAppCommon.getCurrTime();

		if(backgroundTask != null && !backgroundTask.isDone())
		{
			if(bgStart + 30 > current_time)
			{
				weeWXAppCommon.LogMessage("UpdateCheck.java runInTheBackground() executor is still running and is less than 30s old (" +
				                          (current_time - bgStart) + "s), skipping...", true);
				return;
			}

			weeWXAppCommon.LogMessage("UpdateCheck.java runInTheBackground() Cancelling the current background executor...", true);
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
						weeWXApp.hasBootedFully = true;
						return;
					}
				}

				weeWXAppCommon.LogMessage("UpdateCheck.java executor.submit() weeWXAppCommon.getForecast(" +
				                          forced + ", " + onAppStart + ")...", true);
				weeWXAppCommon.getForecast(forced, onAppStart);

				String radtype = weeWXAppCommon.GetStringPref("radtype", weeWXApp.radtype_default);
				if(radtype != null && radtype.equals("image"))
				{
					String radarURL = weeWXAppCommon.GetStringPref("RADAR_URL", weeWXApp.RADAR_URL_default);
					if(radarURL != null && !radarURL.isBlank())
					{
						weeWXAppCommon.LogMessage("UpdateCheck.java executor.submit() weeWXAppCommon.getRadarImage(" +
						                          forced + ", " + onAppStart + ")...", true);
						weeWXAppCommon.getRadarImage(forced, onAppStart);
					}
				}

				weeWXAppCommon.LogMessage("UpdateCheck.java executor.submit() weeWXAppCommon.getWeather(" +
				                          forced + ", " + onAppStart + ")...", true);
				weeWXAppCommon.getWeather(forced, onAppStart);

				weeWXAppCommon.LogMessage("UpdateCheck.java executor.submit() weeWXAppCommon.getWebcam(" +
				                          forced + ", " + onAppStart + ")...", true);
				weeWXAppCommon.getWebcamImage(forced, onAppStart);
			} catch(Exception e) {
				weeWXAppCommon.LogMessage("Error! e: " + e, true);
			}

			if(!weeWXApp.hasBootedFully)
			{
				weeWXApp.hasBootedFully = true;
				weeWXAppCommon.LogMessage("weeWXApp.hasBootedFully is now true...", true);
			}

			weeWXAppCommon.LogMessage("UpdateCheck.java executor.submit() finished running the background updates, " +
			                          "about to release the wake lock...", true);

			wl.release();
		});
	}
}