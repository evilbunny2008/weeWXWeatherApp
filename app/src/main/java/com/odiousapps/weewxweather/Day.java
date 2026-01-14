package com.odiousapps.weewxweather;

import androidx.annotation.NonNull;

class Day
{
	String icon = "";
	String text = "";
	String max = "";
	String min = "";
	long timestamp = 0;

	@NonNull
	public String toString()
	{
		return "[timestamp: " + timestamp + ", icon: " + icon + ", max: " + max + ", min: " + min + ", text: " + text + "]";
	}
}
