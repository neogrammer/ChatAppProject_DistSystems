package com.example.chatauth;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chatauth.fragment.loading.LoadingDialogFragment;
import com.example.chatauth.fragment.chat.ChatWebviewOwnerFragment;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
}