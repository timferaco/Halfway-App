package com.halfway.halfwayapp;

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
        mReqRecycler = (RecyclerView) findViewById(R.id.req_recycler);
        mReqRecycler.setLayoutManager(new LinearLayoutManager(this));
        mReqRecycler.setAdapter(mReqAdapter);

        mRequests = new ArrayList<RequestCard>();
        db = FirebaseFirestore.getInstance();
        Log.d("!!URL1", "Fetch Requests");


        mReqAdapter.notifyDataSetChanged();
    }

    private class ReqHolder extends RecyclerView.ViewHolder {
        private TextView prim;
        private TextView sec;

        public ReqHolder (@NonNull View itemView) {
            super(itemView);
            prim = itemView.findViewById(R.id.prim_user);
            sec = itemView.findViewById(R.id.sec_user);
        }

        public void bind(RequestCard request) {
            prim.setText(request.getPrimaryUserID());
            sec.setText(request.getSecondaryUserID());
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
