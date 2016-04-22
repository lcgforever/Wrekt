package com.citrix.wrekt.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.adapter.PendingFriendRequestAdapter;
import com.citrix.wrekt.data.PendingFriendRequest;
import com.citrix.wrekt.data.pref.StringPreference;
import com.citrix.wrekt.di.annotation.UidPref;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.firebase.api.IFirebaseFactory;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;
import com.citrix.wrekt.view.DividerItemDecoration;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class PendingFriendRequestsFragment extends BaseFragment implements PendingFriendRequestAdapter.PendingRequestActionListener {

    @Inject
    @UidPref
    StringPreference uidPref;

    @Inject
    IFirebaseFactory firebaseFactory;

    @Inject
    IFirebaseUrlFormatter firebaseUrlFormatter;

    private RecyclerView pendingRequestRecyclerView;

    private ActivityComponent activityComponent;
    private PendingFriendRequestAdapter pendingRequestAdapter;
    private Firebase friendRequestsRef;
    private PendingRequestsValueEventListener pendingRequestsValueEventListener;

    public static PendingFriendRequestsFragment newInstance() {
        PendingFriendRequestsFragment fragment = new PendingFriendRequestsFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    public PendingFriendRequestsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inject();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupFirebaseAndListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (friendRequestsRef != null && pendingRequestsValueEventListener != null) {
            friendRequestsRef.removeEventListener(pendingRequestsValueEventListener);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activityComponent = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.layout_friend_list, container, false);

        pendingRequestRecyclerView = (RecyclerView) view.findViewById(R.id.friend_recycler_view);
        pendingRequestRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        pendingRequestRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        pendingRequestAdapter = new PendingFriendRequestAdapter(getActivity(), new ArrayList<PendingFriendRequest>(), this, uidPref.get());
        pendingRequestRecyclerView.setAdapter(pendingRequestAdapter);

        ImageView emptyImageView = (ImageView) view.findViewById(R.id.empty_image_view);
        emptyImageView.setImageResource(R.drawable.ic_empty_chat_illustration);
        TextView emptyMessageTextView = (TextView) view.findViewById(R.id.empty_message_text_view);
        emptyMessageTextView.setText(R.string.pending_friend_requests_empty_message);

        return view;
    }

    @Override
    protected void inject() {
        WrektApplication application = (WrektApplication) getActivity().getApplication();
        activityComponent = application.getAppComponent().plus(new ActivityModule(getActivity()));
        activityComponent.inject(this);
    }

    @Override
    public void onRejectRequestClicked(PendingFriendRequest request) {
        rejectRequest(request);
    }

    @Override
    public void onAcceptRequestClicked(PendingFriendRequest request) {
        acceptRequest(request);
    }

    @Override
    public void onDeleteRequestClicked(PendingFriendRequest request) {
        deleteRequest(request);
    }

    private void setupFirebaseAndListener() {
        friendRequestsRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getFriendRequestsUrl());
        pendingRequestsValueEventListener = new PendingRequestsValueEventListener();
        friendRequestsRef.addValueEventListener(pendingRequestsValueEventListener);
    }

    private void rejectRequest(PendingFriendRequest request) {
        Map<String, Object> statusMap = new HashMap<>();
        statusMap.put("status", "Rejected");
        friendRequestsRef.child(request.getId()).updateChildren(statusMap, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError != null) {
                    Toast.makeText(getActivity(),
                            R.string.friend_request_reject_failed_message,
                            Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    private void acceptRequest(PendingFriendRequest request) {
        addFriend(request.getId(),
                request.getFromUid(),
                request.getFromUsername(),
                request.getFromUserEmail());
    }

    private void deleteRequest(PendingFriendRequest request) {
        friendRequestsRef.child(request.getId()).removeValue(new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError != null) {
                    Toast.makeText(getActivity(),
                            R.string.friend_request_delete_failed_message,
                            Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    private void addFriend(final String requestId,
                           final String friendUid,
                           final String friendUsername,
                           final String friendEmail) {
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
                    finishFriendRequest(requestId);
                    Toast.makeText(getActivity(),
                            String.format(getString(R.string.friend_add_sucessful_message), friendUsername),
                            Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Log.e("findme: ", "Firebase add friend failed: " + firebaseError.getMessage());
                    Toast.makeText(getActivity(),
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


    private class PendingRequestsValueEventListener implements ValueEventListener {

        @SuppressWarnings("unchecked")
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            List<PendingFriendRequest> pendingRequestList = new ArrayList<>();
            if (dataSnapshot != null && dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                Map<String, Map<String, Object>> requestsMap = (Map<String, Map<String, Object>>) dataSnapshot.getValue();
                for (Map<String, Object> requestMap : requestsMap.values()) {
                    String fromUid = requestMap.get("fromUid").toString();
                    String toUid = requestMap.get("toUid").toString();
                    String status = requestMap.get("status").toString();
                    String myUid = uidPref.get();
                    if (status.equals("Pending") && (fromUid.equals(myUid) || toUid.equals(myUid))) {
                        PendingFriendRequest request = new PendingFriendRequest(requestMap.get("id").toString(),
                                fromUid,
                                requestMap.get("fromUserEmail").toString(),
                                requestMap.get("fromUsername").toString(),
                                toUid,
                                requestMap.get("toUserEmail").toString(),
                                requestMap.get("toUsername").toString(),
                                status,
                                (long) requestMap.get("time"));
                        pendingRequestList.add(request);
                    }
                }
            }

            pendingRequestAdapter.updatePendingRequestList(pendingRequestList);
            pendingRequestRecyclerView.setVisibility(pendingRequestList.isEmpty() ? View.GONE : View.VISIBLE);
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    }
}
