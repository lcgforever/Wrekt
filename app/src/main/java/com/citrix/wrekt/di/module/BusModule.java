package com.citrix.wrekt.di.module;

import com.citrix.wrekt.event.MainBus;
import com.squareup.otto.Bus;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module()
public class BusModule {

    @Provides
    @Singleton
    public Bus provideBus() {
        return new MainBus();
    }
}
