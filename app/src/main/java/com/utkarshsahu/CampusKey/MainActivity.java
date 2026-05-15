package com.utkarshsahu.CampusKey;

import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.utkarshsahu.CampusKey.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GlobalStatusManager.StatusListener {

    ActivityMainBinding binding;
    private DatabaseReference dbRef;
    private ValueEventListener profileListener;
    private DatabaseReference thanksRef;
    private ValueEventListener thanksListener;
    private WifiLoginHelper   wifiLoginHelper;
    private ConnectivityManager.NetworkCallback networkCallback;
    private final Handler     mainHandler = new Handler(Looper.getMainLooper());
    private static boolean animationsPlayed = false;
    private Runnable periodicRefreshRunnable;

    private ThanksAdapter thanksAdapter;
    private List<ThanksMember> thanksList;
    private boolean isActivityLaunched = false;
    private boolean isForeground = false;
    private Runnable transitionRunnable;

    @Override
    protected void onResume() {
        super.onResume();
        isForeground = true;
        // If login finished while we were away, we can check status here if needed
    }

    @Override
    protected void onPause() {
        super.onPause();
        isForeground = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupWifiLoginHelper();
        loadDevProfile();
        setupNetworkMonitoring();
        setupThanksList();
        
        // Added 1000ms delay before starting auto-login to allow animations to play
        mainHandler.postDelayed(this::checkCredentials, 1000);
        
        startPeriodicRefresh();

        setupDevButtons();
        GlobalStatusManager.addListener(this);
        
        if (!animationsPlayed) {
            applyAnimations();
            animationsPlayed = true;
        } else {
            ensureAllVisible();
        }
    }

    private void ensureAllVisible() {
        binding.statusIconContainer.setVisibility(View.VISIBLE);
        binding.tvStatus.setVisibility(View.VISIBLE);
        binding.btnRefresh.setVisibility(View.VISIBLE);
        binding.devProfileRow.setVisibility(View.VISIBLE);
        binding.autoLoginRow.setVisibility(View.VISIBLE);
        binding.thanksSection.setVisibility(View.VISIBLE);
        binding.share.setVisibility(View.VISIBLE);
        startPulseAnimation();
    }

    private void applyAnimations() {
        android.view.animation.Interpolator overshoot = new android.view.animation.OvershootInterpolator(1.2f);
        
        binding.getRoot().post(() -> {
            // 1. App Icon (Scale Bounce)
            View icon = findViewById(R.id.statusIconContainer);
            if (icon != null) {
                icon.setScaleX(0.7f);
                icon.setScaleY(0.7f);
                icon.setAlpha(0f);
                icon.setVisibility(View.VISIBLE);
                
                icon.animate()
                    .scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(600)
                    .setInterpolator(overshoot)
                    .start();
            }
            
            // 2. Status Text (Slide Up)
            View statusText = findViewById(R.id.tvStatus);
            if (statusText != null) {
                statusText.setTranslationY(40f);
                statusText.setAlpha(0f);
                statusText.setVisibility(View.VISIBLE);
                
                statusText.animate()
                    .translationY(0f).alpha(1f)
                    .setDuration(500)
                    .setStartDelay(150)
                    .setInterpolator(overshoot)
                    .start();
            }
            
            // 3. Dev Card (Slide Up)
            if (binding.devProfileRow != null) {
                binding.devProfileRow.setTranslationY(60f);
                binding.devProfileRow.setAlpha(0f);
                binding.devProfileRow.setVisibility(View.VISIBLE);
                
                binding.devProfileRow.animate()
                    .translationY(0f).alpha(1f)
                    .setDuration(500)
                    .setStartDelay(300)
                    .setInterpolator(overshoot)
                    .start();
            }
            
            // 4. Auto Login Row (Slide Up)
            if (binding.autoLoginRow != null) {
                binding.autoLoginRow.setTranslationY(60f);
                binding.autoLoginRow.setAlpha(0f);
                binding.autoLoginRow.setVisibility(View.VISIBLE);
                
                binding.autoLoginRow.animate()
                    .translationY(0f).alpha(1f)
                    .setDuration(500)
                    .setStartDelay(400)
                    .setInterpolator(overshoot)
                    .start();
            }
            
            // 5. Thanks Card (Slide Up)
            if (binding.thanksSection != null) {
                binding.thanksSection.setTranslationY(60f);
                binding.thanksSection.setAlpha(0f);
                binding.thanksSection.setVisibility(View.VISIBLE);
                
                binding.thanksSection.animate()
                    .translationY(0f).alpha(1f)
                    .setDuration(500)
                    .setStartDelay(500)
                    .setInterpolator(overshoot)
                    .start();
            }

            // 6. Share Button (Slide Up)
            if (binding.share != null) {
                binding.share.setTranslationY(60f);
                binding.share.setAlpha(0f);
                binding.share.setVisibility(View.VISIBLE);
                
                binding.share.animate()
                    .translationY(0f).alpha(1f)
                    .setDuration(500)
                    .setStartDelay(600)
                    .setInterpolator(overshoot)
                    .start();
            }

            // 7. Refresh Button (Fade In)
            if (binding.btnRefresh != null) {
                binding.btnRefresh.setAlpha(0f);
                binding.btnRefresh.setVisibility(View.VISIBLE);
                
                binding.btnRefresh.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(700)
                    .start();
            }

            // Start continuous pulse after initial bounce
            mainHandler.postDelayed(this::startPulseAnimation, 1200);
        });
    }

    private void startPulseAnimation() {
        if (binding.statusIconContainer == null) return;
        android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(binding.statusIconContainer, "scaleX", 1f, 1.08f, 1f);
        android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(binding.statusIconContainer, "scaleY", 1f, 1.08f, 1f);
        scaleX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        scaleY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        scaleX.setDuration(2000);
        scaleY.setDuration(2000);
        
        android.animation.AnimatorSet pulse = new android.animation.AnimatorSet();
        pulse.playTogether(scaleX, scaleY);
        pulse.start();
    }

    private void loadDevProfile() {
        dbRef = FirebaseDatabase.getInstance().getReference("campuskey_config/developer");
        profileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                String name = snap.child("name").getValue(String.class);
                String photoUrl = snap.child("photoUrl").getValue(String.class);
                if (name != null) binding.tvDevName.setText(name);
                if (photoUrl != null) {
                    Glide.with(MainActivity.this)
                        .load(photoUrl)
                        .transform(new CircleCrop())
                        .placeholder(R.drawable.man)
                        .into(binding.imgDev);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        dbRef.addValueEventListener(profileListener);
    }

    @Override public void onConnectionStarted() { runOnUiThread(() -> binding.topProgressBar.setVisibility(View.VISIBLE)); }
    @Override public void onConnectionFinished() { runOnUiThread(() -> binding.topProgressBar.setVisibility(View.GONE)); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 1. Remove Firebase Listeners
        if (dbRef != null && profileListener != null) {
            dbRef.removeEventListener(profileListener);
        }
        if (thanksRef != null && thanksListener != null) {
            thanksRef.removeEventListener(thanksListener);
        }
        
        // 2. Remove Network Callback
        unregisterNetworkMonitoring();
        
        // 3. Destroy WebView to prevent finalizer errors
        if (wifiLoginHelper != null) {
            wifiLoginHelper.destroyWebView();
        }
        
        // 4. Cleanup other resources
        GlobalStatusManager.removeListener(this);
        stopPeriodicRefresh();
        mainHandler.removeCallbacksAndMessages(null);
        
        if (transitionRunnable != null) {
            mainHandler.removeCallbacks(transitionRunnable);
        }
    }

    private void setupNetworkMonitoring() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                mainHandler.post(() -> {
                    // Only trigger if not already busy and activity not recently launched
                    if (!GlobalStatusManager.isConnecting() && !isActivityLaunched) {
                        binding.tvStatus.setText("Wi-Fi connected! Starting auto-login...");
                        checkCredentials();
                    }
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                mainHandler.post(() -> updateStatusForNoWifi());
            }
        };
        cm.registerNetworkCallback(request, networkCallback);
    }

    private void unregisterNetworkMonitoring() {
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (cm != null) cm.unregisterNetworkCallback(networkCallback);
        }
    }

    private void startPeriodicRefresh() {
        stopPeriodicRefresh(); // Clean up existing if any
        periodicRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !GlobalStatusManager.isConnecting()) {
                    checkCredentials();
                }
                mainHandler.postDelayed(this, 30000); // Check every 30s
            }
        };
        mainHandler.postDelayed(periodicRefreshRunnable, 30000);
    }

    private void stopPeriodicRefresh() {
        if (periodicRefreshRunnable != null) {
            mainHandler.removeCallbacks(periodicRefreshRunnable);
            periodicRefreshRunnable = null;
        }
    }

    private boolean isWifiConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
        return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    private void updateStatusForNoWifi() {
        boolean autoOn = getSharedPreferences("CK_PREFS", MODE_PRIVATE).getBoolean("auto_login", false);
        if (autoOn) {
            binding.tvStatus.setText("Wi-Fi disconnected. Waiting to reconnect... ⏳");
        } else {
            binding.tvStatus.setText("⚠️ Wi-Fi is disconnected.");
        }
    }

    private void setupWifiLoginHelper() {
        wifiLoginHelper = new WifiLoginHelper(this, new WifiLoginHelper.WifiLoginListener() {
            @Override public void onStatusUpdate(String status) { mainHandler.post(() -> binding.tvStatus.setText(status)); }
            @Override
            public void onSuccess() {
                mainHandler.post(() -> {
                    syncDataAndFinish();
                });
            }
            @Override
            public void onFailed(String reason) {
                GlobalStatusManager.setConnecting(false);
                mainHandler.post(() -> binding.tvStatus.setText("⚠️ " + reason));
            }
        });
    }

    private void syncDataAndFinish() {
        if (isActivityLaunched || isFinishing()) return;
        isActivityLaunched = true; // Lock immediately to prevent double-sync
        
        binding.tvStatus.setText("Syncing community data... ☁️");
        
        // 1. Record Launch (Upload)
        new AnalyticsHelper(this).recordLaunch();

        // 2. Fetch Latest Leaderboard (Sync)
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("campuskey/users");
        
        // Watchdog: If Firebase is too slow, proceed anyway after 5s
        mainHandler.postDelayed(() -> {
            if (isActivityLaunched && GlobalStatusManager.isConnecting()) {
                Log.w("CampusKey", "Sync timed out, forcing transition.");
                proceedToConnectedActivity();
            }
        }, 5000);

        usersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                processThanksSnapshot(task.getResult());
            } else {
                Log.e("CampusKey", "Sync fetch failed: " + (task.getException() != null ? task.getException().getMessage() : "unknown"));
            }

            // Final Transition
            mainHandler.postDelayed(this::proceedToConnectedActivity, 800);
        });
    }

    private void proceedToConnectedActivity() {
        if (isFinishing() || !GlobalStatusManager.isConnecting() || !isForeground) return;
        
        // Ensure this only runs once per connection
        if (transitionRunnable != null) {
            mainHandler.removeCallbacks(transitionRunnable);
        }

        transitionRunnable = () -> {
            if (isFinishing()) return;
            
            GlobalStatusManager.setConnecting(false);
            binding.tvStatus.setText("Connected! ✅");
            
            Intent i = new Intent(MainActivity.this, ConnectedActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            
            // Allow future sessions after a delay
            mainHandler.postDelayed(() -> isActivityLaunched = false, 5000);
            transitionRunnable = null;
        };
        mainHandler.post(transitionRunnable);
    }

    private void cancelLoginProcess(String statusMsg) {
        wifiLoginHelper.cancelLogin();
        GlobalStatusManager.setConnecting(false);
        isActivityLaunched = false; 
        if (transitionRunnable != null) {
            mainHandler.removeCallbacks(transitionRunnable);
            transitionRunnable = null;
        }
        binding.tvStatus.setText(statusMsg);
        Log.d("CampusKey", "Login process cancelled: " + statusMsg);
    }

    private void processThanksSnapshot(DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            Log.d("CampusKey", "ProcessThanks: Snapshot is null or empty");
            return;
        }
        Log.d("CampusKey", "Processing Thanks Snapshot, children: " + snapshot.getChildrenCount());
        
        List<ThanksMember> allMembers = new ArrayList<>();
        
        int optedInCount = 0;
        for (DataSnapshot userSnap : snapshot.getChildren()) {
            try {
                Boolean optIn = userSnap.child("optInLeaderboard").getValue(Boolean.class);
                if (optIn != null && optIn) {
                    optedInCount++;
                    String name = userSnap.child("name").getValue(String.class);
                    String insta = userSnap.child("instagram").getValue(String.class);
                    String picUrl = userSnap.child("picUrl").getValue(String.class);
                    Object rawCount = userSnap.child("totalLaunches").getValue();
                    
                    Long count = 0L;
                    if (rawCount instanceof Long) count = (Long) rawCount;
                    else if (rawCount instanceof Integer) count = ((Integer) rawCount).longValue();
                    
                    // Fallback for missing name
                    if (name == null || name.trim().isEmpty()) {
                        name = "CampusKey User";
                    }
                    
                    // Dev filter removed as per user request to see themselves in the list
                    // if (name.trim().equalsIgnoreCase("Utkarsh Sahu")) continue;
                    
                    String instaTag = (insta != null && !insta.isEmpty()) ? "@" + insta : count + " connects";
                    String socialUrl = (insta != null && !insta.isEmpty()) ? "https://instagram.com/" + insta : "";
                    
                    if (picUrl == null || picUrl.isEmpty()) {
                        picUrl = "https://ui-avatars.com/api/?name=" + Uri.encode(name != null ? name : "U") + "&background=random";
                    }
                    
                    allMembers.add(new ThanksMember(name, picUrl, socialUrl, instaTag, count));
                }
            } catch (Exception e) {
                Log.e("CampusKey", "Error parsing user: " + userSnap.getKey());
            }
        }
        
        Log.d("CampusKey", "Total opted-in: " + optedInCount + ", after filtering dev: " + allMembers.size());
        
        Collections.sort(allMembers, (m1, m2) -> Long.compare(m2.getCount(), m1.getCount()));
        
        thanksList.clear();
        for (int i = 0; i < Math.min(30, allMembers.size()); i++) {
            thanksList.add(allMembers.get(i));
        }
        
        // Hide section if empty
        binding.thanksSection.setVisibility(thanksList.isEmpty() ? View.GONE : View.VISIBLE);
        
        Log.d("CampusKey", "Final thanksList size: " + thanksList.size());
        thanksAdapter.notifyDataSetChanged();
        saveThanksToCache();
    }

    private void checkCredentials() {
        if (GlobalStatusManager.isConnecting()) return;

        Cursor cursor = new DatabaseHelper(this).getUser();
        if (cursor.moveToFirst()) {
            String uname = cursor.getString(cursor.getColumnIndexOrThrow("username"));
            String pass = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            cursor.close();
            
            if (isWifiConnected()) {
                binding.tvStatus.setText("Auto-connecting to college Wi-Fi...");
                GlobalStatusManager.setConnecting(true);
                wifiLoginHelper.startLogin(uname, pass);
            } else {
                updateStatusForNoWifi();
            }
        } else {
            cursor.close();
            binding.tvStatus.setText("Credentials required for auto-login.");
            showSaveCredentialsDialog();
        }
    }

    private void showSaveCredentialsDialog() {
        if (isFinishing()) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.save_credentials_dialog, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etUser = dialogView.findViewById(R.id.editTextUsername);
        EditText etPass = dialogView.findViewById(R.id.editTextPassword);
        TextView tvMsg = dialogView.findViewById(R.id.textViewMessage);
        
        dialogView.findViewById(R.id.buttonLogin).setOnClickListener(v -> {
            String u = etUser.getText().toString().trim();
            String p = etPass.getText().toString().trim();
            
            if (u.isEmpty() || p.isEmpty()) {
                tvMsg.setText("Please enter both fields.");
            } else {
                DatabaseHelper db = new DatabaseHelper(this);
                db.deleteUser(); 
                db.addUser(u, p);
                dialog.dismiss();
                Toast.makeText(this, "Credentials saved! 💾", Toast.LENGTH_SHORT).show();
                checkCredentials(); // Trigger login attempt now
            }
        });

        dialog.show();
    }

    private void setupDevButtons() {
        // Auto-Login Switch logic
        boolean isAutoLoginEnabled = getSharedPreferences("CK_PREFS", MODE_PRIVATE).getBoolean("auto_login", false);
        if (binding.switchAutoLogin != null) {
            binding.switchAutoLogin.setChecked(isAutoLoginEnabled);
            binding.switchAutoLogin.setOnCheckedChangeListener((btn, isChecked) -> {
                getSharedPreferences("CK_PREFS", MODE_PRIVATE).edit().putBoolean("auto_login", isChecked).apply();
                Intent serviceIntent = new Intent(this, AutoLoginService.class);
                if (isChecked) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    } else {
                        startService(serviceIntent);
                    }
                    Toast.makeText(this, "Auto-Login Service Started 🚀", Toast.LENGTH_SHORT).show();
                } else {
                    stopService(serviceIntent);
                    Toast.makeText(this, "Auto-Login Service Stopped 🛑", Toast.LENGTH_SHORT).show();
                }
            });
            
            // If enabled, ensure service is running
            if (isAutoLoginEnabled) {
                Intent serviceIntent = new Intent(this, AutoLoginService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            }
        }

        binding.devProfileRow.setOnClickListener(v -> {
            startActivity(new Intent(this, Info.class));
        });
        if (binding.btnTopInfo != null) {
            binding.btnTopInfo.setOnClickListener(v -> {
                startActivity(new Intent(this, Info.class));
            });
        }
        binding.share.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "Connect to Kalinga Wi-Fi easily with CampusKey! https://github.com/UtkarshSahu9906/Campus-Key");
            startActivity(Intent.createChooser(intent, "Share App"));
        });
        
        if (binding.btnRefresh != null) {
            binding.btnRefresh.setOnClickListener(v -> {
                // Animate rotation on click
                binding.btnRefresh.animate().rotationBy(360f).setDuration(500).start();
                checkCredentials();
            });
        }
        
        if (binding.btnThanksInfo != null) {
            binding.btnThanksInfo.setOnClickListener(v -> {
                showThanksInfoDialog();
            });
        }
        
        if (binding.btnAutoLoginInfo != null) {
            binding.btnAutoLoginInfo.setOnClickListener(v -> {
                cancelLoginProcess("Auto-login info opened.");
                showAutoLoginInfoDialog();
            });
        }
    }

    private void showAutoLoginInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_autologin_info, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnDismissAutoLoginInfo).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showThanksInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_thanks_info, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnGoToInfo).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, Info.class));
        });

        dialogView.findViewById(R.id.btnDismissDialog).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void setupThanksList() {
        binding.thanksRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        thanksList = new ArrayList<>();
        thanksAdapter = new ThanksAdapter(thanksList, this);
        binding.thanksRecyclerView.setAdapter(thanksAdapter);

        // 1. Load from cache first for speed
        loadThanksFromCache();
        Log.d("CampusKey", "After cache load, thanksList size: " + thanksList.size());

        // 2. Fetch from Firebase for updates
        thanksRef = FirebaseDatabase.getInstance().getReference("campuskey/users");
        thanksListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                processThanksSnapshot(snapshot);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CampusKey", "Firebase Thanks load cancelled: " + error.getMessage());
            }
        };
        thanksRef.addValueEventListener(thanksListener);
    }

    private void saveThanksToCache() {
        try {
            JSONArray arr = new JSONArray();
            for (ThanksMember m : thanksList) {
                JSONObject obj = new JSONObject();
                obj.put("name", m.getName());
                obj.put("img",  m.getImageUrl());
                obj.put("url",  m.getSocialUrl());
                obj.put("tag",  m.getTagText());
                obj.put("cnt",  m.getCount());
                arr.put(obj);
            }
            getSharedPreferences("CK_PREFS", MODE_PRIVATE).edit().putString("cached_thanks", arr.toString()).apply();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadThanksFromCache() {
        try {
            String json = getSharedPreferences("CK_PREFS", MODE_PRIVATE).getString("cached_thanks", "");
            if (json.isEmpty()) return;
            JSONArray arr = new JSONArray(json);
            thanksList.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                thanksList.add(new ThanksMember(
                    obj.optString("name", "Unknown"), 
                    obj.optString("img", ""),
                    obj.optString("url", ""), 
                    obj.optString("tag", ""), 
                    obj.optLong("cnt", 0)));
            }
            thanksAdapter.notifyDataSetChanged();
        } catch (Exception e) { Log.e("CampusKey", "Cache load error: " + e.getMessage()); }
    }


}