package com.citrix.wrekt.event;

public class FirebaseAuthFailedEvent {

    private String errorMessage;

    public FirebaseAuthFailedEvent(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
