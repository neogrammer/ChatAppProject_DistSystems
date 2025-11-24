package com.example.chatauth.fragment.chat;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
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
//            var args_bundle = getArguments();
//            if(args_bundle == null) throw new RuntimeException("Can't construct webview owner without args!");
//            args_bundle.setClassLoader(Arguments.class.getClassLoader());
//            var args = args_bundle.getParcelable("args", Arguments.class);
//            if(args == null) throw new RuntimeException("Can't construct webview owner without args!");
        }
        webview.addJavascriptInterface(new AndroidBridge(userId, userName), "AndroidBridge");
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
