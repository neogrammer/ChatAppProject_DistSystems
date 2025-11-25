package com.example.chatauth.fragment.register;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.chatauth.R;
import com.example.chatauth.databinding.FragmentRegisterBinding;

public class RegisterFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater);
        current_data = new ViewModelProvider(this).get(RegisterFragmentViewmodel.class);
        binding.setViewModel(current_data);
        binding.setLifecycleOwner(this);

        var observer = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                current_data.registerEnabled.setValue(!current_data.email.getValue().isBlank() && !current_data.password.getValue().isBlank() && !current_data.displayName.getValue().isBlank());
            }
        };

        binding.email.addTextChangedListener(observer);
        binding.password.addTextChangedListener(observer);
        binding.displayName.addTextChangedListener(observer);
        binding.btnRegister.setOnClickListener(v -> {
            //todo
        });

        return binding.getRoot();
    }

    private FragmentRegisterBinding binding;
    private RegisterFragmentViewmodel current_data;

    public static class RegisterFragmentViewmodel extends ViewModel {
        public RegisterFragmentViewmodel(SavedStateHandle state_handle) {
            super();
            email = state_handle.getLiveData("rf_email", "");
            password = state_handle.getLiveData("rf_password", "");
            displayName = state_handle.getLiveData("rf_displayName", "");
            registerEnabled = state_handle.getLiveData("rf_registerEnabled", false);
            errorText = state_handle.getLiveData("rf_errorText", "");
        }

        public final MutableLiveData<String> email;
        public final MutableLiveData<String> password;
        public final MutableLiveData<String> displayName;
        public final MutableLiveData<Boolean> registerEnabled;
        public final MutableLiveData<String> errorText;
    }
}