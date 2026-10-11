package com.ashborne.nexusmemory.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class WebDavClient(
    private val serverUrl: String,
    private val username: String,
    private val tokenOrPass: String
) {
    private val client = OkHttpClient()
    private val credentials = Credentials.basic(username, tokenOrPass)

    suspend fun uploadBackup(encryptedData: ByteArray, remoteFileName: String = "nexus_vault.nexus"): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = "${serverUrl.trimEnd('/')}/$remoteFileName"
                val body = encryptedData.toRequestBody("application/octet-stream".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", credentials)
                    .put(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun downloadBackup(remoteFileName: String = "nexus_vault.nexus"): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "${serverUrl.trimEnd('/')}/$remoteFileName"
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", credentials)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.bytes()
                    } else {
                        null
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }
}
