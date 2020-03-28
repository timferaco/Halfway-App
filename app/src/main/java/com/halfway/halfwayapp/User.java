package com.halfway.halfwayapp;

public class User {
    private String email;
    private String photoURL;


    public User(String email, String photoURL) {
        this.email = email;
        this.photoURL = photoURL;
    }
    public  User() {
        email = "null";
        photoURL = "null";
    }

    public String getEmail() {
        return email;
    }

    public String getPhotoURL() {
        return photoURL;
    }
}
