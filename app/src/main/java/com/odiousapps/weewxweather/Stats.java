package com.odiousapps.weewxweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

public class Stats  extends AppCompatActivity
{
    int REQUEST_CODE = 2;
    WebView wv;
    Common common = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stats);

        if(getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        common = new Common(this);

        View v = findViewById(R.id.wholeScreen);
        //noinspection AndroidLintClickableViewAccessibility
        v.setOnTouchListener(new OnSwipeTouchListener(this)
        {
            @Override
            public void onSwipeRight()
            {
                Common.LogMessage("Swipe Right");
                finish();
            }

            @Override
            public void onSwipeLeft()
            {
                Common.LogMessage("Swipe Left");
                startActivity(new Intent(getBaseContext(), Forecast.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        wv = findViewById(R.id.webView1);
        //noinspection AndroidLintClickableViewAccessibility
        wv.setOnTouchListener(new OnSwipeTouchListener(this)
        {
            @Override
            public void onSwipeRight()
            {
                Common.LogMessage("Swipe Right");
                finish();
            }

            @Override
            public void onSwipeLeft()
            {
                Common.LogMessage("Swipe Left");
                startActivity(new Intent(getBaseContext(), Forecast.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        updateFields();

        IntentFilter filter = new IntentFilter();
        filter.addAction(myService.UPDATE_INTENT);
        registerReceiver(serviceReceiver, filter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.stats_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId())
        {
            case R.id.refresh:
                forceRefresh();
                return true;
            case R.id.forecast:
                startActivity(new Intent(getBaseContext(), Forecast.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            case R.id.webcam:
                startActivity(new Intent(getBaseContext(), Webcam.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            case R.id.mainmenu:
                finish();
                return true;
            case R.id.about:
                startActivity(new Intent(getBaseContext(), About.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            updateFields();
                        }
                    });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public void forceRefresh()
    {
        if(myService.singleton != null)
            myService.singleton.getWeather();
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
        if(bits.length < 157)
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

        stmp = "<tr><td><img style='width:"+iw+"px' src='temperature.png'></td><td>" + bits[3] + bits[60] + "</td><td>" + bits[4] +
                "</td><td>" + bits[2] + "</td><td>" + bits[1]  + bits[60] + "</td><td><img style='width:" + iw + "px' src='temperature.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='droplet.png'></td><td>" + bits[15] + bits[60] + "</td><td>" + bits[16] +
                "</td><td>" + bits[14] + "</td><td>" + bits[13]  + bits[60] + "</td><td><img style='width:"+iw+"px' src='droplet.png'></td></tr>";
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

        stmp = "<tr><td><img style='width:"+iw+"px' src='temperature.png'></td><td>" + bits[67] + bits[60] + "</td><td>" + bits[68] +
                "</td><td>" + bits[66] + "</td><td>" + bits[69]  + bits[60] + "</td><td><img style='width:" + iw + "px' src='temperature.png'></td></tr>";
        sb.append(stmp);

        stmp = "<tr><td><img style='width:"+iw+"px' src='droplet.png'></td><td>" + bits[78] + bits[60] + "</td><td>" + bits[79] +
                "</td><td>" + bits[77] + "</td><td>" + bits[76]  + bits[60] + "</td><td><img style='width:"+iw+"px' src='droplet.png'></td></tr>";
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

            stmp = "<tr><td><img style='width:" + iw + "px' src='temperature.png'></td><td>" + bits[90] + bits[60] + "</td><td>" + bits[91].split(" ")[0] +
                    "</td><td>" + bits[89].split(" ")[0] + "</td><td>" + bits[88] + bits[60] + "</td><td><img style='width:" + iw + "px' src='temperature.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='droplet.png'></td><td>" + bits[101] + bits[60] + "</td><td>" + bits[102].split(" ")[0] +
                    "</td><td>" + bits[100].split(" ")[0] + "</td><td>" + bits[99] + bits[60] + "</td><td><img style='width:" + iw + "px' src='droplet.png'></td></tr>";
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

            stmp = "<tr><td><img style='width:" + iw + "px' src='temperature.png'></td><td>" + bits[113] + bits[60] + "</td><td>" + bits[114].split(" ")[0] +
                    "</td><td>" + bits[112].split(" ")[0] + "</td><td>" + bits[111] + bits[60] + "</td><td><img style='width:" + iw + "px' src='temperature.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='droplet.png'></td><td>" + bits[124] + bits[60] + "</td><td>" + bits[125].split(" ")[0] +
                    "</td><td>" + bits[123].split(" ")[0] + "</td><td>" + bits[122] + bits[60] + "</td><td><img style='width:" + iw + "px' src='droplet.png'></td></tr>";
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

            stmp = "<tr><td><img style='width:" + iw + "px' src='temperature.png'></td><td>" + bits[136] + bits[60] + "</td><td>" + bits[137].split(" ")[0] +
                    "</td><td>" + bits[135].split(" ")[0] + "</td><td>" + bits[134] + bits[60] + "</td><td><img style='width:" + iw + "px' src='temperature.png'></td></tr>";
            sb.append(stmp);

            stmp = "<tr><td><img style='width:" + iw + "px' src='droplet.png'></td><td>" + bits[147] + bits[60] + "</td><td>" + bits[148].split(" ")[0] +
                    "</td><td>" + bits[146].split(" ")[0] + "</td><td>" + bits[145] + bits[60] + "</td><td><img style='width:" + iw + "px' src='droplet.png'></td></tr>";
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
        wv.loadDataWithBaseURL("file:///android_res/drawable/", sb.toString(), "text/html", "utf-8", null);
    }

    @Override
    public void finish()
    {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        unregisterReceiver(serviceReceiver);
    }
}