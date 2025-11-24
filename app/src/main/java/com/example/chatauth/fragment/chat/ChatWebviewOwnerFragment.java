package com.example.chatauth.fragment.chat;

import android.content.Context;
import android.os.Bundle;
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

public class ChatWebviewOwnerFragment extends Fragment {
    public static final String TAG = "WebviewOwnerFragment";

    public WebView load(String userId, String userName) {
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
            if(bridge.userId.equals(userId) && bridge.userName.equals(userName)) return webview;
            webview.removeJavascriptInterface("AndroidBridge");
        }
        bridge = new AndroidBridge(userId, userName);
        webview.addJavascriptInterface(bridge, "AndroidBridge");
        webview.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        return webview;
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

    private WebView webview;
    private WebViewAssetLoader asset_loader;
    private AndroidBridge bridge;

    private static class AndroidBridge {
        private final String userId;
        private final String userName;

        public AndroidBridge(String userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }

        @JavascriptInterface
        public String getUserId() {
            return userId;
        }

        @JavascriptInterface
        public String getUserName() {
            return userName;
        }
    }
}
