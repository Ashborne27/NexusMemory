package com.ashborne.nexusmemory.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.ashborne.nexusmemory.data.MemoryDatabase
import com.ashborne.nexusmemory.data.MemoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MemoryViewModel(application: Application) : AndroidViewModel(application) {
    private val memoryDao = MemoryDatabase.getDatabase(application).memoryDao()
    val allMemories: LiveData<List<MemoryEntity>> = memoryDao.getAllMemories()

    fun insert(memory: MemoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.insertMemory(memory)
        }
    }

    fun delete(memory: MemoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.deleteMemory(memory)
        }
    }
}
