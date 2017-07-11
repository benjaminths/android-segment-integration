package com.segment.analytics.android.integrations.batch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.batch.android.Batch;
import com.batch.android.BatchUserDataEditor;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.test.GroupPayloadBuilder;
import com.segment.analytics.test.ScreenPayloadBuilder;
import com.segment.analytics.test.TrackPayloadBuilder;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25, manifest = Config.NONE)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "com.batch.android.json.*"})
@PrepareForTest({Batch.class, Batch.User.class})
public class BatchIntegrationTest
{
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Mock
    private Analytics analytics;

    @Mock
    private BatchUserDataEditor userDataEditor;

    private Logger logger;

    private Application context;
    private SharedPreferences preferences;

    @Before
    public void batchIntegrationInit() throws NoSuchFieldException, IllegalAccessException
    {
        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(Batch.class);

        Field preferencesField = BatchIntegrationConfig.class.getDeclaredField("PREFERENCES_NAME");
        preferencesField.setAccessible(true);
        final String preferencesKey = (String) preferencesField.get(null);

        context = RuntimeEnvironment.application;
        preferences = context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE);

        logger = Logger.with(Analytics.LogLevel.DEBUG).subLog(BatchIntegration.BATCH_KEY);

        PowerMockito.when(analytics.getApplication()).thenReturn(context);
        PowerMockito.when(analytics.logger(BatchIntegration.BATCH_KEY)).thenReturn(logger);

        // Mock User data editor
        PowerMockito.mockStatic(Batch.User.class);
        PowerMockito.when(Batch.User.editor()).thenReturn(userDataEditor);
        PowerMockito.when(userDataEditor.setIdentifier(Mockito.anyString())).thenReturn(userDataEditor);
        PowerMockito.when(userDataEditor.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(userDataEditor);
        PowerMockito.when(userDataEditor.clearTags()).thenReturn(userDataEditor);
        PowerMockito.when(userDataEditor.clearAttributes()).thenReturn(userDataEditor);
    }

    @After
    public void validate()
    {
        Mockito.validateMockitoUsage();
    }

    @SuppressLint("ApplySharedPref")
    @Test
    public void factoryNoAPIKey()
    {
        preferences.edit().clear().commit();

        BatchIntegration.getFactory(context).create(new ValueMap(), analytics);

        // The API key was null. We don't wan't the config to be saved
        assertThat(preferences.contains(BatchIntegrationConfig.APIKEY_KEY)).isFalse();
    }

    @SuppressLint("ApplySharedPref")
    @Test
    public void factory()
    {
        preferences.edit().clear().commit();

        final String FAKE_GCM_ID = "886025556782";
        final String FAKE_API_KEY = "12345-ABCD";

        ValueMap settings = new ValueMap()
                .putValue(BatchIntegrationConfig.CAN_USE_ADVANCED_DEVICE_INFO_KEY, true)
                .putValue(BatchIntegrationConfig.CAN_USE_ADVERTISING_ID_KEY, false)
                .putValue(BatchIntegrationConfig.GCM_SENDER_ID_KEY, FAKE_GCM_ID)
                .putValue(BatchIntegrationConfig.APIKEY_KEY, FAKE_API_KEY);

        BatchIntegration.getFactory(context).create(settings, analytics);

        // Check preferences to see if it contains the data
        assertThat(preferences.getString(BatchIntegrationConfig.APIKEY_KEY, "")).isEqualTo(FAKE_API_KEY);
        assertThat(preferences.getString(BatchIntegrationConfig.GCM_SENDER_ID_KEY, "")).isEqualTo(FAKE_GCM_ID);
        assertThat(preferences.getBoolean(BatchIntegrationConfig.CAN_USE_ADVANCED_DEVICE_INFO_KEY, false)).isTrue();
        assertThat(preferences.getBoolean(BatchIntegrationConfig.CAN_USE_ADVERTISING_ID_KEY, true)).isFalse();
    }

    @Test
    public void trackCamelCaseEvent() throws JSONException
    {
        PowerMockito.mockStatic(Batch.User.class);

        Integration batchIntegration = BatchIntegration.getFactory(context).create(new ValueMap(), analytics);
        batchIntegration.track(new TrackPayloadBuilder().event("myEvent").build());

        PowerMockito.verifyStatic(Mockito.times(1));

        // Check that trackEvent was called
        Batch.User.trackEvent("MY_EVENT", null, null);
    }

