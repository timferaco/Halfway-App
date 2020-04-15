package com.halfway.halfwayapp;

import android.net.Uri;

public class User {
    private String email;
    private String photoURL;
    private String displayName;


    public User(String email, String photoURL, String displayName) {
        this.email = email;
        this.photoURL = photoURL;
        this.displayName = displayName;
    }
    public  User() {
        email = "null";
        photoURL = null;
    }

    public String getEmail() {
        return email;
    }

    public String getPhotoURL() {
        return photoURL;
    }
}
