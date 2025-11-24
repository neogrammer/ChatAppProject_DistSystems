package com.example.chatauth.fragment.chat;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.webkit.WebViewAssetLoader;

import java.util.ArrayList;

public class ChatWebviewOwnerFragment extends Fragment {
    public static final String TAG = "WebviewOwnerFragment";

    public void load(String userId, String userName) {
        if (webview == null) {
            Context ctx = requireContext().getApplicationContext();
            asset_loader = new WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(ctx))
                    .build();
            webview = new WebView(ctx); // use application context to prevent leaking the activity on config changes
            webview.getSettings().setJavaScriptEnabled(true);
            webview.setWebViewClient(new WebViewClient() {
                @Nullable
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    return asset_loader.shouldInterceptRequest(request.getUrl());
                }
            });

            webview.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    Log.d("ChatWebviewOwnerFragment", consoleMessage.message());
                    return true;
                }
            });
        }
        if(bridge != null) {
            if(bridge.userId.equals(userId) && bridge.userName.equals(userName)) return;
            webview.removeJavascriptInterface("AndroidBridge");
        }
        bridge = new AndroidBridge(userId, userName, () -> {
            for(WithWebviewCallback cb : pending_callbacks) cb.execute(webview);
            pending_callbacks.clear();
        });
        webview.addJavascriptInterface(bridge, "AndroidBridge");
        webview.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return null;
    }

    // executes callback on the ui thread with the webview after its done loading
    public void withLoadedWebview(WithWebviewCallback callback) {
        if(bridge.getLoaded()) bridge.handler.post(() -> callback.execute(webview));
        else pending_callbacks.add(callback);
    }

    private WebView webview;
    private WebViewAssetLoader asset_loader;
    private AndroidBridge bridge;
    private ArrayList<WithWebviewCallback> pending_callbacks = new ArrayList<>();

    @FunctionalInterface
    public interface WithWebviewCallback {
        void execute(WebView webview);
    }

    private static class AndroidBridge {
        private final String userId;
        private final String userName;

        private boolean loaded = false;
        private final Runnable loadCb;

        public AndroidBridge(String userId, String userName, Runnable loadCb) {
            this.userId = userId;
            this.userName = userName;
            this.loadCb = loadCb;
        }

        @JavascriptInterface
        public String getUserId() {
            return userId;
        }

        @JavascriptInterface
        public String getUserName() {
            return userName;
        }

        @JavascriptInterface
        public void setLoaded() {
            handler.post(() -> {
                if(loaded) return;
                loaded = true;
                loadCb.run();
            });
        }

        public boolean getLoaded() { return loaded; }

        public final Handler handler = new Handler(Looper.getMainLooper());
    }
}
