package com.ashborne.nexusmemory.viewmodel

import android.content.Context
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashborne.nexusmemory.backup.BackupManager
import com.ashborne.nexusmemory.data.MemoryDao
import com.ashborne.nexusmemory.data.MemoryEntity
import com.ashborne.nexusmemory.security.CryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class MemoryViewModel(private val dao: MemoryDao) : ViewModel() {

    private val cryptoManager = CryptoManager()

    val allMemories = dao.getAllMemories()

    fun addMemory(text: String, category: String = "Général", isProtected: Boolean = false) {
        viewModelScope.launch {
            val processedText = if (isProtected) {
                val encrypted = cryptoManager.encrypt(text.toByteArray(Charsets.UTF_8))
                Base64.encodeToString(encrypted, Base64.DEFAULT)
            } else {
                text
            }
            dao.insertMemory(
                MemoryEntity(
                    text = processedText,
                    category = category,
                    isProtected = isProtected,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun decryptContent(encryptedBase64: String): String {
        return try {
            val decodedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT)
            String(cryptoManager.decrypt(decodedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            "Erreur de déchiffrement"
        }
    }

    fun exportBackup(context: Context, outputFile: File, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val manager = BackupManager(context, dao)
            val result = manager.exportBackup(outputFile)
            onResult(result.isSuccess)
        }
    }

    fun importBackup(context: Context, inputFile: File, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val manager = BackupManager(context, dao)
            val result = manager.importBackup(inputFile)
            onResult(result)
        }
    }
}
