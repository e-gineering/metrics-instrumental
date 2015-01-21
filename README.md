# metrics-instrumental [![Build Status](https://travis-ci.org/egineering-llc/metrics-instrumental.svg?branch=master)](https://travis-ci.org/egineering-llc/metrics-instrumental)

A direct-access client library and reporter for the [Dropwizard Metrics library](http://dropwizard.github.io/metrics) which publishes metrics to [Instrumental](http://instrumentalapp.com), a cloud-based metrics tool.

Using the direct-access [Instrumental](src/main/java/com/e_gineering/metrics/instrumental/Instrumental.java) or [InstrumentalStreamer](src/main/java/com/e_gineering/metrics/instrumental/InstrumentalStreamer.java), you can synchronously or asynchronously push metrics to Instrumental with or without blocking your application.
If you're using DropWizard Metrics you can make use of the [InstrumentalReporter](src/main/java/com/e_gineering/metrics/instrumental/InstrumentalReporter.java).

## Example Usage
If you're using maven, add the following dependency

```xml
<dependency>
  <groupId>com.e-gineering</groupId>
  <artifactId>metrics-instrumental</artifactId>
  <version>3.1.0.2</version>
</dependency>
```

Then, somewhere in your code, create an Instrumental instance with your api key, and use it!

```java
long start = System.nanoTime();
Instrumental instrumental = new Instrumental("your_api_key");

// Wrap it in an asynch streamer and send things to Instrumental yourself.
InstrumentalStreamer streamer = new InstrumentalStreamer(instrumental, Executors.newSingleThreadExecutor());
streamer.notice("Starting up!"); // This will not block for I/O.
streamer.notice("Really Starting up!", true); // This will block for I/O.

// Non-blocking (via the InstrumentalStreamer)
streamer.send(MetricType.GAUGE, "startup", new Long(System.nanoTime() - start).floatValue(), System.currentTimeMillis() / 1000); 
streamer.send(MetricType.GAUGE, "startup", new Long(System.nanoTime() - start).floatValue(), System.currentTimeMillis() / 1000, false); 

// Blocking (via the InstrumentalStreamer)
streamer.send(MetricType.GAUGE, "startup", new Long(System.nanoTime() - start).floatValue(), System.currentTimeMillis() / 1000, true); 

// Blocking (direct via Instrumental)
instrumental.send(MetricType.GAUGE, "startup", new Long(System.nanoTime() - start).floatValue(), System.currentTimeMillis() / 1000); 

// Publish a DropWizard metrics registry...
InstrumentalReporter instrumentalReporter = InstrumentalReporter.forRegistry(registry)
                                                                .convertRatesTo(TimeUnit.SECONDS)
                                                                .convertDurationsTo(TimeUnit.MILLISECONDS)
                                                                .build(instrumental);
reporter.start(5, TimeUnit.SECONDS);
```

For what it's worth, the Instrumental implementation protectect socket manipulation and output writes with a reentrant lock to prevent multiple writing threads from messing up the socket OutputStream.
It should be safe to mix n' match interaction styles with Instrumental, InstrumentalSender, and InstrumentalReporter.


