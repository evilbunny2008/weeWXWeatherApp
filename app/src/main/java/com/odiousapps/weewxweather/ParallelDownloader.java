package com.odiousapps.weewxweather;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.odiousapps.weewxweather.NetworkClient.getInstance;
import static com.odiousapps.weewxweather.NetworkClient.getRequest;
import static com.odiousapps.weewxweather.NetworkClient.getStream;
import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;
import static com.odiousapps.weewxweather.weeWXAppCommon.doStackOutput;
import static com.odiousapps.weewxweather.weeWXAppCommon.is_blank;
import static com.odiousapps.weewxweather.weeWXAppCommon.is_valid_url;

@DontObfuscate
public class ParallelDownloader
{
	private final ExecutorService executor;
	public final long startTime;

	private boolean shuttingdown = false;

	public ParallelDownloader(int threadCount, String className)
	{
		this.executor = Executors.newFixedThreadPool(threadCount, r ->
		{
			Thread thread = new Thread(r);
			thread.setName(className + "-worker-" + Thread.activeCount());
			return thread;
		});

		startTime = System.currentTimeMillis();
	}

	public record DownloadResult(int id, String url, boolean success, String error,
								 String contentType, long length, String string, Bitmap bm) {}

	public List<DownloadResult> downloadAll(List<Integer> idtypes, List<String> urls,
											List<String> contentTypes, List<Boolean> noCache)
	{
		if(idtypes == null || idtypes.size() == 0 || urls == null || urls.size() == 0 ||
				contentTypes == null || contentTypes.size() == 0 || noCache == null || noCache.size() == 0)
			return null;

		List<Future<DownloadResult>> futures = new ArrayList<>();

		LogMessage("ParallelDownloader.downloadAll() contentTypes: " + contentTypes);

		// Submit all downloads
		for(int i = 0; i < urls.size(); i++)
		{
			int final_i = i;
			String contentType = final_i < contentTypes.size() && !is_blank(contentTypes.get(final_i)) ? contentTypes.get(final_i) : "HTML";
			int id = final_i < idtypes.size() && idtypes.get(final_i) >= 0 ? idtypes.get(final_i) : -1;
			LogMessage("ParallelDownloader.downloadAll(" + id + ") contentType: " + contentType);
			futures.add(executor.submit(() -> getContent(id, urls.get(final_i), contentType, noCache.get(final_i))));
		}

		// Collect results
		List<DownloadResult> results = new ArrayList<>();
		for(int i = 0; i < futures.size(); i++)
		{
			int id = i < idtypes.size() && idtypes.get(i) >= 0 ? idtypes.get(i) : -1;
			Future<DownloadResult> future = futures.get(i);
			try
			{
				results.add(future.get(30, TimeUnit.SECONDS));
			} catch(ExecutionException | InterruptedException | TimeoutException e) {
				if(!shuttingdown)
					results.add(new DownloadResult(id,null, false, e.getLocalizedMessage(),
							"ERROR", 0,null, null));
				else
					results.add(new DownloadResult(id,null, true, null, "NULL", 0,null, null));
			}
		}

		return results;
	}

	private DownloadResult getContent(int id, String url, String contentType, boolean noCache)
	{
		if(url == null)
			return new DownloadResult(id, null, true, "Skipped", contentType, 0, null, null);

		DownloadResult dr = new DownloadResult(id, url, false, "Invalid URL", "ERROR", 0, null, null);

		if(!is_valid_url(url))
			return dr;

		HttpUrl url2 = HttpUrl.parse(url);
		if(url2 == null)
			return dr;

		return getContent(id, url2, contentType, 0, null, noCache);
	}

