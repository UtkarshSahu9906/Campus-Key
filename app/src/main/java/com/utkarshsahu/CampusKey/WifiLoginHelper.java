package com.utkarshsahu.CampusKey;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WifiLoginHelper {

    private static final String TAG        = "CampusKey_Helper";
    private static final String PORTAL_URL = "http://172.24.64.1:8090/httpclient.html";

    public interface WifiLoginListener {
        void onStatusUpdate(String status);
        void onSuccess();
        void onFailed(String reason);
    }

    private final Context context;
    private final WifiLoginListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private WebView loginWebView;
    private boolean pageLoaded = false;
    private boolean loginDone = false;
    private boolean loginSuccess = false;
    private boolean connecting = false;

    public WifiLoginHelper(Context context, WifiLoginListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void startLogin(final String username, final String password) {
        if (connecting) {
            Log.d(TAG, "Login already in progress, skipping.");
            return;
        }
        
        if (!isWifiConnected()) {
            mainHandler.post(() -> listener.onFailed("Wi-Fi is not connected. Please turn on Wi-Fi."));
            return;
        }
        
        loginDone = false;
        loginSuccess = false;
        pageLoaded = false;
        connecting = true;

        mainHandler.post(() -> {
            listener.onStatusUpdate("Initializing portal...");
            destroyWebView();
            
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();

            // Background login: Use application context to avoid leaks/crashes
            loginWebView = new WebView(context.getApplicationContext());
            WebSettings ws = loginWebView.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
            ws.setDomStorageEnabled(true);
            ws.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.6099.210 Mobile Safari/537.36");

            // Avoid renderer crashes in background: Use software layer or none
            loginWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            loginWebView.addJavascriptInterface(new JsBridge(), "CKBridge");

            loginWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    Log.d(TAG, "Page: " + url);
                    if (loginDone) return;
                    if (!pageLoaded) {
                        pageLoaded = true;
                        listener.onStatusUpdate("Injecting credentials...");
                        // Speed: Instant injection without artificial delay
                        injectCredentials(view, username, password);
                    }
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                    super.onReceivedError(view, req, err);
                    if (req != null && req.isForMainFrame() && !loginDone) {
                        Log.w(TAG, "WebView error: " + err.getDescription());
                        pageLoaded = false;
                        listener.onStatusUpdate("Portal error, retrying...");
                        mainHandler.postDelayed(() -> {
                            if (!loginDone && loginWebView != null)
                                loginWebView.loadUrl(PORTAL_URL);
                        }, 2500);
                    }
                }
            });

            loginWebView.loadUrl(PORTAL_URL);

            // Timeout check (reduced from 10s to 7s)
            mainHandler.postDelayed(() -> {
                if (!loginDone) {
                    loginDone = true;
                    connecting = false;
                    listener.onStatusUpdate("Checking internet...");
                    executor.execute(WifiLoginHelper.this::checkInternetAndFinish);
                }
            }, 7000);
        });
    }

    private void injectCredentials(WebView view, String username, String password) {
        String u = escapejs(username);
        String p = escapejs(password);

        String js =
            "(function(){" +
                "if(window._ck_injected)return;" +
                "window._ck_injected=true;" +
                "var oO=XMLHttpRequest.prototype.open,oS=XMLHttpRequest.prototype.send;" +
                "XMLHttpRequest.prototype.open=function(m,u){this._m=m;this._u=u;return oO.apply(this,arguments);};" +
                "XMLHttpRequest.prototype.send=function(b){" +
                    "var s=this;" +
                    "this.addEventListener('load',function(){" +
                        "try{CKBridge.onXhr(s._m||'',s._u||'',(b||'').toString(),s.status,s.responseText);}catch(e){}" +
                    "});" +
                    "return oS.apply(this,arguments);" +
                "};" +
                "function attempt(){" +
                "  var uField=document.getElementById('username');" +
                "  var pField=document.getElementById('password');" +
                "  var btn=document.getElementById('loginbutton');" +
                "  if(uField && pField){" +
                "    uField.value='" + u + "';" +
                "    pField.value='" + p + "';" +
                "    ['input','change','blur'].forEach(function(ev){" +
                "      uField.dispatchEvent(new Event(ev,{bubbles:true}));" +
                "      pField.dispatchEvent(new Event(ev,{bubbles:true}));" +
                "    });" +
                "    setTimeout(function(){" +
                "      if(btn){btn.click();}else{var f=document.querySelector('form');if(f)f.submit();}" +
                "    },50);" +
                "    return true;" +
                "  }" +
                "  return false;" +
                "}" +
                "if(!attempt()){" +
                "  var itv=setInterval(function(){ if(attempt()) clearInterval(itv); }, 50);" +
                "  setTimeout(function(){ clearInterval(itv); }, 5000);" +
                "}" +
            "})();";

        if (view != null) {
            view.evaluateJavascript(js, r -> Log.d(TAG, "JS Injection result: " + r));
        }
    }

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
                        connecting = false;
                        listener.onStatusUpdate("Verifying connection...");
                        executor.execute(WifiLoginHelper.this::checkInternetAndFinish);
                    }
                });
            } else if (fail) {
                mainHandler.post(() -> {
                    if (!loginDone) { 
                        loginDone = true; 
                        connecting = false;
                        listener.onFailed("Invalid credentials."); 
                    }
                });
            }
        }
    }

    private void checkInternetAndFinish() {
        if (!isWifiConnected()) {
            mainHandler.post(() -> {
                connecting = false;
                listener.onFailed("Wi-Fi is not connected.");
            });
            return;
        }

        String[] urls = {
            "http://www.gstatic.com/generate_204",
            "http://kalingauniversity.ac.in",
            "http://www.google.com"
        };
        for (String urlStr : urls) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
                c.setConnectTimeout(2500);
                c.setReadTimeout(2500);
                c.setRequestMethod("HEAD");
                int code = c.getResponseCode();
                c.disconnect();
                if (code >= 200 && code < 500) {
                    mainHandler.post(() -> {
                        connecting = false;
                        listener.onSuccess();
                    });
                    return;
                }
            } catch (IOException ignored) {}
        }
        mainHandler.post(() -> {
            connecting = false;
            if (loginSuccess) {
                listener.onSuccess();
            } else {
                listener.onFailed("No internet connection.");
            }
        });
    }

    public void destroyWebView() {
        if (loginWebView != null) {
            loginWebView.removeJavascriptInterface("CKBridge");
            loginWebView.stopLoading();
            loginWebView.destroy();
            loginWebView = null;
        }
    }

    public void cancelLogin() {
        connecting = false;
        loginDone = true;
        mainHandler.removeCallbacksAndMessages(null);
        destroyWebView();
    }

    private static String escapejs(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("'","\\'").replace("\n","").replace("\r","");
    }

    private boolean isWifiConnected() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                for (android.net.Network network : cm.getAllNetworks()) {
                    android.net.NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                    if (capabilities != null && capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
                        return true;
                    }
                }
                return false;
            } else {
                android.net.NetworkInfo wifiInfo = cm.getNetworkInfo(android.net.ConnectivityManager.TYPE_WIFI);
                return wifiInfo != null && wifiInfo.isConnected();
            }
        }
        return false;
    }
}