    @Test
    public void trackEventTooLong()
    {
        PowerMockito.mockStatic(Batch.User.class);

        Integration batchIntegration = BatchIntegration.getFactory(context).create(new ValueMap(), analytics);
        batchIntegration.track(new TrackPayloadBuilder().event("Test Event Too Long_Aaaaaaaaaaaaaaaaaaaa").build());

        PowerMockito.verifyStatic(Mockito.times(1));

        // Check that trackEvent was called
        Batch.User.trackEvent("TEST_EVENT_TOO_LONG_AAAAAAAAAA", null, null);
    }

    @Test
    public void trackTransactionWithTotal() throws JSONException
    {
        final double TOTAL = 12.499;

        Integration batchIntegration = BatchIntegration.getFactory(context).create(new ValueMap(), analytics);
        batchIntegration.track(new TrackPayloadBuilder().event("anotherEvent")
                .properties(new Properties().putTotal(TOTAL).putTitle("order"))
                .build());

        PowerMockito.verifyStatic(Mockito.times(1));

        // Check that trackEvent was called (with label and jsonObject)
        Batch.User.trackEvent(
                Mockito.eq("ANOTHER_EVENT"),
                Mockito.eq("order"),
                jsonEq(new JSONObject().put("total", TOTAL))
        );

        PowerMockito.verifyStatic(Mockito.times(1));

        // Check that trackTransaction was called too
        Batch.User.trackTransaction(TOTAL);
    }

    @Test
    public void trackTransactionWithRevenue() throws JSONException
    {
        final double REVENUE = 18.63;

        Integration batchIntegration = BatchIntegration.getFactory(context).create(new ValueMap(), analytics);
        batchIntegration.track(new TrackPayloadBuilder().event("anotherEvent")
                .properties(new Properties().putRevenue(REVENUE).putTitle("order"))
                .build());

        PowerMockito.verifyStatic(Mockito.times(1));

        // Check that trackEvent was called (with label and jsonObject)
        Batch.User.trackEvent(
                Mockito.eq("ANOTHER_EVENT"),
                Mockito.eq("order"),
                jsonEq(new JSONObject().put("revenue", REVENUE))
        );

        PowerMockito.verifyStatic(Mockito.times(1));

        // Check that trackTransaction was called too
        Batch.User.trackTransaction(REVENUE);
    }

    @Test
    public void trackTransactionWithValue() throws JSONException
    {
        final double VALUE = 12.489;

        Integration batchIntegration = BatchIntegration.getFactory(context).create(new ValueMap(), analytics);
        batchIntegration.track(new TrackPayloadBuilder().event("anotherEvent")
                .properties(new Properties().putValue(VALUE).putTitle("order"))
                .build());

        PowerMockito.verifyStatic(Mockito.times(1));

        // Check that trackEvent was called (with label and jsonObject)
        Batch.User.trackEvent(
                Mockito.eq("ANOTHER_EVENT"),
                Mockito.eq("order"),
                jsonEq(new JSONObject().put("value", VALUE))
        );

        PowerMockito.verifyStatic(Mockito.times(1));

        // Check that trackTransaction was called too
        Batch.User.trackTransaction(VALUE);
    }

    @Test
    public void screen()
    {
        final String SCREEN_NAME = "main_screen";

        Integration batchIntegration = BatchIntegration.getFactory(context).create(new ValueMap(), analytics);
        batchIntegration.screen(new ScreenPayloadBuilder().name(SCREEN_NAME).build());

        PowerMockito.verifyStatic(Mockito.times(1));

        // Check that trackEvent was called
        Batch.User.trackEvent("SEGMENT_SCREEN", SCREEN_NAME);
    }

    @Test
    public void group()
    {
        final String GROUP_ID = "FooGroup";

        Integration batchIntegration = BatchIntegration.getFactory(context).create(new ValueMap(), analytics);
        batchIntegration.group(new GroupPayloadBuilder().groupId(GROUP_ID).build());

        Mockito.verify(Batch.User.editor()).setAttribute("SEGMENT_GROUP", GROUP_ID);
        Mockito.verify(Batch.User.editor()).save();
    }

