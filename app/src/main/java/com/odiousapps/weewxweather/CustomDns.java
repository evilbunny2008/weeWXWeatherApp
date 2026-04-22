package com.odiousapps.weewxweather;

//
//  This class is needed to get round the Android limitation of not honouring
//  timeouts for DNS queries which can cause the app to hang...
//

import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import org.xbill.DNS.config.AndroidResolverConfigProvider;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import okhttp3.Dns;

import static com.odiousapps.weewxweather.weeWXAppCommon.is_blank;

class CustomDns implements Dns
{
	private record NameDepth(Name name, int depth) {}

	byte[] addr = { 0, 0, 0, 0 };

	// Mini domain block list
	final List<String> bad_domains = Arrays.asList(
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

//	List<InetAddress> getDNSServers()
//	{
//		List<InetAddress> result = new ArrayList<>();
//
//		ConnectivityManager cm = (ConnectivityManager)weeWXApp.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
//
//		Network network = cm.getActiveNetwork();
//		if(network == null)
//			return result;
//
//		LinkProperties lp = cm.getLinkProperties(network);
//		if(lp == null)
//		{
//			LogMessage("CustomDNS.java No DNS servers returned from the OS", KeyValue.w);
//			return result;
//		}
//
//		if(lp.getDnsServers().isEmpty())
//		{
//			LogMessage("CustomDNS.java No DNS servers returned from the OS", KeyValue.w);
//			return result;
//		}
//
//		return lp.getDnsServers();
//	}
//
//	List<InetAddress> DoHLookup(String hostname)
//	{
//		//LogMessage("CustomDNS.lookup() Got new DoH DNS request for " + hostname);
//
//		String[] dnsTypes = {"AAAA", "A"};
//
//		List<InetAddress> serverIPs = new ArrayList<>();
//
//		String[] servers = {"2606:4700::6810:84e5", "104.16.132.229", "2606:4700::6810:85e5", "104.16.133.229"};
//
//		try
//		{
//			Dns[] dnsServers = new Dns[servers.length];
//
//			for(int i = 0; i < servers.length; i++)
//			{
//				InetAddress address = InetAddress.getByName(servers[i]);
//
//				dnsServers[i] = lookuphostname ->
//				{
//					// Always return this DNS server’s address
//					return Collections.singletonList(address);
//				};
//			}
//
//			for(Dns dnsServer : dnsServers)
//			{
//				OkHttpClient client = new OkHttpClient.Builder().dns(dnsServer).build();
//
//				for(String dnsType : dnsTypes)
//				{
//					String utf8 = "utf-8";
//					HttpUrl url = HttpUrl.parse("https://cloudflare-dns.com/dns-query?name=" +
//												URLEncoder.encode(hostname, utf8) + "&type=" + dnsType);
//					if(url == null)
//						return serverIPs;
//
//					Request request = new Request.Builder()
//						.url(url)
//						.header("Accept", "application/dns-json")
//						.header("Connection", "close")
//						.build();
//
//					//LogMessage("request: " + request);
//
//					try(Response response = client.newCall(request).execute())
//					{
//						String body = response.body().string();
//						//LogMessage("response.message(): " + response.message());
//						//LogMessage("response.code(): " + response.code());
//						//LogMessage("response.body(): " + body);
//
//						if(response.isSuccessful() && !is_blank(body))
//						{
//							JSONObject jobj = new JSONObject(body);
//							if(!jobj.has("Answer"))
//								continue;
//
//							JSONArray jarr = jobj.getJSONArray("Answer");
//							for(int i = 0; i < jarr.length(); i++)
//							{
//								JSONObject j = jarr.getJSONObject(i);
//								if(j.has("data"))
//								{
//									String tmp = j.getString("data").strip();
//									if(is_bland(tmp))
//										continue;
//
//									if(!serverIPs.contains(InetAddress.getByName(tmp)))
//										serverIPs.add(InetAddress.getByName(tmp));
//								}
//							}
//						}
//					} catch(SocketTimeoutException | SocketException se) {
//						doStackOutput(se);
//					} catch(Exception e) {
////						LogMessage("CustomDNS.lookup() Error! e: " + e.getMessage(), true, KeyValue.e);
//						doStackOutput(e);
//					}
//				}
//
//				if(!serverIPs.isEmpty())
//					return serverIPs;
//			}
//		} catch(Exception e) {
//			LogMessage("CustomDNS.lookup() Error! e: " + e.getMessage(), true, KeyValue.e);
//			doStackOutput(e);
//		}
//
//		return serverIPs;
//	}

	List<InetAddress> getList(Record[] records)
	{
		List<InetAddress> result = new ArrayList<>();
		for(Record r : records)
		{
			if(r instanceof AAAARecord aaaaRecord)
			{
				InetAddress inetAddress = aaaaRecord.getAddress();
				if(!result.contains(inetAddress))
					result.add(inetAddress);
			} else if(r instanceof ARecord aRecord) {
				InetAddress inetAddress = aRecord.getAddress();
				if(!result.contains(inetAddress))
					result.add(inetAddress);
			}
		}

		return result;
	}

	Set<Name> collectCnames(Name start, int maxDepth)
	{
		Set<Name> result = new LinkedHashSet<>();
		Deque<NameDepth> stack = new ArrayDeque<>();
		Set<Name> visited = new HashSet<>(); // avoid repeating lookups

		stack.push(new NameDepth(start, 0));

		while(!stack.isEmpty())
		{
			NameDepth nd = stack.pop();
			Name current = nd.name;
			int depth = nd.depth;
			if(depth >= maxDepth)
				continue;

			// avoid re-querying same name
			if(visited.contains(current))
				continue;

			visited.add(current);

	        Record[] cnameRecords = new Lookup(current, Type.CNAME).run();
            if(cnameRecords != null)
			{
                for(Record r : cnameRecords)
				{
                    if(r instanceof CNAMERecord)
					{
                        Name target = ((CNAMERecord)r).getTarget();
                        if(result.add(target))
						{
                            // push target to explore further if depth allows
                            stack.push(new NameDepth(target, depth + 1));
                        }
                    }
                }
            }
        }
	    return result;
	}

	@Override
	public List<InetAddress> lookup(String hostname) throws UnknownHostException
	{
		int[] types = {Type.AAAA, Type.A};

		List<InetAddress> results = new ArrayList<>();

		if(is_blank(hostname))
		{
			results.add(InetAddress.getByAddress(addr));
			return results;
		}

		AndroidResolverConfigProvider.setContext(weeWXApp.getInstance());

		String strip = hostname.toLowerCase(Locale.ENGLISH).strip();

		Set<Name> allCnames = null;
		try
		{
			allCnames = collectCnames(new Name(strip), 10);
		} catch (TextParseException ignored) {}

		if(allCnames != null && !allCnames.isEmpty())
		{
			for(Name cname : allCnames)
			{
				for(int type : types)
				{
                    Record[] records = new Lookup(cname, type).run();
                    if(records != null && records.length > 0)
                    {
                        List<InetAddress> result = getList(records);
                        if(result != null && !result.isEmpty())
                            results.addAll(result);
                    }
                }
			}
		} else {
			for(int type : types)
			{
				try
				{
					Record[] records = new Lookup(hostname, type).run();
					if(records != null && records.length > 0)
					{
						List<InetAddress> result = getList(records);
						if(result != null && !result.isEmpty())
							results.addAll(result);
					}
				} catch (TextParseException ignored) {}
			}
		}

        if(results.isEmpty())
			results.add(InetAddress.getByAddress(addr));

		//LogMessage("results: " + results);
		return results;

//		InetAddress[] addresses = Address.getAllByName(hostname);
//
//		for(InetAddress ip : addresses)
//		{
//			if(!results.contains(ip))
//				results.add(ip);
//		}
//
//		return results;
//
//		List<InetAddress> results = new ArrayList<>();
//
//		if(is_blank(hostname))
//			return results;
//
//		hostname = hostname.toLowerCase(Locale.ENGLISH).strip();
//
//		if(bad_domains.contains(hostname))
//			return results;
//
//		for(String bad_domain : bad_domains)
//		{
//			if(hostname.endsWith(bad_domain))
//				return results;
//		}
//
//		int[] types = {Type.AAAA, Type.A};
//
//		List<InetAddress> servers = getDNSServers();
//
//		AndroidResolverConfigProvider.setContext(weeWXApp.getInstance());
//
//		if(weeWXApp.fallback_to_DoH == null || !weeWXApp.fallback_to_DoH)
//		{
//			for(InetAddress dns_server: servers)
//			{
//				SimpleResolver resolver = new SimpleResolver(dns_server);
//				resolver.setTimeout(Duration.ofMillis(default_timeout));
//
//				for(int type: types)
//				{
//					try
//					{
//						//LogMessage("CustomDNS.java resolver.setAddress(" + dns_server.getHostAddress() + ")...", KeyValue.d);
//
//						String DNStype = "AAAA";
//						if(type == Type.A)
//							DNStype = "A";
//
//						//LogMessage("CustomDNS.java Trying to lookup: " + hostname + " @" + dns_server.getHostAddress() + " " + DNStype);
//
//						Lookup lookup = new Lookup(hostname, type);
//						lookup.setResolver(resolver);
//
//						Record[] records = lookup.run();
//						if(records != null && records.length > 0)
//						{
//							List<InetAddress> result = getList(records);
//							if(result != null && !result.isEmpty())
//							{
//								for(InetAddress i: result)
//								{
//									//LogMessage("CustomDNS.java result: " + i, KeyValue.d);
//									if(!results.contains(i))
//										results.add(i);
//								}
//							}
//						}
//					} catch(Exception ignored) {}
//				}
//			}
//
//			if(!results.isEmpty())
//			{
//				weeWXApp.fallback_to_DoH = false;
//				return results;
//			}
//		}
//
//		if(weeWXApp.fallback_to_DoH == null)
//			weeWXApp.fallback_to_DoH = true;
//
//		//LogMessage("CustomDNS.java failed to get a valid result via dnsjava, switching to DoH lookups", KeyValue.d);
//		return DoHLookup(hostname);
	}
}