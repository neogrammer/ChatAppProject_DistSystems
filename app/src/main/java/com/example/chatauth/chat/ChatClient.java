package com.example.chatauth.chat;

import androidx.annotation.Nullable;

import com.example.chatauth.helpers.UIStreamResponse;

import ink.bluballz.chat.v1.AddUserToGroupRequest;
import ink.bluballz.chat.v1.AddUserToGroupResponse;
import ink.bluballz.chat.v1.CreateGroupRequest;
import ink.bluballz.chat.v1.CreateGroupResponse;
import ink.bluballz.chat.v1.GetMessagesRequest;
import ink.bluballz.chat.v1.GetMessagesResponse;
import ink.bluballz.chat.v1.GetUserGroupsRequest;
import ink.bluballz.chat.v1.GetUserGroupsResponse;
import ink.bluballz.chat.v1.SearchUsersRequest;
import ink.bluballz.chat.v1.SearchUsersResponse;
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

    public void getMessagesForGroup(GetMessagesRequest message, String accessToken, @Nullable UIStreamResponse.OnResultCallback<GetMessagesResponse> callback) {
        ChatServiceGrpc.ChatServiceStub authenticatedStub = withAuth(accessToken);
        authenticatedStub.getMessages(message, new UIStreamResponse<>(callback));
    }

    public void createGroup(CreateGroupRequest message, String accessToken, @Nullable UIStreamResponse.OnResultCallback<CreateGroupResponse> callback) {
        ChatServiceGrpc.ChatServiceStub authenticatedStub = withAuth(accessToken);
        authenticatedStub.createGroup(message, new UIStreamResponse<>(callback));
    }

    public void addUserToGroup(AddUserToGroupRequest message, String accessToken, @Nullable UIStreamResponse.OnResultCallback<AddUserToGroupResponse> callback) {
        ChatServiceGrpc.ChatServiceStub authenticatedStub = withAuth(accessToken);
        authenticatedStub.addUserToGroup(message, new UIStreamResponse<>(callback));
    }

    public void getUserGroups(GetUserGroupsRequest message, String accessToken, @Nullable UIStreamResponse.OnResultCallback<GetUserGroupsResponse> callback) {
        ChatServiceGrpc.ChatServiceStub authenticatedStub = withAuth(accessToken);
        authenticatedStub.getUserGroups(message, new UIStreamResponse<>(callback));
    }

    public void searchUsers(SearchUsersRequest message, String accessToken, @Nullable UIStreamResponse.OnResultCallback<SearchUsersResponse> callback) {
        ChatServiceGrpc.ChatServiceStub authenticatedStub = withAuth(accessToken);
        authenticatedStub.searchUsers(message, new UIStreamResponse<>(callback));
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
