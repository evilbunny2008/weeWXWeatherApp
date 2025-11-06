package com.odiousapps.weewxweather;

import android.app.Application;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.LocaleList;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;

@SuppressWarnings({"unused"})
public class weeWXApp extends Application
{
	private static weeWXApp instance;

	@Override
	public void onCreate()
	{
		super.onCreate();
		instance = this;
		applyTheme();
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);

		if((newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) != 0)
		{
			Common.LogMessage("newConfig.uiMode changed, update the theme and mode!");
			Common.reload();
			applyTheme();
			WebViewPreloader.getInstance().init(this, 6);
		}
	}

	void applyTheme()
	{
		//if(DynamicColors.isDynamicColorAvailable())
		//	DynamicColors.applyToActivitiesIfAvailable(this);

		Common.getDayNightMode();

		if(AppCompatDelegate.getDefaultNightMode() != KeyValue.mode)
			AppCompatDelegate.setDefaultNightMode(KeyValue.mode);

		setTheme(KeyValue.theme);

		Common.LogMessage("DayNightMode == " + AppCompatDelegate.getDefaultNightMode());

		Common.SendIntents();
		Common.LogMessage("Theme should have updated!");
	}

	public static weeWXApp getInstance()
	{
		return instance;
	}

	static int getHeight()
	{
		return instance.getResources().getConfiguration().screenHeightDp;
	}

	static int getWidth()
	{
		return instance.getResources().getConfiguration().screenWidthDp;
	}

	static boolean isTablet()
	{
		return instance.getResources().getConfiguration().smallestScreenWidthDp >= 600;
	}

	static LocaleList getLocales()
	{
		return instance.getResources().getConfiguration().getLocales();
	}

	static InputStream openRawResource(int resId)
	{
		return instance.getResources().openRawResource(resId);
	}

	static String getAndroidString(int resId)
	{
		return instance.getString(resId);
	}

	static int smallestScreenWidth()
	{
		return instance.getResources().getConfiguration().smallestScreenWidthDp;
	}

	static int getUImode()
	{
		return instance.getResources().getConfiguration().uiMode;
	}

	static float getDensity()
	{
		return instance.getResources().getDisplayMetrics().density;
	}

	static int getColour(int resId)
	{
		return instance.getColor(resId);
	}

	static Drawable getAndroidDrawable(int resId)
	{
		return AppCompatResources.getDrawable(instance, resId);
	}

	private static StaticLayout newStaticLayout(String text, TextPaint paint, int width)
	{
		return StaticLayout.Builder
				.obtain(text, 0, text.length(), paint, width)
				.setAlignment(Layout.Alignment.ALIGN_CENTER)
				.setLineSpacing(0f, 1f)
				.setIncludePad(true)
				.build();
	}

	public static Bitmap textToBitmap(String text)
	{
		// 1️⃣ Load the reference drawable
		Drawable drawable = getAndroidDrawable(R.drawable.nowebcam);
		if(drawable == null)
			return null;

		int width = drawable.getIntrinsicWidth();
		int height = drawable.getIntrinsicHeight();

		// 2️⃣ Create a new bitmap with same size
		Bitmap bitmap;

		if(text != null && !text.isBlank())
		{
			float textSize = height * 0.8f;

			bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawColor(KeyValue.bgColour);

			// 3️⃣ Prepare the paint
			TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
			paint.setColor(KeyValue.fgColour);
			paint.setTextAlign(Paint.Align.LEFT);
			paint.setTextSize(textSize);

			StaticLayout staticLayout = newStaticLayout(text, paint, width);

			while(textSize > 5f)
			{
				paint.setTextSize(textSize);

				staticLayout = newStaticLayout(text, paint, width);

				if(staticLayout.getHeight() <= height)
					break;

				textSize -= 1f;
			}

			float textY = (height - staticLayout.getHeight()) / 2f;

			// 5️⃣ Draw the text centered
			canvas.save();
			canvas.translate(0, textY);
			staticLayout.draw(canvas);
			canvas.restore();
		} else {
			bitmap = ((BitmapDrawable)drawable).getBitmap();
		}

		return bitmap;
	}
}