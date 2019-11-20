package com.halfway.halfwayapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

public class  MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

        private static final String TAG = MapsActivity.class.getSimpleName();

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
        }

        /**
         * Manipulates the map when it's available.
         * The API invokes this callback when the map is ready for use.
         */
        @Override
        public void onMapReady(GoogleMap googleMap) {


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

        switch(item.getItemId()) {
            case R.id.test_button:
                //your action
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }
}
