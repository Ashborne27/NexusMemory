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

        private fun encryptBytes(plainBytes: ByteArray): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainBytes)
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            return combined
        }

        private fun decryptBytes(combinedBytes: ByteArray): ByteArray {
            val iv = ByteArray(12)
            val encryptedBytes = ByteArray(combinedBytes.size - 12)
            System.arraycopy(combinedBytes, 0, iv, 0, 12)
            System.arraycopy(combinedBytes, 12, encryptedBytes, 0, encryptedBytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            return cipher.doFinal(encryptedBytes)
        }

        @JvmStatic
        fun encryptToString(plainText: String): String {
            val combined = encryptBytes(plainText.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        }

        @JvmStatic
        fun decryptFromString(cipherTextBase64: String): String {
            val combined = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            val decryptedBytes = decryptBytes(combined)
            return String(decryptedBytes, Charsets.UTF_8)
        }

        @JvmStatic
        fun encryptToString(plainBytes: ByteArray): String {
            val combined = encryptBytes(plainBytes)
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        }

        @JvmStatic
        fun decryptFromString(cipherBytes: ByteArray): String {
            val decryptedBytes = decryptBytes(cipherBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        }

        @JvmStatic
        fun encryptHybrid(plainText: String): String = encryptToString(plainText)

        @JvmStatic
        fun encryptHybrid(plainBytes: ByteArray): ByteArray = encryptBytes(plainBytes)

        @JvmStatic
        fun decryptHybrid(cipherTextBase64: String): String = decryptFromString(cipherTextBase64)

        @JvmStatic
        fun decryptHybrid(cipherBytes: ByteArray): ByteArray = decryptBytes(cipherBytes)
    }
}
