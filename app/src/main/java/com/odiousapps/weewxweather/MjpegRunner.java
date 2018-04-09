package com.odiousapps.weewxweather;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

public class MjpegRunner implements Runnable
{
    // https://github.com/andrealaforgia/mjpeg-client/blob/master/src/main/java/com/andrealaforgia/mjpegclient/MjpegRunner.java

    private static final String CONTENT_LENGTH = "Content-Length: ";
    private final URL url;
    private InputStream urlStream;
    private boolean isRunning = true;
    Bitmap bm = null;

    MjpegRunner(URL url) throws Exception
    {
        this.url = url;
        start();
    }

    private void start() throws Exception
    {
        URLConnection urlConn = url.openConnection();
        urlConn.setReadTimeout(5000);
        urlConn.connect();
        urlStream = urlConn.getInputStream();
    }

    /**
     * Stop the loop, and allow it to clean up
     */
    private synchronized void stop()
    {
        isRunning = false;
    }

    public void run() {
        while (isRunning) {
            try {
                Common.LogMessage("waiting for an image.");
                byte[] imageBytes = retrieveNextImage();
                ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                bm = BitmapFactory.decodeStream(bais);
                Common.LogMessage("got an image... wooo!");
                isRunning = false;
            } catch (SocketTimeoutException ste) {
                System.err.println("failed stream read: " + ste);
                stop();

            } catch (IOException e) {
                System.err.println("failed stream read: " + e);
                stop();
            }
        }

        // close streams
        try {
            urlStream.close();
        } catch (IOException ioe) {
            System.err.println("Failed to close the stream: " + ioe);
        }
    }

    private byte[] retrieveNextImage() throws IOException
    {
        int currByte;

        boolean captureContentLength = false;
        StringWriter contentLengthStringWriter = new StringWriter(128);
        StringWriter headerWriter = new StringWriter(128);

        int contentLength = 0;

        while ((currByte = urlStream.read()) > -1) {
            if (captureContentLength) {
                if (currByte == 10 || currByte == 13) {
                    contentLength = Integer.parseInt(contentLengthStringWriter.toString());
                    break;
                }
                contentLengthStringWriter.write(currByte);

            } else {
                headerWriter.write(currByte);
                String tempString = headerWriter.toString();
                int indexOf = tempString.indexOf(CONTENT_LENGTH);
                if (indexOf > 0) {
                    captureContentLength = true;
                }
            }
        }

        // 255 indicates the start of the jpeg image
        //noinspection StatementWithEmptyBody
        while ((urlStream.read()) != 255) {}

        // rest is the buffer
        byte[] imageBytes = new byte[contentLength + 1];
        // since we ate the original 255 , shove it back in
        imageBytes[0] = (byte) 255;
        int offset = 1;
        int numRead;
        while (offset < imageBytes.length
                && (numRead = urlStream.read(imageBytes, offset, imageBytes.length - offset)) >= 0) {
            offset += numRead;
        }

        return imageBytes;
    }
}