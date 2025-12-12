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
import com.example.chatauth.fragment.error.ErrorDialogFragment;
import com.example.chatauth.fragment.loading.LoadingDialogFragment;
import com.example.chatauth.fragment.chat.ChatWebviewOwnerFragment;

import java.lang.ref.WeakReference;

/**
 * The main entry point of the application.
 * This activity is responsible for setting up the main view, initializing view models,
 * managing window insets, and adding the primary fragment that hosts the chat WebView.
 * It also maintains a static reference to itself for global access within the app,
 * although this is generally discouraged, it's used here for simplicity.
 */
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MainActivity.activity_singleton = new WeakReference<>(this);
        current_data = new ViewModelProvider(this).get(MainActivityViewmodel.class);

        // init loading/error dialog
        LoadingDialogFragment.assignActivityHost(this);
        ErrorDialogFragment.assignActivityHost(this);

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