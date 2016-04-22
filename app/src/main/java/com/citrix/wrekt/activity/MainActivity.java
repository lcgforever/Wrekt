package com.citrix.wrekt.activity;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.controller.api.ILoginController;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.event.LogoutFailedEvent;
import com.citrix.wrekt.event.LogoutSuccessfulEvent;
import com.citrix.wrekt.fragment.AllChannelsFragment;
import com.citrix.wrekt.fragment.MyChannelsFragment;
import com.citrix.wrekt.fragment.dialog.LogoutDialogFragment;
import com.citrix.wrekt.service.FriendRequestService;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

public class MainActivity extends BaseActivity implements ActionMenuView.OnMenuItemClickListener,
        LogoutDialogFragment.LogoutActionListener, View.OnClickListener {

    private static final String TAG_LOGOUT_DIALOG = "TAG_LOGOUT_DIALOG";
    private static final int TAB_CHANNEL_COUNT = 2;
    private static final int TAB_ALL_CHANNELS = 0;
    private static final int TAB_MY_CHANNELS = 1;

    @Inject
    ILoginController loginController;

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ActionMenuView actionMenuView;
    private FloatingActionButton createChannelFAB;

    private ActivityComponent activityComponent;
    private FragmentManager fragmentManager;

    public static void start(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inject();

        fragmentManager = getFragmentManager();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setHomeButtonEnabled(false);
            supportActionBar.setDisplayHomeAsUpEnabled(false);
        }

        tabLayout = (TabLayout) findViewById(R.id.channel_tab_layout);
        viewPager = (ViewPager) findViewById(R.id.channel_view_pager);
        actionMenuView = (ActionMenuView) findViewById(R.id.action_menu_view);
        createChannelFAB = (FloatingActionButton) findViewById(R.id.create_channel_fab);

        actionMenuView.setOnMenuItemClickListener(this);
        createChannelFAB.setOnClickListener(this);

        setupViewPager(savedInstanceState);

        // TODO: figure out where is best to start and stop this service
        FriendRequestService.start(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        hideLogoutDialog();
        super.onSaveInstanceState(outState);
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
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_friends:
                FriendActivity.start(this);
                return true;

            case R.id.action_logout:
                showLogoutDialog();
                return true;

            case R.id.action_settings:
                SettingsActivity.start(this);
                return true;

            case R.id.action_about_app:
                AboutAppActivity.start(this);
                return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.create_channel_fab:
                CreateChannelActivity.start(this);
                break;
        }
    }

    @Override
    public void onLogoutConfirmed() {
        loginController.logout();
    }

    @Subscribe
    public void onLogoutSuccessfulEventReceived(LogoutSuccessfulEvent event) {
        FriendRequestService.stop(this);
        LoginActivity.start(this);
        finish();
    }

    @Subscribe
    public void onLogoutFailedEventReceived(LogoutFailedEvent event) {
        Toast.makeText(this, R.string.logout_error_message, Toast.LENGTH_SHORT).show();
    }

    private void setupViewPager(Bundle savedInstanceState) {
        ChannelTabsFragmentAdapter tabsAdapter = new ChannelTabsFragmentAdapter(getSupportFragmentManager());
        viewPager.setAdapter(tabsAdapter);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        if (savedInstanceState == null) {
            // Temporary fix for tab text blink upon changing page
            tabLayout.post(new Runnable() {
                @Override
                public void run() {
                    tabLayout.setupWithViewPager(viewPager);
                }
            });
        } else {
            tabLayout.setupWithViewPager(viewPager);
        }
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


    private class ChannelTabsFragmentAdapter extends FragmentPagerAdapter {

        public ChannelTabsFragmentAdapter(android.support.v4.app.FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case TAB_ALL_CHANNELS:
                    return AllChannelsFragment.newInstance();

                case TAB_MY_CHANNELS:
                    return MyChannelsFragment.newInstance();

                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return TAB_CHANNEL_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case TAB_ALL_CHANNELS:
                    return getString(R.string.tab_all_channels_title);

                case TAB_MY_CHANNELS:
                    return getString(R.string.tab_my_channels_title);

                default:
                    return null;
            }
        }
    }
}
