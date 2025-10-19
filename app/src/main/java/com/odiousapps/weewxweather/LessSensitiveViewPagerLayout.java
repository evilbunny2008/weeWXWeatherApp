package com.odiousapps.weewxweather;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

@SuppressWarnings("FieldCanBeLocal")
public class LessSensitiveViewPagerLayout extends FrameLayout
{
	private float startX, startY;
	private final float SWIPE_SLOP = 50f; // Increase to make horizontal swipes harder

	public LessSensitiveViewPagerLayout(Context context)
	{
		super(context);
	}

	public LessSensitiveViewPagerLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
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
}