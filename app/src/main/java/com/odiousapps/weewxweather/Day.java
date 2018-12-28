package com.odiousapps.weewxweather;

class Day
{
	String day = "";
	String icon = "";
	String text = "";
	String max = "";
	String min = "";
	long timestamp = 0;

	public String toString()
	{
		return "day == " + day + "\nicon == " + icon + "\ntext == " + text + "\nmax == " + max + "\nmin == " + min + "\ntimestamp == " + timestamp + "\n\n";
	}
}
