package com.citrix.wrekt.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.data.LoginState;
import com.citrix.wrekt.data.pref.IntegerPreference;
import com.citrix.wrekt.di.annotation.LoginStatePref;
import com.facebook.AccessToken;

import javax.inject.Inject;

public class SplashActivity extends AppCompatActivity {

    @Inject
    @LoginStatePref
    IntegerPreference loginStatePref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WrektApplication application = (WrektApplication) getApplication();
        application.getAppComponent().inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        LoginState loginState = LoginState.from(loginStatePref.get());
        switch (loginState) {
            case FACEBOOK_LOGGED_IN:
                AccessToken facebookAccessToken = AccessToken.getCurrentAccessToken();
                if (facebookAccessToken == null || facebookAccessToken.isExpired()) {
                    Toast.makeText(this, R.string.login_expired_message, Toast.LENGTH_LONG).show();
                    LoginActivity.start(this);
                } else {
                    MainActivity.start(this);
                }
                break;

            case GOOGLE_PLUS_LOGGED_IN:

                break;

            case GOTOMEETING_LOGGED_IN:

                break;

            case WREKT_LOGGED_IN:

                break;

            case ANONYMOUS_LOGGED_IN:

                break;

            case NOT_LOGGED_IN:
            default:
                LoginActivity.start(this);
                break;
        }

        finish();
    }
}