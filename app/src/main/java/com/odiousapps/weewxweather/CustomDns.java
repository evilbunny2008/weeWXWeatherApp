package com.odiousapps.weewxweather;

//
//  This class is needed to get round the Android limitation of not honouring
//  timeouts for DNS queries which can cause the app to hang...
//

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;
import org.xbill.DNS.config.AndroidResolverConfigProvider;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.odiousapps.weewxweather.weeWXAppCommon.default_timeout;
import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;
import static com.odiousapps.weewxweather.weeWXAppCommon.doStackOutput;

class CustomDns implements Dns
{
	// Mini domain block list
	final List<String> bad_domains = new ArrayList<>(Arrays.asList(
			"3lift.com",
			"4dex.io",
			"adnxs.com",
			"adsrvr.org",
			"amazon-adsystem.com",
			"ay.delivery",
			"datadoghq.com",
			"clarity.ms",
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
			"smartadserver.com",
			"sonobi.com",
			"spellknight.com",
			"teads.tv"
	));

	List<InetAddress> getList(Record[] records)
	{
		List<InetAddress> result = new ArrayList<>();
		for(Record r: records)
		{
			if(r instanceof AAAARecord)
			{
				InetAddress i = ((AAAARecord)r).getAddress();
				if(!result.contains(i))
					result.add(i);
			} else if(r instanceof ARecord) {
				InetAddress i = ((ARecord)r).getAddress();
				if(!result.contains(i))
					result.add(i);
			}
		}

		return result;
	}

	List<InetAddress> getDNSServers()
	{
		List<InetAddress> result = new ArrayList<>();

		ConnectivityManager cm = (ConnectivityManager)weeWXApp.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);

		Network network = cm.getActiveNetwork();
		if(network == null)
			return result;

		LinkProperties lp = cm.getLinkProperties(network);
		if(lp == null)
		{
			LogMessage("CustomDNS.java No DNS servers returned from the OS", KeyValue.w);
			return result;
		}

		if(lp.getDnsServers().isEmpty())
		{
			LogMessage("CustomDNS.java No DNS servers returned from the OS", KeyValue.w);
			return result;
		}

		return lp.getDnsServers();
	}

	List<InetAddress> DoHLookup(String hostname)
	{
		//LogMessage("CustomDNS.lookup() Got new DoH DNS request for " + hostname);

		String[] dnsTypes = {"AAAA", "A"};

		List<InetAddress> serverIPs = new ArrayList<>();

		String[] servers = {"2606:4700::6810:84e5", "104.16.132.229", "2606:4700::6810:85e5", "104.16.133.229"};

		try
		{
			Dns[] dnsServers = new Dns[servers.length];

			for(int i = 0; i < servers.length; i++)
			{
				InetAddress address = InetAddress.getByName(servers[i]);

				dnsServers[i] = lookuphostname ->
				{
					// Always return this DNS server’s address
					return Collections.singletonList(address);
				};
			}

			for(Dns dnsServer : dnsServers)
			{
				OkHttpClient client = new OkHttpClient.Builder().dns(dnsServer).build();

				for(String dnsType : dnsTypes)
				{
					String utf8 = "utf-8";
					HttpUrl url = HttpUrl.parse("https://cloudflare-dns.com/dns-query?name=" +
					                            URLEncoder.encode(hostname, utf8) + "&type=" + dnsType);
					if(url == null)
						return serverIPs;

					Request request = new Request.Builder()
				        .url(url)
				        .header("Accept", "application/dns-json")
				        .build();

					//LogMessage("request: " + request);

					try(Response response = client.newCall(request).execute())
					{
						String body = response.body().string();
						//LogMessage("response.message(): " + response.message());
						//LogMessage("response.code(): " + response.code());
						//LogMessage("response.body(): " + body);

						if(response.isSuccessful() && !body.isBlank())
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
									if(tmp.isBlank())
										continue;

									if(!serverIPs.contains(InetAddress.getByName(tmp)))
										serverIPs.add(InetAddress.getByName(tmp));
								}
							}
						}
					} catch(SocketTimeoutException | SocketException ignored) {
					} catch(Exception e) {
						LogMessage("CustomDNS.lookup() Error! e: " + e.getMessage(), true, KeyValue.e);
						doStackOutput(e);
					}
				}

				if(!serverIPs.isEmpty())
					return serverIPs;
			}
		} catch(Exception e) {
			LogMessage("CustomDNS.lookup() Error! e: " + e.getMessage(), true, KeyValue.e);
			doStackOutput(e);
		}

		return serverIPs;
	}

	@NonNull
	@Override
	public List<InetAddress> lookup(@NonNull String hostname)
	{
		List<InetAddress> results = new ArrayList<>();

		if(hostname.isBlank())
			return results;

		hostname = hostname.strip().toLowerCase();

		if(bad_domains.contains(hostname))
			return results;

		for(String bad_domain : bad_domains)
		{
			if(hostname.endsWith(bad_domain))
				return results;
		}

		int[] types = {Type.AAAA, Type.A};

		List<InetAddress> servers = getDNSServers();

		AndroidResolverConfigProvider.setContext(weeWXApp.getInstance());

		if(weeWXApp.fallback_to_DoH == null || !weeWXApp.fallback_to_DoH)
		{
			for(InetAddress dns_server: servers)
			{
				SimpleResolver resolver = new SimpleResolver(dns_server);
				resolver.setTimeout(Duration.ofMillis(default_timeout));

				for(int type: types)
				{
					try
					{
						//LogMessage("CustomDNS.java resolver.setAddress(" + dns_server.getHostAddress() + ")...", KeyValue.d);

//						String DNStype = "AAAA";
//						if(type == Type.A)
//							DNStype = "A";

						//LogMessage("CustomDNS.java Trying to lookup: " + hostname + " @" + dns_server.getHostAddress() + " " + DNStype);

						Lookup lookup = new Lookup(hostname, type);
						lookup.setResolver(resolver);

						Record[] records = lookup.run();
						if(records != null && records.length > 0)
						{
							List<InetAddress> result = getList(records);
							if(result != null && !result.isEmpty())
							{
								for(InetAddress i: result)
								{
									//LogMessage("CustomDNS.java result: " + i, KeyValue.d);
									if(!results.contains(i))
										results.add(i);
								}
							}
						}
					} catch(Exception ignored) {}
				}
			}

			if(!results.isEmpty())
			{
				weeWXApp.fallback_to_DoH = false;
				return results;
			}
		}

		if(weeWXApp.fallback_to_DoH == null)
			weeWXApp.fallback_to_DoH = true;

		//LogMessage("CustomDNS.java failed to get a valid result via dnsjava, switching to DoH lookups", KeyValue.d);
		return DoHLookup(hostname);
	}
}