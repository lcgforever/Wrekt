package com.citrix.wrekt.event;

import com.citrix.wrekt.data.LoginState;

public class FirebaseAuthSuccessfulEvent {

    private LoginState loginState;

    public FirebaseAuthSuccessfulEvent(LoginState loginState) {
        this.loginState = loginState;
    }

    public LoginState getLoginState() {
        return loginState;
    }
}
