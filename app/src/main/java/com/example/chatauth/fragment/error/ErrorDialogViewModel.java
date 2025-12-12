package com.example.chatauth.fragment.error;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel for {@link ErrorDialogFragment}.
 * @noinspection DataFlowIssue
 */
public class ErrorDialogViewModel extends ViewModel {

    /**
     * Constructor for {@link ErrorDialogViewModel}.
     */
    public ErrorDialogViewModel(SavedStateHandle handle) {
        this.handle = handle;
        _internal_title = handle.getLiveData(TITLE_KEY, null);
        _internal_message = handle.getLiveData(MESSAGE_KEY, null);
        _internal_recoverable = handle.getLiveData(RECOVERABLE_KEY, null);
        title = _internal_title;
        message = _internal_message;
        recoverable = _internal_recoverable;
        is_set = handle.getLiveData(IS_SET, false);
    }

    /**
     * Read-only {@link LiveData} that holds the ErrorDialog's title.
     */
    public final LiveData<String> title;

    /**
     * Read-only {@link LiveData} that holds the ErrorDialog's message.
     */
    public final LiveData<String> message;

    /**
     * Read-only {@link LiveData} that holds the ErrorDialog's recoverable flag.
     */
    public final LiveData<Boolean> recoverable;

    /**
     * Sets the values of {@link #title}, {@link #message}, and {@link #recoverable} if it hasn't been set previously.
     * @param title The title to set.
     * @param message The message to set.
     * @param recoverable Value of recoverable flag to set.
     */
    public void fromParameters(@Nullable String title, @Nullable String message, @Nullable Boolean recoverable) {
        if(is_set.getValue()) return;
        _internal_title.setValue(title);
        _internal_message.setValue(message);
        _internal_recoverable.setValue(recoverable);
        is_set.setValue(true);
    }

    public boolean isSet() {
        return is_set.getValue();
    }

    /**
     * Mutable reference to {@link #title}.
     */
    private final MutableLiveData<String> _internal_title;

    /**
     * Mutable reference to {@link #message}.
     */
    private final MutableLiveData<String> _internal_message;

    /**
     * Mutable reference to {@link #recoverable}.
     */
    private final MutableLiveData<Boolean> _internal_recoverable;

    /**
     * Whether or not this {@link ErrorDialogFragment} has been configured yet.<br>
     * During dialog creation, this is what tells {@link #fromParameters} to set the fields with the given arguments or use whatever is in the ViewModel.
     */
    private final MutableLiveData<Boolean> is_set;
    private SavedStateHandle handle;

    private static final String TITLE_KEY = "0";
    private static final String MESSAGE_KEY = "1";
    private static final String RECOVERABLE_KEY = "2";
    private static final String IS_SET = "3";
}
