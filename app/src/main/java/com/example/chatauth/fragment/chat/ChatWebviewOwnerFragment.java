package com.example.chatauth.fragment.chat;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
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

import com.example.chatauth.MainActivity;
import com.example.chatauth.chat.ChatClient;
import com.example.chatauth.fragment.loading.LoadingDialogFragment;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import ink.bluballz.chat.v1.AddUserToGroupRequest;
import ink.bluballz.chat.v1.ChatMessage;
import ink.bluballz.chat.v1.CreateGroupRequest;
import ink.bluballz.chat.v1.GetMessagesRequest;
import ink.bluballz.chat.v1.GetUserGroupsRequest;

public class ChatWebviewOwnerFragment extends Fragment {
    public static final String TAG = "WebviewOwnerFragment";
    private static final String CHAT_SERVICE_HOST = "10.0.2.2"; // emulator to PC
    private static final int CHAT_SERVICE_PORT = 5065;

    private ChatClient chatClient;

    public void load(String userId, String userName, ChatWebviewFragment controller) {
        if (webview == null) {
            Context ctx = requireContext().getApplicationContext();
            asset_loader = new WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(ctx))
                    .build();
            webview = new WebView(ctx);
            webview.getSettings().setJavaScriptEnabled(true);
            webview.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
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
        bridge = new AndroidBridge(userId, userName, chatClient, this, () -> {
            for(WithWebviewCallback cb : pending_wv_callbacks) cb.execute(webview);
            pending_wv_callbacks.clear();
        }, controller);
        webview.addJavascriptInterface(bridge, "AndroidBridge");
        webview.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        //todo fail if chromium < 125.0
        Log.d("ChatWebviewOwnerFragment", "Using chromium version " + WebView.getCurrentWebViewPackage().versionName);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        chatClient = new ChatClient();
        chatClient.connect(CHAT_SERVICE_HOST, CHAT_SERVICE_PORT);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatClient != null) {
            chatClient.shutdown();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return null;
    }

    // executes callback on the ui thread with the webview after its done loading
    public void withLoadedWebview(WithWebviewCallback callback) {
        if(bridge.getLoaded()) bridge.handler.post(() -> callback.execute(webview));
        else pending_wv_callbacks.add(callback);
    }

    private WebView webview;
    private WebViewAssetLoader asset_loader;
    private AndroidBridge bridge;
    private final ArrayList<WithWebviewCallback> pending_wv_callbacks = new ArrayList<>();

    @FunctionalInterface
    public interface WithWebviewCallback {
        void execute(WebView webview);
    }

    private static class AndroidBridge {
        private final String userId;
        private final String userName;
        private final ChatClient chatClient;
        private final ChatWebviewOwnerFragment fragment;

        private boolean loaded = false;
        private final Runnable loadCb;
        private final WeakReference<ChatWebviewFragment> controller;

        public AndroidBridge(String userId, String userName, ChatClient chatClient, ChatWebviewOwnerFragment fragment, Runnable loadCb, ChatWebviewFragment controller) {
            this.userId = userId;
            this.userName = userName;
            this.chatClient = chatClient;
            this.fragment = fragment;
            this.loadCb = loadCb;
            this.controller = new WeakReference<>(controller);
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

        @JavascriptInterface
        public void showLoadingDialog() { handler.post(LoadingDialogFragment::show); }

        @JavascriptInterface
        public void hideLoadingDialog() { handler.post(LoadingDialogFragment::hide); }


        @JavascriptInterface
        public void postMessage(String b64_msg) {
            var bytes = Base64.decode(b64_msg, Base64.DEFAULT);
            ChatMessage msg = null;
            try {
                msg = ChatMessage.parseFrom(bytes);
            } catch (InvalidProtocolBufferException e) {
                Log.e(TAG, "Failed to parse message: " + e.getMessage());
                return;
            }

            // get access token
            String accessToken;
            try {
                var viewModel = ((MainActivity)fragment.requireActivity()).getViewModel();
                accessToken = viewModel.tokenStore.access();
            } catch (Exception e) {
                Log.e(TAG, "Failed to get access token: " + e.getMessage());
                return;
            }

            // send message via grpc
            chatClient.sendMessage(msg, accessToken, (response, error) -> {
                if (error != null) {
                    Log.e(TAG, "Failed to send message: " + error.getMessage());
                } else if (response != null && response.getSuccess()) {
                    Log.d(TAG, "Message sent successfully");
                } else {
                    Log.w(TAG, "Message send failed");
                }
            });
        }

        @JavascriptInterface
        public void requestMessageHistory(String b64_req, String request_id) {
            var bytes = Base64.decode(b64_req, Base64.DEFAULT);
            GetMessagesRequest req = null;
            try {
                req = GetMessagesRequest.parseFrom(bytes);
            } catch (InvalidProtocolBufferException e) {
                Log.e(TAG, "Failed to parse message history request: " + e.getMessage());
                return;
            }
            //todo Make the grpc call here. pass the response to this.resolvePromisedResponse(response, request_id) to update JS
            // or rejectPromisedResponse on error
        }

        @JavascriptInterface
        public void requestUserGroups(String request_id) {
            GetUserGroupsRequest req = GetUserGroupsRequest.newBuilder().setUserId(userId).build();
            //todo Make the grpc call here. pass the response to this.resolvePromisedResponse(response, request_id) to update JS
            // or rejectPromisedResponse on error
        }

        @JavascriptInterface
        public void searchUsers(String substring, String request_id) {
            //todo make the protobuf object

            //todo Make the grpc call here. pass the response to this.resolvePromisedResponse(response, request_id) to update JS
            // or rejectPromisedResponse on error
        }

        @JavascriptInterface
        public void createGroup(String name, String request_id) {
            CreateGroupRequest req = CreateGroupRequest.newBuilder().setGroupName(name).setUserId(userId).build();

            //todo Make the grpc call here. pass the response to this.resolvePromisedResponse(response, request_id) to update JS
            // or rejectPromisedResponse on error
        }

        public void addUserToGroup(String userId, String groupId, String request_id) {
            AddUserToGroupRequest req = AddUserToGroupRequest.newBuilder().setGroupId(groupId).setUserId(userId).build();

            //todo Make the grpc call here. pass the response to this.resolvePromisedResponse(response, request_id) to update JS
            // or rejectPromisedResponse on error
        }

        public boolean getLoaded() { return loaded; }

        private void resolvePromisedResponse(MessageLite response, String request_id) {
            handler.post(() -> {
                fragment.withLoadedWebview(webview -> {
                    String js = "window.Promiser.resolvePromise("
                            + jsBase64Arg(response)
                            + ",\""
                            + request_id
                            + "\")";
                    webview.evaluateJavascript(js, null);
                });
            });
        }

        private void rejectPromisedResponse(String error, String request_id) {
            handler.post(() -> {
                fragment.withLoadedWebview(webview -> {
                    String js = "window.Promiser.rejectPromise(\""
                            + error
                            + "\",\""
                            + request_id
                            + "\")";
                    webview.evaluateJavascript(js, null);
                });
            });
        }

        private static String jsBase64Arg(MessageLite proto) {
            // Base64 is JS-safe by definition
            return JSONObject.quote(Base64.encodeToString(proto.toByteArray(), Base64.NO_WRAP));
        }

        public final Handler handler = new Handler(Looper.getMainLooper());
    }
}
