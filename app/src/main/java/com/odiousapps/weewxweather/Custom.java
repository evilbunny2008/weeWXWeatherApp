package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Vibrator;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

class Custom
{
    private Common common;
    private WebView wv;
	private SwipeRefreshLayout swipeLayout;

    Custom(Common common)
    {
        this.common = common;
    }

    @SuppressLint("SetJavaScriptEnabled")
    View myCustom(LayoutInflater inflater, ViewGroup container)
    {
        View rootView = inflater.inflate(R.layout.fragment_custom, container, false);
        wv = rootView.findViewById(R.id.custom);
        wv.getSettings().setUserAgentString(Common.UA);
	    wv.getSettings().setJavaScriptEnabled(true);
	    wv.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator != null)
                    vibrator.vibrate(250);
                Common.LogMessage("long press");
                reloadWebView();
                return true;
            }
        });

	    swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
	    swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
	    {
		    @Override
		    public void onRefresh()
		    {
			    swipeLayout.setRefreshing(true);
			    Common.LogMessage("onRefresh();");
			    reloadWebView();
			    swipeLayout.setRefreshing(false);
		    }
	    });

	    wv.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener()
	    {
		    @Override
		    public void onScrollChanged()
		    {
			    if (wv.getScrollY() == 0)
			    {
				    swipeLayout.setEnabled(true);
			    } else {
				    swipeLayout.setEnabled(false);
			    }
		    }
	    });

        wv.setWebViewClient(new WebViewClient()
        {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                return false;
            }
        });

        wv.setOnKeyListener(new View.OnKeyListener()
        {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if(event.getAction() == android.view.KeyEvent.ACTION_DOWN)
                {
                    if((keyCode == android.view.KeyEvent.KEYCODE_BACK))
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
            }
        });

        wv.setWebChromeClient(new WebChromeClient()
	    {
		    @Override
		    public boolean onConsoleMessage(ConsoleMessage cm)
		    {
//		    	Common.LogMessage(cm.message());
			    return true;
		    }
	    });

        reloadWebView();

        IntentFilter filter = new IntentFilter();
        filter.addAction(myService.UPDATE_INTENT);
        filter.addAction(myService.EXIT_INTENT);
        common.context.registerReceiver(serviceReceiver, filter);

        return rootView;
    }

    private void reloadWebView()
    {
        Common.LogMessage("reload custom...");

        String custom = common.GetStringPref("CUSTOM_URL", "");
        String custom_url = common.GetStringPref("custom_url", "");

        if (custom == null || custom.equals(""))
            return;

        if(custom_url != null && !custom_url.equals(""))
        	custom = custom_url;

        wv.loadUrl(custom);
    }

    public void doStop()
    {
        Common.LogMessage("custom.java -- unregisterReceiver");
        try
        {
	        common.context.unregisterReceiver(serviceReceiver);
        } catch (Exception e) {
	        e.printStackTrace();
        }
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
                if(action != null && action.equals(myService.UPDATE_INTENT))
                {
                    reloadWebView();
                } else if(action != null && action.equals(myService.EXIT_INTENT)) {
                    common.context.unregisterReceiver(serviceReceiver);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}