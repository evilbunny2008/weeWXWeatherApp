package com.odiousapps.weewxweather;

import androidx.lifecycle.ViewModel;

public class CommonViewModel extends ViewModel
{
	private Common common;

	public Common getCommon()
	{
		return this.common;
	}

	public void setCommon(Common common)
	{
		this.common = common;
	}
}