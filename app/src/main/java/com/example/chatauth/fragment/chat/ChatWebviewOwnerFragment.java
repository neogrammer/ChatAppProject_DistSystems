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
import ink.bluballz.chat.v1.SearchUsersRequest;

/**
 * A headless {@link Fragment} that owns and manages the lifecycle of a single {@link WebView} instance
 * used for the chat interface. This fragment is retained across configuration changes
 * (e.g., screen rotation) to preserve the WebView's state and the active gRPC connection,
 * preventing the need to reload the web application and re-establish the connection.
 * <p>
 * The primary responsibilities of this fragment are:
 * <ul>
 *   <li>Initializing and configuring the {@link WebView} with a local asset loader for the chat web app.</li>
 *   <li>Managing a {@link ChatClient} for communication with the backend gRPC chat service.</li>
 *   <li>Setting up an {@link AndroidBridge} to facilitate communication between the JavaScript running in the WebView and the native Android code.</li>
 *   <li>Handling the lifecycle of the WebView and the gRPC client, ensuring they are created and shut down correctly.</li>
 *   <li>Providing a mechanism ({@link #withLoadedWebview(WithWebviewCallback)}) for other components, like {@link ChatWebviewFragment}, to safely interact with the WebView once it has finished loading its content.</li>
 * </ul>
 * <p>
 * This "owner" pattern separates the long-lived WebView object from the UI-bound fragment that displays it,
 * leading to better performance and a smoother user experience during configuration changes.
 *
 * @see ChatWebviewFragment The fragment responsible for displaying the WebView managed by this class.
 * @see AndroidBridge The nested class that exposes native functionality to the WebView's JavaScript context.
 * @see ChatClient The gRPC client used for chat operations.
 */
public class ChatWebviewOwnerFragment extends Fragment {
    public static final String TAG = "WebviewOwnerFragment";
    private static final String CHAT_SERVICE_HOST = "10.0.2.2"; // emulator to PC
    private static final int CHAT_SERVICE_PORT = 5065;

    private ChatClient chatClient;

    /**
     * Initializes and loads the WebView for the chat interface.
     * <p>
     * This method sets up the WebView instance if it hasn't been created yet. It configures
     * an {@link WebViewAssetLoader} to serve local assets from the "assets" directory,
     * enables JavaScript, and sets up a {@link WebChromeClient} to log console messages.
     * <p>
     * It then creates and attaches an {@link AndroidBridge} to the WebView, which facilitates
     * communication between the JavaScript running in the WebView and the native Android code.
     * If a bridge already exists for a different user, it is removed and a new one is created.
     * If the user and username are the same as the existing bridge, the method returns early
     * to avoid unnecessary reloading.
     * <p>
     * Finally, it loads the main HTML file for the chat interface from the app's assets.
     *
     * @param userId     The unique identifier for the current user.
     * @param userName   The display name for the current user.
     * @param controller The {@link ChatWebviewFragment} that will display the WebView.
     */
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

    /**
     * Executes a callback with the WebView instance.
     * <p>
     * This method ensures that the provided callback is executed on the UI thread
     * only after the WebView has finished loading its initial content. If the WebView
     * is already loaded, the callback is posted to the UI thread's message queue for
     * immediate execution. If the WebView is not yet loaded, the callback is added to
     * a pending queue and will be executed once the WebView signals it is ready.
     *
     * @param callback The {@link WithWebviewCallback} to be executed with the loaded WebView.
     */ // executes callback on the ui thread with the webview after its done loading
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

    /**
     * Provides a bridge between the JavaScript running in the WebView and the native Android application.
     * This class is instantiated and attached to the WebView, allowing JavaScript code to call its public methods
     * marked with {@link JavascriptInterface}. It handles tasks such as retrieving user data, sending chat messages
     * via gRPC, and managing the lifecycle of the WebView content.
     * <p>
     * Methods in this class facilitate communication from the WebView to the app's backend services,
     * handle UI updates like showing/hiding loading dialogs, and manage asynchronous responses
     * to JavaScript promises.
     */
    private static class AndroidBridge {
        private final String userId;
        private final String userName;
        private final ChatClient chatClient;
        private final ChatWebviewOwnerFragment fragment;

