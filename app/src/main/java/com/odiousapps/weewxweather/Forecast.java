package com.odiousapps.weewxweather;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

import static java.lang.Math.round;

public class Forecast extends AppCompatActivity
{
    Common common = null;
    WebView wv = null;
    int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forecast);

        if(getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        common = new Common(this);
        Common.LogMessage("set things in motion!");

        wv = findViewById(R.id.webView1);

        View v = findViewById(R.id.wholeScreen);
        //noinspection AndroidLintClickableViewAccessibility
        v.setOnTouchListener(new OnSwipeTouchListener(this)
        {
            @Override
            public void onSwipeRight()
            {
                Common.LogMessage("Swipe Right");
                finish();
            }

            @Override
            public void onSwipeLeft()
            {
                Common.LogMessage("Swipe Left");
                startActivity(new Intent(getBaseContext(), Webcam.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        wv = findViewById(R.id.webView1);
        //noinspection AndroidLintClickableViewAccessibility
        wv.setOnTouchListener(new OnSwipeTouchListener(this)
        {
            @Override
            public void onSwipeRight()
            {
                Common.LogMessage("Swipe Right");
                finish();
            }

            @Override
            public void onSwipeLeft()
            {
                Common.LogMessage("Swipe Left");
                startActivity(new Intent(getBaseContext(), Webcam.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        String bits = "<body><h3>Please wait while your forecast is loaded.</h3></body>";
        wv.loadDataWithBaseURL(null, bits, "text/html", "utf-8", null);

        getForecast();
    }

    @Override
    public void finish()
    {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.forecast_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId())
        {
            case R.id.refresh:
                forceRefresh();
                return true;
            case R.id.mainmenu:
                startActivity(new Intent(getBaseContext(), MainActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            case R.id.webcam:
                startActivity(new Intent(getBaseContext(), Webcam.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            case R.id.stats:
                startActivity(new Intent(getBaseContext(), Stats.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            case R.id.settings:
                startActivityForResult(new Intent(getBaseContext(), Settings.class), REQUEST_CODE);
                return true;
            case R.id.about:
                startActivity(new Intent(getBaseContext(), About.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Common.LogMessage("onActivityResult();");

        Common.LogMessage("requestCode == "+requestCode);
        Common.LogMessage("resultCode == "+resultCode);

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE)
        {
            if (data.hasExtra("urlChanged"))
            {
                Common.LogMessage("w00t!");
                String bits = "<body><h3>Please wait while your forecast is loaded.</h3></body>";
                wv.loadDataWithBaseURL(null, bits, "text/html", "utf-8", null);

                if(!common.GetStringPref("FORECAST_URL", "").equals(""))
                    getForecast();
            }
        }
    }

    private void forceRefresh()
    {
        Common.LogMessage("wiping rss cache");
        common.SetIntPref("rssCheck", 0);
        common.SetStringPref("forecastData", "");
        getForecast();
    }

    private void getForecast()
    {
        final String rss = common.GetStringPref("FORECAST_URL", "");
        if(rss.equals(""))
            wv.loadDataWithBaseURL(null, "<html><body>Forecast URL not set, go to settings to change</body></html>", "text/html", "utf-8", null);

        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    int curtime = round(System.currentTimeMillis() / 1000);

                    if(common.GetStringPref("forecastData", "").equals("") || common.GetIntPref("rssCheck", 0) + 3600 < curtime)
                    {
                        generateForecast();

                        Common.LogMessage("no forecast data or cache is more than 3 hour old");
                        URL url = new URL(rss);
                        URLConnection conn = url.openConnection();
                        conn.setDoOutput(true);
                        conn.connect();
                        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

                        String line;
                        StringBuilder sb = new StringBuilder();
                        while ((line = in.readLine()) != null)
                        {
                            line += "\n";
                            sb.append(line);
                        }
                        in.close();

                        Common.LogMessage("updating rss cache");
                        common.SetIntPref("rssCheck", curtime);
                        common.SetStringPref("forecastData", sb.toString().trim());
                    }

                    generateForecast();
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        t.start();
    }

    private void generateForecast()
    {
        JSONObject json;

        try
        {
            Common.LogMessage("getting json data");
            json = new JSONObject(common.GetStringPref("forecastData", ""));

            Common.LogMessage("starting JSON Parsing");

            JSONObject query = json.getJSONObject("query");
            JSONObject results = query.getJSONObject("results");
            JSONObject channel = results.getJSONObject("channel");
            JSONObject item = channel.getJSONObject("item");
            JSONObject units = channel.getJSONObject("units");
            String temp = units.getString("temperature");
            final String desc = channel.getString("description");
            JSONArray forecast = item.getJSONArray("forecast");
            Common.LogMessage("ended JSON Parsing");

            StringBuilder str = new StringBuilder();

            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);

            int start = 0;
            if(hour >= 15)
                start = 1;

            JSONObject tmp = forecast.getJSONObject(start);
            int code = tmp.getInt("code");
            String stmp;

            stmp = "<table style='width:100%;border:0px;'>";
            str.append(stmp);
            stmp = "<tr><td style='width:50%;font-size:16pt;'>" + tmp.getString("date") + "</td>";
            str.append(stmp);
            stmp = "<td style='width:50%;text-align:right;' rowspan='2'><img width='100px' src='file:///android_res/drawable/yahoo"+code+"'></td></tr>";
            str.append(stmp);

            stmp = "<tr><td style='width:50%;font-size:48pt;'>" + tmp.getString("high") + "&deg;" + temp + "</td></tr>";
            str.append(stmp);

            stmp = "<tr><td style='font-size:16pt;'>" + tmp.getString("low") + "&deg;" + temp + "</td>";
            str.append(stmp);

            stmp = "<td style='text-align:right;font-size:16pt;'>" + tmp.getString("text") + "</td></tr></table><br>";
            str.append(stmp);

            stmp = "<table style='width:100%;border:0px;'>";
            str.append(stmp);

            for (int i = start + 1; i <= start + 5; i++)
            {
                tmp = forecast.getJSONObject(i);
                code = tmp.getInt("code");

                stmp = "<tr><td style='width:10%;' rowspan='2'>" + "<img width='40px' src='file:///android_res/drawable/yahoo"+code+"'></td>";
                str.append(stmp);

                stmp = "<td style='width:45%;'><b>" + tmp.getString("day") + ", " + tmp.getString("date") + "</b></td>";
                str.append(stmp);

                stmp = "<td style='width:45%;text-align:right;'><b>" + tmp.getString("high") + "&deg;" + temp + "</b></td></tr>";
                str.append(stmp);

                stmp = "<tr><td>" + tmp.getString("text") + "</td>";
                str.append(stmp);

                stmp = "<td style='text-align:right;'>" + tmp.getString("low") + "&deg;" + temp + "</td></tr>";
                str.append(stmp);

                stmp = "<tr><td style='font-size:10pt;' colspan='5'>&nbsp;</td></tr>";
                str.append(stmp);
            }

            stmp = "</table>";
            str.append(stmp);

            final String bits = str.toString();

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Common.LogMessage("finished building forecast: " + bits);
                    updateForecast(bits, desc);
                }
            });
        } catch (Exception e) {
            //Common.LogMessage("Error parsing data " + e.toString());
            e.printStackTrace();
        }
    }

    private void updateForecast(String bits, String desc)
    {
        String fc = "<html><body style='text-align:center'>";
        fc += bits + "</body></html>";
        wv.loadDataWithBaseURL(null, fc, "text/html", "utf-8", null);

        TextView tv1 = findViewById(R.id.forecast);
        tv1.setText(desc.substring(19));
    }
}