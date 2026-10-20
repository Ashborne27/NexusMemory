package com.ashborne.nexusmemory.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ashborne.nexusmemory.data.MemoryDatabase
import com.ashborne.nexusmemory.data.MemoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = MemoryDatabase.getDatabase(application).memoryDao()

    val allMemories: StateFlow<List<MemoryEntity>> = dao.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insert(title: String, content: String, category: String, isProtected: Boolean) {
        viewModelScope.launch {
            dao.insertMemory(
                MemoryEntity(
                    title = title,
                    content = content,
                    category = category,
                    isProtected = isProtected
                )
            )
        }
    }
}
