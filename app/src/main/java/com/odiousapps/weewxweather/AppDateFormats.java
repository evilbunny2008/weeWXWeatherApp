package com.odiousapps.weewxweather;

import android.content.Context;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Locale;

@SuppressWarnings("unused")
class AppDateFormats
{
	private AppDateFormats() {}

	public static SimpleDateFormat iso8601Offset() { return machine("yyyy-MM-dd'T'HH:mm:ssXXX"); }   // sdf1
	public static SimpleDateFormat isoDate()       { return machine("yyyy-MM-dd"); }                 // sdf4
	public static SimpleDateFormat isoMillis()     { return machine("yyyy-MM-dd HH:mm:ss.SSS"); }    // sdf5
	public static SimpleDateFormat isoSeconds()    { return machine("yyyy-MM-dd HH:mm:ss"); }        // sdf10
	public static SimpleDateFormat isoLocal()      { return machine("yyyy-MM-dd'T'HH:mm:ss"); }      // sdf12
	public static SimpleDateFormat logMillis()     { return machine("dd MMM yyyy HH:mm:ss.SSS"); }   // sdf13
	public static SimpleDateFormat isoMillisOffset(){ return machine("yyyy-MM-dd HH:mm:ss.SSS XXX"); } // sdf14
	public static SimpleDateFormat feedDateHour()  { return machine("dd.MM.yyyy' 'HH"); }            // sdf11

	private static SimpleDateFormat machine(String pattern)
	{
		return new SimpleDateFormat(pattern, Locale.US);
	}

	public static SimpleDateFormat display(Context ctx, String skeleton)
	{
		Locale locale = Locale.getDefault();
		String hour = DateFormat.is24HourFormat(ctx) ? "H" : "h";
		String pattern = DateFormat.getBestDateTimePattern(locale, skeleton.replace("j", hour));
		return new SimpleDateFormat(pattern, locale);
	}

	public static SimpleDateFormat displayWithOffset(Context ctx, String skeleton)
	{
		Locale locale = Locale.getDefault();
		String hour = DateFormat.is24HourFormat(ctx) ? "H" : "h";
		String pattern = DateFormat.getBestDateTimePattern(locale, skeleton.replace("j", hour));
		return new SimpleDateFormat(pattern + " XXX", locale);
	}
}
