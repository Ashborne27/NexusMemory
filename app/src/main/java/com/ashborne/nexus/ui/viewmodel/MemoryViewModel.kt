package com.ashborne.nexus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashborne.nexus.data.local.MemoryEntity
import com.ashborne.nexus.data.repository.MemoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel pour NexusMemory v12.0.
 * Expose les flux réactifs du Repository avec une gestion optimisée du cycle de vie.
 */
class MemoryViewModel(private val repository: MemoryRepository) : ViewModel() {

    val memories: StateFlow<List<MemoryEntity>> = repository.allMemories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addMemory(title: String, content: String) {
        viewModelScope.launch {
            repository.insertMemory(MemoryEntity(title = title, content = content))
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }
}
