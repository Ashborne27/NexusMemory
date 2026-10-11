package com.ashborne.nexusmemory.sync

import android.content.Context
import com.ashborne.nexusmemory.backup.BackupManager
import com.ashborne.nexusmemory.data.MemoryDao
import java.io.File

object SyncManager {

    suspend fun syncUp(
        context: Context,
        dao: MemoryDao,
        serverUrl: String,
        user: String,
        pass: String
    ): Boolean {
        val tempFile = File(context.cacheDir, "sync_upload.nexus")
        val exportSuccess = BackupManager.exportBackup(context, dao, tempFile)
        if (!exportSuccess) return false

        val encryptedBytes = tempFile.readBytes()
        tempFile.delete()

        val client = WebDavClient(serverUrl, user, pass)
        return client.uploadBackup(encryptedBytes)
    }

    suspend fun syncDown(
        context: Context,
        dao: MemoryDao,
        serverUrl: String,
        user: String,
        pass: String
    ): Result<Int> {
        val client = WebDavClient(serverUrl, user, pass)
        val downloadedBytes = client.downloadBackup() ?: return Result.failure(Exception("Échec du téléchargement WebDAV"))

        val tempFile = File(context.cacheDir, "sync_download.nexus")
        tempFile.writeBytes(downloadedBytes)

        val importResult = BackupManager.importBackup(context, dao, tempFile)
        tempFile.delete()

        return importResult
    }
}
