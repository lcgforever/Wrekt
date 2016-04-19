package com.citrix.wrekt.firebase.api;

public interface IFirebaseUrlFormatter {

    String getBaseUrl();

    String getChannelsUrl();

    String getUsersUrl();

    String getChatsUrl();

    String getSubscriptionsUrl();

    String getChannelMembersUrl();

    String getChannelAdminsUrl();
}
