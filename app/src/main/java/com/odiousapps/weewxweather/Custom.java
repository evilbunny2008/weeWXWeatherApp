package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class Custom extends Fragment
{
    private final Common common;
    private WebView wv;
	private SwipeRefreshLayout swipeLayout;

    Custom(Common common)
    {
        this.common = common;
    }

	@SuppressLint("SetJavaScriptEnabled")
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.fragment_custom, container, false);
		wv = rootView.findViewById(R.id.custom);
		wv.getSettings().setUserAgentString(Common.UA);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.setOnLongClickListener(v ->
		{
			Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
			if(vibrator != null)
				vibrator.vibrate(250);
			Common.LogMessage("long press");
			reloadWebView();
			return true;
		});

	    WebSettings settings = wv.getSettings();
	    settings.setDomStorageEnabled(true);

	    swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
	    swipeLayout.setOnRefreshListener(() ->
	    {
		    swipeLayout.setRefreshing(true);
		    Common.LogMessage("onRefresh();");
		    reloadWebView();
		    swipeLayout.setRefreshing(false);
	    });

	    wv.getViewTreeObserver().addOnScrollChangedListener(() -> swipeLayout.setEnabled(wv.getScrollY() == 0));

        wv.setWebViewClient(new WebViewClient()
        {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                return false;
            }
        });

        wv.setOnKeyListener((v, keyCode, event) ->
        {
            if(event.getAction() == KeyEvent.ACTION_DOWN)
            {
                if((keyCode == KeyEvent.KEYCODE_BACK))
                {
                    if(wv != null)
                    {
                        if(wv.canGoBack())
                        {
                            wv.goBack();
                            return true;
                        }
                    }
                }
            }

            return false;
        });

        wv.setWebChromeClient(new WebChromeClient()
	    {
		    @Override
		    public boolean onConsoleMessage(ConsoleMessage cm)
		    {
				Common.LogMessage("My Application: " + cm.message());
				return super.onConsoleMessage(cm);
		    }
	    });

        reloadWebView();
        return rootView;
    }

    private void reloadWebView()
    {
        Common.LogMessage("reload custom...");

        String custom = common.GetStringPref("CUSTOM_URL", "");
        String custom_url = common.GetStringPref("custom_url", "");

        if ((custom == null || custom.equals("")) && (custom_url == null || custom_url.equals("")))
            return;

        if(custom_url != null && !custom_url.equals(""))
        	custom = custom_url;

        wv.loadUrl(custom);
    }

    public void onResume()
    {
	    super.onResume();
	    reloadWebView();
	    IntentFilter filter = new IntentFilter();
	    filter.addAction(Common.UPDATE_INTENT);
	    filter.addAction(Common.REFRESH_INTENT);
	    filter.addAction(Common.EXIT_INTENT);
	    common.context.registerReceiver(serviceReceiver, filter);
	    Common.LogMessage("custom.java -- registerReceiver");
    }

	public void onPause()
	{
		super.onPause();
		try
		{
			common.context.unregisterReceiver(serviceReceiver);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		Common.LogMessage("custom.java -- unregisterReceiver");
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
                    reloadWebView();
                else if(action != null && action.equals(Common.EXIT_INTENT))
                    onPause();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}