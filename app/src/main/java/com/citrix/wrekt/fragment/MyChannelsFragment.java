package com.citrix.wrekt.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.activity.ChannelInfoActivity;
import com.citrix.wrekt.adapter.ChannelAdapter;
import com.citrix.wrekt.data.Channel;
import com.citrix.wrekt.data.pref.StringPreference;
import com.citrix.wrekt.di.annotation.UidPref;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.firebase.api.IFirebaseFactory;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

public class MyChannelsFragment extends BaseFragment implements ChannelAdapter.ChannelClickListener {

    @Inject
    @UidPref
    StringPreference uidPref;

    @Inject
    IFirebaseFactory firebaseFactory;

    @Inject
    IFirebaseUrlFormatter firebaseUrlFormatter;

    private SwipeRefreshLayout channelSwipeRefreshLayout;
    private RecyclerView channelRecyclerView;
    private ChannelAdapter channelAdapter;

    private ActivityComponent activityComponent;
    private Firebase subscriptionRef;
    private ChannelsValueEventListener channelsValueEventListener;

    public static MyChannelsFragment newInstance() {
        MyChannelsFragment fragment = new MyChannelsFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    public MyChannelsFragment() {
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
        subscriptionRef.removeEventListener(channelsValueEventListener);
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
        View view = inflater.inflate(R.layout.layout_channel_list, container, false);

        channelSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.channel_list_swipe_refresh_layout);
        channelSwipeRefreshLayout.setColorSchemeResources(R.color.accent, R.color.primary, R.color.control_highlight);
        channelSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadMyChannels();
            }
        });

        channelRecyclerView = (RecyclerView) view.findViewById(R.id.channel_list_recycler_view);
        channelRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        channelAdapter = new ChannelAdapter(getActivity(), new ArrayList<Channel>(), this);
        channelRecyclerView.setAdapter(channelAdapter);

        TextView emptyMessageTextView = (TextView) view.findViewById(R.id.empty_message_text_view);
        emptyMessageTextView.setText(R.string.my_channel_list_empty_message);

        return view;
    }

    @Override
    protected void inject() {
        WrektApplication application = (WrektApplication) getActivity().getApplication();
        activityComponent = application.getAppComponent().plus(new ActivityModule(getActivity()));
        activityComponent.inject(this);
    }

    @Override
    public void onChannelClicked(String channelId) {
        ChannelInfoActivity.start(getActivity(), channelId);
    }

    private void setupFirebaseAndListener() {
        subscriptionRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getSubscriptionsUrl()).child(uidPref.get());
        channelsValueEventListener = new ChannelsValueEventListener();
        subscriptionRef.addValueEventListener(channelsValueEventListener);
    }

    private void loadMyChannels() {
        channelSwipeRefreshLayout.setRefreshing(true);
        subscriptionRef.addListenerForSingleValueEvent(new ChannelsValueEventListener());
    }


    private class ChannelsValueEventListener implements ValueEventListener {

        @SuppressWarnings("unchecked")
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if (dataSnapshot != null && dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                Map<String, Map<String, Object>> dataMap = (Map<String, Map<String, Object>>) dataSnapshot.getValue();
                Set<String> mySubscriptionIdSet = dataMap.keySet();
                loadChannelData(mySubscriptionIdSet);
            } else {
                channelSwipeRefreshLayout.setRefreshing(false);
                channelRecyclerView.setVisibility(View.GONE);
                Log.e("findme: ", "No channel data retrieved.");
            }
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }

        private void loadChannelData(final Set<String> subscriptionIdSet) {
            Firebase channelsRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getChannelsUrl());
            channelsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @SuppressWarnings("unchecked")
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    channelSwipeRefreshLayout.setRefreshing(false);
                    List<Channel> myChannelList = new ArrayList<>();
                    if (dataSnapshot != null && dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                        Map<String, Map<String, Object>> dataMap = (Map<String, Map<String, Object>>) dataSnapshot.getValue();
                        for (String channelId : dataMap.keySet()) {
                            if (subscriptionIdSet.contains(channelId)) {
                                Map<String, Object> channelMap = dataMap.get(channelId);
                                Channel channel = new Channel(channelMap.get("id").toString(),
                                        channelMap.get("name").toString(),
                                        Long.parseLong(channelMap.get("createTime").toString()),
                                        channelMap.get("category").toString(),
                                        channelMap.get("description").toString(),
                                        channelMap.get("imageUrl").toString(),
                                        Integer.parseInt(channelMap.get("memberCount").toString()),
                                        channelMap.get("adminUid").toString(),
                                        channelMap.get("adminName").toString());
                                myChannelList.add(channel);
                            }
                        }
                    } else {
                        Log.e("findme: ", "No channel data retrieved.");
                    }
                    channelAdapter.updateChannelList(myChannelList);
                    channelRecyclerView.setVisibility(myChannelList.isEmpty() ? View.GONE : View.VISIBLE);
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }
            });
        }
    }
}
