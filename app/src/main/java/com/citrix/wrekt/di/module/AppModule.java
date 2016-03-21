package com.citrix.wrekt.di.module;

import android.content.Context;

import com.citrix.wrekt.WrektApplication;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module()
public class AppModule {

    private WrektApplication application;

    public AppModule(WrektApplication application) {
        this.application = application;
    }

    @Provides
    @Singleton
    public Context provideApplicationContext() {
        return application;
    }
}
