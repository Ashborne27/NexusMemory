package com.ashborne.nexusmemory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).memoryDao()

    private val _searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val allMemories: StateFlow<List<MemoryEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                dao.getAllMemories()
            } else {
                dao.searchMemories("%$query%")
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

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
