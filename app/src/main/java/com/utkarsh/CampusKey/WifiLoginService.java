package com.utkarsh.CampusKey;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WifiLoginService extends Service {

    private static final String TAG        = "CampusKey";
    private static final String PORTAL_URL = "http://172.24.64.1:8090/httpclient.html";

    public  static final String CHANNEL_ID       = "campuskey_channel";
    public  static final String ACTION_CONNECTED = "com.utkarsh.CampusKey.CONNECTED";
    public  static final String ACTION_FAILED    = "com.utkarsh.CampusKey.FAILED";

    private static final int NOTIF_ID_RUNNING = 1;
    private static final int NOTIF_ID_RESULT  = 2;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private volatile Network activeWifiNetwork = null;

    private WebView  loginWebView;
    private boolean  pageLoaded   = false;
    private boolean  loginDone    = false;
    private boolean  loginSuccess = false;

    private final Handler         mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor    = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIF_ID_RUNNING, buildRunningNotif("Watching for college Wi-Fi..."));
        startWifiMonitoring();
    }

    @Override public int onStartCommand(Intent i, int f, int s) { return START_STICKY; }
    @Nullable @Override public IBinder onBind(Intent i) { return null; }

    // ─────────────────────────────────────────────
    // WIFI MONITORING — no socket check, go straight to WebView
    // ─────────────────────────────────────────────
    private void startWifiMonitoring() {
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(@NonNull Network network) {
                activeWifiNetwork = network;
                Log.d(TAG, "WiFi available: " + network);
                updateNotif("Wi-Fi connected. Logging in...");
                mainHandler.postDelayed(() -> startLogin(), 2000);
            }
            @Override public void onLost(@NonNull Network network) {
                Log.d(TAG, "WiFi lost");
                resetState();
                updateNotif("Watching for college Wi-Fi...");
            }
        };

        connectivityManager.registerNetworkCallback(
            new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
            networkCallback);

        Network current = getWifiNetwork();
        if (current != null) {
            activeWifiNetwork = current;
            mainHandler.postDelayed(() -> startLogin(), 1500);
        }
    }

    private Network getWifiNetwork() {
        if (connectivityManager == null) return null;
        for (Network net : connectivityManager.getAllNetworks()) {
            NetworkCapabilities nc = connectivityManager.getNetworkCapabilities(net);
            if (nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return net;
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────
    private void startLogin() {
        if (loginDone) return;

        Cursor cursor = new DatabaseHelper(this).getUser();
        if (!cursor.moveToFirst()) {
            cursor.close();
            showResultNotif(false, "Open CampusKey to save your Wi-Fi credentials.");
            return;
        }
        final String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
        final String password = cursor.getString(cursor.getColumnIndexOrThrow("password"));
        cursor.close();

        Log.d(TAG, "Logging in: " + username);

        mainHandler.post(() -> {
            destroyWebView();
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();

            loginWebView = new WebView(getApplicationContext());
            WebSettings ws = loginWebView.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
            ws.setDomStorageEnabled(true);
            ws.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.6099.210 Mobile Safari/537.36");

            loginWebView.addJavascriptInterface(new JsBridge(), "CKBridge");

            loginWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    Log.d(TAG, "Page: " + url);
                    if (loginDone) return;
                    if (!pageLoaded) {
                        pageLoaded = true;
                        mainHandler.postDelayed(() -> {
                            if (!loginDone) injectCredentials(view, username, password);
                        }, 500);
                    }
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest req,
                                            WebResourceError err) {
                    super.onReceivedError(view, req, err);
                    if (req != null && req.isForMainFrame() && !loginDone) {
                        Log.w(TAG, "WebView error: " + err.getDescription());
                        pageLoaded = false;
                        mainHandler.postDelayed(() -> {
                            if (!loginDone && loginWebView != null)
                                loginWebView.loadUrl(PORTAL_URL);
                        }, 4000);
                    }
                }
            });

            loginWebView.loadUrl(PORTAL_URL);

            // Check internet 10 seconds after loading starts
            mainHandler.postDelayed(() -> {
                if (!loginDone) {
                    loginDone = true;
                    executor.execute(this::checkInternetAndFinish);
                }
            }, 10000);
        });
    }

    // ─────────────────────────────────────────────
    // JS INJECTION
    // Confirmed fields: id=username, id=password, id=loginbutton
    // ─────────────────────────────────────────────
    private void injectCredentials(WebView view, String username, String password) {
        String u = escapejs(username);
        String p = escapejs(password);

        String js =
            // XHR interceptor
            "(function(){" +
                "var oO=XMLHttpRequest.prototype.open,oS=XMLHttpRequest.prototype.send;" +
                "XMLHttpRequest.prototype.open=function(m,u){this._m=m;this._u=u;return oO.apply(this,arguments);};" +
                "XMLHttpRequest.prototype.send=function(b){" +
                    "var s=this;" +
                    "this.addEventListener('load',function(){" +
                        "try{CKBridge.onXhr(s._m||'',s._u||'',(b||'').toString(),s.status,s.responseText);}catch(e){}" +
                    "});" +
                    "return oS.apply(this,arguments);" +
                "};" +
            "})();" +
            // Fill + submit
            "(function(){" +
                "var u=document.getElementById('username');" +
                "var pw=document.getElementById('password');" +
                "var btn=document.getElementById('loginbutton');" +
                "if(!u||!pw){CKBridge.onLog('NO_FIELDS:'+document.body.innerHTML.substring(0,200));return;}" +
                "u.value='" + u + "';" +
                "pw.value='" + p + "';" +
                "['focus','input','change','blur'].forEach(function(ev){" +
                    "u.dispatchEvent(new Event(ev,{bubbles:true}));" +
                    "pw.dispatchEvent(new Event(ev,{bubbles:true}));" +
                "});" +
                "CKBridge.onLog('FILLED cookie='+document.cookie);" +
                "setTimeout(function(){" +
                    "if(btn){btn.click();CKBridge.onLog('CLICKED id='+btn.id);}" +
                    "else{var f=document.querySelector('form');if(f)f.submit();}" +
                "},300);" +
            "})();";

        view.evaluateJavascript(js, r -> Log.d(TAG, "JS: " + r));
    }

    // ─────────────────────────────────────────────
    // JS BRIDGE
    // ─────────────────────────────────────────────
    private class JsBridge {
        @JavascriptInterface
        public void onLog(String msg) { Log.d(TAG, "[JS] " + msg); }

        @JavascriptInterface
        public void onXhr(String method, String url, String body, int status, String response) {
            Log.d(TAG, "[XHR] " + method + " " + url + " status=" + status);
            if (response == null || loginDone) return;
            String r = response.toLowerCase();
            boolean ok   = status == 200 && (r.contains("success") || r.contains("welcome") ||
                           r.contains("connected") || r.contains("logout") || r.contains("true"));
            boolean fail = r.contains("invalid") || r.contains("incorrect") || r.contains("wrong");

            if (ok) {
                mainHandler.post(() -> {
                    if (!loginDone) {
                        loginDone = true; loginSuccess = true;
                        executor.execute(WifiLoginService.this::checkInternetAndFinish);
                    }
                });
            } else if (fail) {
                mainHandler.post(() -> {
                    if (!loginDone) { loginDone = true; onFail("Wrong username or password."); }
                });
            }
        }
    }

    // ─────────────────────────────────────────────
    // INTERNET CHECK — tries college domain first, then Google
    // ─────────────────────────────────────────────
    private void checkInternetAndFinish() {
        Network net = activeWifiNetwork;
        String[] urls = {
            "http://kalingauniversity.ac.in",
            "http://www.gstatic.com/generate_204",
            "http://connectivitycheck.gstatic.com/generate_204",
            "http://www.google.com",
            "http://www.baidu.com"
        };
        for (String urlStr : urls) {
            try {
                HttpURLConnection c;
                if (net != null) c = (HttpURLConnection) net.openConnection(new URL(urlStr));
                else             c = (HttpURLConnection) new URL(urlStr).openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                c.setRequestMethod("HEAD");
                c.setInstanceFollowRedirects(true);
                int code = c.getResponseCode();
                c.disconnect();
                Log.d(TAG, "Internet check " + urlStr + " -> " + code);
                if (code >= 200 && code < 500) {
                    mainHandler.post(this::onSuccess);
                    return;
                }
            } catch (IOException e) {
                Log.w(TAG, "Check failed " + urlStr + ": " + e.getMessage());
            }
        }
        if (loginSuccess) mainHandler.post(this::onSuccess);
        else              mainHandler.post(() -> onFail("No internet after login."));
    }

    // ─────────────────────────────────────────────
    // RESULTS
    // ─────────────────────────────────────────────
    private void onSuccess() {
        Log.d(TAG, "SUCCESS");
        updateNotif("Connected to college Wi-Fi!");
        Intent intent = new Intent(this, ConnectedActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        sendBroadcast(new Intent(ACTION_CONNECTED));
        showResultNotif(true, "Tap to view connection details");
        // Record login in analytics
        new AnalyticsHelper(this).recordLaunch();
        cleanup();
    }

    private void onFail(String reason) {
        Log.w(TAG, "FAIL: " + reason);
        updateNotif("Login failed. Will retry.");
        showResultNotif(false, reason);
        resetState();
        cleanup();
    }

    private void resetState()  { pageLoaded = false; loginDone = false; loginSuccess = false; }

    private void destroyWebView() {
        if (loginWebView != null) {
            loginWebView.removeJavascriptInterface("CKBridge");
            loginWebView.stopLoading();
            loginWebView.destroy();
            loginWebView = null;
        }
    }

    private void cleanup() { mainHandler.post(this::destroyWebView); }

    // ─────────────────────────────────────────────
    // NOTIFICATIONS
    // ─────────────────────────────────────────────
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "CampusKey Wi-Fi", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Auto Wi-Fi login");
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private Notification buildRunningNotif(String text) {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
            new Intent(this, ConnectedActivity.class), PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CampusKey").setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher).setContentIntent(pi)
            .setOngoing(true).setPriority(NotificationCompat.PRIORITY_LOW).build();
    }

    private void updateNotif(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID_RUNNING, buildRunningNotif(text));
    }

    private void showResultNotif(boolean ok, String msg) {
        Intent target = new Intent(this, ok ? ConnectedActivity.class : MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, target,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(ok ? "✅ Wi-Fi Connected!" : "❌ Login Failed")
            .setContentText(msg)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
            .setSmallIcon(R.mipmap.ic_launcher).setContentIntent(pi)
            .setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH).build();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID_RESULT, n);
    }

    private static String escapejs(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("'","\\'").replace("\n","").replace("\r","");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (networkCallback != null && connectivityManager != null) {
            try { connectivityManager.unregisterNetworkCallback(networkCallback); }
            catch (Exception ignored) {}
        }
        executor.shutdown();
        mainHandler.removeCallbacksAndMessages(null);
        cleanup();
    }
}
