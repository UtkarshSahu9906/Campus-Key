package com.utkarshsahu.CampusKey;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class AutoLoginService extends Service {

    private static final String TAG = "CampusKey_AutoLogin";
    private static final String CHANNEL_ID = "AutoLoginChannel";
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private WifiLoginHelper wifiLoginHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, getNotification("Monitoring Wi-Fi connection..."));

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiLoginHelper = new WifiLoginHelper(this, new WifiLoginHelper.WifiLoginListener() {
            @Override public void onStatusUpdate(String status) { Log.d(TAG, "Status: " + status); }
            @Override public void onSuccess() { 
                Log.d(TAG, "Auto-login successful!"); 
                updateNotification("Connected to Wi-Fi successfully! ✅");
            }
            @Override public void onFailed(String reason) { Log.d(TAG, "Auto-login failed: " + reason); }
        });

        setupNetworkCallback();
    }

    private void setupNetworkCallback() {
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d(TAG, "Wi-Fi Available. Attempting auto-login...");
                attemptLogin();
            }
        };

        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    private void attemptLogin() {
        DatabaseHelper db = new DatabaseHelper(this);
        Cursor cursor = db.getUser();
        if (cursor.moveToFirst()) {
            String uname = cursor.getString(cursor.getColumnIndexOrThrow("username"));
            String pass = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            wifiLoginHelper.startLogin(uname, pass);
        }
        cursor.close();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Background Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification getNotification(String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("CampusKey Auto-Login")
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(String content) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(1, getNotification(content));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        if (wifiLoginHelper != null) wifiLoginHelper.destroyWebView();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
