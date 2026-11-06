package com.ashborne.nexus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO Room optimisé via KSP pour NexusMemory v12.0.
 * Fournit des flux réactifs asynchrones non bloquants.
 */
@Dao
interface MemoryDao {

    @Query("SELECT * FROM memories ORDER BY id DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :memoryId")
    suspend fun deleteMemory(memoryId: Long)
}
