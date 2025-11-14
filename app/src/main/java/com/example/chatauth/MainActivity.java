package com.example.chatauth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatauth.R;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private final java.util.concurrent.Executor bg = Executors.newSingleThreadExecutor();
    private AuthClientSample client;
    private TokenStore tokenStore;

    // Point this at your server (LAN IP or public IP)
    private static final String HOST = "24.236.104.52"; // emulator-to-PC
    private static final int    PORT = 55101;      // your compose mapping (55101->50051)


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tokenStore = new TokenStore(this);
        client = new AuthClientSample();
        client.connect(HOST, PORT);

        EditText email = findViewById(R.id.email);
        EditText password = findViewById(R.id.password);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView result = findViewById(R.id.result);

        btnRegister.setOnClickListener(v -> bg.execute(() -> {
            try {
                var res = client.register(email.getText().toString(), password.getText().toString(), "AndroidUser");
                tokenStore.save(res.getTokens().getAccessToken(), res.getTokens().getRefreshToken());
                runOnUiThread(() -> result.setText("Registered: " + res.getEmail()));
            } catch (Exception e) {
                runOnUiThread(() -> result.setText("Register error: " + e.getMessage()));
            }
        }));

        btnLogin.setOnClickListener(v -> bg.execute(() -> {

            try {
                var res = client.login(email.getText().toString(), password.getText().toString());
                tokenStore.save(res.getTokens().getAccessToken(), res.getTokens().getRefreshToken());
                runOnUiThread(() -> result.setText("Logged in: " + res.getEmail()));
            } catch (Exception e) {
                runOnUiThread(() -> result.setText("Login error: " + e.getMessage()));
            }
        }));
    }
}