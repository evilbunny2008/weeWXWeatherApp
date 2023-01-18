package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static java.lang.Math.round;

public class Webcam extends Fragment
{
	private final Common common;
	private ImageView iv;
	private static Bitmap bm;
	private SwipeRefreshLayout swipeLayout;
	private int dark_theme;

	Webcam(Common common)
	{
		this.common = common;
		dark_theme = common.GetIntPref("dark_theme", 2);
		if(dark_theme == 2)
			dark_theme = common.getSystemTheme();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.fragment_webcam, container, false);
		iv = rootView.findViewById(R.id.webcam);

		if(dark_theme == 1)
			iv.setBackgroundColor(0xff000000);

		iv.setOnLongClickListener(v ->
		{
			Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
			if(vibrator != null)
				vibrator.vibrate(250);
			Common.LogMessage("long press");
			reloadWebView(true);
			return true;
		});

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

	@SuppressLint("UseCompatLoadingForDrawables")
	private void reloadWebView(final boolean force)
	{
		Common.LogMessage("reload webcam...");
		final String webURL = common.GetStringPref("WEBCAM_URL", "");

		if(webURL.equals(""))
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
				e.printStackTrace();
			}
		}

		Thread t = new Thread(() ->
		{
			long period = 0;
			long curtime = round(System.currentTimeMillis() / 1000.0);

			File file1 = new File(common.context.getFilesDir(), "webcam.jpg");

			Common.LogMessage("curtime = " + curtime + ", file.lastModified() == " + round(file1.lastModified() / 1000.0));

			if(!force)
			{
				int pos = common.GetIntPref("updateInterval", 1);
				if (pos <= 0)
					return;

				long[] ret = common.getPeriod();
				period = Math.round(ret[0] / 1000.0);
			}

			if(force || !file1.exists() || round(file1.lastModified() / 1000.0) + period < curtime)
			{
				if(downloadWebcam(webURL, common.context.getFilesDir()))
					Common.LogMessage("done downloading, prompt handler to draw to iv");
				else
					Common.LogMessage("Skipped downloading");
			}

			Handler mHandler1 = new Handler(Looper.getMainLooper());
			mHandler1.post(() ->
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
			if (webURL.toLowerCase().endsWith(".mjpeg") || webURL.toLowerCase().endsWith(".mjpg"))
			{
				MjpegRunner mr = new MjpegRunner(url);
				mr.run();

				try
				{
					while (mr.bm == null)
						Thread.sleep(1000);

					Common.LogMessage("trying to set bm");
					bm = mr.bm;
				} catch (Exception e) {
					e.printStackTrace();
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

			FileOutputStream out = null;
			File file = new File(getFiles, "webcam.jpg");

			try
			{
				out = new FileOutputStream(file);
				bm.compress(Bitmap.CompressFormat.JPEG, 85, out); // bmp is your Bitmap instance
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			} finally {
				try
				{
					if (out != null)
						out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public void onResume()
	{
		super.onResume();
		reloadWebView(false);
		IntentFilter filter = new IntentFilter();
		filter.addAction(Common.UPDATE_INTENT);
		filter.addAction(Common.REFRESH_INTENT);
		filter.addAction(Common.EXIT_INTENT);
		common.context.registerReceiver(serviceReceiver, filter);
		Common.LogMessage("webcam.java -- registerReceiver");
	}

	public void onPause()
	{
		super.onPause();
		try
		{
			common.context.unregisterReceiver(serviceReceiver);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Common.LogMessage("webcam.java -- unregisterReceiver");
	}

	private final BroadcastReceiver serviceReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			try
			{
				Common.LogMessage("Weather() We have a hit, so we should probably update the screen.");
				String action = intent.getAction();
				if(action != null && (action.equals(Common.UPDATE_INTENT) || action.equals(Common.REFRESH_INTENT)))
				{
					dark_theme = common.GetIntPref("dark_theme", 2);
					if(dark_theme == 2)
						dark_theme = common.getSystemTheme();
					if(dark_theme == 1)
						iv.setBackgroundColor(0xff000000);
					else
						iv.setBackgroundColor(0xffffffff);
					reloadWebView(false);
				} else if(action != null && action.equals(Common.EXIT_INTENT))
					onPause();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
}