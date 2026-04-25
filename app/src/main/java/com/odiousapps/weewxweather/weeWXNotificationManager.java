package com.odiousapps.weewxweather;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

@DontObfuscate
public class weeWXNotificationManager
{
	private final static EventBroadcaster<String> broadcaster = new EventBroadcaster<>();

	public static void updateNotificationMessage(String message)
	{
		broadcaster.broadcast(message);
	}

	public static void observeNotifications(LifecycleOwner owner, Observer<String> observer)
	{
		broadcaster.observe(owner, observer);
	}

	public static void removeNotificationObserver(Observer<String> observer)
	{
		broadcaster.removeObserver(observer);
	}
}
