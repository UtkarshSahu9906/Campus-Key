package com.utkarsh.CampusKey;

import static com.utkarsh.CampusKey.LocalData.DESCRIPTION;
import static com.utkarsh.CampusKey.LocalData.FORCE;
import static com.utkarsh.CampusKey.LocalData.MAIN_TEXT;
import static com.utkarsh.CampusKey.LocalData.NETWORK_ERROR;
import static com.utkarsh.CampusKey.LocalData.URL;

import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.utkarsh.CampusKey.databinding.ActivityMainBinding;
import com.utkarsh.CampusKey.databinding.SaveCredentialsDialogBinding;
import com.utkarsh.CampusKey.databinding.VersionCheckDialogBinding;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private AlertDialog dialog; // Class-level variable for dialog
    private boolean loginAttempted = false;
    private boolean isGo = false; // Class-level variable

    ActivityMainBinding binding;
    private boolean isNetworkError =false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        LocalData data = new LocalData(MainActivity.this);
        if (AppUtils.getVersionCode(MainActivity.this) < data.getInt(LocalData.MINIMUM_VERSION_CODE)) {
            showUpdateDialog(data.getString(URL),data.getString(MAIN_TEXT),data.getString(DESCRIPTION),data.getBoolean(FORCE));
        } else  {
            getCredentials();
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }



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
                       goToNext();
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

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                isNetworkError=true;


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
                    startLoginAttempt(username, password);
                }
            }
        });

        dialog.show();
    }

    public void getCredentials() {
        DatabaseHelper db = new DatabaseHelper(this);
        Cursor cursor = db.getUser();

        if (cursor.moveToFirst()) {
            startLoginAttempt(cursor.getString(cursor.getColumnIndexOrThrow("username")), cursor.getString(cursor.getColumnIndexOrThrow("password")));

        } else {
            showCredentialSaveDialog();

        }
        cursor.close();
    }



    private void showUpdateDialog(String updateUrl, String main_Text, String description, boolean forceUpdate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflate the dialog layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.version_check_dialog, null);

        // Initialize View Binding
        VersionCheckDialogBinding binding = VersionCheckDialogBinding.inflate(inflater, (ViewGroup) dialogView, false);
        builder.setView(binding.getRoot());

        builder.setCancelable(false); // Allow canceling by tapping outside
        AlertDialog dialog = builder.create();



        // Set the main text and description
        binding.mainText.setText(main_Text);
        binding.descriptionText.setText(description);

        if(!forceUpdate){
            binding.buttonCancel.setVisibility(View.VISIBLE);
        }

        // Set the click listeners for the buttons
        binding.buttonCancel.setOnClickListener(v -> {
            // Close the dialog if the user clicks "Cancel"
            dialog.dismiss();
            getCredentials();
        });

        binding.buttonUpdate.setOnClickListener(v -> {
            // Open the update URL in the browser when the "Update" button is clicked
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
            startActivity(browserIntent);
            dialog.dismiss();
        });


        dialog.show();
    }

    void goToNext() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent intent = new Intent(MainActivity.this, SuccessActivity.class);
                intent.putExtra(NETWORK_ERROR, isNetworkError);
                startActivity(intent);
                finish(); // Close MainActivity if you don't want to return to it

            }
        }, 200);
    }


}