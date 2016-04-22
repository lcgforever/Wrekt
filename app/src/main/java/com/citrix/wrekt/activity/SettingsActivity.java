package com.citrix.wrekt.activity;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.controller.api.ILoginController;
import com.citrix.wrekt.data.pref.StringPreference;
import com.citrix.wrekt.di.annotation.UidPref;
import com.citrix.wrekt.di.annotation.UsernamePref;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.event.LogoutFailedEvent;
import com.citrix.wrekt.event.LogoutSuccessfulEvent;
import com.citrix.wrekt.firebase.api.IFirebaseFactory;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;
import com.citrix.wrekt.fragment.SettingsFragment;
import com.citrix.wrekt.fragment.dialog.ChangeUsernameDialogFragment;
import com.citrix.wrekt.fragment.dialog.LogoutDialogFragment;
import com.citrix.wrekt.service.FriendRequestService;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.squareup.otto.Subscribe;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class SettingsActivity extends BaseActivity implements SettingsFragment.SettingsClickListener,
        LogoutDialogFragment.LogoutActionListener, ChangeUsernameDialogFragment.ChangeUsernameActionListener {

    private static final String TAG_SETTINGS_FRAGMENT = "TAG_SETTINGS_FRAGMENT";
    private static final String TAG_LOGOUT_DIALOG = "TAG_LOGOUT_DIALOG";
    private static final String TAG_CHANGE_USERNAME_DIALOG = "TAG_CHANGE_USERNAME_DIALOG";

    @Inject
    @UidPref
    StringPreference uidPref;

    @Inject
    @UsernamePref
    StringPreference usernamePref;

    @Inject
    ILoginController loginController;

    @Inject
    IFirebaseFactory firebaseFactory;

    @Inject
    IFirebaseUrlFormatter firebaseUrlFormatter;

    private ActivityComponent activityComponent;
    private FragmentManager fragmentManager;

    public static void start(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        inject();

        fragmentManager = getFragmentManager();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, SettingsFragment.newInstance(), TAG_SETTINGS_FRAGMENT)
                    .commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        hideChangeUsernameDialog();
        hideLogoutDialog();
        super.onSaveInstanceState(outState);
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
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onUsernameSettingClicked() {
        showChangeUsernameDialog();
    }

    @Override
    public void onPasswordSettingClicked() {

    }

    @Override
    public void onLogoutSettingClicked() {
        showLogoutDialog();
    }

    @Override
    public void onUsernameChanged(final String newUsername) {
        Firebase userRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getUsersUrl()).child(uidPref.get());
        Map<String, Object> usernameMap = new HashMap<>();
        usernameMap.put("username", newUsername);
        userRef.updateChildren(usernameMap, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError == null) {
                    Log.e("findme: ", "Firebase change username successfully: " + newUsername);
                    usernamePref.set(newUsername);
                    SettingsFragment settingsFragment
                            = (SettingsFragment) getFragmentManager().findFragmentByTag(TAG_SETTINGS_FRAGMENT);
                    if (settingsFragment != null) {
                        settingsFragment.updateViews();
                    }
                } else {
                    Log.e("findme: ", "Firebase change username failed: " + firebaseError.getMessage());
                    Toast.makeText(SettingsActivity.this,
                            R.string.change_username_failed_message,
                            Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    @Override
    public void onLogoutConfirmed() {
        loginController.logout();
    }

    @Subscribe
    public void onLogoutSuccessfulEventReceived(LogoutSuccessfulEvent event) {
        FriendRequestService.stop(this);
        LoginActivity.start(this);
        finish();
    }

    @Subscribe
    public void onLogoutFailedEventReceived(LogoutFailedEvent event) {
        Toast.makeText(this, R.string.logout_error_message, Toast.LENGTH_SHORT).show();
    }

    private void showChangeUsernameDialog() {
        ChangeUsernameDialogFragment fragment
                = (ChangeUsernameDialogFragment) fragmentManager.findFragmentByTag(TAG_CHANGE_USERNAME_DIALOG);
        if (fragment == null) {
            fragment = ChangeUsernameDialogFragment.newInstance(usernamePref.get());
        }
        fragment.show(fragmentManager, TAG_CHANGE_USERNAME_DIALOG);
    }

    private void hideChangeUsernameDialog() {
        ChangeUsernameDialogFragment fragment
                = (ChangeUsernameDialogFragment) fragmentManager.findFragmentByTag(TAG_CHANGE_USERNAME_DIALOG);
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private void showLogoutDialog() {
        LogoutDialogFragment fragment = (LogoutDialogFragment) fragmentManager.findFragmentByTag(TAG_LOGOUT_DIALOG);
        if (fragment == null) {
            fragment = LogoutDialogFragment.newInstance();
        }
        fragment.show(fragmentManager, TAG_LOGOUT_DIALOG);
    }

    private void hideLogoutDialog() {
        LogoutDialogFragment fragment = (LogoutDialogFragment) fragmentManager.findFragmentByTag(TAG_LOGOUT_DIALOG);
        if (fragment != null) {
            fragment.dismiss();
        }
    }
}
