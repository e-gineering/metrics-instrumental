/**
 * Copyright 2015 E-Gineering, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.e_gineering.metrics.instrumental;

import javax.net.SocketFactory;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

/**
 * Creates a reporting connection to Instrumental.
 *
 * Largely based upon the graphite reporting module from Dropwizard Metrics.
 */
public class Instrumental implements InstrumentalSender {

	private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
	private static final Charset ASCII = Charset.forName("ASCII");
	private static byte[] LF = "\n".getBytes(ASCII);

	private String hostname;
	private int port;
	private String apiKey;
	private InetSocketAddress address;
	private SocketFactory socketFactory;

	public Socket socket = null;
	private int failures;

	/**
	 * Creates a connection to Instrumentalapp.com, using the default collector URI, Port, and SocketFactory.
	 *
	 * @param apiKey Your project API key.
	 */
	public Instrumental(String apiKey) {
		this(apiKey, "collector.instrumentalapp.com", 8000);
	}

	/**
	 * Creates a connection to instrumentalapp.com, using the default collector URI, port, and specified SocketFactory.
	 *
	 * @param apiKey Your project API key.
	 * @param socketFactory A SocketFactory to use when creating the underlying socket.
	 */
	public Instrumental(String apiKey, SocketFactory socketFactory) {
		this(apiKey, "collector.instrumentalapp.com", 8000, socketFactory);
	}

	public Instrumental(String apiKey, String hostname, int port) {
		this(apiKey, hostname, port, SocketFactory.getDefault());
	}

	public Instrumental(String apiKey, String hostname, int port, SocketFactory socketFactory) {
		this.hostname = hostname;
		this.port = port;
		this.apiKey = apiKey;
		this.address = null;
		this.socketFactory = socketFactory;
	}

	public Instrumental(String apiKey, InetSocketAddress address) {
		this(apiKey, address, SocketFactory.getDefault());
	}

	public Instrumental(String apiKey, InetSocketAddress address, SocketFactory socketFactory) {
		this.hostname = null;
		this.port = -1;
		this.apiKey = apiKey;
		this.address = address;
		this.socketFactory = socketFactory;
	}

	@Override
	public void connect() throws IllegalStateException, IOException {
		if (isConnected()) {
			throw new IllegalStateException("Already connected");
		}

		if (socket != null) {
			socket.close();
		}

		if (hostname != null) {
			address = new InetSocketAddress(hostname, port);
		}

		socket = socketFactory.createSocket();
		socket.setTcpNoDelay(true);
		socket.setKeepAlive(true);
		socket.setTrafficClass(0x04 | 0x10); // Reliability, low-delay
		socket.setPerformancePreferences(0, 2, 1); // latency more important than bandwidth and connection time.
		if (address.isUnresolved()) {
			throw new UnknownHostException(address.getHostName());
		}
		socket.connect(address);

		String hello = "hello version java/metrics_instrumental/3.1.1 hostname " + socket.getLocalAddress().getHostName() + " pid " + getProcessId("?") + " runtime " + getRuntimeInfo() + " platform " + getPlatformInfo();
		socket.getOutputStream().write(hello.getBytes(ASCII));
		socket.getOutputStream().write(LF);
		socket.getOutputStream().flush();
		socket.getOutputStream().write(("authenticate " + apiKey).getBytes(ASCII));
		socket.getOutputStream().write(LF);
		socket.getOutputStream().flush();
	}

	@Override
	public boolean isConnected() {
		return socket != null && !socket.isClosed() && !socket.isOutputShutdown();
	}

	@Override
	public void send(String name, String value, long timestamp) throws IOException {
		if (!isConnected()) {
			connect();
		}

		try {
			StringBuilder buf = new StringBuilder("gauge ");
			buf.append(sanitize(name));
			buf.append(' ');
			buf.append(sanitize(value));
			buf.append(' ');
			buf.append(Long.toString(timestamp));
			buf.append('\n');
			socket.getOutputStream().write(buf.toString().getBytes(ASCII));
			this.failures = 0;
		} catch (IOException ioe) {
			failures++;
			throw ioe;
		}
	}

	@Override
	public int getFailures() {
		return failures;
	}

	@Override
	public void flush() throws IOException {
		if (isConnected()) {
			socket.getOutputStream().flush();
		}
	}

	@Override
	public void close() throws IOException {
		if (isConnected()) {
			socket.shutdownOutput();
			socket.close();
		}
	}

	private static String getProcessId(final String fallback) {
		// Note: may fail in some JVM implementations
		// therefore fallback has to be provided

		// something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
		final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		final int index = jvmName.indexOf('@');

		if (index < 1) {
			// part before '@' empty (index = 0) / '@' not found (index = -1)
			return fallback;
		}

		try {
			return Long.toString(Long.parseLong(jvmName.substring(0, index)));
		} catch (NumberFormatException e) {
			// ignore
		}
		return fallback;
	}

	private static String getPlatformInfo() {
		return System.getProperty("os.arch", "unknown").replaceAll(" ", "_") + "-" + System.getProperty("os.name", "unknown").replaceAll(" ", "_") + System.getProperty("os.version", "").replaceAll(" ", "_");
	}

	private static String getRuntimeInfo() {
		return System.getProperty("java.vendor", "java").replaceAll(" ", "_") + "/" + System.getProperty("java.version", "?").replaceAll(" ", "_");
	}

	protected String sanitize(String s) {
		return WHITESPACE.matcher(s).replaceAll(".");
	}


}
