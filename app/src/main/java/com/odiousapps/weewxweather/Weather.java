package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;

class Weather
{
    private Common common;
    private View rootView;
    //private WebView wv;
    private GifImageView gif;

    Weather(Common common)
    {
        this.common = common;
    }

    private void checkFields(TextView tv, String txt)
    {
        if(!tv.getText().toString().equals(txt))
            tv.setText(txt);
    }

    private View updateFields()
    {
        Common.LogMessage("updateFields()");
        String bits[] = common.GetStringPref("LastDownload","").split("\\|");
        if(bits.length < 65)
            return rootView;

        checkFields((TextView)rootView.findViewById(R.id.textView), bits[56]);
        checkFields((TextView)rootView.findViewById(R.id.textView2), bits[54] + " " + bits[55]);
        checkFields((TextView)rootView.findViewById(R.id.textView3), bits[0] + bits[60]);

        checkFields((TextView)rootView.findViewById(R.id.textView4), bits[25] + bits[61]);
        checkFields((TextView)rootView.findViewById(R.id.textView5), bits[37] + bits[63]);
        checkFields((TextView)rootView.findViewById(R.id.textView6), bits[29]);
        checkFields((TextView)rootView.findViewById(R.id.textView7), bits[6] + bits[64]);
        checkFields((TextView)rootView.findViewById(R.id.textView8), bits[20] + bits[62]);
        checkFields((TextView)rootView.findViewById(R.id.textView9), bits[12] + bits[60]);
        checkFields((TextView)rootView.findViewById(R.id.textView10), bits[45] + "UVI");
        checkFields((TextView)rootView.findViewById(R.id.textView11), bits[43] + "W/m\u00B2");

        checkFields((TextView)rootView.findViewById(R.id.textView12), bits[57]);
        checkFields((TextView)rootView.findViewById(R.id.textView13), bits[58]);
        checkFields((TextView)rootView.findViewById(R.id.textView14), bits[47]);
        checkFields((TextView)rootView.findViewById(R.id.textView15), bits[48]);

        return rootView;
    }

    View myWeather(LayoutInflater inflater, ViewGroup container)
    {
        rootView = inflater.inflate(R.layout.fragment_weather, container, false);
        rootView.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator != null)
                    vibrator.vibrate(150);
                Common.LogMessage("rootview long press");
                forceRefresh();
                return true;
            }
        });

        gif = rootView.findViewById(R.id.radar);
        gif.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator != null)
                    vibrator.vibrate(150);
                Common.LogMessage("gif long press");
                reloadWebView();
                return true;
            }
        });

        if(new File(common.context.getFilesDir() + "/radar.gif").exists())
            gif.setGifBitmap(common.context.getFilesDir() + "/radar.gif");

        reloadWebView();

        IntentFilter filter = new IntentFilter();
        filter.addAction(myService.UPDATE_INTENT);
        filter.addAction(myService.EXIT_INTENT);
        common.context.registerReceiver(serviceReceiver, filter);

        return updateFields();
    }

    private void forceRefresh()
    {
        if(myService.singleton != null)
            myService.singleton.getWeather();
    }

    private void reloadWebView()
    {
        Common.LogMessage("reload radar...");
        final String radar = common.GetStringPref("RADAR_URL", "");

        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                Bitmap bm;
                try
                {
                    Common.LogMessage("starting to download image from: " + radar);
                    URL url = new URL(radar);

                    InputStream ins = url.openStream();
                    File file = new File(common.context.getFilesDir(), "radar.gif");
                    FileOutputStream out = null;

                    try
                    {
                        out = new FileOutputStream(file);
                        final byte[] b = new byte[2048];
                        int length;
                        while ((length = ins.read(b)) != -1)
                            out.write(b, 0, length);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    } finally
                    {
                        try
                        {
                            if (ins != null)
                                ins.close();
                            if (out != null)
                                out.close();
                        } catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }

                    Common.LogMessage("done downloading, prompt handler to draw to movie");
                    handlerDone.sendEmptyMessage(0);
                } catch (UnknownHostException e) {
                    handlerDone.sendEmptyMessage(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        t.start();
    }

    @SuppressLint("HandlerLeak")
    private Handler handlerDone = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            try
            {
                gif.setGifBitmap(common.context.getFilesDir() + "/radar.gif");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            try
            {
                Common.LogMessage("Weather() We have a hit, so we should probably update the screen.");
                String action = intent.getAction();
                if(action != null && action.equals(myService.UPDATE_INTENT))
                {
                    updateFields();
                    reloadWebView();
                } else if(action != null && action.equals(myService.EXIT_INTENT)) {
                    common.context.unregisterReceiver(serviceReceiver);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}