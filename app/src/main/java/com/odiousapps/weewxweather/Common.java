package com.odiousapps.weewxweather;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.widget.RemoteViews;

class Common
{
    private final static String PREFS_NAME = "WeeWxWeatherPrefs";
    private final static boolean debug_on = true;
    Context context;
    final static String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36";

    Common(Context c)
    {
        System.setProperty("http.agent", UA);
        this.context = c;
    }

    static void LogMessage(String value)
    {
        LogMessage(value, false);
    }

    static void LogMessage(String value, boolean showAnyway)
    {
        if(debug_on || showAnyway)
            Log.i("WeeWx Weather", "message='" + value + "'");
    }

    void SetStringPref(String name, String value)
    {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(name, value);
        editor.apply();

        LogMessage("Updating '" + name + "'='" + value + "'");
    }

    String GetStringPref(String name, String defval)
    {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        String value;

        try
        {
            value = settings.getString(name, defval);
        } catch (ClassCastException cce) {
            //SetStringPref(name, defval);
            return defval;
        } catch (Exception e) {
            LogMessage("GetStringPref(" + name + ", " + defval + ") Err: " + e.toString());
            return defval;
        }

        LogMessage(name + "'='" + value + "'");

        return value;
    }

    @SuppressWarnings("unused")
    void SetLongPref(String name, long value)
    {
        SetStringPref(name, "" + value);
    }

    @SuppressWarnings("unused")
    long GetLongPref(String name)
    {
        return GetLongPref(name, 0);
    }

    @SuppressWarnings("WeakerAccess")
    long GetLongPref(String name, @SuppressWarnings("SameParameterValue") long defval)
    {
        String val = GetStringPref(name, "" + defval);
        if (val == null)
            return defval;
        return Long.parseLong(val);
    }

    void SetIntPref(String name, int value)
    {
        SetStringPref(name, "" + value);
    }

    @SuppressWarnings("unused")
    int GetIntPref(String name)
    {
        return GetIntPref(name, 0);
    }

    int GetIntPref(String name, int defval)
    {
        String val = GetStringPref(name, "" + defval);
        if (val == null)
            return defval;
        return Integer.parseInt(val);
    }

    void SetBoolPref(String name, boolean value)
    {
        String val = "0";
        if (value)
            val = "1";

        SetStringPref(name, val);
    }

    @SuppressWarnings("unused")
    boolean GetBoolPref(String name)
    {
        return GetBoolPref(name, false);
    }

    boolean GetBoolPref(String name, boolean defval)
    {
        String value = "0";
        if (defval)
            value = "1";

        String val = GetStringPref(name, value);
        return val.equals("1");
    }

    RemoteViews buildUpdate(Context context)
    {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        Bitmap myBitmap = Bitmap.createBitmap(600, 440, Bitmap.Config.ARGB_4444);
        Canvas myCanvas = new Canvas(myBitmap);
        Paint paint = new Paint();
//        Typeface clock = Typeface.createFromAsset(context.getAssets(),"Clockopia.ttf");
        paint.setAntiAlias(true);
        paint.setSubpixelText(true);
        //paint.setTypeface(clock);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.CENTER);

        String bits[] = GetStringPref("LastDownload","").split("\\|");
        if(bits.length >= 110)
        {
            paint.setTextSize(64);
            myCanvas.drawText(bits[56], myCanvas.getWidth() / 2, 80, paint);
            paint.setTextSize(48);
            myCanvas.drawText(bits[55], myCanvas.getWidth() / 2, 140, paint);
            paint.setTextSize(200);
            myCanvas.drawText(bits[0] + bits[60], myCanvas.getWidth() / 2, 310, paint);

            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTextSize(64);
            myCanvas.drawText(bits[25] + bits[61], 20, 400, paint);

            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setTextSize(64);
            myCanvas.drawText(bits[20] + bits[62], myCanvas.getWidth() - 20, 400, paint);
        } else {
            paint.setTextSize(200);
            myCanvas.drawText("Error!", myCanvas.getWidth() / 2, 300, paint);
        }

        views.setImageViewBitmap(R.id.widget, myBitmap);
        return views;
    }
}