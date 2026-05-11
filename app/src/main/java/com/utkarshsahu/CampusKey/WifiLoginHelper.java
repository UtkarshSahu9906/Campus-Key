package com.utkarshsahu.CampusKey;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

    public WifiLoginHelper(Context context, WifiLoginListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void startLogin(final String username, final String password) {
        if (loginDone) return;
        
        loginDone = false;
        loginSuccess = false;
        pageLoaded = false;

        mainHandler.post(() -> {
            listener.onStatusUpdate("Initializing portal...");
            destroyWebView();
            
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();

            loginWebView = new WebView(context);
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
                        listener.onStatusUpdate("Injecting credentials...");
                        mainHandler.postDelayed(() -> {
                            if (!loginDone) injectCredentials(view, username, password);
                        }, 500);
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
                        }, 4000);
                    }
                }
            });

            loginWebView.loadUrl(PORTAL_URL);

            // Timeout check
            mainHandler.postDelayed(() -> {
                if (!loginDone) {
                    loginDone = true;
                    listener.onStatusUpdate("Checking internet...");
                    executor.execute(WifiLoginHelper.this::checkInternetAndFinish);
                }
            }, 12000);
        });
    }

    private void injectCredentials(WebView view, String username, String password) {
        String u = escapejs(username);
        String p = escapejs(password);

        String js =
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
            "(function(){" +
                "var u=document.getElementById('username');" +
                "var pw=document.getElementById('password');" +
                "var btn=document.getElementById('loginbutton');" +
                "if(!u||!pw){CKBridge.onLog('NO_FIELDS');return;}" +
                "u.value='" + u + "';" +
                "pw.value='" + p + "';" +
                "['focus','input','change','blur'].forEach(function(ev){" +
                    "u.dispatchEvent(new Event(ev,{bubbles:true}));" +
                    "pw.dispatchEvent(new Event(ev,{bubbles:true}));" +
                "});" +
                "setTimeout(function(){" +
                    "if(btn){btn.click();}else{var f=document.querySelector('form');if(f)f.submit();}" +
                "},300);" +
            "})();";

        view.evaluateJavascript(js, r -> Log.d(TAG, "JS Injection result: " + r));
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
                        listener.onStatusUpdate("Verifying connection...");
                        executor.execute(WifiLoginHelper.this::checkInternetAndFinish);
                    }
                });
            } else if (fail) {
                mainHandler.post(() -> {
                    if (!loginDone) { loginDone = true; listener.onFailed("Invalid credentials."); }
                });
            }
        }
    }

    private void checkInternetAndFinish() {
        String[] urls = {
            "http://kalingauniversity.ac.in",
            "http://www.gstatic.com/generate_204",
            "http://www.google.com"
        };
        for (String urlStr : urls) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                c.setRequestMethod("HEAD");
                int code = c.getResponseCode();
                c.disconnect();
                if (code >= 200 && code < 500) {
                    mainHandler.post(listener::onSuccess);
                    return;
                }
            } catch (IOException ignored) {}
        }
        if (loginSuccess) mainHandler.post(listener::onSuccess);
        else              mainHandler.post(() -> listener.onFailed("No internet connection."));
    }

    public void destroyWebView() {
        if (loginWebView != null) {
            loginWebView.removeJavascriptInterface("CKBridge");
            loginWebView.stopLoading();
            loginWebView.destroy();
            loginWebView = null;
        }
    }

    private static String escapejs(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("'","\\'").replace("\n","").replace("\r","");
    }
}
