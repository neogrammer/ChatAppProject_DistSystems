package com.example.chatauth.helpers;

import androidx.annotation.Nullable;

@FunctionalInterface
public interface JSCallback<T> {
    void execute(@Nullable T value);
}
