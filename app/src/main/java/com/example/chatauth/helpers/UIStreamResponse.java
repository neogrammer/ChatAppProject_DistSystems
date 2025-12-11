package com.example.chatauth.helpers;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import io.grpc.stub.StreamObserver;

/**
 * The UIStreamResponse class is an implementation of the StreamObserver interface
 * that ensures stream responses and errors are handled on the main UI thread.
 *
 * Rather than using 3 different callbacks, the provided callback is executed with a null value and non-null error
 * when an error occurs and vice versa when successful.
 *
 * @param <T> The type of the data being observed in the stream.
 */
public class UIStreamResponse<T> implements StreamObserver<T> {
    public UIStreamResponse(@Nullable OnResultCallback<T> callback) {
        this.callback = callback;
        if(ui_thread == null) ui_thread = new Handler(Looper.getMainLooper());
    }
    @Override
    public void onNext(T value) {
        if(callback != null) ui_thread.post(() -> callback.execute(value, null));
    }

    @Override
    public void onError(Throwable t) {
        if(callback != null) ui_thread.post(() -> callback.execute(null, t));
    }

    @Override
    public void onCompleted() {}

    @FunctionalInterface
    public interface OnResultCallback<T> {
        void execute(@Nullable T value, @Nullable Throwable error);
    }

    protected OnResultCallback<T> callback;
    protected static Handler ui_thread;
}
