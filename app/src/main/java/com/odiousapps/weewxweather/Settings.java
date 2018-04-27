package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

class Settings implements AdapterView.OnItemSelectedListener
{
    private Common common;
    private EditText et1;
	private Button b1;
	private Button b2;

    private ProgressDialog dialog;

    private static int pos;
    private static final String[] paths = {"Manual Updates", "Every 5 Minutes", "Every 10 Minutes", "Every 15 Minutes", "Every 30 Minutes", "Every Hour"};

    Settings(Common common)
    {
        this.common = common;
    }

    View mySettings(LayoutInflater inflater, ViewGroup container)
    {
        final View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        et1 = rootView.findViewById(R.id.settings);
        et1.setText(common.GetStringPref("SETTINGS_URL", "https://example.com/weewx/settings.txt"));

        Spinner s1 = rootView.findViewById(R.id.spinner1);
        ArrayAdapter<String>adapter = new ArrayAdapter<>(common.context, R.layout.spinner_layout, paths);
        adapter.setDropDownViewResource(R.layout.spinner_layout);
        s1.setAdapter(adapter);
        s1.setSelection(common.GetIntPref("updateInterval", 1));
        s1.setOnItemSelectedListener(this);

        boolean bgdl = common.GetBoolPref("bgdl", true);
        CheckBox cb1 = rootView.findViewById(R.id.cb1);
        if(!bgdl)
            cb1.setChecked(false);

        boolean metric = common.GetBoolPref("metric", true);
        CheckBox cb2 = rootView.findViewById(R.id.cb2);
        if(!metric)
            cb2.setChecked(false);

	    b2 = rootView.findViewById(R.id.refresh);
	    b2.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View arg0)
			{
				b2.setEnabled(false);
				dialog = ProgressDialog.show(common.context, "Updating data", "Please wait while we manually refresh weather content.", false);
				dialog.show();

				Thread t = new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							URL url = new URL(common.GetStringPref("BASE_URL", ""));
							HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
							urlConnection.setRequestMethod("GET");
							urlConnection.setDoOutput(true);
							urlConnection.connect();

							BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

							StringBuilder sb = new StringBuilder();
							String line;
							while ((line = in.readLine()) != null)
								sb.append(line);
							in.close();

							String radar = common.GetStringPref("RADAR_URL", "");
							if(!radar.equals(""))
							{
								Common.LogMessage("starting to download image from: " + radar);
								url = new URL(radar);

								InputStream ins = url.openStream();
								File file = new File(common.context.getFilesDir(), "/radar.gif");
								FileOutputStream out = null;

								try
								{
									out = new FileOutputStream(file);
									final byte[] b = new byte[2048];
									int length;
									while ((length = ins.read(b)) != -1)
										out.write(b, 0, length);
								} catch (Exception e) {
									e.printStackTrace();
								} finally {
									try
									{
										if (ins != null)
											ins.close();
										if (out != null)
											out.close();
									} catch (IOException e)
									{
										e.printStackTrace();
									}
								}
							}

							common.SetStringPref("LastDownload", sb.toString().trim());
							String rss = common.GetStringPref("FORECAST_URL", "");
							if(!rss.equals(""))
							{
								int curtime = Math.round(System.currentTimeMillis() / 1000);
								url = new URL(rss);
								URLConnection conn = url.openConnection();
								conn.setDoOutput(true);
								conn.connect();

								in = new BufferedReader(new InputStreamReader(url.openStream()));

								sb = new StringBuilder();
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

							String webURL = common.GetStringPref("WEBCAM_URL", "");
							if(!webURL.equals(""))
							{
								Bitmap bm;
								Common.LogMessage("starting to download bitmap from: " + webURL);
								url = new URL(webURL);
								if (webURL.endsWith("mjpeg") || webURL.endsWith("mjpg"))
								{
									MjpegRunner mr = new MjpegRunner(url);
									mr.run();

									try
									{
										while (mr.bm == null)
											Thread.sleep(1000);

										Common.LogMessage("trying to set bm");
										bm = mr.bm;
									} catch (Exception e) {
										e.printStackTrace();
										return;
									}
								} else {
									InputStream is = url.openStream();
									bm = BitmapFactory.decodeStream(is);
								}

								int width = bm.getWidth();
								int height = bm.getHeight();

								Matrix matrix = new Matrix();
								matrix.postRotate(90);

								Bitmap scaledBitmap = Bitmap.createScaledBitmap(bm, width, height, true);
								bm = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

								FileOutputStream out = null;
								File file = new File(common.context.getFilesDir(), "webcam.jpg");

								try
								{
									out = new FileOutputStream(file);
									bm.compress(Bitmap.CompressFormat.JPEG, 85, out); // bmp is your Bitmap instance
								} catch (Exception e) {
									e.printStackTrace();
								} finally {
									try
									{
										if (out != null)
											out.close();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							}

							myService.singleton.SendIntents();
						} catch (Exception e) {
							Common.LogMessage(e.toString());
						}

						handlerDone.sendEmptyMessage(0);
					}
				});

