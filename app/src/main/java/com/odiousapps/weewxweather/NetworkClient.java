package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;

import java.security.SecureRandom;
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

import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;

@SuppressWarnings({"unused", "SameParameterValue"})
class NetworkClient
{
	final static String[] UAstrings = {
		"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
		"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.3124.85",
		"Mozilla/5.0 (Windows NT 10.0; Win64; x64; Xbox; Xbox One) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edge/44.18363.8131",
		"Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0",
		"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 OPR/118.0.0.0",
		"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
		"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.3124.85",
		"Mozilla/5.0 (Macintosh; Intel Mac OS X 14_7_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3 Safari/605.1.15",
		"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
		"Mozilla/5.0 (X11; Linux x86_64; rv:136.0) Gecko/20100101 Firefox/136.0",
		"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:136.0) Gecko/20100101 Firefox/136.0",
		"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36",
		"Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
		"AppleTV14,1/16.1",
		"Mozilla/5.0 (CrKey armv7l 1.5.16041) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.0 Safari/537.36",
		"Mozilla/5.0 (PlayStation; PlayStation 5/2.26) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0 Safari/605.1.15",
		"Mozilla/5.0 (Windows NT 10.0; Win64; x64; Xbox; Xbox Series X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.82 Safari/537.36 Edge/20.02"
	};

	static String UA;

	private static OkHttpClient clientInstance = null;
	private static OkHttpClient clientNoTimeoutInstance = null;

	static
	{
		resetUA();

		try
		{
			Context context = weeWXApp.getInstance();

			@SuppressLint("CustomX509TrustManager")
			TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager()
			{
				@SuppressLint("TrustAllX509TrustManager") @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
				@SuppressLint("TrustAllX509TrustManager") @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
				@Override public X509Certificate[] getAcceptedIssuers()
				{
					return new X509Certificate[0];
				}
			}};

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustAllCerts, new SecureRandom());
			SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

			// Define connection spec with TLS 1.3 first, fallback to 1.2
			ConnectionSpec modernTLS = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2).allEnabledCipherSuites().build();

//					.callTimeout(weeWXAppCommon.default_timeout, TimeUnit.MILLISECONDS)

			clientNoTimeoutInstance = new OkHttpClient.Builder()
					.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0])
					.hostnameVerifier((hostname, session) -> true)
					.connectionSpecs(Arrays.asList(modernTLS, ConnectionSpec.CLEARTEXT))
					.retryOnConnectionFailure(true)
					.dns(weeWXApp.customDns)
					.build();

			clientInstance = clientNoTimeoutInstance.newBuilder()
					.connectTimeout(weeWXAppCommon.default_timeout, TimeUnit.MILLISECONDS)
					.writeTimeout(weeWXAppCommon.default_timeout, TimeUnit.MILLISECONDS)
					.readTimeout(weeWXAppCommon.default_timeout, TimeUnit.MILLISECONDS)
					.retryOnConnectionFailure(false)
					.build();

		} catch(Exception e) {
			LogMessage("NetworkClient.java Error! e: " + e, true, KeyValue.e);
		}
	}

	private static void resetUA()
	{
		UA = UAstrings[weeWXAppCommon.getNextRandom(0, UAstrings.length)];
	}

	private static OkHttpClient.Builder newInstance()
	{
		return clientInstance.newBuilder();
	}

	static OkHttpClient getNoTimeoutInstance()
	{
		return clientNoTimeoutInstance.newBuilder().build();
	}

	static OkHttpClient getInstance(String url)
	{
		OkHttpClient.Builder newClient = newInstance();

		if(url == null || url.isBlank() || !url.startsWith("http"))
			return newClient.build();

		// windy.com is very noisy... 2s connectivity checks is beyond excessive...
		//if(!url.contains("windy.com"))
		LogMessage("NetworkClient.getInstance() URL: " + url);

		url = noCache(url);

		LogMessage("NetworkClient.getInstance() New URL: " + url);

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

		LogMessage("NetworkClient.getStream() URL: " + url);

		url = noCache(url);

		LogMessage("NetworkClient.getStream() New URL: " + url);

		Uri uri = Uri.parse(url);
		if(uri.getUserInfo() != null && uri.getUserInfo().contains(":"))
		{
			String[] UC = uri.getUserInfo().split(":");
			String credentials = Credentials.basic(UC[0], UC[1]);
			return newClient.readTimeout(weeWXAppCommon.default_timeout, TimeUnit.MILLISECONDS)
					.authenticator((route, response) ->
					{
						if(responseCount(response) >= 3)
							return null;
						return response.request().newBuilder().header("Authorization", credentials).build();
					}).build();
		}

		return newClient.build();
	}

	private static String noCache(String url)
	{
		if(!url.contains("?"))
			url += "?";
		else
			url += "&";

		return url + "noCache=" + System.currentTimeMillis();
	}

	private static int responseCount(Response response)
	{
		int result = 1;

		while((response = response.priorResponse()) != null)
			result++;

		return result;
	}

	static Request getRequest(boolean doHead, String url, boolean noCache)
	{
		if(url == null || url.isBlank() || !url.startsWith("http"))
			return null;

		LogMessage("NetworkClient.getRequest() URL: " + url);

		String referer = url.substring(0, url.indexOf("/", 8));

		LogMessage("NetworkClient.getRequest() referer: " + referer);

		if(noCache)
		{
			url = noCache(url);

			LogMessage("NetworkClient.getRequest() New URL: " + url);
		}

		Request.Builder req = new Request.Builder()
				.header("User-Agent", UA)
				.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
				.header("Cache-Control", "no-cache, no-store, max-age=0")
				.header("Pragma", "no-cache")
				.header("Accept-Language", "en")
				.header("Upgrade-Insecure-Requests", "1")
				.header("Referer", referer)
				.url(url);

		if(doHead)
			req.head();

		return req.build();

	}
}