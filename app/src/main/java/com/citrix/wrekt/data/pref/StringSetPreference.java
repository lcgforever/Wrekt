package com.citrix.wrekt.data.pref;

import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class StringSetPreference {

    private final SharedPreferences preferences;
    private final String key;
    private final Set<String> valueSet;

    public StringSetPreference(SharedPreferences preferences, String key) {
        this.preferences = preferences;
        this.key = key;
        this.valueSet = new HashSet<>();
    }

    public Set<String> get() {
        return preferences.getStringSet(key, new HashSet<String>());
    }

    public boolean isSet() {
        return preferences.contains(key);
    }

    public boolean hasValues() {
        return !valueSet.isEmpty();
    }

    public void add(String value) {
        valueSet.add(value);
        preferences.edit().putStringSet(key, valueSet).apply();
    }

    public void remove(String value) {
        valueSet.remove(value);
        preferences.edit().putStringSet(key, valueSet).apply();
    }

    public void delete() {
        preferences.edit().remove(key).apply();
    }
}