package com.mobiquity.googlefithistorysteps.di.component;

import com.mobiquity.googlefithistorysteps.MyApplication;
import com.mobiquity.googlefithistorysteps.activities.BaseActivity;
import com.mobiquity.googlefithistorysteps.di.module.ApplicationModule;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by hirenpatel on 25/10/17.
 */

@Singleton
@Component(modules = ApplicationModule.class)
public interface ApplicationComponent {
    void inject(MyApplication myApplication);
    void inject(BaseActivity baseActivity);
}