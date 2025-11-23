package com.odiousapps.weewxweather;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
class KeyValue
{
	static int theme;
	static int widget_theme_mode;
	static int mode;
	static int bgColour;
	static int fgColour;

	static int widgetBG;
	static int widgetFG;

	final static Map<Integer, Integer> widgetMinHeight = new HashMap<>();
	final static Map<Integer, Integer> widgetMinWidth = new HashMap<>();
	final static Map<Integer, Integer> widgetMaxHeight = new HashMap<>();
	final static Map<Integer, Integer> widgetMaxWidth = new HashMap<>();

	static boolean save_app_debug_logs = false;
}
