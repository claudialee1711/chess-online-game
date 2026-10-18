package com.chessarena171.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

// Native Google account chooser for the Firebase-backed WebView app.
public class MainActivity extends Activity {
    private static final int RC_GOOGLE_SIGN_IN = 17101;
    private static final String HOME_URL = "https://claudialee1711.github.io/chess-online-game/";
    // OAuth client_type: 3 from app/google-services.json (Web application client ID).
    private static final String WEB_CLIENT_ID = "189359456636-h3ncol3l9bs512mnefjac724c75qbaql.apps.googleusercontent.com";
    private WebView webView;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(43, 27, 17));
        getWindow().setNavigationBarColor(Color.rgb(27, 17, 11));
        getWindow().getDecorView().setSystemUiVisibility(0);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(WEB_CLIENT_ID)
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(27, 17, 11));
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.addJavascriptInterface(new GoogleAuthBridge(), "AndroidGoogleAuth");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme();
                if (scheme.equals("https") || scheme.equals("http")) {
                    String host = uri.getHost() == null ? "" : uri.getHost();
                    if (host.equals("claudialee1711.github.io") ||
                        host.endsWith("firebaseapp.com") ||
                        host.endsWith("web.app") ||
                        host.equals("accounts.google.com") ||
                        host.endsWith("google.com") ||
                        host.endsWith("googleusercontent.com")) {
                        return false;
                    }
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                    catch (Exception ignored) { }
                    return true;
                }
                return true;
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(HOME_URL);
        }
    }

    private class GoogleAuthBridge {
        @JavascriptInterface
        public void signInWithGoogle() {
            runOnUiThread(() -> {
                try {
                    Intent signInIntent = googleSignInClient.getSignInIntent();
                    startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
                } catch (Exception e) {
                    sendAuthError("Tidak bisa membuka pemilih akun Google: " + e.getMessage());
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                String idToken = account.getIdToken();
                if (idToken == null || idToken.isEmpty()) {
                    sendAuthError("Token Google kosong. Periksa OAuth Web Client ID di Firebase.");
                } else {
                    String encoded = Base64.encodeToString(idToken.getBytes(java.nio.charset.StandardCharsets.UTF_8), Base64.NO_WRAP);
                    webView.post(() -> webView.evaluateJavascript(
                            "window.onNativeGoogleSignIn && window.onNativeGoogleSignIn(atob('" + encoded + "'));", null));
                }
            } catch (ApiException e) {
                Log.e("ChessArena171", "Google sign-in failed: " + e.getStatusCode(), e);
                sendAuthError("Login Google gagal (kode " + e.getStatusCode() + "). Coba pilih akun lagi.");
            }
        }
    }

    private void sendAuthError(String message) {
        if (webView == null) return;
        String encoded = Base64.encodeToString(message.getBytes(java.nio.charset.StandardCharsets.UTF_8), Base64.NO_WRAP);
        webView.post(() -> webView.evaluateJavascript(
                "window.onNativeGoogleSignInError && window.onNativeGoogleSignInError(atob('" + encoded + "'));", null));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}