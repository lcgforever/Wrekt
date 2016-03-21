package com.citrix.wrekt.controller.api;

import com.google.android.gms.common.api.GoogleApiClient;

public interface ILoginController {

    void logout();

    GoogleApiClient getGoogleApiClient();

    void setGoogleApiClient(GoogleApiClient googleApiClient);
}
