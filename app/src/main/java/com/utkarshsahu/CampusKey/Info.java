package com.utkarshsahu.CampusKey;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Info extends AppCompatActivity implements GlobalStatusManager.StatusListener {
    @Override
    public void onConnectionStarted() {
        runOnUiThread(() -> findViewById(R.id.topProgressBar).setVisibility(android.view.View.VISIBLE));
    }

    @Override
    public void onConnectionFinished() {
        runOnUiThread(() -> findViewById(R.id.topProgressBar).setVisibility(android.view.View.GONE));
    }

    @Override
    protected void onDestroy() {
        GlobalStatusManager.removeListener(this);
        super.onDestroy();
    }

    private RecyclerView thanksRecyclerView, feedbackRecyclerView;
    private ThanksAdapter thanksAdapter;
    private FeedbackAdapter feedbackAdapter;
    private List<ThanksMember> thanksList;
    private List<FeedbackModel> feedbackList;
    private DatabaseReference feedbackRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        feedbackRef = FirebaseDatabase.getInstance().getReference("feedbacks");

        setupFeedbackButton();
        setupThanksList();
        setupCommunityWall();
        GlobalStatusManager.addListener(this);
        applyAnimations();
    }

    private void setupFeedbackButton() {
        findViewById(R.id.btnFeedback).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SENDTO);
            i.setData(Uri.parse("mailto:utkarshsahu9906@gmail.com"));
            i.putExtra(Intent.EXTRA_SUBJECT, "CampusKey Mission Feedback");
            startActivity(Intent.createChooser(i, "Send Feedback via"));
        });
        
        findViewById(R.id.btnRateApp).setOnClickListener(v -> showRatingDialog());
        findViewById(R.id.btnSeeAllFeedbacks).setOnClickListener(v -> {
            startActivity(new Intent(this, AllFeedbacksActivity.class));
        });

        findViewById(R.id.btnDownloadApp).setOnClickListener(v -> {
            startActivity(new Intent(this, DownloadAppActivity.class));
        });


        findViewById(R.id.btnJoinLeaderboard).setOnClickListener(v -> {
            showJoinLeaderboardDialog();
        });
    }

    private void showJoinLeaderboardDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_join_leaderboard, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        EditText etName = dialogView.findViewById(R.id.etLeaderboardName);
        EditText etInstagram = dialogView.findViewById(R.id.etLeaderboardInstagram);
        EditText etPic = dialogView.findViewById(R.id.etLeaderboardPic);

        // Pre-fill existing data if they want to edit
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        FirebaseDatabase.getInstance().getReference("campuskey/users/" + deviceId).get()
            .addOnSuccessListener(snap -> {
                if (snap.exists() && dialog.isShowing()) {
                    String existingName = snap.child("name").getValue(String.class);
                    String existingInsta = snap.child("instagram").getValue(String.class);
                    String existingPic = snap.child("picUrl").getValue(String.class);
                    if (existingName != null && etName.getText().toString().isEmpty()) etName.setText(existingName);
                    if (existingInsta != null && etInstagram.getText().toString().isEmpty()) etInstagram.setText(existingInsta);
                    if (existingPic != null && etPic.getText().toString().isEmpty()) etPic.setText(existingPic);
                }
            });

        dialogView.findViewById(R.id.btnCancelJoin).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSaveJoin).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String insta = etInstagram.getText().toString().trim();
            String pic = etPic.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Display Name is required", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (insta.isEmpty()) {
                Toast.makeText(this, "Instagram Handle/Link is required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Parse Instagram username from URL
            if (insta.contains("instagram.com/")) {
                insta = insta.substring(insta.indexOf("instagram.com/") + 14);
                if (insta.contains("?")) insta = insta.substring(0, insta.indexOf("?"));
                if (insta.endsWith("/")) insta = insta.substring(0, insta.length() - 1);
            }
            if (insta.startsWith("@")) insta = insta.substring(1);

            new AnalyticsHelper(this).saveLeaderboardProfile(name, insta, pic);
            Toast.makeText(this, "Joined Leaderboard! Note: Updates may take a moment.", Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupThanksList() {
        thanksRecyclerView = findViewById(R.id.thanksRecyclerView);
        thanksRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        thanksList = new ArrayList<>();
        thanksAdapter = new ThanksAdapter(thanksList, this);
        thanksRecyclerView.setAdapter(thanksAdapter);

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("campuskey/users");
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ThanksMember> allMembers = new ArrayList<>();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    Boolean optIn = userSnap.child("optInLeaderboard").getValue(Boolean.class);
                    if (optIn != null && optIn) {
                        String name = userSnap.child("name").getValue(String.class);
                        String insta = userSnap.child("instagram").getValue(String.class);
                        String picUrl = userSnap.child("picUrl").getValue(String.class);
                        Object rawCount = userSnap.child("totalLaunches").getValue();
                        Long count = 0L;
                        if (rawCount instanceof Long) count = (Long) rawCount;
                        else if (rawCount instanceof Integer) count = ((Integer) rawCount).longValue();
                        
                        // Exclude developer as requested
                        if (name == null || name.isEmpty() || name.equalsIgnoreCase("Utkarsh Sahu")) continue;
                        
                        String instaTag = (insta != null && !insta.isEmpty()) ? "@" + insta : count + " connections";
                        String socialUrl = (insta != null && !insta.isEmpty()) ? "https://instagram.com/" + insta : "";
                        
                        if (picUrl == null || picUrl.isEmpty()) {
                            picUrl = "https://ui-avatars.com/api/?name=" + Uri.encode(name) + "&background=random";
                        }
                        
                        allMembers.add(new ThanksMember(name, picUrl, socialUrl, instaTag, count));
                    }
                }
                
                Collections.sort(allMembers, (m1, m2) -> Long.compare(m2.getCount(), m1.getCount()));
                
                thanksList.clear();
                for (int i = 0; i < Math.min(10, allMembers.size()); i++) {
                    thanksList.add(allMembers.get(i));
                }
                
                thanksAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }



    private void setupCommunityWall() {
        feedbackRecyclerView = findViewById(R.id.feedbackRecyclerView);
        feedbackRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        feedbackList = new ArrayList<>();
        feedbackAdapter = new FeedbackAdapter(feedbackList);
        feedbackRecyclerView.setAdapter(feedbackAdapter);

        feedbackRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<FeedbackModel> allFeedback = new ArrayList<>();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    FeedbackModel feedback = postSnapshot.getValue(FeedbackModel.class);
                    if (feedback != null) allFeedback.add(feedback);
                }
                
                // Sort by newest first
                Collections.sort(allFeedback, (f1, f2) -> Long.compare(f2.getTimestamp(), f1.getTimestamp()));
                
                // Only show top 3 highlights
                feedbackList.clear();
                for (int i = 0; i < Math.min(3, allFeedback.size()); i++) {
                    feedbackList.add(allFeedback.get(i));
                }
                
                feedbackAdapter.notifyDataSetChanged();
                
                // Show/Hide "See All" based on count
                findViewById(R.id.btnSeeAllFeedbacks).setVisibility(allFeedback.size() > 3 ? View.VISIBLE : View.GONE);
                findViewById(R.id.tvEmptyFeedback).setVisibility(allFeedback.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Info.this, "Failed to load feedback", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRatingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_give_feedback, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.etFeedbackDisplayName);
        EditText etComment = dialogView.findViewById(R.id.etFeedbackComment);
        TextView[] stars = {
            dialogView.findViewById(R.id.star1),
            dialogView.findViewById(R.id.star2),
            dialogView.findViewById(R.id.star3),
            dialogView.findViewById(R.id.star4),
            dialogView.findViewById(R.id.star5)
        };

        final int[] selectedRating = {5};
        for (int i = 0; i < stars.length; i++) {
            final int rating = i + 1;
            stars[i].setOnClickListener(v -> {
                selectedRating[0] = rating;
                for (int j = 0; j < stars.length; j++) stars[j].setText(j < rating ? "⭐" : "☆");
            });
        }

        AlertDialog dialog = builder.create();
        dialogView.findViewById(R.id.btnSubmitFeedback).setOnClickListener(v -> {
            String comment = etComment.getText().toString().trim();
            String displayName = etName.getText().toString().trim();

            if (comment.isEmpty()) {
                Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
                return;
            }

            if (displayName.isEmpty()) {
                int randomNum = new Random().nextInt(9000) + 1000;
                displayName = "CampusHero_" + randomNum;
            }

            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            FeedbackModel feedback = new FeedbackModel(displayName, selectedRating[0], comment, System.currentTimeMillis());
            
            feedbackRef.child(deviceId).setValue(feedback)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Feedback shared publicly!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
        });

        dialog.show();
    }

    private void applyAnimations() {
        android.view.animation.Animation scaleBounce = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.scale_bounce);
        android.view.animation.Animation slideUp = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_fade_in);

        findViewById(android.R.id.content).post(() -> {
            findViewById(R.id.profileContainer).startAnimation(scaleBounce);
            slideUp.setStartOffset(200);
            findViewById(R.id.userName).startAnimation(slideUp);
            
            android.view.animation.Animation s2 = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_fade_in);
            s2.setStartOffset(400);
            findViewById(R.id.socialRow).startAnimation(s2);
            
            android.view.animation.Animation s3 = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_fade_in);
            s3.setStartOffset(600);
            findViewById(R.id.missionCard).startAnimation(s3);

            android.view.animation.Animation s5 = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_fade_in);
            s5.setStartOffset(800);
            findViewById(R.id.communityWallSection).startAnimation(s5);
            
            android.view.animation.Animation s6 = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_fade_in);
            s6.setStartOffset(1000);
            findViewById(R.id.feedbackCard).startAnimation(s6);

            android.view.animation.Animation s7 = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_fade_in);
            s7.setStartOffset(1200);
            findViewById(R.id.thanksSection).startAnimation(s7);
        });
    }
}