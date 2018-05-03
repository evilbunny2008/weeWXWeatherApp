package com.odiousapps.weewxweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainActivity extends AppCompatActivity
{
    private TabLayout tabLayout;
    private Common common;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        common = new Common(this);

        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        tabLayout = findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        try
        {
            if(common.GetStringPref("BASE_URL", "").equals(""))
                switchToTab(5);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(myService.singleton == null)
            startService(new Intent(this, myService.class));

        IntentFilter filter = new IntentFilter();
        filter.addAction(myService.UPDATE_INTENT);
        filter.addAction(myService.TAB0_INTENT);
        registerReceiver(serviceReceiver, filter);
    }

    @Override
    public void onBackPressed()
    {
        if(common.GetBoolPref("bgdl", true))
        {
            moveTaskToBack(true);
        } else {
            finish();
        }
    }

    @Override
    public void onDestroy()
    {
	    super.onDestroy();

	    stopService(new Intent(this, myService.class));
	    unregisterReceiver(serviceReceiver);
	    System.exit(0);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if(myService.singleton != null)
        {
            Common.LogMessage("pausing app updates");
            myService.singleton.doUpdate = false;
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(myService.singleton != null)
        {
            Common.LogMessage("resuming app updates");
            myService.singleton.doUpdate = true;
            myService.singleton.SendIntents();
        }
    }

    private void switchToTab(int tab)
    {
        if(tab < 0)
            tab = 0;
        if(tab > 6)
            tab = 6;

        if(tabLayout.getTabAt(tab) != null)
        {
            try
            {
                //noinspection ConstantConditions
                tabLayout.getTabAt(tab).select();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void getWeather()
    {
        if(myService.singleton != null)
            myService.singleton.getWeather();
    }

    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            try
            {
                Common.LogMessage("We have a hit, so we should probably update the screen.");
                String action = intent.getAction();
                if(action != null && action.equals(myService.TAB0_INTENT))
                {
                    getWeather();

                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            switchToTab(0);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public static class PlaceholderFragment extends Fragment
    {
        private static final String ARG_SECTION_NUMBER = "section_number";
        private int lastPos = 0;
        private Weather weather;
        private Stats stats;
        private Forecast forecast;
        private Webcam webcam;
        private Custom custom;
        private Settings settings;
        private About about;

        public PlaceholderFragment() {}

        public static PlaceholderFragment newInstance(int sectionNumber)
        {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onDestroyView()
        {
            super.onDestroyView();

            switch(lastPos)
            {
                case 1:
                    weather.doStop();
                    break;
                case 2:
                    stats.doStop();
                    break;
                case 3:
                    forecast.doStop();
                    break;
                case 4:
                    webcam.doStop();
                    break;
                case 5:
                    custom.doStop();
                    break;
                case 6:
                    //settings.doStop();
                    break;
                case 7:
                    //about.doStop();
                    break;
            }

            Common.LogMessage("onDestroyView() has been called lastpos ="+lastPos);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
	        Common common = new Common(getContext());

	        lastPos = getArguments().getInt(ARG_SECTION_NUMBER);

		    if(getArguments().getInt(ARG_SECTION_NUMBER) == 1) {
                weather = new Weather(common);
                return weather.myWeather(inflater, container);
            } else if(getArguments().getInt(ARG_SECTION_NUMBER) == 2) {
                stats = new Stats(common);
                return stats.myStats(inflater, container);
            } else if(getArguments().getInt(ARG_SECTION_NUMBER) == 3) {
                forecast = new Forecast(common);
                return forecast.myForecast(inflater, container);
            } else if(getArguments().getInt(ARG_SECTION_NUMBER) == 4) {
                webcam = new Webcam(common);
                return webcam.myWebcam(inflater, container);
            } else if(getArguments().getInt(ARG_SECTION_NUMBER) == 5) {
			    custom = new Custom(common);
			    return custom.myCustom(inflater, container);
		    } else if(getArguments().getInt(ARG_SECTION_NUMBER) == 6) {
				    settings = new Settings(common);
				    return settings.mySettings(inflater, container);
		    } else if(getArguments().getInt(ARG_SECTION_NUMBER) == 7) {
                about = new About();
                return about.myAbout(inflater, container);
            }

            return null;
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount()
        {
            return 7;
        }
    }
}