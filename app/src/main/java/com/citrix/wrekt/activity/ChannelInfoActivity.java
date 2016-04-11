package com.citrix.wrekt.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.firebase.api.IFirebaseFactory;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Map;

import javax.inject.Inject;

public class ChannelInfoActivity extends BaseActivity implements View.OnClickListener {

    private static final String EXTRA_CHANNEL_ID = "EXTRA_CHANNEL_ID";

    @Inject
    IFirebaseFactory firebaseFactory;

    @Inject
    IFirebaseUrlFormatter firebaseUrlFormatter;

    private CollapsingToolbarLayout collapsingToolbarLayout;
    private ImageView channelImageView;
    private SwipeRefreshLayout channelSwipeRefreshLayout;

    private ActivityComponent activityComponent;
    private ChannelValueEventListener channelValueEventListener;
    private Firebase channelRef;

    public static void start(Context context, String channelId) {
        Intent intent = new Intent(context, ChannelInfoActivity.class);
        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_info);

        inject();

        String channelId = "";
        Intent launchIntent = getIntent();
        if (launchIntent != null) {
            channelId = launchIntent.getStringExtra(EXTRA_CHANNEL_ID);
        } else {
            finish();
            return;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setHomeButtonEnabled(true);
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setDisplayShowTitleEnabled(false);
        }

        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_container);
        channelImageView = (ImageView) findViewById(R.id.channel_image_view);
        channelSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.channel_swipe_refresh_layout);
        FloatingActionButton joinChannelFAB = (FloatingActionButton) findViewById(R.id.join_channel_fab);

        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));
        channelSwipeRefreshLayout.setColorSchemeResources(R.color.accent, R.color.primary, R.color.control_highlight);
        channelSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadChannelInfo();
            }
        });
        joinChannelFAB.setOnClickListener(this);

        setupFirebaseAndListener(channelId);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        channelRef.removeEventListener(channelValueEventListener);
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.join_channel_fab:

                break;
        }
    }

    private void setupFirebaseAndListener(String channelId) {
        Firebase channelsRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getChanneslUrl());
        channelRef = channelsRef.child(channelId);
        channelValueEventListener = new ChannelValueEventListener();
        channelRef.addValueEventListener(channelValueEventListener);
    }

    private void loadChannelInfo() {
        channelSwipeRefreshLayout.setRefreshing(true);
        channelRef.addListenerForSingleValueEvent(new ChannelValueEventListener());
    }


    private class ChannelValueEventListener implements ValueEventListener {

        @SuppressWarnings("unchecked")
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            channelSwipeRefreshLayout.setRefreshing(false);
            if (dataSnapshot != null && dataSnapshot.exists()) {
                Map<String, Object> dataMap = (Map<String, Object>) dataSnapshot.getValue();
                String name = dataMap.get("name").toString();
                int memberCount = Integer.parseInt(dataMap.get("memberCount").toString());
                String description = dataMap.get("description").toString();
                String imageUrl = dataMap.get("imageUrl").toString();
                collapsingToolbarLayout.setTitle(name);
                Picasso.with(ChannelInfoActivity.this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_loading)
                        .error(R.drawable.ic_loading)
                        .into(channelImageView);
            } else {
                Log.e("findme: ", "No channel data retrieved.");
            }
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    }
}