				t.start();
			}
		});

        b1 = rootView.findViewById(R.id.button);
        b1.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View arg0)
            {
                b1.setEnabled(false);
                InputMethodManager mgr = (InputMethodManager)common.context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if(mgr != null)
                    mgr.hideSoftInputFromWindow(et1.getWindowToken(), 0);

                Common.LogMessage("show dialog");
                dialog = ProgressDialog.show(common.context, "Testing submitted URLs", "Please wait while we verify the URL you submitted.", false);
                dialog.show();

                Thread t = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        boolean validURL = false;
                        boolean validURL1 = false;
                        boolean validURL2 = false;
                        boolean validURL3 = false;
                        boolean validURL4 = false;
                        boolean validURL5 = false;
                        String data = "", radar = "", forecast = "", webcam = "", custom = "";

                        CheckBox cb1 = rootView.findViewById(R.id.cb1);
                        CheckBox cb2 = rootView.findViewById(R.id.cb2);

                        if (et1.getText().toString().equals("https://example.com/weewx/settings.txt") || et1.getText().toString().equals(""))
                        {
                            handlerSettings.sendEmptyMessage(0);
                            return;
                        }

                        try
                        {
                            Common.LogMessage("checking: " + et1.getText().toString());
                            URL url = new URL(et1.getText().toString());
                            URLConnection conn = url.openConnection();
                            conn.setDoOutput(true);
                            conn.connect();

                            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = in.readLine()) != null)
                            {
                                line += "\n";
                                sb.append(line);
                            }
                            in.close();

                            String[] bits = sb.toString().replaceAll("[^\\p{ASCII}]", "").trim().split("\\n");

                            for(String bit : bits)
                            {
                                String[] mb = bit.split("=", 2);
                                if (mb[0].equals("data"))
                                    data = mb[1];
                                if (mb[0].equals("radar"))
                                    radar = mb[1];
                                if (mb[0].equals("forecast"))
                                    forecast = mb[1];
                                if (mb[0].equals("webcam"))
                                    webcam = mb[1];
                                if (mb[0].equals("custom"))
                                    custom = mb[1];
                            }

                            validURL = true;
                        } catch (MalformedURLException e)
                        {
                            e.printStackTrace();
                        } catch (IOException e)
                        {
                            e.printStackTrace();
                        } catch (Exception e)
                        {
                            e.printStackTrace();
                        }

                        if (!validURL)
                        {
                            handlerDATA.sendEmptyMessage(0);
                            return;
                        }

                        if (data.equals(""))
                        {
                            handlerDATA.sendEmptyMessage(0);
                            return;
                        }

                        try
                        {
                            Common.LogMessage("checking: " + data);
                            URL url = new URL(data);
                            URLConnection conn = url.openConnection();
                            conn.connect();
                            InputStream in = conn.getInputStream();
                            in.close();
                            validURL1 = true;
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (!validURL1)
                        {
                            handlerDATA.sendEmptyMessage(0);
                            return;
                        }

                        if (!radar.equals(""))
                        {
                            try
                            {
                                Common.LogMessage("checking: " + radar);
                                URL url = new URL(radar);
                                URLConnection conn = url.openConnection();
                                conn.connect();
                                InputStream in = conn.getInputStream();
                                in.close();
                                validURL2 = true;
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if(!validURL2)
                            {
                                handlerRADAR.sendEmptyMessage(0);
                                return;
                            }
                        }

                        if (!forecast.equals(""))
                        {
                            try
                            {
                                forecast = URLEncoder.encode(forecast, "utf-8");
                                Common.LogMessage("forecast=" + forecast);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if(cb2.isChecked())
                                forecast = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20woeid%20in%20(select%20woeid%20from%20geo.places(1)%20where%20text%3D%22" + forecast + "%22)%20and%20u%3D'c'&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
                            else
                                forecast = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20woeid%20in%20(select%20woeid%20from%20geo.places(1)%20where%20text%3D%22" + forecast + "%22)%20and%20u%3D'f'&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";

                            Common.LogMessage("forecast checking: "+forecast);
                            try
                            {
                                Common.LogMessage("checking: " + forecast);
                                URL url = new URL(forecast);
                                URLConnection conn = url.openConnection();
                                conn.connect();
                                InputStream in = conn.getInputStream();
                                in.close();
                                validURL3 = true;
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if(!validURL3)
                            {
                                handlerForecast.sendEmptyMessage(0);
                                return;
                            }
                        }

                        if(!webcam.equals(""))
                        {
                            try
                            {
                                Common.LogMessage("checking: " + webcam);
                                URL url = new URL(webcam);
                                URLConnection conn = url.openConnection();
                                conn.connect();
                                InputStream in = conn.getInputStream();
                                in.close();
                                validURL4 = true;
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if(!validURL4)
                            {
                                handlerWEBCAM.sendEmptyMessage(0);
                                return;
                            }
                        }

                        if (!custom.equals("") && !custom.equals("https://example.com/mobile.html"))
                        {
                            try
                            {
                                Common.LogMessage("checking: " + custom);
                                URL url = new URL(custom);
                                URLConnection conn = url.openConnection();
                                conn.connect();
                                InputStream in = conn.getInputStream();
                                in.close();
                                validURL5 = true;
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if(!validURL5)
                            {
                                handlerCUSTOM.sendEmptyMessage(0);
                                return;
                            }
                        } else
                            custom = "";

                        common.SetStringPref("SETTINGS_URL", et1.getText().toString());
                        common.SetIntPref("updateInterval", pos);
                        common.SetStringPref("BASE_URL", data);
                        common.SetStringPref("RADAR_URL", radar);
                        common.SetStringPref("FORECAST_URL", forecast);
                        common.SetBoolPref("bgdl", cb1.isChecked());
                        common.SetStringPref("WEBCAM_URL", webcam);
                        common.SetStringPref("CUSTOM_URL", custom);
                        common.SetBoolPref("metric", cb2.isChecked());

                        myService.singleton.stopTimer();
                        myService.singleton.startTimer();

                        handlerDone.sendEmptyMessage(0);
                    }
                });

                t.start();
            }
        });

        return rootView;
    }

    @SuppressLint("HandlerLeak")
    private Handler handlerDone = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            Common.LogMessage("sending intents");
            dialog.dismiss();
            Intent intent = new Intent();
            intent.putExtra("urlChanged", true);
            intent = new Intent();
            intent.setAction(myService.TAB0_INTENT);
            common.context.sendBroadcast(intent);
            Common.LogMessage("sent intents");
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handlerSettings = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            b1.setEnabled(true);
            dialog.dismiss();
            new AlertDialog.Builder(common.context)
                .setTitle("Invalid URL")
                .setMessage("Wasn't able to connect or download settings.txt from your server")
                .setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                    }
                }).show();
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handlerDATA = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            b1.setEnabled(true);
            dialog.dismiss();
            new AlertDialog.Builder(common.context)
                    .setTitle("Invalid URL")
                    .setMessage("Wasn't able to connect or download data.txt from your server")
                    .setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                        }
                    }).show();
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handlerRADAR = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            b1.setEnabled(true);
            dialog.dismiss();
            new AlertDialog.Builder(common.context)
                    .setTitle("Invalid URL")
                    .setMessage("Wasn't able to connect or download radar= image from the internet")
                    .setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                        }
                    }).show();
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handlerForecast = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            b1.setEnabled(true);
            dialog.dismiss();
            new AlertDialog.Builder(common.context)
                    .setTitle("Invalid URL")
                    .setMessage("Wasn't able to connect or download the forecast from Yahoo.")
                    .setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                        }
                    }).show();
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handlerWEBCAM = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            b1.setEnabled(true);
            dialog.dismiss();
            new AlertDialog.Builder(common.context)
                    .setTitle("Invalid URL")
                    .setMessage("Wasn't able to connect or download a webcam image from your server")
                    .setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                        }
                    }).show();
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handlerCUSTOM = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            b1.setEnabled(true);
            dialog.dismiss();
            new AlertDialog.Builder(common.context)
                    .setTitle("Invalid URL")
                    .setMessage("Wasn't able to connect or download from the custom URL specified")
                    .setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                        }
                    }).show();
        }
    };

    public void onItemSelected(AdapterView<?> parent, View v, int position, long id)
    {
        pos = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView)
    {
    }
}