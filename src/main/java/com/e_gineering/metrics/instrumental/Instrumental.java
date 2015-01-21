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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Creates a reporting connection to Instrumental.
 *
 * Largely based upon the graphite reporting module from Dropwizard Metrics.
 */
public class Instrumental implements InstrumentalSender {

	private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

	private static final Pattern PARENS = Pattern.compile("[\\(\\)]+");
	private static final Pattern COMMA_SPACE = Pattern.compile(", ");
	private static final Pattern ACCEPTED_NAMES = Pattern.compile("[^A-Za-z0-9_\\-\\.]");

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
		socket.setSoTimeout(5000);
		if (address.isUnresolved()) {
			throw new UnknownHostException(address.getHostName());
		}
		socket.connect(address);

		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "ASCII"));

		String hello = "hello version java/metrics_instrumental/" + InstrumentalVersion.VERSION + " hostname " + socket.getLocalAddress().getHostName() + " pid " + getProcessId("?") + " runtime " + getRuntimeInfo() + " platform " + getPlatformInfo();
		socket.getOutputStream().write(hello.getBytes(ASCII));
		socket.getOutputStream().write(LF);
		socket.getOutputStream().flush();

		if (!"ok".equals(reader.readLine())) {
			close();
			throw new ProtocolException("hello failed");
		}

		socket.getOutputStream().write(("authenticate " + apiKey).getBytes(ASCII));
		socket.getOutputStream().write(LF);
		socket.getOutputStream().flush();

		if (!"ok".equals(reader.readLine())) {
			close();
			throw new ProtocolException("authenticate failed");
		}
	}

	@Override
	public boolean isConnected() {
		return socket != null && !socket.isClosed() && !socket.isOutputShutdown();
	}

	@Override
	public void send(MetricType type, String name, String value, long timestamp) throws IOException {
		if (!isConnected()) {
			connect();
		}

		try {
			StringBuilder buf = new StringBuilder(type.getProtocolKey());
			buf.append(' ');
			buf.append(sanitizeName(name));
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


	/**
	 * Sends a named Notice at the current system time, with no duration to Instrumental
	 *
	 * @param name The text of the notice.
	 */
	public void notice(String name) {
		notice(name, 0, TimeUnit.SECONDS);
	}

	/**
	 * Sends a named Notice at the current system time, with the given duration.
	 *
	 * @param name The text of the notice
	 * @param duration Period duration.
	 * @param durationUnit Period TimeUnit.
	 */
	public void notice(String name, long duration, TimeUnit durationUnit) {
		notice(name, System.currentTimeMillis(), TimeUnit.MILLISECONDS, duration, durationUnit);
	}

	/**
	 * Sends a named Notice at the given start time for the given duration.
	 *
	 * @param name The text of the notice
	 * @param start When the notice started (Measure in wall-clock time like unix timestamp since 1970)
	 * @param startUnit start TimeUnit (ie, MILLISECONDS, or SECONDS, etc.)
	 * @param duration Period duration.
	 * @param durationUnit Period TimeUnit.
	 */
	public void notice(String name, long start, TimeUnit startUnit, long duration, TimeUnit durationUnit) {
		try {
			if (!isConnected()) {
				connect();
			}

			try {
				StringBuilder buf = new StringBuilder("notice ");
				buf.append(Long.toString(TimeUnit.SECONDS.convert(start, startUnit)));
				buf.append(' ');
				buf.append(Long.toString(TimeUnit.SECONDS.convert(duration, durationUnit)));
				buf.append(' ');
				buf.append(sanitizeName(name));
				buf.append('\n');
				socket.getOutputStream().write(buf.toString().getBytes(ASCII));
				this.failures = 0;
			} catch (IOException ioe) {
				failures++;
				throw ioe;
			}
		} catch (IOException ioe) {
			try {
				close();
			} catch (IOException e) {
				// Eat it.
			}
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

	protected String sanitizeName(String s) {
		return ACCEPTED_NAMES.matcher(PARENS.matcher(COMMA_SPACE.matcher(s).replaceAll("-")).replaceAll("__")).replaceAll(".");
	}

	protected String sanitize(String s) {
		return WHITESPACE.matcher(s).replaceAll(".");
	}
}
