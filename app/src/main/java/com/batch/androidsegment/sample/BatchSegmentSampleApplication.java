package com.batch.androidsegment.sample;

import android.app.Application;

import com.segment.analytics.Options;
import com.segment.analytics.android.integrations.batch.BatchIntegration;
import com.segment.analytics.Analytics;

public class BatchSegmentSampleApplication extends Application
{
    private static final String SEGMENT_WRITE_KEY = "MySegmentWriteKey";

    @Override
    public void onCreate()
    {
        super.onCreate();

        Options options = new Options()
                .setIntegration(BatchIntegration.BATCH_KEY, true);

        Analytics analytics = new Analytics.Builder(this, SEGMENT_WRITE_KEY)
                .defaultOptions(options)
                .use(BatchIntegration.getFactory(this))
                .logLevel(Analytics.LogLevel.VERBOSE)
                .build();
        Analytics.setSingletonInstance(analytics);
    }
}
