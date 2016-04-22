package com.citrix.wrekt.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.data.pref.StringPreference;
import com.citrix.wrekt.di.annotation.UidPref;
import com.citrix.wrekt.di.component.AppComponent;
import com.citrix.wrekt.firebase.api.IFirebaseFactory;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;
import com.citrix.wrekt.notification.IRequestNotifier;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class FriendRequestService extends Service {

    public static final int PARAM_REQUEST_ID = 0;
    public static final int PARAM_FRIEND_UID = 1;
    public static final int PARAM_FRIEND_USERNAME = 2;
    public static final int PARAM_FRIEND_EMAIL = 3;

    @Inject
    @UidPref
    StringPreference uidPref;

    @Inject
    IFirebaseFactory firebaseFactory;

    @Inject
    IFirebaseUrlFormatter firebaseUrlFormatter;

    @Inject
    IRequestNotifier requestNotifier;

    private AppComponent appComponent;

    public static void start(Context context) {
        Intent intent = new Intent(context, FriendRequestService.class);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, FriendRequestService.class);
        context.stopService(intent);
    }

    public FriendRequestService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        WrektApplication application = (WrektApplication) getApplication();
        appComponent = application.getAppComponent();
        appComponent.inject(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        appComponent = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String intentAction = intent.getAction();
        if (IRequestNotifier.NOTIFICATION_ACTION_REJECT_REQUEST.equals(intentAction)) {
            String[] params = intent.getStringArrayExtra(IRequestNotifier.EXTRA_PARAMS);
            rejectFriendRequest(params[PARAM_REQUEST_ID]);
        } else if (IRequestNotifier.NOTIFICATION_ACTION_POSTPONE_REQUEST.equals(intentAction)) {
            Toast.makeText(FriendRequestService.this,
                    R.string.friend_request_postponed_message,
                    Toast.LENGTH_SHORT)
                    .show();
        } else if (IRequestNotifier.NOTIFICATION_ACTION_ACCEPT_REQUEST.equals(intentAction)) {
            String[] params = intent.getStringArrayExtra(IRequestNotifier.EXTRA_PARAMS);
            addFriend(params[PARAM_REQUEST_ID],
                    params[PARAM_FRIEND_UID],
                    params[PARAM_FRIEND_USERNAME],
                    params[PARAM_FRIEND_EMAIL],
                    false);
        } else {
            startListeningForFriendRequest();
        }
        requestNotifier.cancelRequestNotification();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void startListeningForFriendRequest() {
        Firebase friendRequestRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getFriendRequestsUrl());
        friendRequestRef.addChildEventListener(new ChildEventListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot != null && dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    Map<String, Object> requestMap = (Map<String, Object>) dataSnapshot.getValue();
                    // Receive new incoming friend request
                    if (requestMap.get("status").toString().equals("Pending")
                            && requestMap.get("toUid").toString().equals(uidPref.get())) {
                        String[] params = {requestMap.get("id").toString(),
                                requestMap.get("fromUid").toString(),
                                requestMap.get("fromUsername").toString(),
                                requestMap.get("fromUserEmail").toString()};
                        requestNotifier.showRequestNotification(params);
                    }
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot != null && dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    Map<String, Object> requestMap = (Map<String, Object>) dataSnapshot.getValue();
                    // My outgoing friend request is accepted
                    if (requestMap.get("fromUid").toString().equals(uidPref.get())) {
                        String status = requestMap.get("status").toString();
                        if (status.equals("Accepted")) {
                            addFriend(requestMap.get("id").toString(),
                                    requestMap.get("toUid").toString(),
                                    requestMap.get("toUsername").toString(),
                                    requestMap.get("toUserEmail").toString(),
                                    true);
                        } else if (status.equals("Rejected")) {
                            Toast.makeText(FriendRequestService.this,
                                    R.string.friend_request_rejected_message,
                                    Toast.LENGTH_SHORT)
                                    .show();
                            deleteFriendRequest(requestMap.get("id").toString());
                        }
                    }
                }
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
        });
    }

    private void addFriend(final String requestId,
                           final String friendUid,
                           final String friendUsername,
                           final String friendEmail,
                           final boolean shouldDeleteRequest) {
        Firebase friendsRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getUserFriendsUrl());
        Firebase myFriendRef = friendsRef.child(uidPref.get()).child(friendUid);
        Map<String, Object> friendMap = new HashMap<>();
        friendMap.put("uid", friendUid);
        friendMap.put("email", friendEmail);
        friendMap.put("username", friendUsername);
        myFriendRef.setValue(friendMap, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError == null) {
                    if (shouldDeleteRequest) {
                        deleteFriendRequest(requestId);
                    } else {
                        finishFriendRequest(requestId);
                    }
                    Toast.makeText(FriendRequestService.this,
                            String.format(getString(R.string.friend_add_sucessful_message), friendUsername),
                            Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Log.e("findme: ", "Firebase add friend failed: " + firebaseError.getMessage());
                    Toast.makeText(FriendRequestService.this,
                            R.string.friend_request_accept_failed_message,
                            Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    private void finishFriendRequest(final String requestId) {
        if (requestId != null) {
            Firebase friendRequestRef = firebaseFactory.createFirebase(
                    firebaseUrlFormatter.getFriendRequestsUrl()).child(requestId);
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("status", "Accepted");
            friendRequestRef.updateChildren(statusMap);
        }
    }

    private void rejectFriendRequest(final String requestId) {
        if (requestId != null) {
            Firebase friendRequestRef = firebaseFactory.createFirebase(
                    firebaseUrlFormatter.getFriendRequestsUrl()).child(requestId);
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("status", "Rejected");
            friendRequestRef.updateChildren(statusMap);
        }
    }

    private void deleteFriendRequest(final String requestId) {
        if (requestId != null) {
            Firebase friendRequestRef = firebaseFactory.createFirebase(
                    firebaseUrlFormatter.getFriendRequestsUrl()).child(requestId);
            friendRequestRef.removeValue();
        }
    }
}
