package com.example.chatauth.fragment.chat;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.example.chatauth.R;

public class ChatWebviewFragment extends Fragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        var frag_manager = requireActivity().getSupportFragmentManager();
        var webview_owner = (ChatWebviewOwnerFragment) frag_manager.findFragmentByTag(ChatWebviewOwnerFragment.TAG);
        if(webview_owner == null) { throw new RuntimeException("Webview owner fragment not found!"); }
        var args = getArguments();
        if(args == null) throw new RuntimeException("Can't construct chat webview without args!");
        args.setClassLoader(Arguments.class.getClassLoader());
        var args_obj = (Arguments)args.getParcelable("args");
        if(args_obj == null) throw new RuntimeException("Can't construct chat webview without args!");
        if(args_obj.userId == null || args_obj.userId.isBlank()) throw new RuntimeException("Can't construct chat webview without userId!");
        if(args_obj.userName == null || args_obj.userName.isBlank()) throw new RuntimeException("Can't construct chat webview without userName!");
        webview = webview_owner.load(args_obj.userId, args_obj.userName);
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
        var old_parent = webview.getParent();
        if(old_parent != null) ((ViewGroup) old_parent).removeView(webview);
        ((ViewGroup)view).addView(webview);
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

    private WebView webview;
}