        private boolean loaded = false;
        private final Runnable loadCb;
        private final WeakReference<ChatWebviewFragment> controller;

        /**
         * Constructs the bridge between the WebView's JavaScript environment and the native Android code.
         *
         * @param userId The ID of the currently logged-in user.
         * @param userName The name of the currently logged-in user.
         * @param chatClient The gRPC client for communicating with the chat service.
         * @param fragment The parent fragment that owns the WebView and this bridge.
         * @param loadCb A callback to be executed once the WebView's content has finished loading.
         * @param controller The fragment that directly displays the WebView UI.
         */
        public AndroidBridge(String userId, String userName, ChatClient chatClient, ChatWebviewOwnerFragment fragment, Runnable loadCb, ChatWebviewFragment controller) {
            this.userId = userId;
            this.userName = userName;
            this.chatClient = chatClient;
            this.fragment = fragment;
            this.loadCb = loadCb;
            this.controller = new WeakReference<>(controller);
        }

        /**
         * Retrieves the user ID of the currently logged-in user.
         * This method is exposed to the JavaScript running in the WebView.
         *
         * @return The unique identifier for the current user.
         */
        @JavascriptInterface
        public String getUserId() {
            return userId;
        }

        /**
         * Gets the user's name.
         * This method is exposed to the JavaScript code running in the WebView.
         *
         * @return The name of the current user.
         */
        @JavascriptInterface
        public String getUserName() {
            return userName;
        }

        /**
         * Called by the JavaScript in the WebView when the web content has finished loading and is
         * ready for interaction. This method signals that the WebView is prepared to receive
         * further JavaScript calls from the native Android side. It ensures that any pending
         * operations that require the WebView to be loaded are executed.
         */
        @JavascriptInterface
        public void setLoaded() {
            handler.post(() -> {
                if(loaded) return;
                loaded = true;
                loadCb.run();
            });
        }

        /**
         * Displays a loading dialog to the user. This method is exposed to the JavaScript
         * running in the WebView and can be called to indicate that a long-running operation
         * is in progress. The dialog is shown on the main UI thread.
         */
        @JavascriptInterface
        public void showLoadingDialog() { handler.post(LoadingDialogFragment::show); }

        /**
         * Hides the loading dialog. This method is exposed to the JavaScript
         * running in the WebView and can be called from there to dismiss the
         * loading indicator on the native Android side. The operation is posted
         * to the main UI thread.
         */
        @JavascriptInterface
        public void hideLoadingDialog() { handler.post(LoadingDialogFragment::hide); }


        /**
         * Receives a Base64 encoded protobuf {@link ChatMessage} from the JavaScript side,
         * decodes it, and sends it to the chat service via gRPC.
         * This method is exposed to the WebView's JavaScript context and can be called
         * from the web application. It handles acquiring the necessary authentication token
         * and forwards the message to the {@link ChatClient}. Log statements are used to
         * indicate the success or failure of the operation.
         *
         * @param b64_msg A Base64 encoded string representing the serialized ChatMessage protobuf.
         */
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

            String accessToken = getAccessToken();
            if(accessToken.isEmpty()) return; //todo error dialog

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

        /**
         * Handles a request from the WebView's JavaScript to fetch message history.
         * The method decodes a Base64 encoded {@link GetMessagesRequest} protobuf message,
         * sends it to the chat service via gRPC, and resolves or rejects a JavaScript
         * Promise based on the outcome.
         * <p>
         * This method is exposed to the JavaScript running in the WebView via the
         * "AndroidBridge" interface.
         *
         * @param b64_req A Base64 encoded string representing the serialized GetMessagesRequest protobuf message.
         * @param request_id A unique identifier for the JavaScript Promise that is waiting for the result.
         *                   This ID is used to resolve or reject the correct Promise in the WebView.
         */
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

