package com.halfway.halfwayapp;

import android.app.DownloadManager;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.util.Comparator;

import androidx.annotation.Nullable;

public class RequestCard {

    private GeoPoint primaryUserLocation;
    private GeoPoint secondaryUserLocation;
    private String primaryUserID;
    private String secondaryUserID;
    private GeoPoint midpoint;
    private String docID;
    private Timestamp timestamp;

    RequestCard(String pdocID, String pUserID, GeoPoint pUserLocation, String secUserId, GeoPoint secUserLocation, GeoPoint mid, Timestamp mTimestamp) {
        docID = pdocID;
        primaryUserID = pUserID;
        primaryUserLocation = pUserLocation;
        secondaryUserID = secUserId;
        secondaryUserLocation = secUserLocation;
        midpoint = mid;
        timestamp = mTimestamp;
    }

    public String getDocID() {
        return docID;
    }

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

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public GeoPoint getSecondaryUserLocation() {
        return secondaryUserLocation;
    }

    public static Comparator<RequestCard> timeStampComparator = new Comparator<RequestCard>() {

        public int compare(RequestCard one, RequestCard two) {

            return two.getTimestamp().toDate().compareTo(one.getTimestamp().toDate());

        }};

}
