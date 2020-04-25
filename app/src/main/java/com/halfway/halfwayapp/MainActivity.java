package com.halfway.halfwayapp;


import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import android.widget.Toast;
import com.google.android.material.navigation.NavigationView;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private RecyclerView mPlaceRecycler;
    private PlaceAdapter mPlaceAdapter;
    private FloatingActionButton floatingActionButton;
    private ArrayList<PlaceCard> mPlaces;
    private OkHttpClient mHTTPClient;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView prof_email;
    private TextView prof_display_name;
    private CircleImageView prof_picture;
    private FirebaseFirestore db;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    private GoogleMap mMap;
    private static final int RC_SIGN_IN = 123;
    private static final int PICK_IMAGE = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Updates Left Drawer
        drawerLayout = findViewById(R.id.drawer);
        navigationView = findViewById(R.id.navigationView);
        floatingActionButton = findViewById(R.id.floatingActionButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        navigationView.bringToFront();
        navigationView.setNavigationItemSelectedListener(MainActivity.this);

        db = FirebaseFirestore.getInstance();

        // Get the SupportMapFragment and register for the callback
        // when the map is ready for use.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mPlaceAdapter = new PlaceAdapter();
        mPlaceRecycler = findViewById(R.id.places_recycler);
        mPlaceRecycler.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration itemDecor = new DividerItemDecoration(getBaseContext(), DividerItemDecoration.VERTICAL);
        mPlaceRecycler.addItemDecoration(itemDecor);
        mPlaces = new ArrayList<PlaceCard>();
        mHTTPClient = new OkHttpClient();

        //Fills Recycler Views
        if(getIntent().getStringExtra("latitude").equals("null")) {
            Toast.makeText(getApplicationContext(), "The other user has not responded to your request", Toast.LENGTH_LONG).show();
        } else {
            mPlaceRecycler.setAdapter(mPlaceAdapter);
            refresh();
        }

        // Firebase Setup
        db = FirebaseFirestore.getInstance();
        //Checks to see if there is a current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // Grabs profile picture
            floatingActionButton = findViewById(R.id.floatingActionButton);
            prof_picture = findViewById(R.id.profilePic);
            StorageReference storageReference = FirebaseStorage.getInstance().getReference("profilePictures/" +user.getEmail());
            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    if(uri != null) {
                        Picasso.get().load(uri).transform(new FriendsSheetActivity.CircleTransform()).into(floatingActionButton);
                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    floatingActionButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_person_white_24dp));
                }
            });;
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

        //Set up Drawer open on FAB hit
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

                    StorageReference storageReference = FirebaseStorage.getInstance().getReference("profilePictures/" + user.getEmail());
                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Picasso.get().load(uri).transform(new FriendsSheetActivity.CircleTransform()).into(prof_picture);
                        }
                    });
                }
            }
        });
        //Close Drawer for onCreate
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

        mMap.setMyLocationEnabled(true);
        LatLng zoomLocation;
        if(!getIntent().getStringExtra("latitude").equals("null")) {
            zoomLocation = new LatLng(Double.valueOf(getIntent().getStringExtra("latitude")), Double.valueOf(getIntent().getStringExtra("longitude")));

            mMap.addMarker(new MarkerOptions().position(zoomLocation).anchor(.5f, .5f).title("Midpoint"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(zoomLocation, 15));
        } else {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()), 15));
                            }
                        }
                    });
        }
    }

    //Set up Menu
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        //If there is no user launch login
        if(user == null) {
            //Launch log in activity
            launchLogin();
        }

        switch (menuItem.getItemId()) {
            case R.id.log_out:
                //Signout from AuthUI
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                // ...
                            }
                        });
                launchLogin();
                break;

            case R.id.change_picture:
                //Launch gallery Intent
                Intent gallery =
                        new Intent(Intent.ACTION_GET_CONTENT, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                gallery.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(gallery, PICK_IMAGE);
                //https://scontent.fbtv1-1.fna.fbcdn.net/v/t31.0-8/s960x960/10679591_648893885224134_7166029734996188708_o.jpg?_nc_cat=110&_nc_sid=da1649&_nc_ohc=ioKCYq4xVOUAX9q5rm4&_nc_ht=scontent.fbtv1-1.fna&_nc_tp=7&oh=f4fc4628684bad08b6e2f8c41890816f&oe=5EA64EB2
                break;
            case R.id.del_acct:
                // Get auth credentials from the user for re-authentication. The example below shows
                // email and password credentials but there are multiple possible providers,
                // such as GoogleAuthProvider or FacebookAuthProvider.
                AuthCredential credential = EmailAuthProvider
                        .getCredential("user@example.com", "password1234");

                // Prompt the user to re-provide their sign-in credentials
                user.reauthenticate(credential)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                //Firebase Listener to Delete account
                                db.collection("UserProfiles").document(user.getEmail()).get().addOnSuccessListener(
                                        new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                if (documentSnapshot.exists()) {
                                                    //Get List of Friends
                                                    ArrayList<String> friends = (ArrayList) documentSnapshot.get("friends");
                                                    if (friends != null) {
                                                        for (int i = 0; i < friends.size(); i++) {
                                                            //Remove all references to you as a friend on others profiles
                                                            db.collection("UserProfiles").document(friends.get(i)).update("friends", FieldValue.arrayRemove(documentSnapshot.get("email")));
                                                        }
                                                    }
                                                    //Gets all requests
                                                    ArrayList<String> requests = (ArrayList) documentSnapshot.get("requests");
                                                    if (requests != null) {
                                                        for (int i = 0; i < requests.size(); i++) {
                                                            //Removes all requests
                                                            db.document(requests.get(i)).delete();
                                                        }
                                                    }
                                                    //Deletes the user's profile picture
                                                    FirebaseStorage.getInstance().getReference("profilePictures/" + user.getEmail()).delete();
                                                    //Deletes the user profile
                                                    db.collection("UserProfiles").document(user.getEmail()).delete();
                                                    //Deletes the FirebaseAuth Account
                                                    user.delete()
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        Log.d("Deleted", "User account deleted.");
                                                                        launchLogin();
                                                                    }
                                                                }
                                                            });
                                                }
                                            }
                                        });
                            }
                        });
                launchLogin();
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
                Toast.makeText(MainActivity.this, "Default", Toast.LENGTH_SHORT).show();
                break;
        }
        return false;
    }

    public void launchLogin() {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //User Chooses new Image
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            FirebaseStorage storageRef = FirebaseStorage.getInstance();
            //Upload photoRef to profile
            StorageReference photoRef = storageRef.getReference("profilePictures/" + user.getEmail());
            photoRef.putFile(data.getData());
            //Load new Profile Picture
            Picasso.get().load(data.getData()).transform(new FriendsSheetActivity.CircleTransform()).into(prof_picture);
            Picasso.get().load(data.getData()).transform(new FriendsSheetActivity.CircleTransform()).into(floatingActionButton);
        }
        //Sign in Results
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                //User Sign in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                //Checks to see if user's email is Verified.
                if (!user.isEmailVerified()) {
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

                floatingActionButton = findViewById(R.id.floatingActionButton);
                StorageReference storageReference = FirebaseStorage.getInstance().getReference("profilePictures/" + user.getEmail());
                storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Picasso.get().load(uri).transform(new FriendsSheetActivity.CircleTransform()).into(floatingActionButton);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        floatingActionButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_person_white_24dp));
                    }
                });

                updateUserProfile();
                drawerLayout.closeDrawers();

            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }


    private class RefreshTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {

            Request request = new Request.Builder()
                    .url("https://maps.googleapis.com/maps/api/place/textsearch/json?type=restaurant&location="+ getIntent().getStringExtra("latitude") + "," + getIntent().getStringExtra("longitude") +"&key=AIzaSyACLyHMHhi7tsD7JRHAD4zubgFVZ7TepQQ")
                    .build();
            Log.d("Request", "https://maps.googleapis.com/maps/api/place/textsearch/json?type=restaurant&location="+ getIntent().getStringExtra("latitude") + "," + getIntent().getStringExtra("longitude") +"&key=AIzaSyACLyHMHhi7tsD7JRHAD4zubgFVZ7TepQQ");
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
        String id, name, address, iconURL;
        PlaceCard temp;
        JSONArray category;
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
                categoryInfo = categoryInfo.substring(0,1).toUpperCase() + categoryInfo.substring(1);

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

            //Binds the bottom view
            Picasso.get().load("https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=" + place.getmIconURL() + "&key=AIzaSyACLyHMHhi7tsD7JRHAD4zubgFVZ7TepQQ").into(iv);
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
