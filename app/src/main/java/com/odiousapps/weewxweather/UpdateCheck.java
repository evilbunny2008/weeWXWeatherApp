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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

@SuppressWarnings("unused")
public class UpdateCheck extends BroadcastReceiver
{
	private final static ExecutorService executor = Executors.newSingleThreadExecutor();

	private static Future<?> backgroundTask;

	private static Instant bgStart = Instant.EPOCH, bgLastRun = Instant.EPOCH;

	@Override
	public void onReceive(Context context, Intent i)
	{
		LogMessage("UpdateCheck.onReceive() i.getAction(): " + i.getAction(), KeyValue.d);

		if(context == null)
		{
			LogMessage("UpdateCheck.onReceive() failed, context == null", KeyValue.d);
			return;
		}

		if(!weeWXAppCommon.UPDATECHECK.equals(i.getAction()))
		{
			LogMessage(i.getAction() + " != " + weeWXAppCommon.UPDATECHECK + ", skipping...", KeyValue.d);
			return;
		}

		if(!weeWXAppCommon.checkConnection())
		{
			LogMessage("UpdateCheck.onReceive() Skipping update due to wifi setting.", KeyValue.d);
			return;
		}

		long[] npwsll = weeWXAppCommon.getNPWSLL();
		if(npwsll[1] <= 0)
		{
			LogMessage("UpdateCheck.onReceive() Skipping, period is invalid or set to manual refresh only...", KeyValue.d);
			return;
		}

		if(npwsll[5] == 0)
		{
			LogMessage("UpdateCheck.onReceive() Skipping, lastDownloadTime == 0, app hasn't been setup...", KeyValue.d);
			return;
		}

		int pos = (int)KeyValue.readVar("updateInterval", weeWXApp.updateInterval_default);
		if(pos == 6 && !KeyValue.isVisible)
		{
			LogMessage("UpdateCheck.onReceive() Skipping, pos == 6 but app isn't visible...", KeyValue.d);
			return;
		}

		LogMessage("UpdateCheck.onReceive() started.");

		cancelAlarm();

		setNextAlarm();

		LogMessage("UpdateCheck.onReceive() " + npwsll[5]+ " < " + npwsll[4] + "?");

		if(npwsll[5] < npwsll[4])
		{
			LogMessage("UpdateCheck.onReceive() There should have been an update since last run, checking...");
			runInTheBackground(true, false);
		} else
			LogMessage("UpdateCheck.onReceive() There shouldn't be an update since last run, skipping...");

		LogMessage("UpdateCheck.onReceive() finished.");
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
		LogMessage("UpdateCheck.setNextAlarm() Attempting to set the alarm...");

		Context context = weeWXApp.getInstance();
		if(context == null)
		{
			LogMessage("UpdateCheck.setNextAlarm() failed, context == null", KeyValue.d);
			return;
		}

		long[] npwsll = weeWXAppCommon.getNPWSLL();
		if(npwsll[1] <= 0)
		{
			LogMessage("UpdateCheck.setNextAlarm() Skipping, period is invalid or set to manual refresh only...", KeyValue.d);
			return;
		}

		if(npwsll[5] == 0)
		{
			LogMessage("UpdateCheck.setNextAlarm() Skipping, lastDownloadTime == 0, app hasn't been setup...", KeyValue.d);
			return;
		}

		int pos = (int)KeyValue.readVar("updateInterval", weeWXApp.updateInterval_default);
		if(pos == 6 && !KeyValue.isVisible)
		{
			LogMessage("UpdateCheck.setNextAlarm() Skipping, pos == 6 but app isn't visible...", KeyValue.d);
			return;
		}

		if(getPendingIntent(context, true) != null)
		{
			LogMessage("UpdateCheck.setNextAlarm() An alarm is already set, did you forget to call cancel first? Skipping...", KeyValue.d);
			return;
		}

		String string_time = weeWXAppCommon.sdf10.format(npwsll[0]);

		LogMessage("UpdateCheck.setNextAlarm() now: " + string_time);

		string_time = weeWXAppCommon.sdf10.format(npwsll[3]);
		LogMessage("UpdateCheck.setNextAlarm() start: " + string_time);
		LogMessage("UpdateCheck.setNextAlarm() period: " + Math.round(npwsll[1] / 1_000D) + "s");
		LogMessage("UpdateCheck.setNextAlarm() wait: " + Math.round(npwsll[2] / 1_000D) + "s");

		LogMessage("UpdateCheck.setNextAlarm() secs to next start: " + Math.round((npwsll[3] - npwsll[0]) / 1_000D) + "s");

		AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

		if(canSetExact(context))
			setExactAlarm(context, alarm, npwsll[3]);
		else
			setInexactAlarm(context, alarm, npwsll[3]);
	}

