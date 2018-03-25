package com.odiousapps.weewxweather;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.util.Timer;
import java.util.TimerTask;

public class Webcam extends Activity
{
    Common common = null;
    WebView wv;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webcam);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        ActionBar actionBar = getActionBar();
        if(actionBar != null)
            actionBar.hide();

        common = new Common(this);

        wv = findViewById(R.id.webcam);

        //noinspection AndroidLintClickableViewAccessibility
        wv.setOnTouchListener(new OnSwipeTouchListener(this)
        {
            @Override
            public void onSwipeUp()
            {
                finish();
            }

            @Override
            public void onSwipeDown()
            {
                if(!common.GetStringPref("CUSTOM_URL", "").equals(""))
                {
                    startActivity(new Intent(getBaseContext(), Custom.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
            }

            @Override
            public void onSwipeRight()
            {
                finish();
            }

            @Override
            public void onSwipeLeft()
            {
                if(!common.GetStringPref("CUSTOM_URL", "").equals(""))
                {
                    startActivity(new Intent(getBaseContext(), Custom.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
            }

            @Override
            public void longPress(MotionEvent e)
            {
                Common.LogMessage("long press");
                reloadWebView();
            }
        });

        reloadWebView();
        Common.LogMessage("set things in motion!");

        new ReloadWebView(300);
    }

    @Override
    public void finish()
    {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    protected void reloadWebView()
    {
        Common.LogMessage("reload webcam...");
        wv.getSettings().setAppCacheEnabled(false);
        wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        wv.getSettings().setUserAgentString(Common.UA);
        wv.clearCache(true);
        String webcam = common.GetStringPref("WEBCAM_URL", "");

        if (webcam == null || webcam.equals(""))
            webcam = "http://mx.cafesydney.com:8888/mjpg/video.mjpg";

        String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "  <head>\n" +
                "    <meta charset='utf-8'>\n" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                "  </head>\n" +
                "  <body style='padding:0px;margin:0px;'>\n" +
                "\t<img style='margin:0px;padding:0px;border:0px;text-align:center;max-width:100%;width:auto;height:auto;'\n" +
                "\tsrc='" + webcam + "?date=" + System.currentTimeMillis() + "'>\n" +
                "  </body>\n" +
                "</html>";
        wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
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