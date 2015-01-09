# metrics-instrumental [![Build Status](https://travis-ci.org/egineering-llc/metrics-instrumental.svg?branch=master)](https://travis-ci.org/egineering-llc/metrics-instrumental)

This is a reporter for the [Dropwizard Metrics library](http://dropwizard.github.io/metrics) which publishes metrics to [Instrumental](http://instrumentalapp.com), a cloud-based metrics tool.

## Example Usage
If you're using maven, add the following dependency


```
<dependency>
  <groupId>com.e-gineering</groupId>
  <artifactId>metrics-instrumental</artifactId>
  <version>3.1.0</version>
</dependency>
```

Then, somewhere in your code, create an Instrumental instance with your api key, and start a reporter.

```
Instrumental instrumental = new Instrumental("your_api_key");
InstrumentalReporter instrumentalReporter = InstrumentalReporter.forRegistry(registry)
                                                                .convertRatesTo(TimeUnit.SECONDS)
                                                                .convertDurationsTo(TimeUnit.MILLISECONDS)
                                                                .build(instrumental);
reporter.start(5, TimeUnit.SECONDS);
```

