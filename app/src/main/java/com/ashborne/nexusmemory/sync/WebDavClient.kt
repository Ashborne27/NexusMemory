package com.ashborne.nexusmemory.sync

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class WebDavClient(
    private val serverUrl: String,
    private val username: String,
    private val token: String
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val credentials = Credentials.basic(username, token)

    /**
     * Envoie un fichier chiffré vers le serveur WebDAV distant
     */
    fun uploadFile(localFile: File, remotePath: String): Boolean {
        val url = "$serverUrl/$remotePath".replace(Regex("(?<!:)//"), "/")
        val requestBody = localFile.asRequestBody()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", credentials)
            .put(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful || response.code == 201 || response.code == 204
            }
        } catch (e: IOException) {
            false
        }
    }

    /**
     * Télécharge un fichier chiffré depuis le serveur WebDAV distant
     */
    fun downloadFile(remotePath: String, destinationFile: File): Boolean {
        val url = "$serverUrl/$remotePath".replace(Regex("(?<!:)//"), "/")
        val request = Request.Builder()
            .url(url)
            .header("Authorization", credentials)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                response.body?.byteStream()?.use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } != null
            }
        } catch (e: IOException) {
            false
        }
    }
}