	static void promptForExact(Context context)
	{
		LogMessage("UpdateCheck.promptForExact() Opening the settings app to ask to enable exact alarms...");
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
		long current_time = System.currentTimeMillis();
		alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, start, getPendingIntent(context, false));
		LogMessage("UpdateCheck.setExactAlarm() Successfully set the exact alarm for " +
		           (Math.round((start - current_time) / 100D) / 10D) + "s time...");
	}

	private static void setInexactAlarm(Context context, AlarmManager alarm, long start)
	{
		long current_time = System.currentTimeMillis();
		alarm.set(AlarmManager.RTC_WAKEUP, start, getPendingIntent(context, false));
		LogMessage("UpdateCheck.setInexactAlarm() Successfully set the inexact alarm for " +
		           (Math.round((start - current_time) / 100D) / 10D) + "s time...");
	}

	static boolean canSetExact(Context context)
	{
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
		{
			LogMessage("UpdateCheck.canSetExact() Build.VERSION.SDK_INT < Build.VERSION_CODES.S so setting exact alarm allowed...");
			return true;
		}

		AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		if(alarm != null && alarm.canScheduleExactAlarms())
		{
			LogMessage("UpdateCheck.canSetExact() alarm.canScheduleExactAlarms() is true...");
			return true;
		}

		LogMessage("UpdateCheck.canSetExact() alarm.canScheduleExactAlarms() is false...");
		return false;
	}

	static void cancelAlarm()
	{
		LogMessage("UpdateCheck.cancelAlarm() Attempting to cancel the current alarm...");

		Context context = weeWXApp.getInstance();
		if(context == null)
		{
			LogMessage("UpdateCheck.cancelAlarm() failed, context == null", KeyValue.d);
			return;
		}

		PendingIntent pi = getPendingIntent(context, true);

		if(pi == null)
		{
			LogMessage("UpdateCheck.cancelAlarm() An alarm wasn't set, skipping...");
			return;
		}

		AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pi);
		pi.cancel();

		LogMessage("UpdateCheck.cancelAlarm() Successfully cancelled the reoccurring alarm...");
	}

	static void runInTheBackground(boolean onReceivedUpdate, boolean onAppStart)
	{
		LogMessage("UpdateCheck.runInTheBackground() Running the background updates...");

		Instant now = Instant.now();

		if(Math.abs(Duration.between(bgLastRun, now).toSeconds()) < 10)
		{
			{
				LogMessage("UpdateCheck.runInTheBackground() this function was called less than 10s ago (" +
				           Math.abs(Duration.between(bgLastRun, now).toSeconds()) + "s), skipping...");
				return;
			}
		}

		Context context = weeWXApp.getInstance();
		if(context == null)
		{
			LogMessage("UpdateCheck.runInTheBackground() Failed, context == null", KeyValue.d);
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		int pos = (int)KeyValue.readVar("updateInterval", weeWXApp.updateInterval_default);
		if(pos < 0)
		{
			LogMessage("UpdateCheck.runInTheBackground() Invalid update frequency...", KeyValue.d);
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		if(pos == 0)
		{
			LogMessage("UpdateCheck.runInTheBackground() Update interval set to manual update... skipping...", KeyValue.d);
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		if(pos == 6 && !KeyValue.isVisible)
		{
			LogMessage("UpdateCheck.runInTheBackground() Update interval set to manual update and app not visible... skipping...", KeyValue.d);
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		if(!weeWXAppCommon.checkConnection())
		{
			LogMessage("UpdateCheck.runInTheBackground() WiFi needed but unavailable... skipping...", KeyValue.d);
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		LogMessage("UpdateCheck.runInTheBackground() Update interval set to: " + weeWXApp.updateOptions[pos]);

		if(onAppStart)
		{
			long[] npwsll = weeWXAppCommon.getNPWSLL();

			if(npwsll[1] <= 0)
			{
				LogMessage("UpdateCheck.runInTheBackground() Period is invalid or set to manual update only... skipping...", KeyValue.d);
				if(!weeWXApp.hasBootedFully)
					weeWXApp.hasBootedFully = true;
				return;
			}

			String string_time = weeWXAppCommon.sdf10.format(npwsll[5]);
			LogMessage("UpdateCheck.runInTheBackground() lastDownloadTime: " + string_time);

			string_time = weeWXAppCommon.sdf10.format(npwsll[4]);
			LogMessage("UpdateCheck.runInTheBackground() lastStart: " + string_time);

			if(npwsll[5] > npwsll[4])
			{
				LogMessage("UpdateCheck.runInTheBackground() Updated since lastStart time... skipping...");
				if(!weeWXApp.hasBootedFully)
					weeWXApp.hasBootedFully = true;
				return;
			}
		}

		LogMessage("UpdateCheck.runInTheBackground() Let's check if runInTheBackground() is already running...");

		String fctype = (String)KeyValue.readVar("fctype", "");
		if(fctype == null || fctype.isBlank())
		{
			LogMessage("UpdateCheck.runInTheBackground() fctype is null or blank, skipping...");
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;

			return;
		}

		ForecastDefaults fcDef = weeWXApp.getFCdefs(fctype);
		if(fcDef == null)
		{
			LogMessage("UpdateCheck.runInTheBackground() fcDef is null, skipping...");
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;

			return;
		}

		if(backgroundTask != null && !backgroundTask.isDone())
		{
			if(Math.abs(Duration.between(bgStart, now).toSeconds()) < fcDef.default_wait_before_killing_executor)
			{
				LogMessage("UpdateCheck.runInTheBackground() runInTheBackground() executor is still running and is less than " +
				           fcDef.default_wait_before_killing_executor + "s old (" +
				           Math.abs(Duration.between(bgStart, now).toSeconds()) + "s), skipping...");
				return;
			}

			LogMessage("UpdateCheck.runInTheBackground() Cancelling the current background executor...");
			backgroundTask.cancel(true);
			backgroundTask = null;
		}

		LogMessage("UpdateCheck.runInTheBackground() bgLastRun = bgStart = " + now.toEpochMilli());
		bgLastRun = bgStart = now;

		LogMessage("UpdateCheck.runInTheBackground() Starting the real work...");

		PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "weeWXApp::UpdateCheck");
		wl.acquire(600_000L);

		backgroundTask = executor.submit(() ->
		{
			LogMessage("UpdateCheck.runInTheBackground() New background thread started...");

			try
			{
				if(onAppStart)
				{
					try
					{
						LogMessage("UpdateCheck.runInTheBackground() onAppStart is true, sleeping for " +
						           (Math.round(weeWXAppCommon.default_wait_on_boot / 100f) / 10.0) + "s so all " +
						           "the fragments have a chance to load...");
						Thread.sleep(weeWXAppCommon.default_wait_on_boot);
					} catch(InterruptedException e) {
						LogMessage("UpdateCheck.runInTheBackground() Sleep interrupted by a thrown InterruptedException, is this normal?");
						//doStackOutput(e);
						//weeWXApp.hasBootedFully = true;
						//return;
					}
				}

				LogMessage("UpdateCheck.runInTheBackground() weeWXAppCommon.getForecast(false, " + onAppStart + ", true)...");
				weeWXAppCommon.getForecast(false, onAppStart, true);

				String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
				if(radtype != null && radtype.equals("image"))
				{
					String radarURL = (String)KeyValue.readVar("RADAR_URL", "");
					if(radarURL != null && !radarURL.isBlank())
					{
						LogMessage("UpdateCheck.runInTheBackground() weeWXAppCommon.getRadarImage(false, " + onAppStart + ", false, true)...");
						weeWXAppCommon.getRadarImage(false, onAppStart, false, true);
					}
				}

				LogMessage("UpdateCheck.runInTheBackground() weeWXAppCommon.getWeather(false, " + onAppStart + ", true)...");
				weeWXAppCommon.getWeather(false, onAppStart, true);

				LogMessage("UpdateCheck.runInTheBackground() weeWXAppCommon.getWebcam(false, " + onAppStart + ", true, true)...");
				weeWXAppCommon.getWebcamImage(false, onAppStart, true, true);
			} catch(Exception e) {
				LogMessage("UpdateCheck.runInTheBackground() Error! e: " + e, true, KeyValue.e);
			}

			if(!weeWXApp.hasBootedFully)
			{
				weeWXApp.hasBootedFully = true;
				LogMessage("UpdateCheck.runInTheBackground() weeWXApp.hasBootedFully is now true...");
			}

			LogMessage("UpdateCheck.runInTheBackground() Finished running the background updates, about to release the wake lock...");

			bgStart = Instant.EPOCH;

			wl.release();
		});
	}
}