package com.odiousapps.weewxweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RNAutoStart extends BroadcastReceiver
{
    public void onReceive(Context c, Intent i)
    {
        Common.LogMessage("Broadcast intent detected " + i.getAction());

        Common.LogMessage("RNAutostart: i=" + i.toString());
        Common common = new Common(c);

        if(myService.singleton == null && common.GetBoolPref("bgdl", true))
        {
            c.startService(new Intent(c, myService.class));
            Common.LogMessage("onReceive() start");
        }
    }
}