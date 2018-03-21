package com.odiousapps.weewxweather;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.RemoteViews;

public class Common
{
    private final static String PREFS_NAME = "WeeWxWeatherPrefs";
    private final static boolean debug_on = true;
    private Context context;
    public final static String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36";

    public Common(Context c)
    {
        System.setProperty ("http.agent", UA);
        this.context = c;
    }

    public static void LogMessage(String value)
    {
        LogMessage(value, false);
    }

    public static void LogMessage(String value, boolean showAnyway)
    {
        if (debug_on || showAnyway)
            System.out.println("WeeWx Weather: ts=" + System.currentTimeMillis() + ", message='" + value + "'");
    }

    public void SetStringPref(String name, String value)
    {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(name, value);
        editor.apply();

        LogMessage("Updating '" + name + "'='" + value + "'");
    }

    public String GetStringPref(String name, String defval)
    {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        String value = defval;

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

    public void SetLongPref(String name, long value)
    {
        SetStringPref(name, "" + value);
    }

    public long GetLongPref(String name)
    {
        return GetLongPref(name, 0);
    }

    public long GetLongPref(String name, long defval)
    {
        String val = GetStringPref(name, "" + defval);
        if (val == null)
            return defval;
        return Long.parseLong(val);
    }

    public void SetIntPref(String name, int value)
    {
        SetStringPref(name, "" + value);
    }

    public int GetIntPref(String name)
    {
        return GetIntPref(name, 0);
    }

    public int GetIntPref(String name, int defval)
    {
        String val = GetStringPref(name, "" + defval);
        if (val == null)
            return defval;
        return Integer.parseInt(val);
    }

    public void SetBoolPref(String name, boolean value)
    {
        String val = "0";
        if (value)
            val = "1";

        SetStringPref(name, val);
    }

    public boolean GetBoolPref(String name)
    {
        return GetBoolPref(name, false);
    }

    public boolean GetBoolPref(String name, boolean defval)
    {
        String value = "0";
        if (defval)
            value = "1";

        String val = GetStringPref(name, value);
        if (val.equals("1"))
            return true;
        return false;
    }

    public RemoteViews buildUpdate(Context context)
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
            myCanvas.drawText(bits[0] + "\u00B0" + bits[60].substring(bits[60].length() -1), myCanvas.getWidth() / 2, 310, paint);

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