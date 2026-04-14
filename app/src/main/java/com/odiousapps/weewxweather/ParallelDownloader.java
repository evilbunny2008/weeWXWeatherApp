package com.odiousapps.weewxweather;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.annotation.NonNull;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;
import static com.odiousapps.weewxweather.weeWXAppCommon.is_valid_url;

public class ParallelDownloader
{
	private final ExecutorService executor;

	public ParallelDownloader(int threadCount)
	{
		this.executor = Executors.newFixedThreadPool(threadCount);
	}

	public record DownloadResult(int id, String url, boolean success, String error,
								 String contentType, long length, String string, Bitmap bm) {}

	public List<DownloadResult> downloadAll(@NonNull List<Integer> idtypes, @NonNull List<String> urls, @NonNull List<String> contentTypes)
	{
		List<Future<DownloadResult>> futures = new ArrayList<>();

		LogMessage("ParallelDownloader.downloadAll() contentTypes: " + contentTypes);

		// Submit all downloads
		for(int i = 0; i < urls.size(); i++)
		{
			int final_i = i;
			String contentType = final_i < contentTypes.size() && !contentTypes.get(final_i).isBlank() ? contentTypes.get(final_i) : "HTML";
			int id = final_i < idtypes.size() && idtypes.get(final_i) >= 0 ? idtypes.get(final_i) : -1;
			LogMessage("ParallelDownloader.downloadAll(" + id + ") contentType: " + contentType);
			futures.add(executor.submit(() -> getContent(id, urls.get(final_i), contentType)));
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
			} catch (TimeoutException e) {
				results.add(new DownloadResult(id,null, false, "Timeout", "ERROR", 0,null, null));
			} catch (ExecutionException | InterruptedException e) {
				results.add(new DownloadResult(id,null, false, e.getMessage(), "ERROR", 0, null, null));
			}
		}

		return results;
	}

	private DownloadResult getContent(int id, String url, String contentType)
	{
		if(url == null)
			return new DownloadResult(id, null, true, "Skipped", contentType, 0, null, null);

		if(!is_valid_url(url))
			return new DownloadResult(id, url, false, "Invalid URL", "ERROR", 0, null, null);

		LogMessage("ParallelDownloader.getContent(" + id + ") id: " + id);
		LogMessage("ParallelDownloader.getContent(" + id + ") contentType: " + contentType);
		LogMessage("ParallelDownloader.getContent(" + id + ") url: " + url);

		if(contentType.equals("MJPEG"))
		{
			OkHttpClient client = NetworkClient.getStream(url);

			Request request = NetworkClient.getRequest(false, url);
			if(request == null)
				return null;

			try(Response response = client.newCall(request).execute())
			{
				if(!response.isSuccessful())
				{
					String bodyStr = response.body().string();
					String error = "HTTP error " + response;
					if(!bodyStr.isBlank())
						error += ", body: " + bodyStr;

					LogMessage("ParallelDownloader.getContent(" + id + ") Error! error: " + error, KeyValue.e);

					return new DownloadResult(id, url, false, error, "ERROR", 0, null, null);
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
					return new DownloadResult(id, url, false, "Download size was 0 bytes", "ERROR", 0, null, null);
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
						return new DownloadResult(id, url, false, warning, "ERROR", 0, null, null);
					}
					offset += read;
				}

				BitmapFactory.Options options = new BitmapFactory.Options();
				Bitmap bm = BitmapFactory.decodeStream(new ByteArrayInputStream(imageBytes), null, options);
				if(bm != null)
				{
					LogMessage("ParallelDownloader.getContent(" + id + ") Got an image... wooo!");
					return new DownloadResult(id, url, true, null, "IMAGE", imageBytes.length, null, bm);
				}

				LogMessage("ParallelDownloader.getContent(" + id + ") Error! Invalid image, trying for another", KeyValue.v);
				return getContent(id, url, contentType);
			} catch(IOException e) {
				LogMessage("ParallelDownloader.getContent(" + id + ") Error! " + e.getMessage(), KeyValue.e);
				return new DownloadResult(id, url, false, e.getLocalizedMessage(), "ERROR", 0, null, null);
			}
		} else if(contentType.equals("IMAGE")) {
			OkHttpClient client = NetworkClient.getInstance(url);

			Request request = NetworkClient.getRequest(false, url);
			if(request == null)
				return null;

			try(Response response = client.newCall(request).execute())
			{
				if(!response.isSuccessful())
				{
					String string = response.body().string();

					String error = "HTTP error " + response;
					if(!string.isBlank())
						error += ", body: " + string;

					LogMessage("ParallelDownloader.getContent(" + id + ") Error! error: " + error, KeyValue.e);

					return new DownloadResult(id, url, false, error, "ERROR", 0, null, null);
				} else if(response.body().contentLength() == 0) {
					LogMessage("ParallelDownloader.getContent(" + id + ") Error! Download size was 0 bytes", KeyValue.e);
					return new DownloadResult(id, url, false, "Download size was 0 bytes", "ERROR", 0, null, null);
				}

				byte[] bytes = response.body().bytes();
				LogMessage("ParallelDownloader.getContent(" + id + ") bytes.length: " + bytes.length);

				BitmapFactory.Options options = new BitmapFactory.Options();
				Bitmap bm = BitmapFactory.decodeStream(new ByteArrayInputStream(bytes), null, options);
				if(bm != null)
				{
					LogMessage("ParallelDownloader.getContent(" + id + ") Got an image... wooo!");
					return new DownloadResult(id, url, true, null, "IMAGE", bytes.length, null, bm);
				}

				LogMessage("ParallelDownloader.getContent(" + id + ") Error! Invalid image returned", KeyValue.e);
				return new DownloadResult(id, url, false, "Invalid image returned", "ERROR", 0, null, null);
			} catch(IOException e) {
				LogMessage("ParallelDownloader.getContent(" + id + ") Error! " + e.getMessage(), KeyValue.e);
				return new DownloadResult(id, url, false, e.getLocalizedMessage(), "ERROR", 0, null, null);
			}
		} else {
			OkHttpClient client = NetworkClient.getInstance(url);

			Request request = NetworkClient.getRequest(false, url);
			if(request == null)
				return null;

			try(Response response = client.newCall(request).execute())
			{
				String string = response.body().string();

				if(!response.isSuccessful())
				{
					String error = "HTTP error " + response;
					if(!string.isBlank())
						error += ", body: " + string;

					LogMessage("ParallelDownloader.getContent(" + id + ") Error! " + error, KeyValue.e);
					return new DownloadResult(id, url, false, error, "ERROR", 0, null, null);
				} else if(string.length() == 0) {
					LogMessage("ParallelDownloader.getContent(" + id + ") Error! Download size was 0 bytes", KeyValue.e);
					return new DownloadResult(id, url, false, "Download size was 0 bytes", "ERROR", 0, null, null);
				}

				LogMessage("ParallelDownloader.getContent(" + id + ") Returning content of " + string.length() + " length");
				return new DownloadResult(id, url, true, null, contentType, string.length(), string, null);
			} catch (Exception e) {
				LogMessage("ParallelDownloader.getContent(" + id + ") Error! " + e.getMessage(), KeyValue.e);
				return new DownloadResult(id, url, false, e.getLocalizedMessage(), "ERROR", 0, null, null);
			}
		}
	}

	public void shutdown()
	{
		executor.shutdown();
	}
}