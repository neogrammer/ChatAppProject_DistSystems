package com.example.chatauth.fragment.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class WebviewOwnerFragment extends Fragment {
    public WebView webview;
    public static final String TAG = "WebviewOwnerFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (webview == null) {
            webview = new WebView(requireContext().getApplicationContext()); // use application context to prevent leaking the activity on config changes
            webview.getSettings().setJavaScriptEnabled(true);
            webview.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return null;
    }
}
