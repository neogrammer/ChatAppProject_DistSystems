package com.example.chatauth.fragment.login;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.chatauth.MainActivity;
import com.example.chatauth.R;
import com.example.chatauth.auth.AuthClientSample;
import com.example.chatauth.auth.TokenStore;
import com.example.chatauth.databinding.FragmentLoginBinding;
import com.example.chatauth.fragment.chat.ChatWebviewFragment;
import com.example.chatauth.fragment.loading.LoadingDialogFragment;

import java.util.concurrent.Executors;

//todo move edit text contents to viewmodel so it survives process death
public class LoginFragment extends Fragment {
    //private final java.util.concurrent.Executor bg = Executors.newSingleThreadExecutor();
//    private AuthClientSample client;
//    private TokenStore tokenStore;

    private FragmentLoginBinding binding;
    private LoginFragmentViewmodel current_data;
    public static final String TAG = "LoginFragment";

    // Point this at your server (LAN IP or public IP)
//    private static final String HOST = "24.236.104.52"; // emulator-to-PC
//    private static final int    PORT = 55101;      // your compose mapping (55101->50051)
//
//    public static final String TAG = "LoginFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(inflater);
        current_data = new ViewModelProvider(this).get(LoginFragmentViewmodel.class);
        binding.setViewModel(current_data);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        var observer = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                current_data.registerEnabled.setValue(!current_data.email.getValue().isBlank() && !current_data.password.getValue().isBlank());
            }
        };

        binding.email.addTextChangedListener(observer);
        binding.password.addTextChangedListener(observer);
        binding.btnLogin.setOnClickListener(v -> {
            LoadingDialogFragment.show();
            final var data = ((MainActivity)requireActivity()).getViewModel();
            data.client.login(binding.email.getText().toString(), binding.password.getText().toString(), (res, err) -> {
                try{
                    data.tokenStore.save(res.getTokens().getAccessToken(), res.getTokens().getRefreshToken());
                    //todo nav
                }
                catch (Exception e) {
                    binding.result.setText("Login error: " + e.getMessage());
                }
                LoadingDialogFragment.hide();
            });
        });
//        tokenStore = new TokenStore(requireContext());
//        client = new AuthClientSample();
//        client.connect(HOST, PORT);

//        binding.btnRegister.setOnClickListener(v -> {
//            LoadingDialogFragment.show();
//            client.register(binding.email.getText().toString(), binding.password.getText().toString(), "AndroidUser", (res, err) -> {
//                try {
//                    tokenStore.save(res.getTokens().getAccessToken(), res.getTokens().getRefreshToken());
//                    binding.result.setText("Registered: " + res.getEmail());
//                } catch (Exception e) {
//                    binding.result.setText("Register error: " + e.getMessage());
//                }
//                LoadingDialogFragment.hide();
//            });
//        });
//
//        binding.btnLogin.setOnClickListener(v -> {
//            LoadingDialogFragment.show();
//            client.login(binding.email.getText().toString(), binding.password.getText().toString(), (res, err) -> {
//                try {
//                    tokenStore.save(res.getTokens().getAccessToken(), res.getTokens().getRefreshToken());
//                    binding.result.setText("Logged in: " + res.getEmail());
//                } catch (Exception e) {
//                    binding.result.setText("Login error: " + e.getMessage());
//                }
//                LoadingDialogFragment.hide();
//            });
//        });



        return binding.getRoot();
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        var nc = NavHostFragment.findNavController(this);
//        var args = new ChatWebviewFragment.Arguments("0", "TestUser");
//        var bundle = new Bundle();
//        bundle.putParcelable("args", args);
//        nc.navigate(R.id.action_loginFragment_to_chatWebviewFragment, bundle);
//    }

    public static class LoginFragmentViewmodel extends ViewModel {
        public LoginFragmentViewmodel(SavedStateHandle state_handle) {
            super();
            email = state_handle.getLiveData("lf_email", "");
            password = state_handle.getLiveData("lf_password", "");
            registerEnabled = state_handle.getLiveData("lf_registerEnabled", false);
            errorText = state_handle.getLiveData("lf_errorText", "");
        }

        public final MutableLiveData<String> email;
        public final MutableLiveData<String> password;
        public final MutableLiveData<Boolean> registerEnabled;
        public final MutableLiveData<String> errorText;
    }
}