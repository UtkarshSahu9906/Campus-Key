package com.utkarsh.CampusKe;

import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.webkit.WebSettings;
import android.widget.Toast;

import com.utkarsh.CampusKe.databinding.ActivityMainBinding;
import com.utkarsh.CampusKe.databinding.SaveCredentialsDialogBinding;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private AlertDialog dialog; // Class-level variable for dialog
    private boolean loginAttempted = false;
    private boolean isGo = false; // Class-level variable

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        getCredentials();


    }

  // To avoid multiple login attempts

    public void startLoginAttempt(String Username, String Password) {
        // Enable JavaScript
        binding.webView.setVisibility(View.VISIBLE);
        binding.webView.getSettings().setJavaScriptEnabled(true);

        // Allow mixed content (HTTP + HTTPS)
        binding.webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Check if the login is successful by looking for the success URL
                if (url.contains("http://172.24.64.1:8090/httpclient.html")) { // Replace with the actual success URL
                    if (!isGo) { // Check if SuccessActivity has already been started
                        isGo = true; // Mark that we have navigated to SuccessActivity
                        Intent intent = new Intent(MainActivity.this, SuccessActivity.class);
                        startActivity(intent);
                        finish(); // Close MainActivity if you don't want to return to it
                    }
                }

                // Prevent multiple form submissions
                if (!loginAttempted) {
                    loginAttempted = true; // Mark login as attempted

                    // Inject JavaScript to auto-fill and submit the form
                    binding.webView.evaluateJavascript(
                            "(function() { " +
                                    "document.getElementById('username').value = '" + Username + "';" + // Set username
                                    "document.getElementById('password').value = '" + Password + "';" + // Set password
                                    "document.getElementById('loginbutton').click();" + // Click the button
                                    "})();",
                            null
                    );
                }
            }
        });

        // Load the URL
        binding.webView.loadUrl("http://172.24.64.1:8090/httpclient.html");


    }

    public void showCredentialSaveDialog() {
        // Create the dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflate the dialog layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.save_credentials_dialog, null);

        // Initialize View Binding
        SaveCredentialsDialogBinding binding = SaveCredentialsDialogBinding.inflate(inflater, (ViewGroup) dialogView, false);
        builder.setView(binding.getRoot());

        builder.setCancelable(false); // Allow canceling by tapping outside
        AlertDialog dialog = builder.create();

        // Set up the button click listener
        binding.buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = binding.editTextUsername.getText().toString();
                String password = binding.editTextPassword.getText().toString();

                if (username.isEmpty() || password.isEmpty()) {
                    binding.textViewMessage.setText("Please fill in all fields.");
                } else {
                    // Save to SQLite
                    DatabaseHelper db = new DatabaseHelper(MainActivity.this);
                    db.addUser(username, password);
                    binding.textViewMessage.setText("Credentials saved!");
                    dialog.dismiss(); // Close dialog after saving
                    startLoginAttempt(username,password);
                }
            }
        });

        dialog.show();
    }
    public void getCredentials() {
        DatabaseHelper db = new DatabaseHelper(this);
        Cursor cursor = db.getUser();

        if (cursor.moveToFirst()) {



            startLoginAttempt(cursor.getString(cursor.getColumnIndexOrThrow("username")),cursor.getString(cursor.getColumnIndexOrThrow("password")));

            Toast.makeText(this, "Username: " + "\nPassword: ", Toast.LENGTH_LONG).show();
        } else {
            showCredentialSaveDialog();
            Toast.makeText(this, "No credentials found", Toast.LENGTH_SHORT).show();
        }
        cursor.close();
    }

    public void dismissCredentialDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss(); // Dismiss the dialog if it's currently showing
        }
    }





}