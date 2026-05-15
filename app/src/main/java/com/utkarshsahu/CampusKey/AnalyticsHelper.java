package com.utkarshsahu.CampusKey;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AnalyticsHelper {

    private static final String TAG = "CampusKey_Analytics";

    private final DatabaseReference db;
    private final String deviceId;
    private final String today;

    public AnalyticsHelper(Context context) {
        db       = FirebaseDatabase.getInstance().getReference();
        deviceId = Settings.Secure.getString(
            context.getContentResolver(), Settings.Secure.ANDROID_ID);
        today    = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    /**
     * Called every time the app launches / service triggers a login.
     * - Increments daily count
     * - Increments user's personal launch count
     * - Updates lastSeen timestamp
     * - On first ever launch: increments totalUniqueUsers
     */
    public void recordLaunch() {
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(new Date());

        DatabaseReference userRef = db.child("campuskey/users/" + deviceId);

        // Increment daily counter
        db.child("campuskey/daily/" + today + "/totalOpens")
          .setValue(ServerValue.increment(1));

        // Increment all-time counter
        db.child("campuskey/summary/totalAllTimeLaunches")
          .setValue(ServerValue.increment(1));

        // Update user node
        userRef.child("totalLaunches").setValue(ServerValue.increment(1))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to increment launches: " + e.getMessage()));
            
        userRef.child("lastSeen").setValue(now);

        // Set firstSeen only if not already set
        userRef.child("firstSeen").get().addOnSuccessListener(snap -> {
            if (!snap.exists()) {
                userRef.child("firstSeen").setValue(now);
                // New user — increment unique count
                db.child("campuskey/summary/totalUniqueUsers")
                  .setValue(ServerValue.increment(1));
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to check firstSeen: " + e.getMessage()));

        Log.d(TAG, "Launch recording triggered for device: " + deviceId);
    }

    /**
     * Save optional name and gender from profile dialog.
     */
    public void saveUserProfile(String name, String gender) {
        if (name == null || name.trim().isEmpty()) return;

        Map<String, Object> data = new HashMap<>();
        data.put("name",   name.trim());
        data.put("gender", gender != null ? gender : "Prefer not to say");

        db.child("campuskey/users/" + deviceId).updateChildren(data);
        Log.d(TAG, "Profile saved: " + name + " | " + gender);
    }

    public void saveLeaderboardProfile(String name, String instagram, String picUrl) {
        if (name == null || name.trim().isEmpty()) return;

        Map<String, Object> data = new HashMap<>();
        data.put("name", name.trim());
        data.put("optInLeaderboard", true);
        
        if (instagram != null && !instagram.trim().isEmpty()) {
            data.put("instagram", instagram.trim());
        }
        if (picUrl != null && !picUrl.trim().isEmpty()) {
            data.put("picUrl", picUrl.trim());
        }
        
        // This ensures they are indexed immediately with at least 0 connects
        db.child("campuskey/users/" + deviceId).child("totalLaunches").get().addOnSuccessListener(snap -> {
            if (!snap.exists()) {
                db.child("campuskey/users/" + deviceId).child("totalLaunches").setValue(0);
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to check totalLaunches: " + e.getMessage()));

        db.child("campuskey/users/" + deviceId).updateChildren(data)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Leaderboard profile saved successfully: " + name))
            .addOnFailureListener(e -> Log.e(TAG, "Leaderboard save FAILED: " + e.getMessage()));
    }
}
