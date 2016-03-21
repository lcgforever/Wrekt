package com.citrix.wrekt.di.module;

import android.app.Activity;
import android.content.Context;

import com.citrix.wrekt.di.annotation.ForActivity;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module()
public class ActivityModule {

    private Activity activity;

    public ActivityModule(Activity activity) {
        this.activity = activity;
    }

    @Provides
    @Singleton
    @ForActivity
    public Context provideActivityContext() {
        return activity;
    }
}
