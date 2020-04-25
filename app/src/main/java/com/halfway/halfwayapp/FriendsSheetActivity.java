package com.halfway.halfwayapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import de.hdodenhof.circleimageview.CircleImageView;



public class FriendsSheetActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {
    //Layout Initializers
    private RecyclerView mFriendSheetRecycler;
    private FriendAdapter mFriendSheetAdapter;
    private FloatingActionButton floatingActionButton;
    private ArrayList<User> mFriends;
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
    private static final int RC_SIGN_IN = 123;
    private static final int PICK_IMAGE = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_sheet);

        //Initialize Layout
        drawerLayout = findViewById(R.id.drawer);
        navigationView = findViewById(R.id.navigationView);
        floatingActionButton = findViewById(R.id.floatingActionButton);
        navigationView.bringToFront();
        navigationView.setNavigationItemSelectedListener(FriendsSheetActivity.this);
        searchView = findViewById(R.id.friends_search);

        //Set Search View
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //True adds friend, false removes friend
                addFriend(s, true);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        // Get the SupportMapFragment and register for the callback
        // when the map is ready for use.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Set up bottom sheet of Friends
        mFriendSheetAdapter = new FriendAdapter();
        mFriendSheetRecycler = findViewById(R.id.friends_sheet_recycler);
        mFriendSheetRecycler.setLayoutManager(new LinearLayoutManager(this));

        //Add Swipe to delete friend gesture
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                //Remove friend from list
                String email = mFriends.remove(viewHolder.getAdapterPosition()).getEmail();
                addFriend(email, false);
                Toast.makeText(getApplicationContext(), "Friend Deleted", Toast.LENGTH_SHORT).show();
                mFriendSheetAdapter.notifyDataSetChanged();
            }
        };
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mFriendSheetRecycler);
        mFriendSheetRecycler.setAdapter(mFriendSheetAdapter);

        //Add decor
        DividerItemDecoration itemDecor = new DividerItemDecoration(getBaseContext(), DividerItemDecoration.VERTICAL);
        mFriendSheetRecycler.addItemDecoration(itemDecor);

        mFriends = new ArrayList<User>();

        // Firebase Setup
        db = FirebaseFirestore.getInstance();

        //Checks to see if there is a current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        //Start Loading info if user is logged in
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
            //Keeps Database up to date with user profile images and names
            updateUserProfile();
            //Fetch friends for Recycler
            fetchFriends();

        } else {
            //No user Logged in
            launchLogin();
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
                            Picasso.get().load(uri).transform(new CircleTransform()).into(prof_picture);
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
        //Update UserProfiles to be consistant with firebase auth

        db.collection("UserProfiles").document(user.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Map<String, Object> profile = new HashMap<>();
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

        //Looks into the current Users Profile
        db.collection("UserProfiles").document(user.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        //If successful and document exists pull the friends arrayList
                        ArrayList<String> temp = (ArrayList<String>) document.get("friends");
                        final ArrayList tempUsers = new ArrayList();

                        //Loops through each friend's information
                        if(temp != null) {
                            for (int i = 0; i < temp.size(); i++) {
                                db.collection("UserProfiles").document(temp.get(i)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot document = task.getResult();
                                            if (document.exists()) {
                                                //If the Friend is found, update the user Information in the friends adapter
                                                User temp = new User(document.get("email").toString(), document.get("name").toString());
                                                tempUsers.add(temp);
                                            } else {
                                                //No Exists
                                            }
                                        }
                                        //Update the Friends adapter
                                        mFriends = tempUsers;
                                        mFriendSheetAdapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                    }
                } else {
                    Log.d("No Friends", ":(");
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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mMap.setMyLocationEnabled(true);
        //Get Current Location for Users
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        } else {
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(0,0)));
                            Toast.makeText(getApplicationContext(), "Current Location not Found", Toast.LENGTH_LONG).show();
                        }
                    }
                });

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
                Toast.makeText(FriendsSheetActivity.this, "Default", Toast.LENGTH_SHORT).show();
                break;
        }
        return false;
    }
    //True == addFriend False == Remove Friend
    private void addFriend(final String email, final boolean addFriend) {

        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        db.collection("UserProfiles").document(email).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        //If Username Exists
                        if(addFriend) {
                            if(email.equals(user.getEmail())) {
                                Toast.makeText(FriendsSheetActivity.this, "You cannot friend yourself :(", Toast.LENGTH_SHORT).show();
                                //Close search View
                                searchView.clearFocus();
                                searchView.setQuery("", false);
                                searchView.setIconified(true);
                            } else {
                                //Add Friend to both profiles
                                db.collection("UserProfiles").document(user.getEmail()).update("friends", FieldValue.arrayUnion(email));
                                db.collection("UserProfiles").document(email).update("friends", FieldValue.arrayUnion(user.getEmail()));
                                //Close search View
                                searchView.clearFocus();
                                searchView.setQuery("", false);
                                searchView.setIconified(true);
                                //Notify User and Updata friend recycler
                                Toast.makeText(FriendsSheetActivity.this, "Friend Added!", Toast.LENGTH_SHORT).show();
                                fetchFriends();
                            }
                        } else {
                            //Remove references from both profiles
                            db.collection("UserProfiles").document(user.getEmail()).update("friends", FieldValue.arrayRemove(email));
                            db.collection("UserProfiles").document(email).update("friends", FieldValue.arrayRemove(user.getEmail()));
                            fetchFriends();
                        }
                    } else {
                        //If the username doesn't exist, notify user via toast
                        Toast.makeText(FriendsSheetActivity.this, "User does not exist", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
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
            Picasso.get().load(data.getData()).transform(new CircleTransform()).into(prof_picture);
            Picasso.get().load(data.getData()).transform(new CircleTransform()).into(floatingActionButton);
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
                            Picasso.get().load(uri).transform(new CircleTransform()).into(floatingActionButton);
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
            //Get Use's Display name
            displayName.setText(friend.getDisplayName());

            //Attempt to load image
            StorageReference storageReference = FirebaseStorage.getInstance().getReference("profilePictures/" + friend.getEmail());
            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Picasso.get().load(uri).transform(new CircleTransform()).into(iv);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    iv.setImageDrawable(getResources().getDrawable(R.drawable.ic_person_black_24dp));
                }
            });
            //Sets up Request Logic
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
                                        //Gets Current users info and places it in an empty request
                                        HashMap<String, Object> requestInfo = new HashMap<>();
                                        requestInfo.put("primaryUserLocation", location.getLatitude() + "," + location.getLongitude());
                                        requestInfo.put("primaryUserEmail", user.getEmail());
                                        requestInfo.put("secondaryUserLocation", null);
                                        requestInfo.put("secondaryUserEmail", friend.getEmail());
                                        requestInfo.put("midpoint", "");
                                        requestInfo.put("timestamp", FieldValue.serverTimestamp());

                                        //Creates the request
                                        DocumentReference request = db.collection("Requests").document();
                                        requestInfo.put("docID", request.getPath());
                                        request.set(requestInfo);

                                        //Puts the request in both user's request list
                                        db.collection("UserProfiles").document(friend.getEmail()).update("requests", FieldValue.arrayUnion(request.getPath()));
                                        db.collection("UserProfiles").document(user.getEmail()).update("requests", FieldValue.arrayUnion(request.getPath()));

                                        //Launches RequestsActivity
                                        Intent launchReqs = new Intent(getApplicationContext(), RequestsActivity.class);
                                        startActivity(launchReqs);
                                    }
                                }
                            });
                }
            });
        }
        //Sets On Swipe Listener
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                String email = mFriends.remove(viewHolder.getAdapterPosition()).getEmail();
                addFriend(email, false);

                mFriendSheetAdapter.notifyDataSetChanged();
            }
        };
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


        //OnSwipeListener
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                String email = mFriends.remove(viewHolder.getAdapterPosition()).getEmail();
                addFriend(email, false);
                mFriendSheetAdapter.notifyDataSetChanged();
            }
        };

    }

    //https://gist.github.com/julianshen/5829333
    //Apache License
    //Turns an image into a circle for Flaoting action buttons
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
