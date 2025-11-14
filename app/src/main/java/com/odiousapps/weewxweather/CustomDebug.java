package com.odiousapps.weewxweather;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings({"unused", "SameParameterValue"})
class CustomDebug
{
	static void writeDebug(String text)
	{
		Context context = weeWXApp.getInstance();
		if(context == null)
			return;

		String fn = context.getApplicationInfo().loadLabel(weeWXApp.getInstance().getPackageManager()) + ".txt";
		writeDebug(fn, text);
	}

	static void writeDebug(String filename, String text)
	{
		String dir = "weeWX";
		writeDebug(dir, filename, text);
	}

	static void writeDebug(String dir, String filename, String text)
	{
		writeDebug(dir, filename, text, 0);
	}

	private static void writeDebug(String dir, String filename, String text, int depth)
	{
		File outFile;

		if(depth > 20)
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

			if(!outFile.exists() && !outFile.mkdirs())
			{
				weeWXAppCommon.LogMessage("Can't make '" + dir + "' dir...");
				return;
			}

			outFile = new File(outFile, filename);

			FileOutputStream FOS = new FileOutputStream(outFile);
			FOS.write(text.getBytes());
			FOS.close();
		} catch(Exception e) {
			//Common.doStackOutput(e);
			writeDebug(dir, filename, text, depth + 1);
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

			if(!outFile.exists() && !outFile.mkdirs())
			{
				weeWXAppCommon.LogMessage("Can't make '" + dir + "' dir...");
				return;
			}

			outFile = new File(outFile, filename);
			try(InputStream in = context.getAssets().open(inFile);
			    OutputStream out = context.openFileOutput(outFile.getAbsolutePath(), Context.MODE_PRIVATE))
			{

				byte[] buffer = new byte[1024];
				int length;
				while((length = in.read(buffer)) > 0)
					out.write(buffer, 0, length);

				weeWXAppCommon.LogMessage("File saved to " + context.getFileStreamPath(outFile.getAbsolutePath()));
			} catch(IOException e) {
				weeWXAppCommon.doStackOutput(e);
			}
		} catch(Exception e) {
			//Common.doStackOutput(e);
			copyFile(inFile, filename, depth + 1);
		}
	}
}