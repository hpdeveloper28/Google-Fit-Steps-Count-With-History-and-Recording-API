package com.mobiquity.googlefithistorysteps;

import android.app.Application;
import android.content.Context;

import com.mobiquity.googlefithistorysteps.di.component.ApplicationComponent;
import com.mobiquity.googlefithistorysteps.di.component.DaggerApplicationComponent;
import com.mobiquity.googlefithistorysteps.di.module.ApplicationModule;

/**
 * Created by hirenpatel on 25/10/17.
 */

public class MyApplication extends Application {

    private static Context context;
    private ApplicationComponent applicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        applicationComponent = DaggerApplicationComponent
                .builder()
                .applicationModule(new ApplicationModule(this))
                .build();
        applicationComponent.inject(this);
    }

    public ApplicationComponent getApplicationComponent() {
        return applicationComponent;
    }

    public static MyApplication get() {
        return (MyApplication) context;
    }

    public static MyApplication get(Context context) {
        return (MyApplication) context.getApplicationContext();
    }
}
