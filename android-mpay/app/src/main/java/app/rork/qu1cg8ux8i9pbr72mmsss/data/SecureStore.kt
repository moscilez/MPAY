package app.rork.qu1cg8ux8i9pbr72mmsss.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/** Encrypts wallet data on-device with an AES-GCM key held in Android Keystore. Nothing leaves the phone. */
class SecureStore(private val context: Context) {
    private val alias = "mpay_wallet_key"
    private val file = File(context.filesDir, "wallet.bin")

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    fun write(plain: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val enc = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val tmp = File(file.parentFile, "wallet.tmp")
        tmp.writeBytes(byteArrayOf(iv.size.toByte()) + iv + enc)
        tmp.renameTo(file)
    }

    fun read(): String? {
        if (!file.exists()) return null
        return runCatching {
            val bytes = file.readBytes()
            val ivLen = bytes[0].toInt()
            val iv = bytes.copyOfRange(1, 1 + ivLen)
            val enc = bytes.copyOfRange(1 + ivLen, bytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(enc), Charsets.UTF_8)
        }.getOrNull()
    }
}

class PinManager(context: Context) {
    private val prefs = context.getSharedPreferences("mpay_security", Context.MODE_PRIVATE)

    val hasPin: Boolean get() = prefs.contains("pin_hash")

    var biometricsEnabled: Boolean
        get() = prefs.getBoolean("biometrics", true)
        set(value) { prefs.edit().putBoolean("biometrics", value).apply() }

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pin_hash", hash(pin, salt))
            .putInt("pin_fails", 0)
            .apply()
    }

    fun verify(pin: String): Boolean {
        val salt = Base64.decode(prefs.getString("pin_salt", null) ?: return false, Base64.NO_WRAP)
        val ok = hash(pin, salt) == prefs.getString("pin_hash", null)
        prefs.edit().putInt("pin_fails", if (ok) 0 else failedAttempts + 1).apply()
        return ok
    }

    val failedAttempts: Int get() = prefs.getInt("pin_fails", 0)

    private fun hash(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 20000, 256)
        val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        return Base64.encodeToString(f.generateSecret(spec).encoded, Base64.NO_WRAP)
    }
}
