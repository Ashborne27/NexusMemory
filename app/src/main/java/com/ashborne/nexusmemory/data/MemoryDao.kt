package com.ashborne.nexusmemory.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories")
    suspend fun getAllMemoriesList(): List<MemoryEntity>

    @Query("""
        SELECT memories.* FROM memories
        JOIN memories_fts ON memories.id = memories_fts.docid
        WHERE memories_fts MATCH :query
    """)
    fun searchMemoriesFts(query: String): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(memories: List<MemoryEntity>)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories")
    suspend fun clearAll()
}