    @Test
    public void identifyWithUserIDAndAnonID()
    {
        final String USER_ID = "userID";
        final String ANON_ID = "anonID";

        Integration batchIntegration = BatchIntegration.getFactory(context).create(new ValueMap(), analytics);
        batchIntegration.identify(new IdentifyPayload.Builder().userId(USER_ID).anonymousId(ANON_ID).build());

        // Check if batchIntegration.identify called Batch.User.editor().setIdentifier(...).save()
        Mockito.verify(Batch.User.editor()).setIdentifier(USER_ID);
        Mockito.verify(Batch.User.editor()).save();
    }

    @Test
    public void reset()
    {
        Integration batchIntegration = BatchIntegration.getFactory(context).create(new ValueMap(), analytics);
        batchIntegration.reset();

        Mockito.verify(Batch.User.editor()).setIdentifier(null);
        Mockito.verify(Batch.User.editor()).clearTags();
        Mockito.verify(Batch.User.editor()).clearAttributes();
        Mockito.verify(Batch.User.editor()).save();
    }

    @Test
    public void testBatchStartIfAutoStartEnabled()
    {
        BatchIntegrationConfig.enableAutomaticLifecycleManagement = true;

        Integration batchIntegration = BatchIntegration.getFactory(context).create(new ValueMap(), analytics);
        Activity activity = Robolectric.buildActivity(Activity.class).start().get();
        batchIntegration.onActivityStarted(activity);

        // Check if batch was started (not stopped/destroyed)
        PowerMockito.verifyStatic(Mockito.times(1));
        Batch.onStart(activity);

        PowerMockito.verifyStatic(Mockito.times(0));
        Batch.onStop(activity);

        PowerMockito.verifyStatic(Mockito.times(0));
        Batch.onDestroy(activity);

        activity = Robolectric.buildActivity(Activity.class).start().stop().get();
        batchIntegration.onActivityStarted(activity);
        batchIntegration.onActivityStopped(activity);

        // Check if batch was stopped (not destroyed)
        PowerMockito.verifyStatic(Mockito.times(1));
        Batch.onStop(activity);

        PowerMockito.verifyStatic(Mockito.times(0));
        Batch.onDestroy(activity);

        activity = Robolectric.buildActivity(Activity.class).start().stop().destroy().get();
        batchIntegration.onActivityStarted(activity);
        batchIntegration.onActivityStopped(activity);
        batchIntegration.onActivityDestroyed(activity);

        // Check if batch was destroyed (not destroyed)
        PowerMockito.verifyStatic(Mockito.times(1));
        Batch.onDestroy(activity);
    }

    @Test
    public void testBatchNotStartIfAutoStartDisabled()
    {
        BatchIntegrationConfig.enableAutomaticLifecycleManagement = false;

        Integration batchIntegration = BatchIntegration.getFactory(context).create(new ValueMap(), analytics);
        Activity activity = Robolectric.buildActivity(Activity.class).start().stop().destroy().get();
        batchIntegration.onActivityStarted(activity);

        batchIntegration.onActivityStarted(activity);
        batchIntegration.onActivityStopped(activity);
        batchIntegration.onActivityDestroyed(activity);

        // Check if batch was started / stopped / destroyed
        PowerMockito.verifyStatic(Mockito.times(0));
        Batch.onStart(activity);

        PowerMockito.verifyStatic(Mockito.times(0));
        Batch.onStop(activity);

        PowerMockito.verifyStatic(Mockito.times(0));
        Batch.onDestroy(activity);
    }

    @Test
    public void testPluginEnvVarSet()
    {
        assertThat(System.getProperty("batch.plugin.version", null)).isNotNull();
    }

    public static JSONObject jsonEq(JSONObject expected)
    {
        return Mockito.argThat(new JSONObjectMatcher(expected));
    }

    private static class JSONObjectMatcher extends TypeSafeMatcher<JSONObject>
    {
        private final JSONObject expected;

        private JSONObjectMatcher(JSONObject expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean matchesSafely(JSONObject jsonObject)
        {
            return expected.toString().equals(jsonObject.toString());
        }

        @Override
        public void describeTo(Description description)
        {
            description.appendText(expected.toString());
        }
    }
}
