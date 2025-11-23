package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;

import org.conscrypt.Conscrypt;

import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.TlsVersion;

@SuppressWarnings({"unused", "SameParameterValue"})
class NetworkClient
{
	final static String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";

	private static OkHttpClient clientInstance = null;

	static
	{
		try
		{
			Context context = weeWXApp.getInstance();

			if(Security.getProvider("Conscrypt") == null)
				Security.insertProviderAt(Conscrypt.newProvider(), 1);

			// Create a trust manager that does not validate certificate chains
			@SuppressLint("CustomX509TrustManager")
			final TrustManager[] trustAllCerts = new TrustManager[]
			{
				new X509TrustManager()
				{
					@SuppressLint("TrustAllX509TrustManager")
					@Override
					public void checkClientTrusted(X509Certificate[] chain, String authType)
					{
					}

					@SuppressLint("TrustAllX509TrustManager")
					@Override
					public void checkServerTrusted(X509Certificate[] chain, String authType)
					{
					}

					@Override
					public X509Certificate[] getAcceptedIssuers()
					{
						return new X509Certificate[0];
					}
				}
			};

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustAllCerts, new SecureRandom());
			final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

			// Define connection spec with TLS 1.3 first, fallback to 1.2
			ConnectionSpec modernTLS = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
					.tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
					.allEnabledCipherSuites()
					.build();

			clientInstance = new OkHttpClient.Builder()
					.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0])
					.hostnameVerifier((hostname, session) -> true)
					.connectionSpecs(Arrays.asList(modernTLS, ConnectionSpec.CLEARTEXT))
					.connectTimeout(60, TimeUnit.SECONDS)
					.writeTimeout(60, TimeUnit.SECONDS)
					.readTimeout(60, TimeUnit.SECONDS)
					.build();

		} catch(Exception e) {
			weeWXAppCommon.LogMessage("Error! e: " + e, true);
		}
	}

	private static OkHttpClient.Builder newInstance()
	{
		return clientInstance.newBuilder();
	}

	static OkHttpClient getInstance(String url)
	{
		OkHttpClient.Builder newClient = newInstance();

		if(url == null || url.isBlank() || !url.startsWith("http"))
			return newClient.build();

		StackTraceElement caller = new Exception().getStackTrace()[1];
		String callerClass  = caller.getClassName();
		String callerMethod = caller.getMethodName();

		// windy.com is very noisy... 2s connectivity checks is beyond excessive...
		if(!url.contains("windy.com"))
			weeWXAppCommon.LogMessage("NetworkClient.java getInstance() " + callerClass +
			                          "." + callerMethod + ", url: " + url, true);

		Uri uri = Uri.parse(url);
		if(uri.getUserInfo() == null || !uri.getUserInfo().contains(":"))
			return newClient.build();

		String[] UC = uri.getUserInfo().split(":");
		String credentials = Credentials.basic(UC[0], UC[1]);
		return newClient.authenticator((route, response) ->
				{
					if(responseCount(response) >= 3)
						return null;
					return response.request().newBuilder().header("Authorization", credentials).build();
				}).build();
	}

	static OkHttpClient getStream(String url)
	{
		OkHttpClient.Builder newClient = newInstance();

		if(url == null || url.isBlank() || !url.startsWith("http"))
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
		if(url == null || url.isBlank() || !url.startsWith("http"))
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