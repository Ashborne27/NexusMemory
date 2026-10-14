package com.ashborne.nexusmemory.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager {

    fun encryptToString(plainText: String): String = encryptStatic(plainText)
    fun decryptFromString(cipherTextBase64: String): String = decryptStatic(cipherTextBase64)
    fun encryptHybrid(plainText: String): String = encryptStatic(plainText)
    fun decryptHybrid(cipherTextBase64: String): String = decryptStatic(cipherTextBase64)

    companion object {
        private const val KEY_ALIAS = "NexusMemoryKey"
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"

        private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        private fun getOrCreateSecretKey(): SecretKey {
            keyStore.getEntry(KEY_ALIAS, null)?.let { entry ->
                if (entry is KeyStore.SecretKeyEntry) return entry.secretKey
            }
            val keyGenerator = KeyGenerator.getInstance(ALGORITHM, "AndroidKeyStore")
            try {
                val builder = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(BLOCK_MODE)
                    .setEncryptionPaddings(PADDING)
                    .setKeySize(256)
                    .setIsStrongBoxBacked(true)
                keyGenerator.init(builder.build())
                return keyGenerator.generateKey()
            } catch (e: Exception) {
                val fallbackBuilder = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(BLOCK_MODE)
                    .setEncryptionPaddings(PADDING)
                    .setKeySize(256)
                keyGenerator.init(fallbackBuilder.build())
                return keyGenerator.generateKey()
            }
        }

        private fun encryptStatic(plainText: String): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        }

        private fun decryptStatic(cipherTextBase64: String): String {
            val combined = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            val iv = ByteArray(12)
            val encryptedBytes = ByteArray(combined.size - 12)
            System.arraycopy(combined, 0, iv, 0, 12)
            System.arraycopy(combined, 12, encryptedBytes, 0, encryptedBytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        }

        fun encryptToString(plainText: String): String = encryptStatic(plainText)
        fun decryptFromString(cipherTextBase64: String): String = decryptStatic(cipherTextBase64)
        fun encryptHybrid(plainText: String): String = encryptStatic(plainText)
        fun decryptHybrid(cipherTextBase64: String): String = decryptStatic(cipherTextBase64)
    }
}
