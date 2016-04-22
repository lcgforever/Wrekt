package com.citrix.wrekt.di.component;

import com.citrix.wrekt.activity.BaseActivity;
import com.citrix.wrekt.activity.ChannelChatActivity;
import com.citrix.wrekt.activity.ChannelInfoActivity;
import com.citrix.wrekt.activity.ChannelMemberActivity;
import com.citrix.wrekt.activity.CreateChannelActivity;
import com.citrix.wrekt.activity.FriendActivity;
import com.citrix.wrekt.activity.FriendChatActivity;
import com.citrix.wrekt.activity.LoginActivity;
import com.citrix.wrekt.activity.MainActivity;
import com.citrix.wrekt.activity.SettingsActivity;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.di.scope.ActivityScope;
import com.citrix.wrekt.fragment.AcceptedFriendsFragment;
import com.citrix.wrekt.fragment.AllChannelsFragment;
import com.citrix.wrekt.fragment.BaseFragment;
import com.citrix.wrekt.fragment.MyChannelsFragment;
import com.citrix.wrekt.fragment.PendingFriendRequestsFragment;
import com.citrix.wrekt.fragment.SettingsFragment;

import dagger.Subcomponent;

@ActivityScope
@Subcomponent(
        modules = {
                ActivityModule.class
        }
)
public interface ActivityComponent {

    void inject(BaseActivity baseActivity);

    void inject(LoginActivity loginActivity);

    void inject(MainActivity mainActivity);

    void inject(CreateChannelActivity createChannelActivity);

    void inject(SettingsActivity settingsActivity);

    void inject(ChannelInfoActivity channelInfoActivity);

    void inject(ChannelMemberActivity channelMemberActivity);

    void inject(ChannelChatActivity channelChatActivity);

    void inject(FriendActivity friendActivity);

    void inject(FriendChatActivity friendChatActivity);

    void inject(BaseFragment baseFragment);

    void inject(AllChannelsFragment allChannelsFragment);

    void inject(MyChannelsFragment myChannelsFragment);

    void inject(AcceptedFriendsFragment acceptedFriendsFragment);

    void inject(PendingFriendRequestsFragment pendingFriendRequestsFragment);

    void inject(SettingsFragment settingsFragment);
}
