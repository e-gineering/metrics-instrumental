# metrics-instrumental

## A Dropwizard Metrics reporter for Instrumental

This is a reporter for the [Dropwizard Metrics library](http://dropwizard.github.io/metrics) which publishes metrics to [Instrumental](http://instrumentalapp.com), a cloud-based metrics tool.

### Example
```
Instrumental instrumental = new Instrumental("your_api_key", "collector.instrumentalapp.com", 8000);
InstrumentalReporter instrumentalReporter = InstrumentalReporter.forRegistry(registry)
                                                                .convertRatesTo(TimeUnit.SECONDS)
                                                                .convertDurationsTo(TimeUnit.MILLISECONDS)
                                                                .build(instrumental);
reporter.start(1, TimeUnit.SECONDS);
```
