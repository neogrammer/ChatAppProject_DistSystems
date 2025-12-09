package com.example.chatauth;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.chatauth.auth.AuthClientSample;
import com.example.chatauth.auth.TokenStore;
import com.example.chatauth.fragment.loading.LoadingDialogFragment;
import com.example.chatauth.fragment.chat.ChatWebviewOwnerFragment;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MainActivity.activity_singleton = new WeakReference<>(this);
        current_data = new ViewModelProvider(this).get(MainActivityViewmodel.class);

        // init loading dialog
        LoadingDialogFragment.assignActivityHost(this);

        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });
        EdgeToEdge.enable(this);

        // add retained webview fragment
        var frag_manager = getSupportFragmentManager();
        if(frag_manager.findFragmentByTag(ChatWebviewOwnerFragment.TAG) == null) {
            frag_manager.beginTransaction().add(new ChatWebviewOwnerFragment(), ChatWebviewOwnerFragment.TAG).commit();
        }
    }

    public MainActivityViewmodel getViewModel() { return current_data; }

    public static final class MainActivityViewmodel extends ViewModel {
        public final AuthClientSample client;
        public final TokenStore tokenStore;

        private static final String HOST = "10.0.2.2"; // emulator-to-PC
        private static final int    PORT = 55101;      // your compose mapping (55101->50051)

        public MainActivityViewmodel() {
            tokenStore = new TokenStore(MainActivity.activity_singleton.get());
            client = new AuthClientSample();
            client.connect(HOST, PORT);
        }
    }

    private MainActivityViewmodel current_data;
    private static WeakReference<MainActivity> activity_singleton;
}