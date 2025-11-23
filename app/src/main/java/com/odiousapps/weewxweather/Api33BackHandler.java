package com.odiousapps.weewxweather;

import android.os.Build;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class Api33BackHandler
{
	static void setup(MainActivity activity)
	{
		activity.getOnBackInvokedDispatcher()
				.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, activity::handleBack);
	}
}
