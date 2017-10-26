package com.mobiquity.googlefithistorysteps.di.module;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import dagger.Module;
import dagger.Provides;

/**
 * Created by hirenpatel on 25/10/17.
 */

@Module
public class ApplicationModule {

    private final Application application;

    public ApplicationModule(Application app) {
        application = app;
    }

    @Provides
    SharedPreferences getSharePreference() {
        return application.getSharedPreferences("SP_GOOGLE_FIT_STEPS_HISTORY", Context.MODE_PRIVATE);
    }


    @Provides
    Context getAppContext(){
        return application.getApplicationContext();
    }
}