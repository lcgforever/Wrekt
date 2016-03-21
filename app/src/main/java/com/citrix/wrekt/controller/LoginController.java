package com.citrix.wrekt.controller;

import android.support.annotation.NonNull;

import com.citrix.wrekt.controller.api.ILoginController;
import com.citrix.wrekt.data.LoginState;
import com.citrix.wrekt.data.pref.IntegerPreference;
import com.citrix.wrekt.di.annotation.LoginStatePref;
import com.citrix.wrekt.event.LogoutFailedEvent;
import com.citrix.wrekt.event.LogoutSuccessfulEvent;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.squareup.otto.Bus;

public class LoginController implements ILoginController {

    private IntegerPreference loginStatePref;
    private Bus bus;
    private GoogleApiClient googleApiClient;

    public LoginController(@LoginStatePref IntegerPreference loginStatePref, Bus bus) {
        this.loginStatePref = loginStatePref;
        this.bus = bus;
    }

    @Override
    public void logout() {
        LoginState loginState = LoginState.from(loginStatePref.get());
        switch (loginState) {
            case FACEBOOK_LOGGED_IN:
                LoginManager.getInstance().logOut();
                setLogoutStateAndPostLogoutEvent();
                break;

            case GOOGLE_PLUS_LOGGED_IN:
                if (googleApiClient != null) {
                    Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess()) {
                                setLogoutStateAndPostLogoutEvent();
                            } else {
                                bus.post(new LogoutFailedEvent(status.getStatusMessage()));
                            }
                        }
                    });
                    googleApiClient.disconnect();
                    googleApiClient = null;
                }
                break;

            case GOTOMEETING_LOGGED_IN:

                break;

            case WREKT_LOGGED_IN:

                break;

            case ANONYMOUS_LOGGED_IN:

                break;

            case NOT_LOGGED_IN:
            default:
                setLogoutStateAndPostLogoutEvent();
                break;
        }
    }

    @Override
    public GoogleApiClient getGoogleApiClient() {
        return googleApiClient;
    }

    @Override
    public void setGoogleApiClient(GoogleApiClient googleApiClient) {
        this.googleApiClient = googleApiClient;
    }

    private void setLogoutStateAndPostLogoutEvent() {
        loginStatePref.set(LoginState.NOT_LOGGED_IN.getValue());
        bus.post(new LogoutSuccessfulEvent());
    }
}
