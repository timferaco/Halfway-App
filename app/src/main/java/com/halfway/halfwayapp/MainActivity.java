package com.halfway.halfwayapp;

import androidx.annotation.*;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.common.api.GoogleApi;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.halfway.halfwayapp.MapRequestHelpers.TaskLoadedCallback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.squareup.picasso.Transformation;

import androidx.appcompat.app.*;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.location.Location;
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
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, TaskLoadedCallback {
    private BottomSheetBehavior mBottomSheetBehavior;
    private TextView mTextViewState;

    private RecyclerView mPlaceRecycler;
    private PlaceAdapter mPlaceAdapter;
    private FloatingActionButton floatingActionButton;
    private FusedLocationProviderClient fusedLocationClient;

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
    private static final int PICK_IMAGE = 100;


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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            floatingActionButton = findViewById(R.id.floatingActionButton);

            Log.d("PhotoURL1", user.getPhotoUrl().toString());

            //floatingActionButton.setImageDrawable(CircleImageView Picasso.get().load(user.getPhotoUrl()));
            Picasso.get().load(user.getPhotoUrl()).transform(new CircleTransform()).into(floatingActionButton);
        } else {
            //No user sign in
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build());

            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN);
        }



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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        switch(item.getItemId()) {

            case R.id.log_in:
                if (user == null) {
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
                }




                break;
            case R.id.log_out:
                if (user != null) {
                    AuthUI.getInstance()
                            .signOut(this)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                public void onComplete(@NonNull Task<Void> task) {
                                    // ...
                                }
                            });

                    floatingActionButton = findViewById(R.id.floatingActionButton);
                    floatingActionButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_person_white_24dp));
                }
                Log.d("TAG1", "");
                break;
            case R.id.change_picture:
                if (user != null) {
                    openGallery();
                } else {
                    // No user is signed in
                }
                break;
            case R.id.del_acct:
                user.delete()
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d("Deleted", "User account deleted.");
                                }
                            }
                        });

                floatingActionButton = findViewById(R.id.floatingActionButton);
                floatingActionButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_person_white_24dp));
                break;
            case R.id.chat:
                //TODO: implement chat
                Log.d("LATLONG", "IN CHAT");
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    Log.d("LATLONG", String.valueOf(location.getLatitude()));
                                    Log.d("LATLONG", String.valueOf(location.getLongitude()));
                                }
                                //Log.d("LATLONG", String.valueOf(location.getLatitude()));
                                //Log.d("LATLONG", String.valueOf(location.getLongitude()));
                            }
                        });



                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void openGallery() {
        Intent gallery =
                new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setPhotoUri(data.getData())
                    .build();


            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d("Updated", "User profile updated.");
                                floatingActionButton = findViewById(R.id.floatingActionButton);
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                Log.d("PhotoURL2", user.getPhotoUrl().toString());
                                Picasso.get().load(user.getPhotoUrl()).transform(new CircleTransform()).into(floatingActionButton);
                            }
                        }
                    });
            Log.d("PhotoURL3", user.getPhotoUrl().toString());
            //Picasso.get().load(user.getPhotoUrl()).transform(new CircleTransform()).into(floatingActionButton);
        }

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                floatingActionButton = findViewById(R.id.floatingActionButton);

                if(!user.isEmailVerified()) {
                    user.sendEmailVerification()
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.d("TAG", "Email sent.");
                                    }
                                }
                            });
                }

                if(user.getPhotoUrl() == null) {
                    floatingActionButton = findViewById(R.id.floatingActionButton);
                    floatingActionButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_person_white_24dp));
                } else {
                    Picasso.get().load(user.getPhotoUrl()).transform(new CircleTransform()).into(floatingActionButton);
                }
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

            Picasso.get().load("https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=" + place.getmIconURL() + "&key=AIzaSyACLyHMHhi7tsD7JRHAD4zubgFVZ7TepQQ").into(iv);
            Log.d("PLACEICONURL",  place.getmIconURL());
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
    //https://gist.github.com/julianshen/5829333
    //Apache License
    private class CircleTransform implements Transformation {
        @Override
        public Bitmap transform(Bitmap source) {
            int size = Math.min(source.getWidth(), source.getHeight());

            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;

            Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
            if (squaredBitmap != source) {
                source.recycle();
            }

            Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());

            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            BitmapShader shader = new BitmapShader(squaredBitmap, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setAntiAlias(true);

            float r = size/2f;
            canvas.drawCircle(r, r, r, paint);

            squaredBitmap.recycle();
            return bitmap;
        }

        @Override
        public String key() {
            return "circle";
        }
    }

    @Override
    public void onTaskDone(Object... values) {
        if (drawDir != null)
            drawDir.remove();
        drawDir = mMap.addPolyline((PolylineOptions) values[0]);
    }





}
