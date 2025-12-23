package com.odiousapps.weewxweather;

import com.caverock.androidsvg.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"unused", "SameParameterValue", "ApplySharedPref", "ConstantConditions",
                   "SameReturnValue", "BooleanMethodIsAlwaysInverted", "SetTextI18n", "StringBufferMayBeStringBuilder"})
class JsoupHelper
{
	record Result(List<Day> days, String desc, long timestamp) {}

	static Result processYahoo(String data)
	{
		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		List<Day> days = new ArrayList<>();
		long timestamp = weeWXAppCommon.getRSSms();
		String desc = "";
		JSONArray jarr;
		JSONObject jobj;

		Document doc = Jsoup.parse(data);

		if(false)
		{
			try(ExecutorService executor = Executors.newSingleThreadExecutor())
			{
				Future<?> backgroundTask = executor.submit(() ->
				{
					String base_url = "https://odiousapps.com/yahoosvg.php";

					Elements svgs = doc.select("svg");

					for(Element svg: svgs)
					{
						if(svg == null || svg.outerHtml().isBlank())
							continue;

						if(!svg.hasAttr("aria-label"))
							continue;

						String filenameOrig = svg.attr("aria-label");

						if(filenameOrig == null || filenameOrig.isBlank() || filenameOrig.equals("Yahoo Weather"))
							continue;

						filenameOrig = filenameOrig.replaceAll(" ", "_");

						int height = 0;

						if(svg.hasAttr("height"))
							height = (int)Float.parseFloat(svg.attr("height"));

						int width = 0;
						if(svg.hasAttr("width"))
							width = (int)Float.parseFloat(svg.attr("width"));

						if(width > 0 && height > 0)
							filenameOrig += "_" + height + "x" + width;

						boolean hasDark = false;
						boolean hasLight = false;

						Element lightSVG = svg.clone();
						if(!lightSVG.select("g.uds-light-mode-icon").isEmpty())
						{
							hasLight = true;
							lightSVG.select("g.uds-dark-mode-icon").remove();
						}

						Element darkSVG = svg.clone();
						if(!darkSVG.select("g.uds-dark-mode-icon").isEmpty())
						{
							hasDark = true;
							darkSVG.select("g.uds-light-mode-icon").remove();
						}

						try
						{
							if(hasLight)
							{
								boolean alreadyWrittenLight = false;
								String outputLight = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
								                     weeWXAppCommon.convertRGB2Hex(lightSVG.outerHtml()) + "\n";
								String filenameLight = null;
								for(int i = 0; i < 10; i++)
								{
									filenameLight = "yahoo" + "_" + filenameOrig + "_" + i + "_light.svg";

									String content = weeWXApp.loadFileFromAssets("icons/yahoo/" + filenameLight);

									if(content == null || content.isBlank())
										break;

									if(content.strip().equals(outputLight.strip()))
									{
										alreadyWrittenLight = true;
										break;
									}
								}

								if(filenameLight != null && !filenameLight.isBlank() && !alreadyWrittenLight)
								{
									Map<String, String> args = Map.of(
											"svgName", filenameLight,
											"svgHeight", "" + height,
											"svgWidth", "" + width,
											"svg", outputLight,
											"appName", BuildConfig.APPLICATION_ID,
											"appVersion", BuildConfig.VERSION_NAME);

									weeWXAppCommon.uploadString(base_url, args);
								}
							}

							if(hasDark)
							{
								boolean alreadyWrittenDark = false;
								String outputDark = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
								                    weeWXAppCommon.convertRGB2Hex(darkSVG.outerHtml()) + "\n";
								String filenameDark = null;
								for(int i = 0; i < 10; i++)
								{
									filenameDark = "yahoo" + "_" + filenameOrig + "_" + i + "_dark.svg";

									String content = weeWXApp.loadFileFromAssets("icons/yahoo/" + filenameDark);

									if(content == null || content.isBlank())
										break;

									if(content.strip().equals(outputDark.strip()))
									{
										alreadyWrittenDark = true;
										break;
									}
								}

								if(filenameDark != null && !filenameDark.isBlank() && !alreadyWrittenDark)
								{
									Map<String, String> args = Map.of(
											"svgName", filenameDark,
											"svgHeight", "" + height,
											"svgWidth", "" + width,
											"svg", outputDark,
											"appName", BuildConfig.APPLICATION_ID,
											"appVersion", BuildConfig.VERSION_NAME);


									weeWXAppCommon.uploadString(base_url, args);
								}
							}

							if(!hasLight && !hasDark)
							{
								boolean alreadyWritten = false;
								String output = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
								                weeWXAppCommon.convertRGB2Hex(svg.outerHtml()) + "\n";
								String filename = null;
								for(int i = 0; i < 10; i++)
								{
									filename = "yahoo" + "_" + filenameOrig + "_" + i + ".svg";

									String content = weeWXApp.loadFileFromAssets("icons/yahoo/" + filename);

									if(content == null || content.isBlank())
										break;

									if(content.strip().equals(output.strip()))
									{
										alreadyWritten = true;
										break;
									}
								}

								if(filename != null && !filename.isBlank() && !alreadyWritten)
								{
									Map<String, String> args = Map.of(
											"svgName", filename,
											"svgHeight", "" + height,
											"svgWidth", "" + width,
											"svg", output,
											"appName", BuildConfig.APPLICATION_ID,
											"appVersion", BuildConfig.VERSION_NAME);

									weeWXAppCommon.uploadString(base_url, args);
								}
							}

							//publish(file);
						} catch(Exception e) {
							weeWXAppCommon.doStackOutput(e);
						}
					}
				});
			} catch(Exception e) {
				weeWXAppCommon.LogMessage("Error! e: " + e.getMessage(), true, KeyValue.e);
				weeWXAppCommon.doStackOutput(e);
			}
		}

		Pattern p = Pattern.compile("self.__next_f\\.push\\(\\[\\d+,\"(.*)\"]\\)", Pattern.DOTALL);

		try
		{
			String forecastStr = null;
			Elements scripts = doc.select("script");

			for(Element element : scripts)
			{
				Matcher m = p.matcher(element.data());
				if(!m.find())
					continue;

				String payload = m.group(1);

				if(payload == null)
					continue;

				String unescaped = payload.replace("\\\"", "\"").replace("\\\\", "\\");

				int colon = unescaped.indexOf(':');
				if(colon < 1)
					continue;

				String jsonPart = unescaped.substring(colon + 1).strip();
				String before = unescaped.substring(0, colon).strip();
				if(before.equals("9"))
				{
					try
					{
						jobj = new JSONObject(jsonPart);
						jarr = jobj.getJSONArray("metadata");
						for(int i = 0; i < jarr.length(); i++)
						{
							if(!jarr.getJSONArray(1).getString(2).equals("1"))
								continue;

							forecastStr = jarr.getJSONArray(1).getJSONObject(3).getString("content");

							boolean isFahrenheit = forecastStr.contains("°F");

							if(metric && isFahrenheit)
							{
								Pattern TEMP_F = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*°F");

								Matcher m2 = TEMP_F.matcher(forecastStr);

								StringBuffer sb = new StringBuffer();

								while(m2.find())
								{
									int f = (int)Float.parseFloat(m2.group(1));
									String c = weeWXAppCommon.F2Cdeg(f);
									m2.appendReplacement(sb, c);
								}

								m2.appendTail(sb);

								forecastStr = sb.toString();
							}

							if(!metric && !isFahrenheit)
							{
								Pattern TEMP_C = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*°C");

								Matcher m2 = TEMP_C.matcher(forecastStr);

								if(m2 == null)
									continue;

								StringBuffer sb = new StringBuffer();

								while(m2.find())
								{
									int c = (int)Float.parseFloat(m2.group(1));
									String f = weeWXAppCommon.C2Fdeg(c);
									m2.appendReplacement(sb, f);
								}

								m2.appendTail(sb);

								forecastStr = sb.toString();
							}
						}
					} catch(Exception e) {
						weeWXAppCommon.LogMessage("Error! e: " + e.getMessage(), true, KeyValue.e);
						weeWXAppCommon.doStackOutput(e);
					}

					continue;
				}

				if(!before.equals("5"))
					continue;

				try
				{
					jarr = new JSONArray(jsonPart);

					jarr = jarr.getJSONObject(3).getJSONArray("children").getJSONArray(4)
							.getJSONObject(3).getJSONArray("children").getJSONArray(2)
							.getJSONObject(3).getJSONArray("children").getJSONArray(1)
							.getJSONArray(1).getJSONObject(3).getJSONArray("dailyForecasts");

					for(int i = 0; i < jarr.length(); i++)
					{
						Day day = new Day();

						jobj = jarr.getJSONObject(i);

						day.timestamp = System.currentTimeMillis() + (86_400_000L * i);

						day.day = weeWXAppCommon.sdf2.format(day.timestamp);

						if(i == 0)
							day.text = forecastStr;

						if(metric)
						{
							if(jobj.getString("unit").equals("°F"))
							{
								try
								{
									day.max = weeWXAppCommon.F2Cdeg(jobj.getInt("highTemperature"));
								} catch(Exception ignored) {}

								try
								{
									day.min = weeWXAppCommon.F2Cdeg(jobj.getInt("lowTemperature"));
								} catch(Exception ignored) {}

							} else {

								try
								{
									day.max = jobj.getInt("highTemperature") + "&deg;C";
								} catch(Exception ignored) {}

								try
								{
									day.min = jobj.getInt("lowTemperature") + "&deg;C";
								} catch(Exception ignored) {}
							}
						} else {
							if(jobj.getString("unit").equals("°F"))
							{
								try
								{
									day.max = jobj.getInt("highTemperature") + "&deg;F";
								} catch(Exception ignored) {}

								try
								{
									day.min = jobj.getInt("lowTemperature") + "&deg;F";
								} catch(Exception ignored) {}
							} else {
								try
								{
									day.max = weeWXAppCommon.C2Fdeg(jobj.getInt("highTemperature"));
								} catch(Exception ignored) {}

								try
								{
									day.min = weeWXAppCommon.F2Cdeg(jobj.getInt("lowTemperature"));
								} catch(Exception ignored) {}
							}
						}

						String content = null;
						String[] options = new String[]{"32x32_1", "32x32_0", "24x24_0"};

						for(String option : options)
						{
							day.icon = "icons/yahoo/yahoo_" + jobj.getString("iconLabel")
									.replaceAll(" ", "_") + "_" + option + "_light.svg";
							content = weeWXApp.loadFileFromAssets(day.icon);
							if(content != null && !content.isBlank())
							{
								day.icon = "file:///android_asset/" + day.icon;
								break;
							}
						}

						if(content == null || content.isBlank())
						{
							if(day.icon != null && !day.icon.isBlank())
							{
								String base_url = "https://odiousapps.com/yahoo-missing-svg.php";

								String forecaseURL = (String)KeyValue.readVar("FORECAST_URL", null);

								weeWXAppCommon.LogMessage("Unable to locate SVG: " + day.icon, KeyValue.d);
								weeWXAppCommon.uploadString(base_url, Map.of(
										"svgMissingName", day.icon,
										"svgMissingURL", forecaseURL,
										"appName", BuildConfig.APPLICATION_ID,
										"appVersion", BuildConfig.VERSION_NAME));
							}

							day.icon = null;
						}

						days.add(day);
					}
				} catch(Exception e) {
					weeWXAppCommon.doStackOutput(e);
				}
			}

			return new Result(days, desc, timestamp);
		} catch(Exception e) {
			weeWXAppCommon.doStackOutput(e);
			return null;
		}
	}

