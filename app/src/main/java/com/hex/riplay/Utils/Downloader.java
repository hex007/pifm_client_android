package com.hex.riplay.Utils;

import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Class to hold simple methods for accessing internet resources.
 * Should always be called from WorkerThread
 */
@WorkerThread
@SuppressWarnings("unused")
public class Downloader {

	private static final String UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36";
	private static final String TAG = "Downloader";


	public static BitmapDrawable getImage(String url) {
		try {
			System.out.println("IMAGE : " + url);
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setDoInput(true);
			connection.setRequestProperty("User-Agent", UA);
			connection.setRequestProperty("Accept", "text/html");
			connection.connect();

			return (BitmapDrawable) BitmapDrawable.createFromStream(connection.getInputStream(), "src");
		} catch (IOException e) {
			return null;
		}
	}


	@SuppressWarnings("SameParameterValue")
	public static String getPage(String url, String postData) throws IOException {
		HttpURLConnection connection = getGenericConnection(url);
		connection.setDoInput(true);

		if (postData == null) {
			System.out.println("GET : " + url);
			connection.setRequestMethod("GET");
			connection.setDoOutput(false);
		} else {
			System.out.println("POST : " + url);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Referer", url);
//			// Load POST data
			connection.setDoOutput(true);
			byte[] data = postData.getBytes();
			connection.setRequestProperty("Content-Length", String.valueOf(data.length));
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.getOutputStream().write(data);
		}

		switch (connection.getResponseCode()) {

			case HttpURLConnection.HTTP_OK:
			case HttpURLConnection.HTTP_CREATED:
			case HttpURLConnection.HTTP_ACCEPTED:
				Log.d(TAG, "getPage: Processing response : " + connection.getResponseCode());
				break;

			case HttpURLConnection.HTTP_MOVED_TEMP:
			case HttpURLConnection.HTTP_MOVED_PERM:
				Log.d(TAG, "getPage: redirected");
				return connection.getHeaderField("Location");

			default:
				throw new IOException(String.format("Error fetching page (%d) : %s", connection.getResponseCode(), url));
		}

		// Read response
		InputStream ist = connection.getInputStream();
		ByteArrayOutputStream content = new ByteArrayOutputStream();

		final byte[] buff = new byte[4096];
		int readCount;

		while ((readCount = ist.read(buff)) != -1) {
			content.write(buff, 0, readCount);
		}
		System.out.println("Page obtained");
		return new String(content.toByteArray());
	}


	public static String getPage(String url) throws IOException {
		return getPage(url, null);
	}


	public static boolean isValidRequest(String loc) throws IOException {
		HttpURLConnection connection = getHeaders(loc);
		switch (connection.getResponseCode()) {

			case HttpURLConnection.HTTP_OK:
			case HttpURLConnection.HTTP_CREATED:
			case HttpURLConnection.HTTP_ACCEPTED:

			case HttpURLConnection.HTTP_MOVED_TEMP:
			case HttpURLConnection.HTTP_MOVED_PERM:
				return true;

			default:
				return false;
		}
	}


	/**
	 * Opens connection to url described by loc
	 */
	private static HttpURLConnection getGenericConnection(String loc) throws IOException {
		URL url = new URL(loc.replaceAll(" ", "%20"));
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("User-Agent", UA);
		connection.setRequestProperty("Accept", "text/html");
		connection.setConnectTimeout(500);

		return connection;
	}


	private static HttpURLConnection getHeaders(String url) throws IOException {
		// Version 2.0
		System.out.println("HEAD : " + url);
		url = url.replaceAll(" ", "%20");

		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestMethod("HEAD");
		connection.setRequestProperty("User-Agent", UA);
		connection.connect();

		return connection;
	}
}
