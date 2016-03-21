package com.citrix.wrekt.di.module;

import com.citrix.wrekt.controller.LoginController;
import com.citrix.wrekt.controller.api.ILoginController;
import com.citrix.wrekt.data.pref.IntegerPreference;
import com.citrix.wrekt.di.annotation.LoginStatePref;
import com.squareup.otto.Bus;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module()
public class LoginModule {

    @Provides
    @Singleton
    public ILoginController provideLoginController(@LoginStatePref IntegerPreference loginStatePref, Bus bus) {
        return new LoginController(loginStatePref, bus);
    }
}
