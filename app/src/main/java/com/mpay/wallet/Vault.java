package com.mpay.wallet;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.*;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** The key is device-bound and requires recent system authentication. No plaintext fallback. */
final class Vault {
    private static final String ALIAS = "mpay.local.v1";
    private final AtomicFile file;
    Vault(Context context) { file = new AtomicFile(new File(context.getNoBackupFilesDir(), "wallet.enc")); }
    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        if (file.getBaseFile().exists()) throw new IOException("Ключ хранилища недоступен. Восстановить данные без ключа нельзя.");
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(300, KeyProperties.AUTH_BIOMETRIC_STRONG | KeyProperties.AUTH_DEVICE_CREDENTIAL).build());
        return generator.generateKey();
    }
    JSONObject read() throws Exception {
        if (!file.getBaseFile().exists()) return new JSONObject().put("cards", new org.json.JSONArray()).put("entries", new org.json.JSONArray());
        byte[] bytes = file.readFully();
        if (bytes.length < 30 || bytes[0] != 1) throw new IOException("Неверный формат хранилища");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, java.util.Arrays.copyOfRange(bytes, 1, 13)));
        return new JSONObject(new String(cipher.doFinal(bytes, 13, bytes.length - 13), java.nio.charset.StandardCharsets.UTF_8));
    }
    void write(JSONObject wallet) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted = cipher.doFinal(wallet.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        FileOutputStream out = null;
        try {
            out = file.startWrite(); out.write(1); out.write(cipher.getIV()); out.write(encrypted); file.finishWrite(out);
        } catch (Exception e) { if (out != null) file.failWrite(out); throw e; }
    }
}
