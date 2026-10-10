package com.ashborne.nexusmemory.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val SYMMETRIC_KEY_ALIAS = "NexusMemorySymmetricKey"
    private const val ASYMMETRIC_KEY_ALIAS = "NexusMemoryAsymmetricKey"
    
    private const val TRANSFORMATION_AES = "AES/GCM/NoPadding"
    private const val TRANSFORMATION_RSA = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private const val TAG_LENGTH = 128

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    init {
        if (!keyStore.containsAlias(SYMMETRIC_KEY_ALIAS)) {
            createSymmetricKey()
        }
        if (!keyStore.containsAlias(ASYMMETRIC_KEY_ALIAS)) {
            createAsymmetricKey()
        }
    }

    private fun createSymmetricKey(): SecretKey {
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    SYMMETRIC_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }

    private fun createAsymmetricKey() {
        KeyGenerator.getInstance("RSA", "AndroidKeyStore") // Note: KeyPairGenerator pour RSA
        val kpg = java.security.KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        kpg.initialize(
            KeyGenParameterSpec.Builder(
                ASYMMETRIC_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setKeySize(4096)
                .build()
        )
        kpg.generateKeyPair()
    }

    private fun getSymmetricKey(): SecretKey {
        val entry = keyStore.getEntry(SYMMETRIC_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey ?: createSymmetricKey()
    }

    private fun getRsaPublicKey(): PublicKey {
        val entry = keyStore.getEntry(ASYMMETRIC_KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
        if (entry != null) {
            return entry.certificate.publicKey
        }
        createAsymmetricKey()
        val newEntry = keyStore.getEntry(ASYMMETRIC_KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        return newEntry.certificate.publicKey
    }

    private fun getRsaPrivateKey(): PrivateKey {
        val entry = keyStore.getEntry(ASYMMETRIC_KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        return entry.privateKey
    }

    fun encrypt(bytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION_AES)
        cipher.init(Cipher.ENCRYPT_MODE, getSymmetricKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(bytes)
        return iv + encryptedBytes
    }

    fun decrypt(bytes: ByteArray): ByteArray {
        val iv = bytes.copyOfRange(0, 12)
        val encryptedBytes = bytes.copyOfRange(12, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION_AES)
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSymmetricKey(), spec)
        return cipher.doFinal(encryptedBytes)
    }

    fun encryptToString(plainText: String): String {
        val encrypted = encrypt(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decryptFromString(encryptedStr: String): String {
        val decoded = Base64.decode(encryptedStr, Base64.NO_WRAP)
        val decrypted = decrypt(decoded)
        return String(decrypted, Charsets.UTF_8)
    }

    // Chiffrement hybride pour export sécurisé inter-appareils
    fun encryptHybrid(bytes: ByteArray): ByteArray {
        // 1. Générer une clé AES éphémère de session
        val aesKeyGen = KeyGenerator.getInstance("AES")
        aesKeyGen.init(256)
        val sessionKey = aesKeyGen.generateKey()

        // 2. Chiffrer les données avec la clé AES de session
        val cipherAes = Cipher.getInstance(TRANSFORMATION_AES)
        cipherAes.init(Cipher.ENCRYPT_MODE, sessionKey)
        val iv = cipherAes.iv
        val encryptedData = cipherAes.doFinal(bytes)

        // 3. Chiffrer la clé de session AES avec la clé publique RSA
        val cipherRsa = Cipher.getInstance(TRANSFORMATION_RSA)
        cipherRsa.init(Cipher.ENCRYPT_MODE, getRsaPublicKey())
        val encryptedSessionKey = cipherRsa.doFinal(sessionKey.encoded)

        // 4. Assembler le format [Taille Cle RSA (4 octets)][Cle RSA chiffrée][IV (12 octets)][Données chiffrées]
        val keySize = encryptedSessionKey.size
        val result = ByteArray(4 + keySize + 12 + encryptedData.size)
        
        result[0] = (keySize shr 24).toByte()
        result[1] = (keySize shr 16).toByte()
        result[2] = (keySize shr 8).toByte()
        result[3] = keySize.toByte()

        System.arraycopy(encryptedSessionKey, 0, result, 4, keySize)
        System.arraycopy(iv, 0, result, 4 + keySize, 12)
        System.arraycopy(encryptedData, 0, result, 4 + keySize + 12, encryptedData.size)

        return result
    }

    // Déchiffrement hybride
    fun decryptHybrid(bytes: ByteArray): ByteArray {
        // 1. Extraire la taille de la clé RSA chiffrée
        val keySize = ((bytes[0].toInt() and 0xFF) shl 24) or
                      ((bytes[1].toInt() and 0xFF) shl 16) or
                      ((bytes[2].toInt() and 0xFF) shl 8) or
                      (bytes[3].toInt() and 0xFF)

        val encryptedSessionKey = bytes.copyOfRange(4, 4 + keySize)
        val iv = bytes.copyOfRange(4 + keySize, 4 + keySize + 12)
        val encryptedData = bytes.copyOfRange(4 + keySize + 12, bytes.size)

        // 2. Déchiffrer la clé AES de session avec la clé privée RSA
        val cipherRsa = Cipher.getInstance(TRANSFORMATION_RSA)
        cipherRsa.init(Cipher.DECRYPT_MODE, getRsaPrivateKey())
        val sessionKeyBytes = cipherRsa.doFinal(encryptedSessionKey)
        val sessionKey: SecretKey = SecretKeySpec(sessionKeyBytes, "AES")

        // 3. Déchiffrer les données avec la clé AES de session
        val cipherAes = Cipher.getInstance(TRANSFORMATION_AES)
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipherAes.init(Cipher.DECRYPT_MODE, sessionKey, spec)
        return cipherAes.doFinal(encryptedData)
    }
}
