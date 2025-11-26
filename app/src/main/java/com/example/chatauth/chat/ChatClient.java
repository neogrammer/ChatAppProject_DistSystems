package com.example.chatauth.chat;

import androidx.annotation.Nullable;

import com.example.chatauth.helpers.UIStreamResponse;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.MetadataUtils;

import ink.bluballz.chat.v1.ChatMessage;
import ink.bluballz.chat.v1.ChatServiceGrpc;
import ink.bluballz.chat.v1.SendMessageResponse;

public class ChatClient {
    private ManagedChannel channel;
    private ChatServiceGrpc.ChatServiceStub stub;

    public void connect(String host, int port) {
        channel = OkHttpChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        stub = ChatServiceGrpc.newStub(channel);
    }

    public void shutdown() {
        if (channel != null) {
            channel.shutdownNow();
            channel = null;
            stub = null;
        }
    }

    public void sendMessage(ChatMessage message, String accessToken, @Nullable UIStreamResponse.OnResultCallback<SendMessageResponse> callback) {
        ChatServiceGrpc.ChatServiceStub authenticatedStub = withAuth(accessToken);
        authenticatedStub.sendMessage(message, new UIStreamResponse<>(callback));
    }

    private ChatServiceGrpc.ChatServiceStub withAuth(String accessToken) {
        Metadata meta = new Metadata();
        Metadata.Key<String> AUTH =
                Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        meta.put(AUTH, "Bearer " + accessToken);

        ClientInterceptor auth = MetadataUtils.newAttachHeadersInterceptor(meta);
        return stub.withInterceptors(auth);
    }
}
