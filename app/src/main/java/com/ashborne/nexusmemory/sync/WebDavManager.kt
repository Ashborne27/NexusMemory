package com.ashborne.nexusmemory.sync

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object WebDavManager {

    suspend fun uploadToWebDav(serverUrl: String, username: String, token: String, data: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$serverUrl/nexus_backup.nexus")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                val credentials = "$username:$token"
                val encodedAuth = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
                setRequestProperty("Authorization", "Basic $encodedAuth")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            OutputStreamWriter(connection.outputStream).use { it.write(data) }
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun downloadFromWebDav(serverUrl: String, username: String, token: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$serverUrl/nexus_backup.nexus")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                val credentials = "$username:$token"
                val encodedAuth = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
                setRequestProperty("Authorization", "Basic $encodedAuth")
            }
            if (connection.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val content = reader.readText()
                reader.close()
                connection.disconnect()
                content
            } else {
                connection.disconnect()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
