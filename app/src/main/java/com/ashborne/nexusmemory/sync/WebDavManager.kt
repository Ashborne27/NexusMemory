package com.ashborne.nexusmemory.sync

import com.ashborne.nexusmemory.security.KeyStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import android.util.Base64
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream

object WebDavManager {

    suspend fun uploadToWebDav(
        webDavUrl: String,
        user: String,
        pass: String,
        jsonContent: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseUrl = if (webDavUrl.endsWith("/")) webDavUrl else "$webDavUrl/"
            val targetUrl = URL("${baseUrl}nexus_backup.enc")
            val connection = targetUrl.openConnection() as HttpURLConnection
            
            val auth = "$user:$pass"
            val encodedAuth = Base64.encodeToString(auth.toByteArray(), Base64.NO_WRAP)
            
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Authorization", "Basic $encodedAuth")
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.doOutput = true

            val cipher = KeyStoreHelper.getEncryptCipher()
            val iv = cipher.iv

            val baos = ByteArrayOutputStream()
            DataOutputStream(baos).use { dos ->
                dos.writeInt(iv.size)
                dos.write(iv)
            }
            CipherOutputStream(baos, cipher).use { cos ->
                cos.write(jsonContent.toByteArray(Charsets.UTF_8))
            }

            val encryptedBytes = baos.toByteArray()

            connection.outputStream.use { os ->
                os.write(encryptedBytes)
            }

            val responseCode = connection.responseCode
            responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun downloadFromWebDav(
        webDavUrl: String,
        user: String,
        pass: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val baseUrl = if (webDavUrl.endsWith("/")) webDavUrl else "$webDavUrl/"
            val targetUrl = URL("${baseUrl}nexus_backup.enc")
            val connection = targetUrl.openConnection() as HttpURLConnection
            
            val auth = "$user:$pass"
            val encodedAuth = Base64.encodeToString(auth.toByteArray(), Base64.NO_WRAP)
            
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Basic $encodedAuth")

            if (connection.responseCode in 200..299) {
                val encryptedBytes = connection.inputStream.use { it.readBytes() }
                
                val bais = ByteArrayInputStream(encryptedBytes)
                val dis = DataInputStream(bais)
                val ivSize = dis.readInt()
                val iv = ByteArray(ivSize)
                dis.readFully(iv)

                val cipher = KeyStoreHelper.getDecryptCipher(iv)
                val decryptedBaos = ByteArrayOutputStream()
                
                CipherInputStream(bais, cipher).use { cis ->
                    cis.copyTo(decryptedBaos)
                }

                decryptedBaos.toString("UTF-8")
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
