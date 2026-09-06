package com.ashborne.nexusmemory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database: AppDatabase = AppDatabase.getDatabase(application)
    private val memoryDao: MemoryDao = database.memoryDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allMemories: StateFlow<List<MemoryEntity>> = _searchQuery
        .flatMapLatest { query: String ->
            if (query.isBlank()) {
                memoryDao.getAllMemories()
            } else {
                memoryDao.searchMemories(query)
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
            memoryDao.insertMemory(MemoryEntity(title = title, content = content))
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            memoryDao.deleteMemory(memory)
        }
    }
}
