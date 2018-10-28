package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static java.lang.Math.round;

class Common
{
	private final static String PREFS_NAME = "WeeWxWeatherPrefs";
	private final static boolean debug_on = true;
	private String appversion = "0.0.0";
	Context context;

	final static String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36";

	static String UPDATE_INTENT = "com.odiousapps.weewxweather.UPDATE_INTENT";
	static String TAB0_INTENT = "com.odiousapps.weewxweather.TAB0_INTENT";
	static String EXIT_INTENT = "com.odiousapps.weewxweather.EXIT_INTENT";
	static String INIGO_INTENT = "com.odiousapps.weewxweather.INIGO_UPDATE";

	private static final long inigo_version = 4000;

	private Thread t = null;

	Common(Context c)
	{
		System.setProperty("http.agent", UA);
		this.context = c;

		try
		{
			PackageManager pm = c.getPackageManager();
			PackageInfo version = pm.getPackageInfo("com.odiousapps.weewxweather", 0);
			appversion = version.versionName;
			LogMessage("appversion=" + appversion);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	long[] getPeriod()
	{
		long[] def = {0, 0};

		int pos = GetIntPref("updateInterval", 1);
		if (pos <= 0)
			return def;

		long period;

		switch (pos)
		{
			case 1:
				period = 5 * 60000;
				break;
			case 2:
				period = 10 * 60000;
				break;
			case 3:
				period = 15 * 60000;
				break;
			case 4:
				period = 30 * 60000;
				break;
			case 5:
				period = 60 * 60000;
				break;
			default:
				return def;
		}

		return new long[]{period, 45000};
	}

	String getAppversion()
	{
		return appversion;
	}

	static void LogMessage(String value)
	{
		LogMessage(value, false);
	}

	static void LogMessage(String value, boolean showAnyway)
	{
		if (debug_on || showAnyway)
			Log.i("weeWX Weather", "message='" + value + "'");
	}

	void SetStringPref(String name, String value)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(name, value);
		editor.apply();

		LogMessage("Updating '" + name + "'='" + value + "'");
	}

	void RemovePref(String name)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove(name);
		editor.apply();

		LogMessage("Removing '" + name + "'");
	}

