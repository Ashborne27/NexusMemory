package com.ashborne.nexusmemory.backup

import android.content.Context
import com.ashborne.nexusmemory.data.MemoryDao
import com.ashborne.nexusmemory.data.MemoryEntity
import com.ashborne.nexusmemory.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BackupManager {

    suspend fun exportBackup(context: Context, dao: MemoryDao, outputFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val memories: List<MemoryEntity> = dao.getAllMemoriesList()
                val jsonArray = JSONArray()
                memories.forEach { memory ->
                    val obj = JSONObject().apply {
                        put("id", memory.id)
                        put("text", memory.text)
                        put("category", memory.category)
                        put("timestamp", memory.timestamp)
                        put("isProtected", memory.isProtected)
                    }
                    jsonArray.put(obj)
                }
                val rawJson = jsonArray.toString()
                val encryptedBytes = CryptoManager.encrypt(rawJson.toByteArray(Charsets.UTF_8))
                outputFile.writeBytes(encryptedBytes)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun importBackup(context: Context, dao: MemoryDao, inputFile: File): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val encryptedBytes = inputFile.readBytes()
                val decryptedBytes = CryptoManager.decrypt(encryptedBytes)
                val rawJson = String(decryptedBytes, Charsets.UTF_8)
                val jsonArray = JSONArray(rawJson)
                val list = mutableListOf<MemoryEntity>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        MemoryEntity(
                            id = obj.optInt("id", 0),
                            text = obj.getString("text"),
                            category = obj.optString("category", "Général"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isProtected = obj.optBoolean("isProtected", false)
                        )
                    )
                }
                dao.insertAll(list)
                Result.success(list.size)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
