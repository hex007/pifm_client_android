/*
 * Copyright (C) 2014 The Android Open Source Project
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hex.riplay.Server;

import android.text.TextUtils;
import android.util.Log;

import com.hex.riplay.MainActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;


/**
 * Implementation of a very basic HTTP server. The contents are loaded from the assets folder. This
 * server handles one request at a time. It only supports GET method.
 */
public class UpdateChecker implements Runnable {


	private static final String TAG = "SimpleWebServer";

	/**
	 * The port number we listen to
	 */
	@SuppressWarnings("FieldCanBeLocal")
	private final int mPort = 8000;
	private final MainActivity activity;

	/**
	 * True if the server is running.
	 */
	private boolean mIsRunning;

	/**
	 * The {@link java.net.ServerSocket} that we listen to.
	 */
	private ServerSocket mServerSocket;


	/**
	 * WebServer constructor.
	 */
	public UpdateChecker(MainActivity activity) {
		this.activity = activity;
	}


	@Override
	public void run() {
		try {
			mServerSocket = new ServerSocket(mPort);
			while (mIsRunning) {
				Socket socket = mServerSocket.accept();
				handle(socket);
				socket.close();
			}
		} catch (SocketException e) {
			// The server was stopped; ignore.
		} catch (IOException e) {
			Log.e(TAG, "Web server error.", e);
		}
	}


	/**
	 * This method starts the web server listening to the specified port.
	 */
	public void start() {
		mIsRunning = true;
		new Thread(this).start();
		Log.i(TAG, "Server started");
	}


	/**
	 * This method stops the web server
	 */
	public void stop() {
		try {
			mIsRunning = false;
			if (null != mServerSocket) {
				mServerSocket.close();
				mServerSocket = null;
			}
			Log.i(TAG, "Server stopped");
		} catch (IOException e) {
			Log.e(TAG, "Error closing the server socket.", e);
		}
	}


	/**
	 * Respond to a request from a client.
	 *
	 * @param socket The client socket.
	 * @throws IOException
	 */
	private void handle(Socket socket) throws IOException {
		BufferedReader reader = null;
		PrintStream output = null;
		String route = null;
		String data = null;

		try {
			StringBuilder builder = new StringBuilder();
			boolean post = false;
			int postLen = 0;
			String line;

			// Read HTTP headers and parse out the route.
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			while (!TextUtils.isEmpty(line = reader.readLine())) {
				if (line.startsWith("GET /")) {
					int start = line.indexOf('/');
					int end = line.indexOf(' ', start);
					route = line.substring(start, end);
				} else if (line.startsWith("POST /")) {
					post = true;
					int start = line.indexOf('/');
					int end = line.indexOf(' ', start);
					route = line.substring(start, end);
				} else if (post && line.split(":")[0].equals("Content-Length")) {
					postLen = Integer.parseInt(line.split(": ")[1]);
				}
			}
			if (post && postLen > 0) {
				for (int i = 0; i < postLen; i++) {
					builder.append((char) reader.read());
				}
				data = builder.toString();
			}
			// Prepare the content to send.
//			if (null == route) {
//				writeServerError(output);
//				return;
//			}
			output = new PrintStream(socket.getOutputStream());

			System.out.println("Status sync requested");
			output.println("HTTP/1.0 204 No Content");
			output.println();
			output.flush();
		} finally {
			if (null != output) {
				output.close();
			}
			if (null != reader) {
				reader.close();
			}
			report(route, data);
		}
	}


	private void report(final String route, final String data) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				switch (route) {
					case "/notify/status":
						activity.updateStatus(data);
						break;
					case "/notify/collection":
						activity.updateCollection(data);
						break;
					default:
						System.out.println("Should not happen");
				}
			}
		});
	}
}
