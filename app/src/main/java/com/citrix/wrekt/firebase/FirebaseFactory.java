package com.citrix.wrekt.firebase;

import com.citrix.wrekt.firebase.api.IFirebaseFactory;
import com.firebase.client.Firebase;

public class FirebaseFactory implements IFirebaseFactory {

    @Override
    public Firebase createFirebase(String url) {
        return new Firebase(url);
    }
}
