package com.halfway.halfwayapp;


import com.google.android.gms.maps.model.LatLng;

public class PlaceCard {

    private String mID;
    private LatLng mCoordinates;
    private String mName;
    private String mAddress;
    private String mCategory;
    private String mIconURL;


    PlaceCard(String id, String name, LatLng coordinates, String address, String category, String iconURL) {
        mID = id;
        mName = name;
        mCoordinates = coordinates;
        mAddress = address;
        mCategory = category;
        mIconURL = iconURL;
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

    public String getmIconURL() { return mIconURL; }

}
