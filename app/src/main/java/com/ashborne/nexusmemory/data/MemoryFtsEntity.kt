package com.ashborne.nexusmemory.data

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "memories_fts")
@Fts4
data class MemoryFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Long,
    val title: String,
    val content: String,
    val category: String
)
