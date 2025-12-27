package com.odiousapps.weewxweather;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

@SuppressWarnings({"unused", "GestureBackNavigation", "deprecation"})
public class WebViewTooOldActivity extends AppCompatActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		new AlertDialog.Builder(this)
				.setTitle("Unsupported WebView")
				.setMessage("Your WebView is too old. Please update your system or device.")
				.setCancelable(false)
				.setPositiveButton("Exit", (dialog, which) -> {
					finishAffinity(); // Finish all activities in this task
				})
				.show();
	}

	// Optional: if user presses back, treat as exit
	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
		finishAffinity();
	}
}
