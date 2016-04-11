package com.citrix.wrekt.di.component;

import com.citrix.wrekt.activity.BaseActivity;
import com.citrix.wrekt.activity.ChannelInfoActivity;
import com.citrix.wrekt.activity.LoginActivity;
import com.citrix.wrekt.activity.MainActivity;
import com.citrix.wrekt.activity.SettingsActivity;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.di.scope.ActivityScope;
import com.citrix.wrekt.fragment.AllChannelsFragment;
import com.citrix.wrekt.fragment.BaseFragment;
import com.citrix.wrekt.fragment.MyChannelsFragment;
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

    void inject(SettingsActivity settingsActivity);

    void inject(ChannelInfoActivity channelInfoActivity);

    void inject(BaseFragment baseFragment);

    void inject(AllChannelsFragment allChannelsFragment);

    void inject(MyChannelsFragment myChannelsFragment);

    void inject(SettingsFragment settingsFragment);
}
