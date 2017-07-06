package com.segment.analytics.android.integrations.batch;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.batch.android.Batch;
import com.batch.android.Config;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.segment.analytics.Analytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.GroupPayload;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BatchIntegration extends Integration
{
    public static final String BATCH_KEY = "Batch.com";

    private static final String LOGGER_TAG = "BatchSegmentIntegration";

    private static final String PLUGIN_VERSION = "Segment/1.0";
    
    private static boolean didSetBatchConfig = false;

    private Logger logger;

    static
    {
        System.setProperty("batch.plugin.version", PLUGIN_VERSION);
    }

    public static Factory getFactory(Context context)
    {
        final Context appContext = context.getApplicationContext();
        // Try to restore the saved configuration if any
        setupBatch(BatchIntegrationConfig.loadConfig(appContext));

        return new Factory()
        {
            @Override
            public Integration<?> create(ValueMap settings, Analytics analytics)
            {
                BatchIntegrationConfig config = new BatchIntegrationConfig(
                        settings.getString(BatchIntegrationConfig.BATCH_API_KEY)
                );

                config.canUseAdvertisingID = settings.getBoolean(BatchIntegrationConfig.CAN_USE_ADVERTISING_ID_KEY, BatchIntegrationConfig.DEFAULT_CAN_USE_ADVERTISING_ID);
                config.canUseAdvancedDeviceInformation = settings.getBoolean(BatchIntegrationConfig.CAN_USE_ADVANCED_DEVICE_INFO_KEY, BatchIntegrationConfig.DEFAULT_CAN_USE_ADV_DEVICE_INFO);
                config.gcmSenderID = settings.getString(BatchIntegrationConfig.GCM_SENDER_ID_KEY);
                
                config.save(appContext);
                setupBatch(config);

                return new BatchIntegration(analytics.logger(BATCH_KEY));
            }

            @Override
            public String key()
            {
                return BATCH_KEY;
            }
        };
    }

    public static void setupBatch(BatchIntegrationConfig integrationConfig)
    {
        if (integrationConfig == null)
        {
            return;
        }

        if (didSetBatchConfig)
        {
            // Config updates are delayed to the next app start
            return;
        }

        if (!BatchIntegrationConfig.enableAutomaticLifecycleManagement)
        {
            Log.v(LOGGER_TAG, "Batch will not use the settings provided by segment, because it has been disabled by the developer.");
            return;
        }
        
        if (!integrationConfig.isValid())
        {
            Log.v(LOGGER_TAG, "The configuration fetched from segment is invalid or incomplete. sBatch will not be automatically configured, and might not work properly.");
            return;
        }
        
        Config config = new Config(integrationConfig.apiKey);
        config.setCanUseAdvertisingID(integrationConfig.canUseAdvertisingID);
        config.setCanUseAdvancedDeviceInformation(integrationConfig.canUseAdvancedDeviceInformation);

        if (!TextUtils.isEmpty(integrationConfig.gcmSenderID))
        {
            Batch.Push.setGCMSenderId(integrationConfig.gcmSenderID);
        }

        Batch.setConfig(config);
        didSetBatchConfig = true;
    }

    BatchIntegration(Logger logger)
    {
        this.logger = logger;
    }

    @Override
    public void track(TrackPayload track)
    {
        super.track(track);

        String eventName = track.event();

        if (eventName == null || eventName.isEmpty())
        {
            String err = "Tried to track location but given location is null";
            logger.error(new BatchSegmentIntegrationException(err), err);
        }
        else
        {
            eventName = formatEventName(eventName);
            trackEvent(track, eventName);
        }
    }

    private static String formatEventName(String name)
    {
        name = name.replaceAll("(?<!^|[A-Z])[A-Z]", "_$0");
        name = name.replaceAll("^a-zA-Z0-9", "_");
        name = name.replaceAll("_+", "_");
        name = name.substring(0, Math.min(name.length(), 30));
        return name.toUpperCase(Locale.US);
    }

    private void trackTransaction(TrackPayload track)
    {
        double amount = track.properties().total();

        String currency = track.properties().currency();
        if (currency != null && !currency.isEmpty())
        {
            logger.verbose("Batch does not handle currency on transaction events");
        }

        Batch.User.trackTransaction(amount);
        logger.verbose("Tracking transaction event");
    }

    private void trackEvent(TrackPayload track, String eventName)
    {
        String label = track.properties().title();
        logger.verbose("Tracking event (name : " + eventName + ")");

        JSONObject datas = new JSONObject();
        try
        {
            Set<Map.Entry<String, Object>> entries = track.properties().entrySet();
            for (Map.Entry<String, Object> entry : entries)
            {
                String value = entry.getValue().toString();
                if (value != null && !value.isEmpty() && !"0".equals(value))
                {
                    datas.put(entry.getKey(), entry.toString());
                }
            }
        }
        catch (JSONException ex)
        {
            logger.error(ex, "Track event error");
        }

        Batch.User.trackEvent(eventName, label, datas);

        if (track.properties().total() != 0)
        {
            trackTransaction(track);
        }
    }

    @Override
    public void identify(IdentifyPayload identify)
    {
        logger.verbose("Identifying user");

        String userId = identify.userId();
        if (userId == null || userId.isEmpty())
        {
            logger.verbose("User not identified, userId is null or empty");
            return;
        }

        Batch.User.editor().setIdentifier(userId).save();
    }

    @Override
    public void reset()
    {
        logger.verbose("Resetting user");
        Batch.User.editor()
                .setIdentifier(null)
                .clearTags()
                .clearAttributes()
                .save();
    }

    @Override
    public void screen(ScreenPayload screen)
    {
        logger.verbose("Tracking screen event");
        final String name = screen.name();
        if (!TextUtils.isEmpty(name))
        {
            Batch.User.trackEvent("SEGMENT_SCREEN", name);
        }
    }

    @Override
    public void group(GroupPayload group)
    {
        String groupId = group.groupId();
        if (groupId == null || groupId.isEmpty())
        {
            return;
        }

        Batch.User.editor().setAttribute("SEGMENT_GROUP", groupId).save();
    }

    @Override
    public void onActivityStarted(Activity activity)
    {
        super.onActivityStarted(activity);

        if (BatchIntegrationConfig.enableAutomaticLifecycleManagement)
        {
            Batch.onStart(activity);
            logger.verbose("Batch started in activity " + activity.getLocalClassName());
        }
    }

    @Override
    public void onActivityStopped(Activity activity)
    {
        if (BatchIntegrationConfig.enableAutomaticLifecycleManagement)
        {
            Batch.onStop(activity);
            logger.verbose("Batch stopped in " + activity.getLocalClassName());
        }

        super.onActivityStopped(activity);
    }

    @Override
    public void onActivityDestroyed(Activity activity)
    {
        if (BatchIntegrationConfig.enableAutomaticLifecycleManagement)
        {
            Batch.onDestroy(activity);
            logger.verbose("Batch destroyed in " + activity.getLocalClassName());
        }

        super.onActivityDestroyed(activity);
    }
}