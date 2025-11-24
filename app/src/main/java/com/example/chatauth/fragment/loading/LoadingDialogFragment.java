package com.example.chatauth.fragment.loading;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.example.chatauth.databinding.DialogLoadingBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.ref.WeakReference;

/**
 * Simple, modal {@link androidx.appcompat.app.AlertDialog AlertDialog} that shows a loading icon.<br>
 * Used during app startup.
 */
public class LoadingDialogFragment extends DialogFragment {

    /**
     * Assigns the host {@link AppCompatActivity} for the {@link LoadingDialogFragment}.
     * The assigned activity is used to manage the dialog's lifecycle and its display.
     *
     * @param new_host the {@link AppCompatActivity} to be set as the host for the dialog fragment.
     *                 This parameter cannot be null.
     */
    public static void assignActivityHost(@NonNull AppCompatActivity new_host) {
        host = new WeakReference<>(new_host);
    }

    /**
     * Displays the {@link LoadingDialogFragment} to indicate a loading state if it's not already shown.
     * The dialog is hosted by an activity assigned using {@link #assignActivityHost(AppCompatActivity)}.
     *
     * If the host activity is not set or is no longer available, a {@link RuntimeException} is thrown.
     *
     * The dialog ensures that only one instance is displayed at a time.
     * It is managed using a static reference to track the current active instance.
     *
     * @throws RuntimeException if the host activity is not assigned or is unavailable.
     */
    public static void show() {
        if(current_instance != null) return;
        var activity = host.get();
        if(activity == null) throw new RuntimeException("Can't show an LoadingDialogFragment without a host activity!");
        current_instance = new LoadingDialogFragment();
        current_instance.show(activity.getSupportFragmentManager(), TAG);
    }

    /**
     * Hides the currently displayed {@link LoadingDialogFragment} if it is active.
     *
     * This method checks if there is an active instance of the dialog fragment and dismisses it.
     * If no instance is active, the method does nothing.
     *
     * This ensures proper cleanup of the dialog fragment when it is no longer needed.
     */
    public static void hide() {
        if(current_instance == null) return;
        current_instance.dismiss();
    }

    /**
     * {@link DialogFragment} lifecycle override.<br>
     * Builds the dialog with the loading icon inflater.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        binding = DialogLoadingBinding.inflate(getLayoutInflater());
        builder.setView(binding.getRoot()).setCancelable(false);
        var dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        current_instance = null;
    }

    public static final String TAG = "LoadingDialogFragment";

    private DialogLoadingBinding binding;

    private static WeakReference<AppCompatActivity> host;
    private static LoadingDialogFragment current_instance;
}
