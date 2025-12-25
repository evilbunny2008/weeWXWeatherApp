package com.odiousapps.weewxweather;

import androidx.annotation.NonNull;

class Day
{
	String day = null;
	String icon = null;
	String text = null;
	String max = null;
	String min = null;
	long timestamp = 0;

	@NonNull
	public String toString()
	{
		return "day == " + day + "\nicon == " + icon + "\ntext == " + text + "\nmax == " + max + "\nmin == " + min + "\ntimestamp == " + timestamp + "\n\n";
	}
}
