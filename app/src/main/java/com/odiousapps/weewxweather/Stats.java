package com.odiousapps.weewxweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static java.lang.Math.round;

public class Stats extends Fragment
{
	private final Common common;
	private View rootView;
	private WebView wv;
	private SeekBar seekBar;
	private int dark_theme;
	private LinearLayout ll1;
	private SwipeRefreshLayout swipeLayout;

	Stats(Common common)
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
		rootView = inflater.inflate(R.layout.fragment_stats, container, false);
		swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
		swipeLayout.setOnRefreshListener(() ->
		{
			swipeLayout.setRefreshing(true);
			Common.LogMessage("onRefresh();");
			forceRefresh();
			swipeLayout.setRefreshing(false);
		});

		wv = rootView.findViewById(R.id.webView1);
		wv.getSettings().setUserAgentString(Common.UA);
		wv.getViewTreeObserver().addOnScrollChangedListener(() -> swipeLayout.setEnabled(wv.getScrollY() == 0));

		ll1 = rootView.findViewById(R.id.ll1);

		seekBar = rootView.findViewById(R.id.pageZoom);
		seekBar.setProgress(common.GetIntPref("seekBar", 10));

		if(dark_theme == 1)
		{
			seekBar.setBackgroundColor(0xff000000);
			ll1.setBackgroundColor(0xff000000);
		} else {
			seekBar.setBackgroundColor(0xffffffff);
			ll1.setBackgroundColor(0xffffffff);
		}
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar seekBar, int i, boolean b)
			{
				updateFields();
				if(b)
					common.SetIntPref("seekBar", i);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{
			}
		});

		updateFields();
		return rootView;
	}

	public void onResume()
	{
		super.onResume();
		updateFields();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Common.UPDATE_INTENT);
		filter.addAction(Common.REFRESH_INTENT);
		filter.addAction(Common.EXIT_INTENT);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
			common.context.registerReceiver(serviceReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
		else
			common.context.registerReceiver(serviceReceiver, filter);
		Common.LogMessage("stats.java -- registerReceiver");
	}

	public void onPause()
	{
		super.onPause();
		try
		{
			common.context.unregisterReceiver(serviceReceiver);
		} catch (Exception e)
		{
			//TODO: ignore this exception...
		}
		Common.LogMessage("stats.java -- unregisterReceiver");
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
				if (action != null && (action.equals(Common.UPDATE_INTENT) || action.equals(Common.REFRESH_INTENT)))
				{
					dark_theme = common.GetIntPref("dark_theme", 2);
					if(dark_theme == 2)
						dark_theme = common.getSystemTheme();

					if (dark_theme == 1)
					{
						seekBar.setBackgroundColor(0xff000000);
						ll1.setBackgroundColor(0xff000000);
					} else {
						seekBar.setBackgroundColor(0xffffffff);
						ll1.setBackgroundColor(0xffffffff);
					}

					updateFields();
				} else if(action != null && action.equals(Common.EXIT_INTENT))
					onPause();
			} catch (Exception e) {
				Common.doStackOutput(e);
			}
		}
	};

	private void forceRefresh()
	{
		common.getWeather();
	}

	private void checkFields(final TextView tv, final String txt)
	{
		Handler mHandler = new Handler(Looper.getMainLooper());
		mHandler.post(() -> reallyCheckFields(tv, txt));
	}

	private void reallyCheckFields(TextView tv, String txt)
	{
		if(!tv.getText().toString().equals(txt))
			tv.setText(txt);

		if(dark_theme == 0)
		{
			tv.setTextColor(0xff000000);
			tv.setBackgroundColor(0xffffffff);
		} else {
			tv.setTextColor(0xffffffff);
			tv.setBackgroundColor(0xff000000);
		}
	}

	private String convert(String cur)
	{
		cur = cur.trim();

		String[] bits = null;

		if(!cur.contains(" "))
			return cur;

		try
		{
			//cur = "9:34:00 PM";
			bits = cur.trim().split(" ");
		} catch (Exception e) {
			Common.doStackOutput(e);
		}

		if(bits == null || bits.length < 2)
			return cur;

		String[] time = null;
		try
		{
			time = bits[0].trim().split(":");
		} catch (Exception e) {
			Common.doStackOutput(e);
		}

		if(time == null || time.length < 3)
			return cur;

		int hours = Integer.parseInt(time[0]);
		int mins = Integer.parseInt(time[1]);
		int secs = Integer.parseInt(time[2]);

		boolean pm = bits[1].trim().equalsIgnoreCase("pm");

		Log.i("weeWx", "pm == '" + bits[1] + "'");

		if(!pm && hours == 12)
			hours = 0;
		else if(pm && hours != 12)
			hours = hours + 12;

		return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs);
	}

	private String getTime(String str)
	{
		str = str.trim();

		if(!str.contains(" "))
			return str;

		try
		{
			return str.split(" ", 2)[0];
		} catch (Exception e) {
			Common.doStackOutput(e);
		}

		return str;
	}

	private void updateFields()
	{
		Thread t = new Thread(() ->
		{
			try
			{
				// Sleep needed to stop frames dropping while loading
				Thread.sleep(500);
			} catch (Exception e) {
				Common.doStackOutput(e);
			}

			int iw = 17;

			String[] bits = common.GetStringPref("LastDownload", "").split("\\|");

			if(bits.length < 65)
				return;

			Handler mHandler = new Handler(Looper.getMainLooper());
			mHandler.post(() -> swipeLayout.setRefreshing(true));

			// Today Stats
			checkFields(rootView.findViewById(R.id.textView), bits[56]);
			checkFields(rootView.findViewById(R.id.textView2), bits[54] + " " + bits[55]);

			double percent = (seekBar.getProgress() + 90) / 100.00;
			NumberFormat formatter = new DecimalFormat("#0.00");
			String p = formatter.format(percent);

			String stmp;
			final StringBuilder sb = new StringBuilder();

			String header = "<html>";
			if(dark_theme == 1)
				header += "<head><style>body{color: #fff; background-color: #000;}img{filter:invert(100%);}</style>" + Common.style_sheet_header + "</head>";
			else
				header += "<head>" + Common.style_sheet_header + "</head>";
			header += "<body style='text-align:center; transform: scale(" + p + "); transform-origin: 0 0;'>";
			String footer = "</body></html>";

			sb.append(header);

			sb.append("<span style='font-size:18pt;font-weight:bold;'>");
			sb.append(getString(R.string.todayStats));
			sb.append("</span>");
			sb.append("<table style='width:100%;border:0px;'>");

			stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-temperature'></i></td><td>" + bits[3] + bits[60] + "</td><td>" + convert(bits[4]) +
					"</td><td>" + convert(bits[2]) + "</td><td>" + bits[1] + bits[60] + "</td><td><i style='font-size:" + iw + "px;' class='flaticon-temperature'></i></td></tr>";
			sb.append(stmp);

			stmp = "<tr><td><i style='font-size:" + round(iw * 1.4) + "px;' class='wi wi-raindrop'></i></td><td>" + bits[15] + bits[60] + "</td><td>" + convert(bits[16]) +
					"</td><td>" + convert(bits[14]) + "</td><td>" + bits[13] + bits[60] + "</td><td><i style='font-size:" + round(iw * 1.4) + "px;' class='wi wi-raindrop'></i></td></tr>";
			sb.append(stmp);

			stmp = "<tr><td><i style='font-size:" + iw + "px;' class='wi wi-humidity'></i></td><td>" + bits[9] + bits[64] + "</td><td>" + convert(bits[10]) +
					"</td><td>" + convert(bits[8]) + "</td><td>" + bits[6] + bits[64] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-humidity'></i></td></tr>";
			sb.append(stmp);

			stmp = "<tr><td><i style='font-size:" + iw + "px;' class='wi wi-barometer'></i></td><td>" + bits[39] + bits[63] + "</td><td>" + convert(bits[40]) +
					"</td><td>" + convert(bits[42]) + "</td><td>" + bits[41] + bits[63] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-barometer'></i></td></tr>";
			sb.append(stmp);

			if(bits.length > 202 && common.GetBoolPref("showIndoor", false))
			{
				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td><td>" + bits[164] + bits[60] + "</td><td>" + convert(bits[165]) +
						"</td><td>" + convert(bits[163]) + "</td><td>" + bits[162] + bits[60] + "</td><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td></tr>";
				sb.append(stmp);

				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td><td>" + bits[169] + bits[64] + "</td><td>" + convert(bits[170]) +
						"</td><td>" + convert(bits[168]) + "</td><td>" + bits[167] + bits[64] + "</td><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td></tr>";
				sb.append(stmp);
			}

			if(bits.length > 205 && !bits[205].isEmpty())
			{
				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-women-sunglasses'></i></td><td>" + bits[205] + "UVI</td><td>" + convert(bits[206]) +
						"</td><td>" + convert(bits[208]) + "</td><td style='text-align:right;'>" + bits[207] + "W/m²</td><td><i style='font-size:" + iw + "px;' class='flaticon-women-sunglasses'></i></td></tr>";
				sb.append(stmp);
			}

			String rain = bits[20];
			String since = getString(R.string.since) + " mn";

			if (bits.length > 160 && !bits[160].isEmpty())
				rain = bits[158];

			if (bits.length > 160 && !bits[158].isEmpty() && !bits[160].isEmpty())
				since = getString(R.string.since) + " " + bits[160];

			stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-windy'></i></td><td colspan='3'>" + bits[19] + bits[61] + " " + bits[32] + " " + convert(bits[33]) +
					"</td><td>" + rain + bits[62] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-umbrella'></i></td></tr>";
			sb.append(stmp);

			stmp = "<tr><td colspan='4'>&nbsp;</td><td colspan='2'>" + since + "</td></tr>";
			sb.append(stmp);

			stmp = "</table><br>";
			sb.append(stmp);

			if (bits.length > 87 && !bits[87].isEmpty())
			{
				sb.append("<span style='font-size:18pt;font-weight:bold;'>");
				sb.append(getString(R.string.yesterdayStats));
				sb.append("</span>");
				sb.append("<table style='width:100%;border:0px;'>");

				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-temperature'></i></td><td>" + bits[67] + bits[60] + "</td><td>" + convert(bits[68]) +
						"</td><td>" + convert(bits[66]) + "</td><td>" + bits[65] + bits[60] + "</td><td><i style='font-size:" + iw + "px;' class='flaticon-temperature'></i></td></tr>";
				sb.append(stmp);

				stmp = "<tr><td><i style='font-size:" + round(iw * 1.4) + "px;' class='wi wi-raindrop'></td><td>" + bits[78] + bits[60] + "</td><td>" + convert(bits[79]) +
						"</td><td>" + convert(bits[77]) + "</td><td>" + bits[76] + bits[60] + "</td><td><i style='font-size:" + round(iw * 1.4) + "px;' class='wi wi-raindrop'></td></tr>";
				sb.append(stmp);

				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='wi wi-humidity'></i></td><td>" + bits[82] + bits[64] + "</td><td>" + convert(bits[83]) +
						"</td><td>" + convert(bits[81]) + "</td><td>" + bits[80] + bits[64] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-humidity'></i></td></tr>";
				sb.append(stmp);

				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='wi wi-barometer'></i></td><td>" + bits[84] + bits[63] + "</td><td>" + convert(bits[85]) +
						"</td><td>" + convert(bits[87]) + "</td><td>" + bits[86] + bits[63] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-barometer'></i></td></tr>";
				sb.append(stmp);

				if(bits.length > 202 && common.GetBoolPref("showIndoor", false))
				{
					stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td><td>" + bits[173] + bits[60] + "</td><td>" + convert(bits[174]) +
							"</td><td>" + convert(bits[172]) + "</td><td>" + bits[171] + bits[60] + "</td><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td></tr>";
					sb.append(stmp);

					stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td><td>" + bits[177] + bits[64] + "</td><td>" + convert(bits[178]) +
							"</td><td>" + convert(bits[176]) + "</td><td>" + bits[175] + bits[64] + "</td><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td></tr>";
					sb.append(stmp);
				}

				if(bits.length > 209 && !bits[209].isEmpty())
				{
					stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-women-sunglasses'></i></td><td>" + bits[209] + "UVI</td><td>" + convert(bits[210]) +
							"</td><td>" + convert(bits[212]) + "</td><td style='text-align:right;'>" + bits[211] + "W/m²</td><td><i style='font-size:" + iw + "px;' class='flaticon-women-sunglasses'></i></td></tr>";
					sb.append(stmp);
				}

				rain = bits[21];
				String before = getString(R.string.before) + " mn";

				if (bits.length > 160 && !bits[159].isEmpty())
					rain = bits[159];

				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-windy'></i></td><td colspan='3'>" + bits[69] + bits[61] + " " + bits[70] + " " + convert(bits[71]) +
						"</td><td>" + rain + bits[62] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-umbrella'></i></td></tr>";
				sb.append(stmp);

				if (bits.length > 160 && !bits[159].isEmpty() && !bits[160].isEmpty())
					before = getString(R.string.before) + " " + bits[160];

				stmp = "<tr><td colspan='4'>&nbsp;</td><td colspan='2'>" + before + "</td></tr>";
				sb.append(stmp);

				stmp = "</table><br>";
				sb.append(stmp);
			}

			if(bits.length > 110 && !bits[110].isEmpty())
			{
				sb.append("<span style='font-size:18pt;font-weight:bold;'>");
				sb.append(getString(R.string.this_months_stats));
				sb.append("</span>");
				sb.append("<table style='width:100%;border:0px;'>");

				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-temperature'></i></td><td>" + bits[90] + bits[60] + "</td><td>" + getTime(bits[91]) +
						"</td><td>" + getTime(bits[89]) + "</td><td>" + bits[88] + bits[60] + "</td><td><i style='font-size:" + iw + "px;' class='flaticon-temperature'></i></td></tr>";
				sb.append(stmp);

				stmp = "<tr><td><i style='font-size:" + round(iw * 1.4) + "px;' class='wi wi-raindrop'></td><td>" + bits[101] + bits[60] + "</td><td>" + getTime(bits[102]) +
						"</td><td>" + getTime(bits[100]) + "</td><td>" + bits[99] + bits[60] + "</td><td><i style='font-size:" + round(iw * 1.4) + "px;' class='wi wi-raindrop'></td></tr>";
				sb.append(stmp);

				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='wi wi-humidity'></i></td><td>" + bits[105] + bits[64] + "</td><td>" + getTime(bits[106]) +
						"</td><td>" + getTime(bits[104]) + "</td><td>" + bits[103] + bits[64] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-humidity'></i></td></tr>";
				sb.append(stmp);

				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='wi wi-barometer'></i></td><td>" + bits[107] + bits[63] + "</td><td>" + getTime(bits[108]) +
						"</td><td>" + getTime(bits[110]) + "</td><td>" + bits[109] + bits[63] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-barometer'></i></td></tr>";
				sb.append(stmp);

				if(bits.length > 202 && common.GetBoolPref("showIndoor", false))
				{
					stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td><td>" + bits[181] + bits[60] + "</td><td>" + getTime(bits[182]) +
							"</td><td>" + getTime(bits[180]) + "</td><td>" + bits[179] + bits[60] + "</td><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td></tr>";
					sb.append(stmp);

					stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td><td>" + bits[185] + bits[64] + "</td><td>" + getTime(bits[186]) +
							"</td><td>" + getTime(bits[184]) + "</td><td>" + bits[183] + bits[64] + "</td><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td></tr>";
					sb.append(stmp);
				}

				if(bits.length > 213 && !bits[213].isEmpty())
				{
					stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-women-sunglasses'></i></td><td>" + bits[213] + "UVI</td><td>" + getTime(bits[214]) +
							"</td><td>" + getTime(bits[216]) + "</td><td style='text-align:right;'>" + bits[215] + "W/m²</td><td><i style='font-size:" + iw + "px;' class='flaticon-women-sunglasses'></i></td></tr>";
					sb.append(stmp);
				}

				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-windy'></i></td><td colspan='3'>" + bits[92] + bits[61] + " " + bits[93] + " " + getTime(bits[94]) +
						"</td><td>" + bits[22] + bits[62] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-umbrella'></i></td></tr>";
				sb.append(stmp);

				stmp = "</table><br>";
				sb.append(stmp);
			}

			if (bits.length > 133 && !bits[133].isEmpty())
			{
				sb.append("<span style='font-size:18pt;font-weight:bold;'>");
				sb.append(getString(R.string.this_year_stats));
				sb.append("</span>");
				sb.append("<table style='width:100%;border:0px;'>");

				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-temperature'></i></td><td>" + bits[113] + bits[60] + "</td><td>" + getTime(bits[114]) +
						"</td><td>" + getTime(bits[112]) + "</td><td>" + bits[111] + bits[60] + "</td><td><i style='font-size:" + iw + "px;' class='flaticon-temperature'></i></td></tr>";
				sb.append(stmp);

				stmp = "<tr><td><i style='font-size:" + round(iw * 1.4) + "px;' class='wi wi-raindrop'></td><td>" + bits[124] + bits[60] + "</td><td>" + getTime(bits[125]) +
						"</td><td>" + getTime(bits[123]) + "</td><td>" + bits[122] + bits[60] + "</td><td><i style='font-size:" + round(iw * 1.4) + "px;' class='wi wi-raindrop'></td></tr>";
				sb.append(stmp);

				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='wi wi-humidity'></i></td><td>" + bits[128] + bits[64] + "</td><td>" + getTime(bits[129]) +
						"</td><td>" + getTime(bits[127]) + "</td><td>" + bits[126] + bits[64] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-humidity'></i></td></tr>";
				sb.append(stmp);

				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='wi wi-barometer'></i></td><td>" + bits[130] + bits[63] + "</td><td>" + getTime(bits[131]) +
						"</td><td>" + getTime(bits[133]) + "</td><td>" + bits[132] + bits[63] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-barometer'></i></td></tr>";
				sb.append(stmp);

				if(bits.length > 202 && common.GetBoolPref("showIndoor", false))
				{
					stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td><td>" + bits[189] + bits[60] + "</td><td>" + getTime(bits[190]) +
							"</td><td>" + getTime(bits[188]) + "</td><td>" + bits[187] + bits[60] + "</td><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td></tr>";
					sb.append(stmp);

					stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td><td>" + bits[193] + bits[64] + "</td><td>" + getTime(bits[194]) +
							"</td><td>" + getTime(bits[192]) + "</td><td>" + bits[191] + bits[64] + "</td><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td></tr>";
					sb.append(stmp);
				}

				if(bits.length > 217 && !bits[217].isEmpty())
				{
					stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-women-sunglasses'></i></td><td>" + bits[217] + "UVI</td><td>" + getTime(bits[218]) +
							"</td><td>" + getTime(bits[220]) + "</td><td style='text-align:right;'>" + bits[219] + "W/m²</td><td><i style='font-size:" + iw + "px;' class='flaticon-women-sunglasses'></i></td></tr>";
					sb.append(stmp);
				}

				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-windy'></i></td><td colspan='3'>" + bits[115] + bits[61] + " " + bits[116] + " " + getTime(bits[117]) +
						"</td><td>" + bits[23] + bits[62] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-umbrella'></i></td></tr>";
				sb.append(stmp);

				stmp = "</table><br>";
				sb.append(stmp);
			}

			if (bits.length > 157 && !bits[157].isEmpty())
			{
				sb.append("<span style='font-size:18pt;font-weight:bold;'>");
				sb.append(getString(R.string.all_time_stats));
				sb.append("</span>");
				sb.append("<table style='width:100%;border:0px;'>");

				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-temperature'></i></td><td>" + bits[136] + bits[60] + "</td><td>" + getTime(bits[137]) +
						"</td><td>" + getTime(bits[135]) + "</td><td>" + bits[134] + bits[60] + "</td><td><i style='font-size:" + iw + "px;' class='flaticon-temperature'></i></td></tr>";
				sb.append(stmp);

				stmp = "<tr><td><i style='font-size:" + round(iw * 1.4) + "px;' class='wi wi-raindrop'></td><td>" + bits[147] + bits[60] + "</td><td>" + getTime(bits[148]) +
						"</td><td>" + getTime(bits[146]) + "</td><td>" + bits[145] + bits[60] + "</td><td><i style='font-size:" + round(iw * 1.4) + "px;' class='wi wi-raindrop'></td></tr>";
				sb.append(stmp);

				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='wi wi-humidity'></i></td><td>" + bits[151] + bits[64] + "</td><td>" + getTime(bits[152]) +
						"</td><td>" + getTime(bits[150]) + "</td><td>" + bits[149] + bits[64] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-humidity'></i></td></tr>";
				sb.append(stmp);

				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='wi wi-barometer'></i></td><td>" + bits[153] + bits[63] + "</td><td>" + getTime(bits[154]) +
						"</td><td>" + getTime(bits[156]) + "</td><td>" + bits[155] + bits[63] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-barometer'></i></td></tr>";
				sb.append(stmp);

				if(bits.length > 202 && common.GetBoolPref("showIndoor", false))
				{
					stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td><td>" + bits[197] + bits[60] + "</td><td>" + getTime(bits[198]) +
							"</td><td>" + getTime(bits[196]) + "</td><td>" + bits[195] + bits[60] + "</td><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td></tr>";
					sb.append(stmp);

					stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td><td>" + bits[201] + bits[64] + "</td><td>" + getTime(bits[202]) +
							"</td><td>" + getTime(bits[200]) + "</td><td>" + bits[199] + bits[64] + "</td><td><i style='font-size:" + iw + "px;' class='flaticon-home-page'></i></td></tr>";
					sb.append(stmp);
				}

				if(bits.length > 221 && !bits[221].isEmpty())
				{
					stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-women-sunglasses'></i></td><td>" + bits[221] + "UVI</td><td>" + getTime(bits[222]) +
							"</td><td>" + getTime(bits[224]) + "</td><td style='text-align:right;'>" + bits[223] + "W/m²</td><td><i style='font-size:" + iw + "px;' class='flaticon-women-sunglasses'></i></td></tr>";
					sb.append(stmp);
				}

				stmp = "<tr><td><i style='font-size:" + iw + "px;' class='flaticon-windy'></i></td><td colspan='3'>" + bits[138] + bits[61] + " " + bits[139] + " " + getTime(bits[140]) +
						"</td><td>" + bits[157] + bits[62] + "</td><td><i style='font-size:" + iw + "px;' class='wi wi-umbrella'></i></td></tr>";
				sb.append(stmp);

				stmp = "</table><br>";
				sb.append(stmp);
			}

			sb.append(footer);

			mHandler.post(() ->
			{
				wv.loadDataWithBaseURL("file:///android_res/drawable/", sb.toString(), "text/html", "utf-8", null);
				swipeLayout.setRefreshing(false);
			});
		});

		t.start();
	}
}