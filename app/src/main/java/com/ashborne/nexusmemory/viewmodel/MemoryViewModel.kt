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

    fun addMemory(text: String, category: String, isProtected: Boolean) {
        viewModelScope.launch {
            val finalContent = if (isProtected) {
                CryptoManager.encryptToString(text)
            } else {
                text
            }
            dao.insert(MemoryEntity(text = finalContent, category = category, timestamp = System.currentTimeMillis(), isProtected = isProtected))
        }
    }

    fun decryptContent(encryptedText: String): String {
        return try {
            CryptoManager.decryptFromString(encryptedText)
        } catch (e: Exception) {
            "[Erreur de déchiffrement]"
        }
    }

    fun encryptContent(plainText: String): String {
        return try {
            CryptoManager.encryptToString(plainText)
        } catch (e: Exception) {
            plainText
        }
    }

    fun exportBackup(context: Context, outputFile: File, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = BackupManager.exportBackup(context, dao, outputFile)
            onResult(success)
        }
    }

    fun importBackup(context: Context, inputFile: File, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = BackupManager.importBackup(context, dao, inputFile)
            onResult(result)
        }
    }
}
