package com.odiousapps.weewxweather;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

import static com.odiousapps.weewxweather.weeWXAppCommon.doStackOutput;
import static com.odiousapps.weewxweather.weeWXAppCommon.LogMessage;

@SuppressWarnings({"unused", "SameParameterValue"})
class CustomDebug
{
	private static final String utf8 = "utf-8";

	static void writeDebug(String dir, String filename, String text) throws IOException
	{
		if(dir == null || filename == null || text == null ||
		   dir.isBlank() || filename.isBlank() || text.isBlank())
			throw new IOException("dir is null or blank or filename is null or blank or text is null or blank");

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
		{
			String mimetype = "text/plain";

			filename = filename.strip();

			String ext = filename.substring(filename.lastIndexOf("."));
			LogMessage("CustomDebug.writeDebug() filename: " + filename);
			LogMessage("CustomDebug.writeDebug() ext: " + ext);

			if(ext.equalsIgnoreCase(".html"))
				mimetype = "text/html";

			outputWithMediaStore(dir, filename, text, mimetype);
		} else {
			File file = weeWXAppCommon.getExtFile(dir, filename);
			writeDebug(file, text, 0);
		}
	}

	static String readDebug(String dir, String filename) throws IOException
	{
		if(dir == null || filename == null || dir.isBlank() || filename.isBlank())
			return null;

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
		{
			return readFromMediaStore(dir, filename);
		} else {
			StringBuilder sb = new StringBuilder();
			File f = weeWXAppCommon.getExtFile("weeWX", "R2_body.html");
			if(f.exists() && f.canRead() && f.length() > 0)
			{
				FileInputStream fis = new FileInputStream(f);
				BufferedReader br = new BufferedReader(new InputStreamReader(fis, utf8));
				String line;
				while((line = br.readLine()) != null)
				{
					sb.append(line);
					sb.append("\n");
				}

				return sb.toString();
			}
		}

		return null;
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
				LogMessage("Failed to remove existing file " + file.getAbsoluteFile(), KeyValue.w);
				//throw new IOException("Failed to remove existing file " + file.getAbsoluteFile());
				return;
			}

