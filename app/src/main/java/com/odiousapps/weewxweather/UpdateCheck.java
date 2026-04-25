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


import androidx.annotation.Nullable;


import static com.odiousapps.weewxweather.weeWXApp.FCTYPE;
import static com.odiousapps.weewxweather.weeWXApp.SKIPPING;
import static com.odiousapps.weewxweather.weeWXApp.SKIPPING_S;
import static com.odiousapps.weewxweather.weeWXApp.UPDATE_FREQUENCY;
import static com.odiousapps.weewxweather.weeWXApp.getFCdefs;
import static com.odiousapps.weewxweather.weeWXApp.hasBootedFully;
import static com.odiousapps.weewxweather.weeWXApp.updateOptions;
import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.odiousapps.weewxweather.weeWXAppCommon.NPWSLL;
import static com.odiousapps.weewxweather.weeWXAppCommon.UPDATECHECK;
import static com.odiousapps.weewxweather.weeWXAppCommon.is_blank;
import static com.odiousapps.weewxweather.weeWXAppCommon.notCheckConnection;
import static com.odiousapps.weewxweather.weeWXAppCommon.getNPWSLL;
import static com.odiousapps.weewxweather.weeWXAppCommon.processUpdates;

@DontObfuscate
public class UpdateCheck extends BroadcastReceiver
{
	private final static ExecutorService executor = Executors.newSingleThreadExecutor();

	@Nullable
	private static Future<?> backgroundTask;

	private static long bgStart, bgLastRun;

	static
	{
		backgroundTask = null;
		bgStart = 0L;
		bgLastRun = 0L;
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		LogMessage("UpdateCheck.onReceive() i.getAction(): " + intent.getAction(), KeyValue.d);

		if(context == null)
		{
			LogMessage("UpdateCheck.onReceive() failed, context == null", KeyValue.d);
			return;
		}

		if(!UPDATECHECK.equals(intent.getAction()))
		{
			LogMessage(intent.getAction() + " != " + UPDATECHECK + SKIPPING, KeyValue.d);
			return;
		}

		if(notCheckConnection())
		{
			LogMessage("UpdateCheck.onReceive() Skipping update due to wifi setting.", KeyValue.d);
			return;
		}

		NPWSLL npwsll = getNPWSLL();
		if(npwsll.periodTime() <= 0)
		{
			LogMessage("UpdateCheck.onReceive() Skipping, period is invalid or set to manual refresh only...", KeyValue.d);
			return;
		}

		if(npwsll.report_time() == 0)
		{
			LogMessage("UpdateCheck.onReceive() Skipping, report_time == 0, app hasn't been setup...", KeyValue.d);
			return;
		}

		int pos = (int)KeyValue.readVar(UPDATE_FREQUENCY, weeWXApp.UpdateFrequency_default);
		if(pos == 6 && !KeyValue.isVisible)
		{
			LogMessage("UpdateCheck.onReceive() Skipping, pos == 6 but app isn't visible...", KeyValue.d);
			return;
		}

		LogMessage("UpdateCheck.onReceive() started.");

		cancelAlarm();

		setNextAlarm();

		LogMessage("UpdateCheck.onReceive() " + npwsll.report_time() + " < " + npwsll.lastStart() + "?");

		if(npwsll.report_time() < npwsll.lastStart())
		{
			LogMessage("UpdateCheck.onReceive() There should have been an update since last run, checking...");
			runInTheBackground(true, false);
		} else
			LogMessage("UpdateCheck.onReceive() There shouldn't be an update since last run, skipping...");

		LogMessage("UpdateCheck.onReceive() finished.");
	}

