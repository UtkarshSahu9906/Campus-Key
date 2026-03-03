package com.utkarsh.CampusKey;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FCMService extends FirebaseMessagingService {

    private static final String TAG        = "CampusKey_FCM";
    private static final String CHANNEL_ID = "campuskey_push";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        saveTokenToFirebase(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage msg) {
        super.onMessageReceived(msg);
        String title = "CampusKey";
        String body  = "";
        if (msg.getNotification() != null) {
            if (msg.getNotification().getTitle() != null) title = msg.getNotification().getTitle();
            if (msg.getNotification().getBody()  != null) body  = msg.getNotification().getBody();
        } else {
            title = msg.getData().getOrDefault("title", title);
            body  = msg.getData().getOrDefault("body", "");
        }
        showNotification(title, body);
    }

    private void showNotification(String title, String body) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "CampusKey Announcements", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
        PendingIntent pi = PendingIntent.getActivity(this, 0,
            new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title).setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pi).setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH).build();

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify((int) System.currentTimeMillis(), n);
    }

    private void saveTokenToFirebase(String token) {
        try {
            String deviceId = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ANDROID_ID);
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new Date());
            FirebaseDatabase.getInstance()
                .getReference("campuskey/users/" + deviceId + "/fcmToken").setValue(token);
            FirebaseDatabase.getInstance()
                .getReference("campuskey/users/" + deviceId + "/lastTokenUpdate").setValue(date);
        } catch (Exception e) {
            Log.e(TAG, "Token save failed: " + e.getMessage());
        }
    }
}
