package com.utkarshsahu.CampusKey;


import static com.utkarshsahu.CampusKey.LocalData.DESCRIPTION;
import static com.utkarshsahu.CampusKey.LocalData.FORCE;
import static com.utkarshsahu.CampusKey.LocalData.LAST_CHECK_TIME;
import static com.utkarshsahu.CampusKey.LocalData.MAIN_TEXT;
import static com.utkarshsahu.CampusKey.LocalData.MINIMUM_VERSION_CODE;
import static com.utkarshsahu.CampusKey.LocalData.NETWORK_ERROR;
import static com.utkarshsahu.CampusKey.LocalData.URL;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.utkarshsahu.CampusKey.databinding.ActivitySuccessBinding;

public class SuccessActivity extends AppCompatActivity {

    private boolean userIsEngaged = false;
    private MediaPlayer mediaPlayer;
    private DatabaseReference databaseReference;
    private LocalData data;
    ActivitySuccessBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        data = new LocalData(this);
        checkAndFetchConfig();

        boolean networkError = getIntent().getBooleanExtra(NETWORK_ERROR, false);

        if (networkError) {
            showErrorState();
        } else {
            showSuccessState();
        }

        setupButtons();
    }

    // ── UI states ────────────────────────────────
    private void showSuccessState() {
        binding.networkErrorAnimation.setVisibility(View.GONE);
        binding.retry.setVisibility(View.GONE);
        binding.doneAnimation.setVisibility(View.VISIBLE);
        binding.doneAnimation.playAnimation();
        binding.connectionStatusMessage.setText("Connected to college Wi-Fi!");
        playSound(R.raw.done_sound);

        // Auto-close after 1 second if user isn't tapping anything
        handler.postDelayed(() -> {
            if (!userIsEngaged) finishAffinity();
        }, 1000);
    }

    private void showErrorState() {
        binding.doneAnimation.setVisibility(View.GONE);
        binding.networkErrorAnimation.setVisibility(View.VISIBLE);
        binding.networkErrorAnimation.playAnimation();
        binding.connectionStatusMessage.setText(
                "Could not connect. Please check your Wi-Fi and try again.");
        binding.retry.setVisibility(View.VISIBLE);
        playSound(R.raw.networ_error_sound);
    }

    // ── Buttons ──────────────────────────────────
    private void setupButtons() {
        binding.imageViewInstagram.setOnClickListener(v ->
                openUrl("https://www.instagram.com/mr._utkarsh_sahu/"));

        binding.imageViewLinkedIn.setOnClickListener(v ->
                openUrl("https://www.linkedin.com/in/utkarshsahu9906/"));

        binding.share.setOnClickListener(v -> {
            userIsEngaged = true;
            String shareBody = "Auto-connect to college Wi-Fi! Download CampusKey:\n"
                    + "https://play.google.com/store/apps/details?id=" + getPackageName();
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, "CampusKey App");
            i.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(i, "Share via"));
        });

        binding.retry.setOnClickListener(v -> {
            Toast.makeText(this, "Retrying...", Toast.LENGTH_SHORT).show();
            handler.postDelayed(() -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }, 800);
        });
    }

    private void openUrl(String url) {
        userIsEngaged = true;
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    // ── Sound ────────────────────────────────────
    private void playSound(int resId) {
        try {
            if (mediaPlayer != null) mediaPlayer.release();
            mediaPlayer = MediaPlayer.create(this, resId);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
        } catch (Exception e) {
            Log.e("CampusKey", "Sound error: " + e.getMessage());
        }
    }

    // ── Firebase config fetch (once per day) ─────
    private void checkAndFetchConfig() {
        long lastCheck = data.getLong(LAST_CHECK_TIME);
        long now = System.currentTimeMillis();
        if (lastCheck + 86_400_000L < now) {
            fetchConfigFromFirebase();
            data.save(LAST_CHECK_TIME, now);
        }
    }

    private void fetchConfigFromFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference("campuskey_config");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) return;
                saveIfNotNull(URL,          snap.child("URL").getValue(String.class));
                saveIfNotNull(MAIN_TEXT,    snap.child("MAIN_TEXT").getValue(String.class));
                saveIfNotNull(DESCRIPTION,  snap.child("DESCRIPTION").getValue(String.class));
                Integer minVer = snap.child("MINIMUM_VERSION_CODE").getValue(Integer.class);
                Boolean force  = snap.child("FORCE").getValue(Boolean.class);
                if (minVer != null) data.save(MINIMUM_VERSION_CODE, minVer);
                if (force  != null) data.save(FORCE, force);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("CampusKey", "Config fetch cancelled: " + error.getMessage());
            }
        });
    }

    private void saveIfNotNull(String key, String value) {
        if (value != null) data.save(key, value);
    }

    // ── Lifecycle ────────────────────────────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
        handler.removeCallbacksAndMessages(null);
    }
}
