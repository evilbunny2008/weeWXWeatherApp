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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

@SuppressWarnings("unused")
public class UpdateCheck extends BroadcastReceiver
{
	private final static ExecutorService executor = Executors.newSingleThreadExecutor();

	private static Future<?> backgroundTask;

	private static long bgStart, bgLastRun;

	@Override
	public void onReceive(Context context, Intent i)
	{
		weeWXAppCommon.LogMessage("UpdateCheck.java onReceive() i.getAction(): " + i.getAction(), KeyValue.d);

		if(context == null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java failed, context == null", KeyValue.d);
			return;
		}

		if(!weeWXAppCommon.UPDATECHECK.equals(i.getAction()))
		{
			weeWXAppCommon.LogMessage(i.getAction() + " != " + weeWXAppCommon.UPDATECHECK + ", skipping...", KeyValue.d);
			return;
		}

		if(!weeWXAppCommon.checkConnection())
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Skipping update due to wifi setting.", KeyValue.d);
			return;
		}

		long[] npwsll = weeWXAppCommon.getNPWSLL();
		if(npwsll[1] <= 0)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Skipping, period is invalid or set to manual refresh only...", KeyValue.d);
			return;
		}

		if(npwsll[5] == 0)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Skipping, lastDownloadTime == 0, app hasn't been setup...", KeyValue.d);
			return;
		}

		int pos = (int)KeyValue.readVar("updateInterval", weeWXApp.updateInterval_default);
		if(pos == 6 && !KeyValue.isVisible)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Skipping, pos == 6 but app isn't visible...", KeyValue.d);
			return;
		}

		weeWXAppCommon.LogMessage("UpdateCheck.java started.");

		cancelAlarm();

		setNextAlarm();

		runInTheBackground(true, false);

		weeWXAppCommon.LogMessage("UpdateCheck.java finished.");
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
		weeWXAppCommon.LogMessage("UpdateCheck.java Attempting to set the alarm...");

		Context context = weeWXApp.getInstance();
		if(context == null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java failed, context == null", KeyValue.d);
			return;
		}

		long[] npwsll = weeWXAppCommon.getNPWSLL();
		if(npwsll[1] <= 0)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Skipping, period is invalid or set to manual refresh only...", KeyValue.d);
			return;
		}

		if(npwsll[5] == 0)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Skipping, lastDownloadTime == 0, app hasn't been setup...", KeyValue.d);
			return;
		}

		int pos = (int)KeyValue.readVar("updateInterval", weeWXApp.updateInterval_default);
		if(pos == 6 && !KeyValue.isVisible)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Skipping, pos == 6 but app isn't visible...", KeyValue.d);
			return;
		}

		if(getPendingIntent(context, true) != null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java An alarm is already set, did you forget to call cancel first? Skipping...", KeyValue.d);
			return;
		}

		String string_time = weeWXAppCommon.sdf10.format(npwsll[0]);

		weeWXAppCommon.LogMessage("UpdateCheck.java now: " + string_time);

		string_time = weeWXAppCommon.sdf10.format(npwsll[3]);
		weeWXAppCommon.LogMessage("UpdateCheck.java start: " + string_time);
		weeWXAppCommon.LogMessage("UpdateCheck.java period: " + Math.round(npwsll[1] / 1_000D) + "s");
		weeWXAppCommon.LogMessage("UpdateCheck.java wait: " + Math.round(npwsll[2] / 1_000D) + "s");

		weeWXAppCommon.LogMessage("UpdateCheck.java secs to next start: " +
		                          Math.round((npwsll[3] - npwsll[0]) / 1_000D) + "s");

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
		                          (Math.round(start / 1_000D) - current_time) + "s time...");
	}

	private static void setInexactAlarm(Context context, AlarmManager alarm, long start)
	{
		long current_time = weeWXAppCommon.getCurrTime();
		alarm.set(AlarmManager.RTC_WAKEUP, start, getPendingIntent(context, false));
		weeWXAppCommon.LogMessage("UpdateCheck.java Successfully set the inexact alarm for " +
		                          (Math.round(start / 1_000D) - current_time) + "s time...");
	}

	static boolean canSetExact(Context context)
	{
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Build.VERSION.SDK_INT < Build.VERSION_CODES.S " +
			                          "so setting exact alarm allowed...");
			return true;
		}

		AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		if(alarm != null && alarm.canScheduleExactAlarms())
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java alarm.canScheduleExactAlarms() is true...");
			return true;
		}

		weeWXAppCommon.LogMessage("UpdateCheck.java alarm.canScheduleExactAlarms() is false...");
		return false;
	}

	static void cancelAlarm()
	{
		weeWXAppCommon.LogMessage("UpdateCheck.java Attempting to cancel the current alarm...");

		Context context = weeWXApp.getInstance();
		if(context == null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java failed, context == null", KeyValue.d);
			return;
		}

		PendingIntent pi = getPendingIntent(context, true);

		if(pi == null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java an alarm wasn't set, skipping...");
			return;
		}

		AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pi);
		pi.cancel();

		weeWXAppCommon.LogMessage("UpdateCheck.java Successfully cancelled the reoccurring alarm...");
	}

	static void runInTheBackground(boolean onReceivedUpdate, boolean onAppStart)
	{
		weeWXAppCommon.LogMessage("UpdateCheck.java runInTheBackground() running the background updates...");

		long current_time = weeWXAppCommon.getCurrTime();

		if(bgLastRun + 10 > current_time)
		{
			{
				weeWXAppCommon.LogMessage("UpdateCheck.java runInTheBackground() this function was called less than " +
				                          "10s ago (" + (current_time - bgLastRun) + "s), skipping...");
				return;
			}
		}

		Context context = weeWXApp.getInstance();
		if(context == null)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java failed, context == null", KeyValue.d);
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		int pos = (int)KeyValue.readVar("updateInterval", weeWXApp.updateInterval_default);
		if(pos < 0)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java Invalid update frequency...", KeyValue.d);
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		if(pos == 0)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java update interval set to manual update... skipping...", KeyValue.d);
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		if(pos == 6 && !KeyValue.isVisible)
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java update interval set to manual update and app not visible... skipping...", KeyValue.d);
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		if(!weeWXAppCommon.checkConnection())
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java WiFi needed but unavailable... skipping...", KeyValue.d);
			if(!weeWXApp.hasBootedFully)
				weeWXApp.hasBootedFully = true;
			return;
		}

		weeWXAppCommon.LogMessage("UpdateCheck.java update interval set to: " + weeWXApp.updateOptions[pos]);

		if(onAppStart)
		{
			long[] npwsll = weeWXAppCommon.getNPWSLL();

			if(npwsll[1] <= 0)
			{
				weeWXAppCommon.LogMessage("UpdateCheck.java Period is invalid or set to manual update only... skipping...", KeyValue.d);
				if(!weeWXApp.hasBootedFully)
					weeWXApp.hasBootedFully = true;
				return;
			}

			String string_time = weeWXAppCommon.sdf10.format(npwsll[5]);
			weeWXAppCommon.LogMessage("UpdateCheck.java lastDownloadTime: " + string_time);

			string_time = weeWXAppCommon.sdf10.format(npwsll[4]);
			weeWXAppCommon.LogMessage("UpdateCheck.java lastStart: " + string_time);

			if(npwsll[5] >= npwsll[4])
			{
				weeWXAppCommon.LogMessage("UpdateCheck.java Updated since lastStart time... skipping...");
				if(!weeWXApp.hasBootedFully)
					weeWXApp.hasBootedFully = true;
				return;
			}
		}

		weeWXAppCommon.LogMessage("UpdateCheck.java Let's check if runInTheBackground() is already running...");

		if(backgroundTask != null && !backgroundTask.isDone())
		{
			if(bgStart + 30 > current_time)
			{
				weeWXAppCommon.LogMessage("UpdateCheck.java runInTheBackground() executor is still running " +
				                          "and is less than 30s old (" + (current_time - bgStart) + "s), skipping...");
				return;
			}

			weeWXAppCommon.LogMessage("UpdateCheck.java runInTheBackground() Cancelling the current background executor...");
			backgroundTask.cancel(true);
			backgroundTask = null;
		}

		weeWXAppCommon.LogMessage("UpdateCheck.java bgLastRun = bgStart = " + current_time);
		bgLastRun = bgStart = current_time;

		weeWXAppCommon.LogMessage("UpdateCheck.java Starting the real work...");

		PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "weeWXApp::UpdateCheck");
		wl.acquire(600_000L);

		backgroundTask = executor.submit(() ->
		{
			weeWXAppCommon.LogMessage("UpdateCheck.java new background thread started...");

			try
			{
				if(onAppStart)
				{
					try
					{
						weeWXAppCommon.LogMessage("UpdateCheck.java onAppStart is true, sleeping for 5s so all " +
						                          "the fragments have a chance to load...");
						Thread.sleep(weeWXAppCommon.default_timeout);
					} catch(InterruptedException e) {
						weeWXAppCommon.LogMessage("UpdateCheck.java sleep interrupted by a " +
						                          "thrown InterruptedException, is this normal?");
						//weeWXAppCommon.doStackOutput(e);
						//weeWXApp.hasBootedFully = true;
						//return;
					}
				}

				weeWXAppCommon.LogMessage("UpdateCheck.java weeWXAppCommon.getForecast(" +
				                          "false, " + onAppStart + ")...");
				weeWXAppCommon.getForecast(false, onAppStart);

				String radtype = (String)KeyValue.readVar("radtype", weeWXApp.radtype_default);
				if(radtype != null && radtype.equals("image"))
				{
					String radarURL = (String)KeyValue.readVar("RADAR_URL", weeWXApp.RADAR_URL_default);
					if(radarURL != null && !radarURL.isBlank())
					{
						weeWXAppCommon.LogMessage("UpdateCheck.java weeWXAppCommon.getRadarImage(" +
						                          "false, " + onAppStart + ")...");
						weeWXAppCommon.getRadarImage(false, onAppStart);
					}
				}

				weeWXAppCommon.LogMessage("UpdateCheck.java weeWXAppCommon.getWeather(" +
				                          "false, " + onAppStart + ")...");
				weeWXAppCommon.getWeather(false, onAppStart);

				weeWXAppCommon.LogMessage("UpdateCheck.java weeWXAppCommon.getWebcam(" +
				                          "false, " + onAppStart + ")...");
				weeWXAppCommon.getWebcamImage(false, onAppStart);
			} catch(Exception e) {
				weeWXAppCommon.LogMessage("UpdateCheck.java Error! e: " + e, true, KeyValue.e);
			}

			if(!weeWXApp.hasBootedFully)
			{
				weeWXApp.hasBootedFully = true;
				weeWXAppCommon.LogMessage("UpdateCheck.java weeWXApp.hasBootedFully is now true...");
			}

			weeWXAppCommon.LogMessage("UpdateCheck.java finished running the background updates, " +
			                          "about to release the wake lock...");

			wl.release();
		});
	}
}