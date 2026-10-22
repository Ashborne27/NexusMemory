package com.ashborne.nexusmemory.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Result as WorkResult
import com.ashborne.nexusmemory.backup.BackupManager
import com.ashborne.nexusmemory.data.MemoryDatabase
import com.ashborne.nexusmemory.sync.WebDavManager

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): WorkResult {
        return try {
            val prefs = applicationContext.getSharedPreferences("nexus_settings", Context.MODE_PRIVATE)
            val webDavUrl = prefs.getString("webdav_url", null) ?: return WorkResult.success()
            val webDavUser = prefs.getString("webdav_user", null) ?: return WorkResult.success()
            val webDavPass = prefs.getString("webdav_pass", null) ?: return WorkResult.success()

            val dao = MemoryDatabase.getDatabase(applicationContext).memoryDao()
            val backupManager = BackupManager(applicationContext, dao)

            val backupJson = backupManager.exportToJson()
            val success = WebDavManager.uploadToWebDav(webDavUrl, webDavUser, webDavPass, backupJson)

            if (success) WorkResult.success() else WorkResult.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            WorkResult.retry()
        }
    }
}
