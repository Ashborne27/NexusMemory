package com.ashborne.nexusmemory.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val content: String,
    val category: String,
    val isProtected: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
