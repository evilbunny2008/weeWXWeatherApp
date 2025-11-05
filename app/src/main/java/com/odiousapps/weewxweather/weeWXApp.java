package com.odiousapps.weewxweather;

import android.app.Application;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.LocaleList;

import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;

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

		if((newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) != 0)
		{
			Common.LogMessage("newConfig.uiMode changed, update the theme and mode!");
			Common.reload();
			applyTheme();
			WebViewPreloader.getInstance().init(this, 6);
		}
	}

	void applyTheme()
	{
		//if(DynamicColors.isDynamicColorAvailable())
		//	DynamicColors.applyToActivitiesIfAvailable(this);

		Common.getDayNightMode();

		if(AppCompatDelegate.getDefaultNightMode() != KeyValue.mode)
			AppCompatDelegate.setDefaultNightMode(KeyValue.mode);

		setTheme(KeyValue.theme);

		Common.LogMessage("DayNightMode == " + AppCompatDelegate.getDefaultNightMode());

		Common.SendIntents();
		Common.LogMessage("Theme should have updated!");
	}

	public static weeWXApp getInstance()
	{
		return instance;
	}

	static int getHeight()
	{
		return instance.getResources().getConfiguration().screenHeightDp;
	}

	static int getWidth()
	{
		return instance.getResources().getConfiguration().screenWidthDp;
	}

	static boolean isTablet()
	{
		return instance.getResources().getConfiguration().smallestScreenWidthDp >= 600;
	}

	static LocaleList getLocales()
	{
		return instance.getResources().getConfiguration().getLocales();
	}

	static InputStream openRawResource(int resId)
	{
		return instance.getResources().openRawResource(resId);
	}

	static String getAndroidString(int resId)
	{
		return instance.getString(resId);
	}

	static int smallestScreenWidth()
	{
		return instance.getResources().getConfiguration().smallestScreenWidthDp;
	}

	static int getUImode()
	{
		return instance.getResources().getConfiguration().uiMode;
	}

	static float getDensity()
	{
		return instance.getResources().getDisplayMetrics().density;
	}

	static int getColour(int resId)
	{
		return instance.getColor(resId);
	}

	static Drawable getAndroidDrawable(int resId)
	{
		return AppCompatResources.getDrawable(instance, resId);
	}
}