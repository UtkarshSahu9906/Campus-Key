package com.utkarsh.CampusKey;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
    private BroadcastReceiver   connectedReceiver;

    private boolean userIsEngaged = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);

        playSuccessAnimation();
        setupButtons();
        loadRealtimeData();
        autoCloseAfter(6000);
        listenForConnectedBroadcast();
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
                            .placeholder(R.drawable.dialog_bg)
                            .error(R.drawable.dialog_bg)
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
        Button btnShare = findViewById(R.id.btnShare);
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                userIsEngaged = true;
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT,
                    "Auto Wi-Fi login at Kalinga University! Download CampusKey:\n" +
                    "https://play.google.com/store/apps/details?id=" + getPackageName());
                startActivity(Intent.createChooser(i, "Share CampusKey"));
            });
        }

        Button btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> finishAffinity());

        // Default social buttons (overridden by Firebase data above)
        View ig = findViewById(R.id.btnInstagram);
        View li = findViewById(R.id.btnLinkedIn);
        View em = findViewById(R.id.btnMail);
        if (ig != null) ig.setOnClickListener(v -> {
            userIsEngaged = true;
            openUrl("https://www.instagram.com/mr._utkarsh_sahu/");
        });
        if (li != null) li.setOnClickListener(v -> {
            userIsEngaged = true;
            openUrl("https://www.linkedin.com/in/utkarshsahu9906/");
        });
        if (em != null) em.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:utkarshsahu9906@gmail.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "CampusKey Feedback");
            startActivity(intent);
        });
    }

    private void openUrl(String url) {
        userIsEngaged = true;
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void autoCloseAfter(long ms) {
        handler.postDelayed(() -> {
            if (!userIsEngaged && !isFinishing()) finishAffinity();
        }, ms);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void listenForConnectedBroadcast() {
        connectedReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent intent) {
                playSuccessAnimation();
            }
        };
        IntentFilter filter = new IntentFilter(WifiLoginService.ACTION_CONNECTED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(connectedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(connectedReceiver, filter);
        }
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
        if (connectedReceiver != null) {
            try { unregisterReceiver(connectedReceiver); } catch (Exception ignored) {}
        }
    }
}
