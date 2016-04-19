package com.citrix.wrekt;

import android.app.Application;

import com.citrix.wrekt.di.component.AppComponent;
import com.citrix.wrekt.di.component.DaggerAppComponent;
import com.citrix.wrekt.di.module.AppModule;
import com.facebook.FacebookSdk;
import com.firebase.client.Firebase;

public class WrektApplication extends Application {

    private AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        createAppComponentAndInject();

        // Initialize Firebase SDK
        Firebase.setAndroidContext(this);
        // Enable Firebase local data storage
        Firebase.getDefaultConfig().setPersistenceEnabled(true);

        // Initialize Facebook SDK
        FacebookSdk.sdkInitialize(this);
    }

    private void createAppComponentAndInject() {
        appComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
        appComponent.inject(this);
    }

    public AppComponent getAppComponent() {
        return appComponent;
    }
}
