package com.segment.analytics.android.integrations.batch;

import android.app.Activity;
import android.location.Location;

import com.batch.android.Batch;
import com.batch.android.BatchUserDataEditor;
import com.batch.android.Config;
import com.batch.android.json.JSONObject;
import com.segment.analytics.Analytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.TrackPayload;

import java.util.Map;
import java.util.Set;

public class BatchIntegration extends Integration
{
    public static final Factory FACTORY = new Integration.Factory()
    {
        @Override
        public Integration<?> create(ValueMap settings, Analytics analytics)
        {
            BatchIntegrationConfig config = new BatchIntegrationConfig(
                    settings.getString("batch_api_key"),
                    settings.getString("gcm_key"),
                    analytics.logger(BATCH_KEY)
            );

            config.setAutoBatchStartStop(settings.getBoolean("autoStartStop", true));

            return new BatchIntegration(config);
        }

        @Override
        public String key()
        {
            return BATCH_KEY;
        }
    };

    public static final String BATCH_KEY = "Batch";

    // Event keys
    public static final String TRACK_DATA_KEY = "track_data";
    public static final String TRACK_TRANSACTION = "transaction";
    public static final String TRACK_LOCATION = "location";
    public static final String TRANSACTION_PROPERTIES_AMOUNT = "amount";

    private final BatchIntegrationConfig integrationConfig;

    BatchIntegration(BatchIntegrationConfig config)
    {
        this.integrationConfig = config;
        initializeBatch();
    }

    private void initializeBatch()
    {
        Batch.Push.setGCMSenderId(integrationConfig.gcmSenderId);

        Config config = new Config(integrationConfig.apiKey);
        config.setCanUseAndroidID(integrationConfig.canUseAndroidID)
                .setCanUseAdvertisingID(integrationConfig.canUseAdvertisingID)
                .setCanUseAdvancedDeviceInformation(integrationConfig.canUseAdvancedDeviceInformation)
                .setCanUseInstanceID(integrationConfig.canUseGoogleInstanceID);

        Batch.setConfig(config);
    }

    @Override
    public void track(TrackPayload track)
    {
        super.track(track);

        String eventName = track.event();

        if (eventName == null || eventName.isEmpty())
        {
            String err = "Tried to track location but given location is null";
            integrationConfig.logger.error(new BatchSegmentIntegrationException(err), err);
        }
        else
        {
            switch (eventName)
            {
                case TRACK_LOCATION:
                    trackLocation(track);
                    break;
                case TRACK_TRANSACTION:
                    trackTransaction(track);
                    break;
                default:
                    trackEvent(track, eventName);
            }
        }
    }

    private void trackLocation(TrackPayload track)
    {
        Location loc = (Location) track.properties().get(TRACK_LOCATION);
        if (loc != null)
        {
            Batch.User.trackLocation(loc);
            integrationConfig.logger.verbose("BatchIntegration : tracking location event");
        }
        else
        {
            String err = "Tried to track location but given location is null";
            integrationConfig.logger.error(new BatchSegmentIntegrationException(err), err);
        }
    }

    private void trackTransaction(TrackPayload track)
    {
        double amount = track.properties().getDouble(TRANSACTION_PROPERTIES_AMOUNT, Integer.MIN_VALUE);
        if (amount != Integer.MIN_VALUE)
        {
            JSONObject data = (JSONObject) track.properties().get(TRACK_DATA_KEY);
            Batch.User.trackTransaction(amount, data);
            integrationConfig.logger.verbose("BatchIntegration : tracking transaction event");
        }
        else
        {
            String err = "Tried to track transaction but no amount given";
            integrationConfig.logger.error(new BatchSegmentIntegrationException(err), err);
        }
    }

    private void trackEvent(TrackPayload track, String eventName)
    {
        String label = track.properties().title();
        JSONObject data = (JSONObject) track.properties().get(TRACK_DATA_KEY);
        Batch.User.trackEvent(eventName, label, data);
        integrationConfig.logger.verbose("BatchIntegration : tracking event (name : " + eventName + ")");
    }

    @Override
    public void identify(IdentifyPayload identify)
    {
        super.identify(identify);

        BatchUserDataEditor batchUserEditor = Batch.User.editor();

        String userId = identify.userId();
        if (userId != null && !userId.isEmpty())
        {
            batchUserEditor.setIdentifier(userId);
        }

        Set<Map.Entry<String, Object>> entries = identify.traits().entrySet();
        for (Map.Entry<String, Object> entry : entries)
        {
            String value = entry.getValue().toString();
            if (value != null && !value.isEmpty())
            {
                batchUserEditor.setAttribute(entry.getKey(), entry.toString());
            }
        }

        batchUserEditor.save();

        integrationConfig.logger.verbose("BatchIntegration : user identified");
    }

    @Override
    public void onActivityStarted(Activity activity)
    {
        super.onActivityStarted(activity);

        if (integrationConfig.autoBatchStartStop)
        {
            Batch.onStart(activity);
            integrationConfig.logger.verbose("BatchIntegration : Batch started in activity");
        }
    }

    @Override
    public void onActivityStopped(Activity activity)
    {
        if (integrationConfig.autoBatchStartStop)
        {
            Batch.onStop(activity);
            integrationConfig.logger.verbose("BatchIntegration : Batch stopped in activity");
        }

        super.onActivityStopped(activity);
    }

    @Override
    public void onActivityDestroyed(Activity activity)
    {
        if (integrationConfig.autoBatchStartStop)
        {
            Batch.onDestroy(activity);
            integrationConfig.logger.verbose("BatchIntegration : Batch destroyed in activity");
        }

        super.onActivityDestroyed(activity);
    }
}