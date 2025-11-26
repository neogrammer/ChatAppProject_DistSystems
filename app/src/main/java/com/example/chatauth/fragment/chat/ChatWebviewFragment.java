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
import ink.bluballz.chat.v1.ChatRoom;

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
        webview_owner.load(args_obj.userId, args_obj.userName);
        addRoom(ChatRoom.newBuilder().setId("main").setRoomName("Anonymous").build(), null);
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

//    @Override
    public void addRoom(ChatRoom room, @Nullable JSCallback<Boolean> callback) {
        webview_owner.withLoadedWebview(webview -> {
            String js = "window.WebviewController.addRoom("
                    + "window.WebviewControllerDecoder.decodeChatRoom("
                    + jsBase64Arg(room)
                    + "))";

            webview.evaluateJavascript(js, callback != null ? r -> callback.execute("true".equals(r)) : null);
        });
    }

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

    private static String jsString(String value) {
        // Handles null → "null", as JSON should
        return value == null ? "null" : JSONObject.quote(value);
    }

    private static String jsBase64Arg(MessageLite proto) {
        // Base64 is JS-safe by definition
        return JSONObject.quote(Base64.encodeToString(proto.toByteArray(), Base64.NO_WRAP));
    }

    private static String jsListOfBase64(MessageLite[] protos) {
        if (protos.length == 0) return "[]";

        StringBuilder out = new StringBuilder("[");
        for (MessageLite p : protos) {
            out.append("window.WebviewControllerDecoder.decodeChatMessage(")
                    .append(jsBase64Arg(p))
                    .append("),");
        }
        out.setLength(out.length() - 1); // strip trailing comma
        out.append("]");
        return out.toString();
    }

    private static String jsRemoveTupleList(RemoveMessageTuple[] tuples) {
        if (tuples.length == 0) return "[]";

        StringBuilder out = new StringBuilder("[");
        for (RemoveMessageTuple t : tuples) {
            out.append("[")
                    .append(jsString(t.messageId))
                    .append(",")
                    .append(jsString(t.roomId))
                    .append("],");
        }
        out.setLength(out.length() - 1);
        out.append("]");
        return out.toString();
    }
    private ChatWebviewOwnerFragment webview_owner;

}