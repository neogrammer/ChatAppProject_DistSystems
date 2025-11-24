package com.example.chatauth.fragment.login;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.chatauth.auth.AuthClientSample;
import com.example.chatauth.auth.TokenStore;
import com.example.chatauth.databinding.FragmentLoginBinding;
import com.example.chatauth.fragment.loading.LoadingDialogFragment;

import java.util.concurrent.Executors;

//todo move edit text contents to viewmodel so it survives process death
public class LoginFragment extends Fragment {
    //private final java.util.concurrent.Executor bg = Executors.newSingleThreadExecutor();
    private AuthClientSample client;
    private TokenStore tokenStore;

    private FragmentLoginBinding binding;

    // Point this at your server (LAN IP or public IP)
    private static final String HOST = "24.236.104.52"; // emulator-to-PC
    private static final int    PORT = 55101;      // your compose mapping (55101->50051)

    public static final String TAG = "LoginFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(inflater);

        tokenStore = new TokenStore(requireContext());
        client = new AuthClientSample();
        client.connect(HOST, PORT);

        binding.btnRegister.setOnClickListener(v -> {
            LoadingDialogFragment.show();
            client.register(binding.email.getText().toString(), binding.password.getText().toString(), "AndroidUser", (res, err) -> {
                try {
                    tokenStore.save(res.getTokens().getAccessToken(), res.getTokens().getRefreshToken());
                    binding.result.setText("Registered: " + res.getEmail());
                } catch (Exception e) {
                    binding.result.setText("Register error: " + e.getMessage());
                }
                LoadingDialogFragment.hide();
            });
        });

        binding.btnLogin.setOnClickListener(v -> {
            LoadingDialogFragment.show();
            client.login(binding.email.getText().toString(), binding.password.getText().toString(), (res, err) -> {
                try {
                    tokenStore.save(res.getTokens().getAccessToken(), res.getTokens().getRefreshToken());
                    binding.result.setText("Logged in: " + res.getEmail());
                } catch (Exception e) {
                    binding.result.setText("Login error: " + e.getMessage());
                }
                LoadingDialogFragment.hide();
            });
        });

        return binding.getRoot();
    }
}