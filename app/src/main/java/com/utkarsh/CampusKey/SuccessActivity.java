package com.utkarsh.CampusKey;

import static com.utkarsh.CampusKey.LocalData.DESCRIPTION;
import static com.utkarsh.CampusKey.LocalData.FORCE;
import static com.utkarsh.CampusKey.LocalData.LAST_CHECK_TIME;
import static com.utkarsh.CampusKey.LocalData.MAIN_TEXT;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.Firebase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static com.utkarsh.CampusKey.LocalData.MINIMUM_VERSION_CODE;
import static com.utkarsh.CampusKey.LocalData.URL;

public class SuccessActivity extends AppCompatActivity {
    boolean isLikedClicked=false;
    private MediaPlayer mediaPlayer;
    // Reference to the Firebase Realtime Database
    private DatabaseReference databaseReference;
    LocalData data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        LottieAnimationView lottieAnimationView = findViewById(R.id.lottieAnimationView);
        lottieAnimationView.playAnimation();
        closeApp();
        soundEffect();

         data = new LocalData(SuccessActivity.this);


checkAndFetchData();

        // Instagram Link
        ImageView imageViewInstagram = findViewById(R.id.imageViewInstagram);
        imageViewInstagram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl("https://www.instagram.com/mr._utkarsh_sahu/"); // Replace with your Instagram link
            }
        });

        // LinkedIn Link
        ImageView imageViewLinkedIn = findViewById(R.id.imageViewLinkedIn);
        imageViewLinkedIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl("https://www.linkedin.com/in/utkarshsahu9906/"); // Replace with your LinkedIn link
            }
        });
    }

    private void openUrl(String url) {
        isLikedClicked=true;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    void closeApp(){

            // Close the app completely after 5 seconds
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isLikedClicked) {
                        finishAffinity(); // Closes all activities in the app
                        // or System.exit(0); // Forcefully exits the app
                    }
                }
            }, 5000);

    }

    void soundEffect(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mediaPlayer = MediaPlayer.create(SuccessActivity.this, R.raw.done_sound); // Replace 'success_sound' with your file name

                // Play the sound
                if (mediaPlayer != null) {
                    mediaPlayer.start();
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            mp.release(); // Release the MediaPlayer resources
                        }
                    });
                }

            }
        },1000);
    }
    void fetchDataFormFirebase() {
        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Replace "users" with the path you want to retrieve data from
        databaseReference.child("CampusKey")// Replace "userID" with the actual key or path
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Extract data from the DataSnapshot object

                            data.save(URL,dataSnapshot.child("URL").getValue(String.class));
                            data.save(MAIN_TEXT,dataSnapshot.child("MAIN_TEXT").getValue(String.class));
                            data.save(DESCRIPTION,dataSnapshot.child("DESCRIPTION").getValue(String.class));
                            data.save(MINIMUM_VERSION_CODE,dataSnapshot.child("MINIMUM_VERSION_CODE").getValue(Integer.class));
                            data.save(FORCE,dataSnapshot.child("FORCE").getValue(Boolean.class));


                        } else {
                            Log.d("FirebaseData", "No such data exists");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle possible errors
                        Log.d("FirebaseData", "Fetch failed: " + databaseError.getMessage());
                    }
                });
    }

    void checkAndFetchData() {
        long lastCheckTime = data.getLong(LAST_CHECK_TIME); // Retrieve the last check time
        long currentTime = System.currentTimeMillis(); // Get the current time

        // Check if more than one day has passed since the last check
        if (lastCheckTime + 86400000 < currentTime) {
            fetchDataFormFirebase(); // Fetch data from Firebase
            data.save(LAST_CHECK_TIME, currentTime); // Save the current time as the last check time
        }
    }


}
