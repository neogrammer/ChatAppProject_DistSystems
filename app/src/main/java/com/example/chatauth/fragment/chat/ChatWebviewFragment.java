package com.example.chatauth.fragment.chat;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.example.chatauth.R;
import com.example.chatauth.helpers.JSCallback;
import com.google.protobuf.AbstractMessageLite;
import com.google.protobuf.MessageLite;


import org.json.JSONObject;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

import ink.bluballz.chat.v1.ChatMessage;
//import ink.bluballz.chat.v1.ChatRoom;

/**
 * A fragment responsible for displaying the WebView
 * <p>
 * This fragment acts as a container for a WebView that is managed and owned by a
 * {@link ChatWebviewOwnerFragment}. It retrieves the WebView from the owner
 * and attaches it to its own view hierarchy. This pattern allows the WebView's state
 * (and the loaded chat application) to be preserved across configuration changes or
 * navigation events that would otherwise destroy this fragment.
 * <p>
 * To use this fragment, it must be created with a {@link Bundle} containing an
 * {@link Arguments} object. The arguments must provide a {@code userId} and a
 * {@code userName}, which are necessary to initialize the chat session in the WebView.
 * This is done automatically with the navigation system.
 * <p>
 * It requires that a {@link ChatWebviewOwnerFragment} with the tag
 * {@link ChatWebviewOwnerFragment#TAG} exists in the same {@link androidx.fragment.app.FragmentManager}.
 * Failure to provide the required arguments or the owner fragment will result in a
 * {@link RuntimeException}.
 *
 * @see ChatWebviewOwnerFragment
 * @see Arguments
 */
public class ChatWebviewFragment extends Fragment /*implements IWebviewController*/ {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        var frag_manager = requireActivity().getSupportFragmentManager();
        webview_owner = (ChatWebviewOwnerFragment) frag_manager.findFragmentByTag(ChatWebviewOwnerFragment.TAG);
        if(webview_owner == null) { throw new RuntimeException("Webview owner fragment not found!"); }
        var args = getArguments();
        if(args == null) throw new RuntimeException("Can't construct chat webview without args!");
        args.setClassLoader(Arguments.class.getClassLoader());
        var args_obj = (Arguments)args.getParcelable("args");
        if(args_obj == null) throw new RuntimeException("Can't construct chat webview without args!");
        if(args_obj.userId == null || args_obj.userId.isBlank()) throw new RuntimeException("Can't construct chat webview without userId!");
        if(args_obj.userName == null || args_obj.userName.isBlank()) throw new RuntimeException("Can't construct chat webview without userName!");
        webview_owner.load(args_obj.userId, args_obj.userName, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat_webview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        webview_owner.withLoadedWebview(webview -> {
            var old_parent = webview.getParent();
            if(old_parent != null) ((ViewGroup) old_parent).removeView(webview);
            ((ViewGroup)view).addView(webview);
        });
    }

    /**
     * A parcelable class for passing arguments to the {@link ChatWebviewFragment}.
     * This class encapsulates the necessary user information required to initialize the chat webview.
     */
    public static class Arguments implements Parcelable {
        public final String userId;
        public final String userName;

        public Arguments(String userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }

        protected Arguments(Parcel in) {
            userId = in.readString();
            userName = in.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel parcel, int i) {
            parcel.writeString(userId);
            parcel.writeString(userName);
        }

        public static final Creator<Arguments> CREATOR = new Creator<Arguments>() {
            @Override
            public Arguments createFromParcel(Parcel in) {
                return new Arguments(in);
            }

            @Override
            public Arguments[] newArray(int size) {
                return new Arguments[size];
            }
        };
    }
    private ChatWebviewOwnerFragment webview_owner;

}