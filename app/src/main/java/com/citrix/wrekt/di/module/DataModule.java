package com.citrix.wrekt.di.module;

import android.content.Context;
import android.content.SharedPreferences;

import com.citrix.wrekt.data.LoginState;
import com.citrix.wrekt.data.pref.IntegerPreference;
import com.citrix.wrekt.di.annotation.LoginStatePref;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static android.content.Context.MODE_PRIVATE;

@Module()
public class DataModule {

    public static final String PREFERENCES_API = "com.citrix.wrekt.PREFERENCES_API";
    public static final String PREF_LOGIN_STATE = "PREF_LOGIN_STATE";

    @Provides
    @Singleton
    SharedPreferences provideSharedPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_API, MODE_PRIVATE);
    }

    @Provides
    @Singleton
    @LoginStatePref
    public IntegerPreference provideLoginStatePreference(SharedPreferences sharedPreferences) {
        return new IntegerPreference(sharedPreferences, PREF_LOGIN_STATE, LoginState.NOT_LOGGED_IN.getValue());
    }
}