	static Result processWCAF(String data)
	{
		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		String desc;
		List<Day> days = new ArrayList<>();
		long timestamp, lastTS;

		try
		{
			String obs = data.split("Prévisions émises à : ", 2)[1].strip();
			obs = obs.split("</span>", 2)[0].strip();

			int i = 0, j = obs.indexOf("h");
			String hour = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String minute = obs.substring(i, j);

			String[] bits = obs.split(" ");
			String date = bits[bits.length - 3];
			String month = bits[bits.length - 2];
			String year = bits[bits.length - 1];

			obs = hour + ":" + minute + " " + date + " " + month + " " + year;

			lastTS = timestamp = 0;
			Date df = weeWXAppCommon.sdf9.parse(obs);
			if(df != null)
				lastTS = timestamp = df.getTime();

			if(timestamp > 0)
				weeWXAppCommon.updateCacheTime(timestamp);

			desc = data.split("<dt>Enregistrées à :</dt>", 2)[1].split("<dd class='mrgn-bttm-0'>")[1].split("</dd>")[0].strip();

			data = data.split("<div class='div-table'>", 2)[1].strip();
			data = data.split("<section><details open='open' class='wxo-detailedfore'>")[0].strip();
			data = data.substring(0, data.length() - 7).strip();

			bits = data.split("<div class='div-column'>");

			for(i = 1; i < bits.length; i++)
			{
				Document doc = Jsoup.parse(bits[i].strip());
				Elements div = doc.select("div");
				for (j = 0; j < div.size(); j++)
				{
					Day day = new Day();
					String text = "", img_url = "", temp = "", pop = "";

					if(div.get(j).className().contains("greybkgrd"))
					{
						j++;
						continue;
					}

					if(div.get(j).toString().contains("div-row-head"))
					{
						if(div.get(j).select("div").html().contains("Ce soir et cette nuit"))
						{
							date = "Cette nuit";
							day.timestamp = lastTS;
						} else if(div.get(j).select("div").html().contains("Nuit")) {
							date = "Nuit";
							day.timestamp = lastTS;
						} else {
							date = div.get(j).html().split("<strong title='", 2)[1].split("'>", 2)[0].strip();
							day.timestamp = weeWXAppCommon.convertDaytoTS(date, Locale.CANADA_FRENCH, lastTS);
						}
					} else
						continue;

					j++;

					if(j >= div.size())
					{
						weeWXAppCommon.LogMessage("continue");
						continue;
					}

					try
					{
						if(div.outerHtml().contains("div-row-data"))
						{
							if(metric)
								temp = div.get(j).select("div").select("span").html().split("<abbr")[0].strip() + "C";
							else
								temp = div.get(j).select("div").select("span").html().split("</abbr>")[1].split("<abbr")[0].strip() + "F";
							text = div.get(j).select("div").select("img").outerHtml().split("alt='", 2)[1].split("'", 2)[0].strip();
							img_url = "https://www.weather.gc.ca" + div.get(j).select("div").select("img").outerHtml().split("src='", 2)[1].split("'", 2)[0].strip();
							pop = div.get(j).select("div").select("small").html().strip();
						}
					} catch(Exception e) {
						weeWXAppCommon.LogMessage("hmmm 2 == " + div.html(), KeyValue.e);
						weeWXAppCommon.doStackOutput(e);
					}

					String fileName = "wca" + img_url.substring(img_url.lastIndexOf('/') + 1)
							.replaceAll("\\.gif$", "") + ".png";

					day.icon = "file:///android_asset/icons/" + fileName;

					day.day = date;
					day.max = temp;
					day.text = text;
					day.min = pop;
					days.add(day);

					lastTS = day.timestamp;
				}
			}

			return new Result(days, desc, timestamp);
		} catch(Exception e) {
			weeWXAppCommon.doStackOutput(e);
			return null;
		}
	}

