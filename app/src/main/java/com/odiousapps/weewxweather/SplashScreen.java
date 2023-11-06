package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashScreen extends AppCompatActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash_screen);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		new Thread(() ->
		{
			try
			{
				// Sleep needed to stop frames dropping while loading
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}

			startApp();
			finish();
		}).start();
	}

	private void startApp()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
		{
			AlarmManager alarms = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
			if(!alarms.canScheduleExactAlarms())
			{
				startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
				finish();
			}
		}

		Intent intent = new Intent(SplashScreen.this, MainActivity.class);
		startActivity(intent);
	}
}