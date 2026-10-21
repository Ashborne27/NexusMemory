package com.ashborne.nexusmemory.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import android.util.Base64

object WebDavManager {

    suspend fun uploadToWebDav(
        webDavUrl: String,
        user: String,
        pass: String,
        jsonContent: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseUrl = if (webDavUrl.endsWith("/")) webDavUrl else "$webDavUrl/"
            val targetUrl = URL("${baseUrl}nexus_backup.json")
            val connection = targetUrl.openConnection() as HttpURLConnection
            
            val auth = "$user:$pass"
            val encodedAuth = Base64.encodeToString(auth.toByteArray(), Base64.NO_WRAP)
            
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Authorization", "Basic $encodedAuth")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            connection.outputStream.use { os ->
                os.write(jsonContent.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun uploadFileToWebDav(
        webDavUrl: String,
        user: String,
        pass: String,
        file: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext false
            val baseUrl = if (webDavUrl.endsWith("/")) webDavUrl else "$webDavUrl/"
            val targetUrl = URL("${baseUrl}${file.name}")
            val connection = targetUrl.openConnection() as HttpURLConnection
            
            val auth = "$user:$pass"
            val encodedAuth = Base64.encodeToString(auth.toByteArray(), Base64.NO_WRAP)
            
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Authorization", "Basic $encodedAuth")
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.doOutput = true

            file.inputStream().use { fis ->
                connection.outputStream.use { os ->
                    fis.copyTo(os)
                }
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
            val targetUrl = URL("${baseUrl}nexus_backup.json")
            val connection = targetUrl.openConnection() as HttpURLConnection
            
            val auth = "$user:$pass"
            val encodedAuth = Base64.encodeToString(auth.toByteArray(), Base64.NO_WRAP)
            
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Basic $encodedAuth")

            if (connection.responseCode in 200..299) {
                connection.inputStream.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
