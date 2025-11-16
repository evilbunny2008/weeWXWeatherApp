package com.odiousapps.weewxweather;

import android.net.Uri;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionSpec;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings({"unused", "SameParameterValue"})
class NetworkClient
{
	private static String URL;

	static OkHttpClient.Builder newInstance()
	{
		return new OkHttpClient.Builder()
				.connectTimeout(60, TimeUnit.SECONDS)
				.writeTimeout(60, TimeUnit.SECONDS)
				.readTimeout(60, TimeUnit.SECONDS)
				.connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS));
	}

	static OkHttpClient getInstance(String url)
	{
		if(url == null || url.isBlank())
			return newInstance().build();

		weeWXAppCommon.LogMessage("getInstance(), url: " + url);

		URL = url;

		Uri uri = Uri.parse(url);
		if(uri.getUserInfo() != null && uri.getUserInfo().contains(":"))
		{
			String[] UC = uri.getUserInfo().split(":");
			String credentials = Credentials.basic(UC[0], UC[1]);
			return newInstance()
					.addInterceptor(myInterceptor)
					.authenticator((route, response) ->
					{
						if(responseCount(response) >= 3)
							return null;
						return response.request().newBuilder().header("Authorization", credentials).build();
					}).build();
		}

		return newInstance().addInterceptor(myInterceptor).build();
	}

	static OkHttpClient getStream(String url)
	{
		if(url == null || url.isBlank())
			return newInstance().build();

		weeWXAppCommon.LogMessage("getStream(), url: " + url);

		URL = url;

		Uri uri = Uri.parse(url);
		if(uri.getUserInfo() != null && uri.getUserInfo().contains(":"))
		{
			String[] UC = uri.getUserInfo().split(":");
			String credentials = Credentials.basic(UC[0], UC[1]);
			return newInstance()
					.readTimeout(0, TimeUnit.MILLISECONDS)
					.addInterceptor(myInterceptor)
					.authenticator((route, response) ->
					{
						if(responseCount(response) >= 3)
							return null;
						return response.request().newBuilder().header("Authorization", credentials).build();
					}).build();
		}

		return newInstance().build();
	}

	private static int responseCount(Response response)
	{
		int result = 1;

		while((response = response.priorResponse()) != null)
			result++;

		return result;
	}

	private static final Interceptor myInterceptor = chain ->
	{
		Request.Builder builder = chain.request()
				.newBuilder()
				.header("User-Agent", weeWXAppCommon.UA)
				.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
				.header("Cache-Control", "max-age=0")
				.header("Accept-Language", "en-au")
				.header("Upgrade-Insecure-Requests", "1");

		if(URL.length() > 0)
			builder.header("Referer", URL);

		return chain.proceed(builder.build());
	};

	static Request newRequest(String url)
	{
		if(url == null || url.isBlank())
			return null;

		weeWXAppCommon.LogMessage("newRequest(), url: " + url);

		URL = url;

		return new Request.Builder()
				.url(URL)
				.build();
	}

	static Request newRequest(boolean getHead, String url)
	{
		if(url == null || url.isBlank())
			return null;

		weeWXAppCommon.LogMessage("newRequest(), url: " + url);

		URL = url;

		return new Request.Builder()
				.url(URL)
				.head()
				.build();
	}
}