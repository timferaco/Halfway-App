package com.halfway.halfwayapp;


public class User {
    private String email;
    private String photoURL;
    private String displayName;


    public User(String email,  String displayName) {
        this.email = email;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhotoURL() {
        return photoURL;
    }
}
