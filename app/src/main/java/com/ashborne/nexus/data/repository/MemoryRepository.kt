package com.ashborne.nexus.data.repository

import com.ashborne.nexus.data.cache.MemoryCacheManager
import com.ashborne.nexus.data.local.MemoryDao
import com.ashborne.nexus.data.local.MemoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * Repository unifié pour NexusMemory v12.0.
 * Coordonne le DAO Room et le cache mémoire StateFlow.
 */
class MemoryRepository(private val memoryDao: MemoryDao) {

    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()
        .onEach { memories ->
            MemoryCacheManager.put("cached_memories", memories)
        }

    suspend fun insertMemory(memory: MemoryEntity) {
        memoryDao.insertMemory(memory)
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteMemory(id)
    }
}
