package com.odiousapps.weewxweather;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.net.Uri;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ScrollView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class GifImageView extends View
{
    private InputStream mInputStream;
    private Movie mMovie;
    private int mWidth, mHeight;
    private long mStart;
    private Context mContext;
    private float scale;

    public GifImageView(Context context)
    {
        super(context);
        this.mContext = context;
    }

    public GifImageView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public GifImageView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        if(attrs.getAttributeName(1).equals("background"))
        {
            int id = Integer.parseInt(attrs.getAttributeValue(1).substring(1));
            setGifImageResource(id);
        }
    }

    private void init()
    {
        setFocusable(false);
        mMovie = Movie.decodeStream(mInputStream);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float width = displayMetrics.widthPixels;
        float height = displayMetrics.heightPixels;

        scale =  width / (float)mMovie.width() * 0.95f;

        Common.LogMessage("scale="+scale+",mMovie.width="+mMovie.width()+",width="+width);
        Common.LogMessage("scale="+scale+",mMovie.height="+mMovie.height()+",height="+height);

        mWidth = (int)Math.round(mMovie.width() * scale);
        mHeight = (int)Math.round(mMovie.height() * scale);

        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        long now = SystemClock.uptimeMillis();

        if (mStart == 0)
            mStart = now;

        if (mMovie != null)
        {
            int duration = mMovie.duration();
            if (duration == 0)
                duration = 1000;

            int relTime = (int) ((now - mStart) % duration);

            canvas.drawColor(Color.TRANSPARENT);
            canvas.scale((float)scale, (float)scale);

            mMovie.setTime(relTime);
            mMovie.draw(canvas, 0, 0);
            this.invalidate();
        }
    }

    public void setGifBitmap(String filename)
    {
        File file = new File(filename);
        try
        {
            mInputStream = new FileInputStream(file);
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setGifImageResource(int id)
    {
        mInputStream = mContext.getResources().openRawResource(id);
        init();
    }

    public void setGifImageUri(Uri uri)
    {
        try
        {
            mInputStream = mContext.getContentResolver().openInputStream(uri);
            init();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}