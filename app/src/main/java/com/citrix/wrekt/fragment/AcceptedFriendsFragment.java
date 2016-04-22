package com.citrix.wrekt.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.activity.FriendChatActivity;
import com.citrix.wrekt.adapter.FriendAdapter;
import com.citrix.wrekt.data.User;
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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class AcceptedFriendsFragment extends BaseFragment implements FriendAdapter.FriendActionListener {

    @Inject
    @UidPref
    StringPreference uidPref;

    @Inject
    IFirebaseFactory firebaseFactory;

    @Inject
    IFirebaseUrlFormatter firebaseUrlFormatter;

    private RecyclerView friendRecyclerView;

    private ActivityComponent activityComponent;
    private FriendAdapter friendAdapter;
    private Firebase friendsRef;
    private FriendsValueEventListener friendsValueEventListener;

    public static AcceptedFriendsFragment newInstance() {
        AcceptedFriendsFragment fragment = new AcceptedFriendsFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    public AcceptedFriendsFragment() {
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
        if (friendsRef != null && friendsValueEventListener != null) {
            friendsRef.removeEventListener(friendsValueEventListener);
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

        friendRecyclerView = (RecyclerView) view.findViewById(R.id.friend_recycler_view);
        friendRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        friendRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        friendAdapter = new FriendAdapter(getActivity(), new ArrayList<User>(), this);
        friendRecyclerView.setAdapter(friendAdapter);

        ImageView emptyImageView = (ImageView) view.findViewById(R.id.empty_image_view);
        emptyImageView.setImageResource(R.drawable.ic_empty_chat_illustration);
        TextView emptyMessageTextView = (TextView) view.findViewById(R.id.empty_message_text_view);
        emptyMessageTextView.setText(R.string.accepted_friend_list_empty_message);

        return view;
    }

    @Override
    protected void inject() {
        WrektApplication application = (WrektApplication) getActivity().getApplication();
        activityComponent = application.getAppComponent().plus(new ActivityModule(getActivity()));
        activityComponent.inject(this);
    }

    @Override
    public void onFriendClicked(User friend) {
        FriendChatActivity.start(getActivity(), friend.getUid(), friend.getUsername());
    }

    private void setupFirebaseAndListener() {
        friendsRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getUserFriendsUrl()).child(uidPref.get());
        friendsValueEventListener = new FriendsValueEventListener();
        friendsRef.addValueEventListener(friendsValueEventListener);
    }


    private class FriendsValueEventListener implements ValueEventListener {

        @SuppressWarnings("unchecked")
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            List<User> friendList = new ArrayList<>();
            if (dataSnapshot != null && dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                Map<String, Map<String, Object>> friendsMap = (Map<String, Map<String, Object>>) dataSnapshot.getValue();
                for (Map<String, Object> friendMap : friendsMap.values()) {
                    User friend = new User(friendMap.get("uid").toString(),
                            friendMap.get("email").toString(),
                            friendMap.get("username").toString(), 0);
                    friendList.add(friend);
                }
            }

            friendAdapter.updateFriendList(friendList);
            friendRecyclerView.setVisibility(friendList.isEmpty() ? View.GONE : View.VISIBLE);
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    }
}
