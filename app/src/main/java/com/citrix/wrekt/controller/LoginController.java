package com.citrix.wrekt.controller;

import android.text.TextUtils;
import android.util.Log;

import com.citrix.wrekt.controller.api.ILoginController;
import com.citrix.wrekt.data.LoginState;
import com.citrix.wrekt.data.pref.IntegerPreference;
import com.citrix.wrekt.data.pref.LongPreference;
import com.citrix.wrekt.di.annotation.LoginExpireTimePref;
import com.citrix.wrekt.di.annotation.LoginStatePref;
import com.citrix.wrekt.event.FirebaseAuthFailedEvent;
import com.citrix.wrekt.event.FirebaseAuthSuccessfulEvent;
import com.citrix.wrekt.event.LogoutSuccessfulEvent;
import com.citrix.wrekt.firebase.api.IFirebaseFactory;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;
import com.facebook.login.LoginManager;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.squareup.otto.Bus;

import java.util.Map;

public class LoginController implements ILoginController {

    private IntegerPreference loginStatePref;
    private LongPreference loginExpireTimePref;
    private Bus bus;
    private IFirebaseFactory firebaseFactory;
    private IFirebaseUrlFormatter firebaseUrlFormatter;
    private Firebase baseRef;

    public LoginController(@LoginStatePref IntegerPreference loginStatePref,
                           @LoginExpireTimePref LongPreference loginExpireTimePref,
                           Bus bus,
                           IFirebaseFactory firebaseFactory,
                           IFirebaseUrlFormatter firebaseUrlFormatter) {
        this.loginStatePref = loginStatePref;
        this.loginExpireTimePref = loginExpireTimePref;
        this.firebaseFactory = firebaseFactory;
        this.firebaseUrlFormatter = firebaseUrlFormatter;
        this.bus = bus;
        baseRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getBaseUrl());
    }

    @Override
    public void signUpThenLogin(final String username, final String password) {
        baseRef.createUser(username, password, new Firebase.ValueResultHandler<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                Log.e("findme: ", "Firebase create user successfully with uid: " + result.get("uid"));
                login(LoginState.WREKT_LOGGED_IN, username, password);
            }

            @Override
            public void onError(FirebaseError firebaseError) {
                // there was an error
                Log.e("findme: ", "Firebase create user error: " + firebaseError.getMessage());
                bus.post(new FirebaseAuthFailedEvent(firebaseError.getMessage()));
            }
        });
    }

    @Override
    public void login(LoginState loginState, String username, String password) {
        switch (loginState) {
            case FACEBOOK_LOGGED_IN:

                break;

            case GOOGLE_PLUS_LOGGED_IN:

                break;

            case GOTOMEETING_LOGGED_IN:

                break;

            case WREKT_LOGGED_IN:
                baseRef.authWithPassword(username, password, new Firebase.AuthResultHandler() {
                    @Override
                    public void onAuthenticated(AuthData authData) {
                        Log.e("findme: ", "Firebase login with password successful with uid: "
                                + authData.getUid() + "  expires: " + authData.getExpires());
                        loginExpireTimePref.set(authData.getExpires() * 1000L);
                        bus.post(new FirebaseAuthSuccessfulEvent(LoginState.WREKT_LOGGED_IN));
                    }

                    @Override
                    public void onAuthenticationError(FirebaseError firebaseError) {
                        // there was an error
                        Log.e("findme: ", "Firebase create user error: " + firebaseError.getMessage());
                        bus.post(new FirebaseAuthFailedEvent(firebaseError.getMessage()));
                    }
                });
                break;

            case ANONYMOUS_LOGGED_IN:

                break;

            case NOT_LOGGED_IN:
            default:
                break;
        }
    }

    @Override
    public void loginWithOAuth(LoginState loginState, String authClient, String authToken) {
        switch (loginState) {
            case FACEBOOK_LOGGED_IN:
                if (!TextUtils.isEmpty(authClient) && !TextUtils.isEmpty(authToken)) {
                    baseRef.authWithOAuthToken(authClient, authToken, new Firebase.AuthResultHandler() {
                        @Override
                        public void onAuthenticated(AuthData authData) {
                            bus.post(new FirebaseAuthSuccessfulEvent(LoginState.FACEBOOK_LOGGED_IN));
                        }

                        @Override
                        public void onAuthenticationError(FirebaseError firebaseError) {
                            Log.e("findme: ", "Auth with firebase error: " + firebaseError.getMessage());
                            bus.post(new FirebaseAuthFailedEvent(firebaseError.getMessage()));
                        }
                    });
                } else {
                    baseRef.unauth();
                }
                break;

            case GOOGLE_PLUS_LOGGED_IN:

                break;

            case GOTOMEETING_LOGGED_IN:

                break;

            case WREKT_LOGGED_IN:
            case ANONYMOUS_LOGGED_IN:
            case NOT_LOGGED_IN:
            default:
                break;
        }
    }

    @Override
    public void logout() {
        LoginState loginState = LoginState.from(loginStatePref.get());
        switch (loginState) {
            case FACEBOOK_LOGGED_IN:
                LoginManager.getInstance().logOut();
                break;

            case GOOGLE_PLUS_LOGGED_IN:

                break;

            case GOTOMEETING_LOGGED_IN:

                break;

            case WREKT_LOGGED_IN:
            case ANONYMOUS_LOGGED_IN:
                baseRef.unauth();
                break;

            case NOT_LOGGED_IN:
            default:
                break;
        }
        setLogoutStateAndPostLogoutEvent();
    }

    private void setLogoutStateAndPostLogoutEvent() {
        loginExpireTimePref.set(0);
        loginStatePref.set(LoginState.NOT_LOGGED_IN.getValue());
        bus.post(new LogoutSuccessfulEvent());
    }
}
