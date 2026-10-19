package com.ashborne.nexusmemory.data

import androidx.room.Entity
import androidx.room.Fts5
import androidx.room.PrimaryKey

@Entity(tableName = "memories_fts")
@Fts5(contentEntity = MemoryEntity::class)
data class MemoryFtsEntity(
    @PrimaryKey val rowid: Int,
    val title: String,
    val content: String,
    val category: String
)
