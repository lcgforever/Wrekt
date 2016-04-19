package com.citrix.wrekt.util;

import android.app.Activity;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;

public class KeyboardUtils {

    public static void openKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInputFromWindow((activity.getCurrentFocus() == null)
                ? null
                : activity.getCurrentFocus().getWindowToken(), InputMethodManager.SHOW_FORCED, 0);
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow((activity.getCurrentFocus() == null)
                ? null
                : activity.getCurrentFocus().getWindowToken(), 0);
    }
}


