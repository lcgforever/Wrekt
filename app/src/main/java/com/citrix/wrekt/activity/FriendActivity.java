package com.citrix.wrekt.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.fragment.AcceptedFriendsFragment;
import com.citrix.wrekt.fragment.PendingFriendRequestsFragment;

public class FriendActivity extends BaseActivity {

    private static final int TAB_FRIEND_COUNT = 2;
    private static final int TAB_ACCEPTED_FRIENDS = 0;
    private static final int TAB_PENDING_FRIEND_REQUESTS = 1;

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ActionMenuView actionMenuView;

    private ActivityComponent activityComponent;

    public static void start(Context context) {
        Intent intent = new Intent(context, FriendActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);

        inject();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setHomeButtonEnabled(true);
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }

        tabLayout = (TabLayout) findViewById(R.id.friend_tab_layout);
        viewPager = (ViewPager) findViewById(R.id.friend_view_pager);
        actionMenuView = (ActionMenuView) findViewById(R.id.action_menu_view);

        setupViewPager(savedInstanceState);
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

    private void setupViewPager(Bundle savedInstanceState) {
        FriendTabsFragmentAdapter tabsAdapter = new FriendTabsFragmentAdapter(getSupportFragmentManager());
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


    private class FriendTabsFragmentAdapter extends FragmentPagerAdapter {

        public FriendTabsFragmentAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case TAB_ACCEPTED_FRIENDS:
                    return AcceptedFriendsFragment.newInstance();

                case TAB_PENDING_FRIEND_REQUESTS:
                    return PendingFriendRequestsFragment.newInstance();

                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return TAB_FRIEND_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case TAB_ACCEPTED_FRIENDS:
                    return getString(R.string.tab_accepted_friends);

                case TAB_PENDING_FRIEND_REQUESTS:
                    return getString(R.string.tab_pending_friend_requests);

                default:
                    return null;
            }
        }
    }
}
