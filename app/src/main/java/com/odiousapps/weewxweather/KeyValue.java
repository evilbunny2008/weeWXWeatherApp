package com.odiousapps.weewxweather;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
class KeyValue
{
	static int theme;
	static int mode;
	static int bgColour;
	static int fgColour;
	static int UpdateFrequency;

	final static int forecast = 1;
	final static int radar = 2;
	final static int rotated = 3;

	final static Map<Integer, Integer> widgetMinHeight = new HashMap<>();
	final static Map<Integer, Integer> widgetMinWidth = new HashMap<>();
	final static Map<Integer, Integer> widgetMaxHeight = new HashMap<>();
	final static Map<Integer, Integer> widgetMaxWidth = new HashMap<>();
}
