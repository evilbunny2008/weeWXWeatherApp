package com.odiousapps.weewxweather;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.InterruptedIOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class Webcam extends Fragment
{
	private Thread webcamThread = null;
	private long wcStart;
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
			swipeLayout.setRefreshing(true);
			Common.LogMessage("onRefresh();");
			loadWebcamImage();
		});

		return rootView;
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		Common.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);

		loadWebcamImage();
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		Common.NotificationManager.getNotificationLiveData().removeObserver(notificationObserver);
	}

	void stopRefreshing()
	{
		if(swipeLayout.isRefreshing())
			swipeLayout.post(() -> swipeLayout.setRefreshing(false));
	}

	private void loadWebcamImage()
	{
		Common.LogMessage("loadWebcamImage...");

		final String webURL = Common.GetStringPref("WEBCAM_URL", "");
		if(webURL == null || webURL.isBlank())
		{
			iv.setImageDrawable(weeWXApp.getAndroidDrawable(R.drawable.nowebcam));
			iv.invalidate();
			stopRefreshing();
			return;
		}

		long current_time = Math.round(System.currentTimeMillis() / 1000.0);

		if(webcamThread != null)
		{
			if(webcamThread.isAlive())
			{
				if(wcStart + 30 > current_time)
					return;

				webcamThread.interrupt();
			}

			webcamThread = null;
		}

		wcStart = current_time;

		webcamThread = new Thread(() ->
		{
			try
			{
				Bitmap bm = Common.loadOrDownloadImage(webURL, "webcam.jpg", true);
				if(bm == null)
				{
					noImageToShow("There was a problem with the webcam URL");
					stopRefreshing();
					wcStart = 0;
					return;
				}

				Common.LogMessage("rl.getAngle()=" + rl.getAngle());
				Common.LogMessage("screenHeightDp=" + weeWXApp.getHeight());
				Common.LogMessage("screenWidthDp=" + weeWXApp.getWidth());
				Common.LogMessage("bm.getWidth()=" + bm.getWidth());
				Common.LogMessage("bm.getHeight()=" + bm.getHeight());

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

				Common.LogMessage("Finished reading webcam.jpg into memory and iv should have updated...");

			} catch(InterruptedIOException e) {
				noImageToShow("Error: " + e);
			} catch(Exception e) {
				noImageToShow("Error: " + e);
				//Common.doStackOutput(e);
			}

			stopRefreshing();
			wcStart = 0;
		});

		webcamThread.start();
	}

	private final Observer<String> notificationObserver = str ->
	{
		Common.LogMessage("notificationObserver == " + str);

		if(str.equals(Common.UPDATE_INTENT) || str.equals(Common.REFRESH_INTENT))
			loadWebcamImage();

		if(str.equals(Common.EXIT_INTENT))
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