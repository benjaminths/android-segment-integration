package com.segment.analytics.android.integrations.batch;

import com.segment.analytics.integrations.Logger;

class BatchIntegrationConfig
{
    boolean canUseAndroidID = false;
    boolean canUseAdvertisingID = false;
    boolean canUseAdvancedDeviceInformation = false;
    boolean canUseGoogleInstanceID = false;
    boolean autoBatchStartStop = false;
    String apiKey;
    String gcmSenderId;
    Logger logger;

    BatchIntegrationConfig(String apiKey, String gcmSenderId, Logger logger)
    {
        this.apiKey = apiKey;
        this.gcmSenderId = gcmSenderId;
        this.logger = logger;
    }

    BatchIntegrationConfig setCanUseAndroidID(boolean canUseAndroidID)
    {
        this.canUseAndroidID = canUseAndroidID;
        return this;
    }

    BatchIntegrationConfig setCanUseAdvertisingID(boolean canUseAdvertisingID)
    {
        this.canUseAdvertisingID = canUseAdvertisingID;
        return this;
    }

    BatchIntegrationConfig setCanUseAdvancedDeviceInformation(boolean canUseAdvancedDeviceInformation)
    {
        this.canUseAdvancedDeviceInformation = canUseAdvancedDeviceInformation;
        return this;
    }

    BatchIntegrationConfig setCanUseGoogleInstanceID(boolean canUseGoogleInstanceID)
    {
        this.canUseGoogleInstanceID = canUseGoogleInstanceID;
        return this;
    }

    BatchIntegrationConfig setAutoBatchStartStop(boolean autoBatchStartStop)
    {
        this.autoBatchStartStop = autoBatchStartStop;
        return this;
    }
}
