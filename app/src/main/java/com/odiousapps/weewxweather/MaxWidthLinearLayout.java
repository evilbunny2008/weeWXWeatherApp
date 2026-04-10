package com.odiousapps.weewxweather;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

@SuppressWarnings("FieldMayBeFinal")
public class MaxWidthLinearLayout extends LinearLayout
{
	private int maxWidth = Integer.MAX_VALUE;

	public MaxWidthLinearLayout(Context context)
	{
		super(context);
	}

	public MaxWidthLinearLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public MaxWidthLinearLayout(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		if(isInEditMode())
		{
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			return;
		}

		int width = MeasureSpec.getSize(widthMeasureSpec);
		if(width > maxWidth)
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST);

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}