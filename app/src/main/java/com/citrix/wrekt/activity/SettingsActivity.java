package com.citrix.wrekt.activity;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.controller.api.ILoginController;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.event.LogoutFailedEvent;
import com.citrix.wrekt.event.LogoutSuccessfulEvent;
import com.citrix.wrekt.fragment.LogoutDialogFragment;
import com.citrix.wrekt.fragment.SettingsFragment;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

public class SettingsActivity extends BaseActivity implements LogoutDialogFragment.LogoutActionListener,
        SettingsFragment.SettingsClickListener {

    private static final String TAG_LOGOUT_DIALOG = "TAG_LOGOUT_DIALOG";

    @Inject
    ILoginController loginController;

    private ActivityComponent activityComponent;
    private FragmentManager fragmentManager;

    public static void start(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        inject();

        fragmentManager = getFragmentManager();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, SettingsFragment.newInstance())
                    .commit();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        hideLogoutDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityComponent = null;
    }

    @Override
    protected void inject() {
        WrektApplication application = (WrektApplication) getApplication();
        activityComponent = application.getAppComponent().plus(new ActivityModule(this));
        activityComponent.inject(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onLogoutSettingClicked() {
        showLogoutDialog();
    }

    @Override
    public void onLogoutConfirmed() {
        loginController.logout();
    }

    @Subscribe
    public void onLogoutSuccessfulEventReceived(LogoutSuccessfulEvent event) {
        LoginActivity.start(this);
        finish();
    }

    @Subscribe
    public void onLogoutFailedEventReceived(LogoutFailedEvent event) {
        Toast.makeText(this, R.string.logout_error_message, Toast.LENGTH_LONG).show();
    }

    private void showLogoutDialog() {
        LogoutDialogFragment fragment = (LogoutDialogFragment) fragmentManager.findFragmentByTag(TAG_LOGOUT_DIALOG);
        if (fragment == null) {
            fragment = LogoutDialogFragment.newInstance();
        }
        fragment.show(fragmentManager, TAG_LOGOUT_DIALOG);
    }

    private void hideLogoutDialog() {
        LogoutDialogFragment fragment = (LogoutDialogFragment) fragmentManager.findFragmentByTag(TAG_LOGOUT_DIALOG);
        if (fragment != null) {
            fragment.dismiss();
        }
    }
}
