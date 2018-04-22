package com.odiousapps.weewxweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import java.util.Locale;

public class Stats
{
    private Common common;
    private View rootView;
    private WebView wv;

    Stats(Common common)
    {
        this.common = common;
    }

    View myStats(LayoutInflater inflater, ViewGroup container)
    {
        rootView = inflater.inflate(R.layout.fragment_stats, container, false);
        rootView.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator != null)
                    vibrator.vibrate(250);
                Common.LogMessage("rootview long press");
                forceRefresh();
                return true;
            }
        });

        wv = rootView.findViewById(R.id.webView1);
	    wv.getSettings().setUserAgentString(Common.UA);
	    wv.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator != null)
                    vibrator.vibrate(250);
                Common.LogMessage("webview long press");
                forceRefresh();
                return true;
            }
        });

        updateFields();

        IntentFilter filter = new IntentFilter();
        filter.addAction(myService.UPDATE_INTENT);
        filter.addAction(myService.EXIT_INTENT);
        common.context.registerReceiver(serviceReceiver, filter);

        return rootView;
    }

    public void doStop()
    {
        Common.LogMessage("stats.java -- unregisterReceiver");
        common.context.unregisterReceiver(serviceReceiver);
    }

    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            try
            {
                Common.LogMessage("We have a hit, so we should probably update the screen.");
                String action = intent.getAction();
                if(action != null && action.equals(myService.UPDATE_INTENT))
                    updateFields();
                else if(action != null && action.equals(myService.EXIT_INTENT))
                    common.context.unregisterReceiver(serviceReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void forceRefresh()
    {
        if(myService.singleton != null)
            myService.singleton.getWeather();
    }

    private void checkFields(TextView tv, String txt)
    {
        if(!tv.getText().toString().equals(txt))
            tv.setText(txt);
    }

    private String convert(String cur)
    {
	    cur = cur.trim();

	    String[] bits = null;

	    if(!cur.contains(" "))
		    return cur;

	    try
	    {
		    //cur = "9:34:00 PM";
		    bits = cur.trim().split(" ");
	    } catch (Exception e) {
    		e.printStackTrace();
	    }

	    if(bits == null || bits.length < 2)
	    	return cur;

	    String[] time = null;
	    try
	    {
		    time = bits[0].trim().split(":");
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }

	    if(time == null || time.length < 3)
	    	return cur;

	    int hours = Integer.parseInt(time[0]);
        int mins = Integer.parseInt(time[1]);
        int secs = Integer.parseInt(time[2]);

	    boolean pm = bits[1].trim().toLowerCase().equals("pm");

	    Log.i("weeWx", "pm == '" + bits[1] + "'");

	    if(!pm && hours == 12)
	    	hours = 0;
	    else if(pm && hours != 12)
	    	hours = hours + 12;

	    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs);
    }

    private String getTime(String str)
    {
    	str = str.trim();

    	if(!str.contains(" "))
    		return str;

    	try
	    {
		    return str.split(" ")[0];
	    } catch (Exception e) {
    		e.printStackTrace();
	    }

	    return str;
    }

    private void updateFields()
    {
        int iw = 17;

        String bits[] = common.GetStringPref("LastDownload", "").split("\\|");
        if(bits.length < 157)
            return;

//      Today Stats
        checkFields((TextView)rootView.findViewById(R.id.textView), bits[56]);
        checkFields((TextView)rootView.findViewById(R.id.textView2), bits[54]);

        String stmp;
        StringBuilder sb = new StringBuilder();

        String header = "<html><body style='text-align:center;'>";
        String footer = "</body></html>";

        sb.append(header);

        sb.append("<span style='font-size:18pt;font-weight:bold;'>Today's Statistics</span>");
        sb.append("<table style='width:100%;border:0px;'>");

        stmp = "<tr><td><img style='width:"+iw+"px' src='temperature.png'></td><td>" + bits[3] + bits[60] + "</td><td>" + convert(bits[4]) +
                "</td><td>" + convert(bits[2]) + "</td><td>" + bits[1]  + bits[60] + "</td><td><img style='width:" + iw + "px' src='temperature.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='droplet.png'></td><td>" + bits[15] + bits[60] + "</td><td>" + convert(bits[16]) +
                "</td><td>" + convert(bits[14]) + "</td><td>" + bits[13]  + bits[60] + "</td><td><img style='width:"+iw+"px' src='droplet.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='humidity.png'></td><td>" + bits[9] + bits[64] + "</td><td>" + convert(bits[10]) +
                "</td><td>" + convert(bits[8]) + "</td><td>" + bits[6]  + bits[64] + "</td><td><img style='width:"+iw+"px' src='humidity.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='barometer.png'></td><td>" + bits[39] + bits[63] + "</td><td>" + convert(bits[40]) +
                "</td><td>" + convert(bits[42]) + "</td><td>" + bits[41]  + bits[63] + "</td><td><img style='width:"+iw+"px' src='barometer.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='windsock.png'></td><td colspan='3'>" + bits[25] + bits[61] + " " + bits[32] + " " + convert(bits[33]) +
                "</td><td>" + bits[20] + bits[62] + "</td><td><img style='width:"+iw+"px' src='umbrella.png'></td></tr>";
        sb.append(stmp);

        stmp = "</table><br>";
        sb.append(stmp);

        sb.append("<span style='font-size:18pt;font-weight:bold;'>Yesterday's Statistics</span>");
        sb.append("<table style='width:100%;border:0px;'>");

        stmp = "<tr><td><img style='width:"+iw+"px' src='temperature.png'></td><td>" + bits[67] + bits[60] + "</td><td>" + convert(bits[68]) +
                "</td><td>" + convert(bits[66]) + "</td><td>" + bits[69]  + bits[60] + "</td><td><img style='width:" + iw + "px' src='temperature.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='droplet.png'></td><td>" + bits[78] + bits[60] + "</td><td>" + convert(bits[79]) +
                "</td><td>" + convert(bits[77]) + "</td><td>" + bits[76]  + bits[60] + "</td><td><img style='width:"+iw+"px' src='droplet.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='humidity.png'></td><td>" + bits[82] + bits[64] + "</td><td>" + convert(bits[83]) +
                "</td><td>" + convert(bits[81]) + "</td><td>" + bits[80]  + bits[64] + "</td><td><img style='width:"+iw+"px' src='humidity.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='barometer.png'></td><td>" + bits[84] + bits[63] + "</td><td>" + convert(bits[85]) +
                "</td><td>" + convert(bits[87]) + "</td><td>" + bits[86]  + bits[63] + "</td><td><img style='width:"+iw+"px' src='barometer.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='windsock.png'></td><td colspan='3'>" + bits[69] + bits[61] + " " + bits[70] + " " + convert(bits[71]) +
                "</td><td>" + bits[21] + bits[62] + "</td><td><img style='width:"+iw+"px' src='umbrella.png'></td></tr>";
        sb.append(stmp);

        stmp = "</table><br>";
        sb.append(stmp);

        //noinspection ConstantConditions
        if(bits.length >= 110 && !bits[110].equals(""))
        {
            sb.append("<span style='font-size:18pt;font-weight:bold;'>This Month's Statistics</span>");
            sb.append("<table style='width:100%;border:0px;'>");

		    stmp = "<tr><td><img style='width:" + iw + "px' src='temperature.png'></td><td>" + bits[90] + bits[60] + "</td><td>" + getTime(bits[91]) +
			            "</td><td>" + getTime(bits[89]) + "</td><td>" + bits[88] + bits[60] + "</td><td><img style='width:" + iw + "px' src='temperature.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='droplet.png'></td><td>" + bits[101] + bits[60] + "</td><td>" + getTime(bits[102]) +
                    "</td><td>" + getTime(bits[100]) + "</td><td>" + bits[99] + bits[60] + "</td><td><img style='width:" + iw + "px' src='droplet.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='humidity.png'></td><td>" + bits[105] + bits[64] + "</td><td>" + getTime(bits[106]) +
                    "</td><td>" + getTime(bits[104]) + "</td><td>" + bits[103] + bits[64] + "</td><td><img style='width:" + iw + "px' src='humidity.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='barometer.png'></td><td>" + bits[107] + bits[63] + "</td><td>" + getTime(bits[108]) +
                    "</td><td>" + getTime(bits[110]) + "</td><td>" + bits[109] + bits[63] + "</td><td><img style='width:" + iw + "px' src='barometer.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='windsock.png'></td><td colspan='3'>" + bits[92] + bits[61] + " " + bits[93] + " " + getTime(bits[94]) +
                    "</td><td>" + bits[22] + bits[62] + "</td><td><img style='width:" + iw + "px' src='umbrella.png'></td></tr>";
            sb.append(stmp);

            stmp = "</table><br>";
            sb.append(stmp);
        }

        //noinspection ConstantConditions
        if (bits.length >= 133 && !bits[133].equals(""))
        {
            sb.append("<span style='font-size:18pt;font-weight:bold;'>This Year's Statistics</span>");
            sb.append("<table style='width:100%;border:0px;'>");

            stmp = "<tr><td><img style='width:" + iw + "px' src='temperature.png'></td><td>" + bits[113] + bits[60] + "</td><td>" + getTime(bits[114]) +
                    "</td><td>" + getTime(bits[112]) + "</td><td>" + bits[111] + bits[60] + "</td><td><img style='width:" + iw + "px' src='temperature.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='droplet.png'></td><td>" + bits[124] + bits[60] + "</td><td>" + getTime(bits[125]) +
                    "</td><td>" + getTime(bits[123]) + "</td><td>" + bits[122] + bits[60] + "</td><td><img style='width:" + iw + "px' src='droplet.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='humidity.png'></td><td>" + bits[128] + bits[64] + "</td><td>" + getTime(bits[129]) +
                    "</td><td>" + getTime(bits[127]) + "</td><td>" + bits[126] + bits[64] + "</td><td><img style='width:" + iw + "px' src='humidity.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='barometer.png'></td><td>" + bits[130] + bits[63] + "</td><td>" + getTime(bits[131]) +
                    "</td><td>" + getTime(bits[133]) + "</td><td>" + bits[132] + bits[63] + "</td><td><img style='width:" + iw + "px' src='barometer.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='windsock.png'></td><td colspan='3'>" + bits[115] + bits[61] + " " + bits[116] + " " + getTime(bits[117]) +
                    "</td><td>" + bits[23] + bits[62] + "</td><td><img style='width:" + iw + "px' src='umbrella.png'></td></tr>";
            sb.append(stmp);

            stmp = "</table><br>";
            sb.append(stmp);
        }

        //noinspection ConstantConditions
        if (bits.length >= 157 && !bits[157].equals(""))
        {
            sb.append("<span style='font-size:18pt;font-weight:bold;'>All Time Statistics</span>");
            sb.append("<table style='width:100%;border:0px;'>");

            stmp = "<tr><td><img style='width:" + iw + "px' src='temperature.png'></td><td>" + bits[136] + bits[60] + "</td><td>" + getTime(bits[137]) +
                    "</td><td>" + getTime(bits[135]) + "</td><td>" + bits[134] + bits[60] + "</td><td><img style='width:" + iw + "px' src='temperature.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='droplet.png'></td><td>" + bits[147] + bits[60] + "</td><td>" + getTime(bits[148]) +
                    "</td><td>" + getTime(bits[146]) + "</td><td>" + bits[145] + bits[60] + "</td><td><img style='width:" + iw + "px' src='droplet.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='humidity.png'></td><td>" + bits[151] + bits[64] + "</td><td>" + getTime(bits[152]) +
                    "</td><td>" + getTime(bits[150]) + "</td><td>" + bits[149] + bits[64] + "</td><td><img style='width:" + iw + "px' src='humidity.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='barometer.png'></td><td>" + bits[153] + bits[63] + "</td><td>" + getTime(bits[154]) +
                    "</td><td>" + getTime(bits[156]) + "</td><td>" + bits[155] + bits[63] + "</td><td><img style='width:" + iw + "px' src='barometer.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='windsock.png'></td><td colspan='3'>" + bits[138] + bits[61] + " " + bits[139] + " " + getTime(bits[140]) +
                    "</td><td>" + bits[157] + bits[62] + "</td><td><img style='width:" + iw + "px' src='umbrella.png'></td></tr>";
            sb.append(stmp);

            stmp = "</table><br>";
            sb.append(stmp);
        }

        sb.append(footer);

        Common.LogMessage("sb: "+sb.toString());
        wv.loadDataWithBaseURL("file:///android_res/drawable/", sb.toString(), "text/html", "utf-8", null);
    }
}
