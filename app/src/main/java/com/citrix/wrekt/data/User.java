package com.citrix.wrekt.data;

import android.support.annotation.NonNull;

public class User implements Comparable<User> {

    private String uid;
    private String userEmail;
    private String username;
    private int sortPriority;

    public User() {
    }

    public User(String uid, String userEmail, String username, int sortPriority) {
        this.uid = uid;
        this.userEmail = userEmail;
        this.username = username;
        this.sortPriority = sortPriority;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public int compareTo(@NonNull User another) {
        if (sortPriority == another.sortPriority) {
            return username.compareTo(another.getUsername());
        } else {
            // High priority first
            return another.sortPriority - sortPriority;
        }
    }
}
