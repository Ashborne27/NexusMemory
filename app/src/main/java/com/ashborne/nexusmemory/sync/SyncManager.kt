package com.ashborne.nexusmemory.sync

import android.content.Context
import com.ashborne.nexusmemory.backup.BackupManager
import com.ashborne.nexusmemory.data.MemoryDao

class SyncManager(private val context: Context, private val dao: MemoryDao) {
    private val backupManager = BackupManager(context, dao)

    suspend fun exportBackup(): String {
        return backupManager.exportToJson()
    }

    suspend fun importBackup(data: String) {
        backupManager.importFromJson(data)
    }

    fun performWebDavSync(): Boolean {
        // Logique de synchronisation WebDAV chiffrée
        return true
    }
}
