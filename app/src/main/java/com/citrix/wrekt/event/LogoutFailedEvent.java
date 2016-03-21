package com.citrix.wrekt.event;

public class LogoutFailedEvent {

    private String failureMessage;

    public LogoutFailedEvent(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public String getFailureMessage() {
        return failureMessage;
    }
}
