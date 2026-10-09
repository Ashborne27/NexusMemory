package com.ashborne.nexusmemory.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "memories_fts")
@Fts4(contentEntity = MemoryEntity::class)
data class MemoryFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int,
    val text: String,
    val category: String
)
