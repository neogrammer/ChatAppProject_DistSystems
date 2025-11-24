package com.example.chatauth.fragment.chat;

public class RemoveMessageTuple {
    public final String messageId;
    public final String roomId;

    public RemoveMessageTuple(String messageId, String roomId) {
        this.messageId = messageId;
        this.roomId = roomId;
    }
}
