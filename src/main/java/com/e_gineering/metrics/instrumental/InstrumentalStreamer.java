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

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Wraps an InstrumentalSender in an asynchronous wrapper.
 */
public class InstrumentalStreamer implements InstrumentalSender, Closeable {
	private InstrumentalSender delegate;
	private ExecutorService executorService;

	/**
	 * Constructor which can be used to create an InstrumentalSender that uses the given ExecutorService to queue
	 * outgoing metrics.
	 *
	 * @param instrumental
	 * @param executorService
	 */
	public InstrumentalStreamer(InstrumentalSender instrumental, ExecutorService executorService) {
		this.delegate = instrumental;
		this.executorService = executorService;
	}

	/**
	 * Private delegate wrapper for posting metrics asynchronously.
	 */
	private class MetricPoster implements Callable<Void> {
		MetricType type;
		String name;
		String value;
		long timestamp;

		private MetricPoster(MetricType type, String name, String value, long timestamp) {
			this.type = type;
			this.name = name;
			this.value = value;
			this.timestamp = timestamp;
		}

		@Override
		public Void call() throws IOException {
			delegate.send(type, name, value, timestamp);
			return null;
		}
	}

	/**
	 * Private delegate wrapper for posting Notices asynchronously.
	 */
	private class NoticePoster implements Callable<Void> {
		String name;
		long start;
		TimeUnit startUnit;
		long duration;
		TimeUnit durationUnit;

		private NoticePoster(String name, long start, TimeUnit startUnit, long duration, TimeUnit durationUnit) {
			this.name = name;
			this.start = start;
			this.startUnit = startUnit;
			this.duration = duration;
			this.durationUnit = durationUnit;
		}

		@Override
		public Void call() throws IOException {
			delegate.notice(name, start, startUnit, duration, durationUnit);
			return null;
		}
	}

	@Override
	public void close() throws IOException {
		delegate.close();
	}

	@Override
	public void connect() throws IllegalStateException, IOException {
		delegate.connect();
	}

	@Override
	public void send(MetricType type, String name, String value, long timestamp) throws IOException {
		send(type, name, value, timestamp, false);
	}

	@edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
	public void send(MetricType type, String name, String value, long timestamp, boolean synchronous) throws IOException {
		if (synchronous) {
			delegate.send(type, name, value, timestamp);
		} else {
			executorService.submit(new MetricPoster(type, name, value, timestamp));
		}
	}

	@Override
	public void notice(String name) {
		notice(name, false);
	}

	public void notice(String name, boolean synchronous) {
		notice(name, 0, TimeUnit.SECONDS, synchronous);
	}

	@Override
	public void notice(String name, long duration, TimeUnit durationUnit) {
		notice(name, duration, durationUnit, false);
	}

	public void notice(String name, long duration, TimeUnit durationUnit, boolean synchronous) {
		notice(name, System.currentTimeMillis(), TimeUnit.MILLISECONDS, duration, durationUnit, synchronous);
	}

	@Override
	public void notice(String name, long start, TimeUnit startUnit, long duration, TimeUnit durationUnit) {
		notice(name, start, startUnit, duration, durationUnit, false);
	}

	@edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
	public void notice(String name, long start, TimeUnit startUnit, long duration, TimeUnit durationUnit, boolean synchronous) {
		if (synchronous) {
			delegate.notice(name, start, startUnit, duration, durationUnit);
		} else {
			executorService.submit(new NoticePoster(name, start, startUnit, duration, durationUnit));
		}
	}

	@Override
	public void flush() throws IOException {
		delegate.flush();

	}

	@Override
	public boolean isConnected() {
		return delegate.isConnected();
	}

	@Override
	public int getFailures() {
		return delegate.getFailures();
	}
}
