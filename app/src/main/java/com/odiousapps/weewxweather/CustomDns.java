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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import okhttp3.Dns;

//@SuppressWarnings({"unused", "SequencedCollectionMethodCanBeUsed"})
class CustomDns implements Dns
{
	boolean isReachableTCP(InetAddress addr)
	{
		try(Socket socket = new Socket())
		{
			// Attempt a HTTPS connection...
			SocketAddress socketAddress = new InetSocketAddress(addr, 443);
			socket.connect(socketAddress, 5_000);
			return true;
		} catch (IOException e) {
			try(Socket socket = new Socket())
			{
				// Attempt a HTTP connection...
				SocketAddress socketAddress = new InetSocketAddress(addr, 80);
				socket.connect(socketAddress, 5_000);
				return true;
			} catch (IOException ioe) {
				return false;
			}
		}
	}

	boolean isReachableUDP(InetAddress addr)
	{
		try(DatagramSocket socket = new DatagramSocket())
		{
			// Attempt a DNS connection...
			byte[] buf = "test".getBytes();
			DatagramPacket packet = new DatagramPacket(buf, buf.length, addr, 53);
			socket.send(packet);
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			String received = new String(packet.getData(), 0, packet.getLength());
			weeWXAppCommon.LogMessage("received: " + received, true, KeyValue.i);
			return true;
		} catch (Exception e) {
			weeWXAppCommon.LogMessage("Error! e: " + e.getMessage(), true, KeyValue.e);
			return false;
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
					weeWXAppCommon.LogMessage("Testing IPv6: " + i.getHostAddress());
					if(isReachableTCP(i))
					{
						weeWXAppCommon.LogMessage(i.getHostAddress() + " was reachable...");
						result.add(i);
					} else {
						weeWXAppCommon.LogMessage(i.getHostAddress() + " wasn't reachable...");
					}
				} catch(Exception e) {
					weeWXAppCommon.LogMessage("Error! e: " + e.getMessage(), KeyValue.e);
					weeWXAppCommon.doStackOutput(e);
				}
			} else if(r instanceof ARecord) {
				try
				{
					InetAddress i = ((ARecord)r).getAddress();
					weeWXAppCommon.LogMessage("Testing IPv4: " + i.getHostAddress());
					if(isReachableTCP(i))
					{
						weeWXAppCommon.LogMessage(i.getHostAddress() + " was reachable...");
						result.add(i);
					} else {
						weeWXAppCommon.LogMessage(i.getHostAddress() + " wasn't reachable...");
					}
				} catch(Exception e) {
					weeWXAppCommon.LogMessage("Error! e: " + e.getMessage(), KeyValue.e);
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
				weeWXAppCommon.LogMessage("No DNS servers returned from the OS", KeyValue.w);
				return new ArrayList<>();
			}

			if(lp.getDnsServers().isEmpty())
			{
				weeWXAppCommon.LogMessage("No DNS servers returned from the OS", KeyValue.w);
				return new ArrayList<>();
			}

			return lp.getDnsServers();
		} catch(Exception ignored) {}

		return new ArrayList<>();
	}

	List<InetAddress> doLookup(String hostname)
	{
		int[] types = new int[]{Type.AAAA, Type.A};

		List<InetAddress> results = new ArrayList<>();
		List<InetAddress> servers = getDNSServers();

		AndroidResolverConfigProvider.setContext(weeWXApp.getInstance());

		for(InetAddress dns_server : servers)
		{
			SimpleResolver resolver = new SimpleResolver(dns_server);

			for(int type: types)
			{
				try
				{
					weeWXAppCommon.LogMessage("resolver.setAddress(" + dns_server.getHostAddress() + ")...", KeyValue.d);
					resolver.setTCP(true);
					resolver.setTimeout(Duration.ofSeconds(10));

					String DNStype = "AAAA";
					if(type == Type.A)
						DNStype = "A";

					weeWXAppCommon.LogMessage("Trying to lookup: " + hostname + " @" + dns_server.getHostAddress() + " " + DNStype);

					Lookup lookup = new Lookup(hostname, type);
					lookup.setResolver(resolver);

					Record[] records = lookup.run();
					if(records != null && records.length > 0)
					{
						List<InetAddress> result = getList(records);
						if(result != null && !result.isEmpty())
						{
							weeWXAppCommon.LogMessage("result #1: " + result, KeyValue.d);
							results.addAll(result);
							//return result;
						}
					}
				} catch(Exception ignored) {}
			}
		}

		return results;
	}

	@NonNull
	@Override
	public List<InetAddress> lookup(@NonNull String hostname)
	{
		try
		{
			return doLookup(hostname);
		} catch(Exception e) {
			weeWXAppCommon.LogMessage("Error! e: " + e, true, KeyValue.e);
			weeWXAppCommon.doStackOutput(e);
		}

		return new ArrayList<>();
	}
}
