package com.example.chatauth.fragment.register;

import android.os.Bundle;

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
import com.example.chatauth.databinding.FragmentRegisterBinding;
import com.example.chatauth.fragment.chat.ChatWebviewFragment;
import com.example.chatauth.fragment.loading.LoadingDialogFragment;

/**
 * A {@link Fragment} for user registration.
 *
 * This fragment provides a user interface for new users to create an account. It includes
 * fields for email, password, and display name. It handles user input validation to enable
 * the registration button and communicates with a backend service to perform the registration.
 *
 * Upon successful registration, it saves the authentication tokens, and navigates the user
 * to the {@link ChatWebviewFragment}. If registration fails,
 * it displays an error message.
 *
 * This class uses a {@link RegisterFragmentViewmodel} to manage its UI state and data,
 * ensuring data persistence across configuration changes.
 */
public class RegisterFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater);
        current_data = new ViewModelProvider(this).get(RegisterFragmentViewmodel.class);
        binding.setViewModel(current_data);
        binding.setLifecycleOwner(getViewLifecycleOwner());

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
            LoadingDialogFragment.show();
            final var data = ((MainActivity)requireActivity()).getViewModel();
            data.client.register(current_data.email.getValue(), current_data.password.getValue(), current_data.displayName.getValue(), (res, err) -> {
                try{
                    data.tokenStore.save(res.getTokens().getAccessToken(), res.getTokens().getRefreshToken());
                    var nc = NavHostFragment.findNavController(this);
                    var args = new ChatWebviewFragment.Arguments(res.getUserId(), res.getDisplayName());
                    var bundle = new Bundle();
                    bundle.putParcelable("args", args);
                    nc.navigate(R.id.action_registerFragment_to_chatWebviewFragment, bundle);
                }
                catch (Exception e) {
                    binding.result.setText("Register error: " + e.getMessage() + "; " + err.getMessage());
                }
                LoadingDialogFragment.hide();
            });
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