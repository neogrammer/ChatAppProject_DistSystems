package com.example.chatauth;

import android.content.Context;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class TokenStore {
    private final Context ctx;

    public TokenStore(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public void save(String access, String refresh) throws Exception {
        MasterKey masterKey = new MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        var prefs = EncryptedSharedPreferences.create(
                ctx,
                "auth_tokens",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
        prefs.edit().putString("access", access).putString("refresh", refresh).apply();
    }

    public String access() throws Exception {
        MasterKey masterKey = new MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        var prefs = EncryptedSharedPreferences.create(
                ctx,
                "auth_tokens",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
        return prefs.getString("access", "");
    }

    public String refresh() throws Exception {
        MasterKey masterKey = new MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        var prefs = EncryptedSharedPreferences.create(
                ctx,
                "auth_tokens",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
        return prefs.getString("refresh", "");
    }
}