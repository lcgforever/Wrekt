package com.citrix.wrekt.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.data.LoginState;
import com.citrix.wrekt.data.pref.IntegerPreference;
import com.citrix.wrekt.di.annotation.LoginStatePref;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import javax.inject.Inject;

public class LoginActivity extends BaseActivity {

    @Inject
    @LoginStatePref
    IntegerPreference loginStatePref;

    private ActivityComponent activityComponent;

    private LoginButton facebookLoginButton;

    private CallbackManager callbackManager;

    public static void start(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inject();

        facebookLoginButton = (LoginButton) findViewById(R.id.facebook_login_button);

        callbackManager = CallbackManager.Factory.create();

        setupFacebookLogin();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityComponent = null;
    }

    @Override
    protected void inject() {
        WrektApplication application = (WrektApplication) getApplication();
        activityComponent = application.getAppComponent().plus(new ActivityModule(this));
        activityComponent.inject(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void setupFacebookLogin() {
        facebookLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                setLoginStateAndFinish(LoginState.FACEBOOK_LOGGED_IN);
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {
                Log.e("findme: ", "Facebook login error: " + error.getMessage());
                Toast.makeText(LoginActivity.this, R.string.login_error_message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoginStateAndFinish(LoginState loginState) {
        loginStatePref.set(loginState.getValue());
        MainActivity.start(LoginActivity.this);
        finish();
    }
}
