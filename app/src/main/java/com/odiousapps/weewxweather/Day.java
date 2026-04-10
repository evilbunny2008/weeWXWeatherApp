package com.odiousapps.weewxweather;

class Day
{
	String icon = "";
	String text = "";
	String max = "";
	String min = "";
	long timestamp = 0;

	public String toString()
	{
		return "[timestamp: " + timestamp + ", icon: " + icon + ", max: " + max + ", min: " + min + ", text: " + text + "]";
	}
}
