package com.ashborne.nexusmemory.data

import androidx.room.Entity
import androidx.room.Fts5
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "memories_fts")
@Fts5(contentEntity = MemoryEntity::class)
data class MemoryFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Long,
    val title: String,
    val content: String,
    val category: String
)
