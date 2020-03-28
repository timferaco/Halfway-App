package com.halfway.halfwayapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class FriendsListActivity extends AppCompatActivity {

    private RecyclerView mUserRecycler;
    private UserAdapter mUserAdapter;
    private ArrayList<User> mUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

        mUserAdapter = new FriendsListActivity.UserAdapter();
        mUserRecycler = (RecyclerView) findViewById(R.id.user_recycler);
        mUserRecycler.setLayoutManager(new LinearLayoutManager(this));
        mUserRecycler.setAdapter(mUserAdapter);

        mUsers = new ArrayList<User>();

        mUserAdapter.notifyDataSetChanged();
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
            Log.d("USERICONURL",  user.getPhotoURL());
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
            return new FriendsListActivity.UserHolder(li.inflate(R.layout.places_cell, parent, false));
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
