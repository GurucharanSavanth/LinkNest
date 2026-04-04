package com.linknest.core.data.backup

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

class BackupCryptoManager @Inject constructor() {
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        return JSONObject()
            .put("kind", ENCRYPTED_BACKUP_KIND)
            .put("algorithm", TRANSFORMATION)
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("payload", Base64.encodeToString(cipherText, Base64.NO_WRAP))
            .toString()
    }

    fun decryptIfNeeded(payload: String): String {
        val root = runCatching { JSONObject(payload) }.getOrNull()
        if (root?.optString("kind") != ENCRYPTED_BACKUP_KIND) {
            return payload
        }

        val algorithm = root.getString("algorithm")
        require(algorithm == TRANSFORMATION) { "Unsupported backup algorithm: $algorithm" }
        val iv = Base64.decode(root.getString("iv"), Base64.NO_WRAP)
        val encryptedPayload = Base64.decode(root.getString("payload"), Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )
        return String(cipher.doFinal(encryptedPayload), StandardCharsets.UTF_8)
    }

    fun isEncryptedPayload(payload: String): Boolean =
        runCatching { JSONObject(payload).optString("kind") == ENCRYPTED_BACKUP_KIND }
            .getOrDefault(false)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "linknest_backup_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val ENCRYPTED_BACKUP_KIND = "linknest.encrypted.backup"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
