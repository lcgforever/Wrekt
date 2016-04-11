package com.citrix.wrekt.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;
import com.squareup.otto.Bus;

import javax.inject.Inject;

public abstract class BaseFragment extends Fragment {

    @Inject
    Bus bus;

    private ActivityComponent activityComponent;

    public BaseFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WrektApplication application = (WrektApplication) getActivity().getApplication();
        activityComponent = application.getAppComponent().plus(new ActivityModule(getActivity()));
        activityComponent.inject(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        bus.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        bus.unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activityComponent = null;
    }

    protected abstract void inject();
}
