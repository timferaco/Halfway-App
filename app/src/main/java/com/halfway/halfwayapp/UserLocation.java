package com.halfway.halfwayapp;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.*;

public class UserLocation {
    private LatLng location;
    private User user;

    public UserLocation(LatLng currLocation, User currUser) {
        location = currLocation;
        user = currUser;
    }
    public UserLocation(){
        location = null;
        user = new User();
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }
    public LatLng getLocation() {
        return location;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
