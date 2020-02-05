package com.halfway.halfwayapp;

import androidx.annotation.*;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.gson.JsonArray;
import com.squareup.picasso.Picasso;

import androidx.appcompat.app.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private BottomSheetBehavior mBottomSheetBehavior;
    private TextView mTextViewState;

    private RecyclerView mPlaceRecycler;
    private PlaceAdapter mPlaceAdapter;

    private ArrayList<PlaceCard> mPlaces;
    private OkHttpClient mHTTPClient;
    final private String MAPS_URL = "https://maps.googleapis.com/maps/api/place/textsearch/json?&location=";

    final private String RESPONSE_TAG = "com.halfway.response";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPlaceAdapter = new PlaceAdapter();
        mPlaceRecycler = (RecyclerView) findViewById(R.id.places_recycler);
        mPlaceRecycler.setLayoutManager(new LinearLayoutManager(this));
        mPlaceRecycler.setAdapter(mPlaceAdapter);

        mPlaces = new ArrayList<PlaceCard>();
        mHTTPClient = new OkHttpClient();

        View bottomSheet = findViewById(R.id.bottom_sheet);

        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        mTextViewState = findViewById(R.id.text_view_state);

        Button buttonExpand = findViewById(R.id.button_expand);
        Button buttonCollapse = findViewById(R.id.button_collapse);

        buttonExpand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        buttonCollapse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        mTextViewState.setText("Collapsed");
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        mTextViewState.setText("Dragging...");
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        mTextViewState.setText("Expanded");
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        mTextViewState.setText("Hidden");
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        mTextViewState.setText("Settling...");
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                mTextViewState.setText("Sliding...");
            }
        });
        refresh();
        mPlaceAdapter.notifyDataSetChanged();
    }

    private class RefreshTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            Request request = new Request.Builder()
                    .url("https://maps.googleapis.com/maps/api/place/textsearch/json?type=bakery&location=44.482618,-73.209405&radius=10000&key=AIzaSyACLyHMHhi7tsD7JRHAD4zubgFVZ7TepQQ")
                    .build();


            try (Response response = mHTTPClient.newCall(request).execute()) {
                Log.d("CALL", "doInBackground: making call to Google API");
                return response.body().string();
            } catch (Exception e) {
                e.printStackTrace();
                return "Error: Could not complete request.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            parseJSON(result);

        }


    }

    private void refresh() {
        RefreshTask rt = new RefreshTask();
        rt.execute();
    }

    private void parseJSON(String jsonString) {
        String id;
        String name;
        PlaceCard temp;
        String address;
        JSONArray category;
        String iconURL;

        try {
            JSONObject responseObject = new JSONObject(jsonString);
            JSONArray items = responseObject.getJSONArray("results");
            for (int i = 0; i < items.length(); i++) {
                //Store id, address, icon and name
                JSONObject item = items.getJSONObject(i);
                id = item.getString("id");
                address = item.getString("formatted_address");
                name = item.getString("name");
                iconURL = item.getString("icon");


                //Store Type
                category = item.getJSONArray("types");
                String categoryInfo = category.get(0).toString();

                //Store Coords
                JSONObject geometry = item.getJSONObject("geometry").getJSONObject("location");
                LatLng coordinates = new LatLng(geometry.getDouble("lat"), geometry.getDouble("lng"));

                //Create Object
                temp = new PlaceCard(id, name, coordinates, address, categoryInfo, iconURL);
                mPlaces.add(temp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private class PlaceHolder extends RecyclerView.ViewHolder {
        private ImageView iv;
        private TextView title;
        private TextView cat;

        public PlaceHolder (@NonNull View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.place_img);
            title = itemView.findViewById(R.id.place_title);
            cat = itemView.findViewById(R.id.place_category);
        }

        public void bind(PlaceCard place) {

            Picasso.get().load(place.getmIconURL()).into(iv);
            title.setText(place.getmName());
            cat.setText(place.getmCategory());
            //iv.setImageBitmap(b);

        }
    }

    private class PlaceAdapter extends RecyclerView.Adapter<PlaceHolder> {
        @NonNull
        @Override
        public PlaceHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater li = LayoutInflater.from(getApplicationContext());
            return new PlaceHolder(li.inflate(R.layout.places_cell, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PlaceHolder holder, int position) {
            holder.bind(mPlaces.get(position));
        }

        @Override
        public int getItemCount() {
            return mPlaces.size();
        }
    }

}
