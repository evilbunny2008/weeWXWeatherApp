package com.odiousapps.weewxweather;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;

import static com.odiousapps.weewxweather.Common.LogMessage;
import static com.odiousapps.weewxweather.Common.doStackOutput;

@SuppressWarnings("unused")
class CustomDebug
{
	static void writeDebug(String text)
	{
		String fn = Common.getContext().getApplicationInfo().loadLabel(weeWXApp.getInstance().getPackageManager()) + ".txt";
	}

	static void writeDebug(String filename, String text)
	{
		String dir = "weeWX";
		writeDebug(dir, filename, text);
	}

	static void writeDebug(String dir, String filename, String text)
	{
		File outFile;

		try
		{
			outFile = new File(Environment.getExternalStorageDirectory(), "Download");
			outFile = new File(outFile, dir);
			if(outFile.exists() && !outFile.isDirectory())
			{
				LogMessage("'" + dir + "' already exist, but it isn't a directory...");
				return;
			}

			if(!outFile.exists() && !outFile.mkdirs())
			{
				LogMessage("Can't make '" + dir + "' dir...");
				return;
			}

			if(!outFile.canWrite())
			{
				LogMessage("Can't write to '" + dir + "' directory...");
				return;
			}

			outFile = new File(outFile, filename);

			if(outFile.exists() && !outFile.delete())
			{
				LogMessage("Couldn't delete the existing file at '" + outFile.getAbsolutePath() + "'");
				return;
			}

			if(!outFile.createNewFile())
			{
				LogMessage("Couldn't create a new file at '" + outFile.getAbsolutePath() + "'");
				return;
			}

			FileOutputStream FOS = new FileOutputStream(outFile);
			FOS.write(text.getBytes());
			FOS.close();
		} catch (Exception e) {
			doStackOutput(e);
		}
	}
}