package com.utkarsh.CampusKey;

import static com.utkarsh.CampusKey.LocalData.*;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.utkarsh.CampusKey.databinding.ActivityMainBinding;
import com.utkarsh.CampusKey.databinding.DialogUserProfileBinding;
import com.utkarsh.CampusKey.databinding.SaveCredentialsDialogBinding;
import com.utkarsh.CampusKey.databinding.VersionCheckDialogBinding;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    private LocalData         localData;
    private AnalyticsHelper   analytics;
    private DatabaseReference dbRef;
    private ValueEventListener profileListener;
    private final Handler     mainHandler = new Handler(Looper.getMainLooper());
    private BroadcastReceiver loginSuccessReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding   = ActivityMainBinding.inflate(getLayoutInflater());
        localData = new LocalData(this);
        setContentView(binding.getRoot());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        analytics = new AnalyticsHelper(this);
        analytics.recordLaunch();

        // Start background WiFi login service
        BootReceiver.startService(this);

        // Show user guide on very first launch
        if (!localData.getBoolean("GUIDE_SHOWN")) {
            startActivity(new Intent(this, UserGuideActivity.class));
            localData.save("GUIDE_SHOWN", true);
        }

        // Subscribe to FCM push notifications
        com.google.firebase.messaging.FirebaseMessaging.getInstance()
                .subscribeToTopic("campuskey_all");

        // Load developer profile from Firebase (live updates)
        loadDevProfile();

        // First launch: ask optional name/gender
        if (!localData.getBoolean(PROFILE_ASKED)) {
            showUserProfileDialog(this::checkCredentials);
        } else {
            checkCredentials();
        }

        setupDevButtons();
        setupLoginSuccessReceiver();
    }

    // ─────────────────────────────────────────────
    // FIREBASE DEV PROFILE — loads name, photo, social links
    // Updates instantly when you change them in the admin panel
    // ─────────────────────────────────────────────
    private void loadDevProfile() {
        dbRef = FirebaseDatabase.getInstance().getReference("campuskey_config/developer");

        profileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                String name      = snap.child("name").getValue(String.class);
                String username  = snap.child("username").getValue(String.class);
                String photoUrl  = snap.child("photoUrl").getValue(String.class);
                String instagram = snap.child("instagram").getValue(String.class);
                String linkedin  = snap.child("linkedin").getValue(String.class);
                String email     = snap.child("email").getValue(String.class);

                // Update name
                if (name != null && !name.isEmpty()) {
                    TextView tvName = binding.getRoot().findViewById(R.id.tvDevName);
                    if (tvName != null) tvName.setText(name);
                }

                // Update photo via Glide
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    ImageView img = binding.getRoot().findViewById(R.id.imgDev);
                    if (img != null) {
                        Glide.with(MainActivity.this)
                                .load(photoUrl)
                                .transform(new CircleCrop())
                                .placeholder(R.drawable.people)
                                .error(R.drawable.people)
                                .into(img);
                    }
                }

                // Override social button listeners with live Firebase data
                if (instagram != null && !instagram.isEmpty()) {
                    final String ig = instagram;
                    binding.btnInstagram.setOnClickListener(v -> openUrl(ig));
                }
                if (linkedin != null && !linkedin.isEmpty()) {
                    final String li = linkedin;
                    binding.btnLinkedIn.setOnClickListener(v -> openUrl(li));
                }
                if (email != null && !email.isEmpty()) {
                    final String em = email;
                    binding.btnMail.setOnClickListener(v -> {
                        Intent i = new Intent(Intent.ACTION_SENDTO);
                        i.setData(Uri.parse("mailto:" + em));
                        i.putExtra(Intent.EXTRA_SUBJECT, "CampusKey Feedback");
                        startActivity(i);
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Firebase failed — keep hardcoded defaults, no crash
            }
        };

        dbRef.addValueEventListener(profileListener);
    }

    // ─────────────────────────────────────────────
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void setupLoginSuccessReceiver() {
        loginSuccessReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent intent) {
                Toast.makeText(MainActivity.this,
                        "Connected to college Wi-Fi!", Toast.LENGTH_SHORT).show();
                binding.tvStatus.setText("Connected! ✅");
            }
        };
        IntentFilter filter = new IntentFilter(WifiLoginService.ACTION_CONNECTED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(loginSuccessReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(loginSuccessReceiver, filter);
        }
    }

    // ─────────────────────────────────────────────
    // VERSION CHECK + CREDENTIAL CHECK
    // ─────────────────────────────────────────────
    private void checkCredentials() {
        int    minVersion = localData.getInt(MINIMUM_VERSION_CODE);
        String updateUrl  = localData.getString(URL);

        if (minVersion > 0
                && updateUrl != null && !updateUrl.isEmpty()
                && AppUtils.getVersionCode(this) < minVersion) {
            showUpdateDialog(
                    updateUrl,
                    localData.getString(MAIN_TEXT),
                    localData.getString(DESCRIPTION),
                    localData.getBoolean(FORCE));
            return;
        }

        Cursor cursor = new DatabaseHelper(this).getUser();
        if (cursor.moveToFirst()) {
            cursor.close();
            binding.tvStatus.setText(
                    "Auto-login active. Will connect when college Wi-Fi is detected. 📡");
        } else {
            cursor.close();
            binding.tvStatus.setText("Save your credentials to enable auto-login.");
            showCredentialSaveDialog();
        }
    }

    // ─────────────────────────────────────────────
    // DIALOGS
    // ─────────────────────────────────────────────
    private void showUserProfileDialog(Runnable onDone) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        DialogUserProfileBinding d  = DialogUserProfileBinding.inflate(getLayoutInflater());
        builder.setView(d.getRoot());
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();

        String[] genders = {"Prefer not to say", "Male", "Female", "Other"};
        ArrayAdapter<String> ga = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, genders);
        ga.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        d.spinnerGender.setAdapter(ga);

        d.btnSkip.setOnClickListener(v -> {
            localData.save(PROFILE_ASKED, true);
            dialog.dismiss();
            onDone.run();
        });
        d.btnSave.setOnClickListener(v -> {
            String name   = d.etName.getText().toString().trim();
            String gender = d.spinnerGender.getSelectedItem().toString();
            analytics.saveUserProfile(name, gender);
            localData.save(PROFILE_ASKED, true);
            dialog.dismiss();
            onDone.run();
        });
        dialog.show();
    }

    public void showCredentialSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        SaveCredentialsDialogBinding d = SaveCredentialsDialogBinding
                .inflate(getLayoutInflater(), null, false);
        builder.setView(d.getRoot());
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();

        d.buttonLogin.setOnClickListener(v -> {
            String uname = d.editTextUsername.getText().toString().trim();
            String pass  = d.editTextPassword.getText().toString().trim();
            if (uname.isEmpty() || pass.isEmpty()) {
                d.textViewMessage.setText("Please fill in all fields.");
            } else {
                new DatabaseHelper(this).addUser(uname, pass);
                d.textViewMessage.setText("Saved! Auto-login is now active ✅");
                dialog.dismiss();
                binding.tvStatus.setText(
                        "Auto-login active. Will connect when college Wi-Fi detected. 📡");
            }
        });
        dialog.show();
    }

    private void showUpdateDialog(String updateUrl, String mainText,
                                  String desc, boolean force) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        VersionCheckDialogBinding d = VersionCheckDialogBinding
                .inflate(getLayoutInflater(), null, false);
        builder.setView(d.getRoot());
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        d.mainText.setText(mainText != null && !mainText.isEmpty()
                ? mainText : "Update Available");
        d.descriptionText.setText(desc != null && !desc.isEmpty()
                ? desc : "A new version of CampusKey is available.");
        if (!force) d.buttonCancel.setVisibility(View.VISIBLE);
        d.buttonCancel.setOnClickListener(v -> { dialog.dismiss(); checkCredentials(); });
        d.buttonUpdate.setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)));
            dialog.dismiss();
        });
        dialog.show();
    }

    // ─────────────────────────────────────────────
    // DEV BUTTONS — defaults (overridden by Firebase data above)
    // ─────────────────────────────────────────────
    private void setupDevButtons() {
        binding.btnInstagram.setOnClickListener(v ->
                openUrl("https://www.instagram.com/mr._utkarsh_sahu/"));
        binding.btnLinkedIn.setOnClickListener(v ->
                openUrl("https://www.linkedin.com/in/utkarshsahu9906/"));
        binding.btnMail.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SENDTO);
            i.setData(Uri.parse("mailto:utkarshsahu9906@gmail.com"));
            i.putExtra(Intent.EXTRA_SUBJECT, "CampusKey Feedback");
            startActivity(i);
        });
    }

    private void openUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    // ─────────────────────────────────────────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firebase listener to prevent memory leaks
        if (dbRef != null && profileListener != null) {
            dbRef.removeEventListener(profileListener);
        }
        if (loginSuccessReceiver != null) {
            try { unregisterReceiver(loginSuccessReceiver); } catch (Exception ignored) {}
        }
        mainHandler.removeCallbacksAndMessages(null);
    }
}