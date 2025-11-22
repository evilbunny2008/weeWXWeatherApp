package com.odiousapps.weewxweather;

import android.net.Uri;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionSpec;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings({"unused", "SameParameterValue"})
class NetworkClient
{
	final static String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";

	private static final OkHttpClient clientInstance = new OkHttpClient.Builder()
			.connectTimeout(60, TimeUnit.SECONDS)
			.writeTimeout(60, TimeUnit.SECONDS)
			.readTimeout(60, TimeUnit.SECONDS)
			.connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS))
			.build();

	private static OkHttpClient.Builder newInstance()
	{
		return clientInstance.newBuilder();
	}

	static OkHttpClient getInstance(String url)
	{
		OkHttpClient.Builder newClient = newInstance();

		if(url == null || url.isBlank())
			return newClient.build();

		weeWXAppCommon.LogMessage("getInstance(), url: " + url);

		Uri uri = Uri.parse(url);
		if(uri.getUserInfo() != null && uri.getUserInfo().contains(":"))
		{
			String[] UC = uri.getUserInfo().split(":");
			String credentials = Credentials.basic(UC[0], UC[1]);
			return newClient.authenticator((route, response) ->
					{
						if(responseCount(response) >= 3)
							return null;
						return response.request().newBuilder().header("Authorization", credentials).build();
					}).build();
		}

		return newClient.build();
	}

	static OkHttpClient getStream(String url)
	{
		OkHttpClient.Builder newClient = newInstance();

		if(url == null || url.isBlank())
			return newClient.build();

		weeWXAppCommon.LogMessage("getStream(), url: " + url);

		Uri uri = Uri.parse(url);
		if(uri.getUserInfo() != null && uri.getUserInfo().contains(":"))
		{
			String[] UC = uri.getUserInfo().split(":");
			String credentials = Credentials.basic(UC[0], UC[1]);
			return newClient.readTimeout(0, TimeUnit.MILLISECONDS)
					.authenticator((route, response) ->
					{
						if(responseCount(response) >= 3)
							return null;
						return response.request().newBuilder().header("Authorization", credentials).build();
					}).build();
		}

		return newClient.build();
	}

	private static int responseCount(Response response)
	{
		int result = 1;

		while((response = response.priorResponse()) != null)
			result++;

		return result;
	}

	static Request getRequest(boolean doHead, String url)
	{
		if(url == null || url.isBlank())
			return null;

		weeWXAppCommon.LogMessage("newRequest(), url: " + url);

		Request.Builder req = new Request.Builder()
				.header("User-Agent", UA)
				.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
				.header("Cache-Control", "max-age=0")
				.header("Accept-Language", "en")
				.header("Upgrade-Insecure-Requests", "1")
				.header("Referer", url)
				.url(url);

		if(doHead)
			req.head();

		return req.build();

	}
}