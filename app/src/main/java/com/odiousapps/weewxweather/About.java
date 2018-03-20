package com.odiousapps.weewxweather;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Window;
import android.widget.TextView;

public class About extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_dialog);

        TextView tv = findViewById(R.id.about);

        String lines = "<html><body>Weather Icons from <a href='https://www.flaticon.com/'>FlatIcon</a> and " +
                "is licensed under <a href='http://creativecommons.org/licenses/by/3.0/'>CC 3.0 BY</a> and " +
                "<a href='https://www.yahoo.com/?ilc=401'>Yahoo! Weather</a><br><br>This app was created by " +
                "<a href='https://odiousapps.com'>OdiousApps</a>.</body</html>";

        tv.setText(Html.fromHtml(lines));
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
