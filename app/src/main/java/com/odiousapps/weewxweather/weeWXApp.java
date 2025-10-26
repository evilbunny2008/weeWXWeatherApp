package com.odiousapps.weewxweather;

import android.app.Application;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

@SuppressWarnings({"unused"})
public class weeWXApp extends Application
{
	private static weeWXApp instance;

	@Override
	public void onCreate()
	{
		super.onCreate();
		instance = this;
		applyTheme();
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		Common.reload();
		applyTheme();
	}

	void applyTheme()
	{
		//if (DynamicColors.isDynamicColorAvailable())
		//	DynamicColors.applyToActivitiesIfAvailable(this);

		Common.getDayNightMode();

		if(AppCompatDelegate.getDefaultNightMode() != KeyValue.mode)
			AppCompatDelegate.setDefaultNightMode(KeyValue.mode);

		setTheme(KeyValue.theme);

		Common.LogMessage("DayNightMode == " + AppCompatDelegate.getDefaultNightMode());

		Common.buildUpdate();
		Common.LogMessage("Theme should have updated!");
	}

	public static weeWXApp getInstance()
	{
		return instance;
	}
}