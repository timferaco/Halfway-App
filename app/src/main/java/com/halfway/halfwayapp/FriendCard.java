package com.halfway.halfwayapp;
import android.net.Uri;

public class FriendCard {

    String email;
    Uri photo;

    FriendCard(String mEmail, String uri){
        email = mEmail;
        photo = Uri.parse(uri);

    }

    public String getEmail() {
        return email;
    }

    public Uri getPhoto() {
        return photo;
    }

}
