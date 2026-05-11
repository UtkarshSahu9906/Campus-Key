package com.utkarshsahu.CampusKey;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AppUrlFetcher {

    private static final String TAG      = "CampusKey";
    private static final String FB_PATH  = "campuskey_config/developer/shareUrl";

    public interface OnSuccess { void onSuccess(String url); }
    public interface OnFailed  { void onFailed(); }

    /**
     * Fetches the app share URL from Firebase Realtime Database.
     * Path: campuskey_config/developer/shareUrl
     *
     * Usage:
     *   AppUrlFetcher.fetch(url -> doSomething(url), () -> fallback());
     */
    public static void fetch(OnSuccess onSuccess, OnFailed onFailed) {
        FirebaseDatabase.getInstance()
                .getReference(FB_PATH)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        String url = snap.getValue(String.class);
                        if (url != null && !url.trim().isEmpty()) {
                            Log.d(TAG, "AppUrlFetcher: got url → " + url);
                            onSuccess.onSuccess(url.trim());
                        } else {
                            Log.w(TAG, "AppUrlFetcher: url is empty in Firebase");
                            onFailed.onFailed();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "AppUrlFetcher: Firebase error → " + error.getMessage());
                        onFailed.onFailed();
                    }
                });
    }
}