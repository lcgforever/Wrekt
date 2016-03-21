package com.citrix.wrekt.data;

public enum LoginState {

    NOT_LOGGED_IN(0),
    FACEBOOK_LOGGED_IN(1),
    GOOGLE_PLUS_LOGGED_IN(2),
    GOTOMEETING_LOGGED_IN(3),
    WREKT_LOGGED_IN(4),
    ANONYMOUS_LOGGED_IN(5);

    int value;

    LoginState(int value) {
        this.value = value;
    }

    public static LoginState from(int value) {
        for (LoginState loginState : values()) {
            if (loginState.value == value) {
                return loginState;
            }
        }

        return NOT_LOGGED_IN;
    }

    public int getValue() {
        return value;
    }
}
