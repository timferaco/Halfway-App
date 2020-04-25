package com.halfway.halfwayapp;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import java.util.Comparator;

public class RequestCard {

    private String primaryUserID;
    private String secondaryUserID;
    private String midpoint;
    private String docID;
    private Timestamp timestamp;

    RequestCard(String pdocID, String pUserID,  String secUserId, String mid, Timestamp mTimestamp) {
        docID = pdocID;
        primaryUserID = pUserID;
        secondaryUserID = secUserId;
        midpoint = mid;
        timestamp = mTimestamp;
    }

    public String getDocID() {
        return docID;
    }

    public String getMidpoint() {
        return midpoint;
    }

    public String getPrimaryUserID() {
        return primaryUserID;
    }

    public String getSecondaryUserID() {
        return secondaryUserID;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    //Sort requests by timeStamp
    public static Comparator<RequestCard> timeStampComparator = new Comparator<RequestCard>() {

        public int compare(RequestCard one, RequestCard two) {

            return two.getTimestamp().toDate().compareTo(one.getTimestamp().toDate());

        }};

}
