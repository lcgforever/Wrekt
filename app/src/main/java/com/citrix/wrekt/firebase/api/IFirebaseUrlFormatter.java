package com.citrix.wrekt.firebase.api;

public interface IFirebaseUrlFormatter {

    String getBaseUrl();

    String getChannelsUrl();

    String getUsersUrl();

    String getPrivateChatsUrl();

    String getChatsUrl();

    String getSubscriptionsUrl();

    String getChannelMembersUrl();

    String getChannelAdminsUrl();

    String getFriendRequestsUrl();

    String getUserFriendsUrl();
}