            String accessToken = getAccessToken();
            if(accessToken.isEmpty()) return; //todo error dialog

            chatClient.getMessagesForGroup(req, accessToken, (response, error) -> {
                if(error != null) {
                    rejectPromisedResponse(error.getMessage(), request_id);
                } else if(response != null) {
                    resolvePromisedResponse(response, request_id);
                }
                else rejectPromisedResponse("Null response received for requestMessageHistory!", request_id);
            });

        }

        /**
         * Asynchronously requests the list of groups the current user is a member of.
         * This method is exposed to the JavaScript running in the WebView.
         * It initiates a gRPC call to the chat service to fetch the user's groups.
         * The result of this operation is communicated back to the JavaScript environment
         * by resolving or rejecting a Promise associated with the provided request_id.
         *
         * @param request_id A unique identifier generated by the JavaScript caller, used to
         *                   correlate this request with a Promise that will be resolved or
         *                   rejected with the server's response or an error.
         */
        @JavascriptInterface
        public void requestUserGroups(String request_id) {
            GetUserGroupsRequest req = GetUserGroupsRequest.newBuilder().setUserId(userId).build();

            String accessToken = getAccessToken();
            if(accessToken.isEmpty()) return; //todo error dialog

            chatClient.getUserGroups(req, accessToken, (response, error) -> {
                if(error != null) {
                    rejectPromisedResponse(error.getMessage(), request_id);
                } else if(response != null) {
                    resolvePromisedResponse(response, request_id);
                }
                else rejectPromisedResponse("Null response received for requestUserGroups!", request_id);
            });
        }

        /**
         * Searches for users based on a substring query. This method is exposed to the JavaScript
         * environment in the WebView.
         * <p>
         * It constructs a {@link SearchUsersRequest} with the provided substring and is intended
         * to make a gRPC call to the backend service. The result of this asynchronous operation
         * should be communicated back to the JavaScript side using either {@code resolvePromisedResponse}
         * on success or {@code rejectPromisedResponse} on failure, identified by the {@code request_id}.
         *
         * @param substring  The string to search for in user names or other user fields.
         * @param request_id A unique identifier generated by the JavaScript side to correlate this
         *                   request with its eventual response (Promise resolution/rejection).
         */
        @JavascriptInterface
        public void searchUsers(String substring, String request_id) {
            SearchUsersRequest req = SearchUsersRequest.newBuilder().setQuery(substring).build();

            String accessToken = getAccessToken();
            if(accessToken.isEmpty()) return; //todo error dialog

            chatClient.searchUsers(req, accessToken, (response, error) -> {
                if(error != null) {
                    rejectPromisedResponse(error.getMessage(), request_id);
                } else if(response != null) {
                    resolvePromisedResponse(response, request_id);
                }
                else rejectPromisedResponse("Null response received for searchUsers!", request_id);
            });
        }

        /**
         * Creates a new chat group with the specified name.
         * This method is exposed to the JavaScript running in the WebView.
         * It constructs a {@link CreateGroupRequest} and is intended to send it via a gRPC call.
         * The result of the gRPC call (success or failure) is then meant to be communicated back
         * to the JavaScript environment using a promise-based mechanism.
         *
         * @param name The desired name for the new group.
         * @param request_id A unique identifier generated by the JavaScript client to track this specific request.
         *                   This ID is used to resolve or reject the corresponding JavaScript Promise.
         */
        @JavascriptInterface
        public void createGroup(String name, String request_id) {
            CreateGroupRequest req = CreateGroupRequest.newBuilder().setGroupName(name).setUserId(userId).build();

            String accessToken = getAccessToken();
            if(accessToken.isEmpty()) return; //todo error dialog

            chatClient.createGroup(req, accessToken, (response, error) -> {
                if(error != null) {
                    rejectPromisedResponse(error.getMessage(), request_id);
                } else if(response != null) {
                    resolvePromisedResponse(response, request_id);
                }
                else rejectPromisedResponse("Null response received for createGroup!", request_id);
            });
        }

