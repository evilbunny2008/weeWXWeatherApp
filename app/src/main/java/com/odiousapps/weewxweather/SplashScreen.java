package com.odiousapps.weewxweather;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import static java.lang.Thread.sleep;

public class SplashScreen extends AppCompatActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash_screen);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					sleep(500);
					startApp();
				} catch (Exception e) {
					e.printStackTrace();
				}
				finish();
			}
		}).start();
	}

	private void startApp()
	{
		Intent intent = new Intent(SplashScreen.this, MainActivity.class);
		startActivity(intent);
	}
}
