package com.example.chatauth.fragment.loginorregister;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.chatauth.R;
import com.example.chatauth.databinding.FragmentLoginOrRegisterBinding;

/**
 * A simple {@link Fragment} subclass that serves as the initial landing screen for the user.
 * It presents the user with two choices: to log in or to register for a new account.
 * User interactions with the "Login" or "Register" buttons will navigate them
 * to the corresponding fragments ({@link com.example.chatauth.fragment.login.LoginFragment}
 * or {@link com.example.chatauth.fragment.register.RegisterFragment}) using the Navigation Component.
 */
public class LoginOrRegisterFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentLoginOrRegisterBinding.inflate(inflater);
        binding.btnRegister.setOnClickListener(v -> {
            var nc = NavHostFragment.findNavController(this);
            nc.navigate(R.id.action_loginOrRegisterFragment_to_registerFragment);
        });
        binding.btnLogin.setOnClickListener(v -> {
            var nc = NavHostFragment.findNavController(this);
            nc.navigate(R.id.action_loginOrRegisterFragment_to_loginFragment);
        });
        return binding.getRoot();
    }

    private FragmentLoginOrRegisterBinding binding;
}