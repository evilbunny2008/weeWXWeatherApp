package com.odiousapps.weewxweather;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import static java.lang.Math.round;

@SuppressWarnings({"UseCompatLoadingForDrawables", "UnspecifiedRegisterReceiverFlag"})
public class Webcam extends Fragment
{
	private boolean isVisible = false;
	private boolean isRunning = false;
	private RotateLayout rl;
	private ImageView iv;
	private static Bitmap bm;
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
			reloadWebView(true);
		});

		return rootView;
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		isRunning = false;
	}

	void stopRefreshing()
	{
		if(!swipeLayout.isRefreshing())
			return;

		swipeLayout.post(() -> swipeLayout.setRefreshing(false));
	}

	private void reloadWebView(final boolean force)
	{
		Common.LogMessage("reload webcam...");
		Context context = Common.getContext();
		if(context == null)
			return;

		final String webURL = Common.GetStringPref("WEBCAM_URL", "");

		if(webURL == null || webURL.isBlank())
		{
			iv.setImageDrawable(context.getDrawable(R.drawable.nowebcam));
			stopRefreshing();
			return;
		}

		String filename = "webcam.jpg";
		File file = new File(Common.getFilesDir(), filename);
		if(file.exists())
		{
			Common.LogMessage("file: "+ file.getAbsolutePath());
			try
			{
				bm = Common.loadImage(file);
				iv.post(() -> iv.setImageBitmap(bm));
				Common.LogMessage("Finished reading " + filename + " into memory and iv should have updated...");
			} catch(Exception e) {
				Common.doStackOutput(e);
			}
		}

		Thread t = new Thread(() ->
		{
			long period = 0;
			long current_time = round(System.currentTimeMillis() / 1000.0);

			File file1 = new File(Common.getFilesDir(), filename);

			Common.LogMessage("current_time = " + current_time + ", file.lastModified() == " +
			                  round(file1.lastModified() / 1000.0), true);

			if(!force)
			{
				int pos = Common.GetIntPref("updateInterval", 1);
				if(pos <= 0)
				{
					stopRefreshing();
					return;
				}

				long[] ret = Common.getPeriod();
				period = Math.round(ret[0] / 1000.0);
			}

			if(force || !file1.exists() || round(file1.lastModified() / 1000.0) + period < current_time)
			{
				downloadWebcam(webURL, context.getFilesDir());
				Common.LogMessage("Finished downloading from webURL: " + webURL, true);
			}

			Common.LogMessage("Prompt iv to redraw...");

			File newFile = new File(Common.getFilesDir(), filename);
			if(newFile.exists() && newFile.canRead())
			{
				bm = Common.loadImage(newFile);
				if(bm == null)
				{
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

				Common.LogMessage("Finished reading " + filename + " into memory and iv should have updated...");

				stopRefreshing();
			}
		});

		t.start();
	}

	static boolean downloadWebcam(String webURL, File getFiles)
	{
		try
		{
			Common.LogMessage("starting to download bitmap from: " + webURL);
			URL url = new URL(webURL);
			if(webURL.toLowerCase(Locale.ENGLISH).endsWith(".mjpeg") ||
			    webURL.toLowerCase(Locale.ENGLISH).endsWith(".mjpg"))
			{
				MjpegRunner mr = new MjpegRunner(url);
				mr.run();

				try
				{
					Common.LogMessage("trying to set bm");
					bm = mr.getBM();
				} catch(Exception e) {
					Common.doStackOutput(e);
					return false;
				}
			} else {
				InputStream is = url.openStream();
				bm = BitmapFactory.decodeStream(is);
			}

			File file = new File(getFiles, "webcam.jpg");
			try(FileOutputStream out = new FileOutputStream(file))
			{
				bm.compress(Bitmap.CompressFormat.JPEG, 85, out);
				return true;
			} catch(Exception e) {
				Common.doStackOutput(e);
			}
		} catch(Exception e) {
			Common.doStackOutput(e);
		}

		return false;
	}

	public void onResume()
	{
		super.onResume();
		Common.LogMessage("webcam.java -- registerReceiver");

		if(isVisible)
			return;

		isVisible = true;

		Common.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);

		if(isRunning)
			return;

		isRunning = true;

		reloadWebView(false);
	}

	public void onPause()
	{
		super.onPause();
		Common.LogMessage("webcam.java -- unregisterReceiver");

		if(!isVisible)
			return;

		isVisible = false;

		Common.NotificationManager.getNotificationLiveData().removeObserver(notificationObserver);
	}

	private final Observer<String> notificationObserver = str ->
	{
		Common.LogMessage("notificationObserver == " + str);

		if(str.equals(Common.UPDATE_INTENT) || str.equals(Common.REFRESH_INTENT))
			reloadWebView(true);

		if(str.equals(Common.EXIT_INTENT))
			onPause();
	};
}