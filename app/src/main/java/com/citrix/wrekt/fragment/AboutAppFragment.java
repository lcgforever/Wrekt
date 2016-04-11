package com.citrix.wrekt.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;

import com.citrix.wrekt.BuildConfig;
import com.citrix.wrekt.R;
import com.citrix.wrekt.activity.CopyrightActivity;

public class AboutAppFragment extends PreferenceFragment
        implements Preference.OnPreferenceClickListener {

    public static AboutAppFragment newInstance() {
        AboutAppFragment aboutAppFragment = new AboutAppFragment();
        aboutAppFragment.setRetainInstance(true);
        return aboutAppFragment;
    }

    public AboutAppFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about_app);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        findPreference(getString(R.string.key_app_version))
                .setSummary(getString(R.string.value_app_version,
                        BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        findPreference(getString(R.string.key_copyright))
                .setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        if (key.equals(getString(R.string.key_copyright))) {
            Intent intent = new Intent(getActivity(), CopyrightActivity.class);
            getActivity().startActivity(intent);
        }

        return false;
    }
}
