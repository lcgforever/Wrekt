package com.citrix.wrekt.di.module;

import android.content.Context;
import android.content.SharedPreferences;

import com.citrix.wrekt.data.LoginState;
import com.citrix.wrekt.data.pref.IntegerPreference;
import com.citrix.wrekt.data.pref.LongPreference;
import com.citrix.wrekt.data.pref.StringPreference;
import com.citrix.wrekt.di.annotation.LoginExpireTimePref;
import com.citrix.wrekt.di.annotation.LoginStatePref;
import com.citrix.wrekt.di.annotation.UidPref;
import com.citrix.wrekt.di.annotation.UserEmailPref;
import com.citrix.wrekt.di.annotation.UsernamePref;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static android.content.Context.MODE_PRIVATE;

@Module()
public class DataModule {

    private static final String PREFERENCES_API = "com.citrix.wrekt.PREFERENCES_API";
    private static final String PREF_LOGIN_STATE = "PREF_LOGIN_STATE";
    private static final String PREF_LOGIN_EXPIRE_TIME = "PREF_LOGIN_EXPIRE_TIME";
    private static final String PREF_UID = "PREF_UID";
    private static final String PREF_USERNAME = "PREF_USERNAME";
    private static final String PREF_USER_EMAIL = "PREF_USER_EMAIL";

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
    @UidPref
    public StringPreference provideUidPreference(SharedPreferences sharedPreferences) {
        return new StringPreference(sharedPreferences, PREF_UID, "");
    }

    @Provides
    @Singleton
    @UsernamePref
    public StringPreference provideUsernamePreference(SharedPreferences sharedPreferences) {
        return new StringPreference(sharedPreferences, PREF_USERNAME, "");
    }

    @Provides
    @Singleton
    @UserEmailPref
    public StringPreference provideUserEmailPref(SharedPreferences sharedPreferences) {
        return new StringPreference(sharedPreferences, PREF_USER_EMAIL);
    }
}
