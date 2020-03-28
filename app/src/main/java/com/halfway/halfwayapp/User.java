package com.halfway.halfwayapp;

import android.net.Uri;

public class User {
    String email;
    Uri photo;

    User(String mEmail, String uri){
        email = mEmail;
        photo = Uri.parse(uri);

    }

    public String getEmail() {
        return email;
    }

    public Uri getPhoto() {
        return photo;
    }
    public  User() {
        email = "null";
        email = "null";
    }


}
