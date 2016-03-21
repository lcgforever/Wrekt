package com.citrix.wrekt.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;
import com.squareup.otto.Bus;

import javax.inject.Inject;

public abstract class BaseActivity extends AppCompatActivity {

    @Inject
    Bus bus;

    private ActivityComponent activityComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WrektApplication application = (WrektApplication) getApplication();
        activityComponent = application.getAppComponent().plus(new ActivityModule(this));
        activityComponent.inject(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        bus.unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityComponent = null;
    }

    protected abstract void inject();
}
