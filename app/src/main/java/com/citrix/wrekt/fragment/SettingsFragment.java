package com.citrix.wrekt.fragment;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.data.LoginState;
import com.citrix.wrekt.data.pref.IntegerPreference;
import com.citrix.wrekt.data.pref.StringPreference;
import com.citrix.wrekt.di.annotation.LoginStatePref;
import com.citrix.wrekt.di.annotation.UserEmailPref;
import com.citrix.wrekt.di.annotation.UsernamePref;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;

import javax.inject.Inject;

public class SettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceClickListener {

    @Inject
    @LoginStatePref
    IntegerPreference loginStatePref;

    @Inject
    @UsernamePref
    StringPreference usernamePref;

    @Inject
    @UserEmailPref
    StringPreference userEmailPref;

    private ActivityComponent activityComponent;
    private SettingsClickListener settingsClickListener;

    public static SettingsFragment newInstance() {
        SettingsFragment settingsFragment = new SettingsFragment();
        settingsFragment.setRetainInstance(true);
        return settingsFragment;
    }

    public SettingsFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            settingsClickListener = (SettingsClickListener) context;
        } catch (ClassCastException exception) {
            throw new ClassCastException(context.toString() + " must implement "
                    + SettingsClickListener.class.getSimpleName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WrektApplication application = (WrektApplication) getActivity().getApplication();
        activityComponent = application.getAppComponent().plus(new ActivityModule(getActivity()));
        activityComponent.inject(this);

        addPreferencesFromResource(R.xml.settings);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activityComponent = null;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        findPreference(getString(R.string.key_username))
                .setOnPreferenceClickListener(this);

        findPreference(getString(R.string.key_logout))
                .setOnPreferenceClickListener(this);

        updateViews();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        if (key.equals(getString(R.string.key_username))) {
            settingsClickListener.onUsernameSettingClicked();
            return true;
        } else if (key.equals(getString(R.string.key_logout))) {
            settingsClickListener.onLogoutSettingClicked();
            return true;
        }

        return false;
    }

    public void updateViews() {
        LoginState loginState = LoginState.from(loginStatePref.get());
        String loginMethodValue;
        switch (loginState) {
            case FACEBOOK_LOGGED_IN:
                loginMethodValue = getString(R.string.login_method_facebook);
                break;

            case GOOGLE_PLUS_LOGGED_IN:
                loginMethodValue = getString(R.string.login_method_google_plus);
                break;

            case GOTOMEETING_LOGGED_IN:
                loginMethodValue = getString(R.string.login_method_g2m);
                break;

            case WREKT_LOGGED_IN:
                loginMethodValue = getString(R.string.login_method_wrekt);
                break;

            default:
                loginMethodValue = "";
                break;
        }
        findPreference(getString(R.string.key_login_method))
                .setSummary(loginMethodValue);

        findPreference(getString(R.string.key_user_email))
                .setSummary(userEmailPref.get());

        findPreference(getString(R.string.key_username))
                .setSummary(usernamePref.get());
    }


    public interface SettingsClickListener {

        void onUsernameSettingClicked();

        void onPasswordSettingClicked();

        void onLogoutSettingClicked();
    }
}
