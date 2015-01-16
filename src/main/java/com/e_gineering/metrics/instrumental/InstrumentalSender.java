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
import java.util.concurrent.TimeUnit;

/**
 * An abstraction for sending data to Instrumental, in case future protocol changes force supporting more than
 * one implementation.
 *
 * Largely based upon the graphite reporting module from Dropwizard Metrics.
 */
public interface InstrumentalSender extends Closeable {
	public void connect() throws IllegalStateException, IOException;

	public void send(MetricType type, String name, String value, long timestamp) throws IOException;

	public void notice(String name);
	public void notice(String name, long duration, TimeUnit durationUnit);
	public void notice(String name, long start, TimeUnit startUnit, long duration, TimeUnit durationUnit);

	void flush() throws IOException;

	boolean isConnected();

	public int getFailures();
}
