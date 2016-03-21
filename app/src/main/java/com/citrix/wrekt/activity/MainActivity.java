package com.citrix.wrekt.activity;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.controller.api.ILoginController;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.event.LogoutFailedEvent;
import com.citrix.wrekt.event.LogoutSuccessfulEvent;
import com.citrix.wrekt.fragment.LogoutDialogFragment;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

public class MainActivity extends BaseActivity implements ActionMenuView.OnMenuItemClickListener,
        LogoutDialogFragment.LogoutActionListener {

    private static final String TAG_LOGOUT_DIALOG = "TAG_LOGOUT_DIALOG";

    @Inject
    ILoginController loginController;

    private ActivityComponent activityComponent;

    private ActionMenuView actionMenuView;

    public static void start(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inject();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setTitle(R.string.main_activity_title);
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }

        actionMenuView = (ActionMenuView) findViewById(R.id.action_menu_view);
        actionMenuView.setOnMenuItemClickListener(this);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        Menu actionMenu = actionMenuView.getMenu();
        actionMenu.clear();
        getMenuInflater().inflate(R.menu.menu_main, actionMenu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager.findFragmentByTag(TAG_LOGOUT_DIALOG) == null) {
                    LogoutDialogFragment logoutDialogFragment = LogoutDialogFragment.newInstance();
                    logoutDialogFragment.show(fragmentManager, TAG_LOGOUT_DIALOG);
                }
                return true;
        }
        return false;
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
}
