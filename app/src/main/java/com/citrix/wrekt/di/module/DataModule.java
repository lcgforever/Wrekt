package com.citrix.wrekt.di.module;

import android.content.Context;
import android.content.SharedPreferences;

import com.citrix.wrekt.data.LoginState;
import com.citrix.wrekt.data.pref.IntegerPreference;
import com.citrix.wrekt.data.pref.LongPreference;
import com.citrix.wrekt.data.pref.StringSetPreference;
import com.citrix.wrekt.di.annotation.LoginExpireTimePref;
import com.citrix.wrekt.di.annotation.LoginStatePref;
import com.citrix.wrekt.di.annotation.MyChannelIdSetPref;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static android.content.Context.MODE_PRIVATE;

@Module()
public class DataModule {

    private static final String PREFERENCES_API = "com.citrix.wrekt.PREFERENCES_API";
    private static final String PREF_LOGIN_STATE = "PREF_LOGIN_STATE";
    private static final String PREF_LOGIN_EXPIRE_TIME = "PREF_LOGIN_EXPIRE_TIME";
    private static final String PREF_MY_CHANNEL_ID_SET = "PREF_MY_CHANNEL_ID_SET";

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

    @Provides
    @Singleton
    @LoginExpireTimePref
    public LongPreference provideLoginExireTimePreference(SharedPreferences sharedPreferences) {
        return new LongPreference(sharedPreferences, PREF_LOGIN_EXPIRE_TIME, 0);
    }

    @Provides
    @Singleton
    @MyChannelIdSetPref
    public StringSetPreference provideMyChannelIdSetPref(SharedPreferences sharedPreferences) {
        return new StringSetPreference(sharedPreferences, PREF_MY_CHANNEL_ID_SET);
    }
}
