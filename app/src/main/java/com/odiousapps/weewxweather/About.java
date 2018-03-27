package com.odiousapps.weewxweather;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class About
{
    private Common common;

    About(Common common)
    {
        this.common = common;
    }

    View myAbout(LayoutInflater inflater, ViewGroup container)
    {
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);
        TextView tv = rootView.findViewById(R.id.about);

        String lines = "<html><body>Big thanks to the <a href='http://weewx.com'>WeeWx project</a>, as this app " +
                "wouldn't be possible otherwise.<br><br>" +
                "Weather Icons from <a href='https://www.flaticon.com/'>FlatIcon</a> and " +
                "is licensed under <a href='http://creativecommons.org/licenses/by/3.0/'>CC 3.0 BY</a><br><br>" +
                "<a href='https://www.yahoo.com/?ilc=401'>Yahoo! Weather</a> Forecast API<br><br>" +
                "This app is by <a href='https://odiousapps.com'>OdiousApps</a>.</body</html>";

        tv.setText(Html.fromHtml(lines));
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        return rootView;
    }
}