	private static PendingIntent getPendingIntent(Context context, boolean doNoCreate, int reqcode)
	{
		Intent intent = new Intent(context, UpdateCheck.class);
		intent.setAction(UPDATECHECK);

		int flags = PendingIntent.FLAG_IMMUTABLE;

		if(doNoCreate)
			flags |= PendingIntent.FLAG_NO_CREATE;

		return PendingIntent.getBroadcast(context, reqcode, intent, flags);
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

		NPWSLL npwsll = getNPWSLL();
		if(npwsll.periodTime() <= 0)
		{
			LogMessage("UpdateCheck.setNextAlarm() Skipping, period is invalid or set to manual refresh only...", KeyValue.d);
			return;
		}

		if(npwsll.report_time() == 0)
		{
			LogMessage("UpdateCheck.setNextAlarm() Skipping, report_time == 0, app hasn't been setup...", KeyValue.d);
			return;
		}

		int pos = (int)KeyValue.readVar(UPDATE_FREQUENCY, weeWXApp.UpdateFrequency_default);
		if(pos == 6 && !KeyValue.isVisible)
		{
			LogMessage("UpdateCheck.setNextAlarm() Skipping, pos == 6 but app isn't visible...", KeyValue.d);
			return;
		}

		String stringTime = weeWXApp.getInstance().sdf10.format(npwsll.nowTime());
		LogMessage("UpdateCheck.setNextAlarm() now: " + stringTime);

		stringTime = weeWXApp.getInstance().sdf10.format(npwsll.startTime());
		LogMessage("UpdateCheck.setNextAlarm() start: " + stringTime);
		LogMessage("UpdateCheck.setNextAlarm() period: " + Math.round(npwsll.periodTime() / 1_000D) + "s");
		LogMessage("UpdateCheck.setNextAlarm() wait: " + Math.round(npwsll.waitTime() / 1_000D) + "s");

		LogMessage("UpdateCheck.setNextAlarm() secs to next start: " + Math.round((npwsll.startTime() - npwsll.nowTime()) / 1_000D) + "s");

		AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

		if(canSetExact(context))
			setExactAlarm(context, alarm, npwsll.startTime());
		else
			setInexactAlarm(context, alarm, npwsll.startTime());
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
		long currentTime = System.currentTimeMillis();
		int reqcode = 0;

		for(long l = start; l < start + (15_000L * weeWXApp.max_alarms); l += 15_000L)
		{
			alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, l, getPendingIntent(context, false, reqcode++));
			LogMessage("UpdateCheck.setExactAlarm() Successfully set the exact alarm for " +
					   (Math.round((l - currentTime) / 100D) / 10D) + "s time...");
		}
	}

	private static void setInexactAlarm(Context context, AlarmManager alarm, long start)
	{
		long currentTime = System.currentTimeMillis();
		int reqcode = 0;

		for(long l = start; l < start + 15_000L * weeWXApp.max_alarms; l += 15_000L)
		{
			alarm.set(AlarmManager.RTC_WAKEUP, l, getPendingIntent(context, false, reqcode++));
			LogMessage("UpdateCheck.setInexactAlarm() Successfully set the inexact alarm for " +
					   (Math.round((l - currentTime) / 100D) / 10D) + "s time...");
		}
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

		for(int reqcode = 0; reqcode < weeWXApp.max_alarms << 1; reqcode++)
		{
			PendingIntent pi = getPendingIntent(context, true, reqcode);

			if(pi != null)
			{
				AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
				alarmManager.cancel(pi);
				pi.cancel();

				LogMessage("UpdateCheck.cancelAlarm() Successfully cancelled the reoccurring alarm...");
			}
		}
	}

	static void runInTheBackground(boolean onReceivedUpdate, boolean onAppStart)
	{
		LogMessage("UpdateCheck.runInTheBackground() Running the background updates...");

		long now = System.currentTimeMillis();
		long dur = (now - bgLastRun) / 1000;
		if(dur < 10)
		{
			{
				LogMessage("UpdateCheck.runInTheBackground() this function was called less than 10s ago (" +
						   dur + SKIPPING_S);
				return;
			}
		}

		Context context = weeWXApp.getInstance();
		if(context == null)
		{
			LogMessage("UpdateCheck.runInTheBackground() Failed, context == null", KeyValue.d);
			if(!hasBootedFully)
				hasBootedFully = true;

			return;
		}

		int pos = (int)KeyValue.readVar(UPDATE_FREQUENCY, weeWXApp.UpdateFrequency_default);
		if(pos == 0)
		{
			LogMessage("UpdateCheck.runInTheBackground() Update interval set to manual update... skipping...", KeyValue.d);
			if(!hasBootedFully)
				hasBootedFully = true;
			return;
		}

		if(pos == 6 && !KeyValue.isVisible)
		{
			LogMessage("UpdateCheck.runInTheBackground() Update interval set to manual update and app not visible... skipping...", KeyValue.d);
			if(!hasBootedFully)
				hasBootedFully = true;
			return;
		}

		if(notCheckConnection())
		{
			LogMessage("UpdateCheck.runInTheBackground() WiFi needed but unavailable... skipping...", KeyValue.d);
			if(!hasBootedFully)
				hasBootedFully = true;
			return;
		}

		LogMessage("UpdateCheck.runInTheBackground() Update interval set to: " + updateOptions[pos]);

		if(onAppStart)
		{
			NPWSLL npwsll = getNPWSLL();

			if(npwsll.periodTime() <= 0)
			{
				LogMessage("UpdateCheck.runInTheBackground() Period is invalid or set to manual update only... skipping...", KeyValue.d);
				if(!hasBootedFully)
					hasBootedFully = true;
				return;
			}

			String stringTime = weeWXApp.getInstance().sdf10.format(npwsll.report_time());
			LogMessage("UpdateCheck.runInTheBackground() report_time: " + stringTime);

			stringTime = weeWXApp.getInstance().sdf10.format(npwsll.lastStart());
			LogMessage("UpdateCheck.runInTheBackground() lastStart: " + stringTime);

			if(npwsll.report_time() > npwsll.lastStart())
			{
				LogMessage("UpdateCheck.runInTheBackground() Updated since lastStart time... skipping...");
				if(!hasBootedFully)
					hasBootedFully = true;
				return;
			}
		}

		LogMessage("UpdateCheck.runInTheBackground() Let's check if runInTheBackground() is already running...");

		String fctype = (String)KeyValue.readVar(FCTYPE, "");
		if(is_blank(fctype))
		{
			LogMessage("UpdateCheck.runInTheBackground() fctype is null or blank, skipping...");
			if(!hasBootedFully)
				hasBootedFully = true;

			return;
		}

		ForecastDefaults fcDef = getFCdefs(fctype);
		if(fcDef == null)
		{
			LogMessage("UpdateCheck.runInTheBackground() fcDef is null, skipping...");
			if(!hasBootedFully)
				hasBootedFully = true;

			return;
		}

		if(backgroundTask != null && !backgroundTask.isDone())
		{
			dur = (now - bgStart) / 1000;
			if(dur < fcDef.default_wait_before_killing_executor)
			{
				LogMessage("UpdateCheck.runInTheBackground() runInTheBackground() executor is still running and is less than " +
						   fcDef.default_wait_before_killing_executor + "s old (" + dur + SKIPPING_S);
				return;
			}

			LogMessage("UpdateCheck.runInTheBackground() Cancelling the current background executor...");
			backgroundTask.cancel(true);
			backgroundTask = null;
		}

		LogMessage("UpdateCheck.runInTheBackground() bgLastRun = bgStart = " + now);
		bgLastRun = now;
		bgStart = now;

		LogMessage("UpdateCheck.runInTheBackground() Starting the real work...");

		PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "weeWXApp::UpdateCheck");
		wl.acquire(600_000L);

		backgroundTask = executor.submit(() ->
		{
			LogMessage("UpdateCheck.runInTheBackground() New background thread started...");

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
					wl.release();
					bgStart = 0;
					if(!hasBootedFully)
						hasBootedFully = true;

					return;
				}
			}

			processUpdates(false, onReceivedUpdate, onAppStart, true, true, true, true, true);

			if(!hasBootedFully)
			{
				hasBootedFully = true;
				LogMessage("UpdateCheck.runInTheBackground() weeWXApp.hasBootedFully is now true...");
			}

			LogMessage("UpdateCheck.runInTheBackground() Finished running the background updates, about to release the wake lock...");

			bgStart = 0;

			wl.release();
		});
	}
}