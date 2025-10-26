package com.odiousapps.weewxweather;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class LessSensitiveViewPagerLayout extends FrameLayout
{
	private float startX, startY;
	private float SWIPE_SLOP = 50f; // Increase to make horizontal swipes harder

	public LessSensitiveViewPagerLayout(@NonNull Context context)
	{
		super(context);
	}

	public LessSensitiveViewPagerLayout(@NonNull Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public boolean onInterceptTouchEvent(MotionEvent ev)
	{
		switch (ev.getActionMasked())
		{
			case MotionEvent.ACTION_DOWN:
				startX = ev.getX();
				startY = ev.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				float dx = Math.abs(ev.getX() - startX);
				float dy = Math.abs(ev.getY() - startY);

				//Common.LogMessage("dx=" + dx + " dy=" + dy + " intercept=" + (dx > SWIPE_SLOP && dx > dy), true);

				// Only intercept if it's a strong horizontal gesture
				if (dx > SWIPE_SLOP && dx > dy)
				{
					// Pass event down to ViewPager2 normally
					return super.onInterceptTouchEvent(ev);
				} else {
					// Let child (e.g. WebView or ScrollView) handle
					return false;
				}
		}

		return super.onInterceptTouchEvent(ev);
	}

	public void setSwipeSlop(float slop)
	{
		SWIPE_SLOP = slop;
	}
}