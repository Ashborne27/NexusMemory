package com.ashborne.nexusmemory.backup

import android.content.Context
import com.ashborne.nexusmemory.data.MemoryDao
import com.ashborne.nexusmemory.data.MemoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupManager(private val context: Context, private val dao: MemoryDao) {

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val list = dao.getAllMemoriesList()
        list.joinToString(separator = "\n") { "${it.title}::${it.content}::${it.category}" }
    }

    suspend fun importFromJson(data: String) = withContext(Dispatchers.IO) {
        val lines = data.lines()
        val entities = mutableListOf<MemoryEntity>()
        for (line in lines) {
            val parts = line.split("::")
            if (parts.size >= 3) {
                entities.add(
                    MemoryEntity(
                        title = parts[0],
                        content = parts[1],
                        category = parts[2]
                    )
                )
            }
        }
        if (entities.isNotEmpty()) {
            dao.insertAll(entities)
        }
    }
}
