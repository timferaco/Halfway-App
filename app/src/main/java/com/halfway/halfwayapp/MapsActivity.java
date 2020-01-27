package com.halfway.halfwayapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.firestore.GeoPoint;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.libraries.places.api.model.PlaceLikelihood;


import com.halfway.halfwayapp.MapRequestHelpers.FetchURL;
import com.halfway.halfwayapp.MapRequestHelpers.TaskLoadedCallback;

import java.util.ArrayList;

import java.util.List;


import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;

public class  MapsActivity extends AppCompatActivity implements OnMapReadyCallback, TaskLoadedCallback {




        private static final String TAG = MapsActivity.class.getSimpleName();
        ArrayList<LatLng> latlngs;
        MarkerOptions options = new MarkerOptions();
        GoogleMap mMap;
        private UserLocation mUserLocation;
        Polyline drawDir;
        FirebaseFirestore db;
        FieldSelector fieldSelector;
        PlacesClient placesClient;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Retrieve the content view that renders the map.
            setContentView(R.layout.activity_maps);

            // Get the SupportMapFragment and register for the callback
            // when the map is ready for use.
            SupportMapFragment mapFragment =
                    (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
            db = FirebaseFirestore.getInstance();
            latlngs = new ArrayList<>();
            saveUserLocation();

            List<Place.Field> placeFields =
                    FieldSelector.allExcept(
                            Place.Field.ADDRESS_COMPONENTS,
                            Place.Field.OPENING_HOURS,
                            Place.Field.PHONE_NUMBER,
                            Place.Field.UTC_OFFSET,
                            Place.Field.WEBSITE_URI);

            fieldSelector =
                    new FieldSelector(
                            placeFields,
                            savedInstanceState);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the main_menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //saveUserLocation();
        switch(item.getItemId()) {
            case R.id.test_button:

                /*//Create marker
                Marker m1 = mMap.addMarker(new MarkerOptions()
                        .position(latlngs.get(0))
                        .anchor(0.5f, 0.5f)
                        .title("Position 1")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                Marker m2 = mMap.addMarker(new MarkerOptions()
                        .position(latlngs.get(1))
                        .anchor(0.5f, 0.5f)
                        .title("Position 2")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                mMap.moveCamera(CameraUpdateFactory.newLatLng(latlngs.get(0)));

                // Construct a CameraPosition focusing on Mountain View and animate the camera to that position.
                //https://developers.google.com/maps/documentation/android-sdk/views
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(latlngs.get(0))      // Sets the center of the map to Mountain View
                        .zoom(13)                   // Sets the zoom
                        .bearing(0)                // Sets the orientation of the camera to east
                        .build();                   // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                //Create the URL to get request from first marker to second marker
                String url = getRequestUrl(latlngs.get(0), latlngs.get(1), 2);
                Log.d("RequestURL", url);
                FetchURL furl = new FetchURL(MapsActivity.this);
                furl.execute(url, "driving");
                */

                Places.initialize(this, getString(R.string.google_api_key));
                placesClient = Places.createClient(this);
                findCurrentPlace();


                break;
            default:
                return super.onOptionsItemSelected(item);
        }



        return true;
    }

    private void saveUserLocation(){
        db.collection("UserLocation")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                Log.d(TAG, document.get("latlng").toString());
                                GeoPoint tempGP = (GeoPoint) document.get("latlng");
                                LatLng tempLL = new LatLng(tempGP.getLatitude(), tempGP.getLongitude());

                                latlngs.add(tempLL);
                                Log.d(TAG, "latlngs size: " + latlngs.size());


                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });


    }
    //Preparing for Directions API, returns the correct info
    private String getRequestUrl(LatLng origin, LatLng dest, int identifier) {
        String url = "";
        //Value of origin
        String org = "origin=" + origin.latitude + "," + origin.longitude;
        //Value of destination
        String destination = "destination=" + dest.latitude + "," + dest.longitude;
        //Set value enable the sensor
        String sensor = "sensor=false";
        //Mode for find direction
        //!!! CAN CHANGE MODE !!!
        String mode = "mode=driving";
        //Build the full param
        String param = org + "&" + destination + "&" + sensor + "&" + mode;
        //Output format
        String output = "json";
        //Create url to request
        url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + param + "&key=" + getString(R.string.google_maps_key);
        return url;

    }

        @Override
        public void onTaskDone(Object... values) {
            if (drawDir != null)
                drawDir.remove();
            drawDir = mMap.addPolyline((PolylineOptions) values[0]);
        }

    private void findCurrentPlace() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(
                    this,
                    "Both ACCESS_WIFI_STATE & ACCESS_FINE_LOCATION permissions are required",
                    Toast.LENGTH_SHORT)
                    .show();
        }

        // Note that it is not possible to request a normal (non-dangerous) permission from
        // ActivityCompat.requestPermissions(), which is why the checkPermission() only checks if
        // ACCESS_FINE_LOCATION is granted. It is still possible to check whether a normal permission
        // is granted or not using ContextCompat.checkSelfPermission().
        if (checkPermission(ACCESS_FINE_LOCATION)) {
            findCurrentPlaceWithPermissions();
        }
    }

    /**
     * Fetches a list of {@link PlaceLikelihood} instances that represent the Places the user is
     * most
     * likely to be at currently.
     */
    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    private void findCurrentPlaceWithPermissions() {

        FindCurrentPlaceRequest currentPlaceRequest =
                FindCurrentPlaceRequest.newInstance(getPlaceFields());
        Task<FindCurrentPlaceResponse> currentPlaceTask =
                placesClient.findCurrentPlace(currentPlaceRequest);

        currentPlaceTask.addOnSuccessListener(
                (response) ->
                        parseResponse(response)

                        );

        currentPlaceTask.addOnFailureListener(
                (exception) -> {
                    Log.d("FAILURE:", exception.toString());
                });

    }


    public static void parseResponse(FindCurrentPlaceResponse response) {

        System.out.println(response.toString());

        List<PlaceLikelihood> allLikelyHoods = response.getPlaceLikelihoods();

        Place a = allLikelyHoods.get(0).getPlace();

        System.out.println(allLikelyHoods.size());
        System.out.println(a.getRating());
        System.out.println(a.getPriceLevel());
        System.out.println(a.getUserRatingsTotal());



    }

    //////////////////////////
    // Helper methods below //
    //////////////////////////



    private boolean checkPermission(String permission) {
        boolean hasPermission =
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
        }
        return hasPermission;
    }

    private List<Place.Field> getPlaceFields() {

        return fieldSelector.getAllFields();
    }

}
