package com.mobiquity.googlefithistorysteps.di.manager;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by hirenpatel on 25/10/17.
 */

@Singleton
public class SharedPreferenceManager {

    public static String KEY_LAST_TIME_STAMP = "sp_last_timestamp";
    SharedPreferences sharedPreferences;

    @Inject
    public SharedPreferenceManager(SharedPreferences preferences) {
        sharedPreferences = preferences;
    }

    public void storeLong(String key, long value) {
        sharedPreferences.edit().putLong(key, value).apply();
    }

    public long getLong(String key) {
        return sharedPreferences.getLong(key, 0);
    }
}
