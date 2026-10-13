package com.odiousapps.weewxweather;

import android.graphics.Bitmap;
import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static com.odiousapps.weewxweather.MainActivity.WEBCAM_IMAGE_ZOOM;
import static com.odiousapps.weewxweather.weeWXApp.getAndroidString;
import static com.odiousapps.weewxweather.weeWXApp.getColours;
import static com.odiousapps.weewxweather.weeWXApp.getHeight;
import static com.odiousapps.weewxweather.weeWXApp.getWidth;
import static com.odiousapps.weewxweather.weeWXApp.textToBitmap;
import static com.odiousapps.weewxweather.weeWXApp.webcamFilename;
import static com.odiousapps.weewxweather.weeWXApp.webcamInterval_default;
import static com.odiousapps.weewxweather.weeWXApp.webcamRefreshOptions;
import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;
import static com.odiousapps.weewxweather.weeWXAppCommon.getImage;
import static com.odiousapps.weewxweather.weeWXAppCommon.processUpdateInBG;
import static com.odiousapps.weewxweather.weeWXNotificationManager.observeNotifications;
import static com.odiousapps.weewxweather.weeWXNotificationManager.removeNotificationObserver;

public class Webcam extends Fragment
{
	private RotateLayout rl;
	private ImageView iv;
	private TextView snapshotTime;
	private SwipeRefreshLayout swipeLayout;
	private int updateInterval = 0;
	private float zoomScale = 1f;
	private float lastTouchX, lastTouchY;
	private boolean dragging;
	private ScaleGestureDetector scaleDetector;
	private GestureDetector tapDetector;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Runnable updateRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			processUpdateInBG(false, false, false, false, true);
			LogMessage("Webcam.java processUpdateInBG(true, false, false, true, " +
					"false, false, false, true);");

			if(updateInterval > 0)
				handler.postDelayed(this, updateInterval);
		}
	};

	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		View rootView = inflater.inflate(R.layout.fragment_webcam, container, false);
		rl = rootView.findViewById(R.id.rotateLayout);
		iv = rootView.findViewById(R.id.webcam);
		snapshotTime = rootView.findViewById(R.id.webcamSnapshotTime);
		swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
		swipeLayout.setBackgroundColor(getColours().bgColour);
		if((boolean)KeyValue.readVar(WEBCAM_IMAGE_ZOOM, false))
			configureZoom();

		swipeLayout.setRefreshing(true);
		swipeLayout.setOnRefreshListener(() ->
		{
			swipeLayout.setRefreshing(true);
			processUpdateInBG(false, false, false, false, true);
		});

//		processUpdateInBG(true, false, false, true,
//				false, false, false, true);
//		LogMessage("Webcam.java processUpdateInBG(true, false, false, true, " +
//				"false, false, false, true);");

		loadWebcamImage();

		observeNotifications(getViewLifecycleOwner(), notificationObserver);

		return rootView;
	}

	@SuppressWarnings("ClickableViewAccessibility")
	private void configureZoom()
	{
		scaleDetector = new ScaleGestureDetector(requireContext(),
				new ScaleGestureDetector.SimpleOnScaleGestureListener()
				{
					@Override
					public boolean onScale(ScaleGestureDetector detector)
					{
						zoomScale = Math.max(1f, Math.min(5f, zoomScale * detector.getScaleFactor()));
						applyZoom();
						return true;
					}
				});
		tapDetector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener()
		{
			@Override
			public boolean onDoubleTap(MotionEvent event)
			{
				zoomScale = 1f;
				iv.setTranslationX(0f);
				iv.setTranslationY(0f);
				applyZoom();
				return true;
			}
		});
		iv.setOnTouchListener((view, event) ->
		{
			scaleDetector.onTouchEvent(event);
			boolean tapped = tapDetector.onTouchEvent(event);
			int action = event.getActionMasked();
			if(action == MotionEvent.ACTION_DOWN)
			{
				view.getParent().requestDisallowInterceptTouchEvent(true);
				lastTouchX = event.getX();
				lastTouchY = event.getY();
				dragging = true;
			}
			if(action == MotionEvent.ACTION_MOVE && event.getPointerCount() == 1 &&
					zoomScale > 1f && dragging && !scaleDetector.isInProgress())
			{
				float limitX = (zoomScale - 1f) * iv.getWidth() / 2f;
				float limitY = (zoomScale - 1f) * iv.getHeight() / 2f;
				iv.setTranslationX(Math.max(-limitX, Math.min(limitX,
						iv.getTranslationX() + event.getX() - lastTouchX)));
				iv.setTranslationY(Math.max(-limitY, Math.min(limitY,
						iv.getTranslationY() + event.getY() - lastTouchY)));
			}
			if(action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_POINTER_DOWN)
				 dragging = false;
			if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
				dragging = false;
			lastTouchX = event.getX();
			lastTouchY = event.getY();
			boolean zoomed = zoomScale > 1f || event.getPointerCount() > 1 || scaleDetector.isInProgress();
			swipeLayout.setEnabled(!zoomed);
			if(zoomed)
				view.getParent().requestDisallowInterceptTouchEvent(true);
			if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
				view.getParent().requestDisallowInterceptTouchEvent(false);
			return true;
		});
	}

	private void applyZoom()
	{
		iv.setScaleX(zoomScale);
		iv.setScaleY(zoomScale);
		if(zoomScale == 1f)
		{
			iv.setTranslationX(0f);
			iv.setTranslationY(0f);
		}
		swipeLayout.setEnabled(zoomScale == 1f);
	}

	@Override
	public void onDestroyView()
	{
		LogMessage("Webcam.onDestroyView()");
		super.onDestroyView();

		removeNotificationObserver(notificationObserver);
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
		handler.removeCallbacks(updateRunnable);
		updateInterval = 0;
		if(!KeyValue.isPrefSet("webcamInterval"))
			return;

		int webcamRefreshInterval = (int)KeyValue.readVar("webcamInterval", webcamInterval_default);
		if(webcamRefreshInterval < 0 || webcamRefreshInterval >= webcamRefreshOptions.length)
			webcamRefreshInterval = webcamInterval_default;

		switch(webcamRefreshInterval)
		{
			case 0 -> updateInterval = 0;
			case 1 -> updateInterval = 10_000;
			case 2 -> updateInterval = 30_000;
			case 3 -> updateInterval = 60_000;
			default -> updateInterval = 300_000;
		}

		if(updateInterval > 0)
			handler.postDelayed(updateRunnable, updateInterval);
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
		LogMessage("screenHeightDp=" + getHeight());
		LogMessage("screenWidthDp=" + getWidth());
		LogMessage("bm.getWidth()=" + bm.getWidth());
		LogMessage("bm.getHeight()=" + bm.getHeight());

		if(getHeight() > getWidth() && bm.getWidth() > bm.getHeight())
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
			updateSnapshotTime();
			stopRefreshing();
		});

		LogMessage("Finished reading webcam.jpg into memory and iv should have updated...");
	}

	// This is the local snapshot file timestamp, not necessarily the camera capture time.
	private void updateSnapshotTime()
	{
		if(snapshotTime == null)
			return;
		File file = new File(weeWXAppCommon.getDataDir(), webcamFilename);
		if(file.exists() && file.lastModified() > 0)
		{
			String time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
					.format(new Date(file.lastModified()));
			snapshotTime.setText(getString(R.string.webcam_snapshot_saved_at, time));
		} else {
			snapshotTime.setText("");
		}
	}

	private void loadWebcamImage()
	{
		LogMessage("loadWebcamImage...");

		try
		{
			Bitmap bm = getImage(webcamFilename);
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