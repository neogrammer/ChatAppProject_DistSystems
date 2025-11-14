package com.example.chatauth;

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
    private AuthServiceBlockingStub stub;

    /** Connect to your gRPC auth server. For dev you can keep plaintext. */
    public void connect(String host, int port) {
        channel = OkHttpChannelBuilder.forAddress(host, port)
                .usePlaintext() // DEV ONLY. Use TLS for production.
                .build();
        stub = AuthServiceGrpc.newBlockingStub(channel);
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

    public AuthResponse register(String email, String pass, String name) {
        RegisterRequest req = RegisterRequest.newBuilder()
                .setEmail(email)
                .setPassword(pass)
                .setDisplayName(name)
                .build();
        return stub.register(req);
    }

    public AuthResponse login(String email, String pass) {
        LoginRequest req = LoginRequest.newBuilder()
                .setEmail(email)
                .setPassword(pass)
                .build();
        return stub.login(req);
    }

    public ValidateTokenResponse validate(String accessToken) {
        ValidateTokenRequest req = ValidateTokenRequest.newBuilder()
                .setToken(accessToken)
                .build();
        return stub.validateToken(req);
    }

    public AuthTokens refresh(String refreshToken) {
        RefreshRequest req = RefreshRequest.newBuilder()
                .setRefreshToken(refreshToken)
                .build();
        return stub.refresh(req);
    }

    /**
     * Returns a stub that adds `Authorization: Bearer <token>` to every call.
     * Use this for any protected service calls.
     */
    public AuthServiceBlockingStub withAuth(String accessToken) {
        Metadata meta = new Metadata();
        Metadata.Key<String> AUTH =
                Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        meta.put(AUTH, "Bearer " + accessToken);

        ClientInterceptor auth = MetadataUtils.newAttachHeadersInterceptor(meta);
        return stub.withInterceptors(auth);
    }
}