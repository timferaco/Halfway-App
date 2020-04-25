package com.halfway.halfwayapp;

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class RequestsActivity extends AppCompatActivity {

    private RecyclerView mReqRecycler;
    private ReqAdapter mReqAdapter;
    private static ArrayList<RequestCard> mRequests;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);

        //Initialize RecyclerView
        mReqAdapter = new ReqAdapter();
        mReqRecycler = findViewById(R.id.req_recycler);
        mReqRecycler.setLayoutManager(new LinearLayoutManager(this));
        mReqRecycler.setAdapter(mReqAdapter);
        mRequests = new ArrayList<RequestCard>();
        db = FirebaseFirestore.getInstance();

        //Load RecyclerView
        fetchRequests();
        mReqAdapter.notifyDataSetChanged();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void fetchRequests(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        db.collection("UserProfiles").document(user.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()) {
                        ArrayList<String> temp = (ArrayList<String>) document.get("requests");
                        final ArrayList requestList = new ArrayList();
                        //Checks to see if there are requests
                        if (temp != null) {
                            //Loops through each Request
                            for (int i = 0; i < temp.size(); i++) {
                                db.document(temp.get(i)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        //Adds Requests to requestList
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot document = task.getResult();
                                            if (document.exists()) {
                                                String docID = (String) document.get("docID");
                                                String primaryUserID = (String) document.get("primaryUserEmail");
                                                String secondaryUserID = (String) document.get("secondaryUserEmail");
                                                String midpoint = (String) document.get("midpoint");
                                                Timestamp timestamp = (Timestamp) document.get("timestamp");
                                                Log.d("TIMESTAMP", timestamp.toString());

                                                requestList.add(new RequestCard(docID, primaryUserID, secondaryUserID, midpoint, timestamp));

                                            } else {
                                                //No Exists
                                            }
                                        }
                                        //Sorts by Timestamp
                                        Collections.sort(requestList, RequestCard.timeStampComparator);
                                        mRequests = requestList;
                                        mReqAdapter.notifyDataSetChanged();
                                    }
                                });

                            }
                        }
                    }
                } else {
                    //Document Doesn't Exist
                }

            }
        });
    }

    private class ReqHolder extends RecyclerView.ViewHolder {
        private TextView prim;
        private TextView sec;
        private CircleImageView primProfile;
        private CircleImageView secProfilePic;
        private TextView timestamp;
        private FirebaseFunctions mFunctions;

        public ReqHolder (@NonNull View itemView) {
            super(itemView);
            prim = itemView.findViewById(R.id.prim_user);
            sec = itemView.findViewById(R.id.sec_user);
            primProfile = itemView.findViewById(R.id.prim_user_image);
            secProfilePic = itemView.findViewById(R.id.sec_user_image);
            timestamp = itemView.findViewById(R.id.timestamp);

        }

        public void bind(final RequestCard request) {
            //Sets Text and Images
            prim.setText(request.getPrimaryUserID());
            sec.setText(request.getSecondaryUserID());

            //Adds pending text if request is pending
            if(request.getMidpoint() == "") {
                timestamp.setText("Pending: " + request.getTimestamp().toDate().toString());
            } else {
                timestamp.setText(request.getTimestamp().toDate().toString());
            }

            //Loads Profile pictures
            StorageReference storageReference = FirebaseStorage.getInstance().getReference("profilePictures/" +request.getPrimaryUserID());
            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Picasso.get().load(uri).transform(new FriendsSheetActivity.CircleTransform()).into(primProfile);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    primProfile.setImageDrawable(getResources().getDrawable(R.drawable.ic_person_black_24dp));
                }
            });

            //Loads other Users Profile Picture
           storageReference = FirebaseStorage.getInstance().getReference("profilePictures/" +request.getSecondaryUserID());
            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Picasso.get().load(uri).transform(new FriendsSheetActivity.CircleTransform()).into(secProfilePic);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("PROFPIC", "FAIL");
                    secProfilePic.setImageDrawable(getResources().getDrawable(R.drawable.ic_person_black_24dp));
                }
            });

            mFunctions = FirebaseFunctions.getInstance();

            //Setup onClickListener for requests
            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final Intent launchRequests = new Intent(getBaseContext(), MainActivity.class);
                        //If there is no midpoint, request is pending
                        if(request.getMidpoint() == "") {
                            //If it is the secondary user then we need to load up request and get midpoint
                            if(request.getSecondaryUserID().equals(user.getEmail())) {
                                fusedLocationClient.getLastLocation()
                                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                                            @Override
                                            public void onSuccess(Location location) {
                                                // Got last known location. In some rare situations this can be null.
                                                if (location != null) {
                                                    final Intent tempLaunchRequests = new Intent(getBaseContext(), MainActivity.class);
                                                    HashMap<String, Object> updatedRequest = new HashMap<>();
                                                    //Update Secondary User Location
                                                    updatedRequest.put("secondaryUserLocation", location.getLatitude()+"," + location.getLongitude());
                                                    db.document(request.getDocID()).update(updatedRequest);

                                                    //Todo: GET MIdPOINT HERE
                                                    //Todo: Send latitude and Longitude not Null

                                                    HashMap<String, Object> docIDSend = new HashMap<>();
                                                    String docID = request.getDocID();
                                                    docID = docID.substring(9, docID.length());


                                                    docIDSend.put("docID", docID);
                                                    Log.d("docID", docID);

                                                    Task<String> result = mFunctions
                                                            .getHttpsCallable("findMidpoint")
                                                            .call(docIDSend)
                                                            .continueWith(new Continuation<HttpsCallableResult, String>() {
                                                                @Override
                                                                public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                                                                    // This continuation runs on either success or failure, but if the task
                                                                    // has failed then getResult() will throw an Exception which will be
                                                                    // propagated down.
                                                                    String result = (String) task.getResult().getData();
                                                                    String[] midpointString = result.split(",");
                                                                    launchRequests.putExtra("latitude", midpointString[0]);
                                                                    launchRequests.putExtra("longitude", midpointString[1]);
                                                                    startActivity(tempLaunchRequests);
                                                                    Log.d("THIS HAS RESPONDED", result);
                                                                    return result;
                                                                }
                                                            });


                                                }
                                            }
                                        });
                            } else {
                                //If it is the primary user just send to activity and notify it is pending
                                launchRequests.putExtra("latitude", "null");
                                startActivity(launchRequests);
                            }

                        } else { //If there is a midpoint, simply add it to Extras and launch activity

                            String[] midpointString = request.getMidpoint().split(",");
                            launchRequests.putExtra("latitude", midpointString[0]);
                            launchRequests.putExtra("longitude", midpointString[1]);
                            startActivity(launchRequests);
                        }
                    }
                });
            }
    }

    private class ReqAdapter extends RecyclerView.Adapter<ReqHolder> {
        @NonNull
        @Override
        public ReqHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater li = LayoutInflater.from(getApplicationContext());
            return new ReqHolder(li.inflate(R.layout.requests_cell, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ReqHolder holder, int position) {
            holder.bind(mRequests.get(position));
        }

        @Override
        public int getItemCount() {
            return mRequests.size();
        }
    }
}
