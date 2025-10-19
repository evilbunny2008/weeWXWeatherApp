package com.odiousapps.weewxweather;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static java.lang.Math.round;

@SuppressWarnings({"UseCompatLoadingForDrawables", "UnspecifiedRegisterReceiverFlag"})
public class Webcam extends Fragment
{
	private Common common;
	private ImageView iv;
	private static Bitmap bm;
	private SwipeRefreshLayout swipeLayout;

	public Webcam()
	{
	}

	Webcam(Common common)
	{
		this.common = common;
	}

	public static Webcam newInstance(Common common)
	{
		return new Webcam(common);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		CommonViewModel commonViewModel = new ViewModelProvider(this).get(CommonViewModel.class);

		if(commonViewModel.getCommon() == null)
		{
			commonViewModel.setCommon(common);
		} else {
			common = commonViewModel.getCommon();
			common.reload(common.context);
		}

		View rootView = inflater.inflate(R.layout.fragment_webcam, container, false);
		iv = rootView.findViewById(R.id.webcam);
		swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
		swipeLayout.setOnRefreshListener(() ->
		{
			swipeLayout.setRefreshing(true);
			Common.LogMessage("onRefresh();");
			reloadWebView(true);
		});

		reloadWebView(false);
		return rootView;
	}

	private void reloadWebView(final boolean force)
	{
		Common.LogMessage("reload webcam...");
		final String webURL = common.GetStringPref("WEBCAM_URL", "");

		if(webURL.isEmpty())
		{
			iv.setImageDrawable(common.context.getApplicationContext().getDrawable(R.drawable.nowebcam));
			return;
		}

		File file = new File(common.context.getFilesDir(), "webcam.jpg");
		if(file.exists())
		{
			Common.LogMessage("file: "+ file);
			try
			{
				BitmapFactory.Options options = new BitmapFactory.Options();
				bm = BitmapFactory.decodeFile(file.toString(), options);
				iv.setImageBitmap(bm);
			} catch (Exception e) {
				Common.doStackOutput(e);
			}
		}

		Thread t = new Thread(() ->
		{
			long period = 0;
			long current_time = round(System.currentTimeMillis() / 1000.0);

			File file1 = new File(common.context.getFilesDir(), "webcam.jpg");

			Common.LogMessage("current_time = " + current_time + ", file.lastModified() == " + round(file1.lastModified() / 1000.0));

			if(!force)
			{
				int pos = common.GetIntPref("updateInterval", 1);
				if (pos <= 0)
					return;

				long[] ret = common.getPeriod();
				period = Math.round(ret[0] / 1000.0);
			}

			if(force || !file1.exists() || round(file1.lastModified() / 1000.0) + period < current_time)
			{
				if(downloadWebcam(webURL, common.context.getFilesDir()))
					Common.LogMessage("done downloading, prompt handler to draw to iv");
				else
					Common.LogMessage("Skipped downloading");
			}

			iv.post(() ->
			{
				iv.setImageBitmap(bm);
				iv.invalidate();
				swipeLayout.setRefreshing(false);
			});
		});

		t.start();
	}

	static boolean downloadWebcam(String webURL, File getFiles)
	{
		try
		{
			Common.LogMessage("starting to download bitmap from: " + webURL);
			URL url = new URL(webURL);
			if (webURL.toLowerCase(Locale.ENGLISH).endsWith(".mjpeg") || webURL.toLowerCase(Locale.ENGLISH).endsWith(".mjpg"))
			{
				MjpegRunner mr = new MjpegRunner(url);
				mr.run();

				try
				{
					Common.LogMessage("trying to set bm");
					bm = mr.bm;
				} catch (Exception e) {
					Common.doStackOutput(e);
					return false;
				}
			} else {
				InputStream is = url.openStream();
				bm = BitmapFactory.decodeStream(is);
			}

			int width = bm.getWidth();
			int height = bm.getHeight();

			Matrix matrix = new Matrix();
			matrix.postRotate(90);

			Bitmap scaledBitmap = Bitmap.createScaledBitmap(bm, width, height, true);
			bm = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

			//FileOutputStream out = null;
			File file = new File(getFiles, "webcam.jpg");

			try(FileOutputStream out = new FileOutputStream(file))
			{
				bm.compress(Bitmap.CompressFormat.JPEG, 85, out); // bmp is your Bitmap instance
				return true;
			} catch (Exception e) {
				Common.doStackOutput(e);
			}
		} catch (Exception e) {
			Common.doStackOutput(e);
		}

		return false;
	}

	public void onResume()
	{
		super.onResume();
		Common.NotificationManager.getNotificationLiveData().observe(getViewLifecycleOwner(), notificationObserver);
		Common.LogMessage("webcam.java -- registerReceiver");
	}

	public void onPause()
	{
		super.onPause();
		Common.NotificationManager.getNotificationLiveData().removeObserver(notificationObserver);
		Common.LogMessage("webcam.java -- unregisterReceiver");
	}

	private final Observer<String> notificationObserver = str ->
	{
		Common.LogMessage("notificationObserver == " + str);

		if (str.equals(Common.UPDATE_INTENT) || str.equals(Common.REFRESH_INTENT))
			reloadWebView(true);

		if(str.equals(Common.EXIT_INTENT))
			onPause();
	};
}