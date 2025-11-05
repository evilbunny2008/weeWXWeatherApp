package com.odiousapps.weewxweather;

import android.content.Context;
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
	private boolean isVisible = false;
	private Thread t = null;
	private long tStart;
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
			loadWebcamImage(true);
		});

		Common.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);

		return rootView;
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

	private void loadWebcamImage(final boolean forceDownload)
	{
		Common.LogMessage("loadWebcamImage...");

		Context context = weeWXApp.getInstance();
		if(context == null)
			return;

		final String webURL = Common.GetStringPref("WEBCAM_URL", "");
		if(webURL == null || webURL.isBlank())
		{
			iv.setImageDrawable(weeWXApp.getAndroidDrawable(R.drawable.nowebcam));
			iv.invalidate();
			stopRefreshing();
			return;
		}

		long current_time = Math.round(System.currentTimeMillis() / 1000.0);

		if(t != null)
		{
			if(tStart + 30 > current_time)
				return;

			tStart = current_time;

			if(t.isAlive())
				t.interrupt();

			t = null;
		}

		t = new Thread(() ->
		{
			try
			{
				Bitmap bm = Common.loadOrDownloadImage(webURL, "webcam.jpg", forceDownload);

				if(bm == null)
				{
					iv.post(() ->
					{
						iv.setImageDrawable(weeWXApp.getAndroidDrawable(R.drawable.nowebcam));
						iv.invalidate();
					});

					stopRefreshing();

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

			} catch(InterruptedIOException ignored) {
			} catch(Exception e) {
				Common.doStackOutput(e);
			}

			stopRefreshing();
		});

		t.start();
	}

	public void onResume()
	{
		super.onResume();
		Common.LogMessage("webcam.java -- registerReceiver");

		if(isVisible)
			return;

		isVisible = true;

		loadWebcamImage(false);
	}

	public void onPause()
	{
		super.onPause();
		Common.LogMessage("webcam.java -- unregisterReceiver");

		if(!isVisible)
			return;

		isVisible = false;
	}

	private final Observer<String> notificationObserver = str ->
	{
		Common.LogMessage("notificationObserver == " + str);

		if(str.equals(Common.UPDATE_INTENT) || str.equals(Common.REFRESH_INTENT))
			loadWebcamImage(true);

		if(str.equals(Common.EXIT_INTENT))
			onPause();
	};
}