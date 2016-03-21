package com.citrix.wrekt;

import android.app.Application;

import com.citrix.wrekt.di.component.AppComponent;
import com.citrix.wrekt.di.component.DaggerAppComponent;
import com.citrix.wrekt.di.module.AppModule;
import com.facebook.FacebookSdk;

public class WrektApplication extends Application {

    private AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        createAppComponentAndInject();

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
