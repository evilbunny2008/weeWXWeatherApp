package com.odiousapps.weewxweather;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener
{
    // App Icon Source
    // http://www.clker.com/cliparts/5/6/4/d/1206565706595088919Anonymous_simple_weather_symbols_13.svg.hi.png

    GestureDetector gestureDetector;
    Common common = null;
    int REQUEST_CODE = 1;


    public void checkFields(TextView tv, String txt)
    {
        if(!tv.getText().toString().equals(txt))
            tv.setText(txt);
    }

    public void updateFields()
    {
        Common.LogMessage("updateFields()");
        String bits[] = common.GetStringPref("LastDownload","").split("\\|");
        if(bits.length < 65)
            return;

        Common.LogMessage("updating fields.");

        checkFields((TextView)findViewById(R.id.textView), bits[56]);
        checkFields((TextView)findViewById(R.id.textView2), bits[54] + " " + bits[55]);
        checkFields((TextView)findViewById(R.id.textView3), bits[0] + bits[60]);

        checkFields((TextView)findViewById(R.id.textView4), bits[25] + bits[61]);
        checkFields((TextView)findViewById(R.id.textView5), bits[37] + bits[63]);
        checkFields((TextView)findViewById(R.id.textView6), bits[29]);
        checkFields((TextView)findViewById(R.id.textView7), bits[6] + bits[64]);
        checkFields((TextView)findViewById(R.id.textView8), bits[20] + bits[62]);
        checkFields((TextView)findViewById(R.id.textView9), bits[12] + bits[60]);
        checkFields((TextView)findViewById(R.id.textView10), bits[45] + "UVI");
        checkFields((TextView)findViewById(R.id.textView11), bits[43] + "W/m\u00B2");

        checkFields((TextView)findViewById(R.id.textView12), bits[57]);
        checkFields((TextView)findViewById(R.id.textView13), bits[58]);
        checkFields((TextView)findViewById(R.id.textView14), bits[47]);
        checkFields((TextView)findViewById(R.id.textView15), bits[48]);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        common = new Common(this);

        gestureDetector = new GestureDetector(MainActivity.this, MainActivity.this);

        if(common.GetStringPref("BASE_URL", "").equals(""))
            startActivityForResult(new Intent(getBaseContext(), Settings.class), REQUEST_CODE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(myService.UPDATE_INTENT);
        registerReceiver(serviceReceiver, filter);

        // use last downloaded data while a bg thread runs
        startService();
        reloadWebView();
        Common.LogMessage("set things in motion!");

        new ReloadWebView(600);
    }

    @Override
    public boolean onFling(MotionEvent motionEvent1, MotionEvent motionEvent2, float X, float Y)
    {
        if(motionEvent1.getX() - motionEvent2.getX() > 100)
        {
            Common.LogMessage("Swipe Left");
            startActivity(new Intent(getBaseContext(), Stats.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            return true;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Common.LogMessage("onActivityResult();");

        Common.LogMessage("requestCode == "+requestCode);
        Common.LogMessage("resultCode == "+resultCode);

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE)
        {
            if (data.hasExtra("urlChanged"))
            {
                Common.LogMessage("w00t!");
                if(!common.GetStringPref("BASE_URL", "").equals(""))
                    getWeather();
            }
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.weather_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId())
        {
            case R.id.forecast:
                startActivity(new Intent(getBaseContext(), Forecast.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            case R.id.refresh:
                getWeather();
                return true;
            case R.id.settings:
                startActivityForResult(new Intent(getBaseContext(), Settings.class), REQUEST_CODE);
                return true;
            case R.id.stats:
                startActivity(new Intent(getBaseContext(), Stats.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            case R.id.about:
                startActivity(new Intent(getBaseContext(), About.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void getWeather()
    {
        if(myService.singleton != null)
            myService.singleton.getWeather();
    }

    @Override
    public void onDestroy()
    {
        stopUpdates();
        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {
        if(common.GetBoolPref("bgdl", true))
            moveTaskToBack(true);
        else
            finish();
    }

    @Override
    public void finish()
    {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        unregisterReceiver(serviceReceiver);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        updateFields();
    }

    private void startService()
    {
        if(myService.singleton == null)
        {
            Common.LogMessage("starting service.");
            if(myService.singleton == null)
                startService(new Intent(this, myService.class));

            Common.LogMessage("Currently listening for broadcasts");

            getWeather();
        } else {
            Common.LogMessage("service already running.");
        }
    }

    private void stopUpdates()
    {
        if(!common.GetBoolPref("bgdl", true))
        {
            stopService(new Intent(this, myService.class));
            Common.LogMessage("Stopping Service.");
        }
    }

    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(myService.singleton == null || !myService.singleton.Update())
                return;

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
                            reloadWebView();
                            updateFields();
                        }
                    });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    protected void reloadWebView()
    {
        Common.LogMessage("reload radar...");
        WebView wv = findViewById(R.id.webView1);
        wv.getSettings().setAppCacheEnabled(false);
        wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        wv.getSettings().setUserAgentString(Common.UA);
        wv.clearCache(true);
        String radar = common.GetStringPref("RADAR_URL", "");

        if (radar != null && !radar.equals(""))
        {
            String html = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "  <head>\n" +
                    "    <meta charset='utf-8'>\n" +
                    "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                    "  </head>\n" +
                    "  <body>\n" +
                    "\t<br>\n" +
                    "\t<img style='margin:0px;padding:0px;border:0px;text-align:center;max-width:100%;width:auto;height:auto;'\n" +
                    "\tsrc='" + radar + "'>\n" +
                    "  </body>\n" +
                    "</html>";
            wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
        } else {
            String html = "<html><body>Radar URL not set, go to settings to change</body></html>";
            wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
        }
    }

    protected class ReloadWebView extends TimerTask
    {
        Activity context;
        Timer timer;

        private ReloadWebView(int seconds)
        {
            Common.LogMessage("new Timer == "+seconds);
            timer = new Timer();
            timer.schedule(this,0,seconds * 1000);
        }

        @Override
        public void run()
        {
            if(context == null || context.isFinishing())
            {
                // Activity killed
                this.cancel();
                return;
            }

            context.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    reloadWebView();
                }
            });
        }
    }
}