package com.odiousapps.weewxweather;

//
//  This class is needed to get round the Android limitation of not honouring
//  timeouts for DNS queries which can cause the app to hang...
//

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.odiousapps.weewxweather.NetworkClient.getBuilder;
import static com.odiousapps.weewxweather.NetworkClient.getRequest;
import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;
import static com.odiousapps.weewxweather.weeWXAppCommon.is_blank;

@DontObfuscate
class CustomDns implements Dns
{
	private record DNSserver(HttpUrl url, List<Dns> serverIPs) {}
	private final List<DNSserver> dnsServers;
	final String[] dnsTypes = {"AAAA", "A"};

	// Mini domain block list
	final static Set<String> bad_domains = Set.of(
			"3lift.com",
			"4dex.io",
			"adnxs.com",
			"adsrvr.org",
			"amazon-adsystem.com",
			"ay.delivery",
			"datadoghq.com",
			"clarity.ms",
			"cloudflareinsights.com",
			"criteo.com",
			"datadoghq-browser-agent.com",
			"doubleclick.net",
			"fuseplatform.net",
			"google.com",
			"google.com.au",
			"google-analytics.com",
			"googlesyndication.com",
			"googletagmanager.com",
			"gstatic.com",
			"gumgum.com",
			"hotjar.com",
			"html-load.com",
			"id5-sync.com",
			"ims.windy.com",
			"ingage.tech",
			"jsdelivr.net",
			"kargo.com",
			"kueezrtb.com",
			"lijit.com",
			"node.windy.com",
			"onetag-sys.com",
			"openx.net",
			"presage.io",
			"pubmatic.com",
			"rubiconproject.com",
			"seedtag.com",
			"servenobid.com",
			"siteimproveanalytics.com",
			"smartadserver.com",
			"sonobi.com",
			"spellknight.com",
			"teads.tv"
	);

	public CustomDns()
	{
		HttpUrl[] urls = {
			HttpUrl.parse("https://cloudflare-dns.com/dns-query"),
			HttpUrl.parse("https://dns.quad9.net/dns-query"),
			HttpUrl.parse("https://dns.google/dns-query")
		};

		String[] cf_servers = {"2606:4700::6810:84e5", "104.16.132.229", "2606:4700::6810:85e5", "104.16.133.229"};
		String[] quad9_servers = {"2620:fe::9", "9.9.9.9", "2620:fe::fe", "149.112.112.112"};
		String[] g_servers = {"2001:4860:4860::8888", "8.8.8.8", "2001:4860:4860::8844", "8.8.4.4"};

		Object[] servers = {cf_servers, quad9_servers, g_servers};

		dnsServers = new ArrayList<>();

		for(int i = 0; i < servers.length; i++)
		{
			String[] serverIPs = (String[])servers[i];

			List<Dns> dnsServerList = new ArrayList<>();
			for (String serverIP : serverIPs)
			{
				try
				{
//					LogMessage("CustomDns() New DNS server IP: " + serverIP + ", url: " + urls[i]);
					InetAddress address = InetAddress.getByName(serverIP);
//					LogMessage("CustomDns() address: " + address);
					Dns lookup = lookuphostname ->
					{
						// Always return the same address
						return Collections.singletonList(address);
					};

					dnsServerList.add(lookup);
//					LogMessage("CustomDns() lookup: " + lookup);
				} catch (UnknownHostException ignored) {}
			}

			dnsServers.add(new DNSserver(urls[i], dnsServerList));
		}

//		LogMessage("CustomDns() dnsServers.size(): " + dnsServers.size());
	}

	@Override
	public List<InetAddress> lookup(String hostname)
	{
		List<InetAddress> serverIPs = new ArrayList<>();

		if(is_blank(hostname))
			return serverIPs;

		hostname = hostname.toLowerCase(Locale.ENGLISH).strip();

		if(bad_domains.contains(hostname))
			return serverIPs;

		for(String bad_domain : bad_domains)
			if(hostname.endsWith(bad_domain))
				return serverIPs;

		for(DNSserver dnsServer : dnsServers)
		{
			try
			{
				for(Dns dnsServerIP : dnsServer.serverIPs())
				{
//					LogMessage("CustomDns.lookup() url: " + dnsServer.url());
//					LogMessage("CustomDns.lookup() dnsServerIP: " + dnsServerIP);
					OkHttpClient client = getBuilder()
						.dns(dnsServerIP)
						.build();

					for(String dnsType : dnsTypes)
					{
						HttpUrl url = dnsServer.url().newBuilder()
							.addQueryParameter("name", hostname)
							.addQueryParameter("type", dnsType)
							.build();

						Request request = getRequest(false, url, false, false)
							.newBuilder().header("Accept", "application/dns-json").build();
						try(Response response = client.newCall(request).execute())
						{
							String body = response.body().string();
//							LogMessage("response.message(): " + response.message());
//							LogMessage("response.code(): " + response.code());
//							LogMessage("response.body(): " + body);

							if(response.isSuccessful() && !is_blank(body))
							{
								JSONObject jobj = new JSONObject(body);
								if(!jobj.has("Answer"))
									continue;

								JSONArray jarr = jobj.getJSONArray("Answer");
								for(int i = 0; i < jarr.length(); i++)
								{
									JSONObject j = jarr.getJSONObject(i);
									if(j.has("data"))
									{
										String tmp = j.getString("data").strip();
										if(is_blank(tmp))
											continue;

										InetAddress serverIP = InetAddress.getByName(tmp);

										if(!serverIPs.contains(serverIP))
											serverIPs.add(serverIP);
									}
								}
							}
						} catch(SocketTimeoutException | SocketException se) {
							//LogMessage("CustomDNS.lookup() Error! e: " + se.getMessage(), true, KeyValue.e);
							//doStackOutput(se);
						}
					}

					if(!serverIPs.isEmpty())
						return serverIPs;
				}
			} catch(JSONException | IOException e) {
				LogMessage("CustomDNS.lookup() Error! e: " + e.getMessage(), true, KeyValue.e);
				//noinspection CallToPrintStackTrace
				e.printStackTrace();
			}
		}

		return serverIPs;
	}
}