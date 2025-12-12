package com.example.chatauth.fragment.error;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.ref.WeakReference;

/**
 * Fragment for showing errors to the user as an {@link AlertDialog}.
 */
public class ErrorDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    /**
     * Assigns a default host that the static {@link #show} method can use to host the fragment.
     * @param new_host The new activity to host the dialog. Should be the activity at the top of the back stack.
     */
    public static void assignActivityHost(@NonNull AppCompatActivity new_host) {
        host = new WeakReference<>(new_host);
    }

    /**
     * Creates a new {@link ErrorDialogFragment} with the activity assigned in {@link #assignActivityHost}.<br>
     * Uses fragment's arguments to attempt to persist arguments through a config change that can happen between construction and display.
     * @param title The title to show in the {@link AlertDialog}. If it's null, no title is set.
     * @param message The message to show in the {@link AlertDialog}.
     * @param recoverable Whether or not this message can be dismissed (whether or not the error this represents is fatal)
     */
    public static ErrorDialogFragment show(@Nullable String title, @NonNull String message, boolean recoverable) {
        var activity = host.get();
        if(activity == null) throw new RuntimeException("Can't show an ErrorDialogFragment without a host activity!");
        Bundle b = new Bundle();
        b.putString(TITLE_KEY, title);
        b.putString(MSG_KEY, message);
        b.putBoolean(RECOV_KEY, recoverable);
        var dialog = new ErrorDialogFragment();
        dialog.setArguments(b);
        dialog.show(activity.getSupportFragmentManager(), ErrorDialogFragment.TAG);
        return dialog;
    }

    /**
     * Lifecycle override for {@link DialogFragment} that returns the {@link AlertDialog} to use.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        current_data = new ViewModelProvider(this).get(ErrorDialogViewModel.class);
        var args = getArguments();
        var set = current_data.isSet();

        // if args are null and the viewmodel isn't set, this was either default constructed somewhere or something went very wrong
        if(args == null && !set) throw new RuntimeException("Can't create a ErrorDialogFragment with null arguments!");

        // don't worry about deserializing parcelbles if the viewmodel is already set
        if(!set) {
            current_data.fromParameters(args.getString(TITLE_KEY), args.getString(MSG_KEY), args.getBoolean(RECOV_KEY));
        }
        var resolved_recoverable = current_data.recoverable.getValue();
        builder.setCancelable(resolved_recoverable).setTitle(current_data.title.getValue()).setMessage(current_data.message.getValue());
        if(resolved_recoverable) {
            builder.setPositiveButton("OK", this);
        }
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(resolved_recoverable);
        return dialog;
    }

    /**
     * Tag for this fragment.
     */
    public static String TAG = "ErrorDialog";

    /**
     * Simple callback for when the positive button is acknowledged; it just dismisses the dialog.
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        dismiss();
    }

    /**
     * ViewModel for ErrorDialogFragment.
     */
    private ErrorDialogViewModel current_data;

    private static final String UNKNOWN_ERROR = "Unknown Error!";
    private static final String TITLE_KEY = "Title";
    private static final String MSG_KEY = "Msg";
    private static final String RECOV_KEY = "Recov";
    private static WeakReference<AppCompatActivity> host;
}
