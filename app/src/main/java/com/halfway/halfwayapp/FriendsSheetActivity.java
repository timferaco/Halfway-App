package com.halfway.halfwayapp;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
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
    SearchView searchView;
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
        searchView = findViewById(R.id.friends_search);

        createNotificationChannel();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                addFriend(s);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });


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
            prof_picture = findViewById(R.id.profilePic);
            StorageReference storageReference = FirebaseStorage.getInstance().getReference("profilePictures/" + user.getEmail());
            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    if (uri != null) {
                        Picasso.get().load(uri).transform(new CircleTransform()).into(floatingActionButton);
                    }

                }
            });

            updateUserProfile();
            fetchFriends();

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


                    StorageReference storageReference = FirebaseStorage.getInstance().getReference("profilePictures/" + user.getEmail());
                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Picasso.get().load(uri).transform(new CircleTransform()).into(prof_picture);
                            Picasso.get().load(uri).transform(new CircleTransform()).into(floatingActionButton);
                        }
                    });
                }
            }
        });
        drawerLayout.closeDrawers();
        /*
        final DocumentReference docRef = db.collection("UserProfiles").document(user.getEmail());
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    //Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d("FRIEND ADDED", "Current data: " + snapshot.getData());
                    Log.d("FRIEND ADDED", "PLS");
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "CHANNELID").setSmallIcon(R.id.icon_only)
                        .setContentTitle("My notification")
                        .setContentText("Much longer text that cannot fit one line...")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText("Much longer text that cannot fit one line..."))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

                    // notificationId is a unique int for each notification that you must define
                    notificationManager.notify(213213, builder.build());



                } else {
                    //Log.d(TAG, "Current data: null");
                }
            }
        });
        */







    }
    //https://developer.android.com/training/notify-user/build-notification#java
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "CHANNELID";
            String description = "description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("CHANNELID", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
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

    private void fetchFriends() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        db.collection("UserProfiles").document(user.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        ArrayList<String> temp = new ArrayList<>();

                        temp = (ArrayList<String>) document.get("friends");
                        final ArrayList tempUsers = new ArrayList();
                        for (int i = 0; i < temp.size(); i++) {

                            db.collection("UserProfiles").document(temp.get(i)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();

                                        if (document.exists()) {
                                            User temp = new User(document.get("email").toString(), document.get("name").toString());
                                            tempUsers.add(temp);
                                            Log.d("!!!Temp", String.valueOf(tempUsers.size()));
                                        } else {
                                            //No Exists
                                        }
                                    }
                                    mFriends = tempUsers;
                                    mFriendSheetAdapter.notifyDataSetChanged();
                                }
                            });

                        }
                    }

                } else {
                    //Document Doesn't Exist
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
        mMap.setMyLocationEnabled(true);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            Log.d("CurrentLocationLat", String.valueOf(currentLocation.latitude));
                            Log.d("CurrentLocationLong", String.valueOf(currentLocation.longitude));
                            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        } else {
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
                        }
                    }
                });

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
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
                            new Intent(Intent.ACTION_GET_CONTENT, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                    gallery.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startActivityForResult(gallery, PICK_IMAGE);
                    //https://scontent.fbtv1-1.fna.fbcdn.net/v/t31.0-8/s960x960/10679591_648893885224134_7166029734996188708_o.jpg?_nc_cat=110&_nc_sid=da1649&_nc_ohc=ioKCYq4xVOUAX9q5rm4&_nc_ht=scontent.fbtv1-1.fna&_nc_tp=7&oh=f4fc4628684bad08b6e2f8c41890816f&oe=5EA64EB2
                } else {
                    // No user is signed in
                }
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
                                Log.d("!!!ONCOMPLETE", "naw");
                                //Firebase Listener to Delete account
                                db.collection("UserProfiles").document(user.getEmail()).get().addOnSuccessListener(
                                        new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                if (documentSnapshot.exists()) {
                                                    ArrayList<String> friends = (ArrayList) documentSnapshot.get("friends");
                                                    Log.d("!!!ONCOMPLETEFRIENDS ", String.valueOf(friends.size()));
                                                    if (friends != null) {
                                                        for (int i = 0; i < friends.size(); i++) {
                                                            Log.d("!!!ONCOMPLETEFRIEND!!", friends.get(i).toString());
                                                            db.collection("UserProfiles").document(friends.get(i)).update("friends", FieldValue.arrayRemove(documentSnapshot.get("email")));
                                                        }
                                                    }
                                                    ArrayList<String> requests = (ArrayList) documentSnapshot.get("requests");
                                                    if (requests != null) {
                                                        for (int i = 0; i < requests.size(); i++) {
                                                            db.document(requests.get(i)).delete();
                                                        }
                                                    }
                                                    FirebaseStorage.getInstance().getReference("profilePictures/" + user.getEmail()).delete();
                                                    db.collection("UserProfiles").document(user.getEmail()).delete();
                                                    //Firebase Listener to Delete account
                                                    Log.d("USERRR", user.getEmail());
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
                                                }
                                            }
                                        });
                            }
                        });
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.EmailBuilder().build());
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setLogo(R.drawable.logo1x)
                                .setAvailableProviders(providers)
                                .build(),
                        RC_SIGN_IN);
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

    public void getCurrentLocation() {
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

    //https://www.youtube.com/watch?v=6u0gzjth4IE
    private String getExtension(Uri uri) {
        ContentResolver cr = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(cr.getType(uri));
    }

    private void addFriend(final String email) {
//email is in the "set"
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        db.collection("UserProfiles").document(email).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    //"friends", FieldValue.arrayUnion(email))
                    if (document.exists()) {
                        db.collection("UserProfiles").document(user.getEmail()).update("friends", FieldValue.arrayUnion(email));
                        db.collection("UserProfiles").document(email).update("friends", FieldValue.arrayUnion(user.getEmail()));
                        searchView.clearFocus();
                        Toast.makeText(FriendsSheetActivity.this, "Friend Added!", Toast.LENGTH_SHORT).show();
                        searchView.setQuery("", false);
                        searchView.setIconified(true);
                        fetchFriends();

                    } else {
                        Toast.makeText(FriendsSheetActivity.this, "User does not exist", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            FirebaseStorage storageRef = FirebaseStorage.getInstance();

            StorageReference photoRef = storageRef.getReference("profilePictures/" + user.getEmail());
            photoRef.putFile(data.getData());
            Picasso.get().load(data.getData()).transform(new CircleTransform()).into(prof_picture);
            Picasso.get().load(data.getData()).transform(new CircleTransform()).into(floatingActionButton);
        }

//Sign in Results

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {

                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                floatingActionButton = findViewById(R.id.floatingActionButton);
                updateUserProfile();

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
                String tempPhotoURL;

                //Pulls from Database if there is a photo, otherwise sets to null white profile
                if (user.getPhotoUrl() == null) {
                    floatingActionButton = findViewById(R.id.floatingActionButton);
                    floatingActionButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_person_white_24dp));
                } else {
                    StorageReference storageReference = FirebaseStorage.getInstance().getReference("profilePictures/" + user.getEmail());
                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Picasso.get().load(uri).transform(new CircleTransform()).into(floatingActionButton);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            floatingActionButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_send_black_24dp));
                        }
                    });
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
        private TextView displayName;
        private Button requestButton;

        public FriendHolder(@NonNull View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.friend_img);
            displayName = itemView.findViewById(R.id.friend_email);
            requestButton = itemView.findViewById(R.id.request_button);
        }

        public void bind(final User friend) {
            Log.d("D!!", friend.getEmail());
            displayName.setText(friend.getDisplayName());

            StorageReference storageReference = FirebaseStorage.getInstance().getReference("profilePictures/" + friend.getEmail());
            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Log.d("SUCCESS", uri.toString());
                    Picasso.get().load(uri).transform(new CircleTransform()).into(iv);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("FAIL", "");
                    iv.setImageDrawable(getResources().getDrawable(R.drawable.ic_send_black_24dp));
                }
            });
            ;
            //Picasso.get().load(friend.getPhotoURL()).into(iv);


            requestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();


                    fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    /* Got last known location. In some rare situations this can be null. */
                                    if (location != null) {
                                        HashMap<String, Object> requestInfo = new HashMap<>();
                                        requestInfo.put("primaryUserLocation", new GeoPoint(location.getLatitude(), location.getLongitude()));
                                        requestInfo.put("primaryUserEmail", user.getEmail());
                                        requestInfo.put("secondaryUserLocation", null);
                                        requestInfo.put("secondaryUserEmail", friend.getEmail());
                                        requestInfo.put("midpoint", null);
                                        requestInfo.put("timestamp", FieldValue.serverTimestamp());

                                        DocumentReference request = db.collection("Requests").document();
                                        requestInfo.put("docID", request.getPath());
                                        request.set(requestInfo);

                                        db.collection("UserProfiles").document(friend.getEmail()).update("requests", FieldValue.arrayUnion(request.getPath()));
                                        db.collection("UserProfiles").document(user.getEmail()).update("requests", FieldValue.arrayUnion(request.getPath()));

                                        Intent launchReqs = new Intent(getApplicationContext(), RequestsActivity.class);
                                        startActivity(launchReqs);
                                    }
                                }
                            });


                }
            });
        }


        private void addRequest(final String email) {
            //email is in the "set"
            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            db.collection("UserProfiles").document(email).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        //"friends", FieldValue.arrayUnion(email))
                        if (document.exists()) {
                            db.collection("UserProfiles").document(user.getEmail()).update("requests", FieldValue.arrayUnion("DocumentReferenceHere"));
                            db.collection("UserProfiles").document(email).update("friends", FieldValue.arrayUnion(user.getEmail()));
                        } else {
                            Log.d("!!!DOES NOT EXIST!!!", "!!!DOES NOT EXIST!!!");
                        }
                    }
                }
            });


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

            float r = size / 2;
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