	static Result processWCA(String data)
	{
		boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
		String desc;
		List<Day> days = new ArrayList<>();
		long timestamp, lastTS;

		try
		{
			String obs = data.split("Forecast issued: ", 2)[1].strip();
			obs = obs.split("</span>", 2)[0].strip();

			int i = 0, j = obs.indexOf(":");
			String hour = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String minute = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String am_pm = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			//String TZ = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			//String DOW = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String date = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String month = obs.substring(i, j);
			i = j + 1;
			j = obs.length();
			String year = obs.substring(i, j);

			obs = hour + ":" + minute + " " + am_pm + " " + date + " " + month + " " + year;

			lastTS = timestamp = 0;
			Date df = weeWXAppCommon.sdf3.parse(obs);
			if(df != null)
				lastTS = timestamp = df.getTime();

			if(timestamp > 0)
				weeWXAppCommon.updateCacheTime(timestamp);

			desc = data.split("<dt>Observed at:</dt>", 2)[1].split("<dd class='mrgn-bttm-0'>")[1].split("</dd>")[0].strip();

			data = data.split("<div class='div-table'>", 2)[1].strip();
			data = data.split("<section><details open='open' class='wxo-detailedfore'>")[0].strip();
			data = data.substring(0, data.length() - 7).strip();

			String[] bits = data.split("<div class='div-column'>");

			for(i = 1; i < bits.length; i++)
			{
				Document doc = Jsoup.parse(bits[i].strip());
				Elements div = doc.select("div");
				for (j = 0; j < div.size(); j++)
				{
					String text = "", img_url = "", temp = "", pop = "";
					Day day = new Day();

					if(div.get(j).className().contains("greybkgrd"))
					{
						j++;
						continue;
					}

					if(div.get(j).toString().contains("div-row-head"))
					{
						if(div.get(j).select("div").html().contains("Tonight"))
						{
							date = "Tonight";
							day.timestamp = lastTS;
						} else if(div.get(j).select("div").html().contains("Night")) {
							date = "Night";
							day.timestamp = lastTS;
						} else {
							date = div.get(j).html().split("<strong title='", 2)[1].split("'>", 2)[0].strip();
							day.timestamp = weeWXAppCommon.convertDaytoTS(date, Locale.CANADA, lastTS);
						}
					} else
						continue;

					j++;

					if(j >= div.size())
					{
						weeWXAppCommon.LogMessage("continue");
						continue;
					}

					try
					{
						if(div.outerHtml().contains("div-row-data"))
						{
							if(metric)
								temp = div.get(j).select("div").select("span").html().split("<abbr")[0].strip() + "C";
							else
								temp = div.get(j).select("div").select("span").html().split("</abbr>")[1].split("<abbr")[0].strip() + "F";
							text = div.get(j).select("div").select("img").outerHtml().split("alt='", 2)[1].split("'", 2)[0].strip();
							img_url = "https://www.weather.gc.ca" + div.get(j).select("div").select("img").outerHtml().split("src='", 2)[1].split("'", 2)[0].strip();
							pop = div.get(j).select("div").select("small").html().strip();
						}
					} catch(Exception e) {
						weeWXAppCommon.LogMessage("hmmm 2 == " + div.html(), KeyValue.e);
						weeWXAppCommon.doStackOutput(e);
					}

					String fileName = img_url.substring(img_url.lastIndexOf('/') + 1).replaceAll("\\.gif$", "");

					fileName = "wca" + fileName + ".png";

					day.icon = "file:///android_asset/icons/" + fileName;

					day.day = date;
					day.max = temp;
					day.text = text;
					day.min = pop;

					days.add(day);

					lastTS = day.timestamp;
				}
			}

			return new Result(days, desc, timestamp);
		} catch(Exception e) {
			weeWXAppCommon.doStackOutput(e);
		}

		return null;
	}

