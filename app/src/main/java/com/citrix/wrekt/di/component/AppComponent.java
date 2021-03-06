package com.citrix.wrekt.di.component;

import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.activity.SplashActivity;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.di.module.AppModule;
import com.citrix.wrekt.di.module.BusModule;
import com.citrix.wrekt.di.module.DataModule;
import com.citrix.wrekt.di.module.FirebaseModule;
import com.citrix.wrekt.di.module.LoginModule;
import com.citrix.wrekt.di.module.NotificationModule;
import com.citrix.wrekt.service.FriendRequestService;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(
        modules = {
                AppModule.class,
                DataModule.class,
                BusModule.class,
                LoginModule.class,
                FirebaseModule.class,
                NotificationModule.class
        }
)
public interface AppComponent {

    ActivityComponent plus(ActivityModule activityModule);

    void inject(WrektApplication application);

    void inject(SplashActivity splashActivity);

    void inject(FriendRequestService friendRequestService);
}
