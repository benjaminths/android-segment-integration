analytics-android-integration-batch
======================================

Batch integration for [analytics-android](https://github.com/segmentio/analytics-android).

## Usage

After adding the dependency, you must register the integration with our SDK. To do this, import the Batch integration:


```
import com.segment.analytics.android.integrations.batch.BatchIntegration;
```

And add the following line:

```
Analytics analytics = new Analytics.Builder(this, "write_key")
                .use(BatchIntegration.FACTORY)
                .logLevel(Analytics.LogLevel.VERBOSE)
                .build();
```

Track an event :

```
Properties trackProperties = new Properties();
trackProperties.putTitle("MyLabel");
Analytics.with(context).track("MyEventName", trackProperties);
```