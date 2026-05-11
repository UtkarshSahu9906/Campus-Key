package com.utkarshsahu.CampusKey;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import com.utkarshsahu.CampusKey.databinding.SaveCredentialsDialogBinding;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ConnectedActivity extends AppCompatActivity {

    private static final String TAG   = "CampusKey";
    private static final String TODAY = new SimpleDateFormat(
        "yyyy-MM-dd", Locale.getDefault()).format(new Date());

    private DatabaseReference   dbRef;
    private ValueEventListener  statsListener;
    private ValueEventListener  profileListener;

    private boolean userIsEngaged = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);

        playSuccessAnimation();
        setupButtons();
        loadRealtimeData();
        applyAnimations();
        autoCloseAfter(6000);
    }

    private void applyAnimations() {
        android.view.animation.Animation scaleBounce = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.scale_bounce);
        android.view.animation.Animation slideUp = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_fade_in);

        View root = findViewById(android.R.id.content);
        root.post(() -> {
            // Lottie Hero (scale)
            findViewById(R.id.doneAnimation).startAnimation(scaleBounce);
            
            // Stats Card
            slideUp.setStartOffset(300);
            ((View) findViewById(R.id.tvUniqueUsers).getParent().getParent()).startAnimation(slideUp);
            
            // Dev Card
            android.view.animation.Animation slideUp2 = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_fade_in);
            slideUp2.setStartOffset(600);
            ((View) findViewById(R.id.imgDev).getParent().getParent()).startAnimation(slideUp2);
            
            // Action Buttons
            android.view.animation.Animation slideUp3 = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_fade_in);
            slideUp3.setStartOffset(900);
            ((View) findViewById(R.id.shareC).getParent()).startAnimation(slideUp3);
        });
    }

    private void playSuccessAnimation() {
        LottieAnimationView anim = findViewById(R.id.doneAnimation);
        if (anim != null) { anim.setVisibility(View.VISIBLE); anim.playAnimation(); }

        TextView tvStatus = findViewById(R.id.tvConnectionStatus);
        if (tvStatus != null) tvStatus.setText("Connected to College Wi-Fi! 🎉");
    }

    // ─────────────────────────────────────────────
    // REAL-TIME FIREBASE DATA
    // ─────────────────────────────────────────────
    private void loadRealtimeData() {
        dbRef = FirebaseDatabase.getInstance().getReference();

        // Stats
        statsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                Long users   = snap.child("campuskey/summary/totalUniqueUsers").getValue(Long.class);
                Long total   = snap.child("campuskey/summary/totalAllTimeLaunches").getValue(Long.class);
                Long todayCt = snap.child("campuskey/daily/" + TODAY + "/totalOpens").getValue(Long.class);

                setText(R.id.tvUniqueUsers,   fmt(users)   + " users");
                setText(R.id.tvTodayCount,    fmt(todayCt) + " today");
                setText(R.id.tvTotalLaunches, fmt(total)   + " total");
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Log.w(TAG, "Stats error: " + e.getMessage());
            }
        };
        dbRef.addValueEventListener(statsListener);

        // Developer profile — loaded from Firebase, updates without app update
        profileListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                String name      = snap.child("name").getValue(String.class);
                String username  = snap.child("username").getValue(String.class);
                String photoUrl  = snap.child("photoUrl").getValue(String.class);
                String instagram = snap.child("instagram").getValue(String.class);
                String linkedin  = snap.child("linkedin").getValue(String.class);
                String email     = snap.child("email").getValue(String.class);

                if (name     != null) setText(R.id.tvDevName,     name);
                if (username != null) setText(R.id.tvDevUsername, "@" + username);

                // Load profile photo via Glide
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    ImageView img = findViewById(R.id.imgDev);
                    if (img != null) {
                        Glide.with(ConnectedActivity.this)
                            .load(photoUrl)
                            .transform(new CircleCrop())
                            .placeholder(R.drawable.man)
                            .error(R.drawable.man)
                            .into(img);
                    }
                }

                // Social button click listeners from Firebase data
                if (instagram != null) {
                    final String ig = instagram;
                    View btn = findViewById(R.id.btnInstagram);
                    if (btn != null) btn.setOnClickListener(v -> openUrl(ig));
                }
                if (linkedin != null) {
                    final String li = linkedin;
                    View btn = findViewById(R.id.btnLinkedIn);
                    if (btn != null) btn.setOnClickListener(v -> openUrl(li));
                }
                if (email != null) {
                    final String em = email;
                    View btn = findViewById(R.id.btnMail);
                    if (btn != null) btn.setOnClickListener(v -> {
                        Intent i = new Intent(Intent.ACTION_SENDTO);
                        i.setData(Uri.parse("mailto:" + em));
                        i.putExtra(Intent.EXTRA_SUBJECT, "CampusKey Feedback");
                        startActivity(i);
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Log.w(TAG, "Profile error: " + e.getMessage());
            }
        };
        dbRef.child("campuskey_config/developer").addValueEventListener(profileListener);
    }

    private String fmt(Long v) { return v != null ? String.valueOf(v) : "—"; }

    private void setText(int id, String text) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(text);
    }

    // ─────────────────────────────────────────────
    // BUTTONS
    // ─────────────────────────────────────────────
    private void setupButtons() {
        // Edit Credentials
        View editBtn = findViewById(R.id.btnEditCredentials);
        if (editBtn != null) {
            editBtn.setOnClickListener(v -> {
                userIsEngaged = true;
                showEditCredentialsDialog();
            });
        }

        // Share button
        View shareBtn = findViewById(R.id.shareC);
        if (shareBtn != null) {
            shareBtn.setOnClickListener(v -> {
                userIsEngaged = true;
                AppUrlFetcher.fetch(
                        shareUrl -> {
                            Intent i = new Intent(Intent.ACTION_SEND);
                            i.setType("text/plain");
                            i.putExtra(Intent.EXTRA_TEXT, "Download CampusKey:\n" + shareUrl);
                            startActivity(Intent.createChooser(i, "Share via"));
                        },
                        () -> Toast.makeText(this, "Couldn't fetch link", Toast.LENGTH_SHORT).show()
                );
            });
        }

        // Social Buttons
        View ig = findViewById(R.id.imageViewInstagram);
        View li = findViewById(R.id.imageViewLinkedIn);
        
        if (ig != null) ig.setOnClickListener(v -> openUrl("https://instagram.com/utkarshsahu9906"));
        if (li != null) li.setOnClickListener(v -> openUrl("https://linkedin.com/in/utkarshsahu9906"));

        // Developer Card Click
        View devCard = findViewById(R.id.imgDev);
        if (devCard != null) devCard.setOnClickListener(v -> startActivity(new Intent(this, Info.class)));
    }

    private void showEditCredentialsDialog() {
        DatabaseHelper db = new DatabaseHelper(this);
        Cursor cursor = db.getUser();
        String currentUname = "";
        String currentPass = "";
        if (cursor.moveToFirst()) {
            currentUname = cursor.getString(cursor.getColumnIndexOrThrow("username"));
            currentPass = cursor.getString(cursor.getColumnIndexOrThrow("password"));
        }
        cursor.close();

        SaveCredentialsDialogBinding d = SaveCredentialsDialogBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(d.getRoot())
                .create();

        d.editTextUsername.setText(currentUname);
        d.editTextPassword.setText(currentPass);
        d.buttonLogin.setText("Update Credentials");

        d.buttonLogin.setOnClickListener(v -> {
            String uname = d.editTextUsername.getText().toString().trim();
            String pass = d.editTextPassword.getText().toString().trim();
            if (uname.isEmpty() || pass.isEmpty()) {
                d.textViewMessage.setText("Please fill in all fields.");
            } else {
                db.deleteUser();
                db.addUser(uname, pass);
                Toast.makeText(this, "Credentials updated successfully! ✅", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    // (Dangling code removed and integrated into setupButtons)

    private void openUrl(String url) {
        userIsEngaged = true;
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void autoCloseAfter(long ms) {
        handler.postDelayed(() -> {
            if (!userIsEngaged && !isFinishing()) finishAffinity();
        }, ms);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (dbRef != null) {
            if (statsListener   != null) dbRef.removeEventListener(statsListener);
            if (profileListener != null) dbRef.child("campuskey_config/developer")
                .removeEventListener(profileListener);
        }
    }
}
