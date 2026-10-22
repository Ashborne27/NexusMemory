package com.ashborne.nexusmemory.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Result
import com.ashborne.nexusmemory.backup.BackupManager
import com.ashborne.nexusmemory.data.MemoryDatabase
import com.ashborne.nexusmemory.sync.WebDavManager

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences("nexus_settings", Context.MODE_PRIVATE)
            val webDavUrl = prefs.getString("webdav_url", null) ?: return Result.success()
            val webDavUser = prefs.getString("webdav_user", null) ?: return Result.success()
            val webDavPass = prefs.getString("webdav_pass", null) ?: return Result.success()

            val dao = MemoryDatabase.getDatabase(applicationContext).memoryDao()
            val backupManager = BackupManager(applicationContext, dao)

            val backupJson = backupManager.exportToJson()
            val success = WebDavManager.uploadToWebDav(webDavUrl, webDavUser, webDavPass, backupJson)

            if (success) Result.success() else Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
