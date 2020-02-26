package com.halfway.halfwayapp;

import androidx.annotation.*;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.halfway.halfwayapp.MapRequestHelpers.FetchURL;
import com.halfway.halfwayapp.MapRequestHelpers.TaskLoadedCallback;
import com.squareup.picasso.Picasso;

import androidx.appcompat.app.*;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.widget.LinearLayout.HORIZONTAL;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, TaskLoadedCallback {
    private BottomSheetBehavior mBottomSheetBehavior;
    private TextView mTextViewState;

    private RecyclerView mPlaceRecycler;
    private PlaceAdapter mPlaceAdapter;

    private ArrayList<PlaceCard> mPlaces;
    private OkHttpClient mHTTPClient;

    private FirebaseFirestore db;
    private GoogleMap mMap;
    private Polyline drawDir;
    private ArrayList<LatLng> latlngs;
    private LatLng midpointLatLng;

    final private String MAPS_URL = "https://maps.googleapis.com/maps/api/place/textsearch/json?&location=";
    final private String RESPONSE_TAG = "com.halfway.response";
    private static final int RC_SIGN_IN = 123;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = FirebaseFirestore.getInstance();
        latlngs = new ArrayList<>();

        // Get the SupportMapFragment and register for the callback
        // when the map is ready for use.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mPlaceAdapter = new PlaceAdapter();
        mPlaceRecycler = (RecyclerView) findViewById(R.id.places_recycler);
        mPlaceRecycler.setLayoutManager(new LinearLayoutManager(this));
        mPlaceRecycler.setAdapter(mPlaceAdapter);

        DividerItemDecoration itemDecor = new DividerItemDecoration(getBaseContext(), DividerItemDecoration.VERTICAL);
        mPlaceRecycler.addItemDecoration(itemDecor);

        mPlaces = new ArrayList<PlaceCard>();
        mHTTPClient = new OkHttpClient();

        View bottomSheet = findViewById(R.id.bottom_sheet);

        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        //mTextViewState.setText("Collapsed");
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        //mTextViewState.setText("Dragging...");
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        //mTextViewState.setText("Expanded");
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        //mTextViewState.setText("Hidden");
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        //mTextViewState.setText("Settling...");
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                //mTextViewState.setText("Sliding...");
            }
        });
        fetchUserLocations();
        refresh();



    }

    public void grabMidpoint(){
        Log.d("INHERE", "TAG");



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the main_menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Manipulates the map when it's available.
     * The API invokes this callback when the map is ready for use.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        // Position the map's camera near Sydney, Australia.
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(-34, 151)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case R.id.test_button:
                //PopupDialog exampleDialog = new PopupDialog();
                //exampleDialog.show(getSupportFragmentManager(), "exampleDialog");
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.EmailBuilder().build());

                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .build(),
                        RC_SIGN_IN);




                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }


    //Preparing for Directions API, returns the correct info
    private String getRequestUrl(LatLng origin, LatLng dest) {
        //Value of origin
        String org = "origin=" + origin.latitude +","+origin.longitude;
        //Value of destination
        String destination = "destination=" + dest.latitude+","+dest.longitude;
        //Set value enable the sensor
        String sensor = "sensor=false";
        //Mode for find direction
        //!!! CAN CHANGE MODE !!!
        String mode = "mode=driving";
        //Build the full param
        String param = org +"&" + destination + "&" +sensor+"&" +mode;
        //Output format
        String output = "json";
        //Create url to request
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + param + "&key=" + getString(R.string.google_maps_key);
        return url;
    }

    private void fetchUserLocations(){
        db.collection("UserLocation")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                GeoPoint tempGP = (GeoPoint) document.get("latlng");
                                LatLng tempLL = new LatLng(tempGP.getLatitude(), tempGP.getLongitude());
                                latlngs.add(tempLL);
                            }
                        } else {
                            Log.w("ACCESS_ERROR", "Error getting documents.", task.getException());
                        }
                    }
                });

            DocumentReference docRef = db.collection( "Midpoint").document("kxLhrrEpYoCNgvogDte7");
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d("TAG", "DocumentSnapshot data: " + document.getData());
                        GeoPoint tempGP = (GeoPoint) document.get("midpoint");
                        LatLng midpoint = new LatLng(tempGP.getLatitude(), tempGP.getLongitude());
                        latlngs.add(midpoint);
                    } else {
                        Log.d("TAG", "No such document");
                    }
                } else {
                    Log.d("TAG", "get failed with ", task.getException());
                }
            }
        });



    }

    private class RefreshTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            grabMidpoint();
            Request request = new Request.Builder()
                    .url("https://maps.googleapis.com/maps/api/place/textsearch/json?type=restaurant&location=43.49041,-72.12494&radius=100000&key=AIzaSyACLyHMHhi7tsD7JRHAD4zubgFVZ7TepQQ")
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
                iconURL = item.getJSONArray("photos").getJSONObject(0).getString("photo_reference");

                //Store Type
                category = item.getJSONArray("types");
                String categoryInfo = category.get(0).toString();
                categoryInfo = categoryInfo.substring(0,1).toUpperCase() + categoryInfo.substring(1, categoryInfo.length());

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
        mPlaceAdapter.notifyDataSetChanged();
    }

    private class PlaceHolder extends RecyclerView.ViewHolder {
        private ImageView iv;
        private TextView title;
        private TextView cat;
        private TextView add;
        private Button but;

        public PlaceHolder (@NonNull View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.place_img);
            title = itemView.findViewById(R.id.place_title);
            add = itemView.findViewById(R.id.place_address);
            cat = itemView.findViewById(R.id.place_category);
            but = itemView.findViewById(R.id.button);
        }

        public void bind(PlaceCard place) {

            Picasso.get().load("https://maps.googleapis.com/maps/api/place/photo?" + place.getmIconURL() + "&key=AIzaSyACLyHMHhi7tsD7JRHAD4zubgFVZ7TepQQ").into(iv);

            title.setText(place.getmName());
            add.setText(place.getmAddress());
            cat.setText(place.getmCategory());
            final LatLng temp = place.getmCoordinates();
            but.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Taken From https://developers.google.com/maps/documentation/urls/android-intents

                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" + temp.latitude + "," + temp.longitude);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                }
            });
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

    @Override
    public void onTaskDone(Object... values) {
        if (drawDir != null)
            drawDir.remove();
        drawDir = mMap.addPolyline((PolylineOptions) values[0]);
    }



}
