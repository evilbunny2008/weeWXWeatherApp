package com.odiousapps.weewxweather;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.webkit.WebView;
import android.widget.TextView;

public class Stats  extends AppCompatActivity implements GestureDetector.OnGestureListener
{
    GestureDetector gestureDetector;

    Common common = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stats);

        if(getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        gestureDetector = new GestureDetector(Stats.this, Stats.this);

        common = new Common(this);

        updateFields();
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        finish();
        return true;
    }

    public void checkFields(TextView tv, String txt)
    {
        if(!tv.getText().toString().equals(txt))
            tv.setText(txt);
    }

    public void updateFields()
    {
        int iw = 17;

        String bits[] = common.GetStringPref("LastDownload", "").split("\\|");
        if (bits.length < 157)
            return;

// Today Stats
        checkFields((TextView)findViewById(R.id.textView), bits[56]);
        checkFields((TextView)findViewById(R.id.textView2), bits[54]);

        String stmp;
        StringBuilder sb = new StringBuilder();

        String header = "<html><body style='text-align:center;'>";
        String footer = "</body></html>";

        sb.append(header);

        sb.append("<span style='font-size:18pt;font-weight:bold;'>Today's Statistics</span>");
        sb.append("<table style='width:100%;border:0px;'>");

        stmp = "<tr><td><img style='width:"+iw+"px' src='temperature.png'></td><td>" + bits[3] + "\u00B0" + bits[60] + "</td><td>" + bits[4] +
                "</td><td>" + bits[2] + "</td><td>" + bits[1]  + "\u00B0" + bits[60] + "</td><td><img style='width:" + iw + "px' src='temperature.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='droplet.png'></td><td>" + bits[15] + "\u00B0" + bits[60] + "</td><td>" + bits[16] +
                "</td><td>" + bits[14] + "</td><td>" + bits[13]  + "\u00B0" + bits[60] + "</td><td><img style='width:"+iw+"px' src='droplet.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='humidity.png'></td><td>" + bits[9] + bits[64] + "</td><td>" + bits[10] +
                "</td><td>" + bits[8] + "</td><td>" + bits[6]  + bits[64] + "</td><td><img style='width:"+iw+"px' src='humidity.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='barometer.png'></td><td>" + bits[39] + bits[63] + "</td><td>" + bits[40] +
                "</td><td>" + bits[42] + "</td><td>" + bits[41]  + bits[63] + "</td><td><img style='width:"+iw+"px' src='barometer.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='windsock.png'></td><td colspan='3'>" + bits[25] + bits[61] + " " + bits[32] + " " + bits[33] +
                "</td><td>" + bits[20] + bits[62] + "</td><td><img style='width:"+iw+"px' src='umbrella.png'></td></tr>";
        sb.append(stmp);

        stmp = "</table><br>";
        sb.append(stmp);

        sb.append("<span style='font-size:18pt;font-weight:bold;'>Yesterday's Statistics</span>");
        sb.append("<table style='width:100%;border:0px;'>");

        stmp = "<tr><td><img style='width:"+iw+"px' src='temperature.png'></td><td>" + bits[67] + "\u00B0" + bits[60] + "</td><td>" + bits[68] +
                "</td><td>" + bits[66] + "</td><td>" + bits[69]  + "\u00B0" + bits[60] + "</td><td><img style='width:" + iw + "px' src='temperature.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='droplet.png'></td><td>" + bits[78] + "\u00B0" + bits[60] + "</td><td>" + bits[79] +
                "</td><td>" + bits[77] + "</td><td>" + bits[76]  + "\u00B0" + bits[60] + "</td><td><img style='width:"+iw+"px' src='droplet.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='humidity.png'></td><td>" + bits[82] + bits[64] + "</td><td>" + bits[83] +
                "</td><td>" + bits[81] + "</td><td>" + bits[80]  + bits[64] + "</td><td><img style='width:"+iw+"px' src='humidity.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='barometer.png'></td><td>" + bits[84] + bits[63] + "</td><td>" + bits[85] +
                "</td><td>" + bits[81] + "</td><td>" + bits[80]  + bits[63] + "</td><td><img style='width:"+iw+"px' src='barometer.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='windsock.png'></td><td colspan='3'>" + bits[69] + bits[61] + " " + bits[70] + " " + bits[71] +
                "</td><td>" + bits[21] + bits[62] + "</td><td><img style='width:"+iw+"px' src='umbrella.png'></td></tr>";
        sb.append(stmp);

        stmp = "</table><br>";
        sb.append(stmp);

        if (bits.length >= 110 && !bits[110].equals(""))
        {
            sb.append("<span style='font-size:18pt;font-weight:bold;'>This Month's Statistics</span>");
            sb.append("<table style='width:100%;border:0px;'>");

            stmp = "<tr><td><img style='width:" + iw + "px' src='temperature.png'></td><td>" + bits[90] + "\u00B0" + bits[60] + "</td><td>" + bits[91].split(" ")[0] +
                    "</td><td>" + bits[89].split(" ")[0] + "</td><td>" + bits[88] + "\u00B0" + bits[60] + "</td><td><img style='width:" + iw + "px' src='temperature.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='droplet.png'></td><td>" + bits[101] + "\u00B0" + bits[60] + "</td><td>" + bits[102].split(" ")[0] +
                    "</td><td>" + bits[100].split(" ")[0] + "</td><td>" + bits[99] + "\u00B0" + bits[60] + "</td><td><img style='width:" + iw + "px' src='droplet.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='humidity.png'></td><td>" + bits[105] + bits[64] + "</td><td>" + bits[106].split(" ")[0] +
                    "</td><td>" + bits[104].split(" ")[0] + "</td><td>" + bits[103] + bits[64] + "</td><td><img style='width:" + iw + "px' src='humidity.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='barometer.png'></td><td>" + bits[107] + bits[63] + "</td><td>" + bits[108].split(" ")[0] +
                    "</td><td>" + bits[110].split(" ")[0] + "</td><td>" + bits[109] + bits[63] + "</td><td><img style='width:" + iw + "px' src='barometer.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='windsock.png'></td><td colspan='3'>" + bits[92] + bits[61] + " " + bits[93] + " " + bits[94].split(" ")[0] +
                    "</td><td>" + bits[22] + bits[62] + "</td><td><img style='width:" + iw + "px' src='umbrella.png'></td></tr>";
            sb.append(stmp);

            stmp = "</table><br>";
            sb.append(stmp);
        }

        if (bits.length >= 133 && !bits[133].equals(""))
        {
            sb.append("<span style='font-size:18pt;font-weight:bold;'>This Year's Statistics</span>");
            sb.append("<table style='width:100%;border:0px;'>");

            stmp = "<tr><td><img style='width:" + iw + "px' src='temperature.png'></td><td>" + bits[113] + "\u00B0" + bits[60] + "</td><td>" + bits[114].split(" ")[0] +
                    "</td><td>" + bits[112].split(" ")[0] + "</td><td>" + bits[111] + "\u00B0" + bits[60] + "</td><td><img style='width:" + iw + "px' src='temperature.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='droplet.png'></td><td>" + bits[124] + "\u00B0" + bits[60] + "</td><td>" + bits[125].split(" ")[0] +
                    "</td><td>" + bits[123].split(" ")[0] + "</td><td>" + bits[122] + "\u00B0" + bits[60] + "</td><td><img style='width:" + iw + "px' src='droplet.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='humidity.png'></td><td>" + bits[128] + bits[64] + "</td><td>" + bits[129].split(" ")[0] +
                    "</td><td>" + bits[127].split(" ")[0] + "</td><td>" + bits[126] + bits[64] + "</td><td><img style='width:" + iw + "px' src='humidity.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='barometer.png'></td><td>" + bits[130] + bits[63] + "</td><td>" + bits[131].split(" ")[0] +
                    "</td><td>" + bits[133].split(" ")[0] + "</td><td>" + bits[132] + bits[63] + "</td><td><img style='width:" + iw + "px' src='barometer.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='windsock.png'></td><td colspan='3'>" + bits[115] + bits[61] + " " + bits[116] + " " + bits[117].split(" ")[0] +
                    "</td><td>" + bits[23] + bits[62] + "</td><td><img style='width:" + iw + "px' src='umbrella.png'></td></tr>";
            sb.append(stmp);

            stmp = "</table><br>";
            sb.append(stmp);
        }

        if (bits.length >= 157 && !bits[157].equals(""))
        {
            sb.append("<span style='font-size:18pt;font-weight:bold;'>All Time Statistics</span>");
            sb.append("<table style='width:100%;border:0px;'>");

            stmp = "<tr><td><img style='width:" + iw + "px' src='temperature.png'></td><td>" + bits[136] + "\u00B0" + bits[60] + "</td><td>" + bits[137].split(" ")[0] +
                    "</td><td>" + bits[135].split(" ")[0] + "</td><td>" + bits[134] + "\u00B0" + bits[60] + "</td><td><img style='width:" + iw + "px' src='temperature.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='droplet.png'></td><td>" + bits[147] + "\u00B0" + bits[60] + "</td><td>" + bits[148].split(" ")[0] +
                    "</td><td>" + bits[146].split(" ")[0] + "</td><td>" + bits[145] + "\u00B0" + bits[60] + "</td><td><img style='width:" + iw + "px' src='droplet.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='humidity.png'></td><td>" + bits[151] + bits[64] + "</td><td>" + bits[152].split(" ")[0] +
                    "</td><td>" + bits[150].split(" ")[0] + "</td><td>" + bits[149] + bits[64] + "</td><td><img style='width:" + iw + "px' src='humidity.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='barometer.png'></td><td>" + bits[153] + bits[63] + "</td><td>" + bits[154].split(" ")[0] +
                    "</td><td>" + bits[156].split(" ")[0] + "</td><td>" + bits[155] + bits[63] + "</td><td><img style='width:" + iw + "px' src='barometer.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='windsock.png'></td><td colspan='3'>" + bits[138] + bits[61] + " " + bits[139] + " " + bits[140].split(" ")[0] +
                    "</td><td>" + bits[157] + bits[62] + "</td><td><img style='width:" + iw + "px' src='umbrella.png'></td></tr>";
            sb.append(stmp);

            stmp = "</table><br>";
            sb.append(stmp);
        }

        sb.append(footer);

        Common.LogMessage("sb: "+sb.toString());

        WebView wv = findViewById(R.id.webView1);
        wv.loadDataWithBaseURL("file:///android_res/drawable/", sb.toString(), "text/html", "utf-8", null);
    }

    @Override
    public void finish()
    {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public boolean onFling(MotionEvent motionEvent1, MotionEvent motionEvent2, float X, float Y)
    {
        if(motionEvent2.getX() - motionEvent1.getX() > 100)
        {
            Common.LogMessage("Swipe Right");
            finish();
        }

        if(motionEvent1.getX() - motionEvent2.getX() > 100)
        {
            Common.LogMessage("Swipe Left");
            startActivity(new Intent(getBaseContext(), Forecast.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }

        return true;
    }

    @Override
    public void onLongPress(MotionEvent arg0)
    {
    }

    @Override
    public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3)
    {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent arg0)
    {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent arg0)
    {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent)
    {
        return gestureDetector.onTouchEvent(motionEvent);
    }

    @Override
    public boolean onDown(MotionEvent arg0)
    {
        return false;
    }
}