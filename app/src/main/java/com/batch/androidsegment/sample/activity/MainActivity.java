package com.batch.androidsegment.sample.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.batch.android.Batch;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.segment.analytics.Traits;
import com.segment.analytics.android.integrations.batch.BatchIntegration;
import com.batch.androidsegment.sample.R;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;

public class MainActivity extends BaseActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_segment_track_transaction).setOnClickListener(new Button.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final JSONObject data = new JSONObject();

                final JSONArray foo = new JSONArray();
                foo.put("bar");
                foo.put("barre");

                try
                {
                    data.put("foo", foo);
                    data.put("number", 2);
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }

                Properties trackProperties = new Properties();
                trackProperties.putValue(BatchIntegration.TRANSACTION_PROPERTIES_AMOUNT, 12);
                trackProperties.put(BatchIntegration.TRACK_DATA_KEY, data);
                Analytics.with(MainActivity.this).track(BatchIntegration.TRACK_TRANSACTION, trackProperties);
            }
        });

        findViewById(R.id.btn_segment_track_event).setOnClickListener(new Button.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final JSONObject data = new JSONObject();
                try
                {
                    data.put("data_example", true);
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }

                Properties trackProperties = new Properties();
                trackProperties.putTitle("MyTrackPropertiesTitle");
                trackProperties.putValue(BatchIntegration.TRACK_DATA_KEY, data); // Could be removed
                Analytics.with(MainActivity.this).track("MyEventName", trackProperties);
            }
        });

        final EditText etName = (EditText) findViewById(R.id.et_name);
        findViewById(R.id.btn_update_name).setOnClickListener(new Button.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String name = etName.getText().toString();
                Analytics.with(MainActivity.this).identify(Batch.User.getInstallationID(), new Traits().putName(name), null);
            }
        });
    }
}
