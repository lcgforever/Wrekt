package com.citrix.wrekt.di.module;

import android.content.Context;

import com.citrix.wrekt.R;
import com.citrix.wrekt.firebase.FirebaseFactory;
import com.citrix.wrekt.firebase.FirebaseUrlFormatter;
import com.citrix.wrekt.firebase.api.IFirebaseFactory;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module()
public class FirebaseModule {

    @Provides
    @Singleton
    public IFirebaseFactory provideFirebaseFactory() {
        return new FirebaseFactory();
    }

    @Provides
    @Singleton
    public IFirebaseUrlFormatter provideFirebaseUrlFormatter(Context context) {
        return new FirebaseUrlFormatter(context, context.getString(R.string.firebase_base_url));
    }
}
