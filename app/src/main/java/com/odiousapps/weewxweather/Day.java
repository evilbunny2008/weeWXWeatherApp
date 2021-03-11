package com.odiousapps.weewxweather;

import androidx.annotation.NonNull;

class Day
{
	String day = "";
	String icon = "";
	String text = "";
	String max = "";
	String min = "";
	long timestamp = 0;

	@NonNull
	public String toString()
	{
		return "day == " + day + "\nicon == " + icon + "\ntext == " + text + "\nmax == " + max + "\nmin == " + min + "\ntimestamp == " + timestamp + "\n\n";
	}
}
