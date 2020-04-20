package com.halfway.halfwayapp;

import com.google.firebase.firestore.GeoPoint;

public class RequestCard {

    private GeoPoint primaryUserLocation;
    private GeoPoint secondaryUserLocation;
    private String primaryUserID;
    private String secondaryUserID;
    private GeoPoint midpoint;
    private String docID;

    RequestCard(String pdocID, String pUserID, GeoPoint pUserLocation, String secUserId, GeoPoint secUserLocation, GeoPoint mid) {
        docID = pdocID;
        primaryUserID = pUserID;
        primaryUserLocation = pUserLocation;
        secondaryUserID = secUserId;
        secondaryUserLocation = secUserLocation;
        midpoint = mid;
    }
    public String getDocID() { return docID; }

    public GeoPoint getMidpoint() {
        return midpoint;
    }

    public String getPrimaryUserID() {
        return primaryUserID;
    }

    public GeoPoint getPrimaryUserLocation() {
        return primaryUserLocation;
    }

    public String getSecondaryUserID() {
        return secondaryUserID;
    }

    public GeoPoint getSecondaryUserLocation() {
        return secondaryUserLocation;
    }
}