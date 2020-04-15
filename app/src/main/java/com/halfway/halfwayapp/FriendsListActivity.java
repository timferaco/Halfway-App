package com.halfway.halfwayapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.opencensus.trace.MessageEvent;

import android.content.Intent;
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

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Objects;


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
        mUserRecycler = (RecyclerView) findViewById(R.id.user_recycler);
        mUserRecycler.setLayoutManager(new LinearLayoutManager(this));
        mUserRecycler.setAdapter(mUserAdapter);

        mUserRecycler.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        mUsers = new ArrayList<User>();
        db = FirebaseFirestore.getInstance();
        fetchFriends();

        DividerItemDecoration itemDecor = new DividerItemDecoration(getBaseContext(), DividerItemDecoration.VERTICAL);
        mUserRecycler.addItemDecoration(itemDecor);




        mUserAdapter.notifyDataSetChanged();
    }

    private void fetchFriends(){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        db.collection("FriendsList").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    ArrayList<User> tempUsers = new ArrayList<User>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Log.d("DocumentSnapshot data! ", Objects.requireNonNull(document.get("email")).toString());
                        Log.d("DocumentSnapshot data! ", Objects.requireNonNull(document.get("photo")).toString());
                        User temp = new User(document.get("email").toString(), document.get("photo").toString());

                        tempUsers.add(temp);
                    }
                    mUsers = tempUsers;
                    Log.d("!!Size!", String.valueOf(mUsers.size()));
                    mUserAdapter.notifyDataSetChanged();

                } else {
                    Log.w("ACCESS_ERROR", "Error getting documents.", task.getException());
                }
            }
        });
        Log.d("!!Size", String.valueOf(mUsers.size()));
    }

    /*@Override
    public void onNoteClick(int position) {
        User user = mUsers.get(position);
        Intent launchChat = new Intent(getBaseContext(), ChatActivity.class);
        launchChat.putExtra("EMAIL", user.getEmail());
        launchChat.putExtra("MESSAGES_CHILD", MESSAGES_CHILD);

        Log.d("SCRATUSEREMAIL", user.getEmail());
        startActivity(launchChat);

    }*/

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

            Picasso.get().load(user.getPhotoURL()).into(prof_pic);
            u_email.setText(user.getEmail());

            DatabaseReference mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
            mFirebaseAuth = FirebaseAuth.getInstance();
            mFirebaseUser = mFirebaseAuth.getCurrentUser();


            final String messagesChild = getMessagesChild(mFirebaseUser.getEmail(), user.getEmail());
            DatabaseReference messagesRef = mFirebaseDatabaseReference.child(messagesChild);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent launchChat = new Intent(getBaseContext(), ChatActivity.class);
                    launchChat.putExtra("EMAIL", user.getEmail());
                    launchChat.putExtra("MESSAGES_CHILD", getMessagesChild(mFirebaseUser.getEmail(), user.getEmail()));
                    Log.d("SCRATUSEREMAIL", messagesChild);
                    startActivity(launchChat);
                }
            });

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
