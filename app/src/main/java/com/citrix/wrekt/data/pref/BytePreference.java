package com.citrix.wrekt.data.pref;

import android.content.SharedPreferences;
import android.util.Base64;

public class BytePreference {

    private final SharedPreferences preferences;
    private final String key;
    private final String defaultValue;

    public BytePreference(SharedPreferences preferences, String key) {
        this.preferences = preferences;
        this.key = key;
        this.defaultValue = null;
    }

    public byte[] get() {
        String persistedString = preferences.getString(key, defaultValue);
        return (persistedString == null) ? null : Base64.decode(persistedString, Base64.DEFAULT);
    }

    public boolean isSet() {
        return preferences.contains(key);
    }

    public void set(byte[] persistentState) {
        String persistedString = null;
        if (persistentState != null) {
            persistedString = Base64.encodeToString(persistentState, Base64.DEFAULT);
        }
        preferences.edit().putString(key, persistedString).apply();
    }

    public void delete() {
        preferences.edit().remove(key).apply();
    }
}
