package com.example.chatauth.fragment.chat;

import androidx.annotation.Nullable;

import com.example.chatauth.helpers.JSCallback;

import ink.bluballz.chat.v1.ChatMessage;
import ink.bluballz.chat.v1.ChatRoom;

public interface IWebviewController {
    void addRoom(ChatRoom room, @Nullable JSCallback<Boolean> callback);
    void removeRoom(String roomId, @Nullable JSCallback<Boolean> callback);
    void switchToRoom(String roomId, @Nullable JSCallback<Boolean> callback);

    void addMessage(ChatMessage message, @Nullable JSCallback<Boolean> callback);
    void addMessages(@Nullable JSCallback<Boolean> callback, ChatMessage ...messages);
    void removeMessage(String messageId, String roomId, @Nullable JSCallback<Boolean> callback);
    void removeMessages(@Nullable JSCallback<Boolean> callback, RemoveMessageTuple ...messages);
    void hasMessage(String messageId, @Nullable String roomId, @Nullable JSCallback<Boolean> callback);
}