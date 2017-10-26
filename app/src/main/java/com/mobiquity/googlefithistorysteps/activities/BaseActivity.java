package com.mobiquity.googlefithistorysteps.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.mobiquity.googlefithistorysteps.MyApplication;
import com.mobiquity.googlefithistorysteps.di.manager.SharedPreferenceManager;

import javax.inject.Inject;

import butterknife.ButterKnife;

/**
 * Created by hirenpatel on 25/10/17.
 */

public abstract class BaseActivity extends AppCompatActivity  {

    @Inject
    SharedPreferenceManager sharedPreferenceManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getViewId());
        ButterKnife.bind(this);
        MyApplication.get(this).getApplicationComponent().inject(this);
        activityOnCreate(savedInstanceState);
    }

    protected abstract int getViewId();

    protected abstract void activityOnCreate(@Nullable Bundle savedInstanceState);


}
