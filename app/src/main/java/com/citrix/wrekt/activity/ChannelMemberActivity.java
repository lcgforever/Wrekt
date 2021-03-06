package com.citrix.wrekt.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.adapter.ChannelMemberAdapter;
import com.citrix.wrekt.data.User;
import com.citrix.wrekt.data.pref.StringPreference;
import com.citrix.wrekt.di.annotation.UidPref;
import com.citrix.wrekt.di.annotation.UserEmailPref;
import com.citrix.wrekt.di.annotation.UsernamePref;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.firebase.api.IFirebaseFactory;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;
import com.citrix.wrekt.view.DividerItemDecoration;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

public class ChannelMemberActivity extends BaseActivity implements ChannelMemberAdapter.ChannelMemberActionListener {

    private static final String EXTRA_CHANNEL_ID = "EXTRA_CHANNEL_ID";
    private static final String EXTRA_ADMIN_UID = "EXTRA_ADMIN_UID";

    @Inject
    @UidPref
    StringPreference uidPref;

    @Inject
    @UserEmailPref
    StringPreference userEmailPref;

    @Inject
    @UsernamePref
    StringPreference usernamePref;

    @Inject
    IFirebaseFactory firebaseFactory;

    @Inject
    IFirebaseUrlFormatter firebaseUrlFormatter;

    private RecyclerView channelMemberRecyclerView;

    private ActivityComponent activityComponent;
    private ChannelMemberAdapter channelMemberAdapter;
    private Firebase channelMembersRef;
    private ChannelMemberChildEventListener channelMemberChildEventListener;
    private Firebase friendsRef;
    private FriendValueEventListener friendValueEventListener;
    private String channelId;
    private String adminUid;
    private Set<String> friendIdSet;

    public static void start(Context context, String channelId, String adminUid) {
        Intent intent = new Intent(context, ChannelMemberActivity.class);
        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
        intent.putExtra(EXTRA_ADMIN_UID, adminUid);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_member);

        inject();

        Intent launchIntent = getIntent();
        if (launchIntent != null) {
            channelId = launchIntent.getStringExtra(EXTRA_CHANNEL_ID);
            adminUid = launchIntent.getStringExtra(EXTRA_ADMIN_UID);
        } else {
            finish();
            return;
        }

        friendIdSet = new HashSet<>();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setHomeButtonEnabled(true);
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }

        channelMemberRecyclerView = (RecyclerView) findViewById(R.id.channel_member_recycler_view);

        channelMemberRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        channelMemberRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        channelMemberAdapter = new ChannelMemberAdapter(this, new ArrayList<User>(), adminUid, uidPref.get(), friendIdSet, this);
        channelMemberRecyclerView.setAdapter(channelMemberAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        channelMemberAdapter.clear();
        setupFirebaseAndListener(channelId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (channelMembersRef != null && channelMemberChildEventListener != null) {
            channelMembersRef.removeEventListener(channelMemberChildEventListener);
        }
        if (friendsRef != null && friendValueEventListener != null) {
            friendsRef.removeEventListener(friendValueEventListener);
        }
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
    public void onAddFriendClicked(User channelMember) {
        Firebase friendRequestRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getFriendRequestsUrl());
        Firebase newRequestRef = friendRequestRef.push();
        String requestId = newRequestRef.getKey();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("id", requestId);
        requestMap.put("fromUid", uidPref.get());
        requestMap.put("fromUserEmail", userEmailPref.get());
        requestMap.put("fromUsername", usernamePref.get());
        requestMap.put("toUid", channelMember.getUid());
        requestMap.put("toUserEmail", channelMember.getUserEmail());
        requestMap.put("toUsername", channelMember.getUsername());
        requestMap.put("time", System.currentTimeMillis());
        requestMap.put("status", "Pending");
        newRequestRef.setValue(requestMap, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError != null) {
                    Log.e("findme: ", "Firebase send friend request failed: " + firebaseError.getMessage());
                    Toast.makeText(ChannelMemberActivity.this,
                            R.string.friend_request_failed_message
                            , Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    @Override
    public void onChatWithFriendClicked(User friend) {
        FriendChatActivity.start(this, friend.getUid(), friend.getUsername());
        finish();
    }

    private void setupFirebaseAndListener(String channelId) {
        friendsRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getUserFriendsUrl()).child(uidPref.get());
        friendValueEventListener = new FriendValueEventListener();
        friendsRef.addValueEventListener(friendValueEventListener);
        channelMembersRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getBaseUrl()).child("channelMembers");
        channelMemberChildEventListener = new ChannelMemberChildEventListener();
        channelMembersRef.child(channelId).addChildEventListener(channelMemberChildEventListener);
    }


    private class ChannelMemberChildEventListener implements ChildEventListener {

        @SuppressWarnings("unchecked")
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            if (dataSnapshot != null && dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                Map<String, Object> memberMap = (Map<String, Object>) dataSnapshot.getValue();
                String uid = memberMap.get("uid").toString();
                int sortPriority = 0;
                if (uidPref.get().equals(uid)) {
                    sortPriority = 3;
                } else if (adminUid.equals(uid)) {
                    sortPriority = 2;
                } else if (friendIdSet.contains(uid)) {
                    sortPriority = 1;
                }
                User channelMember = new User(uid, memberMap.get("email").toString(),
                        memberMap.get("username").toString(), sortPriority);
                channelMemberAdapter.addNewChannelMember(channelMember);
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    }

    private class FriendValueEventListener implements ValueEventListener {

        @SuppressWarnings("unchecked")
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if (dataSnapshot != null && dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                Map<String, Object> friendsMap = (Map<String, Object>) dataSnapshot.getValue();
                friendIdSet = friendsMap.keySet();
                channelMemberAdapter.updateFriendIdSet(friendIdSet);
            }
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    }
}
