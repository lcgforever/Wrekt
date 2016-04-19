package com.citrix.wrekt.service;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.data.pref.StringPreference;
import com.citrix.wrekt.di.annotation.UidPref;
import com.citrix.wrekt.di.component.AppComponent;
import com.citrix.wrekt.firebase.api.IFirebaseFactory;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.squareup.otto.Bus;

import java.util.Map;

import javax.inject.Inject;

public class FriendRequestService extends Service {

    private static final String TAG_FRIEND_REQUEST_DIALOG = "TAG_FRIEND_REQUEST_DIALOG";

    @Inject
    @UidPref
    StringPreference uidPref;

    @Inject
    IFirebaseFactory firebaseFactory;

    @Inject
    IFirebaseUrlFormatter firebaseUrlFormatter;

    @Inject
    Bus bus;

    private AppComponent appComponent;

    public static void start(Context context) {
        Intent intent = new Intent(context, FriendRequestService.class);
        context.startService(intent);
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

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void startListeningForFriendRequest() {
        Firebase friendRequestRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getBaseUrl()).child("friendRequests");
        friendRequestRef.addChildEventListener(new ChildEventListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot != null && dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    Map<String, Map<String, Object>> requestsMap = (Map<String, Map<String, Object>>) dataSnapshot.getValue();
                    for (Map<String, Object> requestMap : requestsMap.values()) {
                        if (requestMap.get("toUid").toString().equals(uidPref.get())) {
                            showFriendRequestDialog(requestMap.get("fromUid").toString());
                        }
                    }
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot != null && dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    Map<String, Map<String, Object>> requestsMap = (Map<String, Map<String, Object>>) dataSnapshot.getValue();
                    for (Map<String, Object> requestMap : requestsMap.values()) {
                        if (requestMap.get("toUid").toString().equals(uidPref.get())
                                && (boolean) requestMap.get("finished")) {

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

    private void showFriendRequestDialog(final String uid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage(R.string.friend_request_dialog_message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                onFriendRequestAccepted(uid);
                                dialog.dismiss();
                            }
                        })
                .setNeutralButton(R.string.action_later,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(FriendRequestService.this,
                                        R.string.friend_request_postponed_message,
                                        Toast.LENGTH_SHORT)
                                        .show();
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(android.R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void onFriendRequestAccepted(String uid) {
        Firebase friendsRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getBaseUrl()).child("userFriends");
        Firebase myFriendRef = friendsRef.child(uidPref.get());

    }
}
