package com.batch.androidsegment.sample.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import com.batch.android.Batch;

public class BaseActivity extends AppCompatActivity
{
    @Override
    protected void onNewIntent(Intent intent)
    {
        Batch.onNewIntent(this, intent);

        super.onNewIntent(intent);
    }
}
