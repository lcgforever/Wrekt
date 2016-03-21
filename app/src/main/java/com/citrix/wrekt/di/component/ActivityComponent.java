package com.citrix.wrekt.di.component;

import com.citrix.wrekt.activity.BaseActivity;
import com.citrix.wrekt.activity.LoginActivity;
import com.citrix.wrekt.activity.MainActivity;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.di.scope.ActivityScope;

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
}