	private DownloadResult getContent(int id, HttpUrl url, String contentType, int attempt, String lastError, boolean noCache)
	{
		if(attempt >= 3)
			return new DownloadResult(id, url.toString(), false, lastError, "ERROR", 0, null, null);

		if(attempt > 0 || Thread.currentThread().isInterrupted())
		{
			try
			{
				Thread.sleep(5_000L);
			} catch(InterruptedException ie) {
				if(!Thread.currentThread().isInterrupted())
				{
					LogMessage("ParallelDownloader.getContent(" + id + ") interrupted!");
					return new DownloadResult(id, url.toString(), false, "interrupted", "ERROR", 0, null, null);
				}
			}
		}

		if(shuttingdown || Thread.currentThread().isInterrupted())
			return new DownloadResult(id, url.toString(), true, null, "NULL", 0, null, null);

		LogMessage("ParallelDownloader.getContent(" + id + ") id: " + id);
		LogMessage("ParallelDownloader.getContent(" + id + ") contentType: " + contentType);
		LogMessage("ParallelDownloader.getContent(" + id + ") url: " + url);

		if(contentType.equals("MJPEG"))
		{
			OkHttpClient client = getStream(url);

			Request request = getRequest(false, url, noCache, true);
			try(Response response = client.newCall(request).execute())
			{
				if(!response.isSuccessful())
				{
					String bodyStr = response.body().string();
					String error = "HTTP error " + response;
					if(!is_blank(bodyStr))
						error += ", body: " + bodyStr;

					LogMessage("ParallelDownloader.getContent(" + id + ") Error! error: " + error, KeyValue.e);

					return getContent(id, url, contentType, attempt + 1, error, noCache);
				}

				LogMessage("ParallelDownloader.getContent(" + id + ") Successfully connected to server, now to grab a frame...");

				InputStream urlStream = response.body().byteStream();

				BufferedReader reader = new BufferedReader(new InputStreamReader(urlStream, StandardCharsets.US_ASCII));

				String line;
				int contentLength = -1;

				while((line = reader.readLine()) != null)
				{
					if(line.isEmpty() && contentLength > 0)
						break;

					if(line.startsWith("Content-Length:"))
						contentLength = Integer.parseInt(line.substring(15).strip());
				}

				if(contentLength == 0)
				{
					LogMessage("ParallelDownloader.getContent(" + id + ") Error! contentLength: " + contentLength, KeyValue.e);
					return getContent(id, url, contentType, attempt + 1, "contentLength was 0", noCache);
				}

				LogMessage("ParallelDownloader.getContent(" + id + ") contentLength: " + contentLength);

				byte[] imageBytes = new byte[contentLength];
				int offset = 0;
				while(offset < contentLength)
				{
					int read = urlStream.read(imageBytes, offset, contentLength - offset);
					if(read == -1)
					{
						String warning = "Stream ended prematurely";
						LogMessage("ParallelDownloader.getContent(" + id + ") Error! " + warning, KeyValue.e);
						return getContent(id, url, contentType, attempt + 1, warning, noCache);
					}

					offset += read;
				}

				BitmapFactory.Options options = new BitmapFactory.Options();
				Bitmap bm = BitmapFactory.decodeStream(new ByteArrayInputStream(imageBytes), null, options);
				if(bm != null)
				{
					LogMessage("ParallelDownloader.getContent(" + id + ") Got an image... wooo!");
					return new DownloadResult(id, url.toString(), true, null, "IMAGE", imageBytes.length, null, bm);
				}

				LogMessage("ParallelDownloader.getContent(" + id + ") Error! Invalid image, trying for another", KeyValue.v);
				return getContent(id, url, contentType, attempt + 1, "Invalid image", noCache);
			} catch (UnknownHostException e) {
				return new DownloadResult(id, url.toString(), false, e.getLocalizedMessage(), "ERROR", 0, null, null);
			} catch(IOException e) {
				LogMessage("ParallelDownloader.getContent(" + id + ") Error! " + e.getMessage(), KeyValue.e);
				doStackOutput(e);
				return getContent(id, url, contentType, attempt + 1, e.getLocalizedMessage(), noCache);
			}
		} else if(contentType.equals("IMAGE")) {
			OkHttpClient client = getInstance(url);

			Request request = getRequest(false, url, noCache, true);
			try(Response response = client.newCall(request).execute())
			{
				if(!response.isSuccessful())
				{
					String string = response.body().string();

					String error = "HTTP error " + response;
					if(!is_blank(string))
						error += ", body: " + string;

					LogMessage("ParallelDownloader.getContent(" + id + ") Error! error: " + error, KeyValue.e);

					return getContent(id, url, contentType, attempt + 1, error, noCache);
				} else if(response.body().contentLength() == 0) {
					LogMessage("ParallelDownloader.getContent(" + id + ") Error! Download size was 0 bytes", KeyValue.e);
					return getContent(id, url, contentType, attempt + 1, "contentLength was 0", noCache);
				}

				byte[] bytes = response.body().bytes();
				LogMessage("ParallelDownloader.getContent(" + id + ") bytes.length: " + bytes.length);

				BitmapFactory.Options options = new BitmapFactory.Options();
				Bitmap bm = BitmapFactory.decodeStream(new ByteArrayInputStream(bytes), null, options);
				if(bm != null)
				{
					LogMessage("ParallelDownloader.getContent(" + id + ") Got an image... wooo!");
					return new DownloadResult(id, url.toString(), true, null, "IMAGE", bytes.length, null, bm);
				}

				LogMessage("ParallelDownloader.getContent(" + id + ") Error! Invalid image returned", KeyValue.e);
				return getContent(id, url, contentType, attempt + 1, "Invalid image returned", noCache);
			} catch (UnknownHostException e) {
				return new DownloadResult(id, url.toString(), false, e.getLocalizedMessage(), "ERROR", 0, null, null);
			} catch(IOException e) {
				LogMessage("ParallelDownloader.getContent(" + id + ") Error! " + e.getMessage(), KeyValue.e);
				doStackOutput(e);
				return getContent(id, url, contentType, attempt + 1, e.getLocalizedMessage(), noCache);
			}
		} else {
			OkHttpClient client = getInstance(url);

			Request request = getRequest(false, url, noCache, true);
			try(Response response = client.newCall(request).execute())
			{
				String string = response.body().string();

				if(!response.isSuccessful())
				{
					String error = "HTTP error " + response;
					if(!is_blank(string))
						error += ", body: " + string;

					LogMessage("ParallelDownloader.getContent(" + id + ") Error! " + error, KeyValue.e);
					return getContent(id, url, contentType, attempt + 1, error, noCache);
				} else if(string.length() == 0) {
					LogMessage("ParallelDownloader.getContent(" + id + ") Error! Download size was 0 bytes", KeyValue.e);
					return getContent(id, url, contentType, attempt + 1, "Download size was 0 bytes", noCache);
				}

				LogMessage("ParallelDownloader.getContent(" + id + ") Returning content of " + string.length() + " length");
				return new DownloadResult(id, url.toString(), true, null, contentType, string.length(), string, null);
			} catch (UnknownHostException e) {
				return new DownloadResult(id, url.toString(), false, e.getLocalizedMessage(), "ERROR", 0, null, null);
			} catch (IOException e) {
				LogMessage("ParallelDownloader.getContent(" + id + ") Error! " + e.getMessage(), KeyValue.e);
				doStackOutput(e);
				return getContent(id, url, contentType, attempt + 1, e.getLocalizedMessage(), noCache);
			}
		}
	}

	public boolean isRunning()
	{
		return !executor.isShutdown();
	}

	public void shutdown()
	{
		shuttingdown = true;
		LogMessage("ParallelDownloader.shutdown()");
		executor.shutdownNow();

		try
		{
			if(!executor.awaitTermination(2, TimeUnit.SECONDS))
				LogMessage("ParallelDownloader.shutdown() Executor did not shut down properly", KeyValue.e);
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}