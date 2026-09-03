package com.ashborne.nexusmemory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).memoryDao()

    val allMemories: StateFlow<List<MemoryEntity>> = dao.getAllMemories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addMemory(title: String, content: String) {
        viewModelScope.launch {
            dao.insertMemory(MemoryEntity(title = title, content = content))
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            dao.deleteMemory(memory)
        }
    }
}
