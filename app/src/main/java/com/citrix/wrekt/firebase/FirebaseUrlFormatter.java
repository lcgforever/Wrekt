package com.citrix.wrekt.firebase;

import android.content.Context;

import com.citrix.wrekt.R;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;

public class FirebaseUrlFormatter implements IFirebaseUrlFormatter {

    private Context context;
    private String baseUrl;

    public FirebaseUrlFormatter(Context context, String baseUrl) {
        this.context = context;
        this.baseUrl = baseUrl;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public String getChannelsUrl() {
        return String.format(context.getString(R.string.firebase_channels_url), baseUrl);
    }

    @Override
    public String getUsersUrl() {
        return String.format(context.getString(R.string.firebase_users_url), baseUrl);
    }

    @Override
    public String getPrivateChatsUrl() {
        return String.format(context.getString(R.string.firebase_private_chats_url), baseUrl);
    }

    @Override
    public String getChatsUrl() {
        return String.format(context.getString(R.string.firebase_chats_url), baseUrl);
    }

    @Override
    public String getSubscriptionsUrl() {
        return String.format(context.getString(R.string.firebase_subscriptions_url), baseUrl);
    }

    @Override
    public String getChannelMembersUrl() {
        return String.format(context.getString(R.string.firebase_channel_members_url), baseUrl);
    }

    @Override
    public String getChannelAdminsUrl() {
        return String.format(context.getString(R.string.firebase_channel_admins_url), baseUrl);
    }

    @Override
    public String getFriendRequestsUrl() {
        return String.format(context.getString(R.string.firebase_friend_requests_url), baseUrl);
    }

    @Override
    public String getUserFriendsUrl() {
        return String.format(context.getString(R.string.firebase_user_friends_url), baseUrl);
    }
}
