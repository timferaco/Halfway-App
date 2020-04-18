package com.halfway.halfwayapp;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class FriendsSheetActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private RecyclerView mFriendSheetRecycler;
    private FriendAdapter mFriendSheetAdapter;
    private FloatingActionButton floatingActionButton;

    private ArrayList<User> mFriends;
    private OkHttpClient mHTTPClient;
    private FusedLocationProviderClient fusedLocationClient;

    private TextView prof_email;
    private TextView prof_display_name;
    private CircleImageView prof_picture;
    private FirebaseFirestore db;

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    private LatLng currentLocation;

    private GoogleMap mMap;

    final private String MAPS_URL = "https://maps.googleapis.com/maps/api/place/textsearch/json?&location=";
    final private String RESPONSE_TAG = "com.halfway.response";
    private static final int RC_SIGN_IN = 123;
    private static final int PICK_IMAGE = 100;
    protected LocationManager locationManager;
    protected LocationListener locationListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_sheet);

        //Updates Left Drawer
        drawerLayout = findViewById(R.id.drawer);
        navigationView = findViewById(R.id.navigationView);
        floatingActionButton = findViewById(R.id.floatingActionButton);

        currentLocation = new LatLng(0, 0);
        getCurrentLocation();
        Log.d("Location!:", String.valueOf(currentLocation.latitude));


        navigationView.bringToFront();
        navigationView.setNavigationItemSelectedListener(FriendsSheetActivity.this);

        db = FirebaseFirestore.getInstance();

        // Get the SupportMapFragment and register for the callback
        // when the map is ready for use.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFriendSheetAdapter = new FriendAdapter();
        mFriendSheetRecycler = findViewById(R.id.friends_sheet_recycler);
        mFriendSheetRecycler.setLayoutManager(new LinearLayoutManager(this));
        mFriendSheetRecycler.setAdapter(mFriendSheetAdapter);

        DividerItemDecoration itemDecor = new DividerItemDecoration(getBaseContext(), DividerItemDecoration.VERTICAL);
        mFriendSheetRecycler.addItemDecoration(itemDecor);

        mFriends = new ArrayList<User>();
        mHTTPClient = new OkHttpClient();


        // Firebase Setup
        db = FirebaseFirestore.getInstance();
        //Checks to see if there is a current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // Grabs profile picture
            floatingActionButton = findViewById(R.id.floatingActionButton);
            Picasso.get().load(user.getPhotoUrl()).transform(new CircleTransform()).into(floatingActionButton);
            updateUserProfile();


        } else {
            //No user sign in
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build());//Google Login doesn't work yet, but it adds the logo on the choose provider screen

            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setLogo(R.drawable.logo1x)
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN);
        }

        //Sets up Drawer
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
                Log.d("DRAWER", "onClick: Open Drawer");
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    prof_email = findViewById(R.id.prof_email);
                    prof_display_name = findViewById(R.id.prof_disp_name);
                    prof_picture = findViewById(R.id.profilePic);
                    prof_email.setText(user.getEmail());
                    prof_display_name.setText(user.getDisplayName());

                    Picasso.get().load(user.getPhotoUrl()).transform(new CircleTransform()).into(prof_picture);
                }
            }
        });
        drawerLayout.closeDrawers();

    }

    public void updateUserProfile() {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        //Update UserProfiles

        db.collection("UserProfiles").document(user.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Map<String, Object> profile = new HashMap<>();
                        ArrayList<String> friends = new ArrayList<>();
                        profile.put("name", user.getDisplayName());
                        profile.put("email", user.getEmail());
                        db.collection("UserProfiles").document(user.getEmail()).update(profile);
                    } else {
                        Map<String, Object> profile = new HashMap<>();
                        ArrayList<String> friends = new ArrayList<>();
                        profile.put("name", user.getDisplayName());
                        profile.put("email", user.getEmail());
                        profile.put("friends", friends);

                        db.collection("UserProfiles").document(user.getEmail()).set(profile);
                    }
                }


            }
        });
    }



    /**
     * Manipulates the map when it's available.
     * The API invokes this callback when the map is ready for use.
     */
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        //Todo: Get Current Location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            Log.d("CurrentLocationLat", String.valueOf(currentLocation.latitude));
                            Log.d("CurrentLocationLong", String.valueOf(currentLocation.longitude));
                            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 20));
                        } else {

                            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
                        }
                    }
                });

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        switch (menuItem.getItemId()) {
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

                    //Launch log in activity
                    List<AuthUI.IdpConfig> providers = Arrays.asList(
                            new AuthUI.IdpConfig.EmailBuilder().build());

                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setLogo(R.drawable.logo1x)
                                    .setAvailableProviders(providers)
                                    .build(),
                            RC_SIGN_IN);
                }
                break;
            case R.id.change_picture:
                //Checks to see if a user is logged in
                if (user != null) {
                    Intent gallery =
                            new Intent(Intent.ACTION_OPEN_DOCUMENT,
                                    android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                    startActivityForResult(gallery, PICK_IMAGE);
                    //https://scontent.fbtv1-1.fna.fbcdn.net/v/t31.0-8/s960x960/10679591_648893885224134_7166029734996188708_o.jpg?_nc_cat=110&_nc_sid=da1649&_nc_ohc=ioKCYq4xVOUAX9q5rm4&_nc_ht=scontent.fbtv1-1.fna&_nc_tp=7&oh=f4fc4628684bad08b6e2f8c41890816f&oe=5EA64EB2
                } else {
                    // No user is signed in
                }
                break;
            case R.id.del_acct:
                //Firebase Listener to Delete account
                user.delete()
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d("Deleted", "User account deleted.");
                                    //Launch log in activity
                                    List<AuthUI.IdpConfig> providers = Arrays.asList(
                                            new AuthUI.IdpConfig.EmailBuilder().build());
                                    startActivityForResult(
                                            AuthUI.getInstance()
                                                    .createSignInIntentBuilder()
                                                    .setLogo(R.drawable.logo1x)
                                                    .setAvailableProviders(providers)
                                                    .build(),
                                            RC_SIGN_IN);
                                }
                            }
                        });

                break;
            case R.id.requests:
                Intent launchReqs = new Intent(this, RequestsActivity.class);
                startActivity(launchReqs);
                break;
            case R.id.friends:
                Intent launchFriends = new Intent(this, FriendsListActivity.class);
                startActivity(launchFriends);
                break;

            default:
                Toast.makeText(FriendsSheetActivity.this, "Default", Toast.LENGTH_SHORT).show();
                break;
        }
        return false;
    }

    public void getCurrentLocation(){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        }
                    }
                });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();


            Log.d("PHOTO!!", data.getDataString());
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setPhotoUri(Uri.parse(data.getDataString()))
                    .build();

            //Updates user's photo with UpdataProfile
            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d("Updated", "User profile updated.");
                                floatingActionButton = findViewById(R.id.floatingActionButton);
                                prof_picture = findViewById(R.id.profilePic);
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                Log.d("PhotoURL2", user.getPhotoUrl().toString());
                                Picasso.get().load(user.getPhotoUrl()).transform(new CircleTransform()).into(prof_picture);
                                Picasso.get().load(user.getPhotoUrl()).transform(new CircleTransform()).into(floatingActionButton);

                                Map<String, Object> profile = new HashMap<>();
                                profile.put("photoURL", user.getPhotoUrl().toString());

                                db.collection("UserProfiles").document(user.getEmail()).update(profile);
                            }

                        }
                    });
        }
        //Sign in Results

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {

                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                floatingActionButton = findViewById(R.id.floatingActionButton);

                //Checks to see if user's email is Verified.
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
                String tempPhotoURL;

                //Pulls from Database if there is a photo, otherwise sets to null white profile
                if(user.getPhotoUrl() == null) {
                    floatingActionButton = findViewById(R.id.floatingActionButton);
                    floatingActionButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_person_white_24dp));
                } else {
                    Log.d("PHOTOURL", user.getPhotoUrl().toString());
                    Picasso.get().load(user.getPhotoUrl()).transform(new CircleTransform()).into(prof_picture);
                    Picasso.get().load(user.getPhotoUrl()).transform(new CircleTransform()).into(floatingActionButton);
                }


                //Update UserProfiles
                Map<String, Object> profile = new HashMap<>();
                ArrayList<String> friends = new ArrayList<>();
                profile.put("name", user.getDisplayName());
                profile.put("email", user.getEmail());

                db.collection("UserProfiles").document(user.getEmail()).update(profile);

            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }

    private class FriendHolder extends RecyclerView.ViewHolder {
        private ImageView iv;
        private TextView title;
        private TextView cat;
        private TextView add;
        private Button but;

        public FriendHolder (@NonNull View itemView) {
            super(itemView);

        }

        public void bind(User friend) {

            //Binds the bottom view
            //Picasso.get().load("https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=" + place.getmIconURL() + "&key=AIzaSyACLyHMHhi7tsD7JRHAD4zubgFVZ7TepQQ").into(iv);
            //title.setText(user.getmName());
            //add.setText(place.getmAddress());
            //cat.setText(place.getmCategory());
            /*but.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Taken From https://developers.google.com/maps/documentation/urls/android-intents

                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" + temp.latitude + "," + temp.longitude);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                }
            });*/
        }
    }

    private class FriendAdapter extends RecyclerView.Adapter<FriendHolder> {
        @NonNull
        @Override
        public FriendHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater li = LayoutInflater.from(getApplicationContext());
            return new FriendHolder(li.inflate(R.layout.friends_sheet_cell, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull FriendHolder holder, int position) {
            holder.bind(mFriends.get(position));
        }

        @Override
        public int getItemCount() {
            return mFriends.size();
        }
    }
    //https://gist.github.com/julianshen/5829333
    //Apache License
    public static class CircleTransform implements Transformation {
        @Override
        public Bitmap transform(Bitmap source) {
            int size = Math.min(source.getWidth(), source.getHeight());

            int x = (source.getWidth() - size) / 3;
            int y = (source.getHeight() - size) / 3;

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

            float r = size/2;
            canvas.drawCircle(r, r, r, paint);

            squaredBitmap.recycle();
            return bitmap;
        }

        @Override
        public String key() {
            return "circle";
        }
    }

}