	@SuppressLint("ApplySharedPref")
	void commit()
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.commit();
	}

	String GetStringPref(String name, String defval)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		String value;

		try
		{
			value = settings.getString(name, defval);
		} catch (ClassCastException cce)
		{
			cce.printStackTrace();
			//SetStringPref(name, defval);
			return defval;
		} catch (Exception e)
		{
			LogMessage("GetStringPref(" + name + ", " + defval + ") Err: " + e.toString());
			e.printStackTrace();
			return defval;
		}

		LogMessage(name + "'='" + value + "'");

		return value;
	}

	@SuppressWarnings({"unused", "SameParameterValue"})
	private void SetLongPref(String name, long value)
	{
		SetStringPref(name, "" + value);
	}

	@SuppressWarnings("unused")
	long GetLongPref(String name)
	{
		return GetLongPref(name, 0);
	}

	@SuppressWarnings("WeakerAccess")
	long GetLongPref(String name, @SuppressWarnings("SameParameterValue") long defval)
	{
		String val = GetStringPref(name, "" + defval);
		if (val == null)
			return defval;
		return Long.parseLong(val);
	}

	void SetIntPref(String name, int value)
	{
		SetStringPref(name, "" + value);
	}

	@SuppressWarnings("unused")
	int GetIntPref(String name)
	{
		return GetIntPref(name, 0);
	}

	int GetIntPref(String name, int defval)
	{
		String val = GetStringPref(name, "" + defval);
		if (val == null)
			return defval;
		return Integer.parseInt(val);
	}

	void SetBoolPref(String name, boolean value)
	{
		String val = "0";
		if (value)
			val = "1";

		SetStringPref(name, val);
	}

	@SuppressWarnings("unused")
	boolean GetBoolPref(String name)
	{
		return GetBoolPref(name, false);
	}

	boolean GetBoolPref(String name, boolean defval)
	{
		String value = "0";
		if (defval)
			value = "1";

		String val = GetStringPref(name, value);
		return val.equals("1");
	}

	RemoteViews buildUpdate(Context context)
	{
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
		Bitmap myBitmap = Bitmap.createBitmap(600, 440, Bitmap.Config.ARGB_4444);
		Canvas myCanvas = new Canvas(myBitmap);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setSubpixelText(true);

		int bgColour = GetIntPref("bgColour", 0xFFFFFFFF);
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(bgColour);

		RectF rectF = new RectF(0, 0, myCanvas.getWidth(), myCanvas.getHeight());
		int cornersRadius = 25;
		myCanvas.drawRoundRect(rectF, cornersRadius, cornersRadius, paint);

		int fgColour = GetIntPref("fgColour", 0xFF000000);

		paint.setStyle(Paint.Style.FILL);
		paint.setColor(fgColour);
		paint.setTextAlign(Paint.Align.CENTER);

		String bits[] = GetStringPref("LastDownload", "").split("\\|");
		if (bits.length > 110)
		{
			paint.setTextSize(64);
			myCanvas.drawText(bits[56], myCanvas.getWidth() / 2, 80, paint);
			paint.setTextSize(48);
			myCanvas.drawText(bits[55], myCanvas.getWidth() / 2, 140, paint);
			paint.setTextSize(200);
			myCanvas.drawText(bits[0] + bits[60], myCanvas.getWidth() / 2, 310, paint);

			paint.setTextAlign(Paint.Align.LEFT);
			paint.setTextSize(64);
			myCanvas.drawText(bits[25] + bits[61], 20, 400, paint);

			paint.setTextAlign(Paint.Align.RIGHT);
			paint.setTextSize(64);

			String rain = bits[20];
			if (bits.length > 158 && !bits[158].equals(""))
				rain = bits[158];

			myCanvas.drawText(rain + bits[62], myCanvas.getWidth() - 20, 400, paint);
		} else
		{
			paint.setTextSize(200);
			myCanvas.drawText("Error!", myCanvas.getWidth() / 2, 300, paint);
		}

		views.setImageViewBitmap(R.id.widget, myBitmap);
		return views;
	}

	String[] processWZ(String data)
	{
		return processWZ(data, false);
	}

	String[] processWZ(String data, boolean showHeader)
	{
		boolean metric = GetBoolPref("metric", true);

		try
		{
			String desc = "", content = "", pubDate = "";

			String[] bits = data.split("<title>");
			if (bits.length >= 2)
				desc = bits[1].split("</title>")[0].trim();

			bits = data.split("<description>");
			if (bits.length >= 3)
			{
				String s = bits[2].split("</description>")[0];
				content = s.substring(9, s.length() - 3).trim();
			}

			bits = data.split("<pubDate>");
			if (bits.length >= 2)
				pubDate = bits[1].split("</pubDate>")[0].trim();

			if (pubDate.equals(""))
				return null;

			StringBuilder str = new StringBuilder();
			String stmp;

			SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault());
			long mdate = sdf.parse(pubDate).getTime();
			sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
			pubDate = sdf.format(mdate);

			content = content.replace("src=\"http://www.weatherzone.com.au/images/icons/fcast_30/", "width=\"40px\" src=\"file:///android_res/drawable/wz")
					.replace(".gif", ".png");

			if(showHeader)
			{
				String[] days = content.split("<b>");
				String day = days[1];
				String[] tmp = day.split("</b>", 2);

				String[] mybits = tmp[1].split("<br />");
				String myimg = mybits[1];
				String mydesc = mybits[2];
				String[] range = mybits[3].split(" - ", 2);


				stmp = "<table style='width:100%;border:0px;'>";
				str.append(stmp);

				stmp = "<tr><td style='width:50%;font-size:48pt;'>" + scrubTemp(range[1], metric) + "</td>";
				str.append(stmp);

				stmp = "<td style='width:50%;text-align:right;'>" + myimg.replace("40px", "80px") + "</td></tr>";
				str.append(stmp);

				stmp = "<tr><td style='font-size:16pt;'>" + scrubTemp(range[0], metric) + "</td>";
				str.append(stmp);

				stmp = "<td style='text-align:right;font-size:16pt;'>" +mydesc + "</td></tr></table><br />";
				str.append(stmp);

				stmp = "<table style='width:100%;border:0px;'>";
				str.append(stmp);

				for(int i = 2; i < days.length; i++)
				{
					day = days[i];
					tmp = day.split("</b>", 2);
					String dayName = tmp[0];

					if (tmp.length <= 1)
						continue;

					mybits = tmp[1].split("<br />");
					myimg = mybits[1];
					mydesc = mybits[2];
					range = mybits[3].split(" - ", 2);

					stmp = "<tr><td style='width:10%;' rowspan='2'>" + myimg + "</td>";
					str.append(stmp);

					stmp = "<td style='width:65%;'><b>" + dayName + "</b></td>";
					str.append(stmp);

					stmp = "<td style='width:25%;text-align:right;'><b>" + scrubTemp(range[1], metric) + "</b></td></tr>";
					str.append(stmp);

					stmp = "<tr><td>" + mydesc + "</td>";
					str.append(stmp);

					stmp = "<td style='text-align:right;'>" + scrubTemp(range[0], metric) + "</td></tr>";
					str.append(stmp);

					stmp = "<tr><td style='font-size:4pt;' colspan='5'>&nbsp;</td></tr>";
					str.append(stmp);
				}

				stmp = "</table>";
				str.append(stmp);
			} else {
				stmp = "<table style='width:100%;border:0px;'>";
				str.append(stmp);

				String[] days = content.split("<b>");
				for (String day : days)
				{
					String[] tmp = day.split("</b>", 2);
					String dayName = tmp[0];

					if (tmp.length <= 1)
						continue;

					String[] mybits = tmp[1].split("<br />");
					String myimg = mybits[1];
					String mydesc = mybits[2];
					String[] range = mybits[3].split(" - ", 2);

					stmp = "<tr><td style='width:10%;' rowspan='2'>" + myimg + "</td>";
					str.append(stmp);

					stmp = "<td style='width:65%;'><b>" + dayName + "</b></td>";
					str.append(stmp);

					stmp = "<td style='width:25%;text-align:right;'><b>" + scrubTemp(range[1], metric) + "</b></td></tr>";
					str.append(stmp);

					stmp = "<tr><td>" + mydesc + "</td>";
					str.append(stmp);

					stmp = "<td style='text-align:right;'>" + scrubTemp(range[0], metric) + "</td></tr>";
					str.append(stmp);

					stmp = "<tr><td style='font-size:4pt;' colspan='5'>&nbsp;</td></tr>";
					str.append(stmp);
				}

				stmp = "</table>";
				str.append(stmp);
			}

			content = "<div style='font-size:12pt;'>" + pubDate + "</div>" + str.toString();

			Common.LogMessage("content=" + content);

			return new String[]{content, desc};
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}

	private String scrubTemp(String temp, boolean metric)
	{
		if(temp.length() <= 7 || metric)
			return temp;

		float i = Float.parseFloat(temp.substring(0, temp.length() - 7));
		LogMessage("i == " + i);
		int f = round(9.0f / 5.0f * i + 32.0f);
		LogMessage("f == " + f);
		return String.valueOf(f) + "&#176;F";
	}

	String[] processYahoo(String data)
	{
		return processYahoo(data, false);
	}

    String[] processYahoo(String data, boolean showHeader)
    {
	    JSONObject json;

	    try
	    {
		    json = new JSONObject(data);

		    Common.LogMessage("starting JSON Parsing");

		    JSONObject query = json.getJSONObject("query");
		    JSONObject results = query.getJSONObject("results");
		    JSONObject channel = results.getJSONObject("channel");
		    String pubdate = channel.getString("lastBuildDate");
		    JSONObject item = channel.getJSONObject("item");
		    JSONObject units = channel.getJSONObject("units");
		    String temp = units.getString("temperature");
		    final String desc = channel.getString("description").substring(19);
		    JSONArray forecast = item.getJSONArray("forecast");
		    Common.LogMessage("ended JSON Parsing");

		    StringBuilder str = new StringBuilder();
		    String stmp;

		    Calendar calendar = Calendar.getInstance();
		    int hour = calendar.get(Calendar.HOUR_OF_DAY);

		    int start = 0;
		    if (hour >= 15)
			    start = 1;

		    JSONObject tmp = forecast.getJSONObject(start);
		    int code = tmp.getInt("code");

//		    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
//		    long rssCheck = GetIntPref("rssCheck", 0);
//		    rssCheck *= 1000;
//		    Date resultdate = new Date(rssCheck);

		    if(showHeader)
		    {
			    stmp = "<div style='font-size:12pt;'>" + pubdate + "</div>";
			    str.append(stmp);

			    stmp = "<table style='width:100%;border:0px;'>";
			    str.append(stmp);

			    stmp = "<tr><td style='width:50%;font-size:48pt;'>" + tmp.getString("high") + "&deg;" + temp + "</td>";
			    str.append(stmp);

			    stmp = "<td style='width:50%;text-align:right;'><img width='80px' src='file:///android_res/drawable/yahoo" + code + ".png'></td></tr>";
			    str.append(stmp);

			    stmp = "<tr><td style='font-size:16pt;'>" + tmp.getString("low") + "&deg;" + temp + "</td>";
			    str.append(stmp);

			    stmp = "<td style='text-align:right;font-size:16pt;'>" + tmp.getString("text") + "</td></tr></table><br />";
			    str.append(stmp);

			    stmp = "<table style='width:100%;border:0px;'>";
			    str.append(stmp);

			    for (int i = start + 1; i <= start + 5; i++)
			    {
				    tmp = forecast.getJSONObject(i);
				    code = tmp.getInt("code");

				    stmp = "<tr><td style='width:10%;' rowspan='2'>" + "<img width='40px' src='file:///android_res/drawable/yahoo" + code + ".png'></td>";
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
		    } else {
			    stmp = "<div style='font-size:12pt;'>" + pubdate + "</div>";
			    str.append(stmp);

			    stmp = "<table style='width:100%;border:0px;'>";
			    str.append(stmp);

			    for (int i = start; i <= start + 5; i++)
			    {
				    tmp = forecast.getJSONObject(i);
				    code = tmp.getInt("code");

				    stmp = "<tr><td style='width:10%;' rowspan='2'>" + "<img width='40px' src='file:///android_res/drawable/yahoo" + code + "'></td>";
				    str.append(stmp);

				    stmp = "<td style='width:45%;'><b>" + tmp.getString("day") + ", " + tmp.getString("date") + "</b></td>";
				    str.append(stmp);

				    stmp = "<td style='width:45%;text-align:right;'><b>" + tmp.getString("high") + "&deg;" + temp + "</b></td></tr>";
				    str.append(stmp);

				    stmp = "<tr><td>" + tmp.getString("text") + "</td>";
				    str.append(stmp);

				    stmp = "<td style='text-align:right;'>" + tmp.getString("low") + "&deg;" + temp + "</td></tr>";
				    str.append(stmp);

				    stmp = "<tr><td style='font-size:5pt;' colspan='5'>&nbsp;</td></tr>";
				    str.append(stmp);
			    }

			    stmp = "</table>";
			    str.append(stmp);
		    }

		    Common.LogMessage("finished building forecast: " + str.toString());
		    return new String[]{str.toString(), desc};
	    } catch (Exception e) {
		    e.printStackTrace();
	    }

	    return null;
    }

	void SendIntents()
	{
		Intent intent = new Intent();
		intent.setAction(Common.UPDATE_INTENT);
		context.sendBroadcast(intent);
		Common.LogMessage("update_intent broadcast.");

		RemoteViews remoteViews = buildUpdate(context);
		ComponentName thisWidget = new ComponentName(context, WidgetProvider.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		manager.updateAppWidget(thisWidget, remoteViews);
		Common.LogMessage("widget intent broadcasted");
	}

	void getWeather()
	{
		if(t != null)
		{
			if(t.isAlive())
				t.interrupt();
			t = null;
		}

		t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					String data = GetStringPref("BASE_URL", "");
					if (data.equals(""))
						return;

					reallyGetWeather(data);
					SendIntents();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		t.start();
	}

	void reallyGetWeather(String data) throws Exception
	{
		Uri uri = Uri.parse(data);
		if (uri.getUserInfo() != null && uri.getUserInfo().contains(":"))
		{
			final String[] UC = uri.getUserInfo().split(":");
			Common.LogMessage("uri username = " + uri.getUserInfo());

			if (UC.length > 1)
			{
				Authenticator.setDefault(new Authenticator()
				{
					protected PasswordAuthentication getPasswordAuthentication()
					{
						return new PasswordAuthentication(UC[0], UC[1].toCharArray());
					}
				});
			}
		}

		URL url = new URL(data);
		HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
		urlConnection.setConnectTimeout(5000);
		urlConnection.setReadTimeout(5000);
		urlConnection.setRequestMethod("GET");
		urlConnection.setDoOutput(true);
		urlConnection.connect();

		StringBuilder sb = new StringBuilder();
		String line;

		InterruptThread it = new InterruptThread(Thread.currentThread(), urlConnection);
		Thread myThread = new Thread(it);

		myThread.start();
		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		while ((line = in.readLine()) != null)
			sb.append(line);
		in.close();

		it.interupt = false;
		myThread.interrupt();

		line = sb.toString().trim();
		if(!line.equals(""))
		{
			String bits[] = line.split("\\|");
			if (Double.valueOf(bits[0]) < Common.inigo_version)
			{
				if(GetLongPref("inigo_version", 0) < Common.inigo_version)
				{
					SetLongPref("inigo_version", Common.inigo_version);
					sendAlert();
				}
			}

			if (Double.valueOf(bits[0]) >= 4000)
			{
				sb = new StringBuilder();
				for (int i = 1; i < bits.length; i++)
				{
					if (sb.length() > 0)
						sb.append("|");
					sb.append(bits[i]);
				}

				line = sb.toString().trim();
			}

			SetStringPref("LastDownload", line);
			SetLongPref("LastDownloadTime", round(System.currentTimeMillis() / 1000));
		}
	}

	class InterruptThread implements Runnable
	{
		Thread parent;
		HttpURLConnection con;
		boolean interupt;

		InterruptThread(Thread parent, HttpURLConnection con)
		{
			this.parent = parent;
			this.con = con;
			this.interupt = true;
		}

		public void run()
		{
			try
			{
				Thread.sleep(15000);
				if(interupt)
				{
					Common.LogMessage("Timer thread forcing parent to quit connection");
					con.disconnect();
					parent.interrupt();
				}
			} catch (InterruptedException e) {
				// TODO: ignore this.
			}
		}
	}

	private void sendAlert()
	{
		Intent intent = new Intent();
		intent.setAction(Common.INIGO_INTENT);
		context.sendBroadcast(intent);
		Common.LogMessage("Send user note about upgrading the Inigo Plugin");
	}
}