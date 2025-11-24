package com.example.chatauth.auth;

import androidx.annotation.Nullable;

import com.example.chatauth.helpers.UIStreamResponse;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.MetadataUtils;

// Generated from your proto (ink.bluballz.chat.auth.v1)
import ink.bluballz.chat.auth.v1.AuthServiceGrpc;
import ink.bluballz.chat.auth.v1.AuthServiceGrpc.AuthServiceBlockingStub;
import ink.bluballz.chat.auth.v1.AuthResponse;
import ink.bluballz.chat.auth.v1.AuthTokens;
import ink.bluballz.chat.auth.v1.LoginRequest;
import ink.bluballz.chat.auth.v1.RefreshRequest;
import ink.bluballz.chat.auth.v1.RegisterRequest;
import ink.bluballz.chat.auth.v1.ValidateTokenRequest;
import ink.bluballz.chat.auth.v1.ValidateTokenResponse;

public class AuthClientSample {
    private ManagedChannel channel;
    private AuthServiceGrpc.AuthServiceStub stub;

    /** Connect to your gRPC auth server. For dev you can keep plaintext. */
    public void connect(String host, int port) {
        channel = OkHttpChannelBuilder.forAddress(host, port)
                .usePlaintext() // DEV ONLY. Use TLS for production.
                .build();
        stub = AuthServiceGrpc.newStub(channel);
    }

    /** Close the channel (e.g., on Activity.onDestroy). */
    public void shutdown() {
        if (channel != null) {
            channel.shutdownNow();
            channel = null;
            stub = null;
        }
    }

    // ===== Convenience RPCs =====

    public void register(String email, String pass, String name, @Nullable UIStreamResponse.OnResultCallback<AuthResponse> then) {
        RegisterRequest req = RegisterRequest.newBuilder()
                .setEmail(email)
                .setPassword(pass)
                .setDisplayName(name)
                .build();
        stub.register(req, new UIStreamResponse<>(then));
    }

    public void login(String email, String pass, @Nullable UIStreamResponse.OnResultCallback<AuthResponse> then) {
        LoginRequest req = LoginRequest.newBuilder()
                .setEmail(email)
                .setPassword(pass)
                .build();
        stub.login(req, new UIStreamResponse<>(then));
    }

    public void validate(String accessToken, @Nullable UIStreamResponse.OnResultCallback<ValidateTokenResponse> then) {
        ValidateTokenRequest req = ValidateTokenRequest.newBuilder()
                .setToken(accessToken)
                .build();
        stub.validateToken(req, new UIStreamResponse<>(then));
    }

    public void refresh(String refreshToken, @Nullable UIStreamResponse.OnResultCallback<AuthTokens> then) {
        RefreshRequest req = RefreshRequest.newBuilder()
                .setRefreshToken(refreshToken)
                .build();
        stub.refresh(req, new UIStreamResponse<>(then));
    }

    /**
     * Returns a stub that adds `Authorization: Bearer <token>` to every call.
     * Use this for any protected service calls.
     */
    public AuthServiceGrpc.AuthServiceStub withAuth(String accessToken) {
        Metadata meta = new Metadata();
        Metadata.Key<String> AUTH =
                Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        meta.put(AUTH, "Bearer " + accessToken);

        ClientInterceptor auth = MetadataUtils.newAttachHeadersInterceptor(meta);
        return stub.withInterceptors(auth);
    }
}