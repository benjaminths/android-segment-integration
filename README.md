analytics-android-integration-batch
======================================

Batch.com integration for [analytics-android](https://github.com/segmentio/analytics-android).

## Usage

After adding the dependency, you must register the integration with our SDK. To do this, import the Batch integration:


```java
import com.segment.analytics.android.integrations.batch.BatchIntegration;
```

And add the following line:

```java
Analytics analytics = new Analytics.Builder(this, "write_key")
                .use(BatchIntegration.getFactory(this))
                .build();
```

Please also add the following to all of your activities:

```
@Override
protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    Batch.onNewIntent(intent);
}
```

Track an event :

```java
Properties trackProperties = new Properties();
trackProperties.putTitle("MyLabel");
Analytics.with(context).track("MyEventName", trackProperties);
```

The sdk-segment-integration module contains the Batch integration, and the app module contains an app sample.

## Integrating with other Batch features (recomended)

If you want to use this integration, but use other Batch features than Analytics, you may want to follow the [standard integration steps](https://batch.com/doc/android/sdk-integration/initial-setup.html), and then add this line in your Application subclass, **before** any segment call:

```java
BatchIntegrationConfig.enableAutomaticLifecycleManagement = false;
```

This will let your code fully control the configuration of the SDK, and calling of the lifecycle methods. Note that by enabling this, you will have to add Batch.onStart/onStop/onDestroy calls yourself, as indicated by Batch's documentation.

