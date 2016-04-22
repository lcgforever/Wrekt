package com.citrix.wrekt.data;

import android.support.annotation.NonNull;

public class PendingFriendRequest implements Comparable<PendingFriendRequest> {

    private String id;
    private String fromUid;
    private String fromUserEmail;
    private String fromUsername;
    private String toUid;
    private String toUserEmail;
    private String toUsername;
    private String status;
    private Long time;

    public PendingFriendRequest() {
    }

    public PendingFriendRequest(String id, String fromUid, String fromUserEmail, String fromUsername, String toUid, String toUserEmail, String toUsername, String status, Long time) {
        this.id = id;
        this.fromUid = fromUid;
        this.fromUserEmail = fromUserEmail;
        this.fromUsername = fromUsername;
        this.toUid = toUid;
        this.toUserEmail = toUserEmail;
        this.toUsername = toUsername;
        this.status = status;
        this.time = time;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFromUid() {
        return fromUid;
    }

    public void setFromUid(String fromUid) {
        this.fromUid = fromUid;
    }

    public String getFromUserEmail() {
        return fromUserEmail;
    }

    public void setFromUserEmail(String fromUserEmail) {
        this.fromUserEmail = fromUserEmail;
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }

    public String getToUid() {
        return toUid;
    }

    public void setToUid(String toUid) {
        this.toUid = toUid;
    }

    public String getToUserEmail() {
        return toUserEmail;
    }

    public void setToUserEmail(String toUserEmail) {
        this.toUserEmail = toUserEmail;
    }

    public String getToUsername() {
        return toUsername;
    }

    public void setToUsername(String toUsername) {
        this.toUsername = toUsername;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }


    @Override
    public int compareTo(@NonNull PendingFriendRequest another) {
        return -1 * time.compareTo(another.time);
    }
}
