package com.odiousapps.weewxweather;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

@SuppressWarnings({"unused", "SameParameterValue", "ApplySharedPref",
		"SameReturnValue", "WeakerAccess", "BooleanMethodIsAlwaysInverted",
		"SetJavaScriptEnabled", "SetTextI18n", "FieldCanBeLocal", "FieldMayBeFinal",
		"CanBeFinal"})
class MaxWidthLinearLayout extends LinearLayout
{
	private int maxWidth = Integer.MAX_VALUE;

	public MaxWidthLinearLayout(Context context)
	{
		super(context);
	}

	public MaxWidthLinearLayout(Context context, @Nullable AttributeSet attrs)
	{
		super(context, attrs);
	}

	public MaxWidthLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int width = MeasureSpec.getSize(widthMeasureSpec);
		if(width > maxWidth)
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST);

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
