package com.odiousapps.weewxweather;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;

import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;
import org.xbill.DNS.Record;
import org.xbill.DNS.config.AndroidResolverConfigProvider;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import okhttp3.Dns;

class CustomDns implements Dns
{
	boolean isReachableTCP(InetAddress addr)
	{
		try(Socket socket = new Socket())
		{
			// Attempt a HTTPS connection...
			SocketAddress socketAddress = new InetSocketAddress(addr, 443);
			socket.connect(socketAddress, 3_000);
			return true;
		} catch (IOException e) {
			try(Socket socket = new Socket())
			{
				// Fall back to a HTTP connection...
				SocketAddress socketAddress = new InetSocketAddress(addr, 80);
				socket.connect(socketAddress, 3_000);
				return true;
			} catch (IOException ioe) {
				return false;
			}
		}
	}

	List<InetAddress> getList(Record[] records)
	{
		List<InetAddress> result = new ArrayList<>();
		for(Record r : records)
		{
			if(r instanceof AAAARecord)
			{
				try
				{
					InetAddress i = ((AAAARecord)r).getAddress();
					weeWXAppCommon.LogMessage("CustomDNS.java Testing IPv6: " + i.getHostAddress());
					if(isReachableTCP(i))
					{
						weeWXAppCommon.LogMessage("CustomDNS.java " + i.getHostAddress() + " was reachable...");
						result.add(i);
					} else {
						weeWXAppCommon.LogMessage("CustomDNS.java " + i.getHostAddress() + " wasn't reachable...");
					}
				} catch(Exception e) {
					weeWXAppCommon.LogMessage("CustomDNS.java Error! e: " + e.getMessage(), KeyValue.e);
					weeWXAppCommon.doStackOutput(e);
				}
			} else if(r instanceof ARecord) {
				try
				{
					InetAddress i = ((ARecord)r).getAddress();
					weeWXAppCommon.LogMessage("CustomDNS.java Testing IPv4: " + i.getHostAddress());
					if(isReachableTCP(i))
					{
						weeWXAppCommon.LogMessage("CustomDNS.java " + i.getHostAddress() + " was reachable...");
						result.add(i);
					} else {
						weeWXAppCommon.LogMessage("CustomDNS.java " + i.getHostAddress() + " wasn't reachable...");
					}
				} catch(Exception e) {
					weeWXAppCommon.LogMessage("CustomDNS.java Error! e: " + e.getMessage(), KeyValue.e);
					weeWXAppCommon.doStackOutput(e);
				}
			}
		}

		return result;
	}

	List<InetAddress> getDNSServers()
	{
		try
		{
			ConnectivityManager cm = (ConnectivityManager)weeWXApp.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);

			Network network = cm.getActiveNetwork();
			if(network == null)
				return new ArrayList<>();

			LinkProperties lp = cm.getLinkProperties(network);
			if(lp == null)
			{
				weeWXAppCommon.LogMessage("CustomDNS.java No DNS servers returned from the OS", KeyValue.w);
				return new ArrayList<>();
			}

			if(lp.getDnsServers().isEmpty())
			{
				weeWXAppCommon.LogMessage("CustomDNS.java No DNS servers returned from the OS", KeyValue.w);
				return new ArrayList<>();
			}

			return lp.getDnsServers();
		} catch(Exception ignored) {}

		return new ArrayList<>();
	}

	@NonNull
	@Override
	public List<InetAddress> lookup(@NonNull String hostname)
	{
		int[] types = new int[]{Type.AAAA, Type.A};

		List<InetAddress> results = new ArrayList<>();

		List<InetAddress> servers = getDNSServers();

		AndroidResolverConfigProvider.setContext(weeWXApp.getInstance());

		for(InetAddress dns_server : servers)
		{
			SimpleResolver resolver = new SimpleResolver(dns_server);
			resolver.setTimeout(Duration.ofSeconds(1));

			for(int type : types)
			{
				try
				{
					weeWXAppCommon.LogMessage("CustomDNS.java resolver.setAddress(" + dns_server.getHostAddress() + ")...", KeyValue.d);

					String DNStype = "AAAA";
					if(type == Type.A)
						DNStype = "A";

					weeWXAppCommon.LogMessage("CustomDNS.java Trying to lookup: " + hostname + " @" + dns_server.getHostAddress() + " " + DNStype);

					Lookup lookup = new Lookup(hostname, type);
					lookup.setResolver(resolver);

					Record[] records = lookup.run();
					if(records != null && records.length > 0)
					{
						List<InetAddress> result = getList(records);
						if(result != null && !result.isEmpty())
						{
							for(InetAddress i : result)
							{
								weeWXAppCommon.LogMessage("CustomDNS.java result: " + i, KeyValue.d);
								if(!results.contains(i))
									results.add(i);
							}
						}
					}
				} catch(Exception ignored) {}
			}
		}

		return results;
	}
}
