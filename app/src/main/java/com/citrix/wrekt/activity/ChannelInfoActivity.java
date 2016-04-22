package com.citrix.wrekt.activity;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.data.pref.StringPreference;
import com.citrix.wrekt.di.annotation.UidPref;
import com.citrix.wrekt.di.annotation.UserEmailPref;
import com.citrix.wrekt.di.annotation.UsernamePref;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.firebase.api.IFirebaseFactory;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;
import com.citrix.wrekt.fragment.dialog.JoinChannelDialogFragment;
import com.citrix.wrekt.fragment.dialog.LeaveChannelDialogFragment;
import com.citrix.wrekt.fragment.dialog.ProgressDialogFragment;
import com.citrix.wrekt.util.TimeUtils;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.MutableData;
import com.firebase.client.Transaction;
import com.firebase.client.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class ChannelInfoActivity extends BaseActivity implements View.OnClickListener,
        JoinChannelDialogFragment.JoinChannelActionListener, ActionMenuView.OnMenuItemClickListener,
        LeaveChannelDialogFragment.LeaveChannelActionListener {

    private static final String EXTRA_CHANNEL_ID = "EXTRA_CHANNEL_ID";
    private static final String TAG_JOIN_CHANNEL_DIALOG = "TAG_JOIN_CHANNEL_DIALOG";
    private static final String TAG_JOIN_PROGRESS_DIALOG = "TAG_JOIN_PROGRESS_DIALOG";
    private static final String TAG_LEAVE_CHANNEL_DIALOG = "TAG_LEAVE_CHANNEL_DIALOG";
    private static final String TAG_LEAVE_CHANNEL_PROGRESS_DIALOG = "TAG_LEAVE_CHANNEL_PROGRESS_DIALOG";

    @Inject
    @UidPref
    StringPreference uidPref;

    @Inject
    @UsernamePref
    StringPreference usernamePref;

    @Inject
    @UserEmailPref
    StringPreference userEmailPref;

    @Inject
    IFirebaseFactory firebaseFactory;

    @Inject
    IFirebaseUrlFormatter firebaseUrlFormatter;

    private ActionMenuView actionMenuView;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private ImageView channelImageView;
    private ViewSwitcher mainContentViewSwitcher;
    private SwipeRefreshLayout channelSwipeRefreshLayout;
    private TextView channelNameTextView;
    private TextView channelCreateTimeTextView;
    private TextView channelCategoryTextView;
    private TextView channelAdminTextView;
    private RelativeLayout channelMemberContainer;
    private TextView channelMemberTextView;
    private TextView channelDescriptionTextView;
    private FloatingActionButton joinChannelFAB;

    private ActivityComponent activityComponent;
    private FragmentManager fragmentManager;
    private ChannelValueEventListener channelValueEventListener;
    private Firebase channelRef;
    private String channelId;
    private String channelName;
    private String channelConfUrl;
    private String adminUid;
    private boolean alreadySubscibed = false;

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

        Intent launchIntent = getIntent();
        if (launchIntent != null) {
            channelId = launchIntent.getStringExtra(EXTRA_CHANNEL_ID);
        } else {
            finish();
            return;
        }

        fragmentManager = getFragmentManager();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setHomeButtonEnabled(true);
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setDisplayShowTitleEnabled(false);
        }

        actionMenuView = (ActionMenuView) findViewById(R.id.action_menu_view);
        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_container);
        channelImageView = (ImageView) findViewById(R.id.channel_image_view);
        mainContentViewSwitcher = (ViewSwitcher) findViewById(R.id.main_content_view_switcher);
        channelSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.channel_swipe_refresh_layout);
        channelNameTextView = (TextView) findViewById(R.id.channel_name_text_view);
        channelCreateTimeTextView = (TextView) findViewById(R.id.channel_create_time_text_view);
        channelCategoryTextView = (TextView) findViewById(R.id.channel_category_text_view);
        channelAdminTextView = (TextView) findViewById(R.id.channel_admin_text_view);
        channelMemberContainer = (RelativeLayout) findViewById(R.id.channel_member_container);
        channelMemberTextView = (TextView) findViewById(R.id.channel_member_text_view);
        channelDescriptionTextView = (TextView) findViewById(R.id.channel_description_text_view);
        joinChannelFAB = (FloatingActionButton) findViewById(R.id.join_channel_fab);

        actionMenuView.setOnMenuItemClickListener(this);
        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));
        channelSwipeRefreshLayout.setColorSchemeResources(R.color.accent, R.color.primary, R.color.control_highlight);
        channelSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadChannelInfo();
            }
        });
        channelMemberContainer.setOnClickListener(this);
        joinChannelFAB.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupFirebaseAndListener(channelId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (channelRef != null && channelValueEventListener != null) {
            channelRef.removeEventListener(channelValueEventListener);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        hideJoinChannelDialog();
        hideJoinProgressDialog();
        hideLeaveChannelDialog();
        hideLeaveProgressDialog();
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
        getMenuInflater().inflate(R.menu.menu_channel_info, actionMenu);
        MenuItem leaveChannelMenuItem = actionMenu.findItem(R.id.action_leave_channel);
        leaveChannelMenuItem.setVisible(alreadySubscibed);
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
            case R.id.action_leave_channel:
                showLeaveChannelDialog();
                return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.join_channel_fab:
                if (alreadySubscibed) {
                    Log.e("findme: ", "Already subscribed to this channel");
                    ChannelChatActivity.start(ChannelInfoActivity.this, channelId, channelName, channelConfUrl);
                    finish();
                } else {
                    showJoinChannelDialog();
                }

                break;

            case R.id.channel_member_container:
                ChannelMemberActivity.start(this, channelId, adminUid);
                break;
        }
    }

    @Override
    public void onJoinChannelConfirmed() {
        showJoinProgressDialog();
        addSubscriptionToChannel();
    }

    @Override
    public void onLeaveChannelConfirmed() {
        showLeaveProgressDialog();
        decrementChannelMemberCount();
    }

    private void setupFirebaseAndListener(final String channelId) {
        Firebase subscriptionRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getSubscriptionsUrl()).child(uidPref.get());
        subscriptionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    Map<String, Object> dataMap = (Map<String, Object>) dataSnapshot.getValue();
                    alreadySubscibed = dataMap.containsKey(channelId);
                }

                // Refresh options menu to show or hide leave channel option
                invalidateOptionsMenu();
                mainContentViewSwitcher.setDisplayedChild(1);
                joinChannelFAB.setImageResource(alreadySubscibed ? R.drawable.ic_chat : R.drawable.ic_join_channel);
                joinChannelFAB.show();
                channelRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getChannelsUrl()).child(channelId);
                channelValueEventListener = new ChannelValueEventListener();
                channelRef.addValueEventListener(channelValueEventListener);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    private void loadChannelInfo() {
        channelSwipeRefreshLayout.setRefreshing(true);
        channelRef.addListenerForSingleValueEvent(new ChannelValueEventListener());
    }

    private void addSubscriptionToChannel() {
        Firebase subscriptionRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getSubscriptionsUrl()).child(uidPref.get());
        Map<String, Object> channelMap = new HashMap<>();
        channelMap.put(channelId, channelName);
        subscriptionRef.updateChildren(channelMap, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError == null) {
                    incrementChannelMemberCount();
                } else {
                    showJoinChannelFailedMessage(firebaseError.getMessage());
                }
            }
        });
    }

    private void incrementChannelMemberCount() {
        Firebase channelRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getChannelsUrl()).child(channelId);
        channelRef.child("memberCount").runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData currentData) {
                if (currentData.getValue() == null) {
                    currentData.setValue(1);
                } else {
                    currentData.setValue((Long) currentData.getValue() + 1);
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(FirebaseError firebaseError, boolean b, DataSnapshot dataSnapshot) {
                if (firebaseError == null) {
                    addChannelMemberData();
                } else {
                    showJoinChannelFailedMessage(firebaseError.getMessage());
                }
            }
        });
    }

    private void addChannelMemberData() {
        Firebase channelMemberRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getChannelMembersUrl()).child(channelId);
        Map<String, Object> memberMap = new HashMap<>();
        Map<String, String> newMemberMap = new HashMap<>();
        newMemberMap.put("uid", uidPref.get());
        newMemberMap.put("username", usernamePref.get());
        newMemberMap.put("email", userEmailPref.get());
        memberMap.put(uidPref.get(), newMemberMap);
        channelMemberRef.updateChildren(memberMap, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError == null) {
                    hideJoinProgressDialog();
                    ChannelChatActivity.start(ChannelInfoActivity.this, channelId, channelName, channelConfUrl);
                    finish();
                } else {
                    showJoinChannelFailedMessage(firebaseError.getMessage());
                }
            }
        });
    }

    private void showJoinChannelFailedMessage(String failureMessage) {
        Log.e("findme: ", "Firebase add subscription failed: " + failureMessage);
        hideJoinProgressDialog();
        Toast.makeText(this,
                R.string.join_channel_failed_message,
                Toast.LENGTH_SHORT)
                .show();
    }

    private void showJoinChannelDialog() {
        JoinChannelDialogFragment fragment
                = (JoinChannelDialogFragment) fragmentManager.findFragmentByTag(TAG_JOIN_CHANNEL_DIALOG);
        if (fragment == null) {
            fragment = JoinChannelDialogFragment.newInstance();
        }
        fragment.show(fragmentManager, TAG_JOIN_CHANNEL_DIALOG);
    }

    private void hideJoinChannelDialog() {
        JoinChannelDialogFragment fragment
                = (JoinChannelDialogFragment) fragmentManager.findFragmentByTag(TAG_JOIN_CHANNEL_DIALOG);
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private void showJoinProgressDialog() {
        ProgressDialogFragment fragment
                = (ProgressDialogFragment) fragmentManager.findFragmentByTag(TAG_JOIN_PROGRESS_DIALOG);
        if (fragment == null) {
            fragment = ProgressDialogFragment.newInstance(R.string.join_channel_progress_dialog_message);
        }
        fragment.show(fragmentManager, TAG_JOIN_PROGRESS_DIALOG);
    }

    private void hideJoinProgressDialog() {
        ProgressDialogFragment fragment
                = (ProgressDialogFragment) fragmentManager.findFragmentByTag(TAG_JOIN_PROGRESS_DIALOG);
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private void decrementChannelMemberCount() {
        Firebase channelRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getChannelsUrl()).child(channelId);
        channelRef.child("memberCount").runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData currentData) {
                if (currentData.getValue() == null) {
                    currentData.setValue(0);
                } else {
                    long newValue = (Long) currentData.getValue() - 1;
                    if (newValue < 0) {
                        newValue = 0;
                    }
                    currentData.setValue(newValue);
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(FirebaseError firebaseError, boolean b, DataSnapshot dataSnapshot) {
                if (firebaseError == null) {
                    removeChannelMemberData();
                } else {
                    showLeaveChannelFailedMessage(firebaseError.getMessage());
                }
            }
        });
    }

    private void removeChannelMemberData() {
        Firebase channelMemberRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getChannelMembersUrl()).child(channelId);
        channelMemberRef.child(uidPref.get()).removeValue(new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError == null) {
                    removeSubscriptionToChannel();
                } else {
                    showLeaveChannelFailedMessage(firebaseError.getMessage());
                }
            }
        });
    }

    private void removeSubscriptionToChannel() {
        Firebase subscriptionRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getSubscriptionsUrl()).child(uidPref.get());
        subscriptionRef.child(channelId).removeValue(new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError == null) {
                    hideLeaveChannelDialog();
                    finish();
                } else {
                    showLeaveChannelFailedMessage(firebaseError.getMessage());
                }
            }
        });
    }

    private void showLeaveChannelFailedMessage(String failureMessage) {
        Log.e("findme: ", "Firebase add subscription failed: " + failureMessage);
        hideLeaveChannelDialog();
        Toast.makeText(this,
                R.string.join_channel_failed_message,
                Toast.LENGTH_SHORT)
                .show();
    }

    private void showLeaveChannelDialog() {
        LeaveChannelDialogFragment fragment
                = (LeaveChannelDialogFragment) fragmentManager.findFragmentByTag(TAG_LEAVE_CHANNEL_DIALOG);
        if (fragment == null) {
            fragment = LeaveChannelDialogFragment.newInstance();
        }
        fragment.show(fragmentManager, TAG_LEAVE_CHANNEL_DIALOG);
    }

    private void hideLeaveChannelDialog() {
        LeaveChannelDialogFragment fragment
                = (LeaveChannelDialogFragment) fragmentManager.findFragmentByTag(TAG_LEAVE_CHANNEL_DIALOG);
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private void showLeaveProgressDialog() {
        ProgressDialogFragment fragment
                = (ProgressDialogFragment) fragmentManager.findFragmentByTag(TAG_LEAVE_CHANNEL_PROGRESS_DIALOG);
        if (fragment == null) {
            fragment = ProgressDialogFragment.newInstance(R.string.leave_channel_progress_dialog_message);
        }
        fragment.show(fragmentManager, TAG_LEAVE_CHANNEL_PROGRESS_DIALOG);
    }

    private void hideLeaveProgressDialog() {
        ProgressDialogFragment fragment
                = (ProgressDialogFragment) fragmentManager.findFragmentByTag(TAG_LEAVE_CHANNEL_PROGRESS_DIALOG);
        if (fragment != null) {
            fragment.dismiss();
        }
    }


    private class ChannelValueEventListener implements ValueEventListener {

        @SuppressWarnings("unchecked")
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            channelSwipeRefreshLayout.setRefreshing(false);
            if (dataSnapshot != null && dataSnapshot.exists()) {
                Map<String, Object> dataMap = (Map<String, Object>) dataSnapshot.getValue();
                channelName = dataMap.get("name").toString();
                channelConfUrl = dataMap.get("conferenceUri").toString();
                adminUid = dataMap.get("adminUid").toString();
                int memberCount = Integer.parseInt(dataMap.get("memberCount").toString());
                String category = dataMap.get("category").toString();
                long createTime = Long.parseLong(dataMap.get("createTime").toString());
                String description = dataMap.get("description").toString();
                String imageUrl = dataMap.get("imageUrl").toString();
                String adminName = dataMap.get("adminName").toString();
                Picasso.with(ChannelInfoActivity.this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_loading)
                        .error(R.drawable.ic_no_image)
                        .into(channelImageView);
                collapsingToolbarLayout.setTitle(channelName);
                channelNameTextView.setText(String.format(getString(R.string.channel_name_format), channelName));
                channelCreateTimeTextView.setText(TimeUtils.getFullDate(createTime));
                channelCategoryTextView.setText(String.format(getString(R.string.channel_category_format), category));
                if (adminUid.equals(uidPref.get())) {
                    channelAdminTextView.setText(String.format(getString(R.string.channel_admin_format), getString(R.string.sender_me_text)));
                } else {
                    channelAdminTextView.setText(String.format(getString(R.string.channel_admin_format), adminName));
                }
                channelMemberTextView.setText(String.format(getString(R.string.channel_member_format), memberCount));
                channelDescriptionTextView.setText(String.format(getString(R.string.channel_description_format), description));
            } else {
                Log.e("findme: ", "No channel data retrieved.");
            }
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    }
}