			FileOutputStream FOS = new FileOutputStream(file);
			FOS.write(text.getBytes());
			FOS.close();
		} catch(Exception e) {
			doStackOutput(e);
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
				LogMessage("'" + dir + "' already exist, but it isn't a directory...", KeyValue.w);
				return;
			}

			if(!outFile.exists())
			{
				if(!outFile.mkdirs())
				{
					LogMessage("Can't make '" + dir + "' dir...", KeyValue.w);
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

				LogMessage("File saved to " + context.getFileStreamPath(outFile.getAbsolutePath()));

				if(needsPublishing)
					weeWXAppCommon.publish(outFile);
			} catch(IOException e) {
				doStackOutput(e);
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
					LogMessage("Couldn't delete " + file.getAbsolutePath(), KeyValue.w);
					return false;
				} else {
					LogMessage("Deleted " + file.getAbsolutePath());
					return true;

				}
			} else {
				LogMessage(file.getAbsolutePath() + " doesn't exist, skipping...");
				return true;
			}
		} catch(Exception e) {
			LogMessage("Error! e: " + e, true, KeyValue.e);
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
			LogMessage("Error! e: " + e, true, KeyValue.e);
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

			LogMessage("writeOutput() Wrote debug html to " + theOutFile);

			if(isAdded)
			{
				fragmentActivity.runOnUiThread(() ->
						Toast.makeText(context, "Wrote debug html to " + theOutFile, Toast.LENGTH_LONG).show());
			}
		} catch(Exception e) {
			LogMessage("writeOutput() Attempted to write to " + filename + " but failed with the following error: " + e.getMessage(), KeyValue.e);

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

	@RequiresApi(api = Build.VERSION_CODES.Q)
	private static String readFromMediaStore(String dir, String filename) throws IOException
	{
		ContentResolver resolver = weeWXApp.getInstance().getContentResolver();
		String folderName = Environment.DIRECTORY_DOWNLOADS + "/" + dir + "/";

		long id;
		Uri fileUri, filesCollection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

		String[] projection = { MediaStore.Files.FileColumns._ID };
		String selection = MediaStore.Files.FileColumns.DISPLAY_NAME + "=? AND " +
		                   MediaStore.Files.FileColumns.RELATIVE_PATH + "=?";
		String[] args = { filename, folderName };

		LogMessage("readFromMediaStore() Looking for " + filename + " in " + folderName);

		try(Cursor cursor = resolver.query(filesCollection, projection, selection, args, null))
		{
			if(cursor == null)
				throw new IOException("Failed to find file #1");

			LogMessage("readFromMediaStore() cursor: " + cursor.getCount());

			if(!cursor.moveToFirst())
				throw new IOException("Failed to find file #2");

			id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID));
			fileUri = ContentUris.withAppendedId(filesCollection, id);

			LogMessage("readFromMediaStore() Found " + filename + ", attempting to read contents...");

			try(InputStream is = resolver.openInputStream(fileUri))
			{
		       if(is == null)
		           throw new IOException("Failed to open file");

				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				byte[] data = new byte[8192];
				int bytesRead;

				while((bytesRead = is.read(data, 0, data.length)) != -1)
				{
					LogMessage("readFromMediaStore() Read " + bytesRead + " bytes from storage");
					buffer.write(data, 0, bytesRead);
				}

				return buffer.toString(utf8);
            }
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.Q)
	private static void outputWithMediaStore(String dir, String filename, String text, String mimetype) throws IOException
	{
		ContentResolver resolver = weeWXApp.getInstance().getContentResolver();
		String folderName = Environment.DIRECTORY_DOWNLOADS + "/" + dir + "/";

		long id;
		Uri fileUri = null;
		Uri filesCollection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

		String[] projection = { MediaStore.Files.FileColumns._ID };
		String selection = MediaStore.Files.FileColumns.DISPLAY_NAME + "=? AND " +
		                   MediaStore.Files.FileColumns.RELATIVE_PATH + "=?";
		String[] args = { filename, folderName };

		LogMessage("outputWithMediaStore() Checking if " + filename + " exists in " + folderName);

		try(Cursor cursor = resolver.query(filesCollection, projection, selection, args, null))
		{
			if(cursor != null && cursor.moveToFirst())
			{
				id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID));
				fileUri = ContentUris.withAppendedId(filesCollection, id);
			}
		}

		if(fileUri != null)
			LogMessage("outputWithMediaStore() Found " + filename + ", attempting to overwrite contents...");

		ContentValues values = new ContentValues();

		if(fileUri == null)
		{
			LogMessage("outputWithMediaStore() Didn't find " + filename + ", creating new file...");

			values.put(MediaStore.Files.FileColumns.DISPLAY_NAME, filename);
			values.put(MediaStore.Files.FileColumns.MIME_TYPE, mimetype);
			values.put(MediaStore.Downloads.IS_PENDING, 1);
			values.put(MediaStore.Files.FileColumns.RELATIVE_PATH, folderName);

			fileUri = resolver.insert(filesCollection, values);
		}

		if(fileUri == null)
			throw new IOException("Failed to write to file");

		try(OutputStream os = resolver.openOutputStream(fileUri, "wt"))
		{
			if(os == null)
			{
				LogMessage("outputWithMediaStore() Failed to write to " + filename + ", skipping...");
				throw new IOException("Failed to write to " + filename + ", skipping...");
			}

			os.write(text.getBytes());
			LogMessage("outputWithMediaStore() Successfully wrote to " + filename);
		}

		values.clear();
		values.put(MediaStore.Downloads.IS_PENDING, 0);
		resolver.update(fileUri, values, null, null);
	}
}