	static Result processBoM2(String data)
	{
		try
		{
			boolean metric = (boolean)KeyValue.readVar("metric", weeWXApp.metric_default);
			List<Day> days = new ArrayList<>();
			long timestamp = 0;

			Document doc = Jsoup.parse(data);
			String desc = doc.title().split(" - Bureau of Meteorology")[0].strip();
			String fcdiv = doc.select("div.forecasts").html();
			String obs = doc.select("span").html().split("issued at ")[1].split("\\.", 2)[0].strip();

			int i = 0, j = obs.indexOf(":");
			String hour = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String minute = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String am_pm = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			//String TZ = obs.substring(i, j);
			i = j + 5;
			j = obs.indexOf(" ", i);
			//String DOW = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String date = obs.substring(i, j);
			i = j + 1;
			j = obs.indexOf(" ", i);
			String month = obs.substring(i, j);
			i = j + 1;
			j = obs.length();
			String year = obs.substring(i, j);

			obs = hour + ":" + minute + " " + am_pm + " " + date + " " + month + " " + year;

			Date df = weeWXAppCommon.sdf3.parse(obs);
			if(df != null)
				timestamp = df.getTime();

			if(timestamp > 0)
				weeWXAppCommon.updateCacheTime(timestamp);

			String[] bits = fcdiv.split("<dl class='forecast-summary'>");
			String bit = bits[1];
			Day day = new Day();

			day.day = bit.split("<a href='", 2)[1].split("'>", 2)[0].split("/forecast/detailed/#d", 2)[1].strip();

			day.timestamp = 0;
			df = weeWXAppCommon.sdf4.parse(day.day);
			if(df != null)
				day.timestamp = df.getTime();

			day.day = weeWXAppCommon.sdf2.format(day.timestamp);

			day.icon = "https://reg.bom.gov.au" + bit.split("<img src='", 2)[1].split("' alt='", 2)[0].strip();

			if(bit.contains("<dd class='max'>"))
				day.max = bit.split("<dd class='max'>")[1].split("</dd>")[0].strip();

			if(bit.contains("<dd class='min'>"))
				day.min = bit.split("<dd class='min'>")[1].split("</dd>")[0].strip();

			day.text = bit.split("<dd class='summary'>")[1].split("</dd>")[0].strip();

			String fileName = "bom2" + day.icon.substring(day.icon.lastIndexOf('/') + 1).replaceAll("-", "_");

			day.icon = "file:///android_asset/icons/" + fileName;

			day.max = day.max.replaceAll("°C", "").strip();
			day.min = day.min.replaceAll("°C", "").strip();

			if(metric)
			{
				day.max += "&deg;C";
				day.min += "&deg;C";
			} else {
				if(!day.max.isBlank())
					day.max += weeWXAppCommon.C2Fdeg((int)Float.parseFloat(day.max));
				if(!day.min.isBlank())
					day.min += weeWXAppCommon.C2Fdeg((int)Float.parseFloat(day.min));
			}

			if(day.max.isBlank() || day.max.startsWith("&deg;"))
				day.max = "N/A";

			days.add(day);

			for(i = 2; i < bits.length; i++)
			{
				day = new Day();
				bit = bits[i];
				day.day = bit.split("<a href='", 2)[1].split("'>", 2)[0].split("/forecast/detailed/#d", 2)[1].strip();

				day.timestamp = 0;
				df = weeWXAppCommon.sdf1.parse(day.day);
				if(df != null)
					day.timestamp = df.getTime();

				day.day = weeWXAppCommon.sdf2.format(day.timestamp);

				day.icon = "https://reg.bom.gov.au" + bit.split("<img src='", 2)[1].split("' alt='", 2)[0].strip();
				day.max = bit.split("<dd class='max'>")[1].split("</dd>")[0].strip();
				day.min = bit.split("<dd class='min'>")[1].split("</dd>")[0].strip();
				day.text = bit.split("<dd class='summary'>")[1].split("</dd>")[0].strip();

				fileName = "bom2" + day.icon.substring(day.icon.lastIndexOf('/') + 1).replaceAll("-", "_");

				day.icon = "file:///android_asset/icons/" + fileName;

				day.max = day.max.replaceAll("°C", "").strip() + "&deg;C";
				day.min = day.min.replaceAll("°C", "").strip() + "&deg;C";

				if(!metric)
				{
					day.max = weeWXAppCommon.C2Fdeg((int)Float.parseFloat(day.max));
					day.min = weeWXAppCommon.C2Fdeg((int)Float.parseFloat(day.min));
				}

				days.add(day);
			}

			return new Result(days, desc, timestamp);
		} catch(Exception e) {
			weeWXAppCommon.doStackOutput(e);
		}

		return null;
	}
}
