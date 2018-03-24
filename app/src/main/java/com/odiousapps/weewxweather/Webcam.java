package com.odiousapps.weewxweather;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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

public class Webcam extends AppCompatActivity implements GestureDetector.OnGestureListener
{
    GestureDetector gestureDetector;
    Common common = null;
    int REQUEST_CODE = 3;

    public void checkFields(TextView tv, String txt)
    {
        if(!tv.getText().toString().equals(txt))
            tv.setText(txt);
    }

    void updateFields()
    {
        String bits[] = common.GetStringPref("LastDownload","").split("\\|");
        if(bits.length < 65)
            return;

        checkFields((TextView)findViewById(R.id.textView), bits[56]);
        checkFields((TextView)findViewById(R.id.textView2), bits[54] + " " + bits[55]);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webcam);

        if(getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        common = new Common(this);

        gestureDetector = new GestureDetector(Webcam.this, Webcam.this);

        updateFields();
        reloadWebView();
        Common.LogMessage("set things in motion!");

        new ReloadWebView(300);
    }

    @Override
    public boolean onFling(MotionEvent motionEvent1, MotionEvent motionEvent2, float X, float Y)
    {
        if(motionEvent2.getX() - motionEvent1.getX() > 100)
        {
            Common.LogMessage("Swipe Left");
            finish();
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
                if(!common.GetStringPref("WEBCAM_URL", "").equals(""))
                    reloadWebView();
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
        inflater.inflate(R.menu.webcam, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId())
        {
            case R.id.mainmenu:
                startActivity(new Intent(getBaseContext(), MainActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            case R.id.refresh:
                reloadWebView();
                return true;
            case R.id.settings:
                startActivityForResult(new Intent(getBaseContext(), Settings.class), REQUEST_CODE);
                return true;
            case R.id.about:
                startActivity(new Intent(getBaseContext(), About.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void finish()
    {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    protected void reloadWebView()
    {
        if(myService.singleton == null || !myService.singleton.Update())
            return;

        Common.LogMessage("reload webcam...");
        WebView wv = findViewById(R.id.webView1);
        wv.getSettings().setAppCacheEnabled(false);
        wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        wv.getSettings().setUserAgentString(Common.UA);
        wv.clearCache(true);
        String webcam = common.GetStringPref("WEBCAM_URL", "");

        if (webcam != null && !webcam.equals(""))
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
                    "\tsrc='" + webcam + "'>\n" +
                    "  </body>\n" +
                    "</html>";
            wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
        } else {
            String html = "<html><body>Webcam URL not set, go to settings to change</body></html>";
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