package com.utkarsh.CampusKe;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;

public class SuccessActivity extends AppCompatActivity {
    boolean isLikedClicked=false;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        LottieAnimationView lottieAnimationView = findViewById(R.id.lottieAnimationView);
        lottieAnimationView.playAnimation();
        closeApp();
        soundEffect();

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


}
