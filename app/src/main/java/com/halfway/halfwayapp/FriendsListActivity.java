package com.halfway.halfwayapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Objects;

public class FriendsListActivity extends AppCompatActivity {

    private RecyclerView mUserRecycler;
    private UserAdapter mUserAdapter;
    private static ArrayList<User> mUsers;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

        mUserAdapter = new FriendsListActivity.UserAdapter();
        mUserRecycler = (RecyclerView) findViewById(R.id.user_recycler);
        mUserRecycler.setLayoutManager(new LinearLayoutManager(this));
        mUserRecycler.setAdapter(mUserAdapter);

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

    private class UserHolder extends RecyclerView.ViewHolder {
        private ImageView prof_pic;
        private TextView u_email;
        private Button req_but;

        public UserHolder (@NonNull View itemView) {
            super(itemView);
            prof_pic = itemView.findViewById(R.id.usr_img);
            u_email = itemView.findViewById(R.id.user_email);
            req_but = itemView.findViewById(R.id.req_button);
        }

        public void bind(User user) {

            Picasso.get().load(user.getPhotoURL()).into(prof_pic);
            u_email.setText(user.getEmail());
            req_but.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //TODO - implement requests
                    Toast.makeText(FriendsListActivity.this, "Request sent!", Toast.LENGTH_SHORT).show();
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
