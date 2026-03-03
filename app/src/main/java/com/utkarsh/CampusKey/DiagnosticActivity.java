package com.utkarsh.CampusKey;

import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiagnosticActivity extends AppCompatActivity {

    private static final String PORTAL_URL  = "http://172.24.64.1:8090/httpclient.html";
    private static final String PORTAL_HOST = "172.24.64.1";

    private TextView logView;
    private ScrollView scrollView;
    private WebView diagWebView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Network wifiNetwork = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(24, 48, 24, 24);
        root.setBackgroundColor(0xFF0D1117);

        TextView title = new TextView(this);
        title.setText("CampusKey Diagnostics");
        title.setTextSize(18f);
        title.setTextColor(0xFFFFFFFF);
        title.setPadding(0, 0, 0, 16);
        root.addView(title);

        Button btnRun = new Button(this);
        btnRun.setText("Run Full Diagnostic");
        root.addView(btnRun);

        Button btnLogin = new Button(this);
        btnLogin.setText("Test Login Only");
        root.addView(btnLogin);

        scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xFF161B22);
        android.widget.LinearLayout.LayoutParams lp =
                new android.widget.LinearLayout.LayoutParams(-1, 0, 1f);
        lp.topMargin = 16;
        scrollView.setLayoutParams(lp);

        logView = new TextView(this);
        logView.setTextColor(0xFF00FF88);
        logView.setTextSize(11f);
        logView.setTypeface(android.graphics.Typeface.MONOSPACE);
        logView.setPadding(16, 16, 16, 16);
        scrollView.addView(logView);
        root.addView(scrollView);

        diagWebView = new WebView(this);
        diagWebView.setVisibility(android.view.View.GONE);
        root.addView(diagWebView);

        setContentView(root);

        btnRun.setOnClickListener(v -> runFullDiagnostic());
        btnLogin.setOnClickListener(v -> testLoginOnly());
    }

    private void runFullDiagnostic() {
        clearLog();
        log("=== CampusKey Diagnostic ===");

        // 1. Credentials
        log("\n1. Checking saved credentials...");
        DatabaseHelper db = new DatabaseHelper(this);
        Cursor c = db.getUser();
        if (c.moveToFirst()) {
            String user = c.getString(c.getColumnIndexOrThrow("username"));
            log("   OK - Username: " + user);
        } else {
            log("   FAIL - No credentials saved!");
        }
        c.close();

        // 2. Network
        log("\n2. Checking network...");
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        Network activeNet = cm.getActiveNetwork();
        NetworkCapabilities caps = cm.getNetworkCapabilities(activeNet);
        if (caps == null) {
            log("   No active network!");
        } else {
            log("   WiFi: " + caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
            log("   Mobile: " + caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        }

        wifiNetwork = null;
        for (Network net : cm.getAllNetworks()) {
            NetworkCapabilities nc = cm.getNetworkCapabilities(net);
            if (nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                wifiNetwork = net;
                log("   WiFi network object: " + net);
                break;
            }
        }
        if (wifiNetwork == null) log("   No WiFi network object found");

        // 3. HTTP checks
        log("\n3. Testing portal connectivity...");
        executor.execute(() -> {
            testHttp(PORTAL_URL, "GET");
            testHttp("http://" + PORTAL_HOST + ":8090/", "GET");
            testHttp("http://" + PORTAL_HOST + "/", "GET");
            testSocket(8090);
            testSocket(80);
            mainHandler.post(() -> {
                log("\n4. Loading portal in WebView...");
                testWebView();
            });
        });
    }

    private void testHttp(String urlStr, String method) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn;
            if (wifiNetwork != null) {
                conn = (HttpURLConnection) wifiNetwork.openConnection(url);
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod(method);
            conn.setInstanceFollowRedirects(false);
            int code = conn.getResponseCode();
            String loc = conn.getHeaderField("Location");
            conn.disconnect();
            mainHandler.post(() -> log("   [" + method + "] " + urlStr
                    + " -> " + code + (loc != null ? " -> " + loc : "")));
        } catch (IOException e) {
            mainHandler.post(() -> log("   [" + method + "] " + urlStr + " -> FAIL: " + e.getMessage()));
        }
    }

    private void testSocket(int port) {
        try {
            java.net.Socket s = new java.net.Socket();
            if (wifiNetwork != null) wifiNetwork.bindSocket(s);
            s.connect(new java.net.InetSocketAddress(PORTAL_HOST, port), 5000);
            s.close();
            mainHandler.post(() -> log("   Socket port " + port + ": OPEN"));
        } catch (IOException e) {
            mainHandler.post(() -> log("   Socket port " + port + ": CLOSED - " + e.getMessage()));
        }
    }

    private void testWebView() {
        diagWebView.getSettings().setJavaScriptEnabled(true);
        diagWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        diagWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                log("   WebView loaded: " + url);
                view.evaluateJavascript(
                    "(function(){" +
                        "var inputs=document.querySelectorAll('input,button');" +
                        "var r=[];" +
                        "inputs.forEach(function(e){r.push(e.tagName+'#'+e.id+'['+e.type+'] name='+e.name);});" +
                        "return JSON.stringify(r);" +
                    "})();",
                    result -> log("   Elements: " + result)
                );
                view.evaluateJavascript("document.title;",
                    t -> log("   Title: " + t));
            }
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                log("   WebView ERROR: " + err.getDescription() + " on " + req.getUrl());
            }
        });
        diagWebView.loadUrl(PORTAL_URL);
    }

    private void testLoginOnly() {
        clearLog();
        log("=== Login Test ===");

        DatabaseHelper db = new DatabaseHelper(this);
        Cursor c = db.getUser();
        if (!c.moveToFirst()) {
            log("No credentials saved!");
            c.close();
            return;
        }
        String username = c.getString(c.getColumnIndexOrThrow("username"));
        String password = c.getString(c.getColumnIndexOrThrow("password"));
        c.close();
        log("Username: " + username);
        log("Loading portal...");

        diagWebView.getSettings().setJavaScriptEnabled(true);
        diagWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        final boolean[] submitted = {false};

        diagWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                log("\nPage loaded: " + url);
                if (!submitted[0]) {
                    submitted[0] = true;
                    String u = username.replace("\\","\\\\").replace("'","\\'");
                    String p = password.replace("\\","\\\\").replace("'","\\'");
                    // First dump form elements
                    view.evaluateJavascript(
                        "(function(){" +
                            "var inputs=document.querySelectorAll('input');" +
                            "var r=[];" +
                            "inputs.forEach(function(i){r.push(i.id+'|'+i.name+'|'+i.type);});" +
                            "var btns=document.querySelectorAll('button,input[type=submit]');" +
                            "btns.forEach(function(b){r.push('BTN:'+b.id+'|'+b.name+'|'+b.type);});" +
                            "return JSON.stringify(r);" +
                        "})();",
                        result -> {
                            log("Form elements: " + result);
                            // Now fill and submit - tries multiple selector strategies
                            view.evaluateJavascript(
                                "(function(){" +
                                    "var u=document.getElementById('username')||document.querySelector('[name=username],[name=user],[type=text]');" +
                                    "var pw=document.getElementById('password')||document.querySelector('[name=password],[type=password]');" +
                                    "var btn=document.getElementById('loginbutton')||document.querySelector('[type=submit],button');" +
                                    "if(!u) return 'NO_USER_FIELD';" +
                                    "if(!pw) return 'NO_PASS_FIELD';" +
                                    "if(!btn) return 'NO_BUTTON';" +
                                    "u.value='" + u + "';" +
                                    "pw.value='" + p + "';" +
                                    "btn.click();" +
                                    "return 'SUBMITTED btn.id='+btn.id+' btn.type='+btn.type;" +
                                "})();",
                                res -> log("Submit result: " + res)
                            );
                        }
                    );
                } else {
                    log("\n*** Page changed after submit! ***");
                    log("New URL: " + url);
                    view.evaluateJavascript(
                        "document.body ? document.body.innerText.substring(0,300) : 'no body';",
                        body -> log("Page content: " + body)
                    );
                }
            }
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                log("ERROR: " + err.getDescription() + " | " + req.getUrl());
            }
        });
        diagWebView.loadUrl(PORTAL_URL);
    }

    private void log(String msg) {
        Log.d("CampusKey_DIAG", msg);
        mainHandler.post(() -> {
            logView.append(msg + "\n");
            scrollView.post(() -> scrollView.fullScroll(android.view.View.FOCUS_DOWN));
        });
    }

    private void clearLog() { logView.setText(""); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        if (diagWebView != null) diagWebView.destroy();
    }
}
