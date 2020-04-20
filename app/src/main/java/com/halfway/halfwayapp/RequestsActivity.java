package com.halfway.halfwayapp;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Objects;

public class RequestsActivity extends AppCompatActivity {

    private RecyclerView mReqRecycler;
    private ReqAdapter mReqAdapter;
    private static ArrayList<RequestCard> mRequests;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);

        mReqAdapter = new ReqAdapter();
        mReqRecycler = findViewById(R.id.req_recycler);
        mReqRecycler.setLayoutManager(new LinearLayoutManager(this));
        mReqRecycler.setAdapter(mReqAdapter);

        mRequests = new ArrayList<RequestCard>();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        fetchRequests();


        mReqAdapter.notifyDataSetChanged();
    }

    private void fetchRequests(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        db.collection("UserProfiles").document(user.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()) {
                        ArrayList<String> temp = new ArrayList<>();

                        temp = (ArrayList<String>) document.get("requests");
                        final ArrayList requestList = new ArrayList();
                        for(int i = 0; i < temp.size(); i++) {

                            db.document(temp.get(i)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                                    if(task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();

                                        if(document.exists()){

                                            GeoPoint primaryUserLocation = (GeoPoint) document.get("primaryUserLocation");
                                            GeoPoint secondaryUserLocation = (GeoPoint) document.get("secondaryUserLocation");
                                            String primaryUserID = (String) document.get("primaryUserEmail");
                                            String secondaryUserID = (String) document.get("secondaryUserEmail");
                                            GeoPoint midpoint = (GeoPoint) document.get("midpoint");

                                            requestList.add(new RequestCard(primaryUserID, primaryUserLocation, secondaryUserID, secondaryUserLocation, midpoint));


                                            //User temp = new User(document.get("email").toString(), document.get("photoURL").toString(), document.get("name").toString());
                                            //tempUsers.add(temp);
                                            //Log.d("!!!Temp", String.valueOf(tempUsers.size()));
                                        } else {
                                            //No Exists
                                        }
                                    }
                                    //mUsers = tempUsers;
                                    mRequests = requestList;
                                    mReqAdapter.notifyDataSetChanged();
                                    //mUserAdapter.notifyDataSetChanged();
                                }
                            });

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

        public ReqHolder (@NonNull View itemView) {
            super(itemView);
            prim = itemView.findViewById(R.id.prim_user);
            sec = itemView.findViewById(R.id.sec_user);
            primProfile = itemView.findViewById(R.id.prim_user_image);
            secProfilePic = itemView.findViewById(R.id.sec_user_image);

        }

        public void bind(RequestCard request) {
            prim.setText(request.getPrimaryUserID());
            sec.setText(request.getSecondaryUserID());

            StorageReference storageReference = FirebaseStorage.getInstance().getReference("profilePictures/" +request.getPrimaryUserID());
            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Picasso.get().load(uri).transform(new FriendsSheetActivity.CircleTransform()).into(primProfile);
                }
            });

           storageReference = FirebaseStorage.getInstance().getReference("profilePictures/" +request.getSecondaryUserID());
            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Picasso.get().load(uri).transform(new FriendsSheetActivity.CircleTransform()).into(secProfilePic);
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
