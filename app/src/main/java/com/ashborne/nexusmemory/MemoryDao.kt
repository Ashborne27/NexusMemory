package com.ashborne.nexusmemory

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE (:category IS NULL OR category = :category) AND (:query IS NULL OR content LIKE '%' || :query || '%') ORDER BY isPinned DESC, timestamp DESC")
    fun getFilteredMemories(category: String?, query: String?): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)
}
