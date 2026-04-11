package com.odiousapps.weewxweather;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static com.odiousapps.weewxweather.weeWXApp.getAndroidString;
import static com.odiousapps.weewxweather.weeWXApp.textToBitmap;
import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;
import static com.odiousapps.weewxweather.weeWXAppCommon.getWebcamImage;
import static com.odiousapps.weewxweather.weeWXAppCommon.processUpdateInBG;

public class Webcam extends Fragment
{
	private RotateLayout rl;
	private ImageView iv;
	private SwipeRefreshLayout swipeLayout;
	private int updateInterval = 0;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Runnable updateRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			processUpdateInBG(true, false, false, true,
					false, false, false, true);

			if(updateInterval > 0)
				handler.postDelayed(this, updateInterval);
		}
	};

	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		View rootView = inflater.inflate(R.layout.fragment_webcam, container, false);
		rl = rootView.findViewById(R.id.rotateLayout);
		iv = rootView.findViewById(R.id.webcam);
		swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
		swipeLayout.setBackgroundColor(weeWXApp.getColours().bgColour);

		swipeLayout.setRefreshing(true);
		swipeLayout.setOnRefreshListener(() ->
		{
			LogMessage("Webcam.java weeWXAppCommon.getWebcamImage(true, false);");
			swipeLayout.setRefreshing(true);
			processUpdateInBG(true, false, false, true,
					false, false, false, true);

		});

		weeWXAppCommon.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);

		return rootView;
	}

	public void onResume()
	{
		super.onResume();
		setLoopInterval();
	}

	public void onPause()
	{
		super.onPause();
		handler.removeCallbacks(updateRunnable);
	}

	void setLoopInterval()
	{
		if(!KeyValue.isPrefSet("webcamInterval"))
			return;

		int webcamRefreshInterval = (int)KeyValue.readVar("webcamInterval", weeWXApp.webcamInterval_default);
		if(webcamRefreshInterval < 0 || webcamRefreshInterval >= weeWXApp.webcamRefreshOptions.length)
			webcamRefreshInterval = weeWXApp.webcamInterval_default;

		switch(webcamRefreshInterval)
		{
			case 0 -> updateInterval = 0;
			case 1 -> updateInterval = 10_000;
			case 2 -> updateInterval = 30_000;
			case 3 -> updateInterval = 60_000;
			default -> updateInterval = 300_000;
		}

		if(updateInterval > 0)
			handler.post(updateRunnable);

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
			noImageToShow(getAndroidString(R.string.webcam_still_downloading));
			stopRefreshing();
			return;
		}

		LogMessage("rl.getAngle()=" + rl.getAngle());
		LogMessage("screenHeightDp=" + weeWXApp.getHeight());
		LogMessage("screenWidthDp=" + weeWXApp.getWidth());
		LogMessage("bm.getWidth()=" + bm.getWidth());
		LogMessage("bm.getHeight()=" + bm.getHeight());

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
			stopRefreshing();
		});

		LogMessage("Finished reading webcam.jpg into memory and iv should have updated...");
	}

	private void loadWebcamImage()
	{
		LogMessage("loadWebcamImage...");

		try
		{
			Bitmap bm = getWebcamImage(false, false, true, false);
			if(bm != null)
				showWebcamImage(bm);
			else
				noImageToShow(getAndroidString(R.string.webcam_still_downloading));
		} catch(Exception e) {
			LogMessage("loadWebcamImage() Error! e: " + e, true, KeyValue.e);
			noImageToShow("Error: " + e);
		}

		stopRefreshing();
	}

	private final Observer<String> notificationObserver = str ->
	{
		LogMessage("Webcam.notificationObserver notificationObserver: " + str);

		if(str.equals(weeWXAppCommon.REFRESH_WEBCAM_INTENT))
			loadWebcamImage();

		if(str.equals(weeWXAppCommon.STOP_WEBCAM_INTENT))
			stopRefreshing();

		if(str.equals(weeWXAppCommon.EXIT_INTENT))
			onPause();
	};

	private void noImageToShow(String text)
	{
		Bitmap bm = textToBitmap(text);
		iv.post(() ->
		{
			iv.setImageBitmap(bm);
			iv.invalidate();
		});
	}
}