        /**
         * Adds a specified user to a specified group. This method is exposed to JavaScript in the WebView.
         * It constructs a gRPC request and is intended to send it to the chat service. The result of
         * the gRPC call (success or failure) should be communicated back to the JavaScript environment
         * using the provided request_id to resolve or reject a JavaScript Promise.
         *
         * @param userId     The ID of the user to be added to the group.
         * @param groupId    The ID of the group to which the user will be added.
         * @param request_id A unique identifier for the request, generated by the JavaScript client.
         *                   This is used to resolve the corresponding Promise in the WebView.
         */
        @JavascriptInterface
        public void addUserToGroup(String userId, String groupId, String request_id) {
            AddUserToGroupRequest req = AddUserToGroupRequest.newBuilder().setGroupId(groupId).setUserId(userId).build();

            String accessToken = getAccessToken();
            if(accessToken.isEmpty()) return; //todo error dialog

            chatClient.addUserToGroup(req, accessToken, (response, error) -> {
                if(error != null) {
                    rejectPromisedResponse(error.getMessage(), request_id);
                } else if(response != null) {
                    resolvePromisedResponse(response, request_id);
                }
                else rejectPromisedResponse("Null response received for addUserToGroup!", request_id);
            });
        }

        /**
         * Checks if the WebView has finished loading its initial content.
         * The 'loaded' flag is set to true by a call from JavaScript (via the {@link #setLoaded()} method)
         * once the web application is ready.
         *
         * @return true if the WebView content has loaded, false otherwise.
         */
        public boolean getLoaded() { return loaded; }

        /**
         * Resolves a JavaScript promise by calling a global function in the WebView.
         * <p>
         * This method serializes a Protobuf message into a Base64-encoded string and
         * passes it, along with the request ID, to the {@code window.Promiser.resolvePromise}
         * JavaScript function. This allows asynchronous Java operations (like gRPC calls)
         * to fulfill promises created in the JavaScript context. The operation is
         * posted to the main thread to ensure it runs on the UI thread, which is required
         * for interacting with the WebView.
         *
         * @param response   The Protobuf message response to be sent to JavaScript.
         * @param request_id The unique identifier for the promise to be resolved.
         */
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

        /**
         * Rejects a JavaScript Promise that was created for a request.
         * This method is called when an asynchronous operation (like a gRPC call) fails.
         * It executes JavaScript in the WebView to call `window.Promiser.rejectPromise`,
         * passing the error message and the original request ID.
         *
         * @param error      A string describing the error that occurred.
         * @param request_id The unique identifier for the request, used to find the corresponding Promise in JavaScript.
         */
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

        private String getAccessToken() {
            String accessToken;
            try {
                var viewModel = ((MainActivity)fragment.requireActivity()).getViewModel();
                accessToken = viewModel.tokenStore.access();
            } catch (Exception e) {
                Log.e(TAG, "Failed to get access token: " + e.getMessage());
                return "";
            }
            return accessToken;
        }

        /**
         * Serializes a Protocol Buffers message to a Base64-encoded string,
         * then wraps it in quotes to be safely injected as a string literal into a JavaScript call.
         * This is used to pass protobuf data from Java to the WebView's JavaScript context.
         *
         * @param proto The Protocol Buffers message to be encoded.
         * @return A JSON-quoted, Base64-encoded string representing the protobuf message.
         */
        private static String jsBase64Arg(MessageLite proto) {
            // Base64 is JS-safe by definition
            return JSONObject.quote(Base64.encodeToString(proto.toByteArray(), Base64.NO_WRAP));
        }

        public final Handler handler = new Handler(Looper.getMainLooper());
    }
}
