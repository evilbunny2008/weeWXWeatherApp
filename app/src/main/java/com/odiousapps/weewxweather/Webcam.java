package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.URL;

class Webcam
{
    private Common common;
    private ImageView iv;
    private Bitmap bm;

    Webcam(Common common)
    {
        this.common = common;
    }

    View myWebcam(LayoutInflater inflater, ViewGroup container)
    {
        View rootView;
        rootView = inflater.inflate(R.layout.fragment_webcam, container, false);
        iv = rootView.findViewById(R.id.webcam);

        iv.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator != null)
                    vibrator.vibrate(150);
                Common.LogMessage("long press");
                reloadWebView();
                return true;
            }
        });

        reloadWebView();
        return rootView;
    }

    private void reloadWebView()
    {
        Common.LogMessage("reload webcam...");
        final String webURL = common.GetStringPref("WEBCAM_URL", "");

        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Common.LogMessage("starting to download bitmap");
                    InputStream is = new URL(webURL).openStream();
                    bm = BitmapFactory.decodeStream(is);

                    int width = bm.getWidth();
                    int height = bm.getHeight();

                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);

                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bm,width,height,true);
                    bm = Bitmap.createBitmap(scaledBitmap , 0, 0, scaledBitmap .getWidth(), scaledBitmap .getHeight(), matrix, true);

                    Common.LogMessage("done downloading, prompt handler to draw to iv");
                    handlerDone.sendEmptyMessage(0);
                } catch (Exception e) {
                    handlerSettings.sendEmptyMessage(0);
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
            iv.setImageBitmap(bm);
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handlerSettings = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            new AlertDialog.Builder(common.context)
                    .setTitle("Invalid Image")
                    .setMessage("You supplied an image in your settings.txt that is invalid or unsupported.")
                    .setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                        }
                    }).show();
        }
    };
}