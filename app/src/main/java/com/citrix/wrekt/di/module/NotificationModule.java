package com.citrix.wrekt.di.module;

import android.content.Context;

import com.citrix.wrekt.notification.IRequestNotifier;
import com.citrix.wrekt.notification.RequestNotifier;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class NotificationModule {

    @Provides
    @Singleton
    public IRequestNotifier provideRequestNotifier(Context context) {
        return new RequestNotifier(context);
    }
}
