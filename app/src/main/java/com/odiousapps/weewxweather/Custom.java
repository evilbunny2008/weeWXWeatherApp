package com.odiousapps.weewxweather;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

import java.util.Timer;
import java.util.TimerTask;

public class Custom extends Activity
{
    Common common = null;
    WebView wv;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        ActionBar actionBar = getActionBar();
        if(actionBar != null)
            actionBar.hide();

        common = new Common(this);
        wv = findViewById(R.id.custom);

        //noinspection AndroidLintClickableViewAccessibility
        wv.setOnTouchListener(new OnSwipeTouchListener(this)
        {
            @Override
            public void onSwipeRight()
            {
                finish();
            }

            @Override
            public void longPress(MotionEvent e)
            {
                Common.LogMessage("long press");
                Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(150);
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
        Common.LogMessage("reload custom...");

        String custom = common.GetStringPref("CUSTOM_URL", "");

        if (custom == null || custom.equals(""))
            return;

        wv.loadUrl(custom);
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