package com.odiousapps.weewxweather;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class Webcam extends Fragment
{
	private RotateLayout rl;
	private ImageView iv;
	private SwipeRefreshLayout swipeLayout;

	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		View rootView = inflater.inflate(R.layout.fragment_webcam, container, false);
		rl = rootView.findViewById(R.id.rotateLayout);
		iv = rootView.findViewById(R.id.webcam);
		swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
		swipeLayout.setBackgroundColor(KeyValue.bgColour);

		swipeLayout.setRefreshing(true);
		swipeLayout.setOnRefreshListener(() ->
		{
			weeWXAppCommon.LogMessage("Webcam.java weeWXAppCommon.getWebcamImage(true, false);", true);
			swipeLayout.setRefreshing(true);
			weeWXAppCommon.getWebcamImage(true, false);
		});

		return rootView;
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		weeWXAppCommon.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);

		loadWebcamImage();
	}

	void stopRefreshing()
	{
		if(swipeLayout.isRefreshing())
			swipeLayout.post(() -> swipeLayout.setRefreshing(false));
	}

	private void showWebcamImage(Bitmap bm)
	{
		if(iv == null)
			return;

		if(bm == null)
		{
			noImageToShow(weeWXApp.getAndroidString(R.string.webcam_still_downloading));
			stopRefreshing();
			return;
		}

		weeWXAppCommon.LogMessage("rl.getAngle()=" + rl.getAngle());
		weeWXAppCommon.LogMessage("screenHeightDp=" + weeWXApp.getHeight());
		weeWXAppCommon.LogMessage("screenWidthDp=" + weeWXApp.getWidth());
		weeWXAppCommon.LogMessage("bm.getWidth()=" + bm.getWidth());
		weeWXAppCommon.LogMessage("bm.getHeight()=" + bm.getHeight());

		if(weeWXApp.getHeight() > weeWXApp.getWidth() && bm.getWidth() > bm.getHeight())
		{
			if(rl.getAngle() != -90)
				rl.post(() -> rl.setAngle(-90));
		} else {
			if(rl.getAngle() != 0)
				rl.post(() -> rl.setAngle(0));
		}

		iv.post(() ->
		{
			iv.setImageBitmap(bm);
			iv.invalidate();
		});

		weeWXAppCommon.LogMessage("Finished reading webcam.jpg into memory and iv should have updated...");
	}

	private void loadWebcamImage()
	{
		weeWXAppCommon.LogMessage("loadWebcamImage...");

		try
		{
			Bitmap bm = weeWXAppCommon.getWebcamImage(false, false);
			if(bm != null)
				showWebcamImage(bm);
			else
				noImageToShow(weeWXApp.getAndroidString(R.string.webcam_still_downloading));
		} catch(Exception e) {
			weeWXAppCommon.LogMessage("Error! e: " + e);
			noImageToShow("Error: " + e);
		}

		stopRefreshing();
	}

	private final Observer<String> notificationObserver = str ->
	{
		weeWXAppCommon.LogMessage("Webcam.java notificationObserver: " + str);

		if(str.equals(weeWXAppCommon.REFRESH_WEBCAM_INTENT))
			loadWebcamImage();

		if(str.equals(weeWXAppCommon.EXIT_INTENT))
			onPause();
	};

	private void noImageToShow(String text)
	{
		Bitmap bm = weeWXApp.textToBitmap(text);
		iv.post(() ->
		{
			iv.setImageBitmap(bm);
			iv.invalidate();
		});
	}
}