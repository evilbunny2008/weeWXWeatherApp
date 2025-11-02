package com.odiousapps.weewxweather;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

@SuppressWarnings("unused")
class myLinearLayout extends LinearLayout
{
	private OnTouchedListener onTouchedListener;

	public interface OnTouchedListener
	{
		void onTouched(View v);
	}

	public void setOnTouchedListener(OnTouchedListener listener)
	{
		onTouchedListener = listener;
	}

	public myLinearLayout(Context context)
	{
		super(context);
	}

	public myLinearLayout(Context context, @Nullable AttributeSet attrs)
	{
		super(context, attrs);
	}

	public myLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev)
	{
		if(onTouchedListener != null)
			onTouchedListener.onTouched(this);

		return false;
	}
}
