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
import com.citrix.wrekt.data.ChannelMember;
import com.citrix.wrekt.data.pref.StringPreference;
import com.citrix.wrekt.di.annotation.UidPref;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.firebase.api.IFirebaseFactory;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;
import com.citrix.wrekt.view.DividerItemDecoration;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class ChannelMemberActivity extends BaseActivity implements ChannelMemberAdapter.ChannelMemberActionListener {

    private static final String EXTRA_CHANNEL_ID = "EXTRA_CHANNEL_ID";
    private static final String EXTRA_ADMIN_UID = "EXTRA_ADMIN_UID";

    @Inject
    @UidPref
    StringPreference uidPref;

    @Inject
    IFirebaseFactory firebaseFactory;

    @Inject
    IFirebaseUrlFormatter firebaseUrlFormatter;

    private RecyclerView channelMemberRecyclerView;

    private ActivityComponent activityComponent;
    private ChannelMemberAdapter channelMemberAdapter;
    private Firebase channelMembersRef;
    private ChannelMemberChildEventListener channelMemberChildEventListener;
    private String channelId;
    private String adminUid;

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
        channelMemberAdapter = new ChannelMemberAdapter(this, new ArrayList<ChannelMember>(), adminUid, uidPref.get(), this);
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
        channelMembersRef.removeEventListener(channelMemberChildEventListener);
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
    public void onAddFriendClicked(String uid) {
        Firebase friendRequestRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getBaseUrl()).child("friendRequests");
        Firebase newRequestRef = friendRequestRef.push();
        String requestId = newRequestRef.getKey();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("requestId", requestId);
        requestMap.put("fromUid", uidPref.get());
        requestMap.put("toUid", uid);
        requestMap.put("finished", false);
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

    private void setupFirebaseAndListener(String channelId) {
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
                    sortPriority = 2;
                } else if (adminUid.equals(uid)) {
                    sortPriority = 1;
                }
                ChannelMember channelMember = new ChannelMember(uid, memberMap.get("email").toString(),
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
}
