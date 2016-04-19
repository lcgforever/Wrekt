package com.citrix.wrekt.controller;

import android.text.TextUtils;
import android.util.Log;

import com.citrix.wrekt.controller.api.ILoginController;
import com.citrix.wrekt.data.LoginState;
import com.citrix.wrekt.data.pref.IntegerPreference;
import com.citrix.wrekt.data.pref.LongPreference;
import com.citrix.wrekt.data.pref.StringPreference;
import com.citrix.wrekt.di.annotation.LoginExpireTimePref;
import com.citrix.wrekt.di.annotation.LoginStatePref;
import com.citrix.wrekt.di.annotation.UidPref;
import com.citrix.wrekt.di.annotation.UserEmailPref;
import com.citrix.wrekt.di.annotation.UsernamePref;
import com.citrix.wrekt.event.FirebaseAuthFailedEvent;
import com.citrix.wrekt.event.FirebaseAuthSuccessfulEvent;
import com.citrix.wrekt.event.LogoutSuccessfulEvent;
import com.citrix.wrekt.firebase.api.IFirebaseFactory;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.squareup.otto.Bus;

import java.util.HashMap;
import java.util.Map;

public class LoginController implements ILoginController {

    private IntegerPreference loginStatePref;
    private LongPreference loginExpireTimePref;
    private StringPreference uidPref;
    private StringPreference usernamePref;
    private StringPreference userEmailPref;
    private Bus bus;
    private IFirebaseFactory firebaseFactory;
    private IFirebaseUrlFormatter firebaseUrlFormatter;
    private Firebase baseRef;
    private Firebase usersRef;

    public LoginController(@LoginStatePref IntegerPreference loginStatePref,
                           @LoginExpireTimePref LongPreference loginExpireTimePref,
                           @UidPref StringPreference uidPref,
                           @UsernamePref StringPreference usernamePref,
                           @UserEmailPref StringPreference userEmailPref,
                           Bus bus,
                           IFirebaseFactory firebaseFactory,
                           IFirebaseUrlFormatter firebaseUrlFormatter) {
        this.loginStatePref = loginStatePref;
        this.loginExpireTimePref = loginExpireTimePref;
        this.uidPref = uidPref;
        this.usernamePref = usernamePref;
        this.userEmailPref = userEmailPref;
        this.firebaseFactory = firebaseFactory;
        this.firebaseUrlFormatter = firebaseUrlFormatter;
        this.bus = bus;
        baseRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getBaseUrl());
        usersRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getUsersUrl());
    }

    @Override
    public void signUpThenLogin(final String email,
                                final String password,
                                final String username) {
        baseRef.createUser(email, password, new Firebase.ValueResultHandler<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                Log.e("findme: ", "Firebase create user successfully with uid: " + result.get("uid"));
                login(LoginState.WREKT_LOGGED_IN, email, password, username);
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
    public void login(final LoginState loginState,
                      final String email,
                      final String password,
                      final String username) {
        switch (loginState) {
            case GOTOMEETING_LOGGED_IN:

                break;

            case WREKT_LOGGED_IN:
                baseRef.authWithPassword(email, password, new Firebase.AuthResultHandler() {
                    @Override
                    public void onAuthenticated(AuthData authData) {
                        Log.e("findme: ", "Firebase login user: " + authData.getUid());
                        loginExpireTimePref.set(authData.getExpires() * 1000L);
                        storeUserAndFinishLogin(LoginState.WREKT_LOGGED_IN, authData.getUid(), email, username);
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

            case FACEBOOK_LOGGED_IN:
            case GOOGLE_PLUS_LOGGED_IN:
            case NOT_LOGGED_IN:
            default:
                break;
        }
    }

    @Override
    public void loginWithOAuth(final LoginState loginState,
                               final String authClient,
                               final String authToken,
                               final String email) {
        switch (loginState) {
            case FACEBOOK_LOGGED_IN:
                if (!TextUtils.isEmpty(authClient) && !TextUtils.isEmpty(authToken)) {
                    baseRef.authWithOAuthToken(authClient, authToken, new Firebase.AuthResultHandler() {
                        @Override
                        public void onAuthenticated(AuthData authData) {
                            Log.e("findme: ", "Firebase login OAuth user successfully: " + authData.getUid());
                            loginExpireTimePref.set(authData.getExpires() * 1000L);
                            Profile facebookProfile = Profile.getCurrentProfile();
                            storeUserAndFinishLogin(loginState, authData.getUid(), email, facebookProfile.getName());
                        }

                        @Override
                        public void onAuthenticationError(FirebaseError firebaseError) {
                            Log.e("findme: ", "Auth with firebase error: " + firebaseError.getMessage());
                            bus.post(new FirebaseAuthFailedEvent(firebaseError.getMessage()));
                        }
                    });
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

    private void storeUserAndFinishLogin(final LoginState loginState,
                                         final String uid,
                                         final String email,
                                         final String username) {
        final Firebase userRef = usersRef.child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Object> userMap;
                // If user already exists, we fetch uid and username; otherwise store the user in Firebase
                if (dataSnapshot != null && dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    userMap = (Map<String, Object>) dataSnapshot.getValue();
                    uidPref.set(userMap.get("uid").toString());
                    usernamePref.set(userMap.get("username").toString());
                    userEmailPref.set(userMap.get("email").toString());
                    // Login successfully till now, post event
                    bus.post(new FirebaseAuthSuccessfulEvent(loginState));
                } else {
                    userMap = new HashMap<>();
                    userMap.put("uid", uid);
                    userMap.put("email", email);
                    userMap.put("username", username);
                    userRef.setValue(userMap, new Firebase.CompletionListener() {
                        @Override
                        public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                            if (firebaseError == null) {
                                Log.e("findme: ", "Firebase store user successfully");
                                uidPref.set(uid);
                                usernamePref.set(username);
                                userEmailPref.set(email);
                                // Login successfully till now, post event
                                bus.post(new FirebaseAuthSuccessfulEvent(loginState));
                            } else {
                                bus.post(new FirebaseAuthFailedEvent(firebaseError.getMessage()));
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    private void setLogoutStateAndPostLogoutEvent() {
        loginExpireTimePref.set(0);
        uidPref.set("");
        usernamePref.set("");
        loginStatePref.set(LoginState.NOT_LOGGED_IN.getValue());
        bus.post(new LogoutSuccessfulEvent());
    }
}
