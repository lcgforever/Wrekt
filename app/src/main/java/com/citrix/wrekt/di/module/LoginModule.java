package com.citrix.wrekt.di.module;

import com.citrix.wrekt.controller.LoginController;
import com.citrix.wrekt.controller.api.ILoginController;
import com.citrix.wrekt.data.pref.IntegerPreference;
import com.citrix.wrekt.data.pref.LongPreference;
import com.citrix.wrekt.di.annotation.LoginExpireTimePref;
import com.citrix.wrekt.di.annotation.LoginStatePref;
import com.citrix.wrekt.firebase.api.IFirebaseFactory;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;
import com.squareup.otto.Bus;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module()
public class LoginModule {

    @Provides
    @Singleton
    public ILoginController provideLoginController(@LoginStatePref IntegerPreference loginStatePref,
                                                   @LoginExpireTimePref LongPreference loginTimeExpirePref,
                                                   Bus bus,
                                                   IFirebaseFactory firebaseFactory,
                                                   IFirebaseUrlFormatter firebaseUrlFormatter) {
        return new LoginController(loginStatePref, loginTimeExpirePref, bus, firebaseFactory, firebaseUrlFormatter);
    }
}
