package com.ashborne.nexusmemory.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashborne.nexusmemory.backup.BackupManager
import com.ashborne.nexusmemory.data.MemoryDao
import com.ashborne.nexusmemory.data.MemoryEntity
import com.ashborne.nexusmemory.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File

class MemoryViewModel(private val dao: MemoryDao) : ViewModel() {

    val allMemories: Flow<List<MemoryEntity>> = dao.getAllMemories()

    fun searchMemories(query: String): Flow<List<MemoryEntity>> {
        return if (query.isBlank()) {
            dao.getAllMemories()
        } else {
            dao.searchMemoriesFts("*$query*")
        }
    }

    fun addMemory(text: String, category: String, isProtected: Boolean) {
        viewModelScope.launch {
            val finalText = if (isProtected) {
                CryptoManager.encryptToString(text)
            } else {
                text
            }
            val entity = MemoryEntity(
                text = finalText,
                category = category,
                timestamp = System.currentTimeMillis(),
                isProtected = isProtected
            )
            dao.insertMemory(entity)
        }
    }

    fun decryptContent(encryptedText: String): String {
        return try {
            CryptoManager.decryptFromString(encryptedText)
        } catch (e: Exception) {
            "[Erreur de déchiffrement]"
        }
    }

    fun exportBackup(context: Context, outputFile: File, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = BackupManager.exportBackup(context, dao, outputFile)
            onResult(result)
        }
    }

    fun importBackup(context: Context, inputFile: File, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = BackupManager.importBackup(context, dao, inputFile)
            onResult(result)
        }
    }
}
