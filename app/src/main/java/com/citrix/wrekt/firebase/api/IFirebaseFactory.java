package com.citrix.wrekt.firebase.api;

import com.firebase.client.Firebase;

public interface IFirebaseFactory {

    Firebase createFirebase(String url);
}
