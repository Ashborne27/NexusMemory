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

class BackupManager(
    private val context: Context,
    private val dao: MemoryDao
) {
    private val cryptoManager = CryptoManager()

    suspend fun exportBackup(outputFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val memories = dao.getAllMemoriesList()
            val jsonArray = JSONArray()

            memories.forEach { memory ->
                val jsonObject = JSONObject().apply {
                    put("id", memory.id)
                    put("text", memory.text)
                    put("category", memory.category)
                    put("isProtected", memory.isProtected)
                    put("timestamp", memory.timestamp)
                }
                jsonArray.put(jsonObject)
            }

            val rawJsonBytes = jsonArray.toString().toByteArray(Charsets.UTF_8)
            val encryptedBytes = cryptoManager.encrypt(rawJsonBytes)

            outputFile.writeBytes(encryptedBytes)
        }
    }

    suspend fun importBackup(inputFile: File): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val encryptedBytes = inputFile.readBytes()
            val decryptedBytes = cryptoManager.decrypt(encryptedBytes)
            val jsonString = String(decryptedBytes, Charsets.UTF_8)

            val jsonArray = JSONArray(jsonString)
            val memoriesToInsert = mutableListOf<MemoryEntity>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                memoriesToInsert.add(
                    MemoryEntity(
                        text = obj.getString("text"),
                        category = obj.optString("category", "Général"),
                        isProtected = obj.optBoolean("isProtected", false),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }

            dao.insertAll(memoriesToInsert)
            memoriesToInsert.size
        }
    }
}
