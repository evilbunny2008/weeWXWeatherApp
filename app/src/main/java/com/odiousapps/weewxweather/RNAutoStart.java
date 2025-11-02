package com.odiousapps.weewxweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RNAutoStart extends BroadcastReceiver
{
	public void onReceive(Context c, Intent i)
	{
		Common.LogMessage("Broadcast intent detected " + i.getAction());
		Common.LogMessage("RNAutostart: i=" + i);

		if(c == null)
		{
			Common.LogMessage("RNAutostart.java failed, c == null");
			return;
		}

		Common.setAlarm("RNAutoStart");

		if(!Common.checkConnection())
		{
			Common.LogMessage("Skipping update due to wifi setting.");
			return;
		}

		Common.reload();
		Common.SendIntents();

		Common.LogMessage("onReceive() end");
	}
}