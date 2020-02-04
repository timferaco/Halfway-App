package com.halfway.halfwayapp;


import com.google.android.gms.maps.model.LatLng;

public class PlaceCard {

    private String mID;
    private LatLng mCoordinates;
    private String mName;
    private String mAddress;
    private String mCategory;


    PlaceCard(String id, String name, LatLng coordinates, String address, String category) {
        mID = id;
        mName = name;
        mCoordinates = coordinates;
        mAddress = address;
        mCategory = category;

    }

    public String getmAddress() {
        return mAddress;
    }

    public String getmCategory() {
        return mCategory;
    }

    public LatLng getmCoordinates() {
        return mCoordinates;
    }

    public String getmID() {
        return mID;
    }

    public String getmName() {
        return mName;
    }

}
