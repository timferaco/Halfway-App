package com.halfway.halfwayapp;

public class User {
    private String userID;
    private String email;

    public User(String ID, String emailAddress) {
        userID = ID;
        email = emailAddress;
    }
    public  User() {
        userID = "null";
        email = "null";
    }


}
