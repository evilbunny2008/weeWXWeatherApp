package com.odiousapps.weewxweather;

import android.content.Context;

import androidx.core.content.ContextCompat;

import static com.odiousapps.weewxweather.weeWXApp.bgColour_default;
import static com.odiousapps.weewxweather.weeWXApp.fgColour_default;
import static com.odiousapps.weewxweather.weeWXApp.getInstance;
import static com.odiousapps.weewxweather.weeWXApp.widgetBG_default;
import static com.odiousapps.weewxweather.weeWXApp.widgetFG_default;

@SuppressWarnings("unused")
class Colours
{
	public int White = 0xFFFFFFFF;
	public int Black = 0xFF000000;

	public int widgetBG = widgetBG_default;
	public int widgetFG = widgetFG_default;

	public int bgColour = bgColour_default;
	public int fgColour = fgColour_default;

	public int AlmostBlack = 0xFF121212;
	public int LightBlueAccent = 0xFF82B1FF;
	public int DarkBlueAccent = 0xFF1E88E5;
	public int DarkGray = 0xFF333333;
	public int LightGray = 0xFFE0E0E0;

	void initOrReinit()
	{
		Context context = getInstance();
		if(context == null)
			return;

		bgColour = (int)KeyValue.readVar("bgColour", bgColour_default);
		fgColour = (int)KeyValue.readVar("fgColour", fgColour_default);

		widgetBG = (int)KeyValue.readVar("widgetBG", widgetBG_default);
		widgetFG = (int)KeyValue.readVar("widgetFG", widgetFG_default);

		White = ContextCompat.getColor(context, R.color.White);
		Black = ContextCompat.getColor(context, R.color.Black);

		AlmostBlack = ContextCompat.getColor(context, R.color.AlmostBlack);
		LightBlueAccent = ContextCompat.getColor(context, R.color.LightBlueAccent);
		DarkBlueAccent = ContextCompat.getColor(context, R.color.DarkBlueAccent);
		DarkGray = ContextCompat.getColor(context, R.color.DarkGray);
		LightGray = ContextCompat.getColor(context, R.color.LightGray);
	}
}