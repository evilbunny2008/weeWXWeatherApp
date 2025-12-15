package com.odiousapps.weewxweather;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import androidx.fragment.app.FragmentActivity;

@SuppressWarnings({"unused", "SameParameterValue"})
class CustomDebug
{
	static void writeDebug(String dir, String filename, String text) throws IOException
	{
		File file = weeWXAppCommon.getExtFile(dir, filename);
		writeDebug(file, text, 0);
	}

	static void writeDebug(File file, String text)
	{
		writeDebug(file, text, 0);
	}

	private static void writeDebug(File file, String text, int depth)
	{
		if(depth > 20)
			return;

		try
		{
			if(file.exists() && !file.delete())
			{
				weeWXAppCommon.LogMessage("Failed to remove existing file " + file.getAbsoluteFile());
				throw new IOException("Failed to remove existing file " + file.getAbsoluteFile());
			}

			FileOutputStream FOS = new FileOutputStream(file);
			FOS.write(text.getBytes());
			FOS.close();
		} catch(Exception e) {
			weeWXAppCommon.doStackOutput(e);
			writeDebug(file, text, depth + 1);
		}
	}

	static void copyFile(String inFile, String filename)
	{
		copyFile(inFile, filename, 0);
	}

	private static void copyFile(String inFile, String filename, int depth)
	{
		File outFile;
		String dir = "weeWX";

		if(depth > 20)
			return;

		Context context = weeWXApp.getInstance();
		if(context == null)
			return;

		try
		{
			outFile = new File(Environment.getExternalStorageDirectory(), "Download");
			outFile = new File(outFile, dir);
			if(outFile.exists() && !outFile.isDirectory())
			{
				weeWXAppCommon.LogMessage("'" + dir + "' already exist, but it isn't a directory...");
				return;
			}

			if(!outFile.exists())
			{
				if(!outFile.mkdirs())
				{
					weeWXAppCommon.LogMessage("Can't make '" + dir + "' dir...");
					return;
				}

				weeWXAppCommon.publish(outFile);
			}

			outFile = new File(outFile, filename);
			boolean needsPublishing = !outFile.exists();

			try(InputStream in = context.getAssets().open(inFile);
			    OutputStream out = context.openFileOutput(outFile.getAbsolutePath(), Context.MODE_PRIVATE))
			{

				byte[] buffer = new byte[1024];
				int length;
				while((length = in.read(buffer)) > 0)
					out.write(buffer, 0, length);

				weeWXAppCommon.LogMessage("File saved to " + context.getFileStreamPath(outFile.getAbsolutePath()));

				if(needsPublishing)
					weeWXAppCommon.publish(outFile);
			} catch(IOException e) {
				weeWXAppCommon.doStackOutput(e);
			}
		} catch(Exception e) {
			//Common.doStackOutput(e);
			copyFile(inFile, filename, depth + 1);
		}
	}

	static boolean deleteFile(File dir, String filename)
	{
		try
		{
			File file = new File(dir, filename);
			if(file.exists() && file.canWrite())
			{
				if(!file.delete())
				{
					weeWXAppCommon.LogMessage("Couldn't delete " + file.getAbsolutePath());
					return false;
				} else {
					weeWXAppCommon.LogMessage("Deleted " + file.getAbsolutePath());
					return true;

				}
			} else {
				weeWXAppCommon.LogMessage(file.getAbsolutePath() + " doesn't exist, skipping...");
				return true;
			}
		} catch(Exception e) {
			weeWXAppCommon.LogMessage("Error! e: " + e, true);
		}

		return false;
	}

	static long getModifiedTime(File file)
	{
		try
		{
			if(file.exists())
				return Math.round(file.lastModified() / 1_000D);
		} catch(Exception e) {
			weeWXAppCommon.LogMessage("Error! e: " + e, true);
		}

		return 0;
	}

	static void writeOutput(Context context, String prefix, String output, boolean isAdded, FragmentActivity fragmentActivity)
	{
		String filename = "weeWX_" + prefix + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".html";

		File outFile = null;

		try
		{
			outFile = weeWXAppCommon.getExtFile("weeWX", filename);
			CustomDebug.writeDebug(outFile, output);
			String theOutFile = outFile.getAbsolutePath();

			weeWXAppCommon.LogMessage("Wrote debug html to " + theOutFile);

			if(isAdded)
			{
				fragmentActivity.runOnUiThread(() ->
						Toast.makeText(context, "Wrote debug html to " + theOutFile, Toast.LENGTH_LONG).show());
			}
		} catch(Exception e) {
			weeWXAppCommon.LogMessage("Attempted to write to " + filename + " but failed with the following error: " + e);

			if(isAdded)
			{
				if(outFile != null)
				{
					String theOutFile = outFile.getAbsolutePath();
					fragmentActivity.runOnUiThread(() ->
							Toast.makeText(context, "Failed to output debug html to " + theOutFile, Toast.LENGTH_LONG).show());
				} else {
					fragmentActivity.runOnUiThread(() ->
							Toast.makeText(context, "Failed to output debug html to " + filename, Toast.LENGTH_LONG).show());
				}
			}
		}
	}
}