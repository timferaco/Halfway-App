package com.halfway.halfwayapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
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
import com.halfway.halfwayapp.MapRequestHelpers.FetchURL;
import com.halfway.halfwayapp.MapRequestHelpers.TaskLoadedCallback;

import java.util.ArrayList;
import java.lang.Object.*;
import com.google.firebase.*;
import java.util.Map;

public class  MapsActivity extends AppCompatActivity implements OnMapReadyCallback, TaskLoadedCallback {




        private static final String TAG = MapsActivity.class.getSimpleName();
        ArrayList<LatLng> latlngs;
        MarkerOptions options = new MarkerOptions();
        GoogleMap mMap;
        private UserLocation mUserLocation;
        Polyline drawDir;
        FirebaseFirestore db;

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
        saveUserLocation();

        Log.d("TAG", Integer.toString(latlngs.size()));
        switch(item.getItemId()) {
            case R.id.test_button:
                //LatLong One Setup
                //LatLng home = new LatLng(44.473100, -73.204666);
                //latlngs.add(home);
                // LatLong Two Setup
                //LatLng artsRiot = new LatLng(44.468231, -73.215122);
                //latlngs.add(artsRiot);

                //Create marker
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
                String url = getRequestUrl(latlngs.get(0), latlngs.get(1));
                FetchURL furl = new FetchURL(MapsActivity.this);
                furl.execute(url, "driving");

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
                                GeoPoint tempGP = (GeoPoint) document.get("latlng");
                                LatLng tempLL = new LatLng(tempGP.getLatitude(), tempGP.getLongitude());

                                latlngs.add(tempLL);


                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });


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

    @Override
    public void onTaskDone(Object... values) {
        if (drawDir != null)
            drawDir.remove();
        drawDir = mMap.addPolyline((PolylineOptions) values[0]);
    }
}
