package com.odiousapps.weewxweather;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

public class weeWXApp extends Application
{
	@Override
	public void onCreate()
	{
		super.onCreate();
		applyTheme(this);
	}

	public static void applyTheme(Context context)
	{
		Common common = new Common(context);
		AppCompatDelegate.setDefaultNightMode(common.getDayNightTheme());
	}
}