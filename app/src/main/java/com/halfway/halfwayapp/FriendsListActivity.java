package com.halfway.halfwayapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class FriendsListActivity extends AppCompatActivity  {

    private RecyclerView mUserRecycler;
    private UserAdapter mUserAdapter;
    private static ArrayList<User> mUsers;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

        mUserAdapter = new UserAdapter();
        mUserRecycler = findViewById(R.id.user_recycler);
        mUserRecycler.setLayoutManager(new LinearLayoutManager(this));
        mUserRecycler.setAdapter(mUserAdapter);

        mUsers = new ArrayList<User>();
        db = FirebaseFirestore.getInstance();

        //Load Recycler View
        fetchFriends();

        //Add Decor
        DividerItemDecoration itemDecor = new DividerItemDecoration(getBaseContext(), DividerItemDecoration.VERTICAL);
        mUserRecycler.addItemDecoration(itemDecor);
        mUserAdapter.notifyDataSetChanged();
    }

    private void fetchFriends(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            db.collection("UserProfiles").document(user.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if(document.exists()) {
                            ArrayList<String> temp = (ArrayList<String>) document.get("friends");
                            final ArrayList tempUsers = new ArrayList();
                            //Loop through friendsList
                            for(int i = 0; i < temp.size(); i++) {
                                db.collection("UserProfiles").document(temp.get(i)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if(task.isSuccessful()) {
                                            DocumentSnapshot document = task.getResult();
                                            if(document.exists()){
                                                //Create a new User for RecyclerView
                                                User temp = new User(document.get("email").toString(),  document.get("name").toString());
                                                tempUsers.add(temp);
                                            } else {
                                                //No Exists
                                            }
                                        }
                                        mUsers = tempUsers;
                                        mUserAdapter.notifyDataSetChanged();
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

    private class UserHolder extends RecyclerView.ViewHolder {
        private ImageView prof_pic;
        private TextView u_email;
        private TextView prev_message;
        private FirebaseAuth mFirebaseAuth;
        private FirebaseUser mFirebaseUser;

        public UserHolder(@NonNull View itemView) {
            super(itemView);
            prof_pic = itemView.findViewById(R.id.usr_img);
            u_email = itemView.findViewById(R.id.user_email);
            prev_message = itemView.findViewById(R.id.previous_message);
        }

        public String getMessagesChild(String userEmail, String firebaseUserEmail) {
            String temp = "";
            if(userEmail.compareTo(firebaseUserEmail) > 0) {
                temp = "messages" + userEmail + firebaseUserEmail;
            } else {
                temp = "messages" + firebaseUserEmail + userEmail;
            }
            temp = temp.replaceAll("[^a-zA-Z]", "");

            return temp;
        }

        public void bind(final User user) {

            u_email.setText(user.getEmail());
            prev_message.setText("Send a Message!");

            DatabaseReference mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
            mFirebaseAuth = FirebaseAuth.getInstance();
            mFirebaseUser = mFirebaseAuth.getCurrentUser();
            //Sets user Image
            StorageReference storageReference = FirebaseStorage.getInstance().getReference("profilePictures/" +user.getEmail());
            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                        Picasso.get().load(uri).transform(new FriendsSheetActivity.CircleTransform()).into(prof_pic);

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    prof_pic.setImageDrawable(getResources().getDrawable(R.drawable.ic_person_black_24dp));
                }
            });

            //Takes the most recent message to display
            final String messagesChild = getMessagesChild(mFirebaseUser.getEmail(), user.getEmail());
            DatabaseReference messagesRef = mFirebaseDatabaseReference.child(messagesChild);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent launchChat = new Intent(getBaseContext(), ChatActivity.class);
                    launchChat.putExtra("EMAIL", user.getEmail());
                    launchChat.putExtra("MESSAGES_CHILD", getMessagesChild(mFirebaseUser.getEmail(), user.getEmail()));
                    startActivity(launchChat);
                }
            });

            //Addchildevent listener to update the last message sent
            messagesRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    prev_message.setText(dataSnapshot.child("text").getValue().toString());
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private class UserAdapter extends RecyclerView.Adapter<FriendsListActivity.UserHolder> {
        @NonNull
        @Override
        public FriendsListActivity.UserHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater li = LayoutInflater.from(getApplicationContext());
            return new FriendsListActivity.UserHolder(li.inflate(R.layout.friends_cell, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull FriendsListActivity.UserHolder holder, int position) {
            holder.bind(mUsers.get(position));
        }
        @Override
        public int getItemCount() {
            return mUsers.size();
        }
    }
}
