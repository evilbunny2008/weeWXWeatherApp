package com.odiousapps.weewxweather;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

class Webcam
{
    private Common common;
    private WebView wv;

    Webcam(Common common)
    {
        this.common = common;
    }

    View myWebcam(LayoutInflater inflater, ViewGroup container)
    {
        View rootView = inflater.inflate(R.layout.fragment_webcam, container, false);
        wv = rootView.findViewById(R.id.webcam);
        reloadWebView();
        return rootView;
    }

    private void reloadWebView()
    {
        Common.LogMessage("reload webcam...");
        wv.getSettings().setAppCacheEnabled(false);
        wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        wv.getSettings().setUserAgentString(Common.UA);
        wv.clearCache(true);
        String webcam = common.GetStringPref("WEBCAM_URL", "");

        if (webcam == null || webcam.equals(""))
            webcam = "http://mx.cafesydney.com:8888/mjpg/video.mjpg";

        String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "  <head>\n" +
                "    <meta charset='utf-8'>\n" +
//                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                "  </head>\n" +
                "  <body style='padding:0px;margin:0px;'>\n" +
                "\t<img style='margin:0px;padding:0px;border:0px;text-align:center;max-width:100%;width:auto;height:auto;'\n" +
                "\tsrc='" + webcam + "?date=" + System.currentTimeMillis() + "'>\n" +
                "  </body>\n" +
                "</html>";
        wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }
}
