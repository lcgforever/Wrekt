package com.citrix.wrekt.firebase;

import android.content.Context;

import com.citrix.wrekt.R;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;

public class FirebaseUrlFormatter implements IFirebaseUrlFormatter {

    private Context context;
    private String baseUrl;

    public FirebaseUrlFormatter(Context context, String baseUrl) {
        this.context = context;
        this.baseUrl = baseUrl;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public String getChanneslUrl() {
        return String.format(context.getString(R.string.firebase_channels_url), baseUrl);
    }
}
